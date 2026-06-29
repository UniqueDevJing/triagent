# 传智健康管理系统架构重设计 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有传智健康系统重构为包含管理端+会员端的完整健康管理平台，覆盖预约管理、会员管理、健康评估、健康干预、知识库、系统设置7大模块，共25张表。

**Architecture:** Modular Monolith 后端 (Spring Boot 3.2)，管理端 SPA (Vue 3 + Element Plus)，会员端 H5 (Vue 3 + Vant UI)，共享同一 Spring Boot 后端，通过 `/api/admin/*` 和 `/api/member/*` 隔离。

**Tech Stack:** Spring Boot 3.2 + Java 17 + MyBatis-Plus 3.5 + MySQL 8.0 + Redis 7 / Vue 3.4 + Element Plus + Vant UI + Pinia + Vite 5

## Global Constraints

- Java 17, Spring Boot 3.2, MyBatis-Plus 3.5
- 所有 API 统一返回 `Result<T>` 格式 `{ code, message, data }`
- 管理端 API 前缀 `/api/admin/`，会员端 `/api/member/`
- 实体需逻辑删除字段 `deleted`，MyBatis-Plus 自动处理
- 密码 BCrypt 加密，JWT HMAC-SHA384 签名
- 前端使用 Element Plus 中文语言包
- 角色级权限控制：ADMIN/DOCTOR/NURSE/MEMBER

---

## Phase 1: 基础架构重构（数据库迁移 + 系统设置 RBAC）

### Task 1.1: 创建数据库迁移脚本

**Files:**
- Create: `health-server/src/main/resources/db/migration/V2__system_settings.sql`
- Create: `health-server/src/main/resources/db/migration/V3__members.sql`

**Interfaces:**
- Consumes: 现有 init.sql (V1)
- Produces: departments, roles, menus, members, physical_exam_plans 表

- [ ] **Step 1: 编写 V2 系统设置迁移脚本**

```sql
-- V2__system_settings.sql
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

INSERT INTO departments (name, code, sort_order) VALUES
('内科', 'NEIKE', 1), ('外科', 'WAIKE', 2), ('体检中心', 'TIJIAN', 3),
('中医科', 'ZHONGYI', 4), ('心理科', 'XINLI', 5), ('营养科', 'YINGYANG', 6);

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    menus JSON,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

INSERT INTO roles (name, code, menus) VALUES
('系统管理员', 'ADMIN', '["dashboard","members","appointments","assessments","interventions","knowledge","statistics","system"]'),
('医生', 'DOCTOR', '["dashboard","members","assessments","interventions","knowledge"]'),
('护士', 'NURSE', '["dashboard","appointments","health-records","knowledge"]');

CREATE TABLE IF NOT EXISTS menus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(100),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

INSERT INTO menus (parent_id, name, path, icon, sort_order) VALUES
(0, '工作台', '/dashboard', 'DataAnalysis', 1),
(0, '会员管理', '/members', 'User', 2),
(0, '预约管理', '/appointments', 'Calendar', 3),
(0, '健康评估', '/assessments', 'DocumentChecked', 4),
(0, '健康干预', '/interventions', 'SetUp', 5),
(0, '知识库', '/knowledge', 'Reading', 6),
(0, '统计分析', '/statistics', 'TrendCharts', 7),
(0, '系统设置', '/system', 'Setting', 8);

-- 扩展 users 表: 添加 department_id
ALTER TABLE users ADD COLUMN department_id BIGINT DEFAULT NULL AFTER email,
ADD COLUMN avatar VARCHAR(200) DEFAULT NULL AFTER department_id,
ADD COLUMN status TINYINT DEFAULT 1 AFTER avatar;
```

- [ ] **Step 2: 执行迁移**

```bash
docker exec -i health-mysql mysql -u root -proot123 --default-character-set=utf8mb4 health_management < health-server/src/main/resources/db/migration/V2__system_settings.sql
```

Expected: 无错误输出

- [ ] **Step 3: 编写 V3 会员表迁移脚本**

```sql
-- V3__members.sql
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    gender TINYINT DEFAULT 0 COMMENT '0未知 1男 2女',
    age INT DEFAULT 0,
    id_card VARCHAR(18),
    phone VARCHAR(20),
    emergency_contact VARCHAR(50),
    emergency_phone VARCHAR(20),
    blood_type VARCHAR(5),
    height DECIMAL(5,2),
    weight DECIMAL(5,2),
    medical_history JSON,
    allergies JSON,
    member_level VARCHAR(20) DEFAULT 'NORMAL',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

CREATE TABLE IF NOT EXISTS physical_exam_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检计划表';
```

