package com.hrms.department.dto;

import com.hrms.department.entity.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for changing a department lifecycle status")
public class UpdateDepartmentStatusRequest {

    @NotNull(message = "Department status is required")
    @Schema(example = "INACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    private DepartmentStatus status;
}
