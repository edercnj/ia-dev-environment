# Implementation Plan — STORY-019: Integration Tests + Parity Verification

## Story Summary

Implement the final quality gate for the Python-to-TypeScript migration: a verifier module (`src/verifier.ts`) and a comprehensive integration test suite that runs the TS CLI with representative YAML fixtures and compares output byte-for-byte against golden reference files. This story validates that the entire migration produces identical output to the Python original.

**Blocked by:** STORY-018 (CLI entry point) — complete.
**Blocks:** STORY-020.

---

## 1. Affected Layers and Components

| Layer | Component | Action | Path |
|-------|-----------|--------|------|
| library | Verifier module | **Create** | `src/verifier.ts` |
| models | VerificationResult, FileDiff | Read-only | `src/models.ts` |
| config | loadConfig | Read-only | `src/config.ts` |
| assembler | runPipeline | Read-only | `src/assembler/pipeline.ts` |
| cli | createCli, runCli | Read-only | `src/cli.ts` |
| domain | validateStack | Read-only | `src/domain/validator.ts` |
| exceptions | CliError, ConfigValidationError | Read-only | `src/exceptions.ts` |
| fixtures | YAML config fixtures (integration) | **Create** | `tests/fixtures/integration/*.yaml` |
| tests | Verifier unit tests | **Create** | `tests/node/verifier.test.ts` |
| tests | Byte-for-byte parity tests | **Create** | `tests/node/integration/byte-for-byte.test.ts` |
| tests | E2E verification tests | **Create** | `tests/node/integration/e2e-verification.test.ts` |
| tests | Verification edge cases | **Create** | `tests/node/integration/verification-edge-cases.test.ts` |
| tests | Dry-run and validate integration tests | **Create** | `tests/node/integration/cli-integration.test.ts` |
| tests | Test helpers for file tree creation | **Create** | `tests/helpers/file-tree.ts` |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/verifier.ts` — Output Verification Module

**Purpose:** Migrate `src/ia_dev_env/verifier.py` to TypeScript. Compares two directory trees byte-for-byte, reporting mismatches, missing files, and extra files. Generates unified diffs for text files and handles binary files gracefully.

**Constants:**

```typescript
export const BINARY_DIFF_MESSAGE = "<binary files differ>";
export const MAX_DIFF_LINES = 200;
```

**Exported Functions:**

```typescript
/**
 * Compare two directory trees byte-for-byte.
 * Returns a VerificationResult with mismatches, missing, and extra files.
 *
 * @param actualDir - Directory produced by the TypeScript CLI
 * @param referenceDir - Golden reference directory
 * @throws Error if either directory does not exist or is not a directory
 */
export function verifyOutput(
  actualDir: string,
  referenceDir: string,
): VerificationResult
```

**Internal (non-exported) Functions:**

```typescript
/** Validate that a path exists and is a directory. */
function validateDirectory(dirPath: string, name: string): void

/** Walk directory recursively, return sorted relative paths. */
function collectRelativePaths(baseDir: string): string[]

/** Compare common files, return list of mismatches. */
function findMismatches(
  actualDir: string,
  referenceDir: string,
  commonPaths: string[],
): FileDiff[]

/** Compare two files byte-for-byte, return FileDiff if different. */
function compareFiles(
  actualFile: string,
  referenceFile: string,
  relativePath: string,
): FileDiff | null

