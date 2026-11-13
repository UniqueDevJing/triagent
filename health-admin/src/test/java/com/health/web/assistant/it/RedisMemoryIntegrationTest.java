package com.health.web.assistant.it;

import com.health.web.assistant.memory.RedisSessionMemoryStore;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 集成测试（Round 4）：真实 Redis 上的会话记忆读写与降级。
 *
 * <p>跑法：设置 {@code IT_REDIS_HOST}/{@code IT_REDIS_PORT}（CI 由 services 提供），
 * 未设置则 {@code assumeTrue} 跳过——本地无 Redis 时不会误报失败，但也绝不冒充"已验证"。
 *
 * <p>覆盖点（对应 Risk Map R4：真实依赖下的行为未验证）：
 * <ul>
 *   <li>append → getHistory 的往返（序列化/编码正确，不是只测 mock）；</li>
 *   <li>同一会话多次 append 的累积；</li>
 *   <li>Redis 连不上时 append 只告警不抛（降级路径在真实客户端下的行为）。</li>
 * </ul>
 */
@Tag("integration")
class RedisMemoryIntegrationTest {

    private static final String HOST = System.getenv("IT_REDIS_HOST");
    private static final String PORT = System.getenv("IT_REDIS_PORT");

    private static StringRedisTemplate templateFor(String host, int port) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(host, port));
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }

    @Test
    @DisplayName("集成：真实 Redis 上 append/getHistory 往返与累积")
    void appendThenRead() {
        Assumptions.assumeTrue(HOST != null && PORT != null, "IT_REDIS_HOST/PORT 未设置，跳过集成测试");

        RedisSessionMemoryStore store = new RedisSessionMemoryStore(
                templateFor(HOST, Integer.parseInt(PORT)));
        String sessionId = "it-" + UUID.randomUUID();

        assertThat(store.getHistory(sessionId)).as("新会话应为空").isNullOrEmpty();

        store.append(sessionId, "我上周做过体检", "好的，已记录");
        String first = store.getHistory(sessionId);
        assertThat(first).contains("我上周做过体检").contains("已记录");

        store.append(sessionId, "想吃点退烧药", "建议先测量体温并就医");
        String second = store.getHistory(sessionId);
        assertThat(second.length()).as("第二次 append 后历史应变长").isGreaterThan(first.length());
        assertThat(second).contains("退烧药").contains("测量体温");
    }

    @Test
    @DisplayName("集成降级：Redis 端口不通时 append 不抛异常（只告警）")
    void appendSurvivesDeadRedis() {
        Assumptions.assumeTrue(HOST != null, "IT_REDIS 未设置，跳过集成测试");

        RedisSessionMemoryStore store = new RedisSessionMemoryStore(templateFor(HOST, 6390));

        assertThatCode(() -> store.append("it-dead-" + UUID.randomUUID(), "u", "a"))
                .as("降级路径不允许把异常抛给业务")
                .doesNotThrowAnyException();
    }
}