package com.hrms.auth.init;

import com.hrms.auth.entity.Permission;
import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.PermissionRepository;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking initial security master data (Roles, Permissions, and Super Admin)...");

        // 1. Seed Permissions
        Map<String, Permission> permissions = seedPermissions();

        // 2. Seed Roles with associated permissions
        Map<String, Role> roles = seedRoles(permissions);

        // 3. Seed Super Admin User if not present
        seedSuperAdmin(roles.get("ROLE_ADMIN"));
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, String> defaultPermissions = Map.ofEntries(
                Map.entry("USER_READ", "Permission to view user accounts"),
                Map.entry("USER_WRITE", "Permission to create or edit user accounts"),
                Map.entry("USER_DELETE", "Permission to delete user accounts"),
                Map.entry("EMPLOYEE_READ", "Permission to view employee records"),
                Map.entry("EMPLOYEE_WRITE", "Permission to create or update employee records"),
                Map.entry("EMPLOYEE_DELETE", "Permission to remove employee records"),
                Map.entry("DEPARTMENT_READ", "Permission to view department structures"),
                Map.entry("DEPARTMENT_WRITE", "Permission to manage departments"),
                Map.entry("LEAVE_APPLY", "Permission to apply for leaves"),
                Map.entry("LEAVE_READ", "Permission to view leave status"),
                Map.entry("LEAVE_APPROVE", "Permission to approve or reject leave requests"),
                Map.entry("PAYROLL_READ", "Permission to view payroll reports"),
                Map.entry("PAYROLL_MANAGE", "Permission to process and manage payroll"),
                Map.entry("AUDIT_LOG_VIEW", "Permission to inspect system audit logs"));

        Map<String, Permission> permissionMap = new HashMap<>();

        for (Map.Entry<String, String> entry : defaultPermissions.entrySet()) {
            String name = entry.getKey();
            String desc = entry.getValue();

            Permission permission = permissionRepository.findByName(name)
                    .orElseGet(() -> {
                        Permission newPermission = Permission.builder()
                                .name(name)
                                .description(desc)
                                .build();
                        log.info("Seeding permission: {}", name);
                        return permissionRepository.save(newPermission);
                    });

            permissionMap.put(name, permission);
        }

        return permissionMap;
    }

    private Map<String, Role> seedRoles(Map<String, Permission> permissions) {
        Map<String, Role> roleMap = new HashMap<>();

        // 1. ROLE_ADMIN (Full system access)
        Role adminRole = getOrCreateRole("ROLE_ADMIN", "System Administrator with full access",
                new HashSet<>(permissions.values()));
        roleMap.put("ROLE_ADMIN", adminRole);

        // 2. ROLE_HR (Human Resources)
        Set<Permission> hrPermissions = new HashSet<>(Arrays.asList(
                permissions.get("EMPLOYEE_READ"),
                permissions.get("EMPLOYEE_WRITE"),
                permissions.get("DEPARTMENT_READ"),
                permissions.get("DEPARTMENT_WRITE"),
                permissions.get("LEAVE_READ"),
                permissions.get("LEAVE_APPROVE"),
                permissions.get("PAYROLL_READ"),
                permissions.get("PAYROLL_MANAGE"),
                permissions.get("USER_READ")));
        Role hrRole = getOrCreateRole("ROLE_HR", "Human Resources Manager", hrPermissions);
        roleMap.put("ROLE_HR", hrRole);

        // 3. ROLE_MANAGER (Team / Department Manager)
        Set<Permission> managerPermissions = new HashSet<>(Arrays.asList(
                permissions.get("EMPLOYEE_READ"),
                permissions.get("DEPARTMENT_READ"),
                permissions.get("LEAVE_READ"),
                permissions.get("LEAVE_APPLY"),
                permissions.get("LEAVE_APPROVE")));
        Role managerRole = getOrCreateRole("ROLE_MANAGER", "Department / Team Manager", managerPermissions);
        roleMap.put("ROLE_MANAGER", managerRole);

        // 4. ROLE_EMPLOYEE (Standard Employee)
        Set<Permission> employeePermissions = new HashSet<>(Arrays.asList(
                permissions.get("EMPLOYEE_READ"),
                permissions.get("LEAVE_READ"),
                permissions.get("LEAVE_APPLY"),
                permissions.get("PAYROLL_READ")));
        Role employeeRole = getOrCreateRole("ROLE_EMPLOYEE", "Standard Employee", employeePermissions);
        roleMap.put("ROLE_EMPLOYEE", employeeRole);

        return roleMap;
    }

    // Approach 1: Strict Sync (Remove permissions that are missing from code)

    private Role getOrCreateRole(String roleName, String description, Set<Permission> permissions) {
        return roleRepository.findByName(roleName)
                .map(existingRole -> {
                    existingRole.setDescription(description);
                    existingRole.setPermissions(new HashSet<>(permissions));
                    log.info("Updated role: {}", roleName);
                    return roleRepository.save(existingRole);
                })
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name(roleName)
                            .description(description)
                            .permissions(new HashSet<>(permissions))
                            .build();
                    log.info("Seeding role: {}", roleName);
                    return roleRepository.save(role);
                });
    }

    private void seedSuperAdmin(Role adminRole) {
        if (!userRepository.existsByUsername("admin")) {
            log.info("Creating default Super Admin user account (username: admin)...");
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .username("admin")
                    .email("admin@hrms.com")
                    .password(passwordEncoder.encode("Admin@123456"))
                    .isActive(true)
                    .accountNonLocked(true)
                    .roles(new HashSet<>(Collections.singletonList(adminRole)))
                    .build();

            userRepository.save(admin);
            log.info("Default Super Admin user initialized successfully.");
        }

    }
}

/*
 * Approach 2: Additive / Merge Sync (Preserves Dynamically Added Permissions)
 * Use this if admins can manually assign custom permissions to roles via
 * UI/APIs in production, and you only want DataInitializer to add any newly
 * added permissions without removing existing ones.
 * private Role getOrCreateRole(String roleName, String description,
 * Set<Permission> permissions) {
 * return roleRepository.findByName(roleName)
 * .map(existingRole -> {
 * existingRole.setDescription(description);
 * 
 * Set<Permission> currentPermissions = existingRole.getPermissions() != null
 * ? existingRole.getPermissions()
 * : new HashSet<>();
 * 
 * // Add new permissions from code into existing permissions
 * boolean isUpdated = currentPermissions.addAll(permissions);
 * 
 * if (isUpdated) {
 * existingRole.setPermissions(currentPermissions);
 * log.info("Added new permissions to existing role: {}", roleName);
 * return roleRepository.save(existingRole);
 * }
 * 
 * return existingRole;
 * })
 * .orElseGet(() -> {
 * Role role = Role.builder()
 * .name(roleName)
 * .description(description)
 * .permissions(new HashSet<>(permissions))
 * .build();
 * log.info("Seeding new role: {}", roleName);
 * return roleRepository.save(role);
 * });
 * }
 */