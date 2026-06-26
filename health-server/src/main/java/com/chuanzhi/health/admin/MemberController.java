package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.*;
import com.chuanzhi.health.service.admin.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(memberService.list(page, size, keyword));
    }
    @GetMapping("/{id}")
    public Result<Member> get(@PathVariable Long id) { return Result.ok(memberService.getById(id)); }
    @PostMapping
    public Result<Member> create(@RequestBody Member m) { return Result.ok(memberService.create(m)); }
    @PutMapping("/{id}")
    public Result<Member> update(@PathVariable Long id, @RequestBody Member m) { m.setId(id); return Result.ok(memberService.update(m)); }
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) { memberService.delete(id); return Result.ok(null); }

    @GetMapping("/{memberId}/exam-plans")
    public Result<?> listPlans(@PathVariable Long memberId, @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(memberService.listPlans(memberId, page, size));
    }
    @PostMapping("/{memberId}/exam-plans")
    public Result<PhysicalExamPlan> createPlan(@PathVariable Long memberId, @RequestBody PhysicalExamPlan plan) {
        plan.setMemberId(memberId); return Result.ok(memberService.createPlan(plan));
    }
    @PutMapping("/exam-plans/{planId}")
    public Result<PhysicalExamPlan> updatePlan(@PathVariable Long planId, @RequestBody PhysicalExamPlan plan) {
        plan.setId(planId); return Result.ok(memberService.updatePlan(plan));
    }
    @DeleteMapping("/exam-plans/{planId}")
    public Result<?> deletePlan(@PathVariable Long planId) { memberService.deletePlan(planId); return Result.ok(null); }
}
