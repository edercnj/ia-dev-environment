# Test Plan -- story-0005-0015: Output Directory Cleanup + Overwrite Protection

## Summary

- Total test files: 3 (1 new unit test file, 2 existing files modified)
- Total test methods: ~27 (10 UT + 6 AT + 6 IT + 5 golden file adjustments)
- Categories covered: AT (Acceptance), UT (Unit), IT (Integration)
- Estimated line coverage: >= 95%
- Estimated branch coverage: >= 90%
- Performance budget: < 15s total (no real pipeline runs in unit tests, integration uses minimal config)

## TPP Order (Transformation Priority Premise)

Tests follow TPP progression:
1. Degenerate case (empty directory -- no conflicts, nil return)
2. Constant (assembler returns fixed number of paths)
3. Variable (single conflict detection)
4. Collection (multiple conflict detection)
5. Conditional (--force flag interaction with conflicts)
6. Composition (--dry-run bypasses --force, error message formatting)

---

## 1. Unit Tests -- EpicReportAssembler Changes

### Test File: `tests/node/assembler/epic-report-assembler.test.ts` (MODIFIED)

Existing tests must be updated to reflect that `docs/epic/` is no longer an output destination.
The assembler now writes to 2 destinations instead of 3.

#### Tests to MODIFY

| # | ID | Test Name | Change | TPP Level |
|---|-----|-----------|--------|-----------|
| 1 | UT-1 | `assemble_validTemplate_returnsTwoFilePaths` | Was `returnsThreeFilePaths`. Assert `result.length === 2`. Assert `result[0]` contains `.claude/templates/`, `result[1]` contains `.github/templates/`. Assert NO element contains `docs/epic`. | L2 (constant) |
| 2 | UT-2 | `assemble_validTemplate_doesNotCopyToDocsEpicPath` | Was `copiesToDocsEpicPath`. Assert `fs.existsSync(join(tempDir, "docs", "epic", TEMPLATE_FILENAME))` is `false`. | L2 (constant) |
| 3 | UT-3 | `assemble_validTemplate_copiesToClaudeTemplatesPath` | No change -- assert file exists at `.claude/templates/`. Remains as-is. | L2 (constant) |
| 4 | UT-4 | `assemble_validTemplate_copiesToGithubTemplatesPath` | No change -- assert file exists at `.github/templates/`. Remains as-is. | L2 (constant) |

#### Tests to REMOVE

| # | Old Test Name | Reason |
|---|---------------|--------|
| 1 | `assemble_outputDirDoesNotExist_createsDirectoryStructure` | Asserts `docs/epic/` directory creation. Replace assertion to check `.claude/templates/` instead. |
| 2 | `assemble_validTemplate_copiesContentVerbatimWithoutPlaceholderResolution` | Reads from `docs/epic/` path. Update to read from `.claude/templates/` path instead. |
| 3 | `assemble_validTemplate_allThreeOutputsAreIdentical` | Compares 3 outputs. Replace with `allTwoOutputsAreIdentical` comparing only `.claude/templates/` and `.github/templates/`. |

**Detailed modifications for Cycle 3 (directory creation):**

```typescript
// BEFORE: asserts docs/epic/ path
const docsPath = join(deepOutputDir, "docs", "epic", TEMPLATE_FILENAME);
expect(fs.existsSync(docsPath)).toBe(true);

// AFTER: asserts .claude/templates/ path
const claudePath = join(deepOutputDir, ".claude", "templates", TEMPLATE_FILENAME);
expect(fs.existsSync(claudePath)).toBe(true);
```

**Detailed modifications for Cycle 7 (verbatim copy):**

```typescript
// BEFORE: reads from docs/epic/
const outputPath = join(tempDir, "docs", "epic", TEMPLATE_FILENAME);

// AFTER: reads from .claude/templates/
const outputPath = join(tempDir, ".claude", "templates", TEMPLATE_FILENAME);
```

**Detailed modifications for Cycle 9 (parity):**

