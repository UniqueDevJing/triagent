package com.health.web.assistant.agent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.system.mapper.AssessmentRecordMapper;
import com.health.web.assistant.a2a.AgentCardRegistry;
import com.health.web.assistant.a2a.AgentHandoff;
import com.health.web.assistant.a2a.HandoffGate;
import com.health.web.assistant.a2a.HandoffLedger;
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
import com.health.web.assistant.tool.DecisionLog;
import com.health.web.assistant.tool.DecisionRecord;
import com.health.web.assistant.tool.ToolAccessGuard;
import com.health.web.assistant.tool.ToolAccessPolicy;
import com.health.web.assistant.tool.ToolEventPublisher;
import com.health.web.assistant.tool.TriageTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Agent 编排层（Phase 3）：
 * 1. 先路由（AgentRegistry）再跑 Advisor 管道（RagAdvice 需知 agent 角色），输入护栏可短路；
 * 2. TRIAGE 分支接入分诊状态机：红旗警示 → 确定性 EMERGENCY（不耗 LLM）；字段缺失 → 多轮澄清 CLARIFY；
 * 3. RAG：RagAdvice 已注入「外部不可信」知识上下文与来源，随 DONE 事件回传前端（可溯源）；
 * 4. PROCEED 路径：每请求可观测 AgentToolkit（TOOL_CALL/TOOL_RESULT 事件经 Sinks 与 token 流 merge）；
 * 5. 结构化分诊用 BeanOutputConverter(TriageResult) 绑定；DONE 携带 agent 名与来源。
 */
@Service
public class AssistantOrchestrator {

    /** 护栏自身故障时的统一兜底话术（不复述用户提问，避免二次泄露） */
    private static final String BLOCKED_BY_GUARD_FAILURE =
            "⚠️ 本次回答未通过安全校验（护栏执行异常），为用药安全已拦截。"
            + "请稍后重试；如确有不适，请前往正规医院由执业医师面诊。";

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
    private final DecisionLog decisionLog;
    private final BudgetGuard budgetGuard;
    /** A2A 能力卡/评估门（可为 null：单测或降级场景跳过交接契约，不影响既有确定性逻辑） */
    private final AgentCardRegistry cardRegistry;
    /** A2A 交接台账：按会话留痕，供 /api/v1/assistant/handoff-log 回放 */
    private final HandoffLedger handoffLedger;

