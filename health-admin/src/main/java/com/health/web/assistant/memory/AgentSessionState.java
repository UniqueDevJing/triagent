package com.health.web.assistant.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话内 Agent 连续性状态（2026-09 优化）。
 * 记录每个会话「当前活跃 Agent」，使多轮对话不被逐条规则路由打断：
 * 预约管家在收症状时收到「胸痛」，应视为预约主诉，而不是被重新路由到分诊触发急救。
 * TTL 兜底：超过 inactiveTtlMs 未活动则视为新会话，重新按消息路由。
 */
@Component
public class AgentSessionState {

    private static final Logger log = LoggerFactory.getLogger(AgentSessionState.class);

    /** 会话无活动超过该时长则遗忘当前 Agent */
    private static final long INACTIVE_TTL_MS = 15 * 60 * 1000L;
    private static final int MAX_SESSIONS = 3000;

    private static final class Entry {
        final String agentName;
        volatile long lastActive;

        Entry(String agentName, long lastActive) {
            this.agentName = agentName;
            this.lastActive = lastActive;
        }
    }

    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();

    /** 返回会话当前 Agent（类型名），超时或不存在返回 null */
    public String get(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        Entry e = sessions.get(sessionId);
        if (e == null) {
            return null;
        }
        if (System.currentTimeMillis() - e.lastActive > INACTIVE_TTL_MS) {
            sessions.remove(sessionId);
            return null;
        }
        return e.agentName;
    }

    /** 记录/刷新会话当前 Agent */
    public void set(String sessionId, String agentName) {
        if (sessionId == null || agentName == null) {
            return;
        }
        sessions.put(sessionId, new Entry(agentName, System.currentTimeMillis()));
        if (sessions.size() > MAX_SESSIONS) {
            evictStale();
        }
    }

    /** 用户明确结束/取消当前流程时清除，下一句按消息重新路由 */
    public void clear(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    private void evictStale() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = sessions.entrySet().iterator();
        while (it.hasNext() && sessions.size() > MAX_SESSIONS) {
            Map.Entry<String, Entry> e = it.next();
            if (now - e.getValue().lastActive > INACTIVE_TTL_MS) {
                it.remove();
            }
        }
        if (sessions.size() > MAX_SESSIONS) {
            log.warn("[session-state] 会话态超上限，最近最少使用被逐出将不可用（仅影响多轮连续性）");
        }
    }
}
