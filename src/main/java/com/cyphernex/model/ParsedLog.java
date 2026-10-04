package com.cyphernex.model;

import java.util.HashMap;
import java.util.Map;

public class ParsedLog {
    private String source;
    private String parserName;
    private String parserVersion;
    private Map<String, Object> fields;
    private double confidence;
    private String validationStatus;

    public ParsedLog() {
        this.fields = new HashMap<>();
    }

    public ParsedLog(String source, String parserName, String parserVersion, Map<String, Object> fields, double confidence, String validationStatus) {
        this.source = source;
        this.parserName = parserName;
        this.parserVersion = parserVersion;
        this.fields = fields != null ? fields : new HashMap<>();
        this.confidence = confidence;
        this.validationStatus = validationStatus;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getParserName() {
        return parserName;
    }

    public void setParserName(String parserName) {
        this.parserName = parserName;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    public void setParserVersion(String parserVersion) {
        this.parserVersion = parserVersion;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }
}
