package com.chuanzhi.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.entity.InterventionPlan;
import com.chuanzhi.health.entity.InterventionTask;
import com.chuanzhi.health.enums.PlanStatus;
import com.chuanzhi.health.enums.TaskStatus;
import com.chuanzhi.health.mapper.InterventionPlanMapper;
import com.chuanzhi.health.mapper.InterventionTaskMapper;
import com.chuanzhi.health.service.InterventionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterventionServiceImpl implements InterventionService {

    private final InterventionPlanMapper planMapper;
    private final InterventionTaskMapper taskMapper;

    @Override
    public IPage<InterventionPlan> pagePlans(int page, int size, String status) {
        LambdaQueryWrapper<InterventionPlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(InterventionPlan::getStatus, PlanStatus.valueOf(status));
        }
        wrapper.orderByDesc(InterventionPlan::getCreatedAt);
        return planMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public InterventionPlan getPlanDetail(Long id) {
        return planMapper.selectById(id);
    }

    @Override
    @Transactional
    public InterventionPlan createPlan(InterventionPlan plan) {
        if (plan.getUserId() == null) throw new com.chuanzhi.health.common.BusinessException("用户ID不能为空");
        plan.setStatus(PlanStatus.ACTIVE);
        planMapper.insert(plan);
        return plan;
    }

    @Override
    public InterventionPlan updatePlan(InterventionPlan plan) {
        planMapper.updateById(plan);
        return planMapper.selectById(plan.getId());
    }

    @Override
    public List<InterventionTask> getTasksByPlanId(Long planId) {
        return taskMapper.selectList(
            new LambdaQueryWrapper<InterventionTask>()
                .eq(InterventionTask::getPlanId, planId)
                .orderByAsc(InterventionTask::getDueDate)
        );
    }

    @Override
    public InterventionTask updateTaskStatus(Long taskId, String status) {
        InterventionTask task = taskMapper.selectById(taskId);
        if (task != null) {
            TaskStatus taskStatus = TaskStatus.valueOf(status);
            task.setStatus(taskStatus);
            if (taskStatus == TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            }
            taskMapper.updateById(task);
        }
        return task;
    }

    @Override
    public InterventionTask addTask(InterventionTask task) {
        task.setStatus(TaskStatus.PENDING);
        taskMapper.insert(task);
        return task;
    }
}
