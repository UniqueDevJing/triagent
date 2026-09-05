package com.health.web.assistant.advisor;

import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import com.health.web.assistant.service.AgentMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 审计与指标（最外层 after，order 最大）。
 * Phase1 落日志；Phase4 上报 metrics（耗时 / turnType / 工具次数 / 护栏命中 / 紧急度分布）。
 */
@Component
public class AuditAdvice implements AgentAdvice {

    private static final Logger log = LoggerFactory.getLogger(AuditAdvice.class);

    private final AgentMetricsService metrics;

    public AuditAdvice(AgentMetricsService metrics) {
        this.metrics = metrics;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public AgentRequest before(AgentRequest request) {
        return request;
    }

    @Override
    public void after(AgentRequest request, AgentResponse response) {
        String urgency = response.getTriage() != null ? response.getTriage().getUrgency() : "-";
        String turnType = response.getTurnType() == null ? "LLM" : response.getTurnType();
        String blockedReason = response.isBlocked() ? response.getBlockReason() : null;
        int sources = request.getSources() == null ? 0 : request.getSources().size();

        metrics.record(new AgentMetricsService.TurnMetric(
                System.currentTimeMillis(),
                request.getSessionId(),
                request.getAgentName(),
                turnType,
                urgency,
                response.getElapsedMs(),
                response.getToolCallCount(),
                response.getToolResultCount(),
                sources,
                blockedReason));

        log.info("[agent-audit] session={} agent={} type={} urgency={} elapsed={}ms tools={}/{} sources={}{}",
                request.getSessionId(), request.getAgentName(), turnType, urgency,
                response.getElapsedMs(), response.getToolCallCount(), response.getToolResultCount(),
                sources,
                blockedReason == null ? "" : " blocked=" + blockedReason);
    }
}
