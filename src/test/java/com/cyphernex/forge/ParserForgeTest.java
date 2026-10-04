package com.cyphernex.forge;

import com.cyphernex.fingerprint.FormatDnaEngine;
import com.cyphernex.fingerprint.Tokenizer;
import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.LogParser;
import com.cyphernex.parser.ParserRegistry;
import com.cyphernex.storage.ParserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserForgeTest {

    private ParserForge parserForge;
    private GroqClient groqClient;
    private GrammarCompiler grammarCompiler;
    private ParserRegistry parserRegistry;
    private ParserRepository parserRepository;
    private MetricsService metricsService;
    private FormatDnaEngine formatDnaEngine;

    private static final String VENDOR_LOG = "VENDOR_EVT|2026-09-16T10:20:30Z|USR-442|10.10.2.15|LOGIN|SUCCESS";

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
        groqClient = new GroqClient("", "", "", metricsService, new ObjectMapper());
        grammarCompiler = new GrammarCompiler();
        parserRegistry = new ParserRegistry();
        parserRepository = new ParserRepository();
        formatDnaEngine = new FormatDnaEngine(new Tokenizer());

        parserForge = new ParserForge(
                groqClient,
                grammarCompiler,
                parserRegistry,
                parserRepository,
                metricsService,
                new ObjectMapper()
        );
    }

    @Test
    void test1_UnknownFormatTriggersParserForge() {
        RawLog rawLog = new RawLog("1", "vendor", VENDOR_LOG, Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(rawLog);

        assertFalse(parserRegistry.getParserByFingerprint(fp.getFingerprint()).isPresent());

        Optional<LearnedParser> learned = parserForge.forgeAndValidate(fp, List.of(rawLog));
        assertTrue(learned.isPresent());
    }

    @Test
    void test2_and_3_VendorFormatLearnedAndExtractsExpectedFields() {
        RawLog rawLog = new RawLog("1", "vendor", VENDOR_LOG, Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(rawLog);

        LearnedParser parser = parserForge.forgeAndValidate(fp, List.of(rawLog)).orElseThrow();
        ParsedLog parsed = parser.parse(rawLog);

        assertEquals("VALID", parsed.getValidationStatus());
        assertEquals("2026-09-16T10:20:30Z", parsed.getFields().get("timestamp"));
        assertEquals("USR-442", parsed.getFields().get("user"));
        assertEquals("10.10.2.15", parsed.getFields().get("sourceIp"));
        assertEquals("LOGIN", parsed.getFields().get("action"));
        assertEquals("SUCCESS", parsed.getFields().get("status"));
    }

    @Test
    void test4_ParserVersionIsV1() {
        RawLog rawLog = new RawLog("1", "vendor", VENDOR_LOG, Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(rawLog);

        LearnedParser parser = parserForge.forgeAndValidate(fp, List.of(rawLog)).orElseThrow();
        assertEquals("v1", parser.getVersion());
    }

    @Test
    void test5_LearnedFingerprintIsCached() {
        RawLog rawLog = new RawLog("1", "vendor", VENDOR_LOG, Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(rawLog);

        parserForge.forgeAndValidate(fp, List.of(rawLog));

        Optional<LogParser> cached = parserRegistry.getParserByFingerprint(fp.getFingerprint());
        assertTrue(cached.isPresent());
        assertEquals("LearnedParser", cached.get().getName());
    }

    @Test
    void test6_RepeatedVendorLogsDoNotCallGroqAgain() {
        RawLog rawLog = new RawLog("1", "vendor", VENDOR_LOG, Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(rawLog);

        // First call triggers learning -> llmCalls = 1
        parserForge.forgeAndValidate(fp, List.of(rawLog));
        assertEquals(1, metricsService.getLlmCalls());

        // Check if parser is cached
        Optional<LogParser> cached = parserRegistry.getParserByFingerprint(fp.getFingerprint());
        assertTrue(cached.isPresent());

        // Subsequent log uses cached parser directly
        ParsedLog parsedAgain = cached.get().parse(rawLog);
        assertNotNull(parsedAgain);
        assertEquals(1, metricsService.getLlmCalls()); // Still 1
    }

    @Test
    void test7_InvalidGrammarIsRejected() {
        RawLog rawLog = new RawLog("1", "vendor", "MALFORMED_DATA", Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(rawLog);

        // Mock a faulty client that returns grammar expecting fields that don't exist
        GroqClient faultyClient = new GroqClient("", "", "", metricsService, new ObjectMapper()) {
            @Override
            public Grammar generateGrammar(FormatFingerprint f, String s) {
                List<GrammarField> fields = new ArrayList<>();
                fields.add(new GrammarField("missing1", "STRING", "delim:99", true, "f1"));
                fields.add(new GrammarField("missing2", "STRING", "delim:100", true, "f2"));
                return new Grammar("FAULTY", "v1", "fp", "DELIMITED", "|", null, fields);
            }
        };

        ParserForge faultyForge = new ParserForge(
                faultyClient,
                grammarCompiler,
                parserRegistry,
                parserRepository,
                metricsService,
                new ObjectMapper()
        );

        Optional<LearnedParser> result = faultyForge.forgeAndValidate(fp, List.of(rawLog));
        assertFalse(result.isPresent());
    }

    @Test
    void test8_MetricsCorrectlyShowOneLearningCall() {
        RawLog rawLog = new RawLog("1", "vendor", VENDOR_LOG, Instant.now());
        FormatFingerprint fp = formatDnaEngine.generateFingerprint(rawLog);

        assertEquals(0, metricsService.getLlmCalls());
        assertEquals(0, metricsService.getParsersLearned());

        parserForge.forgeAndValidate(fp, List.of(rawLog));

        assertEquals(1, metricsService.getLlmCalls());
        assertEquals(1, metricsService.getParsersLearned());
    }
}
