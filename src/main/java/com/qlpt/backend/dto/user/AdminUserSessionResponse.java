package com.qlpt.backend.dto.user;

import com.qlpt.backend.entity.UserSession;
import com.qlpt.backend.enums.Role;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserSessionResponse(
    UUID id,
    String ipAddress,
    String userAgent,
    LocalDateTime loginTime,
    LocalDateTime lastActivityTime,
    boolean active,
    UUID userId,
    String username,
    String fullName,
    Role role
) {
    public static AdminUserSessionResponse fromEntity(UserSession session) {
        if (session == null) return null;
        var user = session.getUser();
        return new AdminUserSessionResponse(
            session.getId(),
            session.getIpAddress(),
            session.getUserAgent(),
            session.getLoginTime(),
            session.getLastActivityTime(),
            session.isActive(),
            user != null ? user.getId() : null,
            user != null ? user.getUsername() : null,
            user != null ? user.getFullName() : null,
            user != null ? user.getRole() : null
        );
    }
}
