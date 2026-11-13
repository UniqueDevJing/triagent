package com.health.web.assistant.a2a;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A2A 交接评估门（acceptance gate）。
 *
 * <p>「多 Agent」最容易做成名词：路由一改 system prompt 就算协作了。
 * 这里把交接变成**有准入条件的任务派发**：目标 Agent 必须先自证「能接、有工具、
 * 上下文齐、不违反医疗安全」，才能从 CREATED 进入 VALIDATED。
 * 任何一条不满足 → REJECTED + 明确违规清单，并写进交接台账供复盘。
 *
 * <p>规则全部是确定性的（不调用 LLM），因此可单测、可回放、可解释；
 * 医疗安全规则采用 fail-closed，与 triagent 既有的 EMERGENCY 短路保持一致。
 */
public final class HandoffGate {

    public static final String RULE_CONTEXT_REF = "a2a.context-ref";
    public static final String RULE_TARGET_EXISTS = "a2a.target-exists";
    public static final String RULE_SKILL_MATCH = "a2a.skill-match";
    public static final String RULE_CAPABILITY_COVER = "a2a.capability-cover";
    public static final String RULE_EVIDENCE_READY = "a2a.evidence-ready";
    public static final String RULE_MEDICAL_SAFETY = "a2a.medical-safety";
    public static final String RULE_ACCEPTANCE = "a2a.acceptance-defined";

    private final Map<String, AgentCard> cards = new LinkedHashMap<>();

    public HandoffGate(Collection<AgentCard> cardList) {
        if (cardList != null) {
            for (AgentCard c : cardList) {
                cards.put(c.agentId(), c);
            }
        }
    }

    public Collection<AgentCard> cards() {
        return List.copyOf(cards.values());
    }

    /**
     * 交接上下文（只读快照）。availableEvidence 由编排层按会话事实/记忆/状态算出，
     * 评估门不自己查库，保持内核与 RuoYi 脚手架解耦。
     */
    public record HandoffContext(
            boolean emergencyFact,
            boolean supervisorOverride,
            Set<String> availableEvidence) {
        public HandoffContext {
            availableEvidence = availableEvidence == null ? Set.of() : Set.copyOf(availableEvidence);
        }
    }

    public record GateVerdict(
            boolean accepted,
            boolean safetyFallback,
            AgentHandoff handoff,
            List<String> checkedRules,
            List<String> violations,
            String summary) {
    }

    public GateVerdict evaluate(AgentHandoff handoff, HandoffContext ctx) {
        HandoffContext safeCtx = ctx == null
                ? new HandoffContext(false, false, Set.of()) : ctx;
        List<String> checked = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        // ① 上下文引用：交接必须能追溯到具体会话，禁止无源任务
        checked.add(RULE_CONTEXT_REF);
        if (handoff.contextRefs().isEmpty()
                || handoff.sessionId() == null || handoff.sessionId().isBlank()) {
            violations.add(RULE_CONTEXT_REF + ": 交接缺少会话上下文引用");
        }

        // ② 目标 Agent 已注册能力卡（防配置漂移：路由到一个没有卡片的角色）
        checked.add(RULE_TARGET_EXISTS);
        AgentCard target = cards.get(handoff.toAgent());
        if (target == null) {
            violations.add(RULE_TARGET_EXISTS + ": 目标 Agent " + handoff.toAgent() + " 未注册 AgentCard");
        }

        // ③ 意图命中目标技能声明
        checked.add(RULE_SKILL_MATCH);
        if (target != null && !target.supports(handoff.intent())) {
            violations.add(RULE_SKILL_MATCH + ": " + handoff.toAgent()
                    + " 未声明技能 " + handoff.intent());
        }

        // ④ 目标工具白名单覆盖任务所需能力（AgentCard 由 ToolAccessPolicy 生成，故此处即真实权限）
        checked.add(RULE_CAPABILITY_COVER);
        if (target != null && !target.covers(handoff.requiredCapabilities())) {
            List<String> missing = new ArrayList<>(handoff.requiredCapabilities());
            missing.removeAll(target.capabilities());
            violations.add(RULE_CAPABILITY_COVER + ": 目标缺少能力 " + missing);
        }

        // ⑤ 必备证据齐备
        checked.add(RULE_EVIDENCE_READY);
        List<String> missingEvidence = new ArrayList<>(handoff.requiredEvidence());
        missingEvidence.removeAll(safeCtx.availableEvidence());
        if (!missingEvidence.isEmpty()) {
            violations.add(RULE_EVIDENCE_READY + ": 缺少证据 " + missingEvidence);
        }

        // ⑥ 医疗安全（fail-closed）：会话已有 EMERGENCY 事实时，禁止把任务交给预约管家
        checked.add(RULE_MEDICAL_SAFETY);
        boolean safetyFallback = safeCtx.emergencyFact()
                && !safeCtx.supervisorOverride()
                && "SCHEDULER".equals(handoff.toAgent());
        if (safetyFallback) {
            violations.add(RULE_MEDICAL_SAFETY
                    + ": 会话存在 EMERGENCY 分诊事实，拒绝派发预约任务，fail-closed 回退分诊");
        }

        // ⑦ 验收断言必须显式定义，否则「完成」无从判定
        checked.add(RULE_ACCEPTANCE);
        if (handoff.acceptance().isEmpty()) {
            violations.add(RULE_ACCEPTANCE + ": 交接未定义验收断言");
        }

        boolean accepted = violations.isEmpty();
        AgentHandoff result = handoff.transition(
                accepted ? HandoffStatus.VALIDATED : HandoffStatus.REJECTED,
                accepted ? "评估门 " + checked.size() + "/" + checked.size() + " 项通过"
                        : String.join(" | ", violations));
        String summary = accepted
                ? "A2A 交接 " + handoff.taskId() + " " + handoff.fromAgent() + " → "
                        + handoff.toAgent() + " [" + handoff.intent() + "] 评估门 "
                        + checked.size() + " 项全通过"
                : "A2A 交接 " + handoff.taskId() + " 被评估门拒绝：" + violations.get(0);
        return new GateVerdict(accepted, safetyFallback, result,
                List.copyOf(checked), List.copyOf(violations), summary);
    }
}
