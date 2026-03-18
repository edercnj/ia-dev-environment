# Test Plan -- story-0005-0014: E2E Tests + Generator Integration

## Summary

- Total test files: 4 (1 E2E suite + 3 test helpers)
- Total test methods: ~42 (estimated)
- Categories covered: AT (Acceptance/E2E), UT (Unit -- test infrastructure), IT (Integration -- module composition)
- Estimated line coverage: >= 95%
- Estimated branch coverage: >= 90%
- Performance budget: < 30s total (all scenarios use mock dispatch, no real subagent calls)

## TPP Order (Transformation Priority Premise)

Tests follow TPP progression:
1. Degenerate case (dry-run -- no execution, no side effects)
2. Happy path (all stories SUCCESS -- simplest real execution)
3. Failure path (retry + block propagation -- conditional branching)
4. Resume path (pre-populated checkpoint -- collection iteration)
5. Partial path (--phase filter -- conditional + iteration)
6. Parallel path (--parallel flag -- derived computation)

---

## 1. Test Infrastructure -- Unit Tests

Before E2E scenarios, the test helpers themselves need validation.
These UT tests ensure the test infrastructure is correct before being used in E2E scenarios.

### 1.1 Test File: `tests/node/e2e/helpers/mock-subagent.ts`

Module under test: Mock subagent dispatch factory.

| # | ID | Test Name | Description | TPP Level | Parallel |
|---|-----|-----------|-------------|-----------|----------|
| 1 | UT-01 | `createMockDispatch_emptyConfig_throwsOnUnknownStory` | Degenerate: no results configured, no defaultResult, dispatch unknown storyId throws | L1 (nil) | yes |
| 2 | UT-02 | `createMockDispatch_defaultResultProvided_returnsDefaultForUnknownStory` | Default result returned when storyId not in config | L2 (constant) | yes |
| 3 | UT-03 | `createMockDispatch_singleResultForStory_returnsConfiguredResult` | Single `SubagentResult` mapped to storyId | L3 (variable) | yes |
| 4 | UT-04 | `createMockDispatch_arrayOfResults_returnsSequentially` | Array of results consumed in order (attempt 1 -> index 0, attempt 2 -> index 1) | L5 (collection) | yes |
| 5 | UT-05 | `createMockDispatch_arrayExhausted_returnsLastElement` | When array is exhausted, last element is returned for subsequent calls | L6 (edge) | yes |
| 6 | UT-06 | `createMockDispatch_callLog_recordsStoryIdAndAttempt` | `callLog` accumulates `{ storyId, attempt }` for each dispatch call | L4 (data) | yes |
| 7 | UT-07 | `createMockDispatch_multipleStories_isolatesCallCounts` | Calling dispatch for storyA does not affect attempt counter for storyB | L5 (collection) | yes |

**Interface contract:**

```typescript
interface MockSubagentConfig {
  results: Record<string, SubagentResult | SubagentResult[]>;
  defaultResult?: SubagentResult;
}

type DispatchFn = (storyId: string) => SubagentResult;

function createMockDispatch(config: MockSubagentConfig): {
  dispatch: DispatchFn;
  callLog: Array<{ storyId: string; attempt: number }>;
}
```

### 1.2 Test File: `tests/node/e2e/helpers/mini-implementation-map.ts`

Module under test: Synthetic 5-story, 3-phase implementation map builder.

