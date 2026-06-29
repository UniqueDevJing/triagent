package com.health.web.controller.system;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysRole;
import com.health.system.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system/role")
public class SysRoleController extends BaseController {
    private final SysRoleMapper roleMapper;
    public SysRoleController(SysRoleMapper roleMapper) { this.roleMapper = roleMapper; }

    @GetMapping
    @Log(title = "角色查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<SysRole>().orderByDesc(SysRole::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(roleMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增角色")
    public AjaxResult create(@RequestBody SysRole role) { roleMapper.insert(role); return success(role); }

    @PutMapping
    @Log(title = "修改角色")
    public AjaxResult update(@RequestBody SysRole role) { roleMapper.updateById(role); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除角色")
    public AjaxResult delete(@PathVariable List<Long> ids) { roleMapper.deleteBatchIds(ids); return success(); }
}
