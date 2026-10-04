package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.LogParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvLogParser implements LogParser {

    @Override
    public String getName() {
        return "CsvLogParser";
    }

    @Override
    public String getVersion() {
        return "v1";
    }

    @Override
    public String getSupportedFormat() {
        return "CSV";
    }

    @Override
    public boolean canParse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null || rawLog.getRawContent().isBlank()) {
            return false;
        }
        String content = rawLog.getRawContent().trim();
        // Disqualify JSON / XML / Syslog PRI
        if (content.startsWith("{") || content.startsWith("<")) {
            return false;
        }
        return content.contains(",") && !content.contains("=");
    }

    @Override
    public ParsedLog parse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null) {
            return new ParsedLog("unknown", getName(), getVersion(), new LinkedHashMap<>(), 0.0, "INVALID");
        }

        String content = rawLog.getRawContent().trim();
        Map<String, Object> fields = new LinkedHashMap<>();

        try {
            String[] lines = content.split("\\r?\\n");
            if (lines.length >= 2) {
                // Multi-line CSV with header and data row
                try (CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {
                    for (CSVRecord record : parser) {
                        fields.putAll(record.toMap());
                        break; // take first data row
                    }
                }
            } else {
                // Single line CSV without header row: parse values as col_0, col_1...
                try (CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT.builder().setTrim(true).build())) {
                    List<CSVRecord> records = parser.getRecords();
                    if (!records.isEmpty()) {
                        CSVRecord record = records.get(0);
                        for (int i = 0; i < record.size(); i++) {
                            fields.put("column_" + (i + 1), record.get(i));
                        }
                    }
                }
            }

            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    1.0,
                    "VALID"
            );
        } catch (Exception e) {
            fields.put("raw", content);
            fields.put("error", e.getMessage());
            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    0.0,
                    "INVALID"
            );
        }
    }
}
