# Task Breakdown -- story-0005-0014: E2E Tests + Generator Integration

**Story:** `story-0005-0014.md`
**Implementation Plan:** `plan-story-0005-0014.md`
**Depends on:** stories 0005-0005 through 0005-0013 (all complete)

---

## Summary

20 tasks organized into 6 groups (G1 through G6) following TDD Red-Green-Refactor cycles. Groups G1 and G2 are **parallel** (test infrastructure with no shared state). Group G3 depends on both G1 and G2 (scenario runner composes mock + map). Group G4 (E2E scenarios) depends on G3 and follows TPP order. Groups G5 and G6 run after G4.

**Legend:**
- **RED** = Write failing tests only (no production code)
- **GREEN** = Write minimum production code to make tests pass
- **REFACTOR** = Improve design without changing behavior
- **E2E** = Integration/end-to-end test scenario
- **DOC** = Documentation update
- **VERIFY** = Validation and coverage check

---

## Group G1: Test Infrastructure -- Mock Subagent

### TASK-01: [RED] Write unit test for createMockDispatch with single result config
- **Type:** RED
- **File(s):** `tests/node/e2e/helpers/mock-subagent.test.ts`
- **Description:** Create test file with `describe("createMockDispatch")` block. Write test cases following TPP:
  1. Single story configured with SUCCESS result returns that result when dispatched (degenerate -- single key)
  2. Unconfigured story uses `defaultResult` when provided (fallback)
  3. Unconfigured story with no `defaultResult` throws an error (missing config)
  4. Call log records storyId and attempt number for each dispatch (observability)
  5. Dispatch returns correct result per storyId (multiple keys, selectivity)
- **Depends On:** none
- **Parallel:** yes (parallel with TASK-06, TASK-07, TASK-08)
- **Complexity:** LOW
- **Acceptance Criteria:**
  - 5 test cases exist
  - All tests fail (function not yet implemented)
  - Tests import `SubagentResult` from `src/checkpoint/types.ts`
  - Tests verify both the returned result and the call log state

### TASK-02: [GREEN] Implement mock-subagent.ts with configurable per-story results
- **Type:** GREEN
- **File(s):** `tests/node/e2e/helpers/mock-subagent.ts`
- **Description:** Create `mock-subagent.ts` exporting `createMockDispatch(config: MockSubagentConfig)`. Implementation:
  - `MockSubagentConfig` interface: `{ results: Record<string, SubagentResult | SubagentResult[]>; defaultResult?: SubagentResult }`
  - `DispatchFn` type: `(storyId: string) => SubagentResult`
  - Returns `{ dispatch: DispatchFn, callLog: Array<{ storyId: string; attempt: number }> }`
  - For single `SubagentResult` values, always return that result
  - For array values, consume sequentially by attempt index (retry support -- implemented in TASK-04)
  - If storyId not found and `defaultResult` exists, use it
  - If storyId not found and no `defaultResult`, throw `Error("No mock result configured for story: {storyId}")`
  - Track every call in `callLog` with incrementing attempt counter per storyId
- **Depends On:** TASK-01
- **Parallel:** no
- **Complexity:** LOW
- **Acceptance Criteria:**
  - All TASK-01 tests pass
  - `callLog` tracks every dispatch call with correct attempt numbers
  - Function is pure (no side effects beyond internal state tracking)
  - File length <= 80 lines

### TASK-03: [RED] Write unit test for retry sequences (array results consumed sequentially)
- **Type:** RED
- **File(s):** `tests/node/e2e/helpers/mock-subagent.test.ts`
- **Description:** Add `describe("retry sequences")` block. Write test cases:
  1. Array of 3 results: first dispatch returns index 0, second returns index 1, third returns index 2 (sequential consumption)
  2. Array of 2 results (FAILED, SUCCESS): simulates retry scenario where first attempt fails and second succeeds
  3. Array exhausted: dispatching beyond array length returns the last element (safe fallback)
  4. Call log shows correct attempt numbers (1, 2, 3) for same storyId across retries
- **Depends On:** TASK-02
- **Parallel:** no
- **Complexity:** LOW
- **Acceptance Criteria:**
  - 4 test cases exist
  - Tests verify sequential consumption of array entries
  - Tests verify call log attempt numbering matches retry sequence

