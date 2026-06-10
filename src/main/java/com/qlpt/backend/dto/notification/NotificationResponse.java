package com.qlpt.backend.dto.notification;

import com.qlpt.backend.entity.Notification;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String content,
    String type,
    boolean isRead,
    LocalDateTime createdAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) return null;
        return new NotificationResponse(
            notification.getId(),
            notification.getTitle(),
            notification.getContent(),
            notification.getType(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
