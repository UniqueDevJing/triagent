package com.health.framework.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求级可观测性过滤器（health-framework 共享，所有模块自动生效）。
 *
 * <p>每个 HTTP 请求分配 traceId，写入 MDC 与响应头 X-Trace-Id，使整条调用链日志可串联。
 * 与 triagent 已有的「每轮 Agent 运行指标（/metrics）」互补：本过滤器覆盖全部 HTTP 入口
 * （含被 Sa-Token 拒绝的未鉴权请求），Agent 指标覆盖单轮编排内部。二者叠加构成
 * “入口延迟 + 内部编排”的完整可观测视图。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TracingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TracingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long start = System.currentTimeMillis();
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            long ms = System.currentTimeMillis() - start;
            log.info("request traceId={} method={} path={} status={} ms={}",
                    traceId, request.getMethod(), request.getRequestURI(), status, ms);
            MDC.remove("traceId");
        }
    }
}
