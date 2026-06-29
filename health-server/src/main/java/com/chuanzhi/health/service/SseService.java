package com.chuanzhi.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> topicClients = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    /** 订阅指定主题 */
    public SseEmitter subscribe(String topic) {
        SseEmitter emitter = new SseEmitter(0L);
        topicClients.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeClient(topic, emitter));
        emitter.onTimeout(() -> removeClient(topic, emitter));
        emitter.onError(e -> removeClient(topic, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"topic\":\"" + topic + "\"}"));
        } catch (IOException e) {
            removeClient(topic, emitter);
        }
        return emitter;
    }

    private void removeClient(String topic, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> clients = topicClients.get(topic);
        if (clients != null) clients.remove(emitter);
    }

    /** 向指定主题广播事件 */
    public void broadcast(String topic, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> clients = topicClients.get(topic);
        if (clients == null || clients.isEmpty()) return;
        try {
            String json = mapper.writeValueAsString(data);
            for (SseEmitter emitter : clients) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(json));
                } catch (IOException e) {
                    clients.remove(emitter);
                }
            }
        } catch (Exception e) {
            log.warn("SSE广播失败 topic={}", topic, e);
        }
    }

    /** 向所有主题广播 */
    public void broadcastAll(String eventName, Object data) {
        topicClients.keySet().forEach(topic -> broadcast(topic, eventName, data));
    }
}
