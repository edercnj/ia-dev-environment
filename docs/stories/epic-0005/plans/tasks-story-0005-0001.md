# Task Breakdown -- story-0005-0001: Execution State Schema + Checkpoint Engine

## Overview

TDD-driven implementation plan for the checkpoint subsystem: types, validation, engine, template, and barrel export. Tasks follow Double-Loop TDD with Transformation Priority Premise (TPP) ordering: degenerate cases first, then unconditional, conditional, iteration, and edge cases.

## Dependencies Summary

- **New files:** `src/checkpoint/types.ts`, `src/checkpoint/validation.ts`, `src/checkpoint/engine.ts`, `src/checkpoint/index.ts`
- **New template:** `resources/templates/_TEMPLATE-EXECUTION-STATE.json`
- **Modified file:** `src/exceptions.ts` (add 2 error classes)
- **New test files:** `tests/node/checkpoint/validation.test.ts`, `tests/node/checkpoint/engine.test.ts`

---

## Phase 0 -- Foundation (No Tests Required)

### TASK-1: Create StoryStatus enum and type interfaces (`types.ts`)

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/types.ts` |
| **Depends On** | -- |
| **Parallel** | Yes (with TASK-2) |

**Description:** Define all TypeScript interfaces and the `StoryStatus` constant enum pattern. Types compile but have no runtime behavior, so no test is required.

**Deliverables:**
- `StoryStatus` as `as const` object + type extraction (avoids `verbatimModuleSyntax` issues with `const enum`)
- `ExecutionMode` interface: `{ readonly parallel: boolean; readonly skipReview: boolean }`
- `StoryEntry` interface: `status`, `commitSha?`, `phase`, `duration?`, `retries`, `blockedBy?`, `summary?`, `findingsCount?`
- `IntegrityGateEntry` interface: `status` (`"PASS"` | `"FAIL"`), `timestamp`, `testCount`, `coverage`, `failedTests?`
- `ExecutionMetrics` interface: `storiesCompleted`, `storiesTotal`, `estimatedRemainingMinutes?`
- `SubagentResult` interface: `status` (`"SUCCESS"` | `"FAILED"` | `"PARTIAL"`), `commitSha?`, `findingsCount`, `summary`
- `ExecutionState` interface: `epicId`, `branch`, `startedAt`, `currentPhase`, `mode`, `stories` (Record), `integrityGates` (Record), `metrics`
- `StoryEntryUpdate` explicit partial type (avoids `Partial<>` friction with `exactOptionalPropertyTypes`)
- All fields `readonly`

**Acceptance:** `npx tsc --noEmit` passes with zero errors.

---

### TASK-2: Create error classes in `exceptions.ts`

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/exceptions.ts` |
| **Depends On** | -- |
| **Parallel** | Yes (with TASK-1) |

**Description:** Add two new error classes following existing patterns in `exceptions.ts`.

**Deliverables:**
- `CheckpointValidationError` extends `Error`: constructor takes `field: string` and `detail: string`, message format `Checkpoint validation failed: ${field} -- ${detail}`, readonly properties `field` and `detail`
- `CheckpointIOError` extends `Error`: constructor takes `path: string` and `operation: string`, message format `Checkpoint I/O failed during '${operation}': ${path}`, readonly properties `path` and `operation`

**Acceptance:** `npx tsc --noEmit` passes. Existing `tests/node/exceptions.test.ts` still green.

---

## Phase 1 -- Validation (TDD Cycle 1)

### TASK-3: RED -- Write validation tests for invalid schema

| Field | Value |
|-------|-------|
| **Type** | RED |
| **Files** | `tests/node/checkpoint/validation.test.ts` |
| **Depends On** | TASK-1, TASK-2 |
| **Parallel** | No |

**Description:** Write failing tests for the validation module. Tests follow TPP order: degenerate (missing fields, wrong types) before conditional (invalid enum values).

