# Task Decomposition -- STORY-019: Integration Tests + Parity Verification

**Status:** PENDING
**Date:** 2026-03-11
**Blocked By:** STORY-018 (CLI entry point)
**Blocks:** STORY-020

---

## G1 -- Foundation: Test Helpers

**Purpose:** Create reusable test utility functions for building synthetic directory trees in temp directories. These helpers are used extensively by the verifier unit tests (G3) and edge-case integration tests (G7). Ported from Python `tests/conftest.py::create_file_tree` and `tests/test_verifier.py::_create_binary_file`.
**Dependencies:** None
**Compiles independently:** Yes -- test utility only, no source code changes.

### T1.1 -- Create `createFileTree` helper

- **File:** `tests/helpers/file-tree.ts` (create)
- **What to implement:**
  1. Export function `createFileTree(baseDir: string, files: Record<string, string>): void`
  2. For each entry in `files`:
     - Compute full path via `path.join(baseDir, relativePath)`
     - Create intermediate directories with `fs.mkdirSync(dir, { recursive: true })`
     - Write content with `fs.writeFileSync(fullPath, content, "utf-8")`
  3. This mirrors the Python `create_file_tree(base: Path, files: Dict[str, str])` from `conftest.py`.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.2 -- Create `createBinaryFile` helper

- **File:** `tests/helpers/file-tree.ts` (modify)
- **What to implement:**
  1. Export function `createBinaryFile(filePath: string, data: Buffer): void`
  2. Create intermediate directories with `fs.mkdirSync(path.dirname(filePath), { recursive: true })`
  3. Write binary data with `fs.writeFileSync(filePath, data)`
  4. This mirrors `_create_binary_file(path: Path, data: bytes)` from `test_verifier.py`.
- **Dependencies on other tasks:** T1.1 (same file)
- **Estimated complexity:** S

### Compilation checkpoint G1

```
npx tsc --noEmit   # zero errors -- new test helper file
```

---

## G2 -- Core Source: Verifier Module