### TASK-04: [GREEN] Implement retry sequence support in mock dispatch
- **Type:** GREEN
- **File(s):** `tests/node/e2e/helpers/mock-subagent.ts`
- **Description:** Extend `createMockDispatch` to handle array-type results:
  - Maintain per-storyId attempt counter (internal `Map<string, number>`)
  - When result is an array, use `attempt - 1` as index (0-based)
  - If index exceeds array length, return last element (clamp to `array.length - 1`)
  - Attempt counter increments on each call for the same storyId
- **Depends On:** TASK-03
- **Parallel:** no
- **Complexity:** LOW
- **Acceptance Criteria:**
  - All TASK-03 tests pass
  - All TASK-01 tests still pass (no regression)
  - Array results consumed in order: attempt 1 -> index 0, attempt 2 -> index 1, etc.

### TASK-05: [REFACTOR] Clean up mock-subagent module
- **Type:** REFACTOR
- **File(s):** `tests/node/e2e/helpers/mock-subagent.ts`
- **Description:** Review implementation for:
  - Extract `MockSubagentConfig` and `DispatchFn` types to module-level exports
  - Ensure all type annotations are explicit (no inferred `any`)
  - Verify JSDoc comment on `createMockDispatch` explains the retry array convention
  - Confirm no function exceeds 25 lines
  - Verify naming follows Rule 03 conventions
- **Depends On:** TASK-04
- **Parallel:** no
- **Complexity:** LOW
- **Acceptance Criteria:**
  - All existing tests still pass
  - Types are exported for use by scenario-runner
  - Module is self-contained with no external dependencies beyond `SubagentResult` type import

---

## Group G2: Test Infrastructure -- Mini Implementation Map

### TASK-06: [RED] Write unit test for mini-implementation-map builder (5 stories, 3 phases)
- **Type:** RED
- **File(s):** `tests/node/e2e/helpers/mini-implementation-map.test.ts`
- **Description:** Create test file with `describe("buildMiniImplementationMap")` block. Write test cases:
  1. Generated markdown contains all 5 story IDs: `story-0001` through `story-0005` (completeness)
  2. Generated markdown contains 3 phase sections: Phase 0, Phase 1, Phase 2 (structure)
  3. Dependency matrix reflects the DAG: story-0001 has no deps, story-0002 blocked by story-0001, story-0003 blocked by story-0001, story-0004 blocked by story-0002 and story-0003, story-0005 blocked by story-0003 (correctness)
  4. Generated markdown is valid (parseable by `parseImplementationMap` without errors) (compatibility)
  5. Phase assignment is correct: phase 0 has story-0001, phase 1 has story-0002 and story-0003, phase 2 has story-0004 and story-0005 (phase distribution)
- **Depends On:** none
- **Parallel:** yes (parallel with TASK-01, TASK-07, TASK-08)
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - 5 test cases exist
  - All tests fail (function not yet implemented)
  - Tests import `parseImplementationMap` from `src/domain/implementation-map/markdown-parser.ts` for compatibility assertion
  - DAG structure exercises fan-out, fan-in, and transitive dependency chains

### TASK-07: [GREEN] Implement mini-implementation-map.ts
- **Type:** GREEN
- **File(s):** `tests/node/e2e/helpers/mini-implementation-map.ts`
- **Description:** Create module exporting `buildMiniImplementationMap(epicId?: string): string`. Implementation:
  - Default `epicId` to `"epic-test"`
  - Generate valid IMPLEMENTATION-MAP.md markdown with:
    - Epic metadata section (epic ID, story count)
    - Dependency matrix table: 5 rows with correct `Blocked By` and `Blocks` columns
    - Phase summary table: 3 phases with story assignments
  - DAG structure:
    ```
    Phase 0: story-0001 (no deps)
    Phase 1: story-0002 (blocked by story-0001), story-0003 (blocked by story-0001)
    Phase 2: story-0004 (blocked by story-0002, story-0003), story-0005 (blocked by story-0003)
    ```
  - Output must parse successfully with `parseImplementationMap`
- **Depends On:** TASK-06
- **Parallel:** no
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - All TASK-06 tests pass
  - Generated markdown matches the format expected by `parseImplementationMap`
  - Function is pure (returns a string, no I/O)
  - File length <= 100 lines