**Test scenarios (minimum):**
1. `validateExecutionState_missingEpicId_throwsCheckpointValidationError` -- missing `epicId` field
2. `validateExecutionState_missingBranch_throwsCheckpointValidationError` -- missing `branch` field
3. `validateExecutionState_missingStartedAt_throwsCheckpointValidationError` -- missing `startedAt`
4. `validateExecutionState_missingStories_throwsCheckpointValidationError` -- missing `stories` map
5. `validateExecutionState_missingMetrics_throwsCheckpointValidationError` -- missing `metrics`
6. `validateExecutionState_invalidCurrentPhaseType_throwsCheckpointValidationError` -- `currentPhase` is string instead of number
7. `validateStoryEntry_invalidStatusEnum_throwsCheckpointValidationError` -- story status is `"UNKNOWN"`
8. `validateStoryEntry_missingRetries_throwsCheckpointValidationError` -- story missing `retries` field
9. `isValidStoryStatus_validValues_returnsTrue` -- all 6 enum values are valid
10. `isValidStoryStatus_invalidValue_returnsFalse` -- `"BOGUS"` returns false
11. `validateExecutionState_validState_returnsWithoutThrowing` -- fully valid state passes

**Acceptance:** All tests fail with `module not found` or `function not found` errors (RED phase).

---

### TASK-4: GREEN -- Implement `validation.ts` to pass tests

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/validation.ts` |
| **Depends On** | TASK-3 |
| **Parallel** | No |

**Description:** Implement the minimum validation logic to make all TASK-3 tests pass.

**Deliverables:**
- `isValidStoryStatus(value: string): value is StoryStatus` -- type guard using a `Set` of valid values
- `validateStoryEntry(entry: unknown, storyId: string): void` -- validates a single story entry (status enum, required fields)
- `validateExecutionState(data: unknown): ExecutionState` -- validates top-level fields, iterates stories map calling `validateStoryEntry`, validates metrics; throws `CheckpointValidationError` on failure; returns typed `ExecutionState` on success

**Acceptance:** All TASK-3 tests pass. `npx tsc --noEmit` passes.

---

### TASK-5: REFACTOR -- Clean up validation

| Field | Value |
|-------|-------|
| **Type** | REFACTOR |
| **Files** | `src/checkpoint/validation.ts` |
| **Depends On** | TASK-4 |
| **Parallel** | No |

**Description:** Refactor without changing behavior. Potential improvements:
- Extract `requireField`-like helper if repeated validation patterns emerge
- Ensure all error messages include field name and expected type/values
- Verify function lengths <= 25 lines, extract sub-validators if needed
- Ensure consistent naming with project conventions

**Acceptance:** All TASK-3 tests still pass. No behavior change.

---

## Phase 2 -- Engine: `createCheckpoint` (TDD Cycle 2)

### TASK-6: RED -- Write `createCheckpoint` tests

| Field | Value |
|-------|-------|
| **Type** | RED |
| **Files** | `tests/node/checkpoint/engine.test.ts` |
| **Depends On** | TASK-4 |
| **Parallel** | No |

**Description:** Write failing tests for `createCheckpoint`. Uses `os.tmpdir()` for test isolation.

**Test scenarios:**
1. `createCheckpoint_validInputs_createsExecutionStateFile` -- file exists after call
2. `createCheckpoint_validInputs_allStoriesPending` -- all stories have status `PENDING`
3. `createCheckpoint_validInputs_retriesZeroForAll` -- retries is 0 for all stories
4. `createCheckpoint_validInputs_metricsInitialized` -- `storiesCompleted` is 0, `storiesTotal` equals story count
5. `createCheckpoint_validInputs_returnsTypedExecutionState` -- return value matches `ExecutionState` shape
6. `createCheckpoint_fiveStories_allMappedCorrectly` -- 5 stories mapped with correct IDs and phases
7. `createCheckpoint_nonExistentDirectory_throwsCheckpointIOError` -- directory does not exist
8. `createCheckpoint_atomicWrite_usesRenamePattern` -- tmp file used (verify by checking no `.tmp` file remains)

**Acceptance:** All tests fail (RED phase).

---

### TASK-7: GREEN -- Implement `createCheckpoint` in `engine.ts`

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/engine.ts` |
| **Depends On** | TASK-6 |
| **Parallel** | No |

**Description:** Implement `createCheckpoint` and the private `atomicWriteJson` helper.

**Deliverables:**
- Private `atomicWriteJson(filePath: string, data: ExecutionState): Promise<void>` -- writes to `.execution-state.json.tmp`, renames to `execution-state.json`
- `createCheckpoint(epicDir: string, epicId: string, branch: string, stories: ReadonlyArray<{id: string; phase: number}>, mode: ExecutionMode): Promise<ExecutionState>` -- builds initial state, all stories `PENDING`, retries 0, metrics initialized, writes atomically, returns typed state
- Validate directory existence before writing; throw `CheckpointIOError` if missing