| # | ID | Test Name | Description | TPP Level | Parallel |
|---|-----|-----------|-------------|-----------|----------|
| 1 | UT-08 | `buildMiniMap_defaultConfig_returnsParsableMarkdown` | Output passes `parseImplementationMap` without error | L2 (constant) | yes |
| 2 | UT-09 | `buildMiniMap_defaultConfig_hasExactlyFiveStories` | `parsedMap.stories.size === 5` | L3 (variable) | yes |
| 3 | UT-10 | `buildMiniMap_defaultConfig_hasThreePhases` | `parsedMap.totalPhases === 3` (phases 0, 1, 2) | L3 (variable) | yes |
| 4 | UT-11 | `buildMiniMap_phaseZero_containsOnlyStory0001` | Phase 0 = `[story-0001]` (root node) | L4 (data) | yes |
| 5 | UT-12 | `buildMiniMap_phaseOne_containsStories0002And0003` | Phase 1 = `[story-0002, story-0003]` (fan-out) | L4 (data) | yes |
| 6 | UT-13 | `buildMiniMap_phaseTwo_containsStories0004And0005` | Phase 2 = `[story-0004, story-0005]` (fan-in + tail) | L4 (data) | yes |
| 7 | UT-14 | `buildMiniMap_story0004_blockedByBothStory0002And0003` | Diamond dependency verified via `DagNode.blockedBy` | L5 (composition) | yes |
| 8 | UT-15 | `buildMiniMap_story0001_isRootWithNoDeps` | `story-0001.blockedBy === []` | L1 (constant) | yes |
| 9 | UT-16 | `buildMiniMap_criticalPath_containsAtLeastStory0001` | Critical path includes the root node | L4 (data) | yes |

**DAG structure:**

```
Phase 0: story-0001 (no deps)
Phase 1: story-0002 (blocked by story-0001), story-0003 (blocked by story-0001)
Phase 2: story-0004 (blocked by story-0002, story-0003), story-0005 (blocked by story-0003)
```

### 1.3 Test File: `tests/node/e2e/helpers/scenario-runner.ts`

Module under test: Orchestration harness wiring all modules.

| # | ID | Test Name | Description | TPP Level | Parallel |
|---|-----|-----------|-------------|-----------|----------|
| 1 | UT-17 | `createScenarioRunner_validConfig_returnsRunnerObject` | Factory returns `{ run, tmpDir }` | L2 (constant) | yes |
| 2 | UT-18 | `runScenario_emptyStories_returnsEmptyExecutionState` | Degenerate: zero stories, no dispatch calls | L1 (nil) | yes |
| 3 | UT-19 | `runScenario_resultShape_containsStateOutputAndCallLog` | Return type has `{ state, output, callLog }` fields | L3 (variable) | yes |

**Interface contract:**

```typescript
interface ScenarioConfig {
  mockConfig: MockSubagentConfig;
  mode?: "full" | "dry-run" | "resume" | "parallel";
  phase?: number;                              // for --phase N
  prePopulatedCheckpoint?: ExecutionState;     // for --resume
}

interface ScenarioResult {
  state: ExecutionState;
  output: string[];
  callLog: Array<{ storyId: string; attempt: number }>;
}
```

---

## 2. Acceptance Tests (E2E Scenarios)

### Test File: `tests/node/e2e/orchestrator-e2e.test.ts`

All 6 acceptance tests correspond to the Gherkin scenarios from the story.
Each test uses `tmpDir` with `beforeEach`/`afterEach` cleanup (same pattern as `tests/node/checkpoint/acceptance.test.ts`).
Timeout: `{ timeout: 30000 }` for the describe block (DoD: < 30s total).

---

### AT-1: Dry-Run Path (degenerate case)

**Gherkin:** E2E dry-run -- shows plan without executing

**TPP Level:** L1 (degenerate) -- no execution, no side effects, constant output

| # | ID | Test Name | Parallel |
|---|-----|-----------|----------|
| 1 | AT-1 | `orchestratorE2E_dryRunMode_showsPlanWithoutExecution` | no |

**Setup/Preconditions:**
- Mini implementation map: 5 stories, 3 phases (from `buildMiniMap()`)
- Write implementation map markdown to `tmpDir`
- Configure mock dispatch: all stories return SUCCESS (irrelevant -- should never be called)
- Execute scenario runner with `mode: "dry-run"`

**Actions:**
1. Create scenario runner with mini map
2. Execute with `dryRun: true`

