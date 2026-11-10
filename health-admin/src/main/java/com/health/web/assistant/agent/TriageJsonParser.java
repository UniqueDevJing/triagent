package com.health.web.assistant.agent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.health.web.assistant.model.TriageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * 结构化分诊输出解析器（T1 从 AssistantOrchestrator 拆出的策略组件）。
 *
 * <p>解析策略：BeanOutputConverter 绑定模型输出的 JSON（优先整段，其次 ```json 代码块，
 * 最后容错扫描全文首个 { 到末尾 }）；解析失败返回 null（不阻断对话）。
 * G8 重试：首轮解析失败时，用一次轻量、纯 JSON 的补充调用把残缺回答提炼成合法 TriageResult，
 * 二次仍失败返回 null。
 */
@Component
public class TriageJsonParser {

    private static final Logger log = LoggerFactory.getLogger(TriageJsonParser.class);

    private final BeanOutputConverter<TriageResult> triageConverter = new BeanOutputConverter<>(TriageResult.class);
    private final ObjectMapper lenientMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public TriageResult parse(String full) {
        if (full == null || full.isBlank()) {
            return null;
        }
        try {
            String trimmed = full.trim();
            if (trimmed.startsWith("{")) {
                return triageConverter.convert(trimmed);
            }
            int s = trimmed.indexOf("```json");
            int start = s < 0 ? -1 : s + 7;
            if (start >= 0) {
                int e = trimmed.indexOf("```", start);
                String json = trimmed.substring(start, e < 0 ? trimmed.length() : e).trim();
                return triageConverter.convert(json);
            }
        } catch (Exception ex) {
            log.debug("BeanOutputConverter 解析失败，降级 lenient 解析: {}", ex.getMessage());
        }
        return lenientExtract(full);
    }

    /** G8 重试：仅在首次结构化解析失败时触发（TRIAGE 轮），成本低；二次仍失败返回 null。 */
    public TriageResult retry(ChatClient chatClient, String fullText) {
        if (chatClient == null || fullText == null || fullText.isBlank()) {
            return null;
        }
        try {
            String text = fullText.length() > 2600 ? fullText.substring(0, 2600) + "…(省略)" : fullText;
            String out = chatClient.prompt()
                    .system("你是一个医疗分诊结构化输出助手。你只输出一个 JSON 对象，禁止输出任何其他文字、解释、Markdown 代码块标记。")
                    .user("下面是助手上一轮的分诊建议全文（可能混有正文、追问或不完整 JSON）。"
                            + "请提炼并修正为**一个完整且合法**的 JSON 对象，字段与类型必须为："
                            + "urgency ∈ [EMERGENCY,URGENT,ROUTINE]（字符串）；departments=字符串数组；"
                            + "hospitalLevel=字符串；confidence=0~1 的数字；followUp=字符串数组；disclaimer=字符串。"
                            + "只输出 JSON：\n\n" + text)
                    .call()
                    .content();
            if (out == null || out.isBlank()) {
                return null;
            }
            return parse(out);
        } catch (Exception e) {
            log.warn("[agent] JSON 重试调用失败: {}", e.getMessage());
            return null;
        }
    }

    private TriageResult lenientExtract(String full) {
        try {
            int s = full.indexOf("{");
            int e = full.lastIndexOf("}");
            if (s < 0 || e <= s) {
                return null;
            }
            JsonNode root = lenientMapper.readTree(full.substring(s, e + 1));
            // 容错归一：模型偶发把 confidence 写为文本 / followUp 写为字符串
            if (root.has("confidence") && root.get("confidence").isTextual()) {
                String c = root.get("confidence").asText().trim().toLowerCase();
                double v = switch (c) {
                    case "low", "较低", "低" -> 0.3;
                    case "medium", "中等", "中" -> 0.6;
                    case "high", "较高", "高" -> 0.9;
                    default -> 0.5;
                };
                ((ObjectNode) root).put("confidence", v);
            }
            if (root.has("followUp") && root.get("followUp").isTextual()) {
                var arr = lenientMapper.createArrayNode();
                arr.add(root.get("followUp").asText());
                ((ObjectNode) root).set("followUp", arr);
            }
            return lenientMapper.treeToValue(root, TriageResult.class);
        } catch (Exception ex) {
            log.debug("lenient 分诊解析失败: {}", ex.getMessage());
            return null;
        }
    }
}
