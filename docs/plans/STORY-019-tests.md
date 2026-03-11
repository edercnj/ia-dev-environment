# Test Plan -- STORY-019: Integration Tests + Parity Verification

## Summary

- Total test files: 6
  - `tests/node/verifier.test.ts` (unit)
  - `tests/node/integration/byte-for-byte.test.ts` (integration)
  - `tests/node/integration/e2e-verification.test.ts` (integration)
  - `tests/node/integration/verification-edge-cases.test.ts` (integration)
  - `tests/node/integration/cli-integration.test.ts` (integration)
  - `tests/helpers/file-tree.ts` (test utility, no dedicated test file)
- Total test methods: ~105 (18 unit + 40 parametrized parity + 8 E2E + 9 edge cases + 11 CLI + implicit helper usage)
- Categories covered: Unit, Integration, E2E, Error Handling, Edge Cases, Parity
- Target line coverage on `src/verifier.ts`: >= 98%
- Target overall line coverage: >= 95%
- Target overall branch coverage: >= 90%
- Performance budget: Full integration suite < 60s

---

## Test File 1: `tests/helpers/file-tree.ts` -- Test Utility

### Purpose

Port of Python `conftest.create_file_tree` and `_create_binary_file` helpers. Used by verifier unit tests and edge case tests to build synthetic directory trees.

### Exports

| Function | Signature | Description |
|----------|-----------|-------------|
| `createFileTree` | `(baseDir: string, files: Record<string, string>) => void` | Create text files from a mapping of relative paths to content. Auto-creates intermediate directories. |
| `createBinaryFile` | `(filePath: string, data: Buffer) => void` | Write binary data to a file. Auto-creates parent directories. |

### Validation

- No dedicated test file. Validated implicitly through verifier unit tests (File 2) and edge case tests (File 5).
- If `createFileTree` fails, all dependent tests fail immediately, making bugs obvious.

---

## Test File 2: `tests/node/verifier.test.ts` -- Verifier Unit Tests

### Key Dependencies Under Test

| Module | Export | Description |
|--------|--------|-------------|
| `src/verifier.ts` | `verifyOutput` | Compare two directory trees byte-for-byte, return `VerificationResult` |
| `src/verifier.ts` | `BINARY_DIFF_MESSAGE` | Constant for binary file diff messages |
| `src/verifier.ts` | `MAX_DIFF_LINES` | Constant limiting diff output length |
| `src/models.ts` | `VerificationResult` | Result type with success, mismatches, missing, extra |
| `src/models.ts` | `FileDiff` | Mismatch detail with path, diff text, sizes |

### Fixture Setup

```
beforeEach:
  - tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "verifier-test-"))

afterEach:
  - fs.rmSync(tmpDir, { recursive: true, force: true })
```

### Mocking Strategy

- **No mocks.** All tests use real file system operations on temp directories.
- `createFileTree` and `createBinaryFile` helpers from `tests/helpers/file-tree.ts` build test data.

### Internal Functions to Test

The verifier exposes only `verifyOutput` publicly. Internal functions (`validateDirectory`, `collectRelativePaths`, `findMismatches`, `compareFiles`, `generateTextDiff`) are exercised through `verifyOutput`. Where the plan lists them as separate test groups, they are tested via controlled inputs that isolate specific code paths.

---

## Group 1: verifyOutput -- Identical Directories (Happy Path)

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 1 | `verifyOutput_identicalDirs_returnsSuccess` | Two dirs with same files/content -> `success: true` | Happy |
| 2 | `verifyOutput_identicalDirs_totalFilesCorrect` | Total files count matches the number of files in both dirs | Happy |

### Setup (both tests)

```typescript
// Arrange
const actualDir = path.join(tmpDir, "actual");
const refDir = path.join(tmpDir, "reference");
const files = { "a.txt": "hello", "b.txt": "world" };
createFileTree(actualDir, files);
createFileTree(refDir, files);

// Act
const result = verifyOutput(actualDir, refDir);
```

### Assertions

| # | Assertion |
|---|-----------|
| 1 | `expect(result.success).toBe(true)` |
| 2 | `expect(result.totalFiles).toBe(2)` (or 3 for the 3-file variant) |

---