```typescript
// BEFORE: compares 3 files
const docsContent = fs.readFileSync(join(tempDir, "docs", "epic", TEMPLATE_FILENAME), "utf-8");
const claudeContent = fs.readFileSync(...);
const githubContent = fs.readFileSync(...);
expect(claudeContent).toBe(docsContent);
expect(githubContent).toBe(docsContent);

// AFTER: compares 2 files
const claudeContent = fs.readFileSync(join(tempDir, ".claude", "templates", TEMPLATE_FILENAME), "utf-8");
const githubContent = fs.readFileSync(join(tempDir, ".github", "templates", TEMPLATE_FILENAME), "utf-8");
expect(githubContent).toBe(claudeContent);
```

---

## 2. Unit Tests -- checkExistingArtifacts Function

### Test File: `tests/node/overwrite-detector.test.ts` (NEW)

Module under test: `src/overwrite-detector.ts` (new module).

**Function signature:**

```typescript
interface OverwriteCheckResult {
  readonly hasConflicts: boolean;
  readonly conflictDirs: readonly string[];
}

function checkExistingArtifacts(outputDir: string): OverwriteCheckResult;
```

**Artifact directories to detect:** `.claude/`, `.github/`, `docs/`

| # | ID | Test Name | Description | TPP Level | Parallel |
|---|-----|-----------|-------------|-----------|----------|
| 1 | UT-5 | `checkExistingArtifacts_emptyDirectory_returnsNoConflicts` | Empty temp dir: `hasConflicts === false`, `conflictDirs === []` | L1 (nil) | yes |
| 2 | UT-6 | `checkExistingArtifacts_directoryWithClaudeOnly_returnsOneConflict` | Dir contains `.claude/`: `hasConflicts === true`, `conflictDirs === [".claude/"]` | L3 (variable) | yes |
| 3 | UT-7 | `checkExistingArtifacts_directoryWithClaudeAndGithub_returnsBothConflicts` | Dir contains `.claude/` and `.github/`: `conflictDirs.length === 2`, both listed | L5 (collection) | yes |
| 4 | UT-8 | `checkExistingArtifacts_directoryWithNonArtifactFiles_returnsNoConflicts` | Dir contains `src/`, `package.json`: `hasConflicts === false` | L3 (variable) | yes |
| 5 | UT-9 | `checkExistingArtifacts_directoryWithAllThreeArtifacts_returnsAllConflicts` | Dir contains `.claude/`, `.github/`, `docs/`: `conflictDirs.length === 3` | L5 (collection) | yes |
| 6 | UT-10 | `checkExistingArtifacts_nonexistentDirectory_returnsNoConflicts` | Dir does not exist: `hasConflicts === false` (nothing to conflict with) | L1 (nil) | yes |

**Setup pattern:**

```typescript
import { mkdtemp, rm, mkdir } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(join(tmpdir(), "overwrite-detector-test-"));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});
```

---

## 3. Unit Tests -- CLI --force Flag

### Test File: `tests/node/cli.test.ts` (MODIFIED)

New tests added to the existing `describe("generate command")` block.

| # | ID | Test Name | Description | TPP Level | Parallel |
|---|-----|-----------|-------------|-----------|----------|
| 1 | UT-11 | `generate_withForce_passesForceToHandler` | `parseCli(["generate", "--config", path, "--force"])` -- verify `--force` is recognized and passed through | L2 (constant) | yes |
| 2 | UT-12 | `generate_withoutForce_defaultsToFalse` | Default `parseCli(["generate", "--config", path])` -- force defaults to `false` | L2 (constant) | yes |

**Mock setup:**

The existing mock for `checkExistingArtifacts` must be added:

```typescript
const mockCheckExistingArtifacts = vi.fn<(outputDir: string) => OverwriteCheckResult>();

vi.mock("../../src/overwrite-detector.js", () => ({
  checkExistingArtifacts: (...args: unknown[]) =>
    mockCheckExistingArtifacts(args[0] as string),
}));
```

Default mock in `beforeEach`:

```typescript
mockCheckExistingArtifacts.mockReturnValue({ hasConflicts: false, conflictDirs: [] });
```

