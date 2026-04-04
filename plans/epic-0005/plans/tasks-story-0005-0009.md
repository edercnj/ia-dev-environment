# Task Breakdown -- story-0005-0009: Partial Execution (`--phase N`, `--story XXXX-YYYY`)

**Story:** `story-0005-0009.md`
**Implementation Plan:** `plan-story-0005-0009.md`
**Depends on:** story-0005-0005 (Core Loop)

---

## Summary

18 tasks organized into 7 groups (A through G) following TDD Red-Green-Refactor cycles.
Groups A-B are sequential (foundation types and error class). Groups C-E are **parallel** (three independent function pairs). Groups F-G are sequential (barrel export, SKILL.md templates, content tests, finalization).

**Legend:**
- **RED** = Write failing tests only (no production code)
- **GREEN** = Write minimum production code to make tests pass
- **REFACTOR** = Improve design without changing behavior
- **SETUP** = Infrastructure/scaffolding (no tests needed)

---

## Group A: Types

### TASK-1: Add partial execution types to types.ts
- **Type:** SETUP
- **File(s):** `src/domain/implementation-map/types.ts`
- **Description:** Append 5 new type definitions at the end of the file (after `MapParseError`):
  - `PhaseExecutionMode` interface: `{ readonly kind: "phase"; readonly phase: number }`
  - `StoryExecutionMode` interface: `{ readonly kind: "story"; readonly storyId: string }`
  - `FullExecutionMode` interface: `{ readonly kind: "full" }`
  - `PartialExecutionMode` discriminated union: `PhaseExecutionMode | StoryExecutionMode | FullExecutionMode`
  - `PrerequisiteResult` interface: `{ readonly valid: boolean; readonly error?: string; readonly unsatisfiedDeps?: readonly string[] }`
- **Depends On:** none
- **Parallel:** no
- **Acceptance Criteria:**
  - All 5 types are defined with `readonly` modifiers
  - `PartialExecutionMode` is a proper discriminated union on `kind`
  - `npx tsc --noEmit` passes
  - No new imports added (types are self-contained)

---

## Group B: Error Class

### TASK-2: Add PartialExecutionError to exceptions.ts
- **Type:** SETUP
- **File(s):** `src/exceptions.ts`
- **Description:** Add `PartialExecutionError` class extending `Error` with fields:
  - `readonly code: string` -- one of: `MUTUAL_EXCLUSIVITY`, `INVALID_PHASE`, `INCOMPLETE_PHASES`, `STORY_NOT_FOUND`, `UNSATISFIED_DEPS`
  - `readonly context: Readonly<Record<string, unknown>>` -- contextual values that caused the error
  - Constructor: `(message: string, code: string, context: Record<string, unknown>)`
  - Sets `this.name = "PartialExecutionError"`
- **Depends On:** TASK-1
- **Parallel:** no
- **Acceptance Criteria:**
  - Class extends `Error` and carries `code` and `context`
  - No imports from domain modules (standalone)
  - `npx tsc --noEmit` passes
  - Follows existing error class patterns in `src/exceptions.ts`

---

## Group C: `parsePartialExecutionMode` (TDD)

### TASK-3: RED -- Write failing tests for parsePartialExecutionMode()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/partial-execution.test.ts`
- **Description:** Create test file with `describe("parsePartialExecutionMode")` block. Write test cases following TPP (degenerate to complex):
  1. Neither flag provided returns `{ kind: "full" }` (degenerate -- no input)
  2. `phase=2, storyId=undefined` returns `{ kind: "phase", phase: 2 }` (simple -- one input)
  3. `phase=0, storyId=undefined` returns `{ kind: "phase", phase: 0 }` (boundary -- phase zero)
  4. `phase=undefined, storyId="0042-0003"` returns `{ kind: "story", storyId: "0042-0003" }` (simple -- other input)
  5. Both `phase=2` and `storyId="0042-0003"` provided throws `PartialExecutionError` with code `MUTUAL_EXCLUSIVITY` (error path)
