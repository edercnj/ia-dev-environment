# Architecture Plan -- story-0005-0001: Execution State Schema + Checkpoint Engine

## 1. Change Scope Assessment

**Scope:** New module -- no existing code is modified.

This story adds a self-contained checkpoint subsystem with zero coupling to the existing generation pipeline (`assembler/`, `domain/`, `config.ts`). The new code lives in its own directory under `src/` and introduces:

- Type definitions (interfaces + enum) for execution state
- A checkpoint engine module (read/write/update with atomic persistence)
- A JSON template file in `resources/templates/`
- A custom error class in the existing `exceptions.ts`

**Impact radius:** None on existing features. This is a foundation module consumed only by future stories (0005-0004, 0005-0005, 0005-0008).

## 2. Component Overview

| New File | Responsibility |
|----------|---------------|
| `src/checkpoint/types.ts` | All interfaces (`ExecutionState`, `StoryEntry`, `IntegrityGateEntry`, `ExecutionMetrics`, `SubagentResult`) and `StoryStatus` enum |
| `src/checkpoint/engine.ts` | Checkpoint engine: `createCheckpoint`, `readCheckpoint`, `updateStoryStatus`, `updateIntegrityGate`, `updateMetrics` |
| `src/checkpoint/validation.ts` | Schema validation logic for `readCheckpoint` (field presence, type checks, enum value guards) |
| `src/checkpoint/index.ts` | Barrel export for public API |
| `resources/templates/_TEMPLATE-EXECUTION-STATE.json` | Reference template with example structure |

**Modification to existing file:**

| Existing File | Change |
|---------------|--------|
| `src/exceptions.ts` | Add `CheckpointValidationError` and `CheckpointIOError` classes |

## 3. Dependency Direction

```
src/checkpoint/engine.ts
    |-- imports --> src/checkpoint/types.ts      (interfaces, enum)
    |-- imports --> src/checkpoint/validation.ts  (schema validators)
    |-- imports --> src/exceptions.ts             (CheckpointValidationError, CheckpointIOError)
    |-- uses   --> node:fs/promises              (readFile, writeFile, rename, stat, access)
    |-- uses   --> node:path                     (join)

src/checkpoint/validation.ts
    |-- imports --> src/checkpoint/types.ts
    |-- imports --> src/exceptions.ts
```

**No reverse dependencies.** Nothing in `assembler/`, `domain/`, `cli.ts`, `models.ts`, or `utils.ts` imports from `checkpoint/`. The checkpoint module is a leaf that future stories will consume.

**Why not reuse `atomicOutput()` from `utils.ts`?** The existing `atomicOutput()` operates at directory level (temp dir + recursive copy + rename). The checkpoint engine needs file-level atomic write (write temp file + `rename()` in the same directory). These are different granularities. A new `atomicWriteFile()` helper inside `engine.ts` is the correct approach -- it writes to `.execution-state.json.tmp` in the same directory and renames, which is guaranteed atomic on POSIX when source and destination are on the same filesystem.

## 4. Key Design Decisions

### 4.1 Atomic Write Strategy

- Write JSON to `${epicDir}/.execution-state.json.tmp` using `fs.writeFile()`
- Rename to `${epicDir}/execution-state.json` using `fs.rename()`
- `rename()` is atomic on the same filesystem (POSIX guarantee)
- The tmp file lives in the same directory as the target (same mount point = atomic rename)
- Extract as a private helper `atomicWriteJson(filePath, data)` inside `engine.ts`

### 4.2 Validation Approach

- Validation happens in `readCheckpoint()` only (trust the writer, validate on read)
- Use `requireField()` pattern from `models.ts` for mandatory field checks
- Enum validation via a `Set` of valid `StoryStatus` values with a type guard function
- Validation errors throw `CheckpointValidationError` with descriptive messages including the field name and expected type/values
- No runtime schema library (zod, ajv) -- hand-written validation matches project convention

### 4.3 Type Patterns

Follow existing project conventions from `models.ts`:

- All fields `readonly`
- Use TypeScript interfaces (not classes) for the state types -- these are serialized data shapes, not behavioral objects
- `StoryStatus` as a `const enum`-like pattern using `as const` object + type extraction (avoids `verbatimModuleSyntax` issues with `const enum`)
- Optional fields use `?` suffix with `undefined` (not `null`) per `exactOptionalPropertyTypes`
- `ExecutionMode` as a standalone interface: `{ readonly parallel: boolean; readonly skipReview: boolean }`

### 4.4 Read-Modify-Write Cycle

All update functions (`updateStoryStatus`, `updateIntegrityGate`, `updateMetrics`) follow:
1. `readCheckpoint(epicDir)` -- validates current state
2. Apply the partial update (spread/merge)
3. `atomicWriteJson()` -- persist atomically
4. Return the updated `ExecutionState`

### 4.5 Error Classes

Two new error classes in `exceptions.ts`, following existing patterns:

- `CheckpointValidationError` -- invalid schema on read (carries `field` and `detail`)
- `CheckpointIOError` -- filesystem failures (directory not found, permission denied; carries `path` and `operation`)

## 5. File Placement

```
src/
|-- checkpoint/
|   |-- index.ts              <-- barrel: re-exports public API
|   |-- types.ts              <-- ExecutionState, StoryEntry, IntegrityGateEntry,
|   |                             ExecutionMetrics, SubagentResult, StoryStatus,
|   |                             ExecutionMode
|   |-- engine.ts             <-- createCheckpoint, readCheckpoint, updateStoryStatus,
|   |                             updateIntegrityGate, updateMetrics, atomicWriteJson (private)
|   +-- validation.ts         <-- validateExecutionState, validateStoryEntry,
|                                  validateStoryStatus, isValidStoryStatus
|-- exceptions.ts             <-- (existing) + CheckpointValidationError, CheckpointIOError
resources/
+-- templates/
    +-- _TEMPLATE-EXECUTION-STATE.json   <-- reference template
tests/
+-- checkpoint/
    |-- engine.test.ts         <-- unit tests for all engine functions
    +-- validation.test.ts     <-- unit tests for schema validation
```

### Why `src/checkpoint/` (not `src/domain/checkpoint/`)?

The `domain/` directory contains stack resolution logic (KP routing, skill registry, pattern mapping). Checkpoint state management is orthogonal to domain resolution. A top-level `src/checkpoint/` directory keeps the module boundary clean and avoids polluting the existing domain module.

## 6. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `rename()` not atomic on network filesystems | Low | Medium | Document requirement: `execution-state.json` must be on a local filesystem. All expected usage is local (`docs/stories/epic-XXXX/`). |
| Race condition if two processes update concurrently | Low | High | Out of scope for this story -- single-writer assumption. Future story can add file locking if needed. |
| `exactOptionalPropertyTypes` causes type friction with `Partial<StoryEntry>` | Medium | Low | Define `StoryEntryUpdate` as an explicit partial type with `undefined` unions instead of using `Partial<>` directly. |
| Template file drifts from TypeScript types | Low | Low | Golden file test validates template structure matches the TypeScript interface fields. |
| `verbatimModuleSyntax` rejects `const enum` | Medium | Medium | Use `as const` object pattern instead: `export const StoryStatus = { PENDING: "PENDING", ... } as const; export type StoryStatus = typeof StoryStatus[keyof typeof StoryStatus];` |
| Large checkpoint files degrade performance | Very Low | Low | Story spec bounds this at <100 stories. JSON parse/stringify of a 100-entry map is sub-millisecond. |
