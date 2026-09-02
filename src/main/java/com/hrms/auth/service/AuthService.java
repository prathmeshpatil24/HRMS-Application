package com.hrms.auth.service;

import com.hrms.auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    TokenRefreshResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest);

    void logout(String refreshToken);

    void logoutAll();

    List<UserSessionResponse> getActiveSessions();

    void revokeSession(Long sessionId);

    UserResponse getCurrentUserProfile();

    void changePassword(ChangePasswordRequest request);
}
