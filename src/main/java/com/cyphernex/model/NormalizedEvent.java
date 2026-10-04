package com.cyphernex.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class NormalizedEvent {
    private String eventId;
    private Instant timestamp;
    private String eventClass;
    private String activity;
    private String severity;
    private String source;
    private String user;
    private String sourceIp;
    private String destinationIp;
    private String host;
    private String sessionId;
    private String requestId;
    private String process;
    private String rawLogId;
    private String rawLog;
    private String parserVersion;
    private double confidence;
    private Provenance provenance;
    private String origin = "LIVE";
    private java.util.List<Map<String, Object>> fieldMappings = new java.util.ArrayList<>();
    private Map<String, Object> parserTrace = new HashMap<>();
    private Map<String, Object> additionalFields;

    public NormalizedEvent() {
        this.timestamp = Instant.now();
        this.additionalFields = new HashMap<>();
        this.fieldMappings = new java.util.ArrayList<>();
        this.parserTrace = new HashMap<>();
    }

    public NormalizedEvent(String eventId, Instant timestamp, String eventClass, String activity, String severity,
                           String source, String user, String sourceIp, String destinationIp, String host,
                           String sessionId, String requestId, String process, String rawLogId, String parserVersion,
                           double confidence, Provenance provenance, Map<String, Object> additionalFields) {
        this.eventId = eventId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.eventClass = eventClass;
        this.activity = activity;
        this.severity = severity;
        this.source = source;
        this.user = user;
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.host = host;
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.process = process;
        this.rawLogId = rawLogId;
        this.parserVersion = parserVersion;
        this.confidence = confidence;
        this.provenance = provenance;
        this.additionalFields = additionalFields != null ? additionalFields : new HashMap<>();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventClass() {
        return eventClass;
    }

    public void setEventClass(String eventClass) {
        this.eventClass = eventClass;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getRawLogId() {
        return rawLogId;
    }

    public void setRawLogId(String rawLogId) {
        this.rawLogId = rawLogId;
    }

    public String getRawLog() {
        return rawLog;
    }

    public void setRawLog(String rawLog) {
        this.rawLog = rawLog;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    public void setParserVersion(String parserVersion) {
        this.parserVersion = parserVersion;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    private java.util.List<String> parentEventIds = new java.util.ArrayList<>();
    private Map<String, String> parentEvidence = new java.util.LinkedHashMap<>();

    public String getId() {
        return eventId;
    }

    public void setId(String id) {
        this.eventId = id;
    }

    public String getDescription() {
        return activity != null && !activity.isBlank() ? activity : (eventClass != null && !eventClass.isBlank() ? eventClass : "Security Event");
    }

    public void setDescription(String description) {
        this.activity = description;
    }

    public int getSeverityWeight() {
        if (severity == null) return 1;
        String s = severity.trim().toUpperCase();
        return switch (s) {
            case "CRITICAL" -> 5;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 1;
        };
    }

    public java.util.List<String> getParentEventIds() {
        if (parentEventIds == null) {
            parentEventIds = new java.util.ArrayList<>();
        }
        return parentEventIds;
    }

    public void setParentEventIds(java.util.List<String> parentEventIds) {
        this.parentEventIds = parentEventIds != null ? parentEventIds : new java.util.ArrayList<>();
    }

    public void addParentEventId(String parentId) {
        if (parentId != null && !parentId.isBlank()) {
            if (this.parentEventIds == null) {
                this.parentEventIds = new java.util.ArrayList<>();
            }
            if (!this.parentEventIds.contains(parentId)) {
                this.parentEventIds.add(parentId);
            }
        }
    }

    public Map<String, String> getParentEvidence() {
        if (parentEvidence == null) {
            parentEvidence = new java.util.LinkedHashMap<>();
        }
        return parentEvidence;
    }

    public void setParentEvidence(Map<String, String> parentEvidence) {
        this.parentEvidence = parentEvidence != null ? parentEvidence : new java.util.LinkedHashMap<>();
    }

    public void addParentEvidence(String parentId, String evidence) {
        if (parentId != null && evidence != null) {
            if (this.parentEvidence == null) {
                this.parentEvidence = new java.util.LinkedHashMap<>();
            }
            this.parentEvidence.put(parentId, evidence);
        }
    }

    public Map<String, Object> getAdditionalFields() {
        if (additionalFields == null) {
            additionalFields = new HashMap<>();
        }
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public String getOrigin() {
        return origin != null ? origin : "LIVE";
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public java.util.List<Map<String, Object>> getFieldMappings() {
        if (fieldMappings == null) {
            fieldMappings = new java.util.ArrayList<>();
        }
        return fieldMappings;
    }

    public void setFieldMappings(java.util.List<Map<String, Object>> fieldMappings) {
        this.fieldMappings = fieldMappings != null ? fieldMappings : new java.util.ArrayList<>();
    }

    public void addFieldMapping(String rawField, Object rawValue, String ocsfField, Object normalizedValue, double confidence, String status, String reason) {
        if (this.fieldMappings == null) {
            this.fieldMappings = new java.util.ArrayList<>();
        }
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("rawField", rawField);
        map.put("rawValue", rawValue);
        map.put("ocsfField", ocsfField);
        map.put("normalizedValue", normalizedValue);
        map.put("confidence", confidence);
        map.put("status", status);
        map.put("reason", reason);
        this.fieldMappings.add(map);
    }

    public Map<String, Object> getParserTrace() {
        if (parserTrace == null) {
            parserTrace = new HashMap<>();
        }
        return parserTrace;
    }

    public void setParserTrace(Map<String, Object> parserTrace) {
        this.parserTrace = parserTrace != null ? parserTrace : new HashMap<>();
    }
}
