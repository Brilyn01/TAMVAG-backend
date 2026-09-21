package com.tamvagbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class UserAuthDtos {

    private UserAuthDtos() {
    }

    public record SignUpRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            @Schema(example = "ama.mensah@example.com")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            @Schema(example = "StrongPassword!2026", format = "password")
            String password,

            @NotBlank(message = "First name is required")
            @Schema(example = "Ama")
            String firstName,

            @NotBlank(message = "Last name is required")
            @Schema(example = "Mensah")
            String lastName,

            @Schema(example = "+233240000000")
            String phoneNumber
    ) {}

    public record SignUpResponse(
            UUID userId,
            String email,
            String firstName,
            String lastName,
            String status,
            UUID customerId,
            String message
    ) {}

    public record SignInRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            @Schema(example = "ama.mensah@example.com")
            String email,

            @NotBlank(message = "Password is required")
            @Schema(example = "StrongPassword!2026", format = "password")
            String password
    ) {}

    public record UserInfo(
            UUID userId,
            String email,
            String firstName,
            String lastName,
            UUID customerId,
            List<String> roles
    ) {}

    public record SignInResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserInfo user
    ) {}

    public record RefreshRequest(
            @NotBlank(message = "Refresh token is required")
            @Schema(example = "opaque-refresh-token-uuid")
            String refreshToken
    ) {}

    public record RefreshResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {}

    public record LogoutRequest(
            @Schema(example = "opaque-refresh-token-uuid")
            String refreshToken
    ) {}

    public record LogoutResponse(
            String message
    ) {}

    public record UserMeResponse(
            UUID userId,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            String status,
            UUID customerId,
            List<String> roles
    ) {}

    public record UpdateMeRequest(
            @Schema(example = "Ama")
            String firstName,

            @Schema(example = "Mensah")
            String lastName,

            @Schema(example = "+233240000000")
            String phoneNumber
    ) {}
}
