# Implementation Plan — STORY-018: CLI Entry Point

## Story Summary

Migrate the Python CLI entry point (`__main__.py`, 209 lines) to TypeScript by expanding the existing stubs `src/cli.ts` and `src/index.ts`. The CLI uses commander (replacing Python click) and exposes two subcommands: `generate` and `validate`.

**Commands:**
- `ia-dev-env generate` — generates .claude/ and .github/ boilerplate from config or interactive mode
- `ia-dev-env validate` — validates a config file without generating output

**Blocked by:** STORY-016 (pipeline orchestrator) — complete.
**Blocks:** None (final integration story).

---

## 1. Affected Layers and Components

| Layer | Component | Action | Path |
|-------|-----------|--------|------|
| cli | CLI command definitions | **Modify** | `src/cli.ts` |
| cli | Entry point / bootstrap | **Modify** | `src/index.ts` |
| cli | Result display and file classification | **Create** | `src/cli-display.ts` |
| config | loadConfig | Read-only | `src/config.ts` |
| assembler | runPipeline | Read-only | `src/assembler/pipeline.ts` |
| interactive | runInteractive | Read-only | `src/interactive.ts` |
| utils | findResourcesDir, setupLogging | Read-only | `src/utils.ts` |
| domain | validateStack | Read-only | `src/domain/validator.ts` |
| exceptions | ConfigValidationError, PipelineError, CliError | Read-only | `src/exceptions.ts` |
| models | PipelineResult, ProjectConfig | Read-only | `src/models.ts` |
| tests | CLI tests | **Create** | `tests/node/cli.test.ts` |
| tests | CLI display tests | **Create** | `tests/node/cli-display.test.ts` |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/cli-display.ts` — Result Display and File Classification

**Purpose:** Extract display logic (summary table, file classification) into a dedicated module. This keeps `src/cli.ts` focused on command wiring and keeps display functions independently testable. The Python equivalent is `_display_result`, `_classify_files`, `_display_summary_table`, and `_is_knowledge_pack_file` (lines 106-187 of `__main__.py`).

#### Exported Types

```typescript
/** Category counts for the summary table. */
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

#### Exported Functions

```typescript
/**
 * Classify generated file paths into component categories.
 * Categorizes by path segments (github, rules, skills, agents, hooks)
 * and file name patterns (README, settings).
 * For skills, reads SKILL.md to detect knowledge packs
 * (via "user-invocable: false" or "# Knowledge Pack" heading).
 */
export function classifyFiles(
  filePaths: readonly string[],
): ComponentCounts

/**
 * Check whether a skill file belongs to a knowledge pack.
 * If the file is not SKILL.md itself, looks for SKILL.md in the same directory.
 * Returns true if SKILL.md contains "user-invocable: false" or starts with "# Knowledge Pack".
 */
export function isKnowledgePackFile(filePath: string): boolean

/**
 * Format the summary table string for terminal output.
 * Only displays categories with count > 0.
 * Includes header, separator lines, and total row.
 */
export function formatSummaryTable(counts: ComponentCounts): string

/**
 * Display the full pipeline result: success line, summary table,
 * output directory, and warnings.
 * Uses console.log for output.
 */
export function displayResult(result: PipelineResult): void
```

#### Key Logic Details

**`classifyFiles(filePaths)`:**
Mirrors Python `_classify_files` (line 119). Iterates each path, splits into segments:
1. If any segment is `"github"` → GitHub
2. If filename contains `"README"` → README
3. If filename contains `"settings"` → Settings
4. If any segment is `"hooks"` → Hooks
5. If any segment is `"agents"` → Agents
6. If any segment is `"skills"` → check `isKnowledgePackFile`; if true → Knowledge Packs, else → Skills
7. If any segment is `"rules"` → Rules

Order matters: github check comes first (a github file might also be in a `skills/` subdirectory).

