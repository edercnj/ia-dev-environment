# Task Decomposition -- STORY-016: Pipeline Orchestrator

**Status:** PENDING
**Date:** 2026-03-11
**Blocked By:** STORY-002 (utils/atomicOutput), STORY-009 through STORY-015 (all 14 assemblers)
**Blocks:** STORY-018

---

## G1 -- Foundation (Types and AssemblerDescriptor)

**Purpose:** Define the `AssemblerDescriptor` type that pairs a name with an assembler instance and its return type classification. This type is the foundation for `buildAssemblers()` and `executeAssemblers()`. Also define the internal `ExecutionResult` type used to normalize the two assembler return types (`string[]` and `AssembleResult`).
**Dependencies:** None
**Compiles independently:** Yes -- only adds new types.

### T1.1 -- Define `AssemblerDescriptor` interface

- **File:** `src/assembler/pipeline.ts` (create)
- **What to implement:**
  1. Import `ProjectConfig` from `../models.js`, `TemplateEngine` from `../template-engine.js`, and `AssembleResult` from `./rules-assembler.js`.
  2. Define `AssemblerDescriptor` interface:
     ```typescript
     interface AssemblerDescriptor {
       readonly name: string;
       readonly assembler: {
         assemble(
           config: ProjectConfig,
           outputDir: string,
           resourcesDir: string,
           engine: TemplateEngine,
         ): string[] | AssembleResult;
       };
     }
     ```
  3. This captures the uniform `assemble(config, outputDir, resourcesDir, engine)` signature shared by all 14 TS assemblers, with the union return type covering both `string[]` (10 assemblers) and `AssembleResult { files, warnings }` (4 assemblers: Rules, Agents, GithubAgents, GithubMcp).
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.2 -- Define result normalization helper

- **File:** `src/assembler/pipeline.ts` (modify)
- **What to implement:**
  1. Define `NormalizedResult` type: `{ files: string[]; warnings: string[] }`.
  2. Implement `normalizeResult(result: string[] | AssembleResult): NormalizedResult`:
     - If `result` is an array, return `{ files: result, warnings: [] }`.
     - Otherwise (it has `files` and `warnings` properties), return `{ files: result.files, warnings: result.warnings }`.
  3. Detection: use `Array.isArray(result)` for branching.
- **Dependencies on other tasks:** T1.1 (needs the types)
- **Estimated complexity:** S

### T1.3 -- Define `DRY_RUN_WARNING` constant

- **File:** `src/assembler/pipeline.ts` (modify)
- **What to implement:**
  1. Add `const DRY_RUN_WARNING = "Dry run -- no files written";` matching the Python constant exactly.
  2. Export it for test access.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Compilation checkpoint G1

```
npx tsc --noEmit   # zero errors -- new file with types only
```

---

## G2 -- Core (buildAssemblers)

**Purpose:** Implement `buildAssemblers()` that returns the fixed ordered list of 14 assembler descriptors per RULE-008. This is the registry that defines pipeline execution order.
**Dependencies:** G1 (needs `AssemblerDescriptor` type)
**Compiles independently:** Yes

### T2.1 -- Implement `buildAssemblers()` function

- **File:** `src/assembler/pipeline.ts` (modify)
- **What to implement:**
  1. Import all 14 assembler classes:
     - `RulesAssembler` from `./rules-assembler.js`
     - `SkillsAssembler` from `./skills-assembler.js`
     - `AgentsAssembler` from `./agents-assembler.js`
     - `PatternsAssembler` from `./patterns-assembler.js`
     - `ProtocolsAssembler` from `./protocols-assembler.js`
     - `HooksAssembler` from `./hooks-assembler.js`
     - `SettingsAssembler` from `./settings-assembler.js`
     - `GithubInstructionsAssembler` from `./github-instructions-assembler.js`
     - `GithubMcpAssembler` from `./github-mcp-assembler.js`
     - `GithubSkillsAssembler` from `./github-skills-assembler.js`
     - `GithubAgentsAssembler` from `./github-agents-assembler.js`
     - `GithubHooksAssembler` from `./github-hooks-assembler.js`
     - `GithubPromptsAssembler` from `./github-prompts-assembler.js`
     - `ReadmeAssembler` from `./readme-assembler.js`
  2. Implement `buildAssemblers(): readonly AssemblerDescriptor[]` returning 14 entries in RULE-008 order:
     ```
     1. RulesAssembler
     2. SkillsAssembler
     3. AgentsAssembler
     4. PatternsAssembler
     5. ProtocolsAssembler
     6. HooksAssembler
     7. SettingsAssembler
     8. GithubInstructionsAssembler
     9. GithubMcpAssembler
     10. GithubSkillsAssembler
     11. GithubAgentsAssembler
     12. GithubHooksAssembler
     13. GithubPromptsAssembler
     14. ReadmeAssembler
     ```
  3. Each descriptor: `{ name: "RulesAssembler", assembler: new RulesAssembler() }` etc.
  4. Export `buildAssemblers` for test access.
