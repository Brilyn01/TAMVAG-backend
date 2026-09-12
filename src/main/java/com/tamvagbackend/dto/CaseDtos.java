package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public class CaseDtos {

    public record CaseActionRequest(
            @NotBlank(message = "action is required")
            String action, // CHALLENGE, HOLD, RELEASE, ESCALATE, BLOCK

            String disposition, // CONFIRMED_RISK, FALSE_POSITIVE, CUSTOMER_CONFIRMED, OTHER

            String assignee,
            String notes
    ) {}

    public record CaseResponse(
            @JsonProperty("case_id")
            UUID caseId,

            @JsonProperty("risk_event_id")
            UUID riskEventId,

            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("risk_score")
            int riskScore,

            @JsonProperty("risk_decision")
            String riskDecision,

            String severity,
            String status,
            String source,
            String assignee,
            String disposition,
            String notes,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("updated_at")
            Instant updatedAt
    ) {}
}
