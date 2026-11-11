package com.hospital.v2.config;

import com.hospital.v2.dao.Db;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一 AI 对话出口（LangChain4j 1.15：ChatModel.chat(ChatRequest) → ChatResponse）。
 * - mode=real 且有 key：OpenAI 兼容对话模型（DashScope / 任意兼容端点）。
 * - 否则：确定性 Mock 模型，按固定剧本驱动 ReAct 工具调用，保证本地零依赖也能跑通全链路。
 */
@Component
public class LlmGateway {
    private final HospitalProps props;
    private final Db db;
    private ChatModel model;
    private final MockChatModel mock = new MockChatModel();

    public LlmGateway(HospitalProps props, Db db) {
        this.props = props;
        this.db = db;
        if (useReal()) {
            this.model = OpenAiChatModel.builder()
                    .baseUrl(props.getLlm().getBaseUrl())
                    .apiKey(props.getLlm().getApiKey())
                    .modelName(props.getLlm().getChatModel())
                    .temperature(0.3)
                    .build();
        } else {
            this.model = new MockChatModel();
        }
    }

    public boolean isReal() { return model instanceof OpenAiChatModel; }

    private boolean useReal() {
        String mode = props.getLlm().getMode();
        return "real".equalsIgnoreCase(mode)
                || ("auto".equalsIgnoreCase(mode) && props.getLlm().getApiKey() != null && !props.getLlm().getApiKey().isBlank());
    }

    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> tools) {
        long t0 = System.currentTimeMillis();
        ChatRequest req = ChatRequest.builder().messages(messages).toolSpecifications(tools).build();
        // 离线模式：直接走确定性 Mock
        if (!isReal()) {
            ChatResponse r = mock.chat(req);
            TokenUsage tu = r.tokenUsage() == null ? new TokenUsage(0, 0) : r.tokenUsage();
            return Response.from(r.aiMessage(), tu, r.finishReason(), Map.of());
        }
        // 真实模式：调用真实模型
        try {
            ChatResponse resp = model.chat(req);
            AiMessage ai = resp.aiMessage();
            TokenUsage tu = resp.tokenUsage() == null ? new TokenUsage(0, 0) : resp.tokenUsage();
            logUsage("triage", props.getLlm().getChatModel(), tu.inputTokenCount(), tu.outputTokenCount(),
                    (int) (System.currentTimeMillis() - t0), true, null);
            return Response.from(ai, tu, resp.finishReason(), Map.of());
        } catch (Exception e) {
            logUsage("triage", props.getLlm().getChatModel(), 0, 0, (int) (System.currentTimeMillis() - t0), false, e.getMessage());
            // 真实模型不可用（限流 / 网络抖动 / Key 失效）时优雅降级到离线 Mock，保证分诊链路不中断
            org.slf4j.LoggerFactory.getLogger(LlmGateway.class)
                    .warn("真实 LLM 调用失败，降级到 Mock：{}", e.getMessage());
            ChatResponse fr = mock.chat(req);
            return Response.from(fr.aiMessage(), new TokenUsage(0, 0), fr.finishReason(), Map.of("degraded", "true"));
        }
    }

    /** 无工具的普通对话（预问诊追问用）。真实模式失败则降级返回 null，由调用方走本地脚本。 */
    public String chatText(String system, String user) {
        if (!isReal()) return null;
        try {
            ChatRequest req = ChatRequest.builder()
                    .messages(List.of(SystemMessage.from(system), UserMessage.from(user))).build();
            ChatResponse resp = model.chat(req);
            return resp.aiMessage().text();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(LlmGateway.class)
                    .warn("真实 LLM 对话失败，降级到本地脚本：{}", e.getMessage());
            return null;
        }
    }

    private void logUsage(String useCase, String model, int pt, int ct, int ms, boolean ok, String err) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("use_case", useCase);
        m.put("model", model);
        m.put("prompt_tokens", pt);
        m.put("completion_tokens", ct);
        m.put("total_tokens", pt + ct);
        m.put("cost_ms", ms);
        m.put("success", ok ? 1 : 0);
        // 截断到 200 字符，避免上游错误 JSON 超列宽导致日志写入本身失败
        m.put("error_msg", err == null ? "" : (err.length() > 200 ? err.substring(0, 200) : err));
        db.insert("INSERT INTO ai_usage_log (use_case, model, prompt_tokens, completion_tokens, total_tokens, cost_ms, success, error_msg) "
                + "VALUES (:use_case,:model,:prompt_tokens,:completion_tokens,:total_tokens,:cost_ms,:success,:error_msg)", m);
    }

    // ===================== 离线 Mock 模型 =====================
    private static final Map<String, long[]> SYMPTOM_TO = new java.util.LinkedHashMap<>();
    static {
        SYMPTOM_TO.put("咳嗽", new long[]{1, 2}); SYMPTOM_TO.put("咳痰", new long[]{1, 2});
        SYMPTOM_TO.put("哮喘", new long[]{1, 2}); SYMPTOM_TO.put("喘息", new long[]{1, 2});
        SYMPTOM_TO.put("胸闷", new long[]{2, 3}); SYMPTOM_TO.put("胸痛", new long[]{2, 3});
        SYMPTOM_TO.put("心悸", new long[]{2, 3}); SYMPTOM_TO.put("高血压", new long[]{2, 4});
        SYMPTOM_TO.put("腹痛", new long[]{3, 5}); SYMPTOM_TO.put("腹泻", new long[]{3, 5});
        SYMPTOM_TO.put("反酸", new long[]{3, 5});
        SYMPTOM_TO.put("头痛", new long[]{4, 6}); SYMPTOM_TO.put("眩晕", new long[]{4, 6});
        SYMPTOM_TO.put("麻木", new long[]{4, 6});
        SYMPTOM_TO.put("骨折", new long[]{6, 8}); SYMPTOM_TO.put("关节", new long[]{6, 8});
        SYMPTOM_TO.put("发热", new long[]{7, 9}); SYMPTOM_TO.put("小儿", new long[]{7, 9});
    }

    private class MockChatModel implements ChatModel {
        private final java.util.regex.Pattern KW = java.util.regex.Pattern.compile("(咳嗽|咳痰|哮喘|喘息|胸闷|胸痛|心悸|高血压|腹痛|腹泻|反酸|头痛|眩晕|麻木|骨折|关节|发热|小儿)");

        @Override
        public ChatResponse chat(ChatRequest request) {
            List<ChatMessage> messages = request.messages();
            int toolTurns = 0;
            String complaint = "";
            for (ChatMessage m : messages) {
                if (m instanceof ToolExecutionResultMessage) toolTurns++;
                if (m instanceof UserMessage um && complaint.isEmpty()) {
                    for (Content c : um.contents()) {
                        if (c instanceof TextContent tc) { complaint = tc.text(); break; }
                    }
                }
            }
            String kw = extractKeyword(complaint);
            long[] pair = SYMPTOM_TO.getOrDefault(kw, new long[]{1, 1});
            long dept = pair[0], doc = pair[1];

            AiMessage out;
            switch (toolTurns) {
                case 0 -> out = toolCall("symptom_tag_search", "{\"keyword\":\"" + kw + "\"}");
                case 1 -> out = toolCall("department_query", "{\"keyword\":\"" + kw + "\"}");
                case 2 -> out = toolCall("doctor_query", "{\"department_id\":" + dept + "}");
                case 3 -> out = toolCall("schedule_query", "{\"doctor_id\":" + doc + ",\"work_date\":\"2026-11-12\",\"period\":\"AM\"}");
                default -> out = AiMessage.from("{\"department_id\":" + dept + ",\"doctor_id\":" + doc
                        + ",\"confidence\":0.92,\"reason\":\"根据症状[" + kw + "]结合知识库与工具核查，推荐对应科室与医生。\"}");
            }
            return ChatResponse.builder().aiMessage(out)
                    .tokenUsage(new TokenUsage(0, 0)).finishReason(FinishReason.STOP).build();
        }

        private String extractKeyword(String text) {
            if (text == null) return "咳嗽";
            java.util.regex.Matcher mt = KW.matcher(text);
            return mt.find() ? mt.group(1) : "咳嗽";
        }

        private AiMessage toolCall(String name, String args) {
            ToolExecutionRequest req = ToolExecutionRequest.builder().name(name).arguments(args).build();
            return AiMessage.from(req);
        }
    }
}