**Assertions:**
- Mock dispatch NEVER called: `callLog.length === 0`
- No `execution-state.json` file created in `tmpDir`
- `output` contains formatted plan text
- `output` contains all 5 story IDs: `story-0001` through `story-0005`
- `output` contains phase numbers: `Phase 0`, `Phase 1`, `Phase 2`
- `output` contains dependency information (e.g., `blocked by`)
- Plan mode is `"full"` in the dry-run planner output

**Dependencies:**
- `buildMiniMap()` (UT-08..UT-16)
- `createMockDispatch()` (UT-01..UT-07)
- `buildDryRunPlan()` from `src/domain/dry-run/planner.ts`
- `formatPlan()` from `src/domain/dry-run/formatter.ts`

---

### AT-2: Happy Path (all 5 stories SUCCESS)

**Gherkin:** E2E happy path -- all stories SUCCESS

**TPP Level:** L3 (simple conditional -> iteration) -- simplest real execution

| # | ID | Test Name | Parallel |
|---|-----|-----------|----------|
| 1 | AT-2 | `orchestratorE2E_allStoriesSuccess_completesWithFullCoverage` | no |

**Setup/Preconditions:**
- Mini implementation map: 5 stories, 3 phases
- Mock dispatch: ALL stories return `{ status: "SUCCESS", commitSha: "sha-xxx", findingsCount: 0, summary: "ok" }`
- No pre-existing checkpoint

**Actions:**
1. Create scenario runner with mini map and SUCCESS-for-all mock
2. Execute with default mode (`full`)

**Assertions:**
- Final `execution-state.json` has 5/5 stories with status `SUCCESS`
- 3 integrity gates (one per phase) with status `PASS`
- Metrics: `storiesCompleted: 5`, `storiesTotal: 5`
- Metrics: `storiesFailed: 0` or undefined, `storiesBlocked: 0` or undefined
- Stories executed in phase order: phase 0 before phase 1, phase 1 before phase 2
- `callLog` has exactly 5 entries (one per story)
- `callLog` stories in phase order: `story-0001` first, then `story-0002`/`story-0003`, then `story-0004`/`story-0005`
- Critical path stories dispatched first within each phase
- `output` contains `PHASE_START`, `STORY_START`, `STORY_COMPLETE`, `GATE_RESULT`, `EPIC_COMPLETE` event markers
- `output` contains `Epic Complete` summary

**Dependencies:**
- `buildMiniMap()`, `createMockDispatch()`
- `createCheckpoint()`, `readCheckpoint()`, `updateStoryStatus()` from `src/checkpoint/engine.ts`
- `getExecutableStories()` from `src/domain/implementation-map/executable-stories.ts`
- `updateIntegrityGate()` from `src/checkpoint/engine.ts`
- `createProgressReporter()` + `emit()` from `src/progress/reporter.ts`

---

### AT-3: Failure Path (retry + block propagation)

**Gherkin:** E2E failure path -- retry + block propagation

**TPP Level:** L4 (complex conditional) -- retry branching + transitive block propagation

| # | ID | Test Name | Parallel |
|---|-----|-----------|----------|
| 1 | AT-3 | `orchestratorE2E_rootStoryFailsWithRetryExhaustion_propagatesBlocksToAllDependents` | no |

**Setup/Preconditions:**
- Mini implementation map: 5 stories, 3 phases
- Mock dispatch configuration:
  - `story-0001`: array of 3 FAILED results `[FAILED, FAILED, FAILED]` (initial + 2 retries)
  - All other stories: SUCCESS (should never be reached due to blocking)

**Actions:**
1. Create scenario runner with mini map and failure mock for `story-0001`
2. Execute with default mode (`full`)

**Assertions:**
- `story-0001`: status `FAILED`, retries `2`
- `story-0002`: status `BLOCKED` (direct dependent of `story-0001`)
- `story-0003`: status `BLOCKED` (direct dependent of `story-0001`)
- `story-0004`: status `BLOCKED` (transitive: depends on `story-0002` and `story-0003`)
- `story-0005`: status `BLOCKED` (transitive: depends on `story-0003`)
- Each blocked story has a `blockedBy` array containing the blocking chain
- `callLog` has exactly 3 entries: `story-0001` dispatched 3 times (initial + 2 retries)
- Stories `story-0002` through `story-0005` NOT dispatched (blocked before dispatch)
- `output` contains `RETRY` event markers (at least 2)
- `output` contains `BLOCKED` event markers (at least 4)
- Metrics: `storiesFailed >= 1`, `storiesBlocked >= 4`