**`isKnowledgePackFile(filePath)`:**
Mirrors Python `_is_knowledge_pack_file` (line 154):
1. If file does not exist on disk, return false
2. If filename is not `SKILL.md`, look for `SKILL.md` in the parent directory
3. If `SKILL.md` not found, return false
4. Read `SKILL.md` content
5. Return true if content contains `"user-invocable: false"` OR content (left-trimmed) starts with `"# Knowledge Pack"`

**`formatSummaryTable(counts)`:**
Mirrors Python `_display_summary_table` (line 172):
1. Compute total from all categories
2. Determine label column width (max of all label lengths and "Component")
3. Build header row: `Component  Files`
4. Build separator row using `─` (U+2500)
5. For each category with count > 0, build a row with right-aligned count
6. Build footer separator and total row

**`displayResult(result)`:**
Mirrors Python `_display_result` (line 106):
1. If `!result.success`, throw error (this case should not occur in practice since pipeline throws on failure)
2. Print `Pipeline: Success ({durationMs}ms)`
3. Print blank line
4. Call `classifyFiles(result.filesGenerated)`, then `formatSummaryTable(counts)`, print it
5. Print `Output: {result.outputDir}`
6. For each warning, print `Warning: {warning}`

### 2.2 `src/cli.ts` — Expanded CLI Commands (Modify existing)

**Current state:** Minimal stub with `createCli()` returning a Command with only name/description/version.

**New structure:** Two subcommands wired with options and action handlers.

#### Exported Functions

```typescript
/**
 * Create the CLI program with generate and validate subcommands.
 * Returns a configured commander Command ready for parsing.
 */
export function createCli(): Command

/**
 * Parse argv and execute the matched command.
 * Delegates to createCli() by default; accepts an injectable
 * command for testing.
 */
export async function runCli(
  argv: readonly string[],
  cli?: Pick<Command, "parseAsync">,
): Promise<void>
```

#### Command: `generate`

```
ia-dev-env generate [options]

Options:
  -c, --config <path>        Path to YAML config file
  -i, --interactive          Run in interactive mode
  -o, --output-dir <dir>     Output directory (default: ".")
  -s, --resources-dir <dir>  Resources templates directory
  -v, --verbose              Enable verbose logging
  --dry-run                  Show what would be generated without writing
```

**Action handler flow:**
1. Validate mutual exclusivity: `--config` and `--interactive` cannot both be specified
2. Validate that at least one is specified: error if neither `--config` nor `--interactive`
3. If `--verbose`, call `setupLogging(true)`
4. Load config: if `--config` → `loadConfig(configPath)`, if `--interactive` → `await runInteractive()`
5. Resolve resources dir: if `--resources-dir` → use it, else → `findResourcesDir()`
6. Execute pipeline: `await runPipeline(config, resourcesDir, outputDir, dryRun)`
7. Display result: `displayResult(result)`

**Error handling:**
- `ConfigValidationError` → friendly message + `process.exit(1)`
- `PipelineError` → friendly message + `process.exit(1)`
- Generic error → stack trace if `--verbose`, generic message otherwise

#### Command: `validate`

```
ia-dev-env validate [options]

Options:
  -c, --config <path>  Path to YAML config file (required)
  -v, --verbose        Enable verbose logging
```

**Action handler flow:**
1. If `--verbose`, call `setupLogging(true)`
2. Load config: `loadConfig(configPath)`
3. Validate stack: `validateStack(config)`
4. If errors: join with newline, print, `process.exit(1)`
5. If valid: print `Config is valid.`

**Error handling:**
- `ConfigValidationError` → friendly message + `process.exit(1)`

#### Internal (non-exported) Functions within `src/cli.ts`

```typescript
/** Validate that --config and --interactive are mutually exclusive. */
function validateGenerateOptions(
  configPath: string | undefined,
  interactive: boolean,
): void

/** Load project config from file path or interactive mode. */
async function loadProjectConfig(
  configPath: string | undefined,
  interactive: boolean,
): Promise<ProjectConfig>

/** Resolve resources directory from explicit path or auto-detection. */
function resolveResourcesDir(
  resourcesDir: string | undefined,
): string

/** Run the generate pipeline with error handling. */
async function executeGenerate(
  config: ProjectConfig,
  resourcesDir: string,
  outputDir: string,
  dryRun: boolean,
): Promise<PipelineResult>
```

