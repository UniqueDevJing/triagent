package com.health.web.assistant.agent;

import com.health.web.assistant.memory.SessionFactStore;
import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import com.health.web.assistant.model.ChatRequest;
import com.health.web.assistant.model.SseEvent;
import com.health.web.assistant.model.TriageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TRIAGE 确定性分支（T1 从 AssistantOrchestrator 拆出的组件）：
 * 红旗警示 → 确定性 EMERGENCY（不耗 LLM）并落库会话事实；字段缺失 → 多轮澄清 CLARIFY。
 * 状态机返回 PROCEED 时返回 null，交回编排层走 LLM 路径。
 */
@Component
public class TriageDeterministicFlow {

    private static final Logger log = LoggerFactory.getLogger(TriageDeterministicFlow.class);

    private final SymptomStateMachine stateMachine;
    private final SessionFactStore sessionFactStore;
    private final TurnFinalizer turnFinalizer;

    public TriageDeterministicFlow(SymptomStateMachine stateMachine,
                                   SessionFactStore sessionFactStore,
                                   TurnFinalizer turnFinalizer) {
        this.stateMachine = stateMachine;
        this.sessionFactStore = sessionFactStore;
        this.turnFinalizer = turnFinalizer;
    }

    /** 处理 EMERGENCY/CLARIFY 决策；PROCEED 返回 null 交回编排层 */
    public Flux<SseEvent> handle(SymptomStateMachine.Decision d,
                                 AgentDefinition agent,
                                 AgentRequest areq,
                                 ChatRequest req,
                                 long start,
                                 boolean triageFirst) {
        long elapsed = System.currentTimeMillis() - start;
        if (d.type() == SymptomStateMachine.DecisionType.EMERGENCY) {
            TriageResult tri = stateMachine.buildEmergencyTriage(d.redFlagWords());
            String answer = stateMachine.emergencyAnswerText(d.redFlagWords());
            log.warn("[agent] 会话={} 红旗拦截 → EMERGENCY，命中={}", req.getSessionId(), d.redFlagWords());
            // 写入会话事实：供同会话后续的预约/轻问诊 Agent 消费（协作）
            sessionFactStore.recordEmergency(req.getSessionId(), tri.getUrgency(),
                    tri.getDepartments(), tri.getDisclaimer());
            AgentResponse resp = new AgentResponse();
            resp.setFullText(answer);
            resp.setTriage(tri);
            resp.setTurnType("EMERGENCY");
            resp.setElapsedMs(elapsed);
            turnFinalizer.finalize(areq, resp, req, d.redFlagWords(), List.of("red-flag-short-circuit"));
            if (triageFirst) {
                return Flux.concat(
                        Flux.just(SseEvent.plan("TRIAGE_FIRST", "预约请求含急症红旗 → 应急分诊优先")),
                        Flux.just(SseEvent.done(answer, tri, agent.getDisplayName(), areq.getSources())));
            }
            return Flux.just(SseEvent.done(answer, tri, agent.getDisplayName(), areq.getSources()));
        }
        if (d.type() == SymptomStateMachine.DecisionType.CLARIFY) {
            String question = d.question();
            log.info("[agent] 会话={} 状态机追问: {}", req.getSessionId(), question);
            AgentResponse resp = new AgentResponse();
            resp.setFullText(question);
            resp.setTurnType("CLARIFY");
            resp.setElapsedMs(elapsed);
            turnFinalizer.finalize(areq, resp, req);
            return Flux.just(
                    SseEvent.clarify(question, d.missingFields()),
                    SseEvent.done(question, null, agent.getDisplayName(), areq.getSources()));
        }
        return null;
    }
}
