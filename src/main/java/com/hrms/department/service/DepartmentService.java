package com.hrms.department.service;

import com.hrms.department.dto.CreateDepartmentRequest;
import com.hrms.department.dto.DepartmentResponse;
import com.hrms.department.dto.UpdateDepartmentRequest;
import com.hrms.department.dto.UpdateDepartmentStatusRequest;
import com.hrms.department.entity.DepartmentStatus;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    List<DepartmentResponse> getAllDepartments();

    List<DepartmentResponse> getAllDepartmentsByStatus(DepartmentStatus status);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request);

    DepartmentResponse updateDepartmentStatus(Long id, UpdateDepartmentStatusRequest request);

    void deleteDepartment(Long id);
}
