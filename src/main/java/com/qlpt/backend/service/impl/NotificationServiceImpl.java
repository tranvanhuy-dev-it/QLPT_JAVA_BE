package com.qlpt.backend.service.impl;

import com.qlpt.backend.service.NotificationService;

import com.qlpt.backend.dto.notification.NotificationResponse;
import com.qlpt.backend.entity.Notification;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final com.qlpt.backend.config.NotificationWebSocketHandler notificationWebSocketHandler;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   com.qlpt.backend.config.NotificationWebSocketHandler notificationWebSocketHandler) {
        this.notificationRepository = notificationRepository;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    @Transactional
    @Override
    public NotificationResponse createNotification(User user, String title, String content, String type) {
        if (user == null) {
            throw new IllegalArgumentException("Người nhận thông báo không thể để trống");
        }

        // Tạo thông báo mới (việc dọn dẹp thông báo cũ hơn 30 ngày do Scheduler đảm nhiệm)
        Notification notification = Notification.builder()
                .title(title)
                .content(content)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.fromEntity(saved);

        try {
            notificationWebSocketHandler.sendNotificationToUser(user.getUsername(), response);
        } catch (Exception e) {
            // Ghi nhận lỗi nhưng không chặn luồng nghiệp vụ chính
            System.err.println("Lỗi khi gửi thông báo WebSocket: " + e.getMessage());
        }

        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    @Override
    public void markAsRead(UUID id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền sửa trạng thái thông báo này");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    @Override
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    @Transactional
    @Override
    public int deleteNotificationsOlderThan(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return notificationRepository.deleteByCreatedAtBefore(cutoff);
    }
}
