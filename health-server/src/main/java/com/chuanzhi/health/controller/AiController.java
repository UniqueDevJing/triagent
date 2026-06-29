package com.chuanzhi.health.controller;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.dto.AiChatRequest;
import com.chuanzhi.health.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI 健康助手", description = "DeepSeek AI 驱动的健康分析、用药提醒、情感陪伴等功能")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI 通用对话")
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@Valid @RequestBody AiChatRequest request) {
        Map<String, Object> result = aiService.chat(
                request.getUserId(), request.getSessionId(), request.getMessage(), request.getFeatureType());
        return Result.ok(result);
    }

    @Operation(summary = "健康数据分析", description = "解析体检数据、生成健康风险报告")
    @PostMapping("/health-analysis")
    public Result<Map<String, Object>> healthAnalysis(@RequestBody Map<String, Object> body) {
        Object data = body.get("data");
        Long userId = body.get("userId") instanceof Number n ? n.longValue() : null;
        return Result.ok(aiService.analyzeHealthData(userId, data != null ? data.toString() : ""));
    }

    @Operation(summary = "用药提醒生成", description = "根据药品信息生成用药提醒时间表")
    @PostMapping("/medication-reminder")
    public Result<Map<String, Object>> medicationReminder(@RequestBody Map<String, Object> body) {
        Object info = body.get("medicationInfo");
        Long userId = body.get("userId") instanceof Number n ? n.longValue() : null;
        return Result.ok(aiService.generateMedicationReminder(userId, info != null ? info.toString() : ""));
    }

    @Operation(summary = "情感陪伴", description = "老年人心理疏导对话")
    @PostMapping("/companion")
    public Result<Map<String, Object>> companion(@RequestBody Map<String, Object> body) {
        Object msg = body.get("message");
        Long userId = body.get("userId") instanceof Number n ? n.longValue() : null;
        return Result.ok(aiService.companionChat(userId, msg != null ? msg.toString() : ""));
    }

    @Operation(summary = "异常行为识别", description = "分析行为描述、识别潜在风险")
    @PostMapping("/behavior-detect")
    public Result<Map<String, Object>> behaviorDetect(@RequestBody Map<String, Object> body) {
        Object desc = body.get("behaviorDesc");
        Long userId = body.get("userId") instanceof Number n ? n.longValue() : null;
        return Result.ok(aiService.detectAbnormalBehavior(userId, desc != null ? desc.toString() : ""));
    }

    @Operation(summary = "用户健康综合分析", description = "整合用户健康档案进行综合分析")
    @PostMapping("/user-health-analysis")
    public Result<Map<String, Object>> userHealthAnalysis(@RequestBody Map<String, Long> body) {
        Long userId = body.getOrDefault("userId", 1L);
        return Result.ok(aiService.analyzeUserHealth(userId));
    }
}
