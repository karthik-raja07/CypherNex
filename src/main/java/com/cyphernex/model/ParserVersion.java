package com.cyphernex.model;

public class ParserVersion {
    private String parserName;
    private String version;
    private String fingerprint;
    private String grammar;
    private String status;
    private double extractionRate;
    private double validationRate;
    private double nullRate;

    public ParserVersion() {
    }

    public ParserVersion(String parserName, String version, String fingerprint, String grammar, String status,
                         double extractionRate, double validationRate, double nullRate) {
        this.parserName = parserName;
        this.version = version;
        this.fingerprint = fingerprint;
        this.grammar = grammar;
        this.status = status;
        this.extractionRate = extractionRate;
        this.validationRate = validationRate;
        this.nullRate = nullRate;
    }

    public String getParserName() {
        return parserName;
    }

    public void setParserName(String parserName) {
        this.parserName = parserName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getGrammar() {
        return grammar;
    }

    public void setGrammar(String grammar) {
        this.grammar = grammar;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getExtractionRate() {
        return extractionRate;
    }

    public void setExtractionRate(double extractionRate) {
        this.extractionRate = extractionRate;
    }

    public double getValidationRate() {
        return validationRate;
    }

    public void setValidationRate(double validationRate) {
        this.validationRate = validationRate;
    }

    public double getNullRate() {
        return nullRate;
    }

    public void setNullRate(double nullRate) {
        this.nullRate = nullRate;
    }
}
