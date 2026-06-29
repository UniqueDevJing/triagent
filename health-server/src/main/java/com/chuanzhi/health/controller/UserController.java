package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.User;
import com.chuanzhi.health.service.SseService;
import com.chuanzhi.health.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理", description = "健康档案用户的增删改查")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SseService sseService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        IPage<User> result = userService.pageUsers(page, size, keyword);
        return Result.ok(PageResult.of(result));
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<User> create(@Valid @RequestBody User user) {
        userService.save(user);
        sseService.broadcast("users", "user_created", user);
        return Result.ok(user);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody User user) {
        user.setId(id);
        userService.updateById(user);
        User updated = userService.getById(id);
        sseService.broadcast("users", "user_updated", updated);
        return Result.ok(updated);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.removeById(id);
        sseService.broadcast("users", "user_deleted", Map.of("id", id));
        return Result.ok();
    }
}
