package com.hrms.auth.service;

import com.hrms.auth.dto.*;
import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.service.impl.AuthServiceImpl;
import com.hrms.common.exception.BadRequestException;
import com.hrms.common.exception.DuplicateResourceException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.security.jwt.JwtProperties;
import com.hrms.security.jwt.JwtTokenProvider;
import com.hrms.security.user.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role employeeRole;
    private User user;

    @BeforeEach
    void setUp() {
        employeeRole = Role.builder()
                .id(1L)
                .name("ROLE_EMPLOYEE")
                .description("Employee Role")
                .permissions(new HashSet<>())
                .build();

        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .email("john.doe@hrms.com")
                .password("encoded_pass")
                .isActive(true)
                .accountNonLocked(true)
                .roles(new HashSet<>(Set.of(employeeRole)))
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .email("john.doe@hrms.com")
                .password("P@ssword123!")
                .build();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals("johndoe", response.getUser().getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException if username is already taken")
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .email("john.doe@hrms.com")
                .password("P@ssword123!")
                .build();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate user and generate tokens with device info")
    void login_Success() {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("johndoe")
                .password("P@ssword123!")
                .deviceInfo("Chrome on Windows 11")
                .build();

        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("sample-refresh-token")
                .user(user)
                .deviceInfo("Chrome on Windows 11")
                .ipAddress("127.0.0.1")
                .expiryDate(Instant.now().plusSeconds(604800))
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("sample-jwt-access-token");
        when(refreshTokenService.createRefreshToken(eq(1L), any(), any())).thenReturn(refreshToken);
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(user));
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);

        AuthResponse authResponse = authService.login(loginRequest, null);

        assertNotNull(authResponse);
        assertEquals("sample-jwt-access-token", authResponse.getAccessToken());
        assertEquals("sample-refresh-token", authResponse.getRefreshToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals("johndoe", authResponse.getUser().getUsername());
    }

    @Test
    @DisplayName("Should successfully logout and revoke current device refresh token")
    void logout_Success() {
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .user(user)
                .build();

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(Optional.of(refreshToken));

        authService.logout("valid-refresh-token");

        verify(refreshTokenService, times(1)).revokeRefreshToken("valid-refresh-token");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should throw BadRequestException if refresh token belongs to different user on logout")
    void logout_WrongUser_ThrowsException() {
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User otherUser = User.builder().id(99L).username("otheruser").build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(2L)
                .token("other-user-token")
                .user(otherUser)
                .build();

        when(refreshTokenService.findByToken("other-user-token")).thenReturn(Optional.of(refreshToken));

        assertThrows(BadRequestException.class, () -> authService.logout("other-user-token"));
        verify(refreshTokenService, never()).revokeRefreshToken("other-user-token");
    }

    @Test
    @DisplayName("Should successfully retrieve all active device sessions for current user")
    void getActiveSessions_Success() {
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshToken session1 = RefreshToken.builder()
                .id(1L)
                .user(user)
                .deviceInfo("Chrome on Windows")
                .ipAddress("192.168.1.10")
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        RefreshToken session2 = RefreshToken.builder()
                .id(2L)
                .user(user)
                .deviceInfo("HRMS Mobile App on iOS")
                .ipAddress("192.168.1.20")
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenService.getActiveSessions(user)).thenReturn(List.of(session1, session2));

        List<UserSessionResponse> activeSessions = authService.getActiveSessions();

        assertNotNull(activeSessions);
        assertEquals(2, activeSessions.size());
        assertEquals("Chrome on Windows", activeSessions.get(0).getDeviceInfo());
        assertEquals("HRMS Mobile App on iOS", activeSessions.get(1).getDeviceInfo());
    }

    @Test
    @DisplayName("Should successfully logout from all devices")
    void logoutAll_Success() {
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.logoutAll();

        verify(refreshTokenService, times(1)).revokeAllUserTokens(user);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
