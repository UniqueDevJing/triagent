package com.health.web.assistant.a2a;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A AgentCard：Agent 的能力自述（发现/协商入口）。
 *
 * <p>语义对齐 Agent2Agent 的 {@code /.well-known/agent.json}：
 * 声明身份、支持的输入输出模式、技能清单与工具能力白名单。
 * 与 {@link com.health.web.assistant.tool.ToolAccessPolicy} 的区别是层次不同——
 * 策略表管「运行时能否调用」，AgentCard 管「跨 Agent 交接前能否接这个任务」，
 * 后者由 AgentCardRegistry 从前者生成，保证自述与真实权限不会漂移。
 *
 * <p>刻意不引用任何外部 SDK：A2A 的 Java SDK 尚未 GA，这里只落地稳定的协议语义
 * （Task / AgentCard / Skill / handoff），可被标准客户端按 JSON 消费，也能被追问实现细节。
 */
public record AgentCard(
        String agentId,
        String name,
        String description,
        String version,
        String protocol,
        List<String> inputModes,
        List<String> outputModes,
        List<AgentSkill> skills,
        List<String> capabilities,
        boolean streaming,
        String safetyLevel) {

    public static final String PROTOCOL = "A2A-semantic/0.3";

    public AgentCard {
        inputModes = inputModes == null ? List.of() : List.copyOf(inputModes);
        outputModes = outputModes == null ? List.of() : List.copyOf(outputModes);
        skills = skills == null ? List.of() : List.copyOf(skills);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }

    /** 目标 Agent 是否声明了自己能接该意图（skill id 精确匹配） */
    public boolean supports(String intent) {
        if (intent == null || intent.isBlank()) {
            return false;
        }
        return skills.stream().anyMatch(s -> intent.equals(s.id()));
    }

    /** 目标 Agent 的工具白名单是否覆盖本次任务所需的全部能力 */
    public boolean covers(List<String> requiredCapabilities) {
        if (requiredCapabilities == null || requiredCapabilities.isEmpty()) {
            return true;
        }
        return capabilities.containsAll(requiredCapabilities);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("agentId", agentId);
        m.put("name", name);
        m.put("description", description);
        m.put("version", version);
        m.put("protocol", protocol);
        m.put("inputModes", inputModes);
        m.put("outputModes", outputModes);
        m.put("skills", skills.stream().map(AgentSkill::toMap).toList());
        m.put("capabilities", capabilities);
        m.put("streaming", streaming);
        m.put("safetyLevel", safetyLevel);
        return m;
    }
}
