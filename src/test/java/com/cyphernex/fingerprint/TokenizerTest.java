package com.cyphernex.fingerprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenizerTest {

    private Tokenizer tokenizer;

    @BeforeEach
    void setUp() {
        tokenizer = new Tokenizer();
    }

    @Test
    void testClassifyTokenTypes() {
        assertEquals(TokenType.IP, tokenizer.classifyValue("192.168.1.10"));
        assertEquals(TokenType.TS, tokenizer.classifyValue("2026-09-16T10:20:30Z"));
        assertEquals(TokenType.TS, tokenizer.classifyValue("10:20:30"));
        assertEquals(TokenType.NUM, tokenizer.classifyValue("4102"));
        assertEquals(TokenType.QUOTE, tokenizer.classifyValue("\"john doe\""));
        assertEquals(TokenType.EMAIL, tokenizer.classifyValue("admin@corp.com"));
        assertEquals(TokenType.ID, tokenizer.classifyValue("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals(TokenType.WORD, tokenizer.classifyValue("LOGIN"));
    }

    @Test
    void testStructuralShapeGeneration() {
        String log = "2026-09-16 10:20:30 user=john ip=192.168.1.10 action=LOGIN";
        String shape = tokenizer.generateStructuralShape(log);

        assertEquals("<TS> <TS> user=<WORD> ip=<IP> action=<WORD>", shape);
    }
}
