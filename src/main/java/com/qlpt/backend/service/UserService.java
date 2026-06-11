package com.qlpt.backend.service;

import com.qlpt.backend.dto.user.UserResponse;
import com.qlpt.backend.entity.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    public User getProfile(UUID userId);

    public Page<UserResponse> getTenantsByLandlord(User landlord, Pageable pageable);

    public Page<UserResponse> getTenantsByLandlord(User landlord, String status, Boolean availableOnly,
            Pageable pageable);

    public UserResponse getTenantDetailForLandlord(UUID tenantId, User landlord);

    public User toggleTenantStatusForLandlord(UUID tenantId, User landlord);

    public Page<User> getAllLandlords(User admin, Pageable pageable);

    public User toggleUserStatus(UUID userId, User admin);

    public User updateProfile(UUID userId, com.qlpt.backend.dto.user.UpdateProfileRequest request);

    public void changePassword(UUID userId, com.qlpt.backend.dto.auth.ChangePasswordRequest request);

    public User resetPassword(UUID targetUserId, User actor);

    public User updateImouSettings(UUID userId, com.qlpt.backend.dto.user.ImouSettingsRequest request);
}