- **Key difference from Python:** In Python, some assemblers receive `resources_dir` in the constructor. In TS, all 14 assemblers are stateless and receive `resourcesDir` as a method parameter. Therefore `buildAssemblers()` takes no arguments (unlike the Python `_build_assemblers(resources_dir)`).
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### Compilation checkpoint G2

```
npx tsc --noEmit   # zero errors -- new function with assembler imports
```

---

## G3 -- Core (executeAssemblers)

**Purpose:** Implement `executeAssemblers()` that iterates the assembler list sequentially, calls each `assemble()` method, normalizes results, aggregates files and warnings, and wraps errors in `PipelineError`.
**Dependencies:** G1 (normalization), G2 (buildAssemblers)
**Compiles independently:** Yes

### T3.1 -- Implement `executeAssemblers()` function

- **File:** `src/assembler/pipeline.ts` (modify)
- **What to implement:**
  1. Import `PipelineError` from `../exceptions.js`.
  2. Implement:
     ```typescript
     export function executeAssemblers(
       assemblers: readonly AssemblerDescriptor[],
       config: ProjectConfig,
       outputDir: string,
       resourcesDir: string,
       engine: TemplateEngine,
     ): NormalizedResult
     ```
  3. Logic:
     - Initialize `files: string[]` and `warnings: string[]` accumulators.
     - Iterate assemblers sequentially.
     - For each assembler, call `assembler.assembler.assemble(config, outputDir, resourcesDir, engine)`.
     - Pass the result through `normalizeResult()`.
     - Push normalized `files` and `warnings` into accumulators.
     - On error: wrap in `PipelineError(name, error.message ?? String(error))` and rethrow.
  4. Return `{ files, warnings }`.
- **Key difference from Python:** TS has uniform `assemble(config, outputDir, resourcesDir, engine)` signature for all 14. No need for the Python `ASSEMBLERS_WITH_RESOURCES_DIR` special-case dispatch.
- **Dependencies on other tasks:** T1.1, T1.2
- **Estimated complexity:** M

### Compilation checkpoint G3

```
npx tsc --noEmit   # zero errors
```

---

## G4 -- Core (runPipeline, runReal, runDry)

**Purpose:** Implement the three orchestration functions that wire everything together: `runPipeline` (public entry point), `runReal` (atomic output mode), and `runDry` (temp dir, discard). Includes duration measurement via `performance.now()`.
**Dependencies:** G1, G2, G3
**Compiles independently:** Yes

### T4.1 -- Implement `runDry()` function

- **File:** `src/assembler/pipeline.ts` (modify)
- **What to implement:**
  1. Import `mkdtemp`, `rm` from `node:fs/promises`, `tmpdir` from `node:os`, `join` from `node:path`.
  2. Implement:
     ```typescript
     async function runDry(
       config: ProjectConfig,
       resourcesDir: string,
     ): Promise<NormalizedResult>
     ```
  3. Logic:
     - Create temp dir: `await mkdtemp(join(tmpdir(), "ia-dev-env-dry-"))`.
     - Create `TemplateEngine(resourcesDir, config)`.
     - Call `executeAssemblers(buildAssemblers(), config, tempDir, resourcesDir, engine)`.
     - In `finally` block: `await rm(tempDir, { recursive: true, force: true })`.
     - Append `DRY_RUN_WARNING` to warnings.
     - Return `{ files, warnings }`.
