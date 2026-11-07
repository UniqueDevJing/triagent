package com.health.web.assistant.tool;

import com.health.web.assistant.agent.AgentType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具访问控制策略（T2）：声明式「角色 → 能力 → 工具」三级映射。
 * 替代原先散落在 AgentRegistry 各 AgentDefinition 里的扁平 allowedTools 字符串集合，
 * 成为唯一权威来源（single source of truth）；AgentRegistry 据此构建 AgentDefinition。
 *
 * 越权审计由 AssistantOrchestrator 在工具放行/拦截时调用 {@link ToolAuditLog} 记录。
 */
@Component
public class ToolAccessPolicy {

    private final Map<AgentType, Set<ToolCapability>> policy = new EnumMap<>(AgentType.class);

    public ToolAccessPolicy() {
        // 分诊顾问：只看疾病库与会员（定位用），不碰评估记录与下单；
        // W4 起可用于"解释性配图"（如症状示意图），但不出诊断影像
        policy.put(AgentType.TRIAGE,
                EnumSet.of(ToolCapability.DISEASE_SEARCH, ToolCapability.MEMBER_SEARCH,
                        ToolCapability.IMAGE_GENERATE));
        // 报告解读：读会员 + 读评估记录 + 查疾病（对照报告结论），不可下单；可出宣教示意图
        policy.put(AgentType.REPORT,
                EnumSet.of(ToolCapability.MEMBER_SEARCH, ToolCapability.ASSESSMENT_READ,
                        ToolCapability.DISEASE_SEARCH, ToolCapability.IMAGE_GENERATE));
        // 预约管家：查会员 + 建预订单（写），不读评估、不查疾病库，也不出图
        policy.put(AgentType.SCHEDULER,
                EnumSet.of(ToolCapability.MEMBER_SEARCH, ToolCapability.APPOINTMENT_CREATE));
    }

    /** 该角色被声明允许的工具名集合（能力展开后的扁平集合） */
    public Set<String> allowedTools(AgentType role) {
        Set<String> tools = new java.util.HashSet<>();
        for (ToolCapability cap : policy.getOrDefault(role, Set.of())) {
            tools.addAll(cap.toolNames());
        }
        return tools;
    }

    /** 判断某工具是否对当前角色越权（供编排层拦截与单测：越权拦截率） */
    public boolean isAllowed(AgentType role, String toolName) {
        return allowedTools(role).contains(toolName);
    }

    /** 该角色拥有的全部能力（供治理/看板展示） */
    public Set<ToolCapability> capabilitiesOf(AgentType role) {
        return policy.getOrDefault(role, Set.of());
    }
}
