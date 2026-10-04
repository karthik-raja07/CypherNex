package com.cyphernex.correlation;

import com.cyphernex.model.NormalizedEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChronoSync {

    // Source name -> Clock offset in milliseconds (relative to reference clock)
    private final Map<String, Long> sourceOffsetMap = new ConcurrentHashMap<>();
    private final Map<String, String> sourceEvidenceMap = new ConcurrentHashMap<>();

    public synchronized void calculateOffsets(List<NormalizedEvent> events) {
        if (events == null || events.size() < 2) {
            return;
        }

        // Group events by correlation key (requestId or sessionId)
        Map<String, List<NormalizedEvent>> correlatedGroups = new HashMap<>();

        for (NormalizedEvent event : events) {
            if (event.getRequestId() != null && !event.getRequestId().isBlank()) {
                correlatedGroups.computeIfAbsent("req:" + event.getRequestId(), k -> new ArrayList<>()).add(event);
            }
            if (event.getSessionId() != null && !event.getSessionId().isBlank()) {
                correlatedGroups.computeIfAbsent("ses:" + event.getSessionId(), k -> new ArrayList<>()).add(event);
            }
        }

        for (Map.Entry<String, List<NormalizedEvent>> entry : correlatedGroups.entrySet()) {
            List<NormalizedEvent> group = entry.getValue();
            if (group.size() < 2) continue;

            // Sort by timestamp
            group.sort(Comparator.comparing(NormalizedEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

            NormalizedEvent baseline = group.get(0);
            String baseSource = baseline.getSource() != null ? baseline.getSource() : "system_a";
            Instant baseTime = baseline.getTimestamp();

            if (!sourceOffsetMap.containsKey(baseSource)) {
                sourceOffsetMap.put(baseSource, 0L);
                sourceEvidenceMap.put(baseSource, "Reference clock baseline");
            }

            for (int i = 1; i < group.size(); i++) {
                NormalizedEvent other = group.get(i);
                String otherSource = other.getSource() != null ? other.getSource() : "system_b";
                Instant otherTime = other.getTimestamp();

                if (!otherSource.equals(baseSource) && baseTime != null && otherTime != null) {
                    // Time diff: otherTime - baseTime
                    long diffMillis = Duration.between(baseTime, otherTime).toMillis();
                    // Offset to align other to baseline is -diffMillis
                    long offset = -diffMillis;
                    sourceOffsetMap.put(otherSource, offset);
                    sourceEvidenceMap.put(otherSource, "Correlated on " + entry.getKey() + " with " + baseSource + " (skew: " + diffMillis + "ms)");
                }
            }
        }
    }

    public List<NormalizedEvent> applyOffsets(List<NormalizedEvent> events) {
        if (events == null) {
            return List.of();
        }
        calculateOffsets(events);

        List<NormalizedEvent> aligned = new ArrayList<>();
        for (NormalizedEvent e : events) {
            long offset = getOffsetForSource(e.getSource());
            Instant adjustedTime = (e.getTimestamp() != null) ? e.getTimestamp().plusMillis(offset) : Instant.now();

            NormalizedEvent copy = new NormalizedEvent(
                    e.getEventId(),
                    adjustedTime,
                    e.getEventClass(),
                    e.getActivity(),
                    e.getSeverity(),
                    e.getSource(),
                    e.getUser(),
                    e.getSourceIp(),
                    e.getDestinationIp(),
                    e.getHost(),
                    e.getSessionId(),
                    e.getRequestId(),
                    e.getProcess(),
                    e.getRawLogId(),
                    e.getParserVersion(),
                    e.getConfidence(),
                    e.getProvenance(),
                    e.getAdditionalFields() != null ? new HashMap<>(e.getAdditionalFields()) : new HashMap<>()
            );
            if (e.getParentEventIds() != null) {
                copy.setParentEventIds(new ArrayList<>(e.getParentEventIds()));
            }
            if (e.getParentEvidence() != null) {
                copy.setParentEvidence(new HashMap<>(e.getParentEvidence()));
            }
            aligned.add(copy);
        }

        // Sort chronologically after offset correction
        aligned.sort(Comparator.comparing(NormalizedEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
        return aligned;
    }

    public long getOffsetForSource(String source) {
        if (source == null) return 0L;
        return sourceOffsetMap.getOrDefault(source, 0L);
    }

    public List<Map<String, Object>> getClockOffsets() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sourceOffsetMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("source", entry.getKey());
            item.put("offsetMs", entry.getValue());
            item.put("offset", (entry.getValue() >= 0 ? "+" : "") + entry.getValue() + "ms");
            item.put("evidence", sourceEvidenceMap.getOrDefault(entry.getKey(), "Shared correlation identifier"));
            results.add(item);
        }
        return results;
    }

    public void setSourceOffset(String source, long offsetMs, String evidence) {
        sourceOffsetMap.put(source, offsetMs);
        if (evidence != null) {
            sourceEvidenceMap.put(source, evidence);
        }
    }

    public void reset() {
        sourceOffsetMap.clear();
        sourceEvidenceMap.clear();
    }
}
