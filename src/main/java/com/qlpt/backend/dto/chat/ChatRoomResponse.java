package com.qlpt.backend.dto.chat;

import com.qlpt.backend.dto.user.UserResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomResponse(
    UUID id,
    UserResponse tenant,
    UserResponse landlord,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String lastMessage,
    LocalDateTime lastMessageTime,
    long unreadCount
) {}
