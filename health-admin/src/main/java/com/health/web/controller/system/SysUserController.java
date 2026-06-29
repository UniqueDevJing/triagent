package com.health.web.controller.system;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysUser;
import com.health.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system/user")
public class SysUserController extends BaseController {
    private final SysUserMapper userMapper;
    public SysUserController(SysUserMapper userMapper) { this.userMapper = userMapper; }

    @GetMapping
    @Log(title = "用户查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(userMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增用户")
    public AjaxResult create(@RequestBody SysUser user) { userMapper.insert(user); return success(user); }

    @PutMapping
    @Log(title = "修改用户")
    public AjaxResult update(@RequestBody SysUser user) { userMapper.updateById(user); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除用户")
    public AjaxResult delete(@PathVariable List<Long> ids) { userMapper.deleteBatchIds(ids); return success(); }
}