---

## 4. Acceptance Tests (Outer Loop)

### Test File: `tests/node/cli.test.ts` (MODIFIED) -- new `describe("overwrite protection")` block

These tests validate the complete behavior as described in the Gherkin scenarios.
They use the same mock infrastructure as the existing CLI tests.

| # | ID | Test Name | Gherkin Scenario | TPP Level | Parallel |
|---|-----|-----------|-----------------|-----------|----------|
| 1 | AT-1 | `generate_docsEpicNotGenerated_inOutput` | docs/epic/ not generated in output | L2 (constant) | no |
| 2 | AT-2 | `generate_emptyDirectory_succeedsWithoutForce` | Empty dir works without --force | L1 (degenerate) | yes |
| 3 | AT-3 | `generate_directoryWithArtifacts_failsWithoutForce` | Existing artifacts fail without --force | L4 (conditional) | yes |
| 4 | AT-4 | `generate_directoryWithArtifacts_succeedsWithForce` | --force overwrites existing artifacts | L4 (conditional) | yes |
| 5 | AT-5 | `generate_dryRun_bypassesForceCheck` | --dry-run bypasses --force check | L6 (composition) | yes |
| 6 | AT-6 | `generate_conflictError_listsConflictingDirectories` | Error message lists conflicting dirs | L5 (collection) | yes |

**AT-1 Detail:**

This test requires a real pipeline run (no mocks for the assembler) to verify `docs/epic/` is absent from output.

```typescript
// Setup: real pipeline run with minimal config
// Assert: docs/epic/ directory does NOT exist in output
// Assert: .claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md EXISTS
// Assert: .github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md EXISTS
```

Note: AT-1 belongs in integration tests (IT section) because it requires real pipeline execution.
It is listed here for Gherkin traceability but implemented as IT-5 below.

**AT-2 Detail:**

```typescript
// Mock: checkExistingArtifacts returns { hasConflicts: false, conflictDirs: [] }
// Action: parseCli(["generate", "--config", configPath])
// Assert: mockRunPipeline was called (generation proceeded)
// Assert: exitSpy NOT called with 1
```

**AT-3 Detail:**

```typescript
// Mock: checkExistingArtifacts returns { hasConflicts: true, conflictDirs: [".claude/", ".github/"] }
// Action: parseCli(["generate", "--config", configPath])  (NO --force)
// Assert: mockRunPipeline was NOT called (blocked)
// Assert: errorSpy called with message containing "existing generated artifacts"
// Assert: exitSpy called with 1
```

**AT-4 Detail:**

```typescript
// Mock: checkExistingArtifacts returns { hasConflicts: true, conflictDirs: [".claude/", ".github/"] }
// Action: parseCli(["generate", "--config", configPath, "--force"])
// Assert: mockRunPipeline WAS called (force bypasses check)
// Assert: exitSpy NOT called with 1
```

**AT-5 Detail:**

```typescript
// Mock: checkExistingArtifacts returns { hasConflicts: true, conflictDirs: [".claude/"] }
// Action: parseCli(["generate", "--config", configPath, "--dry-run"])
// Assert: mockRunPipeline WAS called with dryRun=true
// Assert: checkExistingArtifacts was NOT called (skipped entirely for dry-run)
```

**AT-6 Detail:**

```typescript
// Mock: checkExistingArtifacts returns { hasConflicts: true, conflictDirs: [".claude/", ".github/", "docs/"] }
// Action: parseCli(["generate", "--config", configPath])
// Assert: errorSpy called with message containing ".claude/"
// Assert: errorSpy called with message containing ".github/"
// Assert: errorSpy called with message containing "docs/"
// Assert: errorSpy called with message containing "--force"
```

---

## 5. Integration Tests

### Test File: `tests/node/integration/cli-integration.test.ts` (MODIFIED)

New `describe("OverwriteProtection")` block within the existing CLI integration suite.
These tests use real file system operations and the real CLI (no mocks for `checkExistingArtifacts`).

