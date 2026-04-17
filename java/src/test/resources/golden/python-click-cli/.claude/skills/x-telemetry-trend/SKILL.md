---
name: x-telemetry-trend
description: "Detect cross-epic P95 regressions (>= threshold %) and rank top-10 slowest skills from the global telemetry index. Single-responsibility partner of /x-telemetry-analyze focused on trend detection, not point-in-time reporting. Use to answer 'is skill X getting slower over the last N epics?' with evidence."
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--last N] [--threshold-pct P] [--baseline mean|median] [--format md|json] [--out path]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Telemetry Trend

## Purpose

Detect performance regressions across multiple epics using the append-only
NDJSON telemetry logs. `/x-telemetry-trend` complements `/x-telemetry-analyze`:
analyze answers "which phase is the bottleneck in epic X"; trend answers
"is skill X getting slower over the last N epics?".

The skill consumes the global index at `.claude/telemetry/index.json`
(on-demand; gitignored), aggregates P95 per skill per epic, and emits:

1. Top-10 regressions whose P95 delta vs. a moving baseline exceeds the
   configured threshold.
2. Top-10 slowest skills by mean-of-P95 across the analyzed window.
3. An interpretive observations block.

## When to Use

- Periodic performance audit after a batch of merged stories.
- Before a refactor: confirm that a slow skill is trending up (not a fluke).
- In a CI job that should flag sustained regressions across epics.

## Arguments

| Argument | Behaviour |
| :--- | :--- |
| `--last N` | Analyze the most-recent N epics (default 5). |
| `--threshold-pct P` | Minimum delta % to classify as a regression (default 20). |
| `--baseline mean\|median` | Baseline aggregation (default median — outlier-robust). |
| `--format md\|json` | Output format (default md). |
| `--out path` | Destination path. When omitted, writes to stdout (RULE-007). |
| `--base-dir path` | Override `plans/` (test-only). |
| `--index-path path` | Override the cache path (test-only). |
| `--rebuild-index` | Force rebuild ignoring the cache. |

## Invocation

```bash
java -cp target/classes:target/dependency/* \
     dev.iadev.telemetry.trend.TelemetryTrendCli \
     --last 5 --threshold-pct 20 --baseline median
```

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| 0 | Success. Report emitted to stdout or `--out` path. |
| 1 | Validation error (bad `--last`, `--baseline`, or `--format` value). |
| 5 | Fewer than 2 epics with data — trend analysis needs a comparable series. |
| 6 | `--threshold-pct` is negative. |

## Algorithm

1. Build (or refresh) the per-skill per-epic P95 index by scanning
   `plans/epic-*\/telemetry/events.ndjson`. The index is persisted at
   `.claude/telemetry/index.json` and invalidated when an epic's NDJSON
   mtime changes.
2. Restrict to the most-recent `--last N` epics (natural ID order).
3. For each skill, compute the baseline (mean or median) of the N-1 oldest
   samples and compare against the current (latest) sample.
4. If `deltaPct >= threshold`, emit a regression entry; sort regressions by
   deltaPct descending, cap at top-10.
5. Rank skills by mean-of-P95 across the window; cap at top-10.

## Output Layout (Markdown)

1. Header — generated-at, epics analyzed, threshold, baseline.
2. Top-10 regressions table (skill, baseline P95, current P95, delta%, epics).
3. Top-10 slowest skills table (skill, avg P95, invocations).
4. Observations — interpretive notes (regressions count, slowest skill).

## Performance Contract

- 5 epics × 10 000 events → report in < 10 s (hard SLA, story §3.5).

The index builder uses `TelemetryReader.streamSkippingInvalid()` so memory
stays bounded in the per-skill duration arrays.

## Examples

```bash
# Default: last 5 epics, threshold 20 %, baseline median → stdout
/x-telemetry-trend

# Strict: threshold 10 %, mean baseline, write JSON to disk
/x-telemetry-trend --threshold-pct 10 --baseline mean --format json \
    --out plans/epic-0040/reports/trends.json

# Historical deep dive: last 10 epics, report to a custom path
/x-telemetry-trend --last 10 --out reports/quarterly-trends.md
```
