package com.chuanzhi.health.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanzhi.health.ai.AiConfig;
import com.chuanzhi.health.entity.AiConversation;
import com.chuanzhi.health.entity.AssessmentRecord;
import com.chuanzhi.health.entity.HealthRecord;
import com.chuanzhi.health.entity.User;
import com.chuanzhi.health.mapper.AiConversationMapper;
import com.chuanzhi.health.mapper.AssessmentRecordMapper;
import com.chuanzhi.health.mapper.HealthRecordMapper;
import com.chuanzhi.health.mapper.UserMapper;
import com.chuanzhi.health.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiConfig aiConfig;
    private final AiConversationMapper conversationMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final UserMapper userMapper;

    private static final String SYSTEM_HEALTH_ANALYSIS = """
        你是一个专业的老年健康数据分析师。用户会提供体检数据或健康指标描述。
        请严格按以下JSON格式回复（不要包含markdown代码块标记）：
        {"risk_level":"低/中/高","analysis":"基于数据的具体健康分析","suggestions":["建议1","建议2","建议3"]}
        请基于老年人健康标准进行分析，关注血压、血糖、血脂等关键指标。""";

    private static final String SYSTEM_MEDICATION = """
        你是一个专业的老年人用药管理助手。用户会提供药品信息，请生成科学的用药提醒计划。
        请严格按以下JSON格式回复：
        {"medication_name":"药品通用名","schedule":[{"time":"08:00","dosage":"具体剂量","note":"服用说明"}],"warnings":["注意事项1","注意事项2"],"schedule_desc":"用药计划整体描述"}
        特别注意老年人用药安全，包括药物相互作用、肝肾功能影响。""";

    private static final String SYSTEM_COMPANION = """
        你是一个温暖、耐心的老年人情感陪伴助手，名字叫"小智"。
        你的对话对象是60岁以上的老年人。请用温和、简洁、充满关怀的语气交流。
        每次回复控制在100字以内，语句通俗易懂。
        请严格按以下JSON格式回复：
        {"reply":"你的回复内容","emotion_detected":"检测到的情绪","suggestion":"如果需要关注，给家属的建议，否则为null"}""";

    private static final String SYSTEM_BEHAVIOR = """
        你是一个老年人异常行为识别专家。根据描述的行为表现，判断是否存在健康或安全风险。
        请严格按以下JSON格式回复：
        {"risk_level":"正常/关注/高危","behavior_type":"认知异常/情绪异常/生理异常/安全风险/正常","analysis":"详细分析","action_suggested":"建议采取的具体措施"}
        重点关注：跌倒、走失、用药错误、认知退化、情绪异常等老年人常见风险。""";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public Map<String, Object> chat(Long userId, String sessionId, String message, String featureType) {
        saveConversation(userId, sessionId, "USER", message, featureType);

        String systemPrompt;
        switch (featureType != null ? featureType : "CHAT") {
            case "ANALYSIS" -> systemPrompt = SYSTEM_HEALTH_ANALYSIS;
            case "MEDICATION" -> systemPrompt = SYSTEM_MEDICATION;
            case "COMPANION" -> systemPrompt = SYSTEM_COMPANION;
            case "BEHAVIOR" -> systemPrompt = SYSTEM_BEHAVIOR;
            default -> systemPrompt = "你是传智健康管理系统的AI助手，名为小智。请用友好专业的态度回答用户的健康相关问题。以JSON格式回复：{\"reply\":\"你的回复\"}";
        }

        // 注入用户健康档案上下文
        String healthContext = buildUserHealthContext(userId);
        if (!healthContext.isEmpty()) {
            systemPrompt = healthContext + "\n\n" + systemPrompt;
        }

        // 加载最近对话历史作为上下文
        List<Map<String, String>> history = loadHistory(sessionId, 10);

        String aiResponse = callAiApi(systemPrompt, message, history);
        Map<String, Object> result = parseAiResponse(aiResponse, featureType);

        saveConversation(userId, sessionId, "AI", JSONUtil.toJsonStr(result), featureType);
        return result;
    }

    @Override
    public Map<String, Object> analyzeHealthData(Long userId, String data) {
        String aiResponse = callAiApi(SYSTEM_HEALTH_ANALYSIS, data);
        return parseAiResponse(aiResponse, "ANALYSIS");
    }

    @Override
    public Map<String, Object> analyzeUserHealth(Long userId) {
        // 从数据库读取用户最近的体检记录
        List<HealthRecord> records = healthRecordMapper.selectList(
            new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId != null ? userId : 1L)
                .orderByDesc(HealthRecord::getRecordDate)
                .last("LIMIT 5")
        );

        if (records.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("risk_level", "未知");
            empty.put("analysis", "该用户暂无体检数据记录。");
            empty.put("suggestions", List.of("请先录入体检数据"));
            return empty;
        }

        // 构建包含健康档案上下文的分析请求
        StringBuilder healthData = new StringBuilder();
        healthData.append("以下是该用户的健康档案数据，请综合分析：\n\n");
        for (HealthRecord r : records) {
            healthData.append("【").append(r.getRecordDate()).append(" ").append(r.getType()).append("】\n");
            healthData.append("指标: ").append(r.getMetrics()).append("\n");
            if (r.getDoctorNotes() != null) {
                healthData.append("医生备注: ").append(r.getDoctorNotes()).append("\n");
            }
            healthData.append("\n");
        }
        healthData.append("请基于以上历史数据，给出综合健康分析和趋势判断。");

        String aiResponse = callAiApi(SYSTEM_HEALTH_ANALYSIS, healthData.toString());
        return parseAiResponse(aiResponse, "ANALYSIS");
    }

    @Override
    public Map<String, Object> generateMedicationReminder(Long userId, String medicationInfo) {
        String aiResponse = callAiApi(SYSTEM_MEDICATION, medicationInfo);
        return parseAiResponse(aiResponse, "MEDICATION");
    }

    @Override
    public Map<String, Object> companionChat(Long userId, String message) {
        String systemPrompt = SYSTEM_COMPANION;
        String healthContext = buildUserHealthContext(userId);
        if (!healthContext.isEmpty()) {
            systemPrompt = healthContext + "\n\n" + systemPrompt;
        }
        String aiResponse = callAiApi(systemPrompt, message);
        return parseAiResponse(aiResponse, "COMPANION");
    }

    @Override
    public Map<String, Object> detectAbnormalBehavior(Long userId, String behaviorDesc) {
        String aiResponse = callAiApi(SYSTEM_BEHAVIOR, behaviorDesc);
        return parseAiResponse(aiResponse, "BEHAVIOR");
    }

    /**
     * 构建用户健康档案上下文，让 AI 跨会话"记住"用户健康状况
     */
    private String buildUserHealthContext(Long userId) {
        if (userId == null) return "";
        try {
            User user = userMapper.selectById(userId);
            if (user == null) return "";

            StringBuilder ctx = new StringBuilder();
            ctx.append("【当前用户健康档案 - 请基于以下信息提供个性化服务】\n");
            ctx.append("姓名：").append(user.getName() != null ? user.getName() : "未知");
            if (user.getAge() != null) ctx.append("，年龄：").append(user.getAge()).append("岁");
            if (user.getGender() != null) {
                ctx.append("，性别：").append(user.getGender() == 1 ? "男" : user.getGender() == 0 ? "女" : "未知");
            }
            if (user.getBloodType() != null) ctx.append("，血型：").append(user.getBloodType());
            if (user.getHeight() != null) ctx.append("，身高：").append(user.getHeight()).append("cm");
            if (user.getWeight() != null) ctx.append("，体重：").append(user.getWeight()).append("kg");
            if (user.getAllergies() != null && !user.getAllergies().isBlank())
                ctx.append("，过敏史：").append(user.getAllergies());
            if (user.getMedicalHistory() != null && !user.getMedicalHistory().isBlank())
                ctx.append("，既往病史：").append(user.getMedicalHistory());
            ctx.append("\n");

            // 近期体检记录
            List<HealthRecord> records = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                    .eq(HealthRecord::getUserId, userId)
                    .orderByDesc(HealthRecord::getRecordDate)
                    .last("LIMIT 5")
            );
            if (!records.isEmpty()) {
                ctx.append("\n【近期体检记录】\n");
                for (HealthRecord r : records) {
                    ctx.append("- ").append(r.getRecordDate()).append(" [").append(r.getType()).append("] ");
                    ctx.append(r.getMetrics() != null ? r.getMetrics() : "");
                    if (r.getDoctorNotes() != null && !r.getDoctorNotes().isBlank())
                        ctx.append(" 医嘱：").append(r.getDoctorNotes());
                    ctx.append("\n");
                }
            }

            // 近期评估记录
            List<AssessmentRecord> assessments = assessmentRecordMapper.selectList(
                new LambdaQueryWrapper<AssessmentRecord>()
                    .eq(AssessmentRecord::getUserId, userId)
                    .orderByDesc(AssessmentRecord::getCreatedAt)
                    .last("LIMIT 3")
            );
            if (!assessments.isEmpty()) {
                ctx.append("\n【近期评估记录】\n");
                for (AssessmentRecord a : assessments) {
                    ctx.append("- 得分：").append(a.getTotalScore())
                        .append("，风险等级：").append(a.getRiskLevel() != null ? a.getRiskLevel().getLabel() : "未知");
                    if (a.getReportText() != null && !a.getReportText().isBlank())
                        ctx.append("，报告：").append(a.getReportText());
                    ctx.append("\n");
                }
            }

            log.info("构建用户{}健康上下文成功, 长度={}", userId, ctx.length());
            return ctx.toString();
        } catch (Exception e) {
            log.warn("构建用户健康上下文失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 加载会话最近 N 条对话历史
     */
    private List<Map<String, String>> loadHistory(String sessionId, int limit) {
        List<Map<String, String>> history = new ArrayList<>();
        if (sessionId == null) return history;
        try {
            List<AiConversation> records = conversationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversation>()
                    .eq(AiConversation::getSessionId, sessionId)
                    .orderByDesc(AiConversation::getCreatedAt)
                    .last("LIMIT " + limit)
            );
            // 反转回时间顺序
            java.util.Collections.reverse(records);
            for (AiConversation conv : records) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("role", conv.getRole().equalsIgnoreCase("USER") ? "user" : "assistant");
                item.put("content", conv.getContent());
                history.add(item);
            }
        } catch (Exception e) {
            log.warn("加载对话历史失败: {}", e.getMessage());
        }
        return history;
    }

    /**
     * 调用 DeepSeek API (OpenAI 兼容接口)
     */
    private String callAiApi(String systemPrompt, String userMessage) {
        return callAiApi(systemPrompt, userMessage, null);
    }

    private String callAiApi(String systemPrompt, String userMessage, List<Map<String, String>> history) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", aiConfig.getModel());
            requestBody.set("temperature", 0.7);
            requestBody.set("max_tokens", 1024);

            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.set("role", "system");
            sysMsg.set("content", systemPrompt);
            messages.add(sysMsg);

            // 插入历史消息
            if (history != null) {
                for (Map<String, String> h : history) {
                    JSONObject histMsg = new JSONObject();
                    histMsg.set("role", h.get("role"));
                    histMsg.set("content", h.get("content"));
                    messages.add(histMsg);
                }
            }

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", userMessage);
            messages.add(userMsg);

            requestBody.set("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiConfig.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            log.info("调用DeepSeek API, model={}, message={}", aiConfig.getModel(),
                    userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject respJson = JSONUtil.parseObj(response.body());
                JSONArray choices = respJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    String content = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getStr("content");
                    log.info("DeepSeek API 响应: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
                    return content;
                }
            }

            log.error("DeepSeek API 调用失败, status={}, body={}", response.statusCode(), response.body());
            return null;
        } catch (Exception e) {
            log.error("DeepSeek API 调用异常", e);
            return null;
        }
    }

    /**
     * 解析 AI 返回的 JSON，失败时返回兜底内容
     */
    private Map<String, Object> parseAiResponse(String aiContent, String featureType) {
        if (aiContent == null || aiContent.isBlank()) {
            return buildFallback(featureType);
        }

        // 去除可能的 markdown 代码块标记
        String cleaned = aiContent.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();

        try {
            JSONObject json = JSONUtil.parseObj(cleaned);
            Map<String, Object> result = new LinkedHashMap<>();
            for (String key : json.keySet()) {
                Object value = json.get(key);
                if (value instanceof JSONNull) {
                    // JSONNull → Java null，避免 Jackson 序列化报错
                    continue;
                }
                if (value instanceof JSONArray arr) {
                    List<Object> list = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        Object item = arr.get(i);
                        if (item instanceof JSONNull) continue;
                        if (item instanceof JSONObject obj) {
                            Map<String, Object> map = new LinkedHashMap<>();
                            for (String k : obj.keySet()) map.put(k, obj.get(k));
                            list.add(map);
                        } else {
                            list.add(item);
                        }
                    }
                    result.put(key, list);
                } else {
                    result.put(key, value);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("AI响应JSON解析失败, raw={}", cleaned.substring(0, Math.min(200, cleaned.length())));
            // 非 JSON 格式时，包装为通用回复
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("reply", cleaned);
            return result;
        }
    }

    private Map<String, Object> buildFallback(String featureType) {
        Map<String, Object> result = new LinkedHashMap<>();
        switch (featureType != null ? featureType : "CHAT") {
            case "ANALYSIS" -> {
                result.put("risk_level", "未知");
                result.put("analysis", "AI服务暂时不可用，请稍后重试。");
                result.put("suggestions", List.of("请稍后重新提交数据"));
            }
            case "MEDICATION" -> {
                result.put("medication_name", "未知");
                result.put("schedule", List.of());
                result.put("warnings", List.of("AI服务暂时不可用"));
                result.put("schedule_desc", "请稍后重试");
            }
            case "COMPANION" -> {
                result.put("reply", "不好意思，我暂时有点走神了。请您稍等片刻再和我聊天好吗？");
                result.put("emotion_detected", "未知");
            }
            case "BEHAVIOR" -> {
                result.put("risk_level", "未知");
                result.put("behavior_type", "未知");
                result.put("analysis", "AI服务暂时不可用");
                result.put("action_suggested", "请稍后重试");
            }
            default -> result.put("reply", "AI服务暂时不可用，请稍后重试。");
        }
        return result;
    }

    private void saveConversation(Long userId, String sessionId, String role, String content, String featureType) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setSessionId(sessionId);
        conv.setRole(role);
        conv.setContent(content);
        conv.setFeatureType(featureType);
        conversationMapper.insert(conv);
    }
}