- [ ] **Step 4: 执行 V3 迁移**

```bash
docker exec -i health-mysql mysql -u root -proot123 --default-character-set=utf8mb4 health_management < health-server/src/main/resources/db/migration/V3__members.sql
```

Expected: 无错误输出

- [ ] **Step 5: Commit**

```bash
git add health-server/src/main/resources/db/migration/V2__system_settings.sql health-server/src/main/resources/db/migration/V3__members.sql
git commit -m "feat: 添加系统设置与会员管理数据库表"
```

---

### Task 1.2: 后端实体与 Mapper（系统设置域）

**Files:**
- Create: `health-server/src/main/java/com/chuanzhi/health/entity/Department.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/entity/Role.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/entity/Menu.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/mapper/DepartmentMapper.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/mapper/RoleMapper.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/mapper/MenuMapper.java`
- Modify: `health-server/src/main/java/com/chuanzhi/health/entity/User.java` (添加 departmentId, avatar, status)

- [ ] **Step 1: 创建 Department 实体**

```java
package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("departments")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 Role 实体**

```java
package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("roles")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String menus;  // JSON string
    private String description;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 Menu 实体**

```java
package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("menus")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private Integer sortOrder;
}
```

- [ ] **Step 4: 创建 Mapper 接口**

```java
package com.chuanzhi.health.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanzhi.health.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {}
```

同样创建 `RoleMapper.java` 和 `MenuMapper.java`，均继承 `BaseMapper`。

- [ ] **Step 5: 修改 User 实体添加新字段**

```java
// User.java 添加
private Long departmentId;
private String avatar;
private Integer status; // 0禁用 1启用
```

- [ ] **Step 6: Commit**

```bash
git add health-server/src/main/java/com/chuanzhi/health/entity/Department.java health-server/src/main/java/com/chuanzhi/health/entity/Role.java health-server/src/main/java/com/chuanzhi/health/entity/Menu.java health-server/src/main/java/com/chuanzhi/health/mapper/DepartmentMapper.java health-server/src/main/java/com/chuanzhi/health/mapper/RoleMapper.java health-server/src/main/java/com/chuanzhi/health/mapper/MenuMapper.java
git commit -m "feat: 添加系统设置域实体与Mapper"
```

---

### Task 1.3: 系统设置 Service + Controller

**Files:**
- Create: `health-server/src/main/java/com/chuanzhi/health/service/admin/SystemService.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/service/admin/impl/SystemServiceImpl.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/admin/SystemController.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/dto/UserCreateRequest.java`

- [ ] **Step 1: 创建 SystemService 接口**

```java
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
```

- [ ] **Step 2: 创建 SystemServiceImpl**

```java
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
        return new PageResult<>(result.getTotal(), page, size, result.getRecords());
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
        return new PageResult<>(result.getTotal(), page, size, result.getRecords());
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
        return new PageResult<>(result.getTotal(), page, size, result.getRecords());
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
```

- [ ] **Step 3: 创建 SystemController**

```java
package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.*;
import com.chuanzhi.health.service.admin.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/departments")
    public Result<?> listDepartments(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return Result.success(systemService.listDepartments(page, size));
    }
    @PostMapping("/departments")
    public Result<Department> createDepartment(@RequestBody Department dept) {
        return Result.success(systemService.createDepartment(dept));
    }
    @PutMapping("/departments/{id}")
    public Result<Department> updateDepartment(@PathVariable Long id, @RequestBody Department dept) {
        dept.setId(id);
        return Result.success(systemService.updateDepartment(dept));
    }
    @DeleteMapping("/departments/{id}")
    public Result<?> deleteDepartment(@PathVariable Long id) {
        systemService.deleteDepartment(id);
        return Result.success(null);
    }

    @GetMapping("/roles")
    public Result<?> listRoles(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return Result.success(systemService.listRoles(page, size));
    }
    @PutMapping("/roles/{id}/menus")
    public Result<Role> updateRoleMenus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return Result.success(systemService.updateRoleMenus(id, body.get("menus")));
    }

    @GetMapping("/menus")
    public Result<List<Menu>> getMenuTree() {
        return Result.success(systemService.getMenuTree());
    }

    @GetMapping("/users")
    public Result<?> listUsers(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword) {
        return Result.success(systemService.listUsers(page, size, keyword));
    }
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        return Result.success(systemService.createUser(user));
    }
    @PutMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return Result.success(systemService.updateUser(user));
    }
    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> body) {
        systemService.updateUserStatus(id, body.get("status"));
        return Result.success(null);
    }
    @PutMapping("/users/{id}/password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        systemService.resetPassword(id, body.get("password"));
        return Result.success(null);
    }
    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
cd health-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add health-server/src/main/java/com/chuanzhi/health/service/admin/ health-server/src/main/java/com/chuanzhi/health/admin/
git commit -m "feat: 系统设置模块 - 科室/角色/菜单/用户管理API"
```

