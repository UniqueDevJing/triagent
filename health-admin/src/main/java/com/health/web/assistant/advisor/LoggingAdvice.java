package com.health.web.assistant.advisor;

import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 请求/响应日志（最外层，order 最小）。
 */
@Component
public class LoggingAdvice implements AgentAdvice {

    private static final Logger log = LoggerFactory.getLogger(LoggingAdvice.class);

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public AgentRequest before(AgentRequest request) {
        log.info("[agent] 请求 sessionId={} message={}", request.getSessionId(), truncate(request.getMessage()));
        return request;
    }

    @Override
    public void after(AgentRequest request, AgentResponse response) {
        log.info("[agent] 响应 sessionId={} elapsed={}ms blocked={}",
                request.getSessionId(), response.getElapsedMs(), response.isBlocked());
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
