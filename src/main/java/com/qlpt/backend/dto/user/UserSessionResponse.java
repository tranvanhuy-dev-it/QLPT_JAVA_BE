package com.qlpt.backend.dto.user;

import com.qlpt.backend.entity.UserSession;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserSessionResponse(
    UUID id,
    String ipAddress,
    String userAgent,
    LocalDateTime loginTime,
    LocalDateTime lastActivityTime,
    boolean active,
    boolean current
) {
    public static UserSessionResponse fromEntity(UserSession session, String currentToken) {
        if (session == null) return null;
        boolean isCurrent = currentToken != null && currentToken.equals(session.getToken());
        return new UserSessionResponse(
            session.getId(),
            session.getIpAddress(),
            session.getUserAgent(),
            session.getLoginTime(),
            session.getLastActivityTime(),
            session.isActive(),
            isCurrent
        );
    }
}