**Purpose:** Port `src/ia_dev_env/verifier.py` to TypeScript as `src/verifier.ts`. This module compares two directory trees byte-for-byte and produces a `VerificationResult` with mismatches, missing files, and extra files. Uses synchronous filesystem API (matching Python's synchronous I/O) and the existing `VerificationResult` and `FileDiff` models from `src/models.ts`.
**Dependencies:** G1 is not required (G2 depends only on existing `src/models.ts`)
**Compiles independently:** Yes

### T2.1 -- Implement constants and `validateDirectory`

- **File:** `src/verifier.ts` (create)
- **What to implement:**
  1. Import `VerificationResult`, `FileDiff` from `./models.js`
  2. Import `readFileSync`, `readdirSync`, `statSync`, `existsSync` from `node:fs`
  3. Import `join`, `relative` from `node:path`
  4. Define exported constants:
     ```typescript
     export const BINARY_DIFF_MESSAGE = "<binary files differ>";
     export const MAX_DIFF_LINES = 200;
     ```
  5. Implement non-exported `validateDirectory(dirPath: string, name: string): void`:
     - If path does not exist (`!existsSync(dirPath)`), throw `new Error("{name} does not exist: {dirPath}")`
     - If path is not a directory (`!statSync(dirPath).isDirectory()`), throw `new Error("{name} is not a directory: {dirPath}")`
     - Mirrors Python `_validate_directory(path, name)` which raises `ValueError`.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T2.2 -- Implement `collectRelativePaths`

- **File:** `src/verifier.ts` (modify)
- **What to implement:**
  1. Implement non-exported `collectRelativePaths(baseDir: string): string[]`:
     - Use `readdirSync(baseDir, { recursive: true, withFileTypes: true })` (Node 18.17+)
     - Filter to files only (`dirent.isFile()`)
     - Compute relative path from `baseDir` for each file
     - Return sorted array of relative paths (using string comparison)
  2. **Important:** The `withFileTypes: true` + `recursive: true` combination returns `Dirent` objects. The `dirent.parentPath` (Node 20.12+) or `dirent.path` (Node 18.17+) gives the parent directory. Compute relative via `path.relative(baseDir, path.join(dirent.parentPath ?? dirent.path, dirent.name))`.
  3. Normalize path separators to forward slashes for cross-platform consistency.
  4. Mirrors Python `_collect_relative_paths(base_dir)` using `Path.rglob("*")`.
- **Dependencies on other tasks:** T2.1 (same file)
- **Estimated complexity:** M

### T2.3 -- Implement `generateTextDiff`

- **File:** `src/verifier.ts` (modify)
- **What to implement:**
  1. Implement non-exported `generateTextDiff(actualPath: string, referencePath: string, relativePath: string): string`:
     - Try reading both files as UTF-8 (`readFileSync(path, "utf-8")`)
     - If either throws (binary/malformed), return `BINARY_DIFF_MESSAGE`
     - Split content into lines (preserving line endings with a regex split or `splitlines` equivalent)
     - Generate a minimal unified diff:
       - Header lines: `--- reference/{relativePath}` and `+++ actual/{relativePath}`
       - Walk both line arrays, emit `@@` hunks for differences
       - Limit output to `MAX_DIFF_LINES` lines
     - Return the diff string joined with newlines
  2. **Design decision:** Implement a minimal unified diff (~30 lines) to avoid adding a `diff` npm dependency. The Python version uses `difflib.unified_diff`. The TS version needs to produce `---`/`+++` headers and diff lines for test assertions to pass.
  3. **Alternative simplified approach:** If minimal diff is too complex, use a simple "show first N differing lines" approach. The diff output is for developer debugging, not machine consumption. Tests only assert that `"---"` and `"+++"` markers are present.
  4. Mirrors Python `_generate_text_diff(python_path, reference_path, relative_path)`.
- **Dependencies on other tasks:** T2.1 (constants)
- **Estimated complexity:** M

### T2.4 -- Implement `compareFiles` and `findMismatches`

- **File:** `src/verifier.ts` (modify)
- **What to implement:**
  1. Implement non-exported `compareFiles(actualFile: string, referenceFile: string, relativePath: string): FileDiff | null`:
     - Read both files as buffers: `readFileSync(path)` returning `Buffer`
     - Compare with `Buffer.equals()`
     - If equal, return `null`
     - If different, call `generateTextDiff(actualFile, referenceFile, relativePath)`
     - Return `new FileDiff(relativePath, diffText, actualBuffer.length, referenceBuffer.length)`
     - **Naming note:** `FileDiff` constructor uses `pythonSize` parameter name (from existing model). Map `actualBuffer.length` to the `pythonSize` parameter.
  2. Implement non-exported `findMismatches(actualDir: string, referenceDir: string, commonPaths: string[]): FileDiff[]`:
     - For each relative path in `commonPaths`:
       - Call `compareFiles(join(actualDir, relPath), join(referenceDir, relPath), relPath)`
       - If result is not null, push to mismatches array
     - Return mismatches array
  3. Mirrors Python `_compare_files` and `_find_mismatches`.
- **Dependencies on other tasks:** T2.3 (needs `generateTextDiff`)
- **Estimated complexity:** M

### T2.5 -- Implement exported `verifyOutput`

- **File:** `src/verifier.ts` (modify)
- **What to implement:**
  1. Implement exported `verifyOutput(actualDir: string, referenceDir: string): VerificationResult`:
     - Call `validateDirectory(actualDir, "actualDir")`
     - Call `validateDirectory(referenceDir, "referenceDir")`
     - Collect paths: `actualPaths = new Set(collectRelativePaths(actualDir))`
     - Collect paths: `referencePaths = new Set(collectRelativePaths(referenceDir))`
     - Compute sets:
       - `missing`: paths in `referencePaths` but not in `actualPaths` (sorted)
       - `extra`: paths in `actualPaths` but not in `referencePaths` (sorted)
       - `common`: paths in both (sorted)
     - Compute total: `union(actualPaths, referencePaths).size`
     - Call `findMismatches(actualDir, referenceDir, common)`
     - Compute `success = mismatches.length === 0 && missing.length === 0 && extra.length === 0`
     - Return `new VerificationResult(success, total, mismatches, missing, extra)`
  2. JSDoc with `@param`, `@returns`, `@throws` documentation.
  3. Mirrors Python `verify_output(python_dir, reference_dir)`.
- **Dependencies on other tasks:** T2.2, T2.4
- **Estimated complexity:** M
- **Estimated total file size:** ~120 lines (well under 250-line limit)

### Compilation checkpoint G2

```
npx tsc --noEmit   # zero errors -- verifier module compiles
```

---

## G3 -- Unit Tests: Verifier

**Purpose:** Port all test cases from `tests/test_verifier.py` to TypeScript vitest. Tests the verifier module in isolation using synthetic file trees built with the helpers from G1. This is the primary quality gate for the verifier module.
**Dependencies:** G1 (test helpers), G2 (verifier source)
**Compiles independently:** Yes (test file)

### T3.1 -- Test `verifyOutput` (11 test cases)

- **File:** `tests/node/verifier.test.ts` (create)
- **What to implement:**
  1. Import `verifyOutput` from `../../src/verifier.js`
  2. Import `createFileTree`, `createBinaryFile` from `../helpers/file-tree.js`
  3. Setup: `beforeEach` creates temp dir via `fs.mkdtempSync(path.join(os.tmpdir(), "verifier-test-"))`, `afterEach` cleans up with `fs.rmSync(tmpDir, { recursive: true, force: true })`
  4. Test cases (in `describe("verifyOutput")` block):
     - `verifyOutput_identicalDirs_returnsSuccess` -- Create identical files in two dirs. Assert `result.success === true`.
     - `verifyOutput_identicalDirs_totalFilesCorrect` -- Create 3 identical files. Assert `result.totalFiles === 3`.
     - `verifyOutput_mismatchDetected_returnsFailure` -- Create `a.txt` with "new" vs "old". Assert `result.success === false` and `result.mismatches.length === 1`.
     - `verifyOutput_mismatchContainsDiffString` -- Create files with "new\n" vs "old\n". Assert diff contains `"---"` and `"+++"`.
     - `verifyOutput_mismatchContainsFileSizes` -- Create "short" (5 bytes) vs "much longer text" (16 bytes). Assert `mismatches[0].pythonSize === 5` and `mismatches[0].referenceSize === 16`.
     - `verifyOutput_missingFileDetected` -- actual has `a.txt`, reference has `a.txt` + `b.txt`. Assert `result.missingFiles` contains `"b.txt"`.
     - `verifyOutput_extraFileDetected` -- actual has `a.txt` + `c.txt`, reference has `a.txt`. Assert `result.extraFiles` contains `"c.txt"`.
     - `verifyOutput_nestedDirectoriesCompared` -- Both dirs have `sub/dir/file.txt` with same content. Assert `success === true` and `totalFiles === 1`.
     - `verifyOutput_emptyDirs_returnsSuccess` -- Both dirs empty (just `mkdirSync`). Assert `success === true` and `totalFiles === 0`.
     - `verifyOutput_binaryFileMismatch_handled` -- Create binary files `\x00\x01\x02` vs `\x00\x01\xff`. Assert `success === false` and diff contains `"binary"`.
     - `verifyOutput_whitespaceDifferenceDetected` -- "hello " vs "hello". Assert `success === false` and `mismatches.length === 1`.
- **Dependencies on other tasks:** T1.1, T1.2, T2.5
- **Estimated complexity:** M

### T3.2 -- Test `collectRelativePaths` (3 test cases)

- **File:** `tests/node/verifier.test.ts` (modify)
- **What to implement:**
  1. **Note:** `collectRelativePaths` is not exported. Two options:
     - a) Export it for testing (add `export` keyword in `src/verifier.ts`)
     - b) Test it indirectly through `verifyOutput`
     - **Decision:** Export it with a `/** @internal */` JSDoc tag. This matches the Python approach where `_collect_relative_paths` is imported directly in tests.
  2. Test cases (in `describe("collectRelativePaths")` block):
     - `collectRelativePaths_returnsSortedPaths` -- Create files `c.txt`, `a.txt`, `b.txt`. Assert returns `["a.txt", "b.txt", "c.txt"]`.
     - `collectRelativePaths_includesNestedFiles` -- Create `root.txt` and `sub/nested.txt`. Assert `"sub/nested.txt"` is in result.
     - `collectRelativePaths_emptyDir_returnsEmpty` -- Empty temp dir. Assert returns `[]`.
