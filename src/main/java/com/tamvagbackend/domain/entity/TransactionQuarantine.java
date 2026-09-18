package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "transaction_quarantine",
        indexes = {
                @Index(name = "idx_quarantine_connection", columnList = "connection_id"),
                @Index(name = "idx_quarantine_created_at", columnList = "created_at"),
                @Index(name = "idx_quarantine_status", columnList = "status")
        }
)
public class TransactionQuarantine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quarantine_id", nullable = false)
    private UUID quarantineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private Connection connection;

    @Column(name = "source_event_id")
    private String sourceEventId;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "account_ref_token")
    private String accountRefToken;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getQuarantineId() {
        return quarantineId;
    }

    public void setQuarantineId(UUID quarantineId) {
        this.quarantineId = quarantineId;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getAccountRefToken() {
        return accountRefToken;
    }

    public void setAccountRefToken(String accountRefToken) {
        this.accountRefToken = accountRefToken;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}