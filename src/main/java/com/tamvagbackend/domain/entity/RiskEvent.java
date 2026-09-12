package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_event")
public class RiskEvent {

    @Id
    @Column(name = "risk_event_id", nullable = false)
    private UUID riskEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "decision", nullable = false)
    private String decision; // ALLOW, CHALLENGE, HOLD, BLOCK

    @Column(name = "recommended_action")
    private String recommendedAction;

    @Column(name = "reason_codes", columnDefinition = "TEXT")
    private String reasonCodes; // JSON array of string reason codes

    @Column(name = "ruleset_version")
    private String rulesetVersion;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "status", nullable = false)
    private String status = "EVALUATED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    public RiskEvent() {
        this.riskEventId = UUID.randomUUID();
    }

    public UUID getRiskEventId() { return riskEventId; }
    public void setRiskEventId(UUID riskEventId) { this.riskEventId = riskEventId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public String getReasonCodes() { return reasonCodes; }
    public void setReasonCodes(String reasonCodes) { this.reasonCodes = reasonCodes; }

    public String getRulesetVersion() { return rulesetVersion; }
    public void setRulesetVersion(String rulesetVersion) { this.rulesetVersion = rulesetVersion; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