- **Dependencies on other tasks:** T1.3, T3.1
- **Estimated complexity:** M

### T4.2 -- Implement `runReal()` function

- **File:** `src/assembler/pipeline.ts` (modify)
- **What to implement:**
  1. Import `atomicOutput` from `../utils.js`.
  2. Implement:
     ```typescript
     async function runReal(
       config: ProjectConfig,
       resourcesDir: string,
       outputDir: string,
     ): Promise<NormalizedResult>
     ```
  3. Logic:
     - Use `atomicOutput(outputDir, async (tempDir) => { ... })`.
     - Inside callback: create `TemplateEngine(resourcesDir, config)`, call `executeAssemblers(buildAssemblers(), config, tempDir, resourcesDir, engine)`.
     - Return the `NormalizedResult` from the callback.
- **Dependencies on other tasks:** T3.1
- **Estimated complexity:** M

### T4.3 -- Implement `runPipeline()` public entry point

- **File:** `src/assembler/pipeline.ts` (modify)
- **What to implement:**
  1. Import `PipelineResult` from `../models.js`.
  2. Implement:
     ```typescript
     export async function runPipeline(
       config: ProjectConfig,
       resourcesDir: string,
       outputDir: string,
       dryRun: boolean,
     ): Promise<PipelineResult>
     ```
  3. Logic:
     - Record `start = performance.now()`.
     - Branch on `dryRun`: call `runDry(config, resourcesDir)` or `runReal(config, resourcesDir, outputDir)`.
     - Compute `durationMs = Math.round(performance.now() - start)`.
     - Return `new PipelineResult(true, outputDir, result.files, result.warnings, durationMs)`.
  4. `performance.now()` is available globally in Node.js (no import needed).
- **Dependencies on other tasks:** T4.1, T4.2
- **Estimated complexity:** M

### T4.4 -- Export pipeline from barrel

- **File:** `src/assembler/index.ts` (modify)
- **What to implement:**
  1. Add `export * from "./pipeline.js";` at the end of the barrel file.
  2. This exposes `runPipeline`, `buildAssemblers`, `executeAssemblers`, `DRY_RUN_WARNING`, and `AssemblerDescriptor` to consumers.
- **Dependencies on other tasks:** T4.3
- **Estimated complexity:** S

### Compilation checkpoint G4

```
npx tsc --noEmit   # zero errors -- full pipeline compiles
```

---

## G5 -- Unit Tests (buildAssemblers + executeAssemblers + normalizeResult)

**Purpose:** Test the assembler registry, result normalization, and sequential execution with mocked assemblers. These tests do not touch the filesystem.
**Dependencies:** G1, G2, G3 (source must compile)
**Test file:** `tests/node/assembler/pipeline.test.ts` (create)

### T5.1 -- Test `buildAssemblers` ordering and count

- **File:** `tests/node/assembler/pipeline.test.ts` (create)
- **What to implement:**
  1. `buildAssemblers_returns14Assemblers` -- Verify length is 14.
  2. `buildAssemblers_firstIsRulesAssembler` -- Verify `[0].name === "RulesAssembler"`.
  3. `buildAssemblers_lastIsReadmeAssembler` -- Verify `[13].name === "ReadmeAssembler"`.
  4. `buildAssemblers_orderMatchesRule008` -- Verify all 14 names in exact RULE-008 order.
  5. `buildAssemblers_eachHasAssembleMethod` -- Verify every descriptor's `assembler` has an `assemble` function.
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** S

### T5.2 -- Test `normalizeResult` for both return types

- **File:** `tests/node/assembler/pipeline.test.ts` (modify)
- **What to implement:**
  1. `normalizeResult_stringArray_returnsFilesAndEmptyWarnings` -- Pass `["a.md", "b.md"]`, verify `{ files: ["a.md", "b.md"], warnings: [] }`.
  2. `normalizeResult_assembleResult_returnsFilesAndWarnings` -- Pass `{ files: ["a.md"], warnings: ["warn"] }`, verify both propagated.
  3. `normalizeResult_emptyArray_returnsEmptyResult` -- Pass `[]`, verify `{ files: [], warnings: [] }`.
  4. `normalizeResult_assembleResultEmptyWarnings_returnsEmptyWarnings` -- Pass `{ files: ["a.md"], warnings: [] }`, verify warnings is `[]`.
