# Implementation Plan -- story-0005-0001: Execution State Schema + Checkpoint Engine

## 1. Affected Layers and Components

### New Module: `src/checkpoint/`

Self-contained module with zero coupling to the existing generation pipeline. Does not touch `assembler/`, `domain/`, `config.ts`, `models.ts`, or `utils.ts`. The only existing file modified is `src/exceptions.ts` (two new error classes).

### Impacted Areas

| Area | Impact | Details |
|------|--------|---------|
| `src/checkpoint/` (new) | **New** | Entire module: types, validation, engine, barrel |
| `src/exceptions.ts` (existing) | **Modify** | Add `CheckpointValidationError` and `CheckpointIOError` |
| `resources/templates/` (existing dir) | **Add file** | `_TEMPLATE-EXECUTION-STATE.json` template |
| `tests/node/checkpoint/` (new) | **New** | Unit tests for engine and validation |

No changes to `src/index.ts`, CLI entry points, or existing assembler/domain code. This is a foundation module consumed only by future stories (0005-0004, 0005-0005, 0005-0008).

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/checkpoint/types.ts` -- Interfaces and Enum

**StoryStatus** (as const object + type extraction to avoid `verbatimModuleSyntax` issues with `const enum`):

```typescript
export const StoryStatus = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  SUCCESS: "SUCCESS",
  FAILED: "FAILED",
  BLOCKED: "BLOCKED",
  PARTIAL: "PARTIAL",
} as const;
export type StoryStatus = typeof StoryStatus[keyof typeof StoryStatus];
```

**Interfaces** (all fields `readonly`, optional fields use `?` with `undefined`):

| Interface | Key Fields | Notes |
|-----------|-----------|-------|
| `ExecutionMode` | `parallel: boolean`, `skipReview: boolean` | Standalone, embedded in `ExecutionState` |
| `StoryEntry` | `status: StoryStatus`, `phase: number`, `retries: number`, `commitSha?: string`, `duration?: string`, `blockedBy?: readonly string[]`, `summary?: string`, `findingsCount?: number` | Optional fields follow `exactOptionalPropertyTypes` |
| `IntegrityGateEntry` | `status: "PASS" \| "FAIL"`, `timestamp: string`, `testCount: number`, `coverage: number`, `failedTests?: readonly string[]` | `timestamp` auto-set by engine |
| `ExecutionMetrics` | `storiesCompleted: number`, `storiesTotal: number`, `estimatedRemainingMinutes?: number` | Partial updates via explicit update type |
| `SubagentResult` | `status: "SUCCESS" \| "FAILED" \| "PARTIAL"`, `findingsCount: number`, `summary: string`, `commitSha?: string` | Contract for subagent outputs |
| `ExecutionState` | `epicId: string`, `branch: string`, `startedAt: string`, `currentPhase: number`, `mode: ExecutionMode`, `stories: Readonly<Record<string, StoryEntry>>`, `integrityGates: Readonly<Record<string, IntegrityGateEntry>>`, `metrics: ExecutionMetrics` | Top-level state shape |

**Update types** (explicit rather than `Partial<>` to satisfy `exactOptionalPropertyTypes`):

| Type | Purpose |
|------|---------|
| `StoryEntryUpdate` | Fields that can be updated on a story entry (all optional with `\| undefined`) |
| `MetricsUpdate` | Fields that can be updated on metrics (all optional with `\| undefined`) |

### 2.2 `src/checkpoint/validation.ts` -- Schema Validation

| Export | Signature | Responsibility |
|--------|-----------|----------------|
| `validateExecutionState` | `(data: unknown) => ExecutionState` | Validates full state object, throws `CheckpointValidationError` |
| `validateStoryEntry` | `(data: unknown, storyId: string) => StoryEntry` | Validates a single story entry |
| `isValidStoryStatus` | `(value: string) => value is StoryStatus` | Type guard using `Set` of valid values |

Validation approach:
- Trust the writer, validate on read (`readCheckpoint` only)
- Use `requireField()`-style checks for mandatory fields (pattern from `models.ts`)
- Enum validation via `Set<string>` containing valid `StoryStatus` values
- Errors throw `CheckpointValidationError` with field name and expected type/values
- No runtime schema library (zod, ajv) -- hand-written validation matches project convention

### 2.3 `src/checkpoint/engine.ts` -- Checkpoint CRUD with Atomic Writes

| Export | Signature | Responsibility |
|--------|-----------|----------------|
| `createCheckpoint` | `(epicDir: string, epicId: string, branch: string, storyIds: readonly string[], mode: ExecutionMode) => Promise<ExecutionState>` | Creates initial `execution-state.json` with all stories PENDING |
| `readCheckpoint` | `(epicDir: string) => Promise<ExecutionState>` | Reads + validates JSON, returns typed state |
| `updateStoryStatus` | `(epicDir: string, storyId: string, update: StoryEntryUpdate) => Promise<ExecutionState>` | Read-modify-write with atomic persistence |
| `updateIntegrityGate` | `(epicDir: string, phase: string, result: Omit<IntegrityGateEntry, "timestamp">) => Promise<ExecutionState>` | Registers gate result, auto-sets timestamp |
| `updateMetrics` | `(epicDir: string, update: MetricsUpdate) => Promise<ExecutionState>` | Partial metrics update |

**Private helper:**

| Function | Signature | Responsibility |
|----------|-----------|----------------|
| `atomicWriteJson` | `(filePath: string, data: ExecutionState) => Promise<void>` | Write to `.execution-state.json.tmp`, rename to `execution-state.json` |

Atomic write strategy:
1. `fs.writeFile()` to `${epicDir}/.execution-state.json.tmp`
2. `fs.rename()` to `${epicDir}/execution-state.json`
3. `rename()` is atomic on POSIX when source and destination are on the same filesystem
4. Temp file lives in the same directory as target (same mount point = atomic rename)

Read-modify-write cycle for all update functions:
1. `readCheckpoint(epicDir)` -- validates current state
2. Apply the partial update (object spread/merge)
3. `atomicWriteJson()` -- persist atomically
4. Return the updated `ExecutionState`

**Why not reuse `atomicOutput()` from `utils.ts`?** The existing `atomicOutput()` operates at directory level (temp dir + recursive copy + rename). The checkpoint engine needs file-level atomic write (write temp file + `rename()` in the same directory). Different granularity warrants a dedicated helper.

### 2.4 `src/checkpoint/index.ts` -- Barrel Export

Re-exports public API:
- All types and interfaces from `types.ts`
- `StoryStatus` const object
- All engine functions from `engine.ts`
- `isValidStoryStatus` type guard from `validation.ts`

Internal validation functions (`validateExecutionState`, `validateStoryEntry`) remain non-public -- consumed only by `engine.ts`.

### 2.5 `resources/templates/_TEMPLATE-EXECUTION-STATE.json`

Reference JSON template with example structure matching `ExecutionState` interface. Contains example values (not placeholders) showing a realistic initial state with 2-3 stories.

---

## 3. Existing Classes to Modify

### `src/exceptions.ts` -- Two New Error Classes

**`CheckpointValidationError`** -- invalid schema on read:

```typescript
export class CheckpointValidationError extends Error {
  readonly field: string;
  readonly detail: string;

