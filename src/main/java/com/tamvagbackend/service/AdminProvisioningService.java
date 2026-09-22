package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.AdminUser;
import com.tamvagbackend.domain.repository.AdminUserRepository;
import com.tamvagbackend.dto.AdminAuthDtos.*;
import com.tamvagbackend.security.RolePermissions;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminProvisioningService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminProvisioningService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminProvisionResponse provisionAdmin(AdminProvisionRequest request) {
        if (!RolePermissions.isSupportedAdministrativeRole(request.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid administrative role");
        }

        if (!RolePermissions.isSupportedRole(request.operationalRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operational role");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        if (adminUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An admin account with this email already exists");
        }

        AdminUser admin = new AdminUser();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setFirstName(request.firstName().trim());
        admin.setLastName(request.lastName().trim());
        admin.setRole(RolePermissions.normalizeRole(request.role()));
        admin.setOperationalRole(RolePermissions.normalizeRole(request.operationalRole()));
        admin.setStatus("ACTIVE");

        AdminUser saved = adminUserRepository.save(admin);

        return new AdminProvisionResponse(
                saved.getAdminUserId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole(),
                saved.getOperationalRole(),
                saved.getStatus(),
                "Admin account provisioned successfully"
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummary> listAdmins() {
        return adminUserRepository.findAll().stream()
                .map(a -> new AdminUserSummary(
                        a.getAdminUserId(),
                        a.getEmail(),
                        a.getFirstName(),
                        a.getLastName(),
                        a.getRole(),
                        a.getOperationalRole(),
                        a.getStatus(),
                        a.getCreatedAt()
                ))
                .toList();
    }
}
