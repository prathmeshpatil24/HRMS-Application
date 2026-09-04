package com.hrms.department.entity;

import com.hrms.common.entity.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_departments_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_departments_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_departments_code", columnList = "code"),
                @Index(name = "idx_departments_name", columnList = "name"),
                @Index(name = "idx_departments_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DepartmentStatus status = DepartmentStatus.ACTIVE;
}
