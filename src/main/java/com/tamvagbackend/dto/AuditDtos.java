package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AuditDtos {

    public record AuditEventResponse(
        @JsonProperty("audit_id")
        UUID auditId,

        @JsonProperty("actor_type")
        String actorType,

        @JsonProperty("actor_id")
        String actorId,

        String action,

        @JsonProperty("resource_type")
        String resourceType,

        @JsonProperty("resource_id")
        String resourceId,

        Instant timestamp,

        @JsonProperty("correlation_id")
        String correlationId,

        @JsonProperty("event_hash")
        String eventHash,

        String payload
    ) {}

    public record ConnectorSyncRequest(
        @JsonProperty("customer_id")
        @Schema(
                description = "Customer owning the connection",
                example = "a1b2c3d4-0000-0000-0000-000000000001"
        )
        UUID customerId,

        @JsonProperty("institution_id")
        @Schema(
                description = "Authenticated institution",
                example = "33333333-3333-3333-3333-333333333333"
        )
        UUID institutionId,

        @JsonProperty("sync_mode")
        @Schema(
                description = "Synchronization mode",
                example = "INCREMENTAL",
                allowableValues = {"FULL", "INCREMENTAL"}
        )
        String syncMode // FULL, INCREMENTAL
        ) {}

    public record ConnectorSyncResponse(
        @JsonProperty("sync_id")
        String syncId,

        String status,

        @JsonProperty("records_ingested")
        int recordsIngested,

        @JsonProperty("records_normalized")
        int recordsNormalized,

        @JsonProperty("records_quarantined")
        int recordsQuarantined,

        @JsonProperty("synced_at")
        Instant syncedAt
    ) {}

    public record ConnectionResponse(
        @JsonProperty("connection_id")
        UUID connectionId,

        @JsonProperty("customer_id")
        UUID customerId,

        @JsonProperty("institution_id")
        UUID institutionId,

        String status,

        @JsonProperty("provider_ref")
        String providerRef,

        @JsonProperty("last_sync_at")
        Instant lastSyncAt,

        @JsonProperty("created_at")
        Instant createdAt
    ) {}
}
