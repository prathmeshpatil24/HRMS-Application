package com.hrms.auth.service.impl;

import com.hrms.auth.dto.*;
import com.hrms.auth.entity.Permission;
import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.service.AuthService;
import com.hrms.auth.service.RefreshTokenService;
import com.hrms.common.exception.BadRequestException;
import com.hrms.common.exception.DuplicateResourceException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.TokenRefreshException;
import com.hrms.common.exception.UnauthorizedException;
import com.hrms.common.util.HttpRequestUtils;
import com.hrms.security.jwt.JwtProperties;
import com.hrms.security.jwt.JwtTokenProvider;
import com.hrms.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "ROLE_EMPLOYEE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Processing user registration for username: [{}], email: [{}]", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", DEFAULT_ROLE));
            roles.add(defaultRole);
        } else {
            for (String roleName : request.getRoles()) {
                String formattedRoleName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                Role role = roleRepository.findByName(formattedRoleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", formattedRoleName));
                roles.add(role);
            }
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .accountNonLocked(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: [{}]", savedUser.getId());

        return RegisterResponse.builder()
                .message("User registered successfully")
                .user(mapToUserResponse(savedUser))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing login request for: [{}]", request.getUsernameOrEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail().trim(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateToken(authentication);

        String deviceInfo = (request.getDeviceInfo() != null && !request.getDeviceInfo().isBlank())
                ? request.getDeviceInfo().trim()
                : HttpRequestUtils.getUserAgent(httpRequest);
        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId(), deviceInfo, ipAddress);

        User user = userRepository.findByIdWithRolesAndPermissions(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        log.info("User [{}] logged in successfully from device: [{}] (IP: {})", user.getUsername(), deviceInfo, ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String tokenStr = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(tokenStr)
                .orElseThrow(() -> new TokenRefreshException(tokenStr, "Refresh token is not registered in system"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        String deviceInfo = HttpRequestUtils.getUserAgent(httpRequest);
        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);

        RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(refreshToken, deviceInfo, ipAddress);

        User user = rotatedToken.getUser();
        CustomUserDetails userDetails = CustomUserDetails.build(user);

        String newAccessToken = jwtTokenProvider.generateTokenForUserDetails(userDetails);

        log.info("Refreshed access token successfully for user [{}] on device [{}]", user.getUsername(), rotatedToken.getDeviceInfo());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotatedToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();

        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new BadRequestException("Refresh token is required to logout.");
        }

        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", "token", refreshTokenStr));

        if (!refreshToken.getUser().getId().equals(userDetails.getId())) {
            throw new BadRequestException("The refresh token does not belong to the currently authenticated user.");
        }

        refreshTokenService.revokeRefreshToken(refreshTokenStr);
        SecurityContextHolder.clearContext();
        log.info("User [{}] logged out from device session [{}]", userDetails.getUsername(), refreshToken.getDeviceInfo());
    }

    @Override
    @Transactional
    public void logoutAll() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        refreshTokenService.revokeAllUserTokens(user);
        SecurityContextHolder.clearContext();
        log.info("User [{}] logged out from ALL active devices/sessions", userDetails.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSessionResponse> getActiveSessions() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        return refreshTokenService.getActiveSessions(user).stream()
                .map(token -> UserSessionResponse.builder()
                        .id(token.getId())
                        .deviceInfo(token.getDeviceInfo())
                        .ipAddress(token.getIpAddress())
                        .createdAt(token.getCreatedAt())
                        .lastUsedAt(token.getLastUsedAt())
                        .expiryDate(token.getExpiryDate())
                        .active(!token.isRevoked() && !token.isExpired())
                        .build()
                )
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(Long sessionId) {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        refreshTokenService.revokeSessionById(sessionId, user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();

        User user = userRepository.findByIdWithRolesAndPermissions(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be identical to the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all active sessions / refresh tokens
        refreshTokenService.revokeAllUserTokens(user);

        log.info("Password changed successfully for user [{}]", user.getUsername());
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("User is not authenticated. Valid JWT Bearer token is required.");
        }
        return userDetails;
    }

    private UserResponse mapToUserResponse(User user) {
        Set<String> roleNames = new HashSet<>();
        Set<String> permissionNames = new HashSet<>();

        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                roleNames.add(role.getName());
                if (role.getPermissions() != null) {
                    permissionNames.addAll(
                            role.getPermissions().stream()
                                    .map(Permission::getName)
                                    .collect(Collectors.toSet())
                    );
                }
            }
        }

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(String.format("%s %s", user.getFirstName(), user.getLastName()).trim())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .permissions(permissionNames)
                .active(user.isActive())
                .accountNonLocked(user.isAccountNonLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
