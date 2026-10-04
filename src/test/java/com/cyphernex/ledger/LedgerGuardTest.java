package com.cyphernex.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LedgerGuardTest {

    private LedgerGuard ledgerGuard;

    @BeforeEach
    void setUp() {
        ledgerGuard = new LedgerGuard(null, new ObjectMapper());
    }

    @Test
    @DisplayName("Test first ledger hash is correctly derived from genesis hash")
    void testFirstLedgerHash() throws Exception {
        String recordData = "{\"eventId\":\"EVT-001\",\"activity\":\"UserLogin\",\"user\":\"alice\"}";
        LedgerRecord record1 = ledgerGuard.append(recordData);

        assertNotNull(record1);
        assertEquals(1L, record1.getSequenceNumber());
        assertEquals(LedgerGuard.GENESIS_HASH, record1.getPreviousHash());

        // Calculate expected SHA-256(GENESIS_HASH + recordData)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedBytes = digest.digest((LedgerGuard.GENESIS_HASH + recordData).getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(expectedBytes);

        assertEquals(expectedHash, record1.getCurrentHash());
    }

    @Test
    @DisplayName("Test chained second hash links to first record's currentHash")
    void testChainedSecondHash() throws Exception {
        String data1 = "{\"eventId\":\"EVT-001\",\"activity\":\"UserLogin\"}";
        String data2 = "{\"eventId\":\"EVT-002\",\"activity\":\"FileAccess\"}";

        LedgerRecord record1 = ledgerGuard.append(data1);
        LedgerRecord record2 = ledgerGuard.append(data2);

        assertEquals(1L, record1.getSequenceNumber());
        assertEquals(2L, record2.getSequenceNumber());
        assertEquals(record1.getCurrentHash(), record2.getPreviousHash());

        // Calculate expected SHA-256(record1.currentHash + data2)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedBytes = digest.digest((record1.getCurrentHash() + data2).getBytes(StandardCharsets.UTF_8));
        String expectedHash = HexFormat.of().formatHex(expectedBytes);

        assertEquals(expectedHash, record2.getCurrentHash());
    }

    @Test
    @DisplayName("Test valid ledger verification passes")
    void testValidLedgerVerification() {
        ledgerGuard.append("Record 1");
        ledgerGuard.append("Record 2");
        ledgerGuard.append("Record 3");

        Map<String, Object> result = ledgerGuard.verify();
        assertTrue((Boolean) result.get("valid"));
        assertEquals("Ledger chain verified successfully", result.get("message"));
    }

    @Test
    @DisplayName("Test tampered record detection with exact broken sequence identification")
    void testTamperedRecordDetection() {
        ledgerGuard.append("Record 1");
        ledgerGuard.append("Record 2");
        ledgerGuard.append("Record 3");

        // Tamper record 2
        boolean tampered = ledgerGuard.tamper(2L, "TAMPERED DATA IN RECORD 2");
        assertTrue(tampered);

        Map<String, Object> result = ledgerGuard.verify();
        assertFalse((Boolean) result.get("valid"));
        assertEquals(2L, result.get("brokenSequence"));
        assertEquals("Ledger chain broken at record 2", result.get("message"));
    }

    @Test
    @DisplayName("Test broken previous-hash detection")
    void testBrokenPreviousHashDetection() {
        ledgerGuard.append("Record 1");
        ledgerGuard.append("Record 2");
        ledgerGuard.append("Record 3");

        // Break previous hash link on record 3
        ledgerGuard.getLedger().get(2).setPreviousHash("badhash0000000000000000000000000000000000000000000000000000000000");

        Map<String, Object> result = ledgerGuard.verify();
        assertFalse((Boolean) result.get("valid"));
        assertEquals(3L, result.get("brokenSequence"));
        assertEquals("Ledger chain broken at record 3", result.get("message"));
    }
}
