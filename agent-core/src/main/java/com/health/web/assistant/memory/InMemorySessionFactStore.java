package com.health.web.assistant.memory;

import java.util.List;

/**
 * 内存版事实存储（Phase1 行为不变，进程重启即丢）。
 *
 * <p>内部以 {@link FactBus}{@code <EmergencyFact>} 承载急诊事实——总线作为通用 pub/sub
 * 原语被本组件复用，也使「状态机产出急诊事实 → 其他 Agent / 编排层消费」的链路具备
 * 解耦与可重放能力。对外 {@link SessionFactStore} 接口签名保持不变，
 * health-admin 的 {@code AssistantOrchestrator} 无需改动。
 */
public class InMemorySessionFactStore implements SessionFactStore {

    private final FactBus<EmergencyFact> bus = new InMemoryFactBus<>();

    @Override
    public void recordEmergency(String sessionId, String urgency, List<String> departments, String disclaimer) {
        if (sessionId == null) {
            return;
        }
        bus.publish(sessionId,
                new EmergencyFact(urgency, departments, disclaimer, System.currentTimeMillis()));
    }

    @Override
    public EmergencyFact getEmergency(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        List<EmergencyFact> all = bus.replay(sessionId);
        if (all.isEmpty()) {
            return null;
        }
        EmergencyFact latest = all.get(all.size() - 1);
        if (latest.expired()) {
            bus.clear(sessionId);
            return null;
        }
        return latest;
    }

    @Override
    public void clearEmergency(String sessionId) {
        if (sessionId != null) {
            bus.clear(sessionId);
        }
    }
}
