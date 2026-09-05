package com.health.framework.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RedisSubscriber.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    final Map<String, SseEmitterHolder> emitters = new ConcurrentHashMap<>();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            RealtimeEvent ev = MAPPER.readValue(body, RealtimeEvent.class);
            String prefix = ev.getTopic() + ":";
            emitters.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(prefix))
                    .forEach(entry -> {
                        try {
                            entry.getValue().send(ev.getEvent(), ev.getData());
                        } catch (Exception ex) {
                            emitters.remove(entry.getKey());
                        }
                    });
        } catch (Exception e) {
            log.warn("处理实时事件失败: {}", e.getMessage());
        }
    }

    public void register(String key, SseEmitterHolder holder) {
        emitters.put(key, holder);
    }

    public void unregister(String key) {
        emitters.remove(key);
    }
}
