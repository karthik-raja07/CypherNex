package com.cyphernex.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GraphEvent {
    private String eventId;
    private Instant timestamp;
    private List<String> entities;
    private List<String> relationships;

    public GraphEvent() {
        this.timestamp = Instant.now();
        this.entities = new ArrayList<>();
        this.relationships = new ArrayList<>();
    }

    public GraphEvent(String eventId, Instant timestamp, List<String> entities, List<String> relationships) {
        this.eventId = eventId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.entities = entities != null ? entities : new ArrayList<>();
        this.relationships = relationships != null ? relationships : new ArrayList<>();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getEntities() {
        return entities;
    }

    public void setEntities(List<String> entities) {
        this.entities = entities;
    }

    public List<String> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<String> relationships) {
        this.relationships = relationships;
    }
}