## Group 2: verifyOutput -- Mismatch Detection

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 3 | `verifyOutput_mismatchDetected_returnsFailure` | Different content in same-named file -> `success: false`, 1 mismatch | Happy |
| 4 | `verifyOutput_mismatchContainsDiffString` | Mismatch diff text contains `---` and `+++` unified diff markers | Happy |
| 5 | `verifyOutput_mismatchContainsFileSizes` | `FileDiff.pythonSize` and `FileDiff.referenceSize` reflect actual byte lengths | Happy |

### Setup (test 3)

```typescript
// Arrange
createFileTree(actualDir, { "a.txt": "new" });
createFileTree(refDir, { "a.txt": "old" });

// Act
const result = verifyOutput(actualDir, refDir);
```

### Assertions

| # | Assertion |
|---|-----------|
| 3 | `expect(result.success).toBe(false)` and `expect(result.mismatches).toHaveLength(1)` |
| 4 | `expect(result.mismatches[0].diff).toContain("---")` and `toContain("+++")` |
| 5 | `expect(result.mismatches[0].pythonSize).toBe(5)` for "short" vs `referenceSize = 16` for "much longer text" |

---

## Group 3: verifyOutput -- Missing and Extra Files

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 6 | `verifyOutput_missingFileDetected` | File in reference but not in actual -> appears in `missingFiles` | Happy |
| 7 | `verifyOutput_extraFileDetected` | File in actual but not in reference -> appears in `extraFiles` | Happy |

### Setup (test 6)

```typescript
// Arrange
createFileTree(actualDir, { "a.txt": "a" });
createFileTree(refDir, { "a.txt": "a", "b.txt": "b" });

// Act
const result = verifyOutput(actualDir, refDir);
```

### Assertions

| # | Assertion |
|---|-----------|
| 6 | `expect(result.success).toBe(false)` and `expect(result.missingFiles).toContain("b.txt")` |
| 7 | `expect(result.success).toBe(false)` and `expect(result.extraFiles).toContain("c.txt")` |

---

## Group 4: verifyOutput -- Nested Directories and Edge Cases

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 8 | `verifyOutput_nestedDirectoriesCompared` | Files in nested subdirectories are compared correctly | Happy |
| 9 | `verifyOutput_emptyDirs_returnsSuccess` | Both dirs exist but are empty -> `success: true`, `totalFiles: 0` | Boundary |
| 10 | `verifyOutput_whitespaceDifferenceDetected` | Trailing whitespace difference is detected as a mismatch | Boundary |

### Setup (test 8)

```typescript
// Arrange
const files = { "sub/dir/file.txt": "content" };
createFileTree(actualDir, files);
createFileTree(refDir, files);

// Act
const result = verifyOutput(actualDir, refDir);
```

### Assertions

| # | Assertion |
|---|-----------|
| 8 | `expect(result.success).toBe(true)` and `expect(result.totalFiles).toBe(1)` |
| 9 | `expect(result.success).toBe(true)` and `expect(result.totalFiles).toBe(0)` |
| 10 | `expect(result.success).toBe(false)` and `expect(result.mismatches).toHaveLength(1)` |

---

## Group 5: verifyOutput -- Binary File Handling

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 11 | `verifyOutput_binaryFileMismatch_handled` | Different binary files -> diff text contains "binary" (BINARY_DIFF_MESSAGE) | Boundary |

### Setup

```typescript
// Arrange
createBinaryFile(path.join(actualDir, "img.bin"), Buffer.from([0x00, 0x01, 0x02]));
createBinaryFile(path.join(refDir, "img.bin"), Buffer.from([0x00, 0x01, 0xff]));

// Act
const result = verifyOutput(actualDir, refDir);
```

### Assertions

| # | Assertion |
|---|-----------|
| 11 | `expect(result.success).toBe(false)` and `expect(result.mismatches[0].diff.toLowerCase()).toContain("binary")` |

---

## Group 6: collectRelativePaths -- Path Collection (via verifyOutput)

These tests exercise the internal `collectRelativePaths` function through carefully controlled directory structures.

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 12 | `collectRelativePaths_returnsSortedPaths` | Files are returned in alphabetical sort order | Happy |
| 13 | `collectRelativePaths_includesNestedFiles` | Nested files (e.g., `sub/nested.txt`) are included | Happy |
| 14 | `collectRelativePaths_emptyDir_returnsEmpty` | Empty directory produces empty result | Boundary |

