package com.health.web.assistant.tool;

import com.health.web.assistant.agent.AgentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** T2：声明式「角色→能力→工具」策略的授权与红线校验（纯单测，无需容器） */
class ToolAccessPolicyTest {

    private final ToolAccessPolicy policy = new ToolAccessPolicy();

    private static final Set<String> ALL_TOOLS =
            Set.of("searchDiseases", "searchMembers", "getMemberAssessments", "createPreOrder");

    @Test
    @DisplayName("红线：创建预订单仅限预约管家，分诊/报告均越权")
    void createPreOrderOnlyForScheduler() {
        assertThat(policy.isAllowed(AgentType.SCHEDULER, "createPreOrder")).isTrue();
        assertThat(policy.isAllowed(AgentType.TRIAGE, "createPreOrder")).isFalse();
        assertThat(policy.isAllowed(AgentType.REPORT, "createPreOrder")).isFalse();
    }

    @Test
    @DisplayName("红线：读取评估记录仅报告解读可用，分诊/预约越权")
    void assessmentReadOnlyForReport() {
        assertThat(policy.isAllowed(AgentType.REPORT, "getMemberAssessments")).isTrue();
        assertThat(policy.isAllowed(AgentType.TRIAGE, "getMemberAssessments")).isFalse();
        assertThat(policy.isAllowed(AgentType.SCHEDULER, "getMemberAssessments")).isFalse();
    }

    @Test
    @DisplayName("红线：分诊顾问仅疾病/会员检索，不得读评估、不得下单")
    void triageMinimalSurface() {
        assertThat(policy.isAllowed(AgentType.TRIAGE, "searchDiseases")).isTrue();
        assertThat(policy.isAllowed(AgentType.TRIAGE, "searchMembers")).isTrue();
        assertThat(policy.isAllowed(AgentType.TRIAGE, "getMemberAssessments")).isFalse();
        assertThat(policy.isAllowed(AgentType.TRIAGE, "createPreOrder")).isFalse();
    }

    @Test
    @DisplayName("授权：各角色拥有的工具集合符合声明式能力映射")
    void declaredToolsMatch() {
        assertThat(policy.allowedTools(AgentType.TRIAGE))
                .containsExactlyInAnyOrder("searchDiseases", "searchMembers");
        assertThat(policy.allowedTools(AgentType.REPORT))
                .containsExactlyInAnyOrder("searchMembers", "getMemberAssessments", "searchDiseases");
        assertThat(policy.allowedTools(AgentType.SCHEDULER))
                .containsExactlyInAnyOrder("searchMembers", "createPreOrder");
    }

    @Test
    @DisplayName("越权拦截率：对所有越权尝试，isAllowed 一律拒绝（100% 拦截）")
    void denialRateIsFullForOutOfPolicy() {
        for (AgentType role : AgentType.values()) {
            Set<String> allowed = policy.allowedTools(role);
            for (String tool : ALL_TOOLS) {
                if (!allowed.contains(tool)) {
                    assertThat(policy.isAllowed(role, tool))
                            .as("角色 %s 不应拥有工具 %s", role, tool)
                            .isFalse();
                }
            }
        }
    }
}
