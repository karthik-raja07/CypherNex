package com.cyphernex.identity;

import com.cyphernex.model.IdentityRecord;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.storage.IdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IdentityWeaverTest {

    private IdentityWeaver identityWeaver;

    @BeforeEach
    void setUp() {
        IdentityRepository identityRepository = new IdentityRepository();
        identityWeaver = new IdentityWeaver(identityRepository);
    }

    @Test
    @DisplayName("Test confidence calculation for different evidence combinations")
    void testConfidenceCalculation() {
        assertEquals(0.50, identityWeaver.calculateConfidence(true, false, false));
        assertEquals(0.25, identityWeaver.calculateConfidence(false, true, false));
        assertEquals(0.25, identityWeaver.calculateConfidence(false, false, true));
        assertEquals(0.75, identityWeaver.calculateConfidence(true, true, false));
        assertEquals(1.00, identityWeaver.calculateConfidence(true, true, true));
    }

    @Test
    @DisplayName("Test string similarity for user identifiers and email prefixes")
    void testStringSimilarity() {
        assertTrue(identityWeaver.hasStringSimilarity("john", "john.doe@company.com"));
        assertTrue(identityWeaver.hasStringSimilarity("alice", "alice.smith"));
        assertTrue(identityWeaver.hasStringSimilarity("bob_jones", "bobjones"));
        assertFalse(identityWeaver.hasStringSimilarity("alice", "charlie"));
    }

    @Test
    @DisplayName("Test alias resolution with shared requestId and string similarity")
    void testIdentityResolutionWithSharedRequestId() {
        Instant now = Instant.now();

        NormalizedEvent event1 = new NormalizedEvent();
        event1.setEventId("EVT-1");
        event1.setUser("john");
        event1.setRequestId("REQ-999");
        event1.setTimestamp(now);

        NormalizedEvent event2 = new NormalizedEvent();
        event2.setEventId("EVT-2");
        event2.setUser("john.doe@company.com");
        event2.setRequestId("REQ-999");
        event2.setTimestamp(now.plusSeconds(10));

        NormalizedEvent event3 = new NormalizedEvent();
        event3.setEventId("EVT-3");
        event3.setUser("AD-USER-442");
        event3.setRequestId("REQ-999");
        event3.setTimestamp(now.plusSeconds(20));

        List<IdentityRecord> clusters = identityWeaver.resolveIdentities(List.of(event1, event2, event3));

        assertNotNull(clusters);
        assertEquals(1, clusters.size());

        IdentityRecord cluster = clusters.get(0);
        assertEquals("john", cluster.getCanonicalIdentity());
        assertTrue(cluster.getAliases().contains("john"));
        assertTrue(cluster.getAliases().contains("john.doe@company.com"));
        assertTrue(cluster.getAliases().contains("AD-USER-442"));
        assertTrue(cluster.getConfidence() >= 0.75);
        assertTrue(cluster.getEvidence().contains("shared sessionId/requestId"));

        // Canonical translation
        assertEquals("john", identityWeaver.getCanonicalIdentity("john.doe@company.com"));
        assertEquals("john", identityWeaver.getCanonicalIdentity("AD-USER-442"));
    }
}
