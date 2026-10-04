package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyslogParserTest {

    private SyslogParser parser;

    @BeforeEach
    void setUp() {
        parser = new SyslogParser();
    }

    @Test
    void testParseRfc5424Syslog() {
        String syslog = "<134>1 2026-09-16T10:01:05Z fw01.corp sshd 4102 - - Failed password for invalid user admin from 10.0.0.12 port 54321 ssh2";
        RawLog rawLog = new RawLog("1", "fw", syslog, Instant.now());

        assertTrue(parser.canParse(rawLog));
        ParsedLog parsed = parser.parse(rawLog);

        assertEquals("SyslogParser", parsed.getParserName());
        assertEquals("VALID", parsed.getValidationStatus());
        assertEquals(134, parsed.getFields().get("priority"));
        assertEquals(16, parsed.getFields().get("facility"));
        assertEquals(6, parsed.getFields().get("severity"));
        assertEquals("fw01.corp", parsed.getFields().get("hostname"));
        assertEquals("sshd", parsed.getFields().get("appName"));
        assertEquals("4102", parsed.getFields().get("procId"));
        assertTrue(((String) parsed.getFields().get("message")).contains("Failed password"));
    }

    @Test
    void testParseRfc3164Syslog() {
        String syslog = "<34>Oct 11 22:14:15 mymachine su[1234]: 'su root' failed for lonvick on /dev/pts/8";
        RawLog rawLog = new RawLog("1", "sys", syslog, Instant.now());

        assertTrue(parser.canParse(rawLog));
        ParsedLog parsed = parser.parse(rawLog);

        assertEquals("SyslogParser", parsed.getParserName());
        assertEquals("VALID", parsed.getValidationStatus());
        assertEquals(34, parsed.getFields().get("priority"));
        assertEquals("mymachine", parsed.getFields().get("hostname"));
        assertEquals("su", parsed.getFields().get("appName"));
        assertEquals("1234", parsed.getFields().get("procId"));
    }
}
