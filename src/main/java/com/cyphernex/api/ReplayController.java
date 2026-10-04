package com.cyphernex.api;

import com.cyphernex.correlation.CounterfactualReplay;
import com.cyphernex.correlation.EventCorrelator;
import com.cyphernex.correlation.StoryGraph;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ReplayResult;
import com.cyphernex.storage.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private final CounterfactualReplay counterfactualReplay;
    private final EventRepository eventRepository;
    private final EventCorrelator eventCorrelator;

    public ReplayController(
            CounterfactualReplay counterfactualReplay,
            EventRepository eventRepository,
            EventCorrelator eventCorrelator) {
        this.counterfactualReplay = counterfactualReplay;
        this.eventRepository = eventRepository;
        this.eventCorrelator = eventCorrelator;
    }

    @GetMapping({"", "/", "/{chainId}"})
    public ResponseEntity<Map<String, Object>> getReplayChain(@PathVariable(required = false) String chainId) {
        List<NormalizedEvent> rawEvents = new ArrayList<>(eventRepository.findAll());
        StoryGraph graph = eventCorrelator.correlateToGraph(rawEvents, new StoryGraph());
        List<NormalizedEvent> events = graph.getCorrelatedEvents();
        if (events.isEmpty()) {
            events = rawEvents;
        }
        events.sort(Comparator.comparing(NormalizedEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("chainId", chainId != null ? chainId : "default");
        response.put("totalEvents", events.size());
        response.put("events", events);

        return ResponseEntity.ok(response);
    }

    @PostMapping({"/block", "/{chainId}/block"})
    public ResponseEntity<ReplayResult> blockEvent(
            @PathVariable(required = false) String chainId,
            @RequestBody(required = false) Map<String, Object> payload) {
        String eventId = null;
        if (payload != null) {
            if (payload.containsKey("eventId")) {
                eventId = String.valueOf(payload.get("eventId"));
            } else if (payload.containsKey("blockedEventId")) {
                eventId = String.valueOf(payload.get("blockedEventId"));
            } else if (payload.containsKey("id")) {
                eventId = String.valueOf(payload.get("id"));
            }
        }

        List<NormalizedEvent> rawEvents = new ArrayList<>(eventRepository.findAll());
        StoryGraph graph = eventCorrelator.correlateToGraph(rawEvents, new StoryGraph());
        List<NormalizedEvent> events = graph.getCorrelatedEvents();
        if (events.isEmpty()) {
            events = rawEvents;
        }
        ReplayResult result = counterfactualReplay.computePrevented(eventId, events);
        return ResponseEntity.ok(result);
    }

    @GetMapping({"/best-block", "/{chainId}/best-block"})
    public ResponseEntity<Map<String, Object>> getBestBlock(@PathVariable(required = false) String chainId) {
        List<NormalizedEvent> rawEvents = new ArrayList<>(eventRepository.findAll());
        StoryGraph graph = eventCorrelator.correlateToGraph(rawEvents, new StoryGraph());
        List<NormalizedEvent> events = graph.getCorrelatedEvents();
        if (events.isEmpty()) {
            events = rawEvents;
        }
        Map<String, Object> result = counterfactualReplay.findBestBlockPoint(events);
        return ResponseEntity.ok(result);
    }
}
