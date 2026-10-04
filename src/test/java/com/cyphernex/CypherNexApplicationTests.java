package com.cyphernex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CypherNexApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.cyphernex.ledger.LedgerGuard ledgerGuard;

    @Autowired
    private com.cyphernex.detection.DetectionEngine detectionEngine;

    @Autowired
    private com.cyphernex.parser.ParserRegistry parserRegistry;

    @Autowired
    private com.cyphernex.forge.MetricsService metricsService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ledgerGuard.reset();
        detectionEngine.clear();
        parserRegistry.clearDynamicParsers();
        metricsService.reset();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testDashboardEndpoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void testGetParsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/parsers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void testGetParserLineageEndpoint() throws Exception {
        mockMvc.perform(get("/api/parsers/lineage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.lineage").isArray());
    }

    @Test
    void testDemoLoadAndResetEndpoints() throws Exception {
        mockMvc.perform(post("/api/demo/load"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalLogsIngested").value(6))
                .andExpect(jsonPath("$.demoLogs").isArray());

        mockMvc.perform(get("/api/demo/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.demoLogs").isArray());

        mockMvc.perform(post("/api/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/api/demo/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void testGetMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linesProcessed").exists())
                .andExpect(jsonPath("$.llmCalls").exists())
                .andExpect(jsonPath("$.parsersLearned").exists())
                .andExpect(jsonPath("$.llmMode").exists());
    }

    @Test
    void testGetDetectionsEndpoint() throws Exception {
        mockMvc.perform(get("/api/detections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.detections").isArray());
    }

    @Test
    void testDriftDemoEndpoint() throws Exception {
        mockMvc.perform(post("/api/detections/drift-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRIFT_DETECTED"))
                .andExpect(jsonPath("$.source").value("auth-gateway"))
                .andExpect(jsonPath("$.detection.type").value("PARSER_DRIFT"));
    }

    @Test
    void testSilenceDemoEndpoint() throws Exception {
        mockMvc.perform(post("/api/detections/silence-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"firewall-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SILENCE_DETECTED"))
                .andExpect(jsonPath("$.detection.type").value("LOG_SOURCE_SILENCE"));
    }

    @Test
    void testInjectionDemoEndpoint() throws Exception {
        mockMvc.perform(post("/api/detections/injection-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INJECTION_DETECTED"))
                .andExpect(jsonPath("$.detections").isArray());
    }

    @Test
    void testGetGraphEndpoint() throws Exception {
        mockMvc.perform(get("/api/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.edges").isArray())
                .andExpect(jsonPath("$.narrative").exists());
    }

    @Test
    void testReplayEndpoint() throws Exception {
        mockMvc.perform(post("/api/demo/load"))
                .andExpect(status().isOk());

        // Canonical Replay Endpoints
        mockMvc.perform(get("/api/replay/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.events").isArray());

        mockMvc.perform(post("/api/replay/default/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"some-event\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.preventedEventIds").isArray())
                .andExpect(jsonPath("$.stillHappensEventIds").isArray())
                .andExpect(jsonPath("$.eventsPrevented").isNumber())
                .andExpect(jsonPath("$.impactStoppedPercent").isNumber());

        mockMvc.perform(get("/api/replay/default/best-block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.recommendedEvent").exists())
                .andExpect(jsonPath("$.impactStoppedPercent").isNumber());

        // Legacy endpoint compatibility
        mockMvc.perform(post("/api/graph/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"some-event\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.originalEventsCount").exists())
                .andExpect(jsonPath("$.simulatedEventsCount").exists());
    }

    @Test
    void testResolveIdentityEndpoint() throws Exception {
        mockMvc.perform(post("/api/identity/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.identities").isArray());
    }

    @Test
    void testGetChronoSyncEndpoint() throws Exception {
        mockMvc.perform(get("/api/chronosync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.offsets").isArray());
    }

    @Test
    void testGetLedgerVerifyEndpoint() throws Exception {
        mockMvc.perform(get("/api/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Ledger chain verified successfully"));
    }

    @Test
    void testGetLedgerEndpoint() throws Exception {
        mockMvc.perform(get("/api/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    void testDemoTamperEndpoint() throws Exception {
        // Ingest an event first so we have records
        String payload = "{\"rawContent\":\"{\\\"user\\\":\\\"bob\\\",\\\"action\\\":\\\"ACCESS\\\"}\"}";
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Tamper record 1
        mockMvc.perform(post("/api/ledger/demo-tamper")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNumber\": 1, \"tamperedData\": \"MALICIOUS TAMPER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TAMPERED"))
                .andExpect(jsonPath("$.sequenceNumber").value(1));

        // Verify ledger should now detect tampering
        mockMvc.perform(get("/api/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.brokenSequence").value(1));
    }

    @Test
    void testGetNormalizedLogsEndpoint() throws Exception {
        mockMvc.perform(get("/api/logs/normalized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.events").isArray());
    }

    @Test
    void testIngestJsonLogWithNormalization() throws Exception {
        String payload = "{\"rawContent\":\"{\\\"timestamp\\\":\\\"2026-09-16T10:20:30Z\\\",\\\"user\\\":\\\"john\\\",\\\"action\\\":\\\"LOGIN\\\",\\\"ip\\\":\\\"10.0.0.5\\\"}\"}";

        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedFormat").value("JSON"))
                .andExpect(jsonPath("$.parser").value("JsonLogParser"))
                .andExpect(jsonPath("$.normalizedEvent.user").value("john"))
                .andExpect(jsonPath("$.normalizedEvent.activity").value("LOGIN"))
                .andExpect(jsonPath("$.normalizedEvent.sourceIp").value("10.0.0.5"))
                .andExpect(jsonPath("$.normalizedEvent.confidence").value(0.95))
                .andExpect(jsonPath("$.normalizedEvent.provenance.parserName").value("JsonLogParser"));
    }

    @Test
    void testIngestPlainTextLogWithNormalization() throws Exception {
        String payload = "{\"rawContent\":\"timestamp=2026-09-16T10:20:30Z user=alice action=LOGOUT ip=192.168.1.1\"}";

        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedFormat").value("PLAIN_TEXT"))
                .andExpect(jsonPath("$.parser").value("PlainTextParser"))
                .andExpect(jsonPath("$.normalizedEvent.user").value("alice"))
                .andExpect(jsonPath("$.normalizedEvent.activity").value("LOGOUT"))
                .andExpect(jsonPath("$.normalizedEvent.sourceIp").value("192.168.1.1"));
    }

    @Test
    void testIngestVendorUnknownLogAndCacheHit() throws Exception {
        String payload = "{\"rawContent\":\"VENDOR_EVT|2026-09-16T10:20:30Z|USR-442|10.10.2.15|LOGIN|SUCCESS\"}";

        // First ingest: learned
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LEARNED"))
                .andExpect(jsonPath("$.parser").value("LearnedParser"))
                .andExpect(jsonPath("$.cacheHit").value(false))
                .andExpect(jsonPath("$.normalizedEvent.user").value("USR-442"))
                .andExpect(jsonPath("$.normalizedEvent.sourceIp").value("10.10.2.15"))
                .andExpect(jsonPath("$.normalizedEvent.activity").value("LOGIN"))
                .andExpect(jsonPath("$.normalizedEvent.severity").value("low"))
                .andExpect(jsonPath("$.normalizedEvent.additionalFields.status").value("SUCCESS"));

        // Second ingest: cached
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CACHED"))
                .andExpect(jsonPath("$.parser").value("LearnedParser"))
                .andExpect(jsonPath("$.cacheHit").value(true))
                .andExpect(jsonPath("$.normalizedEvent.user").value("USR-442"));
    }
}
