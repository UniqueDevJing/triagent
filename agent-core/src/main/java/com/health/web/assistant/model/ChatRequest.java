package com.health.web.assistant.model;

/**
 * 前端聊天请求体。
 */
public class ChatRequest {

    /** 会话 ID，用于多轮记忆隔离 */
    private String sessionId;

    /** 用户当前消息（症状描述等） */
    private String message;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