### 2.3 `src/index.ts` — Entry Point (Modify existing)

**Current state:** Has `bootstrap()`, `shouldRunAsCli()`, error handler catching `CliError`.

**Changes:**
- Expand the error handler in `main()` to also handle `ConfigValidationError` and `PipelineError` with friendly messages
- Add verbose-aware stack trace printing for generic errors (read `--verbose` state or use a simple flag)
- The existing structure with `shouldRunAsCli()` detection remains unchanged

**Note:** Most error handling will actually live in the command action handlers inside `cli.ts` using commander's `.exitOverride()` or direct `process.exit()`. The `main()` catch in `index.ts` serves as a last-resort safety net.

### 2.4 `tests/node/cli-display.test.ts` — Display Logic Tests

Test file for `classifyFiles`, `isKnowledgePackFile`, `formatSummaryTable`, and `displayResult`.

### 2.5 `tests/node/cli.test.ts` — CLI Command Tests

Test file for the CLI commands. Tests command parsing, option validation, and integration with the pipeline. Uses commander's `.exitOverride()` to prevent `process.exit()` in tests.

---

## 3. Existing Classes to Modify

### 3.1 `src/cli.ts` — Expand from stub to full CLI

**Current state (9 lines):** Creates a bare `Command` with name, description, version.

**Changes:**
1. Import dependencies: `loadConfig`, `runPipeline`, `runInteractive`, `findResourcesDir`, `setupLogging`, `validateStack`, `ConfigValidationError`, `PipelineError`, `CliError`
2. Add `generate` subcommand with 6 options and async action handler
3. Add `validate` subcommand with 2 options and async action handler
4. Add internal helper functions: `validateGenerateOptions`, `loadProjectConfig`, `resolveResourcesDir`, `executeGenerate`
5. Import and use `displayResult` from `src/cli-display.ts`

**Estimated size:** ~120 lines (well under the 250-line limit)

### 3.2 `src/index.ts` — Enhance error handling

**Current state (43 lines):** Catches `CliError` in `main()`, prints generic message for others.

**Changes:**
1. Add `ConfigValidationError` and `PipelineError` to the import from `./exceptions.js`
2. Expand the `catch` block to detect `ConfigValidationError` and `PipelineError` and print friendly messages
3. The error hierarchy: `CliError` → message, `ConfigValidationError` → message, `PipelineError` → message, other → generic message

**Estimated size:** ~50 lines (minimal growth)

---

## 4. Dependency Direction Validation

```
cli.ts ──imports──> config.ts (loadConfig)
       ──imports──> assembler/pipeline.ts (runPipeline)
       ──imports──> interactive.ts (runInteractive)
       ──imports──> utils.ts (findResourcesDir, setupLogging)
       ──imports──> domain/validator.ts (validateStack)
       ──imports──> exceptions.ts (ConfigValidationError, PipelineError, CliError)
       ──imports──> models.ts (ProjectConfig, PipelineResult)
       ──imports──> cli-display.ts (displayResult)
       ──imports──> commander (framework)

cli-display.ts ──imports──> models.ts (PipelineResult)
               ──imports──> node:fs, node:path (standard library)

index.ts ──imports──> cli.ts (runCli)
         ──imports──> exceptions.ts (CliError, ConfigValidationError, PipelineError)
         ──imports──> node:fs, node:path, node:url (standard library)
```

**Validated:**
- CLI layer depends on application modules (config, pipeline, interactive, utils) — correct for a CLI adapter
- `cli-display.ts` depends only on `models.ts` and standard library — no circular deps
- No domain code imports CLI code
- No circular dependencies introduced
- The `cli.ts` → `assembler/pipeline.ts` dependency is acceptable: CLI is the outermost adapter, pipeline is application-level orchestration

---

## 5. Integration Points

### 5.1 commander Framework

