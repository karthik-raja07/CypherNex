package com.cyphernex.parser;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;

public interface LogParser {

    /**
     * Unique identifier / name of the parser.
     */
    String getName();

    /**
     * Version of the parser implementation or grammar.
     */
    String getVersion();

    /**
     * The log format supported by this parser (e.g., JSON, SYSLOG, CSV, APACHE_COMBINED, etc.).
     */
    String getSupportedFormat();

    /**
     * Checks whether this parser can parse the given raw log.
     *
     * @param rawLog the raw log to inspect
     * @return true if compatible, false otherwise
     */
    boolean canParse(RawLog rawLog);

    /**
     * Parses the raw log into a structured ParsedLog.
     *
     * @param rawLog the raw log to parse
     * @return structured ParsedLog
     */
    ParsedLog parse(RawLog rawLog);
}
