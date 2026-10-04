package com.cyphernex.forge;

import java.util.ArrayList;
import java.util.List;

public class Grammar {
    private String formatName;
    private String version;
    private String fingerprint;
    private String extractionType;
    private String delimiter;
    private String regexPattern;
    private List<GrammarField> fields;

    public Grammar() {
        this.version = "v1";
        this.fields = new ArrayList<>();
    }

    public Grammar(String formatName, String version, String fingerprint, String extractionType,
                   String delimiter, String regexPattern, List<GrammarField> fields) {
        this.formatName = formatName;
        this.version = version != null ? version : "v1";
        this.fingerprint = fingerprint;
        this.extractionType = extractionType;
        this.delimiter = delimiter;
        this.regexPattern = regexPattern;
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
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

    public String getExtractionType() {
        return extractionType;
    }

    public void setExtractionType(String extractionType) {
        this.extractionType = extractionType;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public List<GrammarField> getFields() {
        return fields;
    }

    public void setFields(List<GrammarField> fields) {
        this.fields = fields;
    }
}