  constructor(field: string, detail: string) {
    super(`Checkpoint validation failed for '${field}': ${detail}`);
    this.name = "CheckpointValidationError";
    this.field = field;
    this.detail = detail;
  }
}
```

**`CheckpointIOError`** -- filesystem failures:

```typescript
export class CheckpointIOError extends Error {
  readonly path: string;
  readonly operation: string;

  constructor(path: string, operation: string) {
    super(`Checkpoint I/O failed during '${operation}' at: ${path}`);
    this.name = "CheckpointIOError";
    this.path = path;
    this.operation = operation;
  }
}
```

Pattern consistency:
- Both follow the existing pattern: `extends Error`, `readonly` fields, `this.name` set in constructor
- Match existing error classes (`CliError`, `ConfigValidationError`, `ConfigParseError`, `PipelineError`)
- Contextual fields carry the values that caused the error (per coding standards: "Exceptions MUST carry context")

---

## 4. Dependency Direction Validation

```
src/checkpoint/engine.ts
    |-- imports --> src/checkpoint/types.ts      (interfaces, enum)
    |-- imports --> src/checkpoint/validation.ts  (schema validators)
    |-- imports --> src/exceptions.ts             (CheckpointValidationError, CheckpointIOError)
    |-- uses   --> node:fs/promises              (readFile, writeFile, rename, access)
    |-- uses   --> node:path                     (join)

src/checkpoint/validation.ts
    |-- imports --> src/checkpoint/types.ts       (interfaces, enum, type guards)
    |-- imports --> src/exceptions.ts             (CheckpointValidationError)

src/checkpoint/types.ts
    |-- no imports from src/                     (pure type definitions)

