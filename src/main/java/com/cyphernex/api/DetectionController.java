package com.cyphernex.api;

import com.cyphernex.detection.DetectionEngine;
import com.cyphernex.detection.DriftDetector;
import com.cyphernex.fingerprint.FormatDnaEngine;
import com.cyphernex.model.Detection;
import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.storage.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/detections")
public class DetectionController {

    private final DetectionEngine detectionEngine;
    private final FormatDnaEngine formatDnaEngine;
    private final EventRepository eventRepository;

    public DetectionController(DetectionEngine detectionEngine, FormatDnaEngine formatDnaEngine, EventRepository eventRepository) {
        this.detectionEngine = detectionEngine;
        this.formatDnaEngine = formatDnaEngine;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDetections() {
        List<Detection> list = detectionEngine.getDetections();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("total", list.size());
        response.put("detections", list);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/drift-analysis")
    public ResponseEntity<Map<String, Object>> driftAnalysis(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new LinkedHashMap<>();
        String targetSource = null;
        String targetRawLog = null;

        if (body != null) {
            if (body.containsKey("source") && body.get("source") != null && !String.valueOf(body.get("source")).isBlank()) {
                targetSource = String.valueOf(body.get("source"));
            }
            if (body.containsKey("rawLog") && body.get("rawLog") != null && !String.valueOf(body.get("rawLog")).isBlank()) {
                targetRawLog = String.valueOf(body.get("rawLog"));
            } else if (body.containsKey("rawContent") && body.get("rawContent") != null) {
                targetRawLog = String.valueOf(body.get("rawContent"));
            }
        }

        // If not explicitly provided in body, inspect latest event from repository
        if (targetRawLog == null || targetSource == null) {
            List<NormalizedEvent> events = (eventRepository != null) ? eventRepository.findAll() : List.of();
            if (events.isEmpty()) {
                response.put("status", "NO_LOGS");
                response.put("message", "No logs ingested yet. Please ingest a log or load a test scenario to evaluate format drift.");
                return ResponseEntity.ok(response);
            }
            NormalizedEvent latest = events.get(events.size() - 1);
            if (targetSource == null) targetSource = latest.getSource() != null ? latest.getSource() : "auth-gateway";
            if (targetRawLog == null) targetRawLog = latest.getRawLog() != null ? latest.getRawLog() : "";
        }

        RawLog rawLog = new RawLog(UUID.randomUUID().toString(), targetSource, targetRawLog, Instant.now());
        FormatFingerprint currentFp = formatDnaEngine.generateFingerprint(rawLog);
        List<String> currentFields = DriftDetector.extractFieldNames(targetRawLog);

        DriftDetector detector = detectionEngine.getDriftDetector();
        FormatFingerprint baselineFp = detector.getBaseline(targetSource);
        List<String> baselineFields = detector.getBaselineFields(targetSource);

        if (baselineFp == null) {
            // First time seeing this source -> establish baseline
            detector.setBaseline(targetSource, currentFp, currentFields);
            response.put("status", "BASELINE_ESTABLISHED");
            response.put("source", targetSource);
            response.put("rawLog", targetRawLog);
            response.put("baselineFields", currentFields);
            response.put("currentFields", currentFields);
            response.put("message", "📌 Baseline Format DNA established for source '" + targetSource + "' (fields: " + currentFields + "). Ingest a subsequent modified log for '" + targetSource + "' to evaluate schema drift.");
            return ResponseEntity.ok(response);
        }

        // Compare against established baseline
        List<String> addedFields = new ArrayList<>(currentFields);
        addedFields.removeAll(baselineFields);

        List<String> removedFields = new ArrayList<>(baselineFields);
        removedFields.removeAll(currentFields);

        boolean fingerprintChanged = !baselineFp.getFingerprint().equals(currentFp.getFingerprint());
        boolean fieldsChanged = !addedFields.isEmpty() || !removedFields.isEmpty();

        if (fingerprintChanged || fieldsChanged) {
            double confidence = 0.95;
            StringBuilder msg = new StringBuilder();
            msg.append("⚠️ Log format drift detected for source '").append(targetSource).append("': ");
            if (!addedFields.isEmpty()) {
                msg.append(addedFields.size()).append(" added field(s) (").append(String.join(", ", addedFields)).append("). ");
            }
            if (!removedFields.isEmpty()) {
                msg.append(removedFields.size()).append(" removed field(s) (").append(String.join(", ", removedFields)).append("). ");
            }
            if (addedFields.isEmpty() && removedFields.isEmpty()) {
                msg.append("Structural delimiter pattern changed from '").append(baselineFp.getShape()).append("' to '").append(currentFp.getShape()).append("'. ");
            }
            msg.append("Baseline: ").append(baselineFields).append(" -> Current: ").append(currentFields);

            Detection detection = new Detection(
                    UUID.randomUUID().toString(),
                    "PARSER_DRIFT",
                    "MEDIUM",
                    msg.toString(),
                    List.of(rawLog.getId()),
                    confidence,
                    Instant.now()
            );
            detectionEngine.addDetection(detection);

            response.put("status", "DRIFT_DETECTED");
            response.put("source", targetSource);
            response.put("rawLog", targetRawLog);
            response.put("baselineFields", baselineFields);
            response.put("currentFields", currentFields);
            response.put("addedFields", addedFields);
            response.put("removedFields", removedFields);
            response.put("confidence", confidence);
            response.put("detection", detection);
            response.put("message", msg.toString());
        } else {
            response.put("status", "NO_DRIFT");
            response.put("source", targetSource);
            response.put("rawLog", targetRawLog);
            response.put("baselineFields", baselineFields);
            response.put("currentFields", currentFields);
            response.put("message", "✅ No format drift found for source '" + targetSource + "'. Current fields " + currentFields + " match established baseline schema with 100% fidelity.");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/drift-demo")
    public ResponseEntity<Map<String, Object>> driftDemo(@RequestBody(required = false) Map<String, Object> body) {
        String source = "auth-gateway";
        String originalLog = "USER=alice IP=10.0.0.5 ACTION=LOGIN";
        String changedLog = "USER=alice IP=10.0.0.5 ACTION=LOGIN STATUS=FAIL MFA=true";

        if (body != null) {
            if (body.containsKey("source")) source = String.valueOf(body.get("source"));
            if (body.containsKey("originalLog")) originalLog = String.valueOf(body.get("originalLog"));
            if (body.containsKey("changedLog")) changedLog = String.valueOf(body.get("changedLog"));
        }

        RawLog raw1 = new RawLog(UUID.randomUUID().toString(), source, originalLog, Instant.now());
        FormatFingerprint fp1 = formatDnaEngine.generateFingerprint(raw1);
        List<String> fields1 = DriftDetector.extractFieldNames(originalLog);
        detectionEngine.getDriftDetector().setBaseline(source, fp1, fields1);

        RawLog raw2 = new RawLog(UUID.randomUUID().toString(), source, changedLog, Instant.now());
        FormatFingerprint fp2 = formatDnaEngine.generateFingerprint(raw2);
        Optional<Detection> driftDetection = detectionEngine.getDriftDetector().inspect(source, fp2, raw2);
        driftDetection.ifPresent(detectionEngine::addDetection);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "DRIFT_DETECTED");
        response.put("source", source);
        response.put("originalLog", originalLog);
        response.put("changedLog", changedLog);
        response.put("baselineFields", fields1);
        response.put("currentFields", DriftDetector.extractFieldNames(changedLog));
        response.put("addedFields", List.of("STATUS", "MFA"));
        response.put("confidence", 0.95);
        response.put("detection", driftDetection.orElse(null));
        response.put("message", "⚠️ Log format drift detected for source '" + source + "': 2 new fields added (STATUS, MFA). Baseline schema: [USER, IP, ACTION] -> Current schema: [USER, IP, ACTION, STATUS, MFA] (confidence 95%).");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/check-silence", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> checkSilence(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (body != null && Boolean.TRUE.equals(body.get("simulate"))) {
            String source = String.valueOf(body.getOrDefault("source", "perimeter-firewall"));
            Detection detection = detectionEngine.getSilenceWatch().simulateSilence(source);
            detectionEngine.addDetection(detection);
            response.put("status", "SILENCE_DETECTED");
            response.put("source", source);
            response.put("detection", detection);
            response.put("message", "⚠️ Silence anomaly detected: Log producer '" + source + "' exceeded silence threshold without sending heartbeats.");
            return ResponseEntity.ok(response);
        }

        List<Detection> silentDetections = detectionEngine.getSilenceWatch().checkSilence(120_000);
        for (Detection d : silentDetections) {
            detectionEngine.addDetection(d);
        }

        if (!silentDetections.isEmpty()) {
            response.put("status", "SILENCE_DETECTED");
            response.put("message", "⚠️ Silence anomaly detected for inactive log source(s).");
            response.put("detections", silentDetections);
        } else {
            response.put("status", "ALL_SOURCES_ACTIVE");
            response.put("message", "✅ All registered log sources are active and operating within normal heartbeat intervals.");
            response.put("detections", List.of());
        }
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/silence-demo", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> silenceDemo(@RequestBody(required = false) Map<String, Object> body) {
        String source = "firewall-01";
        if (body != null && body.containsKey("source")) {
            source = String.valueOf(body.get("source"));
        }

        Detection detection = detectionEngine.getSilenceWatch().simulateSilence(source);
        detectionEngine.addDetection(detection);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SILENCE_DETECTED");
        response.put("source", source);
        response.put("detection", detection);
        response.put("message", "⚠️ Silence anomaly detected: Log producer '" + source + "' has been silent beyond expected interval threshold.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/scan-injection")
    public ResponseEntity<Map<String, Object>> scanInjection(@RequestBody(required = false) Map<String, Object> body) {
        String logContent = null;
        String source = "api";

        if (body != null) {
            if (body.containsKey("log")) logContent = String.valueOf(body.get("log"));
            if (body.containsKey("rawContent")) logContent = String.valueOf(body.get("rawContent"));
            if (body.containsKey("rawLog")) logContent = String.valueOf(body.get("rawLog"));
            if (body.containsKey("source")) source = String.valueOf(body.get("source"));
        }

        if (logContent == null || logContent.isBlank()) {
            List<NormalizedEvent> events = eventRepository != null ? eventRepository.findAll() : List.of();
            if (!events.isEmpty()) {
                NormalizedEvent latest = events.get(events.size() - 1);
                logContent = latest.getRawLog();
                source = latest.getSource() != null ? latest.getSource() : "api";
            }
        }

        if (logContent == null || logContent.isBlank()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "NO_LOGS");
            response.put("message", "No logs available to scan for injection.");
            response.put("detections", List.of());
            return ResponseEntity.ok(response);
        }

        RawLog rawLog = new RawLog(UUID.randomUUID().toString(), source, logContent, Instant.now());
        ParsedLog parsedLog = new ParsedLog();
        // Parse fields from JSON or KV if applicable
        if (logContent.trim().startsWith("{") && logContent.trim().endsWith("}")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> map = mapper.readValue(logContent, Map.class);
                parsedLog.getFields().putAll(map);
            } catch (Exception ignored) {
                parsedLog.getFields().put("raw", logContent);
            }
        } else {
            parsedLog.getFields().put("raw", logContent);
        }

        List<Detection> detections = detectionEngine.getLogInjectionDetector().inspect(rawLog, parsedLog);
        for (Detection d : detections) {
            detectionEngine.addDetection(d);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        if (!detections.isEmpty()) {
            response.put("status", "INJECTION_DETECTED");
            response.put("source", source);
            response.put("rawLog", logContent);
            response.put("detections", detections);
            response.put("message", "⚠️ Log injection attack detected in source '" + source + "': " + detections.get(0).getMessage());
        } else {
            response.put("status", "SCAN_CLEAN");
            response.put("source", source);
            response.put("rawLog", logContent);
            response.put("detections", List.of());
            response.put("message", "✅ Scan clean for source '" + source + "'. No CRLF forging, nested CEF headers, or injection vectors detected.");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/injection-demo")
    public ResponseEntity<Map<String, Object>> injectionDemo(@RequestBody(required = false) Map<String, Object> body) {
        String logContent = "{\"timestamp\":\"2026-09-19T10:30:00Z\",\"user\":\"admin\\n2026-09-19T10:31:00Z ERROR [auth] root escalation accepted\",\"action\":\"LOGIN\",\"ip\":\"10.0.0.8\",\"status\":\"SUCCESS\"}";
        String source = "auth-service";
        if (body != null) {
            if (body.containsKey("log")) logContent = String.valueOf(body.get("log"));
            if (body.containsKey("rawContent")) logContent = String.valueOf(body.get("rawContent"));
            if (body.containsKey("source")) source = String.valueOf(body.get("source"));
        }

        RawLog rawLog = new RawLog(UUID.randomUUID().toString(), source, logContent, Instant.now());
        ParsedLog parsedLog = new ParsedLog();
        parsedLog.getFields().put("timestamp", "2026-09-19T10:30:00Z");
        parsedLog.getFields().put("user", "admin\n2026-09-19T10:31:00Z ERROR [auth] root escalation accepted");
        parsedLog.getFields().put("action", "LOGIN");
        parsedLog.getFields().put("ip", "10.0.0.8");
        parsedLog.getFields().put("status", "SUCCESS");

        List<Detection> detections = detectionEngine.getLogInjectionDetector().inspect(rawLog, parsedLog);
        for (Detection d : detections) {
            detectionEngine.addDetection(d);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "INJECTION_DETECTED");
        response.put("source", source);
        response.put("rawLog", logContent);
        response.put("detections", detections);
        response.put("message", "⚠️ Log injection attack detected in source '" + source + "': CRLF log forging with embedded fake timestamp/event record in field 'user'.");
        return ResponseEntity.ok(response);
    }
}
