package com.hrms.auth.service.impl;

import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RefreshTokenRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.service.RefreshTokenService;
import com.hrms.common.exception.BadRequestException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.TokenRefreshException;
import com.hrms.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        return createRefreshToken(userId, "Unknown Device", "127.0.0.1");
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId, String deviceInfo, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Instant now = Instant.now();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .expiryDate(now.plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .lastUsedAt(now)
                .createdAt(now)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new TokenRefreshException(token.getToken(), "Refresh token has been revoked. Please sign in again.");
        }

        if (token.isExpired()) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new login request.");
        }

        return token;
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        return rotateRefreshToken(oldToken, oldToken.getDeviceInfo(), oldToken.getIpAddress());
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, String deviceInfo, String ipAddress) {
        Instant now = Instant.now();

        // Mark old token as revoked and replaced
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(now);
        oldToken.setLastUsedAt(now);

        String effectiveDeviceInfo = (deviceInfo != null && !deviceInfo.isBlank()) ? deviceInfo : oldToken.getDeviceInfo();
        String effectiveIpAddress = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : oldToken.getIpAddress();

        // Create new replacement token for the same device session
        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .expiryDate(now.plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .deviceInfo(effectiveDeviceInfo)
                .ipAddress(effectiveIpAddress)
                .lastUsedAt(now)
                .createdAt(now)
                .build();

        newToken = refreshTokenRepository.save(newToken);
        oldToken.setReplacedByToken(newToken.getToken());
        refreshTokenRepository.save(oldToken);

        return newToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
            log.info("Revoked refresh token for user [{}] on device [{}]", rt.getUser().getUsername(), rt.getDeviceInfo());
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user, Instant.now());
        log.info("Revoked all active refresh tokens for user [{}]", user.getUsername());
    }

    @Override
    @Transactional
    public void revokeSessionById(Long sessionId, User user) {
        RefreshToken token = refreshTokenRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        if (token.isRevoked()) {
            throw new BadRequestException("Session is already terminated / revoked");
        }

        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
        log.info("User [{}] remotely revoked session [{}] on device [{}]", user.getUsername(), sessionId, token.getDeviceInfo());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveSessions(User user) {
        return refreshTokenRepository.findByUserAndRevokedFalseOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByTokenWithUser(token);
    }
}
