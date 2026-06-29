package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.InterventionPlan;
import com.chuanzhi.health.entity.InterventionTask;
import com.chuanzhi.health.service.InterventionService;
import com.chuanzhi.health.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "健康干预", description = "干预计划的制定、任务分配与进度跟踪")
@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService interventionService;
    private final SseService sseService;

    @Operation(summary = "分页查询干预计划")
    @GetMapping("/plans")
    public Result<PageResult<InterventionPlan>> listPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        IPage<InterventionPlan> result = interventionService.pagePlans(page, size, status);
        return Result.ok(PageResult.of(result));
    }

    @Operation(summary = "获取干预计划详情")
    @GetMapping("/plans/{id}")
    public Result<InterventionPlan> getPlan(@PathVariable Long id) {
        return Result.ok(interventionService.getPlanDetail(id));
    }

    @Operation(summary = "创建干预计划")
    @PostMapping("/plans")
    public Result<InterventionPlan> createPlan(@Valid @RequestBody InterventionPlan plan) {
        InterventionPlan created = interventionService.createPlan(plan);
        sseService.broadcast("interventions", "plan_created", created);
        sseService.broadcast("dashboard", "plan_created", created);
        return Result.ok(created);
    }

    @Operation(summary = "更新干预计划")
    @PutMapping("/plans/{id}")
    public Result<InterventionPlan> updatePlan(@PathVariable Long id, @Valid @RequestBody InterventionPlan plan) {
        plan.setId(id);
        InterventionPlan updated = interventionService.updatePlan(plan);
        sseService.broadcast("interventions", "plan_updated", updated);
        return Result.ok(updated);
    }

    @Operation(summary = "获取计划关联的任务列表")
    @GetMapping("/plans/{planId}/tasks")
    public Result<List<InterventionTask>> getTasks(@PathVariable Long planId) {
        return Result.ok(interventionService.getTasksByPlanId(planId));
    }

    @Operation(summary = "更新任务状态")
    @PutMapping("/tasks/{taskId}/status")
    public Result<InterventionTask> updateTaskStatus(
            @PathVariable Long taskId, @RequestParam String status) {
        InterventionTask task = interventionService.updateTaskStatus(taskId, status);
        sseService.broadcast("interventions", "task_status_changed", task);
        sseService.broadcast("dashboard", "task_status_changed", task);
        return Result.ok(task);
    }

    @Operation(summary = "添加干预任务")
    @PostMapping("/tasks")
    public Result<InterventionTask> addTask(@Valid @RequestBody InterventionTask task) {
        InterventionTask created = interventionService.addTask(task);
        sseService.broadcast("interventions", "task_added", created);
        return Result.ok(created);
    }
}
