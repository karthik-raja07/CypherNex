package com.cyphernex.normalization;

import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.Provenance;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class SchemaMapper {

    private static final Set<String> TIMESTAMP_KEYS = Set.of(
            "timestamp", "date", "time", "@timestamp", "ts", "eventtime", "datetime", "logged_at", "received_at"
    );
    private static final Set<String> USER_KEYS = Set.of(
            "user", "username", "account", "user_id", "usr", "user.name", "acct", "duser", "suser", "src_user", "dst_user", "actor"
    );
    private static final Set<String> SRC_IP_KEYS = Set.of(
            "src_ip", "source_ip", "client_ip", "sourceip", "srcip", "ip", "src", "src_endpoint.ip", "c_ip"
    );
    private static final Set<String> DST_IP_KEYS = Set.of(
            "dst_ip", "destination_ip", "server_ip", "destinationip", "dstip", "dst", "dst_endpoint.ip", "s_ip"
    );
    private static final Set<String> HOST_KEYS = Set.of(
            "host", "hostname", "device", "computer", "endpoint.hostname", "asset", "dhost", "shost", "dvchost"
    );
    private static final Set<String> SESSION_KEYS = Set.of(
            "session", "session_id", "sessionid", "sess_id", "sid"
    );
    private static final Set<String> REQUEST_KEYS = Set.of(
            "request", "request_id", "correlation_id", "requestid", "trace_id", "traceid", "msgid", "tx_id"
    );
    private static final Set<String> ACTIVITY_KEYS = Set.of(
            "action", "event", "action_type", "activity", "activity_name", "op", "act", "operation", "signature", "name", "event_name", "type"
    );
    private static final Set<String> SEVERITY_KEYS = Set.of(
            "severity", "level", "priority", "log_level", "sev", "threat_level"
    );
    private static final Set<String> PRIMARY_PROCESS_KEYS = Set.of(
            "process", "process_name", "appname", "program", "tag", "path", "file", "target", "file_name", "filepath"
    );
    private static final Set<String> SECONDARY_PROCESS_KEYS = Set.of(
            "procid", "pid", "process_id"
    );
    private static final Set<String> STATUS_KEYS = Set.of(
            "status", "result", "outcome", "disposition"
    );

    public NormalizedEvent mapToNormalizedEvent(ParsedLog parsedLog, String rawLogId, String fingerprint) {
        if (parsedLog == null) {
            return null;
        }

        Map<String, Object> fields = parsedLog.getFields() != null ? parsedLog.getFields() : new LinkedHashMap<>();
        Map<String, Object> additionalFields = new LinkedHashMap<>();

        Instant timestamp = null;
        String user = null;
        String sourceIp = null;
        String destinationIp = null;
        String host = null;
        String sessionId = null;
        String requestId = null;
        String activity = null;
        String rawSeverity = null;
        String process = null;

        List<Map<String, Object>> fieldMappings = new ArrayList<>();
        boolean isHeaderlessCsv = fields.containsKey("column_1") || fields.containsKey("column_2");

        // Type patterns
        java.util.regex.Pattern ipPattern = java.util.regex.Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
        java.util.regex.Pattern sevPattern = java.util.regex.Pattern.compile("(?i)^(CRITICAL|HIGH|MEDIUM|MED|LOW|INFO|INFORMATIONAL|WARN|WARNING|ERROR|FATAL|DEBUG|NOTICE|EMERGENCY|ALERT)$");
        java.util.regex.Pattern portPattern = java.util.regex.Pattern.compile("^\\d{1,5}$");

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String rawKey = entry.getKey();
            if (rawKey == null) continue;
            String normalizedKey = rawKey.trim().toLowerCase(Locale.ROOT).replace("-", "_");
            Object val = entry.getValue();
            if (val == null) continue;
            String strVal = String.valueOf(val).trim();

            if (TIMESTAMP_KEYS.contains(normalizedKey) || ("column_1".equals(normalizedKey) && (strVal.contains("-") || strVal.contains(":") || strVal.contains("/")))) {
                timestamp = parseTimestamp(strVal);
                double conf = "column_1".equals(normalizedKey) ? 0.85 : 1.0;
                String status = "column_1".equals(normalizedKey) ? "INFERRED_POSITIONAL" : "MAPPED";
                String reason = "column_1".equals(normalizedKey) ? "Positional timestamp inference (col 1 -> time)" : "Standard key mapping (" + rawKey + " -> time)";
                fieldMappings.add(createMappingEntry(rawKey, val, "time", timestamp != null ? timestamp.toString() : strVal, conf, status, reason));
            } else if (SRC_IP_KEYS.contains(normalizedKey) || ("column_4".equals(normalizedKey) && ipPattern.matcher(strVal).matches()) || ("column_5".equals(normalizedKey) && ipPattern.matcher(strVal).matches() && sourceIp == null)) {
                sourceIp = strVal;
                double conf = normalizedKey.startsWith("column_") ? 0.90 : 1.0;
                String status = normalizedKey.startsWith("column_") ? "INFERRED_TYPE" : "MAPPED";
                String reason = normalizedKey.startsWith("column_") ? "IPv4 regex pattern match -> src_endpoint.ip" : "Standard key mapping (" + rawKey + " -> src_endpoint.ip)";
                fieldMappings.add(createMappingEntry(rawKey, val, "src_endpoint.ip", sourceIp, conf, status, reason));
            } else if (DST_IP_KEYS.contains(normalizedKey) || ("column_5".equals(normalizedKey) && ipPattern.matcher(strVal).matches() && destinationIp == null)) {
                destinationIp = strVal;
                double conf = normalizedKey.startsWith("column_") ? 0.90 : 1.0;
                String status = normalizedKey.startsWith("column_") ? "INFERRED_TYPE" : "MAPPED";
                String reason = normalizedKey.startsWith("column_") ? "IPv4 regex pattern match -> dst_endpoint.ip" : "Standard key mapping (" + rawKey + " -> dst_endpoint.ip)";
                fieldMappings.add(createMappingEntry(rawKey, val, "dst_endpoint.ip", destinationIp, conf, status, reason));
            } else if (SEVERITY_KEYS.contains(normalizedKey) || (normalizedKey.startsWith("column_") && sevPattern.matcher(strVal).matches())) {
                rawSeverity = strVal;
                double conf = normalizedKey.startsWith("column_") ? 0.95 : 1.0;
                String status = normalizedKey.startsWith("column_") ? "INFERRED_TYPE" : "MAPPED";
                String reason = normalizedKey.startsWith("column_") ? "Severity pattern match -> severity" : "Standard key mapping (" + rawKey + " -> severity)";
                fieldMappings.add(createMappingEntry(rawKey, val, "severity", strVal.toUpperCase(Locale.ROOT), conf, status, reason));
            } else if (USER_KEYS.contains(normalizedKey) || ("column_2".equals(normalizedKey) && !sevPattern.matcher(strVal).matches() && !ipPattern.matcher(strVal).matches())) {
                user = strVal;
                double conf = "column_2".equals(normalizedKey) ? 0.85 : 1.0;
                String status = "column_2".equals(normalizedKey) ? "INFERRED_POSITIONAL" : "MAPPED";
                String reason = "column_2".equals(normalizedKey) ? "Positional heuristic (col 2 -> actor.user.name)" : "Standard key mapping (" + rawKey + " -> actor.user.name)";
                fieldMappings.add(createMappingEntry(rawKey, val, "actor.user.name", user, conf, status, reason));
            } else if (HOST_KEYS.contains(normalizedKey) || ("column_4".equals(normalizedKey) && !ipPattern.matcher(strVal).matches() && !sevPattern.matcher(strVal).matches())) {
                host = strVal;
                double conf = "column_4".equals(normalizedKey) ? 0.80 : 1.0;
                String status = "column_4".equals(normalizedKey) ? "INFERRED_POSITIONAL" : "MAPPED";
                String reason = "column_4".equals(normalizedKey) ? "Positional heuristic (col 4 -> device.hostname)" : "Standard key mapping (" + rawKey + " -> device.hostname)";
                fieldMappings.add(createMappingEntry(rawKey, val, "device.hostname", host, conf, status, reason));
            } else if (SESSION_KEYS.contains(normalizedKey)) {
                sessionId = strVal;
                fieldMappings.add(createMappingEntry(rawKey, val, "session.id", sessionId, 1.0, "MAPPED", "Standard key mapping (" + rawKey + " -> session.id)"));
            } else if (REQUEST_KEYS.contains(normalizedKey)) {
                requestId = strVal;
                fieldMappings.add(createMappingEntry(rawKey, val, "http_request.uid", requestId, 1.0, "MAPPED", "Standard key mapping (" + rawKey + " -> http_request.uid)"));
            } else if (ACTIVITY_KEYS.contains(normalizedKey) || ("column_3".equals(normalizedKey) && !sevPattern.matcher(strVal).matches() && !ipPattern.matcher(strVal).matches())) {
                if (activity == null || "name".equals(normalizedKey) || "action".equals(normalizedKey) || "act".equals(normalizedKey) || "op".equals(normalizedKey)) {
                    activity = strVal;
                } else {
                    additionalFields.put(rawKey, val);
                }
                double conf = "column_3".equals(normalizedKey) ? 0.85 : 1.0;
                String status = "column_3".equals(normalizedKey) ? "INFERRED_POSITIONAL" : "MAPPED";
                String reason = "column_3".equals(normalizedKey) ? "Positional heuristic (col 3 -> activity_name)" : "Standard key mapping (" + rawKey + " -> activity_name)";
                fieldMappings.add(createMappingEntry(rawKey, val, "activity_name", activity, conf, status, reason));
            } else if (PRIMARY_PROCESS_KEYS.contains(normalizedKey) || ("column_5".equals(normalizedKey) && !ipPattern.matcher(strVal).matches() && !sevPattern.matcher(strVal).matches())) {
                process = strVal;
                double conf = "column_5".equals(normalizedKey) ? 0.85 : 1.0;
                String status = "column_5".equals(normalizedKey) ? "INFERRED_POSITIONAL" : "MAPPED";
                String reason = "column_5".equals(normalizedKey) ? "Positional process heuristic (col 5 -> process.name)" : "Standard key mapping (" + rawKey + " -> process.name)";
                fieldMappings.add(createMappingEntry(rawKey, val, "process.name", process, conf, status, reason));
            } else if (SECONDARY_PROCESS_KEYS.contains(normalizedKey)) {
                if (process == null) {
                    process = strVal;
                } else {
                    additionalFields.put(rawKey, val);
                }
                fieldMappings.add(createMappingEntry(rawKey, val, "process.name", process, 0.90, "MAPPED", "Secondary process key mapping"));
            } else if (STATUS_KEYS.contains(normalizedKey)) {
                additionalFields.put("status", strVal);
                fieldMappings.add(createMappingEntry(rawKey, val, "status", strVal, 1.0, "MAPPED", "Standard status mapping"));
            } else if ("spt".equals(normalizedKey) || "src_port".equals(normalizedKey)) {
                additionalFields.put("src_port", strVal);
                fieldMappings.add(createMappingEntry(rawKey, val, "src_endpoint.port", strVal, 1.0, "MAPPED", "CEF source port mapping"));
            } else if ("dpt".equals(normalizedKey) || "dst_port".equals(normalizedKey)) {
                additionalFields.put("dst_port", strVal);
                fieldMappings.add(createMappingEntry(rawKey, val, "dst_endpoint.port", strVal, 1.0, "MAPPED", "CEF destination port mapping"));
            } else if ("devicevendor".equals(normalizedKey) || "vendor".equals(normalizedKey)) {
                additionalFields.put("vendor", strVal);
                fieldMappings.add(createMappingEntry(rawKey, val, "metadata.product.vendor_name", strVal, 1.0, "MAPPED", "Device vendor mapping"));
            } else if ("deviceproduct".equals(normalizedKey) || "product".equals(normalizedKey)) {
                additionalFields.put("product", strVal);
                fieldMappings.add(createMappingEntry(rawKey, val, "metadata.product.name", strVal, 1.0, "MAPPED", "Device product mapping"));
            } else {
                additionalFields.put(rawKey, val);
                fieldMappings.add(createMappingEntry(rawKey, val, "unmapped." + rawKey, val, 0.85, "UNMAPPED_EXTENSION", "Preserved in unmapped extension dictionary"));
            }
        }

        if (timestamp == null) {
            timestamp = Instant.now();
        }

        String severity = normalizeSeverity(rawSeverity, additionalFields.get("status"));
        String eventClass = inferEventClass(activity, sourceIp, destinationIp, process, fields);

        double confidence = calculateConfidence(parsedLog, user, activity, sourceIp);
        if (isHeaderlessCsv) {
            confidence = Math.min(confidence, 0.78);
        }

        List<String> validatorsPassed = new ArrayList<>();
        validatorsPassed.add("SCHEMA_VALIDATOR");
        validatorsPassed.add("OCSF_MAPPER");
        if (confidence >= 0.90) {
            validatorsPassed.add("CONFIDENCE_CHECK");
        }
        if (isHeaderlessCsv) {
            validatorsPassed.add("HEADERLESS_CSV_INFERENCE_WARNING");
        }

        Provenance provenance = new Provenance(
                parsedLog.getParserName() != null ? parsedLog.getParserName() : "UnknownParser",
                parsedLog.getParserVersion() != null ? parsedLog.getParserVersion() : "v1",
                fingerprint != null ? fingerprint : "unknown-fingerprint",
                validatorsPassed
        );

        NormalizedEvent event = new NormalizedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(timestamp);
        event.setEventClass(eventClass);
        event.setActivity(activity);
        event.setSeverity(severity);
        event.setSource(parsedLog.getSource() != null ? parsedLog.getSource() : "unknown");
        event.setUser(user);
        event.setSourceIp(sourceIp);
        event.setDestinationIp(destinationIp);
        event.setHost(host);
        event.setSessionId(sessionId);
        event.setRequestId(requestId);
        event.setProcess(process);
        event.setRawLogId(rawLogId);
        event.setParserVersion(parsedLog.getParserVersion() != null ? parsedLog.getParserVersion() : "v1");
        event.setConfidence(confidence);
        event.setProvenance(provenance);
        event.setAdditionalFields(additionalFields);
        event.setFieldMappings(fieldMappings);

        return event;
    }

    public String normalizeSeverity(String rawSeverity, Object statusVal) {
        if (rawSeverity != null && !rawSeverity.isBlank()) {
            String lower = rawSeverity.trim().toLowerCase(Locale.ROOT);
            switch (lower) {
                case "info":
                case "informational":
                case "notice":
                case "debug":
                case "information":
                case "low":
                case "6":
                case "7":
                    return "low";

                case "warn":
                case "warning":
                case "medium":
                case "med":
                case "4":
                case "5":
                    return "medium";

                case "error":
                case "err":
                case "failure":
                case "failed":
                case "high":
                case "3":
                case "8":
                    return "high";

                case "critical":
                case "crit":
                case "emergency":
                case "alert":
                case "fatal":
                case "0":
                case "1":
                case "2":
                case "9":
                case "10":
                    return "critical";

                default:
                    try {
                        int num = Integer.parseInt(lower);
                        if (num >= 9) return "critical";
                        if (num >= 7) return "high";
                        if (num == 6) return "low";
                        if (num >= 4) return "medium";
                        if (num == 3) return "high";
                        if (num <= 2) return "critical";
                        return "low";
                    } catch (Exception ignored) {
                        return lower;
                    }
            }
        }

        if (statusVal != null) {
            String statusStr = String.valueOf(statusVal).trim().toLowerCase(Locale.ROOT);
            if ("success".equals(statusStr) || "ok".equals(statusStr) || "allowed".equals(statusStr)) {
                return "low";
            }
            if ("failure".equals(statusStr) || "failed".equals(statusStr) || "error".equals(statusStr) || "denied".equals(statusStr) || "blocked".equals(statusStr)) {
                return "high";
            }
        }

        return "low";
    }

    public String inferEventClass(String activity, String sourceIp, String destinationIp, String process, Map<String, Object> fields) {
        String act = activity != null ? activity.toUpperCase(Locale.ROOT) : "";

        if (act.contains("LOGIN") || act.contains("LOGOUT") || act.contains("AUTH") || act.contains("PASSWORD") || act.contains("SIGNIN")) {
            return "Authentication";
        }
        if (act.contains("CONNECT") || act.contains("HTTP") || act.contains("GET") || act.contains("POST") || act.contains("TRAFFIC") || act.contains("C2") || act.contains("THREAT") || sourceIp != null || destinationIp != null) {
            return "Network Activity";
        }
        if (process != null || act.contains("REBOOT") || act.contains("START") || act.contains("STOP") || act.contains("SERVICE") || act.contains("FILE") || act.contains("EXPORT")) {
            return "System Activity";
        }
        return "Security Event";
    }

    private Map<String, Object> createMappingEntry(String rawField, Object rawValue, String ocsfField, Object normalizedValue, double confidence, String status, String reason) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("rawField", rawField);
        entry.put("rawValue", rawValue);
        entry.put("ocsfField", ocsfField);
        entry.put("normalizedValue", normalizedValue);
        entry.put("confidence", confidence);
        entry.put("status", status);
        entry.put("reason", reason);
        return entry;
    }

    public double calculateConfidence(ParsedLog parsedLog, String user, String activity, String sourceIp) {
        if (parsedLog == null) {
            return 0.0;
        }
        if ("INVALID".equalsIgnoreCase(parsedLog.getValidationStatus())) {
            return 0.0;
        }

        // Directly parsed with high-fidelity validation
        if (parsedLog.getConfidence() >= 0.90 && (user != null || activity != null || sourceIp != null)) {
            return 0.95;
        }
        if (parsedLog.getConfidence() >= 0.80) {
            return 0.80;
        }
        if (!parsedLog.getFields().isEmpty()) {
            return 0.70;
        }
        return 0.0;
    }

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ROOT).withZone(java.time.ZoneOffset.UTC),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(java.time.ZoneOffset.UTC),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS", Locale.ROOT).withZone(java.time.ZoneOffset.UTC),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).withZone(java.time.ZoneOffset.UTC)
    };

    public Instant parseTimestamp(String val) {
        if (val == null || val.isBlank()) {
            return Instant.now();
        }
        String clean = val.trim();
        try {
            return Instant.parse(clean);
        } catch (Exception ignored) {
        }

        try {
            return OffsetDateTime.parse(clean, DateTimeFormatter.ISO_DATE_TIME).toInstant();
        } catch (Exception ignored) {
        }

        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                return java.time.LocalDateTime.parse(clean, dtf).toInstant(java.time.ZoneOffset.UTC);
            } catch (Exception ignored) {
            }
        }

        // Try syslog format: e.g. "Sep 19 10:20:12"
        try {
            DateTimeFormatter syslogFmt = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss", Locale.ENGLISH);
            java.time.MonthDay md = java.time.MonthDay.parse(clean.substring(0, Math.min(clean.length(), 6)), DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH));
            java.time.LocalTime lt = java.time.LocalTime.parse(clean.substring(clean.indexOf(' ', 4) + 1, Math.min(clean.length(), 15)));
            int year = java.time.LocalDate.now().getYear();
            return java.time.LocalDateTime.of(year, md.getMonth(), md.getDayOfMonth(), lt.getHour(), lt.getMinute(), lt.getSecond()).toInstant(java.time.ZoneOffset.UTC);
        } catch (Exception ignored) {
        }

        try {
            long millis = Long.parseLong(clean);
            return millis > 1_000_000_000_000L ? Instant.ofEpochMilli(millis) : Instant.ofEpochSecond(millis);
        } catch (Exception ignored) {
        }

        return Instant.now();
    }
}
