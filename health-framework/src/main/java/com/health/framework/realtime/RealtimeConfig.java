package com.health.framework.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RealtimeConfig {
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory cf, RedisSubscriber sub) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        c.addMessageListener(sub, new ChannelTopic(EventPublisher.CHANNEL));
        return c;
    }
}
