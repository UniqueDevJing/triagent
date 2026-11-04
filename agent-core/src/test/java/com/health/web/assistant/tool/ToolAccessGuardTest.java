package com.health.web.assistant.tool;

import com.health.web.assistant.agent.AgentType;
import com.health.web.assistant.advisor.AgentBlockedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T2 加固 / 协同 T4：ToolAccessGuard 真实越权拦截路径（可观测、可审计、可抛异常） */
class ToolAccessGuardTest {

    private final ToolAccessPolicy policy = new ToolAccessPolicy();
    private final ToolAuditLog audit = new ToolAuditLog();

    @Test
    @DisplayName("授权工具：放行且审计记为 allowed=true")
    void allowedToolPassesAndAudited() {
        ToolAccessGuard guard = new ToolAccessGuard(policy, audit);
        guard.check(AgentType.SCHEDULER, "createPreOrder", "s1", 1L);
        assertThat(audit.recent()).hasSize(1);
        assertThat(audit.recent().get(0).allowed()).isTrue();
        assertThat(audit.recent().get(0).tool()).isEqualTo("createPreOrder");
    }

    @Test
    @DisplayName("越权工具：抛出 AgentBlockedException 且审计记为 allowed=false（真实 deny 事件）")
    void deniedToolThrowsAndAudited() {
        ToolAccessGuard guard = new ToolAccessGuard(policy, audit);
        assertThatThrownBy(() -> guard.check(AgentType.TRIAGE, "createPreOrder", "s2", 2L))
                .isInstanceOf(AgentBlockedException.class);
        assertThat(audit.recent()).hasSize(1);
        assertThat(audit.recent().get(0).allowed()).isFalse();
        assertThat(audit.denialRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("混淆尝试：分诊顾问越权读评估记录同样被拦截并留痕")
    void triageCannotReadAssessments() {
        ToolAccessGuard guard = new ToolAccessGuard(policy, audit);
        assertThatThrownBy(() -> guard.check(AgentType.TRIAGE, "getMemberAssessments", "s3", 3L))
                .isInstanceOf(AgentBlockedException.class);
        assertThat(audit.recent().get(0).allowed()).isFalse();
    }
}
