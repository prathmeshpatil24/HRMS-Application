package com.hrms.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Value("${server.port:8085}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Human Resource Management System (HRMS) REST API")
                        .description("### Production-Grade HRMS Backend API Documentation\n\n"
                                + "This API provides enterprise capabilities for Human Resource Management:\n"
                                + "- **Authentication & Authorization**: Spring Security 6 + JWT access tokens\n"
                                + "- **Role-Based Access Control (RBAC)**: Fine-grained roles (`ROLE_ADMIN`, `ROLE_HR`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`) and permissions\n"
                                + "- **Standard Responses**: Unified `ApiResponse<T>` & `ApiErrorResponse` envelopes\n\n"
                                + "**Authentication Instructions**:\n"
                                + "1. Obtain a JWT token by calling `POST /api/v1/auth/login` (default admin credentials: `admin` / `Admin@123456`).\n"
                                + "2. Click the **Authorize** button on the top right.\n"
                                + "3. Enter your JWT token in the Value field (e.g. `eyJhbGciOi...`)."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HRMS Engineering Team")
                                .email("engineering@hrms.com")
                                .url("https://github.com/prathmeshpatil24/HRMS-Application"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server"),
                        new Server().url("/").description("Current Host Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer Token authentication. Enter only the token string without 'Bearer ' prefix.")));
    }

    @Bean
    public GroupedOpenApi authGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("1-Authentication-Module")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("2-All-HRMS-APIs")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
