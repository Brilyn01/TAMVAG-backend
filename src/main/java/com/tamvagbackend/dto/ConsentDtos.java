package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ConsentDtos {

    public record CreateConsentRequest(
            @NotNull(message = "customer_id is required")
            @JsonProperty("customer_id")
            UUID customerId,

            @NotNull(message = "institution_id is required")
            @JsonProperty("institution_id")
            UUID institutionId,

            @NotBlank(message = "purpose is required")
            String purpose,

            @NotNull(message = "scopes are required")
            List<String> scopes,

            @JsonProperty("duration_days")
            Integer durationDays
    ) {}

    public record ConsentResponse(
            @JsonProperty("consent_id")
            UUID consentId,

            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("institution_id")
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
