package com.hospital.v2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 绑定 application.yml 的 hospital.* 配置。 */
@Component
@ConfigurationProperties(prefix = "hospital")
public class HospitalProps {
    private Llm llm = new Llm();
    private Rag rag = new Rag();

    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }

    public static class Llm {
        private String mode = "auto";        // auto | real | mock
        private String baseUrl;
        private String apiKey;
        private String chatModel = "deepseek-v4-flash";
        private String embeddingModel = "text-embedding-v3";
        private int embeddingDim = 1024;
        private int maxAgentSteps = 7;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public int getEmbeddingDim() { return embeddingDim; }
        public void setEmbeddingDim(int embeddingDim) { this.embeddingDim = embeddingDim; }
        public int getMaxAgentSteps() { return maxAgentSteps; }
        public void setMaxAgentSteps(int maxAgentSteps) { this.maxAgentSteps = maxAgentSteps; }
    }

    public static class Rag {
        private int topK = 4;
        private double scoreThreshold = 0.35;

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getScoreThreshold() { return scoreThreshold; }
        public void setScoreThreshold(double scoreThreshold) { this.scoreThreshold = scoreThreshold; }
    }
}
