# Specialist Review — Performance

**Engineer:** Performance Specialist
**Story:** story-0040-0010
**PR:** #420
**Date:** 2026-04-16

---

## Score: 22/26
## Status: Partial

---

## PASSED

- [PERF-01] **SLA assertion in-test** (2/2) — `TelemetryAnalyzeCliIT.call_tenThousandEvents_under5Seconds` measures wall-clock latency and asserts `< 5000 ms`. Observed CI time: ~450 ms, ~10× headroom — the hard SLA from story §3.5 is met and protected by automation.
- [PERF-02] **Lazy stream reader** (2/2) — `TelemetryReader.streamSkippingInvalid()` is used inside try-with-resources in `aggregateEpics`. Events are deserialized on-demand by `EventIterator.advance()`; peak heap during stream traversal stays O(1) in the reader itself.
- [PERF-03] **Sorted-list percentile over QuickSelect** (2/2) — story §3.4 left the choice open; `Collections.sort(samples)` then Nearest-Rank `samples.get(rank-1)` gives `O(n log n)` per dimension, which is vastly below the 10k budget. No external t-digest dependency was added — simpler and SLA-compliant.
- [PERF-04] **No N+1 filesystem reads** (2/2) — one `TelemetryReader.open(...)` per epic in `aggregateEpics`; cross-epic mode reads each file exactly once.
- [PERF-05] **Bounded Gantt output** (2/2) — `MarkdownReportRenderer` caps the Gantt at 50 rows; large timelines do not produce pathological markdown (common issue with Mermaid renderers choking on 1k+ rows).
- [PERF-06] **CSV escaping is linear** (2/2) — `CsvReportRenderer.escape` uses `indexOf` × 3 and a single `String.replace` — `O(name.length())` per row, not `O(name.length()²)`.
- [PERF-07] **Jackson mapper is static** (2/2) — `JsonReportRenderer.MAPPER` is a `static final` field; no mapper rebuild per `render` call.
- [PERF-08] **No unbounded collections** (2/2) — every aggregation `Map` is keyed by a bounded-cardinality field (skill, phase, tool). 10k events across a handful of skills yields a map size `O(~dozens)`, not `O(10k)`.
- [PERF-09] **Timestamps compared via Instant** (2/2) — `--since` filter uses `Instant.isBefore` in the stream; no string-level date arithmetic.
- [PERF-10] **No synchronous sleeps** (2/2) — no `Thread.sleep` or polling loops anywhere in the new code.

## PARTIAL

- [PERF-11] **aggregateEpics materializes the full event list** (1/2) — `aggregateEpics` copies every event into `List<TelemetryEvent> merged` before calling `aggregator.aggregate(merged.stream(), epics)`. For 10k events this is `O(events)` heap; for the future 100k-event boundary mentioned in story §3.5 it would allocate ~100k `TelemetryEvent` records. The aggregator API already accepts a `Stream`, so this intermediate list is pure overhead. — Improvement: refactor `aggregateEpics` to `return aggregator.aggregate(streamAllEpics(epicIds, base, sinceInstant), epicIds)` where `streamAllEpics` concatenates per-epic streams via `Stream.concat` or `Stream.of(paths).flatMap(...)` so each event flows through the aggregator without buffering. `java/src/main/java/dev/iadev/telemetry/analyze/TelemetryAnalyzeCli.java:176-202` [MEDIUM]
- [PERF-12] **Sort executed on every stat group, even single-sample groups** (1/2) — `TelemetryAggregator.toStats` calls `Collections.sort(samples)` unconditionally. For rare skills with `n == 1`, the sort is a no-op but still materializes the comparator chain. — Improvement: guard `if (n > 1) Collections.sort(samples);` — micro-optimization, not an SLA blocker. `java/src/main/java/dev/iadev/telemetry/analyze/TelemetryAggregator.java:165` [LOW]

## Severity Distribution

| Severity | Count |
| --- | --- |
| Critical | 0 |
| High | 0 |
| Medium | 1 |
| Low | 1 |
