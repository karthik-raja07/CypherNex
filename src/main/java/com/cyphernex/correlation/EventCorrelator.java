package com.cyphernex.correlation;

import com.cyphernex.identity.IdentityWeaver;
import com.cyphernex.model.GraphNode;
import com.cyphernex.model.IdentityRecord;
import com.cyphernex.model.NormalizedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EventCorrelator {

    private final StoryGraph storyGraph;
    private final IdentityWeaver identityWeaver;
    private final ChronoSync chronoSync;

    public EventCorrelator(StoryGraph storyGraph, IdentityWeaver identityWeaver, ChronoSync chronoSync) {
        this.storyGraph = storyGraph;
        this.identityWeaver = identityWeaver;
        this.chronoSync = chronoSync;
    }

    public synchronized void correlate(List<NormalizedEvent> events) {
        correlateToGraph(events, this.storyGraph);
    }

    public synchronized StoryGraph correlateToGraph(List<NormalizedEvent> events, StoryGraph targetGraph) {
        if (targetGraph == null) {
            targetGraph = new StoryGraph();
        }

        if (events == null || events.isEmpty()) {
            targetGraph.clear();
            return targetGraph;
        }

        targetGraph.clear();

        // Step 1: Resolve identities
        List<IdentityRecord> identityClusters = identityWeaver.resolveIdentities(events);

        // Step 2: Apply ChronoSync timeline alignment & clock offsets
        List<NormalizedEvent> alignedEvents = chronoSync.applyOffsets(events);
        targetGraph.setCorrelatedEvents(alignedEvents);

        // Step 3: Create entity nodes and event nodes in target StoryGraph
        for (NormalizedEvent event : alignedEvents) {
            String eventNodeId = "event:" + event.getEventId();
            String eventLabel = event.getActivity() != null ? event.getActivity() : (event.getEventClass() != null ? event.getEventClass() : "SecurityEvent");
            targetGraph.addNode(new GraphNode(eventNodeId, "EVENT", eventLabel));

            // User Node
            if (event.getUser() != null && !event.getUser().isBlank()) {
                String canonicalUser = identityWeaver.getCanonicalIdentity(event.getUser());
                String userNodeId = "user:" + canonicalUser;
                targetGraph.addNode(new GraphNode(userNodeId, "USER", canonicalUser));
                targetGraph.addEdge(userNodeId, eventNodeId, "USER_PERFORMED");
            }

            // Host Node
            if (event.getHost() != null && !event.getHost().isBlank()) {
                String hostNodeId = "host:" + event.getHost();
                targetGraph.addNode(new GraphNode(hostNodeId, "HOST", event.getHost()));
                targetGraph.addEdge(eventNodeId, hostNodeId, "EVENT_ON_HOST");
            }

            // Source IP Node
            if (event.getSourceIp() != null && !event.getSourceIp().isBlank()) {
                String ipNodeId = "ip:" + event.getSourceIp();
                targetGraph.addNode(new GraphNode(ipNodeId, "IP", event.getSourceIp()));
                targetGraph.addEdge(eventNodeId, ipNodeId, "EVENT_FROM_IP");
            }

            // Destination IP Node
            if (event.getDestinationIp() != null && !event.getDestinationIp().isBlank()) {
                String dstIpNodeId = "ip:" + event.getDestinationIp();
                targetGraph.addNode(new GraphNode(dstIpNodeId, "IP", event.getDestinationIp()));
                targetGraph.addEdge(eventNodeId, dstIpNodeId, "EVENT_TO_IP");
            }

            // Session Node
            if (event.getSessionId() != null && !event.getSessionId().isBlank()) {
                String sessionNodeId = "session:" + event.getSessionId();
                targetGraph.addNode(new GraphNode(sessionNodeId, "SESSION", event.getSessionId()));
                targetGraph.addEdge(eventNodeId, sessionNodeId, "EVENT_IN_SESSION");
            }

            // Process Node
            if (event.getProcess() != null && !event.getProcess().isBlank()) {
                String procNodeId = "process:" + event.getProcess();
                targetGraph.addNode(new GraphNode(procNodeId, "PROCESS", event.getProcess()));
                targetGraph.addEdge(eventNodeId, procNodeId, "EVENT_SPAWNED_PROCESS");
            }
        }

        // Step 4: Create sequential correlation edges (EVENT_TRIGGERED / ATTACK_CHAIN)
        Map<String, List<NormalizedEvent>> chainGroups = new HashMap<>();
        for (NormalizedEvent event : alignedEvents) {
            String userKey = event.getUser() != null ? identityWeaver.getCanonicalIdentity(event.getUser()) : null;
            if (userKey != null && !userKey.isBlank()) {
                chainGroups.computeIfAbsent("user:" + userKey.toLowerCase(), k -> new ArrayList<>()).add(event);
            } else if (event.getSessionId() != null && !event.getSessionId().isBlank()) {
                chainGroups.computeIfAbsent("session:" + event.getSessionId().toLowerCase(), k -> new ArrayList<>()).add(event);
            } else if (event.getRequestId() != null && !event.getRequestId().isBlank()) {
                chainGroups.computeIfAbsent("req:" + event.getRequestId().toLowerCase(), k -> new ArrayList<>()).add(event);
            }
        }

        for (List<NormalizedEvent> chain : chainGroups.values()) {
            chain.sort(Comparator.comparing(NormalizedEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
            for (int i = 0; i < chain.size() - 1; i++) {
                NormalizedEvent prev = chain.get(i);
                NormalizedEvent next = chain.get(i + 1);
                String prevEventId = "event:" + prev.getEventId();
                String nextEventId = "event:" + next.getEventId();
                targetGraph.addEdge(prevEventId, nextEventId, "EVENT_TRIGGERED");

                next.addParentEventId(prev.getEventId());
                String evidence = determineEvidence(prev, next);
                next.addParentEvidence(prev.getEventId(), evidence);
            }
        }

        // Synchronize parent relationships & evidence back to input events list
        Map<String, NormalizedEvent> alignedMap = new HashMap<>();
        for (NormalizedEvent ae : alignedEvents) {
            if (ae.getEventId() != null) {
                alignedMap.put(ae.getEventId(), ae);
            }
        }
        for (NormalizedEvent orig : events) {
            if (orig.getEventId() != null && alignedMap.containsKey(orig.getEventId())) {
                NormalizedEvent ae = alignedMap.get(orig.getEventId());
                orig.setParentEventIds(ae.getParentEventIds());
                orig.setParentEvidence(ae.getParentEvidence());
            }
        }

        return targetGraph;
    }

    private String determineEvidence(NormalizedEvent prev, NormalizedEvent next) {
        List<String> reasons = new ArrayList<>();
        if (prev.getSessionId() != null && next.getSessionId() != null && prev.getSessionId().equalsIgnoreCase(next.getSessionId())) {
            reasons.add("Shared Session (" + next.getSessionId() + ")");
        }
        if (prev.getUser() != null && next.getUser() != null) {
            String canonPrev = identityWeaver != null ? identityWeaver.getCanonicalIdentity(prev.getUser()) : prev.getUser();
            String canonNext = identityWeaver != null ? identityWeaver.getCanonicalIdentity(next.getUser()) : next.getUser();
            if (canonPrev.equalsIgnoreCase(canonNext)) {
                reasons.add("Shared Identity (" + canonNext + ")");
            }
        }
        if (prev.getRequestId() != null && next.getRequestId() != null && prev.getRequestId().equalsIgnoreCase(next.getRequestId())) {
            reasons.add("Shared Request (" + next.getRequestId() + ")");
        }
        if (prev.getHost() != null && next.getHost() != null && prev.getHost().equalsIgnoreCase(next.getHost())) {
            reasons.add("Shared Host (" + next.getHost() + ")");
        }
        if (reasons.isEmpty()) {
            reasons.add("ChronoSync Temporal Causality");
        }
        return String.join(" • ", reasons);
    }

    public StoryGraph getStoryGraph() {
        return storyGraph;
    }

    public IdentityWeaver getIdentityWeaver() {
        return identityWeaver;
    }

    public ChronoSync getChronoSync() {
        return chronoSync;
    }
}
