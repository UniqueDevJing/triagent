package com.chuanzhi.health.service.admin;

import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.*;

public interface SystemService {
    // 科室管理
    PageResult<Department> listDepartments(int page, int size);
    Department getDepartment(Long id);
    Department createDepartment(Department dept);
    Department updateDepartment(Department dept);
    void deleteDepartment(Long id);

    // 角色管理
    PageResult<Role> listRoles(int page, int size);
    Role getRole(Long id);
    Role updateRoleMenus(Long id, String menus);

    // 菜单管理
    java.util.List<Menu> getMenuTree();

    // 用户管理（增强）
    PageResult<User> listUsers(int page, int size, String keyword);
    User createUser(User user);
    User updateUser(User user);
    void updateUserStatus(Long id, Integer status);
    void resetPassword(Long id, String newPassword);
    void deleteUser(Long id);
}
