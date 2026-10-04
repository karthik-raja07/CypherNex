package com.cyphernex.correlation;

import com.cyphernex.identity.IdentityWeaver;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ReplayResult;
import com.cyphernex.storage.IdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CounterfactualReplayTest {

    private CounterfactualReplay counterfactualReplay;

    @BeforeEach
    void setUp() {
        StoryGraph storyGraph = new StoryGraph();
        IdentityWeaver identityWeaver = new IdentityWeaver(new IdentityRepository());
        ChronoSync chronoSync = new ChronoSync();
        EventCorrelator eventCorrelator = new EventCorrelator(storyGraph, identityWeaver, chronoSync);
        counterfactualReplay = new CounterfactualReplay(eventCorrelator);
    }

    @Test
    @DisplayName("1 & 2. Blocking one event prevents direct child and propagates through multiple causal levels")
    void testPropagationAcrossMultipleLevels() {
        Instant now = Instant.now();

        NormalizedEvent e1 = new NormalizedEvent();
        e1.setEventId("EVT-1");
        e1.setActivity("INITIAL_ACCESS");
        e1.setSeverity("HIGH"); // weight 3
        e1.setTimestamp(now);

        NormalizedEvent e2 = new NormalizedEvent();
        e2.setEventId("EVT-2");
        e2.setActivity("CREDENTIAL_DUMP");
        e2.setSeverity("CRITICAL"); // weight 5
        e2.setTimestamp(now.plusSeconds(10));
        e2.addParentEventId("EVT-1");

        NormalizedEvent e3 = new NormalizedEvent();
        e3.setEventId("EVT-3");
        e3.setActivity("LATERAL_MOVEMENT");
        e3.setSeverity("HIGH"); // weight 3
        e3.setTimestamp(now.plusSeconds(20));
        e3.addParentEventId("EVT-2");

        NormalizedEvent e4 = new NormalizedEvent();
        e4.setEventId("EVT-4");
        e4.setActivity("DATA_EXFILTRATION");
        e4.setSeverity("CRITICAL"); // weight 5
        e4.setTimestamp(now.plusSeconds(30));
        e4.addParentEventId("EVT-3");

        NormalizedEvent eUnrelated = new NormalizedEvent();
        eUnrelated.setEventId("EVT-99");
        eUnrelated.setActivity("BENIGN_BACKUP");
        eUnrelated.setSeverity("LOW"); // weight 1
        eUnrelated.setTimestamp(now.plusSeconds(5));

        List<NormalizedEvent> allEvents = List.of(e1, e2, e3, e4, eUnrelated);

        // Blocking EVT-1 should propagate to EVT-2, EVT-3, EVT-4
        ReplayResult res = counterfactualReplay.computePrevented("EVT-1", allEvents);

        assertEquals(4, res.getEventsPrevented());
        assertTrue(res.getPreventedEventIds().containsAll(List.of("EVT-1", "EVT-2", "EVT-3", "EVT-4")));

        // 3. Unrelated events remain in stillHappensEventIds
        assertTrue(res.getStillHappensEventIds().contains("EVT-99"));
        assertFalse(res.getStillHappensEventIds().contains("EVT-1"));
        assertFalse(res.getStillHappensEventIds().contains("EVT-4"));

        // 4 & 5. Severity weights & impactStoppedPercent:
        // Prevented: 3 + 5 + 3 + 5 = 16
        // Total: 3 + 5 + 3 + 5 + 1 = 17
        // Impact = 16 / 17 * 100 = 94.11...% -> 94%
        assertEquals(94, res.getImpactStoppedPercent());
    }

    @Test
    @DisplayName("6 & 7. Best block point selects highest impact and ties select earliest timestamp")
    void testBestBlockPointSelectionAndTieBreaker() {
        Instant now = Instant.now();

        // Chain A: EVT-A1 -> EVT-A2 (High + Critical = 3 + 5 = 8)
        NormalizedEvent a1 = new NormalizedEvent();
        a1.setEventId("EVT-A1");
        a1.setActivity("PHISHING");
        a1.setSeverity("HIGH");
        a1.setTimestamp(now);

        NormalizedEvent a2 = new NormalizedEvent();
        a2.setEventId("EVT-A2");
        a2.setActivity("MALWARE_EXEC");
        a2.setSeverity("CRITICAL");
        a2.setTimestamp(now.plusSeconds(10));
        a2.addParentEventId("EVT-A1");

        // Chain B: EVT-B1 -> EVT-B2 (High + Critical = 3 + 5 = 8), but timestamp later
        NormalizedEvent b1 = new NormalizedEvent();
        b1.setEventId("EVT-B1");
        b1.setActivity("BRUTE_FORCE");
        b1.setSeverity("HIGH");
        b1.setTimestamp(now.plusSeconds(5));

        NormalizedEvent b2 = new NormalizedEvent();
        b2.setEventId("EVT-B2");
        b2.setActivity("PRIV_ESC");
        b2.setSeverity("CRITICAL");
        b2.setTimestamp(now.plusSeconds(15));
        b2.addParentEventId("EVT-B1");

        List<NormalizedEvent> allEvents = List.of(b1, b2, a1, a2);

        Map<String, Object> bestBlock = counterfactualReplay.findBestBlockPoint(allEvents);

        assertEquals("SUCCESS", bestBlock.get("status"));
        // Both A1 and B1 prevent 8 weight (50% impact). Tie breaker chooses earliest timestamp: EVT-A1
        assertEquals("EVT-A1", bestBlock.get("recommendedEventId"));
        assertEquals(50, bestBlock.get("impactStoppedPercent"));
    }

    @Test
    @DisplayName("8 & 9. Replay does not modify original events list or data")
    void testReplayDoesNotModifyOriginalEvents() {
        NormalizedEvent e1 = new NormalizedEvent();
        e1.setEventId("EVT-1");
        e1.setActivity("LOGIN");
        e1.setSeverity("MEDIUM");
        e1.setTimestamp(Instant.now());

        NormalizedEvent e2 = new NormalizedEvent();
        e2.setEventId("EVT-2");
        e2.setActivity("FILE_ACCESS");
        e2.setSeverity("HIGH");
        e2.setTimestamp(Instant.now().plusSeconds(5));
        e2.addParentEventId("EVT-1");

        List<NormalizedEvent> originalList = List.of(e1, e2);
        int origSize = originalList.size();

        ReplayResult res = counterfactualReplay.computePrevented("EVT-1", originalList);

        assertEquals(2, res.getEventsPrevented());
        assertEquals(origSize, originalList.size());
        assertEquals("EVT-1", originalList.get(0).getId());
        assertEquals("EVT-2", originalList.get(1).getId());
    }

    @Test
    @DisplayName("10. Test with a simple guaranteed graph E1->E2->E3->E4 and independent E5: all produce distinct results")
    void testGuaranteedChainGraphDifferentBlockPoints() {
        Instant now = Instant.now();

        NormalizedEvent e1 = new NormalizedEvent();
        e1.setEventId("E1");
        e1.setActivity("ACT-1");
        e1.setSeverity("LOW"); // 1
        e1.setTimestamp(now);

        NormalizedEvent e2 = new NormalizedEvent();
        e2.setEventId("E2");
        e2.setActivity("ACT-2");
        e2.setSeverity("MEDIUM"); // 2
        e2.setTimestamp(now.plusSeconds(10));
        e2.addParentEventId("E1");

        NormalizedEvent e3 = new NormalizedEvent();
        e3.setEventId("E3");
        e3.setActivity("ACT-3");
        e3.setSeverity("HIGH"); // 3
        e3.setTimestamp(now.plusSeconds(20));
        e3.addParentEventId("E2");

        NormalizedEvent e4 = new NormalizedEvent();
        e4.setEventId("E4");
        e4.setActivity("ACT-4");
        e4.setSeverity("CRITICAL"); // 5
        e4.setTimestamp(now.plusSeconds(30));
        e4.addParentEventId("E3");

        NormalizedEvent e5 = new NormalizedEvent();
        e5.setEventId("E5");
        e5.setActivity("ACT-5");
        e5.setSeverity("LOW"); // 1
        e5.setTimestamp(now.plusSeconds(40));

        // Total weights = 1 + 2 + 3 + 5 + 1 = 12
        List<NormalizedEvent> allEvents = List.of(e1, e2, e3, e4, e5);

        // Blocking E1: prevented = E1, E2, E3, E4 (weights: 1+2+3+5 = 11 / 12 = 92%), E5 still happens
        ReplayResult res1 = counterfactualReplay.computePrevented("E1", allEvents);
        assertEquals(4, res1.getEventsPrevented());
        assertTrue(res1.getPreventedEventIds().containsAll(List.of("E1", "E2", "E3", "E4")));
        assertEquals(List.of("E5"), res1.getStillHappensEventIds());
        assertEquals(92, res1.getImpactStoppedPercent());

        // Blocking E2: prevented = E2, E3, E4 (weights: 2+3+5 = 10 / 12 = 83%), E1, E5 still happen
        ReplayResult res2 = counterfactualReplay.computePrevented("E2", allEvents);
        assertEquals(3, res2.getEventsPrevented());
        assertTrue(res2.getPreventedEventIds().containsAll(List.of("E2", "E3", "E4")));
        assertTrue(res2.getStillHappensEventIds().containsAll(List.of("E1", "E5")));
        assertEquals(83, res2.getImpactStoppedPercent());

        // Blocking E3: prevented = E3, E4 (weights: 3+5 = 8 / 12 = 67%), E1, E2, E5 still happen
        ReplayResult res3 = counterfactualReplay.computePrevented("E3", allEvents);
        assertEquals(2, res3.getEventsPrevented());
        assertTrue(res3.getPreventedEventIds().containsAll(List.of("E3", "E4")));
        assertTrue(res3.getStillHappensEventIds().containsAll(List.of("E1", "E2", "E5")));
        assertEquals(67, res3.getImpactStoppedPercent());

        // Blocking E4: prevented = E4 (weights: 5 / 12 = 42%), E1, E2, E3, E5 still happen
        ReplayResult res4 = counterfactualReplay.computePrevented("E4", allEvents);
        assertEquals(1, res4.getEventsPrevented());
        assertTrue(res4.getPreventedEventIds().contains("E4"));
        assertTrue(res4.getStillHappensEventIds().containsAll(List.of("E1", "E2", "E3", "E5")));
        assertEquals(42, res4.getImpactStoppedPercent());

        // Blocking E5: prevented = E5 (weights: 1 / 12 = 8%), E1, E2, E3, E4 still happen
        ReplayResult res5 = counterfactualReplay.computePrevented("E5", allEvents);
        assertEquals(1, res5.getEventsPrevented());
        assertTrue(res5.getPreventedEventIds().contains("E5"));
        assertTrue(res5.getStillHappensEventIds().containsAll(List.of("E1", "E2", "E3", "E4")));
        assertEquals(8, res5.getImpactStoppedPercent());
    }

    @Test
    @DisplayName("Empty events list handling")
    void testEmptyEventsList() {
        ReplayResult res = counterfactualReplay.computePrevented("EVT-1", List.of());
        assertEquals(0, res.getEventsPrevented());
        assertEquals(0, res.getTotalEvents());
        assertEquals(0, res.getImpactStoppedPercent());

        Map<String, Object> bestBlock = counterfactualReplay.findBestBlockPoint(List.of());
        assertEquals("EMPTY", bestBlock.get("status"));
    }
}
