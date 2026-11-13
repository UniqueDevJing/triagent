package com.health.web.controller.intervention;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.InterventionPlan;
import com.health.system.domain.Member;
import com.health.system.mapper.InterventionPlanMapper;
import com.health.system.mapper.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/intervention/plan")
public class InterventionController extends BaseController {
    private final InterventionPlanMapper interventionPlanMapper;
    private final MemberMapper memberMapper;
    public InterventionController(InterventionPlanMapper interventionPlanMapper,
                                  MemberMapper memberMapper) {
        this.interventionPlanMapper = interventionPlanMapper;
        this.memberMapper = memberMapper;
    }

    @GetMapping
    @Log(title = "干预计划查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<InterventionPlan> p = interventionPlanMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<InterventionPlan>().orderByDesc(InterventionPlan::getCreateTime));
        fillMemberNames(p.getRecords());
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        InterventionPlan plan = interventionPlanMapper.selectById(id);
        if (plan != null) fillMemberNames(List.of(plan));
        return success(plan);
    }

    @PostMapping
    @Log(title = "新增干预计划")
    public AjaxResult create(@RequestBody InterventionPlan plan) { interventionPlanMapper.insert(plan); return success(plan); }

    @PutMapping("/{id}")
    @Log(title = "修改干预计划")
    public AjaxResult update(@RequestBody InterventionPlan plan) { interventionPlanMapper.updateById(plan); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除干预计划")
    public AjaxResult delete(@PathVariable List<Long> ids) { interventionPlanMapper.deleteBatchIds(ids); return success(); }

    private void fillMemberNames(List<InterventionPlan> plans) {
        if (plans.isEmpty()) return;
        List<Long> memberIds = plans.stream().map(InterventionPlan::getMemberId).filter(id -> id != null).distinct().toList();
        if (memberIds.isEmpty()) return;
        Map<Long, String> nameMap = memberMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m.getName() != null ? m.getName() : "(未知会员)"));
        plans.forEach(p -> p.setMemberName(nameMap.getOrDefault(p.getMemberId(), "(未知会员)")));
    }
}
