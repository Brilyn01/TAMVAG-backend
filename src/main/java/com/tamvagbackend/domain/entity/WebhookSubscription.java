package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscription")
public class WebhookSubscription {

    @Id
    @Column(name = "webhook_id", nullable = false)
    private UUID webhookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "events", nullable = false, columnDefinition = "TEXT")
    private String events;

    @Column(
            name = "signing_secret_ciphertext",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String signingSecretCiphertext;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public WebhookSubscription() {
        this.webhookId = UUID.randomUUID();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(UUID webhookId) {
        this.webhookId = webhookId;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public String getSigningSecretCiphertext() {
        return signingSecretCiphertext;
    }

    public void setSigningSecretCiphertext(String signingSecretCiphertext) {
        this.signingSecretCiphertext = signingSecretCiphertext;
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
}