package com.hrms.department.dto;

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
@Schema(description = "Payload for creating a department")
public class CreateDepartmentRequest {

    @NotBlank(message = "Department code is required")
    @Size(min = 2, max = 20, message = "Department code must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Department code can contain only letters, numbers, and hyphens")
    @Schema(example = "HR", description = "Unique department business code")
    private String code;

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    @Schema(example = "Human Resources", description = "Unique department name")
    private String name;

    @Size(max = 500, message = "Department description cannot exceed 500 characters")
    @Schema(example = "Responsible for people operations and talent management")
    private String description;
}
