package com.health.web.assistant.a2a;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentHandoffTest {

    private static AgentHandoff created() {
        return AgentHandoff.created("s-1", "SUPERVISOR", "REPORT",
                "report.interpret", List.of("getMemberAssessments"),
                List.of("session:s-1"), List.of("user-message"),
                List.of("逐条解读评估记录"));
    }

    @Test
    @DisplayName("新建交接：状态 CREATED、任务 id 唯一、上下文与验收断言齐全")
    void createdContract() {
        AgentHandoff a = created();
        AgentHandoff b = created();

        assertThat(a.status()).isEqualTo(HandoffStatus.CREATED);
        assertThat(a.taskId()).startsWith("task-").isNotEqualTo(b.taskId());
        assertThat(a.fromAgent()).isEqualTo("SUPERVISOR");
        assertThat(a.toAgent()).isEqualTo("REPORT");
        assertThat(a.intent()).isEqualTo("report.interpret");
        assertThat(a.requiredCapabilities()).containsExactly("getMemberAssessments");
        assertThat(a.contextRefs()).containsExactly("session:s-1");
        assertThat(a.acceptance()).isNotEmpty();
        assertThat(a.isCrossAgent()).isTrue();
    }

    @Test
    @DisplayName("状态机只允许 CREATED→VALIDATED→DISPATCHED→FULFILLED")
    void legalTransitions() {
        AgentHandoff h = created()
                .transition(HandoffStatus.VALIDATED, "评估门通过")
                .transition(HandoffStatus.DISPATCHED, "已派发")
                .transition(HandoffStatus.FULFILLED, "已完成");

        assertThat(h.status()).isEqualTo(HandoffStatus.FULFILLED);
        assertThat(h.gateNote()).isEqualTo("已完成");
        assertThat(h.isCrossAgent()).isTrue();
        assertThat(h.contextRefs()).containsExactly("session:s-1");
    }

    @Test
    @DisplayName("拒绝：未评估就完成 / 终态再流转 都抛异常")
    void illegalTransitionsAreRejected() {
        assertThatThrownBy(() -> created().transition(HandoffStatus.FULFILLED, "跳步"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("非法交接状态转移");

        AgentHandoff rejected = created().transition(HandoffStatus.REJECTED, "越权");
        assertThatThrownBy(() -> rejected.transition(HandoffStatus.DISPATCHED, "复活"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("toMap 是可直接下发的 JSON 视图，不丢字段")
    void toMapIsComplete() {
        Map<String, Object> m = created().toMap();

        assertThat(m).containsKeys("taskId", "sessionId", "fromAgent", "toAgent", "intent",
                "requiredCapabilities", "contextRefs", "requiredEvidence", "acceptance",
                "status", "createdAt", "gateNote");
        assertThat(m.get("status")).isEqualTo("CREATED");
    }
}
