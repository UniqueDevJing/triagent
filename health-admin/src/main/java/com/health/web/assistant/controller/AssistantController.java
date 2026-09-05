package com.health.web.assistant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.health.common.core.BaseController;
import com.health.web.assistant.agent.AssistantOrchestrator;
import com.health.web.assistant.model.ChatRequest;
import com.health.web.assistant.model.SseEvent;
import com.health.web.assistant.service.AgentMetricsService;
import com.health.web.assistant.service.PreOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 智能分诊助手端点（前端用）。返回 SSE 流，事件类型见 SseEvent。
 * 需要 Sa-Token 登录态。Phase 2 起登录用户 id 透传到编排层（幂等预订单归属）；
 * 预订单确认与运行指标走独立 JSON 端点。
 */
@Tag(name = "智能分诊助手")
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController extends BaseController {

    private final AssistantOrchestrator orchestrator;
    private final PreOrderService preOrderService;
    private final AgentMetricsService metrics;

    public AssistantController(AssistantOrchestrator orchestrator,
                               PreOrderService preOrderService,
                               AgentMetricsService metrics) {
        this.orchestrator = orchestrator;
        this.preOrderService = preOrderService;
        this.metrics = metrics;
    }

    @Operation(summary = "智能分诊助手（SSE 流式）")
    @SaCheckLogin
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(120_000L);
        orchestrator.chat(req, userId).subscribe(
                event -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(event.getType().name().toLowerCase())
                                .data(event.getData()));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("message", "服务异常")));
                    } catch (IOException ignored) {
                        // 连接已断开，忽略
                    }
                    emitter.complete();
                },
                emitter::complete
        );
        return emitter;
    }

    @Operation(summary = "Agent 运行指标（最近 500 轮聚合）")
    @SaCheckLogin
    @GetMapping("/metrics")
    public Object metrics() {
        return success(metrics.summary());
    }

    @Operation(summary = "确认 AI 预约预订单（两步确认第二步）")
    @SaCheckLogin
    @PostMapping("/preorders/{id}/confirm")
    public Object confirmPreOrder(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        try {
            return success(preOrderService.confirm(userId, id));
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }
}