| # | ID | Test Name | Description | TPP Level | Parallel |
|---|-----|-----------|-------------|-----------|----------|
| 1 | IT-1 | `generate_withForceInDirectoryWithArtifacts_succeeds` | Pre-create `.claude/` and `.github/` in output dir, run with `--force` -- pipeline completes | L4 (conditional) | no |
| 2 | IT-2 | `generate_withoutForceInDirectoryWithArtifacts_showsError` | Pre-create `.claude/` in output dir, run without `--force` -- error message displayed, no files written | L4 (conditional) | no |
| 3 | IT-3 | `generate_withDryRunInDirectoryWithArtifacts_succeeds` | Pre-create `.claude/` in output dir, run with `--dry-run` -- succeeds without requiring `--force` | L6 (composition) | no |
| 4 | IT-4 | `generate_helpText_includesForceOption` | Run `createCli()`, get generate command help text -- contains `--force` with description | L2 (constant) | yes |
| 5 | IT-5 | `generate_fullPipeline_docsEpicNotInOutput` | Run real pipeline with minimal config -- `docs/epic/` dir NOT present, `.claude/templates/` and `.github/templates/` templates ARE present | L2 (constant) | no |

**IT-1 Detail:**

```typescript
// Setup: create tmpDir with .claude/ and .github/ subdirs (with dummy files)
const outputDir = path.join(tmpDir, "output");
fs.mkdirSync(path.join(outputDir, ".claude"), { recursive: true });
fs.writeFileSync(path.join(outputDir, ".claude", "dummy.md"), "old content");
fs.mkdirSync(path.join(outputDir, ".github"), { recursive: true });

// Action:
const cli = createCli();
await cli.parseAsync([
  "node", "test", "generate",
  "--config", configPath,
  "--output-dir", outputDir,
  "--resources-dir", RESOURCES_DIR,
  "--force",
]);

// Assert: exitSpy NOT called with 1
// Assert: .claude/ now contains generated artifacts (not just dummy.md)
```

**IT-2 Detail:**

```typescript
// Setup: create tmpDir with .claude/ subdir
const outputDir = path.join(tmpDir, "output");
fs.mkdirSync(path.join(outputDir, ".claude"), { recursive: true });

// Action:
const cli = createCli();
await cli.parseAsync([
  "node", "test", "generate",
  "--config", configPath,
  "--output-dir", outputDir,
  "--resources-dir", RESOURCES_DIR,
]);

// Assert: errorSpy called with message containing "existing generated artifacts"
// Assert: exitSpy called with 1
```

**IT-3 Detail:**

```typescript
// Setup: create tmpDir with .claude/ subdir
const outputDir = path.join(tmpDir, "output");
fs.mkdirSync(path.join(outputDir, ".claude"), { recursive: true });

// Action:
const cli = createCli();
await cli.parseAsync([
  "node", "test", "generate",
  "--config", configPath,
  "--output-dir", outputDir,
  "--resources-dir", RESOURCES_DIR,
  "--dry-run",
]);

// Assert: exitSpy NOT called with 1
// Assert: no new files created in outputDir (dry-run)
```

**IT-4 Detail:**

```typescript
// Action:
const cli = createCli();
const generateCmd = cli.commands.find((cmd) => cmd.name() === "generate");
const help = generateCmd!.helpInformation();

// Assert:
expect(help).toContain("--force");
expect(help).toContain("overwrite");
```

**IT-5 Detail (docs/epic/ removal verification):**

```typescript
// Setup: run real pipeline with minimal config
const outputDir = path.join(tmpDir, "output");
fs.mkdirSync(outputDir);
const configPath = path.join(FIXTURES_DIR, "minimal.yaml");
const cli = createCli();
await cli.parseAsync([
  "node", "test", "generate",
  "--config", configPath,
  "--output-dir", outputDir,
  "--resources-dir", RESOURCES_DIR,
]);

// Assert: docs/epic/ does NOT exist
expect(fs.existsSync(path.join(outputDir, "docs", "epic"))).toBe(false);

// Assert: templates still exist in both locations
expect(fs.existsSync(
  path.join(outputDir, ".claude", "templates", "_TEMPLATE-EPIC-EXECUTION-REPORT.md"),
)).toBe(true);
expect(fs.existsSync(
  path.join(outputDir, ".github", "templates", "_TEMPLATE-EPIC-EXECUTION-REPORT.md"),
)).toBe(true);
```

