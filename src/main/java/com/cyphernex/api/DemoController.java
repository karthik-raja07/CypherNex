package com.cyphernex.api;

import com.cyphernex.correlation.ChronoSync;
import com.cyphernex.correlation.StoryGraph;
import com.cyphernex.demo.DemoDataGenerator;
import com.cyphernex.detection.DetectionEngine;
import com.cyphernex.forge.MetricsService;
import com.cyphernex.ledger.LedgerGuard;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.ParserRegistry;
import com.cyphernex.storage.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoDataGenerator demoDataGenerator;
    private final LogController logController;
    private final EventRepository eventRepository;
    private final DetectionEngine detectionEngine;
    private final ParserRegistry parserRegistry;
    private final MetricsService metricsService;
    private final StoryGraph storyGraph;
    private final LedgerGuard ledgerGuard;
    private final ChronoSync chronoSync;
    private final com.cyphernex.storage.ParserRepository parserRepository;

    private final java.util.List<Map<String, Object>> activeDemoLogs = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile String workspaceMode = "LIVE_WORKSPACE";
    private volatile Instant lastIngestTime = null;

    public DemoController(
            DemoDataGenerator demoDataGenerator,
            LogController logController,
            EventRepository eventRepository,
            DetectionEngine detectionEngine,
            ParserRegistry parserRegistry,
            MetricsService metricsService,
            StoryGraph storyGraph,
            LedgerGuard ledgerGuard,
            ChronoSync chronoSync,
            com.cyphernex.storage.ParserRepository parserRepository) {
        this.demoDataGenerator = demoDataGenerator;
        this.logController = logController;
        this.eventRepository = eventRepository;
        this.detectionEngine = detectionEngine;
        this.parserRegistry = parserRegistry;
        this.metricsService = metricsService;
        this.storyGraph = storyGraph;
        this.ledgerGuard = ledgerGuard;
        this.chronoSync = chronoSync;
        this.parserRepository = parserRepository;
    }

    @PostMapping({"/load", "/attack-scenario"})
    public ResponseEntity<Map<String, Object>> loadDemoData() {
        // Reset prior state so demo logs do not mix with live logs
        if (eventRepository != null) eventRepository.clear();
        if (detectionEngine != null) detectionEngine.clear();
        if (metricsService != null) metricsService.reset();
        if (storyGraph != null) storyGraph.clear();
        if (ledgerGuard != null) ledgerGuard.reset();
        if (chronoSync != null) chronoSync.reset();
        if (parserRegistry != null) parserRegistry.clearDynamicParsers();
        if (parserRepository != null) parserRepository.clear();

        workspaceMode = "ATTACK_SCENARIO";
        List<RawLog> sampleLogs = demoDataGenerator.generateSampleLogs();
        List<Map<String, Object>> ingestionResults = new ArrayList<>();
        activeDemoLogs.clear();

        for (RawLog rawLog : sampleLogs) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("rawContent", rawLog.getRawContent());
            payload.put("source", rawLog.getSource());
            ResponseEntity<Map<String, Object>> res = logController.ingestLogs(payload);
            if (res.getBody() != null) {
                ingestionResults.add(res.getBody());

                Map<String, Object> demoLogRecord = new LinkedHashMap<>();
                demoLogRecord.put("source", rawLog.getSource());
                demoLogRecord.put("rawLog", rawLog.getRawContent());
                demoLogRecord.put("origin", "DEMO");

                Object normObj = res.getBody().get("normalizedEvent");
                String eventType = "SECURITY_EVENT";
                if (normObj instanceof com.cyphernex.model.NormalizedEvent norm) {
                    norm.setOrigin("DEMO");
                    eventType = norm.getActivity() != null ? norm.getActivity() : (norm.getEventClass() != null ? norm.getEventClass() : "SECURITY_EVENT");
                } else if (res.getBody().containsKey("detectedFormat")) {
                    eventType = String.valueOf(res.getBody().get("detectedFormat"));
                }
                demoLogRecord.put("eventType", eventType);
                demoLogRecord.put("timestamp", rawLog.getReceivedAt() != null ? rawLog.getReceivedAt().toString() : Instant.now().toString());
                activeDemoLogs.add(demoLogRecord);
            }
        }
        lastIngestTime = Instant.now();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("mode", workspaceMode);
        response.put("storageState", "IN_MEMORY_SESSION_STORAGE");
        response.put("message", "Multi-stage attack scenario loaded successfully (" + sampleLogs.size() + " forensic events ingested into fresh workspace).");
        response.put("totalLogsIngested", sampleLogs.size());
        response.put("results", ingestionResults);
        response.put("demoLogs", activeDemoLogs);

        return ResponseEntity.ok(response);
    }

    @GetMapping({"/load", "/attack-scenario"})
    public ResponseEntity<Map<String, Object>> loadDemoDataGet() {
        return loadDemoData();
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getDemoLogs() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("mode", workspaceMode);
        response.put("total", activeDemoLogs.size());
        response.put("demoLogs", activeDemoLogs);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/reset", "/clear-workspace"})
    public ResponseEntity<Map<String, Object>> resetDemo() {
        workspaceMode = "LIVE_WORKSPACE";
        activeDemoLogs.clear();
        lastIngestTime = null;
        if (eventRepository != null) eventRepository.clear();
        if (detectionEngine != null) detectionEngine.clear();
        if (metricsService != null) metricsService.reset();
        if (storyGraph != null) storyGraph.clear();
        if (ledgerGuard != null) ledgerGuard.reset();
        if (chronoSync != null) chronoSync.reset();
        if (parserRegistry != null) parserRegistry.clearDynamicParsers();
        if (parserRepository != null) parserRepository.clear();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("mode", workspaceMode);
        response.put("message", "Workspace cleared completely (metrics, detections, story graph, ledger, and dynamic parsers reset).");
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/reset", "/clear-workspace"})
    public ResponseEntity<Map<String, Object>> resetDemoGet() {
        return resetDemo();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", workspaceMode);
        response.put("lastIngestTime", lastIngestTime != null ? lastIngestTime.toString() : "None");
        response.put("totalLogs", activeDemoLogs.size());
        return ResponseEntity.ok(response);
    }
}

