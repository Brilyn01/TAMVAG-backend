package com.tamvagbackend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feature")
public class Feature {

    @Id
    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "subject_type", nullable = false)
    private String subjectType; // CUSTOMER, ACCOUNT

    @Column(name = "feature_name", nullable = false)
    private String featureName;

    @Column(name = "feature_value", nullable = false, columnDefinition = "TEXT")
    private String featureValue;

    @Column(name = "window_period", nullable = false)
    private String windowPeriod; // 5m, 1h, 24h, 7d, 30d, 90d, 180d, 365d, REAL_TIME

    @Column(name = "feature_version", nullable = false)
    private String featureVersion = "1.0.0";

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    public Feature() {
        this.featureId = UUID.randomUUID();
    }

    public Feature(UUID subjectId, String subjectType, String featureName, String featureValue, String windowPeriod, String featureVersion) {
        this.featureId = UUID.randomUUID();
        this.subjectId = subjectId;
        this.subjectType = subjectType;
        this.featureName = featureName;
        this.featureValue = featureValue;
        this.windowPeriod = windowPeriod;
        this.featureVersion = featureVersion != null ? featureVersion : "1.0.0";
        this.computedAt = Instant.now();
    }

    public UUID getFeatureId() { return featureId; }
    public void setFeatureId(UUID featureId) { this.featureId = featureId; }

    public UUID getSubjectId() { return subjectId; }
    public void setSubjectId(UUID subjectId) { this.subjectId = subjectId; }

    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public String getFeatureValue() { return featureValue; }
    public void setFeatureValue(String featureValue) { this.featureValue = featureValue; }

    public String getWindowPeriod() { return windowPeriod; }
    public void setWindowPeriod(String windowPeriod) { this.windowPeriod = windowPeriod; }

    public String getFeatureVersion() { return featureVersion; }
    public void setFeatureVersion(String featureVersion) { this.featureVersion = featureVersion; }

    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
