package com.health.web.assistant.advisor;

import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;

/**
 * Agent 横切关注点抽象（对应修订版 Advisor 链的有序管道）。
 * 编排层按 getOrder() 升序执行 before，逆序执行 after。
 */
public interface AgentAdvice {

    /** 执行顺序，越小越先执行 */
    int getOrder();

    /**
     * 请求前处理。可抛出 AgentBlockedException 短路整个链路（如输入护栏拦截）。
     */
    AgentRequest before(AgentRequest request);

    /**
     * 响应后观察（日志 / 审计 / 输出护栏）。不应抛异常。
     */
    void after(AgentRequest request, AgentResponse response);
}
