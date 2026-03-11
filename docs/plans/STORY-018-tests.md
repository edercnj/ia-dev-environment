# Test Plan -- STORY-018: CLI Entry Point

## Summary

- Total test files: 2 (`cli-display.test.ts`, `cli.test.ts`)
- Total test methods: ~68 (estimated)
- Categories covered: Unit, Integration, Error Handling
- Estimated line coverage: ~97%
- Estimated branch coverage: ~93%

---

## Test File 1: `tests/node/cli-display.test.ts`

### Key Dependencies Under Test

| Module | Export | Description |
|--------|--------|-------------|
| `src/cli-display.ts` | `classifyFiles` | Categorizes file paths into 8 component categories |
| `src/cli-display.ts` | `isKnowledgePackFile` | Detects knowledge pack files via SKILL.md content |
| `src/cli-display.ts` | `formatSummaryTable` | Builds formatted summary table string |
| `src/cli-display.ts` | `displayResult` | Orchestrates full result display via console.log |
| `src/cli-display.ts` | `ComponentCounts` | Interface for category counts |
| `src/models.ts` | `PipelineResult` | Pipeline result type consumed by displayResult |

### Fixture Setup

```
beforeEach:
  - tmpDir = fs.mkdtempSync(path.join(tmpdir(), "cli-display-test-"))
  - Create SKILL.md files with known content for KP detection tests

afterEach:
  - fs.rmSync(tmpDir, { recursive: true, force: true })
```

### Mocking Strategy

- `classifyFiles`: Construct path arrays directly (no real files needed except for KP detection)
- `isKnowledgePackFile`: Use real temp files with controlled SKILL.md content
- `formatSummaryTable`: Pure function, no mocks -- assert on returned string
- `displayResult`: Spy on `console.log` via `vi.spyOn(console, "log")` and assert output lines

### Helpers Needed

- `buildComponentCounts(overrides?: Partial<ComponentCounts>)` -- returns `ComponentCounts` with all zeros by default
- `createSkillDir(tmpDir, name, content)` -- creates `{tmpDir}/skills/{name}/SKILL.md` with given content
- `buildPipelineResult(overrides?)` -- returns `PipelineResult` with minimal valid defaults

---

## Group 1: classifyFiles -- File Path Classification (8 categories)

### 1.1 Individual Category Detection

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 1 | classifyFiles_rulesPath_countsAsRules | Path with `rules` segment increments rules count | Happy |
| 2 | classifyFiles_skillsPath_countsAsSkills | Path with `skills` segment (non-KP) increments skills count | Happy |
| 3 | classifyFiles_knowledgePackPath_countsAsKnowledgePacks | Path with `skills` segment + KP SKILL.md increments knowledgePacks | Happy |
| 4 | classifyFiles_agentsPath_countsAsAgents | Path with `agents` segment increments agents count | Happy |
| 5 | classifyFiles_hooksPath_countsAsHooks | Path with `hooks` segment increments hooks count | Happy |
| 6 | classifyFiles_settingsFile_countsAsSettings | File with `settings` in name increments settings count | Happy |
| 7 | classifyFiles_readmeFile_countsAsReadme | File with `README` in name increments readme count | Happy |
| 8 | classifyFiles_githubPath_countsAsGithub | Path with `github` segment increments github count | Happy |

### 1.2 Priority and Edge Cases

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 9 | classifyFiles_githubSkillsPath_countsAsGithub_notSkills | GitHub path containing `skills` counts as GitHub (github check first) | Boundary |
| 10 | classifyFiles_githubAgentsPath_countsAsGithub_notAgents | GitHub path containing `agents` counts as GitHub | Boundary |
| 11 | classifyFiles_emptyArray_returnsAllZeros | Empty input array produces all-zero counts | Boundary |
| 12 | classifyFiles_unknownPath_notCounted | Path matching no category is not counted | Boundary |

### 1.3 Mixed Inputs

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 13 | classifyFiles_mixedPaths_correctCounts | Array with paths from multiple categories produces correct per-category counts | Happy |
| 14 | classifyFiles_multipleSameCategory_accumulates | Multiple rules paths sum correctly | Happy |

**Parametrized (it.each):**

```typescript
it.each([
  ["rules/01-identity.md", "rules"],
  ["skills/api-design/SKILL.md", "skills"],  // non-KP, mock isKnowledgePackFile
  ["agents/architect.md", "agents"],
  ["hooks/post-compile-check.sh", "hooks"],
  ["settings.json", "settings"],
  ["README.md", "readme"],
  ["github/instructions/coding.md", "github"],
])("classifyFiles_%sPath_incrementsCorrectCategory", (path, category) => ...)
```

