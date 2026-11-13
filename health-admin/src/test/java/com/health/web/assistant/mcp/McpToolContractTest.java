package com.health.web.assistant.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.system.domain.DiseaseLibrary;
import com.health.web.assistant.a2a.AgentCardRegistry;
import com.health.web.assistant.agent.AgentRegistry;
import com.health.web.assistant.tool.ToolAccessPolicy;
import com.health.web.assistant.tool.TriageTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MCP 工具契约测试：走真实的 {@link ToolCallback#call(String)} 往返，而不是只看注解。
 *
 * <p>钉死两条对外承诺：
 * ① 发布的工具集合固定为 3 个无 PII 只读工具（患者数据/写操作永不外泄）；
 * ② 工具入参异常、数据源异常都返回可读 JSON 结论，不把异常栈抛给 MCP 客户端。
 */
class McpToolContractTest {

    private final TriageTools triageTools = mock(TriageTools.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolAccessPolicy policy = new ToolAccessPolicy();
    private final AgentCardRegistry cards = new AgentCardRegistry(new AgentRegistry(policy), policy);
    private final TriageMcpTools tools = new TriageMcpTools(triageTools, cards);
    private final ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
            .toolObjects(tools).build();

    private ToolCallback cb(String name) {
        return Arrays.stream(provider.getToolCallbacks())
                .filter(c -> c.getToolDefinition().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("MCP 工具未发布: " + name));
    }

    private Set<String> published() {
        return Arrays.stream(provider.getToolCallbacks())
                .map(c -> c.getToolDefinition().name())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @DisplayName("Spring AI 1.0.0 回归：tools/call 返回结构化 JSON 对象，不是被转义的字符串字面量")
    void callReturnsStructuredJson() throws Exception {
        String raw = cb("describe_tool_contract").call("{}");
        JsonNode n = mapper.readTree(raw);

        assertThat(n.isObject())
                .as("String 返回值在 1.0.0 会被 converter 再包一层引号，必须返回 Map/POJO")
                .isTrue();
        assertThat(n.get("server").asText()).isEqualTo("triagent-tools");
    }

    @Test
    @DisplayName("契约：只发布 3 个无 PII 只读工具，患者数据与写操作绝不外泄")
    void onlySafeToolsArePublished() {
        assertThat(published()).containsExactlyInAnyOrder(
                "search_diseases", "describe_agent_cards", "describe_tool_contract");
        assertThat(published()).doesNotContain(
                "searchMembers", "getMemberAssessments", "createPreOrder");
    }

    @Test
    @DisplayName("契约：每个工具都有非空描述与合法输入 schema")
    void everyToolHasDefinition() {
        for (ToolCallback c : provider.getToolCallbacks()) {
            assertThat(c.getToolDefinition().name()).isNotBlank();
            assertThat(c.getToolDefinition().description()).isNotBlank();
            assertThat(c.getToolDefinition().inputSchema()).isNotBlank();
        }
    }

    @Test
    @DisplayName("真调用：疾病库命中 → 结构化 JSON，只带公开字段")
    void realToolCall() throws Exception {
        DiseaseLibrary d = new DiseaseLibrary();
        d.setDiseaseName("偏头痛");
        d.setCategory("神经内科");
        d.setSymptoms("单侧搏动性头痛");
        d.setCauses("血管舒缩功能异常");
        d.setTreatment("内部治疗方案不应输出");
        when(triageTools.searchDiseases("头痛")).thenReturn(List.of(d));

        JsonNode n = mapper.readTree(cb("search_diseases").call("{\"keyword\":\"头痛\"}"));

        assertThat(n.get("ok").asBoolean()).isTrue();
        assertThat(n.get("keyword").asText()).isEqualTo("头痛");
        assertThat(n.get("count").asInt()).isEqualTo(1);
        assertThat(n.get("items").get(0).get("diseaseName").asText()).isEqualTo("偏头痛");
        assertThat(n.toString()).doesNotContain("内部治疗方案不应输出", "treatment", "prevention");
    }

    @Test
    @DisplayName("空关键词 → 可读降级且不查库")
    void blankKeyword() throws Exception {
        JsonNode n = mapper.readTree(cb("search_diseases").call("{\"keyword\":\"  \"}"));

        assertThat(n.get("ok").asBoolean()).isFalse();
        assertThat(n.get("reason").asText()).contains("缺少检索关键词");
        verifyNoInteractions(triageTools);
    }

    @Test
    @DisplayName("数据源异常 → 可读降级，不抛异常栈")
    void failureDegrades() throws Exception {
        when(triageTools.searchDiseases(anyString())).thenThrow(new RuntimeException("db down"));

        JsonNode n = mapper.readTree(cb("search_diseases").call("{\"keyword\":\"头痛\"}"));

        assertThat(n.get("ok").asBoolean()).isFalse();
        assertThat(n.get("reason").asText()).contains("疾病库暂不可用");
        assertThat(n.toString()).doesNotContain("db down");
    }

    @Test
    @DisplayName("A2A × MCP 联动：外部 Agent 可发现三个子 Agent 与七条评估门规则")
    void agentCardsAreDiscoverable() throws Exception {
        JsonNode n = mapper.readTree(cb("describe_agent_cards").call("{}"));

        assertThat(n.get("protocol").asText()).isEqualTo("A2A-semantic/0.3");
        assertThat(n.get("agents").size()).isEqualTo(3);
        assertThat(n.get("handoffRules").size()).isEqualTo(7);
    }

    @Test
    @DisplayName("工具契约自述明确列出被排除的 PII / 写操作工具及原因")
    void contractExplainsExclusions() throws Exception {
        JsonNode n = mapper.readTree(cb("describe_tool_contract").call("{}"));

        assertThat(n.get("exposedTools").size()).isEqualTo(3);
        assertThat(n.get("excludedTools").toString())
                .contains("searchMembers", "getMemberAssessments", "createPreOrder", "PII");
        assertThat(n.get("inProcessGuard").asText()).contains("ToolAccessGuard");
    }
}
