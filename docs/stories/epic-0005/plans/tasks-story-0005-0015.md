# Task Breakdown -- story-0005-0015: Output Directory Cleanup + Overwrite Protection

**Story:** `story-0005-0015.md`

---

## Summary

16 tasks organized into 4 groups (A through D) following TDD Red-Green-Refactor cycles.

**Change 1 (Group A):** Remove `docs/epic/` from `EpicReportAssembler` output destinations.
**Change 2 (Groups B-C):** Add overwrite protection with `--force` flag to `generate` command.
**Finalization (Group D):** Golden file cleanup, help text, and integration tests.

**Legend:**
- **RED** = Write failing tests only (no production code changes)
- **GREEN** = Write minimum production code to make tests pass
- **REFACTOR** = Improve code/tests without changing behavior; update golden files

---

## Group A: Remove `docs/epic/` from EpicReportAssembler

### TASK-1: RED -- Tests expect EpicReportAssembler to emit 2 paths instead of 3
- **Type:** RED
- **Parallel:** no
- **Depends On:** none
- **Files:**
  - `tests/node/assembler/epic-report-assembler.test.ts`
- **Action:** Update existing tests to reflect the new expected behavior (2 output destinations instead of 3). Specifically:
  1. Change `assemble_validTemplate_returnsThreeFilePaths` to `assemble_validTemplate_returnsTwoFilePaths` -- assert `result` has length 2, assert `result[0]` contains `.claude/templates/`, `result[1]` contains `.github/templates/`, and no element contains `docs/epic`.
  2. Remove `assemble_validTemplate_copiesToDocsEpicPath` (Cycle 4) entirely.
  3. Update `assemble_outputDirDoesNotExist_createsDirectoryStructure` (Cycle 3) to assert `.claude/templates/` exists instead of `docs/epic/`.
  4. Update `assemble_validTemplate_copiesContentVerbatimWithoutPlaceholderResolution` (Cycle 7) to read from `.claude/templates/` instead of `docs/epic/`.
  5. Update `assemble_validTemplate_allThreeOutputsAreIdentical` to `assemble_validTemplate_bothOutputsAreIdentical` -- compare only `.claude/templates/` and `.github/templates/`.
  6. Add new test `assemble_validTemplate_doesNotCreateDocsEpicDirectory` asserting `docs/epic/` does NOT exist after assembly.
- **Acceptance:** Tests fail because `EpicReportAssembler` still emits 3 paths including `docs/epic/`.

### TASK-2: GREEN -- Remove DOCS_OUTPUT_SUBDIR from EpicReportAssembler outputs array
- **Type:** GREEN
- **Parallel:** no
- **Depends On:** TASK-1
- **Files:**
  - `src/assembler/epic-report-assembler.ts`
- **Action:** Remove `DOCS_OUTPUT_SUBDIR` from the `outputs` array in the `assemble` method. The array becomes `[CLAUDE_OUTPUT_SUBDIR, GITHUB_OUTPUT_SUBDIR]`. Optionally remove the unused `DOCS_OUTPUT_SUBDIR` constant. Update the JSDoc comment to reflect 2 output locations instead of 3.
- **Acceptance:** All updated tests from TASK-1 pass. Template is still emitted to `.claude/templates/` and `.github/templates/`.

### TASK-3: REFACTOR -- Remove golden files for docs/epic/ across all 8 profiles
- **Type:** REFACTOR
- **Parallel:** no
- **Depends On:** TASK-2
- **Files:**
  - `tests/golden/go-gin/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
  - `tests/golden/java-quarkus/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
  - `tests/golden/java-spring/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
  - `tests/golden/kotlin-ktor/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
  - `tests/golden/python-click-cli/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
  - `tests/golden/python-fastapi/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
  - `tests/golden/rust-axum/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
  - `tests/golden/typescript-nestjs/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (delete)
