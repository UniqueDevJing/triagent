package com.health.web.controller.assessment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.AssessmentRecord;
import com.health.system.domain.Member;
import com.health.system.mapper.AssessmentRecordMapper;
import com.health.system.mapper.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assessment/record")
public class AssessmentController extends BaseController {
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final MemberMapper memberMapper;
    public AssessmentController(AssessmentRecordMapper assessmentRecordMapper,
                                MemberMapper memberMapper) {
        this.assessmentRecordMapper = assessmentRecordMapper;
        this.memberMapper = memberMapper;
    }

    @GetMapping
    @Log(title = "评估记录查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<AssessmentRecord> p = assessmentRecordMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<AssessmentRecord>().orderByDesc(AssessmentRecord::getCreateTime));
        fillMemberNames(p.getRecords());
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        AssessmentRecord r = assessmentRecordMapper.selectById(id);
        if (r != null) fillMemberNames(List.of(r));
        return success(r);
    }

    @PostMapping
    @Log(title = "新增评估记录")
    public AjaxResult create(@RequestBody AssessmentRecord record) { assessmentRecordMapper.insert(record); return success(record); }

    @PutMapping("/{id}")
    @Log(title = "修改评估记录")
    public AjaxResult update(@RequestBody AssessmentRecord record) { assessmentRecordMapper.updateById(record); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除评估记录")
    public AjaxResult delete(@PathVariable List<Long> ids) { assessmentRecordMapper.deleteBatchIds(ids); return success(); }

    private void fillMemberNames(List<AssessmentRecord> records) {
        if (records.isEmpty()) return;
        List<Long> memberIds = records.stream().map(AssessmentRecord::getMemberId).filter(id -> id != null).distinct().toList();
        if (memberIds.isEmpty()) return;
        Map<Long, String> nameMap = memberMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m.getName() != null ? m.getName() : "(未知会员)"));
        records.forEach(r -> r.setMemberName(nameMap.getOrDefault(r.getMemberId(), "(未知会员)")));
    }
}