**Dependencies:**
- `evaluateRetry()` from `src/domain/failure/retry-evaluator.ts`
- `propagateBlocks()` from `src/domain/failure/block-propagator.ts`
- `MAX_RETRIES` constant (2) from `src/domain/failure/types.ts` or `src/checkpoint/types.ts`

---

### AT-4: Resume Path (continue from checkpoint)

**Gherkin:** E2E resume path -- continues from where it stopped

**TPP Level:** L5 (iteration over state) -- reclassification + selective re-dispatch

| # | ID | Test Name | Parallel |
|---|-----|-----------|----------|
| 1 | AT-4 | `orchestratorE2E_resumeFromPartialCheckpoint_onlyDispatchesPendingStories` | no |

**Setup/Preconditions:**
- Pre-create a checkpoint in `tmpDir` with:
  - `story-0001`: status `SUCCESS`, phase 0
  - `story-0002`: status `IN_PROGRESS`, phase 1 (will be reclassified to PENDING by resume)
  - `story-0003`: status `PENDING`, phase 1
  - `story-0004`: status `PENDING`, phase 2
  - `story-0005`: status `PENDING`, phase 2
- Mock dispatch: all stories return SUCCESS

**Actions:**
1. Create scenario runner with pre-populated checkpoint
2. Execute with `mode: "resume"`

**Assertions:**
- `story-0001` NOT re-dispatched (already SUCCESS)
- `story-0002` reclassified from `IN_PROGRESS` to `PENDING` by `reclassifyStories()`, then dispatched
- `story-0003`, `story-0004`, `story-0005` dispatched normally
- `callLog` has exactly 4 entries: stories `0002`, `0003`, `0004`, `0005`
- `story-0001` NOT in `callLog`
- Final state: 5/5 stories with status `SUCCESS`
- Stories dispatched in dependency-respecting order (phase 1 before phase 2)

**Dependencies:**
- `prepareResume()` from `src/checkpoint/resume.ts`
- `reclassifyStories()` from `src/checkpoint/resume.ts`
- `reevaluateBlocked()` from `src/checkpoint/resume.ts`

---

### AT-5: Partial Path (`--phase 2` only)

**Gherkin:** E2E partial path -- `--phase` executes only specific phase

**TPP Level:** L4 (conditional + filter) -- phase filtering with prerequisite validation

| # | ID | Test Name | Parallel |
|---|-----|-----------|----------|
| 1 | AT-5 | `orchestratorE2E_phaseTwo_onlyDispatchesPhase2Stories` | no |

**Setup/Preconditions:**
- Pre-create checkpoint with phases 0 and 1 complete:
  - `story-0001`: status `SUCCESS`, phase 0
  - `story-0002`: status `SUCCESS`, phase 1
  - `story-0003`: status `SUCCESS`, phase 1
  - `story-0004`: status `PENDING`, phase 2
  - `story-0005`: status `PENDING`, phase 2
- Mock dispatch: phase 2 stories return SUCCESS

**Actions:**
1. Create scenario runner with pre-populated checkpoint
2. Execute with `phase: 2`

**Assertions:**
- Only `story-0004` and `story-0005` dispatched
- `story-0001`, `story-0002`, `story-0003` NOT dispatched
- `callLog` has exactly 2 entries
- Integrity gate recorded for phase 2 only
- Final state: `story-0004` and `story-0005` have status `SUCCESS`
- `story-0001`, `story-0002`, `story-0003` remain `SUCCESS` (unchanged)

**Dependencies:**
- `parsePartialExecutionMode()` from `src/domain/implementation-map/partial-execution.ts`
- `validatePhasePrerequisites()` from `src/domain/implementation-map/partial-execution.ts`
- `getStoriesForPhase()` from `src/domain/implementation-map/partial-execution.ts`

