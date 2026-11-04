package com.health.web.assistant.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * DashScope 嵌入客户端（T-A）：OpenAI 兼容 /embeddings 协议，默认 text-embedding-v3。
 *
 * <p>降级边界：超时 / 非 200 / 响应缺字段 → 返回 null（不抛出），由检索器统一降级。
 * 嵌入服务挂掉只损失向量腿，RAG 主链路不受影响。
 */
@Component
@ConditionalOnProperty(name = "assistant.vector-rag.enabled", havingValue = "true")
public class DashScopeEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public DashScopeEmbeddingClient(
            @Value("${assistant.vector-rag.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${assistant.vector-rag.api-key:${LLM_API_KEY:}}") String apiKey,
            @Value("${assistant.vector-rag.model:text-embedding-v3}") String model) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model;
        if (this.apiKey.isBlank()) {
            log.warn("[vector-rag] 未配置嵌入 API key（assistant.vector-rag.api-key），向量腿将不可用并降级纯关键词");
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank() || apiKey.isBlank()) {
            return null;
        }
        try {
            String payload = mapper.writeValueAsString(java.util.Map.of(
                    "model", model,
                    "input", List.of(text.length() > 6000 ? text.substring(0, 6000) : text),
                    "dimensions", 1024));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/embeddings"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                String body = resp.body() == null ? "" : resp.body();
                log.debug("[vector-rag] 嵌入返回 {}：{}", resp.statusCode(),
                        body.substring(0, Math.min(120, body.length())));
                return null;
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode arr = root.path("data").path(0).path("embedding");
            if (!arr.isArray() || arr.isEmpty()) {
                return null;
            }
            float[] v = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                v[i] = (float) arr.get(i).asDouble();
            }
            return v;
        } catch (Exception e) {  // noqa — 降级边界: 任何失败都不抛出
            log.debug("[vector-rag] 嵌入调用失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String model() {
        return model;
    }
}
