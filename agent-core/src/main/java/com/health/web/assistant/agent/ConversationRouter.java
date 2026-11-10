package com.health.web.assistant.agent;

import org.springframework.stereotype.Component;

/**
 * 会话连续性路由与意图线索判定（T1 从 AssistantOrchestrator 拆出的策略组件）。
 *
 * <p>纯函数、零状态，可离线单测与评测（evals/continuity.json 20 案例）。
 * 决策规则：
 * <ol>
 *   <li>会话无当前 Agent → 跟随字面路由；</li>
 *   <li>明确新指令（预约/报告关键词）→ 跟随新意图；</li>
 *   <li>会话正处预约/报告流程时收到主诉 → 留在原 Agent，
 *       除非消息含明显分诊求助词（怎么办/要紧吗…）。</li>
 * </ol>
 */
@Component
public class ConversationRouter {

    private static final String[] TRIAGE_HELP_CUES = {
            "怎么办", "咋办", "要紧", "严重吗", "危险吗", "怎么回事", "是什么病", "什么病",
            "帮我看看", "分析", "疼得", "痛得", "需要注意", "去不去医院", "挂急诊", "会死"
    };

    private static final String[] CANCEL_CUES = {"取消", "不用了", "算了", "不约了", "不挂了", "暂停", "先不了"};

    /** 用户澄清“急诊已排除/看过急诊”后，预约流程可放行并清除急诊事实 */
    private static final String[] EMERGENCY_OVERRIDE_CUES = {
            "不是急症", "排除", "看过急诊", "急诊已排", "没事了", "虚惊", "检查过了", "已就医", "已经去急诊"
    };

    /** 既往/病史等语境词：避免把「以前胸痛过、想复查」这类预约误判为当前急症 */
    private static final String[] PAST_TENSE_CUES = {
            "以前", "之前", "曾", "有过", "病史", "既往", "去年", "上个月", "前阵", "复诊", "复查"
    };

    /** 会话连续性路由决策 + 意图切换判定 */
    public AgentType decideAgent(AgentType fresh, String current, String message) {
        if (current == null) {
            return fresh;
        }
        AgentType cur;
        try {
            cur = AgentType.valueOf(current);
        } catch (Exception e) {
            return fresh;
        }
        if (fresh == cur) {
            return fresh;
        }
        // 明确新指令（预约/报告关键词）→ 跟随新意图
        if (fresh == AgentType.SCHEDULER || fresh == AgentType.REPORT) {
            return fresh;
        }
        // 其余为「消息字面命中分诊」：
        // 会话正处预约/报告流程时，收到主诉/症状回答应留在原 Agent（如预约管家收集主诉“胸痛”），
        // 除非消息含明显分诊求助词（怎么办/要紧吗/严重吗…）。
        if (fresh == AgentType.TRIAGE && (cur == AgentType.SCHEDULER || cur == AgentType.REPORT)) {
            return hasTriageHelpIntent(message) ? AgentType.TRIAGE : cur;
        }
        return fresh;
    }

    public boolean hasTriageHelpIntent(String message) {
        return containsAnyCue(message, TRIAGE_HELP_CUES);
    }

    public boolean hasCancelIntent(String message) {
        return containsAnyCue(message, CANCEL_CUES);
    }

    public boolean hasEmergencyOverride(String message) {
        return containsAnyCue(message, EMERGENCY_OVERRIDE_CUES);
    }

    public boolean hasPastTense(String message) {
        return containsAnyCue(message, PAST_TENSE_CUES);
    }

    private static boolean containsAnyCue(String message, String[] cues) {
        if (message == null) {
            return false;
        }
        for (String cue : cues) {
            if (message.contains(cue)) {
                return true;
            }
        }
        return false;
    }
}
