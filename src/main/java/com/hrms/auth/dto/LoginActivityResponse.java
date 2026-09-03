package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Successful login audit record")
public class LoginActivityResponse {

    private Long id;
    private String deviceInfo;
    private String ipAddress;
    private Instant loggedInAt;
}
