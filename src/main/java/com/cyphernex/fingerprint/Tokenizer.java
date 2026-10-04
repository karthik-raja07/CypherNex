package com.cyphernex.fingerprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Tokenizer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern QUOTE_PATTERN = Pattern.compile("^(\"[^\"]*\"|'[^']*')$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    // Timestamps and Dates
    private static final Pattern ISO_TS_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(?:[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)?$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?$");
    private static final Pattern DATE_SLASH_PATTERN = Pattern.compile("^(?:\\[?\\d{2}/[A-Za-z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2}(?:\\s+[+-]\\d{4})?\\]?|\\d{4}/\\d{2}/\\d{2}|\\d{2}/\\d{2}/\\d{4})$");
    private static final Pattern MONTH_NAME_PATTERN = Pattern.compile("^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)$", Pattern.CASE_INSENSITIVE);

    // IP addresses
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?::\\d+)?$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{1,4}$|^::1$");

    // Identifiers
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern HEX_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{16,64}$");
    private static final Pattern PREFIXED_ID_PATTERN = Pattern.compile("^(?:req|sess|trace|txn|corr|id|session|user_id|req_id|usr)[-_][a-zA-Z0-9_-]+$", Pattern.CASE_INSENSITIVE);

    // Numbers, Words, and Punctuation
    private static final Pattern NUM_PATTERN = Pattern.compile("^[+-]?\\d+(?:\\.\\d+)?$");
    private static final Pattern KV_PATTERN = Pattern.compile("^([a-zA-Z0-9_.-]+)=(.+)$");
    private static final Pattern WORD_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");
    private static final Pattern PUNCT_PATTERN = Pattern.compile("^[\\{\\}\\[\\]\\(\\),;:<>|/\\\\=_-]+$");

    // Token splitter pattern: splits by space or preserves quotes
    private static final Pattern TOKEN_SPLIT_REGEX = Pattern.compile("\"[^\"]*\"|'[^']*'|\\[[^\\]]*\\]|\\S+");
    private static final Pattern XML_TAG_TOKEN_REGEX = Pattern.compile("<[^>]+>|[^<]+");

    public List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return tokens;
        }

        Matcher matcher = TOKEN_SPLIT_REGEX.matcher(input.trim());
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    public TokenType classifyValue(String value) {
        if (value == null || value.isEmpty()) {
            return TokenType.WORD;
        }

        if (QUOTE_PATTERN.matcher(value).matches()) {
            return TokenType.QUOTE;
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return TokenType.EMAIL;
        }
        if (ISO_TS_PATTERN.matcher(value).matches() || TIME_PATTERN.matcher(value).matches() || DATE_SLASH_PATTERN.matcher(value).matches() || MONTH_NAME_PATTERN.matcher(value).matches()) {
            return TokenType.TS;
        }
        if (IPV4_PATTERN.matcher(value).matches() || IPV6_PATTERN.matcher(value).matches()) {
            return TokenType.IP;
        }
        if (UUID_PATTERN.matcher(value).matches() || HEX_ID_PATTERN.matcher(value).matches() || PREFIXED_ID_PATTERN.matcher(value).matches()) {
            return TokenType.ID;
        }
        if (NUM_PATTERN.matcher(value).matches()) {
            return TokenType.NUM;
        }
        if (PUNCT_PATTERN.matcher(value).matches()) {
            return TokenType.PUNCT;
        }
        if (WORD_PATTERN.matcher(value).matches()) {
            return TokenType.WORD;
        }
        return TokenType.WORD;
    }

    public String transformTokenToShape(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }

        // Check if token is key=value
        Matcher kvMatcher = KV_PATTERN.matcher(token);
        if (kvMatcher.matches()) {
            String key = kvMatcher.group(1);
            String value = kvMatcher.group(2);
            TokenType valType = classifyValue(value);
            return key + "=" + valType.getTag();
        }

        TokenType type = classifyValue(token);
        return type.getTag();
    }

    public String generateStructuralShape(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }
        String trimmed = rawContent.trim();

        // 1. JSON structure
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JsonNode root = objectMapper.readTree(trimmed);
                if (root.isObject()) {
                    List<String> fieldShapes = new ArrayList<>();
                    Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String key = entry.getKey();
                        JsonNode val = entry.getValue();
                        String valShape = classifyJsonNode(val);
                        fieldShapes.add("\"" + key + "\":" + valShape);
                    }
                    return "{" + String.join(",", fieldShapes) + "}";
                }
            } catch (Exception ignored) {
            }
        }

        // 2. XML structure
        if (trimmed.startsWith("<") && trimmed.endsWith(">") && !trimmed.matches("^<\\d{1,3}>.*")) {
            Matcher xmlMatcher = XML_TAG_TOKEN_REGEX.matcher(trimmed);
            List<String> xmlShapes = new ArrayList<>();
            while (xmlMatcher.find()) {
                String part = xmlMatcher.group().trim();
                if (part.isEmpty()) continue;
                if (part.startsWith("<") && part.endsWith(">")) {
                    xmlShapes.add(part);
                } else {
                    xmlShapes.add(classifyValue(part).getTag());
                }
            }
            if (!xmlShapes.isEmpty()) {
                return String.join("", xmlShapes);
            }
        }

        // 3. Pipe-delimited structure
        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|");
            List<String> partShapes = new ArrayList<>();
            for (String part : parts) {
                partShapes.add(classifyValue(part.trim()).getTag());
            }
            return String.join("|", partShapes);
        }

        // 4. CSV structure
        if (trimmed.contains(",") && !trimmed.contains("=") && !trimmed.startsWith("<") && !trimmed.startsWith("{")) {
            String[] lines = trimmed.split("\\r?\\n");
            String dataLine = lines.length > 1 ? lines[1] : lines[0];
            String[] cells = dataLine.split(",");
            List<String> cellShapes = new ArrayList<>();
            for (String cell : cells) {
                cellShapes.add(classifyValue(cell.trim()).getTag());
            }
            return String.join(",", cellShapes);
        }

        // 5. Default token-by-token structure
        List<String> rawTokens = tokenize(trimmed);
        List<String> shapeTokens = new ArrayList<>();
        for (String token : rawTokens) {
            shapeTokens.add(transformTokenToShape(token));
        }

        return String.join(" ", shapeTokens);
    }

    private String classifyJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return "<NULL>";
        }
        if (node.isNumber()) {
            return TokenType.NUM.getTag();
        }
        if (node.isBoolean()) {
            return "<BOOL>";
        }
        if (node.isArray()) {
            return "<ARRAY>";
        }
        if (node.isObject()) {
            return "<OBJECT>";
        }
        String text = node.asText();
        return classifyValue(text).getTag();
    }
}