- **Dependencies on other tasks:** T2.2
- **Estimated complexity:** S

### T3.3 -- Test `validateDirectory` (4 test cases)

- **File:** `tests/node/verifier.test.ts` (modify)
- **What to implement:**
  1. **Note:** Like `collectRelativePaths`, export `validateDirectory` with `/** @internal */` for direct testing.
  2. Test cases (in `describe("validateDirectory")` block):
     - `validateDirectory_nonexistentDir_throwsError` -- Pass non-existent path as "actualDir". Assert throws with message matching `"actualDir"`.
     - `validateDirectory_nonexistentReferenceDir_throwsError` -- Pass non-existent path as "referenceDir". Assert throws with message matching `"referenceDir"`.
     - `validateDirectory_fileNotDir_throwsError` -- Create a file, pass it. Assert throws with message matching `"not a directory"`.
     - `validateDirectory_existingDir_noError` -- Pass existing dir. Assert does not throw.
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** S

### Test execution checkpoint G3

```
npx vitest run tests/node/verifier.test.ts   # all 18 tests pass
```

---

## G4 -- YAML Fixtures

**Purpose:** Create 10+ YAML fixture config files in `tests/fixtures/integration/` for integration testing. These files cover a variety of project configurations (minimal, full, various language/framework combos, legacy v2, MCP, and invalid configs). They serve as inputs for the byte-for-byte parity tests and CLI integration tests.
**Dependencies:** None (fixtures are static YAML files, but they are validated by G5 tests)
**Compiles independently:** N/A (YAML files)