- **Depends On:** TASK-2
- **Parallel:** no
- **Acceptance Criteria:**
  - 5 test cases exist
  - All tests fail (function not yet implemented)
  - Tests import from `../../../src/domain/implementation-map/partial-execution.js`
  - Mutual exclusivity test verifies both the error type and the error code

### TASK-4: GREEN -- Implement parsePartialExecutionMode()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/partial-execution.ts`
- **Description:** Create new file `src/domain/implementation-map/partial-execution.ts`. Implement `parsePartialExecutionMode(phase: number | undefined, storyId: string | undefined): PartialExecutionMode`:
  - If both `phase` and `storyId` are defined, throw `PartialExecutionError` with code `MUTUAL_EXCLUSIVITY` and message `"--phase and --story are mutually exclusive"`
  - If only `phase` is defined, return `{ kind: "phase", phase }`
  - If only `storyId` is defined, return `{ kind: "story", storyId }`
  - If neither is defined, return `{ kind: "full" }`
- **Depends On:** TASK-3
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-3 tests pass
  - Function imports only from `./types.js` and `../exceptions.js` (exception: `PartialExecutionError` import is allowed here since this is argument validation, not domain logic)
  - Function length <= 15 lines

---

## Group D: `validatePhasePrerequisites` + `getStoriesForPhase` (TDD)

### TASK-5: RED -- Write failing tests for validatePhasePrerequisites()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/partial-execution.test.ts`
- **Description:** Add `describe("validatePhasePrerequisites")` block. Write test cases following TPP:
  1. Phase 0 with any state returns `{ valid: true }` (degenerate -- no prior phases to check)
  2. Phase 2 with phases 0-1 all SUCCESS returns `{ valid: true }` (happy path)
  3. Phase 2 with phase 1 having a PENDING story returns `{ valid: false, error: "Phases 0..1 must be complete before phase 2" }` (one failure)
  4. Phase 2 with phase 0 SUCCESS but phase 1 having FAILED story returns `{ valid: false }` (different failure status)
  5. Phase 5 when max phase is 3 returns `{ valid: false, error: "Phase 5 does not exist. Max phase is 3." }` (boundary -- out of range)
  6. Phase 1 with phase 0 having mixed SUCCESS and IN_PROGRESS returns `{ valid: false }` (only SUCCESS counts as complete)
- **Depends On:** TASK-4
- **Parallel:** yes (parallel with TASK-7)
- **Acceptance Criteria:**
  - 6 test cases exist
  - All tests fail
  - Tests use `createParsedMap`, `createExecutionState`, `createDagNode` helpers from `./helpers.js`
  - Tests construct multi-phase maps with multiple stories per phase

### TASK-6: GREEN -- Implement validatePhasePrerequisites()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/partial-execution.ts`
- **Description:** Implement `validatePhasePrerequisites(phase: number, parsedMap: ParsedMap, executionState: ExecutionState): PrerequisiteResult`:
  - If `phase >= parsedMap.totalPhases`, return `{ valid: false, error: "Phase {phase} does not exist. Max phase is {max}." }`
  - If `phase === 0`, return `{ valid: true }` (no prior phases)
  - For phases 0..phase-1, collect all story IDs from `parsedMap.phases`
  - Check each story's status in `executionState.stories` -- must be `SUCCESS`
  - If any story is not SUCCESS, return `{ valid: false, error: "Phases 0..{phase-1} must be complete before phase {phase}" }`
  - Otherwise return `{ valid: true }`
- **Depends On:** TASK-5
- **Parallel:** yes (parallel with TASK-8)
- **Acceptance Criteria:**
  - All TASK-5 tests pass
  - Function is pure (no side effects)
  - Imports only from `./types.js`
  - Function length <= 25 lines

### TASK-7: RED -- Write failing tests for getStoriesForPhase()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/partial-execution.test.ts`
- **Description:** Add `describe("getStoriesForPhase")` block. Write test cases following TPP:
  1. Valid phase with single story returns `["0001"]` (simple)
  2. Valid phase with multiple stories returns all IDs (multiple items)
  3. Invalid phase (out of range) returns empty array (boundary)
  4. Phase 0 in a multi-phase map returns only phase 0 stories (selectivity)
