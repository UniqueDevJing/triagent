package com.health.web.assistant.memory;

import java.util.List;

/**
 * 会话级「结构化事实」（2026-09：让多 Agent 产生真实协作）。
 * 各 Agent 结束时把可复用的结论写入（如分诊结果），其他 Agent 在编排层读取：
 * 典型场景——本会话刚分诊出 EMERGENCY，预约管家在落单前被拦截并建议先急诊。
 * 与 SessionMemoryStore（原始文本历史）互补：这里存机器可读事实，那里存对话原文。
 *
 * <p>事实驱动的是「安全拦截」这一正确性行为，与记忆层「可以丢」的降级策略不同：
 * Redis 实现对写入/清除失败记 error，由运维侧感知，而不是假装无事发生。
 */
public interface SessionFactStore {

    /** 急诊事实有效时长：一次分诊后一段时间内，预约/轻问诊都受其约束 */
    long EMERGENCY_TTL_MS = 30 * 60 * 1000L;

    record EmergencyFact(String urgency, List<String> departments, String disclaimer, long ts) {
        public boolean expired() {
            return System.currentTimeMillis() - ts > EMERGENCY_TTL_MS;
        }
    }

    /** 记录一次 EMERGENCY 分诊事实 */
    void recordEmergency(String sessionId, String urgency, List<String> departments, String disclaimer);

    /** 读取未过期的急诊事实；不存在或已过期返回 null */
    EmergencyFact getEmergency(String sessionId);

    /** 用户澄清“已排除/看过急诊”时清除，允许恢复正常预约 */
    void clearEmergency(String sessionId);
}
