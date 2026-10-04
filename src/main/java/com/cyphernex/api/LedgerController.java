package com.cyphernex.api;

import com.cyphernex.ledger.LedgerGuard;
import com.cyphernex.ledger.LedgerRecord;
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
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerGuard ledgerGuard;

    public LedgerController(LedgerGuard ledgerGuard) {
        this.ledgerGuard = ledgerGuard;
    }

    @GetMapping({"/verify", "/verify-integrity"})
    public ResponseEntity<Map<String, Object>> verifyLedger() {
        return ResponseEntity.ok(ledgerGuard.verify());
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLedger() {
        List<LedgerRecord> records = ledgerGuard.getLedger();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("total", records.size());
        response.put("records", records);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/demo-tamper", "/simulate-tamper"})
    public ResponseEntity<Map<String, Object>> demoTamper(@RequestBody(required = false) Map<String, Object> body) {
        long sequence = 1L;
        if (body != null && body.containsKey("sequenceNumber")) {
            try {
                sequence = Long.parseLong(String.valueOf(body.get("sequenceNumber")));
            } catch (NumberFormatException ignored) {
            }
        }

        String tamperedData = "{\"tampered\":true,\"attacker\":\"malicious_actor\",\"action\":\"OVERWRITE\"}";
        if (body != null && body.containsKey("tamperedData")) {
            tamperedData = String.valueOf(body.get("tamperedData"));
        }

        boolean success = ledgerGuard.tamper(sequence, tamperedData);
        Map<String, Object> response = new LinkedHashMap<>();
        if (success) {
            response.put("status", "TAMPERED");
            response.put("sequenceNumber", sequence);
            response.put("tamperedSequence", sequence);
            response.put("message", "Tampering simulated at block #" + sequence + ". Run Verify Ledger Integrity to detect chain break.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "ERROR");
            response.put("message", "No record found at sequence #" + sequence + " to tamper.");
            return ResponseEntity.badRequest().body(response);
        }
    }
}