**Acceptance:** All TASK-6 tests pass. `npx tsc --noEmit` passes.

---

### TASK-8: REFACTOR -- Clean up `createCheckpoint`

| Field | Value |
|-------|-------|
| **Type** | REFACTOR |
| **Files** | `src/checkpoint/engine.ts` |
| **Depends On** | TASK-7 |
| **Parallel** | No |

**Description:** Refactor without changing behavior:
- Extract story initialization into a helper if the function exceeds 25 lines
- Ensure `atomicWriteJson` is clean and reusable for update functions
- Verify naming conventions

**Acceptance:** All TASK-6 tests still pass. No behavior change.

---

## Phase 3 -- Engine: `readCheckpoint` (TDD Cycle 3)

### TASK-9: RED -- Write `readCheckpoint` tests

| Field | Value |
|-------|-------|
| **Type** | RED |
| **Files** | `tests/node/checkpoint/engine.test.ts` |
| **Depends On** | TASK-7 |
| **Parallel** | No |

**Description:** Write failing tests for `readCheckpoint`.

**Test scenarios:**
1. `readCheckpoint_validFile_returnsTypedExecutionState` -- reads and returns valid state
2. `readCheckpoint_validFile_allFieldsPresent` -- all mandatory fields populated
3. `readCheckpoint_missingFile_throwsCheckpointIOError` -- file does not exist
4. `readCheckpoint_invalidJson_throwsCheckpointValidationError` -- malformed JSON
5. `readCheckpoint_missingEpicId_throwsCheckpointValidationError` -- schema validation rejects
6. `readCheckpoint_invalidStoryStatus_throwsCheckpointValidationError` -- enum validation fails
7. `readCheckpoint_roundTrip_createThenRead_matchesOriginal` -- create then read produces identical state

**Acceptance:** All new tests fail (RED phase).

---

### TASK-10: GREEN -- Implement `readCheckpoint`

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/engine.ts` |
| **Depends On** | TASK-9 |
| **Parallel** | No |

**Description:** Implement `readCheckpoint(epicDir: string): Promise<ExecutionState>`.

**Behavior:**
- Read `execution-state.json` from `epicDir`
- Parse JSON, catch `SyntaxError` and wrap in `CheckpointValidationError`
- Pass parsed data through `validateExecutionState()` from validation module
- Throw `CheckpointIOError` if file not found (catch `ENOENT`)
- Return typed `ExecutionState`

**Acceptance:** All TASK-9 tests pass. `npx tsc --noEmit` passes.

---

## Phase 4 -- Engine: Update Functions (TDD Cycles 4-6)

### TASK-11: RED -- Write `updateStoryStatus` tests (atomic write)

| Field | Value |
|-------|-------|
| **Type** | RED |
| **Files** | `tests/node/checkpoint/engine.test.ts` |
| **Depends On** | TASK-10 |
| **Parallel** | No |

**Description:** Write failing tests for `updateStoryStatus`.

**Test scenarios:**
1. `updateStoryStatus_setSuccess_updatesStatusAndCommitSha` -- status changes to `SUCCESS`, `commitSha` set
2. `updateStoryStatus_setFailed_incrementsRetries` -- status `FAILED`, retries updated
3. `updateStoryStatus_otherStoriesUnchanged_preservesState` -- non-targeted stories remain identical
4. `updateStoryStatus_nonExistentStory_throwsCheckpointValidationError` -- story ID not in map
5. `updateStoryStatus_atomicWrite_noTmpFileRemains` -- no `.tmp` file left after operation
6. `updateStoryStatus_returnsUpdatedState` -- returned state reflects the update

**Acceptance:** All new tests fail (RED phase).

---

### TASK-12: GREEN -- Implement `updateStoryStatus`

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/engine.ts` |
| **Depends On** | TASK-11 |
| **Parallel** | No |

**Description:** Implement `updateStoryStatus(epicDir: string, storyId: string, update: StoryEntryUpdate): Promise<ExecutionState>`.

**Behavior:**
1. `readCheckpoint(epicDir)` to get current state
2. Verify `storyId` exists in `stories` map; throw `CheckpointValidationError` if not
3. Merge update into existing story entry (spread existing + spread update)
4. Rebuild `ExecutionState` with updated stories map
5. `atomicWriteJson()` to persist
6. Return updated state

