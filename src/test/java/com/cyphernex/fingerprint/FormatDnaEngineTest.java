package com.cyphernex.fingerprint;

import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FormatDnaEngineTest {

    private FormatDnaEngine formatDnaEngine;

    @BeforeEach
    void setUp() {
        Tokenizer tokenizer = new Tokenizer();
        formatDnaEngine = new FormatDnaEngine(tokenizer);
    }

    @Test
    void testFingerprintConsistency() {
        RawLog log1 = new RawLog("1", "auth", "2026-09-16 10:20:30 user=john ip=192.168.1.10 action=LOGIN", Instant.now());
        RawLog log2 = new RawLog("2", "auth", "2026-09-16 10:20:31 user=alice ip=10.0.0.5 action=LOGOUT", Instant.now());

        FormatFingerprint fp1 = formatDnaEngine.generateFingerprint(log1);
        FormatFingerprint fp2 = formatDnaEngine.generateFingerprint(log2);

        assertNotNull(fp1.getFingerprint());
        assertEquals(fp1.getShape(), fp2.getShape());
        assertEquals(fp1.getFingerprint(), fp2.getFingerprint());
        assertEquals("PLAIN_TEXT", fp1.getFormat());
    }

    @Test
    void testDetectJsonFormat() {
        RawLog jsonLog = new RawLog("1", "app", "{\"timestamp\":\"2026-09-16T10:20:30Z\",\"user\":\"john\"}", Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(jsonLog);
        assertEquals("JSON", fp.getFormat());
    }

    @Test
    void testDetectSyslogFormat() {
        RawLog syslogLog = new RawLog("1", "fw", "<134>1 2026-09-16T10:01:05Z fw01.corp sshd 4102 - - Failed auth", Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(syslogLog);
        assertEquals("SYSLOG", fp.getFormat());
    }

    @Test
    void testDetectXmlFormat() {
        RawLog xmlLog = new RawLog("1", "service", "<event><user>john</user><action>LOGIN</action></event>", Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(xmlLog);
        assertEquals("XML", fp.getFormat());
    }

    @Test
    void testDetectCsvFormat() {
        RawLog csvLog = new RawLog("1", "batch", "2026-09-16T10:20:30Z,john,LOGIN,10.0.0.5", Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(csvLog);
        assertEquals("CSV", fp.getFormat());
    }
}
