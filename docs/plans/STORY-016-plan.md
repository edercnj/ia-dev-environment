# Implementation Plan — STORY-016: Pipeline Orchestrator

## Story Summary

Migrate the pipeline orchestrator from Python (`src/ia_dev_env/assembler/__init__.py`) to TypeScript (`src/assembler/index.ts`). The pipeline coordinates all 14 assemblers in a fixed order, manages atomic output, implements dry-run mode, measures execution duration, and aggregates results into `PipelineResult`.

**Blocked by:** STORY-002 (atomicOutput), STORY-009 through STORY-015 (all 14 assemblers) — all complete.
**Blocks:** STORY-018.

---

## 1. Affected Layers and Components

| Layer | Component | Action | Path |
|-------|-----------|--------|------|
| assembler | Pipeline orchestrator functions | **Create** | `src/assembler/pipeline.ts` |
| assembler | Barrel export + Assembler interface update | **Modify** | `src/assembler/index.ts` |
| models | PipelineResult | Read-only | `src/models.ts` |
| exceptions | PipelineError | Read-only | `src/exceptions.ts` |
| utils | atomicOutput | Read-only | `src/utils.ts` |
| template-engine | TemplateEngine | Read-only | `src/template-engine.ts` |
| tests | Pipeline orchestrator tests | **Create** | `tests/node/assembler/pipeline.test.ts` |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/assembler/pipeline.ts` — Pipeline Orchestrator

**Purpose:** Orchestrate all 14 assemblers in the correct order (RULE-008), with atomic output for real runs, temp-dir-and-discard for dry runs, timing, and result aggregation.

#### Types

```typescript
/**
 * Named assembler entry: associates a display name with its assembler instance
 * and a flag indicating whether it returns AssembleResult or string[].
 */
