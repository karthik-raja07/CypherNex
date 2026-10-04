package com.cyphernex.model;

import java.util.ArrayList;
import java.util.List;

public class ReplayResult {
    private String status = "SUCCESS";
    private String blockedEventId;
    private List<String> preventedEventIds = new ArrayList<>();
    private int eventsPrevented;
    private int totalEvents;
    private int impactStoppedPercent;
    private List<String> stillHappensEventIds = new ArrayList<>();
    private String explanation;

    // Compatibility fields for existing StoryGraph visualization
    private int originalEventsCount;
    private int simulatedEventsCount;
    private NormalizedEvent removedEvent;
    private boolean changed;
    private String originalNarrative;
    private String simulatedNarrative;
    private List<GraphNode> originalNodes = new ArrayList<>();
    private List<GraphEdge> originalEdges = new ArrayList<>();
    private List<GraphNode> simulatedNodes = new ArrayList<>();
    private List<GraphEdge> simulatedEdges = new ArrayList<>();

    public ReplayResult() {}

    public ReplayResult(String blockedEventId, List<String> preventedEventIds, int eventsPrevented,
                        int totalEvents, int impactStoppedPercent, List<String> stillHappensEventIds,
                        String explanation) {
        this.status = "SUCCESS";
        this.blockedEventId = blockedEventId;
        this.preventedEventIds = preventedEventIds != null ? preventedEventIds : new ArrayList<>();
        this.eventsPrevented = eventsPrevented;
        this.totalEvents = totalEvents;
        this.impactStoppedPercent = impactStoppedPercent;
        this.stillHappensEventIds = stillHappensEventIds != null ? stillHappensEventIds : new ArrayList<>();
        this.explanation = explanation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBlockedEventId() {
        return blockedEventId;
    }

    public void setBlockedEventId(String blockedEventId) {
        this.blockedEventId = blockedEventId;
    }

    public List<String> getPreventedEventIds() {
        return preventedEventIds;
    }

    public void setPreventedEventIds(List<String> preventedEventIds) {
        this.preventedEventIds = preventedEventIds != null ? preventedEventIds : new ArrayList<>();
    }

    public int getEventsPrevented() {
        return eventsPrevented;
    }

    public void setEventsPrevented(int eventsPrevented) {
        this.eventsPrevented = eventsPrevented;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public int getImpactStoppedPercent() {
        return impactStoppedPercent;
    }

    public void setImpactStoppedPercent(int impactStoppedPercent) {
        this.impactStoppedPercent = impactStoppedPercent;
    }

    public List<String> getStillHappensEventIds() {
        return stillHappensEventIds;
    }

    public void setStillHappensEventIds(List<String> stillHappensEventIds) {
        this.stillHappensEventIds = stillHappensEventIds != null ? stillHappensEventIds : new ArrayList<>();
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getOriginalEventsCount() {
        return originalEventsCount;
    }

    public void setOriginalEventsCount(int originalEventsCount) {
        this.originalEventsCount = originalEventsCount;
    }

    public int getSimulatedEventsCount() {
        return simulatedEventsCount;
    }

    public void setSimulatedEventsCount(int simulatedEventsCount) {
        this.simulatedEventsCount = simulatedEventsCount;
    }

    public NormalizedEvent getRemovedEvent() {
        return removedEvent;
    }

    public void setRemovedEvent(NormalizedEvent removedEvent) {
        this.removedEvent = removedEvent;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getOriginalNarrative() {
        return originalNarrative;
    }

    public void setOriginalNarrative(String originalNarrative) {
        this.originalNarrative = originalNarrative;
    }

    public String getSimulatedNarrative() {
        return simulatedNarrative;
    }

    public void setSimulatedNarrative(String simulatedNarrative) {
        this.simulatedNarrative = simulatedNarrative;
    }

    public List<GraphNode> getOriginalNodes() {
        return originalNodes;
    }

    public void setOriginalNodes(List<GraphNode> originalNodes) {
        this.originalNodes = originalNodes != null ? originalNodes : new ArrayList<>();
    }

    public List<GraphEdge> getOriginalEdges() {
        return originalEdges;
    }

    public void setOriginalEdges(List<GraphEdge> originalEdges) {
        this.originalEdges = originalEdges != null ? originalEdges : new ArrayList<>();
    }

    public List<GraphNode> getSimulatedNodes() {
        return simulatedNodes;
    }

    public void setSimulatedNodes(List<GraphNode> simulatedNodes) {
        this.simulatedNodes = simulatedNodes != null ? simulatedNodes : new ArrayList<>();
    }

    public List<GraphEdge> getSimulatedEdges() {
        return simulatedEdges;
    }

    public void setSimulatedEdges(List<GraphEdge> simulatedEdges) {
        this.simulatedEdges = simulatedEdges != null ? simulatedEdges : new ArrayList<>();
    }
}