- **Depends On:** TASK-4
- **Parallel:** yes (parallel with TASK-5)
- **Acceptance Criteria:**
  - 4 test cases exist
  - All tests fail
  - Tests use `createParsedMap` helper

### TASK-8: GREEN -- Implement getStoriesForPhase()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/partial-execution.ts`
- **Description:** Implement `getStoriesForPhase(phase: number, parsedMap: ParsedMap): readonly string[]`:
  - Look up `parsedMap.phases.get(phase)`
  - If found, return the story IDs array
  - If not found, return empty array `[]`
- **Depends On:** TASK-7
- **Parallel:** yes (parallel with TASK-6)
- **Acceptance Criteria:**
  - All TASK-7 tests pass
  - Function is pure
  - Imports only from `./types.js`
  - Function length <= 10 lines

---

## Group E: `validateStoryPrerequisites` (TDD)

### TASK-9: RED -- Write failing tests for validateStoryPrerequisites()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/partial-execution.test.ts`
- **Description:** Add `describe("validateStoryPrerequisites")` block. Write test cases following TPP:
  1. Story with no dependencies returns `{ valid: true }` (degenerate -- no deps)
  2. Story with all deps SUCCESS returns `{ valid: true }` (happy path)
  3. Story with one dep PENDING returns `{ valid: false, unsatisfiedDeps: ["0042-0002"] }` (single failure)
  4. Story with multiple unsatisfied deps lists all in `unsatisfiedDeps` (multiple failures)
  5. Story not found in map returns `{ valid: false, error: "Story 0042-9999 not found in implementation map" }` (boundary -- missing story)
  6. Story with dep FAILED (not just PENDING) returns `{ valid: false }` (only SUCCESS satisfies)
- **Depends On:** TASK-4
- **Parallel:** yes (parallel with TASK-5 and TASK-7)
- **Acceptance Criteria:**
  - 6 test cases exist
  - All tests fail
  - Tests construct DAGs with dependency relationships using helpers
  - Validates both `error` and `unsatisfiedDeps` fields in results

### TASK-10: GREEN -- Implement validateStoryPrerequisites()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/partial-execution.ts`
- **Description:** Implement `validateStoryPrerequisites(storyId: string, parsedMap: ParsedMap, executionState: ExecutionState): PrerequisiteResult`:
  - Look up `parsedMap.stories.get(storyId)` -- if not found, return `{ valid: false, error: "Story {storyId} not found in implementation map" }`
  - Get the story's `blockedBy` dependencies
  - If no dependencies, return `{ valid: true }`
  - Check each dependency's status in `executionState.stories` -- must be `SUCCESS`
  - Collect all unsatisfied dependency IDs
  - If any unsatisfied, return `{ valid: false, error: "Dependencies not satisfied: [{list}]", unsatisfiedDeps: [...] }`
  - Otherwise return `{ valid: true }`
- **Depends On:** TASK-9
- **Parallel:** yes (parallel with TASK-6 and TASK-8)
- **Acceptance Criteria:**
  - All TASK-9 tests pass
  - Function is pure
  - Imports only from `./types.js`
  - Function length <= 25 lines

### TASK-11: REFACTOR -- Partial execution functions cleanup
- **Type:** REFACTOR
- **File(s):** `src/domain/implementation-map/partial-execution.ts`
- **Description:** Review all 4 functions for:
  - Extract shared "check all stories have SUCCESS status" logic into a private helper if duplicated between `validatePhasePrerequisites` and `validateStoryPrerequisites`
  - Ensure no function exceeds 25 lines
  - Verify naming conventions follow Rule 03 (intent-revealing names, verbs for functions)
  - Ensure consistent `PrerequisiteResult` construction patterns
- **Depends On:** TASK-6, TASK-8, TASK-10
- **Parallel:** no
- **Acceptance Criteria:**
  - All existing tests still pass (no behavior change)
  - No function exceeds 25 lines
  - Shared status-checking logic extracted if duplicated
  - Code follows Rule 03 naming conventions

---

## Group F: Barrel Export + SKILL.md Templates

