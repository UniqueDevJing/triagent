package com.health.web.assistant.agent;

import com.health.web.assistant.advisor.AgentAdvice;
import com.health.web.assistant.memory.SessionMemoryStore;
import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import com.health.web.assistant.model.ChatRequest;
import com.health.web.assistant.model.TriageResult;
import com.health.web.assistant.tool.DecisionLog;
import com.health.web.assistant.tool.DecisionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 回合收尾器（T1 从 AssistantOrchestrator 拆出的治理组件）：
 * after 链 + 会话记忆落库 + 决策日志（T3，供会话重放与审计复盘）。
 * 决策日志失败只告警不阻断主流程。
 */
@Component
public class TurnFinalizer {

    private static final Logger log = LoggerFactory.getLogger(TurnFinalizer.class);

    private final List<AgentAdvice> advices;
    private final SessionMemoryStore memoryStore;
    private final DecisionLog decisionLog;

    public TurnFinalizer(List<AgentAdvice> advices,
                         SessionMemoryStore memoryStore,
                         DecisionLog decisionLog) {
        this.advices = advices.stream()
                .sorted(Comparator.comparingInt(AgentAdvice::getOrder))
                .toList();
        this.memoryStore = memoryStore;
        this.decisionLog = decisionLog;
    }

    /** 无规则命中的简化收尾 */
    public void finalize(AgentRequest request, AgentResponse response, ChatRequest chat) {
        finalize(request, response, chat, null, null);
    }

    /** 带「输入特征/规则命中」的完整收尾 */
    public void finalize(AgentRequest request, AgentResponse response, ChatRequest chat,
                         List<String> redFlags, List<String> ruleHits) {
        for (AgentAdvice a : advices) {
            try {
                a.after(request, response);
            } catch (Exception e) {
                log.warn("[agent] Advisor.after 异常 {}: {}", a.getClass().getSimpleName(), e.getMessage());
            }
        }
        memoryStore.append(chat.getSessionId(), chat.getMessage(), response.getFullText());
        if (decisionLog != null) {
            try {
                TriageResult tri = response.getTriage();
                decisionLog.record(DecisionRecord.builder()
                        .sessionId(chat.getSessionId())
                        .agentType(request.getAgentName())
                        .turnType(response.getTurnType())
                        .message(chat.getMessage())
                        .redFlags(redFlags)
                        .ruleHits(ruleHits)
                        .finalText(response.getFullText())
                        .urgency(tri == null ? null : tri.getUrgency())
                        .departments(tri == null ? null : tri.getDepartments())
                        .blockedReason(response.isBlocked() ? response.getBlockReason() : null)
                        .elapsedMs(response.getElapsedMs())
                        .toolCalls(response.getToolCallCount())
                        .toolResults(response.getToolResultCount())
                        .build());
            } catch (Exception e) {
                log.warn("[agent] 决策日志记录失败（忽略，不影响主流程）: {}", e.getMessage());
            }
        }
    }
}
