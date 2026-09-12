package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
            String type, // MOBILE_MONEY, BANK_ACCOUNT, WALLET, MERCHANT
            String identifier,
            String reference
    ) {}

    public record RiskEvaluationRequest(
            @NotNull(message = "customer_id is required")
            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("account_id")
            UUID accountId,

            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be greater than zero")
            BigDecimal amount,

            @NotBlank(message = "currency is required")
            String currency,

            DestinationInfo destination,

            @JsonProperty("device_id")
            String deviceId,

            String channel, // MOBILE_APP, USSD, POS, WEB

            @JsonProperty("occurred_at")
            Instant occurredAt,

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
