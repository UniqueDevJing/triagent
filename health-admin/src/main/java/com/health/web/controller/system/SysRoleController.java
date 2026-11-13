package com.health.web.controller.system;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysRole;
import com.health.system.domain.SysRoleMenu;
import com.health.system.mapper.SysRoleMapper;
import com.health.system.mapper.SysRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system/role")
public class SysRoleController extends BaseController {
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SysRoleController(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

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

    @PutMapping("/{id}")
    @Log(title = "修改角色")
    public AjaxResult update(@RequestBody SysRole role) { roleMapper.updateById(role); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除角色")
    public AjaxResult delete(@PathVariable List<Long> ids) { roleMapper.deleteBatchIds(ids); return success(); }

    /** 查询角色已授权菜单 ID 列表 */
    @GetMapping("/{id}/menus")
    public AjaxResult getMenus(@PathVariable Long id) {
        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        return success(menuIds);
    }

    /** 覆盖式授权：body { "menuIds": [1,2,3] }。先清后插，事务保证原子。 */
    @PutMapping("/{id}/menus")
    @Log(title = "角色菜单授权")
    @Transactional
    public AjaxResult updateMenus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (roleMapper.selectById(id) == null) {
            return error("角色不存在");
        }
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.getOrDefault("menuIds", new ArrayList<>());
        List<Long> menuIds = raw.stream()
                .filter(o -> o != null && !o.toString().isBlank())
                .map(o -> Long.valueOf(o.toString()))
                .distinct()
                .collect(Collectors.toList());

        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : menuIds) {
            roleMenuMapper.insert(new SysRoleMenu(id, menuId));
        }
        return success();
    }
}
