package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wallet", uniqueConstraints = {
    @UniqueConstraint(name = "uq_customer_wallet", columnNames = {"customer_id"})
})
public class Wallet {

    @Id
    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "GHS";

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(
        mappedBy = "wallet",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private List<WalletBalance> balances = new ArrayList<>();

    public Wallet() {
        this.walletId = UUID.randomUUID();
    }

    public Wallet(Customer customer, String defaultCurrency) {
        this.walletId = UUID.randomUUID();
        this.customer = customer;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : "GHS";
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<WalletBalance> getBalances() {
        return balances;
    }

    public void setBalances(List<WalletBalance> balances) {
        this.balances = balances;
    }

    public void addBalance(WalletBalance balance) {
        balances.add(balance);
        balance.setWallet(this);
    }
}