### TASK-12: Update index.ts barrel exports
- **Type:** SETUP
- **File(s):** `src/domain/implementation-map/index.ts`
- **Description:** Add re-export line: `export { parsePartialExecutionMode, validatePhasePrerequisites, validateStoryPrerequisites, getStoriesForPhase } from "./partial-execution.js";`. Place after the existing `getExecutableStories` export. Verify new types (`PartialExecutionMode`, `PrerequisiteResult`, etc.) are already re-exported via `export * from "./types.js"`.
- **Depends On:** TASK-11
- **Parallel:** no
- **Acceptance Criteria:**
  - All 4 functions are exported from the barrel
  - New types from `types.ts` are accessible via the barrel (already covered by `export *`)
  - `npx tsc --noEmit` passes
  - No naming conflicts with existing exports

### TASK-13: Add Partial Execution section to Claude SKILL.md
- **Type:** SETUP
- **File(s):** `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- **Description:** Add a new "## Partial Execution" section between "Prerequisites Check" and "Phase 0 -- Preparation". Content includes:
  - Mutual exclusivity rule: `--phase` and `--story` are mutually exclusive; error message if both provided
  - `--phase N` flow: validate phases 0..N-1 complete, filter stories to phase N, execute core loop, run integrity gate
  - `--story XXXX-YYYY` flow: validate all dependencies have SUCCESS status, dispatch single subagent, update checkpoint, NO integrity gate
  - Error message specifications for all 5 failure modes (mutual exclusivity, invalid phase, incomplete phases, story not found, unsatisfied deps)
  - Note that `--phase 0` requires no prerequisite validation (no prior phases)
- **Depends On:** TASK-12
- **Parallel:** no
- **Acceptance Criteria:**
  - Section is placed between Prerequisites Check and Phase 0
  - All 5 error message formats are documented
  - Both `--phase` and `--story` flows are described
  - Content includes keywords: `mutually exclusive`, `--phase`, `--story`, `integrity gate`

### TASK-14: Mirror Partial Execution section in GitHub SKILL.md
- **Type:** SETUP
- **File(s):** `resources/github-skills-templates/dev/x-dev-epic-implement.md`
- **Description:** Add equivalent "## Partial Execution" section to the GitHub template, mirroring the content added in TASK-13. Place between "Prerequisites Check" and "Phase 0 -- Preparation". Content must include the same keywords and error message formats to maintain dual-copy consistency (RULE-001).
- **Depends On:** TASK-13
- **Parallel:** no
- **Acceptance Criteria:**
  - Section is placed in the same relative position as the Claude template
  - All 5 error messages match the Claude template
  - Keywords `mutually exclusive`, `--phase`, `--story`, `integrity gate` are present
  - Dual-copy consistency maintained

---

## Group G: Content Tests + Finalization

### TASK-15: RED -- Write failing content tests for partial execution sections
- **Type:** RED
- **File(s):** `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Description:** Add a new `describe("x-dev-epic-implement SKILL.md -- partial execution")` block with assertions:
  1. Claude SKILL.md contains "Partial Execution" section heading
  2. Claude SKILL.md contains "mutually exclusive" keyword
  3. Claude SKILL.md contains all 5 error message patterns (one test each or parametrized):
     - `"--phase and --story are mutually exclusive"`
     - `"does not exist. Max phase is"`
     - `"must be complete before phase"`
     - `"not found in implementation map"`
     - `"Dependencies not satisfied"`
  4. Claude SKILL.md documents "no integrity gate" for single story mode

  Add to the existing dual-copy consistency `describe` block:
  5. Both templates contain `"mutually exclusive"`
  6. Both templates contain `"--story"` (already tested but verify in partial execution context)
  7. Both templates contain `"Partial Execution"`
- **Depends On:** TASK-14
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 7 new test assertions exist
  - All new tests fail (content not yet added -- but TASK-13/14 already added it, so these should pass)
  - Tests are in the appropriate describe blocks
  - Dual-copy terms are verified in both templates