    /** 结构化分诊输出绑定器（BeanOutputConverter 负责 JSON schema 解析/校验） */
    private final BeanOutputConverter<TriageResult> triageConverter = new BeanOutputConverter<>(TriageResult.class);
    private final ObjectMapper lenientMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** 兼容构造：A2A 组件缺席时退化为原有的纯路由模式（单测与降级场景） */
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
                                 DecisionLog decisionLog,
                                 BudgetGuard budgetGuard) {
        this(chatClient, agentRegistry, stateMachine, triageTools, assessmentRecordMapper,
                preOrderService, memoryStore, sessionState, sessionFactStore, advices,
                toolPolicy, toolGuard, decisionLog, budgetGuard, null, null);
    }

    @Autowired
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
                                 DecisionLog decisionLog,
                                 BudgetGuard budgetGuard,
                                 AgentCardRegistry cardRegistry,
                                 HandoffLedger handoffLedger) {
        this.chatClient = chatClient;
        this.agentRegistry = agentRegistry;
        this.stateMachine = stateMachine;
        this.triageTools = triageTools;
        this.assessmentRecordMapper = assessmentRecordMapper;
        this.preOrderService = preOrderService;
        this.memoryStore = memoryStore;
        this.sessionState = sessionState;
        this.sessionFactStore = sessionFactStore;
        this.advices = advices.stream()
                .sorted(Comparator.comparingInt(AgentAdvice::getOrder))
                .toList();
        this.toolPolicy = toolPolicy;
        this.toolGuard = toolGuard;
        this.decisionLog = decisionLog;
        this.budgetGuard = budgetGuard;
        this.cardRegistry = cardRegistry;
        this.handoffLedger = handoffLedger;
    }

    public Flux<SseEvent> chat(ChatRequest req) {
        return chat(req, null);
    }

    public Flux<SseEvent> chat(ChatRequest req, Long userId) {
        AgentRequest areq = new AgentRequest(req.getSessionId(), req.getMessage());
        long start = System.currentTimeMillis();

        // 路由：会话连续性优先（预约管家收症状时不被「胸痛」抢单成急救），
        // 只有明确的新意图（预约/报告指令词、分诊求助词）或取消词才切换/清空。
        AgentType fresh = agentRegistry.resolve(areq.getMessage()).getType();
        String previousAgent = sessionState == null ? null : sessionState.get(req.getSessionId());
        AgentType decided = decideAgent(fresh, previousAgent, areq.getMessage());
        boolean supervisorPlanned = false;

        // —— Supervisor 规划（组合请求，医疗安全优先）——
        // 同一句话里既预约又报告当前急症红旗（如「胸痛…帮我约…」）→ 不进入预约分支，
        // 规划为「应急分诊优先」：把本轮到 TRIAGE，由红旗状态机确定性输出 EMERGENCY（0 LLM 调用）。
        // 既往/复诊语境（以前胸痛过、复查）不会触发，避免误伤。
        if (decided == AgentType.SCHEDULER
                && !stateMachine.emergencyOverrideClearsFact(areq.getMessage())
                && sessionFactStore.getEmergency(req.getSessionId()) == null) {
            List<String> reds = stateMachine.redFlagsIn(areq.getMessage());
            if (!reds.isEmpty() && !hasPastTense(areq.getMessage())) {
                log.info("[supervisor] 会话={} 规划决策：预约请求内含急症红旗 {} → 应急分诊优先（跳过预约分支）",
                        req.getSessionId(), reds);
                decided = AgentType.TRIAGE;
                supervisorPlanned = true;
            }
        }
        if (hasCancelIntent(areq.getMessage())) {
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

        // —— A2A 交接契约：把 Supervisor 的隐式路由升级为「带准入条件的显式任务派发」——
        // 评估门是确定性的（不调 LLM）：目标 Agent 必须已注册能力卡、声明对应技能、
        // 工具白名单覆盖任务所需能力、随附上下文证据齐备、且不与 EMERGENCY 事实冲突。
        // 结论写入 HandoffLedger（可按会话回放），跨 Agent/被拒时复用 PLAN 事件下发，
        // 不新增 SSE 事件名，前端契约保持不变。
        Flux<SseEvent> handoffPrefix = Flux.empty();
        if (cardRegistry != null) {
            SessionFactStore.EmergencyFact fact = sessionFactStore == null
                    ? null : sessionFactStore.getEmergency(req.getSessionId());
            boolean cancel = hasCancelIntent(areq.getMessage());
            List<String> contextRefs = new ArrayList<>();
            contextRefs.add("session:" + req.getSessionId());
            if (previousAgent != null) {
                contextRefs.add("prior-agent:" + previousAgent);
            }
            if (fact != null) {
                contextRefs.add("fact:EMERGENCY");
            }
            Set<String> evidence = new LinkedHashSet<>();
            evidence.add("user-message");
            if (previousAgent != null) {
                evidence.add("session-state");
            }
            if (fact != null) {
                evidence.add("emergency-fact");
            }
            AgentHandoff handoff = cardRegistry.createHandoff(req.getSessionId(),
                    previousAgent, decided, contextRefs, List.of("user-message"));
            HandoffGate.HandoffContext gateCtx = new HandoffGate.HandoffContext(
                    fact != null, supervisorPlanned || cancel, evidence);
            HandoffGate.GateVerdict verdict = cardRegistry.gate().evaluate(handoff, gateCtx);
            if (handoffLedger != null) {
                handoffLedger.append(verdict.handoff());
            }
            if (!verdict.accepted() || handoff.isCrossAgent() || supervisorPlanned) {
                log.info("[a2a] 会话={} {}", req.getSessionId(), verdict.summary());
                handoffPrefix = Flux.just(SseEvent.plan(
                        verdict.accepted() ? "A2A_HANDOFF" : "A2A_BLOCKED", verdict.summary()));
            }
        }

        // Agent 协作（医疗安全）：会话已有 EMERGENCY 事实 → 预约落单前确定性拦截；
        // 用户澄清「已排除/看过急诊」→ 清除事实放行。
        if (agent.getType() == AgentType.SCHEDULER) {
            boolean override = stateMachine.emergencyOverrideClearsFact(areq.getMessage());
            SessionFactStore.EmergencyFact ef = sessionFactStore.getEmergency(req.getSessionId());
            if (override) {
                if (ef != null) {
                    sessionFactStore.clearEmergency(req.getSessionId());
                    log.info("[agent] 会话={} 用户澄清急诊已排除，恢复预约流程", req.getSessionId());
                }
            } else if (ef != null) {
                log.info("[agent] 会话={} 协作拦截：预约请求被本会话 EMERGENCY 分诊事实拦截", req.getSessionId());
                return handoffPrefix.concatWith(Flux.concat(
                        Flux.just(SseEvent.plan("SAFETY_BLOCK", "检测到本会话 EMERGENCY 分诊事实，预约已拦截")),
                        Flux.just(emergencyBlocksScheduler(ef, areq, req, start))));
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
                completeTurn(areq, br, req);
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
                // 写入会话事实：供同会话后续的预约/轻问诊 Agent 消费（协作）
                sessionFactStore.recordEmergency(req.getSessionId(), tri.getUrgency(),
                        tri.getDepartments(), tri.getDisclaimer());
                AgentResponse resp = new AgentResponse();
                resp.setFullText(answer);
                resp.setTriage(tri);
                resp.setTurnType("EMERGENCY");
                resp.setElapsedMs(System.currentTimeMillis() - start);
                completeTurn(capturedReq, resp, req, d.redFlagWords(), List.of("red-flag-short-circuit"));
                if (supervisorPlanned) {
                    return handoffPrefix.concatWith(Flux.concat(
                            Flux.just(SseEvent.plan("TRIAGE_FIRST", "预约请求含急症红旗 → 应急分诊优先")),
                            Flux.just(SseEvent.done(answer, tri, agent.getDisplayName(), areq.getSources()))));
                }
                return handoffPrefix.concatWith(Flux.just(
                        SseEvent.done(answer, tri, agent.getDisplayName(), areq.getSources())));
            }
            if (d.type() == SymptomStateMachine.DecisionType.CLARIFY) {
                String question = d.question();
                log.info("[agent] 会话={} 状态机追问: {}", req.getSessionId(), question);
                AgentResponse resp = new AgentResponse();
                resp.setFullText(question);
                resp.setTurnType("CLARIFY");
                resp.setElapsedMs(System.currentTimeMillis() - start);
                completeTurn(capturedReq, resp, req);
                return handoffPrefix.concatWith(Flux.just(
                        SseEvent.clarify(question, d.missingFields()),
                        SseEvent.done(question, null, agent.getDisplayName(), areq.getSources())));
            }
        }

        // —— T5 预算熔断：会话 token 预算耗尽 → 跳过 LLM，降级到确定性规则（0 token 消耗）——
        if (budgetGuard != null && budgetGuard.tokenExceeded(req.getSessionId())) {
            String reason = budgetGuard.degradeReason(req.getSessionId());
            log.warn("[agent] 会话={} 预算熔断：{} → 确定性降级（跳过 LLM）", req.getSessionId(), reason);
            String fallback = deterministicFallbackAnswer(agent.getType());
            AgentResponse resp = new AgentResponse();
            resp.setFullText(fallback);
            resp.setTurnType("DEGRADED");
            resp.setElapsedMs(System.currentTimeMillis() - start);
            completeTurn(capturedReq, resp, req, null,
                    List.of("budget-token-exceeded"));
            return handoffPrefix.concatWith(Flux.just(
                    SseEvent.done(fallback, null, agent.getDisplayName(), areq.getSources())));
        }

        // —— LLM 路径（PROCEED / REPORT / SCHEDULER）——
        StringBuilder acc = new StringBuilder();
        Sinks.Many<SseEvent> toolSink = Sinks.many().multicast().onBackpressureBuffer();
        ToolEventPublisher toolEvents = new ToolEventPublisher(toolSink);
        // 每请求构造工具集：携带当前角色与会话，使 ToolAccessGuard 在方法入口做真实越权校验 + 审计；
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

        return handoffPrefix.concatWith(Flux.merge(textFlux, toolSink.asFlux())
                .concatWith(Mono.defer(() -> {
                    String full = acc.toString();
                    AgentResponse resp = new AgentResponse();
                    resp.setFullText(full);
                    // 结构化自校正：解析 + 字段级校验，失败则带错误原因定向修复（成本封顶）
                    TriageResult tri = agent.getType() == AgentType.TRIAGE
                            ? selfCorrectingTriage(full, req.getSessionId()) : null;
                    resp.setTriage(tri);
                    if (tri != null && "EMERGENCY".equalsIgnoreCase(tri.getUrgency())) {
                        sessionFactStore.recordEmergency(req.getSessionId(), tri.getUrgency(),
                                tri.getDepartments(), tri.getDisclaimer());
                    }
                    resp.setTurnType("LLM");
                    resp.setToolCallCount(toolEvents.getCallCount());
                    resp.setToolResultCount(toolEvents.getResultCount());
                    resp.setElapsedMs(System.currentTimeMillis() - start);
                    if (budgetGuard != null) {
                        budgetGuard.recordTokens(req.getSessionId(), full);
                    }
                    completeTurn(capturedReq, resp, req);
                    return Mono.just(SseEvent.done(resp.getFullText(), resp.getTriage(),
                            agent.getDisplayName(), capturedReq.getSources()));
                }))
                .onErrorResume(e -> {
                    log.warn("[agent] 会话={} 推理失败: {}", req.getSessionId(), e.getMessage());
                    return Flux.just(SseEvent.error("服务暂时不可用，请稍后重试"));
                }));
    }

    /** 结构化自校正最多追加的定向修复调用次数（加上首轮解析，总模型调用 ≤ 3，成本封顶） */
    private static final int MAX_TRIAGE_REPAIR_ATTEMPTS = 2;

    /**
     * 自校正结构化输出：首轮解析 + 字段级校验，未通过则把**具体违规原因**回注给模型做定向修复。
     * 与朴素重试的区别：重试提示词携带上一轮输出的确切违规列表（如 confidence="medium" 非 0~1 数字），
     * 模型修的是「被点名的错误」而不是重新猜。全部失败返回 null，由上游走无结构化降级。
     */
    private TriageResult selfCorrectingTriage(String fullText, String sessionId) {
        // 第 1 次：直接解析首轮输出（BeanOutputConverter 优先，lenient 兜底在 parseTriage 内部）
        TriageResult first = parseTriage(fullText);
        List<String> errors = validateTriage(first);
        if (first != null && errors.isEmpty()) {
            return first;
        }
        if (first == null) {
            errors = new ArrayList<>(List.of("输出无法解析为合法 JSON（可能混有正文、Markdown 代码块或字段残缺）"));
        }

        // 第 2~N 次：携带累积违规列表的定向修复调用
        for (int attempt = 1; attempt <= MAX_TRIAGE_REPAIR_ATTEMPTS; attempt++) {
            TriageResult fixed = repairTriageJson(fullText, errors);
            if (fixed == null) {
                log.warn("[agent] 会话={} 自校正第 {} 次修复调用失败（无有效输出）", sessionId, attempt);
                continue;
            }
            List<String> remain = validateTriage(fixed);
            if (remain.isEmpty()) {
                log.info("[agent] 会话={} 自校正成功：第 {} 次定向修复后通过校验（此前违规: {}）",
                        sessionId, attempt, errors);
                return fixed;
            }
            log.info("[agent] 会话={} 自校正第 {} 次修复后仍有违规: {}（累计反馈给下一轮）", sessionId, attempt, remain);
            errors = remain;
        }
        log.warn("[agent] 会话={} 自校正耗尽（{} 次修复），最终违规: {}", sessionId, MAX_TRIAGE_REPAIR_ATTEMPTS, errors);
        return null;
    }

    /**
     * 字段级语义校验：返回违规列表（空 = 通过）。
     * 硬约束只挑语义承重的三项——urgency 驱动安全护栏、departments 驱动预约、confidence 驱动低置信升级；
     * disclaimer/followUp 缺失只影响展示，不触发重试（控制重试率与成本）。
     */
    private List<String> validateTriage(TriageResult t) {
        List<String> errs = new ArrayList<>();
        if (t == null) {
            return errs;
        }
        String u = t.getUrgency() == null ? "" : t.getUrgency().trim().toUpperCase();
        if (u.isEmpty()) {
            errs.add("urgency 缺失（必须 ∈ [EMERGENCY,URGENT,ROUTINE]）");
        } else if (!"EMERGENCY".equals(u) && !"URGENT".equals(u) && !"ROUTINE".equals(u)) {
            errs.add("urgency 取值非法: \"" + t.getUrgency() + "\"（必须 ∈ [EMERGENCY,URGENT,ROUTINE]，禁止 medium/low 等文本）");
        }
        if (t.getDepartments() == null || t.getDepartments().isEmpty()) {
            errs.add("departments 缺失或为空数组（至少给出一个推荐科室）");
        }
        if (t.getConfidence() == null) {
            errs.add("confidence 缺失（必须是 0~1 的数字，禁止 \"medium\" 等文本档位）");
        } else if (t.getConfidence() < 0 || t.getConfidence() > 1) {
            errs.add("confidence 越界: " + t.getConfidence() + "（必须落在 0~1 区间，如 0.82）");
        }
        return errs;
    }

    /**
     * 定向修复调用：把违规列表回注给模型。相比旧的 G8 重试，提示词从
     * 「请提炼成合法 JSON」升级为「上一轮输出违反了以下约束：…请逐条修正」。
     */
    private TriageResult repairTriageJson(String fullText, List<String> violations) {
        if (fullText == null || fullText.isBlank()) {
            return null;
        }
        try {
            String text = fullText.length() > 2600 ? fullText.substring(0, 2600) + "…(省略)" : fullText;
            String violationList = String.join("；", violations);
            String out = chatClient.prompt()
                    .system("你是一个医疗分诊结构化输出助手。你只输出一个 JSON 对象，禁止输出任何其他文字、解释、Markdown 代码块标记。")
                    .user("下面是助手上一轮的分诊建议全文。它的结构化输出违反了以下约束：\n【"
                            + violationList + "】\n"
                            + "请针对上述每条违规逐条修正，输出一个完整且合法的 JSON 对象，字段与类型必须为："
                            + "urgency ∈ [EMERGENCY,URGENT,ROUTINE]（字符串）；departments=字符串数组；"
                            + "hospitalLevel=字符串；confidence=0~1 的数字；followUp=字符串数组；disclaimer=字符串。"
                            + "只输出 JSON：\n\n" + text)
                    .call()
                    .content();
            if (out == null || out.isBlank()) {
                return null;
            }
            return parseTriage(out);
        } catch (Exception e) {
            log.warn("[agent] 定向修复调用失败: {}", e.getMessage());
            return null;
        }
    }

    /** 会话连续性路由决策 + 意图切换判定 */
    AgentType decideAgent(AgentType fresh, String current, String message) {
        if (current == null) {
            return fresh;
        }
        AgentType cur;
        try {
            cur = AgentType.valueOf(current);
        } catch (Exception e) {
            return fresh;
        }
        if (fresh == cur) {
            return fresh;
        }
        // 明确新指令（预约/报告关键词）→ 跟随新意图
        if (fresh == AgentType.SCHEDULER || fresh == AgentType.REPORT) {
            return fresh;
        }
        // 其余为「消息字面命中分诊」：
        // 会话正处预约/报告流程时，收到主诉/症状回答应留在原 Agent（如预约管家收集主诉“胸痛”），
        // 除非消息含明显分诊求助词（怎么办/要紧吗/严重吗…）。
        if (fresh == AgentType.TRIAGE && (cur == AgentType.SCHEDULER || cur == AgentType.REPORT)) {
            return hasTriageHelpIntent(message) ? AgentType.TRIAGE : cur;
        }
        return fresh;
    }

    private static final String[] TRIAGE_HELP_CUES = {
            "怎么办", "咋办", "要紧", "严重吗", "危险吗", "怎么回事", "是什么病", "什么病",
            "帮我看看", "分析", "疼得", "痛得", "需要注意", "去不去医院", "挂急诊", "会死"
    };

    boolean hasTriageHelpIntent(String message) {
        if (message == null) {
            return false;
        }
        for (String cue : TRIAGE_HELP_CUES) {
            if (message.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] CANCEL_CUES = {"取消", "不用了", "算了", "不约了", "不挂了", "暂停", "先不了"};

    boolean hasCancelIntent(String message) {
        if (message == null) {
            return false;
        }
        for (String cue : CANCEL_CUES) {
            if (message.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    /** 用户澄清“急诊已排除/看过急诊”后，预约流程可放行并清除急诊事实 */
    private static final String[] EMERGENCY_OVERRIDE_CUES = {
            "不是急症", "排除", "看过急诊", "急诊已排", "没事了", "虚惊", "检查过了", "已就医", "已经去急诊"
    };

    boolean hasEmergencyOverride(String message) {
        if (message == null) {
            return false;
        }
        for (String cue : EMERGENCY_OVERRIDE_CUES) {
            if (message.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    /** 既往/病史等语境词：避免把「以前胸痛过、想复查」这类预约误判为当前急症 */
    private static final String[] PAST_TENSE_CUES = {
            "以前", "之前", "曾", "有过", "病史", "既往", "去年", "上个月", "前阵", "复诊", "复查"
    };

    boolean hasPastTense(String message) {
        if (message == null) {
            return false;
        }
        for (String cue : PAST_TENSE_CUES) {
            if (message.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Agent 协作拦截：预约管家落单前发现本会话已有 EMERGENCY 分诊事实。
     * 确定性返回（不消耗 LLM），告知先急诊并引导澄清放行。
     */
    private SseEvent emergencyBlocksScheduler(SessionFactStore.EmergencyFact ef,
                                              AgentRequest areq, ChatRequest req, long start) {
        TriageResult tri = new TriageResult();
        tri.setUrgency("EMERGENCY");
        tri.setDepartments(ef.departments());
        tri.setHospitalLevel("立即前往就近急诊或拨打 120");
        tri.setConfidence(0.9);
        tri.setFollowUp(List.of(
                "立即前往就近急诊或拨打 120，不要等待门诊预约",
                "急诊处理后，如有复查需要可再回来预约对应专科门诊"));
        tri.setDisclaimer(ef.disclaimer());

        String depts = String.join("/", ef.departments() == null ? List.of("急诊科") : ef.departments());
        String answer = "系统已暂停本次门诊预约预占。本会话此前分诊结论为【需立即急诊】（建议科室：" + depts + "）。"
                + "\n\n为你的安全考虑，预约管家不能代你预占门诊——请先前往急诊评估。"
                + "\n\n如果你已完成急诊评估、确认并非急症，请回复「已排除 / 看过急诊」，即可恢复预约。"
                + "\n\n" + (ef.disclaimer() == null ? "" : ef.disclaimer());

        AgentResponse resp = new AgentResponse();
        resp.setFullText(answer);
        resp.setTriage(tri);
        resp.setTurnType("EMERGENCY");
        resp.setElapsedMs(System.currentTimeMillis() - start);
        completeTurn(areq, resp, req);
        return SseEvent.done(answer, tri, "预约管家", areq.getSources());
    }

    /**
     * T5 预算熔断后的确定性降级话术（0 LLM 消耗）：按 Agent 角色给出规则化安全指引，
     * 明确告知服务受限与替代路径，避免静默截断。
     */
    private String deterministicFallbackAnswer(AgentType type) {
        return switch (type) {
            case TRIAGE -> "（安全提示）本会话对话量较大，智能分诊已切换为简化模式。"
                    + "若症状持续或加重，请及时前往医院就诊；出现胸痛、呼吸困难、意识模糊等急重情况，"
                    + "请立即前往急诊或拨打 120。你也可以开启新会话继续使用完整分诊功能。";
            case SCHEDULER -> "（预约助手）本会话操作量较大，预约服务已切换为简化模式，暂时无法代你落单。"
                    + "请稍后开启新会话重试，或直接联系人工客服/医院挂号平台完成预约。";
            case REPORT -> "（报告助手）本会话对话量较大，报告解读已切换为简化模式。"
                    + "关键结论请以医院出具的正式报告与医生意见为准；如需完整解读，请开启新会话重试。";
            default -> "（系统提示）本会话资源已达上限，已切换为简化模式。请开启新会话继续使用完整功能。";
        };
    }

    /** after 链 + 记忆落库 + 决策日志（T3） */
    private void completeTurn(AgentRequest request, AgentResponse response, ChatRequest chat) {
        completeTurn(request, response, chat, null, null);
    }

    /** 带「输入特征/规则命中」的完整收尾：决策日志记录本轮最终裁决，供会话重放与审计复盘 */
    private void completeTurn(AgentRequest request, AgentResponse response, ChatRequest chat,
                              List<String> redFlags, List<String> ruleHits) {
        applyAfterAdvices(advices, request, response);
        if (response.isBlocked()) {
            return;   // 护栏 fail-closed 已接管：不写记忆、不记决策日志
        }
        // 护栏执行集中在 applyAfterAdvices（便于单测 fail-closed 语义）
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

    private String buildPrompt(String message, String history) {
        if (history == null || history.isBlank()) {
            return message;
        }
        return "历史对话：\n" + history + "\n\n当前用户：" + message;
    }

    /**
     * 用 BeanOutputConverter 绑定模型输出的 JSON（优先整段，其次 ```json 代码块，
     * 最后容错扫描全文首个 { 到末尾 }）；解析失败返回 null（不阻断对话）。
     */
    private TriageResult parseTriage(String full) {
        if (full == null || full.isBlank()) {
            return null;
        }
        try {
            String trimmed = full.trim();
            if (trimmed.startsWith("{")) {
                return triageConverter.convert(trimmed);
            }
            int s = trimmed.indexOf("```json");
            int start = s < 0 ? -1 : s + 7;
            if (start >= 0) {
                int e = trimmed.indexOf("```", start);
                String json = trimmed.substring(start, e < 0 ? trimmed.length() : e).trim();
                return triageConverter.convert(json);
            }
        } catch (Exception ex) {
            log.debug("BeanOutputConverter 解析失败，降级 lenient 解析: {}", ex.getMessage());
        }
        return lenientExtract(full);
    }

    private TriageResult lenientExtract(String full) {
        try {
            int s = full.indexOf("{");
            int e = full.lastIndexOf("}");
            if (s < 0 || e <= s) {
                return null;
            }
            com.fasterxml.jackson.databind.JsonNode root = lenientMapper.readTree(full.substring(s, e + 1));
            // 容错归一：模型偶发把 confidence 写为文本 / followUp 写为字符串
            if (root.has("confidence") && root.get("confidence").isTextual()) {
                String c = root.get("confidence").asText().trim().toLowerCase();
                double v = switch (c) {
                    case "low", "较低", "低" -> 0.3;
                    case "medium", "中等", "中" -> 0.6;
                    case "high", "较高", "高" -> 0.9;
                    default -> 0.5;
                };
                ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("confidence", v);
            }
            if (root.has("followUp") && root.get("followUp").isTextual()) {
                var arr = lenientMapper.createArrayNode();
                arr.add(root.get("followUp").asText());
                ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("followUp", arr);
            }
            return lenientMapper.treeToValue(root, TriageResult.class);
        } catch (Exception ex) {
            log.debug("lenient 分诊解析失败: {}", ex.getMessage());
            return null;
        }
    }
    /**
     * 依次执行输出护栏（{@code after}），**任何护栏抛异常都按失败关闭处理**。
     *
     * <p>为什么必须 fail-closed：{@code after} 承担的是"处方/剂量输出拦截"。
     * 它自己崩掉却继续放行，等于护栏被静默绕过，用户会收到未过滤、可能含具体剂量的回答。
     * 安全组件出错时唯一可接受的降级是"拦住"，不是"放过"。
     *
     * @return 是否被拦截（true 表示已置 blocked，调用方不应继续后续流程）
     */
    static boolean applyAfterAdvices(java.util.List<AgentAdvice> advices,
                                     com.health.web.assistant.model.AgentRequest request,
                                     com.health.web.assistant.model.AgentResponse response) {
        for (AgentAdvice advice : advices) {
            try {
                advice.after(request, response);
            } catch (RuntimeException e) {
                log.error("[agent] Advisor.after 异常，按失败关闭处理 {}: {}",
                        advice.getClass().getSimpleName(), e.getMessage(), e);
                response.setBlocked(true);
                response.setBlockReason("guardrail-failure:" + advice.getClass().getSimpleName());
                response.setFullText(BLOCKED_BY_GUARD_FAILURE);
                return true;
            }
        }
        return false;
    }

}
