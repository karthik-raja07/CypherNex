package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogInjectionDetector {

    // Regex patterns for suspicious embedded log lines & injection vectors
    private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]|%0[aAdD]|\\\\n|\\\\r");
    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile("(?i)\\b(ERROR|WARN|WARNING|CRITICAL|ALERT|EMERGENCY|FATAL)\\b");
    private static final Pattern SYSLOG_HEADER_PATTERN = Pattern.compile("<\\d{1,3}>\\d?\\s*");
    private static final Pattern CEF_PATTERN = Pattern.compile("(?i)CEF:\\d+\\|[^|]+\\|[^|]+\\|");
    private static final Pattern EMBEDDED_TIMESTAMP_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?\\b");
    private static final Pattern EMBEDDED_KV_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+=[^\\s]+(?:\\s+[a-zA-Z0-9_.-]+=[^\\s]+)+");
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    private static final Pattern PRIVILEGE_INJECTION_PATTERN = Pattern.compile("(?i)\\b(root\\s+escalat|admin\\s+granted|privilege\\s+escalat|sudo\\s+bypass|shadow\\s+access)\\b");

    public List<Detection> inspect(RawLog rawLog, ParsedLog parsedLog) {
        List<Detection> detections = new ArrayList<>();
        String logId = (rawLog != null) ? rawLog.getId() : UUID.randomUUID().toString();
        String rawContent = (rawLog != null && rawLog.getRawContent() != null) ? rawLog.getRawContent() : "";

        // 1. Inspect individual parsed fields
        if (parsedLog != null && parsedLog.getFields() != null && !parsedLog.getFields().isEmpty()) {
            for (Map.Entry<String, Object> entry : parsedLog.getFields().entrySet()) {
                String fieldName = entry.getKey();
                Object valObj = entry.getValue();
                if (valObj == null) continue;

                String val = String.valueOf(valObj);
                InspectionResult res = evaluateFieldValue(fieldName, val);
                if (res.isSuspicious) {
                    Detection d = new Detection(
                            UUID.randomUUID().toString(),
                            "LOG_INJECTION",
                            res.severity,
                            "Log injection attack detected in field '" + fieldName + "': " + res.reason + " [Matched snippet: \"" + sanitizeSnippet(res.snippet) + "\"]",
                            List.of(logId),
                            res.confidence,
                            Instant.now()
                    );
                    detections.add(d);
                }
            }
        }

        // 2. If no field-level injection detected or raw log was provided, inspect raw content
        if (detections.isEmpty() && !rawContent.isBlank()) {
            InspectionResult rawRes = evaluateRawContent(rawContent);
            if (rawRes.isSuspicious) {
                Detection d = new Detection(
                        UUID.randomUUID().toString(),
                        "LOG_INJECTION",
                        rawRes.severity,
                        "Log injection attack detected: " + rawRes.reason + " [Matched snippet: \"" + sanitizeSnippet(rawRes.snippet) + "\"]",
                        List.of(logId),
                        rawRes.confidence,
                        Instant.now()
                );
                detections.add(d);
            }
        }

        return detections;
    }

    public InspectionResult evaluateFieldValue(String fieldName, String value) {
        if (value == null || value.length() < 3) {
            return InspectionResult.clean();
        }

        // 1. CRLF log forgery with embedded fake timestamp / log level / privilege keywords
        if (CRLF_PATTERN.matcher(value).find()) {
            if (LOG_LEVEL_PATTERN.matcher(value).find() || EMBEDDED_TIMESTAMP_PATTERN.matcher(value).find() || PRIVILEGE_INJECTION_PATTERN.matcher(value).find()) {
                return new InspectionResult(true, "HIGH", "CRLF log forging with embedded fake timestamp/event record", value, 0.98);
            }
            return new InspectionResult(true, "HIGH", "CRLF control character newline injection in field '" + fieldName + "'", value, 0.90);
        }

        // 2. Nested CEF injection
        if (CEF_PATTERN.matcher(value).find()) {
            return new InspectionResult(true, "HIGH", "Nested CEF (Common Event Format) record injection detected", extractSnippet(value, CEF_PATTERN), 0.95);
        }

        // 3. Nested Syslog header injection
        if (SYSLOG_HEADER_PATTERN.matcher(value).find() && (value.contains("sshd") || value.contains("Failed password") || LOG_LEVEL_PATTERN.matcher(value).find())) {
            return new InspectionResult(true, "HIGH", "Nested Syslog priority header injection detected", extractSnippet(value, SYSLOG_HEADER_PATTERN), 0.95);
        }

        // 4. Multiple key=value pairs injected inside a single simple token field like 'user', 'ip', 'action', 'host'
        if ("user".equalsIgnoreCase(fieldName) || "ip".equalsIgnoreCase(fieldName) || "sourceIp".equalsIgnoreCase(fieldName) || "action".equalsIgnoreCase(fieldName) || "host".equalsIgnoreCase(fieldName)) {
            if (EMBEDDED_KV_PATTERN.matcher(value).find()) {
                return new InspectionResult(true, "HIGH", "Embedded key-value sequence forged inside token field '" + fieldName + "'", value, 0.92);
            }
            if (LOG_LEVEL_PATTERN.matcher(value).find() && (value.contains("[") || value.contains(":") || value.contains("root") || value.contains("admin"))) {
                return new InspectionResult(true, "HIGH", "Forged log level and status marker inside token field '" + fieldName + "'", value, 0.94);
            }
            if (PRIVILEGE_INJECTION_PATTERN.matcher(value).find()) {
                return new InspectionResult(true, "CRITICAL", "Privilege manipulation keyword injected inside field '" + fieldName + "'", value, 0.96);
            }
        }

        // 5. Suspicious unprintable control characters
        if (CONTROL_CHAR_PATTERN.matcher(value).find()) {
            return new InspectionResult(true, "MEDIUM", "Unprintable terminal control characters / evasion bytes detected", value, 0.88);
        }

        return InspectionResult.clean();
    }

    public InspectionResult evaluateRawContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return InspectionResult.clean();
        }

        // Check for CRLF embedded fake log line
        Matcher crlfMatcher = CRLF_PATTERN.matcher(raw);
        if (crlfMatcher.find()) {
            if (LOG_LEVEL_PATTERN.matcher(raw).find() && EMBEDDED_TIMESTAMP_PATTERN.matcher(raw).find()) {
                return new InspectionResult(true, "CRITICAL", "Multi-line CRLF log forgery with fake timestamp and log level markers", raw, 0.98);
            }
            if (PRIVILEGE_INJECTION_PATTERN.matcher(raw).find()) {
                return new InspectionResult(true, "CRITICAL", "CRLF log forgery with embedded privilege escalation vector", raw, 0.98);
            }
        }

        // Nested CEF inside JSON or Key-Value quotes
        Matcher cefMatcher = CEF_PATTERN.matcher(raw);
        if (cefMatcher.find() && (raw.startsWith("{") || raw.contains("="))) {
            return new InspectionResult(true, "HIGH", "Nested CEF payload embedded within parent log record", extractSnippet(raw, CEF_PATTERN), 0.95);
        }

        return InspectionResult.clean();
    }

    private String extractSnippet(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            int start = Math.max(0, m.start() - 10);
            int end = Math.min(text.length(), m.end() + 30);
            return text.substring(start, end);
        }
        return text.length() > 60 ? text.substring(0, 60) + "..." : text;
    }

    private String sanitizeSnippet(String text) {
        if (text == null) return "";
        return text.replace("\r", "\\r").replace("\n", "\\n");
    }

    public static class InspectionResult {
        public final boolean isSuspicious;
        public final String severity;
        public final String reason;
        public final String snippet;
        public final double confidence;

        public InspectionResult(boolean isSuspicious, String severity, String reason, String snippet, double confidence) {
            this.isSuspicious = isSuspicious;
            this.severity = severity;
            this.reason = reason;
            this.snippet = snippet;
            this.confidence = confidence;
        }

        public static InspectionResult clean() {
            return new InspectionResult(false, "NONE", "Clean", "", 0.0);
        }
    }
}