/** Generate unified diff string for text files. Binary returns BINARY_DIFF_MESSAGE. */
function generateTextDiff(
  actualPath: string,
  referencePath: string,
  relativePath: string,
): string
```

**Key design decisions:**

1. **Naming divergence from Python:** The Python verifier uses `python_dir` / `python_file` / `python_size` parameter names. The TS version uses `actualDir` / `actualFile` / `actualSize` instead, since the TS verifier compares TS CLI output against golden files (not Python vs reference). However, the `FileDiff` model in `src/models.ts` already uses `pythonSize` and `referenceSize` field names from the STORY-003 migration. We keep those model field names unchanged for backward compatibility but use `actualSize` in internal verifier logic, mapping to `pythonSize` when constructing `FileDiff`. This is a deliberate naming mismatch documented here.

2. **Synchronous API:** The verifier uses synchronous `fs.readFileSync`, `fs.readdirSync`, `fs.statSync` (matching the Python synchronous I/O). The golden files and output directories are local, so async provides no benefit and would complicate the API.

3. **Diff generation:** Uses a simple line-by-line diff algorithm. Node.js has no built-in `difflib.unified_diff` equivalent, so we implement a minimal unified diff generator or use the `diff` npm package. Decision: implement a minimal unified diff to avoid a new dependency, keeping the output format compatible with the Python version.

**Estimated size:** ~120 lines (well under 250-line limit).

### 2.2 `tests/helpers/file-tree.ts` — Test Helper for File Tree Creation

**Purpose:** Port the Python `conftest.create_file_tree` helper used extensively in verifier tests. Creates a directory tree from a simple `Record<string, string>` mapping.

```typescript
/**
 * Create a file tree from a mapping of relative paths to content.
 * Automatically creates intermediate directories.
 */
export function createFileTree(
  baseDir: string,
  files: Record<string, string>,
): void

/**
 * Create a binary file at the given path.
 * Automatically creates intermediate directories.
 */
export function createBinaryFile(
  filePath: string,
  data: Buffer,
): void
```

**Estimated size:** ~25 lines.

### 2.3 YAML Fixture Configs — `tests/fixtures/integration/`

Create 10+ YAML fixture files for integration tests. These are the input configs fed to the TS CLI.

| # | Fixture File | Description | Source/Basis |
|---|-------------|-------------|-------------|
| 1 | `minimal.yaml` | Only required fields (project, architecture, interfaces, language, framework) | New, based on `minimal_v3_config.yaml` pattern |
| 2 | `full.yaml` | All sections populated (data, security, infra, observability, testing, mcp, conventions) | New, comprehensive |
| 3 | `java-spring-rest.yaml` | Java 21 + Spring Boot microservice with REST, gRPC, PostgreSQL, Redis, K8s | Copy/adapt from `resources/config-templates/setup-config.java-spring.yaml` |
| 4 | `python-fastapi.yaml` | Python 3.12 + FastAPI microservice with REST, MongoDB | Copy/adapt from `resources/config-templates/setup-config.python-fastapi.yaml` |
| 5 | `go-gin-grpc.yaml` | Go 1.22 + Gin microservice with gRPC, Kafka | Copy/adapt from `resources/config-templates/setup-config.go-gin.yaml` |
| 6 | `kotlin-ktor-events.yaml` | Kotlin 2.0 + Ktor event-driven with Kafka | Copy/adapt from `resources/config-templates/setup-config.kotlin-ktor.yaml` |
| 7 | `ts-nestjs-fullstack.yaml` | TypeScript 5 + NestJS monolith with REST + GraphQL | Copy/adapt from `resources/config-templates/setup-config.typescript-nestjs.yaml` |
| 8 | `rust-axum-library.yaml` | Rust + Axum library style, no interfaces | Copy/adapt from `resources/config-templates/setup-config.rust-axum.yaml`, change style to `library` and remove interfaces |
| 9 | `config-v2.yaml` | Legacy v2 format with `type` and `stack` fields | New, based on `valid_v2_stack_config.yaml` pattern |
| 10 | `config-with-mcp.yaml` | Config with MCP servers configured | New, based on full.yaml + MCP servers |
| 11 | `invalid-missing-section.yaml` | Missing required `language` section (for error tests) | New |
| 12 | `invalid-bad-yaml.yaml` | Malformed YAML syntax (for error tests) | New |

### 2.4 Test Files

#### `tests/node/verifier.test.ts` — Verifier Unit Tests

Direct port of `tests/test_verifier.py`. Tests the verifier module in isolation with synthetic file trees.

**Test classes/groups and scenarios:**

```
TestVerifyOutput:
  verifyOutput_identicalDirs_returnsSuccess
  verifyOutput_identicalDirs_totalFilesCorrect
  verifyOutput_mismatchDetected_returnsFailure
  verifyOutput_mismatchContainsDiffString
  verifyOutput_mismatchContainsFileSizes
  verifyOutput_missingFileDetected
  verifyOutput_extraFileDetected
  verifyOutput_nestedDirectoriesCompared
  verifyOutput_emptyDirs_returnsSuccess
  verifyOutput_binaryFileMismatch_handled
  verifyOutput_whitespaceDifferenceDetected

