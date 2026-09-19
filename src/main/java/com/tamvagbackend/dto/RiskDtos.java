package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RiskDtos {

    public record DestinationInfo(
            @Schema(example = "BANK_ACCOUNT")
            String type, // MOBILE_MONEY, BANK_ACCOUNT, WALLET, MERCHANT
            @Schema(example = "201******321")
            String identifier,
            @Schema(example = "GCB-TRANSFER-001")
            String reference
    ) {}

    public record RiskEvaluationRequest(
            @NotNull(message = "customer_id is required")
            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @JsonProperty("account_id")
            @Schema(
                    description = "Optional. Copy a seeded account_id from a transaction/profile response if needed.",
                    example = "00000000-0000-0000-0000-000000000000"
            )
            UUID accountId,

            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be greater than zero")
            @Schema(example = "15000.00")
            BigDecimal amount,

            @NotBlank(message = "currency is required")
            @Schema(example = "GHS")
            String currency,

            @Schema(example = "{\"type\":\"BANK_ACCOUNT\",\"identifier\":\"201******321\",\"reference\":\"GCB-TRANSFER-001\"}")
            DestinationInfo destination,

            @JsonProperty("device_id")
            @Schema(example = "device_kwame_001")
            String deviceId,

            @Schema(example = "MOBILE_APP", allowableValues = {"MOBILE_APP", "USSD", "POS", "WEB"})
            String channel, // MOBILE_APP, USSD, POS, WEB

            @JsonProperty("occurred_at")
            @Schema(example = "2026-09-19T12:00:00Z")
            Instant occurredAt,

            @Schema(example = "{\"authentication_method\":\"BIOMETRIC\",\"beneficiary_age_seconds\":30,\"ip_risk\":0.1}")
            Map<String, Object> context // authentication_method, beneficiary_age_seconds, ip_risk, etc.
    ) {}

    public record ReasonDetail(
            String code,
            String label,
            String severity
    ) {}

    public record RiskEvaluationResponse(
            @JsonProperty("risk_event_id")
            UUID riskEventId,

            @JsonProperty("risk_score")
            int riskScore,

            String decision, // ALLOW, CHALLENGE, HOLD, BLOCK

            @JsonProperty("recommended_action")
            String recommendedAction,

            List<ReasonDetail> reasons,

            @JsonProperty("reason_codes")
            List<String> reasonCodes,

            @JsonProperty("ruleset_version")
            String rulesetVersion,

            @JsonProperty("model_version")
            String modelVersion,

            @JsonProperty("feature_snapshot_id")
            String featureSnapshotId,

            @JsonProperty("expires_at")
            Instant expiresAt
    ) {}
}
