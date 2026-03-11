# Tech Lead Review — STORY-018

## Decision: CONDITIONAL GO
## Score: 35.5/40

## Rubric Details

### A. Code Hygiene (7/8)

- A1. No unused imports: 1 — All imports in `cli-display.ts`, `cli.ts`, and `index.ts` are used.
- A2. No unused variables: 1 — No unused variables detected.
- A3. No dead code: 1 — No unreachable code or dead branches.
- A4. No compiler warnings: 1 — `npx tsc --noEmit` passes cleanly.
- A5. Method signatures are minimal: 1 — Signatures expose only necessary parameters; `cache` in `isKnowledgePackFile` is optional and well-motivated.
- A6. No magic numbers/strings: 1 — Named constants throughout: `SKILL_MD_FILENAME`, `KP_MARKER_INVOCABLE`, `KP_MARKER_HEADING`, `SEPARATOR_CHAR`, `HEADER_LABEL`, `HEADER_COUNT`, `TOTAL_LABEL`, `PROGRAM_NAME`, `MUTUAL_EXCLUSIVE_MSG`, etc.
- A7. No commented-out code: 1 — Clean; no commented-out code in source files.
- A8. Clean import organization: 0 — `cli.ts` imports `type { ProjectConfig, PipelineResult }` and `{ DEFAULT_FOUNDATION }` from `./models.js` on separate lines (lines 21-22). While functional, the convention prefers consolidated imports from the same module. Minor, but deduction applies as the pattern appears inconsistent with the rest of the codebase.

### B. Naming (4/4)

- B1. Intention-revealing names: 1 — `classifyFiles`, `isKnowledgePackFile`, `formatSummaryTable`, `displayResult`, `validateGenerateOptions`, `handleKnownError` — all clearly convey purpose.
- B2. No disinformation: 1 — Names accurately describe behavior.
- B3. Meaningful distinctions: 1 — `ComponentCounts` vs `GenerateOptions` vs `ValidateOptions` are distinct and meaningful. `CATEGORY_LABELS` clearly maps display labels to keys.
- B4. Searchable names: 1 — All constants are named and searchable. No single-letter variables except in lambda/destructuring contexts (`[label, key]`).

### C. Functions (3.5/5)

- C1. Single responsibility: 1 — Functions are well-decomposed: `resolveSkillMdPath`, `isKnowledgePackContent`, `classifySingleFile`, `computeTotal`, `computeLabelWidth` are focused helpers.
- C2. Size <= 25 lines: 0 — `formatSummaryTable` is 28 lines (227-254). `createCli` is 39 lines (194-232). Both exceed the hard limit.
- C3. Max 4 parameters: 1 — Maximum is 4 params (`executeGenerate`: config, resourcesDir, outputDir, dryRun).
- C4. No boolean flag parameters: 1 — No boolean flags as function parameters in source code. `interactive` and `verbose` are option fields in typed interfaces, not direct boolean params.
- C5. Command-query separation: 0.5 — `displayResult` both classifies files (query) and prints output (command) in one function. While the plan specified this design, it slightly violates CQS. The `formatSummaryTable` returning a string rather than printing is a correct CQS separation.

### D. Vertical Formatting (3/4)

- D1. Blank lines between concepts: 1 — Consistent blank lines between functions, between import groups, and between logical blocks within functions.
- D2. Newspaper rule: 1 — High-level exports (`classifyFiles`, `formatSummaryTable`, `displayResult`) appear after private helpers that they depend on. In `cli.ts`, helper functions precede `createCli` and `runCli`.
- D3. Class/module size <= 250 lines: 0 — `cli-display.ts` is 281 lines total, exceeding the 250-line limit.
- D4. Related code is grouped: 1 — Constants grouped at top, SKILL.md resolution helpers grouped together, classification functions grouped, formatting functions grouped.

### E. Design (3/3)

- E1. Law of Demeter: 1 — No train wrecks. Access is direct: `result.success`, `result.filesGenerated`, `counts[key]`. The deepest chain is `segments[segments.length - 1]` which is appropriate for array access.
- E2. DRY: 1 — `CATEGORY_LABELS` array avoids duplicating label-key mappings. `handleKnownError` centralizes error handling for both commands. KP cache avoids redundant reads.
- E3. CQS respected: 1 — `formatSummaryTable` returns string without side effects. `classifyFiles` returns counts without side effects. `displayResult` is the only mixed function (acceptable as a top-level orchestrator).

### F. Error Handling (3/3)

- F1. Rich exceptions with context: 1 — `CliError` carries message and code (`"MUTUAL_EXCLUSIVE"`, `"MISSING_INPUT"`, `"CONFIG_NOT_FOUND"`). `ConfigValidationError` carries section names. `PipelineError` carries assembler name and detail.
- F2. No null returns: 1 — `resolveSkillMdPath` returns `string | undefined` (not null). All other functions return concrete values.
- F3. No generic catch-all without re-throw: 1 — `handleKnownError` discriminates known error types before falling through to generic handling. The `catch` in `handleGenerate`/`handleValidate` delegates to `handleKnownError` which always calls `process.exit(1)` (typed as `never`).

### G. Architecture (5/5)

