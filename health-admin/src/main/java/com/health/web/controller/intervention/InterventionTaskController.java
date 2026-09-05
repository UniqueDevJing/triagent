package com.health.web.controller.intervention;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.InterventionTask;
import com.health.system.mapper.InterventionTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/intervention/task")
public class InterventionTaskController extends BaseController {

    private final InterventionTaskMapper taskMapper;

    public InterventionTaskController(InterventionTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @GetMapping
    public AjaxResult list(@RequestParam Long planId) {
        List<InterventionTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<InterventionTask>()
                        .eq(InterventionTask::getPlanId, planId)
                        .orderByAsc(InterventionTask::getCreateTime));
        return success(tasks);
    }

    @PostMapping
    @Log(title = "新增干预任务")
    public AjaxResult create(@RequestBody InterventionTask task) {
        taskMapper.insert(task);
        return success(task);
    }

    @PutMapping("/{id}")
    @Log(title = "修改干预任务")
    public AjaxResult update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        InterventionTask task = taskMapper.selectById(id);
        if (task == null) return error("任务不存在");

        if (body.containsKey("status")) {
            task.setStatus((String) body.get("status"));
            if ("COMPLETED".equals(body.get("status"))) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }
        if (body.containsKey("title")) task.setTitle((String) body.get("title"));
        if (body.containsKey("description")) task.setDescription((String) body.get("description"));

        taskMapper.updateById(task);
        return success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "删除干预任务")
    public AjaxResult delete(@PathVariable Long id) {
        taskMapper.deleteById(id);
        return success();
    }
}