The CLI uses commander v12+ with the following patterns:
- `program.command("generate")` — defines subcommand
- `.option("-c, --config <path>", ...)` — defines option with value
- `.option("-i, --interactive", ...)` — defines boolean flag
- `.action(async (options) => {...})` — async action handler
- `.requiredOption()` — for validate's `--config`
- Commander handles `--help` and `--version` automatically

**Python → TypeScript mapping:**
| Python (click) | TypeScript (commander) |
|----------------|----------------------|
| `@click.group()` | `new Command()` |
| `@main.command()` | `program.command("name")` |
| `@click.option("--flag", is_flag=True)` | `.option("-f, --flag")` |
| `@click.option("--opt", type=click.Path(exists=True))` | `.option("-o, --opt <path>")` (validation manual) |
| `click.UsageError(...)` | `commander.error(...)` or throw `CliError` |
| `click.ClickException(...)` | Console.error + process.exit(1) |
| `click.echo(...)` | `console.log(...)` |

### 5.2 loadConfig (from `src/config.ts`)

Signature: `loadConfig(path: string): ProjectConfig`
- Reads YAML, handles v2 → v3 migration, validates required sections
- Throws `ConfigParseError` on YAML parse failure
- Throws `ConfigValidationError` on missing required sections

### 5.3 runPipeline (from `src/assembler/pipeline.ts`)

Signature: `runPipeline(config, resourcesDir, outputDir, dryRun): Promise<PipelineResult>`
- Async — must be awaited
- Throws `PipelineError` on assembler failure

### 5.4 runInteractive (from `src/interactive.ts`)

Signature: `runInteractive(): Promise<ProjectConfig>`
- Async — uses inquirer for interactive prompts
- Returns a fully-built `ProjectConfig`

### 5.5 validateStack (from `src/domain/validator.ts`)

Signature: `validateStack(config: ProjectConfig): string[]`
- Synchronous
- Returns array of error messages (empty = valid)

### 5.6 findResourcesDir (from `src/utils.ts`)

Signature: `findResourcesDir(metaUrl?: string): string`
- Synchronous
- Throws `Error` if resources directory not found

### 5.7 setupLogging (from `src/utils.ts`)

Signature: `setupLogging(verbose: boolean): void`
- Synchronous
- Suppresses `console.debug` when verbose is false

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

No new environment variables or configuration fields. The CLI consumes existing `ProjectConfig` via YAML files.

The CLI options map to the following existing infrastructure:
- `--config` → `loadConfig(path)` (existing)
- `--interactive` → `runInteractive()` (existing)
- `--output-dir` → passed to `runPipeline` (existing)
- `--resources-dir` → passed to `runPipeline` or `findResourcesDir()` (existing)
- `--verbose` → `setupLogging(verbose)` (existing)
- `--dry-run` → passed to `runPipeline` as boolean (existing)

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Commander async error handling: commander does not propagate errors from async `.action()` by default | High | High | Use `program.parseAsync()` (already in `runCli`) which properly awaits async actions. Wrap action body in try/catch for known errors. |
| `--config` path validation (click had `exists=True`) | Medium | Medium | Commander does not validate file existence. Add manual `existsSync(configPath)` check before calling `loadConfig`. Throw `CliError` if missing. |
| Mutual exclusivity not built into commander | Medium | Medium | Commander v12 has `.conflicts()` API for options. Use `configOption.conflicts("interactive")`. Alternatively, validate manually in the action handler (matching Python approach). Prefer manual validation for clearer error messages. |
| `process.exit()` in tests prevents test runner completion | High | High | Use commander's `.exitOverride()` in tests so `process.exit()` throws an exception instead. In production, let normal exit behavior apply. |
| `isKnowledgePackFile` reads files during classification — files may not exist in dry-run | Medium | Medium | In dry-run mode, files are written to a temp dir that is deleted. The `classifyFiles` function is called with paths that may no longer exist. `isKnowledgePackFile` must handle non-existent files gracefully (return false). Already handled: Python checks `file_path.is_file()` first. |
| Interactive mode requires TTY | Low | Low | `inquirer` requires a TTY for prompts. If stdin is not a TTY and `--interactive` is used, inquirer will fail. Add a guard: check `process.stdin.isTTY` before entering interactive mode. |
| `formatSummaryTable` output format differs from Python | Low | Medium | Snapshot-test the exact output format. The Python uses `click.echo` with f-string formatting; TypeScript uses `console.log`. Verify alignment with fixed-width formatting. |
| CLI tests may be flaky with real file I/O | Medium | Medium | Use temp directories for config files. Mock `runPipeline` and `runInteractive` in command tests. Test `classifyFiles` with constructed path arrays (no real files needed except for KP detection). |