- **Dependencies on other tasks:** T1.2
- **Estimated complexity:** S

### T5.3 -- Test `executeAssemblers` sequential execution and error wrapping

- **File:** `tests/node/assembler/pipeline.test.ts` (modify)
- **What to implement:**
  1. `executeAssemblers_callsAllAssemblersInOrder` -- Create 3 mock assembler descriptors with spied `assemble` methods. Verify all 3 called in order (using `callOrder` tracking).
  2. `executeAssemblers_aggregatesFilesFromMultipleAssemblers` -- Mock 2 assemblers returning `["a.md"]` and `["b.md"]`. Verify result has `files: ["a.md", "b.md"]`.
  3. `executeAssemblers_aggregatesWarningsFromAssembleResult` -- Mock assembler returning `{ files: ["a.md"], warnings: ["warn1"] }`. Verify warnings aggregated.
  4. `executeAssemblers_mixedReturnTypes_normalizesCorrectly` -- One returning `string[]`, another returning `AssembleResult`. Verify both normalized and merged.
  5. `executeAssemblers_assemblerThrows_wrapsPipelineError` -- Mock assembler that throws `Error("disk full")`. Verify `PipelineError` with correct `assemblerName` and `reason`.
  6. `executeAssemblers_emptyList_returnsEmptyResult` -- Pass empty array. Verify `{ files: [], warnings: [] }`.
- **Dependencies on other tasks:** T3.1
- **Estimated complexity:** M

### Test execution checkpoint G5

```
npx vitest run tests/node/assembler/pipeline.test.ts
```

---

## G6 -- Unit + Integration Tests (runPipeline, runReal, runDry)

**Purpose:** Test the three orchestration functions including dry-run behavior, atomic output integration, and duration measurement. Uses temp directories for isolation.
**Dependencies:** G4, G5
**Test file:** `tests/node/assembler/pipeline.test.ts` (modify, append new describe blocks)

### T6.1 -- Test `runDry` behavior

- **File:** `tests/node/assembler/pipeline.test.ts` (modify)
- **What to implement:**
  1. `runPipeline_dryRun_appendsDryRunWarning` -- Call `runPipeline(config, resourcesDir, outputDir, true)` with valid config and resources. Verify `result.warnings` contains `DRY_RUN_WARNING`.
  2. `runPipeline_dryRun_doesNotWriteToOutputDir` -- Verify `outputDir` is empty or unchanged after dry-run.
  3. `runPipeline_dryRun_returnsSuccessTrue` -- Verify `result.success === true`.
  4. `runPipeline_dryRun_returnsOutputDir` -- Verify `result.outputDir === outputDir` (the original, not the temp dir).
  5. `runPipeline_dryRun_cleansUpTempDir` -- After completion, no `ia-dev-env-dry-*` temp dirs remain (best-effort check).
- **Note:** These tests need real `resourcesDir` pointing to the project's `resources/` directory, or a minimal fixture. Consider using `findResourcesDir()` from `utils.ts` or creating a minimal fixture.
- **Dependencies on other tasks:** T4.1, T4.3
- **Estimated complexity:** M

### T6.2 -- Test `runReal` behavior

- **File:** `tests/node/assembler/pipeline.test.ts` (modify)
- **What to implement:**
  1. `runPipeline_real_writesFilesToOutputDir` -- Call with `dryRun=false`, verify output dir contains generated files.
  2. `runPipeline_real_returnsSuccessTrue` -- Verify `result.success === true`.
  3. `runPipeline_real_filesGeneratedMatchesOutputDirContents` -- Verify `result.filesGenerated.length > 0`.
  4. `runPipeline_real_atomicOutputProtectsOnFailure` -- If a mocked assembler throws mid-pipeline, verify output dir is not left in partial state. (May require mocking one assembler to fail.)
- **Dependencies on other tasks:** T4.2, T4.3
- **Estimated complexity:** M

### T6.3 -- Test duration measurement

