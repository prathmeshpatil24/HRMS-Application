package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password change payload")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(example = "OldP@ssword123!", description = "Current existing user password")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,}$",
            message = "New password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(example = "NewP@ssword123!", description = "New password conforming to security policy")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Schema(example = "NewP@ssword123!", description = "Confirmation of the new password")
    private String confirmPassword;
}
