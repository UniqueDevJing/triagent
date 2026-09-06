package com.health.web.assistant.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * P1-4：LLM 出口显式超时。
 * Spring AI OpenAI 模型底层复用 Spring Boot 的 RestClient.Builder，
 * 通过 RestClientCustomizer 统一注入连接/读超时，防止 DeepSeek 偶发挂起
 * 占满 SSE 120s 窗口（读超时触发后走既有 error 事件兜底，前端可控）。
 */
@Configuration
public class LlmTimeoutConfig {

    @Bean
    RestClientCustomizer llmTimeoutCustomizer() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(60));
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
