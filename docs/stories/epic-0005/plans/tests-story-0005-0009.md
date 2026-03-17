# Test Plan -- story-0005-0009: Partial Execution (`--phase N`, `--story XXXX-YYYY`)

**Story:** `story-0005-0009.md`
**Implementation Plan:** `plan-story-0005-0009.md`

**Framework:** Vitest (pool: forks, maxForks: 3, maxConcurrency: 5)
**Coverage Targets:** >= 95% line, >= 90% branch
**Test Naming:** `[functionUnderTest]_[scenario]_[expectedBehavior]`

---

## Test File Structure

```
tests/
  domain/
    implementation-map/
      partial-execution.test.ts       # UT-01 through UT-22 (pure function unit tests)
      helpers.ts                      # Extended: no new helpers needed (reuses existing factories)
  node/
    content/
      x-dev-epic-implement-content.test.ts  # IT-01 through IT-06 (content assertions for SKILL.md)
```

---

## 1. Acceptance Tests (AT-N) -- Outer Loop

These acceptance tests map 1:1 to the 7 Gherkin scenarios in Section 7 of the story. They are written first and remain RED until all inner-loop unit tests drive the implementation to completion. All acceptance tests compose the 4 pure functions from `partial-execution.ts`.

**Test File:** `tests/domain/implementation-map/partial-execution.test.ts`

| ID | Gherkin Scenario | Description | Input/Setup | Expected Result | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| AT-01 | Phase 2 with phases 0-1 complete | Full phase execution flow: parse mode, validate phase prerequisites, get stories for phase | `parsePartialExecutionMode(2, undefined)` returns phase mode. `parsedMap` with 3 phases (0,1,2), all phase 0-1 stories SUCCESS in `executionState`. | `parsePartialExecutionMode` returns `{ kind: "phase", phase: 2 }`. `validatePhasePrerequisites` returns `{ valid: true }`. `getStoriesForPhase(2, parsedMap)` returns phase 2 story IDs. | TASK-types | yes |
| AT-02 | Phase N with prior phase incomplete | Phase mode with incomplete prior phase aborts | `parsedMap` with 3 phases. Phase 0 stories SUCCESS, phase 1 has one PENDING story. | `validatePhasePrerequisites(2, parsedMap, state)` returns `{ valid: false, error: "Phases 0..1 must be complete before phase 2" }` | TASK-types | yes |
| AT-03 | Story with deps satisfied | Story mode with all dependencies SUCCESS | `parsePartialExecutionMode(undefined, "0042-0003")` returns story mode. Story "0042-0003" depends on "0042-0001" and "0042-0002", both SUCCESS. | `parsePartialExecutionMode` returns `{ kind: "story", storyId: "0042-0003" }`. `validateStoryPrerequisites` returns `{ valid: true }`. | TASK-types | yes |
| AT-04 | Story with dep not satisfied | Story mode with unsatisfied dependency aborts | Story "0042-0003" depends on "0042-0001" (SUCCESS) and "0042-0002" (PENDING). | `validateStoryPrerequisites` returns `{ valid: false, error: "Dependencies not satisfied: [0042-0002]", unsatisfiedDeps: ["0042-0002"] }` | TASK-types | yes |
| AT-05 | `--phase` and `--story` mutually exclusive | Both flags provided throws error | `parsePartialExecutionMode(2, "0042-0003")` | Throws `PartialExecutionError` with code `MUTUAL_EXCLUSIVITY` and message containing "--phase and --story are mutually exclusive" | TASK-types | yes |
| AT-06 | `--phase` with invalid number | Phase exceeding max phase returns error | `parsedMap` with phases 0-3 (4 phases). `validatePhasePrerequisites(5, parsedMap, state)` | Returns `{ valid: false, error: "Phase 5 does not exist. Max phase is 3." }` | TASK-types | yes |
| AT-07 | `--story` with nonexistent ID | Story not in map returns error | `parsedMap` where "0042-9999" does not exist. `validateStoryPrerequisites("0042-9999", parsedMap, state)` | Returns `{ valid: false, error: "Story 0042-9999 not found in implementation map" }` | TASK-types | yes |

---

## 2. Unit Tests (UT-N) -- Inner Loop (TPP Order)