- **Action:** Delete the `docs/epic/` directory (and its contents) from every golden profile. Also remove the now-empty `docs/epic/` parent directory in each profile. Run `byte-for-byte.test.ts` integration tests to confirm parity still holds.
- **Acceptance:** Integration tests pass. No `docs/epic/` in any golden directory. No `extraFiles` or `missingFiles` in verification results.

---

## Group B: Overwrite Detection Module (Unit Tests)

### TASK-4: RED -- Test checkExistingArtifacts returns no conflicts for empty directory
- **Type:** RED
- **Parallel:** yes (with TASK-5, TASK-6)
- **Depends On:** TASK-3
- **Files:**
  - `tests/node/overwrite-detector.test.ts` (new file)
- **Action:** Create a new test file for the overwrite detection module. Write test `checkExistingArtifacts_emptyDir_returnsNoConflicts` that:
  1. Creates a temp directory (empty).
  2. Calls `checkExistingArtifacts(tempDir)`.
  3. Asserts result is `{ hasConflicts: false, conflictDirs: [] }`.
- **Acceptance:** Test fails because `checkExistingArtifacts` does not exist yet.

### TASK-5: RED -- Test checkExistingArtifacts detects existing .claude/ and .github/
- **Type:** RED
- **Parallel:** yes (with TASK-4, TASK-6)
- **Depends On:** TASK-3
- **Files:**
  - `tests/node/overwrite-detector.test.ts`
- **Action:** Add test `checkExistingArtifacts_withExistingArtifactDirs_returnsConflicts` that:
  1. Creates a temp directory with `.claude/` and `.github/` subdirectories.
  2. Calls `checkExistingArtifacts(tempDir)`.
  3. Asserts `hasConflicts` is `true`.
  4. Asserts `conflictDirs` contains `.claude/` and `.github/`.
- **Acceptance:** Test fails because `checkExistingArtifacts` does not exist yet.

### TASK-6: RED -- Test checkExistingArtifacts detects partial conflicts
- **Type:** RED
- **Parallel:** yes (with TASK-4, TASK-5)
- **Depends On:** TASK-3
- **Files:**
  - `tests/node/overwrite-detector.test.ts`
- **Action:** Add test `checkExistingArtifacts_withOnlyDocsDir_returnsPartialConflict` that:
  1. Creates a temp directory with only `docs/` subdirectory (no `.claude/`, no `.github/`).
  2. Calls `checkExistingArtifacts(tempDir)`.
  3. Asserts `hasConflicts` is `true`.
  4. Asserts `conflictDirs` equals `["docs/"]`.
- **Acceptance:** Test fails because `checkExistingArtifacts` does not exist yet.

### TASK-7: GREEN -- Implement checkExistingArtifacts function
- **Type:** GREEN
- **Parallel:** no
- **Depends On:** TASK-4, TASK-5, TASK-6
- **Files:**
  - `src/overwrite-detector.ts` (new file)
- **Action:** Create `src/overwrite-detector.ts` with:
  1. Export interface `OverwriteCheckResult { hasConflicts: boolean; conflictDirs: string[] }`.
  2. Export constant `ARTIFACT_DIRS = [".claude", ".github", ".agents", ".codex", "docs", "k8s", "tests"]` -- the known generated output directories.
  3. Export function `checkExistingArtifacts(outputDir: string): OverwriteCheckResult` that checks `fs.existsSync` for each artifact directory under `outputDir` and returns the list of existing ones with trailing `/`.
- **Acceptance:** All 3 RED tests from TASK-4, TASK-5, TASK-6 pass.

### TASK-8: RED -- Test formatConflictMessage produces expected error string
- **Type:** RED
- **Parallel:** no
- **Depends On:** TASK-7
- **Files:**
  - `tests/node/overwrite-detector.test.ts`
- **Action:** Add test `formatConflictMessage_withConflictDirs_returnsFormattedErrorMessage` that:
  1. Calls `formatConflictMessage([".claude/", ".github/", "docs/"])`.
  2. Asserts result contains `"Error: Output directory contains existing generated artifacts:"`.
  3. Asserts result contains `"  - .claude/ (exists)"`.
  4. Asserts result contains `"  - .github/ (exists)"`.
  5. Asserts result contains `"  - docs/ (exists)"`.
  6. Asserts result contains `"Use --force to overwrite existing files, or specify a different --output-dir."`.
