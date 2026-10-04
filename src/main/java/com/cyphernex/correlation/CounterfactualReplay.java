package com.cyphernex.correlation;

import com.cyphernex.model.GraphEdge;
import com.cyphernex.model.GraphNode;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ReplayResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CounterfactualReplay {

    private final EventCorrelator eventCorrelator;

    public CounterfactualReplay(EventCorrelator eventCorrelator) {
        this.eventCorrelator = eventCorrelator;
    }

    /**
     * Fixed-point forward propagation through the causal event graph.
     */
    public ReplayResult computePrevented(String blockedEventId, List<NormalizedEvent> allEvents) {
        if (allEvents == null || allEvents.isEmpty()) {
            return new ReplayResult(null, new ArrayList<>(), 0, 0, 0, new ArrayList<>(), "No events available for simulation.");
        }

        // Ensure causal parent relationships are present
        List<NormalizedEvent> eventsCopy = new ArrayList<>(allEvents);
        boolean hasExistingParents = eventsCopy.stream().anyMatch(e -> e.getParentEventIds() != null && !e.getParentEventIds().isEmpty());
        if (!hasExistingParents && eventCorrelator != null) {
            StoryGraph g = eventCorrelator.correlateToGraph(eventsCopy, new StoryGraph());
            if (!g.getCorrelatedEvents().isEmpty()) {
                eventsCopy = g.getCorrelatedEvents();
            }
        }

        // Resolve target blocked ID
        String targetBlockedId = null;
        if (blockedEventId != null && !blockedEventId.isBlank()) {
            String raw = blockedEventId.trim();
            String stripped = raw.startsWith("event:") ? raw.substring(6) : raw;
            for (NormalizedEvent ev : eventsCopy) {
                if (Objects.equals(ev.getId(), raw)
                        || Objects.equals(ev.getId(), stripped)
                        || (ev.getActivity() != null && ev.getActivity().equalsIgnoreCase(raw))) {
                    targetBlockedId = ev.getId();
                    break;
                }
            }
            if (targetBlockedId == null) {
                targetBlockedId = stripped;
            }
        }

        Set<String> prevented = new LinkedHashSet<>();
        if (targetBlockedId != null) {
            prevented.add(targetBlockedId);

            // Fixed-point forward propagation
            boolean changed = true;
            while (changed) {
                changed = false;
                for (NormalizedEvent ev : eventsCopy) {
                    String evId = ev.getId();
                    if (!prevented.contains(evId) && ev.getParentEventIds() != null) {
                        for (String parentId : ev.getParentEventIds()) {
                            if (prevented.contains(parentId)) {
                                prevented.add(evId);
                                changed = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        List<String> preventedList = new ArrayList<>(prevented);
        List<String> stillHappensList = new ArrayList<>();
        List<NormalizedEvent> simulatedEvents = new ArrayList<>();

        int totalWeight = 0;
        int preventedWeight = 0;

        for (NormalizedEvent ev : eventsCopy) {
            int w = ev.getSeverityWeight();
            totalWeight += w;
            if (prevented.contains(ev.getId())) {
                preventedWeight += w;
            } else {
                stillHappensList.add(ev.getId());
                simulatedEvents.add(ev);
            }
        }

        int impactPercent = (totalWeight > 0)
                ? (int) Math.round(((double) preventedWeight / totalWeight) * 100.0)
                : 0;

        final String finalBlockedId = targetBlockedId;
        String explanation;
        if (finalBlockedId == null || preventedList.isEmpty()) {
            explanation = "No event blocked. Full incident timeline occurs.";
        } else {
            NormalizedEvent blockedEv = eventsCopy.stream()
                    .filter(e -> Objects.equals(e.getId(), finalBlockedId))
                    .findFirst()
                    .orElse(null);
            String act = blockedEv != null ? blockedEv.getDescription() : finalBlockedId;
            String usr = (blockedEv != null && blockedEv.getUser() != null) ? (" by " + blockedEv.getUser()) : "";
            explanation = "Blocking event '" + act + "'" + usr + " prevents " + preventedList.size()
                    + " of " + eventsCopy.size() + " events (" + impactPercent + "% impact stopped).";
        }

        ReplayResult result = new ReplayResult(
                finalBlockedId,
                preventedList,
                preventedList.size(),
                eventsCopy.size(),
                impactPercent,
                stillHappensList,
                explanation
        );

        // Build simulated StoryGraph for visual timeline comparison
        StoryGraph origGraph = eventCorrelator.correlateToGraph(eventsCopy, new StoryGraph());
        StoryGraph simGraph = eventCorrelator.correlateToGraph(simulatedEvents, new StoryGraph());

        result.setOriginalEventsCount(eventsCopy.size());
        result.setSimulatedEventsCount(simulatedEvents.size());
        result.setOriginalNarrative(origGraph.generateNarrative());
        result.setSimulatedNarrative(simGraph.generateNarrative());
        result.setOriginalNodes(origGraph.getNodes());
        result.setOriginalEdges(origGraph.getEdges());
        result.setSimulatedNodes(simGraph.getNodes());
        result.setSimulatedEdges(simGraph.getEdges());
        result.setChanged(!preventedList.isEmpty());
        if (finalBlockedId != null) {
            eventsCopy.stream().filter(e -> Objects.equals(e.getId(), finalBlockedId)).findFirst().ifPresent(result::setRemovedEvent);
        }

        return result;
    }

    /**
     * Evaluates blocking every candidate event and returns the one with the highest impact.
     * Tie-breaker: selects earliest event by timestamp.
     */
    public Map<String, Object> findBestBlockPoint(List<NormalizedEvent> allEvents) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (allEvents == null || allEvents.isEmpty()) {
            response.put("status", "EMPTY");
            response.put("message", "No events available to evaluate block points.");
            response.put("recommendedEvent", null);
            response.put("impactStoppedPercent", 0);
            response.put("replayResult", new ReplayResult());
            return response;
        }

        List<NormalizedEvent> sorted = new ArrayList<>(allEvents);
        boolean hasExistingParents = sorted.stream().anyMatch(e -> e.getParentEventIds() != null && !e.getParentEventIds().isEmpty());
        if (!hasExistingParents && eventCorrelator != null) {
            StoryGraph g = eventCorrelator.correlateToGraph(sorted, new StoryGraph());
            if (!g.getCorrelatedEvents().isEmpty()) {
                sorted = g.getCorrelatedEvents();
            }
        }
        sorted.sort(Comparator.comparing(NormalizedEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

        NormalizedEvent bestEvent = null;
        ReplayResult bestResult = null;
        int maxImpact = -1;

        for (NormalizedEvent candidate : sorted) {
            ReplayResult res = computePrevented(candidate.getId(), sorted);
            if (res.getImpactStoppedPercent() > maxImpact) {
                maxImpact = res.getImpactStoppedPercent();
                bestEvent = candidate;
                bestResult = res;
            }
        }

        response.put("status", "SUCCESS");
        response.put("recommendedEvent", bestEvent);
        response.put("recommendedEventId", bestEvent != null ? bestEvent.getId() : null);
        response.put("impactStoppedPercent", maxImpact >= 0 ? maxImpact : 0);
        response.put("replayResult", bestResult);

        return response;
    }

    /**
     * Backward-compatible helper method returning Map for legacy callers.
     */
    public Map<String, Object> runReplay(List<NormalizedEvent> realEvents, String eventIdToRemove) {
        ReplayResult res = computePrevented(eventIdToRemove, realEvents);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", res.getStatus());
        map.put("blockedEventId", res.getBlockedEventId());
        map.put("preventedEventIds", res.getPreventedEventIds());
        map.put("eventsPrevented", res.getEventsPrevented());
        map.put("totalEvents", res.getTotalEvents());
        map.put("impactStoppedPercent", res.getImpactStoppedPercent());
        map.put("stillHappensEventIds", res.getStillHappensEventIds());
        map.put("explanation", res.getExplanation());
        map.put("originalEventsCount", res.getOriginalEventsCount());
        map.put("simulatedEventsCount", res.getSimulatedEventsCount());
        map.put("removedEvent", res.getRemovedEvent());
        map.put("changed", res.isChanged());
        map.put("originalNarrative", res.getOriginalNarrative());
        map.put("simulatedNarrative", res.getSimulatedNarrative());
        map.put("originalNodes", res.getOriginalNodes());
        map.put("originalEdges", res.getOriginalEdges());
        map.put("simulatedNodes", res.getSimulatedNodes());
        map.put("simulatedEdges", res.getSimulatedEdges());
        return map;
    }
}
