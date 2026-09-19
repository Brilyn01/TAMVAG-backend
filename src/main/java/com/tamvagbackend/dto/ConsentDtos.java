package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ConsentDtos {

    public record CreateConsentRequest(
            @NotNull(message = "customer_id is required")
            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @NotNull(message = "institution_id is required")
            @JsonProperty("institution_id")
            @Schema(example = "33333333-3333-3333-3333-333333333333")
            UUID institutionId,

            @NotBlank(message = "purpose is required")
            @Schema(example = "CREDIT_ASSESSMENT")
            String purpose,

            @NotNull(message = "scopes are required")
            @Schema(example = "[\"INCOME\", \"CASH_FLOW\", \"DEBT\"]")
            List<String> scopes,

            @JsonProperty("duration_days")
            @Schema(example = "90")
            Integer durationDays
    ) {}

    public record ConsentResponse(
            @JsonProperty("consent_id")
            UUID consentId,

            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @JsonProperty("institution_id")
            @Schema(example = "33333333-3333-3333-3333-333333333333")
            UUID institutionId,

            @JsonProperty("institution_name")
            String institutionName,

            String purpose,
            List<String> scopes,
            String status,

            @JsonProperty("granted_at")
            Instant grantedAt,

            @JsonProperty("expires_at")
            Instant expiresAt,

            @JsonProperty("revoked_at")
            Instant revokedAt,

            @JsonProperty("created_at")
            Instant createdAt
    ) {}
}
