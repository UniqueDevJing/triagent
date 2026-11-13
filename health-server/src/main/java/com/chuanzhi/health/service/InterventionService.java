package com.chuanzhi.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.entity.InterventionPlan;
import com.chuanzhi.health.entity.InterventionTask;

import java.util.List;

public interface InterventionService {
    IPage<InterventionPlan> pagePlans(int page, int size, String status);
    InterventionPlan getPlanDetail(Long id);
    InterventionPlan createPlan(InterventionPlan plan);
    InterventionPlan updatePlan(InterventionPlan plan);
    List<InterventionTask> getTasksByPlanId(Long planId);
    InterventionTask updateTaskStatus(Long taskId, String status);
    InterventionTask addTask(InterventionTask task);
}