### Implementation Note

If `collectRelativePaths` is not exported from `src/verifier.ts`, these behaviors are verified indirectly:
- Test 12: Create dirs with files `c.txt`, `a.txt`, `b.txt` in both actual and ref; verify `verifyOutput` returns success (sorted comparison works).
- Test 13: Create `root.txt` and `sub/nested.txt`; verify `verifyOutput` finds both.
- Test 14: Verified by test 9 (empty dirs).

If `collectRelativePaths` is exported for testing (recommended), test directly:

```typescript
// Arrange
createFileTree(tmpDir, { "c.txt": "c", "a.txt": "a", "b.txt": "b" });

// Act
const paths = collectRelativePaths(tmpDir);

// Assert
expect(paths).toEqual(["a.txt", "b.txt", "c.txt"]);
```

---

## Group 7: validateDirectory -- Directory Validation (via verifyOutput error paths)

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 15 | `validateDirectory_nonexistentActualDir_throwsError` | Non-existent actual dir throws Error with descriptive message | Error |
| 16 | `validateDirectory_nonexistentReferenceDir_throwsError` | Non-existent reference dir throws Error with descriptive message | Error |
| 17 | `validateDirectory_fileAsActualDir_throwsError` | Regular file passed as actual dir throws Error mentioning "not a directory" | Error |
| 18 | `validateDirectory_fileAsReferenceDir_throwsError` | Regular file passed as reference dir throws Error mentioning "not a directory" | Error |

### Setup (test 15)

```typescript
// Arrange
const refDir = path.join(tmpDir, "ref");
fs.mkdirSync(refDir);

// Act & Assert
expect(() => verifyOutput(path.join(tmpDir, "nope"), refDir))
  .toThrow(/does not exist/);
```

### Assertions

| # | Assertion |
|---|-----------|
| 15 | `expect(...).toThrow()` with message matching `actualDir` or `does not exist` |
| 16 | `expect(...).toThrow()` with message matching `referenceDir` or `does not exist` |
| 17 | `expect(...).toThrow()` with message matching `not a directory` |
| 18 | `expect(...).toThrow()` with message matching `not a directory` |

---

## Test File 3: `tests/node/integration/byte-for-byte.test.ts` -- Byte-for-Byte Parity Tests

### Key Dependencies Under Test

| Module | Export | Description |
|--------|--------|-------------|
| `src/config.ts` | `loadConfig` | Parse YAML config file into `ProjectConfig` |
| `src/assembler/pipeline.ts` | `runPipeline` | Execute full generation pipeline |
| `src/verifier.ts` | `verifyOutput` | Compare output against golden reference |
| `src/models.ts` | `ProjectConfig`, `PipelineResult`, `VerificationResult` | Data models |

### Config Profiles (Parametrized)

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

### Fixture Setup

```
beforeEach:
  - tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "parity-test-"))

afterEach:
  - fs.rmSync(tmpDir, { recursive: true, force: true })
```

### Golden Directory Layout

```
tests/golden/
  go-gin/
  java-quarkus/
  java-spring/
  kotlin-ktor/
  python-click-cli/
  python-fastapi/
  rust-axum/
  typescript-nestjs/
```

**Note:** Golden directories are not yet committed (glob returned no results). Tests MUST skip gracefully if golden files are missing, with message: `"Golden files not found. Run: npx tsx scripts/generate-golden.ts --all"`.

### Mocking Strategy

- **No mocks.** Real `loadConfig`, `runPipeline`, `verifyOutput`. This is a full integration test.

### Skip Strategy

```typescript
function skipIfNoGolden(profileName: string): void {
  const goldenPath = path.join(GOLDEN_DIR, profileName);
  if (!fs.existsSync(goldenPath)) {
    it.skip(`Golden files not found for ${profileName}. Run: npx tsx scripts/generate-golden.ts --all`);
  }
}
```

### Helper

