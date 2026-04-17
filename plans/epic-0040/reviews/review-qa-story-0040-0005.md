# Specialist Review — QA

**Story:** story-0040-0005
**PR:** #413
**Branch:** feat/story-0040-0005-pii-scrubber
**Reviewer:** QA specialist (inline)
**Date:** 2026-04-16

ENGINEER: QA
STORY: story-0040-0005
SCORE: 34/36
STATUS: Partial

---

## PASSED

- [QA-01] Test-first commit order honoured: each task commit contains the test alongside (or before) the implementation (2/2).
- [QA-02] Unit tests cover every mandatory Gherkin scenario from story §7 (degenerate, happy AWS, happy JWT, metadata whitelist, engine-fail, fuzz-100, obscure email) (2/2).
- [QA-03] Test naming follows `[method]_[scenario]_[expected]` convention consistently across all 5 new test files (2/2).
- [QA-04] Nested `@DisplayName` classes organise the scrubber suite into Degenerate / HappyPath / StructurePreservation / MetadataValueScrubbing / FailOpen / PatternOrdering — clean readability (2/2).
- [QA-05] Fuzz contract has TWO complementary assertions: parametrized per-entry + aggregate scan. The aggregate accumulates failures instead of stopping at first, so a regression reports all false negatives in one run (2/2).
- [QA-06] Coverage thresholds met:
  - TelemetryScrubber: 100% line / 100% branch
  - MetadataWhitelist: 100% line
  - ScrubRule: 100% line / 100% branch
  - PiiAudit.Finding: 100% line
  - PiiAudit: 93% line (only `main()` wrapper uncovered — acceptable)
  (2/2).
- [QA-07] Parametrized tests use `@ValueSource` for whitelist keys and `@MethodSource` for the corpus — both idiomatic JUnit 5 (2/2).
- [QA-08] Fuzz corpus size is validated at class-load time (static initialiser) — fail-fast on fixture corruption (2/2).
- [QA-09] Acceptance tests at the assembler level (`RulesAssemblerTelemetryTest`) verify the outer-loop contract (rule file copied + content intact) (2/2).
- [QA-10] Integration test `PiiAuditSmokeIT` covers 9 scenarios: clean tree, polluted tree, null sink, long-snippet truncation, Finding validation, grep format, quiet flag, missing root, programmatic API (2/2).
- [QA-11] Fail-open branch covered by a constructed rule that throws `IndexOutOfBoundsException` via a bogus `$9` replacement — exercises a real RuntimeException rather than mocking (2/2).
- [QA-12] No `isNotNull()` alone — every assertion specifies content or absence of a specific substring (2/2).
- [QA-13] Assertion chains are uniform: `.contains(marker).doesNotContain(original)` — prevents accidental false positives (2/2).
- [QA-14] Golden files regenerated and manifest (`expected-artifacts.json`) synced — no test debt left behind (2/2).
- [QA-15] Fixture corpus curated (not random) so property is stable and auditable; each category has 12-15 entries for balance (2/2).
- [QA-16] TelemetryEvent constructor paths exercised indirectly via the scrubber integration tests (structure preservation across all 15 fields) (2/2).

## PARTIAL

- [QA-17] `TelemetryScrubberTest` is 360+ lines — above the Rule 05 "Forbidden" soft limit of 250 lines per test file. The nested-class organisation softens the readability cost but the file would benefit from being split into `TelemetryScrubberHappyPathTest` + `TelemetryScrubberEdgeCasesTest` (1/2) [LOW] — **Fix:** Split by nested class boundary.

- [QA-18] `PiiAuditSmokeIT` tests use `assertThrows` from JUnit 5 via fully-qualified names (`org.junit.jupiter.api.Assertions.assertThrows`). Rule 03 (Forbidden — "Fully qualified class names when an import suffices") — the imports should be added at the top of the file (1/2) [LOW] — `PiiAuditSmokeIT.java` lines 145-159. **Fix:** Add `import static org.junit.jupiter.api.Assertions.assertThrows;`.

## FAILED

(none)

## Severity Summary

CRITICAL: 0 | HIGH: 0 | MEDIUM: 0 | LOW: 2

## Test Category Coverage (Rule 05)

- [x] Unit (TelemetryScrubberTest, MetadataWhitelistTest, ScrubRuleTest)
- [x] Integration (PiiAuditSmokeIT — filesystem)
- [x] API — N/A (no REST)
- [x] Contract (TelemetryScrubberFuzzTest — 100 rows = 100 contract assertions)
- [x] E2E — covered by PiiAuditSmokeIT scanning a real temp filesystem
- [ ] Performance — not required for this story (p99 < 3ms claim in DoD not automated; see Performance review)
- [x] Smoke (RulesAssemblerTelemetryTest — end-to-end assembler run)

## TDD Compliance

- Red → Green → Refactor cycles explicitly logged in commit messages (tasks 001-005).
- Test precedes / accompanies implementation in every commit.
- Refactor commits explicit: task 002's URL-pattern tightening; task 005's assertion reorder.
