# Story Planning Report -- story-0039-0012

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0012 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema | v1 (FALLBACK_MISSING_FIELD) |

## Planning Summary

Per-phase telemetry emitted by `x-release`: each phase of the release flow writes
one JSONL line to `plans/release-metrics.jsonl` (committed to the repo per
RULE-006, append-only, file-locked). Phase 13 SUMMARY reads the last 5 releases
and prints top-3 slowest phases with delta vs. moving average. Bypass flag
`--telemetry off` for CI/debug contexts. Plan decomposes into 14 tasks organized
in a hexagonal split (domain `BenchmarkAnalyzer` pure + outbound adapter
`TelemetryWriter` with file I/O), driven by RED-before-GREEN pairing and a final
TL+PO consolidation gate.

## Architecture Assessment

- **Affected layers:** domain (new package `dev.iadev.release.telemetry`), adapter.outbound (file writer), config (SKILL.md instrumentation + SUMMARY wiring), test (smoke).
- **New components:**
  - `TelemetrySink` (domain.port) — port for emit(phaseMetric).
  - `PhaseMetric` (domain.model) — record with `releaseVersion`, `releaseType`, `phase`, `startedAt`, `endedAt`, `durationSec`, `outcome`.
  - `BenchmarkAnalyzer` (domain) — pure aggregator: reads `Stream<PhaseMetric>`, returns `Top3Benchmark` value object or `InsufficientHistory` sentinel.
  - `FileTelemetryWriter` (adapter.outbound.telemetry) — implements `TelemetrySink` using `FileChannel` + `FileLock` for atomic append.
  - `JsonlTelemetryReader` (adapter.outbound.telemetry) — streams `PhaseMetric` from JSONL file for SUMMARY.
- **Reused components:** SLF4J logger (warn-only on write failure); Jackson for JSON line serialization (consistent with rest of codebase).
- **Dependency direction:** application/SKILL -> domain.port (TelemetrySink) <- adapter.outbound (FileTelemetryWriter). Domain imports ZERO file I/O — verified by Rule 04.

## Test Strategy Summary

- **Acceptance tests (outer loop):** 6 Gherkin scenarios from §7 mapped to:
  - 1 smoke test (TASK-013) covering happy path + 5-release boundary + SKIPPED outcome.
  - 2 ITs in writer (TASK-001 degenerate off, TASK-005 write-fail warn-only).
  - 1 unit test in analyzer (TASK-010 <5 boundary).
- **Unit tests (inner loop, TPP order):**
  - Writer: nil (off) -> constant (1 line written) -> scalar (lock released) -> conditional (IOException warn).
  - Analyzer: nil (empty) -> collection (avg over N) -> scalar (<5 boundary).
- **Coverage target:** >=95% line, >=90% branch on `release.telemetry` package.
- **Framework:** JUnit 5 + AssertJ; `@TempDir` for JSONL path in ITs.

## Security Assessment Summary

- **OWASP categories applicable:**
  - **A03 Injection** — `releaseVersion` and `phase` values rendered into JSONL; MUST go through Jackson's proper string escaping (never string concat) to prevent JSON injection via crafted tag names.
  - **A08 Data Integrity Failures** — file lock prevents torn writes when releases overlap; append-only semantics preserve audit trail; no rewrite of prior lines.
  - **A05 Security Misconfiguration** — JSONL path is a hardcoded constant `plans/release-metrics.jsonl` (no user-controlled path to prevent traversal); file perms 0644.
- **No new secrets** handled. No network I/O. No deserialization of untrusted input (reader only consumes own-written files, but still uses strict Jackson config `FAIL_ON_UNKNOWN_PROPERTIES=false` with explicit field mapping).
- **Risk level:** LOW — local file append with fixed path and structured output.

## Implementation Approach

- Domain/adapter split enforced: `BenchmarkAnalyzer` is pure (takes `Stream<PhaseMetric>`, returns value object); `FileTelemetryWriter` does all I/O.
- Writer uses `FileChannel.open(path, APPEND, CREATE)` + `channel.lock()` then `writeAndFlush()` inside try-with-resources. Lock scoped to the single write.
- Write failures are caught and logged as `TELEMETRY_WRITE_FAILED` warn; never propagate — release flow MUST continue (per story DoD).
- `--telemetry off` handled in SKILL.md: flag parsed once at entry, stored in shell var; `emit_telemetry` wrapper becomes `:` (no-op) when off. Zero Java-side conditional — keeps writer trivially testable.
- SUMMARY integration (Phase 13): bash calls `java -cp … BenchmarkAnalyzerCli --last 5 --path plans/release-metrics.jsonl`; CLI wrapper serializes `Top3Benchmark` to the format shown in story §5.2.
- Tech Lead consistency: file adapter follows same pattern as existing `FileChangelogWriter` (constructor-injected `Path`, SLF4J logger field, `Jackson ObjectMapper` shared bean).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 14 |
| Architecture tasks | 1 (TASK-012 SKILL.md + SUMMARY) |
| Test tasks | 4 RED (TASK-001, 003, 005, 008, 010) + 4 GREEN counterparts + 1 smoke (TASK-013) |
| Security tasks | 1 (TASK-007) |
| Quality gate + validation tasks | 1 merged (TASK-014) |
| Merged tasks | 5 (writer impls + analyzer impls + final gate) |
| Augmented tasks | 1 (TASK-002 augmented with SEC-007 criteria via injection rule) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Torn JSONL lines on concurrent releases | ARCH | HIGH | LOW | FileLock around write+flush (TASK-004) |
| Domain accidentally imports file I/O | TL | MEDIUM | MEDIUM | Stream-input analyzer + import-guard test in TASK-014 |
| JSON injection via crafted release version / phase name | SEC | MEDIUM | LOW | Jackson serialization only; never string concat (TASK-007) |
| `--telemetry off` flag leaks through to writer partially | TL | MEDIUM | LOW | No-op wrapper at SKILL.md level (TASK-012) |
| SUMMARY crashes on empty/corrupt JSONL on first-ever run | QA | MEDIUM | MEDIUM | `InsufficientHistory` sentinel + defensive JSONL parse (TASK-011) |
| File perms expose metrics to other local users | SEC | LOW | LOW | 0644 explicit perms; no PII in payload |
| Write failure silently hides metrics forever | ARCH | LOW | LOW | WARN log with `TELEMETRY_WRITE_FAILED` code visible in release output |

## DoR Status

READY — see `dor-story-0039-0012.md`. All 10 mandatory checks PASS; conditional checks N/A (compliance=none, contract_tests=false).
