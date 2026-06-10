package com.qlpt.backend.service;

import com.qlpt.backend.dto.user.UserResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.ContractRepository;
import com.qlpt.backend.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    public User getProfile(UUID userId);
    public Page<UserResponse> getTenantsByLandlord(User landlord, Pageable pageable);
    public Page<UserResponse> getTenantsByLandlord(User landlord, String status, Boolean availableOnly, Pageable pageable);
    public UserResponse getTenantDetailForLandlord(UUID tenantId, User landlord);
    public User toggleTenantStatusForLandlord(UUID tenantId, User landlord);
    public Page<User> getAllLandlords(User admin, Pageable pageable);
    public User toggleUserStatus(UUID userId, User admin);
    public User updateProfile(UUID userId, com.qlpt.backend.dto.user.UpdateProfileRequest request);
    public void changePassword(UUID userId, com.qlpt.backend.dto.auth.ChangePasswordRequest request);
    public User resetPassword(UUID targetUserId, User actor);
}
