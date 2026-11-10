package com.health.web.assistant.agent;

import com.health.web.assistant.memory.SessionFactStore;
import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import com.health.web.assistant.model.SseEvent;
import com.health.web.assistant.model.TriageResult;
import com.health.web.assistant.tool.BudgetGuard;
import com.health.web.assistant.tool.ToolEventPublisher;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * LLM 回合收尾组装器（T1 从 AssistantOrchestrator 拆出的组件）：
 * 流式结束后做结构化分诊解析（含 G8 重试）、EMERGENCY 事实落库、token 计量与回合留痕，
 * 产出 DONE 事件。
 */
@Component
public class LlmTurnAssembler {

    private final ChatClient chatClient;
    private final TriageJsonParser triageParser;
    private final SessionFactStore sessionFactStore;
    private final TurnFinalizer turnFinalizer;

    public LlmTurnAssembler(ChatClient chatClient,
                            TriageJsonParser triageParser,
                            SessionFactStore sessionFactStore,
                            TurnFinalizer turnFinalizer) {
        this.chatClient = chatClient;
        this.triageParser = triageParser;
        this.sessionFactStore = sessionFactStore;
        this.turnFinalizer = turnFinalizer;
    }

    public Mono<SseEvent> assemble(AgentRequest capturedReq,
                                   com.health.web.assistant.model.ChatRequest chat,
                                   AgentType agentType,
                                   String agentDisplayName,
                                   long start,
                                   ToolEventPublisher toolEvents,
                                   StringBuilder acc,
                                   BudgetGuard budgetGuard) {
        return Mono.defer(() -> {
            String full = acc.toString();
            AgentResponse resp = new AgentResponse();
            resp.setFullText(full);
            // G8 重试机制：TRIAGE 轮结构化 JSON 解析失败 → 追加一次「纯 JSON 提炼」调用
            TriageResult tri = agentType == AgentType.TRIAGE ? triageParser.parse(full) : null;
            if (tri == null && agentType == AgentType.TRIAGE) {
                tri = triageParser.retry(chatClient, full);
                if (tri != null) {
                    org.slf4j.LoggerFactory.getLogger(LlmTurnAssembler.class)
                            .info("[agent] 会话={} JSON 重试成功（首轮解析失败）", chat.getSessionId());
                }
            }
            resp.setTriage(tri);
            if (tri != null && "EMERGENCY".equalsIgnoreCase(tri.getUrgency())) {
                sessionFactStore.recordEmergency(capturedReq.getSessionId(), tri.getUrgency(),
                        tri.getDepartments(), tri.getDisclaimer());
            }
            resp.setTurnType("LLM");
            resp.setToolCallCount(toolEvents.getCallCount());
            resp.setToolResultCount(toolEvents.getResultCount());
            resp.setElapsedMs(System.currentTimeMillis() - start);
            if (budgetGuard != null) {
                budgetGuard.recordTokens(capturedReq.getSessionId(), full);
            }
            turnFinalizer.finalize(capturedReq, resp, chat);
            return Mono.just(SseEvent.done(resp.getFullText(), resp.getTriage(),
                    agentDisplayName, capturedReq.getSources()));
        });
    }
}