---

## 11. Implementation Groups (Execution Order)

### G1: Display and classification functions (pure logic, independently testable)

**File:** `src/cli-display.ts`

**Functions:**
- `classifyFiles`
- `isKnowledgePackFile`
- `formatSummaryTable`
- `displayResult`

**Test scenarios:**
1. `classifyFiles_rulesPath_countsAsRules`
2. `classifyFiles_skillsPath_countsAsSkills`
3. `classifyFiles_knowledgePackPath_countsAsKnowledgePacks`
4. `classifyFiles_agentsPath_countsAsAgents`
5. `classifyFiles_hooksPath_countsAsHooks`
6. `classifyFiles_settingsFile_countsAsSettings`
7. `classifyFiles_readmeFile_countsAsReadme`
8. `classifyFiles_githubPath_countsAsGithub`
9. `classifyFiles_githubSkillsPath_countsAsGithub_notSkills`
10. `classifyFiles_emptyArray_returnsAllZeros`
11. `classifyFiles_unknownPath_notCounted`
12. `classifyFiles_mixedPaths_correctCounts`
13. `isKnowledgePackFile_userInvocableFalse_returnsTrue`
14. `isKnowledgePackFile_knowledgePackHeading_returnsTrue`
15. `isKnowledgePackFile_regularSkill_returnsFalse`
16. `isKnowledgePackFile_nonExistentFile_returnsFalse`
17. `isKnowledgePackFile_nonSkillMdLooksUpParent_returnsTrue`
18. `isKnowledgePackFile_noSkillMdInParent_returnsFalse`
19. `formatSummaryTable_allCategories_formatsCorrectly`
20. `formatSummaryTable_onlyNonZero_displaysSubset`
21. `formatSummaryTable_allZeros_showsOnlyHeaderAndTotal`
22. `formatSummaryTable_totalIsCorrect`
23. `formatSummaryTable_alignmentIsCorrect`
24. `displayResult_success_printsStatusAndTable`
25. `displayResult_withWarnings_printsEachWarning`
26. `displayResult_notSuccess_throwsError`

### G2: Generate command (depends on G1 + existing modules)

**File:** `src/cli.ts`

**Functions:**
- `createCli` (expanded with generate subcommand)
- `validateGenerateOptions`
- `loadProjectConfig`
- `resolveResourcesDir`
- `executeGenerate`

**Test scenarios:**
1. `validateGenerateOptions_bothConfigAndInteractive_throwsError`
2. `validateGenerateOptions_neitherConfigNorInteractive_throwsError`
3. `validateGenerateOptions_onlyConfig_noError`
4. `validateGenerateOptions_onlyInteractive_noError`
5. `generate_withConfig_loadConfigCalled`
6. `generate_withInteractive_runInteractiveCalled`
7. `generate_withResourcesDir_usesProvidedPath`
8. `generate_withoutResourcesDir_autoDetects`
9. `generate_withVerbose_enablesLogging`
10. `generate_withDryRun_passedToPipeline`
11. `generate_configValidationError_friendlyMessage`
12. `generate_pipelineError_friendlyMessage`
13. `generate_outputDirDefault_usesDot`
14. `generate_customOutputDir_passedToPipeline`
15. `resolveResourcesDir_explicitPath_returnsPath`
16. `resolveResourcesDir_undefined_autoDetects`
17. `resolveResourcesDir_autoDetectFails_throwsCliError`

### G3: Validate command (depends on existing modules)

**File:** `src/cli.ts`