---

## Group 2: isKnowledgePackFile -- Knowledge Pack Detection

### 2.1 Detection Logic

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 15 | isKnowledgePackFile_userInvocableFalse_returnsTrue | SKILL.md containing `user-invocable: false` returns true | Happy |
| 16 | isKnowledgePackFile_knowledgePackHeading_returnsTrue | SKILL.md starting with `# Knowledge Pack` returns true | Happy |
| 17 | isKnowledgePackFile_regularSkill_returnsFalse | SKILL.md without KP markers returns false | Happy |
| 18 | isKnowledgePackFile_nonExistentFile_returnsFalse | Path to non-existent file returns false | Boundary |

### 2.2 Parent Directory Lookup

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 19 | isKnowledgePackFile_nonSkillMdInKpDir_looksUpParent_returnsTrue | Non-SKILL.md file in KP skill dir finds SKILL.md in parent and returns true | Happy |
| 20 | isKnowledgePackFile_nonSkillMdNoParentSkillMd_returnsFalse | Non-SKILL.md file with no SKILL.md in parent returns false | Boundary |
| 21 | isKnowledgePackFile_skillMdDirectly_returnsTrue | SKILL.md file itself with KP markers returns true | Happy |

### 2.3 Content Variations

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 22 | isKnowledgePackFile_userInvocableFalseInYamlFrontmatter_returnsTrue | `user-invocable: false` embedded in YAML frontmatter detected | Happy |
| 23 | isKnowledgePackFile_knowledgePackHeadingWithLeadingWhitespace_returnsTrue | Content with leading whitespace before `# Knowledge Pack` detected via lstrip | Boundary |
| 24 | isKnowledgePackFile_userInvocableTrue_returnsFalse | `user-invocable: true` does not match | Negative |

---

## Group 3: formatSummaryTable -- Summary Table Formatting

### 3.1 Table Structure

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 25 | formatSummaryTable_allCategories_formatsCorrectly | All categories with non-zero counts produce correct table | Happy |
| 26 | formatSummaryTable_onlyNonZero_displaysSubset | Only categories with count > 0 appear in rows | Happy |
| 27 | formatSummaryTable_allZeros_showsOnlyHeaderAndTotal | All zero counts produce header, separators, and total=0 only | Boundary |

### 3.2 Alignment and Totals

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 28 | formatSummaryTable_totalIsCorrect | Total row shows sum of all category counts | Happy |
| 29 | formatSummaryTable_alignmentIsCorrect | Label column width accommodates longest label; count column right-aligned | Happy |
| 30 | formatSummaryTable_containsHeaderRow | Output contains `Component` and `Files` header | Happy |
| 31 | formatSummaryTable_containsSeparatorLines | Output contains `\u2500` separator characters | Happy |

### 3.3 Single Category

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 32 | formatSummaryTable_singleCategory_showsOneRowPlusTotal | Only one non-zero category produces one data row | Boundary |
| 33 | formatSummaryTable_largeNumbers_alignedCorrectly | Counts > 99 still align properly | Boundary |

---

## Group 4: displayResult -- Pipeline Result Display

### 4.1 Success Display

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 34 | displayResult_success_printsStatusLine | Prints `Pipeline: Success ({durationMs}ms)` | Happy |
| 35 | displayResult_success_printsSummaryTable | Calls classifyFiles and prints formatted table | Happy |
| 36 | displayResult_success_printsOutputDir | Prints `Output: {outputDir}` | Happy |

### 4.2 Warnings

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 37 | displayResult_withWarnings_printsEachWarning | Each warning printed as `Warning: {text}` | Happy |
| 38 | displayResult_noWarnings_noWarningLines | No `Warning:` lines when warnings array is empty | Boundary |
| 39 | displayResult_multipleWarnings_allPrinted | Three warnings produce three `Warning:` lines | Happy |

### 4.3 Failure

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 40 | displayResult_notSuccess_throwsError | PipelineResult with success=false throws error | Error |

---

## Test File 2: `tests/node/cli.test.ts`

### Key Dependencies Under Test

