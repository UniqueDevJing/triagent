package com.health.web.assistant.a2a;

import com.health.web.assistant.agent.AgentDefinition;
import com.health.web.assistant.agent.AgentRegistry;
import com.health.web.assistant.agent.AgentType;
import com.health.web.assistant.tool.ToolAccessPolicy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A 能力卡注册中心 + 交接契约工厂（health-admin 侧接入）。
 *
 * <p>卡片不是手写的一份静态文案，而是从 {@link ToolAccessPolicy}（运行时唯一权限权威）
 * 与 {@link AgentRegistry}（角色定义）**生成**的，因此「对外声明的能力」和
 * 「实际放行给模型的工具」不会漂移——这是评估门 {@link HandoffGate} 有意义的前提。
 *
 * <p>同时把 Supervisor 的隐式路由翻译成显式 {@link AgentHandoff}：
 * 意图（skill id）、目标必备能力、验收断言都由角色映射表给出，编排层只负责填会话事实。
 */
@Component
public class AgentCardRegistry {

    /** 角色 → 对外声明的技能 id（即交接契约里的 intent） */
    private static final Map<AgentType, String> INTENT = new EnumMap<>(AgentType.class);
    /** 角色 → 完成该任务必须具备的工具能力（交给评估门做 capability-cover 校验） */
    private static final Map<AgentType, List<String>> REQUIRED = new EnumMap<>(AgentType.class);
    /** 角色 → 任务验收断言（A2A 语义里「完成」必须可判定） */
    private static final Map<AgentType, List<String>> ACCEPTANCE = new EnumMap<>(AgentType.class);

    static {
        INTENT.put(AgentType.TRIAGE, "triage.symptom");
        INTENT.put(AgentType.REPORT, "report.interpret");
        INTENT.put(AgentType.SCHEDULER, "appointment.preorder");

        REQUIRED.put(AgentType.TRIAGE, List.of("searchDiseases"));
        REQUIRED.put(AgentType.REPORT, List.of("getMemberAssessments"));
        REQUIRED.put(AgentType.SCHEDULER, List.of("createPreOrder"));

        ACCEPTANCE.put(AgentType.TRIAGE, List.of(
                "输出可解析的结构化 TriageResult（紧急度/科室/置信度/随访）",
                "不给确诊结论并附免责声明"));
        ACCEPTANCE.put(AgentType.REPORT, List.of(
                "逐条解读评估记录且按日期倒序",
                "结尾提示不替代医生诊断"));
        ACCEPTANCE.put(AgentType.SCHEDULER, List.of(
                "两步确认：先生成 PENDING 预订单，用户确认后才生效",
                "同一会员+科室+日期幂等，绝不重复下单"));
    }

    private final Map<String, AgentCard> cards = new LinkedHashMap<>();
    private final HandoffGate gate;

    public AgentCardRegistry(AgentRegistry agentRegistry, ToolAccessPolicy policy) {
        for (AgentType type : AgentType.values()) {
            AgentDefinition definition = agentRegistry.get(type);
            List<String> capabilities = new ArrayList<>(policy.allowedTools(type));
            capabilities.sort(String::compareTo);
            String skillId = INTENT.get(type);
            cards.put(type.name(), new AgentCard(
                    type.name(),
                    definition == null ? type.getDisplayName() : definition.getDisplayName(),
                    descriptionOf(type),
                    "1.0.0",
                    AgentCard.PROTOCOL,
                    List.of("text/plain"),
                    List.of("text/plain", "application/json", "text/event-stream"),
                    List.of(new AgentSkill(
                            skillId,
                            type.getDisplayName(),
                            descriptionOf(type),
                            tagsOf(type),
                            examplesOf(type))),
                    List.copyOf(capabilities),
                    true,
                    type == AgentType.SCHEDULER ? "strict-two-phase-confirm" : "medical-fail-closed"));
        }
        this.gate = new HandoffGate(cards.values());
    }

    public HandoffGate gate() {
        return gate;
    }

    public List<AgentCard> all() {
        return List.copyOf(cards.values());
    }

    public AgentCard get(String agentId) {
        return cards.get(agentId);
    }

    /** 能力卡发现端点的 JSON 视图（A2A agent.json 语义） */
    public Map<String, Object> discoveryDocument() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("protocol", AgentCard.PROTOCOL);
        doc.put("name", "triagent-multi-agent-supervisor");
        doc.put("description", "医疗多智能体分诊系统：Supervisor 按 A2A 交接契约派发任务给分诊/报告/预约三个子 Agent");
        doc.put("version", "1.0.0");
        doc.put("agents", cards.values().stream().map(AgentCard::toMap).toList());
        doc.put("handoffRules", List.of(
                HandoffGate.RULE_CONTEXT_REF, HandoffGate.RULE_TARGET_EXISTS,
                HandoffGate.RULE_SKILL_MATCH, HandoffGate.RULE_CAPABILITY_COVER,
                HandoffGate.RULE_EVIDENCE_READY, HandoffGate.RULE_MEDICAL_SAFETY,
                HandoffGate.RULE_ACCEPTANCE));
        return doc;
    }

    public String intentOf(AgentType type) {
        return INTENT.getOrDefault(type, INTENT.get(AgentType.TRIAGE));
    }

    /** 按角色构造一份待评估的交接契约（状态 CREATED，由评估门决定 VALIDATED/REJECTED） */
    public AgentHandoff createHandoff(String sessionId, String fromAgent, AgentType to,
                                      List<String> contextRefs, List<String> requiredEvidence) {
        return AgentHandoff.created(
                sessionId,
                fromAgent == null ? "SUPERVISOR" : fromAgent,
                to.name(),
                intentOf(to),
                REQUIRED.getOrDefault(to, List.of()),
                contextRefs,
                requiredEvidence,
                ACCEPTANCE.getOrDefault(to, List.of()));
    }

    private static String descriptionOf(AgentType type) {
        return switch (type) {
            case TRIAGE -> "症状分诊：红旗急症确定性短路、多轮澄清、输出结构化分诊结果与置信度";
            case REPORT -> "报告解读：检索会员历史评估记录，逐条通俗解读并标注复查红线";
            case SCHEDULER -> "预约管家：两步确认式科室+日期预占，幂等预订单，急症场景 fail-closed";
        };
    }

    private static List<String> tagsOf(AgentType type) {
        return switch (type) {
            case TRIAGE -> List.of("triage", "emergency", "structured-output");
            case REPORT -> List.of("report", "assessment", "rag");
            case SCHEDULER -> List.of("appointment", "two-phase-confirm", "idempotent");
        };
    }

    private static List<String> examplesOf(AgentType type) {
        return switch (type) {
            case TRIAGE -> List.of("持续头痛三天伴恶心，该挂什么科", "胸痛并出汗");
            case REPORT -> List.of("帮我解读最近的体检报告", "总评分和风险等级说明什么");
            case SCHEDULER -> List.of("帮我约下周二心内科", "改期到 2026-11-20");
        };
    }
}
