package com.hrms.department.service;

import com.hrms.common.exception.DuplicateResourceException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.department.dto.CreateDepartmentRequest;
import com.hrms.department.dto.DepartmentResponse;
import com.hrms.department.dto.UpdateDepartmentRequest;
import com.hrms.department.dto.UpdateDepartmentStatusRequest;
import com.hrms.department.entity.Department;
import com.hrms.department.entity.DepartmentStatus;
import com.hrms.department.repository.DepartmentRepository;
import com.hrms.department.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void createDepartment_normalizesValuesAndDefaultsToActive() {
        CreateDepartmentRequest request = CreateDepartmentRequest.builder()
                .code(" hr-01 ")
                .name(" Human   Resources ")
                .description("  People   operations  ")
                .build();
        when(departmentRepository.existsByCodeIgnoreCase("HR-01")).thenReturn(false);
        when(departmentRepository.existsByNameIgnoreCase("Human Resources")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(1L);
            return department;
        });

        DepartmentResponse response = departmentService.createDepartment(request);

        assertEquals(1L, response.getDepartmentId());
        assertEquals("HR-01", response.getCode());
        assertEquals("Human Resources", response.getName());
        assertEquals("People operations", response.getDescription());
        assertEquals(DepartmentStatus.ACTIVE, response.getStatus());
    }

    @Test
    void createDepartment_throwsConflictForDuplicateCode() {
        CreateDepartmentRequest request = CreateDepartmentRequest.builder()
                .code("HR")
                .name("Human Resources")
                .build();
        when(departmentRepository.existsByCodeIgnoreCase("HR")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> departmentService.createDepartment(request));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_excludesCurrentDepartmentFromUniquenessChecks() {
        Department department = Department.builder().id(2L).code("OPS").name("Operations").build();
        UpdateDepartmentRequest request = UpdateDepartmentRequest.builder()
                .code("OPS")
                .name("Operations")
                .description("  ")
                .build();
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByCodeIgnoreCaseAndIdNot("OPS", 2L)).thenReturn(false);
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Operations", 2L)).thenReturn(false);
        when(departmentRepository.save(department)).thenReturn(department);

        DepartmentResponse response = departmentService.updateDepartment(2L, request);

        assertEquals("OPS", response.getCode());
        assertEquals("Operations", response.getName());
        assertNull(response.getDescription());
        verify(departmentRepository).existsByCodeIgnoreCaseAndIdNot("OPS", 2L);
        verify(departmentRepository).existsByNameIgnoreCaseAndIdNot("Operations", 2L);
    }

    @Test
    void updateDepartmentStatus_updatesOnlyLifecycleStatus() {
        Department department = Department.builder().id(2L).code("OPS").name("Operations").build();
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);

        DepartmentResponse response = departmentService.updateDepartmentStatus(2L,
                UpdateDepartmentStatusRequest.builder().status(DepartmentStatus.INACTIVE).build());

        assertEquals(DepartmentStatus.INACTIVE, response.getStatus());
        assertEquals("OPS", response.getCode());
    }

    @Test
    void getAllDepartments_returnsNameOrderedRepositoryResults() {
        Department finance = Department.builder().id(1L).code("FIN").name("Finance").build();
        Department hr = Department.builder().id(2L).code("HR").name("Human Resources").build();
        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(finance, hr));

        List<DepartmentResponse> departments = departmentService.getAllDepartments();

        assertEquals(List.of("Finance", "Human Resources"), departments.stream().map(DepartmentResponse::getName).toList());
    }

    @Test
    void deleteDepartment_deletesExistingDepartment() {
        Department department = Department.builder().id(2L).code("OPS").name("Operations").build();
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));

        departmentService.deleteDepartment(2L);

        verify(departmentRepository).delete(department);
    }

    @Test
    void getDepartmentById_throwsNotFoundWhenMissing() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }
}
