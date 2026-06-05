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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        // Tránh trả về mật khẩu
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Page<User>> getMyTenants(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User landlord = userDetails.getUser();
        Page<User> tenants = userService.getTenantsByLandlord(landlord, pageable);
        // Tránh trả về mật khẩu cho danh sách
        tenants.forEach(t -> t.setPassword(null));
        return ResponseEntity.ok(tenants);
    }
}