| Module | Export | Description |
|--------|--------|-------------|
| `src/cli.ts` | `createCli` | Creates commander Command with generate and validate subcommands |
| `src/cli.ts` | `runCli` | Parses argv and executes matched command |
| `src/cli.ts` | `validateGenerateOptions` (internal) | Validates mutual exclusivity of --config and --interactive |
| `src/config.ts` | `loadConfig` | Loads YAML config (mocked) |
| `src/assembler/pipeline.ts` | `runPipeline` | Runs generation pipeline (mocked) |
| `src/interactive.ts` | `runInteractive` | Interactive mode (mocked) |
| `src/utils.ts` | `findResourcesDir` | Auto-detects resources dir (mocked) |
| `src/utils.ts` | `setupLogging` | Configures verbose logging (mocked) |
| `src/domain/validator.ts` | `validateStack` | Validates project config (mocked) |
| `src/exceptions.ts` | `ConfigValidationError` | Config validation error type |
| `src/exceptions.ts` | `PipelineError` | Pipeline error type |
| `src/exceptions.ts` | `CliError` | CLI error type |

### Fixture Setup

```
beforeEach:
  - Reset all mocks via vi.clearAllMocks()
  - Configure default mock return values:
    - loadConfig → valid ProjectConfig
    - runPipeline → PipelineResult(true, ".", ["file.md"], [], 42)
    - runInteractive → valid ProjectConfig
    - findResourcesDir → "/mock/resources"
    - validateStack → [] (no errors)
  - Spy on console.log, console.error
  - Spy on process.exit (or use commander .exitOverride())

afterEach:
  - Restore all spies
```

### Mocking Strategy

```typescript
// Mock external dependencies to isolate CLI wiring logic
vi.mock("../src/config.js", () => ({
  loadConfig: vi.fn().mockReturnValue(buildTestConfig()),
}));

vi.mock("../src/assembler/pipeline.js", () => ({
  runPipeline: vi.fn().mockResolvedValue(
    new PipelineResult(true, ".", ["rules/01-identity.md"], [], 42),
  ),
}));

vi.mock("../src/interactive.js", () => ({
  runInteractive: vi.fn().mockResolvedValue(buildTestConfig()),
}));

vi.mock("../src/utils.js", () => ({
  findResourcesDir: vi.fn().mockReturnValue("/mock/resources"),
  setupLogging: vi.fn(),
}));

vi.mock("../src/domain/validator.js", () => ({
  validateStack: vi.fn().mockReturnValue([]),
}));

vi.mock("../src/cli-display.js", () => ({
  displayResult: vi.fn(),
}));
```

### Commander Testing Pattern

```typescript
// Use .exitOverride() to prevent process.exit() in tests
const cli = createCli();
cli.exitOverride(); // Throws CommanderError instead of calling process.exit

// Parse constructed argv
await cli.parseAsync(["node", "test", "generate", "--config", "path.yaml"]);

// Assert on mocked function calls
expect(loadConfig).toHaveBeenCalledWith("path.yaml");
```

### Helpers Needed

- `buildTestConfig()` -- returns minimal valid `ProjectConfig`
- `parseWithExitOverride(args: string[])` -- creates CLI, applies `.exitOverride()`, parses args
- `buildPipelineResult(overrides?)` -- returns `PipelineResult` with defaults

---

## Group 5: validateGenerateOptions -- Mutual Exclusivity Validation

> Note: `validateGenerateOptions` is internal (not exported). Test indirectly through CLI command parsing, or export for testing via a `@internal` annotation.

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 41 | validateGenerateOptions_bothConfigAndInteractive_throwsError | Providing both `--config` and `--interactive` produces error | Error |
| 42 | validateGenerateOptions_neitherConfigNorInteractive_throwsError | Providing neither produces error | Error |
| 43 | validateGenerateOptions_onlyConfig_noError | Only `--config` succeeds without error | Happy |
| 44 | validateGenerateOptions_onlyInteractive_noError | Only `--interactive` succeeds without error | Happy |

**Testing approach:** Parse CLI args and check for thrown `CliError` or `CommanderError`:

```typescript
it("validateGenerateOptions_bothConfigAndInteractive_throwsError", async () => {
  const cli = createCli();
  cli.exitOverride();
  await expect(
    cli.parseAsync(["node", "test", "generate", "--config", "x.yaml", "--interactive"]),
  ).rejects.toThrow(/mutually exclusive/);
});
```

---

## Group 6: CLI generate Command -- Integration with Pipeline

### 6.1 Config-Based Generation

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 45 | generate_withConfig_callsLoadConfig | `--config path.yaml` calls `loadConfig("path.yaml")` | Integration |
| 46 | generate_withConfig_callsRunPipeline | After loading config, `runPipeline` called with loaded config | Integration |
| 47 | generate_withConfig_callsDisplayResult | After pipeline, `displayResult` called with PipelineResult | Integration |

