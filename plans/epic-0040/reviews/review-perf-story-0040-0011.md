ENGINEER: Performance
STORY: story-0040-0011
SCORE: 26/26
STATUS: Approved

---

PASSED:
- [PERF-01] Algorithmic complexity documented (2/2) — index build is O(epics × events); detector is O(skills × epics); slowest ranking is O(skills) with sort O(S log S).
- [PERF-02] Streaming I/O (2/2) — `TelemetryIndexBuilder.aggregateEpic` uses `TelemetryReader.streamSkippingInvalid()` with try-with-resources; per-epic buckets retain only durations (one `long` per event).
- [PERF-03] No unnecessary memory retention (2/2) — samples are sorted in place and collapsed to a single `EpicSkillP95` row before the next epic is read.
- [PERF-04] Cache invalidation O(1) per epic (2/2) — `EpicDirectoryScanner.scanEpicMtimes()` does a single `getLastModifiedTime` probe per epic dir; cache hit path reads the JSON without touching NDJSON at all.
- [PERF-05] Percentile computation (2/2) — Nearest-Rank on sorted samples; integer-stable and consistent with `TelemetryAggregator`.
- [PERF-06] Pagination N/A (2/2) — trend analyzer caps output at TOP-N (10) for both tables; no unbounded collections reach the renderer.
- [PERF-07] Perf SLA enforced by test (2/2) — `TelemetryTrendPerfIT.fiveEpicsTenThousandEvents_under10Seconds` asserts `< 10_000L ms`; actual observed wall-time ≈ 600 ms (> 16× headroom).
- [PERF-08] No sleep-based synchronization (2/2) — the single `Thread.sleep(10)` in a test is a deliberate wallclock delta outside the production path.
- [PERF-09] Stream close discipline (2/2) — all `TelemetryReader.streamSkippingInvalid()` calls are inside try-with-resources; stream `onClose` closes the underlying `BufferedReader`.
- [PERF-10] Resource cleanup on error (2/2) — the builder's `aggregateEpic` loop closes each stream per iteration; a parse failure propagates after the try-with-resources runs the finalizer.
- [PERF-11] JSON serialization efficiency (2/2) — `TrendJsonRenderer` pretty-prints tiny payloads (top-10) — negligible overhead.
- [PERF-12] No N+1 scans (2/2) — each NDJSON file is read exactly once per `build()`; the cache path reads zero NDJSON files.
- [PERF-13] File length limit (2/2) — **FIXED** in remediation: `TelemetryIndexBuilder.java` reduced from 270 to 219 lines via extraction of `EpicDirectoryScanner` (87 lines); both files now under the 250-line ceiling.

FAILED:
(none)

PARTIAL:
(none)