### T4.1 -- Create minimal and full fixtures

- **Files:**
  - `tests/fixtures/integration/minimal.yaml` (create)
  - `tests/fixtures/integration/full.yaml` (create)
- **What to implement:**
  1. `minimal.yaml` -- Only required sections: `project`, `architecture`, `interfaces`, `language`, `framework`. Based on existing `tests/fixtures/minimal_v3_config.yaml` pattern.
     ```yaml
     project:
       name: "minimal-tool"
       purpose: "Minimal config for integration tests"
     architecture:
       style: library
     interfaces:
       - type: cli
     language:
       name: python
       version: "3.9"
     framework:
       name: click
       version: "8.1"
     ```
  2. `full.yaml` -- All sections populated: `project`, `architecture`, `interfaces`, `language`, `framework`, `data`, `infrastructure`, `security`, `testing`, `mcp`. Based on `tests/conftest.py::FULL_PROJECT_DICT`.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T4.2 -- Create language/framework-specific fixtures

- **Files:**
  - `tests/fixtures/integration/java-spring-rest.yaml` (create)
  - `tests/fixtures/integration/python-fastapi.yaml` (create)
  - `tests/fixtures/integration/go-gin-grpc.yaml` (create)
  - `tests/fixtures/integration/kotlin-ktor-events.yaml` (create)
  - `tests/fixtures/integration/ts-nestjs-fullstack.yaml` (create)
  - `tests/fixtures/integration/rust-axum-library.yaml` (create)
- **What to implement:**
  1. Derive each fixture from the corresponding `resources/config-templates/setup-config.{profile}.yaml` template, simplifying or adapting as needed for the test scenario.
  2. Each fixture must:
     - Have a unique `project.name`
     - Use the correct `language`, `framework`, `architecture`, and `interfaces` for the scenario
     - Be parseable by `loadConfig()` without errors
  3. Fixture-specific details:
     - `java-spring-rest.yaml`: Java 21, Spring Boot, REST + gRPC, PostgreSQL, Redis, K8s
     - `python-fastapi.yaml`: Python 3.12, FastAPI, REST, MongoDB
     - `go-gin-grpc.yaml`: Go 1.23, Gin, gRPC, Kafka
     - `kotlin-ktor-events.yaml`: Kotlin 2.1, Ktor, event-driven, Kafka
     - `ts-nestjs-fullstack.yaml`: TypeScript 5, NestJS, REST + GraphQL, monolith
     - `rust-axum-library.yaml`: Rust, Axum, library style, no interfaces (use `interfaces: []` or `[{type: cli}]`)
- **Dependencies on other tasks:** None
- **Estimated complexity:** M

### T4.3 -- Create v2 and MCP fixtures

- **Files:**
  - `tests/fixtures/integration/config-v2.yaml` (create)
  - `tests/fixtures/integration/config-with-mcp.yaml` (create)
- **What to implement:**
  1. `config-v2.yaml` -- Legacy v2 format using `type` and `stack` fields. Based on `tests/fixtures/valid_v2_stack_config.yaml` pattern.
     ```yaml
     type: api
     stack: java-spring
     project:
       name: "legacy-service"
       purpose: "Testing v2 migration"
     ```
  2. `config-with-mcp.yaml` -- Full v3 config with MCP servers:
     ```yaml
     # ... standard sections ...
     mcp:
       servers:
         - id: "test-mcp"
           url: "https://mcp.example.com"
           capabilities: ["search", "fetch"]
           env:
             API_KEY: "$TEST_API_KEY"
     ```
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T4.4 -- Create invalid fixtures

- **Files:**
  - `tests/fixtures/integration/invalid-missing-section.yaml` (create)
  - `tests/fixtures/integration/invalid-bad-yaml.yaml` (create)
- **What to implement:**
  1. `invalid-missing-section.yaml` -- Valid YAML but missing required `language` section:
     ```yaml
     project:
       name: "broken"
       purpose: "Missing language"
     architecture:
       style: library
     interfaces:
       - type: cli
     framework:
       name: click
       version: "8.1"
     ```
  2. `invalid-bad-yaml.yaml` -- Malformed YAML syntax:
     ```yaml
     project:
       name: "broken
       purpose: missing closing quote
     invalid: [unclosed bracket
     ```
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Validation checkpoint G4

```
# Verify valid fixtures can be parsed (quick manual check)
npx tsx -e "import { loadConfig } from './src/config.js'; loadConfig('tests/fixtures/integration/minimal.yaml')"
```

---

## G5 -- Integration Tests: Byte-for-Byte Parity