---

### Task 1.4: 管理端系统设置页面

**Files:**
- Create: `health-admin/src/views/system/Users.vue`
- Create: `health-admin/src/views/system/Roles.vue`
- Create: `health-admin/src/views/system/Departments.vue`
- Create: `health-admin/src/views/system/Menus.vue`
- Create: `health-admin/src/api/modules/system.js`
- Modify: `health-admin/src/router/index.js` (添加系统设置路由)
- Modify: `health-admin/src/App.vue` (根据角色过滤菜单)

- [ ] **Step 1: 创建 system API 模块**

```javascript
// health-admin/src/api/modules/system.js
import request from '@/api/request'

export const getUsers = (params) => request.get('/admin/users', { params })
export const createUser = (data) => request.post('/admin/users', data)
export const updateUser = (id, data) => request.put(`/admin/users/${id}`, data)
export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const resetPassword = (id, password) => request.put(`/admin/users/${id}/password`, { password })
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)

export const getDepartments = (params) => request.get('/admin/departments', { params })
export const createDepartment = (data) => request.post('/admin/departments', data)
export const updateDepartment = (id, data) => request.put(`/admin/departments/${id}`, data)
export const deleteDepartment = (id) => request.delete(`/admin/departments/${id}`)

export const getRoles = (params) => request.get('/admin/roles', { params })
export const updateRoleMenus = (id, menus) => request.put(`/admin/roles/${id}/menus`, { menus })

export const getMenuTree = () => request.get('/admin/menus')
```

- [ ] **Step 2: 创建用户管理页面**

```vue
<!-- health-admin/src/views/system/Users.vue -->
<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openDialog()">新增用户</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'DOCTOR' ? 'warning' : 'info'">
            {{ roleMap[row.role] || row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" @click="handleResetPwd(row)">重置密码</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" :page-size="size" @current-change="fetch" layout="prev,pager,next" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码" required><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.role"><el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" /></el-select></el-form-item>
        <el-form-item label="科室"><el-select v-model="form.departmentId"><el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as sysApi from '@/api/modules/system'

const roleMap = { ADMIN: '管理员', DOCTOR: '医生', NURSE: '护士' }
const roles = [{ code: 'ADMIN', name: '管理员' }, { code: 'DOCTOR', name: '医生' }, { code: 'NURSE', name: '护士' }]
const departments = ref([])
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const form = ref({})

onMounted(() => { fetch(); loadDepartments() })

async function fetch() {
  loading.value = true
  const res = await sysApi.getUsers({ page: page.value, size: size.value })
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

async function loadDepartments() {
  const res = await sysApi.getDepartments({ page: 1, size: 100 })
  departments.value = res.data.records
}

function openDialog(row) {
  form.value = row ? { ...row } : { role: 'NURSE', status: 1 }
  dialogVisible.value = true
}

async function save() {
  if (form.value.id) {
    await sysApi.updateUser(form.value.id, form.value)
  } else {
    await sysApi.createUser(form.value)
  }
  dialogVisible.value = false
  ElMessage.success('保存成功')
  fetch()
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await sysApi.updateUserStatus(row.id, newStatus)
  row.status = newStatus
  ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
}

async function handleResetPwd(row) {
  try { const pwd = '123456'; await sysApi.resetPassword(row.id, pwd); ElMessage.success(`已重置为 ${pwd}`) } catch {}
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await sysApi.deleteUser(row.id)
  ElMessage.success('已删除')
  fetch()
}
</script>
```

- [ ] **Step 3: 创建科室管理页面**

