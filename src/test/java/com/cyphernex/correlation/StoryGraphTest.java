package com.cyphernex.correlation;

import com.cyphernex.identity.IdentityWeaver;
import com.cyphernex.model.GraphEdge;
import com.cyphernex.model.GraphNode;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.storage.IdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StoryGraphTest {

    private StoryGraph storyGraph;
    private EventCorrelator eventCorrelator;

    @BeforeEach
    void setUp() {
        storyGraph = new StoryGraph();
        IdentityWeaver identityWeaver = new IdentityWeaver(new IdentityRepository());
        ChronoSync chronoSync = new ChronoSync();
        eventCorrelator = new EventCorrelator(storyGraph, identityWeaver, chronoSync);
    }

    @Test
    @DisplayName("Test manual graph node and edge creation")
    void testGraphNodeAndEdgeCreation() {
        storyGraph.addNode("user:john", "USER", "john");
        storyGraph.addNode("event:EVT-1", "EVENT", "LOGIN");
        storyGraph.addEdge("user:john", "event:EVT-1", "USER_PERFORMED");

        List<GraphNode> nodes = storyGraph.getNodes();
        List<GraphEdge> edges = storyGraph.getEdges();

        assertEquals(2, nodes.size());
        assertEquals(1, edges.size());
        assertEquals("USER_PERFORMED", edges.get(0).getRelationship());
        assertEquals("user:john", edges.get(0).getSource());
        assertEquals("event:EVT-1", edges.get(0).getTarget());
    }

    @Test
    @DisplayName("Test event correlation pipeline and attack chain creation")
    void testEventCorrelationPipeline() {
        Instant now = Instant.now();

        // 1. LOGIN
        NormalizedEvent e1 = new NormalizedEvent();
        e1.setEventId("E1");
        e1.setUser("john");
        e1.setActivity("LOGIN");
        e1.setSourceIp("10.10.2.15");
        e1.setTimestamp(now);

        // 2. PROCESS_STARTED
        NormalizedEvent e2 = new NormalizedEvent();
        e2.setEventId("E2");
        e2.setUser("john.doe@company.com");
        e2.setActivity("PROCESS_STARTED");
        e2.setProcess("powershell.exe");
        e2.setHost("HOST-01");
        e2.setTimestamp(now.plusSeconds(5));

        // 3. FILE_ACCESS
        NormalizedEvent e3 = new NormalizedEvent();
        e3.setEventId("E3");
        e3.setUser("AD-USER-442");
        e3.setActivity("FILE_ACCESS");
        e3.setTimestamp(now.plusSeconds(10));

        // 4. NETWORK_CONNECTION
        NormalizedEvent e4 = new NormalizedEvent();
        e4.setEventId("E4");
        e4.setUser("john");
        e4.setActivity("NETWORK_CONNECTION");
        e4.setDestinationIp("198.51.100.4");
        e4.setTimestamp(now.plusSeconds(15));

        eventCorrelator.correlate(List.of(e1, e2, e3, e4));

        List<GraphNode> nodes = storyGraph.getNodes();
        List<GraphEdge> edges = storyGraph.getEdges();

        assertFalse(nodes.isEmpty());
        assertFalse(edges.isEmpty());

        // Verify sequential triggered links exist
        boolean hasTriggered = edges.stream().anyMatch(e -> "EVENT_TRIGGERED".equals(e.getRelationship()));
        assertTrue(hasTriggered, "Should create EVENT_TRIGGERED edges between correlated attack chain events");

        // Narrative verification
        String narrative = storyGraph.generateNarrative();
        assertNotNull(narrative);
        assertTrue(narrative.contains("User john"));
        assertTrue(narrative.contains("logged in"));
        assertTrue(narrative.contains("accessed a sensitive file"));
    }
}