**Purpose:** Port `tests/test_byte_for_byte.py` to TypeScript. Parametrized tests that run the pipeline for each of the 8 golden config profiles and compare output byte-for-byte against golden reference files using the verifier. This is the core parity gate for the migration.
**Dependencies:** G2 (verifier module), G4 (fixtures for reference -- though G5 uses config-templates, not integration fixtures)
**Compiles independently:** Yes

### T5.1 -- Implement test infrastructure and helpers

- **File:** `tests/node/integration/byte-for-byte.test.ts` (create)
- **What to implement:**
  1. Import `loadConfig` from `../../../src/config.js`, `runPipeline` from `../../../src/assembler/pipeline.js`, `verifyOutput` from `../../../src/verifier.js`, `VerificationResult` from `../../../src/models.js`
  2. Define constants:
     ```typescript
     const CONFIG_PROFILES = [
       "go-gin", "java-quarkus", "java-spring", "kotlin-ktor",
       "python-click-cli", "python-fastapi", "rust-axum", "typescript-nestjs",
     ] as const;
     const PROJECT_ROOT = path.resolve(__dirname, "../../..");
     const CONFIG_TEMPLATES_DIR = path.join(PROJECT_ROOT, "resources", "config-templates");
     const GOLDEN_DIR = path.join(PROJECT_ROOT, "tests", "golden");
     const RESOURCES_DIR = path.join(PROJECT_ROOT, "resources");
     const GOLDEN_MISSING_MSG = "Golden files not found. Run: npx tsx scripts/generate-golden.ts --all";
     ```
  3. Implement helper `runPipelineForProfile(profileName: string, outputDir: string)`:
     - Load config from `CONFIG_TEMPLATES_DIR/setup-config.{profileName}.yaml`
     - Run `await runPipeline(config, RESOURCES_DIR, outputDir, false)`
     - Return pipeline result
  4. Implement helper `formatMismatches(result: VerificationResult): string`:
     - For each mismatch: `"MISMATCH: {path} (actual={pythonSize}B, ref={referenceSize}B)\n{diff[:500]}"`
     - For each missing: `"MISSING: {path}"`
     - For each extra: `"EXTRA: {path}"`
     - Return joined string
  5. Implement `skipIfNoGolden(profileName: string)`:
     - Check if `GOLDEN_DIR/{profileName}` exists
     - If not, call `it.skip` or use conditional `describe` approach
- **Dependencies on other tasks:** T2.5 (verifyOutput)
- **Estimated complexity:** M

### T5.2 -- Implement parametrized parity tests (5 test types x 8 profiles = 40 tests)

- **File:** `tests/node/integration/byte-for-byte.test.ts` (modify)
- **What to implement:**
  1. Use `describe.each(CONFIG_PROFILES)` or `it.each` for parametrization.
  2. Each profile gets a temp dir in `beforeEach`, cleaned in `afterEach`.
  3. Test cases per profile:
     - `pipelineMatchesGoldenFiles_{profile}` -- Run pipeline, verify against golden. Assert `result.success === true` (with `formatMismatches` in error message).
     - `noMissingFiles_{profile}` -- Assert `result.missingFiles` is empty.
     - `noExtraFiles_{profile}` -- Assert `result.extraFiles` is empty.
     - `pipelineSuccessForProfile_{profile}` -- Assert pipeline returns `success: true` (does not need golden files).
     - `totalFilesGreaterThanZero_{profile}` -- Assert `result.totalFiles > 0`.
  4. Tests that need golden files should skip gracefully if golden directory is missing.
  5. **Timeout:** Set `testTimeout: 30000` (30s) for the describe block if needed, as each profile runs a full pipeline.
- **Dependencies on other tasks:** T5.1
- **Estimated complexity:** L

### Test execution checkpoint G5

```
npx vitest run tests/node/integration/byte-for-byte.test.ts   # 40 tests (or skipped if no golden files)
```

---

## G6 -- Integration Tests: CLI and E2E

**Purpose:** Create CLI integration tests (dry-run, validate, error handling) and E2E verification tests. The CLI tests invoke the CLI programmatically using `createCli().exitOverride().parseAsync()` to test command behavior without spawning child processes. The E2E tests run the full pipeline flow for each profile.
**Dependencies:** G2 (verifier), G4 (fixtures), STORY-018 (CLI must be fully implemented)
**Compiles independently:** Yes

### T6.1 -- Implement CLI integration tests

