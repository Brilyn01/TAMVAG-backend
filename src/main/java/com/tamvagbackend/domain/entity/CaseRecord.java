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

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "case_type", nullable = false)
    private String caseType = "RISK_EVENT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_event_id")
    private RiskEvent riskEvent;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "priority", nullable = false)
    private String priority = "NORMAL";

    @Column(name = "status", nullable = false)
    private String status = "OPEN";

    @Column(name = "source", nullable = false)
    private String source = "RULES_ENGINE";

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "disposition")
    private String disposition;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public CaseRecord() {
        this.caseId = UUID.randomUUID();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public RiskEvent getRiskEvent() {
        return riskEvent;
    }

    public void setRiskEvent(RiskEvent riskEvent) {
        this.riskEvent = riskEvent;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}