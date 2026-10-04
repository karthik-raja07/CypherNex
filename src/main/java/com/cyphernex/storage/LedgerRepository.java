package com.cyphernex.storage;

import com.cyphernex.ledger.LedgerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class LedgerRepository {

    private static final Logger log = LoggerFactory.getLogger(LedgerRepository.class);

    private final Map<Long, LedgerRecord> inMemoryStore = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;

    public LedgerRepository() {
        this(null);
    }

    public LedgerRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(LedgerRecord record) {
        if (record == null) {
            return;
        }

        // Always update in-memory store
        inMemoryStore.put(record.getSequenceNumber(), record);

        // Attempt PostgreSQL persistence if available
        if (databaseManager != null) {
            String sql = """
                INSERT INTO ledger_records (
                    sequence_number, record_data, previous_hash, current_hash, created_at
                ) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (sequence_number) DO UPDATE
                SET record_data = EXCLUDED.record_data,
                    previous_hash = EXCLUDED.previous_hash,
                    current_hash = EXCLUDED.current_hash
            """;

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, record.getSequenceNumber());
                ps.setString(2, record.getRecordData());
                ps.setString(3, record.getPreviousHash());
                ps.setString(4, record.getCurrentHash());
                ps.setTimestamp(5, record.getCreatedAt() != null ? Timestamp.from(record.getCreatedAt()) : Timestamp.from(java.time.Instant.now()));
                ps.executeUpdate();
            } catch (Exception e) {
                log.debug("Could not persist ledger record to database: {}", e.getMessage());
            }
        }
    }

    public Optional<LedgerRecord> findBySequenceNumber(long sequenceNumber) {
        return Optional.ofNullable(inMemoryStore.get(sequenceNumber));
    }

    public List<LedgerRecord> findAll() {
        return new ArrayList<>(inMemoryStore.values());
    }

    public long count() {
        return inMemoryStore.size();
    }
}
