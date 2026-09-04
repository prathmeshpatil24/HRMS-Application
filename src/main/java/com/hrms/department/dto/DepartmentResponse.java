package com.hrms.department.dto;

import com.hrms.department.entity.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Department details")
public class DepartmentResponse {

    @Schema(example = "1")
    private Long departmentId;

    @Schema(example = "HR")
    private String code;

    @Schema(example = "Human Resources")
    private String name;

    @Schema(example = "Responsible for people operations and talent management")
    private String description;

    @Schema(example = "ACTIVE")
    private DepartmentStatus status;

    @Schema(description = "Identifier of the user who created the department")
    private Long createdBy;

    @Schema(description = "Timestamp when the department was created")
    private LocalDateTime createdAt;

    @Schema(description = "Identifier of the user who last updated the department")
    private Long updatedBy;

    @Schema(description = "Timestamp when the department was last updated")
    private LocalDateTime updatedAt;
}
