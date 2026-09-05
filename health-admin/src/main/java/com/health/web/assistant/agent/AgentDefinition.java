package com.health.web.assistant.agent;

import java.util.Set;

/**
 * 一个 Agent 的静态定义：角色提示词 + 可用工具白名单。
 * 多 Agent 共享同一个 ChatClient（模型/温度一致），仅按定义切换 system prompt 与工具集。
 */
public class AgentDefinition {

    private final AgentType type;
    private final String systemPrompt;
    /** 该 Agent 允许调用的 @Tool 方法名白名单（与 AgentToolkit 中 @Tool name 对应） */
    private final Set<String> allowedTools;

    public AgentDefinition(AgentType type, String systemPrompt, Set<String> allowedTools) {
        this.type = type;
        this.systemPrompt = systemPrompt;
        this.allowedTools = allowedTools;
    }

    public AgentType getType() {
        return type;
    }

    public String getDisplayName() {
        return type.getDisplayName();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public Set<String> getAllowedTools() {
        return allowedTools;
    }
}
