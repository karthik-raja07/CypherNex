package com.cyphernex.correlation;

import com.cyphernex.model.NormalizedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChronoSyncTest {

    private ChronoSync chronoSync;

    @BeforeEach
    void setUp() {
        chronoSync = new ChronoSync();
    }

    @Test
    @DisplayName("Test clock offset calculation when two systems share requestId with 4s skew")
    void testClockOffsetCalculation() {
        Instant baseTime = Instant.parse("2026-09-16T10:00:00Z");
        Instant skewedTime = Instant.parse("2026-09-16T10:00:04Z"); // 4000ms ahead

        NormalizedEvent eventA = new NormalizedEvent();
        eventA.setEventId("EVT-A");
        eventA.setSource("SystemA");
        eventA.setRequestId("REQ-CORR-1");
        eventA.setTimestamp(baseTime);

        NormalizedEvent eventB = new NormalizedEvent();
        eventB.setEventId("EVT-B");
        eventB.setSource("SystemB");
        eventB.setRequestId("REQ-CORR-1");
        eventB.setTimestamp(skewedTime);

        chronoSync.calculateOffsets(List.of(eventA, eventB));

        long offsetB = chronoSync.getOffsetForSource("SystemB");
        assertEquals(-4000L, offsetB);

        List<Map<String, Object>> offsets = chronoSync.getClockOffsets();
        assertFalse(offsets.isEmpty());

        // Applying offset should correct skewed time to 10:00:00Z
        List<NormalizedEvent> aligned = chronoSync.applyOffsets(List.of(eventA, eventB));
        assertEquals(2, aligned.size());
        assertEquals(baseTime, aligned.get(0).getTimestamp());
        assertEquals(baseTime, aligned.get(1).getTimestamp());
    }
}
