package com.cyphernex.parser;

import com.cyphernex.model.RawLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserRegistryTest {

    private ParserRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ParserRegistry();
    }

    @Test
    void testBuiltInParsersRegistered() {
        assertEquals(5, registry.getAllParsers().size());
        assertTrue(registry.getParser("JsonLogParser").isPresent());
        assertTrue(registry.getParser("CsvLogParser").isPresent());
        assertTrue(registry.getParser("XmlLogParser").isPresent());
        assertTrue(registry.getParser("SyslogParser").isPresent());
        assertTrue(registry.getParser("PlainTextParser").isPresent());
    }

    @Test
    void testFindParserForFormat() {
        Optional<LogParser> jsonParser = registry.findParserForFormat("JSON");
        assertTrue(jsonParser.isPresent());
        assertEquals("JsonLogParser", jsonParser.get().getName());

        Optional<LogParser> syslogParser = registry.findParserForFormat("SYSLOG");
        assertTrue(syslogParser.isPresent());
        assertEquals("SyslogParser", syslogParser.get().getName());
    }

    @Test
    void testFindParserForLog() {
        RawLog jsonLog = new RawLog("1", "app", "{\"user\":\"john\"}", Instant.now());
        Optional<LogParser> parser = registry.findParserForLog(jsonLog);
        assertTrue(parser.isPresent());
        assertEquals("JsonLogParser", parser.get().getName());
    }

    @Test
    void testCacheAndRetrieveByFingerprint() {
        String fp = "test-fingerprint-12345";
        LogParser jsonParser = registry.getParser("JsonLogParser").orElseThrow();

        assertFalse(registry.getParserByFingerprint(fp).isPresent());

        registry.cacheParserByFingerprint(fp, jsonParser);
        Optional<LogParser> cached = registry.getParserByFingerprint(fp);

        assertTrue(cached.isPresent());
        assertEquals("JsonLogParser", cached.get().getName());
    }
}
