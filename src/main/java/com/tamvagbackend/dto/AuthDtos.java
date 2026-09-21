package com.tamvagbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record TokenRequest(
            @NotBlank
            @Schema(
                    description = "Seeded GCB pilot client ID",
                    example = "app_gcb_pilot_2026"
            )
            String clientId,

            @NotBlank
            @Schema(
                    description = "GCB pilot client secret configured in the deployment environment",
                    example = "your-client-secret",
                    format = "password"
            )
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
