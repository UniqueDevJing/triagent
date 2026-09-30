package com.health.web.assistant.memory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存版事实存储（Phase1 行为不变，进程重启即丢）。 */
public class InMemorySessionFactStore implements SessionFactStore {

    private final Map<String, EmergencyFact> emergencies = new ConcurrentHashMap<>();

    @Override
    public void recordEmergency(String sessionId, String urgency, List<String> departments, String disclaimer) {
        if (sessionId == null) {
            return;
        }
        emergencies.put(sessionId,
                new EmergencyFact(urgency, departments, disclaimer, System.currentTimeMillis()));
    }

    @Override
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

    @Override
    public void clearEmergency(String sessionId) {
        if (sessionId != null) {
            emergencies.remove(sessionId);
        }
    }
}
