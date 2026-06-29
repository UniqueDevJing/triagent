package com.health.framework.ai;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {

    private final AiConfig aiConfig;
    private final WebClient webClient;

    public DeepSeekClient(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.webClient = WebClient.builder()
                .baseUrl(aiConfig.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .build();
    }

    public Flux<String> chat(String message, List<Map<String, String>> history) {
        List<Map<String, String>> messages = new java.util.ArrayList<>(history);
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> body = Map.of(
                "model", aiConfig.getModel(),
                "messages", messages,
                "stream", true
        );

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(data -> !"[DONE]".equals(data.trim()))
                .map(this::extractContent);
    }

    private String extractContent(String raw) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(raw);
            return node.path("choices").get(0).path("delta").path("content").asText("");
        } catch (Exception e) {
            return "";
        }
    }
}
