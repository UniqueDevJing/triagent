package com.health.web.assistant.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SSE 事件统一封装。data 为任意可 JSON 序列化的负载（String / Map / POJO）。
 * Controller 通过 SseEmitter.event().name(type.name().toLowerCase()).data(data) 发送。
 */
public class SseEvent {

    private final SseEventType type;
    private final Object data;

    public SseEvent(SseEventType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public SseEventType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public static SseEvent token(String text) {
        return new SseEvent(SseEventType.TOKEN, text);
    }

    public static SseEvent done(String answer, TriageResult triage) {
        return done(answer, triage, null, null);
    }

    public static SseEvent done(String answer, TriageResult triage, String agent) {
        return done(answer, triage, agent, null);
    }

    /** 结束事件：携带完整回答、结构化分诊、命中的 Agent 名称与知识来源引用（RAG，Phase3） */
    public static SseEvent done(String answer, TriageResult triage, String agent, List<Map<String, Object>> sources) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("answer", answer);
        m.put("triage", triage);
        m.put("agent", agent);
        m.put("sources", sources == null ? List.of() : sources);
        return new SseEvent(SseEventType.DONE, m);
    }

    public static SseEvent error(String message) {
        return new SseEvent(SseEventType.ERROR, Map.of("message", message));
    }

    /** 状态机追问（Phase3）：question 为澄清问题，missing 为缺失字段标识 */
    public static SseEvent clarify(String question, List<String> missingFields) {
        return new SseEvent(SseEventType.CLARIFY,
                Map.of("question", question,
                        "missing", missingFields == null ? List.of() : missingFields));
    }

    /** 工具被模型选中，即将执行（透明推理） */
    public static SseEvent toolCall(String name, String arguments) {
        return new SseEvent(SseEventType.TOOL_CALL, Map.of("name", name, "arguments", arguments));
    }

    /** 工具执行完成（透明推理，result 为便于前端展示的摘要） */
    public static SseEvent toolResult(String name, String result) {
        return new SseEvent(SseEventType.TOOL_RESULT, Map.of("name", name, "result", result));
    }

    /** Supervisor 编排决策（Phase5）：如组合请求→应急分诊优先、EMERGENCY 事实拦截预约等 */
    public static SseEvent plan(String decision, String reason) {
        return new SseEvent(SseEventType.PLAN,
                Map.of("decision", decision, "reason", reason));
    }
}
