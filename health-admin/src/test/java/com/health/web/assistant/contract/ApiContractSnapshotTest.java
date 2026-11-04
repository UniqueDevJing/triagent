package com.health.web.assistant.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.web.assistant.model.AgentResponse;
import com.health.web.assistant.model.SseEvent;
import com.health.web.assistant.model.SseEventType;
import com.health.web.assistant.tool.AgentToolkit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨层契约快照（P1-1）。
 *
 * <p>这些断言锁死的是**对外协议**，不是实现细节：
 * <ul>
 *   <li>{@link AgentToolkit} 的 {@code @Tool} 工具名与参数名 —— 决定模型能看到的 tool schema；</li>
 *   <li>{@link SseEventType} 的枚举名 —— 前端按字符串匹配事件类型；</li>
 *   <li>{@link AgentResponse} 的 JSON 字段集合 —— 前后端 DTO；</li>
 *   <li>{@link SseEvent} 的 JSON 结构 —— SSE 线上格式。</li>
 * </ul>
 *
 * <p><b>契约变更时的正确做法</b>：先改这里的期望值（= 显式承认协议变了），再改实现，
 * 而不是删断言。历史教训：T1 抽出 agent-core 时 {@code AgentRequest} 构造签名变了，
 * 因为没有这类快照，红队测试一直红着没人发现。
 */
class ApiContractSnapshotTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ───────────────────────── ① 工具 schema ─────────────────────────

    @Test
    @DisplayName("契约：@Tool 工具名与参数名固定（模型可见的工具面）")
    void toolSchemaIsStable() {
        Map<String, String> actual = new TreeMap<>();
        for (Method method : AgentToolkit.class.getDeclaredMethods()) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool == null) {
                continue;
            }
            List<String> params = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                ToolParam param = parameter.getAnnotation(ToolParam.class);
                boolean required = param == null || param.required();
                params.add(parameter.getName() + (required ? "" : "?"));
            }
            params.sort(String::compareTo);
            actual.put(tool.name(), String.join(",", params));
        }

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("createPreOrder", "appointmentDate?,department,memberId?,symptomSummary");
        expected.put("getMemberAssessments", "memberId");
        expected.put("searchDiseases", "keyword");
        expected.put("searchMembers", "keyword");

        assertThat(actual)
                .as("工具 schema 变了 = 模型看到的工具面变了，必须显式确认")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("契约：每个 @Tool 都必须有非空 name 与 description（缺了模型无法正确选择）")
    void everyToolHasNameAndDescription() {
        for (Method method : AgentToolkit.class.getDeclaredMethods()) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool == null) {
                continue;
            }
            assertThat(tool.name()).as("%s 的工具名", method.getName()).isNotBlank();
            assertThat(tool.description()).as("%s 的工具描述", method.getName()).isNotBlank();
        }
    }

    // ───────────────────────── ② SSE 事件类型 ─────────────────────────

    @Test
    @DisplayName("契约：SSE 事件类型枚举固定（前端按字符串匹配）")
    void sseEventTypesAreStable() {
        Set<String> actual = Arrays.stream(SseEventType.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(actual).isEqualTo(new TreeSet<>(List.of(
                "CLARIFY", "DONE", "ERROR", "PLAN", "TOKEN", "TOOL_CALL", "TOOL_RESULT")));
    }

    // ───────────────────────── ③ DTO 与线上 JSON ─────────────────────────

    @Test
    @DisplayName("契约：AgentResponse 的 JSON 字段集合固定")
    void agentResponseJsonFieldsAreStable() throws Exception {
        AgentResponse response = new AgentResponse();
        response.setFullText("x");
        response.setElapsedMs(1L);
        response.setBlocked(false);
        response.setTurnType("TRIAGE");
        response.setToolCallCount(0);
        response.setToolResultCount(0);

        JsonNode node = mapper.readTree(mapper.writeValueAsString(response));
        Set<String> fields = new TreeSet<>();
        node.fieldNames().forEachRemaining(fields::add);

        assertThat(fields).isEqualTo(new TreeSet<>(List.of(
                "blocked", "blockReason", "elapsedMs", "fullText",
                "toolCallCount", "toolResultCount", "triage", "turnType")));
    }

    @Test
    @DisplayName("契约：SseEvent 线上 JSON 为 {type, data}，且各工厂方法的 type 固定")
    void sseEventJsonShapeIsStable() throws Exception {
        assertEnvelope(SseEvent.token("你好"), "TOKEN");
        assertEnvelope(SseEvent.done("答案", null), "DONE");
        assertEnvelope(SseEvent.error("boom"), "ERROR");
        assertEnvelope(SseEvent.clarify("哪不舒服？", List.of("symptom")), "CLARIFY");
        assertEnvelope(SseEvent.toolCall("searchDiseases", "{}"), "TOOL_CALL");
        assertEnvelope(SseEvent.toolResult("searchDiseases", "[]"), "TOOL_RESULT");
        assertEnvelope(SseEvent.plan("TRIAGE", "reason"), "PLAN");
    }

    private void assertEnvelope(SseEvent event, String expectedType) throws Exception {
        JsonNode node = mapper.readTree(mapper.writeValueAsString(event));
        Set<String> fields = new TreeSet<>();
        node.fieldNames().forEachRemaining(fields::add);

        assertThat(node.get("type").asText()).as("事件类型").isEqualTo(expectedType);
        assertThat(fields).as("%s 事件的外层字段", expectedType).isEqualTo(new TreeSet<>(List.of("data", "type")));
    }
}