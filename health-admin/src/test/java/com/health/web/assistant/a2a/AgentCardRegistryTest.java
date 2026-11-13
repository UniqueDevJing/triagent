package com.health.web.assistant.a2a;

import com.health.web.assistant.agent.AgentRegistry;
import com.health.web.assistant.agent.AgentType;
import com.health.web.assistant.tool.ToolAccessPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCardRegistryTest {

    private final ToolAccessPolicy policy = new ToolAccessPolicy();
    private final AgentCardRegistry cards = new AgentCardRegistry(new AgentRegistry(policy), policy);

    @Test
    @DisplayName("三个子 Agent 各有一张能力卡，协议语义字段完整")
    void threeCards() {
        assertThat(cards.all()).hasSize(3)
                .extracting(AgentCard::agentId)
                .containsExactlyInAnyOrder("TRIAGE", "REPORT", "SCHEDULER");

        for (AgentCard c : cards.all()) {
            assertThat(c.protocol()).isEqualTo(AgentCard.PROTOCOL);
            assertThat(c.inputModes()).isNotEmpty();
            assertThat(c.outputModes()).contains("text/event-stream");
            assertThat(c.skills()).hasSize(1);
            assertThat(c.skills().get(0).id()).isNotBlank();
            assertThat(c.streaming()).isTrue();
            assertThat(c.toMap()).containsKeys("agentId", "skills", "capabilities", "safetyLevel");
        }
    }

    @Test
    @DisplayName("卡片能力由 ToolAccessPolicy 生成：自述权限与运行时放行权限零漂移")
    void capabilitiesMatchRuntimePolicy() {
        for (AgentType type : AgentType.values()) {
            AgentCard card = cards.get(type.name());
            Set<String> declared = Set.copyOf(card.capabilities());
            assertThat(declared)
                    .as("%s 的 AgentCard 必须与 ToolAccessPolicy 一致", type)
                    .isEqualTo(policy.allowedTools(type));
        }
        // 预约管家不能有读评估记录的权限（医疗数据最小权限）
        assertThat(cards.get("SCHEDULER").capabilities())
                .contains("createPreOrder")
                .doesNotContain("getMemberAssessments");
    }

    @Test
    @DisplayName("交接工厂按角色给出意图/必备能力/验收断言，评估门正常放行")
    void handoffFactoryAndGate() {
        for (AgentType type : AgentType.values()) {
            AgentHandoff h = cards.createHandoff("s-1", "SUPERVISOR", type,
                    List.of("session:s-1"), List.of("user-message"));

            assertThat(h.status()).isEqualTo(HandoffStatus.CREATED);
            assertThat(h.intent()).isNotBlank();
            assertThat(cards.get(type.name()).supports(h.intent())).isTrue();
            assertThat(h.requiredCapabilities()).isNotEmpty();
            assertThat(h.acceptance()).isNotEmpty();

            HandoffGate.GateVerdict v = cards.gate().evaluate(h,
                    new HandoffGate.HandoffContext(false, false, Set.of("user-message")));
            assertThat(v.accepted()).as("%s 的正常交接应放行", type).isTrue();
            assertThat(v.violations()).isEmpty();
        }
    }

    @Test
    @DisplayName("医疗安全：会话有 EMERGENCY 事实时，预约交接被评估门拒绝")
    void emergencyBlocksSchedulerHandoff() {
        AgentHandoff h = cards.createHandoff("s-1", "SUPERVISOR", AgentType.SCHEDULER,
                List.of("session:s-1", "fact:EMERGENCY"), List.of("user-message"));
        HandoffGate.GateVerdict v = cards.gate().evaluate(h,
                new HandoffGate.HandoffContext(true, false,
                        Set.of("user-message", "emergency-fact")));

        assertThat(v.accepted()).isFalse();
        assertThat(v.safetyFallback()).isTrue();
        assertThat(v.handoff().status()).isEqualTo(HandoffStatus.REJECTED);
        assertThat(v.summary()).contains("评估门拒绝");
    }

    @Test
    @DisplayName("发现文档含全部 Agent 与评估门规则，可直接作为 agent.json 响应")
    void discoveryDocument() {
        Map<String, Object> doc = cards.discoveryDocument();

        assertThat(doc.get("protocol")).isEqualTo(AgentCard.PROTOCOL);
        assertThat((List<?>) doc.get("agents")).hasSize(3);
        List<?> ruleList = (List<?>) doc.get("handoffRules");
        assertThat(ruleList.stream().map(String::valueOf).toList()).hasSize(7)
                .contains(HandoffGate.RULE_MEDICAL_SAFETY, HandoffGate.RULE_CAPABILITY_COVER);
    }
}
