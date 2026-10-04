package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlainTextParserTest {

    private PlainTextParser parser;

    @BeforeEach
    void setUp() {
        parser = new PlainTextParser();
    }

    @Test
    void testParseKeyValuePlainText() {
        String log = "timestamp=2026-09-16T10:20:30Z user=john action=LOGIN ip=10.0.0.5";
        RawLog rawLog = new RawLog("1", "app", log, Instant.now());

        assertTrue(parser.canParse(rawLog));
        ParsedLog parsed = parser.parse(rawLog);

        assertEquals("PlainTextParser", parsed.getParserName());
        assertEquals("VALID", parsed.getValidationStatus());
        assertEquals("2026-09-16T10:20:30Z", parsed.getFields().get("timestamp"));
        assertEquals("john", parsed.getFields().get("user"));
        assertEquals("LOGIN", parsed.getFields().get("action"));
        assertEquals("10.0.0.5", parsed.getFields().get("ip"));
    }

    @Test
    void testParseGenericMessageFallback() {
        String log = "System reboot initiated by root";
        RawLog rawLog = new RawLog("2", "sys", log, Instant.now());

        ParsedLog parsed = parser.parse(rawLog);

        assertEquals("PlainTextParser", parsed.getParserName());
        assertEquals("System reboot initiated by root", parsed.getFields().get("message"));
    }
}
