# Task Decomposition -- STORY-018: CLI Entry Point

**Status:** PENDING
**Date:** 2026-03-11
**Blocked By:** STORY-004 (config loader), STORY-016 (pipeline orchestrator), STORY-017 (interactive mode)
**Blocks:** STORY-019

---

## G1 -- Foundation (New Types and Interfaces)

**Purpose:** Define the `ComponentCounts` interface used by the display module and ensure all type contracts are in place before implementing logic. This group introduces no runtime behavior -- only types.
**Dependencies:** None
**Compiles independently:** Yes -- only adds new types.

### T1.1 -- Define `ComponentCounts` interface

- **File:** `src/cli-display.ts` (create)
- **What to implement:**
  1. Define and export the `ComponentCounts` interface:
     ```typescript
     export interface ComponentCounts {
       readonly rules: number;
       readonly skills: number;
       readonly knowledgePacks: number;
       readonly agents: number;
       readonly hooks: number;
       readonly settings: number;
       readonly readme: number;
       readonly github: number;
     }
     ```
  2. Import `PipelineResult` from `./models.js` (needed by later functions in this file; import now to validate dependency).
  3. Add stub exports for all four functions (`classifyFiles`, `isKnowledgePackFile`, `formatSummaryTable`, `displayResult`) with `throw new Error("not implemented")` bodies so the file compiles and downstream imports resolve.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Compilation checkpoint G1

```
npx tsc --noEmit   # zero errors -- new file with types and stubs
```

---

## G2 -- Display Module (cli-display.ts)

**Purpose:** Implement the four display/classification functions in `src/cli-display.ts`. These are pure functions (except `isKnowledgePackFile` which reads the filesystem and `displayResult` which writes to stdout). Splitting display logic from CLI wiring keeps `src/cli.ts` under 250 lines and makes classification independently testable.
**Dependencies:** G1 (needs the `ComponentCounts` type and file scaffold)
**Compiles independently:** Yes

### T2.1 -- Implement `isKnowledgePackFile(filePath)`

- **File:** `src/cli-display.ts` (modify)
- **What to implement:**
  1. Import `existsSync`, `readFileSync` from `node:fs` and `basename`, `dirname`, `join` from `node:path`.
  2. Implement the function following this logic (mirrors Python `_is_knowledge_pack_file`):
     - If file does not exist on disk (`!existsSync(filePath)`), return `false`.
     - If `basename(filePath)` is not `"SKILL.md"`, look for `SKILL.md` in `dirname(filePath)`.
     - If `SKILL.md` not found, return `false`.
     - Read `SKILL.md` content as UTF-8.
     - Return `true` if content contains `"user-invocable: false"` OR any line (left-trimmed) starts with `"# Knowledge Pack"`.
  3. JSDoc: document the function purpose, parameters, return value, and edge cases.
- **Dependencies on other tasks:** T1.1 (file must exist)
- **Estimated complexity:** S

### T2.2 -- Implement `classifyFiles(filePaths)`