---

### AT-6: Parallel Path (`--parallel` mock)

**Gherkin:** E2E parallel -- parallel mode dispatch

**TPP Level:** L5 (derived computation) -- parallel identification within phases

| # | ID | Test Name | Parallel |
|---|-----|-----------|----------|
| 1 | AT-6 | `orchestratorE2E_parallelMode_identifiesParallelizableStoriesAndCompletes` | no |

**Setup/Preconditions:**
- Mini implementation map: 5 stories, 3 phases
- Mock dispatch: all stories return SUCCESS
- Execute with `parallelMode: true`

**Actions:**
1. Create scenario runner with mini map and parallel mode
2. Execute with `mode: "parallel"`

**Assertions:**
- All 5 stories dispatched
- `callLog` has 5 entries
- Final state: 5/5 stories with status `SUCCESS`
- Within phase 1: `story-0002` and `story-0003` identified as parallelizable (both blocked only by `story-0001`, which is in phase 0 -- they share no intra-phase dependencies)
- Within phase 0: `story-0001` is the only story (parallelCount = 1)
- `output` contains parallel execution indicators

**Dependencies:**
- `getExecutableStories()` uses the same filtering regardless of parallel flag
- Parallel identification is an output characteristic, not a dispatch mechanism change in mock

---

## 3. Integration Tests (Cross-Module Composition)

These IT tests validate the data flow between modules within the scenario runner,
covering the integration points documented in plan-story-0005-0014.md section 5.2.

### Test File: `tests/node/e2e/orchestrator-e2e.test.ts` (same file, separate `describe`)

| # | ID | Test Name | Description | TPP Level | Parallel |
|---|-----|-----------|-------------|-----------|----------|
| 1 | IT-01 | `orchestratorIntegration_parseToCheckpoint_mapStoriesMatchCheckpointEntries` | ParsedMap stories match checkpoint story entries after `createCheckpoint` | L3 | no |
| 2 | IT-02 | `orchestratorIntegration_checkpointToExecutableStories_pendingStoriesReturned` | `getExecutableStories` returns phase-0 stories from a fresh checkpoint | L3 | no |
| 3 | IT-03 | `orchestratorIntegration_failureToBlockPropagation_blockedStoriesMatchDagDependents` | `propagateBlocks` returns dependents matching DAG `blocks` adjacency | L5 | no |
| 4 | IT-04 | `orchestratorIntegration_progressReporter_emitsEventsInCorrectOrder` | Reporter emits PHASE_START before STORY_START within same phase | L4 | no |
| 5 | IT-05 | `orchestratorIntegration_retryEvaluator_respectsMaxRetries` | After MAX_RETRIES (2), evaluateRetry returns `shouldRetry: false` | L3 | no |
| 6 | IT-06 | `orchestratorIntegration_partialExecution_validatePhasePrerequisitesPassesWhenPriorPhasesComplete` | Phases 0..1 SUCCESS -> `validatePhasePrerequisites(2)` returns `valid: true` | L4 | no |

---

## 4. Cross-Module Data Flow Verification

The E2E tests verify these data flow paths end-to-end:

| Source Module | Data | Consumer Module | Verified In |
|---------------|------|-----------------|-------------|
| `markdown-parser` | `DependencyMatrixRow[]`, `PhaseSummaryRow[]` | `dag-builder` | UT-08..UT-16 (via buildMiniMap) |
| `dag-builder` | `Map<string, DagNode>` | `phase-computer`, `critical-path` | UT-10..UT-14 |
| `phase-computer` | `Map<number, string[]>` | `executable-stories`, `partial-execution` | AT-2, AT-5, IT-02 |
| `executable-stories` | `string[]` (ordered) | Orchestrator loop | AT-2, AT-4, IT-02 |
| `checkpoint/engine` | `ExecutionState` | `resume`, `executable-stories`, assertions | AT-2..AT-6, IT-01 |
| `failure/retry-evaluator` | `RetryDecision` | Orchestrator retry logic | AT-3, IT-05 |
| `failure/block-propagator` | `BlockPropagationResult` | Checkpoint updates | AT-3, IT-03 |
| `progress/reporter` | Formatted output + metrics | Assertions on output | AT-2..AT-6, IT-04 |
| `dry-run/planner` | `DryRunPlan` | Assertions on plan structure | AT-1 |
| `checkpoint/resume` | Reclassified stories | Orchestrator loop | AT-4 |

