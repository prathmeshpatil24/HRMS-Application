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
@Schema(description = "Generic message response")
public class MessageResponse {

    @Schema(example = "Operation completed successfully", description = "Response message")
    private String message;
}