### 6.2 Interactive Mode

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 48 | generate_withInteractive_callsRunInteractive | `--interactive` calls `runInteractive()` | Integration |
| 49 | generate_withInteractive_passesResultToPipeline | Config from `runInteractive` passed to `runPipeline` | Integration |

### 6.3 Options Forwarding

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 50 | generate_withOutputDir_passedToPipeline | `--output-dir /out` forwarded to `runPipeline` as outputDir | Happy |
| 51 | generate_outputDirDefault_usesDot | Without `--output-dir`, default `"."` used | Happy |
| 52 | generate_withResourcesDir_usesProvidedPath | `--resources-dir /res` forwarded directly, no auto-detect | Happy |
| 53 | generate_withoutResourcesDir_callsFindResourcesDir | Without `--resources-dir`, `findResourcesDir()` called | Happy |
| 54 | generate_withVerbose_callsSetupLogging | `--verbose` calls `setupLogging(true)` | Happy |
| 55 | generate_withDryRun_passedToPipeline | `--dry-run` forwarded to `runPipeline` as `dryRun=true` | Happy |

### 6.4 Missing Options

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 56 | generate_noOptions_showsError | `generate` without `--config` or `--interactive` produces error | Error |

---

## Group 7: CLI validate Command -- Config Validation

### 7.1 Valid Config

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 57 | validate_validConfig_printsValidMessage | Valid config prints `Config is valid.` | Happy |
| 58 | validate_validConfig_callsLoadConfig | `--config path.yaml` calls `loadConfig` | Happy |
| 59 | validate_validConfig_callsValidateStack | Loaded config passed to `validateStack` | Happy |

### 7.2 Invalid Config

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 60 | validate_invalidStack_printsErrors | `validateStack` returning errors prints error messages | Error |
| 61 | validate_invalidStack_exitsWithCode1 | Invalid stack causes exit code 1 | Error |

### 7.3 Config Errors

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 62 | validate_configParseError_showsFriendlyMessage | `loadConfig` throwing `ConfigParseError` produces friendly message | Error |
| 63 | validate_configValidationError_showsFriendlyMessage | `loadConfig` throwing `ConfigValidationError` produces friendly message | Error |

### 7.4 Options

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 64 | validate_withVerbose_callsSetupLogging | `--verbose` calls `setupLogging(true)` | Happy |
| 65 | validate_missingConfigOption_showsError | `validate` without `--config` produces error (required option) | Error |

---

## Group 8: Error Handling -- CLI Error Boundaries

### 8.1 Generate Error Types

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 66 | generate_configValidationError_showsFriendlyMessage | `ConfigValidationError` from `loadConfig` shows message without stack trace | Error |
| 67 | generate_pipelineError_showsFriendlyMessage | `PipelineError` from `runPipeline` shows message without stack trace | Error |

### 8.2 Verbose Error Mode

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 68 | generate_genericError_verboseMode_showsStackTrace | Unknown error with `--verbose` prints full stack trace | Error |
| 69 | generate_genericError_nonVerbose_showsGenericMessage | Unknown error without `--verbose` prints generic message only | Error |

### 8.3 Exit Codes

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 70 | generate_onError_exitsWithCode1 | Any handled error sets exit code to 1 | Error |
| 71 | validate_onError_exitsWithCode1 | Validate errors set exit code to 1 | Error |

---

## Coverage Estimation

| Group | Functions/Paths | Branches | Est. Tests | Line % | Branch % |
|-------|----------------|----------|-----------|--------|----------|
| 1. classifyFiles | 1 | 8 (category checks) + 2 (KP check, github-first) | 14 | 100% | 95% |
| 2. isKnowledgePackFile | 1 | 5 (exists, is SKILL.md, parent lookup, content checks) | 10 | 100% | 95% |
| 3. formatSummaryTable | 1 | 2 (count > 0 filter, width calc) | 9 | 100% | 100% |
| 4. displayResult | 1 | 3 (success check, warnings loop, empty warnings) | 7 | 100% | 100% |
| 5. validateGenerateOptions | 1 | 3 (both, neither, one-of) | 4 | 100% | 100% |
| 6. generate command | 4 (create, load, resolve, execute) | 6 (config/interactive, resources, verbose, dry-run) | 12 | 95% | 90% |
| 7. validate command | 1 | 4 (valid, errors, parse error, missing option) | 9 | 95% | 90% |
| 8. error handling | 0 (cross-cutting) | 4 (error types, verbose) | 6 | 95% | 90% |
| **Total** | **10** | **33** | **~71** | **~97%** | **~93%** |