- **File:** `tests/node/assembler/pipeline.test.ts` (modify)
- **What to implement:**
  1. `runPipeline_durationMs_isPositiveNumber` -- Verify `result.durationMs > 0`.
  2. `runPipeline_durationMs_isInteger` -- Verify `Number.isInteger(result.durationMs)` or at least `typeof result.durationMs === "number"`.
  3. `runPipeline_durationMs_reasonableRange` -- Verify `result.durationMs < 30000` (pipeline should not take 30s in tests).
- **Dependencies on other tasks:** T4.3
- **Estimated complexity:** S

### T6.4 -- Test warnings aggregation across assemblers

- **File:** `tests/node/assembler/pipeline.test.ts` (modify)
- **What to implement:**
  1. `runPipeline_aggregatesWarningsFromMultipleAssemblers` -- Use a config that triggers warnings (e.g., MCP servers with literal env values trigger `GithubMcpAssembler` warnings). Verify warnings from multiple assemblers appear in `result.warnings`.
  2. `runPipeline_noWarnings_returnsEmptyArray` -- Use config with no warning-triggering conditions (except dry-run if applicable). Verify warnings array is empty or contains only `DRY_RUN_WARNING`.
- **Dependencies on other tasks:** T4.3
- **Estimated complexity:** S

### Test execution checkpoint G6

```
npx vitest run tests/node/assembler/pipeline.test.ts
```

---

## G7 -- Verification (compilation, full test suite, coverage)

**Purpose:** Final verification that all code compiles cleanly, all tests pass, and coverage thresholds are met.
**Dependencies:** G1 through G6 all complete.

### T7.1 -- Full compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors across the entire project.
- **Dependencies on other tasks:** G4

### T7.2 -- Run all pipeline tests

- **Command:** `npx vitest run tests/node/assembler/pipeline.test.ts`
- **Expected:** All tests pass.
- **Dependencies on other tasks:** G5, G6

### T7.3 -- Coverage verification

- **Command:** `npx vitest run --coverage tests/node/assembler/pipeline.test.ts`
- **Expected:** >= 95% line coverage, >= 90% branch coverage on `src/assembler/pipeline.ts`.
- **Coverage strategy:**
  - `normalizeResult` exercised for both `string[]` and `AssembleResult` branches
  - `executeAssemblers` exercised for success (all 14), error wrapping, and empty list
  - `runDry` exercised: temp dir creation, cleanup, warning append
  - `runReal` exercised: `atomicOutput` callback, TemplateEngine creation
  - `runPipeline` exercised: `dryRun=true` and `dryRun=false` branches
  - `performance.now()` duration measurement exercised
- **Dependencies on other tasks:** G5, G6

### T7.4 -- Run full project test suite

- **Command:** `npx vitest run`
- **Expected:** All existing tests continue to pass. No regressions from adding `pipeline.ts` and its barrel export.
- **Dependencies on other tasks:** T7.2

### T7.5 -- Verify acceptance criteria (Gherkin)

- **What to verify:**
  1. Pipeline executes 14 assemblers in RULE-008 order (T5.1 covers this)
  2. Dry-run does not alter filesystem (T6.1 covers this)
  3. Atomic output protects against partial failure (T6.2 covers this)
  4. `durationMs` is a positive number (T6.3 covers this)
  5. Warnings aggregated from multiple assemblers (T6.4 covers this)
  6. `PipelineResult` contains all required fields: `success`, `outputDir`, `filesGenerated`, `warnings`, `durationMs`
- **Dependencies on other tasks:** G5, G6

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Test Cases | Complexity |
|-------|---------|----------------|----------------|-------|------------|------------|
| G1 | Types + normalization + constants | 1 (`pipeline.ts`) | 0 | 3 | 0 | S |
| G2 | `buildAssemblers()` registry | 0 | 1 (`pipeline.ts`) | 1 | 0 | S |
| G3 | `executeAssemblers()` orchestration | 0 | 1 (`pipeline.ts`) | 1 | 0 | M |
| G4 | `runPipeline` + `runReal` + `runDry` | 0 | 2 (`pipeline.ts`, `index.ts`) | 4 | 0 | M |
| G5 | Unit tests: registry + normalization + execution | 1 (`pipeline.test.ts`) | 0 | 3 | ~15 | M |
| G6 | Unit + integration tests: orchestration | 0 | 1 (`pipeline.test.ts`) | 4 | ~11 | M |
| G7 | Verification + coverage | 0 | 0 | 5 | 0 (verification) | S |
| **Total** | | **2 new files** | **3 modified** | **21 tasks** | **~26 test cases** | |

