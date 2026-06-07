package com.qlpt.backend.dto;

import com.qlpt.backend.entity.Role;
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
    String permanentAddress
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
            user.getPermanentAddress()
        );
    }
}
