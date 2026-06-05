package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/landlords")
    public ResponseEntity<Page<User>> getAllLandlords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User admin = userDetails.getUser();
        return ResponseEntity.ok(userService.getAllLandlords(admin, pageable));
    }

    @PostMapping("/landlords/{id}/toggle")
    public ResponseEntity<User> toggleLandlordStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User admin = userDetails.getUser();
        return ResponseEntity.ok(userService.toggleUserStatus(id, admin));
    }
}
