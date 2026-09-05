package com.health.web.assistant.agent;

/**
 * Agent 类型（Phase 2 多 Agent 路由的目标）。
 */
public enum AgentType {
    /** 症状分诊顾问：推断疾病/科室/紧急度，输出结构化 TriageResult */
    TRIAGE("分诊顾问"),
    /** 报告解读专家：解读体检/评估报告 */
    REPORT("报告解读"),
    /** 预约管家：两步确认式预约（幂等预订单） */
    SCHEDULER("预约管家");

    private final String displayName;

    AgentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
