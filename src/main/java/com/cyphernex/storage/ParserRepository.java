package com.cyphernex.storage;

import com.cyphernex.model.ParserVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ParserRepository {

    private static final Logger log = LoggerFactory.getLogger(ParserRepository.class);

    private final Map<String, ParserVersion> inMemoryStore = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;

    public ParserRepository() {
        this(null);
    }

    public ParserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(ParserVersion parserVersion) {
        if (parserVersion == null || parserVersion.getParserName() == null) {
            return;
        }

        // Always update in-memory store
        inMemoryStore.put(parserVersion.getParserName(), parserVersion);

        // Attempt PostgreSQL persistence if available
        if (databaseManager != null) {
            String sql = """
                INSERT INTO parser_versions (
                    parser_name, version, fingerprint, grammar_json, status,
                    extraction_rate, validation_rate, null_rate
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parserVersion.getParserName());
                ps.setString(2, parserVersion.getVersion());
                ps.setString(3, parserVersion.getFingerprint());
                ps.setString(4, parserVersion.getGrammar());
                ps.setString(5, parserVersion.getStatus());
                ps.setDouble(6, parserVersion.getExtractionRate());
                ps.setDouble(7, parserVersion.getValidationRate());
                ps.setDouble(8, parserVersion.getNullRate());
                ps.executeUpdate();
            } catch (Exception e) {
                log.debug("Could not persist parser version to database: {}", e.getMessage());
            }
        }
    }

    public Optional<ParserVersion> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inMemoryStore.get(name));
    }

    public List<ParserVersion> findAll() {
        return new ArrayList<>(inMemoryStore.values());
    }

    public void clear() {
        inMemoryStore.clear();
    }
}