```vue
<!-- health-admin/src/views/system/Departments.vue -->
<template>
  <div class="page-container">
    <div class="page-header">
      <h2>科室管理</h2>
      <el-button type="primary" @click="openDialog()">新增科室</el-button>
    </div>
    <el-table :data="tableData" stripe>
      <el-table-column prop="name" label="科室名称" />
      <el-table-column prop="code" label="编码" />
      <el-table-column prop="sortOrder" label="排序" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑科室' : '新增科室'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as sysApi from '@/api/modules/system'

const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({})

onMounted(() => fetch())
async function fetch() { const res = await sysApi.getDepartments({ page: 1, size: 100 }); tableData.value = res.data.records }
function openDialog(row) { form.value = row ? { ...row } : { sortOrder: 0 }; dialogVisible.value = true }
async function save() {
  if (form.value.id) await sysApi.updateDepartment(form.value.id, form.value)
  else await sysApi.createDepartment(form.value)
  dialogVisible.value = false; ElMessage.success('保存成功'); fetch()
}
async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await sysApi.deleteDepartment(row.id); ElMessage.success('已删除'); fetch()
}
</script>
```

- [ ] **Step 4: 创建角色管理页面**

```vue
<!-- health-admin/src/views/system/Roles.vue -->
<template>
  <div class="page-container">
    <div class="page-header"><h2>角色管理</h2></div>
    <el-table :data="tableData" stripe>
      <el-table-column prop="name" label="角色" />
      <el-table-column prop="code" label="编码" />
      <el-table-column label="菜单权限" width="200">
        <template #default="{ row }">
          <el-button text size="small" @click="openMenuDialog(row)">配置菜单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="menuVisible" title="配置菜单权限" width="400px">
      <el-checkbox-group v-model="selectedMenus">
        <el-checkbox v-for="m in menus" :key="m.path" :value="m.path" :label="m.name" style="display:block;margin-bottom:8px" />
      </el-checkbox-group>
      <template #footer><el-button @click="menuVisible = false">取消</el-button><el-button type="primary" @click="saveMenus">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as sysApi from '@/api/modules/system'

const tableData = ref([])
const menus = ref([])
const menuVisible = ref(false)
const selectedMenus = ref([])
let currentRole = null

onMounted(async () => {
  const [r, m] = await Promise.all([sysApi.getRoles({ page: 1, size: 20 }), sysApi.getMenuTree()])
  tableData.value = r.data.records; menus.value = m.data
})

function openMenuDialog(row) {
  currentRole = row
  selectedMenus.value = typeof row.menus === 'string' ? JSON.parse(row.menus) : (row.menus || [])
  menuVisible.value = true
}

async function saveMenus() {
  await sysApi.updateRoleMenus(currentRole.id, JSON.stringify(selectedMenus.value))
  currentRole.menus = JSON.stringify(selectedMenus.value)
  menuVisible.value = false; ElMessage.success('保存成功')
}
</script>
```

- [ ] **Step 5: 更新路由添加系统设置**

```javascript
// router/index.js 添加路由
{
  path: '/system',
  redirect: '/system/users',
  meta: { title: '系统设置', role: 'ADMIN' },
  children: [
    { path: 'users', name: 'SystemUsers', component: () => import('@/views/system/Users.vue'), meta: { title: '用户管理', role: 'ADMIN' } },
    { path: 'roles', name: 'SystemRoles', component: () => import('@/views/system/Roles.vue'), meta: { title: '角色设置', role: 'ADMIN' } },
    { path: 'departments', name: 'SystemDepartments', component: () => import('@/views/system/Departments.vue'), meta: { title: '科室管理', role: 'ADMIN' } },
  ]
}
```

- [ ] **Step 6: Commit**

```bash
git add health-admin/src/views/system/ health-admin/src/api/modules/system.js health-admin/src/router/index.js
git commit -m "feat: 系统设置前端 - 用户/科室/角色管理页面"
```

---

## Phase 2: 会员管理

### Task 2.1: 会员管理后端

**Files:**
- Create: `health-server/src/main/java/com/chuanzhi/health/entity/Member.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/entity/PhysicalExamPlan.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/mapper/MemberMapper.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/mapper/PhysicalExamPlanMapper.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/service/admin/MemberService.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/service/admin/impl/MemberServiceImpl.java`
- Create: `health-server/src/main/java/com/chuanzhi/health/admin/MemberController.java`

- [ ] **Step 1: 创建 Member 实体**

