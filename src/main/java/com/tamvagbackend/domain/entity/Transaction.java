package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction", uniqueConstraints = {
    @UniqueConstraint(
            name = "uq_account_source_system_event",
            columnNames = {"account_id", "source_system", "source_event_id"}
    )
})
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "source_event_id", nullable = false)
    private String sourceEventId;

    @Column(name = "direction", nullable = false, length = 10)
    private String direction; // IN, OUT

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GHS";

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "status", nullable = false)
    private String status = "COMPLETED"; // RECEIVED, NORMALISED, DUPLICATE, PENDING, COMPLETED, FAILED, REVERSED

    @Column(name = "channel")
    private String channel; // MOBILE_APP, USSD, POS, WEB, BRANCH

    @Column(name = "counterparty", columnDefinition = "TEXT")
    private String counterparty;

    @Column(name = "merchant_category")
    private String merchantCategory;

    @Column(name = "reference")
    private String reference;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "normalisation_version", nullable = false)
    private String normalisationVersion = "1.0.0";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Transaction() {
        this.transactionId = UUID.randomUUID();
    }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getNormalisationVersion() { return normalisationVersion; }
    public void setNormalisationVersion(String normalisationVersion) { this.normalisationVersion = normalisationVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