---

## 5. Type Compatibility Notes

### Risk: Two `ExecutionState` types exist

- `checkpoint/types.ts`: `Record<string, StoryEntry>` with full checkpoint fields
- `domain/implementation-map/types.ts`: `Record<string, { readonly status: StoryStatus }>` minimal stub

**Mitigation:** The scenario runner uses `checkpoint/types.ts` for all checkpoint operations. The `getExecutableStories()` function from `executable-stories.ts` imports `ExecutionState` from `implementation-map/types.ts`, which is structurally compatible (the checkpoint `StoryEntry` includes `status`). The E2E tests validate this compatibility implicitly through successful execution.

### Risk: Dry-run `ParsedMap` type divergence

- `domain/dry-run/types.ts`: `ParsedMap` uses `readonly number[]` for `phases`, `StoryNode` interface
- `domain/implementation-map/types.ts`: `ParsedMap` uses `ReadonlyMap<number, readonly string[]>` for `phases`, `DagNode` interface

**Mitigation:** AT-1 (dry-run scenario) uses the dry-run planner's own `ParsedMap` type directly. The scenario runner converts the implementation-map `ParsedMap` to the dry-run `ParsedMap` format via an adapter function within `scenario-runner.ts`. This adapter is tested in UT-17..UT-19.

---

## 6. Test Execution Configuration

### Vitest Configuration

```typescript
describe("Orchestrator E2E", { timeout: 30000 }, () => {
  // AT-1 through AT-6
});
```

- Pool: `forks` with `maxForks: 3` (existing config, adequate for E2E tests)
- Temp directories: `beforeEach`/`afterEach` with `mkdtempSync`/`rmSync` (same pattern as checkpoint tests)
- No sequential requirement between test files (each test is self-contained)
- AT tests within the E2E describe block run sequentially (shared describe, each creates own tmpDir)

### WriteFn Pattern (from `tests/node/progress/reporter.test.ts`)

```typescript
function createCapturingWriteFn(): {
  readonly writeFn: WriteFn;
  readonly lines: string[];
} {
  const lines: string[] = [];
  const writeFn: WriteFn = (text: string) => { lines.push(text); };
  return { writeFn, lines };
}
```

All progress output captured via `WriteFn` -- no real stdout writes during tests.

---

## 7. Golden File & Generator Verification (Already Covered)

Per the implementation plan analysis, the following are already covered by existing tests:

| Verification | Existing Test | Status |
|-------------|---------------|--------|
| SKILL.md byte-for-byte (all 8 profiles) | `tests/node/integration/byte-for-byte.test.ts` | COVERED |
| SKILL.md content validation (627 lines) | `tests/node/content/x-dev-epic-implement-content.test.ts` | COVERED |
| `_TEMPLATE-EXECUTION-STATE.json` validation | `tests/node/checkpoint/acceptance.test.ts` | COVERED |
| `_TEMPLATE-EPIC-EXECUTION-REPORT.md` validation | `tests/node/content/epic-execution-report-content.test.ts` | COVERED |
| Generator auto-discovery (`SkillsAssembler`) | `byte-for-byte.test.ts` via pipeline execution | COVERED |
| GitHub skills registration (`SKILL_GROUPS.dev`) | `byte-for-byte.test.ts` via pipeline execution | COVERED |
| Dual copy consistency (23 critical terms) | `x-dev-epic-implement-content.test.ts` | COVERED |

No additional golden file tests are required.

---

## 8. File Summary

### New Files

