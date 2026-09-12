package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
            UUID customerId,

            @JsonProperty("institution_id")
            UUID institutionId,

            @JsonProperty("sync_mode")
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
}
