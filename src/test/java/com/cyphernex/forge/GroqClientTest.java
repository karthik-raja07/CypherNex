package com.cyphernex.forge;

import com.cyphernex.model.FormatFingerprint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqClientTest {

    private MetricsService metricsService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testMockFallbackWhenNoKeyConfigured() {
        GroqClient client = new GroqClient("", "", "", metricsService, objectMapper);
        assertEquals("MOCK", client.getLlmMode());
        assertFalse(client.isConfigured());

        FormatFingerprint fp = new FormatFingerprint("fp-123", "VENDOR_EVT|ts|usr|ip|act|st", "VENDOR_EVT");
        Grammar grammar = client.generateGrammar(fp, "VENDOR_EVT|2026-09-16T10:20:30Z|USR-442|10.10.2.15|LOGIN|SUCCESS");

        assertNotNull(grammar);
        assertEquals("v1", grammar.getVersion());
        assertEquals(5, grammar.getFields().size());
        assertEquals(1, metricsService.getLlmCalls());
    }

    @Test
    void testMarkdownJsonCleanupWithFences() {
        String fencedJson = "```json\n" +
                "{\n" +
                "  \"formatName\": \"VENDOR_EVT\",\n" +
                "  \"version\": \"v1\",\n" +
                "  \"extractionType\": \"DELIMITED\",\n" +
                "  \"delimiter\": \"|\",\n" +
                "  \"fields\": [\n" +
                "    {\"name\": \"timestamp\", \"type\": \"TIMESTAMP\", \"extractionRule\": \"delim:1\", \"required\": true, \"targetOcsfField\": \"time\"}\n" +
                "  ]\n" +
                "}\n" +
                "```";

        String cleaned = GroqClient.cleanMarkdownJson(fencedJson);
        assertTrue(cleaned.startsWith("{"));
        assertTrue(cleaned.endsWith("}"));
        assertFalse(cleaned.contains("```"));
    }

    @Test
    void testMarkdownJsonCleanupWithSurroundingText() {
        String wrappedJson = "Here is the declarative grammar for your log:\n" +
                "```\n" +
                "{\n" +
                "  \"formatName\": \"VENDOR_EVT\",\n" +
                "  \"version\": \"v1\",\n" +
                "  \"fields\": [\n" +
                "    {\"name\": \"user\", \"type\": \"STRING\", \"extractionRule\": \"delim:2\", \"required\": true, \"targetOcsfField\": \"user.name\"}\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "Hope this helps!";

        String cleaned = GroqClient.cleanMarkdownJson(wrappedJson);
        assertTrue(cleaned.startsWith("{"));
        assertTrue(cleaned.endsWith("}"));
    }

    @Test
    void testGrammarValidationSuccess() {
        Grammar grammar = new Grammar(
                "VENDOR_EVT",
                "v1",
                "fp-123",
                "DELIMITED",
                "|",
                null,
                List.of(new GrammarField("timestamp", "TIMESTAMP", "delim:1", true, "time"))
        );

        GroqClient.validateGrammar(grammar);
    }

    @Test
    void testGrammarValidationRejectsMalformed() {
        assertThrows(IllegalArgumentException.class, () -> GroqClient.validateGrammar(null));

        Grammar missingFormat = new Grammar(null, "v1", "fp", "DELIMITED", "|", null, List.of(new GrammarField("a", "b", "c", true, "d")));
        assertThrows(IllegalArgumentException.class, () -> GroqClient.validateGrammar(missingFormat));

        Grammar emptyFields = new Grammar("TEST", "v1", "fp", "DELIMITED", "|", null, List.of());
        assertThrows(IllegalArgumentException.class, () -> GroqClient.validateGrammar(emptyFields));
    }

    @Test
    void testGrammarValidationRejectsUnsafeTokens() {
        Grammar unsafe = new Grammar(
                "MALICIOUS",
                "v1",
                "fp",
                "DELIMITED",
                "|",
                null,
                List.of(new GrammarField("maliciousField", "STRING", "Runtime.getRuntime().exec()", true, "bad"))
        );

        assertThrows(SecurityException.class, () -> GroqClient.validateGrammar(unsafe));
    }
}
