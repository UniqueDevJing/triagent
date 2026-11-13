package com.health.web.assistant.mcp;

import com.health.system.domain.DiseaseLibrary;
import com.health.web.assistant.a2a.AgentCardRegistry;
import com.health.web.assistant.tool.TriageTools;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * triagent 对外发布的 MCP 工具集（只读、无患者 PII、无会话状态）。
 *
 * <p><b>安全边界（刻意设计）</b>：{@code /sse} 握手端点对 MCP 客户端公开，因此这里
 * 只放疾病知识库检索与能力自述。会员检索（含姓名/手机号）、评估记录（含健康数据）、
 * 预约下单（写操作）一律<b>不</b>经 MCP 暴露，仍留在进程内的 {@code AgentToolkit}，
 * 由 ToolAccessGuard 做角色越权校验、Sa-Token 做登录态校验、ToolAuditLog 留痕。
 * 这样「协议开放」与「医疗数据最小权限」不冲突，也是与 CodeCanvas（只暴露无状态工具）
 * 一致的取舍。
 *
 * <p><b>⚠️ 返回类型刻意用 {@code Map} 而不是 {@code String}</b>：Spring AI 1.0.0 的
 * {@code MethodToolCallback} 会对 String 返回值再做一次 JSON 转义，客户端在
 * {@code tools/call} 里拿到的是「被引号包住的 JSON 字符串」而非结构化对象
 * （1.1.8 无此问题）。返回 POJO/Map 时 converter 直接序列化为 JSON 对象，
 * 这是 1.0.0 上唯一能让 MCP 客户端拿到结构化结果的写法，已由契约测试钉死。
 */
public class TriageMcpTools {

    private static final int MAX_KEYWORD = 50;
    private static final int MAX_FIELD = 300;
    private static final int MAX_ITEMS = 20;

    private final TriageTools triageTools;
    private final AgentCardRegistry cards;

    public TriageMcpTools(TriageTools triageTools, AgentCardRegistry cards) {
        this.triageTools = triageTools;
        this.cards = cards;
    }

    @Tool(name = "search_diseases",
            description = "按症状/病名关键词检索疾病知识库（只读，返回病名、分类、症状、病因；不含任何患者数据）")
    public Map<String, Object> searchDiseases(
            @ToolParam(description = "症状或疾病关键词，如 头痛、胸痛、腹痛") String keyword) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (keyword == null || keyword.isBlank()) {
            out.put("ok", false);
            out.put("reason", "缺少检索关键词，请提供症状或病名");
            return out;
        }
        String kw = keyword.trim();
        if (kw.length() > MAX_KEYWORD) {
            kw = kw.substring(0, MAX_KEYWORD);
        }
        try {
            List<DiseaseLibrary> rows = triageTools.searchDiseases(kw);
            out.put("ok", true);
            out.put("keyword", kw);
            out.put("count", rows.size());
            out.put("items", rows.stream().limit(MAX_ITEMS).map(d -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("diseaseName", brief(d.getDiseaseName()));
                row.put("category", brief(d.getCategory()));
                row.put("symptoms", brief(d.getSymptoms()));
                row.put("causes", brief(d.getCauses()));
                return row;
            }).toList());
        } catch (Exception e) {
            // 工具失败返回可读结论，不把异常栈抛给 MCP 客户端
            out.put("ok", false);
            out.put("reason", "疾病库暂不可用：" + e.getClass().getSimpleName());
        }
        return out;
    }

    /** A2A 与 MCP 的联动点：外部 Agent 可先发现 triagent 有哪些子 Agent 能接什么任务 */
    @Tool(name = "describe_agent_cards",
            description = "返回 triagent 三个子 Agent 的 A2A 能力卡（技能、工具白名单、交接评估门规则）")
    public Map<String, Object> describeAgentCards() {
        return cards.discoveryDocument();
    }

    @Tool(name = "describe_tool_contract",
            description = "返回本 MCP Server 的工具契约与安全边界：哪些工具对外、哪些工具仅限进程内及原因")
    public Map<String, Object> describeToolContract() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("server", "triagent-tools");
        out.put("protocol", "MCP / SSE transport (spring-ai-starter-mcp-server-webmvc 1.0.0)");
        out.put("exposedTools", List.of(
                Map.of("name", "search_diseases", "sideEffect", "none", "scope", "疾病知识库只读"),
                Map.of("name", "describe_agent_cards", "sideEffect", "none", "scope", "静态能力自述"),
                Map.of("name", "describe_tool_contract", "sideEffect", "none", "scope", "静态契约自述")));
        out.put("excludedTools", List.of(
                Map.of("name", "searchMembers", "reason", "含姓名/手机号等患者 PII"),
                Map.of("name", "getMemberAssessments", "reason", "含个人健康评估数据"),
                Map.of("name", "createPreOrder", "reason", "写操作，且需两步确认与登录态")));
        out.put("inProcessGuard", "ToolAccessGuard 角色白名单 + ToolAuditLog 审计 + Sa-Token 登录态");
        return out;
    }

    private static String brief(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim();
        return v.length() <= MAX_FIELD ? v : v.substring(0, MAX_FIELD) + "...";
    }
}
