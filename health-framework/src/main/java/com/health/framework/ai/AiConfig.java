package com.health.framework.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private String apiKey;
    private String model = "deepseek-chat";
    private String baseUrl = "https://api.deepseek.com/v1";
}
