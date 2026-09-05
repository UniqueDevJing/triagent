package com.health.framework.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.SysRole;
import com.health.system.domain.SysRoleMenu;
import com.health.system.domain.SysUserRole;
import com.health.system.domain.SysMenu;
import com.health.system.mapper.SysMenuMapper;
import com.health.system.mapper.SysRoleMapper;
import com.health.system.mapper.SysRoleMenuMapper;
import com.health.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public StpInterfaceImpl(SysRoleMapper roleMapper,
                            SysUserRoleMapper userRoleMapper,
                            SysRoleMenuMapper roleMenuMapper,
                            SysMenuMapper menuMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return new ArrayList<>();

        List<Long> menuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        if (menuIds.isEmpty()) return new ArrayList<>();

        return menuMapper.selectBatchIds(menuIds).stream()
                .map(SysMenu::getPerms)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return new ArrayList<>();
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleKey)
                .collect(Collectors.toList());
    }
}