## Dependency Graph

```
G1: FOUNDATION (types, normalization, constants) -- no dependencies
  |
  v
G2: CORE (buildAssemblers) -- depends on G1
  |
  v
G3: CORE (executeAssemblers) -- depends on G1, G2
  |
  v
G4: ORCHESTRATION (runPipeline, runReal, runDry, barrel export) -- depends on G1, G2, G3
  |
  +----> G5: UNIT TESTS (registry, normalization, execution) -- depends on G1, G2, G3
  |        |
  +----> G6: INTEGRATION TESTS (orchestration, dry-run, atomic output) -- depends on G4, G5
             |
             v
           G7: VERIFICATION (compilation, coverage, acceptance) -- depends on ALL
```

- G1 through G4 are sequential (each builds on the previous).
- G5 can start as soon as G3 compiles (does not need G4).
- G6 depends on both G4 (runPipeline must exist) and G5 (test file must exist).
- G7 depends on all groups.

## File Inventory

### Source files (1 new, 1 modified)

| File | Action | Content |
|------|--------|---------|
| `src/assembler/pipeline.ts` | Create | `AssemblerDescriptor` type, `NormalizedResult` type, `normalizeResult()`, `DRY_RUN_WARNING`, `buildAssemblers()`, `executeAssemblers()`, `runDry()`, `runReal()`, `runPipeline()` |
| `src/assembler/index.ts` | Modify | Add `export * from "./pipeline.js";` barrel export |

### Test files (1 new)

| File | Action | Content |
|------|--------|---------|
| `tests/node/assembler/pipeline.test.ts` | Create | ~26 test cases across 6 describe blocks |

## Key Implementation Notes

1. **Uniform assembler signature (TS simplification):** Unlike Python where some assemblers receive `resources_dir` in their constructor and `_execute_assemblers` has special-case dispatch via `ASSEMBLERS_WITH_RESOURCES_DIR`, all 14 TS assemblers share the same `assemble(config, outputDir, resourcesDir, engine)` signature. This eliminates the need for any conditional dispatching in `executeAssemblers`.

2. **Two return types require normalization:** 10 assemblers return `string[]` (just files), 4 return `AssembleResult { files, warnings }` (RulesAssembler, AgentsAssembler, GithubAgentsAssembler, GithubMcpAssembler). The `normalizeResult()` helper handles this with `Array.isArray()`.

3. **`atomicOutput` is callback-based (not context manager):** The TS `atomicOutput<T>(destDir, callback)` in `src/utils.ts` uses a callback pattern (not Python's `with` statement). `runReal` must pass a `(tempDir) => Promise<NormalizedResult>` callback.

4. **Duration uses `performance.now()`:** Node.js provides `performance.now()` globally (from `perf_hooks`). Use `Math.round()` to convert to integer milliseconds, matching the Python `int()` behavior.

5. **`runDry` uses its own temp dir (not `atomicOutput`):** The dry-run path creates a temp dir manually, runs assemblers, then cleans up in a `finally` block. It does NOT use `atomicOutput` because there is no destination to atomically replace.

6. **Barrel export keeps `index.ts` clean:** The pipeline logic lives in `pipeline.ts`. The barrel `index.ts` only adds `export * from "./pipeline.js";` -- no implementation code in the barrel.

7. **Error wrapping:** `executeAssemblers` catches errors from individual assemblers and wraps them in `PipelineError(name, reason)` from `src/exceptions.ts`. The `reason` should be the original error's message.

8. **`PipelineResult` already exists:** The `PipelineResult` class in `src/models.ts` (line 468) has the exact fields needed: `success`, `outputDir`, `filesGenerated`, `warnings`, `durationMs`. No model changes required.
