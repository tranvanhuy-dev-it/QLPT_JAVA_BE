package com.qlpt.backend;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.qlpt.backend.config.JwtTokenProvider;
import com.qlpt.backend.dto.auth.GoogleLoginRequest;
import com.qlpt.backend.dto.auth.JwtResponse;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.repository.UserRepository;
import com.qlpt.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    public void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "googleClientId", "mock-client-id");
    }

    @Test
    public void testAuthenticateGoogleUser_NewUser_Success() {
        GoogleLoginRequest request = new GoogleLoginRequest("mock-token");

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getSubject()).thenReturn("google-sub-123");
        when(payload.getEmail()).thenReturn("newuser@gmail.com");
        when(payload.get("name")).thenReturn("New User");

        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("encoded-random-pwd");
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(tokenProvider.generateToken(any())).thenReturn("mock-jwt-token");

        try (MockedConstruction<GoogleIdTokenVerifier> mocked = mockConstruction(GoogleIdTokenVerifier.class,
                (mock, context) -> {
                    when(mock.verify("mock-token")).thenReturn(idToken);
                })) {

            JwtResponse response = authService.authenticateGoogleUser(request);

            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("newuser", response.getUsername());
            assertEquals("LANDLORD", response.getRole());
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Test
    public void testAuthenticateGoogleUser_ExistingLandlord_Success() {
        GoogleLoginRequest request = new GoogleLoginRequest("mock-token");
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .username("existinglandlord")
                .email("existing@gmail.com")
                .role(Role.LANDLORD)
                .googleId("google-sub-123")
                .status("ACTIVE")
                .build();

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getSubject()).thenReturn("google-sub-123");
        when(payload.getEmail()).thenReturn("existing@gmail.com");

        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.of(existingUser));
        when(tokenProvider.generateToken(any())).thenReturn("mock-jwt-token");

        try (MockedConstruction<GoogleIdTokenVerifier> mocked = mockConstruction(GoogleIdTokenVerifier.class,
                (mock, context) -> {
                    when(mock.verify("mock-token")).thenReturn(idToken);
                })) {

            JwtResponse response = authService.authenticateGoogleUser(request);

            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("existinglandlord", response.getUsername());
            assertEquals("LANDLORD", response.getRole());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Test
    public void testAuthenticateGoogleUser_ExistingTenant_Success() {
        GoogleLoginRequest request = new GoogleLoginRequest("mock-token");
        User existingTenant = User.builder()
                .id(UUID.randomUUID())
                .username("existingtenant")
                .email("tenant@gmail.com")
                .role(Role.TENANT)
                .googleId("google-sub-123")
                .status("ACTIVE")
                .build();

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getSubject()).thenReturn("google-sub-123");
        when(payload.getEmail()).thenReturn("tenant@gmail.com");

        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.of(existingTenant));
        when(tokenProvider.generateToken(any())).thenReturn("mock-jwt-token");

        try (MockedConstruction<GoogleIdTokenVerifier> mocked = mockConstruction(GoogleIdTokenVerifier.class,
                (mock, context) -> {
                    when(mock.verify("mock-token")).thenReturn(idToken);
                })) {

            JwtResponse response = authService.authenticateGoogleUser(request);

            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("existingtenant", response.getUsername());
            assertEquals("TENANT", response.getRole());
            verify(userRepository, never()).save(any(User.class));
        }
    }
}
