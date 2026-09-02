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
@Schema(description = "User login credentials payload")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(example = "johndoe", description = "Username or registered email address")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Schema(example = "P@ssword123!", description = "User password")
    private String password;

    @Schema(example = "Chrome on Windows 11", description = "Optional device identification")
    private String deviceInfo;
}
