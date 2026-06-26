package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.*;
import com.chuanzhi.health.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberMapper memberMapper;
    private final PhysicalExamPlanMapper planMapper;

    public PageResult<Member> list(int page, int size, String keyword) {
        Page<Member> pg = new Page<>(page, size);
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            qw.like(Member::getName, keyword).or().like(Member::getPhone, keyword);
        qw.orderByDesc(Member::getCreatedAt);
        Page<Member> result = memberMapper.selectPage(pg, qw);
        return PageResult.of(result);
    }

    public Member getById(Long id) { return memberMapper.selectById(id); }
    public Member create(Member m) { memberMapper.insert(m); return m; }
    public Member update(Member m) { memberMapper.updateById(m); return memberMapper.selectById(m.getId()); }
    public void delete(Long id) { memberMapper.deleteById(id); }

    // 体检计划
    public PageResult<PhysicalExamPlan> listPlans(Long memberId, int page, int size) {
        Page<PhysicalExamPlan> pg = new Page<>(page, size);
        LambdaQueryWrapper<PhysicalExamPlan> qw = new LambdaQueryWrapper<>();
        qw.eq(PhysicalExamPlan::getMemberId, memberId).orderByDesc(PhysicalExamPlan::getCreatedAt);
        Page<PhysicalExamPlan> result = planMapper.selectPage(pg, qw);
        return PageResult.of(result);
    }
    public PhysicalExamPlan createPlan(PhysicalExamPlan p) { planMapper.insert(p); return p; }
    public PhysicalExamPlan updatePlan(PhysicalExamPlan p) { planMapper.updateById(p); return planMapper.selectById(p.getId()); }
    public void deletePlan(Long id) { planMapper.deleteById(id); }
}
