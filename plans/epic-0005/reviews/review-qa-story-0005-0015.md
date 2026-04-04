# QA Review -- story-0005-0015: Output Directory Cleanup + Overwrite Protection

```
ENGINEER: QA
STORY: story-0005-0015
SCORE: 33/36
STATUS: Rejected
---
PASSED:
- [1] Test exists for each AC (2/2) — All 7 Gherkin scenarios from the story have corresponding tests: empty dir without --force (AT-2/cli.test.ts:425), existing artifacts without --force (AT-3/cli.test.ts:439), --force overwrites (AT-4/cli.test.ts:459), --dry-run bypasses check (AT-5/cli.test.ts:474), docs/epic/ not generated (IT-5/cli-integration.test.ts:196), error message lists dirs (AT-6/cli.test.ts:494), help text documents --force (IT-4/cli-integration.test.ts:186). Full Gherkin-to-test traceability confirmed.
- [2] Line coverage >= 95% (2/2) — 99.45% overall. overwrite-detector.ts at 100%, cli.ts at 100%, epic-report-assembler.ts at 100%.
- [3] Branch coverage >= 90% (2/2) — 97.33% overall. overwrite-detector.ts at 100%, cli.ts at 97.95%.
- [4] Test naming convention (2/2) — All test names follow methodUnderTest_scenario_expectedBehavior: checkExistingArtifacts_emptyDir_returnsNoConflicts, assemble_validTemplate_doesNotCreateDocsEpicDirectory, generate_withForce_existingArtifacts_proceedsNormally, formatConflictMessage_withConflictDirs_returnsFormattedMessage, etc.
- [5] AAA pattern (2/2) — All tests follow Arrange-Act-Assert with clear separation. Mocks configured in Arrange, single action call, explicit assertions. Examples: overwrite-detector.test.ts:24-29 (empty dir), cli.test.ts:439-457 (conflict error).
- [6] Parametrized tests for data-driven scenarios (2/2) — checkExistingArtifacts tests use data-driven approach with varying directory combinations (empty, .claude only, .claude+.github, all three, non-artifact files, nonexistent dir). formatConflictMessage tested with both single and multiple directories.
- [7] Exception paths tested (2/2) — Error paths thoroughly covered: conflict without --force shows error and exits (AT-3 cli.test.ts:439-457, IT-2 cli-integration.test.ts:148-167), nonexistent directory returns no conflicts (overwrite-detector.test.ts:78-85), error message content validated (AT-6 cli.test.ts:494-511).
- [8] No test interdependency (2/2) — Each test creates its own temp directory via beforeEach/mkdtemp and cleans up via afterEach/rm. Mocks are cleared with vi.clearAllMocks() in beforeEach. No shared mutable state between tests.
- [9] Fixtures centralized (2/2) — Test helpers use aFullProjectConfig() from tests/fixtures/project-config.fixture.ts. CLI tests use buildTestConfig() and buildTestPipelineResult() factories. Integration tests use FIXTURES_DIR and RESOURCES_DIR constants.
- [10] Unique test data (2/2) — Each test creates unique temp directories via mkdtemp with descriptive prefixes ("overwrite-detector-test-", "cli-test-", "cli-int-"). No fixed IDs that could conflict across parallel runs.
- [11] Edge cases covered (2/2) — Nonexistent directory (overwrite-detector.test.ts:78-85), non-artifact files only (overwrite-detector.test.ts:52-62), all three artifact dirs (overwrite-detector.test.ts:64-76), single directory conflict (formatConflictMessage with single dir, overwrite-detector.test.ts:108-116), dry-run with existing artifacts (cli.test.ts:474-492).
- [12] Integration tests for DB/API (2/2) — N/A for DB (no database in this project). CLI integration tests in cli-integration.test.ts use real file system operations with createCli() running the actual pipeline: IT-1 (force with artifacts), IT-2 (without force with artifacts), IT-3 (dry-run with artifacts), IT-5 (full pipeline verifying docs/epic absence). These are genuine integration tests exercising the real CLI stack.
- [15] Tests follow TPP progression (2/2) — overwrite-detector.test.ts follows clear TPP: L1 degenerate (empty dir, nonexistent dir) -> L3 variable (single conflict, non-artifact files) -> L5 collection (multiple conflicts, all three artifacts). epic-report-assembler.test.ts follows: L1 (template missing) -> L2 (constant path checks) -> L5 (collection of paths). cli.test.ts acceptance tests: L1 (empty dir) -> L4 (conditional --force) -> L6 (composition dry-run bypass).
- [17] Acceptance tests validate E2E behavior (2/2) — Integration test IT-5 (cli-integration.test.ts:196-222) runs the real pipeline with minimal config and validates docs/epic/ absence plus template presence in .claude/templates/ and .github/templates/. IT-1 through IT-3 exercise the real CLI overwrite detection against real file system.
- [18] TDD coverage thresholds maintained (2/2) — 99.45% line coverage (threshold: 95%), 97.33% branch coverage (threshold: 90%). All 3,424 tests passing. Zero regressions.
FAILED:
- [13] Commits show test-first pattern (0/2) — All changes are uncommitted in the working tree. There is no git history showing RED->GREEN->REFACTOR cycle. The story test plan (tests-story-0005-0015.md section 9) prescribes a step-by-step TDD order (tests first, then implementation), but no commit evidence exists to verify this was followed. Previous stories in this repo show explicit TDD commit markers (e.g., [TDD:RED], [TDD:GREEN], [TDD:REFACTOR] in commits 0619918, 3172f99, d175f4f), but story-0005-0015 has zero committed increments. [SEVERITY: HIGH]
PARTIAL:
- [14] Explicit refactoring after green (1/2) — tests/node/assembler/epic-report-assembler.test.ts:174 — Improvement: The test names were renamed from "three" to "two" (e.g., returnsThreeFilePaths -> returnsTwoFilePaths, allThreeOutputsAreIdentical -> bothOutputsAreIdentical) which indicates refactoring occurred, and the formatConflictMessage function is cleanly separated from checkExistingArtifacts (good SRP). However, without commit history, there is no evidence that refactoring happened as a separate step after green. Previous stories committed REFACTOR phases explicitly. Recommend committing in RED/GREEN/REFACTOR increments per project convention. [SEVERITY: MEDIUM]
- [16] No test written after implementation (1/2) — tests/node/overwrite-detector.test.ts:1 — Improvement: The test plan (section 9) prescribes "Step 1: Unit tests for overwrite-detector (TDD red phase)" before "Step 1b: Create src/overwrite-detector.ts (make tests green)". The test file and source file are both well-structured and consistent with TDD output, but without commit timestamps there is no proof tests were written first. The test plan itself is correctly ordered. Recommend committing test files before implementation files in future stories to provide auditable evidence. [SEVERITY: MEDIUM]
```

