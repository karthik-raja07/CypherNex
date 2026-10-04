package com.cyphernex.parser;

import com.cyphernex.model.RawLog;
import com.cyphernex.parser.builtin.CsvLogParser;
import com.cyphernex.parser.builtin.JsonLogParser;
import com.cyphernex.parser.builtin.PlainTextParser;
import com.cyphernex.parser.builtin.SyslogParser;
import com.cyphernex.parser.builtin.XmlLogParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ParserRegistry {

    private final Map<String, LogParser> parsersByName = new ConcurrentHashMap<>();
    private final Map<String, LogParser> parsersByFormat = new ConcurrentHashMap<>();
    private final Map<String, LogParser> parsersByFingerprint = new ConcurrentHashMap<>();

    public ParserRegistry() {
        registerBuiltInDefaults();
    }

    public ParserRegistry(List<LogParser> customParsers) {
        registerBuiltInDefaults();
        if (customParsers != null) {
            for (LogParser parser : customParsers) {
                registerParser(parser);
            }
        }
    }

    private void registerBuiltInDefaults() {
        registerParser(new JsonLogParser());
        registerParser(new CsvLogParser());
        registerParser(new XmlLogParser());
        registerParser(new SyslogParser());
        registerParser(new PlainTextParser());
    }

    public void registerParser(LogParser parser) {
        if (parser != null && parser.getName() != null) {
            parsersByName.put(parser.getName(), parser);
            if (parser.getSupportedFormat() != null) {
                parsersByFormat.put(parser.getSupportedFormat().toUpperCase(), parser);
            }
        }
    }

    public Optional<LogParser> getParser(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parsersByName.get(name));
    }

    public Collection<LogParser> getAllParsers() {
        return new ArrayList<>(parsersByName.values());
    }

    public Optional<LogParser> findParserForFormat(String format) {
        if (format == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parsersByFormat.get(format.toUpperCase()));
    }

    public Optional<LogParser> findParserForLog(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null) {
            return Optional.empty();
        }
        for (LogParser parser : parsersByName.values()) {
            if (parser.canParse(rawLog)) {
                return Optional.of(parser);
            }
        }
        return Optional.empty();
    }

    public void cacheParserByFingerprint(String fingerprint, LogParser parser) {
        if (fingerprint != null && parser != null) {
            parsersByFingerprint.put(fingerprint, parser);
        }
    }

    public Optional<LogParser> getParserByFingerprint(String fingerprint) {
        if (fingerprint == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parsersByFingerprint.get(fingerprint));
    }

    public void clearDynamicParsers() {
        parsersByFingerprint.clear();
        parsersByName.entrySet().removeIf(e -> e.getValue() instanceof com.cyphernex.forge.LearnedParser);
        parsersByFormat.entrySet().removeIf(e -> e.getValue() instanceof com.cyphernex.forge.LearnedParser);
    }
}
