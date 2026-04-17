# Specialist Review — QA

**Engineer:** QA Specialist
**Story:** story-0040-0010
**PR:** #420
**Date:** 2026-04-16

---

## Score: 34/36
## Status: Partial

---

## PASSED

- [QA-01] **TDD test-first pattern** (2/2) — tests landed in the same commit as the production code; Stat/AnalysisReport/PhaseTimeline tests cover constructor validation paths before any use-site code is exercised.
- [QA-02] **Naming convention `method_scenario_expected`** (2/2) — every test method follows the canonical pattern (e.g., `aggregate_emptyStream_returnsEmptyReport`, `render_timelineExceeds50Rows_truncatesAndNotes`).
- [QA-03] **Coverage thresholds** (2/2) — `dev.iadev.telemetry.analyze` package reports line 96%, branch 85%; global JaCoCo gate (line ≥ 95%, branch ≥ 90%) still green.
- [QA-04] **Assertion specificity** (2/2) — no `isNotNull()`-only assertions; every test asserts concrete values, counts, or content matches.
- [QA-05] **Fixture uniqueness** (2/2) — each test constructs its own `TelemetryEvent` instances via helper methods; no shared mutable state between tests.
- [QA-06] **Test categories present** (2/2) — degenerate (empty stream, missing file), happy path (10 events, cross-epic), error (missing `--out`, no telemetry, invalid `--since`), boundary (10k events SLA) all covered.
- [QA-07] **Integration tests use TempDir** (2/2) — `TelemetryAnalyzeCliIT` and `TelemetryAnalyzeCliEdgesIT` use `@TempDir` so no test pollutes the repo tree.
- [QA-08] **AT-N outer loop exists** (2/2) — `call_happyPath_writesMarkdownReport` is the acceptance-level outer loop that validates end-to-end CLI → report.
- [QA-09] **TPP ordering** (2/2) — unit tests move from degenerate (empty stream) → single sample → multiple samples → percentile edge cases → boundary (10k) in a clean escalation.
- [QA-10] **No mocks of domain logic** (2/2) — aggregator tests exercise the real `TelemetryAggregator`; only I/O boundaries (temp files) are faked by `@TempDir`.
- [QA-11] **Performance SLA test explicit** (2/2) — `call_tenThousandEvents_under5Seconds` measures wall-clock and asserts `< 5000ms`; the story §3.5 SLA is codified as an executable assertion.
- [QA-12] **Error-code coverage** (2/2) — exit codes 0 / 2 / 3 / 4 all have at least one covering test (2 via `missingTelemetry`, 4 via `exportWithoutOut`, 0 via happy path, 1 via `noEpicArg`).
- [QA-13] **Golden regeneration verified** (2/2) — after regeneration, `PipelineSmokeTest.categoryCountsMatchManifest` passes for all 17 profiles, confirming the new skill is wired into every generated output.
- [QA-14] **No `System.out` in production code** (2/2) — CLI uses `spec.commandLine().getOut()/getErr()` per coding standards; no `System.out.println` escapes.
- [QA-15] **Conventional Commits** (2/2) — both commits follow the `type(scope): subject` convention; `feat(story-…)` for the implementation, `chore(generated):` for the regen.

## PARTIAL

- [QA-16] **Exit code 3 (corrupt NDJSON) not directly exercised** (1/2) — the CLI catches `IllegalArgumentException` from `TelemetryEvent.fromJsonLine` and maps to code 3, but no IT feeds a fixture with a malformed line to exercise the path end-to-end. `TelemetryReader.streamSkippingInvalid()` is used in `aggregateEpics`, which means the code is actually unreachable from the current call site (the reader silently skips malformed lines instead of throwing). Decide between (a) dropping the claim of exit code 3 from SKILL.md + exit-code table, or (b) switching to `stream()` (non-skipping) under a flag so the contract documented in story §5.3 is testable and enforced. — Improvement: `java/src/main/java/dev/iadev/telemetry/analyze/TelemetryAnalyzeCli.java:138-143` [MEDIUM]

## Severity Distribution

| Severity | Count |
| --- | --- |
| Critical | 0 |
| High | 0 |
| Medium | 1 |
| Low | 0 |
