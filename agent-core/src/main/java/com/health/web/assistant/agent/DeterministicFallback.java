package com.health.web.assistant.agent;

import org.springframework.stereotype.Component;

/**
 * 确定性降级话术（T1 从 AssistantOrchestrator 拆出的策略组件）。
 *
 * <p>T5 预算熔断后的 0 LLM 消耗兜底：按 Agent 角色给出规则化安全指引，
 * 明确告知服务受限与替代路径，避免静默截断。
 */
@Component
public class DeterministicFallback {

    public String answer(AgentType type) {
        return switch (type) {
            case TRIAGE -> "（安全提示）本会话对话量较大，智能分诊已切换为简化模式。"
                    + "若症状持续或加重，请及时前往医院就诊；出现胸痛、呼吸困难、意识模糊等急重情况，"
                    + "请立即前往急诊或拨打 120。你也可以开启新会话继续使用完整分诊功能。";
            case SCHEDULER -> "（预约助手）本会话操作量较大，预约服务已切换为简化模式，暂时无法代你落单。"
                    + "请稍后开启新会话重试，或直接联系人工客服/医院挂号平台完成预约。";
            case REPORT -> "（报告助手）本会话对话量较大，报告解读已切换为简化模式。"
                    + "关键结论请以医院出具的正式报告与医生意见为准；如需完整解读，请开启新会话重试。";
            default -> "（系统提示）本会话资源已达上限，已切换为简化模式。请开启新会话继续使用完整功能。";
        };
    }
}
