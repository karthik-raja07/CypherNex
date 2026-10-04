package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.ParserVersion;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DriftDetectorTest {

    private DriftDetector driftDetector;

    @BeforeEach
    void setUp() {
        driftDetector = new DriftDetector();
    }

    @Test
    @DisplayName("Test drift detection when structural fingerprint changes for a known source")
    void testFingerprintDriftDetection() {
        String source = "auth-gateway";
        FormatFingerprint fp1 = new FormatFingerprint("fp_original_111", "USER=<WORD> IP=<IP> ACTION=<WORD>", "PLAIN_TEXT");
        FormatFingerprint fp2 = new FormatFingerprint("fp_drifted_222", "USER=<WORD> IP=<IP> ACTION=<WORD> STATUS=<WORD>", "PLAIN_TEXT");

        RawLog raw1 = new RawLog("log-1", source, "USER=john IP=10.0.0.5 ACTION=LOGIN", Instant.now());
        RawLog raw2 = new RawLog("log-2", source, "USER=john IP=10.0.0.5 ACTION=LOGIN STATUS=SUCCESS", Instant.now());

        // First log establishes baseline -> no drift
        Optional<Detection> d1 = driftDetector.inspect(source, fp1, raw1);
        assertTrue(d1.isEmpty());

        // Same log -> no drift
        Optional<Detection> d2 = driftDetector.inspect(source, fp1, raw1);
        assertTrue(d2.isEmpty());

        // Structural change -> drift detected
        Optional<Detection> d3 = driftDetector.inspect(source, fp2, raw2);
        assertTrue(d3.isPresent());
        assertEquals("PARSER_DRIFT", d3.get().getType());
        assertEquals("MEDIUM", d3.get().getSeverity());
        assertTrue(d3.get().getMessage().contains("auth-gateway"));
    }

    @Test
    @DisplayName("Test shadow parser comparison and promotion")
    void testShadowParserComparison() {
        ParserVersion activeV1 = new ParserVersion("ParserA", "v1", "fp1", "{}", "ACTIVE", 0.70, 0.70, 0.30);
        ParserVersion shadowV2Better = new ParserVersion("ParserA", "v2", "fp2", "{}", "SHADOW", 0.95, 0.95, 0.05);

        String resultBetter = driftDetector.compareAndEvaluate(activeV1, shadowV2Better);
        assertEquals("PROMOTED", resultBetter);
        assertEquals("ACTIVE", shadowV2Better.getStatus());
        assertEquals("DEPRECATED", activeV1.getStatus());

        ParserVersion activeV2 = new ParserVersion("ParserB", "v1", "fp1", "{}", "ACTIVE", 0.90, 0.90, 0.10);
        ParserVersion shadowV2Worse = new ParserVersion("ParserB", "v2", "fp2", "{}", "SHADOW", 0.50, 0.50, 0.50);

        String resultWorse = driftDetector.compareAndEvaluate(activeV2, shadowV2Worse);
        assertEquals("RETAINED", resultWorse);
        assertEquals("ACTIVE", activeV2.getStatus());
        assertEquals("SHADOW", shadowV2Worse.getStatus());
    }
}
