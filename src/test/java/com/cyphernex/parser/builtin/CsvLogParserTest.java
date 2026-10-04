package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvLogParserTest {

    private CsvLogParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvLogParser();
    }

    @Test
    void testParseCsvWithHeader() {
        String csv = "timestamp,user,action,ip\n2026-09-16T10:20:30Z,john,LOGIN,10.0.0.5";
        RawLog rawLog = new RawLog("1", "csv-source", csv, Instant.now());

        assertTrue(parser.canParse(rawLog));
        ParsedLog parsed = parser.parse(rawLog);

        assertEquals("CsvLogParser", parsed.getParserName());
        assertEquals("VALID", parsed.getValidationStatus());
        assertEquals("john", parsed.getFields().get("user"));
        assertEquals("LOGIN", parsed.getFields().get("action"));
        assertEquals("10.0.0.5", parsed.getFields().get("ip"));
    }
}
