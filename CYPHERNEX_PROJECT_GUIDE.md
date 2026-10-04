# CypherNex — Complete Developer & Viva Explanation Guide

---

## 1. Project Overview

### What CypherNex Is
**CypherNex** is a Java-based, adaptive security log intelligence engine designed to solve the real-world challenge of ingesting, normalizing, correlating, and investigating heterogeneous security logs in real time.

### Main Problem It Solves
Traditional SIEM (Security Information and Event Management) platforms suffer from:
1. **Parser Fragility & Inflexibility**: New or unknown vendor formats fail silently or require days of manual regex engineering.
2. **LLM Cost & Latency Explosion**: Feeding every raw log to a Large Language Model (LLM) is too expensive, slow, and non-deterministic.
3. **Siloed Logs & Identity Fragmentation**: Security events appear across disconnected services with alias variations (`john`, `john.doe@company.com`, `AD-USER-442`).
4. **Clock Skew & Timeline Distortion**: Different servers possess unsynchronized clocks, obscuring true causal sequences.
5. **Tamper Vulnerability**: Attackers modify local log files to hide attack trails.
6. **Lack of Remediation Simulation**: Security analysts cannot easily determine which specific intervention point in an attack kill-chain would have maximized attack mitigation.

### Main Purpose
CypherNex combines:
- **Format DNA & Parser Forge**: An on-the-fly grammar synthesis pipeline using Groq LLM only once per format fingerprint, cached indefinitely for high-throughput zero-LLM parsing.
- **OCSF Normalization**: Standardizing all parsed outputs to the Open Cybersecurity Schema Framework.
- **Identity Weaver & ChronoSync**: Resolving fragmented user aliases and aligning time across skewed distributed sources.
- **StoryGraph**: Constructing a directed causal event graph and automated narrative.
- **Detections**: Parser drift, silent logging dropouts, and prompt/log injection detection.
- **LedgerGuard**: Tamper-evident cryptographic SHA-256 hash chaining.
- **What-If Counterfactual Replay**: Fixed-point causal propagation simulation that evaluates kill-chain block points.

### Architecture Pipeline Diagram
```text
+---------------------------------------------------------------------------------------------------+
|                                       CYPHERNEX PIPELINE                                          |
+---------------------------------------------------------------------------------------------------+

                                          [ RAW LOG ]
                                               |
                                               v
                                    +----------------------+
                                    |   Format DNA Engine  | ---> Computes SHA-256 Fingerprint
                                    |     & Tokenizer      |      from Structural Token Shape
                                    +----------------------+
                                               |
                                               v
                                    +----------------------+
                     +------------> |   Parser Registry    | <------------+
                     |              +----------------------+              |
                     |                         |                          |
               [Cache Hit]               [Cache Miss]                     |
                     |                         |                          |
                     v                         v                          |
            +----------------+       +-------------------+                |
            | Built-in Parser|       |   Parser Forge    |                |
            | (JSON/CSV/XML/ |       | (Calls Groq API / |                |
            |  Syslog/Text)  |       |  MOCK mode once)  |                |
            +----------------+       +-------------------+                |
                     |                         |                          |
                     |                [Grammar Validation]                |
                     |                         |                          |
                     |                         v                          |
                     |               +-------------------+                |
                     |               |  GrammarCompiler  | --- [Register & Cache]
                     |               | -> LearnedParser  |
                     |               +-------------------+
                     |                         |
                     +------------+------------+
                                  |
                                  v
                         +-----------------+
                         |   ParsedLog     | (Field-Value Map)
                         +-----------------+
                                  |
                                  v
                         +-----------------+
                         | OCSF Normalizer | (Maps to standard OCSF Schema)
                         +-----------------+
                                  |
                     +------------+------------+
                     |                         |
                     v                         v
           +--------------------+    +--------------------+
           |  Detection Engine  |    |    LedgerGuard     |
           | - Drift Detection  |    | - SHA-256 Chaining |
           | - SilenceWatch     |    | - Tamper Evident   |
           | - Log Injection    |    +--------------------+
           +--------------------+              |
                     |                         v
                     v               +--------------------+
             [Detections List]       |  EventRepository   |
                                     |  & DatabaseManager |
                                     +--------------------+
                                               |
                                               v
                                    +----------------------+
                                    |   Identity Weaver    | (Clustering Aliases)
                                    +----------------------+
                                               |
                                               v
                                    +----------------------+
                                    |      ChronoSync      | (Clock Skew Alignment)
                                    +----------------------+
                                               |
                                               v
                                    +----------------------+
                                    |      StoryGraph      | (Causal Graph & Narrative)
                                    +----------------------+
                                               |
                                               v
                                    +----------------------+
                                    | CounterfactualReplay | (What-If Simulation)
                                    +----------------------+
                                               |
                                               v
                                    +----------------------+
                                    | Spring Boot REST API |
                                    | & Vanilla Dashboard  |
                                    +----------------------+
```

### Module Distribution & Roles
- **Java 17**: Core language for strongly typed, deterministic parsing, graph algorithms, and cryptographic chaining.
- **Spring Boot 3.2.5**: Standalone web server hosting REST APIs and serving the responsive dashboard UI.
- **Frontend**: Lightweight, single-file HTML5/CSS3/Vanilla JS single-page dashboard rendered directly from `DashboardController`.
- **LLM (Groq / Llama-3.3-70b / GPT-OSS-120b)**: Used strictly in `GroqClient` for one-time declarative grammar generation when an unknown format fingerprint is first encountered.
- **Storage**: Dual-mode resilience â€” PostgreSQL when available; in-memory thread-safe repositories (`ConcurrentHashMap`, `CopyOnWriteArrayList`) when standalone.

---

## 2. Technology Stack

| Technology | Version | Why It Is Used | Where It Is Used |
|---|---|---|---|
| **Java** | 17 LTS | High-performance, strongly typed object-oriented execution, modern records, pattern matching, text blocks, and strict memory safety. | Entire codebase (`src/main/java`). |
| **Spring Boot** | 3.2.5 | Application bootstrap, dependency injection (`@Component`, `@Service`, `@RestController`), embedded Tomcat server, configuration binding (`@Value`). | `CypherNexApplication`, `api/`, `config/`. |
| **Spring Web MVC** | 3.2.5 | Exposes RESTful endpoints, JSON request/response serialisation, and delivers the dynamic UI. | `com.cyphernex.api.*` controllers. |
| **Jackson Databind & JSR310** | 2.15+ | High-speed JSON serialization/deserialization, ISO-8601 timestamp conversion, and dynamic JSON schema mapping. | `JsonLogParser`, `ParserForge`, `LedgerGuard`, `LogController`. |
| **Apache Commons CSV** | 1.10.0 | High-performance CSV delimiter and header parsing for multi-line and single-line CSV telemetry. | `CsvLogParser.java`. |
| **Woodstox & JAXB** | 6.6.2 | StAX XML stream processing for parsing complex XML formatted logs without entity expansion vulnerabilities. | `XmlLogParser.java`. |
| **JGraphT** | 1.5.2 | Industry-standard graph data structures (`DefaultDirectedGraph`, `DefaultEdge`) for tracking node causality. | `StoryGraph.java`. |
| **PostgreSQL Driver** | 42.6+ | JDBC driver for persisting normalized events, parser grammars, identity clusters, and tamper-evident ledger records. | `DatabaseManager.java`. |
| **Java `java.net.http.HttpClient`** | Java 17 Built-in | Native asynchronous HTTP client for calling Groq LLM API without third-party HTTP client overhead. | `GroqClient.java`. |
| **Java `java.security.MessageDigest`** | Java 17 Built-in | Cryptographic SHA-256 computation for Format DNA fingerprinting and LedgerGuard block chaining. | `FormatDnaEngine.java`, `LedgerGuard.java`. |
| **JUnit 5 & Spring Boot Test** | 5.10+ | Comprehensive automated unit and integration test suite (86 passing automated tests). | `src/test/java/**`. |

---

## 3. Complete Dashboard Guide

