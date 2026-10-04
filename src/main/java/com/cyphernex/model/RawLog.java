package com.cyphernex.model;

import java.time.Instant;

public class RawLog {
    private String id;
    private String source;
    private String rawContent;
    private Instant receivedAt;

    public RawLog() {
        this.receivedAt = Instant.now();
    }

    public RawLog(String id, String source, String rawContent, Instant receivedAt) {
        this.id = id;
        this.source = source;
        this.rawContent = rawContent;
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
