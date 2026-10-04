package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SilenceWatchTest {

    private SilenceWatch silenceWatch;

    @BeforeEach
    void setUp() {
        silenceWatch = new SilenceWatch();
    }

    @Test
    @DisplayName("Test silence detection when active log source stops producing events")
    void testSilenceDetection() {
        String source = "firewall-01";
        // Record initial event 10 seconds ago
        Instant oldTime = Instant.now().minusSeconds(10);
        silenceWatch.recordEvent(source, oldTime);

        // Check with 5 second threshold -> should detect silence
        List<Detection> detections = silenceWatch.checkSilence(5000);
        assertFalse(detections.isEmpty());
        assertEquals("LOG_SOURCE_SILENCE", detections.get(0).getType());
        assertEquals("HIGH", detections.get(0).getSeverity());
        assertTrue(detections.get(0).getMessage().contains("firewall-01"));

        // Check with 60 second threshold -> should not detect silence yet
        List<Detection> none = silenceWatch.checkSilence(60000);
        assertTrue(none.isEmpty());
    }

    @Test
    @DisplayName("Test simulate silence helper for demo")
    void testSimulateSilence() {
        Detection detection = silenceWatch.simulateSilence("switch-02");
        assertNotNull(detection);
        assertEquals("LOG_SOURCE_SILENCE", detection.getType());
        assertEquals("HIGH", detection.getSeverity());
        assertTrue(detection.getMessage().contains("switch-02"));
    }
}