```typescript
function runPipelineForProfile(profileName: string, outputDir: string): Promise<PipelineResult> {
  const configPath = path.join(CONFIG_TEMPLATES_DIR, `setup-config.${profileName}.yaml`);
  const config = loadConfig(configPath);
  return runPipeline(config, RESOURCES_DIR, outputDir, false);
}

function formatMismatches(result: VerificationResult): string {
  const lines: string[] = [];
  for (const m of result.mismatches) {
    lines.push(`MISMATCH: ${m.path} (actual=${m.pythonSize}B, ref=${m.referenceSize}B)`);
    lines.push(m.diff.slice(0, 500));
  }
  for (const p of result.missingFiles) lines.push(`MISSING: ${p}`);
  for (const p of result.extraFiles) lines.push(`EXTRA: ${p}`);
  return lines.join("\n");
}
```

---

## Group 8: Parity -- Pipeline Matches Golden Files (8 profiles x 5 assertions = 40 tests)

Uses `describe.each(CONFIG_PROFILES)` or `it.each` for parametrization.

| # | Test Name Pattern | Description | Type |
|---|-------------------|-------------|------|
| 19-26 | `pipelineMatchesGoldenFiles_{profile}` | TS pipeline output is byte-for-byte identical to golden reference | Integration |
| 27-34 | `noMissingFiles_{profile}` | No files exist in golden but not in output | Integration |
| 35-42 | `noExtraFiles_{profile}` | No files exist in output but not in golden | Integration |
| 43-50 | `pipelineSuccessForProfile_{profile}` | Pipeline returns `success: true` (no golden needed) | Integration |
| 51-58 | `totalFilesGreaterThanZero_{profile}` | Verification result shows > 0 total files | Integration |

### Sample Test (test 19)

```typescript
// Arrange
const outputDir = path.join(tmpDir, "output");

// Act
await runPipelineForProfile(profileName, outputDir);
const goldenPath = path.join(GOLDEN_DIR, profileName);
const result = verifyOutput(outputDir, goldenPath);

// Assert
expect(result.success).toBe(true);  // If false, formatMismatches(result) shows details
```

### Assertions per Profile

| Test | Assertion |
|------|-----------|
| `pipelineMatchesGoldenFiles` | `expect(result.success).toBe(true)` with `formatMismatches` on failure |
| `noMissingFiles` | `expect(result.missingFiles).toEqual([])` |
| `noExtraFiles` | `expect(result.extraFiles).toEqual([])` |
| `pipelineSuccessForProfile` | `expect(pipelineResult.success).toBe(true)` |
| `totalFilesGreaterThanZero` | `expect(result.totalFiles).toBeGreaterThan(0)` |

---

## Test File 4: `tests/node/integration/e2e-verification.test.ts` -- E2E Verification

### Purpose

Full end-to-end flow: load config, run pipeline, verify against golden. Port of `test_e2e_verification.py`.

### Key Dependencies Under Test

Same as File 3 (loadConfig, runPipeline, verifyOutput).

### Fixture Setup

Same as File 3 (tmpDir creation/cleanup).

---

## Group 9: E2E -- Full Flow per Profile (8 parametrized tests)

| # | Test Name Pattern | Description | Type |
|---|-------------------|-------------|------|
| 59-66 | `fullFlowForProfile_{profile}` | Load config -> run pipeline -> verify output against golden -> all pass | E2E |

### Sample Test

```typescript
it.each(CONFIG_PROFILES)("fullFlowForProfile_%s", async (profileName) => {
  // Skip if no golden
  const goldenPath = path.join(GOLDEN_DIR, profileName);
  if (!fs.existsSync(goldenPath)) return; // or it.skip

  // Arrange
  const configPath = path.join(CONFIG_TEMPLATES_DIR, `setup-config.${profileName}.yaml`);
  const config = loadConfig(configPath);
  const outputDir = path.join(tmpDir, "output");

  // Act
  const pipelineResult = await runPipeline(config, RESOURCES_DIR, outputDir, false);
  const verification = verifyOutput(outputDir, goldenPath);

  // Assert
  expect(pipelineResult.success).toBe(true);
  expect(verification.success).toBe(true);
});
```

---

## Test File 5: `tests/node/integration/verification-edge-cases.test.ts` -- Edge Cases

### Key Dependencies Under Test

