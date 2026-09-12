package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    @Column(name = "ledger_entry_id", nullable = false)
    private UUID ledgerEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "entry_type", nullable = false)
    private String entryType; // INCOME, EXPENSE, TRANSFER, SAVING, DEBT, INVESTMENT, FEE, OTHER

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GHS";

    @Column(name = "balance_effect", nullable = false, length = 20)
    private String balanceEffect; // DEBIT, CREDIT

    @Column(name = "balance_after", precision = 20, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "counterparty_id")
    private String counterpartyId;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence = BigDecimal.ONE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public LedgerEntry() {
        this.ledgerEntryId = UUID.randomUUID();
    }

    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBalanceEffect() { return balanceEffect; }
    public void setBalanceEffect(String balanceEffect) { this.balanceEffect = balanceEffect; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
