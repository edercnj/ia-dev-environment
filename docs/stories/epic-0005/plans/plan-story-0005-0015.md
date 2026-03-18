# Implementation Plan -- story-0005-0015: Output Directory Cleanup + Overwrite Protection

**Story:** `story-0005-0015.md`
**Type:** Enhancement -- CLI behavior change + assembler output cleanup
**Dependencies:** None (isolated story)

---

## 1. Affected Layers and Components

This story touches two concerns:
1. **Assembler layer** -- remove the redundant `docs/epic/` output from `EpicReportAssembler`
2. **CLI layer** -- add `--force` flag and overwrite detection before pipeline execution

### Components Affected

| Component | Path | Change Type |
|-----------|------|-------------|
| EpicReportAssembler | `src/assembler/epic-report-assembler.ts` | MODIFY -- remove `docs/epic/` output |
| CLI (generate command) | `src/cli.ts` | MODIFY -- add `--force` flag, overwrite detection |
| Exceptions | `src/exceptions.ts` | NO CHANGE -- CLI throws `CliError` directly for overwrite conflicts |
| Golden files (8 profiles) | `tests/golden/{profile}/docs/epic/` | DELETE -- remove `docs/epic/` directory from all 8 profiles |
| EpicReportAssembler tests | `tests/node/assembler/epic-report-assembler.test.ts` | MODIFY -- update expectations from 3 to 2 outputs |
| CLI tests | `tests/node/cli.test.ts` | MODIFY -- add `--force` flag tests |
| CLI integration tests | `tests/node/integration/cli-integration.test.ts` | MODIFY -- add overwrite detection integration tests |
| Overwrite detector | `src/overwrite-detector.ts` | CREATE -- new module |
| Overwrite detector tests | `tests/node/overwrite-detector.test.ts` | CREATE -- unit tests |

---

## 2. New Classes/Interfaces to Create

### 2.1 Overwrite Detector Module

| # | File | Package Location | Description |
|---|------|-----------------|-------------|
| 1 | `src/overwrite-detector.ts` | `src/` (root -- utility module) | Pure function to detect existing generated artifacts in output directory |

**Interface:**

```typescript
/** Result of checking for existing artifacts in the output directory. */
export interface OverwriteCheckResult {
  readonly hasConflicts: boolean;
  readonly conflictDirs: readonly string[];
}

/** Directories that the pipeline generates and that constitute a conflict. */
const GENERATED_DIRS = [".claude", ".github", ".codex", ".agents", "docs"] as const;

/**
 * Checks whether outputDir already contains generated artifacts.
 * Returns list of conflicting directories.
 */
export function checkExistingArtifacts(outputDir: string): OverwriteCheckResult;

/**
 * Formats the overwrite error message listing conflicting directories.
 */
export function formatOverwriteError(conflictDirs: readonly string[]): string;
```

**Rationale:** Extracted as a separate module (not embedded in `cli.ts`) to keep functions testable independently and maintain SRP. The function is pure (reads filesystem, returns data) and has no side effects.

### 2.2 OverwriteError Exception

| # | File | Location | Description |
|---|------|----------|-------------|
| 2 | `src/exceptions.ts` | Append to existing file | New `OverwriteError` class extending `CliError` |

```typescript
export class OverwriteError extends CliError {
  readonly conflictDirs: readonly string[];

  constructor(conflictDirs: readonly string[]) {
    super(
      formatOverwriteError(conflictDirs),
      "OVERWRITE_CONFLICT",
    );
    this.name = "OverwriteError";
    this.conflictDirs = [...conflictDirs];
  }
}
```

**Rationale:** Extends `CliError` so `handleKnownError` in `cli.ts` already catches it without modification. The `conflictDirs` field carries context per coding standards ("exceptions MUST carry context").

### 2.3 Test Files

| # | File | Description |
|---|------|-------------|
| 3 | `tests/node/overwrite-detector.test.ts` | Unit tests for `checkExistingArtifacts` and `formatOverwriteError` |

