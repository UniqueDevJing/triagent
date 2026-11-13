package com.health.web.assistant.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预算熔断单测：工具次数上限 / token 估算累计 / 熔断计数 / 重置。
 * 注：@Value 默认值仅在 Spring 容器生效；单测通过反射设置阈值（与容器默认一致）。
 */
class BudgetGuardTest {

    private BudgetGuard guard(int maxToolCalls, int maxTokens) throws Exception {
        BudgetGuard g = new BudgetGuard();
        java.lang.reflect.Field fe = BudgetGuard.class.getDeclaredField("enabled");
        fe.setAccessible(true);
        fe.set(g, true);
        java.lang.reflect.Field ft = BudgetGuard.class.getDeclaredField("maxToolCalls");
        ft.setAccessible(true);
        ft.set(g, maxToolCalls);
        java.lang.reflect.Field fk = BudgetGuard.class.getDeclaredField("maxTokens");
        fk.setAccessible(true);
        fk.set(g, maxTokens);
        return g;
    }

    @Test
    @DisplayName("工具次数熔断：上限内放行，超限拒绝；计数继续累计")
    void toolCallCircuitBreaker() throws Exception {
        BudgetGuard g = guard(3, 100000);
        assertThat(g.tryConsumeToolCall("s1")).isTrue();
        assertThat(g.tryConsumeToolCall("s1")).isTrue();
        assertThat(g.tryConsumeToolCall("s1")).isTrue();
        assertThat(g.tryConsumeToolCall("s1")).isFalse(); // 第 4 次 → 熔断
        assertThat(g.tryConsumeToolCall("s1")).isFalse();
        BudgetGuard.BudgetStatus st = g.status("s1");
        assertThat(st.toolCalls()).isEqualTo(5);          // 拒绝也计数
        assertThat(st.toolExceeded()).isTrue();
        assertThat(g.degradeReason("s1")).contains("工具调用次数达上限");
        // 其他会话不受影响
        assertThat(g.tryConsumeToolCall("s2")).isTrue();
    }

    @Test
    @DisplayName("token 预算：chars/2 估算累计，超限触发降级")
    void tokenBudget() throws Exception {
        BudgetGuard g = guard(100, 100);
        assertThat(BudgetGuard.estimateTokens("abcd")).isEqualTo(2);
        assertThat(BudgetGuard.estimateTokens("")).isZero();
        assertThat(BudgetGuard.estimateTokens((String) null)).isZero();
        g.recordTokens("t1", "x".repeat(120)); // 60 tokens
        assertThat(g.tokenExceeded("t1")).isFalse();
        g.recordTokens("t1", "y".repeat(120)); // 累计 120 > 100
        assertThat(g.tokenExceeded("t1")).isTrue();
        assertThat(g.degradeReason("t1")).contains("token 预算耗尽");
        assertThat(g.status("t1").tokens()).isEqualTo(120);
    }

    @Test
    @DisplayName("熔断触发数与重置：超限会话计数，reset 后归零")
    void circuitCountAndReset() throws Exception {
        BudgetGuard g = guard(2, 50);
        g.tryConsumeToolCall("a");
        g.tryConsumeToolCall("a");
        g.tryConsumeToolCall("a"); // a 超限
        g.recordTokens("b", "z".repeat(200)); // b token 超限
        assertThat(g.circuitBreakCount()).isEqualTo(2);
        g.reset("a");
        assertThat(g.toolExceeded("a")).isFalse();
        assertThat(g.circuitBreakCount()).isEqualTo(1);
        g.reset("b");
        assertThat(g.circuitBreakCount()).isZero();
        assertThat(g.degradeReason("b")).isNull();
    }

    @Test
    @DisplayName("开关关闭与空会话：直接放行、不记录")
    void disabledAndNullSafety() throws Exception {
        BudgetGuard g = guard(1, 10);
        java.lang.reflect.Field fe = BudgetGuard.class.getDeclaredField("enabled");
        fe.setAccessible(true);
        fe.set(g, false);
        assertThat(g.tryConsumeToolCall("off")).isTrue();
        assertThat(g.tryConsumeToolCall("off")).isTrue(); // 不熔断
        g.recordTokens("off", "xxxx");                    // 开关关：不累计
        assertThat(g.status("off").tokens()).isZero();
        assertThat(g.tryConsumeToolCall(null)).isTrue();
        assertThat(g.status("unknown").toolCalls()).isZero();
        assertThat(g.tokenExceeded("unknown")).isFalse();
    }
}