**Functions:**
- `createCli` (add validate subcommand)

**Test scenarios:**
1. `validate_validConfig_printsValid`
2. `validate_invalidStack_printsErrors`
3. `validate_configParseError_friendlyMessage`
4. `validate_configValidationError_friendlyMessage`
5. `validate_withVerbose_enablesLogging`
6. `validate_missingConfigOption_showsError`

### G4: Error handling in index.ts (depends on G2, G3)

**File:** `src/index.ts`

**Changes:**
- Expand catch block for `ConfigValidationError` and `PipelineError`

**Test scenarios:**
1. `main_cliError_printsFriendlyMessage`
2. `main_configValidationError_printsFriendlyMessage`
3. `main_pipelineError_printsFriendlyMessage`
4. `main_genericError_printsGenericMessage`
5. `main_setsExitCodeToOne`

### G5: Integration and compile check

- Run `npx tsc --noEmit` to verify compilation
- Run full test suite with coverage
- Manual smoke test: `npx tsx src/index.ts generate --help`
- Manual smoke test: `npx tsx src/index.ts validate --help`

---

## 12. Testing Strategy

### Test infrastructure

Tests use vitest (as per `package.json` scripts). Follow established patterns from `tests/node/cli-help.test.ts` and `tests/node/index-bootstrap.test.ts`.

### CLI command testing approach

Commander commands are tested by:
1. Creating the CLI with `createCli()`
2. Calling `.exitOverride()` to prevent `process.exit()`
3. Parsing constructed argv arrays: `cli.parseAsync(["node", "test", "generate", "--config", "path"])`
4. Asserting on side effects (mocked function calls, console output)

### Mocking strategy

```typescript
// Mock the pipeline to avoid running real assemblers
vi.mock("../src/assembler/pipeline.js", () => ({
  runPipeline: vi.fn().mockResolvedValue(
    new PipelineResult(true, ".", ["file1.md"], [], 42),
  ),
}));

// Mock interactive to avoid TTY requirement
vi.mock("../src/interactive.js", () => ({
  runInteractive: vi.fn().mockResolvedValue(buildTestConfig()),
}));

// Mock config loader for controlled testing
vi.mock("../src/config.js", () => ({
  loadConfig: vi.fn().mockReturnValue(buildTestConfig()),
}));
```

For `cli-display.ts` tests:
- `classifyFiles`: construct path arrays directly (e.g., `["/out/rules/01-foo.md", "/out/skills/bar/SKILL.md"]`)
- `isKnowledgePackFile`: use real temp files with known SKILL.md content
- `formatSummaryTable`: assert on returned string format
- `displayResult`: spy on `console.log` and assert output lines

### Coverage targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | All code paths: config/interactive, verbose on/off, dry-run on/off, all error types, all classification categories |
| Branch | >= 90% | config vs interactive, resourcesDir explicit vs auto, all `classifyFiles` category branches, KP detection branches, error type branches in catch |

---

## 13. File-by-File Mapping (Python to TypeScript)