| Module | Export | Description |
|--------|--------|-------------|
| `src/verifier.ts` | `verifyOutput` | Directory comparison |
| `src/assembler/pipeline.ts` | `runPipeline` | Pipeline execution |
| `src/models.ts` | `ProjectConfig` | Config model |
| `tests/fixtures/project-config.fixture.ts` | `aProjectConfig`, `aValidationTestConfig` | Config factories |
| `tests/helpers/file-tree.ts` | `createFileTree` | File tree builder |

### Fixture Setup

```
beforeEach:
  - tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "edge-case-test-"))

afterEach:
  - fs.rmSync(tmpDir, { recursive: true, force: true })
```

### Mocking Strategy

- **No mocks.** Real pipeline and verifier. The point is to test real behavior.

### Minimal Config Helper

```typescript
function makeMinimalConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("minimal-tool", "Minimal CLI tool"),
    new ArchitectureConfig("library"),
    [new InterfaceConfig("cli")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig("commander", "12", "npm"),
  );
}
```

---

## Group 10: Minimal Config

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 67 | `minimalConfig_producesOutput` | Pipeline with minimal config generates > 0 files | Happy |
| 68 | `minimalConfig_verifiesAgainstSelf` | Running pipeline twice and comparing output to itself -> `success: true` | Happy |

### Assertions

| # | Assertion |
|---|-----------|
| 67 | `expect(pipelineResult.success).toBe(true)` and `expect(pipelineResult.filesGenerated.length).toBeGreaterThan(0)` |
| 68 | `expect(verifyOutput(dirA, dirB).success).toBe(true)` |

---

## Group 11: Idempotency

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 69 | `pipeline_isIdempotent` | Two consecutive pipeline runs with same config produce identical output | Happy |

### Setup

```typescript
// Arrange
const config = makeMinimalConfig();
const dir1 = path.join(tmpDir, "run1");
const dir2 = path.join(tmpDir, "run2");

// Act
await runPipeline(config, RESOURCES_DIR, dir1, false);
await runPipeline(config, RESOURCES_DIR, dir2, false);
const result = verifyOutput(dir1, dir2);

// Assert
expect(result.success).toBe(true);
```

---

## Group 12: Empty Reference / Empty Output

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 70 | `allFiles_reportedAsExtra` | Pipeline output vs empty reference -> all files are `extraFiles` | Boundary |
| 71 | `allFiles_reportedAsMissing` | Empty output vs populated reference -> all files are `missingFiles` | Boundary |

### Assertions

| # | Assertion |
|---|-----------|
| 70 | `expect(result.success).toBe(false)` and `expect(result.extraFiles.length).toBeGreaterThan(0)` and `expect(result.missingFiles).toEqual([])` |
| 71 | `expect(result.success).toBe(false)` and `expect(result.missingFiles).toHaveLength(2)` and `expect(result.extraFiles).toEqual([])` |

---

## Group 13: Invalid Directories

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 72 | `nonexistentActualDir_throwsError` | Passing non-existent path as actual dir -> throws Error | Error |
| 73 | `nonexistentReferenceDir_throwsError` | Passing non-existent path as reference dir -> throws Error | Error |
| 74 | `fileAsActualDir_throwsError` | Passing a regular file as actual dir -> throws Error with "not a directory" | Error |
| 75 | `fileAsReferenceDir_throwsError` | Passing a regular file as reference dir -> throws Error with "not a directory" | Error |

### Sample Test (test 72)

```typescript
// Arrange
const refDir = path.join(tmpDir, "ref");
fs.mkdirSync(refDir);

// Act & Assert
expect(() => verifyOutput(path.join(tmpDir, "nonexistent"), refDir))
  .toThrow(/does not exist/);
```

---

## Test File 6: `tests/node/integration/cli-integration.test.ts` -- CLI Integration Tests

### Key Dependencies Under Test

| Module | Export | Description |
|--------|--------|-------------|
| `src/cli.ts` | `createCli` | CLI program factory |
| `src/config.ts` | `loadConfig` | Config parser (real, not mocked) |
| `src/assembler/pipeline.ts` | `runPipeline` | Pipeline (real, not mocked) |
| `src/domain/validator.ts` | `validateStack` | Stack validator (real, not mocked) |

### Fixture Setup

