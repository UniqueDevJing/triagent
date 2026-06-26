package com.chuanzhi.health.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.*;
import com.chuanzhi.health.mapper.*;
import com.chuanzhi.health.service.admin.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<Department> listDepartments(int page, int size) {
        Page<Department> pg = new Page<>(page, size);
        Page<Department> result = departmentMapper.selectPage(pg,
            new LambdaQueryWrapper<Department>().orderByAsc(Department::getSortOrder));
        return PageResult.of(result);
    }

    @Override
    public Department getDepartment(Long id) {
        return departmentMapper.selectById(id);
    }

    @Override
    public Department createDepartment(Department dept) {
        departmentMapper.insert(dept);
        return dept;
    }

    @Override
    public Department updateDepartment(Department dept) {
        departmentMapper.updateById(dept);
        return departmentMapper.selectById(dept.getId());
    }

    @Override
    public void deleteDepartment(Long id) {
        departmentMapper.deleteById(id);
    }

    @Override
    public PageResult<Role> listRoles(int page, int size) {
        Page<Role> pg = new Page<>(page, size);
        Page<Role> result = roleMapper.selectPage(pg, new LambdaQueryWrapper<>());
        return PageResult.of(result);
    }

    @Override
    public Role getRole(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public Role updateRoleMenus(Long id, String menus) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new com.chuanzhi.health.common.BusinessException("角色不存在");
        role.setMenus(menus);
        roleMapper.updateById(role);
        return role;
    }

    @Override
    public List<Menu> getMenuTree() {
        List<Menu> all = menuMapper.selectList(
            new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSortOrder));
        List<Menu> roots = all.stream().filter(m -> m.getParentId() == 0).collect(Collectors.toList());
        for (Menu root : roots) {
            root.setChildren(all.stream().filter(m -> m.getParentId().equals(root.getId())).collect(Collectors.toList()));
        }
        return roots;
    }

    @Override
    public PageResult<User> listUsers(int page, int size, String keyword) {
        Page<User> pg = new Page<>(page, size);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(User::getUsername, keyword).or().like(User::getName, keyword);
        }
        qw.orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(pg, qw);
        return PageResult.of(result);
    }

    @Override
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        user.setPassword(null); // 不允许通过此方法修改密码
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) throw new com.chuanzhi.health.common.BusinessException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) throw new com.chuanzhi.health.common.BusinessException("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