src/checkpoint/index.ts
    |-- re-exports from types.ts, engine.ts, validation.ts (barrel only)
```

**Validation:**
- `types.ts` has zero internal dependencies -- pure leaf
- `validation.ts` depends only on `types.ts` and `exceptions.ts` -- no cycles
- `engine.ts` depends on `types.ts`, `validation.ts`, `exceptions.ts` -- no cycles
- **No reverse dependencies**: nothing in `assembler/`, `domain/`, `cli.ts`, `models.ts`, or `utils.ts` imports from `checkpoint/`
- The checkpoint module is a leaf module consumed only by future stories
- Dependency direction is inward (engine -> validation -> types), matching the architecture convention

**`import type` enforcement**: Under `verbatimModuleSyntax`, type-only imports must use `import type { ... }`. All interface/type imports from `types.ts` into `engine.ts` and `validation.ts` must use `import type` syntax.

---

## 5. Integration Points

### 5.1 Current Story (0005-0001) -- None

This story has **zero integration points** with existing code. It is a standalone foundation module. The only touchpoint is `src/exceptions.ts` which gains two new classes that do not affect existing consumers.

### 5.2 Future Consumers (Downstream Stories)

| Consumer Story | Integration Point |
|----------------|-------------------|
| story-0005-0004 | Imports `createCheckpoint`, `readCheckpoint`, `ExecutionState` to initialize/resume epic execution |
| story-0005-0005 | Imports `updateStoryStatus`, `updateIntegrityGate`, `updateMetrics` for real-time state tracking |
| story-0005-0008 | Imports `readCheckpoint`, `ExecutionState` for reporting and progress display |

### 5.3 Node.js API Surface

| API | Module |
|-----|--------|
| `node:fs/promises` | `readFile`, `writeFile`, `rename`, `access`, `mkdir` |
| `node:path` | `join` |

No new npm dependencies required. All filesystem operations use Node.js built-in modules only.

### 5.4 File System Contracts

| File | Location | Format |
|------|----------|--------|
| `execution-state.json` | `docs/stories/epic-XXXX/` (or any `epicDir`) | JSON matching `ExecutionState` interface |
| `.execution-state.json.tmp` | Same directory as target | Temporary file, deleted on successful rename |

---

## 6. Configuration Changes

**None required.**

- No changes to `tsconfig.json` -- the `include: ["src/**/*.ts"]` glob already captures `src/checkpoint/*.ts`
- No changes to `vitest.config.ts` -- the `include: ["tests/**/*.test.ts"]` glob already captures `tests/node/checkpoint/*.test.ts`
- No changes to `package.json` -- no new dependencies
- No changes to `tsup` build config -- the barrel export from `src/checkpoint/index.ts` is available for import but not exposed via the CLI entry point
- Coverage config in `vitest.config.ts` already includes `src/**/*.ts` and excludes `tests/**`

---

## 7. Risk Assessment

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| 1 | `rename()` not atomic on network filesystems | Low | Medium | Document requirement: `execution-state.json` must be on a local filesystem. All expected usage is local (`docs/stories/epic-XXXX/`). |
| 2 | Race condition if two processes update concurrently | Low | High | Out of scope for this story -- single-writer assumption. Future story can add file locking if needed. |
| 3 | `exactOptionalPropertyTypes` causes type friction with `Partial<StoryEntry>` | Medium | Low | Define `StoryEntryUpdate` and `MetricsUpdate` as explicit types with `| undefined` unions instead of using `Partial<>` directly. |
| 4 | Template file drifts from TypeScript types | Low | Low | Golden file test validates template structure matches the TypeScript interface fields. |
| 5 | `verbatimModuleSyntax` rejects `const enum` | Medium | Medium | Use `as const` object pattern instead of `const enum`. All type-only imports must use `import type` syntax. |
| 6 | Large checkpoint files degrade performance | Very Low | Low | Story spec bounds at <100 stories. JSON parse/stringify of a 100-entry map is sub-millisecond. Performance DoD target: <50ms for read/write. |
| 7 | `noUncheckedIndexedAccess` friction with `Record<string, StoryEntry>` reads | Medium | Low | All map lookups must guard against `undefined` return. Use explicit checks before accessing `state.stories[storyId]`. |
| 8 | New error classes break `instanceof` checks in `index.ts` | Very Low | Low | New error classes are not referenced in `index.ts` error handling. They are thrown/caught within the checkpoint module or by future consumers. No changes to existing error handling. |

---

## 8. Implementation Order (TDD)

Following inner-to-outer and TPP (simple to complex):

