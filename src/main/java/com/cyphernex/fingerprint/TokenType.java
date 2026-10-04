package com.cyphernex.fingerprint;

public enum TokenType {
    IP("<IP>"),
    TS("<TS>"),
    NUM("<NUM>"),
    QUOTE("<QUOTE>"),
    EMAIL("<EMAIL>"),
    ID("<ID>"),
    WORD("<WORD>"),
    PUNCT("<PUNCT>");

    private final String tag;

    TokenType(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
