package com.cyphernex.normalization;

import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.builtin.CsvLogParser;
import com.cyphernex.parser.builtin.JsonLogParser;
import com.cyphernex.parser.builtin.SyslogParser;
import com.cyphernex.parser.builtin.XmlLogParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcsfNormalizerTest {

    private SchemaMapper schemaMapper;
    private OcsfNormalizer normalizer;

    @BeforeEach
    void setUp() {
        schemaMapper = new SchemaMapper();
        normalizer = new OcsfNormalizer(schemaMapper);
    }

    @Test
    void testJsonToNormalizedEvent() {
        JsonLogParser jsonParser = new JsonLogParser();
        String json = "{\"timestamp\":\"2026-09-16T10:20:30Z\",\"user\":\"alice\",\"action\":\"LOGIN\",\"ip\":\"192.168.1.50\",\"severity\":\"INFO\",\"status\":\"SUCCESS\"}";
        RawLog rawLog = new RawLog("raw-1", "auth-service", json, Instant.now());
        ParsedLog parsed = jsonParser.parse(rawLog);

        NormalizedEvent event = normalizer.normalize(parsed, rawLog.getId(), "fp-json-123");

        assertNotNull(event.getEventId());
        assertEquals("alice", event.getUser());
        assertEquals("192.168.1.50", event.getSourceIp());
        assertEquals("LOGIN", event.getActivity());
        assertEquals("low", event.getSeverity());
        assertEquals("Authentication", event.getEventClass());
        assertEquals("SUCCESS", event.getAdditionalFields().get("status"));
        assertEquals(0.95, event.getConfidence());
        assertEquals("JsonLogParser", event.getProvenance().getParserName());
    }

    @Test
    void testCsvToNormalizedEvent() {
        CsvLogParser csvParser = new CsvLogParser();
        String csv = "timestamp,user,action,ip,department\n2026-09-16T10:20:30Z,bob,CONNECT,10.0.0.8,finance";
        RawLog rawLog = new RawLog("raw-2", "network-csv", csv, Instant.now());
        ParsedLog parsed = csvParser.parse(rawLog);

        NormalizedEvent event = normalizer.normalize(parsed, rawLog.getId(), "fp-csv-123");

        assertEquals("bob", event.getUser());
        assertEquals("CONNECT", event.getActivity());
        assertEquals("10.0.0.8", event.getSourceIp());
        assertEquals("finance", event.getAdditionalFields().get("department"));
        assertEquals("CsvLogParser", event.getProvenance().getParserName());
    }

    @Test
    void testXmlToNormalizedEvent() {
        XmlLogParser xmlParser = new XmlLogParser();
        String xml = "<event><timestamp>2026-09-16T10:20:30Z</timestamp><user>charlie</user><action>LOGOUT</action><host>srv01</host><device_type>laptop</device_type></event>";
        RawLog rawLog = new RawLog("raw-3", "xml-service", xml, Instant.now());
        ParsedLog parsed = xmlParser.parse(rawLog);

        NormalizedEvent event = normalizer.normalize(parsed, rawLog.getId(), "fp-xml-123");

        assertEquals("charlie", event.getUser());
        assertEquals("LOGOUT", event.getActivity());
        assertEquals("srv01", event.getHost());
        assertEquals("laptop", event.getAdditionalFields().get("device_type"));
    }

    @Test
    void testSyslogToNormalizedEvent() {
        SyslogParser syslogParser = new SyslogParser();
        String syslog = "<134>1 2026-09-16T10:01:05Z fw01.corp sshd 4102 - - Failed password for invalid user admin from 10.0.0.12 port 54321 ssh2";
        RawLog rawLog = new RawLog("raw-4", "firewall", syslog, Instant.now());
        ParsedLog parsed = syslogParser.parse(rawLog);

        NormalizedEvent event = normalizer.normalize(parsed, rawLog.getId(), "fp-syslog-123");

        assertEquals("fw01.corp", event.getHost());
        assertEquals("sshd", event.getProcess());
        assertEquals("low", event.getSeverity()); // priority 134 -> severity 6 -> low
        assertNotNull(event.getProvenance());
    }

    @Test
    void testLearnedVendorFormatToNormalizedEvent() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", "2026-09-16T10:20:30Z");
        fields.put("user", "USR-442");
        fields.put("sourceIp", "10.10.2.15");
        fields.put("action", "LOGIN");
        fields.put("status", "SUCCESS");

        ParsedLog parsed = new ParsedLog("vendor-src", "LearnedParser", "v1", fields, 1.0, "VALID");

        NormalizedEvent event = normalizer.normalize(parsed, "raw-5", "fp-vendor-123");

        assertEquals("USR-442", event.getUser());
        assertEquals("10.10.2.15", event.getSourceIp());
        assertEquals("LOGIN", event.getActivity());
        assertEquals("low", event.getSeverity());
        assertEquals("SUCCESS", event.getAdditionalFields().get("status"));
        assertEquals("LearnedParser", event.getProvenance().getParserName());
        assertEquals("v1", event.getProvenance().getParserVersion());
        assertEquals(0.95, event.getConfidence());
    }

    @Test
    void testFieldMappingAliases() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("date", "2026-09-16T10:20:30Z");
        fields.put("username", "dave");
        fields.put("client_ip", "10.0.0.99");
        fields.put("server_ip", "172.16.0.1");
        fields.put("device", "workstation-1");
        fields.put("session_id", "sess-889");
        fields.put("correlation_id", "req-101");
        fields.put("process_name", "explorer.exe");

        ParsedLog parsed = new ParsedLog("app", "CustomParser", "v1", fields, 0.95, "VALID");
        NormalizedEvent event = normalizer.normalize(parsed, "raw-6", "fp-6");

        assertEquals("dave", event.getUser());
        assertEquals("10.0.0.99", event.getSourceIp());
        assertEquals("172.16.0.1", event.getDestinationIp());
        assertEquals("workstation-1", event.getHost());
        assertEquals("sess-889", event.getSessionId());
        assertEquals("req-101", event.getRequestId());
        assertEquals("explorer.exe", event.getProcess());
    }

    @Test
    void testSeverityMapping() {
        assertEquals("low", schemaMapper.normalizeSeverity("INFO", null));
        assertEquals("medium", schemaMapper.normalizeSeverity("WARN", null));
        assertEquals("medium", schemaMapper.normalizeSeverity("WARNING", null));
        assertEquals("high", schemaMapper.normalizeSeverity("ERROR", null));
        assertEquals("high", schemaMapper.normalizeSeverity("FAILURE", null));
        assertEquals("critical", schemaMapper.normalizeSeverity("CRITICAL", null));
        assertEquals("critical", schemaMapper.normalizeSeverity("FATAL", null));
        assertEquals("custom_tag", schemaMapper.normalizeSeverity("CUSTOM_TAG", null));
    }

    @Test
    void testConfidenceLevels() {
        ParsedLog highConfidence = new ParsedLog("s1", "P1", "v1", Map.of("user", "u1"), 1.0, "VALID");
        assertEquals(0.95, schemaMapper.calculateConfidence(highConfidence, "u1", null, null));

        ParsedLog mediumConfidence = new ParsedLog("s2", "P2", "v1", Map.of("data", "val"), 0.85, "PARTIAL");
        assertEquals(0.80, schemaMapper.calculateConfidence(mediumConfidence, null, null, null));

        ParsedLog invalid = new ParsedLog("s3", "P3", "v1", Map.of(), 0.0, "INVALID");
        assertEquals(0.0, schemaMapper.calculateConfidence(invalid, null, null, null));
    }

    @Test
    void testProvenancePreservation() {
        ParsedLog parsed = new ParsedLog("src", "MyParser", "v2", Map.of("action", "START"), 0.95, "VALID");
        NormalizedEvent event = normalizer.normalize(parsed, "raw-id-99", "fp-hash-99");

        assertEquals("MyParser", event.getProvenance().getParserName());
        assertEquals("v2", event.getProvenance().getParserVersion());
        assertEquals("fp-hash-99", event.getProvenance().getFingerprint());
        assertTrue(event.getProvenance().getValidatorsPassed().contains("SCHEMA_VALIDATOR"));
        assertTrue(event.getProvenance().getValidatorsPassed().contains("OCSF_MAPPER"));
    }

    @Test
    void testUnknownFieldPreservation() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("user", "eva");
        fields.put("custom_tenant_id", "tenant-99");
        fields.put("risk_score", 42);
        fields.put("extra_flag", true);

        ParsedLog parsed = new ParsedLog("src", "JsonLogParser", "v1", fields, 1.0, "VALID");
        NormalizedEvent event = normalizer.normalize(parsed, "raw-id-7", "fp-7");

        assertEquals("eva", event.getUser());
        assertEquals("tenant-99", event.getAdditionalFields().get("custom_tenant_id"));
        assertEquals(42, event.getAdditionalFields().get("risk_score"));
        assertEquals(true, event.getAdditionalFields().get("extra_flag"));
    }
}
