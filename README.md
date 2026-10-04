# CypherNex

CypherNex is a Java-based Adaptive Log Intelligence Engine designed to autonomously ingest, fingerprint, parse, normalize, correlate, and secure heterogeneous log streams.

## Architecture

The system pipeline processes mixed log telemetry through an end-to-end adaptive lifecycle:

```
Mixed Logs
  → Format DNA
  → Parser Forge
  → OCSF
  → Identity Weaver
  → ChronoSync
  → StoryGraph
  → Detections
  → LedgerGuard
  → Replay
```

## Technology Stack

- **Language & Runtime:** Java 17+
- **Build Tool:** Maven
- **Framework:** Spring Boot 3
- **Serialization / Parsing:** Jackson, Apache Commons CSV, JAXB / Woodstox
- **Grammar Synthesis:** Groq API (Llama 3.3 70B)
- **Persistence:** PostgreSQL
- **Graph & Topology:** JGraphT
- **Integrity & Verification:** `java.security.MessageDigest` (SHA-256 Chained Ledger)
- **Testing:** JUnit 5 / Spring Boot Test

## Status

This project foundation establishes the complete package layout, extensible domain interfaces, REST endpoints, data models, and verification suites. Full module implementations are engineered incrementally in subsequent stages.
