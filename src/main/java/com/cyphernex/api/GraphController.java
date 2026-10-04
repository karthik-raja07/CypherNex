package com.cyphernex.api;

import com.cyphernex.correlation.CounterfactualReplay;
import com.cyphernex.correlation.EventCorrelator;
import com.cyphernex.correlation.StoryGraph;
import com.cyphernex.model.GraphEdge;
import com.cyphernex.model.GraphNode;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.storage.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final StoryGraph storyGraph;
    private final EventCorrelator eventCorrelator;
    private final EventRepository eventRepository;
    private final CounterfactualReplay counterfactualReplay;

    public GraphController(
            StoryGraph storyGraph,
            EventCorrelator eventCorrelator,
            EventRepository eventRepository,
            CounterfactualReplay counterfactualReplay) {
        this.storyGraph = storyGraph;
        this.eventCorrelator = eventCorrelator;
        this.eventRepository = eventRepository;
        this.counterfactualReplay = counterfactualReplay;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getGraph() {
        List<NormalizedEvent> events = eventRepository.findAll();
        eventCorrelator.correlate(events);

        List<GraphNode> nodes = storyGraph.getNodes();
        List<GraphEdge> edges = storyGraph.getEdges();
        String narrative = storyGraph.generateNarrative();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("totalNodes", nodes.size());
        response.put("totalEdges", edges.size());
        response.put("nodes", nodes);
        response.put("edges", edges);
        response.put("narrative", narrative);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/replay")
    public ResponseEntity<Map<String, Object>> replayGraph(@RequestBody(required = false) Map<String, Object> payload) {
        String eventIdToRemove = null;
        if (payload != null) {
            if (payload.containsKey("eventId")) {
                eventIdToRemove = String.valueOf(payload.get("eventId"));
            } else if (payload.containsKey("removedEventId")) {
                eventIdToRemove = String.valueOf(payload.get("removedEventId"));
            } else if (payload.containsKey("event")) {
                eventIdToRemove = String.valueOf(payload.get("event"));
            }
        }

        List<NormalizedEvent> realEvents = eventRepository.findAll();
        Map<String, Object> replayResult = counterfactualReplay.runReplay(realEvents, eventIdToRemove);
        return ResponseEntity.ok(replayResult);
    }
}
