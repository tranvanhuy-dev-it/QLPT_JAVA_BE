package com.qlpt.backend.service;

import com.qlpt.backend.dto.notification.NotificationResponse;
import com.qlpt.backend.entity.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    public NotificationResponse createNotification(User user, String title, String content, String type);
    public NotificationResponse createNotification(User user, String title, String content, String type, UUID referenceId);

    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable);

    public long getUnreadCount(User user);

    public void markAsRead(UUID id, User user);

    public void markAllAsRead(User user);

    public void sendNotificationToAllTenants(User landlord, String title, String content);

    public int deleteNotificationsOlderThan(int days);
}
