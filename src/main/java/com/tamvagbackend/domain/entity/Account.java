package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "account_type", nullable = false)
    private String accountType; // MOBILE_MONEY, BANK_CURRENT, SAVINGS, WALLET

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GHS";

    @Column(name = "account_ref_token")
    private String accountRefToken;

    @Column(name = "masked_identifier")
    private String maskedIdentifier;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Account() {
        this.accountId = UUID.randomUUID();
    }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getAccountRefToken() { return accountRefToken; }
    public void setAccountRefToken(String accountRefToken) { this.accountRefToken = accountRefToken; }

    public String getMaskedIdentifier() { return maskedIdentifier; }
    public void setMaskedIdentifier(String maskedIdentifier) { this.maskedIdentifier = maskedIdentifier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
