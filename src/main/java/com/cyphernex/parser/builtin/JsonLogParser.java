package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.LogParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JsonLogParser implements LogParser {

    private final ObjectMapper objectMapper;

    public JsonLogParser() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonLogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public String getName() {
        return "JsonLogParser";
    }

    @Override
    public String getVersion() {
        return "v1";
    }

    @Override
    public String getSupportedFormat() {
        return "JSON";
    }

    @Override
    public boolean canParse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null || rawLog.getRawContent().isBlank()) {
            return false;
        }
        String content = rawLog.getRawContent().trim();
        if (!content.startsWith("{") || !content.endsWith("}")) {
            return false;
        }
        try {
            objectMapper.readTree(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ParsedLog parse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null) {
            return new ParsedLog("unknown", getName(), getVersion(), new HashMap<>(), 0.0, "INVALID");
        }

        try {
            Map<String, Object> fields = objectMapper.readValue(
                    rawLog.getRawContent(),
                    new TypeReference<Map<String, Object>>() {}
            );
            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    1.0,
                    "VALID"
            );
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("raw", rawLog.getRawContent());
            fallback.put("error", e.getMessage());
            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fallback,
                    0.0,
                    "INVALID"
            );
        }
    }
}
