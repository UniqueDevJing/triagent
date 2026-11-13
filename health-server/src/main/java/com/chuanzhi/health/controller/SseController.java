package com.chuanzhi.health.controller;

import com.chuanzhi.health.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "实时推送", description = "SSE 实时数据推送端点")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @Operation(summary = "订阅指定主题的实时事件")
    @GetMapping(value = "/subscribe/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String topic) {
        return sseService.subscribe(topic);
    }
}
