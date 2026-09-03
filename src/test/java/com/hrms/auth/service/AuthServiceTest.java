package com.hrms.auth.service;

import com.hrms.auth.dto.AuthResponse;
import com.hrms.auth.dto.LoginActivityResponse;
import com.hrms.auth.dto.LoginRequest;
import com.hrms.auth.dto.RegisterRequest;
import com.hrms.auth.dto.RegisterResponse;
import com.hrms.auth.entity.LoginActivity;
import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.LoginActivityRepository;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.service.impl.AuthServiceImpl;
import com.hrms.security.jwt.JwtProperties;
import com.hrms.security.jwt.JwtTokenProvider;
import com.hrms.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private LoginActivityRepository loginActivityRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtProperties jwtProperties;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        Role employeeRole = Role.builder().id(1L).name("ROLE_EMPLOYEE").permissions(new HashSet<>()).build();
        user = User.builder().id(1L).firstName("John").lastName("Doe").username("johndoe")
                .email("john.doe@hrms.com").password("encoded_pass").isActive(true)
                .accountNonLocked(true).roles(new HashSet<>(List.of(employeeRole))).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_returnsOnlyAccessTokenAndPersistsDeviceAndIpAudit() {
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("sample-access-token");
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(user));
        when(jwtProperties.getExpirationMs()).thenReturn(86_400_000L);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.10");

        AuthResponse response = authService.login(LoginRequest.builder()
                .usernameOrEmail("johndoe").password("P@ssword123!").build(), httpRequest);

        assertEquals("sample-access-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        ArgumentCaptor<LoginActivity> activityCaptor = ArgumentCaptor.forClass(LoginActivity.class);
        verify(loginActivityRepository).save(activityCaptor.capture());
        assertEquals("Mozilla/5.0", activityCaptor.getValue().getDeviceInfo());
        assertEquals("192.168.1.10", activityCaptor.getValue().getIpAddress());
    }

    @Test
    void getLoginHistory_returnsSavedAuditEntriesForAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CustomUserDetails.build(user), null, CustomUserDetails.build(user).getAuthorities()));
        LoginActivity activity = LoginActivity.builder().id(10L).user(user).deviceInfo("Chrome")
                .ipAddress("203.0.113.10").loggedInAt(Instant.parse("2026-09-03T10:00:00Z")).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(loginActivityRepository.findByUserOrderByLoggedInAtDesc(user)).thenReturn(List.of(activity));

        List<LoginActivityResponse> history = authService.getLoginHistory();

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Chrome", history.get(0).getDeviceInfo());
        assertEquals("203.0.113.10", history.get(0).getIpAddress());
    }

    @Test
    void logout_marksMatchingOpenLoginActivityWithLogoutTimestamp() {
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()));
        LoginActivity activity = LoginActivity.builder().id(10L).user(user).deviceInfo("Mozilla/5.0")
                .ipAddress("192.168.1.10").loggedInAt(Instant.now()).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.10");
        when(loginActivityRepository.findFirstByUserAndDeviceInfoAndIpAddressAndLogoutAtIsNullOrderByLoggedInAtDesc(
                user, "Mozilla/5.0", "192.168.1.10")).thenReturn(Optional.of(activity));

        authService.logout(httpRequest);

        assertNotNull(activity.getLogoutAt());
        verify(loginActivityRepository).findFirstByUserAndDeviceInfoAndIpAddressAndLogoutAtIsNullOrderByLoggedInAtDesc(
                user, "Mozilla/5.0", "192.168.1.10");
    }

    @Test
    void register_usesDefaultEmployeeRole() {
        Role role = Role.builder().id(1L).name("ROLE_EMPLOYEE").permissions(new HashSet<>()).build();
        RegisterRequest request = RegisterRequest.builder().firstName("John").lastName("Doe").username("johndoe")
                .email("john.doe@hrms.com").password("P@ssword123!").build();
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@hrms.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        RegisterResponse response = authService.register(request);

        assertEquals("johndoe", response.getUser().getUsername());
    }
}
