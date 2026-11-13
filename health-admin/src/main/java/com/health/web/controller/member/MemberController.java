package com.health.web.controller.member;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.Member;
import com.health.system.mapper.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/member")
public class MemberController extends BaseController {
    private final MemberMapper memberMapper;
    public MemberController(MemberMapper memberMapper) { this.memberMapper = memberMapper; }

    @GetMapping
    @Log(title = "会员查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String name) {
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<Member>()
                .like(name != null && !name.isEmpty(), Member::getName, name)
                .orderByDesc(Member::getCreateTime);
        Page<Member> p = memberMapper.selectPage(new Page<>(page, pageSize), qw);
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(memberMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增会员")
    public AjaxResult create(@RequestBody Member member) { memberMapper.insert(member); return success(member); }

    @PutMapping("/{id}")
    @Log(title = "修改会员")
    public AjaxResult update(@RequestBody Member member) { memberMapper.updateById(member); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除会员")
    public AjaxResult delete(@PathVariable List<Long> ids) { memberMapper.deleteBatchIds(ids); return success(); }
}
