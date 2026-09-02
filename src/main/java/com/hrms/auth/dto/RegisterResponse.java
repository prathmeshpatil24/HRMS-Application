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
@Schema(description = "User registration response payload")
public class RegisterResponse {

    @Schema(example = "User registered successfully", description = "Registration status message")
    private String message;

    @Schema(description = "Registered user details")
    private UserResponse user;
}
