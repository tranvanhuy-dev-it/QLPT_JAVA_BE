package com.qlpt.backend.service;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.config.JwtTokenProvider;
import com.qlpt.backend.dto.auth.JwtResponse;
import com.qlpt.backend.dto.auth.LoginRequest;
import com.qlpt.backend.dto.auth.RegisterRequest;
import com.qlpt.backend.dto.user.TenantCreateRequest;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    public void initDefaultAdmin();
    public User registerUser(RegisterRequest request);
    public JwtResponse authenticateUser(LoginRequest request);
    public User createTenantAccount(TenantCreateRequest request, User landlord);
}
