package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.auth.JwtResponse;
import com.qlpt.backend.dto.auth.LoginRequest;
import com.qlpt.backend.dto.auth.RegisterRequest;
import com.qlpt.backend.dto.auth.GoogleLoginRequest;
import com.qlpt.backend.dto.user.TenantCreateRequest;
import com.qlpt.backend.dto.user.UserResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (request.getRole() == com.qlpt.backend.enums.Role.TENANT) {
            throw new RuntimeException("Người thuê phải do chủ trọ cấp tài khoản, không thể tự đăng ký");
        }
        User user = authService.registerUser(request);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse jwtResponse = authService.authenticateUser(request);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/google")
    public ResponseEntity<JwtResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        JwtResponse jwtResponse = authService.authenticateGoogleUser(request);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/create-tenant")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<UserResponse> createTenant(
            @Valid @RequestBody TenantCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        User tenant = authService.createTenantAccount(request, landlord);
        return ResponseEntity.ok(UserResponse.fromEntity(tenant));
    }
}
