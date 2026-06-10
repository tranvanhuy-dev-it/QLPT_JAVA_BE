package com.qlpt.backend.service;

import com.qlpt.backend.dto.auth.GoogleLoginRequest;
import com.qlpt.backend.dto.auth.JwtResponse;
import com.qlpt.backend.dto.auth.LoginRequest;
import com.qlpt.backend.dto.auth.RegisterRequest;
import com.qlpt.backend.dto.user.TenantCreateRequest;
import com.qlpt.backend.entity.User;

public interface AuthService {
    public void initDefaultAdmin();

    public User registerUser(RegisterRequest request);

    public JwtResponse authenticateUser(LoginRequest request);

    public User createTenantAccount(TenantCreateRequest request, User landlord);

    public JwtResponse authenticateGoogleUser(GoogleLoginRequest request);
}
