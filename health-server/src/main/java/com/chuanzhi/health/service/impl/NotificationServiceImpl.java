package com.chuanzhi.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.entity.Notification;
import com.chuanzhi.health.mapper.NotificationMapper;
import com.chuanzhi.health.service.NotificationService;
import com.chuanzhi.health.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final SseService sseService;

    @Override
    public IPage<Notification> listNotifications(Long userId, int page, int size) {
        return notificationMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
        );
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
        );
    }

    @Override
    public void markAsRead(Long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    @Override
    public void createNotification(Long userId, String title, String content, String type, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type != null ? type : "SYSTEM");
        n.setIsRead(0);
        n.setRelatedId(relatedId);
        notificationMapper.insert(n);
        sseService.broadcast("notifications:" + userId, "new_notification",
            java.util.Map.of("title", title, "content", content, "type", type));
    }
}