### TASK-16: GREEN -- Verify content tests pass (adjust if needed)
- **Type:** GREEN
- **File(s):** `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Description:** Run the new content tests from TASK-15. Since TASK-13 and TASK-14 already added the SKILL.md content, the tests should pass. If any test fails due to keyword mismatch, adjust either the test assertion or the SKILL.md content to align. Ensure all existing content tests also still pass (no regression).
- **Depends On:** TASK-15
- **Parallel:** no
- **Acceptance Criteria:**
  - All new content tests pass
  - All existing content tests still pass (no regression)
  - Dual-copy consistency verified for partial execution terms

### TASK-17: Coverage validation
- **Type:** SETUP
- **File(s):** (no new files)
- **Description:** Run `npx vitest run --coverage` scoped to `src/domain/implementation-map/partial-execution.ts`. Verify:
  - Line coverage >= 95%
  - Branch coverage >= 90%
  If thresholds are not met, identify uncovered branches and add targeted tests.
- **Depends On:** TASK-16
- **Parallel:** yes (parallel with TASK-18)
- **Acceptance Criteria:**
  - Line coverage >= 95% for `partial-execution.ts`
  - Branch coverage >= 90% for `partial-execution.ts`
  - Coverage report generated

### TASK-18: Type checking and final validation
- **Type:** SETUP
- **File(s):** (no new files)
- **Description:** Run `npx tsc --noEmit` to verify entire project compiles. Verify:
  - Zero compiler errors and warnings
  - All existing tests pass (`npx vitest run`)
  - No imports in `partial-execution.ts` violate dependency direction (only `./types.js` and optionally `../exceptions.js` for `parsePartialExecutionMode`)
  - `PartialExecutionError` in `src/exceptions.ts` has no domain imports
- **Depends On:** TASK-16
- **Parallel:** yes (parallel with TASK-17)
- **Acceptance Criteria:**
  - `npx tsc --noEmit` exits with code 0
  - Full test suite passes
  - No dependency direction violations
  - `partial-execution.ts` imports verified clean

---

## Dependency Graph

```
TASK-1 → TASK-2 → TASK-3 → TASK-4 ──┬── TASK-5 → TASK-6 ──┐
                                      ├── TASK-7 → TASK-8 ──┤
                                      └── TASK-9 → TASK-10 ─┤
                                                             ↓
                                                          TASK-11
                                                             ↓
                                   TASK-12 → TASK-13 → TASK-14 → TASK-15 → TASK-16 ──┬── TASK-17
                                                                                       └── TASK-18
```

Tasks 5/6, 7/8, and 9/10 form three **parallel tracks** after TASK-4. All three converge at TASK-11 (refactor). After TASK-11, the chain is sequential through barrel export, templates, and content tests. TASK-17 and TASK-18 are parallelizable at the end.

---

## Traceability Matrix

| Gherkin Scenario | Tasks |
|-----------------|-------|
| Phase 2 with phases 0-1 complete | TASK-5, TASK-6 |
| Phase N with prior phase incomplete | TASK-5, TASK-6 |
| Story with deps satisfied | TASK-9, TASK-10 |
| Story with dep not satisfied | TASK-9, TASK-10 |
| `--phase` and `--story` mutually exclusive | TASK-3, TASK-4 |
| `--phase` with invalid number | TASK-5, TASK-6 |
| `--story` with nonexistent ID | TASK-9, TASK-10 |
| SKILL.md updated with partial execution | TASK-13, TASK-14, TASK-15, TASK-16 |
| No integrity gate for single story | TASK-13, TASK-14, TASK-15 |

---

## Estimated Effort

| Group | Tasks | Estimated Lines (prod) | Estimated Lines (test) |
|-------|-------|----------------------|----------------------|
| A: Types | 1 | ~20 | 0 |
| B: Error Class | 2 | ~12 | 0 |
| C: parsePartialExecutionMode | 3-4 | ~15 | ~40 |
| D: validatePhasePrerequisites + getStoriesForPhase | 5-8 | ~35 | ~80 |
| E: validateStoryPrerequisites + Refactor | 9-11 | ~25 | ~50 |
| F: Barrel + SKILL.md Templates | 12-14 | ~5 (barrel) + ~80 (templates) | 0 |
| G: Content Tests + Finalization | 15-18 | 0 | ~30 |
| **Total** | **18** | **~192** | **~200** |
