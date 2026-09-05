package com.health.framework.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String CHANNEL = "health-events";

    private final StringRedisTemplate redis;

    public EventPublisher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void publish(String topic, String event, Object data) {
        try {
            String json = data instanceof String s ? s : MAPPER.writeValueAsString(data);
            RealtimeEvent envelope = new RealtimeEvent(topic, event, json);
            String payload = MAPPER.writeValueAsString(envelope);
            redis.convertAndSend(CHANNEL, payload);
            log.debug("已发布实时事件 topic={} event={}", topic, event);
        } catch (Exception e) {
            log.warn("发布实时事件失败: {}", e.getMessage());
        }
    }
}