- **Acceptance:** Test fails because `formatConflictMessage` does not exist yet.

### TASK-9: GREEN -- Implement formatConflictMessage function
- **Type:** GREEN
- **Parallel:** no
- **Depends On:** TASK-8
- **Files:**
  - `src/overwrite-detector.ts`
- **Action:** Add exported function `formatConflictMessage(conflictDirs: string[]): string` that builds the multi-line error message per the story spec:
  ```
  Error: Output directory contains existing generated artifacts:
    - .claude/ (exists)
    - .github/ (exists)

  Use --force to overwrite existing files, or specify a different --output-dir.
  ```
- **Acceptance:** TASK-8 test passes.

### TASK-10: REFACTOR -- Clean up overwrite-detector module
- **Type:** REFACTOR
- **Parallel:** no
- **Depends On:** TASK-9
- **Files:**
  - `src/overwrite-detector.ts`
  - `tests/node/overwrite-detector.test.ts`
- **Action:** Review the module for coding standards compliance: JSDoc on all exports, `readonly` on interface fields, file header comment, verify line/function length limits. Add edge-case test `checkExistingArtifacts_nonexistentDir_returnsNoConflicts` for a directory that does not exist at all. Ensure the ARTIFACT_DIRS list matches the actual generated top-level directories (cross-reference with golden files).
- **Acceptance:** All tests pass. Module conforms to coding standards.

---

## Group C: CLI Integration -- `--force` Flag and Overwrite Check

### TASK-11: RED -- Test CLI recognizes --force flag
- **Type:** RED
- **Parallel:** no
- **Depends On:** TASK-10
- **Files:**
  - `tests/node/cli.test.ts`
- **Action:** Add test `generate_withForce_passesForceToHandler` in the `generate command` describe block. Parse CLI with `["generate", "--config", configFilePath, "--force"]` and verify `mockRunPipeline` is called (the pipeline should execute when `--force` is present). Also add test `cli_generateHelp_showsForceOption` asserting the help text contains `"--force"`.
- **Acceptance:** Tests fail because `--force` is not yet a recognized option.

### TASK-12: GREEN -- Add --force flag to generate command and integrate overwrite check
- **Type:** GREEN
- **Parallel:** no
- **Depends On:** TASK-11
- **Files:**
  - `src/cli.ts`
- **Action:**
  1. Add `--force` option to `registerGenerateCommand`: `.option("-f, --force", "Overwrite existing generated artifacts.", false)`.
  2. Add `force?: boolean` to the `GenerateOptions` interface.
  3. Import `checkExistingArtifacts` and `formatConflictMessage` from `./overwrite-detector.js`.
  4. In `handleGenerate`, after `resolveResourcesDir` and before `executeGenerate`, add the overwrite check:
     - Skip check if `options.dryRun` is truthy.
     - Call `checkExistingArtifacts(options.outputDir)`.
     - If `hasConflicts && !options.force`, throw `new CliError(formatConflictMessage(conflictDirs), "OVERWRITE_CONFLICT")`.
- **Acceptance:** TASK-11 tests pass. The `--force` flag is recognized by commander.

### TASK-13: RED -- Test CLI blocks generation without --force when artifacts exist
- **Type:** RED
- **Parallel:** yes (with TASK-14)
- **Depends On:** TASK-12
- **Files:**
  - `tests/node/cli.test.ts`
- **Action:** Add test `generate_withoutForce_existingArtifacts_showsConflictError` that:
  1. Creates `.claude/` directory inside `tmpDir`.
  2. Parses CLI with `["generate", "--config", configFilePath, "--output-dir", tmpDir]`.
  3. Asserts `errorSpy` was called with a string containing `"existing generated artifacts"`.
  4. Asserts `exitSpy` was called with `1`.
  5. Asserts `mockRunPipeline` was NOT called.
