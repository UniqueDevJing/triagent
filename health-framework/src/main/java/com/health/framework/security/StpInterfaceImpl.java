package com.health.framework.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.SysRole;
import com.health.system.mapper.SysRoleMapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysRoleMapper roleMapper;

    public StpInterfaceImpl(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<>());
        return roles.stream().map(SysRole::getRoleKey).toList();
    }
}
