package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile details response")
public class UserResponse {

    @Schema(example = "1", description = "User unique ID")
    private Long id;

    @Schema(example = "John", description = "First name")
    private String firstName;

    @Schema(example = "Doe", description = "Last name")
    private String lastName;

    @Schema(example = "John Doe", description = "Full name")
    private String fullName;

    @Schema(example = "johndoe", description = "Username")
    private String username;

    @Schema(example = "john.doe@company.com", description = "Email address")
    private String email;

    @Schema(example = "[\"ROLE_EMPLOYEE\"]", description = "Assigned roles")
    private Set<String> roles;

    @Schema(example = "[\"EMPLOYEE_READ\"]", description = "Assigned permissions")
    private Set<String> permissions;

    @Schema(example = "true", description = "Account active status")
    private boolean active;

    @Schema(example = "true", description = "Account unlocked status")
    private boolean accountNonLocked;

    @Schema(description = "Timestamp when user account was created")
    private LocalDateTime createdAt;
}
