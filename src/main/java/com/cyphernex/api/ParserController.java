package com.cyphernex.api;

import com.cyphernex.parser.LogParser;
import com.cyphernex.parser.ParserRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parsers")
public class ParserController {

    private final ParserRegistry parserRegistry;
    private final com.cyphernex.storage.ParserRepository parserRepository;

    public ParserController(ParserRegistry parserRegistry, com.cyphernex.storage.ParserRepository parserRepository) {
        this.parserRegistry = parserRegistry;
        this.parserRepository = parserRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getParsers() {
        Map<String, Object> response = new LinkedHashMap<>();
        List<Map<String, Object>> parserList = new ArrayList<>();

        for (LogParser parser : parserRegistry.getAllParsers()) {
            Map<String, Object> parserInfo = new LinkedHashMap<>();
            parserInfo.put("name", parser.getName());
            parserInfo.put("version", parser.getVersion());
            parserInfo.put("supportedFormat", parser.getSupportedFormat());
            parserList.add(parserInfo);
        }

        response.put("status", "SUCCESS");
        response.put("total", parserList.size());
        response.put("parsers", parserList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lineage")
    public ResponseEntity<Map<String, Object>> getParserLineage() {
        List<com.cyphernex.model.ParserVersion> versions = parserRepository != null ? parserRepository.findAll() : List.of();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("total", versions.size());
        response.put("lineage", versions);
        return ResponseEntity.ok(response);
    }
}
