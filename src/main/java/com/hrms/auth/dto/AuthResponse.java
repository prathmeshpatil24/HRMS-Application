package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication token response")
public class AuthResponse {

    @Schema(description = "JWT Access Token for API requests")
    private String accessToken;

    @Schema(description = "Refresh Token for renewing access tokens")
    private String refreshToken;

    @Builder.Default
    @Schema(example = "Bearer", description = "Token type")
    private String tokenType = "Bearer";

    @Schema(example = "86400000", description = "Access token expiry duration in milliseconds")
    private Long expiresIn;

    @Schema(description = "Authenticated user profile")
    private UserResponse user;
}
