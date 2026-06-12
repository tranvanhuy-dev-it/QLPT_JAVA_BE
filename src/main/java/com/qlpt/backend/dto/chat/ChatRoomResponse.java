package com.qlpt.backend.dto.chat;

import com.qlpt.backend.dto.room.RoomResponse;
import com.qlpt.backend.dto.user.UserResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomResponse(
    UUID id,
    RoomResponse room,
    UserResponse tenant,
    UserResponse landlord,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String lastMessage,
    LocalDateTime lastMessageTime,
    long unreadCount
) {}
