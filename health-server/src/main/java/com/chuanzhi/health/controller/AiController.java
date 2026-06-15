package com.chuanzhi.health.controller;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.dto.AiChatRequest;
import com.chuanzhi.health.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@Valid @RequestBody AiChatRequest request) {
        Map<String, Object> result = aiService.chat(
                null, request.getSessionId(), request.getMessage(), request.getFeatureType());
        return Result.ok(result);
    }

    @PostMapping("/health-analysis")
    public Result<Map<String, Object>> healthAnalysis(@RequestBody Map<String, String> body) {
        return Result.ok(aiService.analyzeHealthData(null, body.get("data")));
    }

    @PostMapping("/medication-reminder")
    public Result<Map<String, Object>> medicationReminder(@RequestBody Map<String, String> body) {
        return Result.ok(aiService.generateMedicationReminder(null, body.get("medicationInfo")));
    }

    @PostMapping("/companion")
    public Result<Map<String, Object>> companion(@RequestBody Map<String, String> body) {
        return Result.ok(aiService.companionChat(null, body.get("message")));
    }

    @PostMapping("/behavior-detect")
    public Result<Map<String, Object>> behaviorDetect(@RequestBody Map<String, String> body) {
        return Result.ok(aiService.detectAbnormalBehavior(null, body.get("behaviorDesc")));
    }

    @PostMapping("/user-health-analysis")
    public Result<Map<String, Object>> userHealthAnalysis(@RequestBody Map<String, Long> body) {
        Long userId = body.getOrDefault("userId", 1L);
        return Result.ok(aiService.analyzeUserHealth(userId));
    }
}
