package com.hrms.department.service.impl;

import com.hrms.common.exception.DuplicateResourceException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.department.dto.CreateDepartmentRequest;
import com.hrms.department.dto.DepartmentResponse;
import com.hrms.department.dto.UpdateDepartmentRequest;
import com.hrms.department.dto.UpdateDepartmentStatusRequest;
import com.hrms.department.entity.Department;
import com.hrms.department.entity.DepartmentStatus;
import com.hrms.department.repository.DepartmentRepository;
import com.hrms.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String code = normalizeCode(request.getCode());
        String name = normalizeText(request.getName());

        ensureUniqueCode(code, null);
        ensureUniqueName(name, null);

        Department department = Department.builder()
                .code(code)
                .name(name)
                .description(normalizeDescription(request.getDescription()))
                .build();
        Department savedDepartment = departmentRepository.save(department);

        log.info("Created department [{}] with ID [{}]", savedDepartment.getCode(), savedDepartment.getId());
        return  mapToResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    // get all departments by status
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartmentsByStatus(DepartmentStatus status) {
        return departmentRepository.findAllByStatusOrderByNameAsc(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        return mapToResponse(findDepartmentById(id));
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = findDepartmentById(id);
        String code = normalizeCode(request.getCode());
        String name = normalizeText(request.getName());

        ensureUniqueCode(code, id);
        ensureUniqueName(name, id);

        department.setCode(code);
        department.setName(name);
        department.setDescription(normalizeDescription(request.getDescription()));
        Department savedDepartment = departmentRepository.save(department);

        log.info("Updated department [{}] with ID [{}]", savedDepartment.getCode(), savedDepartment.getId());
        return mapToResponse(savedDepartment);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartmentStatus(Long id, UpdateDepartmentStatusRequest request) {
        Department department = findDepartmentById(id);
        department.setStatus(request.getStatus());
        Department savedDepartment = departmentRepository.save(department);

        log.info("Updated department [{}] status to [{}]", savedDepartment.getId(), savedDepartment.getStatus());
        return mapToResponse(savedDepartment);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = findDepartmentById(id);
        departmentRepository.delete(department);
        log.info("Deleted department [{}] with ID [{}]", department.getCode(), id);
    }

    private Department findDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }

    private void ensureUniqueCode(String code, Long id) {
        boolean exists = id == null
                ? departmentRepository.existsByCodeIgnoreCase(code)
                : departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id);
        if (exists) {
            throw new DuplicateResourceException("Department", "code", code);
        }
    }

    private void ensureUniqueName(String name, Long id) {
        boolean exists = id == null
                ? departmentRepository.existsByNameIgnoreCase(name)
                : departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (exists) {
            throw new DuplicateResourceException("Department", "name", name);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return normalizeText(description);
    }

    // Helper method to map Department entity to DepartmentResponse DTO
    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .departmentId(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .status(department.getStatus())
                .createdBy(department.getCreatedBy())
                .createdAt(department.getCreatedAt())
                .updatedBy(department.getUpdatedBy())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
