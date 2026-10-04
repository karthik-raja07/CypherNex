package com.cyphernex.ledger;

import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.storage.LedgerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LedgerGuard {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final List<LedgerRecord> ledger = new ArrayList<>();
    private final LedgerRepository ledgerRepository;
    private final ObjectMapper objectMapper;
    private String lastHash = GENESIS_HASH;

    public LedgerGuard(LedgerRepository ledgerRepository, ObjectMapper objectMapper) {
        this.ledgerRepository = ledgerRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public synchronized LedgerRecord append(String recordData) {
        return append(recordData, computeSha256(recordData != null ? recordData : ""), computeSha256(recordData != null ? recordData : ""));
    }

    public synchronized LedgerRecord append(String recordData, String rawLogHash, String normalizedEventHash) {
        long sequenceNumber = ledger.size() + 1L;
        String previousHash = this.lastHash;
        String currentHash = computeSha256(previousHash + (recordData != null ? recordData : ""));

        LedgerRecord record = new LedgerRecord(
                sequenceNumber,
                recordData,
                rawLogHash != null ? rawLogHash : computeSha256(recordData != null ? recordData : ""),
                normalizedEventHash != null ? normalizedEventHash : computeSha256(recordData != null ? recordData : ""),
                previousHash,
                currentHash,
                Instant.now()
        );

        ledger.add(record);
        this.lastHash = currentHash;

        if (ledgerRepository != null) {
            ledgerRepository.save(record);
        }

        return record;
    }

    public synchronized LedgerRecord appendEvent(NormalizedEvent event) {
        try {
            String recordData = objectMapper.writeValueAsString(event);
            String rawHash = (event != null && event.getRawLog() != null) ? computeSha256(event.getRawLog()) : computeSha256(recordData);
            String normHash = computeSha256(recordData);
            return append(recordData, rawHash, normHash);
        } catch (Exception e) {
            String fallback = event != null ? (event.getEventId() + ":" + event.getActivity() + ":" + event.getUser()) : "EMPTY_EVENT";
            return append(fallback, computeSha256(fallback), computeSha256(fallback));
        }
    }

    public synchronized Map<String, Object> verify() {
        Map<String, Object> result = new LinkedHashMap<>();
        String prev = GENESIS_HASH;

        for (int i = 0; i < ledger.size(); i++) {
            LedgerRecord record = ledger.get(i);

            // Check previous hash continuity
            if (!record.getPreviousHash().equals(prev)) {
                result.put("valid", false);
                result.put("brokenSequence", record.getSequenceNumber());
                result.put("expectedPreviousHash", prev);
                result.put("actualPreviousHash", record.getPreviousHash());
                result.put("currentHash", record.getCurrentHash());
                result.put("totalRecords", ledger.size());
                result.put("message", "Ledger chain broken at record " + record.getSequenceNumber());
                return result;
            }

            // Verify current hash calculation: SHA-256(previousHash + recordData)
            String expected = computeSha256(record.getPreviousHash() + (record.getRecordData() != null ? record.getRecordData() : ""));
            if (!record.getCurrentHash().equals(expected)) {
                result.put("valid", false);
                result.put("brokenSequence", record.getSequenceNumber());
                result.put("expectedHash", expected);
                result.put("actualHash", record.getCurrentHash());
                result.put("totalRecords", ledger.size());
                result.put("message", "Ledger chain broken at record " + record.getSequenceNumber());
                return result;
            }

            prev = record.getCurrentHash();
        }

        result.put("valid", true);
        result.put("totalRecords", ledger.size());
        result.put("lastHash", lastHash);
        result.put("genesisHash", GENESIS_HASH);
        result.put("message", "Ledger chain verified successfully");
        return result;
    }

    public synchronized boolean tamper(long sequenceNumber, String tamperedData) {
        for (LedgerRecord record : ledger) {
            if (record.getSequenceNumber() == sequenceNumber) {
                record.setRecordData(tamperedData);
                if (ledgerRepository != null) {
                    ledgerRepository.save(record);
                }
                return true;
            }
        }
        return false;
    }

    public synchronized List<LedgerRecord> getLedger() {
        return new ArrayList<>(ledger);
    }

    public synchronized void reset() {
        ledger.clear();
        lastHash = GENESIS_HASH;
    }

    public String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}