The CypherNex Dashboard is served at `http://localhost:8080/` via [`DashboardController.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/api/DashboardController.java).

### UI Components Inventory

#### 1. Header & Live Indicator
- **What the user sees**: "CypherNex â€” Adaptive Log Intelligence Engine" with a pulsing green dot "LIVE ENGINE".
- **Meaning**: Confirms the application is active and running.

#### 2. Top Metric Bar (8 Metric Cards)
- **Lines Processed**: Total raw log lines received.
- **LLM Calls**: Number of external/mock LLM calls made by Parser Forge.
- **LLM Mode**: Indicates whether active mode is `GROQ` or `MOCK`.
- **Parsers Learned**: Count of dynamically generated parsers in the registry.
- **Cache Hits**: Number of incoming logs matched immediately to existing parsers without LLM invocation.
- **Cache Misses**: Number of incoming logs requiring lookup/learning.
- **Events Ingested**: Total count of normalized OCSF events stored.
- **Detections**: Total count of security and anomaly detections triggered.

#### 3. Log Ingestion Panel
- **Textarea (`#log-input`)**: Multi-line input for raw log content.
- **Source Input (`#log-source`)**: Text field specifying the log source (e.g., `auth-service`, `firewall-01`).
- **Preset Buttons (`JSON`, `CSV`, `XML`, `Syslog`, `Plain Text`, `Vendor Custom`)**:
  - *Action*: Populates the textarea and source field with realistic preset log strings.
- **"Ingest Log" Button**:
  - *Action*: Sends `POST /api/logs/ingest` with `rawContent` and `source`.
  - *Screen Change*: Shows ingestion JSON result, increments metrics, appends to event log and ledger, and refreshes StoryGraph and replay timeline.

#### 4. Demo Dataset Panel
- **"Load Demo Dataset" Button**:
  - *Action*: Calls `POST /api/demo/load`. Ingests 6 distinct formats across different sources.
  - *Screen Change*: Updates all metrics, populates the "Raw Logs" table, links events into StoryGraph, and refreshes the Replay timeline.
- **"Reset Demo" Button**:
  - *Action*: Calls `POST /api/demo/reset`. Clears repositories, StoryGraph, detections, Ledger, and resets counters.
- **"DEMO DATASET â€” RAW LOGS" Table**:
  - *What it displays*: Table with columns for `Source`, `Raw Content`, `Event Type`, and `Timestamp`.

#### 5. What-If Counterfactual Replay Panel
- **Metrics Bar (Exactly 2 Metrics)**:
  - `Events prevented: X of Y`
  - `Impact stopped: NN%`
- **"Suggest best block point" Button**:
  - *Action*: Calls `GET /api/replay/default/best-block`.
  - *Screen Change*: Identifies the event providing maximum kill-chain mitigation and highlights it with a dashed yellow outline and a `â­ RECOMMENDED` badge (does *not* auto-block).
- **"Reset timeline" Button**:
  - *Action*: Clears the active block, removes recommendation markers, and restores normal styling.
- **Event Timeline Table**:
  - *Columns*: `Timestamp`, `Activity / Description`, `Source`, `Severity`, `Replay Status`.
  - *Row States*:
    - **Blocked Event**: Red background, red border, badge `blocked here`.
    - **Prevented Events**: Dark grey background, strikethrough text, badge `never happens`.
    - **Still Happens Events**: Normal background, badge `still happens`.
  - *Row Hover Tooltip*: Displays causal lineage evidence (e.g. `Causal Evidence: Shared Identity (john) â€¢ Shared Host (HOST-01)`).
  - *Row Click Interaction*: Toggles block on/off via `POST /api/replay/default/block`.

#### 6. Real-Time Detection Alerts Panel
- **"Trigger Drift Demo" Button**: Calls `POST /api/detections/drift-demo`. Ingests a mutated log schema and shows a `PARSER_DRIFT` warning.
- **"Simulate Silence" Button**: Calls `POST /api/detections/silence-demo`. Simulates telemetry loss from a source and raises a `SILENCE_WATCH` alert.
- **"Test Log Injection" Button**: Calls `POST /api/detections/injection-demo`. Tests multi-line CRLF payload injection and flags `LOG_INJECTION_ATTEMPT`.
- **Detections Table**: Displays active detections with severity tags, source, rule name, and timestamp.

#### 7. StoryGraph Panel
- **Header Badge (`#sg-counts`)**: Displays total graph node count and edge count.
- **Narrative Box (`#sg-narrative`)**: Auto-generated plain-English attack narrative synthesizing the chronological incident progression.
- **Correlated Graph Edges Table**: Shows directed links (`Source Node -> Target Node`, `Relationship Type`).

#### 8. LedgerGuard Panel
- **"Verify Chain" Button**: Calls `GET /api/ledger/verify`. Validates all SHA-256 hashes from genesis. Shows a green `VALID` badge or red `BROKEN` warning.
- **"Demo Tamper Record #1" Button**: Calls `POST /api/ledger/tamper` with `sequenceNumber: 1`. Mutates the first record's data in-memory.
- **"Reset Ledger" Button**: Calls `POST /api/ledger/reset`.
- **Ledger Records Table**: Displays sequence numbers, SHA-256 current hash prefix, previous hash prefix, and payload.

#### 9. Dynamic Parsers Lineage Panel
- **Active Parsers Table**: Lists all active parsers (e.g. `SyslogParser`, `LearnedParser`), version (`v1`, `v2`), extraction rates, and fingerprint hashes.

---

## 4. Dashboard Metrics

| Metric Label | UI Element ID | Source Method & Class | API Endpoint | Calculation Logic | What Causes It to Change |
|---|---|---|---|---|---|
| **Lines Processed** | `m-lines` | `MetricsService.getLinesProcessed()` | `GET /api/metrics` | Counter integer incremented by `incrementLinesProcessed()`. | Increments by 1 on every `POST /api/logs/ingest` call or by 6 during Demo Load. |
| **LLM Calls** | `m-llm` | `MetricsService.getLlmCalls()` | `GET /api/metrics` | Counter integer incremented by `incrementLlmCalls()`. | Increments whenever `GroqClient.generateGrammar()` is executed. |
| **LLM Mode** | `m-llm-mode` | `GroqClient.getLlmMode()` | `GET /api/logs/ingest` (response field) | Returns `"GROQ"` if `groq.api.key` is non-empty, otherwise `"MOCK"`. | Configured in `application.properties`. |
| **Parsers Learned** | `m-parsers` | `MetricsService.getParsersLearned()` | `GET /api/metrics` | Counter integer incremented when `ParserForge` validates a new grammar. | Increments whenever an unknown log fingerprint successfully generates a `LearnedParser`. |
| **Cache Hits** | `m-cache-hits` | `MetricsService.getCacheHits()` | `GET /api/metrics` | Counter integer incremented by `incrementCacheHits()`. | Increments when an incoming log matches a known cached fingerprint in `ParserRegistry`. |
| **Cache Misses** | `m-cache-misses` | `MetricsService.getCacheMisses()` | `GET /api/metrics` | Counter integer incremented by `incrementCacheMisses()`. | Increments when an incoming log does not match any cached fingerprint. |
| **Events** | `m-events` | `EventRepository.count()` | `GET /api/logs/normalized` | Size of normalized events repository list. | Increases on every successful log ingestion. |
| **Detections** | `m-detections` | `DetectionEngine.getDetections().size()` | `GET /api/detections` | Size of active detections list. | Increases when drift, silence, or injection triggers fire. |
| **Events Prevented** | `metric-events-prevented` | `ReplayResult.getEventsPrevented()` / `getTotalEvents()` | `POST /api/replay/{chainId}/block` | `preventedEventIds.size()` out of `allEvents.size()`. | Updates immediately when an event row is clicked in the Replay timeline. |
| **Impact Stopped** | `metric-impact-stopped` | `ReplayResult.getImpactStoppedPercent()` | `POST /api/replay/{chainId}/block` | `Math.round((sum(weights of prevented) / sum(weights of all)) * 100)`. | Updates immediately on block selection based on severity weights (LOW=1, MED=2, HIGH=3, CRIT=5). |

---

## 5. Log Ingestion Pipeline

### Detailed Step-by-Step Flow

```text
  [ Raw Log String ]
         |
         v
  1. Tokenizer.tokenize(rawContent)
         | Extracts tokens: TIMESTAMP, IP, DELIMITER, KEY_VALUE, WORD, NUMBER, etc.
         v
  2. FormatDnaEngine.generateFingerprint(rawLog)
         | Generates structural shape string and computes SHA-256 fingerprint hash
         v
  3. ParserRegistry.getParserByFingerprint(fingerprint)
         +---> [MATCH FOUND] ---> Cache Hit! Use cached parser
         |
         +---> [NO MATCH] ---> Check built-in parsers (JsonLogParser, XmlLogParser, SyslogParser, CsvLogParser, PlainTextParser)
                                     |
                                     +---> [MATCH FOUND] ---> Cache & use built-in parser
                                     |
                                     +---> [NO MATCH] ---> Cache Miss! Invoke ParserForge
                                                                   |
                                                                   v
                                                        4. ParserForge.forgeAndValidate()
                                                                   |
                                                                   v
                                                        5. GroqClient.generateGrammar()
                                                           (Sends prompt to Groq API / MOCK)
                                                                   |
                                                                   v
                                                        6. Parse & clean JSON response to Grammar DTO
                                                                   |
                                                                   v
                                                        7. GrammarCompiler.compile(grammar) -> LearnedParser
                                                                   |
                                                                   v
                                                        8. Validate against sample log:
                                                           extractionRate >= 0.80, validationRate >= 0.80, nullRate <= 0.20
                                                                   |
                                                                   v
                                                        9. Cache LearnedParser in ParserRegistry & persist to DB
                                                                   |
                                                                   v
  10. LogParser.parse(rawLog) -> ParsedLog (Map<String, Object> fields)
         |
         v
  11. OcsfNormalizer.normalize(parsedLog) -> NormalizedEvent (OCSF Schema)
         |
         v
  12. LedgerGuard.appendEvent(normalizedEvent) -> LedgerRecord (SHA-256 Chained)
         |
         v
  13. DetectionEngine.inspect(rawLog, parsedLog, normalizedEvent, fingerprint)
         |
         v
  14. EventRepository.save(normalizedEvent)
```

### Classes Involved
1. [`LogController.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/api/LogController.java): Entrypoint handling `POST /api/logs/ingest`.
2. [`Tokenizer.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/fingerprint/Tokenizer.java): Scans characters into structured token streams.
3. [`FormatDnaEngine.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/fingerprint/FormatDnaEngine.java): Builds Format DNA shape and SHA-256 fingerprint.
4. [`ParserRegistry.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/parser/ParserRegistry.java): Thread-safe repository of built-in and dynamically learned parsers.
5. [`ParserForge.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/forge/ParserForge.java): Orchestrates LLM grammar generation and validation.
6. [`GroqClient.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/forge/GroqClient.java): Communicates with Groq Cloud API.
7. [`GrammarCompiler.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/forge/GrammarCompiler.java) & [`LearnedParser.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/forge/LearnedParser.java): Instantiates executable regex/delimited log parsers.
8. [`OcsfNormalizer.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/normalization/OcsfNormalizer.java) & [`SchemaMapper.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/normalization/SchemaMapper.java): Transforms fields into standard OCSF format.
9. [`LedgerGuard.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/ledger/LedgerGuard.java): Computes cryptographic audit chain.
10. [`DetectionEngine.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/detection/DetectionEngine.java): Evaluates detection rules.
11. [`EventRepository.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/storage/EventRepository.java): Persists normalized events.

---

## 6. Format DNA

### Concept & Design
Format DNA is an innovative structural fingerprinting technique that abstracts away dynamic values (e.g. specific IP addresses, timestamps, usernames) while preserving the syntactic skeleton of the log.

### How a Fingerprint is Generated
1. **Tokenization** via [`Tokenizer.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/fingerprint/Tokenizer.java):
   - Categorizes substrings into: `JSON_START`, `XML_TAG`, `PRI_HEADER`, `TIMESTAMP_ISO`, `IPV4`, `DELIMITER`, `KEY_VALUE`, `WORD`, `NUMBER`, etc.
2. **Shape String Assembly**:
   - Replaces literal text with token identifiers.
   - Example for `VENDOR_EVT|2026-09-16T10:05:00Z|USR-999|172.16.0.4|DATABASE_QUERY|SUCCESS`:
     - Shape: `WORD|TIMESTAMP_ISO|WORD|IPV4|WORD|WORD`
3. **SHA-256 Hashing** via [`FormatDnaEngine.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/fingerprint/FormatDnaEngine.java):
   - Computes: `SHA-256(shape)` -> e.g. `3b9797f7fe2637df4d50ed44543be917af46b1aea5f83fd68e8b59a5b155f153`.

### Why This Drastically Reduces LLM Calls
In a production SIEM receiving 100,000 logs/sec, millions of logs share the exact same structural shape. CypherNex calls the LLM **only once per distinct fingerprint**, compiles the resulting grammar, and caches the compiled `LearnedParser`. All subsequent logs with the same fingerprint are parsed in memory at native Java speeds (~sub-millisecond) with **0 external LLM API calls**.

---

## 7. Parser Forge & Groq LLM Integration

### When the LLM is Called
The LLM is invoked **only on a cache miss** where:
1. The fingerprint does not exist in `ParserRegistry`.
2. No built-in parser (`JsonLogParser`, `XmlLogParser`, `SyslogParser`, `CsvLogParser`, `PlainTextParser`) can parse the log with sufficient confidence.

### What is Sent to the LLM
`GroqClient.java` sends a structured system prompt and a user prompt containing:
- Format Fingerprint
- Raw sample log line
- Exact declarative JSON schema specification:
  ```json
  {
    "formatName": "string",
    "version": "v1",
    "extractionType": "DELIMITED",
    "delimiter": "|",
    "regexPattern": "",
    "fields": [
      {
        "name": "action",
        "type": "STRING",
        "extractionRule": "delim:5",
        "required": true,
        "targetOcsfField": "activity_name"
      }
    ]
  }
  ```

### Response Cleaning & Validation
- **Markdown Stripping**: Removes enclosing ```json ... ``` fences.
- **JSON Validation**: Parses response through Jackson `ObjectMapper`.
- **Field Completeness**: Verifies `formatName`, `extractionType`, `fields` list.
- **Safety**: Rejects any code generation attempt (ensuring 100% declarative syntax).

### In-Memory Validation & Quality Gate
Before promoting a learned parser to active status, `ParserForge.java` executes a three-part quality check against the sample log:
1. `extractionRate = extractedFields / totalExpectedFields >= 0.80` (at least 80% of fields extracted).
2. `validationRate = validFields / extractedFields >= 0.80` (at least 80% valid).
3. `nullRate = missingFields / totalExpectedFields <= 0.20` (no more than 20% null).

If the grammar passes, it is compiled into a [`LearnedParser`](file:///d:/CypherNex/src/main/java/com/cyphernex/forge/LearnedParser.java), cached in `ParserRegistry`, and saved to `ParserRepository`.

### MOCK vs. Real LLM Mode
- If `groq.api.key` is defined in `application.properties`, `GroqClient` executes live HTTPS POST calls to `https://api.groq.com/openai/v1/chat/completions`.
- If `groq.api.key` is blank, `GroqClient` falls back to `generateMockGrammar()`, generating deterministic regex/delimited grammars for offline testing.

---

## 8. Normalization & OCSF

### What Normalization Means
Heterogeneous log sources format the same concept differently:
- A user may appear as `user`, `username`, `user.name`, `account`, `usr`, `column_2`.
- An action may appear as `action`, `event`, `activity_name`, `action_type`.
- A source IP may appear as `ip`, `src_ip`, `client_ip`, `srcip`.

Normalization transforms these varied structures into uniform, strongly typed [`NormalizedEvent`](file:///d:/CypherNex/src/main/java/com/cyphernex/model/NormalizedEvent.java) objects adhering to the **OCSF (Open Cybersecurity Schema Framework)** standard.

### Core Normalized Fields
- `eventId`: UUID
- `timestamp`: ISO-8601 UTC `Instant`
- `eventClass`: `Authentication`, `Network Activity`, `System Activity`, `Security Event`
- `activity`: `LOGIN`, `PROCESS_STARTED`, `FILE_ACCESS`, `NETWORK_CONNECTION`, `DATABASE_QUERY`
- `severity`: `low`, `medium`, `high`, `critical`
- `source`: Reporting system/host
- `user`: Normalized username/identity
- `sourceIp` / `destinationIp`: IP strings
- `host`: Machine hostname
- `sessionId`: Session tracking string
- `requestId`: Correlation/trace ID
- `process`: Binary/executable name
- `provenance`: Parser name, parser version, fingerprint hash, validators passed

---

## 9. Identity Weaver

### Problem Solved
Attackers frequently pivot across accounts, protocols, and network identities during a multi-stage attack. An analyst observing `john`, `john.doe@company.com`, and `AD-USER-442` across different systems might not realize they represent the same human actor.

### Implementation ([`IdentityWeaver.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/identity/IdentityWeaver.java))
`IdentityWeaver` builds a graph of identity aliases and clusters them into canonical identities using multi-factor evidence scoring:
1. **Shared Session or Request ID (+0.50)**: Two identifiers appearing with the same `sessionId` or `requestId`.
2. **String Similarity (+0.25)**: Prefix matching, email prefix extraction (`john.doe` from `john.doe@company.com`), or edit-distance heuristics.
3. **Temporal Proximity (+0.25)**: Actions occurring within 5 minutes across related endpoints.

### Clustering & Canonical Resolution
When pair confidence reaches >= 0.50, an edge is added between aliases. Connected components are resolved using Breadth-First Search (BFS), and the cleanest identifier (e.g. `john`) is chosen as the canonical identity. All subsequent correlation and StoryGraph stages map these aliases to the single canonical identity.

---

## 10. ChronoSync

### Problem Solved
Distributed servers, firewalls, and containers often suffer from clock skew (ranging from hundreds of milliseconds to several seconds). Uncorrected timestamps scramble the apparent sequence of events, causing cause to appear after effect.

### Implementation ([`ChronoSync.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/correlation/ChronoSync.java))
1. Groups events that share correlation anchors (`requestId` or `sessionId`).
2. Identifies the baseline clock from the earliest reporting source.
3. Computes the offset Delta t = -(t_other - t_base) for each secondary source.
4. Generates aligned events where t_aligned = t_original + offset.
5. Preserves all causal parent relationships (`parentEventIds`, `parentEvidence`) during alignment.

---

## 11. StoryGraph & Entity Graph

### Deep Architecture & Concepts
[`StoryGraph.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/correlation/StoryGraph.java) constructs a directed graph representing the entities and causal progression of an attack.

- **Nodes ([`GraphNode.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/model/GraphNode.java))**:
  - `EVENT`: Individual security actions (e.g. `event:UUID`).
  - `USER`: Canonical users (e.g. `user:john`).
  - `HOST`: Machines (e.g. `host:HOST-01`).
  - `IP`: IP endpoints (e.g. `ip:10.10.2.15`).
  - `SESSION`: Active sessions (e.g. `session:SES-99`).
  - `PROCESS`: Executed binaries (e.g. `process:powershell.exe`).
- **Edges ([`GraphEdge.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/model/GraphEdge.java))**:
  - `USER_PERFORMED`: Connects user node to event node.
  - `EVENT_ON_HOST`: Connects event node to host node.
  - `EVENT_FROM_IP` / `EVENT_TO_IP`: Connects event to IP nodes.
  - `EVENT_IN_SESSION`: Connects event to session node.
  - `EVENT_SPAWNED_PROCESS`: Connects event to process node.
  - `EVENT_TRIGGERED`: **Causal attack chain edge** linking sequential events.

### Attack Chain Synthesis
[`EventCorrelator.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/correlation/EventCorrelator.java) chains sequential events that share canonical user identity, session ID, or request ID. For each link:
- `next.addParentEventId(prev.getEventId())`
- `next.addParentEvidence(prev.getEventId(), determineEvidence(prev, next))`

### Automated Narrative Generation
`StoryGraph.generateNarrative()` iterates through the correlated timeline and automatically synthesizes a plain-English incident summary detailing initial access, lateral movement, host targeting, and final actions.

---

## 12. Detections Engine

Implemented in [`DetectionEngine.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/detection/DetectionEngine.java):

### A. Parser Drift Detection ([`DriftDetector.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/detection/DriftDetector.java))
- **Problem**: Upstream application updates change log schemas (adding/removing fields), causing existing parsers to fail or lose fields.
- **Logic**: Stores baseline format fingerprints per source. If a new log from an existing source generates a different fingerprint or experiences a sudden drop in extraction rate, a `PARSER_DRIFT` detection is raised.

### B. SilenceWatch ([`SilenceWatch.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/detection/SilenceWatch.java))
- **Problem**: Attackers disable logging daemons or network outages cause silent telemetry loss.
- **Logic**: Tracks `lastSeen` timestamps per log source. If elapsed time exceeds `silenceThresholdSeconds` (or when simulated via `simulateSilence()`), a `SILENCE_WATCH` alert with severity `HIGH` is emitted.

### C. Log Injection Defence ([`LogInjectionDetector.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/detection/LogInjectionDetector.java))
- **Problem**: Attackers inject CRLF (`\r\n`), forged timestamps, fake syslog PRI headers, or ANSI escape codes into log fields to fake audit trails or manipulate downstream SIEM parsers.
- **Logic**: Inspects raw content and parsed fields for CRLF characters, embedded ISO-8601 timestamps inside user strings, fake syslog prefixes (`<134>1`), or escape sequences. Flags `LOG_INJECTION_ATTEMPT`.

---

## 13. Drift Detection & Shadow Testing

### Current Implementation Flow
1. Baseline schema fingerprint F1 is recorded for a source.
2. A mutated schema arrives with fingerprint F2.
3. `DriftDetector.inspect()` identifies the divergence and creates a `PARSER_DRIFT` detection.
4. `ParserForge` compiles a candidate v2 grammar in shadow mode.
5. Ingestion metrics compare extraction rates between v1 and v2.

*Note*: Dynamic runtime promotion of v2 over v1 is managed via `ParserRegistry.cacheParserByFingerprint()`, which hot-swaps the active parser for that specific format fingerprint.

---

## 14. LedgerGuard â€” Tamper-Evident Audit Chain

### What LedgerGuard Is
[`LedgerGuard.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/ledger/LedgerGuard.java) implements a high-throughput, in-memory and database-backed cryptographic blockchain for log audit immutability.

### Cryptographic Chaining
Each event is serialized to JSON and appended as a [`LedgerRecord`](file:///d:/CypherNex/src/main/java/com/cyphernex/ledger/LedgerRecord.java):
- Record 1: Prev = 0000...0000, Curr = SHA-256(Prev + Data_1)
- Record 2: Prev = Curr_1, Curr = SHA-256(Curr_1 + Data_2)
- Record N: Prev = Curr_N-1, Curr = SHA-256(Curr_N-1 + Data_N)

### Verification & Tamper Detection
- `verify()` iterates linearly from record 1 to N. If any record's `previousHash` does not match the previous record's `currentHash`, or if recomputing `SHA-256(previousHash + recordData)` does not match `currentHash`, verification immediately halts and reports the exact broken sequence number.
- `tamper(sequenceNumber, tamperedData)` intentionally modifies the payload of a record without re-hashing the chain to demonstrate instantaneous tamper detection in the dashboard.

---

## 15. Counterfactual What-If Replay

### How It Works
What-If Replay ([`CounterfactualReplay.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/correlation/CounterfactualReplay.java)) is a deterministic forward propagation simulation on the causal event graph.

### Fixed-Point Forward Propagation Algorithm
Given a blocked event ID B and all events E:
1. Initialize Prevented = { B }.
2. Repeatedly scan all events e in E:
   - If e is not in Prevented and any parent ID in e.parentEventIds is in Prevented:
     - Add e to Prevented.
3. Repeat step 2 until no new events can be added (fixed-point reached).
4. StillHappens = E \ Prevented.

### Severity Weights & Impact Calculation
- `LOW` = 1
- `MEDIUM` = 2
- `HIGH` = 3
- `CRITICAL` = 5

ImpactStoppedPercent = round( (Sum of weights of Prevented events / Sum of weights of All events) * 100 )

### Best Block Point Algorithm
- `findBestBlockPoint(allEvents)` evaluates `computePrevented(candidate.id, allEvents)` for **every single event** in the dataset.
- Selects the candidate with the highest `impactStoppedPercent`.
- Tie-breaker: If two events yield identical maximum impact, the **earliest event by timestamp** is selected.

---

## 16. Complete REST API Reference Table

| HTTP Method | Endpoint | Purpose | Request Body | Response Structure | Controller | Used in Dashboard UI? |
|---|---|---|---|---|---|---|
| `GET` | `/` or `/dashboard` | Serves main single-page UI | None | HTML String | `DashboardController` | Yes (Main Entry) |
| `POST` | `/api/logs/ingest` | Ingests a single raw log | `{"rawContent": "...", "source": "..."}` | `status`, `fingerprint`, `shape`, `parser`, `fields`, `normalizedEvent` | `LogController` | Yes |
| `GET` | `/api/logs/normalized` | Returns all normalized OCSF events | None | `{"status": "SUCCESS", "total": N, "events": [...]}` | `LogController` | Yes |
| `GET` | `/api/logs` | Alias for normalized events | None | `{"status": "SUCCESS", "total": N, "logs": [...]}` | `LogController` | No (Backend API) |
| `POST` | `/api/demo/load` | Ingests the 6-log multi-format demo dataset | None | `{"status": "SUCCESS", "totalLogsIngested": 6, "demoLogs": [...]}` | `DemoController` | Yes |
| `GET` | `/api/demo/load` | GET alias for demo dataset loading | None | Same as POST | `DemoController` | Yes |
| `GET` | `/api/demo/logs` | Returns active demo raw logs | None | `{"status": "SUCCESS", "total": N, "demoLogs": [...]}` | `DemoController` | Yes |
| `POST` | `/api/demo/reset` | Resets demo dataset and all repositories | None | `{"status": "SUCCESS", "message": "..."}` | `DemoController` | Yes |
| `GET` | `/api/demo/reset` | GET alias for demo reset | None | Same as POST | `DemoController` | Yes |
| `GET` | `/api/metrics` | Returns engine runtime metrics | None | `linesProcessed`, `llmCalls`, `parsersLearned`, `cacheHits`, `cacheMisses` | `MetricsController` | Yes |
| `POST` | `/api/metrics/reset` | Resets metrics counters to zero | None | `{"status": "SUCCESS"}` | `MetricsController` | No (Backend API) |
| `GET` | `/api/detections` | Returns all security/drift detections | None | `{"status": "SUCCESS", "total": N, "detections": [...]}` | `DetectionController` | Yes |
| `POST` | `/api/detections/drift-demo` | Triggers a simulated schema drift | Optional source & log map | `{"status": "DRIFT_DETECTED", "detection": {...}}` | `DetectionController` | Yes |
| `POST` | `/api/detections/silence-demo` | Triggers a simulated SilenceWatch alert | `{"source": "firewall-01"}` | `{"status": "SILENCE_DETECTED", "detection": {...}}` | `DetectionController` | Yes |
| `POST` | `/api/detections/injection-demo` | Triggers a log injection test | `{"log": "..."}` | `{"status": "INJECTION_DETECTED", "detections": [...]}` | `DetectionController` | Yes |
| `GET` | `/api/graph` | Returns StoryGraph nodes, edges, and narrative | None | `totalNodes`, `totalEdges`, `narrative`, `nodes`, `edges` | `GraphController` | Yes |
| `GET` | `/api/graph/nodes` | Returns list of graph nodes | None | List of `GraphNode` | `GraphController` | No (Backend API) |
| `GET` | `/api/graph/edges` | Returns list of graph edges | None | List of `GraphEdge` | `GraphController` | No (Backend API) |
| `GET` | `/api/graph/narrative` | Returns generated attack narrative | None | `{"narrative": "..."}` | `GraphController` | No (Backend API) |
| `POST` | `/api/graph/build` | Rebuilds graph from supplied events | List of `NormalizedEvent` | Built `StoryGraph` summary | `GraphController` | No (Backend API) |
| `GET` | `/api/ledger` | Returns all chained ledger records | None | `{"status": "SUCCESS", "total": N, "records": [...]}` | `LedgerController` | Yes |
| `GET` | `/api/ledger/verify` | Verifies cryptographic SHA-256 chain | None | `{"valid": true/false, "brokenSequence": N, "message": "..."}` | `LedgerController` | Yes |
| `POST` | `/api/ledger/tamper` | Modifies payload of a record to demo tamper detection | `{"sequenceNumber": 1, "tamperedData": "..."}` | `{"status": "TAMPERED", "tamperedRecord": 1}` | `LedgerController` | Yes |
| `POST` | `/api/ledger/reset` | Clears all ledger records | None | `{"status": "SUCCESS"}` | `LedgerController` | Yes |
| `GET` | `/api/replay` or `/api/replay/{chainId}` | Returns chronological timeline with causal parents & evidence | None | `{"status": "SUCCESS", "totalEvents": N, "events": [...]}` | `ReplayController` | Yes |
| `POST` | `/api/replay/block` or `/api/replay/{chainId}/block` | Computes What-If replay for a blocked event | `{"eventId": "<id>"}` | Full `ReplayResult` DTO | `ReplayController` | Yes |
| `GET` | `/api/replay/best-block` or `/api/replay/{chainId}/best-block` | Finds recommended block point | None | `{"status": "SUCCESS", "recommendedEvent": {...}, "impactStoppedPercent": N, "replayResult": {...}}` | `ReplayController` | Yes |
| `GET` | `/api/parsers` | Returns all registered parsers | None | List of parser names | `ParserController` | No (Backend API) |
| `GET` | `/api/parsers/lineage` | Returns parser version history and stats | None | `{"status": "SUCCESS", "total": N, "lineage": [...]}` | `ParserController` | Yes |
| `GET` | `/api/identity/records` | Returns resolved identity records | None | `{"status": "SUCCESS", "records": [...]}` | `IdentityController` | No (Backend API) |
| `GET` | `/api/chronosync/offsets` | Returns calculated clock offsets per source | None | `{"status": "SUCCESS", "offsets": [...]}` | `ChronoSyncController` | No (Backend API) |

---

## 17. Java Project Structure & Package Hierarchy

```text
d:\CypherNex\src\main\java\com\cyphernex\
â”œâ”€â”€ CypherNexApplication.java            # Spring Boot application entrypoint
â”œâ”€â”€ api\                                # REST Controllers and Web Handlers
â”‚   â”œâ”€â”€ ChronoSyncController.java       # Exposes clock skew offsets
â”‚   â”œâ”€â”€ DashboardController.java        # Single-page dynamic HTML/CSS/JS dashboard
â”‚   â”œâ”€â”€ DemoController.java             # Demo dataset loader and reset handler
â”‚   â”œâ”€â”€ DetectionController.java        # Drift, SilenceWatch, and Injection endpoints
â”‚   â”œâ”€â”€ GlobalExceptionHandler.java     # Centralized HTTP error response handler
â”‚   â”œâ”€â”€ GraphController.java            # StoryGraph endpoints
â”‚   â”œâ”€â”€ IdentityController.java         # Identity Weaver cluster endpoints
â”‚   â”œâ”€â”€ LedgerController.java           # LedgerGuard verification and tamper demo endpoints
â”‚   â”œâ”€â”€ LogController.java              # Main log ingestion and normalized events API
â”‚   â”œâ”€â”€ MetricsController.java          # Performance and caching metrics API
â”‚   â”œâ”€â”€ ParserController.java           # Parser registry and lineage API
â”‚   â””â”€â”€ ReplayController.java           # Counterfactual What-If simulation endpoints
â”œâ”€â”€ config\                             # Spring configuration beans
â”‚   â””â”€â”€ AppConfig.java                  # Jackson ObjectMapper bean definition
â”œâ”€â”€ correlation\                        # Timeline alignment, graph, and replay engine
â”‚   â”œâ”€â”€ ChronoSync.java                 # Clock offset calculation and timeline adjustment
â”‚   â”œâ”€â”€ CounterfactualReplay.java       # Fixed-point causal propagation simulation
â”‚   â”œâ”€â”€ EventCorrelator.java            # Multi-attribute causal attack chain linker
â”‚   â””â”€â”€ StoryGraph.java                 # JGraphT directed entity graph and narrative generator
â”œâ”€â”€ demo\                               # Demo data generator
â”‚   â””â”€â”€ DemoDataGenerator.java          # Generates 6 diverse format logs (JSON, CSV, XML, Syslog, Key-Value, Vendor)
â”œâ”€â”€ detection\                          # Real-time detection engine
â”‚   â”œâ”€â”€ DetectionEngine.java            # Aggregator for all detection rules
â”‚   â”œâ”€â”€ DriftDetector.java              # Format DNA schema drift detector
â”‚   â”œâ”€â”€ LogInjectionDetector.java       # CRLF and payload injection detector
â”‚   â””â”€â”€ SilenceWatch.java               # Telemetry dropout and source heartbeat monitor
â”œâ”€â”€ fingerprint\                        # Format DNA and tokenization engine
â”‚   â”œâ”€â”€ FormatDnaEngine.java            # Structural shape extractor and SHA-256 fingerprint generator
â”‚   â”œâ”€â”€ Tokenizer.java                  # Lexical character scanner
â”‚   â””â”€â”€ TokenType.java                  # Token classification enum
â”œâ”€â”€ forge\                              # On-the-fly parser generation and LLM bridge
â”‚   â”œâ”€â”€ Grammar.java                    # Declarative JSON grammar model
â”‚   â”œâ”€â”€ GrammarCompiler.java            # Compiles Grammar into LearnedParser
â”‚   â”œâ”€â”€ GrammarField.java               # Field extraction rule definition
â”‚   â”œâ”€â”€ GroqApiException.java           # Custom runtime exception for LLM errors
â”‚   â”œâ”€â”€ GroqClient.java                 # Native HTTP client for Groq Cloud API (with MOCK fallback)
â”‚   â”œâ”€â”€ LearnedParser.java              # Executable parser driven by compiled grammar
â”‚   â”œâ”€â”€ MetricsService.java             # Thread-safe telemetry and metrics counter
â”‚   â””â”€â”€ ParserForge.java                # Grammar validation gatekeeper and quality manager
â”œâ”€â”€ identity\                           # Identity correlation engine
â”‚   â”œâ”€â”€ IdentityEvidence.java           # Pairwise matching evidence record
â”‚   â””â”€â”€ IdentityWeaver.java             # Graph-based alias clustering (BFS)
â”œâ”€â”€ ledger\                             # Cryptographic audit ledger
â”‚   â”œâ”€â”€ LedgerGuard.java                # SHA-256 blockchain implementation
â”‚   â””â”€â”€ LedgerRecord.java               # Chained record entity
â”œâ”€â”€ model\                              # Core domain DTOs and entities
â”‚   â”œâ”€â”€ Detection.java                  # Security alert entity
â”‚   â”œâ”€â”€ FieldValue.java                 # Generic field value wrapper
â”‚   â”œâ”€â”€ FormatFingerprint.java          # Structural fingerprint representation
â”‚   â”œâ”€â”€ GraphEdge.java                  # Directed graph edge model
â”‚   â”œâ”€â”€ GraphEvent.java                 # Event node data wrapper
â”‚   â”œâ”€â”€ GraphNode.java                  # Entity node model
â”‚   â”œâ”€â”€ IdentityRecord.java             # Canonical identity cluster model
â”‚   â”œâ”€â”€ NormalizedEvent.java            # Standard OCSF normalized event
â”‚   â”œâ”€â”€ ParsedLog.java                  # Intermediate parser extraction map
â”‚   â”œâ”€â”€ ParserVersion.java              # Versioned parser metadata entity
â”‚   â”œâ”€â”€ Provenance.java                 # Parsing lineage audit trail
â”‚   â”œâ”€â”€ RawLog.java                     # Raw unparsed log container
â”‚   â””â”€â”€ ReplayResult.java               # Replay simulation response DTO
â”œâ”€â”€ normalization\                      # OCSF standardization
â”‚   â”œâ”€â”€ OcsfEvent.java                  # OCSF constant definitions
â”‚   â”œâ”€â”€ OcsfNormalizer.java             # Normalizer facade
â”‚   â””â”€â”€ SchemaMapper.java               # Dynamic and positional field-to-OCSF mapper
â”œâ”€â”€ parser\                             # Parser registry and built-in parsers
â”‚   â”œâ”€â”€ LogParser.java                  # Common log parser interface
â”‚   â”œâ”€â”€ ParserRegistry.java             # Concurrent parser cache and registry
â”‚   â””â”€â”€ builtin\
â”‚       â”œâ”€â”€ CsvLogParser.java           # Apache Commons CSV parser
â”‚       â”œâ”€â”€ JsonLogParser.java          # Jackson JSON tree parser
â”‚       â”œâ”€â”€ PlainTextParser.java        # Key-value / plain text regex parser
â”‚       â”œâ”€â”€ SyslogParser.java           # RFC 5424 / RFC 3164 Syslog parser
â”‚       â””â”€â”€ XmlLogParser.java           # Woodstox XML stream parser
â””â”€â”€ storage\                            # Persistence layer
    â”œâ”€â”€ DatabaseManager.java            # PostgreSQL JDBC connection manager and table initializer
    â”œâ”€â”€ EventRepository.java            # Dual-mode (PostgreSQL / In-Memory) event store
    â”œâ”€â”€ IdentityRepository.java         # Identity cluster store
    â”œâ”€â”€ LedgerRepository.java           # Ledger record store
    â””â”€â”€ ParserRepository.java           # Parser version metadata store
```

---

## 18. End-to-End Data Flows

### FLOW 1: Known Built-in Log (e.g. JSON Auth Log)
```text
1. User clicks JSON preset and Ingests: {"user": "john", "action": "LOGIN", "status": "SUCCESS"}
2. LogController receives POST /api/logs/ingest
3. Tokenizer categorizes JSON structure; FormatDnaEngine computes SHA-256 fingerprint.
4. ParserRegistry identifies JSON format -> selects JsonLogParser.
5. JsonLogParser.parse() extracts fields {"user": "john", "action": "LOGIN", "status": "SUCCESS"}.
6. SchemaMapper transforms fields to NormalizedEvent (user=john, activity=LOGIN, eventClass=Authentication).
7. LedgerGuard appends event to SHA-256 cryptographic chain.
8. DetectionEngine runs background checks (no alerts triggered).
9. EventRepository persists the NormalizedEvent.
10. UI refreshes, showing parsed JSON, incremented metrics, updated Ledger, and new timeline row.
```

### FLOW 2: Unknown Vendor Log (First Time)
```text
1. User ingests custom log: VENDOR_EVT|2026-09-16T10:05:00Z|USR-999|172.16.0.4|DATABASE_QUERY|SUCCESS
2. FormatDnaEngine extracts shape: WORD|TIMESTAMP_ISO|WORD|IPV4|WORD|WORD and hashes fingerprint.
3. ParserRegistry has no matching parser (Cache Miss).
4. ParserForge is invoked -> calls GroqClient.generateGrammar().
5. GroqClient sends sample to Groq API (or generates mock grammar).
6. Groq returns JSON grammar specifying extractionType: DELIMITED, delimiter: "|", and 6 fields.
7. GrammarCompiler compiles Grammar into LearnedParser.
8. ParserForge validates extraction rate (100% >= 80%). Validation passes!
9. ParserForge increments 'Parsers Learned' metric, caches LearnedParser in ParserRegistry, and saves ParserVersion.
10. LearnedParser parses the log -> SchemaMapper normalizes to OCSF -> LedgerGuard chains record.
```

### FLOW 3: Repeated Known Vendor Log (Subsequent Ingestions)
```text
1. User ingests identical format: VENDOR_EVT|2026-09-16T10:10:00Z|USR-100|172.16.0.8|DATABASE_QUERY|SUCCESS
2. FormatDnaEngine hashes the same shape -> same fingerprint.
3. ParserRegistry.getParserByFingerprint() finds the cached LearnedParser immediately (Cache Hit).
4. LearnedParser parses the log instantly in memory without invoking GroqClient.
5. Zero LLM calls made; latency < 1ms.
```

### FLOW 4: Drifted Schema Log
```text
1. User clicks "Trigger Drift Demo".
2. Baseline schema for auth-gateway was: USER=john IP=10.0.0.5 ACTION=LOGIN
3. New log arrives with mutated schema: USER=john IP=10.0.0.5 ACTION=LOGIN STATUS=SUCCESS
4. FormatDnaEngine computes a new shape with extra KEY_VALUE token.
5. DriftDetector compares current fingerprint with stored baseline.
6. DriftDetector flags PARSER_DRIFT and creates Detection object with severity MEDIUM.
7. DetectionEngine adds alert to detections list; UI alerts table updates immediately.
```

### FLOW 5: Correlated Incident Attack Progression
```text
1. Demo dataset is loaded (6 logs: Login -> Powershell Process -> Shadow File Access -> Firewall Failure -> Network Connection -> DB Query).
2. IdentityWeaver matches 'john', 'john.doe@company.com', and 'AD-USER-442' via shared RequestId (REQ-701) and Host (HOST-01), clustering them to canonical user 'john'.
3. ChronoSync applies clock skew alignment across sources.
4. EventCorrelator links the sequential events into a directed attack chain:
   FILE_ACCESS -> NETWORK_CONNECTION -> LOGIN -> PROCESS_STARTED
5. StoryGraph creates nodes and EVENT_TRIGGERED edges, and synthesizes a plain-English attack narrative.
6. ReplayController provides the correlated graph to the Replay UI.
```

### FLOW 6: Ledger Tampering & Verification
```text
1. User clicks "Demo Tamper Record #1".
2. LedgerGuard modifies data of record #1 to "TAMPERED_MALICIOUS_PAYLOAD".
3. User clicks "Verify Chain".
4. LedgerGuard.verify() checks record #1: SHA-256(Genesis + TamperedData) != record1.currentHash.
5. Verification fails! UI displays red badge: "Chain Broken at Record #1".
```

### FLOW 7: Counterfactual What-If Replay
```text
1. User inspects the Replay Timeline.
2. User clicks "Suggest best block point".
3. Backend evaluates computePrevented() for all 6 events. Identifies FILE_ACCESS (root event) as stopping 67% impact (4 of 6 events prevented).
4. Dashboard marks FILE_ACCESS with 'â­ RECOMMENDED' and yellow dashed border.
5. User clicks FILE_ACCESS row.
6. Frontend sends POST /api/replay/default/block with eventId of FILE_ACCESS.
7. Backend fixed-point propagation calculates:
   - Prevented: FILE_ACCESS, NETWORK_CONNECTION, LOGIN, PROCESS_STARTED (4 events).
   - Still Happens: Firewall Syslog, DB Query (2 events).
   - Impact stopped: 67%.
8. UI updates instantly:
   - FILE_ACCESS row turns red ("blocked here").
   - 3 child event rows turn grey with strikethrough ("never happens").
   - 2 independent rows remain normal ("still happens").
   - Top metrics display: "Events prevented: 4 of 6", "Impact stopped: 67%".
```

---

## 19. Database & Storage Architecture

### Dual-Mode Persistence Architecture
CypherNex features an intelligent **dual-mode resilient storage architecture** managed by [`DatabaseManager.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/storage/DatabaseManager.java):

1. **PostgreSQL Mode**: If a PostgreSQL instance is running at `jdbc:postgresql://localhost:5432/cyphernex`, `DatabaseManager` automatically connects, runs DDL scripts, and initializes tables:
   - `normalized_events`
   - `parser_versions`
   - `ledger_records`
   - `identity_records`
2. **Resilient In-Memory Mode**: If PostgreSQL is not reachable, the application logs a warning and automatically falls back to thread-safe in-memory storage (`ConcurrentHashMap`, `CopyOnWriteArrayList`), ensuring 100% feature availability without hard database dependencies.

---

## 20. Error Handling & Edge Cases

| Scenario | Handled By | System Behavior | What the User Sees in Dashboard |
|---|---|---|---|
| **Empty Log Input** | `LogController.java` | Checks `rawContent.isBlank()`; returns HTTP 400 Bad Request. | JSON error message: `{"status": "INVALID_INPUT", "message": "No log content provided..."}` |
| **Malformed XML / CSV** | Built-in Parsers | Catches exceptions; returns `ParsedLog` with `validationStatus = "INVALID"` and error description. | Event ingested with fallback fields and confidence = 0.0. |
| **Groq API Timeout / Failure** | `GroqClient.java` | Connect timeout set to 10s; catches `IOException`/`InterruptedException`; falls back to `generateMockGrammar()`. | Ingestion succeeds seamlessly via fallback grammar; `llmMode` remains intact. |
| **Groq Returns Non-JSON / Markdown** | `GroqClient.java` | `cleanMarkdownFences()` strips markdown; Jackson validates JSON; rejects malformed responses. | Parser Forge skips invalid grammar; logs warning. |
| **Failed Grammar Quality Gate** | `ParserForge.java` | Checks extraction/validation rates >= 0.80; rejects grammars below threshold. | Returns `UNKNOWN_FORMAT` safely without crashing or polluting registry. |
| **Tampered Ledger Record** | `LedgerGuard.java` | SHA-256 hash mismatch detected during `verify()`. | Dashboard displays red warning badge: `Chain Broken at Record #X`. |
| **Replay on Empty Dataset** | `CounterfactualReplay.java` | Checks `allEvents.isEmpty()`; returns safe empty DTO. | Timeline shows: "No events available. Load demo dataset to populate timeline." |

---

## 21. Demo Dataset Specifications

The demo dataset is generated by [`DemoDataGenerator.java`](file:///d:/CypherNex/src/main/java/com/cyphernex/demo/DemoDataGenerator.java) and contains **6 distinct logs**:

1. **JSON Log** (`auth-service`):
   `{"timestamp":"2026-09-16T10:00:00Z","user":"john","action":"LOGIN","ip":"10.10.2.15","status":"SUCCESS","requestId":"REQ-701","sessionId":"SES-99"}`
2. **CSV Log** (`process-monitor`):
   `2026-09-16T10:01:00Z,john,PROCESS_STARTED,HOST-01,powershell.exe`
3. **XML Log** (`file-auditor`):
   `<log><timestamp>2026-09-16T10:02:00Z</timestamp><user>john.doe@company.com</user><action>FILE_ACCESS</action><host>HOST-01</host><file>/etc/shadow</file><requestId>REQ-701</requestId></log>`
4. **Syslog Log** (`firewall-01`):
   `<134>1 2026-09-16T10:03:00Z fw01.corp sshd 4102 - - Failed password for invalid user admin from 192.168.1.50 port 22 ssh2`
5. **Plain Text Key-Value Log** (`network-gateway`):
   `timestamp=2026-09-16T10:04:00Z user=AD-USER-442 action=NETWORK_CONNECTION ip=198.51.100.4 host=HOST-01 requestId=REQ-701`
6. **Unknown Vendor Delimited Log** (`custom-sensor`):
   `VENDOR_EVT|2026-09-16T10:05:00Z|USR-999|172.16.0.4|DATABASE_QUERY|SUCCESS`

---

## 22. Dashboard Button-to-Code Mapping Master Table

| Button Text / UI Control | Frontend JS Function | API Endpoint Called | Backend Controller | Backend Service / Component | Core Java Method Executed | UI Screen Change |
|---|---|---|---|---|---|---|
| **Ingest Log** | `ingestLog()` | `POST /api/logs/ingest` | `LogController` | `FormatDnaEngine`, `ParserForge`, `OcsfNormalizer`, `LedgerGuard` | `LogController.ingestLogs()` | Renders ingestion JSON result, increments top metrics, appends to event log and ledger table. |
| **Load Demo Dataset** | `loadDemoData()` | `POST /api/demo/load` | `DemoController` | `DemoDataGenerator`, `LogController` | `DemoController.loadDemoData()` | Populates "Raw Logs" table with 6 logs, updates all 8 top metrics, builds StoryGraph, and refreshes Replay timeline. |
| **Reset Demo** | `resetDemo()` | `POST /api/demo/reset` | `DemoController` | `EventRepository`, `DetectionEngine`, `StoryGraph`, `LedgerGuard` | `DemoController.resetDemo()` | Empties all tables, resets metrics to 0, clears StoryGraph narrative and Replay timeline. |
| **Preset: JSON** | `setPreset('json')` | None (Client-side) | N/A | N/A | N/A | Fills textarea with sample JSON auth log and sets source to `auth-service`. |
| **Preset: CSV** | `setPreset('csv')` | None (Client-side) | N/A | N/A | N/A | Fills textarea with sample CSV process log and sets source to `process-service`. |
| **Preset: XML** | `setPreset('xml')` | None (Client-side) | N/A | N/A | N/A | Fills textarea with sample XML file audit log and sets source to `xml-service`. |
| **Preset: Syslog** | `setPreset('syslog')` | None (Client-side) | N/A | N/A | N/A | Fills textarea with sample Syslog SSH failure log and sets source to `syslog-service`. |
| **Preset: Plain Text** | `setPreset('plain')` | None (Client-side) | N/A | N/A | N/A | Fills textarea with sample key-value network log and sets source to `plain-service`. |
| **Preset: Vendor Custom** | `setPreset('vendor')` | None (Client-side) | N/A | N/A | N/A | Fills textarea with unknown pipe-delimited sensor log and sets source to `custom-sensor`. |
| **Suggest best block point** | `suggestBestBlockPoint()` | `GET /api/replay/default/best-block` | `ReplayController` | `CounterfactualReplay` | `CounterfactualReplay.findBestBlockPoint()` | Highlights the highest-impact event row with a dashed yellow outline and `â­ RECOMMENDED` badge without auto-blocking. |
| **Reset timeline** | `resetTimeline()` | None (Client-side) | N/A | `DashboardController.js` | `renderReplayTimeline()` | Clears active blocked event, removes recommendation highlights, resets metrics to "0 of Y (0%)", restores normal row styles. |
| **Timeline Event Row (Click)** | `toggleBlockEvent(id)` | `POST /api/replay/default/block` | `ReplayController` | `CounterfactualReplay` | `CounterfactualReplay.computePrevented()` | Toggles block: clicked row becomes red ("blocked here"), child events become grey strikethrough ("never happens"), independent events remain normal ("still happens"), metrics update. |
| **Trigger Drift Demo** | `triggerDriftDemo()` | `POST /api/detections/drift-demo` | `DetectionController` | `DriftDetector` | `DriftDetector.inspect()` | Ingests mutated schema; displays `PARSER_DRIFT` alert in Detections table. |
| **Simulate Silence** | `simulateSilence()` | `POST /api/detections/silence-demo` | `DetectionController` | `SilenceWatch` | `SilenceWatch.simulateSilence()` | Emits `SILENCE_WATCH` alert for `firewall-01`; updates Detections table. |
| **Test Log Injection** | `testLogInjection()` | `POST /api/detections/injection-demo` | `DetectionController` | `LogInjectionDetector` | `LogInjectionDetector.inspect()` | Ingests CRLF payload; displays `LOG_INJECTION_ATTEMPT` alert in Detections table. |
| **Verify Chain** | `verifyLedger()` | `GET /api/ledger/verify` | `LedgerController` | `LedgerGuard` | `LedgerGuard.verify()` | Displays green badge `CHAIN VALID (N records)` or red badge `CHAIN BROKEN AT #X`. |
| **Demo Tamper Record #1** | `tamperLedger()` | `POST /api/ledger/tamper` | `LedgerController` | `LedgerGuard` | `LedgerGuard.tamper()` | Mutates payload of record #1 in-memory; alerts user to click "Verify Chain" to witness detection. |
| **Reset Ledger** | `resetLedger()` | `POST /api/ledger/reset` | `LedgerController` | `LedgerGuard` | `LedgerGuard.reset()` | Clears all ledger records and resets genesis hash. |

---

## 23. Viva Explanation Guide

### 30-Second Elevator Pitch
> *"CypherNex is an adaptive security log intelligence engine that solves log parser fragility and SIEM alert fatigue. When an unknown log format arrives, instead of failing or requiring manual regex writing, CypherNex extracts its structural 'Format DNA', queries an LLM once to generate a declarative JSON grammar, validates it, and compiles it into a high-speed cached parser. It normalizes all logs to OCSF standard, unifies fragmented user identities with Identity Weaver, corrects clock skew with ChronoSync, builds a causal StoryGraph attack narrative, guarantees audit integrity using LedgerGuard SHA-256 hash chaining, and allows security analysts to simulate kill-chain mitigation points using What-If Counterfactual Replay."*

### Why Questions for Examiners

#### Q1: Why did we build CypherNex?
> Modern cloud environments ingest petabytes of logs daily across hundreds of microservices. Writing and maintaining manual log parsers is a bottleneck, while sending all logs to an LLM is economically impossible. CypherNex bridges this gap by combining structural fingerprinting, one-shot LLM grammar synthesis, and zero-cost cached execution.

#### Q2: Why Java 17 and Spring Boot?
> Java provides strong type safety, deterministic memory performance, multi-threading support for high-throughput ingestion, and rich graph libraries (JGraphT). Spring Boot allows rapid microservice deployment, clean dependency injection, and embedded web serving without heavy external infrastructure.

#### Q3: Why use an LLM only for grammar generation instead of parsing directly?
> Parsing directly with an LLM introduces latency (500ms - 2s per log), financial cost ($0.001 - $0.02 per log), and non-deterministic hallucination risks. Generating a declarative JSON grammar once turns the LLM into a parser compiler: the resulting parser executes deterministically in microseconds in native Java with zero recurring API costs.

#### Q4: Why OCSF?
> The Open Cybersecurity Schema Framework (OCSF) provides an open standard for security events. Normalizing all logs to OCSF allows downstream detection engines, correlation graphs, and ML models to query a uniform schema regardless of whether the original log came from AWS, Windows, Linux Syslog, or custom IoT sensors.

#### Q5: Why StoryGraph and Identity Weaver?
> Real attacks span multiple accounts (`john`, `john.doe@company.com`) and hosts over time. StoryGraph connects isolated logs into a unified causal directed graph, while Identity Weaver uses multi-attribute evidence to cluster fragmented aliases into single human actors.

#### Q6: Why LedgerGuard?
> Advanced Persistent Threats (APTs) often alter local log files to hide privilege escalation and exfiltration. LedgerGuard creates a cryptographic SHA-256 hash chain where modifying even a single character in past records breaks the hash continuity, making unauthorized alterations immediately detectable.

#### Q7: Why What-If Counterfactual Replay?
> Incident response requires knowing which firewall rule or account revocation would have been most effective. What-If Replay uses fixed-point forward propagation across the causal graph to simulate attack disruption, measuring exact prevented event counts and percentage impact stopped.

---

## 24. Current Project Status

### Fully Implemented
- [x] **Format DNA Engine & Tokenizer**: Lexical tokenization, shape abstraction, and SHA-256 fingerprinting.
- [x] **Built-in Parsers**: JSON, CSV (Apache Commons), XML (Woodstox StAX), Syslog (RFC 5424/3164), Plain Text key-value.
- [x] **Parser Forge & Groq LLM**: Real Groq API client, mock fallback, declarative grammar compilation, and validation quality gate (>= 80%).
- [x] **Parser Registry & Metrics**: Thread-safe caching, hit/miss tracking, lineage history.
- [x] **OCSF Normalizer & Schema Mapper**: Comprehensive mapping to OCSF schema, positional CSV column support, severity normalization.
- [x] **Identity Weaver**: Pairwise confidence scoring (session, request, string similarity, timing) and BFS alias clustering.
- [x] **ChronoSync**: Correlation group baseline tracking, clock skew calculation, and timestamp alignment.
- [x] **StoryGraph & Narrative Engine**: JGraphT entity/event graph, sequential causal chaining, automated plain-English narrative.
- [x] **Detection Engine**: Parser drift detection, SilenceWatch timeout monitor, and CRLF log injection defense.
- [x] **LedgerGuard**: SHA-256 cryptographic hash chaining, full chain verification, and in-memory tamper simulation.
- [x] **Counterfactual What-If Replay**: Fixed-point causal propagation, severity-weighted impact percentage, candidate best-block optimizer, interactive UI with hover causal tooltips and state toggling.
- [x] **Spring Boot REST API**: 28+ tested endpoints.
- [x] **Responsive Single-Page Dashboard**: HTML5/Vanilla CSS/JS UI with live metrics, preset injection, detection demos, and interactive replay timeline.
- [x] **Dual-Mode Persistence**: PostgreSQL JDBC support with resilient in-memory fallback.
- [x] **Comprehensive Test Suite**: 86 automated unit and integration tests passing with 0 failures.

### Partially Implemented
- None. All specified core architectural components are active and verified.

### Specified But Not Currently Implemented
- External distributed Kafka/RabbitMQ message broker ingestion queue (currently direct HTTP REST ingestion).
- Distributed multi-node consensus for LedgerGuard (currently local node cryptographic blockchain).

### Backend Features Not Visible in Current UI
- `GET /api/identity/records`: Direct JSON dump of resolved identity clusters.
- `GET /api/chronosync/offsets`: Direct JSON dump of computed clock offsets per source.
- `GET /api/parsers`: Direct JSON array of all registered parser names.
- `POST /api/metrics/reset`: Direct endpoint for zeroing telemetry counters.

---

## 25. Summary Verification Metrics
- **Automated Tests**: 86 passing tests (`mvn test` -> 0 failures, 0 errors, 0 skipped).
- **Maven Packaging**: Clean build (`mvn clean package -DskipTests` -> `BUILD SUCCESS`).
- **End-to-End Live Validation**: Verified via PowerShell REST test script across all 12 controllers and UI panels.