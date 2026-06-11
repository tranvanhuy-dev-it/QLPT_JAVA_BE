package com.qlpt.backend.service.impl;

import com.qlpt.backend.service.AuthService;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.config.JwtTokenProvider;
import com.qlpt.backend.dto.auth.JwtResponse;
import com.qlpt.backend.dto.auth.LoginRequest;
import com.qlpt.backend.dto.auth.RegisterRequest;
import com.qlpt.backend.dto.auth.GoogleLoginRequest;
import com.qlpt.backend.dto.user.TenantCreateRequest;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Value("${app.google.client-id}")
    private String googleClientId;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final com.qlpt.backend.repository.UserSessionRepository userSessionRepository;

    public AuthServiceImpl(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       com.qlpt.backend.repository.UserSessionRepository userSessionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userSessionRepository = userSessionRepository;
    }

    private void createSession(User user, String token) {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            String ipAddress = "unknown";
            String userAgent = "unknown";
            if (attributes != null) {
                jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    ipAddress = xForwardedFor.split(",")[0].trim();
                }
                userAgent = request.getHeader("User-Agent");
            }
            com.qlpt.backend.entity.UserSession session = com.qlpt.backend.entity.UserSession.builder()
                    .user(user)
                    .token(token)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .loginTime(java.time.LocalDateTime.now())
                    .lastActivityTime(java.time.LocalDateTime.now())
                    .active(true)
                    .build();
            userSessionRepository.save(session);
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu phiên đăng nhập: " + e.getMessage());
        }
    }

    @PostConstruct
    @Transactional
    @Override
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
    @Override
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }

        // SECURITY: Role luôn là LANDLORD cho endpoint tự đăng ký.
        // Không dùng request.getRole() để tránh người dùng tự đặt role = ADMIN.
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .role(Role.LANDLORD)
                .status("ACTIVE")
                .identityCard(request.getIdentityCard())
                .idCardIssueDate(request.getIdCardIssueDate())
                .idCardIssuePlace(request.getIdCardIssuePlace())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Override
    public JwtResponse authenticateUser(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            createSession(user, jwt);

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
    @Override
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

    @Transactional
    @Override
    public JwtResponse authenticateGoogleUser(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.credential());
            if (idToken == null) {
                throw new RuntimeException("Token xác thực Google không hợp lệ!");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject(); // Unique Google ID
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("Không tìm thấy email trong tài khoản Google!");
            }

            // Tìm user theo googleId trước
            User user = userRepository.findByGoogleId(googleId).orElse(null);

            if (user == null) {
                // Nếu chưa có googleId, tìm theo email
                user = userRepository.findByEmail(email).orElse(null);
                
                if (user != null) {
                    // Liên kết tài khoản hiện có với googleId
                    user.setGoogleId(googleId);
                    user = userRepository.save(user);
                } else {
                    // Tạo tài khoản mới, mặc định vai trò là LANDLORD (Chủ trọ)
                    String baseUsername = email.split("@")[0];
                    String username = baseUsername;
                    int count = 1;
                    while (userRepository.existsByUsername(username)) {
                        username = baseUsername + count;
                        count++;
                    }

                    user = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên
                            .email(email)
                            .fullName(name != null ? name : baseUsername)
                            .role(Role.LANDLORD)
                            .status("ACTIVE")
                            .googleId(googleId)
                            .createdAt(java.time.LocalDateTime.now())
                            .build();

                    user = userRepository.save(user);
                }
            }

            // Đăng nhập thủ công vào Spring Security Context
            CustomUserDetails userDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);

            createSession(user, jwt);

            boolean isExpired = false;
            if (user.getRole() == Role.LANDLORD) {
                java.time.LocalDate now = java.time.LocalDate.now();
                java.time.LocalDateTime regDate = user.getCreatedAt() != null ? user.getCreatedAt() : java.time.LocalDateTime.now().minusDays(50);
                
                boolean trialActive = regDate.plusDays(45).isAfter(java.time.LocalDateTime.now());
                boolean subscriptionActive = user.getSubscriptionExpiredAt() != null && !user.getSubscriptionExpiredAt().isBefore(now);
                isExpired = !trialActive && !subscriptionActive;
            }

            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getRole().name(), isExpired);

        } catch (Exception e) {
            throw new RuntimeException("Đăng nhập bằng Google thất bại: " + e.getMessage());
        }
    }
}
