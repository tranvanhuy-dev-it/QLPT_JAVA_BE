package com.qlpt.backend.dto.chat;

import com.qlpt.backend.entity.Message;
import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID chatRoomId,
    UUID senderId,
    String senderName,
    String senderUsername,
    String content,
    String type,
    String mediaUrl,
    boolean isRead,
    LocalDateTime createdAt
) {
    public static MessageResponse fromEntity(Message message) {
        if (message == null) return null;
        return new MessageResponse(
            message.getId(),
            message.getChatRoom().getId(),
            message.getSender().getId(),
            message.getSender().getFullName(),
            message.getSender().getUsername(),
            message.getContent(),
            message.getType(),
            message.getMediaUrl(),
            message.isRead(),
            message.getCreatedAt()
        );
    }
}