- **File:** `tests/node/integration/cli-integration.test.ts` (create)
- **What to implement:**
  1. **Test approach:** Use `createCli().exitOverride()` from `src/cli.ts` to prevent `process.exit()`. Capture stdout/stderr via `vi.spyOn(console, "log")` and `vi.spyOn(console, "error")`. Mock `loadConfig`, `runPipeline`, and filesystem operations as needed.
  2. **Dry-run tests** (`describe("DryRun")`):
     - `dryRun_producesNoOutputFiles` -- Run generate with `--dry-run --config {valid-fixture}`. Verify the output directory is empty or does not exist after execution.
     - `dryRun_returnsFileList` -- Verify pipeline result includes `filesGenerated` array.
     - `dryRun_containsDryRunWarning` -- Verify pipeline result warnings include `"Dry run"`.
     - `dryRun_withValidConfig_succeeds` -- Verify exit code is 0.
  3. **Validate command tests** (`describe("ValidateCommand")`):
     - `validate_validConfig_printsValid` -- Run `validate --config {valid-fixture}`. Verify `"Config is valid."` printed.
     - `validate_invalidConfig_exitsWithError` -- Run `validate --config {invalid-missing-section.yaml}`. Verify error exit.
     - `validate_missingSection_showsSectionName` -- Verify error message contains the name of the missing section (e.g., `"language"`).
  4. **Error handling tests** (`describe("ErrorHandling")`):
     - `generate_invalidConfigPath_showsError` -- Run `generate --config nonexistent.yaml`. Verify error about file not found.
     - `generate_malformedYaml_showsParseError` -- Run `generate --config {invalid-bad-yaml.yaml}`. Verify parse error.
     - `generate_missingRequiredSection_showsValidationError` -- Run `generate --config {invalid-missing-section.yaml}`. Verify validation error.
     - `validate_nonexistentConfig_showsError` -- Run `validate --config nonexistent.yaml`. Verify error.
- **Dependencies on other tasks:** T4.1, T4.4 (needs fixtures)
- **Estimated complexity:** L

### T6.2 -- Implement E2E verification tests

- **File:** `tests/node/integration/e2e-verification.test.ts` (create)
- **What to implement:**
  1. Port `tests/test_e2e_verification.py::TestE2EVerification`.
  2. Import `loadConfig`, `runPipeline`, `verifyOutput`.
  3. Use same `CONFIG_PROFILES`, `CONFIG_TEMPLATES_DIR`, `GOLDEN_DIR`, `RESOURCES_DIR` constants as G5.
  4. Parametrized test (8 profiles):
     - `fullFlowForProfile_{profile}` -- Load config, run pipeline, verify success, verify output against golden via `verifyOutput`. Assert both pipeline success and verification success.
  5. Skip gracefully if golden files missing.
  6. Implement `formatFailures(result: VerificationResult): string` helper for readable assertion messages (mirrors Python `_format_failures`).
- **Dependencies on other tasks:** T2.5 (verifyOutput), golden files
- **Estimated complexity:** M

### Test execution checkpoint G6

```
npx vitest run tests/node/integration/cli-integration.test.ts tests/node/integration/e2e-verification.test.ts
```

---

## G7 -- Edge Cases and Coverage

**Purpose:** Port `tests/test_verification_edge_cases.py` edge-case tests to TypeScript, and run full coverage report to verify thresholds are met. Covers minimal config, idempotency, empty directories, and invalid directory arguments.
**Dependencies:** G2 (verifier), G1 (file-tree helpers), existing project-config fixtures
**Compiles independently:** Yes

### T7.1 -- Implement edge-case tests

- **File:** `tests/node/integration/verification-edge-cases.test.ts` (create)
- **What to implement:**
  1. Import `runPipeline` from `../../../src/assembler/pipeline.js`, `verifyOutput` from `../../../src/verifier.js`, `ProjectConfig` from `../../../src/models.js`
  2. Import `createFileTree` from `../../helpers/file-tree.js`
  3. Define `RESOURCES_DIR` pointing to `resources/`
  4. Define `makeMinimalConfig()` factory (mirrors Python `_make_minimal_config()`) using `MINIMAL_PROJECT_DICT` pattern from `tests/fixtures/project-config.fixture.ts` or directly constructing a `ProjectConfig` with minimal required fields.
  5. Test groups and cases:

  **`describe("MinimalConfig")`:**
  - `minimalConfig_producesOutput` -- Run pipeline with minimal config. Assert `result.success === true` and `result.filesGenerated.length > 0`.
  - `minimalConfig_verifiesAgainstSelf` -- Run pipeline twice to different dirs. `verifyOutput(dirA, dirB)` should return `success === true`.

  **`describe("Idempotency")`:**
  - `pipeline_isIdempotent` -- Run pipeline twice with same config. Verify both outputs are identical via `verifyOutput`.

  **`describe("EmptyReference")`:**
  - `allFiles_reportedAsExtra` -- Run pipeline to generate output, create empty reference dir. Verify `result.success === false`, `result.extraFiles.length > 0`, `result.missingFiles` is empty.

  **`describe("EmptyOutput")`:**
  - `allFiles_reportedAsMissing` -- Create reference dir with files, create empty actual dir. Verify `result.success === false`, `result.missingFiles.length === 2`, `result.extraFiles` is empty.

  **`describe("InvalidDirectories")`:**
  - `nonexistentActualDir_throwsError` -- Pass non-existent actual dir. Assert throws with `"actualDir"` in message.
  - `nonexistentReferenceDir_throwsError` -- Pass non-existent reference dir. Assert throws with `"referenceDir"` in message.
  - `fileAsActualDir_throwsError` -- Pass file as actual dir. Assert throws with `"not a directory"` in message.
  - `fileAsReferenceDir_throwsError` -- Pass file as reference dir. Assert throws with `"not a directory"` in message.