### TASK-08: [RED] Write test validating parser compatibility (parseImplementationMap on generated map)
- **Type:** RED
- **File(s):** `tests/node/e2e/helpers/mini-implementation-map.test.ts`
- **Description:** Add `describe("parser compatibility")` block. Write deeper validation tests:
  1. `parseImplementationMap` on generated map returns correct `totalPhases` count (3)
  2. Parsed phases map has correct story count per phase (phase 0: 1, phase 1: 2, phase 2: 2)
  3. Parsed dependency rows have correct `blockedBy` arrays for each story
  4. `buildDag` on parsed rows produces valid DAG nodes with correct edge counts
  5. `computePhases` on the DAG produces phases matching the expected structure
- **Depends On:** none
- **Parallel:** yes (parallel with TASK-01, TASK-06)
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - 5 test cases exist
  - Tests use real parser and DAG builder functions (not mocks)
  - Tests verify the full parse pipeline: markdown -> parsed rows -> DAG -> phases

### TASK-09: [GREEN] Ensure generated map passes real parser validation
- **Type:** GREEN
- **File(s):** `tests/node/e2e/helpers/mini-implementation-map.ts`
- **Description:** Adjust `buildMiniImplementationMap` output if any TASK-08 tests fail. Fine-tune markdown format to ensure:
  - Dependency matrix table aligns with `parseImplementationMap` expected format
  - Phase summary table matches expected section markers
  - All parsed fields (blockedBy, blocks, phases) are correctly populated
- **Depends On:** TASK-07, TASK-08
- **Parallel:** no
- **Complexity:** LOW
- **Acceptance Criteria:**
  - All TASK-08 tests pass
  - All TASK-06 tests still pass
  - No regressions in parser compatibility

---

## Group G3: Test Infrastructure -- Scenario Runner

### TASK-10: [RED] Write unit test for scenario runner setup (temp dir, map writing)
- **Type:** RED
- **File(s):** `tests/node/e2e/helpers/scenario-runner.test.ts`
- **Description:** Create test file with `describe("ScenarioRunner")` block. Write test cases:
  1. `setup()` creates temp directory and writes implementation map file (I/O)
  2. `setup()` parses the map and returns valid `ParsedMap` with correct story count (composition)
  3. `run()` with all-SUCCESS mock returns `ExecutionState` with 5/5 SUCCESS stories (happy path integration)
  4. `run()` with dry-run mode returns plan output without dispatching (mode support)
  5. `run()` captures output lines from progress reporter (observability)
  6. `teardown()` cleans up temp directory (cleanup)
- **Depends On:** TASK-05, TASK-09
- **Parallel:** no
- **Complexity:** HIGH
- **Acceptance Criteria:**
  - 6 test cases exist
  - All tests fail (runner not yet implemented)
  - Tests verify both I/O setup (temp dir, file writes) and orchestration output
  - Tests use mock-subagent from G1 and mini-implementation-map from G2

### TASK-11: [GREEN] Implement scenario-runner.ts with orchestration loop
- **Type:** GREEN
- **File(s):** `tests/node/e2e/helpers/scenario-runner.ts`
- **Description:** Create module exporting `ScenarioRunner` class (or functional equivalent). Implementation:
  - `setup(epicId?: string)`: create temp dir via `fs.mkdtemp`, write mini implementation map, parse map, build DAG, compute phases
  - `run(options)`: execute orchestration loop composing real modules:
    - Create checkpoint via `CheckpointEngine.createCheckpoint`
    - For each phase: get executable stories, dispatch via mock, handle retry/block, update checkpoint, run integrity gate
    - Support options: `{ mockConfig, resume?, phase?, dryRun?, parallel? }`
    - For `dryRun`: use `DryRunPlanner` and `DryRunFormatter`, skip dispatch
    - For `resume`: load existing checkpoint via `resumeFromCheckpoint`
    - For `phase`: filter to specified phase via `getStoriesForPhase` + `validatePhasePrerequisites`
    - Capture all output via `WriteFn` pattern (array of strings)
  - `teardown()`: remove temp directory
  - Return type: `{ state: ExecutionState, output: string[], callLog: Array<{ storyId: string; attempt: number }> }`
- **Depends On:** TASK-10
- **Parallel:** no
- **Complexity:** HIGH
- **Acceptance Criteria:**
  - All TASK-10 tests pass
  - Runner composes real modules (no mocks except subagent dispatch)
  - Supports all 5 execution modes: full, dry-run, resume, phase, parallel
  - File length <= 200 lines
  - Uses same I/O patterns as existing checkpoint acceptance tests

