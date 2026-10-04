package com.cyphernex.api;

import com.cyphernex.identity.IdentityWeaver;
import com.cyphernex.model.IdentityRecord;
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
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityWeaver identityWeaver;
    private final EventRepository eventRepository;

    public IdentityController(IdentityWeaver identityWeaver, EventRepository eventRepository) {
        this.identityWeaver = identityWeaver;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/resolve")
    public ResponseEntity<Map<String, Object>> resolveIdentitiesGet() {
        return resolveIdentities(null);
    }

    @PostMapping("/resolve")
    public ResponseEntity<Map<String, Object>> resolveIdentities(@RequestBody(required = false) List<NormalizedEvent> events) {
        List<NormalizedEvent> targetEvents = (events != null && !events.isEmpty()) ? events : eventRepository.findAll();
        List<IdentityRecord> clusters = identityWeaver.resolveIdentities(targetEvents);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("totalClusters", clusters.size());
        response.put("identities", clusters);

        if (!clusters.isEmpty()) {
            IdentityRecord primary = clusters.get(0);
            response.put("canonicalIdentity", primary.getCanonicalIdentity());
            response.put("aliases", primary.getAliases());
            response.put("confidence", primary.getConfidence());
            response.put("evidence", primary.getEvidence());
        }

        return ResponseEntity.ok(response);
    }
}
