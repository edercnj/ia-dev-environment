# QA Specialist Review — EPIC-0050

ENGINEER: QA
STORY: EPIC-0050 (aggregate of 9 merged stories + PR #600 coverage remediation)
PR: #599 (`epic/0050 → develop`) + #600 (coverage remediation, merged into epic/0050)
DATE: 2026-04-23
SMOKE_TESTS: true (QA-19 and QA-20 in scope)

SCORE: 30/34 (adjusted max — see N/A notes below)

STATUS: **Partial** (no CRITICAL failures; 1 item at 0 due to test-after pattern, 2 at 1 due to incremental improvements possible)

## Context

EPIC-0050 is a **metadata-and-governance epic**: 0 Java main-source files changed, 1 new rule, ~17 SKILL.md frontmatter edits, 14 agent metadata changes, 1 new bash script, 1 CI step. Story-0050-0011 (coverage remediation) added 32 new JUnit tests across 7 files + 2 new files to close a coverage gate. **The TDD items of the QA rubric are evaluated against story-0050-0011 specifically** (the only story with new production tests); for the 8 other stories (metadata-only), TDD items are N/A.

## N/A rationale

| Item | Applies? | Reason |
|---|---|---|
| QA-12 | N/A | Project has `database = none` and `interfaces = cli`. No DB/API code touched. Excluded from max. |

Adjusted max: 40 − 2 (QA-12) − 4 (QA-13/14 TDD items with mixed applicability — see below) = treated as /34 for Partial scoring where TDD items outside story-0050-0011 are marked inapplicable.

Actual accounting:
- Items with score /2 and applicable: 15 items × 2 = 30 max (scored below).
- QA-13/14 scored at 0 for story-0050-0011 (test-after); counted in the /34 adjusted max as "applicable but failed".

## Scores (per-item)

### PASSED (16 items)

- **QA-01** (2/2) Acceptance criteria coverage — Each story has Gherkin scenarios; the audit script has a self-test; Rule 23 has scenario-level validation via story-0050-0001 DoD. Coverage-remediation story 0050-0011 explicitly names `RULE-005-01` and tests target the specific lines/branches uncovered.
- **QA-02** (2/2) Line coverage 95.21% ≥ 95% — Absolute gate PASS (Rule 05 RULE-005-01 now explicit in source-of-truth).
- **QA-03** (2/2) Branch coverage 90.01% ≥ 90% — Absolute gate PASS.
- **QA-04** (2/2) Test naming — All 32 new tests follow `[method]_[scenario]_[expected]`: `copyTemplateFile_missingSource_throwsUnchecked`, `parse_listWithNull_throws`, `claudeMd_rootFiles`, `consolidate_inputContainsDirectory_skipsDir`, etc.
- **QA-05** (2/2) AAA pattern — Arrange (fixtures via `@TempDir` / `Files.writeString`), Act (method call), Assert (AssertJ chained assertions) in every test.
- **QA-07** (2/2) Exception paths with specific assertions — `assertThatThrownBy(() -> ...).isInstanceOf(UncheckedIOException.class).hasMessageContaining("Failed to copy template")` in all 8 CopyHelpers error-path tests. No bare `isNotNull()` assertions.
- **QA-08** (2/2) No test interdependency — Every test uses `@TempDir` isolation or static method calls without shared mutable state.
- **QA-10** (2/2) Unique test data — Each test creates its own `@TempDir` payload or uses static method inputs; zero shared mutable state.
- **QA-11** (2/2) Edge cases — Null inputs, empty collections, missing files, paths-that-are-files (boundary), empty directories, non-existent paths, non-string list elements — all exercised.
- **QA-15** (2/2) TPP progression — CopyHelpersTest error paths progress from degenerate (missing source) → boundary (parent-is-file) → null-parent no-op; PlatformParserTest goes absent → string → list → invalid types.
- **QA-18** (2/2) Coverage thresholds maintained — Coverage remediation closed the gap and Rule 05 now enforces absolute gate (pre-existing deficits no longer grandfathered).
- **QA-19** (2/2) Smoke tests exist and cover critical path — `Epic0047CompressionSmokeTest`, `CliModesSmokeTest`, `ProfileRegistrationIntegrityTest`, `OutputDirectoryIntegrityTest`, `OnboardingSmokeIT`, `GoldenFileTest`, `PlatformGoldenFileTest`.
- **QA-20** (2/2) All smoke tests pass — `mvn verify` → BUILD SUCCESS with 4,156 tests, 0 failures, 0 errors, 0 skipped.
- **QA-17** (2/2) Acceptance tests validate end-to-end behavior — Story-level Gherkin scenarios are validated by golden-file regeneration across 9 stacks + platform; each goldens run is a full end-to-end validation of the generator pipeline.

### PARTIAL (2 items)

- **QA-06** (1/2) Parametrized tests for data-driven scenarios
  - Finding: New tests mostly use `@Test` per-scenario (e.g., 5 separate `@Test` methods for each root-file name in `FileCategorizerTest`). `@ParameterizedTest` with `@ValueSource` or `@CsvSource` would be more concise.
  - Impact: Low. Current form is clearly readable but adds ~25 lines compared to parametrized form.
  - Fix: Migrate `FileCategorizerTest.RootFiles` (5 tests) and `InfraFiles` (3 tests) to `@ParameterizedTest(name = "{0} -> {1}")` with `@CsvSource`.

- **QA-09** (1/2) Fixtures centralized (no duplicate records/classes across test files)
  - Finding: `ValidateConfigServiceTest.buildConfigWithArchUnit` was moved from inside a nested class to the outer test class (good), but is still local to that single test file. Similar config-builders likely exist in other tests (e.g., `RulesAssemblerTest`, `AssemblerPipelineTest`).
  - Impact: Low. Not a regression — matches the existing codebase style.
  - Fix (follow-up story): extract a `ProjectConfigFixtures` test-support class under `src/test/java/dev/iadev/testsupport/` with static factory methods.

### FAILED (2 items — test-after pattern in story-0050-0011)

- **QA-13** (0/2) Commits show test-first pattern
  - Finding: In story-0050-0011 (coverage remediation, PR #600), the ~32 new tests were written **after** their targeted production code already existed on `develop` for months. The commit `feat/story-0050-0011-coverage-gap-closure` introduces all tests in a single commit without preceding test-first commits.
  - Impact: **Medium** — this violates Rule 05 TDD requirements. Mitigation: the remediation's purpose was to close a pre-existing coverage gap, not to introduce new production code. There was no new production code to test-first against; the tests target already-shipped behavior.
  - Fix (procedural, not code): document the remediation exemption in Rule 05 itself — "coverage-remediation PRs that add tests for already-shipped code are exempt from QA-13/QA-14/QA-16 scoring, provided no new production code is introduced in the same PR."

- **QA-16** (0/2) No test written after implementation
  - Finding: Same root cause as QA-13 — the 32 tests were added for production code that shipped in prior PRs (unchanged in this PR).
  - Impact: **Medium** — same nuance applies. Honestly scored as 0 per the skill's literal rubric; contextually acceptable for a coverage-closure PR.

### N/A (2 items — not applicable to this epic)

- **QA-12** Integration tests for DB/API interactions — project has `database = none` and `interfaces = cli`; no DB/API code in scope.
- **QA-14** Explicit refactoring after green — no new production code was introduced in the remediation, so no refactor step applies.

## Findings with file:line (for Remediation Tracking)

| Severity | File | Issue | Fix |
|---|---|---|---|
| MEDIUM | PR #600 commit graph | Test-after pattern in coverage remediation violates QA-13/QA-16 literal rubric | Add Rule 05 exemption clause for coverage-remediation PRs that introduce zero new production code. |
| LOW | `FileCategorizerTest.java:41-110` | 8 single-assertion @Test methods could be parametrized | `@ParameterizedTest` + `@CsvSource` for root-file and infra-file lists. |
| LOW | `ValidateConfigServiceTest.buildConfigWithArchUnit` | Helper is local to test file | Extract to `ProjectConfigFixtures` shared test-support class. |

## Verdict

**STATUS: Partial** — No critical/high findings; epic passes the absolute coverage gate. 2 medium findings are procedural (test-after pattern acceptable for coverage-remediation PRs, but should be documented as an exemption in Rule 05 itself); 2 low findings are incremental improvements suitable for a follow-up cleanup story.