```
beforeEach:
  - tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "cli-int-test-"))
  - Create valid fixture: write a known-good YAML to tmpDir
  - Create invalid fixtures: write malformed YAML, missing-section YAML

afterEach:
  - fs.rmSync(tmpDir, { recursive: true, force: true })
  - Restore console.log / console.error spies
```

### Mocking Strategy

- **Minimal mocking.** Real `loadConfig`, `runPipeline`, `validateStack`.
- `process.exit` is spied on to prevent test process termination.
- `console.log` and `console.error` are spied on to capture output for assertions.
- Uses `createCli().parseAsync([...])` for direct invocation (no process spawning).

### CLI Invocation Helper

```typescript
async function runCli(args: string[]): Promise<void> {
  const cli = createCli();
  await cli.parseAsync(["node", "test", ...args]);
}
```

---

## Group 14: Dry-Run Mode

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 76 | `dryRun_producesNoOutputFiles` | `generate --dry-run --config fixture.yaml` produces no files in output dir | Happy |
| 77 | `dryRun_returnsFileList` | Dry-run result includes list of files that would be generated | Happy |
| 78 | `dryRun_containsDryRunWarning` | Console output contains dry-run indicator message | Happy |
| 79 | `dryRun_withValidConfig_succeeds` | Dry-run with valid config exits without error | Happy |

### Sample Test (test 76)

```typescript
// Arrange
const outputDir = path.join(tmpDir, "output");
fs.mkdirSync(outputDir);
const configPath = path.join(tmpDir, "valid.yaml");
// (write valid YAML fixture to configPath)

// Act
await runCli([
  "generate",
  "--config", configPath,
  "--output-dir", outputDir,
  "--dry-run",
  "--resources-dir", RESOURCES_DIR,
]);

// Assert
const files = fs.readdirSync(outputDir);
expect(files).toHaveLength(0); // No files created
```

### Assertions

| # | Assertion |
|---|-----------|
| 76 | Output directory remains empty (or unchanged) |
| 77 | `console.log` spy called with content listing expected file paths |
| 78 | `console.log` spy called with string containing "dry" or "DRY" |
| 79 | `process.exit` NOT called with 1 |

---

## Group 15: Validate Command

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 80 | `validate_validConfig_printsValid` | `validate --config valid.yaml` prints "Config is valid." | Happy |
| 81 | `validate_invalidConfig_exitsWithError` | `validate --config invalid.yaml` calls `process.exit(1)` | Error |
| 82 | `validate_missingSection_showsSectionName` | Config missing required section -> error message mentions the missing section | Error |

### Sample Test (test 80)

```typescript
// Arrange
const configPath = path.join(tmpDir, "valid.yaml");
// (write valid config YAML)

// Act
await runCli(["validate", "--config", configPath]);

// Assert
expect(logSpy).toHaveBeenCalledWith("Config is valid.");
```

### Assertions

| # | Assertion |
|---|-----------|
| 80 | `expect(logSpy).toHaveBeenCalledWith("Config is valid.")` |
| 81 | `expect(exitSpy).toHaveBeenCalledWith(1)` |
| 82 | `expect(errorSpy).toHaveBeenCalledWith(expect.stringContaining("Missing required config sections"))` |

---

## Group 16: Error Handling

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 83 | `generate_invalidConfigPath_showsError` | Non-existent config path -> "Config file not found" error | Error |
| 84 | `generate_malformedYaml_showsParseError` | Malformed YAML file -> "Failed to parse config file" error | Error |
| 85 | `generate_missingRequiredSection_showsValidationError` | Config missing `language` section -> "Missing required config sections" error | Error |
| 86 | `validate_nonexistentConfig_showsError` | Non-existent config for validate -> "Config file not found" error | Error |

### Sample Test (test 84)

```typescript
// Arrange
const badYamlPath = path.join(tmpDir, "bad.yaml");
fs.writeFileSync(badYamlPath, ":::invalid: yaml: [[[", "utf-8");

// Act
await runCli(["generate", "--config", badYamlPath]);

// Assert
expect(errorSpy).toHaveBeenCalledWith(
  expect.stringContaining("Failed to parse config file"),
);
expect(exitSpy).toHaveBeenCalledWith(1);
```

