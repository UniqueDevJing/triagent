package com.health.web.controller.member;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.ExamPlan;
import com.health.system.mapper.ExamPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/member/exam-plan")
public class ExamPlanController extends BaseController {
    private final ExamPlanMapper examPlanMapper;
    public ExamPlanController(ExamPlanMapper examPlanMapper) { this.examPlanMapper = examPlanMapper; }

    @GetMapping
    @Log(title = "体检计划查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<ExamPlan> p = examPlanMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<ExamPlan>().orderByDesc(ExamPlan::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(examPlanMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增体检计划")
    public AjaxResult create(@RequestBody ExamPlan examPlan) { examPlanMapper.insert(examPlan); return success(examPlan); }

    @PutMapping
    @Log(title = "修改体检计划")
    public AjaxResult update(@RequestBody ExamPlan examPlan) { examPlanMapper.updateById(examPlan); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除体检计划")
    public AjaxResult delete(@PathVariable List<Long> ids) { examPlanMapper.deleteBatchIds(ids); return success(); }
}