### Phase 1: Error Classes (in `src/exceptions.ts`)
1. **Test**: `CheckpointValidationError` constructor, `name`, `field`, `detail`, `instanceof Error`
2. **Implement**: `CheckpointValidationError` class
3. **Test**: `CheckpointIOError` constructor, `name`, `path`, `operation`, `instanceof Error`
4. **Implement**: `CheckpointIOError` class

### Phase 2: Types (`src/checkpoint/types.ts`)
5. **Implement**: `StoryStatus` const object + type, all interfaces, update types
6. No runtime tests needed -- compile-time validation via `tsc --noEmit`

### Phase 3: Validation (`src/checkpoint/validation.ts`)
7. **Test**: `isValidStoryStatus` with valid values returns `true`
8. **Test**: `isValidStoryStatus` with invalid value returns `false`
9. **Implement**: `isValidStoryStatus` type guard
10. **Test**: `validateExecutionState` with valid full state returns typed object
11. **Test**: `validateExecutionState` with missing `epicId` throws `CheckpointValidationError`
12. **Test**: `validateExecutionState` with invalid story status throws with "invalid status" message
13. **Implement**: `validateExecutionState`, `validateStoryEntry`

### Phase 4: Engine -- Create (`src/checkpoint/engine.ts`)
14. **Test**: `createCheckpoint` with valid inputs creates `execution-state.json` with all stories PENDING
15. **Test**: `createCheckpoint` sets metrics (completed=0, total=storyCount)
16. **Test**: `createCheckpoint` with non-existent directory throws `CheckpointIOError`
17. **Implement**: `createCheckpoint`, `atomicWriteJson` (private)

### Phase 5: Engine -- Read
18. **Test**: `readCheckpoint` with valid file returns typed `ExecutionState`
19. **Test**: `readCheckpoint` with invalid schema throws `CheckpointValidationError`
20. **Test**: `readCheckpoint` with missing file throws `CheckpointIOError`
21. **Implement**: `readCheckpoint`

### Phase 6: Engine -- Update
22. **Test**: `updateStoryStatus` changes status and preserves other stories
23. **Test**: `updateStoryStatus` with retry increment
24. **Test**: `updateStoryStatus` with non-existent story throws
25. **Implement**: `updateStoryStatus`
26. **Test**: `updateIntegrityGate` registers result with auto-timestamp
27. **Implement**: `updateIntegrityGate`
28. **Test**: `updateMetrics` partial update
29. **Implement**: `updateMetrics`

### Phase 7: Atomic Write Verification
30. **Test**: Verify `.execution-state.json.tmp` is created then renamed (atomic write contract)
31. Refactor if needed

### Phase 8: Template + Golden File
32. **Create**: `resources/templates/_TEMPLATE-EXECUTION-STATE.json`
33. **Test**: Golden file test validates template structure matches interface fields

### Phase 9: Barrel Export
34. **Create**: `src/checkpoint/index.ts` with re-exports

---

## 9. Test File Structure

```
tests/
+-- node/
    +-- checkpoint/
        |-- engine.test.ts          <-- Phases 4-7 (createCheckpoint, readCheckpoint, updates, atomic write)
        +-- validation.test.ts      <-- Phase 3 (validateExecutionState, isValidStoryStatus)
    +-- exceptions.test.ts          <-- Phase 1 (add tests for new error classes to existing file)
```

All tests use `vitest` (`describe`, `it`, `expect`), temporary directories via `node:fs/promises` + `node:os` `tmpdir()`, and follow the `[methodUnderTest]_[scenario]_[expectedBehavior]` naming convention observed in existing tests.

---

## 10. Acceptance Criteria Traceability

| Gherkin Scenario | Test Phase | Test File |
|-----------------|------------|-----------|
| Checkpoint creation with empty state | Phase 4, test 14-15 | `engine.test.ts` |
| Read valid checkpoint with schema validation | Phase 5, test 18 | `engine.test.ts` |
| Atomic update of story status to SUCCESS | Phase 6, test 22 | `engine.test.ts` |
| Atomic update of story status to FAILED with retry | Phase 6, test 23 | `engine.test.ts` |
| Reject checkpoint with missing epicId | Phase 3, test 11 | `validation.test.ts` |
| Reject invalid status enum | Phase 3, test 12 | `validation.test.ts` |
| Update integrity gate result | Phase 6, test 26 | `engine.test.ts` |
| Create checkpoint in non-existent directory | Phase 4, test 16 | `engine.test.ts` |
