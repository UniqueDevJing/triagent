package com.health.web.assistant.agent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.health.web.assistant.tool.ToolEventPublisher;
import com.health.web.assistant.tool.TriageTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

    /** 结构化分诊输出绑定器（BeanOutputConverter 负责 JSON schema 解析/校验） */
    private final BeanOutputConverter<TriageResult> triageConverter = new BeanOutputConverter<>(TriageResult.class);
    private final ObjectMapper lenientMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AssistantOrchestrator(ChatClient chatClient,
                                 AgentRegistry agentRegistry,
                                 SymptomStateMachine stateMachine,
                                 TriageTools triageTools,
                                 AssessmentRecordMapper assessmentRecordMapper,
                                 PreOrderService preOrderService,
                                 SessionMemoryStore memoryStore,
                                 AgentSessionState sessionState,
                                 SessionFactStore sessionFactStore,
                                 List<AgentAdvice> advices) {
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
        AgentType decided = decideAgent(fresh, sessionState.get(req.getSessionId()), areq.getMessage());
        boolean supervisorPlanned = false;

        // —— Supervisor 规划（组合请求，医疗安全优先）——
        // 同一句话里既预约又报告当前急症红旗（如「胸痛…帮我约…」）→ 不进入预约分支，
        // 规划为「应急分诊优先」：把本轮到 TRIAGE，由红旗状态机确定性输出 EMERGENCY（0 LLM 调用）。
        // 既往/复诊语境（以前胸痛过、复查）不会触发，避免误伤。
        if (decided == AgentType.SCHEDULER
                && !hasEmergencyOverride(areq.getMessage())
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

        // Agent 协作（医疗安全）：会话已有 EMERGENCY 事实 → 预约落单前确定性拦截；
        // 用户澄清「已排除/看过急诊」→ 清除事实放行。
        if (agent.getType() == AgentType.SCHEDULER) {
            boolean override = hasEmergencyOverride(areq.getMessage());
            SessionFactStore.EmergencyFact ef = sessionFactStore.getEmergency(req.getSessionId());
            if (override) {
                if (ef != null) {
                    sessionFactStore.clearEmergency(req.getSessionId());
                    log.info("[agent] 会话={} 用户澄清急诊已排除，恢复预约流程", req.getSessionId());
                }
            } else if (ef != null) {
                log.info("[agent] 会话={} 协作拦截：预约请求被本会话 EMERGENCY 分诊事实拦截", req.getSessionId());
                return Flux.concat(
                        Flux.just(SseEvent.plan("SAFETY_BLOCK", "检测到本会话 EMERGENCY 分诊事实，预约已拦截")),
                        Flux.just(emergencyBlocksScheduler(ef, areq, req, start)));
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
                completeTurn(capturedReq, resp, req);
                if (supervisorPlanned) {
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
                completeTurn(capturedReq, resp, req);
                return Flux.just(
                        SseEvent.clarify(question, d.missingFields()),
                        SseEvent.done(question, null, agent.getDisplayName(), areq.getSources()));
            }
        }

        // —— LLM 路径（PROCEED / REPORT / SCHEDULER）——
        StringBuilder acc = new StringBuilder();
        Sinks.Many<SseEvent> toolSink = Sinks.many().multicast().onBackpressureBuffer();
        ToolEventPublisher toolEvents = new ToolEventPublisher(toolSink);
        AgentToolkit toolkit = new AgentToolkit(userId, toolEvents, triageTools, assessmentRecordMapper, preOrderService);
        ToolCallback[] callbacks = Arrays.stream(ToolCallbacks.from(toolkit))
                .filter(cb -> agent.getAllowedTools().contains(cb.getToolDefinition().name()))
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
                .concatWith(Mono.defer(() -> {
                    String full = acc.toString();
                    AgentResponse resp = new AgentResponse();
                    resp.setFullText(full);
                    // G8 重试机制：TRIAGE 轮结构化 JSON 解析失败 → 追加一次「纯 JSON 提炼」调用
                    TriageResult tri = agent.getType() == AgentType.TRIAGE ? parseTriage(full) : null;
                    if (tri == null && agent.getType() == AgentType.TRIAGE) {
                        tri = retryStructuredJson(full);
                        if (tri != null) {
                            log.info("[agent] 会话={} JSON 重试成功（首轮解析失败）", req.getSessionId());
                        }
                    }
                    resp.setTriage(tri);
                    if (tri != null && "EMERGENCY".equalsIgnoreCase(tri.getUrgency())) {
                        sessionFactStore.recordEmergency(req.getSessionId(), tri.getUrgency(),
                                tri.getDepartments(), tri.getDisclaimer());
                    }
                    resp.setTurnType("LLM");
                    resp.setToolCallCount(toolEvents.getCallCount());
                    resp.setToolResultCount(toolEvents.getResultCount());
                    resp.setElapsedMs(System.currentTimeMillis() - start);
                    completeTurn(capturedReq, resp, req);
                    return Mono.just(SseEvent.done(full, resp.getTriage(),
                            agent.getDisplayName(), capturedReq.getSources()));
                }))
                .onErrorResume(e -> {
                    log.warn("[agent] 会话={} 推理失败: {}", req.getSessionId(), e.getMessage());
                    return Flux.just(SseEvent.error("服务暂时不可用，请稍后重试"));
                });
    }

    /**
     * G8 重试：用一次轻量、纯 JSON 的补充调用，把首轮含正文/残缺 JSON 的回答提炼成合法 TriageResult。
     * 仅在首次结构化解析失败时触发（TRIAGE 轮），成本低；二次仍失败则返回 null。
     */
    private TriageResult retryStructuredJson(String fullText) {
        if (fullText == null || fullText.isBlank()) {
            return null;
        }
        try {
            String text = fullText.length() > 2600 ? fullText.substring(0, 2600) + "…(省略)" : fullText;
            String out = chatClient.prompt()
                    .system("你是一个医疗分诊结构化输出助手。你只输出一个 JSON 对象，禁止输出任何其他文字、解释、Markdown 代码块标记。")
                    .user("下面是助手上一轮的分诊建议全文（可能混有正文、追问或不完整 JSON）。"
                            + "请提炼并修正为**一个完整且合法**的 JSON 对象，字段与类型必须为："
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
            log.warn("[agent] JSON 重试调用失败: {}", e.getMessage());
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

    /** after 链 + 记忆落库 */
    private void completeTurn(AgentRequest request, AgentResponse response, ChatRequest chat) {
        for (AgentAdvice a : advices) {
            try {
                a.after(request, response);
            } catch (Exception e) {
                log.warn("[agent] Advisor.after 异常 {}: {}", a.getClass().getSimpleName(), e.getMessage());
            }
        }
        memoryStore.append(chat.getSessionId(), chat.getMessage(), response.getFullText());
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
}
