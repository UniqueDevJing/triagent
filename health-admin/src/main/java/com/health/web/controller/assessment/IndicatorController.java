package com.health.web.controller.assessment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.AssessmentIndicator;
import com.health.system.mapper.AssessmentIndicatorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessment/indicator")
public class IndicatorController extends BaseController {
    private final AssessmentIndicatorMapper assessmentIndicatorMapper;
    public IndicatorController(AssessmentIndicatorMapper assessmentIndicatorMapper) { this.assessmentIndicatorMapper = assessmentIndicatorMapper; }

    @GetMapping
    @Log(title = "评估指标查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<AssessmentIndicator> p = assessmentIndicatorMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<AssessmentIndicator>().orderByDesc(AssessmentIndicator::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(assessmentIndicatorMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增评估指标")
    public AjaxResult create(@RequestBody AssessmentIndicator indicator) { assessmentIndicatorMapper.insert(indicator); return success(indicator); }

    @PutMapping("/{id}")
    @Log(title = "修改评估指标")
    public AjaxResult update(@RequestBody AssessmentIndicator indicator) { assessmentIndicatorMapper.updateById(indicator); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除评估指标")
    public AjaxResult delete(@PathVariable List<Long> ids) { assessmentIndicatorMapper.deleteBatchIds(ids); return success(); }
}
