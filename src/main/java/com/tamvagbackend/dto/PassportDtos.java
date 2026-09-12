package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PassportDtos {

    public record CreatePassportRequest(
            @NotNull(message = "customer_id is required")
            @JsonProperty("customer_id")
            UUID customerId,

            @NotBlank(message = "passport_type is required")
            @JsonProperty("passport_type")
            String passportType, // LENDING_PROFILE, BUSINESS_PROFILE, FINANCIAL_SUMMARY, PAYMENT_TRUST_PROFILE, INTERNATIONAL_PROFILE

            @NotBlank(message = "purpose is required")
            String purpose,

            @NotNull(message = "data_categories are required")
            @JsonProperty("data_categories")
            List<String> dataCategories, // INCOME, CASH_FLOW, DEBT, REPAYMENT, SAVINGS, BUSINESS_FLOWS

            @JsonProperty("validity_days")
            Integer validityDays
    ) {}

    public record CreateShareRequest(
            @NotNull(message = "recipient_id is required")
            @JsonProperty("recipient_id")
            UUID recipientId,

            @NotBlank(message = "purpose is required")
            String purpose,

            @NotNull(message = "scopes are required")
            List<String> scopes,

            @JsonProperty("duration_hours")
            Integer durationHours
    ) {}

    public record PassportResponse(
            @JsonProperty("passport_id")
            UUID passportId,

            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("passport_type")
            String passportType,

            String purpose,

            @JsonProperty("data_categories")
            List<String> dataCategories,

            String status,
            String version,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("expires_at")
            Instant expiresAt,

            ProfileDtos.CustomerProfileResponse payload
    ) {}

    public record PassportShareResponse(
            @JsonProperty("share_id")
            UUID shareId,

            @JsonProperty("passport_id")
            UUID passportId,

            @JsonProperty("recipient_id")
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
    ) {}
}
