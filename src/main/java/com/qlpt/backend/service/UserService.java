package com.qlpt.backend.service;

import com.qlpt.backend.entity.Role;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.exception.ResourceNotFoundException;
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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));
    }

    public Page<User> getTenantsByLandlord(User landlord, Pageable pageable) {
        return userRepository.findByRoleAndLandlordId(Role.TENANT, landlord.getId(), pageable);
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
}
