package com.qlpt.backend.service;

import com.qlpt.backend.dto.UserResponse;
import com.qlpt.backend.entity.Role;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.ContractRepository;
import com.qlpt.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ContractRepository contractRepository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.contractRepository = contractRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getTenantsByLandlord(User landlord, Pageable pageable) {
        return getTenantsByLandlord(landlord, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getTenantsByLandlord(User landlord, String status, Boolean availableOnly, Pageable pageable) {
        Page<User> tenants = userRepository.findTenants(Role.TENANT, landlord.getId(), status, availableOnly, pageable);
        if (tenants.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> tenantIds = tenants.stream().map(User::getId).collect(Collectors.toList());
        List<UUID> activeTenantIds = contractRepository.findTenantIdsWithActiveContracts(tenantIds);
        java.util.Set<UUID> activeTenantSet = new java.util.HashSet<>(activeTenantIds);

        return tenants.map(u -> UserResponse.fromEntity(u, activeTenantSet.contains(u.getId())));
    }

    @Transactional(readOnly = true)
    public UserResponse getTenantDetailForLandlord(UUID tenantId, User landlord) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người thuê"));

        if (tenant.getRole() != Role.TENANT) {
            throw new RuntimeException("Tài khoản này không phải là người thuê");
        }

        if (tenant.getLandlord() == null || !tenant.getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền xem thông tin tài khoản người thuê này");
        }

        boolean hasActiveContract = contractRepository.findTenantIdsWithActiveContracts(List.of(tenantId)).size() > 0;
        return UserResponse.fromEntity(tenant, hasActiveContract);
    }

    @Transactional
    public User toggleTenantStatusForLandlord(UUID tenantId, User landlord) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người thuê"));

        if (tenant.getRole() != Role.TENANT) {
            throw new RuntimeException("Tài khoản này không phải là người thuê");
        }

        if (!tenant.getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền quản lý tài khoản người thuê này");
        }

        if ("ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            tenant.setStatus("INACTIVE");
        } else {
            tenant.setStatus("ACTIVE");
        }

        return userRepository.save(tenant);
    }

    // ==========================================
    // ADMIN ACTIONS
    // ==========================================

    public Page<User> getAllLandlords(User admin, Pageable pageable) {
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Chỉ quản trị viên mới có quyền xem danh sách chủ trọ");
        }
        return userRepository.findByRole(Role.LANDLORD, pageable);
    }

    @Transactional
    public User toggleUserStatus(UUID userId, User admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Chỉ quản trị viên mới có quyền cập nhật trạng thái người dùng");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng để thay đổi trạng thái"));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Không thể khóa tài khoản quản trị viên khác");
        }

        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("INACTIVE");
        } else {
            user.setStatus("ACTIVE");
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(UUID userId, com.qlpt.backend.dto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));
        
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, com.qlpt.backend.dto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));
        
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }
        
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public User resetPassword(UUID targetUserId, User actor) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng đích"));
        
        // Authorization check
        if (actor.getRole() == Role.ADMIN) {
            // Admin can reset anyone except other admins
            if (targetUser.getRole() == Role.ADMIN && !actor.getId().equals(targetUser.getId())) {
                throw new RuntimeException("Quản trị viên không thể đặt lại mật khẩu của quản trị viên khác");
            }
        } else if (actor.getRole() == Role.LANDLORD) {
            // Landlord can only reset their tenants
            if (targetUser.getRole() != Role.TENANT) {
                throw new RuntimeException("Chủ trọ chỉ có quyền đặt lại mật khẩu của người thuê");
            }
            if (targetUser.getLandlord() == null || !targetUser.getLandlord().getId().equals(actor.getId())) {
                throw new RuntimeException("Tài khoản người thuê này không thuộc sự quản lý của bạn");
            }
        } else {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }
        
        // Reset password to their phone number, fallback to '123456' if phone is empty
        String rawPassword = (targetUser.getPhone() != null && !targetUser.getPhone().trim().isEmpty()) 
                ? targetUser.getPhone().trim() 
                : "123456";
                
        targetUser.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(targetUser);
    }
}
