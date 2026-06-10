package com.qlpt.backend.service;

import com.qlpt.backend.dto.notification.NotificationResponse;
import com.qlpt.backend.entity.Notification;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
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
        return NotificationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
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
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    @Transactional
    public int deleteNotificationsOlderThan(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return notificationRepository.deleteByCreatedAtBefore(cutoff);
    }
}
