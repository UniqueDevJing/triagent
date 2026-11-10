package com.health.web.assistant.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * T6 请求级 Trace（OpenTelemetry span 结构兼容）：
 * traceId/spanId/parentSpanId/name/attributes/events/status。
 * 单次请求的所有 span 组成一颗树，可还原完整决策链（路由 → 护栏 → 分诊 → 工具 → 收尾）。
 * 所有 attributes/events 在写入时即经 {@link SensitiveRedactor} 脱敏。
 */
public class RequestTrace {

    private final String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    private final String sessionId;
    private final long startMs = System.currentTimeMillis();
    private final List<Span> spans = new ArrayList<>();
    private volatile Span current;
    private volatile boolean finished;

    public RequestTrace(String sessionId) {
        this.sessionId = sessionId;
    }

    /** 开启一个顶层 span（parent=trace 根） */
    public Span begin(String name) {
        return begin(name, null);
    }

    public synchronized Span begin(String name, String parentSpanId) {
        Span span = new Span(traceId, UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                parentSpanId, name);
        spans.add(span);
        current = span;
        return span;
    }

    /** 当前活跃 span（供 gateway 挂子 span），无则返回 trace 根 */
    public synchronized Span active() {
        return current;
    }

    public synchronized void finish() {
        this.finished = true;
        for (Span s : spans) {
            s.closeIfOpen();
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public String sessionId() {
        return sessionId;
    }

    /** OTel 风格 JSON 导出（手写序列化，避免 agent-core 引入 Jackson） */
    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"traceId\":\"").append(traceId).append('"')
                .append(",\"sessionId\":\"").append(TraceJson.escape(sessionId)).append('"')
                .append(",\"startMs\":").append(startMs)
                .append(",\"finished\":").append(finished)
                .append(",\"spans\":[");
        for (int i = 0; i < spans.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            spans.get(i).toJson(sb);
        }
        sb.append("]}");
        return sb.toString();
    }

    /** OTel 风格 span */
    public static final class Span {
        private final String traceId;
        private final String spanId;
        private final String parentSpanId;
        private final String name;
        private final long startMs = System.currentTimeMillis();
        private long endMs = 0;
        private String status = "UNSET";
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final List<String> events = new ArrayList<>();

        Span(String traceId, String spanId, String parentSpanId, String name) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.name = name;
        }

        public String spanId() {
            return spanId;
        }

        public Span attr(String key, String value) {
            attributes.put(key, SensitiveRedactor.redactField(key, value));
            return this;
        }

        public Span event(String name) {
            events.add(SensitiveRedactor.redact(name));
            return this;
        }

        public Span event(String name, String detail) {
            events.add(SensitiveRedactor.redact(name + ": " + detail));
            return this;
        }

        public void ok() {
            close("OK");
        }

        public void error(String message) {
            attributes.put("error.message", SensitiveRedactor.redact(message));
            close("ERROR");
        }

        public void closeIfOpen() {
            if (endMs == 0) {
                close("UNSET");
            }
        }

        private void close(String status) {
            this.endMs = System.currentTimeMillis();
            this.status = status;
        }

        private void toJson(StringBuilder sb) {
            sb.append("{\"traceId\":\"").append(traceId).append('"')
                    .append(",\"spanId\":\"").append(spanId).append('"')
                    .append(",\"parentSpanId\":")
                    .append(parentSpanId == null ? "null" : "\"" + parentSpanId + "\"")
                    .append(",\"name\":\"").append(TraceJson.escape(name)).append('"')
                    .append(",\"startMs\":").append(startMs)
                    .append(",\"endMs\":").append(endMs)
                    .append(",\"durationMs\":").append(endMs == 0 ? 0 : endMs - startMs)
                    .append(",\"status\":\"").append(status).append('"')
                    .append(",\"attributes\":{");
            boolean first = true;
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(TraceJson.escape(e.getKey())).append("\":\"")
                        .append(TraceJson.escape(e.getValue())).append('"');
            }
            sb.append("},\"events\":[");
            for (int i = 0; i < events.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(TraceJson.escape(events.get(i))).append('"');
            }
            sb.append("]}");
        }
    }
}
