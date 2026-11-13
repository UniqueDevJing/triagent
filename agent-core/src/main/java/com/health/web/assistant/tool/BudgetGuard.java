package com.health.web.assistant.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 成本/延迟预算与熔断（T5）：单会话粒度的工具调用次数上限 + token 预算上限。
 * 超限后由调用方降级到确定性规则（不消耗 LLM），降级原因经 {@link #degradeReason(String)}
 * 供决策日志留痕（ruleHits=budget-*），满足「熔断触发数可观测」验收。
 *
 * token 采用 chars/2 估算（与 M2 帕累托成本口径一致，无 usage API 依赖）。
 */
@Component
public class BudgetGuard {

    /** 预算熔断开关（配置项 assistant.budget.enabled，默认开） */
    @Value("${assistant.budget.enabled:true}")
    private boolean enabled;

    /** 单会话工具调用次数上限（配置项 assistant.budget.max-tool-calls） */
    @Value("${assistant.budget.max-tool-calls:20}")
    private int maxToolCalls;

    /** 单会话 token 预算上限（估算值，配置项 assistant.budget.max-tokens） */
    @Value("${assistant.budget.max-tokens:60000}")
    private int maxTokens;

    private static final class Usage {
        final AtomicInteger toolCalls = new AtomicInteger();
        final AtomicLong tokens = new AtomicLong();
    }

    private final Map<String, Usage> sessions = new ConcurrentHashMap<>();

    /** token 估算：中文/混合文本按 chars/2（与 M2 成本口径一致） */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }

    /**
     * 工具入口熔断：会话内第 n 次工具调用前调用。
     * 超过上限返回 false（调用方应抛 AgentBlockedException 降级），计数仍累加以反映真实压力。
     */
    public boolean tryConsumeToolCall(String sessionId) {
        if (!enabled || sessionId == null) {
            return true;
        }
        return sessions.computeIfAbsent(sessionId, k -> new Usage()).toolCalls.incrementAndGet() <= maxToolCalls;
    }

    /** 记录一次 LLM 输出的估算 token（会话累计） */
    public void recordTokens(String sessionId, String text) {
        if (!enabled || sessionId == null || text == null || text.isEmpty()) {
            return;
        }
        sessions.computeIfAbsent(sessionId, k -> new Usage()).tokens.addAndGet(estimateTokens(text));
    }

    /** 会话 token 预算是否已耗尽 */
    public boolean tokenExceeded(String sessionId) {
        Usage u = sessions.get(sessionId);
        return u != null && u.tokens.get() > maxTokens;
    }

    /** 会话工具调用是否已达上限 */
    public boolean toolExceeded(String sessionId) {
        Usage u = sessions.get(sessionId);
        return u != null && u.toolCalls.get() > maxToolCalls;
    }

    /** 熔断触发数：当前处于任一超限状态的会话数（验收指标） */
    public int circuitBreakCount() {
        return (int) sessions.values().stream()
                .filter(u -> u.toolCalls.get() > maxToolCalls || u.tokens.get() > maxTokens)
                .count();
    }

    /** 降级原因（未超限返回 null；供决策日志 ruleHits/说明） */
    public String degradeReason(String sessionId) {
        if (tokenExceeded(sessionId)) {
            return "会话 token 预算耗尽（估算 " + sessions.get(sessionId).tokens.get() + " > " + maxTokens + "）";
        }
        if (toolExceeded(sessionId)) {
            return "会话工具调用次数达上限（" + maxToolCalls + "）";
        }
        return null;
    }

    /** 会话用量快照（供看板/测试） */
    public BudgetStatus status(String sessionId) {
        Usage u = sessions.get(sessionId);
        return new BudgetStatus(
                u == null ? 0 : u.toolCalls.get(),
                u == null ? 0 : u.tokens.get(),
                toolExceeded(sessionId),
                tokenExceeded(sessionId));
    }

    /** 重置某会话（管理员/测试用） */
    public void reset(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    /** 会话用量快照 */
    public record BudgetStatus(int toolCalls, long tokens, boolean toolExceeded, boolean tokenExceeded) {
    }
}