**Acceptance:** All TASK-11 tests pass.

---

### TASK-13: RED -- Write `updateIntegrityGate` tests

| Field | Value |
|-------|-------|
| **Type** | RED |
| **Files** | `tests/node/checkpoint/engine.test.ts` |
| **Depends On** | TASK-12 |
| **Parallel** | No |

**Description:** Write failing tests for `updateIntegrityGate`.

**Test scenarios:**
1. `updateIntegrityGate_passResult_registersGateEntry` -- `integrityGates["phase-0"]` contains result
2. `updateIntegrityGate_timestampAutoPopulated_setsIso8601` -- timestamp is set automatically
3. `updateIntegrityGate_failResult_recordsFailedTests` -- `failedTests` array stored
4. `updateIntegrityGate_returnsUpdatedState` -- returned state reflects the gate entry

**Acceptance:** All new tests fail (RED phase).

---

### TASK-14: GREEN -- Implement `updateIntegrityGate`

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/engine.ts` |
| **Depends On** | TASK-13 |
| **Parallel** | No |

**Description:** Implement `updateIntegrityGate(epicDir: string, phase: number, result: Omit<IntegrityGateEntry, "timestamp">): Promise<ExecutionState>`.

**Behavior:**
1. `readCheckpoint(epicDir)` to get current state
2. Build `IntegrityGateEntry` with auto-populated `timestamp` (ISO-8601)
3. Update `integrityGates` map with key `"phase-${phase}"`
4. `atomicWriteJson()` to persist
5. Return updated state

**Acceptance:** All TASK-13 tests pass.

---

### TASK-15: RED -- Write `updateMetrics` tests

| Field | Value |
|-------|-------|
| **Type** | RED |
| **Files** | `tests/node/checkpoint/engine.test.ts` |
| **Depends On** | TASK-14 |
| **Parallel** | No |

**Description:** Write failing tests for `updateMetrics`.

**Test scenarios:**
1. `updateMetrics_updateCompleted_mergesPartialMetrics` -- `storiesCompleted` updated, `storiesTotal` unchanged
2. `updateMetrics_updateEstimate_setsRemainingMinutes` -- `estimatedRemainingMinutes` set
3. `updateMetrics_returnsUpdatedState` -- returned state reflects metrics

**Acceptance:** All new tests fail (RED phase).

---

### TASK-16: GREEN -- Implement `updateMetrics`

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/engine.ts` |
| **Depends On** | TASK-15 |
| **Parallel** | No |

**Description:** Implement `updateMetrics(epicDir: string, metrics: Partial<ExecutionMetrics>): Promise<ExecutionState>`.

**Behavior:**
1. `readCheckpoint(epicDir)` to get current state
2. Merge partial metrics into existing metrics (spread existing + spread update)
3. `atomicWriteJson()` to persist
4. Return updated state

**Acceptance:** All TASK-15 tests pass.

---

## Phase 5 -- Template and Golden File (TDD Cycle 7)

### TASK-17: Create template file

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `resources/templates/_TEMPLATE-EXECUTION-STATE.json` |
| **Depends On** | TASK-1 |
| **Parallel** | Yes (can run after TASK-1, parallel with Phases 1-4) |

**Description:** Create the reference JSON template matching the `ExecutionState` interface structure.

**Content:** A well-structured JSON file with example values and all fields present:
- `epicId`: `"XXXX"` (placeholder)
- `branch`: `"feat/epic-XXXX-full-implementation"`
- `startedAt`: ISO-8601 placeholder
- `currentPhase`: `0`
- `mode`: `{ "parallel": false, "skipReview": false }`
- `stories`: one example entry with all `StoryEntry` fields
- `integrityGates`: one example entry with all `IntegrityGateEntry` fields
- `metrics`: all `ExecutionMetrics` fields

**Acceptance:** Valid JSON. File exists at expected path.

---

### TASK-18: RED -- Write golden file test for template

| Field | Value |
|-------|-------|
| **Type** | RED |
| **Files** | `tests/node/checkpoint/engine.test.ts` (or dedicated template test file) |
| **Depends On** | TASK-17 |
| **Parallel** | No |

**Description:** Write a failing test that validates the template file structure against the `ExecutionState` interface.

