package com.tamvagbackend.controller;

import com.tamvagbackend.dto.AdminAuthDtos.AdminLoginRequest;
import com.tamvagbackend.dto.AdminAuthDtos.AdminLoginResponse;
import com.tamvagbackend.dto.AdminAuthDtos.AdminLogoutResponse;
import com.tamvagbackend.service.AdminAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/auth")
@Tag(
        name = "Admin Authentication",
        description = "Administrative authentication, session management, and JWT token issuance"
)
public class AdminAuthController {

    private final AdminAuthenticationService adminAuthenticationService;

    public AdminAuthController(AdminAuthenticationService adminAuthenticationService) {
        this.adminAuthenticationService = adminAuthenticationService;
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

    @PostMapping("/logout")
    @Operation(
            summary = "Admin logout",
            description = "Logs out the administrative user session",
            security = {}
    )
    public ResponseEntity<AdminLogoutResponse> logout() {
        return ResponseEntity.ok(adminAuthenticationService.logout());
    }
}



