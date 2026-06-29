package com.health.web.controller.assessment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.AssessmentRecord;
import com.health.system.mapper.AssessmentRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessment/record")
public class AssessmentController extends BaseController {
    private final AssessmentRecordMapper assessmentRecordMapper;
    public AssessmentController(AssessmentRecordMapper assessmentRecordMapper) { this.assessmentRecordMapper = assessmentRecordMapper; }

    @GetMapping
    @Log(title = "评估记录查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<AssessmentRecord> p = assessmentRecordMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<AssessmentRecord>().orderByDesc(AssessmentRecord::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(assessmentRecordMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增评估记录")
    public AjaxResult create(@RequestBody AssessmentRecord record) { assessmentRecordMapper.insert(record); return success(record); }

    @PutMapping
    @Log(title = "修改评估记录")
    public AjaxResult update(@RequestBody AssessmentRecord record) { assessmentRecordMapper.updateById(record); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除评估记录")
    public AjaxResult delete(@PathVariable List<Long> ids) { assessmentRecordMapper.deleteBatchIds(ids); return success(); }
}
