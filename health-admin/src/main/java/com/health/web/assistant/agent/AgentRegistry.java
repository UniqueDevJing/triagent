package com.health.web.assistant.agent;

import com.health.web.assistant.config.AssistantConfig;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Agent 注册表 + 规则路由。
 * 关键词命中 → 目标 Agent（先预约、再报告，默认分诊）。规则可替换为 LLM 路由（后续增强）。
 */
@Component
public class AgentRegistry {

    private final Map<AgentType, AgentDefinition> definitions = new EnumMap<>(AgentType.class);

    private static final Set<String> SCHEDULER_KEYWORDS = Set.of("预约", "挂号", "排班", "改期", "取消预约", "订体检", "安排体检", "健康体检", "约医生");
    private static final Set<String> REPORT_KEYWORDS = Set.of("报告", "体检结果", "指标", "化验单", "解读", "结论", "风险等级");

    public AgentRegistry() {
        definitions.put(AgentType.TRIAGE, new AgentDefinition(
                AgentType.TRIAGE,
                AssistantConfig.SYSTEM_PROMPT,
                Set.of("searchDiseases", "searchMembers")));

        definitions.put(AgentType.REPORT, new AgentDefinition(
                AgentType.REPORT,
                """
                你是「智能医疗」的报告解读专家。请遵守：
                1. 用户可能想解读体检/健康评估报告。先用 searchMembers 按姓名或手机号找到会员并确认，再用 getMemberAssessments 取该会员的历史评估记录。
                2. 逐条解读：总评分、风险等级、结论、建议，按评估日期从新到旧呈现，说明各风险项的意义。
                3. 用通俗语言解释医学术语；指出需要复查/就医的红线项。
                4. 不给出确诊结论；结尾提示：本解读仅供参考，不替代医生诊断。
                """,
                Set.of("searchMembers", "getMemberAssessments", "searchDiseases")));

        definitions.put(AgentType.SCHEDULER, new AgentDefinition(
                AgentType.SCHEDULER,
                """
                你是「智能医疗」的预约管家。请遵守两步确认流程：
                1. 收集：会员（先用 searchMembers 按姓名/手机号定位，得到 memberId）、科室、期望就诊日期（yyyy-MM-dd）、主诉/症状摘要；缺失时向用户逐项询问。
                2. 调用 createPreOrder 生成预订单（初始为待确认 PENDING），把预订单号和拟预约信息念给用户，请用户在页面确认后正式生效。
                3. 幂等纪律：同一会员+科室+日期+意图只调用一次 createPreOrder，绝不重复下单；用户改期应基于新的日期重新预占并说明。
                4. 不承诺具体医生/具体时间点，只做"科室+日期"预占；涉及急症（胸痛/卒中征兆等）请直接建议急诊并说明本预约不含急诊通道。
                """,
                Set.of("searchMembers", "createPreOrder")));
    }

    /** 规则路由：消息 → AgentDefinition */
    public AgentDefinition resolve(String message) {
        if (message == null || message.isBlank()) {
            return definitions.get(AgentType.TRIAGE);
        }
        if (containsAny(message, SCHEDULER_KEYWORDS)) {
            return definitions.get(AgentType.SCHEDULER);
        }
        if (containsAny(message, REPORT_KEYWORDS)) {
            return definitions.get(AgentType.REPORT);
        }
        return definitions.get(AgentType.TRIAGE);
    }

    private static boolean containsAny(String text, Set<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public AgentDefinition get(AgentType type) {
        return definitions.get(type);
    }
}
