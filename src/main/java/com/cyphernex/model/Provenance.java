package com.cyphernex.model;

import java.util.ArrayList;
import java.util.List;

public class Provenance {
    private String parserName;
    private String parserVersion;
    private String fingerprint;
    private List<String> validatorsPassed;

    public Provenance() {
        this.validatorsPassed = new ArrayList<>();
    }

    public Provenance(String parserName, String parserVersion, String fingerprint, List<String> validatorsPassed) {
        this.parserName = parserName;
        this.parserVersion = parserVersion;
        this.fingerprint = fingerprint;
        this.validatorsPassed = validatorsPassed != null ? validatorsPassed : new ArrayList<>();
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

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public List<String> getValidatorsPassed() {
        return validatorsPassed;
    }

    public void setValidatorsPassed(List<String> validatorsPassed) {
        this.validatorsPassed = validatorsPassed;
    }
}
