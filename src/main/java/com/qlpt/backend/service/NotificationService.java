package com.qlpt.backend.service;

import com.qlpt.backend.dto.notification.NotificationResponse;
import com.qlpt.backend.entity.Notification;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationService {
    public NotificationResponse createNotification(User user, String title, String content, String type);
    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable);
    public long getUnreadCount(User user);
    public void markAsRead(UUID id, User user);
    public void markAllAsRead(User user);
    public int deleteNotificationsOlderThan(int days);
}