### TASK-12: [REFACTOR] Extract common patterns and simplify
- **Type:** REFACTOR
- **File(s):** `tests/node/e2e/helpers/scenario-runner.ts`
- **Description:** Review scenario runner for:
  - Extract phase iteration loop into a private helper function
  - Extract story dispatch + retry logic into a private helper function
  - Ensure no function exceeds 25 lines
  - Verify type safety: `ExecutionState` from `checkpoint/types.ts`, `ParsedMap` from `implementation-map/types.ts`
  - Add JSDoc describing how each execution mode is selected
  - Ensure cleanup is robust (try/finally pattern for teardown)
- **Depends On:** TASK-11
- **Parallel:** no
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - All existing tests still pass
  - No function exceeds 25 lines
  - Cleanup is guaranteed even on test failures (try/finally)
  - Type imports are clean (no cross-boundary violations)

---

## Group G4: E2E Scenarios (TPP Order)

### TASK-13: [E2E] Dry-run path -- no execution, plan displayed
- **Type:** E2E
- **File(s):** `tests/node/e2e/orchestrator-e2e.test.ts`
- **Description:** Create E2E test file with `describe("Orchestrator E2E", { timeout: 30000 })`. Add `describe("dry-run path")`:
  1. Mock dispatch is NEVER called (callLog length is 0)
  2. No checkpoint file created in temp directory
  3. Output contains formatted plan with all 5 story IDs
  4. Output contains all 3 phase numbers (0, 1, 2)
  5. Output contains dependency information for each story
  6. Plan mode is "full" (no phase/story filter applied)
- **Depends On:** TASK-12
- **Parallel:** no
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - 6 assertions pass
  - Zero subagent dispatches
  - Plan output contains story IDs, phases, and dependencies
  - Test uses `ScenarioRunner` with `{ dryRun: true }` option
  - Follows TPP: degenerate case first (no execution at all)

### TASK-14: [E2E] Happy path -- 5/5 SUCCESS
- **Type:** E2E
- **File(s):** `tests/node/e2e/orchestrator-e2e.test.ts`
- **Description:** Add `describe("happy path")`:
  1. Final `ExecutionState` has 5/5 stories with status SUCCESS
  2. 3 integrity gates (one per phase) with status PASS
  3. Metrics: `storiesCompleted: 5`, `storiesTotal: 5`, `storiesFailed: 0`, `storiesBlocked: 0`
  4. Stories executed in phase order: phase 0 first, then phase 1, then phase 2
  5. Mock dispatch called exactly 5 times (one per story)
  6. Progress output contains PHASE_START, STORY_COMPLETE, GATE_RESULT, EPIC_COMPLETE events
  7. Critical path stories dispatched first within each phase
- **Depends On:** TASK-13
- **Parallel:** no
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - 7 assertions pass
  - All stories complete with SUCCESS status
  - Phase order and critical path priority verified via callLog
  - Integrity gates recorded for all 3 phases

### TASK-15: [E2E] Failure path -- retry + block propagation
- **Type:** E2E
- **File(s):** `tests/node/e2e/orchestrator-e2e.test.ts`
- **Description:** Add `describe("failure path")`. Mock config:
  - story-0001: array of 3 FAILED results (exhausts MAX_RETRIES=2: initial + 2 retries)
  - All other stories: SUCCESS (but should never be dispatched due to blocking)
  Assertions:
  1. story-0001: status FAILED, retries: 2
  2. story-0002, story-0003, story-0004, story-0005: status BLOCKED (transitive block from root)
  3. Each blocked story has `blockedBy` array containing the blocking chain
  4. Metrics: `storiesFailed: 1`, `storiesBlocked: 4`
  5. Mock dispatch called exactly 3 times for story-0001 (initial + 2 retries)
  6. Mock dispatch NOT called for stories 0002-0005 (blocked before dispatch)
  7. Progress output contains RETRY and BLOCK events
- **Depends On:** TASK-14
- **Parallel:** no
- **Complexity:** HIGH
- **Acceptance Criteria:**
  - 7 assertions pass
  - Retry budget exhausted correctly (MAX_RETRIES=2)
  - Transitive block propagation verified: story-0001 failure blocks all 4 downstream stories
  - Call log confirms exactly 3 dispatches total

