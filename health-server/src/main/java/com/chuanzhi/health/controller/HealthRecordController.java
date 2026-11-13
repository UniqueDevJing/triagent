package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.HealthRecord;
import com.chuanzhi.health.service.HealthRecordService;
import com.chuanzhi.health.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Tag(name = "健康档案", description = "健康体检记录的增删改查与实时推送")
@RestController
@RequestMapping("/api/health-records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService recordService;
    private final SseService sseService;

    @Operation(summary = "分页查询健康档案")
    @GetMapping
    public Result<PageResult<HealthRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        IPage<HealthRecord> result = recordService.pageRecords(page, size, userId);
        return Result.ok(PageResult.of(result));
    }

    @Operation(summary = "根据用户ID查询健康档案")
    @GetMapping("/user/{userId}")
    public Result<List<HealthRecord>> getByUser(@PathVariable Long userId) {
        return Result.ok(recordService.getByUserId(userId));
    }

    @Operation(summary = "获取用户最新健康指标")
    @GetMapping("/user/{userId}/latest")
    public Result<Map<String, Object>> getLatestMetrics(@PathVariable Long userId) {
        return Result.ok(recordService.getLatestMetrics(userId));
    }

    @Operation(summary = "根据ID查询健康档案")
    @GetMapping("/{id}")
    public Result<HealthRecord> getById(@PathVariable Long id) {
        return Result.ok(recordService.getById(id));
    }

    @Operation(summary = "创建健康档案")
    @PostMapping
    public Result<HealthRecord> create(@Valid @RequestBody HealthRecord record) {
        HealthRecord created = recordService.create(record);
        sseService.broadcast("health_records", "health_record_created", created);
        sseService.broadcast("dashboard", "health_record_created", created);
        return Result.ok(created);
    }

    @Operation(summary = "更新健康档案")
    @PutMapping("/{id}")
    public Result<HealthRecord> update(@PathVariable Long id, @Valid @RequestBody HealthRecord record) {
        record.setId(id);
        HealthRecord updated = recordService.update(record);
        sseService.broadcast("health_records", "health_record_updated", updated);
        sseService.broadcast("dashboard", "health_record_updated", updated);
        return Result.ok(updated);
    }

    @Operation(summary = "删除健康档案")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        recordService.delete(id);
        sseService.broadcast("health_records", "health_record_deleted", Map.of("id", id));
        sseService.broadcast("dashboard", "health_record_deleted", Map.of("id", id));
        return Result.ok();
    }

    @Operation(summary = "SSE实时推送（兼容旧端点）")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseService.subscribe("dashboard");
    }
}