---

## 3. Existing Files to Modify

### 3.1 `src/assembler/epic-report-assembler.ts`

**Change:** Remove `DOCS_OUTPUT_SUBDIR` from the outputs array.

| Line(s) | Current | After |
|---------|---------|-------|
| 18 | `const DOCS_OUTPUT_SUBDIR = path.join("docs", "epic");` | DELETE line (or keep as dead-code warning reminder) |
| 60-64 | `const outputs = [DOCS_OUTPUT_SUBDIR, CLAUDE_OUTPUT_SUBDIR, GITHUB_OUTPUT_SUBDIR];` | `const outputs = [CLAUDE_OUTPUT_SUBDIR, GITHUB_OUTPUT_SUBDIR];` |
| JSDoc (line 42) | "Copies ... to docs/epic/, .claude/templates/, and .github/templates/." | "Copies ... to .claude/templates/ and .github/templates/." |

**Impact:** Output changes from 3 files to 2 files. All tests referencing `docs/epic/` must be updated.

### 3.2 `src/cli.ts`

**Changes:**

1. **Add `--force` option** to `registerGenerateCommand`:
   ```typescript
   .option("-f, --force", "Overwrite existing generated artifacts.", false)
   ```

2. **Extend `GenerateOptions` interface** -- add `force?: boolean`

3. **Add overwrite check** in `handleGenerate`, between `resolveResourcesDir` and `executeGenerate`:
   ```typescript
   if (!options.dryRun && !options.force) {
     const result = checkExistingArtifacts(options.outputDir);
     if (result.hasConflicts) {
       throw new OverwriteError(result.conflictDirs);
     }
   }
   ```

4. **Import** `checkExistingArtifacts` from `./overwrite-detector.js` and `OverwriteError` from `./exceptions.js`

**Key behavior:**
- `--dry-run` bypasses the check entirely (dry-run never writes)
- `--force` bypasses the check (explicit user intent)
- No conflicts found = proceed normally (no prompt needed)
- Conflicts found without `--force` = throw `OverwriteError`

### 3.3 `src/exceptions.ts`

**Change:** Add `OverwriteError` class (see section 2.2 above).

### 3.4 `tests/node/assembler/epic-report-assembler.test.ts`

**Tests to modify:**

| Test | Current Assertion | New Assertion |
|------|-------------------|---------------|
| Cycle 3: `createsDirectoryStructure` | Checks `docs/epic/` exists | Check `.claude/templates/` exists instead |
| Cycle 4: `copiesToDocsEpicPath` | Asserts `docs/epic/` path exists | DELETE this test (no longer valid) |
| Cycle 7: `copiesContentVerbatimWithoutPlaceholderResolution` | Reads from `docs/epic/` | Read from `.claude/templates/` instead |
| Cycle 8: `returnsThreeFilePaths` | Expects length 3, checks `docs/epic/` | Expects length 2, checks only `.claude/templates/` and `.github/templates/` |
| Cycle 9: `allThreeOutputsAreIdentical` | Compares 3 outputs including `docs/epic/` | Compares 2 outputs (`.claude/templates/` and `.github/templates/`) |

**New test to add:**

| Test | Description |
|------|-------------|
| `assemble_validTemplate_doesNotCreateDocsEpic` | Asserts `docs/epic/` directory does NOT exist after assembly |

### 3.5 `tests/node/cli.test.ts`

**Tests to add:**

| # | Test Name | Scenario |
|---|-----------|----------|
| 1 | `handleGenerate_forceFlag_passedCorrectly` | Verify `--force` is parsed from argv |
| 2 | `handleGenerate_outputDirWithConflicts_noForce_throwsError` | Mock `checkExistingArtifacts` returning conflicts, verify error |
| 3 | `handleGenerate_outputDirWithConflicts_withForce_proceedsNormally` | Mock conflicts + `--force`, verify pipeline runs |
| 4 | `handleGenerate_dryRunWithConflicts_noForce_proceedsNormally` | Mock conflicts + `--dry-run` without `--force`, verify no error |
| 5 | `handleGenerate_emptyOutputDir_noForce_proceedsNormally` | Mock no conflicts, verify pipeline runs |

