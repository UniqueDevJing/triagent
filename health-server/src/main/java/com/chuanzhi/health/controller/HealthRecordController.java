package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.HealthRecord;
import com.chuanzhi.health.service.HealthRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/health-records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService recordService;

    // SSE 客户端列表（实时推送）
    private static final List<SseEmitter> sseClients = new CopyOnWriteArrayList<>();

    @GetMapping
    public Result<PageResult<HealthRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        IPage<HealthRecord> result = recordService.pageRecords(page, size, userId);
        return Result.ok(PageResult.of(result));
    }

    @GetMapping("/user/{userId}")
    public Result<List<HealthRecord>> getByUser(@PathVariable Long userId) {
        return Result.ok(recordService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/latest")
    public Result<Map<String, Object>> getLatestMetrics(@PathVariable Long userId) {
        return Result.ok(recordService.getLatestMetrics(userId));
    }

    @GetMapping("/{id}")
    public Result<HealthRecord> getById(@PathVariable Long id) {
        return Result.ok(recordService.getById(id));
    }

    @PostMapping
    public Result<HealthRecord> create(@Valid @RequestBody HealthRecord record) {
        HealthRecord created = recordService.create(record);
        broadcastEvent("health_record_created", created);
        return Result.ok(created);
    }

    @PutMapping("/{id}")
    public Result<HealthRecord> update(@PathVariable Long id, @Valid @RequestBody HealthRecord record) {
        record.setId(id);
        return Result.ok(recordService.update(record));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        recordService.delete(id);
        return Result.ok();
    }

    /**
     * SSE 实时推送端点 — Dashboard 订阅
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // 无超时
        sseClients.add(emitter);
        emitter.onCompletion(() -> sseClients.remove(emitter));
        emitter.onTimeout(() -> sseClients.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"message\":\"实时连接已建立\"}"));
        } catch (IOException e) {
            sseClients.remove(emitter);
        }
        return emitter;
    }

    public static void broadcastEvent(String event, Object data) {
        for (SseEmitter emitter : sseClients) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException e) {
                sseClients.remove(emitter);
            }
        }
    }
}
