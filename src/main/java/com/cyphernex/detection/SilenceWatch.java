package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SilenceWatch {

    public static class SourceState {
        private final String source;
        private long eventCount;
        private Instant lastSeen;

        public SourceState(String source, Instant lastSeen) {
            this.source = source;
            this.eventCount = 1;
            this.lastSeen = lastSeen != null ? lastSeen : Instant.now();
        }

        public synchronized void record(Instant timestamp) {
            this.eventCount++;
            this.lastSeen = timestamp != null ? timestamp : Instant.now();
        }

        public String getSource() {
            return source;
        }

        public long getEventCount() {
            return eventCount;
        }

        public Instant getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(Instant lastSeen) {
            this.lastSeen = lastSeen;
        }
    }

    private final Map<String, SourceState> sourceStates = new ConcurrentHashMap<>();

    public void recordEvent(String source, Instant timestamp) {
        if (source == null || source.isBlank()) return;
        sourceStates.compute(source, (k, existing) -> {
            if (existing == null) {
                return new SourceState(source, timestamp);
            } else {
                existing.record(timestamp);
                return existing;
            }
        });
    }

    public List<Detection> checkSilence(long silenceThresholdMillis) {
        List<Detection> detections = new ArrayList<>();
        Instant now = Instant.now();

        for (SourceState state : sourceStates.values()) {
            if (state.getEventCount() >= 1 && state.getLastSeen() != null) {
                long elapsed = Duration.between(state.getLastSeen(), now).toMillis();
                if (elapsed >= silenceThresholdMillis) {
                    Detection detection = new Detection(
                            UUID.randomUUID().toString(),
                            "LOG_SOURCE_SILENCE",
                            "HIGH",
                            "Expected logs from " + state.getSource() + " have stopped arriving (silent for " + (elapsed / 1000) + "s).",
                            List.of(),
                            0.95,
                            now
                    );
                    detections.add(detection);
                }
            }
        }
        return detections;
    }

    public Optional<Detection> checkSourceSilence(String source, long silenceThresholdMillis) {
        if (source == null) return Optional.empty();
        SourceState state = sourceStates.get(source);
        if (state != null && state.getEventCount() >= 1 && state.getLastSeen() != null) {
            long elapsed = Duration.between(state.getLastSeen(), Instant.now()).toMillis();
            if (elapsed >= silenceThresholdMillis) {
                Detection detection = new Detection(
                        UUID.randomUUID().toString(),
                        "LOG_SOURCE_SILENCE",
                        "HIGH",
                        "Expected logs from " + source + " have stopped arriving (silent for " + (elapsed / 1000) + "s).",
                        List.of(),
                        0.95,
                        Instant.now()
                );
                return Optional.of(detection);
            }
        }
        return Optional.empty();
    }

    public Detection simulateSilence(String source) {
        if (source == null) source = "firewall-01";
        SourceState state = sourceStates.computeIfAbsent(source, s -> new SourceState(s, Instant.now().minusSeconds(60)));
        state.setLastSeen(Instant.now().minusSeconds(60));

        return new Detection(
                UUID.randomUUID().toString(),
                "LOG_SOURCE_SILENCE",
                "HIGH",
                "Expected logs from " + source + " have stopped arriving.",
                List.of(),
                0.95,
                Instant.now()
        );
    }

    public void reset() {
        sourceStates.clear();
    }
}