**Mock to add:** `vi.mock("../../src/overwrite-detector.js", ...)` -- mock `checkExistingArtifacts`.

### 3.6 `tests/node/integration/cli-integration.test.ts`

**Integration tests to add:**

| # | Test Name | Scenario |
|---|-----------|----------|
| 1 | `generate_existingArtifacts_noForce_failsWithConflictMessage` | Create temp dir with `.claude/`, run generate without `--force`, assert error message |
| 2 | `generate_existingArtifacts_withForce_succeedsOverwriting` | Create temp dir with `.claude/`, run generate with `--force`, assert success |
| 3 | `generate_dryRun_existingArtifacts_noForce_succeeds` | Create temp dir with `.claude/`, run with `--dry-run`, assert no error |

### 3.7 Golden Files -- 8 Profiles

**Action:** Delete `docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` from all 8 golden profiles.

| # | Path to Delete |
|---|---------------|
| 1 | `tests/golden/go-gin/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| 2 | `tests/golden/java-quarkus/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| 3 | `tests/golden/java-spring/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| 4 | `tests/golden/kotlin-ktor/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| 5 | `tests/golden/python-click-cli/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| 6 | `tests/golden/python-fastapi/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| 7 | `tests/golden/rust-axum/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| 8 | `tests/golden/typescript-nestjs/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |

If the `docs/epic/` directories become empty after this deletion, remove the directories entirely.

---

## 4. Dependency Direction Validation

```
src/overwrite-detector.ts  -- new module, depends on: node:fs, node:path (standard library only)
src/exceptions.ts          -- OverwriteError extends CliError (same module, no new dependencies)
src/cli.ts                 -- imports from overwrite-detector.ts and exceptions.ts

Direction: cli.ts (adapter/inbound) -> overwrite-detector.ts (utility) -> node stdlib
```

**Validation:**
- `overwrite-detector.ts` has zero external dependencies (only `node:fs`, `node:path`) -- pure utility
- `OverwriteError` in `exceptions.ts` extends `CliError` which is already in the same file -- no circular dependency
- `cli.ts` already imports from `exceptions.ts`, adding `overwrite-detector.ts` import follows the existing pattern
- No domain layer involved -- this is purely a CLI/infrastructure concern
- `epic-report-assembler.ts` change is a simple array reduction -- no new dependencies

**Result:** All dependency directions are valid. No circular dependencies introduced.

---

## 5. Integration Points

### 5.1 CLI -> Overwrite Detector -> Pipeline

The overwrite check is a **gate** between CLI option parsing and pipeline execution:

```
handleGenerate()
  |-- validateGenerateOptions()       (existing)
  |-- loadProjectConfig()             (existing)
  |-- resolveResourcesDir()           (existing)
  |-- checkExistingArtifacts()        (NEW gate)
  |     |-- if hasConflicts && !force && !dryRun -> throw OverwriteError
  |-- executeGenerate()               (existing)
```

The gate is **stateless** -- it reads the filesystem once and returns a result. It does NOT modify state.

### 5.2 EpicReportAssembler -> Pipeline

The assembler change is transparent to the pipeline. The assembler returns 2 paths instead of 3. The `PipelineResult.files` array will have one fewer entry, but the pipeline aggregation logic in `executeAssemblers` handles this automatically since it uses spread.

### 5.3 Golden File Tests -> Byte-for-Byte Comparison

The integration test (`byte-for-byte.test.ts`) compares generated output against golden files. Removing `docs/epic/` from both the assembler output and the golden files keeps them in sync. No test infrastructure changes needed.

---

## 6. Database Changes

