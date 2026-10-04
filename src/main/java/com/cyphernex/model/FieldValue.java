package com.cyphernex.model;

public class FieldValue {
    private Object value;
    private double confidence;
    private String evidence;

    public FieldValue() {
    }

    public FieldValue(Object value, double confidence, String evidence) {
        this.value = value;
        this.confidence = confidence;
        this.evidence = evidence;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
