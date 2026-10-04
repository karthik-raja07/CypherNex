package com.cyphernex.forge;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.LogParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LearnedParser implements LogParser {

    private final String name;
    private final String version;
    private final String supportedFormat;
    private final Grammar grammar;
    private Pattern compiledRegex;

    public LearnedParser(String name, String version, String supportedFormat, Grammar grammar) {
        this.name = name != null ? name : "LearnedParser";
        this.version = version != null ? version : "v1";
        this.supportedFormat = supportedFormat != null ? supportedFormat : "CUSTOM";
        this.grammar = grammar;
        if (grammar != null && grammar.getRegexPattern() != null && !grammar.getRegexPattern().isBlank()) {
            try {
                this.compiledRegex = Pattern.compile(grammar.getRegexPattern());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getSupportedFormat() {
        return supportedFormat;
    }

    @Override
    public boolean canParse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null || rawLog.getRawContent().isBlank() || grammar == null) {
            return false;
        }
        String content = rawLog.getRawContent().trim();

        if (grammar.getExtractionType() == null || "DELIMITED".equalsIgnoreCase(grammar.getExtractionType())) {
            String delim = grammar.getDelimiter() != null && !grammar.getDelimiter().isEmpty() ? grammar.getDelimiter() : "|";
            return content.contains(delim);
        }

        if ("REGEX".equalsIgnoreCase(grammar.getExtractionType()) && compiledRegex != null) {
            return compiledRegex.matcher(content).matches() || compiledRegex.matcher(content).find();
        }

        return true;
    }

    @Override
    public ParsedLog parse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null) {
            return new ParsedLog("unknown", getName(), getVersion(), new LinkedHashMap<>(), 0.0, "INVALID");
        }

        String content = rawLog.getRawContent().trim();
        Map<String, Object> fields = new LinkedHashMap<>();

        if (grammar != null && (grammar.getExtractionType() == null || "DELIMITED".equalsIgnoreCase(grammar.getExtractionType()))) {
            String delim = grammar.getDelimiter() != null && !grammar.getDelimiter().isEmpty() ? grammar.getDelimiter() : "|";
            String[] parts = content.split(Pattern.quote(delim), -1);

            // Dynamically detect 1-based vs 0-based indexing in rules
            boolean uses1Based = false;
            boolean has0 = false;
            int maxRuleIdx = -1;
            for (GrammarField field : grammar.getFields()) {
                int rawIdx = parseDelimiterIndex(field.getExtractionRule());
                if (rawIdx == 0) has0 = true;
                if (rawIdx > maxRuleIdx) maxRuleIdx = rawIdx;
            }
            if (!has0 && maxRuleIdx == parts.length) {
                uses1Based = true;
            }

            for (GrammarField field : grammar.getFields()) {
                int rawIdx = parseDelimiterIndex(field.getExtractionRule());
                int index = uses1Based ? (rawIdx - 1) : rawIdx;
                if (index >= 0 && index < parts.length) {
                    String val = parts[index].trim();
                    if (!val.isEmpty()) {
                        fields.put(field.getName(), val);
                    }
                }
            }
        } else if (grammar != null && "REGEX".equalsIgnoreCase(grammar.getExtractionType()) && compiledRegex != null) {
            Matcher matcher = compiledRegex.matcher(content);
            if (matcher.find()) {
                for (GrammarField field : grammar.getFields()) {
                    int groupIdx = parseGroupIndex(field.getExtractionRule());
                    if (groupIdx > 0 && groupIdx <= matcher.groupCount()) {
                        String val = matcher.group(groupIdx);
                        if (val != null && !val.isBlank()) {
                            fields.put(field.getName(), val.trim());
                        }
                    }
                }
            }
        } else {
            // Generic rule parsing fallback
            fields.put("message", content);
        }

        boolean valid = !fields.isEmpty();
        return new ParsedLog(
                rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                getName(),
                getVersion(),
                fields,
                valid ? 1.0 : 0.0,
                valid ? "VALID" : "INVALID"
        );
    }

    private int parseDelimiterIndex(String rule) {
        if (rule == null || rule.isBlank()) {
            return -1;
        }
        String clean = rule.trim().toLowerCase();
        if (clean.contains(":")) {
            clean = clean.substring(clean.indexOf(':') + 1).trim();
        } else if (clean.contains("[")) {
            clean = clean.replaceAll("[\\[\\]]", "").trim();
        }
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            Matcher m = Pattern.compile("\\d+").matcher(clean);
            if (m.find()) {
                return Integer.parseInt(m.group());
            }
            return -1;
        }
    }

    private int parseGroupIndex(String rule) {
        if (rule == null || rule.isBlank()) {
            return -1;
        }
        String clean = rule.trim().toLowerCase();
        if (clean.contains(":")) {
            clean = clean.substring(clean.indexOf(':') + 1).trim();
        }
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            Matcher m = Pattern.compile("\\d+").matcher(clean);
            if (m.find()) {
                return Integer.parseInt(m.group());
            }
            return -1;
        }
    }

    public Grammar getGrammar() {
        return grammar;
    }
}
