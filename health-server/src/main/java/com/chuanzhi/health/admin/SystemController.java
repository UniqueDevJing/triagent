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
        return Result.ok(systemService.listDepartments(page, size));
    }
    @PostMapping("/departments")
    public Result<Department> createDepartment(@RequestBody Department dept) {
        return Result.ok(systemService.createDepartment(dept));
    }
    @PutMapping("/departments/{id}")
    public Result<Department> updateDepartment(@PathVariable Long id, @RequestBody Department dept) {
        dept.setId(id);
        return Result.ok(systemService.updateDepartment(dept));
    }
    @DeleteMapping("/departments/{id}")
    public Result<?> deleteDepartment(@PathVariable Long id) {
        systemService.deleteDepartment(id);
        return Result.ok(null);
    }

    @GetMapping("/roles")
    public Result<?> listRoles(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(systemService.listRoles(page, size));
    }
    @PutMapping("/roles/{id}/menus")
    public Result<Role> updateRoleMenus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return Result.ok(systemService.updateRoleMenus(id, body.get("menus")));
    }

    @GetMapping("/menus")
    public Result<List<Menu>> getMenuTree() {
        return Result.ok(systemService.getMenuTree());
    }

    @GetMapping("/users")
    public Result<?> listUsers(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword) {
        return Result.ok(systemService.listUsers(page, size, keyword));
    }
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        return Result.ok(systemService.createUser(user));
    }
    @PutMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return Result.ok(systemService.updateUser(user));
    }
    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> body) {
        systemService.updateUserStatus(id, body.get("status"));
        return Result.ok(null);
    }
    @PutMapping("/users/{id}/password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        systemService.resetPassword(id, body.get("password"));
        return Result.ok(null);
    }
    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id);
        return Result.ok(null);
    }
}
