# Specialist Review — Performance

**Story:** story-0040-0005
**PR:** #413
**Branch:** feat/story-0040-0005-pii-scrubber
**Reviewer:** Performance specialist (inline)
**Date:** 2026-04-16

ENGINEER: Performance
STORY: story-0040-0005
SCORE: 22/26
STATUS: Partial

---

## PASSED

- [PERF-01] Regex patterns compiled once (at class-load time, into `DEFAULT_RULES`) rather than per-invocation — the scrubber's hot path only calls `Pattern.matcher(...)` + `replaceAll(...)` (2/2).
- [PERF-02] No string concatenation in loops inside the scrubber or audit; no `String +` in log messages (uses SLF4J parameterised logging) (2/2).
- [PERF-03] `TelemetryScrubber.scrub` allocates ONE new event per call, plus one new metadata map when metadata is non-empty — allocation profile is bounded (2/2).
- [PERF-04] Metadata scrub skips entirely when metadata is null/empty (`scrubMetadata` line 190) — avoids LinkedHashMap construction on the common path (2/2).
- [PERF-05] `scrubString(null)` fast-paths to `return null` before iterating rules (2/2).
- [PERF-06] `PiiAudit.scanFile` iterates the file line-by-line via `Files.readAllLines` — no backtracking across lines and no whole-file regex (2/2).
- [PERF-07] Whitelist lookup is `Set.contains` on an immutable `Set.copyOf` (hash-backed for String) — O(1) per metadata entry (2/2).
- [PERF-08] No synchronisation / locks introduced; scrubber instances are effectively immutable and thread-safe (2/2).
- [PERF-09] Rules use `List.copyOf` to defensively snapshot the pipeline — one-time cost at construction, zero cost at scrub-time (2/2).
- [PERF-10] The fuzz test runs 100 parametrized iterations in ~0.1s elapsed (mvn surefire report) — scrub overhead per event is well under the 3ms DoD target (back-of-envelope: 1ms per 100 ≈ 10μs per event) (2/2).

## PARTIAL

- [PERF-11] No explicit JMH benchmark proving the `p99 < 3ms per event` DoD claim. Surefire wall-clock timing (0.16s for 100 entries) is a reasonable proxy but does not establish a hard SLA (1/2) [LOW] — **Fix:** Add a micro-benchmark (JMH or simple `System.nanoTime()` loop) asserting that scrubbing 10k events completes under 30s, or document the DoD as informational.

- [PERF-12] Each scrub pass iterates the ENTIRE rule list on every string, even when the first rule already replaced everything. For the common-case clean event (the overwhelming majority) this means 8 regex `find` probes per string field × 4 string fields = 32 probes per event. Acceptable at current volume but worth noting for when telemetry volume grows (1/2) [LOW] — **Fix:** Add an early-exit heuristic (e.g., if the event has no digits, upper-case letter, or `@`, skip the pipeline). Optional.

## FAILED

(none)

## Severity Summary

CRITICAL: 0 | HIGH: 0 | MEDIUM: 0 | LOW: 2

## Notes

- No N+1 queries (no DB).
- No connection pools touched.
- No async patterns introduced; scrubber is purely CPU-bound.
- No new cache or resource cleanup concerns.
- Timeout and circuit-breaker concerns do not apply.
- GC pressure: one metadata map + one TelemetryEvent record per scrub; tenured-gen friendly.
