package com.cyphernex.api;

import com.cyphernex.fingerprint.FormatDnaEngine;
import com.cyphernex.forge.GroqClient;
import com.cyphernex.forge.MetricsService;
import com.cyphernex.forge.ParserForge;
import com.cyphernex.ledger.LedgerGuard;
import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.normalization.OcsfNormalizer;
import com.cyphernex.parser.LogParser;
import com.cyphernex.parser.ParserRegistry;
import com.cyphernex.storage.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final FormatDnaEngine formatDnaEngine;
    private final ParserRegistry parserRegistry;
    private final ParserForge parserForge;
    private final OcsfNormalizer ocsfNormalizer;
    private final EventRepository eventRepository;
    private final LedgerGuard ledgerGuard;
    private final com.cyphernex.detection.DetectionEngine detectionEngine;
    private final MetricsService metricsService;
    private final GroqClient groqClient;
    private final ObjectMapper objectMapper;

    public LogController(
            FormatDnaEngine formatDnaEngine,
            ParserRegistry parserRegistry,
            ParserForge parserForge,
            OcsfNormalizer ocsfNormalizer,
            EventRepository eventRepository,
            LedgerGuard ledgerGuard,
            com.cyphernex.detection.DetectionEngine detectionEngine,
            MetricsService metricsService,
            GroqClient groqClient,
            ObjectMapper objectMapper) {
        this.formatDnaEngine = formatDnaEngine;
        this.parserRegistry = parserRegistry;
        this.parserForge = parserForge;
        this.ocsfNormalizer = ocsfNormalizer;
        this.eventRepository = eventRepository;
        this.ledgerGuard = ledgerGuard;
        this.detectionEngine = detectionEngine;
        this.metricsService = metricsService;
        this.groqClient = groqClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingestLogs(@RequestBody(required = false) Object payload) {
        metricsService.incrementLinesProcessed();

        String rawContent = extractRawContent(payload);
        String source = extractSource(payload);

        if (rawContent == null || rawContent.isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "INVALID_INPUT");
            error.put("message", "No log content provided in request body");
            return ResponseEntity.badRequest().body(error);
        }

        RawLog rawLog = new RawLog(UUID.randomUUID().toString(), source, rawContent, Instant.now());
        FormatFingerprint fingerprint = formatDnaEngine.generateFingerprint(rawLog);

        boolean cacheHit = false;
        String status;
        Optional<LogParser> parserOpt = parserRegistry.getParserByFingerprint(fingerprint.getFingerprint());

        if (parserOpt.isPresent() && parserOpt.get().canParse(rawLog)) {
            cacheHit = true;
            status = "CACHED";
            metricsService.incrementCacheHits();
        } else {
            cacheHit = false;
            metricsService.incrementCacheMisses();

            // First check built-in parsers
            parserOpt = parserRegistry.findParserForFormat(fingerprint.getFormat());
            if (parserOpt.isEmpty() || !parserOpt.get().canParse(rawLog)) {
                parserOpt = parserRegistry.findParserForLog(rawLog);
            }

            if (parserOpt.isPresent()) {
                status = "KNOWN";
                parserRegistry.cacheParserByFingerprint(fingerprint.getFingerprint(), parserOpt.get());
            } else {
                // Unknown format -> invoke Parser Forge
                parserOpt = parserForge.forgeAndValidate(fingerprint, List.of(rawLog)).map(p -> (LogParser) p);
                if (parserOpt.isPresent()) {
                    status = "LEARNED";
                } else {
                    status = "UNKNOWN_FORMAT";
                }
            }
        }

        if (parserOpt.isEmpty()) {
            Map<String, Object> unknownResponse = new LinkedHashMap<>();
            unknownResponse.put("status", "UNKNOWN_FORMAT");
            unknownResponse.put("fingerprint", fingerprint.getFingerprint());
            unknownResponse.put("shape", fingerprint.getShape());
            unknownResponse.put("detectedFormat", fingerprint.getFormat());
            return ResponseEntity.ok(unknownResponse);
        }

        LogParser parser = parserOpt.get();
        ParsedLog parsedLog = parser.parse(rawLog);

        if ("LEARNED".equals(status)) {
            metricsService.incrementLearnedParsersUsed();
        } else {
            metricsService.incrementBuiltinParsersUsed();
        }

        // Normalize to OCSF
        NormalizedEvent normalizedEvent = ocsfNormalizer.normalize(parsedLog, rawLog.getId(), fingerprint.getFingerprint());
        normalizedEvent.setRawLog(rawContent);
        normalizedEvent.setOrigin("LIVE");
        eventRepository.save(normalizedEvent);

        // Append to LedgerGuard tamper-evident hash chain
        if (ledgerGuard != null) {
            ledgerGuard.appendEvent(normalizedEvent);
        }

        // Run Detection Engine (Drift, SilenceWatch, Log Injection) without blocking ingestion
        if (detectionEngine != null) {
            detectionEngine.inspect(rawLog, parsedLog, normalizedEvent, fingerprint);
        }

        // Trace metadata
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("rawInput", rawContent);
        
        Map<String, Object> formatDnaTrace = new LinkedHashMap<>();
        formatDnaTrace.put("fingerprint", fingerprint.getFingerprint());
        formatDnaTrace.put("shape", fingerprint.getShape());
        formatDnaTrace.put("detectedFormat", fingerprint.getFormat());
        formatDnaTrace.put("structuralPattern", fingerprint.getStructuralPattern());
        formatDnaTrace.put("tokenCount", fingerprint.getTokenCount());
        trace.put("formatDna", formatDnaTrace);

        Map<String, Object> parserDecision = new LinkedHashMap<>();
        parserDecision.put("selectedParser", parser.getName());
        parserDecision.put("parserVersion", parser.getVersion());
        parserDecision.put("decisionType", status);
        parserDecision.put("supportedFormat", parser.getSupportedFormat());
        trace.put("parserSelection", parserDecision);

        trace.put("generatedRules", (parser instanceof com.cyphernex.forge.LearnedParser lp) ? lp.getGrammar() : "BUILT_IN_CORE_PARSER_LOGIC");
        trace.put("validationResult", parsedLog.getValidationStatus() != null ? parsedLog.getValidationStatus() : "VALID");
        trace.put("llmUsed", "LEARNED".equals(status));
        trace.put("cacheUsed", cacheHit);
        trace.put("parserRegistryUpdated", "LEARNED".equals(status));
        trace.put("fieldMappings", normalizedEvent.getFieldMappings());
        trace.put("storageState", "IN_MEMORY_SESSION_STORAGE");

        normalizedEvent.setParserTrace(trace);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("fingerprint", fingerprint.getFingerprint());
        response.put("shape", fingerprint.getShape());
        response.put("detectedFormat", fingerprint.getFormat());
        response.put("parser", parser.getName());
        response.put("parserVersion", parser.getVersion());
        response.put("llmMode", groqClient.getLlmMode());
        response.put("llmCalls", metricsService.getLlmCalls());
        response.put("cacheHit", cacheHit);
        response.put("fields", parsedLog.getFields());
        response.put("fieldMappings", normalizedEvent.getFieldMappings());
        response.put("normalizedEvent", normalizedEvent);
        response.put("trace", trace);
        response.put("storageState", "IN_MEMORY_SESSION_STORAGE");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/normalized")
    public ResponseEntity<Map<String, Object>> getNormalizedEvents() {
        Map<String, Object> response = new LinkedHashMap<>();
        List<NormalizedEvent> events = eventRepository.findAll();
        response.put("status", "SUCCESS");
        response.put("total", events.size());
        response.put("storageState", "IN_MEMORY_SESSION_STORAGE");
        response.put("events", events);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trace")
    public ResponseEntity<Map<String, Object>> getLatestTrace() {
        List<NormalizedEvent> events = eventRepository.findAll();
        Map<String, Object> response = new LinkedHashMap<>();
        if (events.isEmpty()) {
            response.put("status", "EMPTY");
            response.put("message", "No logs ingested yet to trace.");
            return ResponseEntity.ok(response);
        }
        NormalizedEvent latest = events.get(events.size() - 1);
        response.put("status", "SUCCESS");
        response.put("eventId", latest.getEventId());
        response.put("trace", latest.getParserTrace());
        response.put("fieldMappings", latest.getFieldMappings());
        response.put("storageState", "IN_MEMORY_SESSION_STORAGE");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stream")
    public ResponseEntity<Map<String, Object>> getStreamLogs() {
        List<NormalizedEvent> events = eventRepository.findAll();
        List<Map<String, Object>> stream = new java.util.ArrayList<>();
        for (NormalizedEvent event : events) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("eventId", event.getEventId());
            item.put("source", event.getSource());
            item.put("rawLog", event.getRawLog() != null ? event.getRawLog() : (event.getDescription() != null ? event.getDescription() : "Raw log"));
            item.put("origin", event.getOrigin() != null ? event.getOrigin() : "LIVE");
            item.put("eventType", event.getActivity() != null ? event.getActivity() : (event.getEventClass() != null ? event.getEventClass() : "SECURITY_EVENT"));
            item.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : Instant.now().toString());
            item.put("severity", event.getSeverity());
            item.put("user", event.getUser());
            item.put("sourceIp", event.getSourceIp());
            item.put("destinationIp", event.getDestinationIp());
            item.put("host", event.getHost());
            item.put("process", event.getProcess());
            item.put("fieldMappings", event.getFieldMappings());
            item.put("parserTrace", event.getParserTrace());
            item.put("parentEventIds", event.getParentEventIds());
            item.put("parentEvidence", event.getParentEvidence());
            item.put("additionalFields", event.getAdditionalFields());
            stream.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("total", stream.size());
        response.put("streamLogs", stream);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLogs() {
        Map<String, Object> response = new LinkedHashMap<>();
        List<NormalizedEvent> events = eventRepository.findAll();
        response.put("status", "SUCCESS");
        response.put("total", events.size());
        response.put("logs", events);
        return ResponseEntity.ok(response);
    }

    private String extractRawContent(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String str) {
            return str;
        }
        if (payload instanceof Map<?, ?> map) {
            if (map.containsKey("rawContent")) {
                return String.valueOf(map.get("rawContent"));
            }
            if (map.containsKey("raw")) {
                return String.valueOf(map.get("raw"));
            }
            if (map.containsKey("log")) {
                return String.valueOf(map.get("log"));
            }
            if (map.containsKey("message")) {
                return String.valueOf(map.get("message"));
            }
            if (map.containsKey("content")) {
                return String.valueOf(map.get("content"));
            }
            try {
                return objectMapper.writeValueAsString(map);
            } catch (Exception e) {
                return map.toString();
            }
        }
        return payload.toString();
    }

    private String extractSource(Object payload) {
        if (payload instanceof Map<?, ?> map && map.containsKey("source")) {
            return String.valueOf(map.get("source"));
        }
        return "api";
    }
}
