package com.hrms.auth.controller;

import com.hrms.auth.dto.*;
import com.hrms.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Endpoints for user registration, JWT authentication, profiles, and login auditing")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with specified roles or default ROLE_EMPLOYEE.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest requestUrl) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "success", true,
                        "status", HttpStatus.CREATED,
                        "message", "User registered successfully",
                        "data", response,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", requestUrl.getRequestURI()
                ));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT access token", description = "Authenticates user credentials, records device and IP metadata, and returns an access token only.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of(
                        "success", true,
                        "status", HttpStatus.OK,
                        "message", "Login successful",
                        "data", response,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpRequest.getRequestURI()
                ));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer Authentication") // @SecurityRequirement is a Swagger/OpenAPI annotation. It tells Swagger UI:
    // This API endpoint requires authentication/security
    @Operation(summary = "Logout", description = "With stateless JWT authentication, this endpoint confirms logout; the client must discard its access token.")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of(
                        "success", true,
                        "status", HttpStatus.OK,
                        "message", "Logout successful",
                        "data", new MessageResponse("Logged out successfully. Discard the access token on the client."),
                        "timestamp", java.time.LocalDateTime.now()
                        //"path", "/logout"
                ));
    }


    @GetMapping("/login-history")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get login history", description = "Retrieves device and IP address information for successful logins.")
    public ResponseEntity<?> getLoginHistory(HttpServletRequest request) {
        List<LoginActivityResponse> history = authService.getLoginHistory();

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("success", true,
                        "status", HttpStatus.OK,
                        "message", "Login history retrieved successfully",
                        "data", history,
                        "timestamp", java.time.LocalDateTime.now(), "path", request.getRequestURI()));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current authenticated user profile", description = "Returns details and assigned roles/permissions of the currently authenticated user.")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest httpRequest) {
        UserResponse user = authService.getCurrentUserProfile();

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("success", true,
                        "status", HttpStatus.OK,
                        "message", "Current user profile retrieved successfully",
                        "data", user,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpRequest.getRequestURI()));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Change password", description = "Changes password for the currently authenticated user.")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        authService.changePassword(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("success", true,
                        "status", HttpStatus.OK,
                        "message", "Password changed successfully",
                        "data", new MessageResponse("Password has been updated successfully"),
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpRequest.getRequestURI()));
    }
}
