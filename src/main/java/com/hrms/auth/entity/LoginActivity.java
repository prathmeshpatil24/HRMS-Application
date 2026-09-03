package com.hrms.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Records successful sign-ins without storing a reusable authentication token. */
@Entity
@Table(name = "login_activities", indexes = {
        @Index(name = "idx_login_activities_user_id_logged_in_at", columnList = "user_id,logged_in_at")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_login_activities_user"))
    private User user;

    @Column(name = "device_info", nullable = false, length = 512)
    private String deviceInfo;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "logged_in_at", nullable = false, updatable = false)
    private Instant loggedInAt;
}
