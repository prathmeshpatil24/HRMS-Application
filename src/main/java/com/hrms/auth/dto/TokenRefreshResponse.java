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
@Schema(description = "Token refresh response")
public class TokenRefreshResponse {

    @Schema(description = "New JWT Access Token")
    private String accessToken;

    @Schema(description = "Rotated Refresh Token")
    private String refreshToken;

    @Builder.Default
    @Schema(example = "Bearer", description = "Token type")
    private String tokenType = "Bearer";

    @Schema(example = "86400000", description = "Access token expiry duration in milliseconds")
    private Long expiresIn;
}