```java
package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("members")
public class Member {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private Integer gender;
    private Integer age;
    private String idCard;
    private String phone;
    private String emergencyContact;
    private String emergencyPhone;
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    private String medicalHistory;
    private String allergies;
    private String memberLevel;
    private Integer status;
    @TableLogic private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 PhysicalExamPlan 实体** (同上模式，字段对应 V3 迁移)

- [ ] **Step 3: 创建 MemberService 和 MemberServiceImpl**

```java
package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.*;
import com.chuanzhi.health.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberMapper memberMapper;
    private final PhysicalExamPlanMapper planMapper;

    public PageResult<Member> list(int page, int size, String keyword) {
        Page<Member> pg = new Page<>(page, size);
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            qw.like(Member::getName, keyword).or().like(Member::getPhone, keyword);
        qw.orderByDesc(Member::getCreatedAt);
        Page<Member> result = memberMapper.selectPage(pg, qw);
        return new PageResult<>(result.getTotal(), page, size, result.getRecords());
    }

    public Member getById(Long id) { return memberMapper.selectById(id); }
    public Member create(Member m) { memberMapper.insert(m); return m; }
    public Member update(Member m) { memberMapper.updateById(m); return memberMapper.selectById(m.getId()); }
    public void delete(Long id) { memberMapper.deleteById(id); }

    // 体检计划
    public PageResult<PhysicalExamPlan> listPlans(Long memberId, int page, int size) {
        Page<PhysicalExamPlan> pg = new Page<>(page, size);
        LambdaQueryWrapper<PhysicalExamPlan> qw = new LambdaQueryWrapper<>();
        qw.eq(PhysicalExamPlan::getMemberId, memberId).orderByDesc(PhysicalExamPlan::getCreatedAt);
        Page<PhysicalExamPlan> result = planMapper.selectPage(pg, qw);
        return new PageResult<>(result.getTotal(), page, size, result.getRecords());
    }
    public PhysicalExamPlan createPlan(PhysicalExamPlan p) { planMapper.insert(p); return p; }
    public PhysicalExamPlan updatePlan(PhysicalExamPlan p) { planMapper.updateById(p); return planMapper.selectById(p.getId()); }
    public void deletePlan(Long id) { planMapper.deleteById(id); }
}
```

- [ ] **Step 4: 创建 MemberController**

```java
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword) {
        return Result.success(memberService.list(page, size, keyword));
    }
    @GetMapping("/{id}")
    public Result<Member> get(@PathVariable Long id) { return Result.success(memberService.getById(id)); }
    @PostMapping
    public Result<Member> create(@RequestBody Member m) { return Result.success(memberService.create(m)); }
    @PutMapping("/{id}")
    public Result<Member> update(@PathVariable Long id, @RequestBody Member m) { m.setId(id); return Result.success(memberService.update(m)); }
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) { memberService.delete(id); return Result.success(null); }

    @GetMapping("/{memberId}/exam-plans")
    public Result<?> listPlans(@PathVariable Long memberId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(memberService.listPlans(memberId, page, size));
    }
    @PostMapping("/{memberId}/exam-plans")
    public Result<PhysicalExamPlan> createPlan(@PathVariable Long memberId, @RequestBody PhysicalExamPlan plan) {
        plan.setMemberId(memberId); return Result.success(memberService.createPlan(plan));
    }
    @PutMapping("/exam-plans/{planId}")
    public Result<PhysicalExamPlan> updatePlan(@PathVariable Long planId, @RequestBody PhysicalExamPlan plan) {
        plan.setId(planId); return Result.success(memberService.updatePlan(plan));
    }
    @DeleteMapping("/exam-plans/{planId}")
    public Result<?> deletePlan(@PathVariable Long planId) { memberService.deletePlan(planId); return Result.success(null); }
}
```

- [ ] **Step 5: Commit**

```bash
git add health-server/src/main/java/com/chuanzhi/health/entity/Member.java health-server/src/main/java/com/chuanzhi/health/entity/PhysicalExamPlan.java health-server/src/main/java/com/chuanzhi/health/mapper/MemberMapper.java health-server/src/main/java/com/chuanzhi/health/mapper/PhysicalExamPlanMapper.java health-server/src/main/java/com/chuanzhi/health/service/admin/MemberService.java health-server/src/main/java/com/chuanzhi/health/admin/MemberController.java
git commit -m "feat: 会员管理 - 会员CRUD与体检计划API"
```

---

### Task 2.2: 管理端会员管理页面

**Files:**
- Create: `health-admin/src/views/members/MemberList.vue`
- Create: `health-admin/src/views/members/MemberDetail.vue`
- Create: `health-admin/src/api/modules/members.js`

- [ ] **Step 1: 创建 members API 模块**

```javascript
import request from '@/api/request'
export const getMembers = (params) => request.get('/admin/members', { params })
export const getMember = (id) => request.get(`/admin/members/${id}`)
export const createMember = (data) => request.post('/admin/members', data)
export const updateMember = (id, data) => request.put(`/admin/members/${id}`, data)
export const deleteMember = (id) => request.delete(`/admin/members/${id}`)
export const getExamPlans = (memberId, params) => request.get(`/admin/members/${memberId}/exam-plans`, { params })
export const createExamPlan = (memberId, data) => request.post(`/admin/members/${memberId}/exam-plans`, data)
export const updateExamPlan = (planId, data) => request.put(`/admin/members/exam-plans/${planId}`, data)
```

- [ ] **Step 2: 创建会员列表页** - 使用标准 Element Plus 表格 + 分页 + 搜索 + 新增/编辑对话框，字段参照 Member 实体

- [ ] **Step 3: 创建会员详情页** - 显示会员基本信息、体检计划列表、评估记录、干预方案（Tab 切换）

- [ ] **Step 4: 更新路由**

```javascript
{
  path: '/members',
  redirect: '/members/list',
  children: [
    { path: 'list', name: 'MemberList', component: () => import('@/views/members/MemberList.vue'), meta: { title: '会员列表' } },
    { path: ':id', name: 'MemberDetail', component: () => import('@/views/members/MemberDetail.vue'), meta: { title: '会员详情' } },
  ]
}
```

- [ ] **Step 5: Commit**

---

后续阶段遵循相同模式，关键里程碑如下：

## Phase 3: 预约管理（最复杂模块）

### Task 3.1: 预约管理数据库迁移
- V4: exam_item_categories, exam_items, packages, package_items, appointments

### Task 3.2: 预约管理后端
- ExamItemCategory, ExamItem, Package, PackageItem, Appointment 实体
- 对应 Mapper
- AppointmentService (含批量导入导出、状态流转)
- AppointmentController + PackageController + ExamItemController

### Task 3.3: 预约管理前端
- 预约列表 + 日历视图
- 新增预约（步骤式：选会员→选套餐→确认时间）
- 套餐管理页面
- 检测项/检测项目组管理页面
- Excel 批量导入 + 模板下载

## Phase 4: 健康评估扩展

### Task 4.1: 评估域数据库迁移
- V5: assessment_indicators, assessment_templates 扩展, tcm_constitutions, psychology_assessments

### Task 4.2: 评估后端
- AssessmentIndicator, TcmConstitution, PsychologyAssessment 实体
- 扩展 AssessmentService 支持三种评估类型

### Task 4.3: 评估前端
- 指标管理页面
- 中医体质辨识页面
- 心理评测管理页面

## Phase 5: 健康干预扩展

### Task 5.1: 干预域数据库迁移
- V6: chronic_disease_mgmt, diet_logs, crowd_programs

### Task 5.2: 干预后端
- 新增实体和 Service

### Task 5.3: 干预前端
- 慢病管理页面
- 膳食日志页面
- 人群方案页面

## Phase 6: 知识库扩展

### Task 6.1: 知识库数据库迁移
- V7: education_contents, education_words, exercise_library, disease_library, health_recipes
- knowledge_categories 扩展

### Task 6.2: 知识库后端
- 6 个新实体 + Mapper + Service

### Task 6.3: 知识库前端
- 宣教内容/宣教词管理
- 运动项目库/疾病库/食谱库

## Phase 7: 会员端 H5

### Task 7.1: 会员端项目初始化
- Vite + Vue 3 + Vant UI 脚手架
- 手机号验证码登录
- JWT 认证

### Task 7.2: 会员端核心页面
- 首页、个人档案、我的预约、评估结果、干预方案、膳食记录
- 知识库浏览（文章/食谱/宣教）

## Phase 8: 统计分析

### Task 8.1: 统计后端
- Dashboard 增强
- 报表统计接口

### Task 8.2: 统计前端
- 数据概览看板
- 报表导出

## Phase 9: DevOps

### Task 9.1: Nginx 配置更新
- 管理端 + 会员端多入口配置

### Task 9.2: Docker Compose 更新
- 多阶段构建
- 环境变量管理

---

**当前计划聚焦 Phase 1-2 的详细步骤。Phase 3-9 将在后续按相同模式展开。**
