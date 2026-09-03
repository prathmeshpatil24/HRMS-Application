package com.hrms.auth.service.impl;

import com.hrms.auth.dto.*;
import com.hrms.auth.entity.Permission;
import com.hrms.auth.entity.LoginActivity;
import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.repository.LoginActivityRepository;
import com.hrms.auth.service.AuthService;
import com.hrms.common.exception.BadRequestException;
import com.hrms.common.exception.DuplicateResourceException;
import com.hrms.common.exception.ResourceNotFoundException;
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
import java.time.Instant;
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
    private final LoginActivityRepository loginActivityRepository;
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
                //.message("User registered successfully")
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

        User user = userRepository.findByIdWithRolesAndPermissions(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        String deviceInfo = resolveDeviceInfo(request, httpRequest);
        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);


        // Record the login activity
        loginActivityRepository.save(LoginActivity.builder()
                .user(user)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .loggedInAt(Instant.now())
                .build());

        log.info("User [{}] logged in successfully from device: [{}] (IP: {})", user.getUsername(), deviceInfo, ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest httpRequest) {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));
        String deviceInfo = HttpRequestUtils.getUserAgent(httpRequest);
        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);

        loginActivityRepository
                .findFirstByUserAndDeviceInfoAndIpAddressAndLogoutAtIsNullOrderByLoggedInAtDesc(user, deviceInfo, ipAddress)
                .ifPresent(activity -> activity.markLoggedOut(Instant.now()));

        SecurityContextHolder.clearContext();
        log.info("User [{}] logged out. The client must discard its access token.", userDetails.getUsername());
    }


    @Override
    @Transactional(readOnly = true)
    public List<LoginActivityResponse> getLoginHistory() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        List<LoginActivityResponse> loginActivityResponse = loginActivityRepository.findByUserOrderByLoggedInAtDesc(user).stream()
                .map(activity -> LoginActivityResponse.builder()
                        .id(activity.getId())
                        .deviceInfo(activity.getDeviceInfo())
                        .ipAddress(activity.getIpAddress())
                        .loggedInAt(activity.getLoggedInAt())
                        .logoutAt(activity.getLogoutAt())
                        .build()
                )
                .toList();
        return loginActivityResponse;
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

        log.info("Password changed successfully for user [{}]", user.getUsername());
    }

    private String resolveDeviceInfo(LoginRequest request, HttpServletRequest httpRequest) {
        if (request.getDeviceInfo() != null && !request.getDeviceInfo().isBlank()) {
            return request.getDeviceInfo().trim();
        }
        return HttpRequestUtils.getUserAgent(httpRequest);
    }

    // Helper method to retrieve the currently authenticated user's details
    private CustomUserDetails getAuthenticatedUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated. Valid JWT Bearer token is required.");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    // Helper method to map User entity to UserResponse DTO
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
