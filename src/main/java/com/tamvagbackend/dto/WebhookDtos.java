package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class WebhookDtos {

    public record CreateWebhookRequest(

            @NotNull(message = "application_id is required")
            @JsonProperty("application_id")
            UUID applicationId,

            @NotBlank(message = "url is required")
            String url,

            @NotEmpty(message = "events must contain at least one event")
            List<@NotBlank(message = "event type must not be blank") String> events

    ) {}

    public record WebhookResponse(
        @JsonProperty("webhook_id")
        UUID webhookId,

        @JsonProperty("application_id")
        UUID applicationId,

        String url,

        String status,

        List<String> events,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("updated_at")
        Instant updatedAt
    ) {}

    public record CreateWebhookResponse(
            @JsonProperty("webhook")
            WebhookResponse webhook,

            @JsonProperty("signing_secret")
            String signingSecret
    ) {}

    public record WebhookDeliveryResponse(
            @JsonProperty("delivery_id")
            UUID deliveryId,

            @JsonProperty("webhook_id")
            UUID webhookId,

            @JsonProperty("event_id")
            UUID eventId,

            @JsonProperty("event_type")
            String eventType,

            String status,

            @JsonProperty("attempt_count")
            int attemptCount,

            @JsonProperty("next_attempt_at")
            Instant nextAttemptAt,

            @JsonProperty("last_attempt_at")
            Instant lastAttemptAt,

            @JsonProperty("response_status")
            Integer responseStatus,

            @JsonProperty("last_error")
            String lastError,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("completed_at")
            Instant completedAt
    ) {}
}