- G1. SRP at module level: 1 — `cli-display.ts` handles display/formatting, `cli.ts` handles command wiring, `index.ts` handles bootstrap. Clear separation.
- G2. DIP: 1 — `runCli` accepts injectable `Pick<Command, "parseAsync">` for testing. Dependencies are imported as modules (not instantiated internally), enabling mock injection via `vi.mock`.
- G3. Layer boundaries respected: 1 — CLI layer depends on application modules (config, pipeline, interactive, utils) and domain (validator). No reverse dependencies.
- G4. Follows implementation plan: 1 — All functions from the plan are implemented: `classifyFiles`, `isKnowledgePackFile`, `formatSummaryTable`, `displayResult`, `validateGenerateOptions`, `loadProjectConfig`, `resolveResourcesDir`, `executeGenerate`, `createCli`, `runCli`. Both `generate` and `validate` commands match the plan's option specs.
- G5. No circular dependencies: 1 — `cli-display.ts` depends only on `models.ts` and `node:fs/path`. `cli.ts` depends on `cli-display.ts`, `config.ts`, pipeline, interactive, utils, validator, exceptions, models. `index.ts` depends on `cli.ts` and exceptions. No cycles.

### H. Framework & Infra (4/4)

- H1. Commander used idiomatically: 1 — `.command()`, `.option()`, `.requiredOption()`, `.action()`, `.parseAsync()` all used correctly. `.version()` wired to `DEFAULT_FOUNDATION.version`.
- H2. Externalized configuration: 1 — No hardcoded configuration values. All paths come from CLI options or auto-detection. Version from `DEFAULT_FOUNDATION`.
- H3. No hardcoded paths: 1 — Default output dir is `"."` (current directory). Resources dir auto-detected or user-supplied. Config path is user-supplied.
- H4. CLI help text matches spec: 1 — `generate` has 6 options (--config, --interactive, --output-dir, --resources-dir, --verbose, --dry-run). `validate` has 2 options (--config required, --verbose). Help text verified by tests.

### I. Tests (2.5/3)

- I1. Coverage >= 95% line / 90% branch: 1 — `cli-display.ts`: 97.46% stmts, 94.44% branch. `cli.ts`: 96.77% stmts, 97.5% branch. `index.ts`: 97.5% stmts, 91.66% branch. All above thresholds.
- I2. All acceptance criteria covered: 1 — Both commands tested with all option combinations. Error paths for `ConfigValidationError`, `PipelineError`, `ConfigParseError`, `CliError` all tested. Mutual exclusivity, missing input, config not found, resources not found, verbose, dry-run, help, version all covered. 39 tests in `cli-display.test.ts` + 35 tests in `cli.test.ts` = 74 total.
- I3. Test quality: 0.5 — Test naming follows `[method]_[scenario]_[expected]` convention. AAA pattern observed. No test interdependency. However: (a) `main_pipelineError_printsFriendlyMessage` test name implies asserting on friendly message output but actually asserts on rejection propagation; (b) the `index.ts` error handling tests use conditional assertions (`if (process.exitCode === 1)`) that may silently pass without verifying anything.

### J. Security & Production (1/1)

- J1. No sensitive data in output: 1 — Error messages use generic text for unknown errors (`"Command failed. Run with --help for usage."`). Stack traces only shown when `--verbose` is explicitly enabled. No credentials or secrets in output. `process.exit(1)` prevents further execution after errors.

## Critical Issues

None.

## Medium Issues

1. **`cli-display.ts` exceeds 250-line file limit (281 lines).** Rule 03 mandates <= 250 lines per file. Consider extracting the knowledge pack detection functions (`resolveSkillMdPath`, `isKnowledgePackContent`, `isKnowledgePackFile`) into a dedicated `cli-kp-detect.ts` module, or moving the constants and `CATEGORY_LABELS` mapping to a separate constants file.

2. **`formatSummaryTable` exceeds 25-line function limit (28 lines).** Extract the header/footer construction into a small helper (e.g., `buildTableBoundary`) to bring the function under the limit.

3. **`createCli` exceeds 25-line function limit (39 lines).** Extract the `generate` and `validate` subcommand definitions into private helper functions (e.g., `addGenerateCommand(program)` and `addValidateCommand(program)`).

## Low Issues

1. **`cli.ts` lines 21-22: Split imports from same module.** `type { ProjectConfig, PipelineResult }` and `{ DEFAULT_FOUNDATION }` are imported separately from `./models.js`. Consolidate into a single import statement.

2. **`main_pipelineError_printsFriendlyMessage` test name is misleading.** The test asserts on rejection propagation rather than verifying the friendly message was printed to `console.error`. Rename or fix the assertion to match the stated behavior.

3. **Conditional assertion in `main_configValidationError_printsFriendlyMessage`.** The `if (process.exitCode === 1)` guard means the `expect` inside it may never execute, making the test vacuously pass. Use unconditional assertions.

4. **Pre-existing test failures in `cli-help.test.ts`.** Two tests fail due to `TemplateEngine` constructor signature changes from a prior story. Not caused by STORY-018, but should be tracked for cleanup.

5. **`displayResult` mixes query and command.** It calls `classifyFiles` (query) and `console.log` (command) in the same function. Acceptable for a top-level orchestrator but noted for awareness.

## Summary

STORY-018 delivers a well-structured CLI entry point with clean separation between display (`cli-display.ts`), command wiring (`cli.ts`), and bootstrap (`index.ts`). The code follows the implementation plan faithfully, uses commander idiomatically, and achieves strong test coverage (97%+ statements, 91%+ branches across all three files). The main issues are the file size (281 lines in `cli-display.ts`) and two functions exceeding the 25-line limit (`formatSummaryTable` at 28 lines, `createCli` at 39 lines). These are straightforward refactors that do not require rearchitecting. Conditional GO pending those extractions.
