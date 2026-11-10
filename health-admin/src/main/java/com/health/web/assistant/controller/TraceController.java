package com.health.web.assistant.controller;

import com.health.web.assistant.trace.TraceStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * T6 Trace 查询端点（只读）：按 sessionId 返回最近请求的完整决策链
 * （OTel 风格 span 树 JSON，已脱敏）。供前端「决策记录」面板与运维排查使用。
 */
@RestController
@RequestMapping("/api/v1/assistant")
public class TraceController {

    private final TraceStore traceStore;

    public TraceController(TraceStore traceStore) {
        this.traceStore = traceStore;
    }

    @GetMapping("/trace")
    public String trace(@RequestParam String sessionId,
                        @RequestParam(defaultValue = "5") int limit) {
        return traceStore.recentJson(sessionId, Math.min(Math.max(limit, 1), 20));
    }
}