| Python (`__main__.py`) | TypeScript | Notes |
|------------------------|-----------|-------|
| `main()` (click group) | `createCli()` in `src/cli.ts` | Commander `Command` replaces click group |
| `generate()` (click command) | `.command("generate")` in `createCli()` | Async action handler |
| `validate()` (click command) | `.command("validate")` in `createCli()` | Async action handler |
| `_validate_generate_options()` | `validateGenerateOptions()` in `src/cli.ts` | Internal function |
| `_load_project_config()` | `loadProjectConfig()` in `src/cli.ts` | Internal async function |
| `_resolve_resources_dir()` | `resolveResourcesDir()` in `src/cli.ts` | Internal function |
| `_execute_generate()` | `executeGenerate()` in `src/cli.ts` | Internal async function |
| `_display_result()` | `displayResult()` in `src/cli-display.ts` | Exported for testability |
| `_classify_files()` | `classifyFiles()` in `src/cli-display.ts` | Exported for testability |
| `_is_knowledge_pack_file()` | `isKnowledgePackFile()` in `src/cli-display.ts` | Exported for testability |
| `_display_summary_table()` | `formatSummaryTable()` in `src/cli-display.ts` | Returns string instead of printing directly |
| `click.echo(...)` | `console.log(...)` | Direct replacement |
| `click.UsageError(...)` | `CliError` or `commander.error(...)` | Commander has built-in error display |
| `click.ClickException(...)` | `console.error(msg)` + `process.exit(1)` | No click equivalent in commander |
| `@click.version_option(version=__version__)` | `.version(DEFAULT_FOUNDATION.version)` | Already in existing stub |
| `@click.option("--config", type=click.Path(exists=True))` | `.option("-c, --config <path>")` + manual validation | Commander does not validate file existence |
| `@click.option("--interactive", is_flag=True)` | `.option("-i, --interactive")` | Boolean flag (default false) |
| `@click.option("--output-dir", default=".")` | `.option("-o, --output-dir <dir>", ".", ".")` | Default "." |
| `@click.option("--resources-dir", type=click.Path(exists=True))` | `.option("-s, --resources-dir <dir>")` + manual validation | Optional |
| `@click.option("--verbose", is_flag=True)` | `.option("-v, --verbose")` | Boolean flag |
| `@click.option("--dry-run", is_flag=True)` | `.option("--dry-run")` | Boolean flag |

### Key differences from Python

1. **Framework:** click → commander. Click uses decorators; commander uses method chaining.
2. **Path validation:** Click's `type=click.Path(exists=True)` validates file existence at parse time. Commander does not — manual `existsSync()` check needed.
3. **Mutual exclusivity:** Click does not have built-in mutual exclusivity either (the Python code validates manually). Commander has `.conflicts()` but manual validation provides better error messages.
4. **Async actions:** Click commands are synchronous in Python (even though `run_pipeline` is sync there). Commander supports async actions via `.parseAsync()`.
5. **Error display:** Click formats errors nicely with `ClickException`. Commander prints errors to stderr. Custom error formatting needed.
6. **Display module split:** Python keeps all display logic in `__main__.py`. TypeScript splits it into `cli-display.ts` for testability and to keep `cli.ts` under 250 lines.
7. **Return vs print:** Python's `_display_summary_table` prints directly. TypeScript's `formatSummaryTable` returns a string (SRP, testability), and `displayResult` orchestrates printing.
8. **File paths:** Python uses `Path` objects throughout. TypeScript uses `string` paths split with `path.sep` or `/`.

---

## 14. Acceptance Criteria Checklist

From story requirements:

- [ ] `ia-dev-env generate --config <path>` loads config and runs pipeline
- [ ] `ia-dev-env generate --interactive` launches interactive mode
- [ ] `--config` and `--interactive` are mutually exclusive (error if both)
- [ ] Error if neither `--config` nor `--interactive` is provided
- [ ] `--output-dir` defaults to `"."`
- [ ] `--resources-dir` auto-detects if not specified
- [ ] `--verbose` enables debug logging
- [ ] `--dry-run` runs pipeline without writing files
- [ ] Result displays summary table with component categories
- [ ] `classifyFiles()` categorizes by path segments and file names
- [ ] Knowledge pack detection reads SKILL.md for `"user-invocable: false"` or `"# Knowledge Pack"`
- [ ] `ia-dev-env validate --config <path>` validates config without generating
- [ ] `validate` prints `"Config is valid."` on success
- [ ] `validate` prints error messages on invalid stack
- [ ] `ConfigValidationError` → friendly message + exit 1
- [ ] `PipelineError` → friendly message + exit 1
- [ ] Generic error → stack trace if verbose, generic message otherwise
- [ ] `ia-dev-env --help` shows available commands
- [ ] `ia-dev-env --version` shows version

From DoD:

- [ ] Coverage >= 95% line, >= 90% branch
- [ ] Unit + integration tests
- [ ] JSDoc on all exported functions and types
- [ ] Zero compiler warnings (`npx tsc --noEmit`)
- [ ] Output behavior matches Python CLI
