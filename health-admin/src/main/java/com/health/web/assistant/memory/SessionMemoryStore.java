package com.health.web.assistant.memory;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话记忆存储。Phase1 使用内存 Map（单实例可用）；
 * Phase2 切换为 Redis（复用现有 Redis），以保持多实例一致性。
 */
@Component
public class SessionMemoryStore {

    private final Map<String, List<String>> store = new ConcurrentHashMap<>();
    private static final int MAX_ROUNDS = 8;

    /** 返回历史上下文（已截断到最近 MAX_ROUNDS 轮） */
    public String getHistory(String sessionId) {
        List<String> rounds = store.getOrDefault(sessionId, List.of());
        return String.join("\n", rounds);
    }

    /** 追加一轮对话 */
    public void append(String sessionId, String userMsg, String assistantMsg) {
        List<String> rounds = store.computeIfAbsent(sessionId, k -> new LinkedList<>());
        rounds.add("用户：" + userMsg + "\n助手：" + assistantMsg);
        if (rounds.size() > MAX_ROUNDS) {
            rounds.subList(0, rounds.size() - MAX_ROUNDS).clear();
        }
    }
}
