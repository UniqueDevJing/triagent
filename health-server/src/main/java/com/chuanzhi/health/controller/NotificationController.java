package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.Notification;
import com.chuanzhi.health.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "通知中心", description = "系统通知查看与管理")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取通知列表")
    @GetMapping
    public Result<PageResult<Notification>> list(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<Notification> result = notificationService.listNotifications(userId, page, size);
        return Result.ok(PageResult.of(result));
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount(@RequestParam Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return Result.ok(Map.of("count", count));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<?> markAllRead(@RequestParam Long userId) {
        notificationService.markAllRead(userId);
        return Result.ok();
    }
}
