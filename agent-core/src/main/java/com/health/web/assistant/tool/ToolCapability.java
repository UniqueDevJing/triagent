package com.health.web.assistant.tool;

import java.util.List;

/**
 * 工具能力（语义分组）：把具体 @Tool 方法名从「角色策略」中解耦（T2）。
 * 角色只声明需要哪些能力（如 MEMBER_SEARCH），能力再映射到一组工具名；
 * 工具改名只需改本枚举，角色策略无需动，且能力命名自带可审查语义
 * （如 APPOINTMENT_CREATE 明显比 createPreOrder 危险，便于红线审查）。
 */
public enum ToolCapability {

    /** 按症状检索疾病库 */
    DISEASE_SEARCH(List.of("searchDiseases")),
    /** 按姓名/手机号检索会员 */
    MEMBER_SEARCH(List.of("searchMembers")),
    /** 读取会员历史评估/体检记录（含健康数据，需授权） */
    ASSESSMENT_READ(List.of("getMemberAssessments")),
    /** 创建预约预订单（写操作，仅预约管家可用） */
    APPOINTMENT_CREATE(List.of("createPreOrder")),
    /**
     * 生成式配图（W4）：仅限患者教育示意图。
     * 单列成能力而非混进现有项，是为了让"谁能出图"在策略表里一眼可审——
     * 预约管家不需要出图，就不该有这项能力。
     */
    IMAGE_GENERATE(List.of("generateImage"));

    private final List<String> toolNames;

    ToolCapability(List<String> toolNames) {
        this.toolNames = toolNames;
    }

    /** 该能力对应的具体 @Tool 方法名 */
    public List<String> toolNames() {
        return toolNames;
    }
}
