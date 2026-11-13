package com.health.web.assistant.a2a;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A2A 交接契约（Task 语义）。
 *
 * <p>一次 Supervisor → 子 Agent 的下发不再是隐式的「改个 system prompt」，
 * 而是显式携带：任务 id、来源/目标、意图、目标必须具备的工具能力、
 * 随附的上下文证据、以及**验收断言**（acceptance）。
 * 评估门 {@link HandoffGate} 在任务进入执行前逐项校验，校验结论写回状态与留痕。
 */
public record AgentHandoff(
        String taskId,
        String sessionId,
        String fromAgent,
        String toAgent,
        String intent,
        List<String> requiredCapabilities,
        List<String> contextRefs,
        List<String> requiredEvidence,
        List<String> acceptance,
        HandoffStatus status,
        long createdAt,
        String gateNote) {

    public AgentHandoff {
        requiredCapabilities = requiredCapabilities == null ? List.of() : List.copyOf(requiredCapabilities);
        contextRefs = contextRefs == null ? List.of() : List.copyOf(contextRefs);
        requiredEvidence = requiredEvidence == null ? List.of() : List.copyOf(requiredEvidence);
        acceptance = acceptance == null ? List.of() : List.copyOf(acceptance);
    }

    public static AgentHandoff created(String sessionId, String fromAgent, String toAgent,
                                       String intent, List<String> requiredCapabilities,
                                       List<String> contextRefs, List<String> requiredEvidence,
                                       List<String> acceptance) {
        return new AgentHandoff(
                "task-" + UUID.randomUUID().toString().substring(0, 8),
                sessionId,
                fromAgent == null ? "SUPERVISOR" : fromAgent,
                toAgent,
                intent,
                requiredCapabilities,
                contextRefs,
                requiredEvidence,
                acceptance,
                HandoffStatus.CREATED,
                System.currentTimeMillis(),
                null);
    }

    /** 状态转移（非法跳跃抛异常，保证交接链可审计） */
    public AgentHandoff transition(HandoffStatus next, String note) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "非法交接状态转移: " + status + " -> " + next + " (task=" + taskId + ")");
        }
        return new AgentHandoff(taskId, sessionId, fromAgent, toAgent, intent,
                requiredCapabilities, contextRefs, requiredEvidence, acceptance,
                next, createdAt, note);
    }

    public boolean isCrossAgent() {
        return fromAgent != null && !fromAgent.equals(toAgent);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskId", taskId);
        m.put("sessionId", sessionId);
        m.put("fromAgent", fromAgent);
        m.put("toAgent", toAgent);
        m.put("intent", intent);
        m.put("requiredCapabilities", requiredCapabilities);
        m.put("contextRefs", contextRefs);
        m.put("requiredEvidence", requiredEvidence);
        m.put("acceptance", acceptance);
        m.put("status", status.name());
        m.put("createdAt", createdAt);
        m.put("gateNote", gateNote);
        return m;
    }
}
