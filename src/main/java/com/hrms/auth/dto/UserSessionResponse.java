package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User active device session information")
public class UserSessionResponse {

    @Schema(example = "1", description = "Session ID")
    private Long id;

    @Schema(example = "Chrome on Windows 11", description = "Device or browser identification")
    private String deviceInfo;

    @Schema(example = "192.168.1.100", description = "Client IP address from which session was initiated")
    private String ipAddress;

    @Schema(description = "Timestamp when the session was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when the session / refresh token was last used")
    private Instant lastUsedAt;

    @Schema(description = "Timestamp when the session expires")
    private Instant expiryDate;

    @Schema(example = "true", description = "Indicates whether the session is currently active")
    private boolean active;
}