- **Acceptance:** Test passes (GREEN immediately because TASK-12 already integrated the check). If it fails, adjust TASK-12 implementation.

### TASK-14: RED -- Test CLI allows generation with --dry-run even when artifacts exist
- **Type:** RED
- **Parallel:** yes (with TASK-13)
- **Depends On:** TASK-12
- **Files:**
  - `tests/node/cli.test.ts`
- **Action:** Add test `generate_withDryRun_existingArtifacts_skipsOverwriteCheck` that:
  1. Creates `.claude/` and `.github/` directories inside `tmpDir`.
  2. Parses CLI with `["generate", "--config", configFilePath, "--output-dir", tmpDir, "--dry-run"]`.
  3. Asserts `mockRunPipeline` WAS called with `dryRun=true`.
  4. Asserts `errorSpy` was NOT called with anything containing `"existing generated artifacts"`.
- **Acceptance:** Test passes (GREEN immediately because TASK-12 already skips check for dry-run).

### TASK-15: REFACTOR -- Consolidate CLI tests and verify help text
- **Type:** REFACTOR
- **Parallel:** no
- **Depends On:** TASK-13, TASK-14
- **Files:**
  - `tests/node/cli.test.ts`
  - `tests/node/cli-help.test.ts`
- **Action:**
  1. Verify the help text test in `cli.test.ts` (`cli_generateHelp_showsAllOptions`) now asserts `--force` is present.
  2. Add test `generate_withForceAndExistingArtifacts_proceedsNormally` that creates `.claude/` and `.github/` in `tmpDir`, parses with `--force`, and asserts pipeline is called.
  3. Add test `generate_emptyDir_withoutForce_proceedsNormally` that parses with an empty `tmpDir` as output-dir and asserts pipeline is called without error.
  4. Review all new CLI tests for consistent naming conventions and mock cleanup.
- **Acceptance:** All tests pass. Help text includes `--force` documentation.

---

## Group D: Integration Verification

### TASK-16: REFACTOR -- Run full test suite and verify coverage thresholds
- **Type:** REFACTOR
- **Parallel:** no
- **Depends On:** TASK-15
- **Files:**
  - All modified files (verification only, no new changes expected)
- **Action:**
  1. Run full test suite: `npx vitest run --coverage`.
  2. Verify line coverage >= 95% and branch coverage >= 90%.
  3. Run `byte-for-byte.test.ts` integration tests for all 8 profiles to confirm golden file parity after `docs/epic/` removal.
  4. Run `npx tsc --noEmit` to confirm zero compilation errors.
  5. If any coverage gaps exist, add targeted tests for uncovered branches.
- **Acceptance:** All tests pass. Coverage thresholds met. Zero compiler warnings. No `docs/epic/` in any output.

---

## Dependency Graph

```
TASK-1 (RED)
  |
  v
TASK-2 (GREEN)
  |
  v
TASK-3 (REFACTOR)
  |
  +---> TASK-4 (RED) --+
  +---> TASK-5 (RED) --+--> TASK-7 (GREEN)
  +---> TASK-6 (RED) --+       |
                               v
                         TASK-8 (RED)
                               |
                               v
                         TASK-9 (GREEN)
                               |
                               v
                         TASK-10 (REFACTOR)
                               |
                               v
                         TASK-11 (RED)
                               |
                               v
                         TASK-12 (GREEN)
                               |
                         +-----+-----+
                         |           |
                         v           v
                   TASK-13 (RED) TASK-14 (RED)
                         |           |
                         +-----+-----+
                               |
                               v
                         TASK-15 (REFACTOR)
                               |
                               v
                         TASK-16 (REFACTOR)
```

## Parallelism Summary

| Tasks | Can Run in Parallel |
|-------|-------------------|
| TASK-4, TASK-5, TASK-6 | Yes -- independent RED tests for overwrite detector |
| TASK-13, TASK-14 | Yes -- independent CLI integration tests |
| All others | No -- sequential dependencies |
