package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refresh token payload for renewing access tokens")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", description = "Valid refresh token string")
    private String refreshToken;
}
