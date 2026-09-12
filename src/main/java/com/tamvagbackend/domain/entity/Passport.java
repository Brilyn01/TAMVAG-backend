package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passport")
public class Passport {

    @Id
    @Column(name = "passport_id", nullable = false)
    private UUID passportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "passport_type", nullable = false)
    private String passportType; // LENDING_PROFILE, BUSINESS_PROFILE, FINANCIAL_SUMMARY, PAYMENT_TRUST_PROFILE, INTERNATIONAL_PROFILE

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "data_categories", nullable = false, columnDefinition = "TEXT")
    private String dataCategories; // JSON array of categories

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "version", nullable = false)
    private String version = "1.0";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    public Passport() {
        this.passportId = UUID.randomUUID();
    }

    public UUID getPassportId() { return passportId; }
    public void setPassportId(UUID passportId) { this.passportId = passportId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getPassportType() { return passportType; }
    public void setPassportType(String passportType) { this.passportType = passportType; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getDataCategories() { return dataCategories; }
    public void setDataCategories(String dataCategories) { this.dataCategories = dataCategories; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
