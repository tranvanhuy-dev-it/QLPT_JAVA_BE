package com.qlpt.backend.service;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.config.JwtTokenProvider;
import com.qlpt.backend.dto.auth.JwtResponse;
import com.qlpt.backend.dto.auth.LoginRequest;
import com.qlpt.backend.dto.auth.RegisterRequest;
import com.qlpt.backend.dto.user.TenantCreateRequest;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostConstruct
    @Transactional
    public void initDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .role(Role.ADMIN)
                    .status("ACTIVE")
                    .build();
            userRepository.save(admin);
        }
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .role(request.getRole())
                .status("ACTIVE")
                .identityCard(request.getIdentityCard())
                .idCardIssueDate(request.getIdCardIssueDate())
                .idCardIssuePlace(request.getIdCardIssuePlace())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public JwtResponse authenticateUser(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            boolean isExpired = false;
            if (user.getRole() == Role.LANDLORD) {
                java.time.LocalDate now = java.time.LocalDate.now();
                java.time.LocalDateTime regDate = user.getCreatedAt() != null ? user.getCreatedAt() : java.time.LocalDateTime.now().minusDays(50);
                
                boolean trialActive = regDate.plusDays(45).isAfter(java.time.LocalDateTime.now());
                boolean subscriptionActive = user.getSubscriptionExpiredAt() != null && !user.getSubscriptionExpiredAt().isBefore(now);
                isExpired = !trialActive && !subscriptionActive;
            }

            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getRole().name(), isExpired);
        } catch (org.springframework.security.authentication.DisabledException
                 | org.springframework.security.authentication.LockedException ex) {
            throw new RuntimeException("Tài khoản của bạn chưa được kích hoạt hoặc đã bị khóa. Vui lòng liên hệ Admin!");
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new RuntimeException("Đăng nhập thất bại: " + ex.getMessage());
        }
    }

    @Transactional
    public User createTenantAccount(TenantCreateRequest request, User landlord) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập cho người thuê đã tồn tại!");
        }

        User tenant = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .role(Role.TENANT)
                .status("ACTIVE")
                .landlord(landlord)
                .identityCard(request.getIdentityCard())
                .idCardIssueDate(request.getIdCardIssueDate())
                .idCardIssuePlace(request.getIdCardIssuePlace())
                .permanentAddress(request.getPermanentAddress())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        return userRepository.save(tenant);
    }
}
