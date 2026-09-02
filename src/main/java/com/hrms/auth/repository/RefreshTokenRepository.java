package com.hrms.auth.repository;

import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Query("SELECT r FROM RefreshToken r JOIN FETCH r.user u LEFT JOIN FETCH u.roles WHERE r.token = :token")
    Optional<RefreshToken> findByTokenWithUser(@Param("token") String token);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    List<RefreshToken> findByUserAndRevokedFalseOrderByCreatedAtDesc(User user);

    Optional<RefreshToken> findByIdAndUser(Long id, User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :revokedAt WHERE r.user = :user AND r.revoked = false")
    void revokeAllUserTokens(@Param("user") User user, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now OR r.revoked = true")
    int deleteExpiredOrRevokedTokens(@Param("now") Instant now);

    int deleteByUser(User user);
}