- **Dependencies on other tasks:** T1.1, T2.5
- **Estimated complexity:** M

### T7.2 -- Add `test:integration` npm script

- **File:** `package.json` (modify)
- **What to implement:**
  1. Add a convenience script for running integration tests separately:
     ```json
     "test:integration": "vitest run tests/node/integration/"
     ```
  2. This allows running the slower integration tests independently from unit tests.
- **Dependencies on other tasks:** All integration test files must exist
- **Estimated complexity:** S

### T7.3 -- Full coverage verification

- **Command:** `npx vitest run --coverage`
- **What to verify:**
  1. Total line coverage >= 95%
  2. Total branch coverage >= 90%
  3. `src/verifier.ts` specifically >= 98% line coverage (every function and branch is tested)
  4. Zero compilation warnings: `npx tsc --noEmit`
- **If coverage gaps exist:**
  - Identify uncovered branches in the coverage report
  - Add targeted tests in the appropriate test file (verifier.test.ts or edge-cases.test.ts)
  - Common gaps: error paths in `generateTextDiff` binary handling, edge cases in `collectRelativePaths`
- **Dependencies on other tasks:** All prior groups complete
- **Estimated complexity:** S (verification) or M (if gap-filling needed)

### T7.4 -- Full regression check

- **Command:** `npx vitest run`
- **What to verify:**
  1. All existing tests continue to pass (no regressions)
  2. All new tests pass
  3. Integration test suite completes in < 60s
- **Dependencies on other tasks:** T7.3
- **Estimated complexity:** S

### Test execution checkpoint G7

```
npx vitest run tests/node/integration/verification-edge-cases.test.ts   # 9 tests pass
npx vitest run --coverage                                                # thresholds met
npx tsc --noEmit                                                        # zero errors
```

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Test Cases | Complexity |
|-------|---------|----------------|----------------|-------|------------|------------|
| G1 | Test helpers | 1 (`file-tree.ts`) | 0 | 2 | 0 | S |
| G2 | Verifier module | 1 (`verifier.ts`) | 0 | 5 | 0 | M |
| G3 | Verifier unit tests | 1 (`verifier.test.ts`) | 1 (`verifier.ts` -- export internals) | 3 | 18 | M |
| G4 | YAML fixtures | 12 fixture files | 0 | 4 | 0 | M |
| G5 | Byte-for-byte parity | 1 (`byte-for-byte.test.ts`) | 0 | 2 | 40 | L |
| G6 | CLI + E2E integration | 2 (`cli-integration.test.ts`, `e2e-verification.test.ts`) | 0 | 2 | 19 | L |
| G7 | Edge cases + coverage | 1 (`verification-edge-cases.test.ts`) | 1 (`package.json`) | 4 | 9 | M |
| **Total** | | **19 new files** | **2 modified** | **22 tasks** | **~86 test cases** | |

## Dependency Graph

```
G1: TEST HELPERS (createFileTree, createBinaryFile) -- no dependencies
  |
  +----> G3: VERIFIER UNIT TESTS -- depends on G1, G2
  |        |
  |        +----> G5: BYTE-FOR-BYTE PARITY -- depends on G2 (G3 should pass first)
  |        |
  |        +----> G7: EDGE CASES + COVERAGE -- depends on G1, G2
  |
G2: VERIFIER MODULE (src/verifier.ts) -- depends on existing src/models.ts only
  |
  +----> G5: BYTE-FOR-BYTE PARITY -- depends on G2
  |
  +----> G6: CLI + E2E -- depends on G2, G4
  |
  +----> G7: EDGE CASES -- depends on G2
  |
G4: YAML FIXTURES -- no dependencies (static files)
  |
  +----> G6: CLI + E2E -- depends on G4 (needs fixture files for testing)

G7: EDGE CASES + COVERAGE -- depends on ALL (final verification)
```

**Parallelizable:** G1 and G2 can start simultaneously. G4 can start simultaneously with G1/G2. G3 can start as soon as G1 + G2 are done. G5 and G6 can start as soon as G2 + G4 are done (G3 passing is recommended but not blocking). G7 must be last.

