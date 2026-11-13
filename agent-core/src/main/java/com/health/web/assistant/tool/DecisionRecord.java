package com.health.web.assistant.tool;

import java.util.List;

/**
 * 单次决策记录（T3 决策日志与重放）。
 * 覆盖「输入特征 / 规则命中 / LLM 输出 / 最终裁决」，供同一会话重放与可审计复盘。
 */
public record DecisionRecord(
        long ts,
        String sessionId,
        String agentType,      // 本轮决议的 Agent 角色
        String turnType,       // EMERGENCY / CLARIFY / LLM / BLOCKED / SAFETY_BLOCK
        String message,        // 用户消息（已截断）
        String redFlags,       // 命中红旗征象（逗号分隔）
        String ruleHits,       // 触发的规则名（逗号分隔）
        String finalText,      // 助手最终文本（已截断）
        String urgency,        // 分诊紧急度（若有）
        String departments,    // 建议科室（若有）
        String blockedReason,  // 拦截原因（若被拦截）
        long elapsedMs,
        int toolCalls,
        int toolResults
) {
    public static Builder builder() {
        return new Builder();
    }

    /** 构造器：文本类字段自动截断，避免单条记录过大。 */
    public static class Builder {
        private long ts = System.currentTimeMillis();
        private String sessionId, agentType, turnType, message, redFlags, ruleHits;
        private String finalText, urgency, departments, blockedReason;
        private long elapsedMs;
        private int toolCalls, toolResults;

        public Builder ts(long v) { this.ts = v; return this; }
        public Builder sessionId(String v) { this.sessionId = v; return this; }
        public Builder agentType(String v) { this.agentType = v; return this; }
        public Builder turnType(String v) { this.turnType = v; return this; }
        public Builder message(String v) { this.message = v == null ? null : truncate(v, 256); return this; }
        public Builder redFlags(List<String> v) { this.redFlags = (v == null || v.isEmpty()) ? null : String.join(",", v); return this; }
        public Builder ruleHits(List<String> v) { this.ruleHits = (v == null || v.isEmpty()) ? null : String.join(",", v); return this; }
        public Builder finalText(String v) { this.finalText = v == null ? null : truncate(v, 512); return this; }
        public Builder urgency(String v) { this.urgency = v; return this; }
        public Builder departments(List<String> v) { this.departments = (v == null || v.isEmpty()) ? null : String.join(",", v); return this; }
        public Builder blockedReason(String v) { this.blockedReason = v; return this; }
        public Builder elapsedMs(long v) { this.elapsedMs = v; return this; }
        public Builder toolCalls(int v) { this.toolCalls = v; return this; }
        public Builder toolResults(int v) { this.toolResults = v; return this; }

        public DecisionRecord build() {
            return new DecisionRecord(ts, sessionId, agentType, turnType, message, redFlags,
                    ruleHits, finalText, urgency, departments, blockedReason, elapsedMs, toolCalls, toolResults);
        }

        private static String truncate(String s, int n) {
            return s.length() <= n ? s : s.substring(0, n) + "...(" + (s.length() - n) + "字省略)";
        }
    }
}