- **File:** `src/cli-display.ts` (modify)
- **What to implement:**
  1. Import `sep` from `node:path` (for platform-safe splitting).
  2. Implement the function following this classification order (mirrors Python `_classify_files`):
     - Initialize all counts to 0.
     - For each path in `filePaths`, split into segments (split on `/` and `\`).
     - Check order (first match wins):
       1. If any segment is `"github"` -> increment `github`
       2. If filename contains `"README"` -> increment `readme`
       3. If filename contains `"settings"` -> increment `settings`
       4. If any segment is `"hooks"` -> increment `hooks`
       5. If any segment is `"agents"` -> increment `agents`
       6. If any segment is `"skills"` -> call `isKnowledgePackFile(filePath)`: if true -> `knowledgePacks`, else -> `skills`
       7. If any segment is `"rules"` -> increment `rules`
     - Return `ComponentCounts` object.
  3. JSDoc: document classification priority and segment matching.
- **Dependencies on other tasks:** T2.1 (`isKnowledgePackFile` must be implemented)
- **Estimated complexity:** M

### T2.3 -- Implement `formatSummaryTable(counts)`

- **File:** `src/cli-display.ts` (modify)
- **What to implement:**
  1. Implement the function (mirrors Python `_display_summary_table`):
     - Define the label/value pairs: `[["Rules (.claude)", counts.rules], ["Skills (.claude)", counts.skills], ...]` covering all 8 categories.
     - Filter to only categories with count > 0.
     - Compute total from all categories (including zero ones).
     - Determine label column width: max of all label lengths and `"Component"`.
     - Build header row: `"Component"` padded to label width + `"  Files"` right-aligned.
     - Build separator using `"\u2500"` (box-drawing horizontal line).
     - For each non-zero category, build a row: label padded + right-aligned count.
     - Build footer separator and total row.
     - Return the assembled string (newline-separated).
  2. The function returns a string -- it does NOT call `console.log`.
  3. JSDoc: document format, Unicode separator character, and alignment rules.
- **Dependencies on other tasks:** T1.1 (needs `ComponentCounts` type)
- **Estimated complexity:** M

### T2.4 -- Implement `displayResult(result)`

- **File:** `src/cli-display.ts` (modify)
- **What to implement:**
  1. Implement the function (mirrors Python `_display_result`):
     - If `!result.success`, throw an `Error("Pipeline did not succeed")`.
     - Print `"Pipeline: Success ({durationMs}ms)"` via `console.log`.
     - Print blank line.
     - Call `classifyFiles(result.filesGenerated)`, then `formatSummaryTable(counts)`, then print the table.
     - Print blank line.
     - Print `"Output: {result.outputDir}"`.
     - For each warning in `result.warnings`, print `"Warning: {warning}"`.
  2. JSDoc: document when it throws, output format, and console usage.
- **Dependencies on other tasks:** T2.2, T2.3 (needs classification and formatting)
- **Estimated complexity:** S

### Compilation checkpoint G2

```
npx tsc --noEmit   # zero errors -- cli-display.ts fully implemented
```

---

## G3 -- CLI Commands (Expand src/cli.ts)

**Purpose:** Expand the existing `src/cli.ts` stub with `generate` and `validate` subcommands using commander. Wire options, validation, and action handlers that call into existing modules (`loadConfig`, `runPipeline`, `runInteractive`, `validateStack`). Import `displayResult` from `src/cli-display.ts`.
**Dependencies:** G2 (needs `displayResult` from `cli-display.ts`), plus existing modules from STORY-004/016/017
**Compiles independently:** Yes

### T3.1 -- Implement internal helper functions

- **File:** `src/cli.ts` (modify)
- **What to implement:**
  1. Add imports:
     - `existsSync` from `node:fs`
     - `loadConfig` from `./config.js`
     - `runPipeline` from `./assembler/pipeline.js`
     - `runInteractive` from `./interactive.js`
     - `findResourcesDir`, `setupLogging` from `./utils.js`
     - `validateStack` from `./domain/validator.js`
     - `CliError`, `ConfigValidationError`, `PipelineError` from `./exceptions.js`
     - `ProjectConfig`, `PipelineResult` from `./models.js`
     - `displayResult` from `./cli-display.js`
  2. Implement `validateGenerateOptions(configPath, interactive)`:
     - If both `configPath` and `interactive` are set, throw `CliError("Cannot use --config and --interactive together", "MUTUAL_EXCLUSIVE")`.
     - If neither is set, throw `CliError("Must specify --config or --interactive", "MISSING_INPUT")`.
  3. Implement `async loadProjectConfig(configPath, interactive)`:
     - If `configPath` is defined, validate file exists with `existsSync(configPath)`, throw `CliError` if missing.
     - Return `loadConfig(configPath)` or `await runInteractive()`.
  4. Implement `resolveResourcesDir(resourcesDir)`:
     - If `resourcesDir` is defined, validate it exists with `existsSync`, throw `CliError` if missing.
     - Otherwise call `findResourcesDir()`.
     - Return the resolved path.
  5. Implement `async executeGenerate(config, resourcesDir, outputDir, dryRun)`:
     - Call `await runPipeline(config, resourcesDir, outputDir, dryRun)`.
     - Call `displayResult(result)`.
     - Return `result`.
- **Dependencies on other tasks:** G2 (displayResult)
- **Estimated complexity:** M

### T3.2 -- Implement `generate` subcommand

- **File:** `src/cli.ts` (modify)
- **What to implement:**
  1. Inside `createCli()`, add `.command("generate")` with:
     - `.description("Generate .claude/ and .github/ boilerplate")`
     - `.option("-c, --config <path>", "Path to YAML config file")`
     - `.option("-i, --interactive", "Run in interactive mode")`
     - `.option("-o, --output-dir <dir>", "Output directory", ".")`
     - `.option("-s, --resources-dir <dir>", "Resources templates directory")`
     - `.option("-v, --verbose", "Enable verbose logging")`
     - `.option("--dry-run", "Show what would be generated without writing")`
  2. Add async `.action()` handler:
     - Extract options: `configPath`, `interactive`, `outputDir`, `resourcesDir`, `verbose`, `dryRun`.
     - If `verbose`, call `setupLogging(true)`.
     - Call `validateGenerateOptions(configPath, interactive)`.
     - Call `loadProjectConfig(configPath, interactive)`.
     - Call `resolveResourcesDir(resourcesDir)`.
     - Call `executeGenerate(config, resolvedResourcesDir, outputDir, dryRun ?? false)`.
  3. Error handling in the action: wrap in try/catch for `ConfigValidationError`, `PipelineError`, `CliError` -- print friendly message via `console.error` and call `process.exit(1)`. For unknown errors, if `verbose` print stack trace, else print generic message.
- **Dependencies on other tasks:** T3.1
- **Estimated complexity:** M

### T3.3 -- Implement `validate` subcommand

- **File:** `src/cli.ts` (modify)
- **What to implement:**
  1. Inside `createCli()`, add `.command("validate")` with:
     - `.description("Validate a config file without generating output")`
     - `.requiredOption("-c, --config <path>", "Path to YAML config file")`
     - `.option("-v, --verbose", "Enable verbose logging")`
  2. Add async `.action()` handler:
     - Extract `configPath` and `verbose`.
     - If `verbose`, call `setupLogging(true)`.
     - Validate file exists with `existsSync(configPath)`, throw `CliError` if missing.
     - Call `loadConfig(configPath)`.
     - Call `validateStack(config)`.
     - If errors array is non-empty: print each error, call `process.exit(1)`.
     - If valid: print `"Config is valid."`.
  3. Error handling: same pattern as generate (try/catch for known error types).
- **Dependencies on other tasks:** T3.1 (shared imports and patterns)
- **Estimated complexity:** M

### T3.4 -- Update `PROGRAM_DESCRIPTION`

- **File:** `src/cli.ts` (modify)
- **What to implement:**
  1. Update the description from `"ia-dev-environment CLI foundation (STORY-001)."` to `"CLI tool that generates .claude/ and .github/ boilerplate for AI-assisted development environments"` (matches project identity).
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Compilation checkpoint G3

```
npx tsc --noEmit   # zero errors -- full CLI with generate and validate
```

---

## G4 -- Error Handling (Update src/index.ts)

**Purpose:** Expand the `main()` catch block in `src/index.ts` to handle `ConfigValidationError` and `PipelineError` with friendly messages, in addition to the existing `CliError` handling. This serves as a last-resort safety net for errors not caught by the command action handlers.
**Dependencies:** G3 (CLI commands must exist and may throw these errors)
**Compiles independently:** Yes

### T4.1 -- Expand error handler in `main()`

- **File:** `src/index.ts` (modify)
- **What to implement:**
  1. Add `ConfigValidationError` and `PipelineError` to the import from `./exceptions.js`.
  2. Expand the catch block in `main()` with this hierarchy:
     - `CliError` -> print `error.message`
     - `ConfigValidationError` -> print `error.message`
     - `PipelineError` -> print `error.message`
     - Other `Error` -> print `GENERIC_ERROR_MESSAGE`
  3. All error cases set `process.exitCode = 1`.
  4. Keep the existing structure; minimal changes.
- **Dependencies on other tasks:** G3
- **Estimated complexity:** S

### Compilation checkpoint G4

```
npx tsc --noEmit   # zero errors -- updated error handling
```

---

## G5 -- Unit Tests (cli-display.ts)

**Purpose:** Test all four exported functions in `cli-display.ts`: `classifyFiles`, `isKnowledgePackFile`, `formatSummaryTable`, and `displayResult`. Uses constructed path arrays, temp files for KP detection, and console spies for output assertions.
**Dependencies:** G2 (source must compile)
**Test file:** `tests/node/cli-display.test.ts` (create)

### T5.1 -- Test `isKnowledgePackFile`

- **File:** `tests/node/cli-display.test.ts` (create)
- **What to implement:**
  1. `isKnowledgePackFile_userInvocableFalse_returnsTrue` -- Create temp `SKILL.md` with `user-invocable: false` in YAML frontmatter. Verify returns `true`.
  2. `isKnowledgePackFile_knowledgePackHeading_returnsTrue` -- Create temp `SKILL.md` starting with `# Knowledge Pack`. Verify returns `true`.
  3. `isKnowledgePackFile_regularSkill_returnsFalse` -- Create temp `SKILL.md` with normal skill content (no KP markers). Verify returns `false`.
  4. `isKnowledgePackFile_nonExistentFile_returnsFalse` -- Pass a path that does not exist. Verify returns `false`.
  5. `isKnowledgePackFile_nonSkillMdLooksUpParent_returnsTrue` -- Create temp dir with `SKILL.md` (KP content) and a sibling file. Pass the sibling file path. Verify returns `true` (looks up SKILL.md in parent).
  6. `isKnowledgePackFile_noSkillMdInParent_returnsFalse` -- Pass a file path in a directory with no `SKILL.md`. Verify returns `false`.
- **Test infrastructure:** Use `mkdtempSync` for temp directories, clean up in `afterEach`.
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** M

### T5.2 -- Test `classifyFiles`

- **File:** `tests/node/cli-display.test.ts` (modify)
- **What to implement:**
  1. `classifyFiles_rulesPath_countsAsRules` -- Pass `["/out/.claude/rules/01-foo.md"]`. Verify `rules === 1`.
  2. `classifyFiles_skillsPath_countsAsSkills` -- Pass skills path (mock `isKnowledgePackFile` to return `false`). Verify `skills === 1`.
  3. `classifyFiles_knowledgePackPath_countsAsKnowledgePacks` -- Pass skills path with `isKnowledgePackFile` returning `true`. Verify `knowledgePacks === 1`.
  4. `classifyFiles_agentsPath_countsAsAgents` -- Pass `["/out/.claude/agents/foo.md"]`. Verify `agents === 1`.
  5. `classifyFiles_hooksPath_countsAsHooks` -- Pass `["/out/.claude/hooks/post.sh"]`. Verify `hooks === 1`.
  6. `classifyFiles_settingsFile_countsAsSettings` -- Pass `["/out/.claude/settings.json"]`. Verify `settings === 1`.
  7. `classifyFiles_readmeFile_countsAsReadme` -- Pass `["/out/.claude/README.md"]`. Verify `readme === 1`.
  8. `classifyFiles_githubPath_countsAsGithub` -- Pass `["/out/.github/instructions/foo.md"]`. Verify `github === 1`.
  9. `classifyFiles_githubSkillsPath_countsAsGithub_notSkills` -- Pass `["/out/.github/skills/bar/SKILL.md"]`. Verify `github === 1` and `skills === 0` (github takes priority).
  10. `classifyFiles_emptyArray_returnsAllZeros` -- Pass `[]`. Verify all counts are 0.
  11. `classifyFiles_unknownPath_notCounted` -- Pass a path matching no category. Verify all counts remain 0.
  12. `classifyFiles_mixedPaths_correctCounts` -- Pass multiple paths of different categories. Verify each count is correct.
- **Dependencies on other tasks:** T2.2
- **Estimated complexity:** M

### T5.3 -- Test `formatSummaryTable`

- **File:** `tests/node/cli-display.test.ts` (modify)
- **What to implement:**
  1. `formatSummaryTable_allCategories_formatsCorrectly` -- Pass counts with all categories > 0. Verify output contains all category labels and correct counts.
  2. `formatSummaryTable_onlyNonZero_displaysSubset` -- Pass counts with some zeros. Verify zero categories are omitted from output.
  3. `formatSummaryTable_allZeros_showsOnlyHeaderAndTotal` -- Pass all-zero counts. Verify only header, separator, and total row appear.
  4. `formatSummaryTable_totalIsCorrect` -- Verify the total row sums all categories.
  5. `formatSummaryTable_alignmentIsCorrect` -- Verify separator uses `\u2500` and count column is right-aligned.
- **Dependencies on other tasks:** T2.3
- **Estimated complexity:** S

### T5.4 -- Test `displayResult`

- **File:** `tests/node/cli-display.test.ts` (modify)
- **What to implement:**
  1. `displayResult_success_printsStatusAndTable` -- Create a `PipelineResult` with `success: true`. Spy on `console.log`. Verify output includes `"Pipeline: Success"` and output directory.
  2. `displayResult_withWarnings_printsEachWarning` -- Create a `PipelineResult` with warnings. Verify each warning printed with `"Warning: "` prefix.
  3. `displayResult_notSuccess_throwsError` -- Create a `PipelineResult` with `success: false`. Verify it throws an error.
- **Dependencies on other tasks:** T2.4
- **Estimated complexity:** S

### Test execution checkpoint G5

```
npx vitest run tests/node/cli-display.test.ts
```

---

## G6 -- Integration Tests (CLI end-to-end: parse -> execute -> output)

**Purpose:** Test the CLI commands end-to-end by parsing constructed argv arrays through commander, with mocked dependencies (`runPipeline`, `runInteractive`, `loadConfig`). Validates option parsing, mutual exclusivity, error handling, and output. Uses commander's `.exitOverride()` to prevent `process.exit()` in tests.
**Dependencies:** G3 (CLI commands must be implemented), G5 (display tests should pass first)
**Test file:** `tests/node/cli.test.ts` (create)

### T6.1 -- Test `generate` command option parsing and validation

- **File:** `tests/node/cli.test.ts` (create)
- **What to implement:**
  1. Mock `loadConfig`, `runPipeline`, `runInteractive`, `findResourcesDir`, `setupLogging`, `displayResult`.
  2. `generate_withConfig_loadConfigCalled` -- Parse `["node", "test", "generate", "--config", "path.yaml"]`. Verify `loadConfig` called with `"path.yaml"`.
  3. `generate_withInteractive_runInteractiveCalled` -- Parse `["node", "test", "generate", "--interactive"]`. Verify `runInteractive` called.
  4. `generate_bothConfigAndInteractive_throwsError` -- Parse with both `--config` and `--interactive`. Verify error about mutual exclusivity.
  5. `generate_neitherConfigNorInteractive_throwsError` -- Parse `["node", "test", "generate"]` with neither option. Verify error.
  6. `generate_withVerbose_enablesLogging` -- Parse with `--verbose`. Verify `setupLogging(true)` called.
  7. `generate_withDryRun_passedToPipeline` -- Parse with `--dry-run`. Verify `runPipeline` called with `dryRun: true`.
  8. `generate_withResourcesDir_usesProvidedPath` -- Parse with `--resources-dir /custom`. Verify path passed to pipeline.
  9. `generate_withoutResourcesDir_autoDetects` -- Parse without `--resources-dir`. Verify `findResourcesDir` called.
  10. `generate_outputDirDefault_usesDot` -- Parse without `--output-dir`. Verify pipeline called with `"."`.
  11. `generate_customOutputDir_passedToPipeline` -- Parse with `--output-dir /custom`. Verify pipeline called with `"/custom"`.
  12. `generate_configValidationError_friendlyMessage` -- Mock `loadConfig` to throw `ConfigValidationError`. Verify friendly error output.
  13. `generate_pipelineError_friendlyMessage` -- Mock `runPipeline` to throw `PipelineError`. Verify friendly error output.
- **Mocking strategy:** Use `vi.mock()` for all external modules. Use commander's `.exitOverride()` to prevent `process.exit()` in tests.
- **Dependencies on other tasks:** T3.2
- **Estimated complexity:** L

### T6.2 -- Test `validate` command

- **File:** `tests/node/cli.test.ts` (modify)
- **What to implement:**
  1. `validate_validConfig_printsValid` -- Mock `loadConfig` to return valid config, `validateStack` to return `[]`. Verify `"Config is valid."` printed.
  2. `validate_invalidStack_printsErrors` -- Mock `validateStack` to return `["Missing language"]`. Verify error printed and exit code 1.
  3. `validate_configParseError_friendlyMessage` -- Mock `loadConfig` to throw `ConfigParseError`. Verify friendly error output.
  4. `validate_configValidationError_friendlyMessage` -- Mock `loadConfig` to throw `ConfigValidationError`. Verify friendly error output.
  5. `validate_withVerbose_enablesLogging` -- Parse with `--verbose`. Verify `setupLogging(true)` called.
  6. `validate_missingConfigOption_showsError` -- Parse `["node", "test", "validate"]` without `--config`. Verify commander shows error (required option).
- **Dependencies on other tasks:** T3.3
- **Estimated complexity:** M

### T6.3 -- Test `index.ts` error handling

- **File:** `tests/node/cli.test.ts` (modify) or `tests/node/index-bootstrap.test.ts` (modify)
- **What to implement:**
  1. `main_cliError_printsFriendlyMessage` -- Verify `CliError` triggers friendly console.error output.
  2. `main_configValidationError_printsFriendlyMessage` -- Verify `ConfigValidationError` triggers friendly output.
  3. `main_pipelineError_printsFriendlyMessage` -- Verify `PipelineError` triggers friendly output.
  4. `main_genericError_printsGenericMessage` -- Verify unknown errors show `GENERIC_ERROR_MESSAGE`.
  5. `main_setsExitCodeToOne` -- Verify `process.exitCode` is set to `1` on error.
- **Dependencies on other tasks:** T4.1
- **Estimated complexity:** M

### T6.4 -- Test `--help` and `--version`

- **File:** `tests/node/cli.test.ts` (modify)
- **What to implement:**
  1. `cli_help_showsAvailableCommands` -- Parse `["node", "test", "--help"]`. Verify output includes `"generate"` and `"validate"`.
  2. `cli_version_showsVersion` -- Parse `["node", "test", "--version"]`. Verify output includes the version string.
  3. `cli_generateHelp_showsAllOptions` -- Parse `["node", "test", "generate", "--help"]`. Verify output includes `--config`, `--interactive`, `--output-dir`, `--resources-dir`, `--verbose`, `--dry-run`.
  4. `cli_validateHelp_showsAllOptions` -- Parse `["node", "test", "validate", "--help"]`. Verify output includes `--config`, `--verbose`.
- **Dependencies on other tasks:** T3.2, T3.3
- **Estimated complexity:** S

### Test execution checkpoint G6

```
npx vitest run tests/node/cli.test.ts
```

---

## G7 -- Cleanup (JSDoc, Help Text, Final Verification)

**Purpose:** Final pass for documentation, alignment, and verification. Ensure all exported functions have JSDoc, help text is consistent, compilation is clean, and coverage thresholds are met.
**Dependencies:** G1 through G6 all complete.

### T7.1 -- JSDoc on all exported functions and types

- **Files:** `src/cli-display.ts`, `src/cli.ts`
- **What to implement:**
  1. Verify every exported function has a JSDoc block with:
     - `@param` for each parameter
     - `@returns` describing return value
     - `@throws` if applicable
  2. Verify the `ComponentCounts` interface has a JSDoc description.
  3. Verify `createCli()` and `runCli()` JSDoc is up to date with the new subcommands.
- **Dependencies on other tasks:** G2, G3
- **Estimated complexity:** S

### T7.2 -- Help text alignment review

- **Files:** `src/cli.ts`
- **What to implement:**
  1. Verify `generate` and `validate` subcommand descriptions are concise and consistent.
  2. Verify all option descriptions start with a capital letter and are under 80 characters.
  3. Verify the program description matches the project identity.
- **Dependencies on other tasks:** T3.2, T3.3, T3.4
- **Estimated complexity:** S

### T7.3 -- Full compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors across the entire project.
- **Dependencies on other tasks:** G4

### T7.4 -- Run all new tests

- **Command:** `npx vitest run tests/node/cli-display.test.ts tests/node/cli.test.ts`
- **Expected:** All tests pass.
- **Dependencies on other tasks:** G5, G6

### T7.5 -- Coverage verification

- **Command:** `npx vitest run --coverage tests/node/cli-display.test.ts tests/node/cli.test.ts`
- **Expected:** >= 95% line coverage, >= 90% branch coverage on `src/cli-display.ts` and `src/cli.ts`.
- **Coverage strategy:**
  - `isKnowledgePackFile`: all 6 branches (non-existent file, SKILL.md direct, parent lookup found, parent lookup not found, KP markers present, KP markers absent)
  - `classifyFiles`: all 8 category branches + github-priority-over-skills branch + unknown path + empty array
  - `formatSummaryTable`: all-nonzero, some-zero, all-zero, alignment
  - `displayResult`: success path, warnings, failure path
  - `validateGenerateOptions`: both set, neither set, only config, only interactive
  - `loadProjectConfig`: config path, interactive path, missing file path
  - `resolveResourcesDir`: explicit path, auto-detect, missing explicit path
  - `generate` action: all option combinations, all error types
  - `validate` action: valid config, invalid stack, parse error, validation error
  - `index.ts` catch: all 4 error type branches
- **Dependencies on other tasks:** G5, G6

### T7.6 -- Run full project test suite (regression)

- **Command:** `npx vitest run`
- **Expected:** All existing tests continue to pass. No regressions.
- **Dependencies on other tasks:** T7.4

### T7.7 -- Smoke tests (manual verification)

- **Commands:**
  ```bash
  npx tsx src/index.ts --help
  npx tsx src/index.ts --version
  npx tsx src/index.ts generate --help
  npx tsx src/index.ts validate --help
  ```
- **Expected:** Help text displays correctly with all subcommands and options. Version matches `0.1.0`.
- **Dependencies on other tasks:** T7.3

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Test Cases | Complexity |
|-------|---------|----------------|----------------|-------|------------|------------|
| G1 | Types + stubs | 1 (`cli-display.ts`) | 0 | 1 | 0 | S |
| G2 | Display module | 0 | 1 (`cli-display.ts`) | 4 | 0 | M |
| G3 | CLI commands | 0 | 1 (`cli.ts`) | 4 | 0 | M |
| G4 | Error handling | 0 | 1 (`index.ts`) | 1 | 0 | S |
| G5 | Unit tests (display) | 1 (`cli-display.test.ts`) | 0 | 4 | ~26 | M |
| G6 | Integration tests (CLI) | 1 (`cli.test.ts`) | 0 | 4 | ~28 | L |
| G7 | Cleanup + verification | 0 | 2 (`cli-display.ts`, `cli.ts`) | 7 | 0 (verification) | S |
| **Total** | | **3 new files** | **3 modified** | **25 tasks** | **~54 test cases** | |

## Dependency Graph

```
G1: FOUNDATION (ComponentCounts type, file scaffold) -- no dependencies
  |
  v
G2: DISPLAY MODULE (classifyFiles, isKnowledgePackFile, formatSummaryTable, displayResult) -- depends on G1
  |
  +----> G5: UNIT TESTS (display functions) -- depends on G2
  |
  v
G3: CLI COMMANDS (generate, validate subcommands) -- depends on G2
  |
  +----> G6: INTEGRATION TESTS (CLI end-to-end) -- depends on G3, G5
  |
  v
G4: ERROR HANDLING (index.ts catch block) -- depends on G3
  |
  v
G7: CLEANUP + VERIFICATION (JSDoc, help text, compilation, coverage) -- depends on ALL
```

- G1 and G2 are sequential (G2 fills in the stubs created by G1).
- G5 can start as soon as G2 compiles (does not need G3 or G4).
- G3 depends on G2 (needs `displayResult`).
- G6 depends on G3 (CLI must exist) and G5 (display tests should pass first).
- G4 depends on G3 (error types from CLI commands).
- G7 depends on all groups.

## File Inventory

### Source files (1 new, 2 modified)

| File | Action | Content |
|------|--------|---------|
| `src/cli-display.ts` | Create | `ComponentCounts` type, `isKnowledgePackFile()`, `classifyFiles()`, `formatSummaryTable()`, `displayResult()` |
| `src/cli.ts` | Modify | Add `generate` and `validate` subcommands, internal helpers (`validateGenerateOptions`, `loadProjectConfig`, `resolveResourcesDir`, `executeGenerate`), update description |
| `src/index.ts` | Modify | Add `ConfigValidationError` and `PipelineError` to catch hierarchy |

### Test files (2 new)

| File | Action | Content |
|------|--------|---------|
| `tests/node/cli-display.test.ts` | Create | ~26 test cases: isKnowledgePackFile (6), classifyFiles (12), formatSummaryTable (5), displayResult (3) |
| `tests/node/cli.test.ts` | Create | ~28 test cases: generate command (13), validate command (6), index.ts errors (5), help/version (4) |

## Key Implementation Notes

1. **Display module split:** Python keeps all display logic in `__main__.py`. TypeScript splits it into `cli-display.ts` for testability and to keep `cli.ts` under the 250-line limit. `formatSummaryTable` returns a string (not printing directly) for SRP and testability.

2. **Commander async actions:** Use `program.parseAsync()` (already in `runCli`) which properly awaits async actions. Action handlers have their own try/catch for known error types.

3. **Mutual exclusivity:** Commander v12 has `.conflicts()` but manual validation in `validateGenerateOptions` provides clearer error messages (matching the Python approach).

4. **Path validation:** Click's `type=click.Path(exists=True)` validates at parse time. Commander does not -- manual `existsSync()` checks are needed in `loadProjectConfig` and `resolveResourcesDir`.

5. **`isKnowledgePackFile` filesystem safety:** Must handle non-existent files gracefully (return `false`) because in dry-run mode, generated files are written to a temp directory that is deleted before `classifyFiles` is called. The function checks `existsSync()` first.

6. **Test isolation:** CLI tests use `vi.mock()` for all external modules (`loadConfig`, `runPipeline`, `runInteractive`) to avoid real file I/O. Display tests for `isKnowledgePackFile` use real temp files. Commander's `.exitOverride()` prevents `process.exit()` in tests.

7. **Error hierarchy in `index.ts`:** The catch block checks `instanceof` in this order: `CliError`, `ConfigValidationError`, `PipelineError`, then generic `Error`. Most errors are caught by command action handlers; the `index.ts` catch is a safety net.

8. **`PipelineResult` already exists:** The `PipelineResult` class in `src/models.ts` has all required fields (`success`, `outputDir`, `filesGenerated`, `warnings`, `durationMs`). No model changes required.
