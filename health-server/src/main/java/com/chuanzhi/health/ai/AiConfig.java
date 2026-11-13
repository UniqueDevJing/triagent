package com.chuanzhi.health.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private String apiKey;
    private String model;
    private String baseUrl;
}
