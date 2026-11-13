package com.health.web.assistant.memory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存版会话记忆：单实例可用，进程重启即丢（默认实现，Phase1 行为不变）。 */
public class InMemorySessionMemoryStore implements SessionMemoryStore {

    private final Map<String, List<String>> store = new ConcurrentHashMap<>();
    static final int MAX_ROUNDS = 8;

    @Override
    public String getHistory(String sessionId) {
        List<String> rounds = store.getOrDefault(sessionId, List.of());
        return String.join("\n", rounds);
    }

    @Override
    public void append(String sessionId, String userMsg, String assistantMsg) {
        List<String> rounds = store.computeIfAbsent(sessionId, k -> new LinkedList<>());
        rounds.add("用户：" + userMsg + "\n助手：" + assistantMsg);
        if (rounds.size() > MAX_ROUNDS) {
            rounds.subList(0, rounds.size() - MAX_ROUNDS).clear();   // Truncating：截掉最旧
        }
    }
}
