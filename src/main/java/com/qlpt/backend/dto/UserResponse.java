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
    Role role
) {
    public static UserResponse fromEntity(User user) {
        if (user == null || !org.hibernate.Hibernate.isInitialized(user)) return null;
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getFullName(),
            user.getStatus(),
            user.getRole()
        );
    }
}
