package com.health.web.assistant.advisor;

/**
 * Advisor.before 中抛出以短路链路，编排层捕获后直接返回 error 事件。
 */
public class AgentBlockedException extends RuntimeException {

    public AgentBlockedException(String message) {
        super(message);
    }
}
