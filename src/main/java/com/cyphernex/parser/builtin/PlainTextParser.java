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
public class PlainTextParser implements LogParser {

    private static final Pattern CEF_PATTERN = Pattern.compile("(?:([A-Za-z]{3}\\s+\\d+\\s+[\\d:]+)\\s+([\\w.-]+)\\s+)?CEF:\\s*(\\d+)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|?(.*)");
    private static final Pattern KV_PATTERN = Pattern.compile("([a-zA-Z0-9_.-]+)=(?:\"([^\"]*)\"|'([^']*)'|([^|;,\\r\\n]+?(?=(?:\\s+[a-zA-Z0-9_.-]+=|[|;,]|$))))");

    @Override
    public String getName() {
        return "PlainTextParser";
    }

    @Override
    public String getVersion() {
        return "v1";
    }

    @Override
    public String getSupportedFormat() {
        return "PLAIN_TEXT";
    }

    @Override
    public boolean canParse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null || rawLog.getRawContent().isBlank()) {
            return false;
        }
        String content = rawLog.getRawContent().trim();
        // Disqualify JSON, XML, Syslog PRI
        if (content.startsWith("{") || content.startsWith("<")) {
            return false;
        }
        return content.contains("CEF:") || (content.contains("=") && KV_PATTERN.matcher(content).find());
    }

    @Override
    public ParsedLog parse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null) {
            return new ParsedLog("unknown", getName(), getVersion(), new LinkedHashMap<>(), 0.0, "INVALID");
        }

        String content = rawLog.getRawContent().trim();
        Map<String, Object> fields = new LinkedHashMap<>();

        // Check CEF format first
        Matcher cefMatcher = CEF_PATTERN.matcher(content);
        if (cefMatcher.find()) {
            if (cefMatcher.group(1) != null) fields.put("timestamp", cefMatcher.group(1).trim());
            if (cefMatcher.group(2) != null) fields.put("host", cefMatcher.group(2).trim());
            fields.put("cefVersion", cefMatcher.group(3));
            fields.put("deviceVendor", cefMatcher.group(4));
            fields.put("deviceProduct", cefMatcher.group(5));
            fields.put("deviceVersion", cefMatcher.group(6));
            fields.put("signatureId", cefMatcher.group(7));
            fields.put("name", cefMatcher.group(8));
            fields.put("severity", cefMatcher.group(9));

            String extension = cefMatcher.group(10);
            if (extension != null && !extension.isBlank()) {
                Matcher kvMatcher = KV_PATTERN.matcher(extension);
                while (kvMatcher.find()) {
                    String key = kvMatcher.group(1).trim();
                    String val = kvMatcher.group(2) != null ? kvMatcher.group(2) :
                            (kvMatcher.group(3) != null ? kvMatcher.group(3) : kvMatcher.group(4));
                    if (val != null) fields.put(key, val.trim());
                }
            }

            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "cef-device",
                    getName(),
                    getVersion(),
                    fields,
                    0.98,
                    "VALID"
            );
        }

        // Standard or pipe-separated Key-Value matching
        Matcher matcher = KV_PATTERN.matcher(content);
        boolean foundAny = false;
        while (matcher.find()) {
            foundAny = true;
            String key = matcher.group(1).trim();
            String val = matcher.group(2) != null ? matcher.group(2) :
                    (matcher.group(3) != null ? matcher.group(3) : matcher.group(4));
            if (val != null) {
                fields.put(key, val.trim());
            }
        }

        if (!foundAny) {
            fields.put("message", content);
        }

        return new ParsedLog(
                rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                getName(),
                getVersion(),
                fields,
                foundAny ? 0.95 : 0.70,
                "VALID"
        );
    }
}