### Assertions

| # | Assertion |
|---|-----------|
| 83 | `expect(errorSpy).toHaveBeenCalledWith(expect.stringContaining("Config file not found"))` |
| 84 | `expect(errorSpy).toHaveBeenCalledWith(expect.stringContaining("Failed to parse config file"))` |
| 85 | `expect(errorSpy).toHaveBeenCalledWith(expect.stringContaining("Missing required config sections"))` |
| 86 | `expect(errorSpy).toHaveBeenCalledWith(expect.stringContaining("Config file not found"))` |

---

## Integration Fixtures: `tests/fixtures/integration/*.yaml`

### Fixture Inventory

| # | File | Purpose | Used By |
|---|------|---------|---------|
| 1 | `minimal.yaml` | Only required fields | Edge case tests, dry-run tests |
| 2 | `full.yaml` | All sections populated | Validate tests |
| 3 | `java-spring-rest.yaml` | Java 21 + Spring Boot + REST + PostgreSQL + Redis + K8s | Parity (via config-templates) |
| 4 | `python-fastapi.yaml` | Python 3.12 + FastAPI + REST + MongoDB | Parity (via config-templates) |
| 5 | `go-gin-grpc.yaml` | Go 1.22 + Gin + gRPC + Kafka | Parity (via config-templates) |
| 6 | `kotlin-ktor-events.yaml` | Kotlin 2.0 + Ktor + Kafka event-driven | Parity (via config-templates) |
| 7 | `ts-nestjs-fullstack.yaml` | TypeScript 5 + NestJS + REST + GraphQL | Parity (via config-templates) |
| 8 | `rust-axum-library.yaml` | Rust + Axum library, no interfaces | Parity (via config-templates) |
| 9 | `config-v2.yaml` | Legacy v2 format with `type`/`stack` | V2 migration tests |
| 10 | `config-with-mcp.yaml` | Full config + MCP servers | MCP integration |
| 11 | `invalid-missing-section.yaml` | Missing `language` section | Error handling tests |
| 12 | `invalid-bad-yaml.yaml` | Malformed YAML syntax | Error handling tests |

**Note:** Parity tests (File 3) use the existing `resources/config-templates/setup-config.{profile}.yaml` configs directly, not the new integration fixtures. The new fixtures in `tests/fixtures/integration/` serve the edge case, dry-run, validate, and error tests.

---

## Coverage Strategy

### Target Metrics

| Scope | Line | Branch | Strategy |
|-------|------|--------|----------|
| `src/verifier.ts` | >= 98% | >= 95% | Dedicated unit test file (18 tests) covers all functions and branches |
| Overall (`src/**/*.ts`) | >= 95% | >= 90% | Unit tests (existing + verifier) + integration tests exercise all pipeline paths |

### Coverage Gaps to Watch

| Code Path | Risk | Test That Covers It |
|-----------|------|---------------------|
| `validateDirectory` -- nonexistent path | Low | Tests 15-16 |
| `validateDirectory` -- file-not-dir | Low | Tests 17-18 |
| `generateTextDiff` -- binary file (UnicodeDecodeError equivalent) | Medium | Test 11 |
| `generateTextDiff` -- MAX_DIFF_LINES truncation | Medium | Add dedicated test: create two files with > 200 differing lines, verify diff is truncated |
| `compareFiles` -- identical files (returns null) | Low | Tests 1-2 |
| `compareFiles` -- different files (returns FileDiff) | Low | Tests 3-5 |
| `collectRelativePaths` -- empty dir | Low | Test 9/14 |
| `collectRelativePaths` -- nested dirs | Low | Test 8/13 |

### Additional Tests for Coverage Gaps

| # | Test Name | Description | Purpose |
|---|-----------|-------------|---------|
| 87 | `generateTextDiff_longDiff_truncatedAtMaxLines` | Files with > 200 differing lines -> diff truncated at `MAX_DIFF_LINES` | Branch: MAX_DIFF_LINES check |
| 88 | `verifyOutput_multipleCommonFiles_allCompared` | 5+ common files, mix of matching and mismatching -> correct counts | Branch: loop completion |
| 89 | `verifyOutput_mixedMissingExtraAndMismatch_allReported` | Combination of missing, extra, and mismatched files -> all three categories populated | Branch: all 3 failure types |

