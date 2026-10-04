package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.LogParser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SyslogParser implements LogParser {

    // RFC 5424 pattern: <PRI>VERSION TIMESTAMP HOSTNAME APP-NAME PROCID MSGID [STRUCTURED-DATA] MSG
    private static final Pattern RFC5424_PATTERN = Pattern.compile(
            "^<(\\d{1,3})>(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)(?:\\s+(\\[[^\\]]*\\]|-))?\\s*(.*)$"
    );

    // RFC 3164 with PRI pattern: <PRI>MMM DD HH:MM:SS HOSTNAME TAG[PID]: MSG or TAG: MSG
    private static final Pattern RFC3164_PRI_PATTERN = Pattern.compile(
            "^<(\\d{1,3})>([A-Za-z]{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+([a-zA-Z0-9_.-]+)(?:\\[(\\d+)\\])?:\\s*(.*)$"
    );

    // RFC 3164 without PRI pattern
    private static final Pattern RFC3164_NO_PRI_PATTERN = Pattern.compile(
            "^([A-Za-z]{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+([a-zA-Z0-9_.-]+)(?:\\[(\\d+)\\])?:\\s*(.*)$"
    );

    @Override
    public String getName() {
        return "SyslogParser";
    }

    @Override
    public String getVersion() {
        return "v1";
    }

    @Override
    public String getSupportedFormat() {
        return "SYSLOG";
    }

    @Override
    public boolean canParse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null || rawLog.getRawContent().isBlank()) {
            return false;
        }
        String content = rawLog.getRawContent().trim();
        return RFC5424_PATTERN.matcher(content).matches() ||
                RFC3164_PRI_PATTERN.matcher(content).matches() ||
                RFC3164_NO_PRI_PATTERN.matcher(content).matches() ||
                content.matches("^<\\d{1,3}>.*");
    }

    @Override
    public ParsedLog parse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null) {
            return new ParsedLog("unknown", getName(), getVersion(), new LinkedHashMap<>(), 0.0, "INVALID");
        }

        String content = rawLog.getRawContent().trim();
        Map<String, Object> fields = new LinkedHashMap<>();

        // Try RFC 5424
        Matcher m5424 = RFC5424_PATTERN.matcher(content);
        if (m5424.matches()) {
            int pri = Integer.parseInt(m5424.group(1));
            fields.put("priority", pri);
            fields.put("facility", pri / 8);
            fields.put("severity", pri % 8);
            fields.put("version", m5424.group(2));
            fields.put("timestamp", m5424.group(3));
            fields.put("hostname", cleanField(m5424.group(4)));
            fields.put("appName", cleanField(m5424.group(5)));
            fields.put("procId", cleanField(m5424.group(6)));
            fields.put("msgId", cleanField(m5424.group(7)));
            if (m5424.group(8) != null && !"-".equals(m5424.group(8))) {
                fields.put("structuredData", m5424.group(8));
            }
            fields.put("message", m5424.group(9));

            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    1.0,
                    "VALID"
            );
        }

        // Try RFC 3164 with PRI
        Matcher m3164Pri = RFC3164_PRI_PATTERN.matcher(content);
        if (m3164Pri.matches()) {
            int pri = Integer.parseInt(m3164Pri.group(1));
            fields.put("priority", pri);
            fields.put("facility", pri / 8);
            fields.put("severity", pri % 8);
            fields.put("timestamp", m3164Pri.group(2));
            fields.put("hostname", m3164Pri.group(3));
            fields.put("appName", m3164Pri.group(4));
            if (m3164Pri.group(5) != null) {
                fields.put("procId", m3164Pri.group(5));
            }
            fields.put("message", m3164Pri.group(6));

            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    1.0,
                    "VALID"
            );
        }

        // Try RFC 3164 without PRI
        Matcher m3164NoPri = RFC3164_NO_PRI_PATTERN.matcher(content);
        if (m3164NoPri.matches()) {
            fields.put("timestamp", m3164NoPri.group(1));
            fields.put("hostname", m3164NoPri.group(2));
            fields.put("appName", m3164NoPri.group(3));
            if (m3164NoPri.group(4) != null) {
                fields.put("procId", m3164NoPri.group(4));
            }
            fields.put("message", m3164NoPri.group(5));

            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    0.95,
                    "VALID"
            );
        }

        // Generic PRI fallback
        if (content.startsWith("<") && content.contains(">")) {
            int closingIdx = content.indexOf('>');
            try {
                int pri = Integer.parseInt(content.substring(1, closingIdx));
                fields.put("priority", pri);
                fields.put("facility", pri / 8);
                fields.put("severity", pri % 8);
                fields.put("message", content.substring(closingIdx + 1).trim());
                return new ParsedLog(
                        rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                        getName(),
                        getVersion(),
                        fields,
                        0.8,
                        "VALID"
                );
            } catch (Exception ignored) {
            }
        }

        fields.put("message", content);
        return new ParsedLog(
                rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                getName(),
                getVersion(),
                fields,
                0.5,
                "PARTIAL"
        );
    }

    private String cleanField(String value) {
        return "-".equals(value) ? null : value;
    }
}
