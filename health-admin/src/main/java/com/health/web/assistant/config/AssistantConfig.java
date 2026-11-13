package com.health.web.assistant.config;

import com.health.web.assistant.model.TriageResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置：集中维护系统提示词与分诊约束。
 * ChatClient 由 spring-ai-openai 自动配置 ChatClient.Builder（读取 spring.ai.openai.*），
 * 此处基于 Builder 显式创建 ChatClient bean（Spring AI 1.0.0 不会自动注册 ChatClient 实例）。
 *
 * 经验固化：给模型的输出要求必须带**结构即 schema**——纯文字类型说明不足以约束 LLM
 * （曾出现 confidence="medium"、followUp=字符串 导致 BeanOutputConverter 解析失败）。
 * 因此 TRIAGE 系统提示词直接拼接 BeanOutputConverter(TriageResult).getJsonSchema()。
 */
@Configuration
public class AssistantConfig {

    /** 由 BeanOutputConverter 生成的分诊 JSON Schema（TriageResult），追加进 TRIAGE 提示词 */
    private static final String TRIAGE_JSON_SCHEMA =
            new BeanOutputConverter<>(TriageResult.class).getJsonSchema();

    public static final String SYSTEM_PROMPT = """
            你是「智能医疗」的智能分诊与就医助手。请遵守：
            1. 根据用户症状描述，推断可能的疾病与推荐科室（可调用 searchDiseases 工具）。
            2. 评估紧急程度：EMERGENCY（需立即急诊，如胸痛/呼吸困难/卒中征兆）、URGENT（尽快就诊）、ROUTINE（普通门诊）。
            3. 若用户是会员，可调用 searchMembers 获取信息以个性化建议。
            4. 多轮澄清关键症状（部位、时长、伴随症状）。
            5. 结尾必须输出一个 JSON（置于 ```json ``` 代码块），**字段与类型必须严格符合文末给出的 JSON Schema**：
               urgency 取 EMERGENCY|URGENT|ROUTINE 之一；departments 为字符串数组；hospitalLevel 为字符串；
               confidence 为 0~1 的数字（禁止 low/medium/high 等文本）；followUp 为字符串数组；disclaimer 为字符串。
            6. 绝不给出确诊结论，必须提示 disclaimer。
            7. 正文用对话口吻给建议即可；除 部位/时长/伴随 外不要连环追问用户——未尽的问题放进 JSON 的 followUp 字段，不要在正文结尾再抛问题。JSON 必须始终输出。
            """
            + "\n\n### 输出 JSON Schema（必须严格符合，系统会按此校验）：\n"
            + TRIAGE_JSON_SCHEMA + "\n";

    @Bean
    public ChatClient assistantChatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(SYSTEM_PROMPT).build();
    }
}
