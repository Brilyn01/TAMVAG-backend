package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CaseDtos {

    private CaseDtos() {
    }

    public record CaseActionRequest(

            @NotBlank(message = "action is required")
            @Pattern(
                    regexp = "CHALLENGE|HOLD|RELEASE|ESCALATE|BLOCK|INVESTIGATE",
                    message = "action must be one of CHALLENGE, HOLD, RELEASE, ESCALATE, BLOCK, INVESTIGATE"
            )
            String action,

            @Pattern(
                    regexp = "CONFIRMED_RISK|FALSE_POSITIVE|CUSTOMER_CONFIRMED|OTHER",
                    message = "disposition must be one of CONFIRMED_RISK, FALSE_POSITIVE, CUSTOMER_CONFIRMED, OTHER"
            )
            String disposition,

            @Size(max = 100, message = "assignee must not exceed 100 characters")
            String assignee,

            @Size(max = 5000, message = "notes must not exceed 5000 characters")
            String notes
    ) {
    }

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
    ) {
    }
}