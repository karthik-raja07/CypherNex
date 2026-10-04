package com.cyphernex.api;

import com.cyphernex.correlation.ChronoSync;
import com.cyphernex.storage.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chronosync")
public class ChronoSyncController {

    private final ChronoSync chronoSync;
    private final EventRepository eventRepository;

    public ChronoSyncController(ChronoSync chronoSync, EventRepository eventRepository) {
        this.chronoSync = chronoSync;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getClockOffsets() {
        // Automatically sync offsets on available events
        chronoSync.calculateOffsets(eventRepository.findAll());

        List<Map<String, Object>> offsets = chronoSync.getClockOffsets();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("total", offsets.size());
        response.put("offsets", offsets);
        return ResponseEntity.ok(response);
    }
}
