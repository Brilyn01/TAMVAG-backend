package com.tamvagbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record CreateApplicationRequest(
            @NotNull UUID institutionId,
            @NotBlank String name,
            @NotEmpty List<String> scopes
    ) {
    }

    public record ApplicationResponse(
            UUID applicationId,
            UUID institutionId,
            String clientId,
            String name,
            String status,
            List<String> scopes,
            String createdAt
    ) {
    }

    public record CreateApplicationResponse(
            ApplicationResponse application,
            String clientSecret
    ) {
    }

    public record UpdateStatusRequest(
            @NotBlank String status
    ) {
    }

    public record UpdateScopesRequest(
            @NotEmpty List<String> scopes
    ) {
    }

    public record RotateSecretResponse(
            UUID applicationId,
            String clientId,
            String clientSecret
    ) {
    }
}