package com.cyphernex.fingerprint;

import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.RawLog;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public class FormatDnaEngine {

    private final Tokenizer tokenizer;

    private static final Pattern SYSLOG_PRI_PATTERN = Pattern.compile("^<\\d{1,3}>.*");
    private static final Pattern SYSLOG_RFC3164_PATTERN = Pattern.compile("^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("^<[a-zA-Z0-9_.-]+(?:\\s+[^>]*)?>.*</[a-zA-Z0-9_.-]+>$|^<[a-zA-Z0-9_.-]+(?:\\s+[^>]*)?/>$", Pattern.DOTALL);

    public FormatDnaEngine(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public FormatFingerprint generateFingerprint(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null || rawLog.getRawContent().isBlank()) {
            return new FormatFingerprint("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "", "UNKNOWN", Instant.now(), Instant.now());
        }

        String content = rawLog.getRawContent().trim();
        String format = detectFormat(content);
        String shape = tokenizer.generateStructuralShape(content);
        String fingerprint = computeSha256(shape);

        return new FormatFingerprint(fingerprint, shape, format, Instant.now(), Instant.now());
    }

    public String detectFormat(String content) {
        if (content == null || content.isBlank()) {
            return "UNKNOWN";
        }
        String trimmed = content.trim();

        // 1. JSON check
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return "JSON";
        }

        // 2. Syslog check (PRI tag <134> or RFC 3164 timestamp header)
        if (SYSLOG_PRI_PATTERN.matcher(trimmed).matches() || SYSLOG_RFC3164_PATTERN.matcher(trimmed).matches()) {
            return "SYSLOG";
        }

        // 3. XML check
        if (trimmed.startsWith("<") && trimmed.endsWith(">") && XML_TAG_PATTERN.matcher(trimmed).matches()) {
            return "XML";
        }

        // 4. CSV check: contains commas, no key=value signs, and multiple comma-separated values
        if (trimmed.contains(",") && !trimmed.contains("=") && trimmed.split(",").length >= 2) {
            return "CSV";
        }

        // 5. Plain text key=value
        if (trimmed.contains("=")) {
            return "PLAIN_TEXT";
        }

        // 6. Pipe-delimited or unknown
        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
            return "CUSTOM_DELIMITED";
        }

        return "UNKNOWN";
    }

    public String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }
}
