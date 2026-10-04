package com.cyphernex.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Detection {
    private String id;
    private String type;
    private String severity;
    private String message;
    private List<String> eventIds;
    private double confidence;
    private Instant createdAt;

    public Detection() {
        this.createdAt = Instant.now();
        this.eventIds = new ArrayList<>();
    }

    public Detection(String id, String type, String severity, String message, List<String> eventIds, double confidence, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.eventIds = eventIds != null ? eventIds : new ArrayList<>();
        this.confidence = confidence;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getEventIds() {
        return eventIds;
    }

    public void setEventIds(List<String> eventIds) {
        this.eventIds = eventIds;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
