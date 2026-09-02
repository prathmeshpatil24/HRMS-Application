package com.hrms.auth.service;

import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.User;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(Long userId);

    RefreshToken createRefreshToken(Long userId, String deviceInfo, String ipAddress);

    RefreshToken verifyExpiration(RefreshToken token);

    RefreshToken rotateRefreshToken(RefreshToken oldToken);

    RefreshToken rotateRefreshToken(RefreshToken oldToken, String deviceInfo, String ipAddress);

    void revokeRefreshToken(String token);

    void revokeAllUserTokens(User user);

    void revokeSessionById(Long sessionId, User user);

    List<RefreshToken> getActiveSessions(User user);

    Optional<RefreshToken> findByToken(String token);
}
