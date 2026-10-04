package com.cyphernex.identity;

import java.time.Instant;

public class IdentityEvidence {
    private String evidenceType;
    private String value;
    private String source;
    private Instant observedAt;
    private double confidence;

    public IdentityEvidence() {
        this.observedAt = Instant.now();
    }

    public IdentityEvidence(String evidenceType, String value, String source, Instant observedAt, double confidence) {
        this.evidenceType = evidenceType;
        this.value = value;
        this.source = source;
        this.observedAt = observedAt != null ? observedAt : Instant.now();
        this.confidence = confidence;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
