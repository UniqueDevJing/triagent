package com.health.web.assistant.a2a;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A2A 交接台账：把每次 handoff 的评估结论按会话留痕，支持回放。
 *
 * <p>与 DecisionLog 分工：DecisionLog 记「一轮对话的最终裁决」，
 * HandoffLedger 记「Agent 之间发生了什么交接、为什么放行/拒绝」——
 * 前者是结果，后者是协作过程。两者都能按 sessionId 重放，构成可解释链路。
 */
@Component
public class HandoffLedger {

    private static final int WINDOW = 2000;

    private final ConcurrentLinkedDeque<AgentHandoff> recent = new ConcurrentLinkedDeque<>();
    private final Map<String, List<AgentHandoff>> bySession = new ConcurrentHashMap<>();

    public void append(AgentHandoff h) {
        if (h == null) {
            return;
        }
        recent.addLast(h);
        while (recent.size() > WINDOW) {
            recent.pollFirst();
        }
        bySession.computeIfAbsent(h.sessionId(), k -> new CopyOnWriteArrayList<>()).add(h);
    }

    /** 某会话的完整交接链（按发生顺序） */
    public List<AgentHandoff> replay(String sessionId) {
        List<AgentHandoff> l = bySession.get(sessionId);
        return l == null ? List.of() : List.copyOf(l);
    }

    public List<AgentHandoff> all() {
        return new ArrayList<>(recent);
    }

    public long accepted() {
        return recent.stream().filter(h -> h.status() == HandoffStatus.VALIDATED
                || h.status() == HandoffStatus.DISPATCHED
                || h.status() == HandoffStatus.FULFILLED).count();
    }

    public long rejected() {
        return recent.stream().filter(h -> h.status() == HandoffStatus.REJECTED).count();
    }

    public int size() {
        return recent.size();
    }

    public void clear(String sessionId) {
        if (sessionId == null) {
            return;
        }
        bySession.remove(sessionId);
        recent.removeIf(h -> sessionId.equals(h.sessionId()));
    }
}
