package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionEventDtos {

    public record TransactionIngestionEvent(
            @JsonProperty("event_id")
            String eventId,

            @NotNull(message = "account_id is required")
            @JsonProperty("account_id")
            UUID accountId,

            @JsonProperty("customer_id")
            UUID customerId,

            @NotBlank(message = "source_event_id is required")
            @JsonProperty("source_event_id")
            String sourceEventId,

            @NotBlank(message = "direction is required")
            String direction, // IN, OUT

            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be greater than zero")
            BigDecimal amount,

            String currency, // GHS, NGN, USD, etc.

            @JsonProperty("occurred_at")
            Instant occurredAt,

            String channel, // USSD, MOBILE_APP, POS, WEB
            String counterparty,
            String reference,

            @JsonProperty("source_system")
            String sourceSystem
    ) {}

    public record IngestionAckResponse(
            @JsonProperty("event_id")
            String eventId,

            String status,
            String message,

            @JsonProperty("queued_at")
            Instant queuedAt
    ) {}
}
