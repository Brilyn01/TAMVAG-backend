package com.tamvagbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AdminAuthDtos {

    private AdminAuthDtos() {
    }

    public record AdminLoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            @Schema(example = "admin@tamva.com")
            String email,

            @NotBlank(message = "Password is required")
            @Schema(example = "AdminSecret!2026", format = "password")
            String password
    ) {}

    public record AdminUserInfo(
            UUID adminUserId,
            String email,
            String firstName,
            String lastName,
            String role,
            String operationalRole,
            String status
    ) {}

    public record AdminLoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            AdminUserInfo adminUser,
            Set<String> permissions
    ) {}

    public record AdminProvisionRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            @Schema(example = "analyst@tamva.com")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            @Schema(example = "AnalystPassword!2026", format = "password")
            String password,

            @NotBlank(message = "First name is required")
            @Schema(example = "Kofi")
            String firstName,

            @NotBlank(message = "Last name is required")
            @Schema(example = "Annan")
            String lastName,

            @NotBlank(message = "Administrative role is required")
            @Schema(example = "ADMIN", allowableValues = {"ADMIN", "SUPER_ADMIN"})
            String role,

            @NotBlank(message = "Operational role is required")
            @Schema(example = "RISK_ANALYST", allowableValues = {"RISK_ANALYST", "SECURITY_ADMINISTRATOR", "PLATFORM_OPERATOR"})
            String operationalRole
    ) {}

    public record AdminProvisionResponse(
            UUID adminUserId,
            String email,
            String firstName,
            String lastName,
            String role,
            String operationalRole,
            String status,
            String message
    ) {}

    public record AdminUserSummary(
            UUID adminUserId,
            String email,
            String firstName,
            String lastName,
            String role,
            String operationalRole,
            String status,
            Instant createdAt
    ) {}

    public record AdminLogoutResponse(
            @Schema(example = "Admin logged out successfully")
            String message
    ) {}
}

