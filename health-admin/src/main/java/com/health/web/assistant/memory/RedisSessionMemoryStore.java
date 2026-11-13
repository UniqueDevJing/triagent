package com.health.web.assistant.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 版会话记忆：服务重启 / 多实例部署后上下文不丢（Phase2 落地，2026-10）。
 *
 * <p>降级策略：记忆属于「上下文」而非「正确性」——Redis 任何异常都只记 warn 并按无记忆处理，
 * 绝不让问答链路因为记忆层 500。这与数据层的取舍正好相反（预订单幂等是正确性，必须硬失败）。
 */
public class RedisSessionMemoryStore implements SessionMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionMemoryStore.class);

    static final int MAX_ROUNDS = 8;
    /** 会话记忆存活时长：与「一次咨询会话」的生命周期对齐，闲置 2 小时后遗忘 */
    static final Duration TTL = Duration.ofHours(2);
    static final String KEY_PREFIX = "agent:memory:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public RedisSessionMemoryStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    @Override
    public String getHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "";
        }
        try {
            String raw = redis.opsForValue().get(key(sessionId));
            if (raw == null || raw.isBlank()) {
                return "";
            }
            List<String> rounds = mapper.readValue(raw, new TypeReference<List<String>>() { });
            return String.join("\n", rounds);
        } catch (Exception e) {
            log.warn("会话记忆读取失败（按无记忆处理）: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public void append(String sessionId, String userMsg, String assistantMsg) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            String raw = redis.opsForValue().get(key(sessionId));
            List<String> rounds = raw == null || raw.isBlank()
                    ? new ArrayList<>()
                    : mapper.readValue(raw, new TypeReference<List<String>>() { });
            rounds.add("用户：" + userMsg + "\n助手：" + assistantMsg);
            if (rounds.size() > MAX_ROUNDS) {
                rounds.subList(0, rounds.size() - MAX_ROUNDS).clear();   // 与内存版同一截断策略
            }
            redis.opsForValue().set(key(sessionId), mapper.writeValueAsString(rounds), TTL);
        } catch (Exception e) {
            log.warn("会话记忆写入失败（本轮上下文可能丢失）: {}", e.getMessage());
        }
    }
}