## Detailed Analysis

### Test Inventory (20 new/modified tests)

| File | New Tests | Modified Tests |
|------|-----------|---------------|
| tests/node/overwrite-detector.test.ts | 8 (6 checkExistingArtifacts + 2 formatConflictMessage) | 0 |
| tests/node/cli.test.ts | 7 (overwrite protection block) + 1 (help --force) | 0 |
| tests/node/assembler/epic-report-assembler.test.ts | 0 | 5 (cycles 3,4,7,8,9 updated) |
| tests/node/integration/cli-integration.test.ts | 5 (OverwriteProtection block) | 0 |
| **Total** | **21 new** | **5 modified** |

### Coverage Report

| Metric | Value | Threshold | Status |
|--------|-------|-----------|--------|
| Line Coverage | 99.45% | >= 95% | PASS |
| Branch Coverage | 97.33% | >= 90% | PASS |
| Tests Passing | 3,424/3,424 | 100% | PASS |
| Test Files | 98 passed | 0 failed | PASS |

### Gherkin-to-Test Traceability

| Gherkin Scenario | Test IDs | Covered |
|-----------------|----------|---------|
| Empty dir without --force | cli.test.ts:425, cli-integration.test.ts:196 | YES |
| Existing artifacts without --force | cli.test.ts:439, cli-integration.test.ts:148 | YES |
| --force overwrites | cli.test.ts:459, cli-integration.test.ts:125 | YES |
| --dry-run bypasses check | cli.test.ts:474, cli-integration.test.ts:169 | YES |
| docs/epic/ not generated | epic-report-assembler.test.ts:98, cli-integration.test.ts:196 | YES |
| Error lists conflicting dirs | cli.test.ts:494 | YES |
| Help text documents --force | cli-integration.test.ts:186, cli.test.ts:640 | YES |

### Blocking Issues

1. **No TDD commit evidence (items 13, 14, 16):** All changes exist as uncommitted working tree modifications. The project convention (visible in prior stories) is to commit with explicit `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]` markers. This story has zero commits, making it impossible to audit the TDD workflow. This is the only blocking issue. The fix is straightforward: commit changes in incremental steps following the TDD cycle before submitting for review.

### Positive Observations

- Clean module separation: `overwrite-detector.ts` is a pure function module with zero side effects beyond `existsSync`.
- `OverwriteCheckResult` uses `readonly` properties and `readonly string[]` -- good immutability.
- `ARTIFACT_DIRS` uses `as const` for type safety.
- All 8 golden file profiles updated consistently.
- Integration tests use real CLI and real file system -- no mocks for the overwrite detector at the integration level.
- `formatConflictMessage` separated from detection logic (SRP).
- `--dry-run` bypass is correctly placed before the check call (not after), avoiding unnecessary filesystem reads.