### TASK-16: [E2E] Resume path -- continue from checkpoint
- **Type:** E2E
- **File(s):** `tests/node/e2e/orchestrator-e2e.test.ts`
- **Description:** Add `describe("resume path")`. Setup:
  - Pre-create checkpoint with story-0001 SUCCESS, story-0002 IN_PROGRESS, story-0003/0004/0005 PENDING
  - Mock config: all remaining stories return SUCCESS
  Assertions:
  1. story-0001 NOT re-dispatched (already SUCCESS in checkpoint)
  2. story-0002 reclassified from IN_PROGRESS to PENDING, then dispatched
  3. story-0003, story-0004, story-0005 dispatched in phase order
  4. Final state: 5/5 SUCCESS
  5. Mock dispatch called exactly 4 times (story-0002 through story-0005)
  6. Reclassification entries recorded for story-0002
- **Depends On:** TASK-15
- **Parallel:** no
- **Complexity:** HIGH
- **Acceptance Criteria:**
  - 6 assertions pass
  - Already-completed stories are NOT re-dispatched
  - IN_PROGRESS stories are reclassified to PENDING before dispatch
  - Resume uses existing checkpoint file (not a fresh one)

### TASK-17: [E2E] Partial path -- --phase 2 only
- **Type:** E2E
- **File(s):** `tests/node/e2e/orchestrator-e2e.test.ts`
- **Description:** Add `describe("partial path")`. Setup:
  - Pre-create checkpoint with phases 0 and 1 all SUCCESS (story-0001, story-0002, story-0003)
  - Execute with `{ phase: 2 }` mode
  - Mock config: phase 2 stories (story-0004, story-0005) return SUCCESS
  Assertions:
  1. Only story-0004 and story-0005 dispatched (phase 2 stories)
  2. story-0001, story-0002, story-0003 NOT dispatched (phases 0-1 already complete)
  3. Integrity gate recorded for phase 2 only
  4. Mock dispatch called exactly 2 times
  5. Phase prerequisite validation passes (phases 0-1 complete)
- **Depends On:** TASK-16
- **Parallel:** no
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - 5 assertions pass
  - Only specified phase stories are dispatched
  - Prerequisite validation confirms prior phases are complete
  - Pre-existing checkpoint entries are preserved unchanged

### TASK-18: [E2E] Parallel path -- parallel mock dispatch
- **Type:** E2E
- **File(s):** `tests/node/e2e/orchestrator-e2e.test.ts`
- **Description:** Add `describe("parallel path")`. Setup:
  - Execute with `{ parallel: true }` mode
  - Mock config: all stories return SUCCESS
  Assertions:
  1. Within each phase, independent stories are identified as parallelizable (phase 1: story-0002 and story-0003 are independent)
  2. Mock dispatch called for all 5 stories
  3. Final state: 5/5 SUCCESS
  4. Progress output contains parallel execution indicators
  5. Stories within same phase can be dispatched concurrently (verified via call log ordering)
- **Depends On:** TASK-17
- **Parallel:** no
- **Complexity:** MEDIUM
- **Acceptance Criteria:**
  - 5 assertions pass
  - Parallel mode flag propagated correctly through orchestration
  - Independent stories within a phase are treated as parallelizable
  - All stories complete regardless of execution mode

---

## Group G5: Documentation

### TASK-19: [DOC] Update CLAUDE.md with x-dev-epic-implement skill entry
- **Type:** DOC
- **File(s):** `CLAUDE.md`
- **Description:** Add `x-dev-epic-implement` to the skills index table in the `CLAUDE.md` file at the project root. Entry format matches existing skill entries:
  - Skill name: `x-dev-epic-implement`
  - Path: `/x-dev-epic-implement`
  - Description: Orchestrates full epic implementation from implementation map through phased execution, checkpoint management, integrity gates, and progress reporting. Supports dry-run, resume, partial execution, and parallel modes.
- **Depends On:** TASK-18
- **Parallel:** no
- **Complexity:** LOW
- **Acceptance Criteria:**
  - `x-dev-epic-implement` appears in the skills table
  - Table formatting is consistent with existing entries
  - Skill count in the summary table is updated accordingly

---

## Group G6: Verification

