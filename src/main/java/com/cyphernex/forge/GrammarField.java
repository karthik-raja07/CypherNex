package com.cyphernex.forge;

public class GrammarField {
    private String name;
    private String type;
    private String extractionRule;
    private boolean required;
    private String targetOcsfField;

    public GrammarField() {
    }

    public GrammarField(String name, String type, String extractionRule, boolean required, String targetOcsfField) {
        this.name = name;
        this.type = type;
        this.extractionRule = extractionRule;
        this.required = required;
        this.targetOcsfField = targetOcsfField;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExtractionRule() {
        return extractionRule;
    }

    public void setExtractionRule(String extractionRule) {
        this.extractionRule = extractionRule;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getTargetOcsfField() {
        return targetOcsfField;
    }

    public void setTargetOcsfField(String targetOcsfField) {
        this.targetOcsfField = targetOcsfField;
    }
}
