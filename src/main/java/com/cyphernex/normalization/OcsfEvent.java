package com.cyphernex.normalization;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class OcsfEvent {
    private int classUid;
    private String className;
    private int categoryUid;
    private String categoryName;
    private int activityId;
    private String activityName;
    private int severityId;
    private String severity;
    private Instant time;
    private Map<String, Object> unmapped;
    private Map<String, Object> rawData;

    public OcsfEvent() {
        this.time = Instant.now();
        this.unmapped = new HashMap<>();
        this.rawData = new HashMap<>();
    }

    public OcsfEvent(int classUid, String className, int categoryUid, String categoryName,
                     int activityId, String activityName, int severityId, String severity,
                     Instant time, Map<String, Object> unmapped, Map<String, Object> rawData) {
        this.classUid = classUid;
        this.className = className;
        this.categoryUid = categoryUid;
        this.categoryName = categoryName;
        this.activityId = activityId;
        this.activityName = activityName;
        this.severityId = severityId;
        this.severity = severity;
        this.time = time != null ? time : Instant.now();
        this.unmapped = unmapped != null ? unmapped : new HashMap<>();
        this.rawData = rawData != null ? rawData : new HashMap<>();
    }

    public int getClassUid() {
        return classUid;
    }

    public void setClassUid(int classUid) {
        this.classUid = classUid;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getCategoryUid() {
        return categoryUid;
    }

    public void setCategoryUid(int categoryUid) {
        this.categoryUid = categoryUid;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getSeverityId() {
        return severityId;
    }

    public void setSeverityId(int severityId) {
        this.severityId = severityId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public Map<String, Object> getUnmapped() {
        return unmapped;
    }

    public void setUnmapped(Map<String, Object> unmapped) {
        this.unmapped = unmapped;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, Object> rawData) {
        this.rawData = rawData;
    }
}
