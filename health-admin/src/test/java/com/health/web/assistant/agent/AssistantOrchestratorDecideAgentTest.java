package com.health.web.assistant.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 会话连续性路由决策矩阵（decideAgent 纯函数，无需容器） */
class AssistantOrchestratorDecideAgentTest {

    private final AssistantOrchestrator orchestrator =
            new AssistantOrchestrator(null, null, null, null, null, null, null, null, null, List.of());

    @Test
    @DisplayName("无会话态：按消息字面路由")
    void noSessionFollowsMessage() {
        assertThat(orchestrator.decideAgent(AgentType.SCHEDULER, null, "帮我预约"))
                .isEqualTo(AgentType.SCHEDULER);
        assertThat(orchestrator.decideAgent(AgentType.TRIAGE, null, "我胸痛"))
                .isEqualTo(AgentType.TRIAGE);
    }

    @Test
    @DisplayName("核心修复：预约流程中回答症状「胸痛」→ 沿用预约管家，不被抢转急救")
    void symptomAnswerStaysWithScheduler() {
        assertThat(orchestrator.decideAgent(
                AgentType.TRIAGE, AgentType.SCHEDULER.name(), "症状是胸痛"))
                .isEqualTo(AgentType.SCHEDULER);
    }

    @Test
    @DisplayName("明确分诊求助词 → 才切换到 TRIAGE")
    void triageHelpCueSwitches() {
        assertThat(orchestrator.decideAgent(
                AgentType.TRIAGE, AgentType.SCHEDULER.name(), "胸痛要紧吗？怎么办"))
                .isEqualTo(AgentType.TRIAGE);
    }

    @Test
    @DisplayName("报告/预约关键词命中 → 始终跟随新意图")
    void explicitDirectiveSwitches() {
        assertThat(orchestrator.decideAgent(AgentType.REPORT, AgentType.SCHEDULER.name(), "解读报告"))
                .isEqualTo(AgentType.REPORT);
        assertThat(orchestrator.decideAgent(AgentType.SCHEDULER, AgentType.TRIAGE.name(), "帮我预约呼吸内科"))
                .isEqualTo(AgentType.SCHEDULER);
    }

    @ParameterizedTest
    @CsvSource({
            "cancel, 不约了",
            "cancel, 先不了",
            "triageCue, 胸痛怎么办",
            "triageCue, 严重吗",
            "override, 看过急诊了",
            "noTriageCue, 帮孙明伟预约呼吸内科",
            "noOverride, 我胸痛"
    })
    @DisplayName("意图词表：取消 / 分诊求助 / 急诊澄清放行")
    void intentCues(String kind, String message) {
        switch (kind) {
            case "cancel" -> assertThat(orchestrator.hasCancelIntent(message)).isTrue();
            case "triageCue" -> assertThat(orchestrator.hasTriageHelpIntent(message)).isTrue();
            case "override" -> assertThat(orchestrator.hasEmergencyOverride(message)).isTrue();
            case "noTriageCue" -> assertThat(orchestrator.hasTriageHelpIntent(message)).isFalse();
            case "noOverride" -> assertThat(orchestrator.hasEmergencyOverride(message)).isFalse();
        }
    }
}
