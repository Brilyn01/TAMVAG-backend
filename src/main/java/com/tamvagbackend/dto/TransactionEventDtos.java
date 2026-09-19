package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionEventDtos {

    public record TransactionIngestionEvent(
            @JsonProperty("event_id")
            @Schema(example = "swagger-event-001")
            String eventId,

            @NotNull(message = "account_id is required")
            @JsonProperty("account_id")
            @Schema(
                    description = "Required. Copy the account_id associated with the customer from the relevant API response.",
                    example = "00000000-0000-0000-0000-000000000000"
            )
            UUID accountId,

            @JsonProperty("customer_id")
            @Schema(example = "a1b2c3d4-0000-0000-0000-000000000001")
            UUID customerId,

            @NotBlank(message = "source_event_id is required")
            @JsonProperty("source_event_id")
            @Schema(example = "swagger-source-event-001")
            String sourceEventId,

            @NotBlank(message = "direction is required")
            @Schema(example = "IN", allowableValues = {"IN", "OUT"})
            String direction, // IN, OUT

            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be greater than zero")
            @Schema(example = "2500.00")
            BigDecimal amount,

            @Schema(example = "GHS")
            String currency, // GHS, NGN, USD, etc.

            @JsonProperty("occurred_at")
            @Schema(example = "2026-09-19T12:00:00Z")
            Instant occurredAt,

            @Schema(example = "MOBILE_APP", allowableValues = {"USSD", "MOBILE_APP", "POS", "WEB"})
            String channel, // USSD, MOBILE_APP, POS, WEB
            @Schema(example = "GCB Bank Ghana")
            String counterparty,
            @Schema(example = "Swagger integration test transaction")
            String reference,

            @JsonProperty("source_system")
            @Schema(example = "swagger_test")
            String sourceSystem
    ) {}

    public record IngestionAckResponse(
            @JsonProperty("event_id")
            @Schema(example = "swagger-event-001")
            String eventId,

            String status,
            String message,

            @JsonProperty("queued_at")
            Instant queuedAt
    ) {}
}
