package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.user.UserResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Page<UserResponse>> getMyTenants(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean availableOnly,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        Page<UserResponse> tenants = userService.getTenantsByLandlord(landlord, status, availableOnly, pageable);
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/tenants/{id}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<UserResponse> getTenantDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        UserResponse tenant = userService.getTenantDetailForLandlord(id, landlord);
        return ResponseEntity.ok(tenant);
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<UserResponse> toggleTenantStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        User updated = userService.toggleTenantStatusForLandlord(id, landlord);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @jakarta.validation.Valid @RequestBody com.qlpt.backend.dto.user.UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        User updated = userService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @jakarta.validation.Valid @RequestBody com.qlpt.backend.dto.auth.ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        userService.changePassword(user.getId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<UserResponse> resetPassword(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User actor = userDetails.getUser();
        User updated = userService.resetPassword(id, actor);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }
}
