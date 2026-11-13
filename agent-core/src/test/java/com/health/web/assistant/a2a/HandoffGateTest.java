package com.health.web.assistant.a2a;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HandoffGateTest {

    private static AgentCard card(String id, String skillId, List<String> caps) {
        return new AgentCard(id, id, "desc", "1.0.0", AgentCard.PROTOCOL,
                List.of("text/plain"), List.of("application/json"),
                List.of(new AgentSkill(skillId, "skill", "desc", List.of("t"), List.of())),
                caps, true, "medical-fail-closed");
    }

    private static final List<AgentCard> CARDS = List.of(
            card("TRIAGE", "triage.symptom", List.of("searchDiseases")),
            card("REPORT", "report.interpret", List.of("getMemberAssessments")),
            card("SCHEDULER", "appointment.preorder", List.of("createPreOrder")));

    private static AgentHandoff handoff(String to, String intent, List<String> required,
                                        List<String> refs, List<String> evidence,
                                        List<String> acceptance) {
        return new AgentHandoff("task-x", "s-1", "SUPERVISOR", to, intent,
                required, refs, evidence, acceptance,
                HandoffStatus.CREATED, System.currentTimeMillis(), null);
    }

    private static AgentHandoff ok(String to, String intent, List<String> required) {
        return handoff(to, intent, required, List.of("session:s-1"),
                List.of("user-message"), List.of("验收断言"));
    }

    private static HandoffGate.HandoffContext ctx(boolean emergency, Set<String> evidence) {
        return new HandoffGate.HandoffContext(emergency, false, evidence);
    }

    @Test
    @DisplayName("七条规则全通过 → VALIDATED，checkedRules 完整")
    void allRulesPass() {
        HandoffGate gate = new HandoffGate(CARDS);
        HandoffGate.GateVerdict v = gate.evaluate(
                ok("REPORT", "report.interpret", List.of("getMemberAssessments")),
                ctx(false, Set.of("user-message")));

        assertThat(v.accepted()).isTrue();
        assertThat(v.violations()).isEmpty();
        assertThat(v.checkedRules()).containsExactly(
                HandoffGate.RULE_CONTEXT_REF, HandoffGate.RULE_TARGET_EXISTS,
                HandoffGate.RULE_SKILL_MATCH, HandoffGate.RULE_CAPABILITY_COVER,
                HandoffGate.RULE_EVIDENCE_READY, HandoffGate.RULE_MEDICAL_SAFETY,
                HandoffGate.RULE_ACCEPTANCE);
        assertThat(v.handoff().status()).isEqualTo(HandoffStatus.VALIDATED);
        assertThat(v.summary()).contains("评估门 7 项全通过");
        assertThat(v.safetyFallback()).isFalse();
    }

    @Test
    @DisplayName("目标未注册能力卡 / 意图不匹配 → REJECTED")
    void targetAndSkillRules() {
        HandoffGate gate = new HandoffGate(CARDS);

        HandoffGate.GateVerdict unknown = gate.evaluate(
                ok("UNKNOWN", "report.interpret", List.of()), ctx(false, Set.of("user-message")));
        assertThat(unknown.accepted()).isFalse();
        assertThat(unknown.violations()).anyMatch(s -> s.contains(HandoffGate.RULE_TARGET_EXISTS));

        HandoffGate.GateVerdict wrongSkill = gate.evaluate(
                ok("REPORT", "appointment.preorder", List.of()), ctx(false, Set.of("user-message")));
        assertThat(wrongSkill.accepted()).isFalse();
        assertThat(wrongSkill.violations()).anyMatch(s -> s.contains(HandoffGate.RULE_SKILL_MATCH));
    }

    @Test
    @DisplayName("能力覆盖：卡片由 ToolAccessPolicy 生成，权限漂移时拒绝交接")
    void capabilityCoverage() {
        HandoffGate.GateVerdict v = new HandoffGate(CARDS).evaluate(
                ok("SCHEDULER", "appointment.preorder",
                        List.of("createPreOrder", "getMemberAssessments")),
                ctx(false, Set.of("user-message")));

        assertThat(v.accepted()).isFalse();
        assertThat(v.violations()).anyMatch(s -> s.contains("getMemberAssessments"));
        assertThat(v.handoff().status()).isEqualTo(HandoffStatus.REJECTED);
    }

    @Test
    @DisplayName("医疗安全 fail-closed：EMERGENCY 事实下拒绝派发预约任务")
    void medicalSafetyFailClosed() {
        HandoffGate.GateVerdict v = new HandoffGate(CARDS).evaluate(
                ok("SCHEDULER", "appointment.preorder", List.of("createPreOrder")),
                ctx(true, Set.of("user-message", "emergency-fact")));

        assertThat(v.accepted()).isFalse();
        assertThat(v.safetyFallback()).isTrue();
        assertThat(v.violations()).anyMatch(s -> s.contains(HandoffGate.RULE_MEDICAL_SAFETY));
        assertThat(v.handoff().status()).isEqualTo(HandoffStatus.REJECTED);
    }

    @Test
    @DisplayName("证据缺失 / 上下文引用缺失 / 验收断言为空 → 逐条拒绝")
    void missingEvidenceContextAndAcceptance() {
        HandoffGate gate = new HandoffGate(CARDS);

        assertThat(gate.evaluate(ok("TRIAGE", "triage.symptom", List.of("searchDiseases")),
                ctx(false, Set.of())).violations())
                .anyMatch(s -> s.contains(HandoffGate.RULE_EVIDENCE_READY));

        assertThat(gate.evaluate(handoff("TRIAGE", "triage.symptom", List.of(),
                        List.of(), List.of("user-message"), List.of("验收")),
                ctx(false, Set.of("user-message"))).violations())
                .anyMatch(s -> s.contains(HandoffGate.RULE_CONTEXT_REF));

        assertThat(gate.evaluate(handoff("TRIAGE", "triage.symptom", List.of(),
                        List.of("session:s-1"), List.of("user-message"), List.of()),
                ctx(false, Set.of("user-message"))).violations())
                .anyMatch(s -> s.contains(HandoffGate.RULE_ACCEPTANCE));
    }

    @Test
    @DisplayName("台账按会话回放，统计放行/拒绝数")
    void ledgerReplay() {
        HandoffLedger ledger = new HandoffLedger();
        ledger.append(new HandoffGate(CARDS).evaluate(
                ok("TRIAGE", "triage.symptom", List.of("searchDiseases")),
                ctx(false, Set.of("user-message"))).handoff());
        ledger.append(new HandoffGate(CARDS).evaluate(
                ok("SCHEDULER", "appointment.preorder", List.of("createPreOrder")),
                ctx(true, Set.of("user-message"))).handoff());
        // 另一会话的交接，验证回放按 sessionId 隔离
        ledger.append(AgentHandoff.created("s-2", "SUPERVISOR", "TRIAGE",
                "triage.symptom", List.of("searchDiseases"), List.of("session:s-2"),
                List.of("user-message"), List.of("验收")));

        assertThat(ledger.size()).isEqualTo(3);
        assertThat(ledger.replay("s-1")).hasSize(2);
        assertThat(ledger.accepted()).isEqualTo(1);
        assertThat(ledger.rejected()).isEqualTo(1);
    }
}
