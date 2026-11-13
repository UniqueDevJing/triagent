package com.health.web.assistant.memory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Redis 版事实存储：急症拦截结论在服务重启后依然有效（Phase2 落地，2026-10）。
 *
 * <p>TTL 交给 Redis（30 分钟），与内存版的 expired() 语义一致；读取后再做一次
 * expired() 兜底校验，防御极端情况下写入延迟造成的过期残留。
 */
public class RedisSessionFactStore implements SessionFactStore {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionFactStore.class);

    static final String KEY_PREFIX = "agent:fact:";
    static final Duration TTL = Duration.ofMillis(EMERGENCY_TTL_MS);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public RedisSessionFactStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    @Override
    public void recordEmergency(String sessionId, String urgency, List<String> departments, String disclaimer) {
        if (sessionId == null) {
            return;
        }
        try {
            EmergencyFact fact = new EmergencyFact(urgency, departments, disclaimer, System.currentTimeMillis());
            redis.opsForValue().set(key(sessionId), mapper.writeValueAsString(fact), TTL);
        } catch (Exception e) {
            // 事实影响安全拦截，丢失比记忆丢失更严重：记 error 而非 warn
            log.error("急诊事实写入失败（重启后该会话的急症拦截将失效）: {}", e.getMessage());
        }
    }

    @Override
    public EmergencyFact getEmergency(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        try {
            String raw = redis.opsForValue().get(key(sessionId));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            EmergencyFact f = mapper.readValue(raw, EmergencyFact.class);
            return f.expired() ? null : f;   // 兜底校验（正常应由 Redis TTL 过期）
        } catch (Exception e) {
            log.error("急诊事实读取失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void clearEmergency(String sessionId) {
        if (sessionId != null) {
            try {
                redis.delete(key(sessionId));
            } catch (Exception e) {
                log.error("急诊事实清除失败: {}", e.getMessage());
            }
        }
    }
}
