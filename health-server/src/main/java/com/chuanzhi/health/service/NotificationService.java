package com.chuanzhi.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.entity.Notification;

public interface NotificationService {
    IPage<Notification> listNotifications(Long userId, int page, int size);
    long getUnreadCount(Long userId);
    void markAsRead(Long id);
    void markAllRead(Long userId);
    void createNotification(Long userId, String title, String content, String type, Long relatedId);
}