**Test scenarios:**
1. `template_fileExists_atExpectedPath` -- template file exists at `resources/templates/_TEMPLATE-EXECUTION-STATE.json`
2. `template_validJson_parsesWithoutError` -- JSON.parse succeeds
3. `template_hasAllRequiredFields_matchesExecutionStateInterface` -- all top-level fields present: `epicId`, `branch`, `startedAt`, `currentPhase`, `mode`, `stories`, `integrityGates`, `metrics`
4. `template_storyEntry_hasAllRequiredFields` -- example story has `status`, `phase`, `retries`
5. `template_integrityGateEntry_hasAllRequiredFields` -- example gate has `status`, `timestamp`, `testCount`, `coverage`
6. `template_passesValidation_noValidationErrors` -- `validateExecutionState` does not throw

**Acceptance:** Tests fail because template does not yet pass full validation or test file references non-existent module paths.

---

### TASK-19: GREEN -- Verify golden file matches

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `resources/templates/_TEMPLATE-EXECUTION-STATE.json` (adjust if needed) |
| **Depends On** | TASK-18 |
| **Parallel** | No |

**Description:** Adjust template content (if needed) so all TASK-18 golden file tests pass. The template must be a valid `ExecutionState` that passes `validateExecutionState`.

**Acceptance:** All TASK-18 tests pass.

---

## Phase 6 -- Barrel Export

### TASK-20: Create barrel `index.ts`

| Field | Value |
|-------|-------|
| **Type** | GREEN |
| **Files** | `src/checkpoint/index.ts` |
| **Depends On** | TASK-16 (all implementation complete) |
| **Parallel** | No |

**Description:** Create the barrel export file that re-exports the public API.

**Exports:**
- From `types.ts`: `StoryStatus`, `ExecutionState`, `StoryEntry`, `IntegrityGateEntry`, `ExecutionMetrics`, `SubagentResult`, `ExecutionMode`, `StoryEntryUpdate`
- From `engine.ts`: `createCheckpoint`, `readCheckpoint`, `updateStoryStatus`, `updateIntegrityGate`, `updateMetrics`
- From `validation.ts`: `validateExecutionState`, `isValidStoryStatus`

**Acceptance:** `npx tsc --noEmit` passes. Consumers can import from `src/checkpoint/index.ts`.

---

## Task Dependency Graph

```
TASK-1 (types.ts) ──────┬──────────────────────────────── TASK-17 (template)
                         │                                      │
TASK-2 (exceptions.ts) ──┤                                 TASK-18 (RED: golden)
                         │                                      │
                    TASK-3 (RED: validation)                TASK-19 (GREEN: golden)
                         │
                    TASK-4 (GREEN: validation)
                         │
                    TASK-5 (REFACTOR: validation)
                         │
                    TASK-6 (RED: createCheckpoint)
                         │
                    TASK-7 (GREEN: createCheckpoint)
                         │
                    TASK-8 (REFACTOR: createCheckpoint)
                         │
                    TASK-9 (RED: readCheckpoint)
                         │
                    TASK-10 (GREEN: readCheckpoint)
                         │
                    TASK-11 (RED: updateStoryStatus)
                         │
                    TASK-12 (GREEN: updateStoryStatus)
                         │
                    TASK-13 (RED: updateIntegrityGate)
                         │
                    TASK-14 (GREEN: updateIntegrityGate)
                         │
                    TASK-15 (RED: updateMetrics)
                         │
                    TASK-16 (GREEN: updateMetrics)
                         │
                    TASK-20 (barrel index.ts) ◄── also depends on TASK-19
```

## Parallelism Summary

| Group | Tasks | Notes |
|-------|-------|-------|
| **A** | TASK-1, TASK-2 | Foundation types and errors, no dependencies between them |
| **B** | TASK-17 | Template creation can start as soon as TASK-1 completes |
| **Sequential** | TASK-3 through TASK-16 | Strict TDD Red-Green-Refactor chain |
| **B2** | TASK-18, TASK-19 | Golden file tests, can run in parallel with Phase 4 after TASK-17 |
| **Final** | TASK-20 | Depends on all prior tasks |

## Estimated Scope

| Metric | Count |
|--------|-------|
| Total tasks | 20 |
| RED tasks | 7 |
| GREEN tasks | 10 |
| REFACTOR tasks | 3 |
| New source files | 4 |
| Modified source files | 1 |
| New test files | 2 |
| Template files | 1 |
