package com.tamvagbackend.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record TokenRequest(
            @NotBlank
            String clientId,

            @NotBlank
            String clientSecret
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            String scope
    ) {
    }
}