package com.health.web.assistant.memory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 记忆层实现装配：agent.memory.store = memory（默认）| redis。
 *
 * <p>抽出接口而不直接改类的原因：单实例自用时内存版零依赖、行为可预测；
 * 需要「重启不丢上下文 / 多实例一致」时切 Redis，改一个配置项即可，测试也各自独立。
 */
@Configuration
public class AssistantMemoryConfig {

    @Bean
    public SessionMemoryStore sessionMemoryStore(
            StringRedisTemplate redisTemplate,
            @Value("${agent.memory.store:memory}") String mode) {
        return "redis".equalsIgnoreCase(mode)
                ? new RedisSessionMemoryStore(redisTemplate)
                : new InMemorySessionMemoryStore();
    }

    @Bean
    public SessionFactStore sessionFactStore(
            StringRedisTemplate redisTemplate,
            @Value("${agent.memory.store:memory}") String mode) {
        return "redis".equalsIgnoreCase(mode)
                ? new RedisSessionFactStore(redisTemplate)
                : new InMemorySessionFactStore();
    }
}