---

## 6. Golden File Updates

### Affected Files: 8 profiles x 1 directory = 8 deletions

The `docs/epic/` directory and its contents must be removed from ALL golden file profiles:

| # | Golden File Path | Action |
|---|-----------------|--------|
| 1 | `tests/golden/go-gin/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |
| 2 | `tests/golden/java-quarkus/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |
| 3 | `tests/golden/java-spring/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |
| 4 | `tests/golden/kotlin-ktor/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |
| 5 | `tests/golden/python-click-cli/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |
| 6 | `tests/golden/python-fastapi/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |
| 7 | `tests/golden/rust-axum/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |
| 8 | `tests/golden/typescript-nestjs/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | DELETE |

After deletion, also remove the now-empty `docs/epic/` directories from each golden profile.

**Verification:** The existing `byte-for-byte.test.ts` suite (8 profiles) will automatically validate that the pipeline output matches the updated golden files. No changes needed to the test file itself -- only the golden data changes.

---

## 7. New Source Files

### `src/overwrite-detector.ts` (NEW)

```typescript
export interface OverwriteCheckResult {
  readonly hasConflicts: boolean;
  readonly conflictDirs: readonly string[];
}

const ARTIFACT_DIRS = [".claude", ".github", "docs"] as const;

export function checkExistingArtifacts(outputDir: string): OverwriteCheckResult {
  // Check for ARTIFACT_DIRS existence in outputDir
  // Return { hasConflicts, conflictDirs }
}
```

### `src/cli.ts` (MODIFIED)

Changes to `GenerateOptions`, `registerGenerateCommand`, and `handleGenerate`:

```typescript
interface GenerateOptions {
  readonly config?: string;
  readonly interactive?: boolean;
  readonly outputDir: string;
  readonly resourcesDir?: string;
  readonly verbose?: boolean;
  readonly dryRun?: boolean;
  readonly force?: boolean;        // NEW
}
```

New option registration:

```typescript
.option("-f, --force", "Overwrite existing generated artifacts.", false)
```

New guard in `handleGenerate` (before `executeGenerate`):

```typescript
if (!options.dryRun) {
  const { hasConflicts, conflictDirs } = checkExistingArtifacts(options.outputDir);
  if (hasConflicts && !options.force) {
    const dirList = conflictDirs.map((d) => `  - ${d} (exists)`).join("\n");
    throw new CliError(
      `Output directory contains existing generated artifacts:\n${dirList}\n\nUse --force to overwrite existing files, or specify a different --output-dir.`,
      "OVERWRITE_CONFLICT",
    );
  }
}
```

---

## 8. Gherkin-to-Test Traceability Matrix

| Gherkin Scenario | Test ID(s) | Category |
|-----------------|------------|----------|
| Empty dir works without --force | AT-2, IT-1 (clean dir path) | AT + IT |
| Existing artifacts fail without --force | AT-3, IT-2 | AT + IT |
| --force overwrites existing artifacts | AT-4, IT-1 | AT + IT |
| --dry-run bypasses --force check | AT-5, IT-3 | AT + IT |
| docs/epic/ not generated in output | UT-1, UT-2, IT-5 | UT + IT |
| Error message lists conflicting dirs | AT-6 | AT |
| Help text documents --force | IT-4 | IT |

---

## 9. Implementation Order

