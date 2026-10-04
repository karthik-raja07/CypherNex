package com.cyphernex.forge;

import com.cyphernex.model.FormatFingerprint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqClient(
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}") String apiUrl,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model,
            MetricsService metricsService,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.apiUrl = apiUrl != null && !apiUrl.isBlank() ? apiUrl.trim() : "https://api.groq.com/openai/v1/chat/completions";
        this.model = model != null && !model.isBlank() ? model.trim() : "llama-3.3-70b-versatile";
        this.metricsService = metricsService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getLlmMode() {
        return isConfigured() ? "GROQ" : "MOCK";
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public Grammar generateGrammar(FormatFingerprint fingerprint, String sampleLog) {
        metricsService.incrementLlmCalls();

        if (isConfigured()) {
            return callGroqApi(fingerprint, sampleLog);
        } else {
            return generateMockGrammar(fingerprint, sampleLog);
        }
    }

    public Grammar callGroqApi(FormatFingerprint fingerprint, String sampleLog) {
        String fpStr = fingerprint != null ? fingerprint.getFingerprint() : "unknown-fingerprint";
        String sample = sampleLog != null ? sampleLog.trim() : "";

        String systemInstruction = "You are a declarative log parser generator for the CypherNex log intelligence engine.\n"
                + "Analyze the sample log and output ONLY a declarative JSON grammar.\n"
                + "CRITICAL RULES:\n"
                + "1. Output MUST be valid JSON only. Never output commentary, preamble, or explanations.\n"
                + "2. NEVER generate Java code, executable code, shell scripts, or SQL.\n"
                + "3. Output MUST follow this exact schema:\n"
                + "{\n"
                + "  \"formatName\": \"string\",\n"
                + "  \"version\": \"v1\",\n"
                + "  \"extractionType\": \"DELIMITED\" or \"REGEX\",\n"
                + "  \"delimiter\": \"string\",\n"
                + "  \"regexPattern\": \"string\",\n"
                + "  \"fields\": [\n"
                + "    {\n"
                + "      \"name\": \"string\",\n"
                + "      \"type\": \"TIMESTAMP\"|\"STRING\"|\"IP\"|\"NUMBER\",\n"
                + "      \"extractionRule\": \"delim:<index>\" or \"group:<index>\",\n"
                + "      \"required\": true,\n"
                + "      \"targetOcsfField\": \"string\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        String userPrompt = "Generate a declarative log parsing grammar for the following log:\n"
                + "Fingerprint: " + fpStr + "\n"
                + "Sample Log: " + sample + "\n\n"
                + "Extract all key security fields present (e.g. timestamp, user, sourceIp, action, status).\n"
                + "For delimited logs (e.g. separated by | or ,), use extractionRule 'delim:1', 'delim:2', etc. (1-indexed based on position).\n"
                + "Output JSON only.";

        try {
            String requestBody = objectMapper.writeValueAsString(new GroqChatRequest(
                    model,
                    List.of(
                            new GroqMessage("system", systemInstruction),
                            new GroqMessage("user", userPrompt)
                    ),
                    0.1
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new GroqApiException("Groq connection error: " + e.getMessage(), e);
            }

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode == 401) {
                throw new GroqApiException("Groq authentication error: Invalid or unauthorized API key (HTTP 401)", 401);
            } else if (statusCode == 429) {
                throw new GroqApiException("Groq rate limit or quota exceeded (HTTP 429)", 429);
            } else if (statusCode == 400) {
                throw new GroqApiException("Groq invalid request or unsupported model (HTTP 400): " + extractErrorMessage(responseBody), 400);
            } else if (statusCode < 200 || statusCode >= 300) {
                throw new GroqApiException("Groq API error (HTTP " + statusCode + "): " + extractErrorMessage(responseBody), statusCode);
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new GroqApiException("Groq returned empty response choices");
            }

            String rawContent = choices.get(0).path("message").path("content").asText();
            String cleanedJson = cleanMarkdownJson(rawContent);

            Grammar grammar = objectMapper.readValue(cleanedJson, Grammar.class);
            validateGrammar(grammar);

            if (fingerprint != null && (grammar.getFingerprint() == null || grammar.getFingerprint().isBlank())) {
                grammar.setFingerprint(fingerprint.getFingerprint());
            }

            return grammar;

        } catch (GroqApiException e) {
            log.error("Groq API error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse or execute Groq request: {}", e.getMessage());
            throw new GroqApiException("Groq processing error: " + e.getMessage(), e);
        }
    }

    public static String cleanMarkdownJson(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Groq response content is empty");
        }
        String trimmed = content.trim();

        if (trimmed.contains("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.indexOf("```", start);
            if (end != -1) {
                trimmed = trimmed.substring(start, end);
            } else {
                trimmed = trimmed.substring(start);
            }
        } else if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.indexOf("```", start);
            if (end != -1) {
                trimmed = trimmed.substring(start, end);
            } else {
                trimmed = trimmed.substring(start);
            }
        }

        trimmed = trimmed.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed.trim();
    }

    public static void validateGrammar(Grammar grammar) {
        if (grammar == null) {
            throw new IllegalArgumentException("Grammar cannot be null");
        }
        if (grammar.getFormatName() == null || grammar.getFormatName().isBlank()) {
            throw new IllegalArgumentException("Grammar formatName is missing or blank");
        }
        if (grammar.getFields() == null || grammar.getFields().isEmpty()) {
            throw new IllegalArgumentException("Grammar fields collection is empty");
        }

        for (GrammarField field : grammar.getFields()) {
            if (field.getName() == null || field.getName().isBlank()) {
                throw new IllegalArgumentException("Grammar field contains empty name");
            }
            if (field.getExtractionRule() == null || field.getExtractionRule().isBlank()) {
                throw new IllegalArgumentException("Grammar field " + field.getName() + " has empty extraction rule");
            }

            // Security check against injection/code execution strings
            String combined = (field.getName() + " " + field.getExtractionRule()).toLowerCase(Locale.ROOT);
            if (combined.contains("runtime") || combined.contains("exec") || combined.contains("system.")
                    || combined.contains("processbuilder") || combined.contains("<script") || combined.contains("drop table")) {
                throw new SecurityException("Grammar contains prohibited unsafe tokens in field definition: " + field.getName());
            }
        }
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "No response body";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("error") && node.get("error").has("message")) {
                return node.get("error").get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    public Grammar generateMockGrammar(FormatFingerprint fingerprint, String sampleLog) {
        String fp = fingerprint != null ? fingerprint.getFingerprint() : "unknown-fp";
        String trimmed = sampleLog != null ? sampleLog.trim() : "";

        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|");
            String formatName = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : "VENDOR_EVT";

            List<GrammarField> fields = new ArrayList<>();
            fields.add(new GrammarField("timestamp", "TIMESTAMP", "delim:1", true, "time"));
            fields.add(new GrammarField("user", "STRING", "delim:2", true, "user.name"));
            fields.add(new GrammarField("sourceIp", "IP", "delim:3", true, "src_endpoint.ip"));
            fields.add(new GrammarField("action", "STRING", "delim:4", true, "activity_name"));
            fields.add(new GrammarField("status", "STRING", "delim:5", true, "status"));

            return new Grammar(
                    formatName,
                    "v1",
                    fp,
                    "DELIMITED",
                    "|",
                    null,
                    fields
            );
        }

        List<GrammarField> fallbackFields = new ArrayList<>();
        fallbackFields.add(new GrammarField("message", "STRING", "delim:0", true, "message"));
        return new Grammar(
                "GENERIC_DELIMITED",
                "v1",
                fp,
                "DELIMITED",
                "|",
                null,
                fallbackFields
        );
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModel() {
        return model;
    }

    private static class GroqChatRequest {
        public String model;
        public List<GroqMessage> messages;
        public double temperature;

        public GroqChatRequest(String model, List<GroqMessage> messages, double temperature) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
        }
    }

    private static class GroqMessage {
        public String role;
        public String content;

        public GroqMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
