package com.tamvagbackend.controller;

import com.tamvagbackend.dto.AdminAuthDtos.AdminLoginRequest;
import com.tamvagbackend.dto.AdminAuthDtos.AdminLoginResponse;
import com.tamvagbackend.dto.AdminAuthDtos.AdminProvisionRequest;
import com.tamvagbackend.dto.AdminAuthDtos.AdminProvisionResponse;
import com.tamvagbackend.service.AdminAuthenticationService;
import com.tamvagbackend.service.AdminProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/auth")
@Tag(
        name = "Admin Authentication",
        description = "Administrative authentication, registration, and JWT token issuance"
)
public class AdminAuthController {

    private final AdminAuthenticationService adminAuthenticationService;
    private final AdminProvisioningService adminProvisioningService;

    public AdminAuthController(
            AdminAuthenticationService adminAuthenticationService,
            AdminProvisioningService adminProvisioningService
    ) {
        this.adminAuthenticationService = adminAuthenticationService;
        this.adminProvisioningService = adminProvisioningService;
    }

    @PostMapping("/signup")
    @Operation(
            summary = "Register a new admin",
            description = "Creates a new administrative user account. This is a public registration endpoint.",
            security = {}
    )
    public ResponseEntity<AdminProvisionResponse> signup(@Valid @RequestBody AdminProvisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminProvisioningService.provisionAdmin(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Admin login",
            description = "Authenticates an administrative user and issues a backend-signed admin JWT",
            security = {}
    )
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthenticationService.login(request));
    }
}

