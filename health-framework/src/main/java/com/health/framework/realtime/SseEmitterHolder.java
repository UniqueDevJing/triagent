package com.health.framework.realtime;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class SseEmitterHolder {
    private final String topic;
    private final Long userId;
    private final SseEmitter emitter;

    public SseEmitterHolder(String topic, Long userId, long timeout) {
        this.topic = topic;
        this.userId = userId;
        this.emitter = new SseEmitter(timeout);
    }

    public SseEmitter getEmitter() { return emitter; }
    public String getTopic() { return topic; }
    public Long getUserId() { return userId; }

    public void send(String eventName, String data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    public void sendConnected() throws IOException {
        emitter.send(SseEmitter.event().name("connected")
                .data("{\"topic\":\"" + topic + "\",\"userId\":" + userId + "}"));
    }
}
