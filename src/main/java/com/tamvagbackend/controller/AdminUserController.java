package com.tamvagbackend.controller;

import com.tamvagbackend.dto.AdminAuthDtos.AdminProvisionRequest;
import com.tamvagbackend.dto.AdminAuthDtos.AdminProvisionResponse;
import com.tamvagbackend.dto.AdminAuthDtos.AdminUserSummary;
import com.tamvagbackend.service.AdminProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/users")
@Tag(
        name = "Admin User Management",
        description = "Administrative account provisioning and management"
)
public class AdminUserController {

    private final AdminProvisioningService adminProvisioningService;

    public AdminUserController(AdminProvisioningService adminProvisioningService) {
        this.adminProvisioningService = adminProvisioningService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_admin:provision')")
    @Operation(
            summary = "Provision an administrative user",
            description = "Creates a new admin account with designated role and operational role. Requires SUPER_ADMIN authority."
    )
    public ResponseEntity<AdminProvisionResponse> provision(@Valid @RequestBody AdminProvisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminProvisioningService.provisionAdmin(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_admin:manage')")
    @Operation(
            summary = "List administrative users",
            description = "Lists all provisioned administrative users. Requires SUPER_ADMIN authority."
    )
    public ResponseEntity<List<AdminUserSummary>> listAdmins() {
        return ResponseEntity.ok(adminProvisioningService.listAdmins());
    }
}
