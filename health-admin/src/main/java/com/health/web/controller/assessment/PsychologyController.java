package com.health.web.controller.assessment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.PsychologyAssessment;
import com.health.system.mapper.PsychologyAssessmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessment/psychology")
public class PsychologyController extends BaseController {
    private final PsychologyAssessmentMapper psychologyAssessmentMapper;
    public PsychologyController(PsychologyAssessmentMapper psychologyAssessmentMapper) { this.psychologyAssessmentMapper = psychologyAssessmentMapper; }

    @GetMapping
    @Log(title = "心理评估查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<PsychologyAssessment> p = psychologyAssessmentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<PsychologyAssessment>().orderByDesc(PsychologyAssessment::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(psychologyAssessmentMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增心理评估")
    public AjaxResult create(@RequestBody PsychologyAssessment assessment) { psychologyAssessmentMapper.insert(assessment); return success(assessment); }

    @PutMapping("/{id}")
    @Log(title = "修改心理评估")
    public AjaxResult update(@RequestBody PsychologyAssessment assessment) { psychologyAssessmentMapper.updateById(assessment); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除心理评估")
    public AjaxResult delete(@PathVariable List<Long> ids) { psychologyAssessmentMapper.deleteBatchIds(ids); return success(); }
}