### 2.1 `parsePartialExecutionMode(phase?, storyId?)`

**Test File:** `tests/domain/implementation-map/partial-execution.test.ts`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-01 | `parsePartialExecutionMode_neitherFlagProvided_returnsFullMode` | `phase = undefined`, `storyId = undefined` | Returns `{ kind: "full" }` | 1 (nil -> constant) | none | yes |
| UT-02 | `parsePartialExecutionMode_phaseOnly_returnsPhaseMode` | `phase = 2`, `storyId = undefined` | Returns `{ kind: "phase", phase: 2 }` | 3 (constant -> variable) | none | yes |
| UT-03 | `parsePartialExecutionMode_storyOnly_returnsStoryMode` | `phase = undefined`, `storyId = "0042-0003"` | Returns `{ kind: "story", storyId: "0042-0003" }` | 3 (constant -> variable) | none | yes |
| UT-04 | `parsePartialExecutionMode_bothFlagsProvided_throwsPartialExecutionError` | `phase = 2`, `storyId = "0042-0003"` | Throws `PartialExecutionError` with code `MUTUAL_EXCLUSIVITY` | 4 (unconditional -> conditional) | none | yes |
| UT-05 | `parsePartialExecutionMode_bothFlagsProvided_errorMessageContainsMutuallyExclusive` | `phase = 2`, `storyId = "0042-0003"` | Error message contains "--phase and --story are mutually exclusive" | 4 (unconditional -> conditional) | none | yes |
| UT-06 | `parsePartialExecutionMode_phaseZero_returnsPhaseMode` | `phase = 0`, `storyId = undefined` | Returns `{ kind: "phase", phase: 0 }` | 6 (edge: boundary value) | none | yes |

### 2.2 `validatePhasePrerequisites(phase, parsedMap, executionState)`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-07 | `validatePhasePrerequisites_phaseZero_returnsValid` | `phase = 0`, `parsedMap` with phases 0-2, any `executionState` | Returns `{ valid: true }` (no prior phases to check) | 1 (nil -> constant) | none | yes |
| UT-08 | `validatePhasePrerequisites_phase1WithPhase0AllSuccess_returnsValid` | `phase = 1`, `parsedMap` with phases 0-1, phase 0 stories all SUCCESS | Returns `{ valid: true }` | 3 (constant -> variable) | none | yes |
| UT-09 | `validatePhasePrerequisites_phase2WithPhases0And1AllSuccess_returnsValid` | `phase = 2`, `parsedMap` with phases 0-2, all phase 0 and 1 stories SUCCESS | Returns `{ valid: true }` | 5 (iteration over phases) | none | yes |
| UT-10 | `validatePhasePrerequisites_phase2WithPhase1Pending_returnsInvalid` | `phase = 2`, phase 0 stories SUCCESS, phase 1 has one PENDING story | Returns `{ valid: false, error: "Phases 0..1 must be complete before phase 2" }` | 4 (unconditional -> conditional) | none | yes |
| UT-11 | `validatePhasePrerequisites_phase2WithPhase0Failed_returnsInvalid` | `phase = 2`, phase 0 has one FAILED story, phase 1 stories SUCCESS | Returns `{ valid: false, error: "Phases 0..1 must be complete before phase 2" }` | 4 (unconditional -> conditional) | none | yes |
| UT-12 | `validatePhasePrerequisites_phaseExceedsMaxPhase_returnsInvalid` | `phase = 5`, `parsedMap` with phases 0-3 (totalPhases = 4) | Returns `{ valid: false, error: "Phase 5 does not exist. Max phase is 3." }` | 4 (unconditional -> conditional) | none | yes |
| UT-13 | `validatePhasePrerequisites_phaseEqualsMaxPhase_returnsInvalid` | `phase = 4`, `parsedMap` with phases 0-3 (totalPhases = 4) | Returns `{ valid: false, error: "Phase 4 does not exist. Max phase is 3." }` | 6 (edge: boundary at max) | none | yes |
| UT-14 | `validatePhasePrerequisites_negativePhase_returnsInvalid` | `phase = -1`, `parsedMap` with phases 0-2 | Returns `{ valid: false }` with appropriate error message | 6 (edge: boundary below min) | none | yes |

