package com.health.web.assistant.memory;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话级「结构化事实」（2026-09：让多 Agent 产生真实协作）。
 * 各 Agent 结束时把可复用的结论写入（如分诊结果），其他 Agent 在编排层读取：
 * 典型场景——本会话刚分诊出 EMERGENCY，预约管家在落单前被拦截并建议先急诊。
 * 与 SessionMemoryStore（原始文本历史）互补：这里存机器可读事实，那里存对话原文。
 */
@Component
public class SessionFactStore {

    /** 急诊事实有效时长：一次分诊后一段时间内，预约/轻问诊都受其约束 */
    private static final long EMERGENCY_TTL_MS = 30 * 60 * 1000L;

    public record EmergencyFact(String urgency, List<String> departments, String disclaimer, long ts) {
        public boolean expired() {
            return System.currentTimeMillis() - ts > EMERGENCY_TTL_MS;
        }
    }

    private final Map<String, EmergencyFact> emergencies = new ConcurrentHashMap<>();

    /** 记录一次 EMERGENCY 分诊事实 */
    public void recordEmergency(String sessionId, String urgency, List<String> departments, String disclaimer) {
        if (sessionId == null) {
            return;
        }
        emergencies.put(sessionId,
                new EmergencyFact(urgency, departments, disclaimer, System.currentTimeMillis()));
    }

    /** 读取未过期的急诊事实；不存在或已过期返回 null */
    public EmergencyFact getEmergency(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        EmergencyFact f = emergencies.get(sessionId);
        if (f == null) {
            return null;
        }
        if (f.expired()) {
            emergencies.remove(sessionId);
            return null;
        }
        return f;
    }

    /** 用户澄清“已排除/看过急诊”时清除，允许恢复正常预约 */
    public void clearEmergency(String sessionId) {
        if (sessionId != null) {
            emergencies.remove(sessionId);
        }
    }
}
