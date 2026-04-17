# QA Specialist Review — story-0040-0006

**Story:** story-0040-0006 — Instrument implementation skills + create telemetry-phase.sh helper
**Reviewer:** QA
**Date:** 2026-04-16
**PR:** #415
**Commit range:** e6787e3f0..4307154b0

## Score

**34/36 (94%) — PARTIAL**

## Checklist

| ID | Item | Score | Notes |
|----|------|-------|-------|
| Q1 | Test naming `[method]_[scenario]_[expected]` | 2/2 | All 22 tests conform. |
| Q2 | @DisplayName on every test | 2/2 | |
| Q3 | Happy + error + boundary categories | 2/2 | Helper IT covers 5 scenarios; lint test covers 9. |
| Q4 | No weak assertions (isNotNull alone) | 2/2 | Specific `.isEqualTo`, `.hasSize`, `.contains`. |
| Q5 | No `sleep()` for async sync | 2/2 | `p.waitFor(10, TimeUnit.SECONDS)` only. |
| Q6 | @TempDir for FS state | 2/2 | Per-test isolation. |
| Q7 | Test execution order independent | 2/2 | No static mutable state. |
| Q8 | TDD test-first (git log) | 2/2 | Each task commit bundles test + impl atomically. |
| Q9 | Fixtures / shared setup | 2/2 | `@BeforeAll loadSchema()`, `assumeBashAvailable()`. |
| Q10 | Test file ≤ 250 lines | 1/2 | TelemetryPhaseHelperIT ~320 lines (MEDIUM). |
| Q11 | Gherkin acceptance coverage | 2/2 | All 6 story §7 scenarios mapped. |
| Q12 | Parametrized where appropriate | 1/2 | 4 Markers ITs share structure (LOW). |
| Q13 | Coverage ≥ 95%/90% | 2/2 | Lint at 97% lines, 94% branches. |
| Q14 | No mocking of domain | 2/2 | Real shell + real FS + real schema. |
| Q15 | Double-Loop TDD (AT drives UT) | 2/2 | Markers ITs = acceptance; LintTest = inner. |
| Q16 | No production data / no network | 2/2 | `@TempDir` + classpath only. |
| Q17 | No commented tests / no silent @Disabled | 2/2 | `assumeTrue` with reason strings. |
| Q18 | Self-documenting assertions | 2/2 | `.as("...")` messages attached. |

## Findings

### PARTIAL

- **[Q10] TelemetryPhaseHelperIT exceeds 250-line limit (1/2) — MEDIUM**
  - File: `java/src/test/java/dev/iadev/telemetry/hooks/TelemetryPhaseHelperIT.java` (~320 lines)
  - Fix: split helper methods into a separate `TelemetryPhaseTestFixture` class OR wrap related tests in `@Nested` groups.

- **[Q12] Near-identical structure across 4 Markers ITs (1/2) — LOW**
  - Files: `XEpicImplementMarkersIT.java`, `XStoryImplementMarkersIT.java`, `XTaskImplementMarkersIT.java`, `XTaskPlanMarkersIT.java`
  - Fix: extract a parametrized base class `AbstractSkillMarkerContract(skillFile, expectedPairs, skillToken)`.

## Status

PARTIAL — zero blocker findings; 2 follow-up improvements of MEDIUM / LOW severity.
