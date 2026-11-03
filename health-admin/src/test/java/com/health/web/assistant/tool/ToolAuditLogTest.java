package com.health.web.assistant.tool;

import com.health.web.assistant.agent.AgentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** T2：工具越权审计日志指标——覆盖率与越权拦截率（纯单测，无需容器） */
class ToolAuditLogTest {

    private static final Set<String> ALL_TOOLS =
            Set.of("searchDiseases", "searchMembers", "getMemberAssessments", "createPreOrder");

    private final ToolAccessPolicy policy = new ToolAccessPolicy();

    @Test
    @DisplayName("审计覆盖率：一次完整请求（4 工具全部尝试）后，覆盖 100% 工具")
    void coverageFullAfterFullAttempt() {
        ToolAuditLog audit = new ToolAuditLog();
        for (String tool : ALL_TOOLS) {
            boolean allowed = policy.isAllowed(AgentType.SCHEDULER, tool);
            audit.record("s1", 1L, AgentType.SCHEDULER, tool, allowed);
        }
        assertThat(audit.coverage(ALL_TOOLS)).isEqualTo(1.0);
        assertThat(audit.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("越权拦截率：SCHEDULER 尝试全部工具 → 2 放行 2 拦截，denialRate=0.5")
    void denialRateReflectsBlocked() {
        ToolAuditLog audit = new ToolAuditLog();
        for (String tool : ALL_TOOLS) {
            boolean allowed = policy.isAllowed(AgentType.SCHEDULER, tool);
            audit.record("s1", 1L, AgentType.SCHEDULER, tool, allowed);
        }
        assertThat(audit.denialRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("红线用例通过率：越权尝试均被记录为未放行，且审计确实捕获")
    void redLineAttemptsRecordedDenied() {
        ToolAuditLog audit = new ToolAuditLog();
        // TRIAGE 试图越权调用 createPreOrder / getMemberAssessments
        audit.record("s2", 2L, AgentType.TRIAGE, "createPreOrder",
                policy.isAllowed(AgentType.TRIAGE, "createPreOrder"));
        audit.record("s2", 2L, AgentType.TRIAGE, "getMemberAssessments",
                policy.isAllowed(AgentType.TRIAGE, "getMemberAssessments"));

        boolean allDenied = audit.recent().stream()
                .filter(e -> "createPreOrder".equals(e.tool()) || "getMemberAssessments".equals(e.tool()))
                .allMatch(e -> !e.allowed());
        assertThat(allDenied).isTrue();
        assertThat(audit.recent()).hasSize(2);
    }
}
