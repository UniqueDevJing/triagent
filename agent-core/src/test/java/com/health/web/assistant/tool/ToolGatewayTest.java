package com.health.web.assistant.tool;

import com.health.web.assistant.advisor.AgentBlockedException;
import com.health.web.assistant.agent.AgentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T5 统一 ToolGateway 验收：权限 / 熔断 / 幂等 / 超时 / 审计。
 * 核心约束：所有工具执行必须经 invoke 唯一入口，不存在绕过守卫的调用路径。
 */
class ToolGatewayTest {

    private ToolAccessGuard guard;
    private ToolAuditLog audit;
    private BudgetGuard budget;
    private ToolGateway gateway;

    @BeforeEach
    void setup() {
        audit = new ToolAuditLog();
        guard = new ToolAccessGuard(new ToolAccessPolicy(), audit);
        budget = new BudgetGuard();
        ReflectionTestUtils.setField(budget, "enabled", true);
        ReflectionTestUtils.setField(budget, "maxToolCalls", 20);
        ReflectionTestUtils.setField(budget, "maxTokens", 60000);
        gateway = new ToolGateway(guard, budget, 500, null);
    }

    @Test
    @DisplayName("权限：SCHEDULER 调 searchDiseases 被拒并留审计")
    void permissionDeniedAndAudited() {
        assertThatThrownBy(() -> gateway.invoke(AgentType.SCHEDULER, "s1", 1L,
                "searchDiseases", Map.of("keyword", "头痛"), false, () -> "should-not-run"))
                .isInstanceOf(Exception.class);
        assertThat(audit.recent()).hasSize(1);
        assertThat(audit.recent().get(0).allowed()).isFalse();
        assertThat(audit.denialRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("熔断：会话工具调用超上限抛 AgentBlockedException")
    void budgetCircuitBreak() {
        ReflectionTestUtils.setField(budget, "maxToolCalls", 1);
        gateway.invoke(AgentType.TRIAGE, "s2", 1L, "searchDiseases",
                Map.of("keyword", "头痛"), false, () -> "ok");
        assertThatThrownBy(() -> gateway.invoke(AgentType.TRIAGE, "s2", 1L,
                "searchDiseases", Map.of("keyword", "头痛"), false, () -> "ok"))
                .isInstanceOf(AgentBlockedException.class)
                .hasMessageContaining("预算熔断");
    }

    @Test
    @DisplayName("幂等：同会话同参数重复调用只执行一次，返回首次结果")
    void idempotentInvokeExecutesOnce() {
        AtomicInteger executions = new AtomicInteger();
        String first = gateway.invoke(AgentType.SCHEDULER, "s3", 1L, "createPreOrder",
                Map.of("department", "心内科", "date", "2026-11-11"), true, () -> {
                    executions.incrementAndGet();
                    return "order-1";
                });
        String second = gateway.invoke(AgentType.SCHEDULER, "s3", 1L, "createPreOrder",
                Map.of("department", "心内科", "date", "2026-11-11"), true, () -> {
                    executions.incrementAndGet();
                    return "order-2";
                });
        assertThat(executions.get()).isEqualTo(1);
        assertThat(first).isEqualTo("order-1");
        assertThat(second).isEqualTo("order-1");
    }

    @Test
    @DisplayName("幂等：不同参数不共享幂等键")
    void idempotentDifferentArgsExecuteSeparately() {
        gateway.invoke(AgentType.SCHEDULER, "s4", 1L, "createPreOrder",
                Map.of("department", "心内科"), true, () -> "a");
        String b = gateway.invoke(AgentType.SCHEDULER, "s4", 1L, "createPreOrder",
                Map.of("department", "呼吸内科"), true, () -> "b");
        assertThat(b).isEqualTo("b");
    }

    @Test
    @DisplayName("超时：执行超过硬超时被终止并抛 AgentBlockedException")
    void timeoutTerminates() {
        ToolGateway tightGateway = new ToolGateway(guard, budget, 80, null);
        assertThatThrownBy(() -> tightGateway.invoke(AgentType.TRIAGE, "s5", 1L,
                "searchDiseases", Map.of("keyword", "头痛"), false, () -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "late";
                }))
                .isInstanceOf(AgentBlockedException.class)
                .hasMessageContaining("超时");
    }

    @Test
    @DisplayName("审计：成功执行的权限记录 allowed=true")
    void auditRecordsAllowed() {
        gateway.invoke(AgentType.TRIAGE, "s6", 1L, "searchDiseases",
                Map.of("keyword", "头痛"), false, () -> "ok");
        assertThat(audit.recent()).hasSize(1);
        assertThat(audit.recent().get(0).allowed()).isTrue();
    }

    @Test
    @DisplayName("参数摘要：map 迭代顺序不影响幂等键")
    void digestStableAcrossMapOrder() {
        String d1 = ToolGateway.digest(Map.of("a", 1, "b", 2));
        String d2 = ToolGateway.digest(new java.util.TreeMap<>(Map.of("b", 2, "a", 1)));
        assertThat(d1).isEqualTo(d2);
    }
}
