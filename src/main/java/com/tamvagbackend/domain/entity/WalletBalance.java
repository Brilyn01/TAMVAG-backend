package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_balance", uniqueConstraints = {
    @UniqueConstraint(name = "uq_wallet_currency", columnNames = {"wallet_id", "currency"})
})
public class WalletBalance {

    @Id
    @Column(name = "balance_id", nullable = false)
    private UUID balanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency; // GHS, NGN, KES, ZAR, EGP, USD, GBP, EUR

    @Column(name = "available_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal availableAmount = BigDecimal.ZERO;

    @Column(name = "locked_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal lockedAmount = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public WalletBalance() {
        this.balanceId = UUID.randomUUID();
    }

    public WalletBalance(Wallet wallet, String currency, BigDecimal availableAmount) {
        this.balanceId = UUID.randomUUID();
        this.wallet = wallet;
        this.currency = currency;
        this.availableAmount = availableAmount != null ? availableAmount : BigDecimal.ZERO;
        this.lockedAmount = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    public UUID getBalanceId() { return balanceId; }
    public void setBalanceId(UUID balanceId) { this.balanceId = balanceId; }

    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(BigDecimal availableAmount) { this.availableAmount = availableAmount; }

    public BigDecimal getLockedAmount() { return lockedAmount; }
    public void setLockedAmount(BigDecimal lockedAmount) { this.lockedAmount = lockedAmount; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
