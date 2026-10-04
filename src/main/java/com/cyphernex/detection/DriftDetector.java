package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParserVersion;
import com.cyphernex.model.RawLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DriftDetector {

    // Source -> Baseline Fingerprint
    private final Map<String, FormatFingerprint> sourceBaselineMap = new ConcurrentHashMap<>();
    // Source -> Baseline Fields
    private final Map<String, List<String>> sourceBaselineFieldsMap = new ConcurrentHashMap<>();
    // Source -> Full History of Logs & Fingerprints
    private final Map<String, List<Map<String, Object>>> sourceHistoryMap = new ConcurrentHashMap<>();
    // Global History of Drift Reports
    private final List<Map<String, Object>> driftReports = new ArrayList<>();

    public synchronized Optional<Detection> inspect(String source, FormatFingerprint fingerprint, RawLog rawLog) {
        if (source == null || fingerprint == null) {
            return Optional.empty();
        }

        String rawContent = (rawLog != null) ? rawLog.getRawContent() : "";
        List<String> currentFields = extractFieldNames(rawContent);

        // Record history
        Map<String, Object> historyEntry = new LinkedHashMap<>();
        historyEntry.put("timestamp", Instant.now().toString());
        historyEntry.put("fingerprint", fingerprint.getFingerprint());
        historyEntry.put("shape", fingerprint.getShape());
        historyEntry.put("fields", currentFields);
        historyEntry.put("rawLog", rawContent);
        sourceHistoryMap.computeIfAbsent(source, k -> new ArrayList<>()).add(historyEntry);

        FormatFingerprint baseline = sourceBaselineMap.get(source);
        List<String> baselineFields = sourceBaselineFieldsMap.get(source);

        if (baseline == null) {
            // First log for this source -> establish baseline
            sourceBaselineMap.put(source, fingerprint);
            sourceBaselineFieldsMap.put(source, currentFields);
            return Optional.empty();
        }

        // Compare current log with baseline
        List<String> addedFields = new ArrayList<>(currentFields);
        addedFields.removeAll(baselineFields != null ? baselineFields : List.of());

        List<String> removedFields = new ArrayList<>(baselineFields != null ? baselineFields : List.of());
        removedFields.removeAll(currentFields);

        boolean fingerprintChanged = !baseline.getFingerprint().equals(fingerprint.getFingerprint());
        boolean fieldsChanged = !addedFields.isEmpty() || !removedFields.isEmpty();

        if (fingerprintChanged || fieldsChanged) {
            Instant now = Instant.now();
            double confidence = 0.95;

            StringBuilder msg = new StringBuilder();
            msg.append("Log format drift detected for source '").append(source).append("': ");
            if (!addedFields.isEmpty()) {
                msg.append(addedFields.size()).append(" added field(s) (").append(String.join(", ", addedFields)).append("). ");
            }
            if (!removedFields.isEmpty()) {
                msg.append(removedFields.size()).append(" removed field(s) (").append(String.join(", ", removedFields)).append("). ");
            }
            if (addedFields.isEmpty() && removedFields.isEmpty()) {
                msg.append("Structural delimiter pattern changed from '").append(baseline.getShape()).append("' to '").append(fingerprint.getShape()).append("'. ");
            }
            msg.append("Baseline: ").append(baselineFields).append(" -> Current: ").append(currentFields);

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("source", source);
            report.put("baselineFields", baselineFields != null ? baselineFields : List.of());
            report.put("currentFields", currentFields);
            report.put("addedFields", addedFields);
            report.put("removedFields", removedFields);
            report.put("oldFingerprint", baseline.getFingerprint());
            report.put("newFingerprint", fingerprint.getFingerprint());
            report.put("oldShape", baseline.getShape());
            report.put("newShape", fingerprint.getShape());
            report.put("driftConfidence", confidence);
            report.put("driftDetected", true);
            report.put("message", msg.toString());
            report.put("timestamp", now.toString());
            driftReports.add(report);

            String logId = (rawLog != null) ? rawLog.getId() : UUID.randomUUID().toString();
            Detection detection = new Detection(
                    UUID.randomUUID().toString(),
                    "PARSER_DRIFT",
                    "MEDIUM",
                    msg.toString(),
                    List.of(logId),
                    confidence,
                    now
            );

            return Optional.of(detection);
        }

        return Optional.empty();
    }

    public static List<String> extractFieldNames(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return List.of();
        }
        String trimmed = rawContent.trim();
        Set<String> fields = new LinkedHashSet<>();

        // 1. JSON check: extract keys
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            Pattern jsonKeyPattern = Pattern.compile("\"([a-zA-Z0-9_.-]+)\"\\s*:");
            Matcher m = jsonKeyPattern.matcher(trimmed);
            while (m.find()) {
                fields.add(m.group(1));
            }
            if (!fields.isEmpty()) return new ArrayList<>(fields);
        }

        // 2. Key-Value pairs: USER=alice IP=10.0.0.5 or key="value" or key:value
        Pattern kvPattern = Pattern.compile("(?i)(?:^|[\\s,|])([a-zA-Z0-9_.-]+)=[\"']?[^\\s,;|\"']+[\"']?");
        Matcher kvMatcher = kvPattern.matcher(trimmed);
        while (kvMatcher.find()) {
            fields.add(kvMatcher.group(1));
        }
        if (!fields.isEmpty()) return new ArrayList<>(fields);

        // 3. XML tags
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            Pattern xmlPattern = Pattern.compile("<([a-zA-Z0-9_.-]+)(?:\\s+[^>]*)?>");
            Matcher xmlMatcher = xmlPattern.matcher(trimmed);
            while (xmlMatcher.find()) {
                String tag = xmlMatcher.group(1);
                if (!tag.startsWith("/") && !tag.equalsIgnoreCase("log")) {
                    fields.add(tag);
                }
            }
            if (!fields.isEmpty()) return new ArrayList<>(fields);
        }

        // 4. Pipe-delimited or CSV columns
        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|");
            for (int i = 0; i < parts.length; i++) {
                fields.add("field_" + (i + 1));
            }
            return new ArrayList<>(fields);
        } else if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            for (int i = 0; i < parts.length; i++) {
                fields.add("col_" + (i + 1));
            }
            return new ArrayList<>(fields);
        }

        return List.of("raw_message");
    }

    public List<Detection> detectDrift(List<NormalizedEvent> events) {
        return new ArrayList<>(driftReports.stream().map(r -> new Detection(
                UUID.randomUUID().toString(),
                "PARSER_DRIFT",
                "MEDIUM",
                String.valueOf(r.getOrDefault("message", "Drift detected")),
                List.of(),
                0.95,
                Instant.now()
        )).toList());
    }

    public String compareAndEvaluate(ParserVersion activeVersion, ParserVersion shadowVersion) {
        if (activeVersion == null && shadowVersion == null) return "ACTIVE";
        if (activeVersion == null) return "ACTIVE";
        if (shadowVersion == null) return "ACTIVE";

        double activeScore = (activeVersion.getExtractionRate() * 0.4) + (activeVersion.getValidationRate() * 0.4) - (activeVersion.getNullRate() * 0.2);
        double shadowScore = (shadowVersion.getExtractionRate() * 0.4) + (shadowVersion.getValidationRate() * 0.4) - (shadowVersion.getNullRate() * 0.2);

        if (shadowScore > activeScore) {
            shadowVersion.setStatus("ACTIVE");
            activeVersion.setStatus("DEPRECATED");
            return "PROMOTED";
        } else {
            shadowVersion.setStatus("SHADOW");
            activeVersion.setStatus("ACTIVE");
            return "RETAINED";
        }
    }

    public void setBaseline(String source, FormatFingerprint fingerprint) {
        if (source != null && fingerprint != null) {
            sourceBaselineMap.put(source, fingerprint);
            sourceBaselineFieldsMap.put(source, extractFieldNames(fingerprint.getShape()));
        }
    }

    public void setBaseline(String source, FormatFingerprint fingerprint, List<String> fields) {
        if (source != null && fingerprint != null) {
            sourceBaselineMap.put(source, fingerprint);
            sourceBaselineFieldsMap.put(source, fields != null ? fields : extractFieldNames(fingerprint.getShape()));
        }
    }

    public FormatFingerprint getBaseline(String source) {
        return sourceBaselineMap.get(source);
    }

    public List<String> getBaselineFields(String source) {
        return sourceBaselineFieldsMap.getOrDefault(source, List.of());
    }

    public List<Map<String, Object>> getSourceHistory(String source) {
        return sourceHistoryMap.getOrDefault(source, List.of());
    }

    public List<Map<String, Object>> getDriftReports() {
        return new ArrayList<>(driftReports);
    }

    public void reset() {
        sourceBaselineMap.clear();
        sourceBaselineFieldsMap.clear();
        sourceHistoryMap.clear();
        driftReports.clear();
    }
}