### 2.3 `validateStoryPrerequisites(storyId, parsedMap, executionState)`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-15 | `validateStoryPrerequisites_storyNotInMap_returnsInvalid` | `storyId = "0042-9999"`, `parsedMap` without that story | Returns `{ valid: false, error: "Story 0042-9999 not found in implementation map" }` | 2 (nil -> constant) | none | yes |
| UT-16 | `validateStoryPrerequisites_storyWithNoDeps_returnsValid` | `storyId = "0042-0001"`, story has `blockedBy: []`, any `executionState` | Returns `{ valid: true }` | 3 (constant -> variable) | none | yes |
| UT-17 | `validateStoryPrerequisites_allDepsSuccess_returnsValid` | `storyId = "0042-0003"`, story depends on "0042-0001" and "0042-0002", both SUCCESS | Returns `{ valid: true }` | 5 (iteration over deps) | none | yes |
| UT-18 | `validateStoryPrerequisites_oneDepPending_returnsInvalid` | `storyId = "0042-0003"`, dep "0042-0001" SUCCESS, dep "0042-0002" PENDING | Returns `{ valid: false, error: "Dependencies not satisfied: [0042-0002]", unsatisfiedDeps: ["0042-0002"] }` | 4 (unconditional -> conditional) | none | yes |
| UT-19 | `validateStoryPrerequisites_multipleUnsatisfiedDeps_listsAllInUnsatisfiedDeps` | `storyId = "0042-0004"`, depends on "0042-0001" (PENDING), "0042-0002" (FAILED), "0042-0003" (SUCCESS) | Returns `{ valid: false, unsatisfiedDeps: ["0042-0001", "0042-0002"] }` (0042-0003 excluded since it is SUCCESS) | 5 (scalar -> collection) | none | yes |
| UT-20 | `validateStoryPrerequisites_depMissingFromExecutionState_treatedAsUnsatisfied` | `storyId = "0042-0003"`, dep "0042-0001" SUCCESS, dep "0042-0002" missing from `executionState` entirely | Returns `{ valid: false, unsatisfiedDeps: ["0042-0002"] }` | 6 (edge: absent entry) | none | yes |

### 2.4 `getStoriesForPhase(phase, parsedMap)`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-21 | `getStoriesForPhase_validPhaseWithStories_returnsStoryIds` | `phase = 0`, `parsedMap.phases` has `{0: ["0001", "0002"]}` | Returns `["0001", "0002"]` | 2 (nil -> constant) | none | yes |
| UT-22 | `getStoriesForPhase_phaseNotInMap_returnsEmptyArray` | `phase = 5`, `parsedMap.phases` has only phases 0-2 | Returns `[]` (empty readonly array) | 1 (nil -> constant) | none | yes |

---

## 3. Integration Tests (IT-N) -- Content Assertions

These tests verify that the SKILL.md templates contain the partial execution documentation added by this story. They extend the existing `x-dev-epic-implement-content.test.ts` file.

**Test File:** `tests/node/content/x-dev-epic-implement-content.test.ts`

