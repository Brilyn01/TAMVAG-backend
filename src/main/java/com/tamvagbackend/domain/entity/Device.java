package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "fingerprint_token", nullable = false)
    private String fingerprintToken;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "trust_state", nullable = false)
    private String trustState = "TRUSTED";

    public Device() {
        this.deviceId = UUID.randomUUID();
    }

    public Device(Customer customer, String fingerprintToken) {
        this.deviceId = UUID.randomUUID();
        this.customer = customer;
        this.fingerprintToken = fingerprintToken;
        this.firstSeenAt = Instant.now();
        this.lastSeenAt = Instant.now();
        this.trustState = "TRUSTED";
    }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getFingerprintToken() { return fingerprintToken; }
    public void setFingerprintToken(String fingerprintToken) { this.fingerprintToken = fingerprintToken; }

    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getTrustState() { return trustState; }
    public void setTrustState(String trustState) { this.trustState = trustState; }
}
