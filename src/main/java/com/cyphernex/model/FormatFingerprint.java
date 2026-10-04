package com.cyphernex.model;

import java.time.Instant;

public class FormatFingerprint {
    private String fingerprint;
    private String shape;
    private String format;
    private String structuralPattern;
    private int tokenCount;
    private Instant firstSeen;
    private Instant lastSeen;

    public FormatFingerprint() {
        this.firstSeen = Instant.now();
        this.lastSeen = Instant.now();
    }

    public FormatFingerprint(String fingerprint, String shape, String format) {
        this(fingerprint, shape, format, Instant.now(), Instant.now());
    }

    public FormatFingerprint(String fingerprint, String shape, String format, Instant firstSeen, Instant lastSeen) {
        this.fingerprint = fingerprint;
        this.shape = shape;
        this.format = format;
        this.structuralPattern = shape;
        this.tokenCount = shape != null ? shape.length() : 0;
        this.firstSeen = firstSeen != null ? firstSeen : Instant.now();
        this.lastSeen = lastSeen != null ? lastSeen : Instant.now();
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStructuralPattern() {
        return structuralPattern != null ? structuralPattern : shape;
    }

    public void setStructuralPattern(String structuralPattern) {
        this.structuralPattern = structuralPattern;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
}

