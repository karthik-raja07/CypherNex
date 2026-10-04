package com.cyphernex.api;

import com.cyphernex.forge.GroqClient;
import com.cyphernex.forge.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;
    private final GroqClient groqClient;

    public MetricsController(MetricsService metricsService, GroqClient groqClient) {
        this.metricsService = metricsService;
        this.groqClient = groqClient;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics(groqClient.getLlmMode()));
    }
}
