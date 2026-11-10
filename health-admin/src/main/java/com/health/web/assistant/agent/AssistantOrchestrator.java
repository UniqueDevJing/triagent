package com.health.web.assistant.agent;

import com.health.system.mapper.AssessmentRecordMapper;
import com.health.web.assistant.advisor.AgentAdvice;
import com.health.web.assistant.advisor.AgentBlockedException;
import com.health.web.assistant.memory.AgentSessionState;
import com.health.web.assistant.memory.SessionFactStore;
import com.health.web.assistant.memory.SessionMemoryStore;
import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import com.health.web.assistant.model.ChatRequest;
import com.health.web.assistant.model.SseEvent;
import com.health.web.assistant.model.TriageResult;
import com.health.web.assistant.service.PreOrderService;
import com.health.web.assistant.tool.AgentToolkit;
import com.health.web.assistant.tool.BudgetGuard;
import com.health.web.assistant.tool.ToolAccessGuard;
import com.health.web.assistant.tool.ToolAccessPolicy;
import com.health.web.assistant.tool.ToolEventPublisher;
import com.health.web.assistant.tool.TriageTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.List;

/**
 * Agent 编排层（Phase 3 / T1 瘦身版）：职责收窄为「流程编排」——
 * 路由 → 输入护栏 → 确定性分支（红旗短路/多轮澄清/预算熔断/急诊协作拦截）→ LLM 流式 → 回合收尾。
 * 策略与话术已拆为可独立测试的组件：
 * {@link ConversationRouter} 连续性路由与意图线索、{@link SupervisorSafetyPlanner} 组合请求规划与协作拦截、
 * {@link EmergencyIntervention} 急诊拦截话术、{@link DeterministicFallback} 预算降级话术、
 * {@link TriageJsonParser} 结构化分诊解析与重试、{@link TurnFinalizer} after 链 + 记忆 + 决策日志。
 */
