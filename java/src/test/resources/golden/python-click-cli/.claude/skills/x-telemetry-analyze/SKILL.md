---
name: x-telemetry-analyze
description: "Analyze telemetry NDJSON for one or more epics and produce a Markdown report with skill/phase/tool aggregates, Mermaid Gantt timeline, and optional JSON/CSV exports. Use to answer 'which phase is the bottleneck?' and 'is skill X getting slower?' questions for operator visibility."
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "--epic EPIC-XXXX | --epics A,B [--export json|csv --out path] [--since YYYY-MM-DD]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Telemetry Analyze

## Purpose

Transform the append-only NDJSON telemetry logs produced by the
`telemetry-*.sh` hooks into an actionable report. The analysis answers two
recurring operator questions:

1. **Which skill/phase is the bottleneck for epic X?** Per-skill and per-phase
   tables with total duration, average, P50, P95.
2. **How does skill performance change across epics?** Cross-epic comparison
   via `--epics` (tables stay aligned by epic id).

The report is written to
`plans/epic-XXXX/reports/telemetry-report-EPIC-XXXX.md` by default and
follows the layout in `_TEMPLATE-TELEMETRY-REPORT.md`.

## When to Use

- Before a retro to see where a workflow cycle spent its time.
- When a skill "feels slower" and you want evidence.
- Before a refactor to establish a baseline.

## Arguments

| Argument | Behaviour |
| :--- | :--- |
| `--epic EPIC-XXXX` | Report for a single epic (tables + Gantt). |
| `--epics A,B,C` | Cross-epic comparison report. |
| `--export json --out path` | Write the structured JSON schema (§5.1). |
| `--export csv --out path` | Write the tabular CSV (§5.2). |
| `--since YYYY-MM-DD` | Drop events whose timestamp is earlier than the date. |
| `--by-tool` | Emphasize the tool breakdown in the Markdown report. |
| `--base-dir path` | Override `plans/` (useful in tests). |

The default Markdown report lands in `plans/epic-XXXX/reports/` — no extra
flag required.

## Invocation

```bash
java -cp target/classes:target/dependency/* \
     dev.iadev.telemetry.analyze.TelemetryAnalyzeCli \
     --epic EPIC-0040
```

The CLI is intentionally standalone — it does NOT require the full
`ia-dev-env` generator lifecycle. It reads the NDJSON file, aggregates, and
writes exactly one output artifact.

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| 0 | Success. Malformed NDJSON lines are silently skipped (warnings go to stderr via SLF4J). |
| 2 | Epic has no `events.ndjson` — message cites the expected path. |
| 3 | Reserved for catastrophic parse failure (I/O error during the pre-flight probe). In the default append-only NDJSON mode, single malformed lines are skipped rather than aborting the run — the rationale is that telemetry is append-only and a mid-file corruption should not prevent aggregation of the remaining `N-1` events. |
| 4 | `--export` set without `--out`. |

## Performance Contract

- 10 000 events → report in < 5 s (hard SLA, story §3.5).
- 100 000 events → report in < 30 s.

The aggregator streams events through `TelemetryReader.streamSkippingInvalid()`
so memory stays bounded: the largest retained structure is the per-skill
duration list, which holds one `long` per matching event.

## Output

Every Markdown report contains the seven canonical sections declared in
story-0040-0010 §3.2:

1. Header (epics, generated-at, total events).
2. Resumo geral (duration totals + top-5 skills/phases).
3. Por skill (invocations / total / avg / P50 / P95).
4. Por fase (same columns, keyed by `skill/phase`).
5. Por tool (invocations / duration per tool — Bash, Write, Edit, Skill, Agent, MCPs).
6. Mermaid Gantt timeline (capped at 50 rows).
7. Observações (outliers, truncation notes).

The JSON export follows the schema in §5.1; the CSV export follows the
layout in §5.2 with RFC 4180 quoting.
