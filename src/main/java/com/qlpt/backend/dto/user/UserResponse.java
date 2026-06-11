package com.qlpt.backend.dto.user;

import com.qlpt.backend.enums.Role;
import com.qlpt.backend.entity.User;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String phone,
    String fullName,
    String status,
    Role role,
    Boolean hasActiveContract,
    String identityCard,
    java.time.LocalDate idCardIssueDate,
    String idCardIssuePlace,
    String permanentAddress,
    java.time.LocalDateTime createdAt,
    java.time.LocalDate subscriptionExpiredAt,
    String imouAppId,
    String imouAppSecret
) {
    public static UserResponse fromEntity(User user) {
        return fromEntity(user, null);
    }

    public static UserResponse fromEntity(User user, Boolean hasActiveContract) {
        if (user == null) return null;
        try {
            user.getUsername();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getFullName(),
            user.getStatus(),
            user.getRole(),
            hasActiveContract,
            user.getIdentityCard(),
            user.getIdCardIssueDate(),
            user.getIdCardIssuePlace(),
            user.getPermanentAddress(),
            user.getCreatedAt(),
            user.getSubscriptionExpiredAt(),
            user.getImouAppId(),
            user.getImouAppSecret() != null ? "********" : null
        );
    }
}
