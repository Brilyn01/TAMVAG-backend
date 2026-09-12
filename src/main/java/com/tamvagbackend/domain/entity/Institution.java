package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "institution")
public class Institution {

    @Id
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type; // BANK, DEMI, PSP, LENDER, FINTECH

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "regulatory_reference")
    private String regulatoryReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Institution() {
        this.institutionId = UUID.randomUUID();
    }

    public Institution(UUID institutionId, String name, String type, String status, String regulatoryReference) {
        this.institutionId = institutionId != null ? institutionId : UUID.randomUUID();
        this.name = name;
        this.type = type;
        this.status = status != null ? status : "ACTIVE";
        this.regulatoryReference = regulatoryReference;
        this.createdAt = Instant.now();
    }

    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRegulatoryReference() { return regulatoryReference; }
    public void setRegulatoryReference(String regulatoryReference) { this.regulatoryReference = regulatoryReference; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
