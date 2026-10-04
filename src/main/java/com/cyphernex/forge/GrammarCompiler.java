package com.cyphernex.forge;

import org.springframework.stereotype.Component;

@Component
public class GrammarCompiler {

    public LearnedParser compile(Grammar grammar) {
        if (grammar == null) {
            throw new IllegalArgumentException("Grammar cannot be null");
        }
        String name = "LearnedParser";
        String version = grammar.getVersion() != null ? grammar.getVersion() : "v1";
        String format = grammar.getFormatName() != null ? grammar.getFormatName() : "CUSTOM";
        return new LearnedParser(name, version, format, grammar);
    }
}
