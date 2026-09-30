package com.health.web.assistant.memory;

/**
 * 会话记忆存储（最近 N 轮原始对话文本）。
 *
 * <p>2026-10 抽成接口：记忆是「上下文」而不是「正确性」，实现允许有降级路径——
 * Redis 版在 Redis 不可达时静默退化为无记忆，绝不能让问答因为记忆层挂掉而 500。
 * 实现选择见 {@link AssistantMemoryConfig}（agent.memory.store = memory | redis）。
 */
public interface SessionMemoryStore {

    /** 返回历史上下文（已截断到最近 MAX_ROUNDS 轮）；无历史返回空串 */
    String getHistory(String sessionId);

    /** 追加一轮对话 */
    void append(String sessionId, String userMsg, String assistantMsg);
}
