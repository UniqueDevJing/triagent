package com.health.web.assistant.model;

/**
 * 编排层内部响应对象，收集模型输出并供 Advisor.after 观察。
 */
public class AgentResponse {

    /** 模型聚合后的完整文本 */
    private String fullText;

    /** 解析出的结构化分诊结果（解析失败为 null） */
    private TriageResult triage;

    /** 耗时（毫秒） */
    private long elapsedMs;

    /** 是否被护栏短路 */
    private boolean blocked;

    /** 短路原因 */
    private String blockReason;

    /** Phase4：本轮类型 LLM / EMERGENCY / CLARIFY / BLOCKED（供护栏与审计区分语义） */
    private String turnType;

    /** Phase4：本轮工具调用次数（工具事件统计） */
    private int toolCallCount;

    /** Phase4：本轮工具结果事件数 */
    private int toolResultCount;

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public TriageResult getTriage() {
        return triage;
    }

    public void setTriage(TriageResult triage) {
        this.triage = triage;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public String getTurnType() {
        return turnType;
    }

    public void setTurnType(String turnType) {
        this.turnType = turnType;
    }

    public int getToolCallCount() {
        return toolCallCount;
    }

    public void setToolCallCount(int toolCallCount) {
        this.toolCallCount = toolCallCount;
    }

    public int getToolResultCount() {
        return toolResultCount;
    }

    public void setToolResultCount(int toolResultCount) {
        this.toolResultCount = toolResultCount;
    }
}
