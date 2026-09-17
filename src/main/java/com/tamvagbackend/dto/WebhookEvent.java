package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record WebhookEvent(
        @JsonProperty("event_id")
        UUID eventId,

        @JsonProperty("event_type")
        String eventType,

        Instant timestamp,

        Object data
) {
}