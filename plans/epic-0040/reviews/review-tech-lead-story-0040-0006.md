# Tech Lead Review — story-0040-0006

**Story:** story-0040-0006 — Instrument implementation skills + create telemetry-phase.sh helper
**PR:** #415 (https://github.com/edercnj/ia-dev-environment/pull/415)
**Date:** 2026-04-16
**Reviewer:** Tech Lead
**Template Version:** EPIC-0024 standard
**Commit range:** e6787e3f0..4307154b0 (7 commits)

## Summary

Story-0040-0006 delivers the Phase 3 critical-path work of EPIC-0040 (telemetria de execução de skills):

- New `telemetry-phase.sh` helper (~145 lines) with fail-open contract, sourced by the 4 implementation skills.
- `dev.iadev.ci.TelemetryMarkerLint` (~207 lines) — CI-grade balance validator for SKILL.md.
- 4 SKILL.md files instrumented with 15 total phase marker pairs (epic=4, story=5, task-impl=3 TDD, task-plan=3).
- Rule 13 gains a canonical "Telemetry Markers" section.
- 22 new tests (5 helper IT + 9 lint unit + 8 skill markers IT) — all green.
- Golden regeneration across 19 profiles, smoke manifest refreshed.

## Decision

**GO** — score 44/48 (92%), 0 CRITICAL, 0 test failures, coverage gate met.

## 45-Point Rubric Scorecard

| Section | Score | Max | Notes |
| :--- | ---: | ---: | :--- |
| A. Code Hygiene | 8 | 8 | No unused imports, no dead code, zero warnings, ≤4 params everywhere. |
| B. Naming | 4 | 4 | `DUPLICATE_START`, `validateBalance`, `extractMarkers` — intention-revealing. |
| C. Functions | 3 | 5 | `validateBalance` 60 lines — exceeds 25-line cap. |
| D. Vertical Formatting | 3 | 4 | TelemetryPhaseHelperIT 325 lines — exceeds 250-line cap. |
| E. Design | 3 | 3 | LoD, CQS clean, production DRY. |
| F. Error Handling | 3 | 3 | `UncheckedIOException` carries path; no null; specific catch. |
| G. Architecture | 4 | 5 | New `dev.iadev.ci` package lacks ADR reference. |
| H. Framework & Infra | 4 | 4 | Native-compatible, env-var config, stderr structured. |
| I. Tests & Execution | 6 | 6 | 7229 tests pass, 95%/90% coverage, smoke all green. |
| J. Security | 1 | 1 | Fail-open contract + inherited scrubber chain. |
| K. TDD Process | 5 | 5 | Atomic per-task commits with RED→GREEN→REFACTOR documentation. |
| **Total** | **44** | **48** | **92%** — above 38/45 GO threshold. |

## Test Execution Results (EPIC-0042)

| Suite | Result | Details |
| :--- | :--- | :--- |
| Unit + Integration | PASS | 6308 surefire + 921 failsafe = 7229 tests, 0 failures, 0 errors, 0 skipped |
| Coverage (Jacoco) | PASS | 95% instructions / 90% branches overall (Jacoco `check` gate met) |
| Smoke tests | PASS | HooksSmokeIT, CrossProfileConsistencySmokeTest, PipelineSmokeTest, TelemetrySmokeTest, TelemetryBenchmarkSmokeTest |
| `telemetry-phase.sh` helper coverage | PASS | 5/5 IT scenarios green |
| `TelemetryMarkerLint` coverage | PASS | 97% lines / 94% branches (per-class) |

## Findings (sorted by severity)

### MEDIUM

- **[C2 / FIND-004] `validateBalance()` body 60 lines > 25-line cap**
  - File: `java/src/main/java/dev/iadev/ci/TelemetryMarkerLint.java:97-170`
  - Root cause: three sub-responsibilities inline (handle start, handle end, report unclosed).
  - Fix: extract `handleStartMarker(...)`, `handleEndMarker(...)`, `reportUnclosedStarts(...)` — reduces body to ~15 lines.
  - Impact: readability, Rule 03 §Hard Limits compliance. Not a correctness issue.

- **[D2 / FIND-001] TelemetryPhaseHelperIT 325 lines > 250-line cap**
  - File: `java/src/test/java/dev/iadev/telemetry/hooks/TelemetryPhaseHelperIT.java`
  - Fix: extract `TelemetryPhaseTestFixture` with `runHelperProcess()`, `readNdjson()`, `assumeBashAvailable()`. OR use `@Nested` groups.
  - Impact: Rule 05 §Forbidden.

### LOW

- **[G3 / FIND-005] `dev.iadev.ci` package lacks ADR**
  - File: `java/src/main/java/dev/iadev/ci/package-info.java`
  - Current: package-info explains purpose inline but no ADR link.
  - Fix: add a brief ADR or explicit reference in package-info that CI tooling is off-layer by design per Rule 04.
  - Impact: Rule 04 §Deviations compliance.

- **[FIND-002] Four Markers ITs share structure — parametrized base opportunity**
- **[FIND-003] Missing tree-walking SKILL.md scanner — add `AllSkillsMarkerLintIT`**

## Cross-File Consistency Check

| Invariant | Status |
| :--- | :--- |
| All 4 skill files use identical marker comment pattern (`<!-- TELEMETRY: phase.start -->`) | PASS |
| All 4 skill files use identical Bash command prefix | PASS |
| All Markers ITs use identical assertion scaffolding | PASS (opportunity — FIND-002) |
| `HooksAssembler.TELEMETRY_SCRIPTS` ordering preserved | PASS |
| Golden tree mirrors source-of-truth | PASS (19 profiles regenerated verbatim) |
| Smoke manifest matches directory counts | PASS (regenerated via `ExpectedArtifactsGenerator.main()`) |

## Rule Compliance

| Rule | Finding |
| :--- | :--- |
| Rule 01 | ✓ |
| Rule 02 | ✓ (N/A — pure tooling) |
| Rule 03 | ✗ `validateBalance` 60 lines (FIND-004) |
| Rule 04 | ✗ New `ci` package without ADR (FIND-005) |
| Rule 05 | ✗ Test file > 250 lines (FIND-001) |
| Rule 06 | ✓ |
| Rule 07 | ✓ |
| Rule 08 | ✓ |
| Rule 09 | ✓ |
| Rule 13 | ✓ (new §Telemetry Markers section authored) |
| Rule 18 | ✓ (Conventional Commits with `task(task-0040-0006-NNN)` scope) |
| Rule 19 | ✓ |

## Out-of-Scope Observations

- Merge commit `9d75a07a1` carries 0040-0005's files. Those are reviewed separately on PR #414; this review scopes only to 0040-0006 commits.
- During review, a stray merge state was observed in the worktree (MERGE_HEAD pointing at 0040-0003's squash commit on develop). Aborted cleanly; no impact on final branch.

## Recommendation

**Merge.** The 2 MEDIUM findings are code-hygiene refactors that do not impact correctness, coverage, or security; they are tracked in the remediation file and can be addressed by a light touch-up commit or a follow-up story.

---

Report: `plans/epic-0040/reviews/review-tech-lead-story-0040-0006.md`
Dashboard: `plans/epic-0040/reviews/dashboard-story-0040-0006.md` (updated)
Remediation: `plans/epic-0040/reviews/remediation-story-0040-0006.md` (updated)
