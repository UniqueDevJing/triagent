package com.health.web.assistant.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Redis 版记忆/事实存储：用 Mockito 复刻 CacheBackend 语义（对齐 PreOrderServiceTest 的做法，
 * 纯单测不启容器）。重点钉住三件事：重启后能恢复、截断策略与内存版一致、Redis 故障时降级不抛。
 */
@ExtendWith(MockitoExtension.class)
class RedisAssistantMemoryStoreTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    /** 用内存 Map 模拟 Redis 的 GET/SET，让多轮 append→get 可以串起来 */
    private final AtomicReference<String> memoryRound = new AtomicReference<>();
    private final AtomicReference<String> memoryFact = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenAnswer(inv -> {
            String k = inv.getArgument(0);
            return k.contains("fact") ? memoryFact.get() : memoryRound.get();
        });
        // ValueOperations.set(...) 是 void 方法，只能用 doAnswer 打桩
        lenient().doAnswer(inv -> {
            memoryRound.set(inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString());
        lenient().doAnswer(inv -> {
            memoryRound.set(inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString(), eq(RedisSessionMemoryStore.TTL));
        lenient().doAnswer(inv -> {
            memoryFact.set(inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString(), eq(RedisSessionFactStore.TTL));
        lenient().doAnswer(inv -> null).when(redis).delete(anyString());
    }

    @Test
    @DisplayName("会话记忆：append 后 getHistory 能读回（重启场景的持久化等价物）")
    void memoryRoundtrip() {
        RedisSessionMemoryStore store = new RedisSessionMemoryStore(redis);
        store.append("s1", "我胸痛", "建议尽快就医");
        store.append("s1", "要挂哪个科", "急诊科");
        assertThat(store.getHistory("s1")).contains("我胸痛").contains("急诊科");
    }

    @Test
    @DisplayName("会话记忆：超过 8 轮截掉最旧（与内存版同一策略）")
    void memoryTruncatesToEight() {
        RedisSessionMemoryStore store = new RedisSessionMemoryStore(redis);
        for (int i = 1; i <= 10; i++) {
            store.append("s2", "问" + i, "答" + i);
        }
        String history = store.getHistory("s2");
        // 注意「问1」是「问10」的子串，不能用 contains 反向断言
        assertThat(history).startsWith("用户：问3").contains("用户：问10");
        assertThat(history).doesNotContain("用户：问2");
    }

    @Test
    @DisplayName("会话记忆：Redis 异常时降级为空，不向上抛（记忆可以丢，问答不能 500）")
    void memoryDegradesGracefullyOnRedisFailure() {
        when(valueOps.get(anyString())).thenThrow(new IllegalStateException("connection refused"));
        RedisSessionMemoryStore store = new RedisSessionMemoryStore(redis);
        assertThat(store.getHistory("s3")).isEmpty();          // 读降级
        store.append("s3", "a", "b");                          // 写不抛
        assertThat(store.getHistory("s3")).isEmpty();
    }

    @Test
    @DisplayName("急诊事实：record 后 get 读回相同字段（重启后急症拦截仍生效）")
    void factRoundtrip() {
        RedisSessionFactStore store = new RedisSessionFactStore(redis);
        store.recordEmergency("s4", "EMERGENCY", List.of("急诊科", "心内科"), "请立即前往急诊");
        SessionFactStore.EmergencyFact f = store.getEmergency("s4");
        assertThat(f).isNotNull();
        assertThat(f.urgency()).isEqualTo("EMERGENCY");
        assertThat(f.departments()).containsExactly("急诊科", "心内科");
        assertThat(f.disclaimer()).isEqualTo("请立即前往急诊");
        assertThat(f.expired()).isFalse();
    }

    @Test
    @DisplayName("急诊事实：不存在 / 已过期返回 null")
    void factMissingOrExpired() {
        RedisSessionFactStore store = new RedisSessionFactStore(redis);
        assertThat(store.getEmergency("none")).isNull();
        // 手工写入一条 31 分钟前的事实（内存版同款过期语义，Redis TTL 之外的兜底校验）
        long stale = System.currentTimeMillis() - 31 * 60 * 1000L;
        memoryFact.set("{\"urgency\":\"EMERGENCY\",\"departments\":[\"急诊科\"],\"disclaimer\":\"d\",\"ts\":" + stale + "}");
        assertThat(store.getEmergency("stale")).isNull();
    }
}
