package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CaseDtos {

    private CaseDtos() {
    }

    public record CreateManualCaseRequest(

            @NotBlank(message = "title is required")
            @Size(max = 255, message = "title must not exceed 255 characters")
            String title,

            @Size(max = 5000, message = "description must not exceed 5000 characters")
            String description,

            @Pattern(
                    regexp = "LOW|MEDIUM|HIGH|CRITICAL",
                    message = "severity must be one of LOW, MEDIUM, HIGH, CRITICAL"
            )
            String severity,

            @Pattern(
                    regexp = "LOW|NORMAL|HIGH|URGENT",
                    message = "priority must be one of LOW, NORMAL, HIGH, URGENT"
            )
            String priority,

            UUID customerId
    ) {
    }

    public record CaseActionRequest(

            @NotBlank(message = "action is required")
            @Schema(
                    description = "Analyst action",
                    example = "INVESTIGATE",
                    allowableValues = {
                            "CHALLENGE",
                            "HOLD",
                            "RELEASE",
                            "ESCALATE",
                            "BLOCK",
                            "INVESTIGATE"
                    }
            )
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

            @JsonProperty("case_type")
            String caseType,

            @JsonProperty("institution_id")
            UUID institutionId,

            @JsonProperty("risk_event_id")
            UUID riskEventId,

            @JsonProperty("customer_id")
            UUID customerId,

            @JsonProperty("risk_score")
            Integer riskScore,

            @JsonProperty("risk_decision")
            String riskDecision,

            String title,

            String description,

            String severity,

            String priority,

            String status,

            String source,

            String assignee,

            @JsonProperty("created_by")
            String createdBy,

            String disposition,

            String resolution,

            String notes,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("updated_at")
            Instant updatedAt,

            @JsonProperty("resolved_at")
            Instant resolvedAt
    ) {
    }
}