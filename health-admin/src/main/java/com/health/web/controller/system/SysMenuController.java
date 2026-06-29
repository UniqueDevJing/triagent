package com.health.web.controller.system;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysMenu;
import com.health.system.mapper.SysMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system/menu")
public class SysMenuController extends BaseController {
    private final SysMenuMapper menuMapper;
    public SysMenuController(SysMenuMapper menuMapper) { this.menuMapper = menuMapper; }

    @GetMapping
    @Log(title = "菜单查询")
    public AjaxResult list() {
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getOrderNum));
        return success(menus);
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(menuMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增菜单")
    public AjaxResult create(@RequestBody SysMenu menu) { menuMapper.insert(menu); return success(menu); }

    @PutMapping
    @Log(title = "修改菜单")
    public AjaxResult update(@RequestBody SysMenu menu) { menuMapper.updateById(menu); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除菜单")
    public AjaxResult delete(@PathVariable List<Long> ids) { menuMapper.deleteBatchIds(ids); return success(); }
}
