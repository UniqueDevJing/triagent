package com.health.web.assistant.model;

import java.util.List;
import java.util.Map;

/**
 * 编排层内部请求对象，在 Advisor 链中流转并被增强。
 */
public class AgentRequest {

    private String sessionId;
    private String message;
    /** 由 MemoryAdvice 注入的历史上下文 */
    private String historyContext;
    /** 系统提示词（基线） */
    private String systemPrompt;
    /** Phase3：路由命中的 Agent 名称（AgentType.name()），供 Advisor 按角色决策 */
    private String agentName;
    /** Phase3：RAG 注入的外部知识块（已标记不可信/来源），由 RagAdvice 填充 */
    private String ragContext;
    /** Phase3：知识来源引用 [{source,title,refId,score}]，随 DONE 事件返回前端 */
    private List<Map<String, Object>> sources;

    public AgentRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHistoryContext() {
        return historyContext;
    }

    public void setHistoryContext(String historyContext) {
        this.historyContext = historyContext;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getRagContext() {
        return ragContext;
    }

    public void setRagContext(String ragContext) {
        this.ragContext = ragContext;
    }

    public List<Map<String, Object>> getSources() {
        return sources;
    }

    public void setSources(List<Map<String, Object>> sources) {
        this.sources = sources;
    }
}
