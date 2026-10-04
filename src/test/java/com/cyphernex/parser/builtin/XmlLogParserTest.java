package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlLogParserTest {

    private XmlLogParser parser;

    @BeforeEach
    void setUp() {
        parser = new XmlLogParser();
    }

    @Test
    void testParseXml() {
        String xml = "<event><timestamp>2026-09-16T10:20:30Z</timestamp><user>john</user><action>LOGIN</action><ip>10.0.0.5</ip></event>";
        RawLog rawLog = new RawLog("1", "xml-source", xml, Instant.now());

        assertTrue(parser.canParse(rawLog));
        ParsedLog parsed = parser.parse(rawLog);

        assertEquals("XmlLogParser", parsed.getParserName());
        assertEquals("VALID", parsed.getValidationStatus());
        assertEquals("john", parsed.getFields().get("user"));
        assertEquals("LOGIN", parsed.getFields().get("action"));
        assertEquals("10.0.0.5", parsed.getFields().get("ip"));
    }
}
