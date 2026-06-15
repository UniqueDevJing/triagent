package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.InterventionPlan;
import com.chuanzhi.health.entity.InterventionTask;
import com.chuanzhi.health.service.InterventionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService interventionService;

    @GetMapping("/plans")
    public Result<PageResult<InterventionPlan>> listPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        IPage<InterventionPlan> result = interventionService.pagePlans(page, size, status);
        return Result.ok(PageResult.of(result));
    }

    @GetMapping("/plans/{id}")
    public Result<InterventionPlan> getPlan(@PathVariable Long id) {
        return Result.ok(interventionService.getPlanDetail(id));
    }

    @PostMapping("/plans")
    public Result<InterventionPlan> createPlan(@Valid @RequestBody InterventionPlan plan) {
        return Result.ok(interventionService.createPlan(plan));
    }

    @PutMapping("/plans/{id}")
    public Result<InterventionPlan> updatePlan(@PathVariable Long id, @Valid @RequestBody InterventionPlan plan) {
        plan.setId(id);
        return Result.ok(interventionService.updatePlan(plan));
    }

    @GetMapping("/plans/{planId}/tasks")
    public Result<List<InterventionTask>> getTasks(@PathVariable Long planId) {
        return Result.ok(interventionService.getTasksByPlanId(planId));
    }

    @PutMapping("/tasks/{taskId}/status")
    public Result<InterventionTask> updateTaskStatus(
            @PathVariable Long taskId, @RequestParam String status) {
        return Result.ok(interventionService.updateTaskStatus(taskId, status));
    }

    @PostMapping("/tasks")
    public Result<InterventionTask> addTask(@Valid @RequestBody InterventionTask task) {
        return Result.ok(interventionService.addTask(task));
    }
}
