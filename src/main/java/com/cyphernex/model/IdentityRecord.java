package com.cyphernex.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentityRecord {
    private String canonicalIdentity;
    private Set<String> aliases;
    private double confidence;
    private List<String> evidence;

    public IdentityRecord() {
        this.aliases = new LinkedHashSet<>();
        this.evidence = new ArrayList<>();
    }

    public IdentityRecord(String canonicalIdentity, Set<String> aliases, double confidence, List<String> evidence) {
        this.canonicalIdentity = canonicalIdentity;
        this.aliases = aliases != null ? aliases : new LinkedHashSet<>();
        this.confidence = confidence;
        this.evidence = evidence != null ? evidence : new ArrayList<>();
    }

    public String getCanonicalIdentity() {
        return canonicalIdentity;
    }

    public void setCanonicalIdentity(String canonicalIdentity) {
        this.canonicalIdentity = canonicalIdentity;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }
}