| ID | Description | Input/Setup | Expected Result | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|
| IT-01 | `skillMd_containsPartialExecutionSection` | Read `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | Content contains "Partial Execution" as a section heading | SKILL.md update | yes |
| IT-02 | `skillMd_partialExecution_containsMutualExclusivityRule` | Same content | Content contains "mutually exclusive" | SKILL.md update | yes |
| IT-03 | `skillMd_partialExecution_containsPhaseFlowDescription` | Same content | Content contains "--phase" AND references integrity gate in the phase flow context | SKILL.md update | yes |
| IT-04 | `skillMd_partialExecution_containsStoryFlowDescription` | Same content | Content contains "--story" AND references "no integrity gate" or equivalent for single story mode | SKILL.md update | yes |
| IT-05 | `skillMd_partialExecution_containsErrorSpecifications` | Same content | Content contains at least 3 of: "does not exist", "must be complete", "not found", "not satisfied" | SKILL.md update | yes |
| IT-06 | `dualCopy_partialExecutionTerms_presentInBothTemplates` | Read both SKILL.md (Claude) and x-dev-epic-implement.md (GitHub) | Both files contain: "Partial Execution", "mutually exclusive", "--phase", "--story", "integrity gate" | SKILL.md update | yes |

---

## 4. TPP Order Summary

The unit tests are designed to be implemented in this order within each function, guiding the implementation through progressively more complex transformations:

### parsePartialExecutionMode

```
Level 1: UT-01                       -- Neither flag -> return full mode (nil -> constant)
Level 3: UT-02, UT-03               -- Single flag -> return typed mode (constant -> variable)
Level 4: UT-04, UT-05               -- Both flags -> throw error (unconditional -> conditional)
Level 6: UT-06                       -- Phase 0 boundary (edge case)
```

### validatePhasePrerequisites

```
Level 1: UT-07                       -- Phase 0 -> always valid (nil -> constant)
Level 3: UT-08                       -- Phase 1 with phase 0 complete (constant -> variable)
Level 4: UT-10, UT-11, UT-12        -- Incomplete phase, failed phase, invalid phase number (conditional)
Level 5: UT-09                       -- Phase 2 iterating over phases 0 and 1 (iteration)
Level 6: UT-13, UT-14               -- Phase at max boundary, negative phase (edge cases)
```

### validateStoryPrerequisites

```
Level 2: UT-15                       -- Story not found -> constant error (nil -> constant)
Level 3: UT-16                       -- Story with no deps -> valid (constant -> variable)
Level 4: UT-18                       -- One dep unsatisfied -> conditional check
Level 5: UT-17, UT-19               -- All deps satisfied / multiple unsatisfied (iteration)
Level 6: UT-20                       -- Dep missing from execution state (edge case)
```

### getStoriesForPhase

```
Level 1: UT-22                       -- Phase not in map -> empty array (nil -> constant)
Level 2: UT-21                       -- Valid phase -> return stories (constant -> variable)
```

---

## 5. Double-Loop TDD Execution Order

### Phase A: Write ALL Acceptance Tests (AT-01 through AT-07) -- All RED

All 7 acceptance tests are written first, mapping 1:1 to the Gherkin scenarios. They all fail because `partial-execution.ts` does not exist yet.

### Phase B: Types (no tests -- compilation only)

1. Add `PhaseExecutionMode`, `StoryExecutionMode`, `FullExecutionMode`, `PartialExecutionMode`, `PrerequisiteResult` to `types.ts`.
2. Add `PartialExecutionError` to `exceptions.ts`.
3. Run `npx tsc --noEmit` to verify.

### Phase C: `parsePartialExecutionMode` (drives AT-01, AT-03, AT-05 toward GREEN)

```
Inner loop: UT-01 -> UT-02 -> UT-03 -> UT-04 -> UT-05 -> UT-06
```

After this phase: `parsePartialExecutionMode()` is complete. AT-05 turns GREEN.

### Phase D: `validatePhasePrerequisites` (drives AT-02, AT-06 toward GREEN)

```
Inner loop: UT-07 -> UT-08 -> UT-09 -> UT-10 -> UT-11 -> UT-12 -> UT-13 -> UT-14
```

After this phase: `validatePhasePrerequisites()` is complete. AT-01, AT-02, AT-06 turn GREEN.

### Phase E: `validateStoryPrerequisites` (drives AT-03, AT-04, AT-07 toward GREEN)

```
Inner loop: UT-15 -> UT-16 -> UT-17 -> UT-18 -> UT-19 -> UT-20
```

After this phase: `validateStoryPrerequisites()` is complete. AT-03, AT-04, AT-07 turn GREEN.

### Phase F: `getStoriesForPhase` (drives AT-01 composition toward GREEN)

```
Inner loop: UT-22 -> UT-21
```

After this phase: `getStoriesForPhase()` is complete. All 7 acceptance tests GREEN.

### Phase G: Barrel Exports

1. Add exports to `src/domain/implementation-map/index.ts`.
2. Run `npx tsc --noEmit` to verify.

### Phase H: SKILL.md Template Updates + Content Tests

1. Add "Partial Execution" section to both SKILL.md templates.
2. Write IT-01 through IT-06 in `x-dev-epic-implement-content.test.ts`.
3. Run content tests -- all GREEN.

### Phase I: Verification

1. Verify coverage >= 95% line, >= 90% branch.
2. Run `npx tsc --noEmit` for type checking.
3. Run full test suite -- no regressions.

---

## 6. Test Helper Reuse

All existing factory functions from `tests/domain/implementation-map/helpers.ts` are reused directly:

- `createDagNode(overrides)` -- create `DagNode` instances for `parsedMap.stories`
- `createParsedMap(overrides)` -- create `ParsedMap` with custom phases and stories
- `createExecutionState(stories)` -- create `ExecutionState` with story status records

No new helpers are needed. The existing factories accept the same `ParsedMap` and `ExecutionState` types that the new functions consume.

---

## 7. Coverage Strategy

| File | Expected Lines | Expected Branches | Strategy |
|:---|:---|:---|:---|
| `partial-execution.ts` | 100% | 100% | UT-01 through UT-22 cover every branch: both flags, single flag, no flag, phase 0 (no prior phases), phase N (iteration), invalid phase (above max, negative, at max), story not found, no deps, all deps satisfied, partial deps unsatisfied, dep missing from state. |
| `types.ts` (additions) | 100% | N/A | Type-only additions. `PartialExecutionMode` union and `PrerequisiteResult` interface have no logic. |
| `exceptions.ts` (addition) | 100% | N/A | `PartialExecutionError` constructor tested via throw/catch in UT-04, UT-05. |
| `index.ts` (addition) | 100% | N/A | Re-export line covered by any import in tests. |

**Total new test count:** 7 acceptance + 22 unit + 6 integration/content = **35 test scenarios**

---

## 8. Parallelism and Execution

All tests are marked `Parallel: yes` because:
- `parsePartialExecutionMode`, `validatePhasePrerequisites`, `validateStoryPrerequisites`, and `getStoriesForPhase` are **pure functions** (no shared state, no I/O, no side effects).
- Test data is created via factory functions per test (no mutation of shared state).
- Content tests read files synchronously (read-only).
- No database, filesystem writes, or network calls.

Vitest runs all test files in parallel across forked workers (maxForks: 3).

**Estimated execution time:** < 2 seconds for all 35 tests (pure computation + file reads).

---

## 9. Traceability Matrix

| Gherkin Scenario | AT | Unit Tests (Inner Loop) | Integration Tests |
|:---|:---|:---|:---|
| Phase 2 with phases 0-1 complete | AT-01 | UT-01, UT-02, UT-07, UT-08, UT-09, UT-21 | -- |
| Phase N with prior phase incomplete | AT-02 | UT-10, UT-11 | -- |
| Story with deps satisfied | AT-03 | UT-03, UT-16, UT-17 | -- |
| Story with dep not satisfied | AT-04 | UT-18, UT-19, UT-20 | -- |
| `--phase` and `--story` mutually exclusive | AT-05 | UT-04, UT-05 | -- |
| `--phase` with invalid number | AT-06 | UT-12, UT-13, UT-14 | -- |
| `--story` with nonexistent ID | AT-07 | UT-15 | -- |
| SKILL.md updated with partial execution | -- | -- | IT-01 through IT-06 |

---

## 10. PartialExecutionError Test Assertions

The `PartialExecutionError` class (added to `src/exceptions.ts`) is tested indirectly through `parsePartialExecutionMode`. Specific assertions:

| Assertion | Test ID | Verification |
|:---|:---|:---|
| Error is instance of `PartialExecutionError` | UT-04 | `expect(() => ...).toThrow(PartialExecutionError)` |
| Error is instance of `Error` (inheritance) | UT-04 | `expect(error).toBeInstanceOf(Error)` |
| `code` field equals `MUTUAL_EXCLUSIVITY` | UT-04 | `expect(error.code).toBe("MUTUAL_EXCLUSIVITY")` |
| `context` field contains both flag values | UT-04 | `expect(error.context).toEqual({ phase: 2, storyId: "0042-0003" })` |
| `message` is human-readable | UT-05 | `expect(error.message).toContain("mutually exclusive")` |
| `name` field equals `PartialExecutionError` | UT-04 | `expect(error.name).toBe("PartialExecutionError")` |
