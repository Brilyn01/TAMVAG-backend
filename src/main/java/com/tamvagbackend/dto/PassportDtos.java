package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import com.tamvagbackend.dto.ProfileDtos.CustomerProfileResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PassportDtos {

    private PassportDtos() {
    }

    public record CreatePassportRequest(

            @NotNull(message = "customer_id is required")
            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @NotBlank(message = "passport_type is required")
            @JsonProperty("passport_type")
            @Schema(example = "FINANCIAL_SUMMARY")
            String passportType,

            @NotBlank(message = "purpose is required")
            @Schema(example = "GCB credit assessment")
            String purpose,

            @NotNull(message = "data_categories is required")
            @NotEmpty(message = "data_categories must not be empty")
            @JsonProperty("data_categories")
            @Schema(example = "[\"INCOME\", \"CASH_FLOW\", \"DEBT\"]")
            List<@NotBlank(message = "data category must not be blank") String> dataCategories,

            @Min(value = 1, message = "validity_days must be at least 1")
            @Max(value = 365, message = "validity_days must not exceed 365")
            @JsonProperty("validity_days")
            @Schema(example = "30")
            Integer validityDays
    ) {
    }

    public record CreateShareRequest(

            @NotNull(message = "recipient_id is required")
            @JsonProperty("recipient_id")
            @Schema(example = "44444444-4444-4444-4444-444444444444")
            UUID recipientId,

            @NotBlank(message = "purpose is required")
            @Schema(example = "GCB credit assessment")
            String purpose,

            @NotNull(message = "scopes is required")
            @NotEmpty(message = "scopes must not be empty")
            @Schema(example = "[\"INCOME\", \"CASH_FLOW\"]")
            List<@NotBlank(message = "scope must not be blank") String> scopes,

            @Min(value = 1, message = "duration_hours must be at least 1")
            @Max(value = 72, message = "duration_hours must not exceed 72")
            @JsonProperty("duration_hours")
            @Schema(example = "24")
            Integer durationHours
    ) {
    }

    public record PassportResponse(

            @JsonProperty("passport_id")
            UUID passportId,

            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @JsonProperty("passport_type")
            @Schema(example = "FINANCIAL_SUMMARY")
            String passportType,

            String purpose,

            @JsonProperty("data_categories")
            @Schema(example = "[\"INCOME\", \"CASH_FLOW\", \"DEBT\"]")
            List<String> dataCategories,

            String status,

            String version,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("expires_at")
            Instant expiresAt,

            CustomerProfileResponse profile
    ) {
    }

    public record PassportShareResponse(

            @JsonProperty("share_id")
            UUID shareId,

            @JsonProperty("passport_id")
            UUID passportId,

            @JsonProperty("recipient_id")
            @Schema(example = "44444444-4444-4444-4444-444444444444")
            UUID recipientId,

            @JsonProperty("recipient_name")
            String recipientName,

            String purpose,

            List<String> scopes,

            @JsonProperty("share_token")
            String shareToken,

            String status,

            @JsonProperty("shared_at")
            Instant sharedAt,

            @JsonProperty("expires_at")
            Instant expiresAt,

            @JsonProperty("revoked_at")
            Instant revokedAt
    ) {
    }
}