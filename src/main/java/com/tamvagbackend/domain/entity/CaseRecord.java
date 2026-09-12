package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_record")
public class CaseRecord {

    @Id
    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_event_id", nullable = false)
    private RiskEvent riskEvent;

    @Column(name = "severity", nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, TRIAGED, INVESTIGATING, ACTIONED, RESOLVED, ESCALATED

    @Column(name = "source", nullable = false)
    private String source = "RULES_ENGINE";

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "disposition")
    private String disposition; // CONFIRMED_RISK, FALSE_POSITIVE, CUSTOMER_CONFIRMED, OTHER

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CaseRecord() {
        this.caseId = UUID.randomUUID();
    }

    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }

    public RiskEvent getRiskEvent() { return riskEvent; }
    public void setRiskEvent(RiskEvent riskEvent) { this.riskEvent = riskEvent; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
