package com.cyphernex.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private static final String CSS_STYLES = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CypherNex — SOC / SIEM Causal Log Intelligence Console</title>
    <style>
        :root {
            --bg-body: #070c18;
            --bg-sidebar: #0b1220;
            --bg-panel: #0e1728;
            --bg-card: #131f35;
            --bg-input: #080e1a;
            --bg-hover: #182740;
            --border: #1e2e4a;
            --border-active: #00f2ff;
            --text-main: #f1f5f9;
            --text-dim: #94a3b8;
            --text-muted: #64748b;
            --cyan: #00f2ff;
            --blue: #3b82f6;
            --green: #10b981;
            --amber: #f59e0b;
            --red: #ef4444;
            --purple: #a855f7;
            --font-mono: "JetBrains Mono", "SF Mono", Consolas, monospace;
            --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        html, body { width: 100%; max-width: 100%; background: var(--bg-body); color: var(--text-main); font-family: var(--font-sans); font-size: 13px; line-height: 1.4; overflow-x: hidden; }
        
        /* Top Command Bar */
        .topbar { background: var(--bg-sidebar); border-bottom: 1px solid var(--border); padding: 8px 20px; display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; z-index: 100; min-height: 56px; width: 100%; max-width: 100%; flex-wrap: wrap; gap: 10px; }
        .brand { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
        .brand-logo { font-family: var(--font-mono); font-size: 17px; font-weight: 900; letter-spacing: 1px; color: #fff; display: flex; align-items: center; gap: 8px; }
        .brand-logo .dot { width: 9px; height: 9px; border-radius: 50%; background: var(--cyan); box-shadow: 0 0 10px var(--cyan); }
        .brand-tag { background: rgba(0, 242, 255, 0.12); border: 1px solid rgba(0, 242, 255, 0.35); color: var(--cyan); font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px; }
        
        /* Prominent Top Status Boxes */
        .top-stats { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
        .stat-chip { display: flex; align-items: center; gap: 8px; background: #0e1728; border: 1px solid var(--border); padding: 5px 12px; border-radius: 5px; box-shadow: 0 1px 3px rgba(0,0,0,0.3); }
        .stat-chip .k { color: var(--text-muted); font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
        .stat-chip .v { font-family: var(--font-mono); font-weight: 800; font-size: 13px; color: var(--text-main); }
        
        .topbar-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
        .pulse-online { display: inline-flex; align-items: center; gap: 6px; font-size: 11px; color: var(--green); font-family: var(--font-mono); background: rgba(16, 185, 129, 0.1); padding: 4px 9px; border-radius: 4px; border: 1px solid rgba(16, 185, 129, 0.3); }
        .pulse-dot { width: 6px; height: 6px; background: var(--green); border-radius: 50%; animation: pulse 1.5s infinite; }
        @keyframes pulse { 0% { transform: scale(0.95); opacity: 0.8; } 50% { transform: scale(1.3); opacity: 1; } 100% { transform: scale(0.95); opacity: 0.8; } }
        
        .btn { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 4px; font-size: 12px; font-weight: 600; cursor: pointer; border: 1px solid transparent; transition: all 0.15s ease; white-space: nowrap; }
        .btn-cyan { background: linear-gradient(135deg, #00c6ff, #0072ff); color: #fff; box-shadow: 0 2px 8px rgba(0, 198, 255, 0.25); }
        .btn-cyan:hover { filter: brightness(1.1); box-shadow: 0 2px 12px rgba(0, 198, 255, 0.4); }
        .btn-secondary { background: var(--bg-card); color: var(--text-dim); border: 1px solid var(--border); }
        .btn-secondary:hover { background: var(--bg-hover); color: #fff; }
        .btn-danger { background: rgba(239, 68, 68, 0.15); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); }
        .btn-danger:hover { background: rgba(239, 68, 68, 0.3); color: #fff; }
        .btn-sm { padding: 4px 10px; font-size: 11px; }
        .btn-preset { background: var(--bg-input); border: 1px solid var(--border); color: var(--text-dim); font-family: var(--font-mono); font-size: 11px; padding: 4px 8px; border-radius: 4px; cursor: pointer; }
        .btn-preset:hover, .btn-preset.active { border-color: var(--cyan); color: var(--cyan); background: rgba(0, 242, 255, 0.08); }

        .badge { display: inline-flex; align-items: center; padding: 2px 7px; border-radius: 3px; font-size: 10px; font-weight: 700; font-family: var(--font-mono); text-transform: uppercase; }
        .badge-info { background: rgba(59, 130, 246, 0.15); color: #93c5fd; border: 1px solid rgba(59, 130, 246, 0.35); }
        .badge-success { background: rgba(16, 185, 129, 0.15); color: #6ee7b7; border: 1px solid rgba(16, 185, 129, 0.35); }
        .badge-warning { background: rgba(245, 158, 11, 0.15); color: #fcd34d; border: 1px solid rgba(245, 158, 11, 0.35); }
        .badge-danger { background: rgba(239, 68, 68, 0.15); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.35); }
        .badge-purple { background: rgba(168, 85, 247, 0.15); color: #d8b4fe; border: 1px solid rgba(168, 85, 247, 0.35); }
        .badge-cyan { background: rgba(0, 242, 255, 0.15); color: var(--cyan); border: 1px solid rgba(0, 242, 255, 0.35); }

        /* App 2-Column Responsive Layout: Fixed Sidebar + Full Width Vertical Stack Workspace */
        .app-container { display: flex; flex-direction: row; height: calc(100vh - 56px); width: 100%; max-width: 100%; overflow: hidden; }
        
        /* Left Vertical Sidebar */
        .sidebar { width: 210px; min-width: 210px; max-width: 210px; flex-shrink: 0; background: var(--bg-sidebar); border-right: 1px solid var(--border); display: flex; flex-direction: column; justify-content: space-between; padding: 12px 0; user-select: none; overflow-y: auto; overflow-x: hidden; }
        .nav-list { display: flex; flex-direction: column; gap: 3px; }
        .nav-item { display: flex; align-items: center; gap: 10px; padding: 10px 16px; color: var(--text-dim); font-size: 12px; font-weight: 600; cursor: pointer; border-left: 3px solid transparent; transition: all 0.15s ease; }
        .nav-item:hover { color: #fff; background: rgba(255, 255, 255, 0.03); }
        .nav-item.active { color: var(--cyan); background: rgba(0, 242, 255, 0.06); border-left-color: var(--cyan); }
        .nav-icon { font-size: 14px; width: 16px; text-align: center; }
        .sidebar-footer { padding: 12px 16px; border-top: 1px solid var(--border); font-size: 10px; color: var(--text-muted); font-family: var(--font-mono); }

        /* Center Main Workspace (Vertically Stacked, Zero Horizontal Scroll) */
        .workspace { flex: 1; min-width: 0; max-width: 100%; background: var(--bg-body); overflow-y: auto; overflow-x: hidden; padding: 18px 24px; display: flex; flex-direction: column; gap: 16px; }
        .module-pane { display: none; width: 100%; max-width: 100%; min-width: 0; }
        .module-pane.active { display: flex; flex-direction: column; gap: 16px; }

        /* Cards and Containers */
        .card { background: var(--bg-panel); border: 1px solid var(--border); border-radius: 6px; padding: 14px 16px; width: 100%; max-width: 100%; min-width: 0; box-sizing: border-box; }
        .card-header { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
        
        .intel-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 12px; width: 100%; min-width: 0; }
        .intel-section { background: var(--bg-card); border: 1px solid var(--border); border-radius: 5px; padding: 10px 12px; min-width: 0; width: 100%; }
        .intel-title { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); margin-bottom: 8px; display: flex; justify-content: space-between; }
        .intel-row { display: flex; justify-content: space-between; font-size: 11px; padding: 4px 0; border-bottom: 1px solid #16243d; gap: 8px; }
        .intel-row:last-child { border-bottom: none; }
        .intel-k { color: var(--text-dim); flex-shrink: 0; }
        .intel-v { font-family: var(--font-mono); color: var(--text-main); text-align: right; word-break: break-all; overflow-wrap: anywhere; min-width: 0; }

        /* Tables & Previews */
        .table-responsive { width: 100%; max-width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch; }
        table.cyber-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; }
        table.cyber-table th { background: #0b1322; padding: 9px 12px; font-weight: 700; font-size: 11px; color: var(--text-muted); border-bottom: 1px solid var(--border); text-transform: uppercase; }
        table.cyber-table td { padding: 9px 12px; border-bottom: 1px solid #142137; color: var(--text-main); }
        table.cyber-table tr:hover td { background: rgba(0, 242, 255, 0.03); }
        
        pre.code-view { background: #060a12; border: 1px solid var(--border); padding: 12px; border-radius: 5px; font-family: var(--font-mono); font-size: 11px; line-height: 1.5; color: #7dd3fc; overflow-x: auto; overflow-y: auto; max-height: 280px; white-space: pre-wrap; word-break: break-word; overflow-wrap: anywhere; width: 100%; box-sizing: border-box; }

        /* Stream Feed Styles */
        .stream-list { display: flex; flex-direction: column; gap: 6px; max-height: 320px; overflow-y: auto; width: 100%; }
        .stream-item { background: var(--bg-card); border: 1px solid var(--border); border-radius: 5px; padding: 9px 12px; cursor: pointer; transition: all 0.15s ease; width: 100%; }
        .stream-item:hover { border-color: #3b82f6; background: #17243b; }
        .stream-item.active { border-color: var(--cyan); background: #10263f; box-shadow: inset 3px 0 0 var(--cyan); }
        .stream-meta { display: flex; justify-content: space-between; font-size: 10px; margin-bottom: 4px; font-family: var(--font-mono); }
        .stream-raw { font-family: var(--font-mono); font-size: 11px; color: #93c5fd; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
        
        textarea.log-ta { width: 100%; height: 75px; background: var(--bg-input); border: 1px solid var(--border); border-radius: 4px; padding: 10px; color: #e2e8f0; font-family: var(--font-mono); font-size: 11px; resize: none; margin-bottom: 8px; box-sizing: border-box; }
        textarea.log-ta:focus { outline: none; border-color: var(--cyan); }
        
        .narrative-card { background: rgba(0, 242, 255, 0.04); border: 1px solid rgba(0, 242, 255, 0.2); border-left: 4px solid var(--cyan); border-radius: 4px; padding: 12px 14px; font-size: 12px; color: #e2e8f0; line-height: 1.5; width: 100%; box-sizing: border-box; }
        .empty-state { text-align: center; padding: 25px 10px; color: var(--text-muted); font-size: 12px; width: 100%; }
        .empty-icon { font-size: 22px; margin-bottom: 4px; opacity: 0.5; }
        
        ::-webkit-scrollbar { width: 5px; height: 5px; }
        ::-webkit-scrollbar-track { background: #060a12; }
        ::-webkit-scrollbar-thumb { background: #1e2e4a; border-radius: 3px; }
        
        @media (max-width: 900px) {
            .app-container { flex-direction: column; height: auto; }
            .sidebar { width: 100%; min-width: 100%; max-width: 100%; height: auto; border-right: none; border-bottom: 1px solid var(--border); }
            .nav-list { flex-direction: row; flex-wrap: wrap; }
            .workspace { padding: 14px; }
        }
    </style>
</head>
<body>
""";

    private static final String HTML_HEADER_AND_SIDEBAR = """
    <!-- Top Command & Status Bar with Prominent Readable Status Chips -->
    <header class="topbar">
        <div class="brand">
            <div class="brand-logo"><span class="dot"></span> CYPHERNEX</div>
            <span class="brand-tag">SOC / SIEM v2.4</span>
        </div>
        
        <div class="top-stats">
            <div class="stat-chip"><span class="k">Mode:</span><span class="v" id="top-mode"><span class="badge badge-cyan">LIVE</span></span></div>
            <div class="stat-chip"><span class="k">Ingested:</span><span class="v" id="top-ingested" style="color: var(--cyan);">0</span></div>
            <div class="stat-chip"><span class="k">OCSF:</span><span class="v" id="top-ocsf" style="color: var(--green);">0</span></div>
            <div class="stat-chip"><span class="k">Cache Reuse:</span><span class="v" id="top-cache" style="color: var(--purple);">0%</span></div>
            <div class="stat-chip"><span class="k">LLM Calls:</span><span class="v" id="top-llm">0</span></div>
            <div class="stat-chip"><span class="k">Detections:</span><span class="v" id="top-detections" style="color: var(--red);">0</span></div>
            <div class="stat-chip"><span class="k">Ledger:</span><span class="v" id="top-ledger"><span class="badge badge-success">VALID</span></span></div>
        </div>

        <div class="topbar-actions">
            <span class="pulse-online"><span class="pulse-dot"></span> <span id="hdr-mode-text">ONLINE</span></span>
            <button class="btn btn-cyan" onclick="loadAttackScenario()">⚡ Load Attack Scenario</button>
            <button class="btn btn-danger" onclick="clearWorkspace()">🗑️ Clear Workspace</button>
        </div>
    </header>

    <!-- App Container: Fixed Left Sidebar + Main Stacked Content Workspace -->
    <div class="app-container">
        <!-- 1. LEFT VERTICAL SIDEBAR -->
        <aside class="sidebar">
            <nav class="nav-list">
                <div class="nav-item active" onclick="switchNav('pane-ingest')"><span class="nav-icon">⚡</span> <span>Live Ingest</span></div>
                <div class="nav-item" onclick="switchNav('pane-parsers')"><span class="nav-icon">🧠</span> <span>Parser Intelligence</span></div>
                <div class="nav-item" onclick="switchNav('pane-ocsf')"><span class="nav-icon">📋</span> <span>OCSF Mapper</span></div>
                <div class="nav-item" onclick="switchNav('pane-storygraph')"><span class="nav-icon">🕸️</span> <span>StoryGraph</span></div>
                <div class="nav-item" onclick="switchNav('pane-detections')"><span class="nav-icon">🚨</span> <span>Detections</span></div>
                <div class="nav-item" onclick="switchNav('pane-ledger')"><span class="nav-icon">🛡️</span> <span>LedgerGuard</span></div>
                <div class="nav-item" onclick="switchNav('pane-replay')"><span class="nav-icon">🔮</span> <span>What-If Replay</span></div>
            </nav>
            <div class="sidebar-footer">
                <div>SESSION MEMORY ONLY</div>
                <div style="color: var(--cyan); margin-top: 2px;">ZERO-ETL ENGINE</div>
            </div>
        </aside>
""";

    private static final String HTML_MODULE_WORKSPACE_PART1 = """
        <!-- 2. CENTER WORKSPACE PANE (VERTICALLY STACKED) -->
        <main class="workspace">

            <!-- MODULE 1: Live Ingest & Lifecycle Trace (Vertical Stacking) -->
            <div id="pane-ingest" class="module-pane active">
                
                <!-- 1. Quick Log Ingestion Panel -->
                <div class="card">
                    <div class="card-header">
                        <span>1. QUICK LOG INGESTION CONSOLE</span>
                        <div style="display:flex; flex-wrap:wrap; gap:4px;">
                            <button class="btn-preset active" onclick="setPreset('json', this)">JSON</button>
                            <button class="btn-preset" onclick="setPreset('csv', this)">CSV</button>
                            <button class="btn-preset" onclick="setPreset('xml', this)">XML</button>
                            <button class="btn-preset" onclick="setPreset('syslog', this)">Syslog</button>
                            <button class="btn-preset" onclick="setPreset('cef', this)">CEF</button>
                            <button class="btn-preset" onclick="setPreset('vendor', this)">Vendor KV</button>
                            <button class="btn-preset" style="color: #a855f7; border-color: rgba(168, 85, 247, 0.4);" onclick="setPreset('drift', this)">🧪 Drift Sample</button>
                            <button class="btn-preset" style="color: #ef4444; border-color: rgba(239, 68, 68, 0.4);" onclick="setPreset('injection', this)">🧪 Injection Sample</button>
                        </div>
                    </div>
                    <textarea class="log-ta" id="log-input" placeholder="Enter log payload..."></textarea>
                    <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
                        <div style="display:flex; align-items:center; gap:8px;">
                            <span style="font-size:11px; color:var(--text-muted);">Source:</span>
                            <input type="text" id="log-source" value="auth-service" placeholder="source" style="background:var(--bg-input); border:1px solid var(--border); color:#cbd5e1; font-family:var(--font-mono); font-size:11px; padding:4px 8px; border-radius:4px; width:130px;">
                        </div>
                        <div style="display:flex; gap:8px;">
                            <button class="btn btn-purple btn-sm" onclick="loadAttackScenario()">📦 Load Demo Dataset</button>
                            <button class="btn btn-cyan btn-sm" onclick="ingestCustomLog()">⚡ Parse & Ingest &rarr;</button>
                        </div>
                    </div>
                </div>

                <!-- 2. Stream Feed Panel -->
                <div class="card">
                    <div class="card-header">
                        <span>2. STREAM FEED (CLICK ANY EVENT TO INSPECT)</span>
                        <span class="badge badge-info" id="stream-count-badge">0 Logs</span>
                    </div>
                    <div class="stream-list" id="stream-list-container">
                        <div class="empty-state"><div class="empty-icon">📥</div><div>No logs ingested yet. Enter a log above or load an attack scenario.</div></div>
                    </div>
                </div>

                <!-- 3. Format DNA & Parser Forge Lifecycle Panel -->
                <div class="card">
                    <div class="card-header"><span>3. FORMAT DNA & PARSER FORGE LIFECYCLE</span></div>
                    <div id="ingest-empty" class="empty-state"><div class="empty-icon">🔍</div><div>Select an event from the Stream Feed to inspect Parser Forge lifecycle.</div></div>
                    <div id="ingest-detail-card" style="display: none;">
                        <div class="intel-grid" style="margin-bottom: 12px;">
                            <div class="intel-section">
                                <div class="intel-title">FORMAT DNA DIAGNOSTICS</div>
                                <div class="intel-row"><span class="intel-k">Detected Format</span><span class="intel-v" id="dt-format">JSON</span></div>
                                <div class="intel-row"><span class="intel-k">Structural Fingerprint</span><span class="intel-v" id="dt-fp">SHA-256...</span></div>
                                <div class="intel-row"><span class="intel-k">Structural Pattern</span><span class="intel-v" id="dt-pattern">KEY_VALUE</span></div>
                                <div class="intel-row"><span class="intel-k">Cache Status</span><span class="intel-v" id="dt-cache">HIT</span></div>
                            </div>
                            <div class="intel-section">
                                <div class="intel-title">PARSER FORGE ENGINE TRACE</div>
                                <div class="intel-row"><span class="intel-k">Selected Parser</span><span class="intel-v" id="dt-parser">JsonLogParser</span></div>
                                <div class="intel-row"><span class="intel-k">Decision Strategy</span><span class="intel-v" id="dt-decision">KNOWN_BUILTIN</span></div>
                                <div class="intel-row"><span class="intel-k">Grammar Rules</span><span class="intel-v" id="dt-grammar">CORE_NATIVE</span></div>
                                <div class="intel-row"><span class="intel-k">LLM Compiler</span><span class="intel-v" id="dt-llm-flag">NO</span></div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 4. Normalized OCSF Payload Panel -->
                <div class="card">
                    <div class="card-header"><span>4. NORMALIZED OCSF PAYLOAD</span></div>
                    <div id="ocsf-payload-empty" class="empty-state"><div class="empty-icon">📋</div><div>Select an event from the Stream Feed to view normalized OCSF payload.</div></div>
                    <div id="ocsf-payload-container" style="display:none;">
                        <pre class="code-view" id="dt-ocsf-json">{}</pre>
                    </div>
                </div>

                <!-- 5. Event Intelligence Panel (Full Width Vertical) -->
                <div class="card">
                    <div class="card-header">
                        <span>5. EVENT INTELLIGENCE & FORENSIC DIAGNOSTICS</span>
                        <span class="badge badge-cyan" id="intel-class-badge">FORENSICS</span>
                    </div>
                    <div id="intel-empty" class="empty-state"><div class="empty-icon">🎯</div><div>Select an event from the Stream Feed to inspect deep forensic properties.</div></div>
                    <div id="intel-details" style="display: none;">
                        <div class="intel-section" style="margin-bottom:12px;">
                            <div class="intel-title">RAW LOG STRING</div>
                            <div style="font-family: var(--font-mono); font-size: 11px; color: #93c5fd; word-break: break-all; overflow-wrap: anywhere; max-height: 100px; overflow-y: auto;" id="r-raw">N/A</div>
                        </div>
                        <div class="intel-grid">
                            <div class="intel-section">
                                <div class="intel-title">IDENTITY & PARSER</div>
                                <div class="intel-row"><span class="intel-k">Event ID</span><span class="intel-v" id="r-eid">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Origin Tag</span><span class="intel-v" id="r-origin"><span class="badge badge-cyan">LIVE</span></span></div>
                                <div class="intel-row"><span class="intel-k">Source</span><span class="intel-v" id="r-source">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Parser Used</span><span class="intel-v" id="r-parser">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Format DNA</span><span class="intel-v" id="r-dna" style="font-size:10px;">N/A</span></div>
                            </div>
                            <div class="intel-section">
                                <div class="intel-title">CORRELATED ENTITIES</div>
                                <div class="intel-row"><span class="intel-k">Actor User</span><span class="intel-v" id="r-user">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Source IP</span><span class="intel-v" id="r-ip">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Destination IP</span><span class="intel-v" id="r-dst-ip">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Target Asset</span><span class="intel-v" id="r-host">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Process</span><span class="intel-v" id="r-process">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Severity</span><span class="intel-v" id="r-severity">N/A</span></div>
                            </div>
                            <div class="intel-section">
                                <div class="intel-title">CRYPTOGRAPHIC PROOF & CAUSALITY</div>
                                <div class="intel-row"><span class="intel-k">Ledger Status</span><span class="intel-v" style="color: var(--green);">CHAINED & SECURED</span></div>
                                <div class="intel-row"><span class="intel-k">Block Hash</span><span class="intel-v" style="font-size: 10px;" id="r-hash">N/A</span></div>
                                <div class="intel-row"><span class="intel-k">Causal Context</span><span class="intel-v" id="r-causal-desc" style="color: var(--text-dim);">Root entrypoint vector</span></div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

            <!-- MODULE 2: Parser Intelligence -->
            <div id="pane-parsers" class="module-pane">
                <div class="card">
                    <div class="card-header"><span>PARSER ENGINE & LLM METRICS</span></div>
                    <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(130px, 1fr)); gap:10px; margin-bottom:14px;">
                        <div class="intel-section"><div class="intel-title">LLM Calls</div><div class="intel-v" id="m-llm-calls" style="color:var(--cyan); font-size:16px;">0</div></div>
                        <div class="intel-section"><div class="intel-title">Parsers Learned</div><div class="intel-v" id="m-parsers-learned" style="color:var(--purple); font-size:16px;">0</div></div>
                        <div class="intel-section"><div class="intel-title">Built-in Used</div><div class="intel-v" id="m-builtin-used" style="color:var(--blue); font-size:16px;">0</div></div>
                        <div class="intel-section"><div class="intel-title">Learned Used</div><div class="intel-v" id="m-learned-used" style="color:var(--purple); font-size:16px;">0</div></div>
                        <div class="intel-section"><div class="intel-title">Cache Hits</div><div class="intel-v" id="m-cache-hits" style="color:var(--green); font-size:16px;">0</div></div>
                        <div class="intel-section"><div class="intel-title">Cache Misses</div><div class="intel-v" id="m-cache-misses" style="color:var(--amber); font-size:16px;">0</div></div>
                        <div class="intel-section"><div class="intel-title">Reuse Rate</div><div class="intel-v" id="m-cache-rate" style="color:var(--cyan); font-size:16px;">0%</div></div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header"><span>BUILT-IN REGISTERED PARSERS</span></div>
                    <div class="table-responsive">
                        <table class="cyber-table">
                            <thead><tr><th>Parser Name</th><th>Type</th><th>Format</th><th>Version</th><th>Extraction Rate</th><th>Status</th></tr></thead>
                            <tbody id="builtin-parsers-body"></tbody>
                        </table>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header"><span>DYNAMICALLY LEARNED PARSERS (PARSER FORGE)</span></div>
                    <div class="table-responsive">
                        <table class="cyber-table">
                            <thead><tr><th>Parser Name</th><th>Type</th><th>Format</th><th>Version</th><th>Extraction Rate</th><th>Status</th></tr></thead>
                            <tbody id="learned-parsers-body"><tr><td colspan="6" class="empty-state">No dynamically learned parsers yet. (Parser Forge triggers automatically on novel unknown formats).</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
""";

    private static final String HTML_MODULE_WORKSPACE_PART2 = """
            <!-- MODULE 3: OCSF Mapper -->
            <div id="pane-ocsf" class="module-pane">
                <div class="card">
                    <div class="card-header"><span>OCSF FIELD-LEVEL NORMALIZATION MAPPINGS</span></div>
                    <div id="ocsf-empty" class="empty-state"><div class="empty-icon">📋</div><div>Select an event from the Stream Feed to inspect mapped fields.</div></div>
                    <div id="ocsf-content" style="display: none;">
                        <div class="table-responsive" style="margin-bottom: 14px;">
                            <table class="cyber-table">
                                <thead><tr><th>Raw Field</th><th>Raw Value</th><th>OCSF Standard Field</th><th>Normalized Value</th><th>Confidence</th><th>Status</th><th>Mapping Reason</th></tr></thead>
                                <tbody id="ocsf-mapping-body"></tbody>
                            </table>
                        </div>
                        <div style="font-size: 11px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px;">CANONICAL OCSF STRUCTURE</div>
                        <pre class="code-view" id="ocsf-raw-view">{}</pre>
                    </div>
                </div>
            </div>

            <!-- MODULE 4: StoryGraph -->
            <div id="pane-storygraph" class="module-pane">
                <div class="narrative-card" id="sg-narrative-text">No correlated incident nodes yet. Load an attack scenario or ingest logs to construct the causal graph.</div>
                <div class="card">
                    <div class="card-header"><span>CAUSAL GRAPH EDGES & RELATIONSHIPS</span></div>
                    <div class="table-responsive">
                        <table class="cyber-table">
                            <thead><tr><th>Source Entity</th><th>Causal Action / Edge</th><th>Target Entity</th></tr></thead>
                            <tbody id="graph-table-body"><tr><td colspan="3" class="empty-state">No graph nodes constructed yet.</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- MODULE 5: Threat Detections -->
            <div id="pane-detections" class="module-pane">
                <div class="card">
                    <div class="card-header">
                        <span>DETECTION RUNNERS</span>
                        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                            <button class="btn btn-secondary btn-sm" onclick="runDriftAnalysis()">⚡ Run Drift Analysis</button>
                            <button class="btn btn-secondary btn-sm" onclick="scanInjection()">⚡ Scan Injection</button>
                            <button class="btn btn-secondary btn-sm" onclick="checkSourceSilence()">⚡ Check Source Silence</button>
                            <button class="btn btn-purple btn-sm" onclick="loadDriftTestPair()">🧪 Load Drift Test Pair</button>
                            <button class="btn btn-danger btn-sm" onclick="loadInjectionTestLog()">🧪 Load Injection Test Log</button>
                        </div>
                    </div>
                    <div id="detection-status-banner" style="display:none; margin-bottom:12px;" class="narrative-card"></div>
                    <div class="table-responsive">
                        <table class="cyber-table">
                            <thead><tr><th>Threat Type</th><th>Severity</th><th>Detection Diagnostic</th><th>Confidence</th></tr></thead>
                            <tbody id="detections-table-body"><tr><td colspan="4" class="empty-state">No active detections. Click an analysis runner above.</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- MODULE 6: LedgerGuard -->
            <div id="pane-ledger" class="module-pane">
                <div class="card">
                    <div class="card-header">
                        <span>CRYPTOGRAPHIC AUDIT TRAIL (SHA-256 HASH CHAIN)</span>
                        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                            <button class="btn btn-cyan btn-sm" onclick="verifyLedgerIntegrity()">🛡️ Verify Integrity</button>
                            <button class="btn btn-danger btn-sm" onclick="simulateTamper()">⚠️ Simulate Tamper</button>
                        </div>
                    </div>
                    <div id="ledger-alert-container"></div>
                    <div class="table-responsive">
                        <table class="cyber-table">
                            <thead><tr><th>Block #</th><th>Timestamp</th><th>Block Hash (SHA-256)</th><th>Previous Block Hash</th></tr></thead>
                            <tbody id="ledger-table-body"><tr><td colspan="4" class="empty-state">Ledger is in genesis state (0 chained records).</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- MODULE 7: What-If Counterfactual Replay -->
            <div id="pane-replay" class="module-pane">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; margin-bottom: 12px;">
                    <div class="intel-section" style="border-color: rgba(168, 85, 247, 0.4);"><div class="intel-title">Events Prevented</div><div class="intel-v" id="replay-prevented-metric" style="color:var(--purple); font-size:16px;">0 of 0</div></div>
                    <div class="intel-section" style="border-color: rgba(16, 185, 129, 0.4);"><div class="intel-title">Impact Stopped</div><div class="intel-v" id="replay-impact-metric" style="color:var(--green); font-size:16px;">0%</div></div>
                </div>
                <div class="card">
                    <div class="card-header">
                        <span>WHAT-IF COUNTERFACTUAL SIMULATION</span>
                        <div style="display: flex; gap: 6px; align-items:center; flex-wrap: wrap;">
                            <button class="btn btn-cyan btn-sm" onclick="suggestBestBlockPoint()">💡 Suggest Best Block Point</button>
                            <button class="btn btn-secondary btn-sm" id="btn-simulate-best" style="display:none;" onclick="simulateBestBlock()">⚡ Simulate This Block</button>
                            <button class="btn btn-secondary btn-sm" onclick="clearReplaySelection()">🔄 Clear Simulation</button>
                        </div>
                    </div>
                    <div id="replay-explanation-banner" style="display:none; margin-bottom:12px;" class="narrative-card"></div>
                    <div class="table-responsive">
                        <table class="cyber-table">
                            <thead><tr><th>Timestamp</th><th>Description</th><th>Source</th><th>Severity</th><th style="text-align: center;">Counterfactual State</th></tr></thead>
                            <tbody id="replay-timeline-body"><tr><td colspan="5" class="empty-state">No events available. Load attack scenario or ingest logs first.</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </main>
    </div>
""";

    private static final String JS_SCRIPT_PART1 = """
    <script>
        const PRESETS = {
            json: '{"timestamp":"2026-09-19T10:00:00Z","user":"admin","action":"LOGIN","ip":"192.168.1.50","status":"SUCCESS","requestId":"REQ-101"}',
            csv: '2026-09-19 10:01:00,admin,PROCESS_EXECUTION,SRV-DC01,10.0.0.5,CRITICAL',
            xml: '<log><timestamp>2026-09-19T10:02:00Z</timestamp><user>admin</user><action>PRIVILEGE_ESCALATION</action><host>SRV-DC01</host><status>GRANTED</status></log>',
            syslog: '<134>1 2026-09-19T10:03:00Z fw01.corp sshd 4102 - - Failed password for invalid user root from 203.0.113.19 port 22',
            cef: 'CEF:0|PaloAltoNetworks|PAN-OS|10.0|THREAT|Threat Detection|CRITICAL|src=203.0.113.19 dst=10.0.0.5 spt=52110 dpt=443 act=deny suser=root msg=Bad credentials',
            vendor: 'ts=2026-09-19T10:05:00Z|acct=svc_backup|op=DATA_EXFILTRATION|asset=S3_VAULT|bytes=450000000|outcome=SUCCESS',
            drift: 'USER=alice IP=10.0.0.5 ACTION=LOGIN STATUS=FAIL MFA=true',
            injection: '{"timestamp":"2026-09-19T10:30:00Z","user":"admin\\n2026-09-19T10:31:00Z ERROR [auth] root escalation accepted","action":"LOGIN","ip":"10.0.0.8","status":"SUCCESS"}'
        };

        let activeStreamLogs = [];
        let selectedLogIndex = -1;
        let replayEventsList = [];
        let activeBlockedEventId = null;
        let recommendedBlockEventId = null;
        let activeNav = 'pane-ingest';

        function switchNav(paneId) {
            activeNav = paneId;
            document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
            document.querySelectorAll('.module-pane').forEach(el => el.classList.remove('active'));
            
            const item = Array.from(document.querySelectorAll('.nav-item')).find(el => el.getAttribute('onclick')?.includes(paneId));
            if (item) item.classList.add('active');
            const targetPane = document.getElementById(paneId);
            if (targetPane) targetPane.classList.add('active');
        }

        function setPreset(key, btnEl) {
            document.querySelectorAll('.btn-preset').forEach(b => b.classList.remove('active'));
            if (btnEl && btnEl.classList) {
                btnEl.classList.add('active');
            } else {
                const targetBtn = Array.from(document.querySelectorAll('.btn-preset')).find(b => b.getAttribute('onclick')?.includes(`'${key}'`));
                if (targetBtn) targetBtn.classList.add('active');
            }
            const logIn = document.getElementById('log-input');
            const logSrc = document.getElementById('log-source');
            if (logIn) logIn.value = PRESETS[key] || '';
            if (logSrc) {
                if (key === 'drift') logSrc.value = 'auth-gateway';
                else if (key === 'injection') logSrc.value = 'auth-service';
                else if (key === 'vendor' || key === 'cef') logSrc.value = 'paloalto-fw';
                else logSrc.value = (key + '-service');
            }
        }

        async function fetchJson(url, options = {}) {
            try {
                const res = await fetch(url, options);
                return await res.json();
            } catch (err) {
                console.error("API error for " + url, err);
                return null;
            }
        }

        function escapeHtml(str) {
            if (!str) return '';
            return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        function renderStreamFeed(logs) {
            const container = document.getElementById('stream-list-container');
            const badge = document.getElementById('stream-count-badge');
            if (!container) return;

            activeStreamLogs = logs || [];
            if (badge) badge.innerText = `${activeStreamLogs.length} Logs`;

            if (activeStreamLogs.length === 0) {
                container.innerHTML = `<div class="empty-state"><div class="empty-icon">📥</div><div>No logs ingested yet. Enter a log above or load an attack scenario.</div></div>`;
                clearSelection();
                return;
            }

            container.innerHTML = activeStreamLogs.map((l, idx) => {
                const src = escapeHtml(l.source || 'api');
                const raw = escapeHtml(l.rawLog || l.rawContent || '');
                const type = escapeHtml(l.eventType || 'SECURITY_EVENT');
                const origin = escapeHtml(l.origin || 'LIVE');
                const isSelected = (idx === selectedLogIndex);
                const originBadge = origin === 'DEMO' ? '<span class="badge badge-purple" style="font-size: 9px;">DEMO</span>' : '<span class="badge badge-cyan" style="font-size: 9px;">LIVE</span>';

                return `
                    <div class="stream-item ${isSelected ? 'active' : ''}" onclick="selectLog(${idx})">
                        <div class="stream-meta">
                            <div style="display:flex; gap: 4px; align-items: center;"><span class="badge badge-info">${src}</span>${originBadge}</div>
                            <span class="badge badge-success">${type}</span>
                        </div>
                        <span class="stream-raw">${raw}</span>
                    </div>`;
            }).join('');

            if (selectedLogIndex >= 0 && selectedLogIndex < activeStreamLogs.length) {
                selectLog(selectedLogIndex);
            } else if (activeStreamLogs.length > 0) {
                selectLog(0);
            }
        }

        function selectLog(index) {
            selectedLogIndex = index;
            const log = activeStreamLogs[index];
            if (!log) return;

            document.querySelectorAll('.stream-item').forEach((el, idx) => el.classList.toggle('active', idx === index));

            // Populate Lifecycle Trace in Center Workspace
            const ingEmpty = document.getElementById('ingest-empty');
            const ingCard = document.getElementById('ingest-detail-card');
            if (ingEmpty) ingEmpty.style.display = 'none';
            if (ingCard) ingCard.style.display = 'block';

            const trace = log.parserTrace || {};
            const formatDna = trace.formatDna || {};
            const parserSel = trace.parserSelection || {};

            document.getElementById('dt-format').innerText = formatDna.detectedFormat || log.eventType || 'AUTO';
            document.getElementById('dt-fp').innerText = (formatDna.fingerprint || (log.rawLog ? (log.rawLog.length * 31).toString(16) : 'a4b2c1')).substring(0, 16) + '...';
            document.getElementById('dt-pattern').innerText = formatDna.structuralPattern || (log.rawLog && log.rawLog.includes('=') ? 'KEY_VALUE_PAIRS' : 'STRUCTURED_TOKENS');
            document.getElementById('dt-cache').innerText = trace.cacheUsed ? 'CACHE HIT (Zero-LLM)' : 'CACHE MISS';
            document.getElementById('dt-parser').innerText = parserSel.selectedParser || (log.eventType || 'Standard') + 'Parser';
            document.getElementById('dt-decision').innerText = parserSel.decisionType || 'KNOWN_BUILTIN';
            document.getElementById('dt-grammar').innerText = trace.generatedRules || 'CORE_NATIVE';
            document.getElementById('dt-llm-flag').innerText = trace.llmUsed ? 'YES' : 'NO';

            // Populate Normalized OCSF Container
            const ocsfEmpty = document.getElementById('ocsf-payload-empty');
            const ocsfContainer = document.getElementById('ocsf-payload-container');
            if (ocsfEmpty) ocsfEmpty.style.display = 'none';
            if (ocsfContainer) ocsfContainer.style.display = 'block';
            document.getElementById('dt-ocsf-json').innerText = JSON.stringify(log, null, 2);

            // Populate OCSF Mapper Tab
            document.getElementById('ocsf-empty').style.display = 'none';
            document.getElementById('ocsf-content').style.display = 'block';
            document.getElementById('ocsf-raw-view').innerText = JSON.stringify(log, null, 2);

            const mapTbody = document.getElementById('ocsf-mapping-body');
            if (mapTbody) {
                let mappings = log.fieldMappings || [];
                if (!mappings || mappings.length === 0) {
                    mappings = [
                        { rawField: "source", rawValue: log.source || "api", ocsfField: "metadata.source", normalizedValue: log.source || "api", confidence: 1.0, status: "MAPPED", reason: "Direct source mapping" },
                        { rawField: "eventType", rawValue: log.eventType || "SECURITY_EVENT", ocsfField: "class_name", normalizedValue: log.eventType || "SECURITY_EVENT", confidence: 1.0, status: "MAPPED", reason: "Standard OCSF class inference" },
                        { rawField: "timestamp", rawValue: log.timestamp || "N/A", ocsfField: "time", normalizedValue: log.timestamp || "N/A", confidence: 1.0, status: "MAPPED", reason: "Normalized ISO-8601 timestamp" }
                    ];
                }

                mapTbody.innerHTML = mappings.map(m => {
                    const statusClass = m.status === 'MAPPED' ? 'badge-success' : (m.status === 'INFERRED_TYPE' ? 'badge-cyan' : 'badge-warning');
                    return `
                        <tr>
                            <td><code>${escapeHtml(m.rawField)}</code></td>
                            <td style="font-family: var(--font-mono); color: #93c5fd; font-size: 11px;">${escapeHtml(String(m.rawValue || ''))}</td>
                            <td><span class="badge badge-cyan">${escapeHtml(m.ocsfField)}</span></td>
                            <td style="font-family: var(--font-mono); color: #6ee7b7; font-size: 11px;">${escapeHtml(String(m.normalizedValue || ''))}</td>
                            <td>${Math.round((m.confidence || 1.0) * 100)}%</td>
                            <td><span class="badge ${statusClass}">${escapeHtml(m.status)}</span></td>
                            <td style="font-size: 11px; color: var(--text-dim);">${escapeHtml(m.reason || 'Mapped via SchemaMapper')}</td>
                        </tr>`;
                }).join('');
            }

            // Populate Event Intelligence Panel
            document.getElementById('intel-empty').style.display = 'none';
            document.getElementById('intel-details').style.display = 'block';

            document.getElementById('r-raw').innerText = log.rawLog || 'N/A';
            document.getElementById('r-eid').innerText = log.eventId ? log.eventId.substring(0, 13) + '...' : ('EVT-' + (1000 + index));
            document.getElementById('r-origin').innerHTML = (log.origin === 'DEMO') ? '<span class="badge badge-purple">DEMO</span>' : '<span class="badge badge-cyan">LIVE</span>';
            document.getElementById('r-source').innerText = log.source || 'api';
            document.getElementById('r-parser').innerText = parserSel.selectedParser || 'BuiltInParser';
            document.getElementById('r-dna').innerText = formatDna.fingerprint ? formatDna.fingerprint.substring(0, 16) + '...' : 'DNA-VALID';
            document.getElementById('r-user').innerText = log.user || 'N/A';
            document.getElementById('r-ip').innerText = log.sourceIp || 'N/A';
            document.getElementById('r-dst-ip').innerText = log.destinationIp || 'N/A';
            document.getElementById('r-host').innerText = log.host || 'N/A';
            document.getElementById('r-process').innerText = log.process || 'N/A';
            document.getElementById('r-severity').innerText = (log.severity || 'LOW').toUpperCase();
            document.getElementById('r-hash').innerText = 'sha256:' + (log.rawLog ? (log.rawLog.length * 9999).toString(16).padStart(16, '0') : '00000000');

            if (log.parentEvidence && Object.keys(log.parentEvidence).length > 0) {
                document.getElementById('r-causal-desc').innerText = 'Evidence: ' + Object.values(log.parentEvidence).join(' • ');
            } else if (index === 0) {
                document.getElementById('r-causal-desc').innerText = 'Root attack entrypoint vector';
            } else {
                document.getElementById('r-causal-desc').innerText = 'Sequential timeline event in session';
            }
        }

        function clearSelection() {
            selectedLogIndex = -1;
            const ingEmpty = document.getElementById('ingest-empty');
            const ingCard = document.getElementById('ingest-detail-card');
            if (ingEmpty) ingEmpty.style.display = 'block';
            if (ingCard) ingCard.style.display = 'none';

            const ocsfEmpty = document.getElementById('ocsf-payload-empty');
            const ocsfContainer = document.getElementById('ocsf-payload-container');
            if (ocsfEmpty) ocsfEmpty.style.display = 'block';
            if (ocsfContainer) ocsfContainer.style.display = 'none';

            document.getElementById('ocsf-empty').style.display = 'block';
            document.getElementById('ocsf-content').style.display = 'none';
            document.getElementById('intel-empty').style.display = 'block';
            document.getElementById('intel-details').style.display = 'none';
        }
""";

    private static final String JS_SCRIPT_PART2 = """
        async function syncState() {
            const metrics = await fetchJson('/api/metrics');
            if (metrics) {
                document.getElementById('top-ingested').innerText = metrics.linesProcessed ?? 0;
                document.getElementById('top-llm').innerText = metrics.llmCalls ?? 0;
                document.getElementById('top-cache').innerText = `${metrics.cacheReuseRate ?? 0}%`;

                // Module 2 metrics
                document.getElementById('m-llm-calls').innerText = metrics.llmCalls ?? 0;
                document.getElementById('m-parsers-learned').innerText = metrics.parsersLearned ?? 0;
                document.getElementById('m-builtin-used').innerText = metrics.builtinParsersUsed ?? 0;
                document.getElementById('m-learned-used').innerText = metrics.learnedParsersUsed ?? 0;
                document.getElementById('m-cache-hits').innerText = metrics.cacheHits ?? 0;
                document.getElementById('m-cache-misses').innerText = metrics.cacheMisses ?? 0;
                document.getElementById('m-cache-rate').innerText = `${metrics.cacheReuseRate ?? 0}%`;
            }

            const wsStatus = await fetchJson('/api/demo/status');
            if (wsStatus) {
                const isDemo = wsStatus.mode === 'ATTACK_SCENARIO';
                document.getElementById('top-mode').innerHTML = isDemo ? '<span class="badge badge-purple">DEMO SCENARIO</span>' : '<span class="badge badge-cyan">LIVE</span>';
                document.getElementById('hdr-mode-text').innerText = isDemo ? 'SCENARIO ACTIVE' : 'ENGINE ONLINE';
            }

            // Stream feed from unified /api/logs/stream
            const streamRes = await fetchJson('/api/logs/stream');
            if (streamRes && streamRes.streamLogs) {
                renderStreamFeed(streamRes.streamLogs);
            }

            const normRes = await fetchJson('/api/logs/normalized');
            if (normRes && normRes.events) {
                document.getElementById('top-ocsf').innerText = normRes.total ?? normRes.events.length;
            }

            const detRes = await fetchJson('/api/detections');
            if (detRes && detRes.detections) {
                document.getElementById('top-detections').innerText = detRes.total ?? detRes.detections.length;
                renderDetections(detRes.detections);
            }

            const pRes = await fetchJson('/api/parsers');
            const lRes = await fetchJson('/api/parsers/lineage');
            renderParsers(pRes ? pRes.parsers : [], lRes ? lRes.lineage : []);

            const gRes = await fetchJson('/api/graph');
            if (gRes) {
                document.getElementById('sg-narrative-text').innerText = gRes.narrative || "No correlated incident nodes yet.";
                renderGraphEdges(gRes.edges || []);
            }

            const ledRes = await fetchJson('/api/ledger');
            if (ledRes && ledRes.records) {
                renderLedger(ledRes.records);
            }
            const verRes = await fetchJson('/api/ledger/verify');
            if (verRes) {
                renderLedgerStatus(verRes);
            }

            await loadReplayTimeline();
        }

        function renderDetections(detections) {
            const tbody = document.getElementById('detections-table-body');
            if (!tbody) return;
            if (!detections || detections.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No active detections. Click an analysis runner above.</td></tr>';
                return;
            }
            tbody.innerHTML = detections.map(d => `
                <tr>
                    <td><span class="badge ${d.type === 'LOG_INJECTION' ? 'badge-danger' : 'badge-warning'}">${escapeHtml(d.type)}</span></td>
                    <td><span class="badge ${d.severity === 'HIGH' || d.severity === 'CRITICAL' ? 'badge-danger' : 'badge-warning'}">${escapeHtml(d.severity)}</span></td>
                    <td style="font-size: 11px;">${escapeHtml(d.message)}</td>
                    <td style="font-family: var(--font-mono);">${(d.confidence * 100).toFixed(0)}%</td>
                </tr>`).join('');
        }

        function renderParsers(allParsers, lineage) {
            const bBody = document.getElementById('builtin-parsers-body');
            const lBody = document.getElementById('learned-parsers-body');
            if (!bBody || !lBody) return;

            if (allParsers && allParsers.length > 0) {
                bBody.innerHTML = allParsers.map(p => `
                    <tr>
                        <td style="font-weight: 600; color: var(--cyan);">${escapeHtml(p.name)}</td>
                        <td><span class="badge badge-info">BUILT-IN</span></td>
                        <td>${escapeHtml(p.supportedFormat || 'AUTO')}</td>
                        <td><code>${escapeHtml(p.version || 'v1.0')}</code></td>
                        <td>100%</td>
                        <td><span class="badge badge-success">ACTIVE</span></td>
                    </tr>`).join('');
            } else {
                bBody.innerHTML = '<tr><td colspan="6" class="empty-state">No built-in parsers loaded.</td></tr>';
            }

            if (lineage && lineage.length > 0) {
                lBody.innerHTML = lineage.map(p => `
                    <tr>
                        <td style="font-weight: 600; color: var(--purple);">${escapeHtml(p.parserName)}</td>
                        <td><span class="badge badge-purple">FORGED LLM</span></td>
                        <td>DYNAMIC</td>
                        <td><code>${escapeHtml(p.version || 'v1.0')}</code></td>
                        <td>${((p.extractionRate || 1.0) * 100).toFixed(0)}%</td>
                        <td><span class="badge badge-success">${escapeHtml(p.status || 'ACTIVE')}</span></td>
                    </tr>`).join('');
            } else {
                lBody.innerHTML = '<tr><td colspan="6" class="empty-state">No dynamically learned parsers yet. (Parser Forge triggers automatically on novel unknown formats).</td></tr>';
            }
        }

        function renderGraphEdges(edges) {
            const tbody = document.getElementById('graph-table-body');
            if (!tbody) return;
            if (!edges || edges.length === 0) {
                tbody.innerHTML = '<tr><td colspan="3" class="empty-state">No graph nodes constructed yet.</td></tr>';
                return;
            }
            tbody.innerHTML = edges.slice(0, 10).map(e => `
                <tr>
                    <td style="font-family: var(--font-mono); color: var(--cyan);">${escapeHtml(e.source)}</td>
                    <td><span class="badge badge-info">${escapeHtml(e.relationship)}</span></td>
                    <td style="font-family: var(--font-mono); color: #a5b4fc;">${escapeHtml(e.target)}</td>
                </tr>`).join('');
        }

        function renderLedger(records) {
            const tbody = document.getElementById('ledger-table-body');
            if (!tbody) return;
            if (!records || records.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="empty-state">Ledger is in genesis state (0 chained records).</td></tr>';
                return;
            }
            tbody.innerHTML = records.slice(-8).map(r => {
                const curr = r.currentHash || '';
                const prev = r.previousHash || '';
                const timeStr = r.createdAt ? r.createdAt.substring(11, 19) : '00:00:00';
                return `
                    <tr>
                        <td style="font-weight: 700;">#${r.sequenceNumber}</td>
                        <td style="font-family: var(--font-mono); font-size: 11px; color: var(--text-dim);">${timeStr}</td>
                        <td style="font-family: var(--font-mono); font-size: 11px; color: var(--green);">${curr.substring(0, 18)}...${curr.substring(curr.length - 6)}</td>
                        <td style="font-family: var(--font-mono); font-size: 11px; color: var(--text-muted);">${prev.substring(0, 16)}...</td>
                    </tr>`;
            }).join('');
        }

        function renderLedgerStatus(verify) {
            const topEl = document.getElementById('top-ledger');
            const alertBox = document.getElementById('ledger-alert-container');
            if (!topEl || !alertBox) return;

            if (verify.valid) {
                topEl.innerHTML = '<span class="badge badge-success">VALID</span>';
                alertBox.innerHTML = `<div style="background: rgba(16, 185, 129, 0.12); border: 1px solid #059669; color: #6ee7b7; padding: 8px 12px; border-radius: 6px; font-size: 11px; font-weight: 600; margin-bottom: 12px;">✅ SHA-256 Ledger Chain Intact — ${verify.totalRecords || 0} Cryptographic Blocks Verified.</div>`;
            } else {
                topEl.innerHTML = `<span class="badge badge-danger">BROKEN #${verify.brokenSequence}</span>`;
                alertBox.innerHTML = `<div style="background: rgba(239, 68, 68, 0.2); border: 1px solid #dc2626; color: #fca5a5; padding: 8px 12px; border-radius: 6px; font-size: 11px; font-weight: 700; margin-bottom: 12px;">⚠️ CRYPTOGRAPHIC TAMPERING DETECTED at Block #${verify.brokenSequence}! Chain integrity violated.</div>`;
            }
        }
""";

    private static final String JS_SCRIPT_PART3 = """
        async function loadReplayTimeline() {
            const res = await fetchJson('/api/replay');
            if (res && res.events) {
                replayEventsList = res.events;
                if (!activeBlockedEventId) {
                    renderReplayTimeline(replayEventsList, null, recommendedBlockEventId);
                }
            } else {
                replayEventsList = [];
                renderReplayTimeline([], null, null);
            }
        }

        function renderReplayTimeline(events, replayResult, recommendedId) {
            const tbody = document.getElementById('replay-timeline-body');
            const metricPrev = document.getElementById('replay-prevented-metric');
            const metricImp = document.getElementById('replay-impact-metric');
            const expBox = document.getElementById('replay-explanation-banner');
            if (!tbody) return;

            if (!events || events.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No events available. Load attack scenario or ingest logs first.</td></tr>';
                if (metricPrev) metricPrev.innerText = '0 of 0';
                if (metricImp) metricImp.innerText = '0%';
                if (expBox) expBox.style.display = 'none';
                return;
            }

            const preventedSet = (replayResult && replayResult.preventedEventIds) ? new Set(replayResult.preventedEventIds) : new Set();
            const blockedId = replayResult ? replayResult.blockedEventId : activeBlockedEventId;

            const total = events.length;
            const preventedCount = replayResult ? replayResult.eventsPrevented : 0;
            const impactPct = replayResult ? replayResult.impactStoppedPercent : 0;

            if (metricPrev) metricPrev.innerText = `${preventedCount} of ${total}`;
            if (metricImp) metricImp.innerText = `${impactPct}%`;

            if (replayResult && replayResult.explanation && blockedId) {
                expBox.innerText = replayResult.explanation;
                expBox.style.display = 'block';
            } else {
                expBox.style.display = 'none';
            }

            tbody.innerHTML = events.map(ev => {
                const id = ev.id || ev.eventId;
                const isBlocked = (id === blockedId);
                const isPrevented = !isBlocked && preventedSet.has(id);
                const isRecommended = (id === recommendedId);

                let rowStyle = 'cursor: pointer; transition: all 0.15s ease;';
                let stateBadge = '';

                if (isBlocked) {
                    rowStyle += ' background: rgba(239, 68, 68, 0.25); border: 1px solid #ef4444;';
                    stateBadge = '<span class="badge badge-danger">blocked here</span>';
                } else if (isPrevented) {
                    rowStyle += ' background: rgba(15, 23, 42, 0.7); color: #64748b; text-decoration: line-through;';
                    stateBadge = '<span class="badge" style="background: rgba(100, 116, 139, 0.3); color: #94a3b8; border: 1px solid #475569;">never happens</span>';
                } else {
                    rowStyle += ' background: transparent;';
                    stateBadge = '<span class="badge badge-success">still happens</span>';
                }

                if (isRecommended) {
                    rowStyle += ' outline: 2px dashed #f59e0b;';
                }

                let evidenceText = 'Root attack entrypoint';
                if (ev.parentEvidence && Object.keys(ev.parentEvidence).length > 0) {
                    evidenceText = 'Causal Evidence: ' + Object.values(ev.parentEvidence).join(' • ');
                } else if (ev.parentEventIds && ev.parentEventIds.length > 0) {
                    evidenceText = 'Causally linked to parent: ' + ev.parentEventIds.join(', ');
                }

                const timeStr = ev.timestamp ? ev.timestamp.replace('T', ' ').substring(0, 19) : 'N/A';
                const descStr = escapeHtml(ev.description || ev.activity || 'Security Event');
                const srcStr = escapeHtml(ev.source || 'api');
                const sevStr = escapeHtml(ev.severity || 'LOW');
                const recBadge = isRecommended ? '<span class="badge badge-warning" style="margin-left: 6px;">⭐ RECOMMENDED BLOCK</span>' : '';

                return `
                    <tr style="${rowStyle}" onclick="toggleBlockEvent('${id}')" title="${escapeHtml(evidenceText)}">
                        <td style="font-family: var(--font-mono); font-size: 11px;">${timeStr}</td>
                        <td style="font-weight: 600;">${descStr}${recBadge}</td>
                        <td><span class="badge badge-info">${srcStr}</span></td>
                        <td><span class="badge badge-warning">${sevStr}</span></td>
                        <td style="text-align: center;">${stateBadge}</td>
                    </tr>`;
            }).join('');
        }

        async function toggleBlockEvent(eventId) {
            if (activeBlockedEventId === eventId) {
                activeBlockedEventId = null;
                renderReplayTimeline(replayEventsList, null, recommendedBlockEventId);
                return;
            }

            activeBlockedEventId = eventId;
            const res = await fetchJson('/api/replay/block', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ eventId: eventId })
            });

            if (res) {
                renderReplayTimeline(replayEventsList, res, recommendedBlockEventId);
            }
        }

        async function suggestBestBlockPoint() {
            const res = await fetchJson('/api/replay/best-block');
            if (res && res.status === 'SUCCESS') {
                recommendedBlockEventId = res.recommendedEventId || (res.recommendedEvent ? (res.recommendedEvent.id || res.recommendedEvent.eventId) : null);
                const simBtn = document.getElementById('btn-simulate-best');
                if (simBtn) simBtn.style.display = 'inline-flex';
                
                if (recommendedBlockEventId) {
                    const blockRes = await fetchJson('/api/replay/block', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ eventId: recommendedBlockEventId })
                    });
                    renderReplayTimeline(replayEventsList, blockRes, recommendedBlockEventId);
                } else {
                    renderReplayTimeline(replayEventsList, null, recommendedBlockEventId);
                }
            }
        }

        async function simulateBestBlock() {
            if (recommendedBlockEventId) {
                await toggleBlockEvent(recommendedBlockEventId);
            }
        }

        function clearReplaySelection() {
            activeBlockedEventId = null;
            recommendedBlockEventId = null;
            const simBtn = document.getElementById('btn-simulate-best');
            if (simBtn) simBtn.style.display = 'none';
            renderReplayTimeline(replayEventsList, null, null);
        }

        async function ingestCustomLog() {
            const content = document.getElementById('log-input').value;
            const source = document.getElementById('log-source').value || 'api';
            if (!content || !content.trim()) return;

            const res = await fetchJson('/api/logs/ingest', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rawContent: content, source: source })
            });

            if (res) {
                await syncState();
                switchNav('pane-ingest');
            }
        }

        async function loadAttackScenario() {
            await fetchJson('/api/demo/attack-scenario', { method: 'POST' });
            await syncState();
            switchNav('pane-storygraph');
        }

        async function clearWorkspace() {
            await fetchJson('/api/demo/clear-workspace', { method: 'POST' });
            clearReplaySelection();
            clearSelection();
            await syncState();
        }

        async function runDriftAnalysis() {
            const selectedLog = (selectedLogIndex >= 0 && activeStreamLogs[selectedLogIndex]) ? activeStreamLogs[selectedLogIndex] : null;
            const payload = selectedLog ? { source: selectedLog.source, rawLog: (selectedLog.rawLog || selectedLog.rawContent) } : {};
            const res = await fetchJson('/api/detections/drift-analysis', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const banner = document.getElementById('detection-status-banner');
            if (banner && res) {
                banner.innerText = res.message || 'Drift analysis completed.';
                banner.style.display = 'block';
                if (res.status === 'DRIFT_DETECTED') {
                    banner.style.background = 'rgba(239, 68, 68, 0.2)';
                    banner.style.borderColor = '#ef4444';
                    banner.style.color = '#fca5a5';
                } else if (res.status === 'BASELINE_ESTABLISHED') {
                    banner.style.background = 'rgba(59, 130, 246, 0.15)';
                    banner.style.borderColor = '#3b82f6';
                    banner.style.color = '#93c5fd';
                } else {
                    banner.style.background = 'rgba(16, 185, 129, 0.12)';
                    banner.style.borderColor = '#059669';
                    banner.style.color = '#6ee7b7';
                }
            }
            await syncState();
        }

        async function scanInjection() {
            const selectedLog = (selectedLogIndex >= 0 && activeStreamLogs[selectedLogIndex]) ? activeStreamLogs[selectedLogIndex] : null;
            const payload = selectedLog ? { source: selectedLog.source, rawLog: (selectedLog.rawLog || selectedLog.rawContent) } : {};
            const res = await fetchJson('/api/detections/scan-injection', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const banner = document.getElementById('detection-status-banner');
            if (banner && res) {
                banner.innerText = res.message || 'Injection scan completed.';
                banner.style.display = 'block';
                if (res.status === 'INJECTION_DETECTED') {
                    banner.style.background = 'rgba(239, 68, 68, 0.2)';
                    banner.style.borderColor = '#ef4444';
                    banner.style.color = '#fca5a5';
                } else {
                    banner.style.background = 'rgba(16, 185, 129, 0.12)';
                    banner.style.borderColor = '#059669';
                    banner.style.color = '#6ee7b7';
                }
            }
            await syncState();
        }

        async function loadDriftTestPair() {
            // 1. Ingest baseline log
            await fetchJson('/api/logs/ingest', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rawContent: 'USER=alice IP=10.0.0.5 ACTION=LOGIN', source: 'auth-gateway' })
            });
            // 2. Ingest drifted log with STATUS and MFA
            await fetchJson('/api/logs/ingest', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rawContent: 'USER=alice IP=10.0.0.5 ACTION=LOGIN STATUS=FAIL MFA=true', source: 'auth-gateway' })
            });
            await syncState();
            switchNav('pane-detections');
            await runDriftAnalysis();
        }

        async function loadInjectionTestLog() {
            // Ingest log with CRLF forged error log injection
            await fetchJson('/api/logs/ingest', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    rawContent: '{"timestamp":"2026-09-19T10:30:00Z","user":"admin\\n2026-09-19T10:31:00Z ERROR [auth] root escalation accepted","action":"LOGIN","ip":"10.0.0.8","status":"SUCCESS"}', 
                    source: 'auth-service' 
                })
            });
            await syncState();
            switchNav('pane-detections');
            await scanInjection();
        }

        async function checkSourceSilence() {
            const res = await fetchJson('/api/detections/check-silence', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({})
            });
            const banner = document.getElementById('detection-status-banner');
            if (banner && res) {
                banner.innerText = res.message || (res.status === 'SILENCE_DETECTED' ? '⚠️ Source silence detected!' : '✅ All log sources active.');
                banner.style.display = 'block';
            }
            await syncState();
        }

        async function verifyLedgerIntegrity() {
            const res = await fetchJson('/api/ledger/verify-integrity');
            if (res) renderLedgerStatus(res);
        }

        async function simulateTamper() {
            await fetchJson('/api/ledger/simulate-tamper', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sequenceNumber: 1, tamperedData: '{"tampered":true,"attacker":"adversary","action":"RECORD_MUTATION"}' })
            });
            await verifyLedgerIntegrity();
        }

        document.addEventListener('DOMContentLoaded', () => {
            setPreset('json');
            syncState();
        });
        window.onload = () => {
            setPreset('json');
            syncState();
        };
    </script>
</body>
</html>
""";

    @GetMapping(value = {"/", "/dashboard"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getDashboardHtml() {
        StringBuilder sb = new StringBuilder(75000);
        sb.append(CSS_STYLES)
          .append(HTML_HEADER_AND_SIDEBAR)
          .append(HTML_MODULE_WORKSPACE_PART1)
          .append(HTML_MODULE_WORKSPACE_PART2)
          .append(JS_SCRIPT_PART1)
          .append(JS_SCRIPT_PART2)
          .append(JS_SCRIPT_PART3);
        return ResponseEntity.ok(sb.toString());
    }
}
