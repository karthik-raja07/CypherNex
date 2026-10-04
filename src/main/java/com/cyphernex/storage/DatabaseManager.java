package com.cyphernex.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final String dbUrl;
    private final String username;
    private final String password;
    private volatile Boolean available = null;

    public DatabaseManager(
            @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/cyphernex}") String dbUrl,
            @Value("${spring.datasource.username:postgres}") String username,
            @Value("${spring.datasource.password:postgres}") String password) {
        this.dbUrl = dbUrl;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    public void init() {
        if (isDatabaseAvailable()) {
            log.info("PostgreSQL database is available at {}. Initializing schema...", dbUrl);
            initializeTables();
        } else {
            log.warn("PostgreSQL database is NOT available at {}. Operating in resilient IN-MEMORY storage mode.", dbUrl);
        }
    }

    public boolean isDatabaseAvailable() {
        try (Connection conn = getConnection()) {
            boolean ok = conn != null && !conn.isClosed();
            this.available = ok;
            return ok;
        } catch (Exception e) {
            this.available = false;
            log.debug("Database availability check failed: {}", e.getMessage());
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        DriverManager.setLoginTimeout(2);
        return DriverManager.getConnection(dbUrl, username, password);
    }

    public void initializeTables() {
        String createNormalizedEvents = """
            CREATE TABLE IF NOT EXISTS normalized_events (
                id BIGSERIAL PRIMARY KEY,
                event_id VARCHAR(64) UNIQUE,
                timestamp TIMESTAMP WITH TIME ZONE,
                event_class VARCHAR(64),
                activity VARCHAR(64),
                severity VARCHAR(32),
                source VARCHAR(64),
                user_identity VARCHAR(128),
                source_ip VARCHAR(64),
                destination_ip VARCHAR(64),
                host VARCHAR(128),
                session_id VARCHAR(128),
                request_id VARCHAR(128),
                process VARCHAR(128),
                raw_log_id VARCHAR(64),
                parser_version VARCHAR(32),
                confidence DOUBLE PRECISION,
                provenance_json TEXT,
                additional_fields_json TEXT
            );
        """;

        String createParserVersions = """
            CREATE TABLE IF NOT EXISTS parser_versions (
                id BIGSERIAL PRIMARY KEY,
                parser_name VARCHAR(128),
                version VARCHAR(32),
                fingerprint VARCHAR(128),
                grammar_json TEXT,
                status VARCHAR(32),
                extraction_rate DOUBLE PRECISION,
                validation_rate DOUBLE PRECISION,
                null_rate DOUBLE PRECISION,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );
        """;

        String createLedgerRecords = """
            CREATE TABLE IF NOT EXISTS ledger_records (
                id BIGSERIAL PRIMARY KEY,
                sequence_number BIGINT UNIQUE,
                record_data TEXT,
                previous_hash VARCHAR(128),
                current_hash VARCHAR(128),
                created_at TIMESTAMP WITH TIME ZONE
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createNormalizedEvents);
            stmt.execute(createParserVersions);
            stmt.execute(createLedgerRecords);
            log.info("Database schema initialized successfully.");
        } catch (Exception e) {
            log.warn("Failed to initialize database tables: {}. Falling back to in-memory mode.", e.getMessage());
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getUsername() {
        return username;
    }
}