---

## Branch Coverage Strategy

Critical branches requiring explicit test coverage:

| Branch | Location | Tests Covering It |
|--------|----------|-------------------|
| `github` in path parts (first check) | `classifyFiles` | #8, #9, #10 |
| `README` in filename | `classifyFiles` | #7 |
| `settings` in filename | `classifyFiles` | #6 |
| `hooks` in path parts | `classifyFiles` | #5 |
| `agents` in path parts | `classifyFiles` | #4 |
| `skills` in path parts + KP check | `classifyFiles` | #2, #3 |
| `rules` in path parts | `classifyFiles` | #1 |
| No category match (fallthrough) | `classifyFiles` | #12 |
| File does not exist | `isKnowledgePackFile` | #18 |
| Filename is not SKILL.md | `isKnowledgePackFile` | #19, #20 |
| Parent SKILL.md exists / not exists | `isKnowledgePackFile` | #19, #20 |
| Content contains `user-invocable: false` | `isKnowledgePackFile` | #15, #22 |
| Content starts with `# Knowledge Pack` | `isKnowledgePackFile` | #16, #23 |
| `count > 0` filter in table rows | `formatSummaryTable` | #26, #27 |
| `result.success` is false | `displayResult` | #40 |
| Warnings array empty vs non-empty | `displayResult` | #37, #38 |
| `--config` and `--interactive` both set | `validateGenerateOptions` | #41 |
| Neither `--config` nor `--interactive` | `validateGenerateOptions` | #42 |
| `--config` path (load from file) | generate action | #45-#47 |
| `--interactive` path (run interactive) | generate action | #48-#49 |
| `--resources-dir` explicit vs auto | generate action | #52, #53 |
| `--verbose` on/off | generate action | #54 |
| `--dry-run` on/off | generate action | #55, #51 |
| `ConfigValidationError` catch | error handler | #66 |
| `PipelineError` catch | error handler | #67 |
| Generic error + verbose | error handler | #68 |
| Generic error - verbose | error handler | #69 |
| `validateStack` returns errors | validate action | #60 |
| `validateStack` returns empty | validate action | #57 |

---

## Risks and Gaps

1. **`isKnowledgePackFile` requires real files:** Unlike other functions, KP detection reads files from disk. Tests must create temp SKILL.md files with known content. Use `fs.mkdtempSync` + `fs.writeFileSync` in `beforeEach`, clean up in `afterEach`.

2. **`classifyFiles` calls `isKnowledgePackFile` internally:** For unit tests of `classifyFiles`, mock `isKnowledgePackFile` (via `vi.mock` or module-level spying) to isolate classification logic from file I/O. For integration-level tests, use real temp files.

3. **Commander `.exitOverride()` behavior:** Commander calls `process.exit()` by default on errors. Tests MUST call `.exitOverride()` on the program to prevent test runner termination. The thrown `CommanderError` has `exitCode` and `message` properties for assertions.

4. **Async action handlers:** Commander's `parseAsync()` must be used (not `parse()`) since generate action is async. If `parse()` is used accidentally, async errors will be unhandled rejections.

5. **`process.exit` mocking:** For tests that verify exit codes without `.exitOverride()`, spy on `process.exit` via `vi.spyOn(process, "exit").mockImplementation(() => undefined as never)`. Restore in `afterEach`.

6. **Console output capture:** `displayResult` and error handlers print to `console.log` / `console.error`. Spy on both. Verify exact output strings match Python CLI behavior for compatibility.

7. **`--config` path validation:** The Python CLI uses `click.Path(exists=True)` which validates file existence at parse time. Commander does not do this. The TypeScript implementation must add manual `existsSync` check. Test that a non-existent config path produces a friendly error.

8. **Internal functions not exported:** `validateGenerateOptions`, `loadProjectConfig`, `resolveResourcesDir`, and `executeGenerate` are internal to `cli.ts`. Test them indirectly through CLI command parsing. Alternatively, export them with `@internal` JSDoc annotation for direct unit testing.

---

## Test Naming Convention

All tests follow the project standard:

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

Examples:
- `classifyFiles_rulesPath_countsAsRules`
- `isKnowledgePackFile_userInvocableFalse_returnsTrue`
- `formatSummaryTable_allZeros_showsOnlyHeaderAndTotal`
- `generate_withConfig_callsLoadConfig`
- `validate_invalidStack_printsErrors`
- `generate_configValidationError_showsFriendlyMessage`