interface NamedAssembler {
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

#### Exported Functions

```typescript
/** Build the ordered list of 14 assemblers (RULE-008). */
export function buildAssemblers(): NamedAssembler[]

/**
 * Execute all assemblers sequentially, aggregating files and warnings.
 * Throws PipelineError if any assembler fails.
 */
export function executeAssemblers(
  assemblers: NamedAssembler[],
  config: ProjectConfig,
  outputDir: string,
  resourcesDir: string,
  engine: TemplateEngine,
): { files: string[]; warnings: string[] }

/**
 * Main entry point. Delegates to runReal or runDry based on dryRun flag.
 */
export function runPipeline(
  config: ProjectConfig,
  resourcesDir: string,
  outputDir: string,
  dryRun: boolean,
): Promise<PipelineResult>
```

#### Internal (non-exported) Functions

```typescript
/** Execute pipeline with atomic output to destination directory. */
async function runReal(
  config: ProjectConfig,
  resourcesDir: string,
  outputDir: string,
  startMs: number,
): Promise<PipelineResult>

/** Execute pipeline in a temp directory, discard output, append dry-run warning. */
async function runDry(
  config: ProjectConfig,
  resourcesDir: string,
  outputDir: string,
  startMs: number,
): Promise<PipelineResult>

/** Execute assemblers in a temporary directory, then clean up. */
async function runInTemp(
  config: ProjectConfig,
  resourcesDir: string,
): Promise<{ files: string[]; warnings: string[] }>

/** Compute elapsed time in milliseconds from two performance.now() timestamps. */
function computeDurationMs(startMs: number, endMs: number): number
```

#### Constants

```typescript
export const DRY_RUN_WARNING = "Dry run -- no files written";
```

#### Key Logic Details

**`buildAssemblers()`:**
Returns 14 `NamedAssembler` entries in the exact order specified by RULE-008:
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

Each assembler is instantiated with no constructor args (key difference from Python where some took `resources_dir` in constructor).

**`executeAssemblers()`:**
- Iterates assemblers sequentially via `for...of`.
- Calls `assembler.assemble(config, outputDir, resourcesDir, engine)` — uniform 4-arg signature (key difference from Python where some used 3 args, some 4).
- Normalizes return values: if result is an `AssembleResult` (has `files` and `warnings` properties), extracts both arrays. If result is `string[]`, treats it as files with no warnings.
- On any exception, wraps it in `PipelineError(name, reason)` where `reason` is `error.message` if present, otherwise `String(error)`, and rethrows.
- Returns aggregated `{ files, warnings }`.

**`runPipeline()`:**
- Captures `performance.now()` at start.
- Delegates to `runDry` or `runReal` based on `dryRun` flag.
- Returns `PipelineResult`.

**`runReal()`:**
- Uses `atomicOutput(outputDir, async (tempDir) => {...})` from `src/utils.ts`.
- Inside callback: creates `TemplateEngine(resourcesDir, config)`, calls `executeAssemblers`, returns `{ files, warnings }`.
- After atomicOutput resolves: computes duration, constructs `PipelineResult(true, outputDir, files, warnings, durationMs)`.

**`runDry()`:**
- Calls `runInTemp(config, resourcesDir)` which creates temp dir, runs assemblers, cleans up.
- Appends `DRY_RUN_WARNING` to warnings.
- Computes duration, constructs `PipelineResult(true, outputDir, files, warnings, durationMs)`.

**`runInTemp()`:**
- Creates temp dir with `fs.promises.mkdtemp(path.join(os.tmpdir(), "ia-dev-env-dry-"))`.
- Creates `TemplateEngine(resourcesDir, config)`.
- Calls `executeAssemblers(assemblers, config, tempDir, resourcesDir, engine)`.
- In `finally` block: removes temp dir with `fs.promises.rm(tempDir, { recursive: true, force: true })`.

**`computeDurationMs()`:**
- Returns `Math.round(endMs - startMs)` (performance.now() already in ms, unlike Python's seconds).

### 2.2 `tests/node/assembler/pipeline.test.ts`

Test file for the pipeline orchestrator.

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/index.ts` — Add barrel export and update Assembler interface

**Current state:** Contains an `Assembler` interface (unused, with `assemble(): Promise<void>`) and barrel exports for all assemblers.

**Changes:**
1. Remove or deprecate the existing `Assembler` interface — it does not match any real assembler signature. The `NamedAssembler` interface in `pipeline.ts` will serve as the canonical type.
2. Add barrel export:
```typescript
// --- STORY-016: Pipeline Orchestrator ---
export * from "./pipeline.js";
```

**Decision:** Keep the existing `Assembler` interface for now (it may be referenced externally), but do NOT use it in the pipeline. The pipeline uses its own `NamedAssembler` type which accurately models the actual assembler signatures.

---

## 4. Dependency Direction Validation

```
pipeline.ts ──imports──> models.ts (ProjectConfig, PipelineResult)
             ──imports──> exceptions.ts (PipelineError)
             ──imports──> utils.ts (atomicOutput)
             ──imports──> template-engine.ts (TemplateEngine)
             ──imports──> assembler/*.ts (14 assembler classes)
             ──imports──> node:fs/promises, node:os, node:path (standard library)
```

**Validated:**
- Pipeline depends on domain models, exceptions, utilities, and assemblers — all within the same architectural layer or lower.
- No circular dependencies: assemblers do NOT import from pipeline.
- No framework dependencies beyond standard library.
- Dependencies point inward (pipeline orchestrates assemblers; assemblers do not know about pipeline).

---

## 5. Integration Points

### 5.1 atomicOutput (from `src/utils.ts`)

The TypeScript `atomicOutput` uses a callback pattern:
```typescript
atomicOutput<T>(destDir: string, callback: (tempDir: string) => Promise<T>): Promise<T>
```

This replaces the Python context manager pattern:
```python
with atomic_output(output_dir) as temp_dir:
    ...
```

The pipeline must pass a callback that creates the engine, runs assemblers, and returns the aggregated result. The `atomicOutput` function handles:
- Creating temp dir
- Copying temp to dest (with backup/rollback)
- Cleaning up temp dir

### 5.2 TemplateEngine (from `src/template-engine.ts`)

Created per-run (not shared across runs). Constructor: `new TemplateEngine(resourcesDir, config)`.

### 5.3 PipelineResult (from `src/models.ts`)

Already exists with the expected shape:
```typescript
class PipelineResult {
  readonly success: boolean;
  readonly outputDir: string;
  readonly filesGenerated: readonly string[];
  readonly warnings: readonly string[];
  readonly durationMs: number;
}
```

### 5.4 PipelineError (from `src/exceptions.ts`)

Already exists:
```typescript
class PipelineError extends Error {
  readonly assemblerName: string;
  readonly reason: string;
}
```

### 5.5 AssembleResult (from `src/assembler/rules-assembler.ts`)

```typescript
interface AssembleResult {
  files: string[];
  warnings: string[];
}
```

Used by 4 assemblers. The pipeline must normalize this alongside the `string[]` return from the other 10 assemblers.

---

## 6. Database Changes

None. This story is pure orchestration logic.

---

## 7. API Changes

None. Internal module — no external API surface.

---

## 8. Event Changes

None. No event-driven components involved.

---

## 9. Configuration Changes

None. Uses existing `ProjectConfig` and `TemplateEngine`. No new environment variables or config fields.

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Return type normalization: AssembleResult vs string[] | Medium | High | Use duck-typing check: if result has `files` and `warnings` properties, treat as AssembleResult. Otherwise treat as string[]. Unit test with both return types. |
| Assembler order drift from Python | Low | High | Hardcode the exact 14-assembler order matching RULE-008 and the Python `_build_assemblers()`. Add a unit test that verifies the order by name. |
| performance.now() precision vs Python time.monotonic() | Low | Low | Both provide monotonic timing. performance.now() returns ms directly (not seconds). No conversion factor needed (unlike Python's `* 1000`). |
| atomicOutput callback vs Python context manager | Medium | Medium | The callback pattern naturally scopes the temp dir lifecycle. The callback must return the aggregated result so it can be used after atomicOutput resolves. |
| Dry-run temp dir cleanup on error | Low | Medium | Use try/finally pattern (matching Python). Even if assemblers throw, the temp dir is cleaned up. |
| PipelineError wrapping hides original stack trace | Low | Low | Include original error as `cause` in PipelineError constructor call (using `new PipelineError(name, reason)` — the existing class does not support `cause`. Consider using `Error.cause` if needed for debugging). |
| All assemblers are synchronous but pipeline is async | Low | Low | The pipeline is async due to atomicOutput (which uses async fs operations). Assemblers are synchronous — wrapping them in an async pipeline is fine. |
| Existing `Assembler` interface in index.ts does not match actual signatures | Low | Low | Do not use the existing interface. Create a new typed structure in pipeline.ts. The old interface can be removed in a future cleanup story. |
| File path types: assemblers return string[] with absolute paths from temp dir | Medium | Medium | In dry-run mode, paths reference the temp dir which is cleaned up. This matches Python behavior where dry-run returns paths to files that no longer exist. The CLI displays the path list regardless. |

---

## 11. Implementation Groups (Execution Order)

### G1: Core pipeline functions (pure logic)

**Functions:**
- `computeDurationMs`
- `buildAssemblers`

**Test scenarios:**
1. `computeDurationMs_positiveDelta_returnsRoundedMs`
2. `computeDurationMs_zeroDelta_returnsZero`
3. `buildAssemblers_returns14Entries`
4. `buildAssemblers_orderMatchesRule008`
5. `buildAssemblers_allNamesAreCorrect`
6. `buildAssemblers_allAssemblersHaveAssembleMethod`

### G2: Assembler execution (depends on G1)

**Functions:**
- `executeAssemblers`

**Test scenarios:**
1. `executeAssemblers_allSucceed_aggregatesFiles`
2. `executeAssemblers_assemblerReturnsAssembleResult_extractsFilesAndWarnings`
3. `executeAssemblers_assemblerReturnsStringArray_treatsAsFiles`
4. `executeAssemblers_mixedReturnTypes_aggregatesCorrectly`
5. `executeAssemblers_assemblerThrows_wrapsPipelineError`
6. `executeAssemblers_assemblerThrows_includesAssemblerName`
7. `executeAssemblers_emptyAssemblerList_returnsEmptyResult`
8. `executeAssemblers_executesInOrder`

### G3: Pipeline entry points (depends on G2)

**Functions:**
- `runPipeline`
- `runReal` (internal)
- `runDry` (internal)
- `runInTemp` (internal)

**Test scenarios:**
1. `runPipeline_realMode_usesAtomicOutput`
2. `runPipeline_realMode_returnsSuccessTrue`
3. `runPipeline_realMode_filesGeneratedContainsAllFiles`
4. `runPipeline_realMode_durationMsIsPositive`
5. `runPipeline_realMode_outputDirMatchesParam`
6. `runPipeline_dryRun_appendsDryRunWarning`
7. `runPipeline_dryRun_noFilesWrittenToOutputDir`
8. `runPipeline_dryRun_returnsSuccessTrue`
9. `runPipeline_dryRun_durationMsIsPositive`
10. `runPipeline_dryRun_cleansUpTempDir`
11. `runPipeline_assemblerFails_throwsPipelineError`
12. `runPipeline_assemblerFails_atomicOutputRollsBack`
13. `runPipeline_warningsFromMultipleAssemblers_aggregated`

### G4: Barrel export + compile check

- Add export to `src/assembler/index.ts`.
- Run `npx tsc --noEmit` to verify compilation.

---

## 12. Testing Strategy

### Test infrastructure

Tests follow the established pattern (see other assembler test files):
- `beforeEach`: create `tmpDir` with `fs.mkdtempSync`, set up `resourcesDir` and `outputDir`.
- `afterEach`: clean up with `fs.rmSync(tmpDir, { recursive: true, force: true })`.
- Mock assemblers for unit tests to avoid running all 14 real assemblers.

### Mocking strategy

For `executeAssemblers` unit tests, create stub assemblers:

```typescript
function createStubAssembler(
  files: string[],
  warnings?: string[],
): NamedAssembler["assembler"] {
  return {
    assemble: () => warnings
      ? { files, warnings }
      : files,
  };
}

function createFailingAssembler(error: Error): NamedAssembler["assembler"] {
  return {
    assemble: () => { throw error; },
  };
}
```

For `runPipeline` integration tests, use real assemblers with a minimal valid config and resources directory (from the project's own `resources/` directory).

### Coverage targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | All code paths: real/dry, success/failure, all return type variants |
| Branch | >= 90% | dryRun true/false, AssembleResult vs string[] normalization, error wrapping |

---

## 13. File-by-File Mapping (Python to TypeScript)

| Python | TypeScript | Notes |
|--------|-----------|-------|
| `_build_assemblers(resources_dir)` | `buildAssemblers()` | No `resourcesDir` param — TS assemblers take it per-call |
| `ASSEMBLERS_WITH_RESOURCES_DIR` | Not needed | All TS assemblers have uniform 4-arg signature |
| `_execute_assemblers(config, resources_dir, output_dir, engine)` | `executeAssemblers(assemblers, config, outputDir, resourcesDir, engine)` | Takes assembler list as param for testability |
| `_run_in_temp(config, resources_dir)` | `runInTemp(config, resourcesDir)` | Internal async function |
| `_compute_duration_ms(start, end)` | `computeDurationMs(startMs, endMs)` | No `* 1000` — performance.now() already in ms |
| `run_pipeline(config, resources_dir, output_dir, dry_run)` | `runPipeline(config, resourcesDir, outputDir, dryRun)` | Exported, async (due to atomicOutput) |
| `_run_dry(config, resources_dir, output_dir, start)` | `runDry(config, resourcesDir, outputDir, startMs)` | Internal async |
| `_run_real(config, resources_dir, output_dir, start)` | `runReal(config, resourcesDir, outputDir, startMs)` | Internal async |
| `DRY_RUN_WARNING` | `DRY_RUN_WARNING` | Same constant value |
| `MILLISECONDS_PER_SECOND = 1000` | Not needed | performance.now() returns ms directly |

### Key differences from Python

1. **Async vs sync:** TypeScript pipeline is async because `atomicOutput` is async. Python is synchronous.
2. **Uniform assembler signature:** All 14 TypeScript assemblers use `assemble(config, outputDir, resourcesDir, engine)`. Python had a split where some used 3 args (no `resources_dir`), some used 4. The `ASSEMBLERS_WITH_RESOURCES_DIR` constant and conditional call are eliminated.
3. **No constructor args:** Python instantiated some assemblers with `resources_dir` in constructor. TypeScript assemblers all take no constructor args.
4. **Return type normalization:** Python assemblers all returned `List[Path]`. TypeScript has two return types: `string[]` (10 assemblers) and `AssembleResult { files: string[], warnings: string[] }` (4 assemblers). The pipeline must duck-type check to normalize.
5. **Duration measurement:** Python uses `time.monotonic()` (returns seconds, multiply by 1000). TypeScript uses `performance.now()` (returns ms directly).
6. **atomicOutput pattern:** Python uses context manager (`with atomic_output(dir) as temp:`). TypeScript uses callback (`atomicOutput(dir, async (temp) => {...})`).
7. **Path types:** Python returns `List[Path]`. TypeScript returns `string[]`.

---

## 14. Acceptance Criteria Checklist

From story Gherkin scenarios:

- [ ] Pipeline executes 14 assemblers in the order defined by RULE-008
- [ ] PipelineResult.success is true on successful execution
- [ ] filesGenerated contains files from all assemblers
- [ ] Dry-run does not alter the output directory
- [ ] Dry-run PipelineResult contains list of files that would be generated
- [ ] Dry-run warnings contains "Dry run -- no files written"
- [ ] Atomic output protects against partial failures (no partial files in outputDir on error)
- [ ] Temp dir is cleaned up on failure
- [ ] PipelineResult.durationMs is a positive number
- [ ] Warnings from multiple assemblers are aggregated in PipelineResult.warnings

From DoD:

- [ ] Coverage >= 95% line, >= 90% branch
- [ ] Unit + integration tests
- [ ] JSDoc on all exported functions and types
- [ ] Zero compiler warnings (`npx tsc --noEmit`)
- [ ] Output identical to Python pipeline behavior