```
Step 1: Unit tests for overwrite-detector (inner-layer first, TDD red phase)
  1a. Create tests/node/overwrite-detector.test.ts with UT-5..UT-10
  1b. Create src/overwrite-detector.ts (make tests green)

Step 2: Unit tests for EpicReportAssembler changes
  2a. Update tests/node/assembler/epic-report-assembler.test.ts (UT-1..UT-4)
  2b. Modify src/assembler/epic-report-assembler.ts (remove docs/epic)

Step 3: Unit tests for CLI --force flag
  3a. Add UT-11, UT-12 to tests/node/cli.test.ts
  3b. Add --force option to src/cli.ts

Step 4: Acceptance tests for overwrite protection
  4a. Add AT-2..AT-6 to tests/node/cli.test.ts
  4b. Wire checkExistingArtifacts into handleGenerate

Step 5: Integration tests
  5a. Add IT-1..IT-5 to tests/node/integration/cli-integration.test.ts

Step 6: Golden file updates
  6a. Delete docs/epic/ from all 8 golden profiles
  6b. Run byte-for-byte tests to verify

Step 7: Verification
  7a. Run full test suite with coverage
  7b. Verify coverage >= 95% line, >= 90% branch
  7c. Verify all 8 golden profiles pass byte-for-byte
  7d. Verify --help output includes --force documentation
```

---

## 10. Coverage Estimation

| Module | Public Functions | Branches | Est. Tests | Line % | Branch % |
|--------|-----------------|----------|-----------|--------|----------|
| `overwrite-detector.ts` (new) | 1 (`checkExistingArtifacts`) | 4 (empty dir, nonexistent dir, partial artifacts, all artifacts) | 6 | 100% | 100% |
| `epic-report-assembler.ts` (modified) | 1 (`assemble`) | 3 (template missing, sections missing, success) | 10 (existing, updated) | 100% | 100% |
| `cli.ts` (modified) | 3 (`handleGenerate`, `registerGenerateCommand`, `createCli`) | 6 (force+conflicts, force+clean, no-force+conflicts, no-force+clean, dry-run bypass, help) | 8 | >= 95% | >= 90% |
| **Total new/modified** | **5** | **13** | **~24** | **>= 95%** | **>= 90%** |

---

## 11. Quality Checks

1. [x] Every Gherkin scenario maps to at least 1 test (traceability matrix in section 8)
2. [x] Every error path tested (AT-3: conflict without --force, AT-6: error message content)
3. [x] Degenerate cases covered (UT-5: empty dir, UT-10: nonexistent dir, AT-2: clean dir)
4. [x] Test categories: UT (unit) + AT (acceptance) + IT (integration)
5. [x] TPP order followed: nil -> constant -> variable -> collection -> conditional -> composition
6. [x] No mocking of domain logic -- only external boundaries mocked (file system in unit tests via temp dirs, pipeline in CLI tests)
7. [x] Temp directories with `beforeEach`/`afterEach` cleanup -- no test interdependence
8. [x] Golden files updated across all 8 profiles
9. [x] Backward compatibility verified (empty dir path unchanged, --force defaults to false)
10. [x] Help text coverage (IT-4)

---

## 12. File Summary

### New Files

| # | File | Lines (est.) | Description |
|---|------|-------------|-------------|
| 1 | `tests/node/overwrite-detector.test.ts` | ~100 | Unit tests for checkExistingArtifacts |
| 2 | `src/overwrite-detector.ts` | ~30 | Overwrite detection function |

### Modified Files

| # | File | Change |
|---|------|--------|
| 1 | `src/assembler/epic-report-assembler.ts` | Remove `DOCS_OUTPUT_SUBDIR` from outputs array |
| 2 | `src/cli.ts` | Add `--force` flag, integrate `checkExistingArtifacts` guard |
| 3 | `tests/node/assembler/epic-report-assembler.test.ts` | Update cycles 3, 4, 7, 8, 9 for 2-path output |
| 4 | `tests/node/cli.test.ts` | Add UT-11, UT-12, AT-2..AT-6 + mock for overwrite-detector |
| 5 | `tests/node/integration/cli-integration.test.ts` | Add IT-1..IT-5 in new describe block |

### Deleted Files

| # | File | Reason |
|---|------|--------|
| 1-8 | `tests/golden/*/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | docs/epic/ no longer generated (all 8 profiles) |

### Total Estimated New Lines: ~200 (source) + ~150 (test modifications)
