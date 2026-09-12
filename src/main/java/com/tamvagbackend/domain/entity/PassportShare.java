package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passport_share")
public class PassportShare {

    @Id
    @Column(name = "share_id", nullable = false)
    private UUID shareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_id", nullable = false)
    private Passport passport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Institution recipient;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "scopes", nullable = false, columnDefinition = "TEXT")
    private String scopes;

    @Column(name = "share_token", nullable = false, unique = true)
    private String shareToken;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, REVOKED

    @Column(name = "shared_at", nullable = false)
    private Instant sharedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public PassportShare() {
        this.shareId = UUID.randomUUID();
    }

    public UUID getShareId() { return shareId; }
    public void setShareId(UUID shareId) { this.shareId = shareId; }

    public Passport getPassport() { return passport; }
    public void setPassport(Passport passport) { this.passport = passport; }

    public Institution getRecipient() { return recipient; }
    public void setRecipient(Institution recipient) { this.recipient = recipient; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSharedAt() { return sharedAt; }
    public void setSharedAt(Instant sharedAt) { this.sharedAt = sharedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
