package com.health.web.assistant.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.web.assistant.model.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

/**
 * 把工具调用过程发布到 SSE 流（透明推理）。
 * 每个请求一个实例，绑定独立的 Sinks.Many，由编排层 merge 到 token 流中。
 */
public class ToolEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ToolEventPublisher.class);
    private static final int MAX_RESULT_LEN = 1200;

    private final Sinks.Many<SseEvent> sink;
    private final ObjectMapper mapper = new ObjectMapper();
    /** Phase4：本轮工具调用/结果事件计数（供审计指标） */
    private final java.util.concurrent.atomic.AtomicInteger callCount =
            new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger resultCount =
            new java.util.concurrent.atomic.AtomicInteger();

    public ToolEventPublisher(Sinks.Many<SseEvent> sink) {
        this.sink = sink;
    }

    public int getCallCount() {
        return callCount.get();
    }

    public int getResultCount() {
        return resultCount.get();
    }

    /** 模型选中工具，即将执行 */
    public void call(String toolName, Object arguments) {
        callCount.incrementAndGet();
        emit(SseEvent.toolCall(toolName, json(arguments)));
    }

    /** 工具执行完成（payload 为便于前端展示的摘要，非全量数据） */
    public void result(String toolName, Object resultSummary) {
        resultCount.incrementAndGet();
        emit(SseEvent.toolResult(toolName, json(resultSummary)));
    }

    /** 结束工具事件流（由编排层在整轮生成完成后调用） */
    public void complete() {
        sink.tryEmitComplete();
    }

    private void emit(SseEvent event) {
        Sinks.EmitResult r = sink.tryEmitNext(event);
        if (r.isFailure()) {
            log.debug("工具事件发送失败: {} ({})", event.getType(), r);
        }
    }

    private String json(Object o) {
        if (o == null) {
            return "null";
        }
        try {
            String s = mapper.writeValueAsString(o);
            return s.length() > MAX_RESULT_LEN ? s.substring(0, MAX_RESULT_LEN) + "…(已截断)" : s;
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}