TestCollectRelativePaths:
  collectRelativePaths_returnsSortedPaths
  collectRelativePaths_includesNestedFiles
  collectRelativePaths_emptyDir_returnsEmpty

TestValidateDirectory:
  validateDirectory_nonexistentDir_throwsError
  validateDirectory_fileNotDir_throwsError
  validateDirectory_existingDir_noError
```

#### `tests/node/integration/byte-for-byte.test.ts` — Byte-for-Byte Parity Tests

Direct port of `tests/test_byte_for_byte.py`. Parametrized tests that run the pipeline for each config profile and compare against golden reference files.

**Config profiles:**

```typescript
const CONFIG_PROFILES = [
  "go-gin",
  "java-quarkus",
  "java-spring",
  "kotlin-ktor",
  "python-click-cli",
  "python-fastapi",
  "rust-axum",
  "typescript-nestjs",
] as const;
```

**Test scenarios (parametrized per profile):**

```
TestByteForByte:
  pipelineMatchesGoldenFiles_{profile}
  noMissingFiles_{profile}
  noExtraFiles_{profile}
  pipelineSuccessForProfile_{profile}
  totalFilesGreaterThanZero_{profile}
```

**Implementation approach:**
- Uses `loadConfig` + `runPipeline` directly (not via CLI process spawn) for speed
- Reads config from `resources/config-templates/setup-config.{profile}.yaml`
- Compares output against `tests/golden/{profile}/`
- Skips gracefully if golden files are missing (with message: "Golden files not found. Run: npx tsx scripts/generate-golden.ts --all")
- Uses vitest `describe.each` or `it.each` for parametrization

#### `tests/node/integration/e2e-verification.test.ts` — E2E Verification Tests

Direct port of `tests/test_e2e_verification.py`. Full end-to-end flow: load config, run pipeline, verify against golden.

**Test scenarios:**

```
TestE2EVerification:
  fullFlowForProfile_{profile} (parametrized, 8 profiles)
```

#### `tests/node/integration/verification-edge-cases.test.ts` — Edge Case Tests

Direct port of `tests/test_verification_edge_cases.py`.

**Test scenarios:**

```
TestMinimalConfig:
  minimalConfig_producesOutput
  minimalConfig_verifiesAgainstSelf

TestIdempotency:
  pipeline_isIdempotent

TestEmptyReference:
  allFiles_reportedAsExtra

TestEmptyOutput:
  allFiles_reportedAsMissing

TestInvalidDirectories:
  nonexistentActualDir_throwsError
  nonexistentReferenceDir_throwsError
  fileAsActualDir_throwsError
  fileAsReferenceDir_throwsError
```

#### `tests/node/integration/cli-integration.test.ts` — CLI Integration Tests

New tests not present in Python (story requirement for dry-run, validate, and error handling).

**Test scenarios:**

```
TestDryRun:
  dryRun_producesNoOutputFiles
  dryRun_returnsFileList
  dryRun_containsDryRunWarning
  dryRun_withValidConfig_succeeds

TestValidateCommand:
  validate_validConfig_printsValid
  validate_invalidConfig_exitsWithError
  validate_missingSection_showsSectionName

TestErrorHandling:
  generate_invalidConfigPath_showsError
  generate_malformedYaml_showsParseError
  generate_missingRequiredSection_showsValidationError
  validate_nonexistentConfig_showsError