### TASK-20: [VERIFY] Run full test suite + coverage validation
- **Type:** VERIFY
- **File(s):** (no new files)
- **Description:** Run full validation suite:
  1. `npx tsc --noEmit` -- zero compiler errors
  2. `npx vitest run` -- all tests pass (existing + new E2E tests)
  3. `npx vitest run --coverage` -- verify coverage thresholds:
     - Line coverage >= 95%
     - Branch coverage >= 90%
  4. Verify existing golden file tests still pass (`byte-for-byte.test.ts`)
  5. Verify E2E test execution time < 30s total (performance budget from DoD)
  6. Verify no dependency direction violations in new test helper files
- **Depends On:** TASK-19
- **Parallel:** no
- **Complexity:** LOW
- **Acceptance Criteria:**
  - Zero compiler errors
  - Full test suite passes
  - Coverage >= 95% line, >= 90% branch
  - Golden file tests pass (no regression)
  - E2E tests complete within 30s budget
  - All DoD criteria from story-0005-0014 satisfied

---

## Dependency Graph

```
TASK-01 → TASK-02 → TASK-03 → TASK-04 → TASK-05 ──┐
                                                      ├── TASK-10 → TASK-11 → TASK-12 ──┐
TASK-06 → TASK-07 ──┐                                │                                   │
                     ├── TASK-09 ─────────────────────┘                                   │
TASK-08 ────────────┘                                                                     │
                                                                                          ↓
                     TASK-13 → TASK-14 → TASK-15 → TASK-16 → TASK-17 → TASK-18 → TASK-19 → TASK-20
```

**Parallel tracks:**
- **Track A** (G1): TASK-01 through TASK-05 (mock subagent)
- **Track B** (G2): TASK-06 through TASK-09 (mini implementation map)
- Tracks A and B are **fully parallel** (no shared state)
- TASK-06 and TASK-08 are parallel within Track B (both RED tests, independent)
- Tracks converge at TASK-10 (scenario runner depends on both mock and map)
- TASK-13 through TASK-18 are **sequential** (TPP order: degenerate -> happy -> failure -> resume -> partial -> parallel)

---

## Traceability Matrix

| Gherkin Scenario | Tasks |
|-----------------|-------|
| E2E dry-run -- shows plan without executing | TASK-13 |
| E2E happy path -- all stories SUCCESS | TASK-14 |
| E2E failure path -- retry + block propagation | TASK-15 |
| E2E resume path -- continues from checkpoint | TASK-16 |
| E2E partial path -- `--phase` executes specific phase | TASK-17 |
| Parallel path -- parallel mock dispatch | TASK-18 |
| Golden file test -- SKILL.md byte-for-byte | TASK-20 (verified via existing `byte-for-byte.test.ts`) |
| Golden file test -- templates byte-for-byte | TASK-20 (verified via existing `byte-for-byte.test.ts`) |
| Generator integration -- skill included in dual copy | TASK-20 (verified via existing golden file tests) |
| CLAUDE.md updated with skill entry | TASK-19 |

---

## Estimated Effort

| Group | Tasks | Estimated Lines (test) | Estimated Lines (helper/prod) | Complexity |
|-------|-------|----------------------|------------------------------|-----------|
| G1: Mock Subagent | 01-05 | ~50 | ~70 | LOW |
| G2: Mini Implementation Map | 06-09 | ~70 | ~90 | MEDIUM |
| G3: Scenario Runner | 10-12 | ~60 | ~180 | HIGH |
| G4: E2E Scenarios | 13-18 | ~400 | 0 | HIGH |
| G5: Documentation | 19 | 0 | ~5 (CLAUDE.md) | LOW |
| G6: Verification | 20 | 0 | 0 | LOW |
| **Total** | **20** | **~580** | **~345** | -- |

**Total estimated new lines: ~925**

---

## Risk Notes

1. **Type divergence:** `dry-run/types.ts` defines its own `ParsedMap` stub that differs from `implementation-map/types.ts`. The dry-run scenario (TASK-13) should use dry-run planner types directly. All other scenarios use `checkpoint/types.ts` for `ExecutionState` and `implementation-map/types.ts` for DAG operations.

2. **Memory pressure:** E2E tests use mock dispatch (no real subagent processes). Pool config `maxForks: 3` is adequate. No special memory considerations needed.

3. **Test isolation:** Each E2E scenario uses its own temp directory created by `ScenarioRunner.setup()`. No shared state between scenarios. Cleanup guaranteed via `afterEach` + try/finally.

4. **Performance budget:** All 6 E2E scenarios must complete within 30s total (story DoD). Mock dispatch is synchronous -- no timing concerns.
