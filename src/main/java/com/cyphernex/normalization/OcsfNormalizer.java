package com.cyphernex.normalization;

import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParsedLog;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OcsfNormalizer {

    private final SchemaMapper schemaMapper;

    public OcsfNormalizer(SchemaMapper schemaMapper) {
        this.schemaMapper = schemaMapper;
    }

    public NormalizedEvent normalize(ParsedLog parsedLog, String rawLogId, String fingerprint) {
        return schemaMapper.mapToNormalizedEvent(parsedLog, rawLogId, fingerprint);
    }

    public NormalizedEvent normalize(ParsedLog parsedLog) {
        return schemaMapper.mapToNormalizedEvent(parsedLog, UUID.randomUUID().toString(), "unknown-fingerprint");
    }

    public SchemaMapper getSchemaMapper() {
        return schemaMapper;
    }
}