## File Inventory

### Source files (1 new)

| File | Action | Content |
|------|--------|---------|
| `src/verifier.ts` | Create | `verifyOutput()`, `validateDirectory()`, `collectRelativePaths()`, `findMismatches()`, `compareFiles()`, `generateTextDiff()`, `BINARY_DIFF_MESSAGE`, `MAX_DIFF_LINES` (~120 lines) |

### Test helper files (1 new)

| File | Action | Content |
|------|--------|---------|
| `tests/helpers/file-tree.ts` | Create | `createFileTree()`, `createBinaryFile()` (~25 lines) |

### Test files (5 new)

| File | Action | Content |
|------|--------|---------|
| `tests/node/verifier.test.ts` | Create | 18 test cases: verifyOutput (11), collectRelativePaths (3), validateDirectory (4) |
| `tests/node/integration/byte-for-byte.test.ts` | Create | 40 parametrized tests: 5 test types x 8 profiles |
| `tests/node/integration/cli-integration.test.ts` | Create | 11 test cases: dry-run (4), validate (3), error handling (4) |
| `tests/node/integration/e2e-verification.test.ts` | Create | 8 parametrized tests: full flow x 8 profiles |
| `tests/node/integration/verification-edge-cases.test.ts` | Create | 9 test cases: minimal (2), idempotency (1), empty (2), invalid dirs (4) |

### Fixture files (12 new)

| File | Action |
|------|--------|
| `tests/fixtures/integration/minimal.yaml` | Create |
| `tests/fixtures/integration/full.yaml` | Create |
| `tests/fixtures/integration/java-spring-rest.yaml` | Create |
| `tests/fixtures/integration/python-fastapi.yaml` | Create |
| `tests/fixtures/integration/go-gin-grpc.yaml` | Create |
| `tests/fixtures/integration/kotlin-ktor-events.yaml` | Create |
| `tests/fixtures/integration/ts-nestjs-fullstack.yaml` | Create |
| `tests/fixtures/integration/rust-axum-library.yaml` | Create |
| `tests/fixtures/integration/config-v2.yaml` | Create |
| `tests/fixtures/integration/config-with-mcp.yaml` | Create |
| `tests/fixtures/integration/invalid-missing-section.yaml` | Create |
| `tests/fixtures/integration/invalid-bad-yaml.yaml` | Create |

### Modified files (2)

| File | Action | Change |
|------|--------|--------|
| `src/verifier.ts` | Modify (in G3) | Export `collectRelativePaths` and `validateDirectory` with `@internal` JSDoc |
| `package.json` | Modify (in G7) | Add `"test:integration"` script |

## Key Implementation Notes

1. **Synchronous verifier API:** The verifier uses synchronous `fs` methods (`readFileSync`, `readdirSync`, `statSync`) matching the Python original. Async provides no benefit for local file comparison and would complicate the test API.

2. **`FileDiff.pythonSize` naming:** The existing `FileDiff` model uses `pythonSize` (from STORY-003). The TS verifier maps `actualBuffer.length` to this field. This is a documented naming divergence -- the field stores "actual output size" in the TS context.

3. **Unified diff implementation:** Implement a minimal unified diff generator (~30 lines) rather than adding a `diff` npm dependency. Tests only assert that `"---"` and `"+++"` markers are present in diff output, so the exact format does not need to match Python's `difflib.unified_diff` byte-for-byte.

4. **Golden files:** The byte-for-byte tests depend on golden files in `tests/golden/{profile}/`. If these do not exist yet (the glob returned empty), tests must skip gracefully with a message: `"Golden files not found. Run: npx tsx scripts/generate-golden.ts --all"`. Generating golden files may be a prerequisite step before G5 can fully pass.

5. **`readdirSync` recursive + withFileTypes:** Requires Node.js 18.17+. The project already requires `>=18` per `package.json`.

6. **Path normalization:** `collectRelativePaths` should normalize path separators to forward slashes (`/`) for consistent cross-platform comparison, since golden files use Unix separators.

7. **CLI integration test approach:** Use `createCli().exitOverride()` from commander to prevent `process.exit()` in tests. This avoids spawning child processes and is significantly faster. Capture console output via `vi.spyOn(console, "log")`.

8. **Test timeout:** Integration tests running full pipeline may need extended timeout (30s per test). Set `testTimeout` in the describe block or vitest config.

9. **Existing fixture reuse:** The `tests/fixtures/project-config.fixture.ts` already provides helper functions (`aDomainTestConfig`, `aValidationTestConfig`, `aProjectConfig`). Edge-case tests in G7 can use these or construct `ProjectConfig` directly.