| # | File | Lines (est.) | Description |
|---|------|-------------|-------------|
| 1 | `tests/node/e2e/orchestrator-e2e.test.ts` | ~400-500 | Main E2E test suite: 6 AT + 6 IT |
| 2 | `tests/node/e2e/helpers/mock-subagent.ts` | ~60-80 | Configurable mock for subagent dispatch |
| 3 | `tests/node/e2e/helpers/mini-implementation-map.ts` | ~80-100 | Synthetic 5-story, 3-phase map builder |
| 4 | `tests/node/e2e/helpers/scenario-runner.ts` | ~150-200 | Orchestration harness wiring all modules |

### Modified Files

| # | File | Change |
|---|------|--------|
| 1 | `CLAUDE.md` | Add `x-dev-epic-implement` to skills table (~2 lines) |

### Total Estimated New Lines: ~700-880

---

## 9. Coverage Estimation

| Module/Helper | Public Functions | Branches | Est. Tests | Line % | Branch % |
|---------------|-----------------|----------|-----------|--------|----------|
| `mock-subagent.ts` (helper) | 1 (`createMockDispatch`) | 4 (default result, array vs single, array exhausted, unknown story) | 7 | 100% | 100% |
| `mini-implementation-map.ts` (helper) | 1 (`buildMiniMap`) | 0 (pure data builder) | 9 | 100% | N/A |
| `scenario-runner.ts` (helper) | 1 (`createScenarioRunner`) | 5 (dry-run, resume, phase, parallel, full) | 3 | ~95% | ~90% |
| `orchestrator-e2e.test.ts` (E2E suite) | 0 (test file) | 0 | 12 | N/A | N/A |
| **Total** | **3** | **9** | **~31** | **>=95%** | **>=90%** |

Note: The modules under test (checkpoint, failure, progress, dry-run, implementation-map) are NOT modified by this story. Their coverage is maintained by existing test suites. The coverage targets above apply to the NEW test infrastructure code only.

---

## 10. Implementation Order

```
Step 1: Test infrastructure (inner-layer first)
  1a. mock-subagent.ts         + UT-01..UT-07
  1b. mini-implementation-map.ts + UT-08..UT-16
  1c. scenario-runner.ts       + UT-17..UT-19

Step 2: E2E scenarios (TPP order)
  2a. AT-1: Dry-run path       (degenerate)
  2b. AT-2: Happy path         (simplest execution)
  2c. AT-3: Failure path       (retry + block)
  2d. AT-4: Resume path        (pre-populated checkpoint)
  2e. AT-5: Partial path       (--phase filter)
  2f. AT-6: Parallel path      (parallel mode)

Step 3: Integration tests
  3a. IT-01..IT-06             (cross-module composition)

Step 4: Documentation
  4a. Update CLAUDE.md skills index

Step 5: Verification
  5a. Run full test suite with coverage
  5b. Verify coverage >= 95% line, >= 90% branch for new code
  5c. Verify existing golden file tests still pass
  5d. Verify E2E suite completes in < 30s
```

---

## 11. Quality Checks

1. [x] Every Gherkin scenario maps to exactly 1 AT test (AT-1 through AT-6)
2. [x] Every error path tested (retry exhaustion in AT-3, block propagation in AT-3)
3. [x] Degenerate case covered (AT-1: dry-run with zero side effects)
4. [x] Test categories: UT (infrastructure) + AT (acceptance/E2E) + IT (integration)
5. [x] TPP order followed: degenerate -> happy -> failure -> resume -> partial -> parallel
6. [x] No mocking of domain logic -- only the subagent dispatch boundary is mocked
7. [x] All assertions use captured `WriteFn` -- no real stdout writes
8. [x] Temp directories with `beforeEach`/`afterEach` cleanup -- no test interdependence
9. [x] Cross-module data flow validated (section 4)
10. [x] Type compatibility risks identified and mitigated (section 5)
11. [x] Performance budget: < 30s total with mock dispatch
12. [x] Golden file and generator integration already covered by existing tests (section 7)
