package com.health.web.assistant.tool;

import com.health.web.assistant.advisor.AgentBlockedException;
import com.health.web.assistant.agent.AgentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * T5 统一 ToolGateway：所有工具执行的唯一入口——
 * <ol>
 *   <li><b>权限</b>：ToolAccessGuard.check（越权抛 AgentBlockedException + ToolAuditLog 留痕）；</li>
 *   <li><b>熔断</b>：BudgetGuard.tryConsumeToolCall（会话工具调用次数上限）；</li>
 *   <li><b>幂等</b>：声明式幂等键（会话+角色+工具+参数摘要），同键重复调用直接返回首次结果，
 *       不重复执行（对写操作兜底，服务层幂等之外的第二道防线）；</li>
 *   <li><b>超时</b>：工具执行硬超时，超时终止并抛 AgentBlockedException（防模型触发长查询拖死会话）；</li>
 *   <li><b>审计</b>：记录执行耗时与结果状态（权限审计由 ToolAccessGuard 负责）。</li>
 * </ol>
 * 约束：编排层与工具实现都必须经由 {@link #invoke} 执行工具逻辑，不存在绕过守卫的调用路径。
 */
@Component
public class ToolGateway {

    private final ToolAccessGuard guard;
    private final BudgetGuard budget;
    private final long timeoutMs;
    private final com.health.web.assistant.trace.TraceStore traceStore;

    /** 幂等结果缓存（会话粒度）：key = sessionId|role|tool|argsDigest */
    private final Map<String, CachedResult> idempotency = new java.util.concurrent.ConcurrentHashMap<>();

    /** null 结果哨兵：ConcurrentHashMap 不允许 null value */
    private record CachedResult(Object value) {
        static final CachedResult NULL = new CachedResult(null);
    }

    public ToolGateway(ToolAccessGuard guard,
                       BudgetGuard budget,
                       @Value("${assistant.tool.timeout-ms:10000}") long timeoutMs,
                       com.health.web.assistant.trace.TraceStore traceStore) {
        this.guard = guard;
        this.budget = budget;
        this.timeoutMs = timeoutMs;
        this.traceStore = traceStore;
    }

    /**
     * 工具执行唯一入口。
     *
     * @param role       当前 Agent 角色
     * @param sessionId  会话 ID
     * @param userId     用户 ID（审计用）
     * @param toolName   工具名
     * @param args       参数摘要（仅幂等键参与，不做脱敏日志）
     * @param idempotent 是否为幂等写操作（同一会话相同参数直接返回首次结果）
     * @param action     工具逻辑
     */
    public <T> T invoke(AgentType role, String sessionId, Long userId, String toolName,
                        Map<String, Object> args, boolean idempotent, Supplier<T> action) {
        // 1) 权限：越权即抛（内部记录审计）
        guard.check(role, toolName, sessionId, userId);
        // 2) 熔断：会话工具调用次数上限
        if (budget != null && !budget.tryConsumeToolCall(sessionId)) {
            throw new AgentBlockedException("本会话工具调用次数已达上限（预算熔断），已降级为确定性处理");
        }
        // 3) 幂等：同键短路返回首次结果
        String key = idempotent ? sessionId + "|" + role + "|" + toolName + "|" + digest(args) : null;
        if (key != null) {
            CachedResult prev = idempotency.get(key);
            if (prev != null) {
                return cast(prev);
            }
        }
        // 4) 超时 + 5) 执行审计（T6：同时挂 Trace 子 span，任意线程可还原决策链）
        long start = System.currentTimeMillis();
        com.health.web.assistant.trace.RequestTrace.Span span = null;
        if (traceStore != null) {
            com.health.web.assistant.trace.RequestTrace trace = traceStore.active(sessionId);
            if (trace != null) {
                com.health.web.assistant.trace.RequestTrace.Span parent = trace.active();
                span = trace.begin("tool." + toolName, parent == null ? null : parent.spanId());
                span.attr("role", role.name());
            }
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tool-gateway-" + toolName);
            t.setDaemon(true);
            return t;
        });
        try {
            Future<T> future = executor.submit(action::get);
            T result;
            try {
                result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                if (span != null) {
                    span.error("timeout " + timeoutMs + "ms");
                }
                throw new AgentBlockedException(
                        "工具 " + toolName + " 执行超时（" + timeoutMs + "ms），已终止并降级处理");
            } catch (InterruptedException ie) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                if (span != null) {
                    span.error("interrupted");
                }
                throw new AgentBlockedException("工具 " + toolName + " 执行被中断，已降级处理");
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause() == null ? ee : ee.getCause();
                if (span != null) {
                    span.error(String.valueOf(cause.getMessage()));
                }
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new AgentBlockedException("工具 " + toolName + " 执行失败: " + cause.getMessage());
            }
            long elapsed = System.currentTimeMillis() - start;
            if (key != null) {
                idempotency.put(key, result == null ? CachedResult.NULL : new CachedResult(result));
            }
            if (span != null) {
                span.attr("elapsedMs", String.valueOf(elapsed));
                span.ok();
            }
            ToolGatewayLog.audit(toolName, role, sessionId, elapsed, true);
            return result;
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> T cast(CachedResult cached) {
        return cached == CachedResult.NULL ? null : (T) cached.value();
    }

    /** 参数摘要：排序后取 hash，避免 HashMap 迭代顺序不稳定导致幂等键漂移 */
    static String digest(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return "none";
        }
        return Integer.toHexString(new TreeMap<>(args).toString().hashCode());
    }

    /** 独立静态审计日志类，避免 ToolGateway 自身 logger 与脱敏策略耦合（T6 接入点） */
    private static final class ToolGatewayLog {
        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(ToolGatewayLog.class);

        static void audit(String toolName, AgentType role, String sessionId, long elapsedMs, boolean ok) {
            if (log.isInfoEnabled()) {
                log.info("[tool-gateway] tool={} role={} session={} elapsed={}ms ok={}",
                        toolName, role, sessionId, elapsedMs, ok);
            }
        }
    }
}
