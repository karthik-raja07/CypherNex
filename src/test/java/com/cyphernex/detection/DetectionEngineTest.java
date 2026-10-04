package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DetectionEngineTest {

    private DetectionEngine detectionEngine;
    private DriftDetector driftDetector;
    private SilenceWatch silenceWatch;
    private LogInjectionDetector logInjectionDetector;

    @BeforeEach
    void setUp() {
        driftDetector = new DriftDetector();
        silenceWatch = new SilenceWatch();
        logInjectionDetector = new LogInjectionDetector();
        detectionEngine = new DetectionEngine(driftDetector, silenceWatch, logInjectionDetector);
    }

    @Test
    @DisplayName("Test detection engine collection from multiple detectors")
    void testDetectionEngineCollection() {
        // 1. Manually add a detection
        Detection manual = new Detection("det-1", "SUSPICIOUS_BEHAVIOR", "MEDIUM", "Test warning", List.of(), 0.8, Instant.now());
        detectionEngine.addDetection(manual);

        // 2. Ingest a log with injection
        RawLog rawLog = new RawLog("raw-1", "sys-1", "raw", Instant.now());
        ParsedLog parsedLog = new ParsedLog();
        parsedLog.getFields().put("user", "admin\n2026-09-16 ERROR root login");
        NormalizedEvent event = new NormalizedEvent();
        event.setSource("sys-1");
        FormatFingerprint fp = new FormatFingerprint("fp1", "USER=<WORD>", "PLAIN_TEXT");

        detectionEngine.inspect(rawLog, parsedLog, event, fp);

        List<Detection> all = detectionEngine.getDetections();
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(d -> "LOG_INJECTION".equals(d.getType())));
        assertTrue(all.stream().anyMatch(d -> "SUSPICIOUS_BEHAVIOR".equals(d.getType())));
    }
}
