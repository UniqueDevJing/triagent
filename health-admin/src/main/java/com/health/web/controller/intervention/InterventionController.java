package com.health.web.controller.intervention;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.InterventionPlan;
import com.health.system.mapper.InterventionPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/intervention/plan")
public class InterventionController extends BaseController {
    private final InterventionPlanMapper interventionPlanMapper;
    public InterventionController(InterventionPlanMapper interventionPlanMapper) { this.interventionPlanMapper = interventionPlanMapper; }

    @GetMapping
    @Log(title = "干预计划查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<InterventionPlan> p = interventionPlanMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<InterventionPlan>().orderByDesc(InterventionPlan::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(interventionPlanMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增干预计划")
    public AjaxResult create(@RequestBody InterventionPlan plan) { interventionPlanMapper.insert(plan); return success(plan); }

    @PutMapping
    @Log(title = "修改干预计划")
    public AjaxResult update(@RequestBody InterventionPlan plan) { interventionPlanMapper.updateById(plan); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除干预计划")
    public AjaxResult delete(@PathVariable List<Long> ids) { interventionPlanMapper.deleteBatchIds(ids); return success(); }
}
