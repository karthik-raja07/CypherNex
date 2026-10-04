package com.cyphernex.ledger;

import java.time.Instant;

public class LedgerRecord {
    private long sequenceNumber;
    private String recordData;
    private String rawLogHash;
    private String normalizedEventHash;
    private String previousHash;
    private String currentHash;
    private Instant createdAt;

    public LedgerRecord() {
        this.createdAt = Instant.now();
    }

    public LedgerRecord(long sequenceNumber, String recordData, String previousHash, String currentHash, Instant createdAt) {
        this.sequenceNumber = sequenceNumber;
        this.recordData = recordData;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public LedgerRecord(long sequenceNumber, String recordData, String rawLogHash, String normalizedEventHash, String previousHash, String currentHash, Instant createdAt) {
        this.sequenceNumber = sequenceNumber;
        this.recordData = recordData;
        this.rawLogHash = rawLogHash;
        this.normalizedEventHash = normalizedEventHash;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getRecordData() {
        return recordData;
    }

    public void setRecordData(String recordData) {
        this.recordData = recordData;
    }

    public String getRawLogHash() {
        return rawLogHash;
    }

    public void setRawLogHash(String rawLogHash) {
        this.rawLogHash = rawLogHash;
    }

    public String getNormalizedEventHash() {
        return normalizedEventHash;
    }

    public void setNormalizedEventHash(String normalizedEventHash) {
        this.normalizedEventHash = normalizedEventHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

