package com.cyphernex.forge;

import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.ParserVersion;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.ParserRegistry;
import com.cyphernex.storage.ParserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ParserForge {

    private static final Logger log = LoggerFactory.getLogger(ParserForge.class);

    private final GroqClient groqClient;
    private final GrammarCompiler grammarCompiler;
    private final ParserRegistry parserRegistry;
    private final ParserRepository parserRepository;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public ParserForge(
            GroqClient groqClient,
            GrammarCompiler grammarCompiler,
            ParserRegistry parserRegistry,
            ParserRepository parserRepository,
            MetricsService metricsService,
            ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.grammarCompiler = grammarCompiler;
        this.parserRegistry = parserRegistry;
        this.parserRepository = parserRepository;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public Optional<LearnedParser> forgeAndValidate(FormatFingerprint fingerprint, List<RawLog> sampleLogs) {
        if (sampleLogs == null || sampleLogs.isEmpty()) {
            return Optional.empty();
        }

        String sampleLog = sampleLogs.get(0).getRawContent();
        Grammar grammar = groqClient.generateGrammar(fingerprint, sampleLog);

        if (grammar == null || grammar.getFields() == null || grammar.getFields().isEmpty()) {
            log.warn("ParserForge: Received invalid or empty grammar for fingerprint: {}", fingerprint != null ? fingerprint.getFingerprint() : "unknown");
            return Optional.empty();
        }

        LearnedParser learnedParser = grammarCompiler.compile(grammar);

        // Validation against sample log
        ParsedLog parsed = learnedParser.parse(sampleLogs.get(0));
        int totalExpected = grammar.getFields().size();
        int extractedCount = 0;
        int validCount = 0;

        for (GrammarField field : grammar.getFields()) {
            Object val = parsed.getFields().get(field.getName());
            if (val != null && !String.valueOf(val).isBlank()) {
                extractedCount++;
                validCount++;
            }
        }

        double extractionRate = totalExpected > 0 ? (double) extractedCount / totalExpected : 0.0;
        double validationRate = extractedCount > 0 ? (double) validCount / extractedCount : 0.0;
        double nullRate = totalExpected > 0 ? (double) (totalExpected - extractedCount) / totalExpected : 1.0;

        boolean passed = extractionRate >= 0.80 && validationRate >= 0.80 && nullRate <= 0.20;

        if (!passed) {
            log.warn("ParserForge validation failed for grammar {}: extractionRate={}, validationRate={}, nullRate={}",
                    grammar.getFormatName(), extractionRate, validationRate, nullRate);
            return Optional.empty();
        }

        // Validation passed -> promote, register, cache and persist ParserVersion
        metricsService.incrementParsersLearned();

        String fp = fingerprint != null ? fingerprint.getFingerprint() : grammar.getFingerprint();
        parserRegistry.cacheParserByFingerprint(fp, learnedParser);

        try {
            String grammarJson = objectMapper.writeValueAsString(grammar);
            ParserVersion pv = new ParserVersion(
                    learnedParser.getName(),
                    learnedParser.getVersion(),
                    fp,
                    grammarJson,
                    "ACTIVE",
                    extractionRate,
                    validationRate,
                    nullRate
            );
            parserRepository.save(pv);
        } catch (Exception e) {
            log.warn("Failed to serialize grammar for ParserVersion: {}", e.getMessage());
        }

        return Optional.of(learnedParser);
    }

    public GroqClient getGroqClient() {
        return groqClient;
    }

    public MetricsService getMetricsService() {
        return metricsService;
    }
}