```

---

## 3. Existing Classes to Modify

### 3.1 `src/models.ts` — No changes required

`VerificationResult` and `FileDiff` already exist (lines 490-529). The current `FileDiff` uses `pythonSize` and `referenceSize` field names. These are kept as-is for backward compatibility. The verifier maps its internal `actualSize` concept to the existing `pythonSize` field.

### 3.2 `package.json` — Potential test script addition

Add an integration test script to run integration tests separately from unit tests:

```json
{
  "scripts": {
    "test:integration": "vitest run --config vitest.integration.config.ts"
  }
}
```

Alternatively, use vitest's built-in `--testPathPattern` to filter integration tests. Decision: use a single vitest config with glob patterns, and add a convenience script.

### 3.3 `vitest.config.ts` — Potential include pattern update

The current config includes `tests/**/*.test.ts`. This already covers the new integration test paths (`tests/node/integration/*.test.ts`). No change needed unless we want separate configs for unit vs integration.

**Decision:** Keep a single `vitest.config.ts`. Add a separate `vitest.integration.config.ts` only if integration tests need different timeouts or setup.

---

## 4. Dependency Direction Validation

```
src/verifier.ts ──imports──> src/models.ts (VerificationResult, FileDiff)
                ──imports──> node:fs, node:path (standard library)

tests/node/verifier.test.ts ──imports──> src/verifier.ts
                            ──imports──> tests/helpers/file-tree.ts

tests/node/integration/*.test.ts ──imports──> src/verifier.ts
                                 ──imports──> src/config.ts (loadConfig)
                                 ──imports──> src/assembler/pipeline.ts (runPipeline)
                                 ──imports──> src/models.ts (ProjectConfig, etc.)
                                 ──imports──> tests/helpers/file-tree.ts
                                 ──imports──> tests/fixtures/project-config.fixture.ts
```

**Validated:**
- `src/verifier.ts` depends only on `src/models.ts` (domain models) and Node.js standard library
- No circular dependencies introduced
- Verifier does not import any assembler, CLI, or config code
- Test files import source modules in the correct direction (test -> source, never source -> test)
- No domain code imports verifier code

---

## 5. Integration Points

### 5.1 Pipeline Orchestrator (`src/assembler/pipeline.ts`)

Integration tests call `runPipeline(config, resourcesDir, outputDir, dryRun)` directly. This is the same function used by the CLI. The integration tests bypass the CLI command parsing layer to test the core generation logic.

**Signature:** `async function runPipeline(config, resourcesDir, outputDir, dryRun): Promise<PipelineResult>`

### 5.2 Config Loader (`src/config.ts`)

Integration tests call `loadConfig(path)` to parse YAML fixture files into `ProjectConfig`.

**Signature:** `function loadConfig(path: string): ProjectConfig`

### 5.3 Verifier (`src/verifier.ts`)

Integration tests use the verifier to compare pipeline output against golden reference directories.

**Signature:** `function verifyOutput(actualDir, referenceDir): VerificationResult`

### 5.4 Golden Reference Files (`tests/golden/`)

8 golden directories already exist, generated by the Python CLI:
- `go-gin`, `java-quarkus`, `java-spring`, `kotlin-ktor`
- `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`

These are the byte-for-byte reference output. The integration tests compare TS CLI output against these.

### 5.5 Config Templates (`resources/config-templates/`)

9 existing config templates serve as inputs for the golden file generation and integration tests. The new integration fixtures in `tests/fixtures/integration/` are derived from these but may have modifications for specific test scenarios.

### 5.6 CLI Integration (`src/cli.ts`)

For CLI-level integration tests (dry-run, validate, error handling), tests invoke the CLI via:
1. Direct function calls to `createCli()` + `parseAsync()` with `.exitOverride()` (preferred, avoids process spawning)
2. Or via `child_process.execFile` spawning `npx tsx src/index.ts` (backup approach for true E2E)

**Decision:** Use approach 1 (direct invocation) for speed and reliability. Reserve approach 2 for smoke tests only if needed.

---

## 6. Database Changes

N/A — no database in this project.

---

## 7. API Changes

N/A — CLI tool with no HTTP/gRPC API.

---

## 8. Event Changes

N/A — no event-driven components.

---

## 9. Configuration Changes

### 9.1 New npm Script

```json
"test:integration": "vitest run tests/node/integration/"
```

This enables running integration tests separately (they are slower due to running the full pipeline).

### 9.2 Optional: `vitest.integration.config.ts`

If integration tests need longer timeouts:

```typescript
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["tests/node/integration/**/*.test.ts"],
    testTimeout: 30000, // 30s per test (pipeline generation can be slow)
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      include: ["src/**/*.ts"],
      exclude: ["dist/**", "resources/**", "tests/**"],
    },
  },
});
```

**Decision:** Start without a separate config. Add one only if timeouts become an issue.

### 9.3 No New Environment Variables

No new env vars needed. The verifier and tests use only file paths derived from the project structure.

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden files stale or incomplete | Medium | High | Golden files already exist for 8 profiles and are committed. Verify they were generated by the latest Python version. Add a script to regenerate (`scripts/generate-golden.ts`). |
| Unified diff output format differs from Python `difflib` | Medium | Medium | The diff output is used for developer debugging, not for automated comparison. Minor format differences are acceptable. Test that diffs contain `---` and `+++` markers. |
| `FileDiff.pythonSize` naming mismatch with TS verifier semantics | Low | Low | Document the naming divergence. The field stores "actual size" in the TS context but keeps the `pythonSize` name from the data contract. This is an intentional tradeoff to avoid breaking the model API. |
| Integration test performance (8 profiles x full pipeline) | Medium | Medium | Each pipeline run takes ~100-500ms. Total: ~4s for 8 profiles. Well within the 60s budget. Use `describe.each` to parallelize where possible. |
| Binary file handling edge cases | Low | Low | Test with synthetic binary files (bytes `\x00\x01\x02` etc.). The verifier catches `UnicodeDecodeError` equivalent (try/catch around text decoding). |
| Missing `diff` npm package (no native unified diff in Node.js) | Medium | Medium | Implement a minimal unified diff function (~30 lines) rather than adding a dependency. The Python `difflib.unified_diff` output format is simple: header lines (`---`, `+++`, `@@`) + context/change lines. |
| v2 config migration during integration tests | Low | Low | One fixture (`config-v2.yaml`) tests v2 migration. The migration logic is already implemented and tested in STORY-004. Integration tests validate it end-to-end. |
| File path separators (Windows vs Unix) in verifier | Low | Medium | Use `path.relative()` and `path.join()` consistently. Golden files use Unix separators. Tests run on macOS/Linux (CI). Add a note about Windows compatibility. |
| Coverage regression from new untested code paths | Medium | High | The verifier is new code that must have its own unit tests reaching >=95% line coverage. Integration tests add coverage for the pipeline + config + assemblers. Monitor total coverage after implementation. |
| `tests/golden/` size in git repository | Low | Low | The golden files are text-based (markdown, YAML, JSON). Total size per profile is ~50-100 KB. 8 profiles = ~400-800 KB. Acceptable for a test fixture repository. |

---

## 11. Implementation Groups (Execution Order)

### G1: Test Helpers (no dependencies on new code)

**Files:**
- `tests/helpers/file-tree.ts`

**Functions:**
- `createFileTree(baseDir, files)`
- `createBinaryFile(filePath, data)`

**Test validation:** Used by G2 tests. No separate test file needed; these are test utilities.

### G2: Verifier Module (depends on existing models only)

**Files:**
- `src/verifier.ts`
- `tests/node/verifier.test.ts`

**Functions:**
- `verifyOutput(actualDir, referenceDir)`
- `validateDirectory(dirPath, name)`
- `collectRelativePaths(baseDir)`
- `findMismatches(actualDir, referenceDir, commonPaths)`
- `compareFiles(actualFile, referenceFile, relativePath)`
- `generateTextDiff(actualPath, referencePath, relativePath)`

**Test scenarios (from Python test_verifier.py):**
1. `verifyOutput_identicalDirs_returnsSuccess`
2. `verifyOutput_identicalDirs_totalFilesCorrect`
3. `verifyOutput_mismatchDetected_returnsFailure`
4. `verifyOutput_mismatchContainsDiffString`
5. `verifyOutput_mismatchContainsFileSizes`
6. `verifyOutput_missingFileDetected`
7. `verifyOutput_extraFileDetected`
8. `verifyOutput_nestedDirectoriesCompared`
9. `verifyOutput_emptyDirs_returnsSuccess`
10. `verifyOutput_binaryFileMismatch_handled`
11. `verifyOutput_whitespaceDifferenceDetected`
12. `collectRelativePaths_returnsSortedPaths`
13. `collectRelativePaths_includesNestedFiles`
14. `collectRelativePaths_emptyDir_returnsEmpty`
15. `validateDirectory_nonexistentDir_throwsError`
16. `validateDirectory_nonexistentReferenceDir_throwsError`
17. `validateDirectory_fileAsDir_throwsError`
18. `validateDirectory_fileAsReferenceDir_throwsError`

### G3: Integration Fixtures (YAML configs)

**Files:**
- `tests/fixtures/integration/minimal.yaml`
- `tests/fixtures/integration/full.yaml`
- `tests/fixtures/integration/java-spring-rest.yaml`
- `tests/fixtures/integration/python-fastapi.yaml`
- `tests/fixtures/integration/go-gin-grpc.yaml`
- `tests/fixtures/integration/kotlin-ktor-events.yaml`
- `tests/fixtures/integration/ts-nestjs-fullstack.yaml`
- `tests/fixtures/integration/rust-axum-library.yaml`
- `tests/fixtures/integration/config-v2.yaml`
- `tests/fixtures/integration/config-with-mcp.yaml`
- `tests/fixtures/integration/invalid-missing-section.yaml`
- `tests/fixtures/integration/invalid-bad-yaml.yaml`

**Validation:** Run `loadConfig()` on each valid fixture to confirm parsing succeeds.

### G4: Byte-for-Byte Parity Tests (depends on G2 + G3 + golden files)

**Files:**
- `tests/node/integration/byte-for-byte.test.ts`

**Test scenarios (parametrized x 8 profiles):**
1. `pipelineMatchesGoldenFiles_{profile}` (8 tests)
2. `noMissingFiles_{profile}` (8 tests)
3. `noExtraFiles_{profile}` (8 tests)
4. `pipelineSuccessForProfile_{profile}` (8 tests)
5. `totalFilesGreaterThanZero_{profile}` (8 tests)

**Total:** 40 parametrized test cases.

### G5: E2E Verification Tests (depends on G2 + golden files)

**Files:**
- `tests/node/integration/e2e-verification.test.ts`

**Test scenarios:**
1. `fullFlowForProfile_{profile}` (8 parametrized tests)

### G6: Verification Edge Cases (depends on G2)

**Files:**
- `tests/node/integration/verification-edge-cases.test.ts`

**Test scenarios:**
1. `minimalConfig_producesOutput`
2. `minimalConfig_verifiesAgainstSelf`
3. `pipeline_isIdempotent`
4. `allFiles_reportedAsExtra` (empty reference)
5. `allFiles_reportedAsMissing` (empty output)
6. `nonexistentActualDir_throwsError`
7. `nonexistentReferenceDir_throwsError`
8. `fileAsActualDir_throwsError`
9. `fileAsReferenceDir_throwsError`

### G7: CLI Integration Tests (depends on G3)

**Files:**
- `tests/node/integration/cli-integration.test.ts`

**Test scenarios:**
1. `dryRun_producesNoOutputFiles`
2. `dryRun_returnsFileList`
3. `dryRun_containsDryRunWarning`
4. `dryRun_withValidConfig_succeeds`
5. `validate_validConfig_printsValid`
6. `validate_invalidConfig_exitsWithError`
7. `validate_missingSection_showsSectionName`
8. `generate_invalidConfigPath_showsError`
9. `generate_malformedYaml_showsParseError`
10. `generate_missingRequiredSection_showsValidationError`
11. `validate_nonexistentConfig_showsError`

### G8: Coverage Verification and Final Integration

- Run full test suite: `npx vitest run --coverage`
- Verify: line coverage >= 95%, branch coverage >= 90%
- Run `npx tsc --noEmit` to confirm zero compilation errors
- If coverage gaps exist, add targeted tests for uncovered branches

---

## 12. Testing Strategy

### Test Infrastructure

Tests use vitest (as per `vitest.config.ts`). Follow established patterns from `tests/node/assembler/pipeline.test.ts`:
- `describe`/`it` blocks with descriptive `methodUnderTest_scenario_expectedBehavior` naming
- `beforeEach`/`afterEach` for temp directory setup/teardown
- `vi.fn()` and `vi.mock()` for mocking (only where needed; integration tests use real code)

### Fixture Strategy

**Unit test fixtures (verifier):** Use `createFileTree()` helper to build synthetic directories in `tmp_path`. No dependency on golden files or real configs.

**Integration test fixtures:** Two categories:
1. **Config templates (existing):** `resources/config-templates/setup-config.{profile}.yaml` — used for byte-for-byte parity tests against golden files
2. **New integration fixtures:** `tests/fixtures/integration/*.yaml` — used for additional integration scenarios (v2 migration, MCP, library style, error cases)

### Golden File Management

Golden files are already committed in `tests/golden/{profile}/` for 8 profiles. They were generated by the Python CLI and represent the byte-for-byte expected output.

**Regeneration script:** Create `scripts/generate-golden.ts` (optional, out of STORY-019 scope) or document the regeneration process. For now, the existing golden files are used as-is.

**Skip strategy:** If golden files are missing for a profile, tests skip with a message:
```
Golden files not found. Run: npx tsx scripts/generate-golden.ts --all
```

### Mocking Strategy

**Verifier unit tests:** No mocks needed. Use real file system operations on temp directories.

**Byte-for-byte tests:** No mocks. Real `loadConfig` + `runPipeline` + `verifyOutput`.

**CLI integration tests:** Minimal mocking. Use `createCli().exitOverride()` to prevent `process.exit()`. Capture stdout/stderr via `vi.spyOn(console, "log")` and `vi.spyOn(console, "error")`.

### Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line (total) | >= 95% | Verifier unit tests cover all code paths. Integration tests cover pipeline + config + assemblers. |
| Branch (total) | >= 90% | Verifier tests cover: identical/different files, missing/extra files, binary/text, empty dirs, nested dirs, invalid dirs. Integration tests cover: all 8 profiles, dry-run, validate success/error. |
| Line (verifier.ts) | >= 98% | Every function and branch in the verifier is testable and tested. |

---

## 13. File-by-File Mapping (Python to TypeScript)

### Source Module

| Python (`src/ia_dev_env/verifier.py`) | TypeScript (`src/verifier.ts`) | Notes |
|---------------------------------------|-------------------------------|-------|
| `verify_output(python_dir, reference_dir)` | `verifyOutput(actualDir, referenceDir)` | Renamed `python_dir` to `actualDir` |
| `_validate_directory(path, name)` | `validateDirectory(dirPath, name)` | Same logic |
| `_collect_relative_paths(base_dir)` | `collectRelativePaths(baseDir)` | Uses `fs.readdirSync` + recursive walk |
| `_find_mismatches(python_dir, reference_dir, common_paths)` | `findMismatches(actualDir, referenceDir, commonPaths)` | Same logic |
| `_compare_files(python_file, reference_file, relative_path)` | `compareFiles(actualFile, referenceFile, relativePath)` | Returns `FileDiff | null` instead of `Optional[FileDiff]` |
| `_generate_text_diff(python_path, reference_path, relative_path)` | `generateTextDiff(actualPath, referencePath, relativePath)` | Minimal unified diff implementation |
| `BINARY_DIFF_MESSAGE` | `BINARY_DIFF_MESSAGE` | Same constant |
| `MAX_DIFF_LINES` | `MAX_DIFF_LINES` | Same constant |

### Test Modules

| Python Test | TypeScript Test | Notes |
|-------------|----------------|-------|
| `tests/test_verifier.py` | `tests/node/verifier.test.ts` | 1:1 migration of all test cases |
| `tests/test_byte_for_byte.py` | `tests/node/integration/byte-for-byte.test.ts` | Same 8 profiles, same 5 test types |
| `tests/test_e2e_verification.py` | `tests/node/integration/e2e-verification.test.ts` | Same 8 profiles |
| `tests/test_verification_edge_cases.py` | `tests/node/integration/verification-edge-cases.test.ts` | All 4 test classes ported |
| N/A (new) | `tests/node/integration/cli-integration.test.ts` | New: dry-run, validate, error handling |

### Key TypeScript vs Python Differences

1. **Path handling:** Python `pathlib.Path` -> TypeScript `node:path` functions (`join`, `relative`, `resolve`)
2. **File I/O:** Python `Path.read_bytes()` -> TypeScript `fs.readFileSync(path)` returning `Buffer`
3. **Buffer comparison:** Python `bytes == bytes` -> TypeScript `Buffer.equals(buffer)`
4. **Text decoding:** Python `Path.read_text(encoding="utf-8")` -> TypeScript `fs.readFileSync(path, "utf-8")` (throws on invalid UTF-8 in strict mode; use try/catch)
5. **Directory walk:** Python `Path.rglob("*")` -> TypeScript `fs.readdirSync(dir, { recursive: true, withFileTypes: true })`
6. **Set operations:** Python `set(a) - set(b)` -> TypeScript `new Set()` with manual difference computation
7. **Unified diff:** Python `difflib.unified_diff()` -> Custom implementation or `createTwoFilesPatch` from `diff` package
8. **Null handling:** Python `Optional[T]` -> TypeScript `T | null`
9. **Parametrized tests:** Python `@pytest.mark.parametrize` -> vitest `describe.each` or `it.each`
10. **Temp dirs:** Python `tmp_path` (pytest fixture) -> vitest `beforeEach` with `fs.mkdtempSync`

---

## 14. Acceptance Criteria Checklist

From story requirements:

**Verifier (3.1):**
- [ ] `verifyOutput(actualDir, referenceDir)` returns `VerificationResult`
- [ ] Compares byte-for-byte all files
- [ ] Reports mismatches with unified diffs
- [ ] Reports missing files (in reference but not in actual)
- [ ] Reports extra files (in actual but not in reference)
- [ ] Handles binary files gracefully

**Fixtures (3.2):**
- [ ] 10+ fixtures created covering all listed scenarios
- [ ] Minimal config fixture
- [ ] Full config fixture
- [ ] Java Spring REST fixture
- [ ] Python FastAPI fixture
- [ ] Go Gin gRPC fixture
- [ ] Kotlin Ktor events fixture
- [ ] TypeScript NestJS fullstack fixture
- [ ] Rust Axum library fixture
- [ ] Config v2 (legacy) fixture
- [ ] Config with MCP fixture

**Integration Tests (3.3):**
- [ ] Byte-for-byte parity tests for all 8 golden profiles
- [ ] E2E verification tests
- [ ] Dry-run mode tests
- [ ] Validate command tests (success and error)
- [ ] Error handling tests (invalid config, invalid path)

**Coverage (3.4):**
- [ ] Total line coverage >= 95%
- [ ] Total branch coverage >= 90%

**DoD:**
- [ ] All tests passing
- [ ] Zero compiler warnings (`npx tsc --noEmit`)
- [ ] Integration test suite completes in < 60s
- [ ] Test naming follows `methodUnderTest_scenario_expectedBehavior` convention
