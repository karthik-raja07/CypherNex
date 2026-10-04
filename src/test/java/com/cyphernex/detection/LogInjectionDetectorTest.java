package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogInjectionDetectorTest {

    private LogInjectionDetector injectionDetector;

    @BeforeEach
    void setUp() {
        injectionDetector = new LogInjectionDetector();
    }

    @Test
    @DisplayName("Test detection of embedded CRLF and fake log level inside username")
    void testEmbeddedCrlfInjection() {
        RawLog rawLog = new RawLog("raw-1", "api", "raw content", Instant.now());
        ParsedLog parsedLog = new ParsedLog();
        parsedLog.getFields().put("user", "admin\n2026-09-16T10:00:00Z ERROR [auth] fake log injected");
        parsedLog.getFields().put("action", "LOGIN");

        List<Detection> detections = injectionDetector.inspect(rawLog, parsedLog);
        assertFalse(detections.isEmpty());
        assertEquals("LOG_INJECTION", detections.get(0).getType());
        assertEquals("HIGH", detections.get(0).getSeverity());
        assertTrue(detections.get(0).getMessage().contains("user"));
    }

    @Test
    @DisplayName("Test detection of injected key=value pairs inside a single token")
    void testEmbeddedKeyValueInjection() {
        RawLog rawLog = new RawLog("raw-2", "api", "raw content", Instant.now());
        ParsedLog parsedLog = new ParsedLog();
        parsedLog.getFields().put("user", "john user=root action=GRANT_ADMIN ip=127.0.0.1");

        List<Detection> detections = injectionDetector.inspect(rawLog, parsedLog);
        assertFalse(detections.isEmpty());
        assertEquals("LOG_INJECTION", detections.get(0).getType());
    }

    @Test
    @DisplayName("Test clean log produces no injection detection")
    void testCleanLogNoDetection() {
        RawLog rawLog = new RawLog("raw-3", "api", "user=alice action=LOGIN ip=10.0.0.1", Instant.now());
        ParsedLog parsedLog = new ParsedLog();
        parsedLog.getFields().put("user", "alice");
        parsedLog.getFields().put("action", "LOGIN");
        parsedLog.getFields().put("ip", "10.0.0.1");

        List<Detection> detections = injectionDetector.inspect(rawLog, parsedLog);
        assertTrue(detections.isEmpty());
    }
}