@Service
public class AssistantOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AssistantOrchestrator.class);

    private final ChatClient chatClient;
    private final AgentRegistry agentRegistry;
    private final SymptomStateMachine stateMachine;
    private final TriageTools triageTools;
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final PreOrderService preOrderService;
    private final SessionMemoryStore memoryStore;
    private final AgentSessionState sessionState;
    private final SessionFactStore sessionFactStore;
    private final List<AgentAdvice> advices;
    private final ToolAccessPolicy toolPolicy;
    private final ToolAccessGuard toolGuard;
    private final BudgetGuard budgetGuard;
    private final ConversationRouter router;
    private final SupervisorSafetyPlanner supervisorPlanner;
    private final EmergencyIntervention emergencyIntervention;
    private final DeterministicFallback fallbacks;
    private final TriageJsonParser triageParser;
    private final TurnFinalizer turnFinalizer;
    private final LlmTurnAssembler llmTurnAssembler;

    public AssistantOrchestrator(ChatClient chatClient,
                                 AgentRegistry agentRegistry,
                                 SymptomStateMachine stateMachine,
                                 TriageTools triageTools,
                                 AssessmentRecordMapper assessmentRecordMapper,
                                 PreOrderService preOrderService,
                                 SessionMemoryStore memoryStore,
                                 AgentSessionState sessionState,
                                 SessionFactStore sessionFactStore,
                                 List<AgentAdvice> advices,
                                 ToolAccessPolicy toolPolicy,
                                 ToolAccessGuard toolGuard,
                                 BudgetGuard budgetGuard,
                                 ConversationRouter router,
                                 SupervisorSafetyPlanner supervisorPlanner,
                                 EmergencyIntervention emergencyIntervention,
                                 DeterministicFallback fallbacks,
                                 TriageJsonParser triageParser,
                                 TurnFinalizer turnFinalizer,
                                 LlmTurnAssembler llmTurnAssembler) {
        this.chatClient = chatClient;
        this.agentRegistry = agentRegistry;
        this.stateMachine = stateMachine;
        this.triageTools = triageTools;
        this.assessmentRecordMapper = assessmentRecordMapper;
        this.preOrderService = preOrderService;
        this.memoryStore = memoryStore;
        this.sessionState = sessionState;
        this.sessionFactStore = sessionFactStore;
        this.advices = advices;
        this.toolPolicy = toolPolicy;
        this.toolGuard = toolGuard;
        this.budgetGuard = budgetGuard;
        this.router = router;
        this.supervisorPlanner = supervisorPlanner;
        this.emergencyIntervention = emergencyIntervention;
        this.fallbacks = fallbacks;
        this.triageParser = triageParser;
        this.turnFinalizer = turnFinalizer;
        this.llmTurnAssembler = llmTurnAssembler;
    }

    public Flux<SseEvent> chat(ChatRequest req) {
        return chat(req, null);
    }

    public Flux<SseEvent> chat(ChatRequest req, Long userId) {
        AgentRequest areq = new AgentRequest(req.getSessionId(), req.getMessage());
        long start = System.currentTimeMillis();

        // 路由：会话连续性优先（预约管家收症状时不被「胸痛」抢单成急救），
        // Supervisor 安全规划：预约请求内含当前急症红旗 → 本轮应急分诊优先（0 LLM 确定性输出）。
        AgentType fresh = agentRegistry.resolve(areq.getMessage()).getType();
        AgentType decided = router.decideAgent(fresh, sessionState.get(req.getSessionId()), areq.getMessage());
        boolean triageFirst = decided == AgentType.SCHEDULER
                && supervisorPlanner.shouldTriageFirst(req.getSessionId(), areq.getMessage());
        if (triageFirst) {
            decided = AgentType.TRIAGE;
        }
        if (router.hasCancelIntent(areq.getMessage())) {
            sessionState.clear(req.getSessionId());
        } else {
            sessionState.set(req.getSessionId(), decided.name());
        }
        AgentDefinition agent = agentRegistry.get(decided);
        areq.setAgentName(agent.getType().name());
        if (decided != fresh) {
            log.info("[agent] 会话={} 连续性路由：当前消息字面命中 {}，沿用会话 Agent {}",
                    req.getSessionId(), fresh, decided);
        }

        // Agent 协作（医疗安全）：会话已有 EMERGENCY 事实 → 预约落单前确定性拦截；
        // 用户澄清「已排除/看过急诊」→ 清除事实放行。
        if (agent.getType() == AgentType.SCHEDULER) {
            SessionFactStore.EmergencyFact ef = supervisorPlanner.gateScheduler(req.getSessionId(), areq.getMessage());
            if (ef != null) {
                log.info("[agent] 会话={} 协作拦截：预约请求被本会话 EMERGENCY 分诊事实拦截", req.getSessionId());
                AgentResponse resp = new AgentResponse();
                SseEvent block = emergencyIntervention.blockedSchedulerEvent(ef, areq.getSources(), resp);
                resp.setElapsedMs(System.currentTimeMillis() - start);
                turnFinalizer.finalize(areq, resp, req);
                return Flux.concat(
                        Flux.just(SseEvent.plan("SAFETY_BLOCK", "检测到本会话 EMERGENCY 分诊事实，预约已拦截")),
                        Flux.just(block));
            }
        }

        for (AgentAdvice a : advices) {
            try {
                areq = a.before(areq);
            } catch (AgentBlockedException ex) {
                AgentResponse br = new AgentResponse();
                br.setBlocked(true);
                br.setBlockReason(ex.getMessage());
                br.setTurnType("BLOCKED");
                br.setElapsedMs(System.currentTimeMillis() - start);
                turnFinalizer.finalize(areq, br, req);
                return Flux.just(SseEvent.error(ex.getMessage()));
            }
        }
        final AgentRequest capturedReq = areq;

        // —— TRIAGE 分支：状态机 红旗短路 / 多轮澄清（确定性，离线可演示）——
        if (agent.getType() == AgentType.TRIAGE) {
            SymptomStateMachine.Decision d = stateMachine.decide(req.getSessionId(), areq.getMessage());
            if (d.type() == SymptomStateMachine.DecisionType.EMERGENCY) {
                TriageResult tri = stateMachine.buildEmergencyTriage(d.redFlagWords());
                String answer = stateMachine.emergencyAnswerText(d.redFlagWords());
                log.warn("[agent] 会话={} 红旗拦截 → EMERGENCY，命中={}", req.getSessionId(), d.redFlagWords());
                sessionFactStore.recordEmergency(req.getSessionId(), tri.getUrgency(),
                        tri.getDepartments(), tri.getDisclaimer());
                AgentResponse resp = new AgentResponse();
                resp.setFullText(answer);
                resp.setTriage(tri);
                resp.setTurnType("EMERGENCY");
                resp.setElapsedMs(System.currentTimeMillis() - start);
                turnFinalizer.finalize(capturedReq, resp, req, d.redFlagWords(), List.of("red-flag-short-circuit"));
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
                resp.setElapsedMs(System.currentTimeMillis() - start);
                turnFinalizer.finalize(capturedReq, resp, req);
                return Flux.just(
                        SseEvent.clarify(question, d.missingFields()),
                        SseEvent.done(question, null, agent.getDisplayName(), areq.getSources()));
            }
        }

        // —— 预算熔断：会话 token 预算耗尽 → 跳过 LLM，降级到确定性规则（0 token 消耗）——
        if (budgetGuard != null && budgetGuard.tokenExceeded(req.getSessionId())) {
            String reason = budgetGuard.degradeReason(req.getSessionId());
            log.warn("[agent] 会话={} 预算熔断：{} → 确定性降级（跳过 LLM）", req.getSessionId(), reason);
            String fallback = fallbacks.answer(agent.getType());
            AgentResponse resp = new AgentResponse();
            resp.setFullText(fallback);
            resp.setTurnType("DEGRADED");
            resp.setElapsedMs(System.currentTimeMillis() - start);
            turnFinalizer.finalize(capturedReq, resp, req, null, List.of("budget-token-exceeded"));
            return Flux.just(SseEvent.done(fallback, null, agent.getDisplayName(), areq.getSources()));
        }

        // —— LLM 路径（PROCEED / REPORT / SCHEDULER）——
        StringBuilder acc = new StringBuilder();
        Sinks.Many<SseEvent> toolSink = Sinks.many().multicast().onBackpressureBuffer();
        ToolEventPublisher toolEvents = new ToolEventPublisher(toolSink);
        // 每请求构造工具集：ToolAccessGuard 在方法入口做真实越权校验 + 审计；
        // BudgetGuard 在同一入口做工具调用次数熔断（T5），超限抛 AgentBlockedException 降级。
        AgentToolkit toolkit = new AgentToolkit(toolGuard, budgetGuard, agent.getType(), req.getSessionId(),
                userId, toolEvents, triageTools, assessmentRecordMapper, preOrderService);
        // 纵深防御：按声明式策略把越权工具从模型可见列表摘除（模型不会去调），
        // 真实放行/拦截决策与审计由 AgentToolkit 内的 guard.check 在调用时记录。
        ToolCallback[] callbacks = Arrays.stream(ToolCallbacks.from(toolkit))
                .filter(cb -> toolPolicy.isAllowed(agent.getType(), cb.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);

        String userPrompt = (areq.getRagContext() != null ? areq.getRagContext() + "\n" : "")
                + buildPrompt(areq.getMessage(), memoryStore.getHistory(req.getSessionId()));
        log.info("[agent] 会话={} agent={} tools={} ragSources={} userId={}",
                req.getSessionId(), agent.getType(), agent.getAllowedTools(),
                areq.getSources() == null ? 0 : areq.getSources().size(), userId);

        Flux<SseEvent> textFlux = chatClient.prompt()
                .system(agent.getSystemPrompt())
                .user(userPrompt)
                .toolCallbacks(callbacks)
                .stream()
                .content()
                .flatMap(token -> {
                    acc.append(token);
                    return Flux.just(SseEvent.token(token));
                })
                .doFinally(sig -> toolEvents.complete());

        return Flux.merge(textFlux, toolSink.asFlux())
                .concatWith(llmTurnAssembler.assemble(
                        capturedReq, req, agent.getType(), agent.getDisplayName(), start, toolEvents, acc, budgetGuard))
                .onErrorResume(e -> {
                    log.warn("[agent] 会话={} 推理失败: {}", req.getSessionId(), e.getMessage());
                    return Flux.just(SseEvent.error("服务暂时不可用，请稍后重试"));
                });
    }

    private String buildPrompt(String message, String history) {
        if (history == null || history.isBlank()) {
            return message;
        }
        return "历史对话：\n" + history + "\n\n当前用户：" + message;
    }
}