None. This project has no database.

---

## 7. API Changes

### CLI Option Changes

| Option | Before | After |
|--------|--------|-------|
| `--force` / `-f` | Does not exist | New boolean flag, default `false` |

### Help Text Update

The `generate` command description and options should reflect:
- `--output-dir` default is `"."` (current directory)
- `--force` is required when re-generating over existing artifacts
- `--dry-run` does not require `--force`

### Exit Code Changes

| Condition | Before | After |
|-----------|--------|-------|
| Existing artifacts, no `--force` | Silently overwrites | Exit 1 with error message |
| Existing artifacts, with `--force` | N/A (flag didn't exist) | Proceeds normally (overwrite) |

---

## 8. Event Changes

None. This project is not event-driven.

---

## 9. Configuration Changes

No new environment variables or configuration files. The `--force` flag is a CLI option only.

The `GenerateOptions` interface in `cli.ts` gains one field:

```typescript
interface GenerateOptions {
  readonly config?: string;
  readonly interactive?: boolean;
  readonly outputDir: string;
  readonly resourcesDir?: string;
  readonly verbose?: boolean;
  readonly dryRun?: boolean;
  readonly force?: boolean;          // NEW
}
```

---

## 10. Risk Assessment

### Low Risk

| Risk | Mitigation |
|------|-----------|
| Golden file mismatches after removing `docs/epic/` | Delete golden files first, regenerate, run byte-for-byte tests to confirm |
| Existing CI pipelines using `ia-dev-env generate` without `--force` against non-empty directories | If output dir has no `.claude/`/`.github/` etc., no conflict is detected -- backward compatible |
| `atomicOutput` in `utils.ts` renames the entire output dir -- overwrite detection must happen BEFORE this | The gate is placed in `handleGenerate()` before `executeGenerate()` which calls `runPipeline()` |

### Medium Risk

| Risk | Mitigation |
|------|-----------|
| Race condition: artifacts created between check and pipeline execution | Acceptable for a CLI tool -- user-interactive context. The `atomicOutput` function handles the actual write atomically |
| `GENERATED_DIRS` list gets out of sync with pipeline targets | Define `GENERATED_DIRS` based on the same constants or use the `AssemblerTarget` type from `pipeline.ts`. Alternatively, document in code comment. |

### No Risk

| Item | Reason |
|------|--------|
| Breaking change for `--output-dir <new-empty-dir>` usage | Empty directories pass the check -- no `--force` needed |
| Breaking change for `--dry-run` users | Dry-run explicitly bypasses the overwrite check |
| Performance impact | `fs.existsSync` on 5 directories is negligible |

---

## Implementation Order

Following the project convention of inner-layers-first:

1. **`src/overwrite-detector.ts`** -- pure utility, no dependencies (+ unit tests)
2. **`src/exceptions.ts`** -- add `OverwriteError` class
3. **`src/assembler/epic-report-assembler.ts`** -- remove `docs/epic/` output (+ update unit tests)
4. **`src/cli.ts`** -- add `--force` flag and overwrite gate (+ update unit tests)
5. **Golden files** -- delete `docs/epic/` from all 8 profiles
6. **Integration tests** -- add CLI integration tests for overwrite scenarios
7. **CLAUDE.md** -- update help documentation (if applicable)

---

## File Count Summary

| Action | Count | Files |
|--------|-------|-------|
| CREATE | 2 | `src/overwrite-detector.ts`, `tests/node/overwrite-detector.test.ts` |
| MODIFY | 5 | `src/exceptions.ts`, `src/assembler/epic-report-assembler.ts`, `src/cli.ts`, `tests/node/assembler/epic-report-assembler.test.ts`, `tests/node/cli.test.ts` |
| MODIFY (integration) | 1 | `tests/node/integration/cli-integration.test.ts` |
| DELETE | 8 | `tests/golden/{profile}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (8 profiles) |
| **Total** | **16** | |
