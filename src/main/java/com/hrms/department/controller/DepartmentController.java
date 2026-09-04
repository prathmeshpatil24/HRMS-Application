package com.hrms.department.controller;

import com.hrms.department.dto.CreateDepartmentRequest;
import com.hrms.department.dto.DepartmentResponse;
import com.hrms.department.dto.UpdateDepartmentRequest;
import com.hrms.department.dto.UpdateDepartmentStatusRequest;
import com.hrms.department.entity.DepartmentStatus;
import com.hrms.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/departments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Department Management", description = "Manage the single-company department master data")
@SecurityRequirement(name = "Bearer Authentication")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping( value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR') and hasAuthority('DEPARTMENT_WRITE')")
    @Operation(summary = "Create a department", description = "Creates an active department. Department code and name must be unique.")
    public ResponseEntity<?> createDepartment(@Valid @RequestBody CreateDepartmentRequest request,
                                              HttpServletRequest httpServletRequest)
    {
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Department created successfully",
                        "data", response,
                        "status", HttpStatus.CREATED,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpServletRequest.getRequestURI()
                ));
    }

    // need to differentiate between the two GET endpoints, one for all departments and one for departments by status. We can use a different path for the second endpoint.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER') and hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "List departments", description = "Returns all departments ordered by name, including active and inactive records.")
    public ResponseEntity<?> getAllDepartments(HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of(
                        "message", "Departments retrieved successfully",
                        "data", departmentService.getAllDepartments(),
                        "status", HttpStatus.OK,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpServletRequest.getRequestURI()
                ));
    }

    @GetMapping("/by-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER') and hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "List departments by status", description = "Returns all departments filtered by status (ACTIVE or INACTIVE) and ordered by name.")
    public ResponseEntity<?> getAllDepartmentsByStatus(@RequestParam("status") String status, HttpServletRequest httpServletRequest) {

        List<DepartmentResponse> allDepartmentsByStatus = departmentService.getAllDepartmentsByStatus(DepartmentStatus.valueOf(status));

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of(
                        "message", "Departments retrieved successfully",
                        "data",  allDepartmentsByStatus,
                        "status", HttpStatus.OK,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpServletRequest.getRequestURI()
                ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER') and hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "Get a department", description = "Returns a department by its identifier.")
    public ResponseEntity<?> getDepartmentById(@PathVariable @Positive Long id, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of(
                        "message", "Department retrieved successfully",
                        "data", departmentService.getDepartmentById(id),
                        "status", HttpStatus.OK,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpServletRequest.getRequestURI()
                ));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR') and hasAuthority('DEPARTMENT_WRITE')")
    @Operation(summary = "Update department details", description = "Updates code, name, and description. Use the status endpoint to activate or deactivate a department.")
    public ResponseEntity<?> updateDepartment(@PathVariable @Positive Long id,
                                              @Valid @RequestBody UpdateDepartmentRequest request,
                                              HttpServletRequest httpServletRequest) {
        DepartmentResponse departmentResponse = departmentService.updateDepartment(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of(
                        "message", "Department updated successfully",
                        "data", departmentResponse,
                        "status", HttpStatus.OK,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpServletRequest.getRequestURI()
                ));
    }

    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR') and hasAuthority('DEPARTMENT_WRITE')")
    @Operation(summary = "Change department status", description = "Activates or deactivates a department without altering its master data.")
    public ResponseEntity<?> updateDepartmentStatus(@PathVariable @Positive Long id,
                                                    @Valid @RequestBody UpdateDepartmentStatusRequest request,
                                                    HttpServletRequest httpServletRequest) {
        DepartmentResponse departmentResponse = departmentService.updateDepartmentStatus(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of(
                        "message", "Department status updated successfully",
                        "data", departmentResponse,
                        "status", HttpStatus.OK,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpServletRequest.getRequestURI()
                ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR') and hasAuthority('DEPARTMENT_WRITE')")
    @Operation(summary = "Delete a department", description = "Permanently removes a department. Deactivate it instead when historical retention is required.")
    public ResponseEntity<?> deleteDepartment(@PathVariable @Positive Long id,
                                              HttpServletRequest httpServletRequest) {
        departmentService.deleteDepartment(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(Map.of(
                        "message", "Department deleted successfully",
                        "status", HttpStatus.NO_CONTENT,
                        "timestamp", java.time.LocalDateTime.now(),
                        "path", httpServletRequest.getRequestURI()
                ));
    }
}
