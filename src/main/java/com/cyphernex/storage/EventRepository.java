package com.cyphernex.storage;

import com.cyphernex.model.NormalizedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class EventRepository {

    private static final Logger log = LoggerFactory.getLogger(EventRepository.class);

    private final Map<String, NormalizedEvent> inMemoryStore = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;
    private final ObjectMapper objectMapper;

    public EventRepository() {
        this(null, new ObjectMapper());
    }

    public EventRepository(DatabaseManager databaseManager, ObjectMapper objectMapper) {
        this.databaseManager = databaseManager;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public void save(NormalizedEvent event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        // Always update in-memory store
        inMemoryStore.put(event.getEventId(), event);

        // Attempt PostgreSQL persistence if available
        if (databaseManager != null) {
            String sql = """
                INSERT INTO normalized_events (
                    event_id, timestamp, event_class, activity, severity, source,
                    user_identity, source_ip, destination_ip, host, session_id,
                    request_id, process, raw_log_id, parser_version, confidence,
                    provenance_json, additional_fields_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
            """;

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, event.getEventId());
                ps.setTimestamp(2, event.getTimestamp() != null ? Timestamp.from(event.getTimestamp()) : Timestamp.from(java.time.Instant.now()));
                ps.setString(3, event.getEventClass());
                ps.setString(4, event.getActivity());
                ps.setString(5, event.getSeverity());
                ps.setString(6, event.getSource());
                ps.setString(7, event.getUser());
                ps.setString(8, event.getSourceIp());
                ps.setString(9, event.getDestinationIp());
                ps.setString(10, event.getHost());
                ps.setString(11, event.getSessionId());
                ps.setString(12, event.getRequestId());
                ps.setString(13, event.getProcess());
                ps.setString(14, event.getRawLogId());
                ps.setString(15, event.getParserVersion());
                ps.setDouble(16, event.getConfidence());
                ps.setString(17, event.getProvenance() != null ? objectMapper.writeValueAsString(event.getProvenance()) : "{}");
                ps.setString(18, event.getAdditionalFields() != null ? objectMapper.writeValueAsString(event.getAdditionalFields()) : "{}");
                ps.executeUpdate();
            } catch (Exception e) {
                log.debug("Could not persist normalized event to database: {}", e.getMessage());
            }
        }
    }

    public Optional<NormalizedEvent> findById(String eventId) {
        if (eventId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inMemoryStore.get(eventId));
    }

    public List<NormalizedEvent> findAll() {
        return new ArrayList<>(inMemoryStore.values());
    }

    public long count() {
        return inMemoryStore.size();
    }

    public void clear() {
        inMemoryStore.clear();
    }
}