---

## Test Matrix Summary

| Group | File | Test Count | Type | Dependencies |
|-------|------|------------|------|-------------|
| G1: Identical dirs | verifier.test.ts | 2 | Unit | file-tree.ts |
| G2: Mismatch detection | verifier.test.ts | 3 | Unit | file-tree.ts |
| G3: Missing/extra files | verifier.test.ts | 2 | Unit | file-tree.ts |
| G4: Nested/edge cases | verifier.test.ts | 3 | Unit | file-tree.ts |
| G5: Binary files | verifier.test.ts | 1 | Unit | file-tree.ts |
| G6: Path collection | verifier.test.ts | 3 | Unit | file-tree.ts |
| G7: Directory validation | verifier.test.ts | 4 | Unit | -- |
| G8: Byte-for-byte parity | byte-for-byte.test.ts | 40 | Integration | golden files, config-templates |
| G9: E2E full flow | e2e-verification.test.ts | 8 | E2E | golden files, config-templates |
| G10: Minimal config | verification-edge-cases.test.ts | 2 | Integration | pipeline, verifier |
| G11: Idempotency | verification-edge-cases.test.ts | 1 | Integration | pipeline, verifier |
| G12: Empty dirs | verification-edge-cases.test.ts | 2 | Integration | pipeline, verifier, file-tree.ts |
| G13: Invalid dirs | verification-edge-cases.test.ts | 4 | Integration | verifier |
| G14: Dry-run | cli-integration.test.ts | 4 | Integration | CLI, pipeline |
| G15: Validate command | cli-integration.test.ts | 3 | Integration | CLI, validator |
| G16: Error handling | cli-integration.test.ts | 4 | Integration | CLI |
| Coverage gap fillers | verifier.test.ts | 3 | Unit | file-tree.ts |
| **Total** | | **~89 explicit + ~16 parametrized variants** | | |

**Grand total: ~105 test methods** (including parametrized expansions across 8 profiles).

---

## Execution Plan

### Running Unit Tests Only

```bash
npx vitest run tests/node/verifier.test.ts
```

### Running Integration Tests Only

```bash
npx vitest run tests/node/integration/
```

### Running All Tests with Coverage

```bash
npx vitest run --coverage
```

### Expected Timeouts

| Test File | Expected Duration | Notes |
|-----------|------------------|-------|
| verifier.test.ts | < 2s | Pure file I/O on temp dirs |
| byte-for-byte.test.ts | < 15s | 8 profiles x ~1s pipeline each |
| e2e-verification.test.ts | < 15s | Same as byte-for-byte |
| verification-edge-cases.test.ts | < 5s | Minimal pipeline runs |
| cli-integration.test.ts | < 5s | CLI parsing + validation |
| **Total** | **< 42s** | Well within 60s budget |

---

## Naming Convention Reference

All test names follow `[methodUnderTest]_[scenario]_[expectedBehavior]`:

```
verifyOutput_identicalDirs_returnsSuccess
verifyOutput_mismatchDetected_returnsFailure
verifyOutput_binaryFileMismatch_handled
validateDirectory_nonexistentActualDir_throwsError
collectRelativePaths_returnsSortedPaths
pipelineMatchesGoldenFiles_{profile}
noMissingFiles_{profile}
fullFlowForProfile_{profile}
minimalConfig_producesOutput
pipeline_isIdempotent
dryRun_producesNoOutputFiles
validate_validConfig_printsValid
generate_malformedYaml_showsParseError
```

---

## Vitest Configuration Notes

The existing `vitest.config.ts` already covers all test paths via `tests/**/*.test.ts`. No changes needed for test discovery. The coverage configuration already enforces:
- `lines: 95` threshold
- `branches: 90` threshold
- V8 provider with text + lcov reporters
- Source includes `src/**/*.ts`, excludes `dist/`, `resources/`, `tests/`

If integration tests require longer timeouts, add a per-file configuration:

```typescript
// In byte-for-byte.test.ts
import { describe, it, expect } from "vitest";
// vitest automatically uses the project-level config
// Override timeout if needed:
describe("byte-for-byte parity", { timeout: 30000 }, () => { ... });
```
