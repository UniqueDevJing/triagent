package com.chuanzhi.health.service;

import java.util.Map;

public interface AiService {
    Map<String, Object> chat(Long userId, String sessionId, String message, String featureType);
    Map<String, Object> analyzeHealthData(Long userId, String data);
    Map<String, Object> analyzeUserHealth(Long userId);
    Map<String, Object> generateMedicationReminder(Long userId, String medicationInfo);
    Map<String, Object> companionChat(Long userId, String message);
    Map<String, Object> detectAbnormalBehavior(Long userId, String behaviorDesc);
}
