# Implementation Plan -- story-0005-0014: E2E Tests + Generator Integration

**Story:** `story-0005-0014.md`
**Type:** Capstone -- integration testing + generator registration
**Dependencies:** All stories 0005-0005 through 0005-0013 (complete)

---

## 1. Affected Layers and Components

This story is purely a **test and integration** story. No new domain logic is created. All modules under test already exist from stories 0005-0005 through 0005-0013.

### Modules Under Test (read-only, not modified)

| Module | Path | Story Origin |
|--------|------|-------------|
| Checkpoint Engine | `src/checkpoint/engine.ts` | 0005-0001 |
| Checkpoint Types | `src/checkpoint/types.ts` | 0005-0001 |
| Checkpoint Validation | `src/checkpoint/validation.ts` | 0005-0003 |
| Checkpoint Resume | `src/checkpoint/resume.ts` | 0005-0006 |
| Implementation Map Parser | `src/domain/implementation-map/markdown-parser.ts` | 0005-0004 |
| DAG Builder | `src/domain/implementation-map/dag-builder.ts` | 0005-0004 |
| DAG Validator | `src/domain/implementation-map/dag-validator.ts` | 0005-0004 |
| Phase Computer | `src/domain/implementation-map/phase-computer.ts` | 0005-0004 |
| Critical Path | `src/domain/implementation-map/critical-path.ts` | 0005-0004 |
| Executable Stories | `src/domain/implementation-map/executable-stories.ts` | 0005-0004 |
| Partial Execution | `src/domain/implementation-map/partial-execution.ts` | 0005-0009 |
| Retry Evaluator | `src/domain/failure/retry-evaluator.ts` | 0005-0007 |
| Block Propagator | `src/domain/failure/block-propagator.ts` | 0005-0007 |
| Dry-Run Planner | `src/domain/dry-run/planner.ts` | 0005-0012 |
| Dry-Run Formatter | `src/domain/dry-run/formatter.ts` | 0005-0012 |
| Progress Reporter | `src/progress/reporter.ts` | 0005-0013 |
| Progress Formatter | `src/progress/formatter.ts` | 0005-0013 |
| Metrics Calculator | `src/progress/metrics-calculator.ts` | 0005-0013 |
| Epic Report Assembler | `src/assembler/epic-report-assembler.ts` | 0005-0011 |
| Skills Assembler | `src/assembler/skills-assembler.ts` | existing |
| GitHub Skills Assembler | `src/assembler/github-skills-assembler.ts` | existing |
| Pipeline | `src/assembler/pipeline.ts` | existing |

### Generator Registration (may require modification)

| File | Action | Reason |
|------|--------|--------|
| `src/assembler/skills-assembler.ts` | VERIFY (likely no change) | Core skills are auto-discovered from `resources/skills-templates/core/` directory scan |
| `src/assembler/github-skills-assembler.ts` | VERIFY (likely no change) | `x-dev-epic-implement` already registered in `SKILL_GROUPS.dev` array |
| `CLAUDE.md` (project root) | MODIFY | Add `x-dev-epic-implement` to the skills index table |

---

## 2. New Files to Create

### 2.1 E2E Test File

| # | File | Description |
|---|------|-------------|
| 1 | `tests/node/e2e/orchestrator-e2e.test.ts` | Main E2E test suite -- 6 scenarios |

### 2.2 E2E Test Infrastructure

| # | File | Description |
|---|------|-------------|
| 2 | `tests/node/e2e/helpers/mock-subagent.ts` | Configurable mock for subagent dispatch returning `SubagentResult` |
| 3 | `tests/node/e2e/helpers/mini-implementation-map.ts` | Synthetic 5-story, 3-phase implementation map builder |
| 4 | `tests/node/e2e/helpers/scenario-runner.ts` | Orchestrator execution harness wiring all modules together |

### 2.3 Golden File Tests (if not already covered)

| # | File | Description |
|---|------|-------------|
| 5 | `tests/node/content/execution-state-template-content.test.ts` | Content validation for `_TEMPLATE-EXECUTION-STATE.json` (may already be covered in acceptance.test.ts -- verify before creating) |

### 2.4 Documentation Updates

| # | File | Action |
|---|------|--------|
| 6 | `CLAUDE.md` | MODIFY -- add `x-dev-epic-implement` to skills table |

---

## 3. Existing Files to Modify

| # | File | Change |
|---|------|--------|
| 1 | `CLAUDE.md` | Add `x-dev-epic-implement` skill entry to the skills table in the README section |

**NOTE:** The generator registration for `x-dev-epic-implement` is already in place:
- `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` exists (auto-discovered by `SkillsAssembler.selectCoreSkills()`)
- `resources/github-skills-templates/dev/x-dev-epic-implement.md` exists (registered in `SKILL_GROUPS.dev`)
- Golden files exist for all 8 profiles at:
  - `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/{profile}/.agents/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md`
- The `_TEMPLATE-EXECUTION-STATE.json` template exists at `resources/templates/` and is validated in `tests/node/checkpoint/acceptance.test.ts`
- The `_TEMPLATE-EPIC-EXECUTION-REPORT.md` template exists at `resources/templates/` and is handled by `EpicReportAssembler`

---

## 4. Dependency Direction Validation

The E2E tests compose modules following the same dependency direction as production code:

```
Test Runner
  |
  +---> Orchestrator Harness (scenario-runner.ts)
  |       |
  |       +---> Implementation Map Parser (domain)
  |       +---> Executable Stories Filter (domain)
  |       +---> Partial Execution Validator (domain)
  |       +---> Checkpoint Engine (src/checkpoint)
  |       +---> Checkpoint Resume (src/checkpoint)
  |       +---> Retry Evaluator (domain/failure)
  |       +---> Block Propagator (domain/failure)
  |       +---> Progress Reporter (src/progress)
  |       +---> Dry-Run Planner (domain/dry-run)
  |       +---> Mock Subagent Dispatch (test helper)
  |
  +---> Assertions on ExecutionState, IntegrityGates, Metrics
```

**Key constraints:**
- Tests import from `src/` modules directly -- no circular dependencies
- Mock subagent replaces the `Task` tool call boundary -- it is the only mock needed
- Domain modules (`implementation-map`, `failure`, `dry-run`) have zero external dependencies
- `checkpoint` module does real file I/O against temp directories (same pattern as existing checkpoint tests)
- `progress` module uses captured `WriteFn` (same pattern as `tests/node/progress/reporter.test.ts`)

---

## 5. Integration Points

### 5.1 Module Composition for E2E

The E2E tests must wire the following modules into an orchestration loop:

```
1. Parse implementation map markdown --> ParsedMap
2. Build DAG, compute phases, critical path
3. Create checkpoint (execution-state.json in tmpDir)
4. For each phase:
   a. Get executable stories (getExecutableStories)
   b. For each story:
      - Update status to IN_PROGRESS
      - Dispatch mock subagent --> SubagentResult
      - If FAILED: evaluate retry (evaluateRetry)
        - If retry: update retries, re-dispatch
        - If budget exhausted: propagate blocks (propagateBlocks)
      - Update status to SUCCESS/FAILED
      - Emit progress events
   c. Run integrity gate (mock compile/test results)
   d. Update integrity gate in checkpoint
5. Emit EPIC_COMPLETE event
6. Read final execution-state.json and assert
```

### 5.2 Cross-Module Data Flow

| Source Module | Data | Consumer Module |
|---------------|------|-----------------|
| `markdown-parser` | `DependencyMatrixRow[]`, `PhaseSummaryRow[]` | `dag-builder` |
| `dag-builder` | `Map<string, DagNode>` | `phase-computer`, `critical-path` |
| `phase-computer` | `Map<number, string[]>` | `executable-stories`, `partial-execution` |
| `executable-stories` | `string[]` (ordered) | Orchestrator loop |
| `checkpoint/engine` | `ExecutionState` | `resume`, `executable-stories`, assertions |
| `failure/retry-evaluator` | `RetryDecision` | Orchestrator retry logic |
| `failure/block-propagator` | `BlockPropagationResult` | Checkpoint updates |
| `progress/reporter` | Formatted output + metrics persistence | Assertions on output |
| `dry-run/planner` | `DryRunPlan` | Assertions on plan structure |

### 5.3 Boundary: Mock Subagent Dispatch

The subagent dispatch is the ONLY external boundary mocked in E2E tests. The mock:

- Accepts: `storyId: string`, `epicId: string`, `branch: string`
- Returns: `SubagentResult` (configurable per scenario via a lookup map)
- Configuration: `Record<string, SubagentResult | SubagentResult[]>` (array for retry scenarios)

---

## 6. Test Infrastructure Needed

### 6.1 Mock Subagent Dispatch (`tests/node/e2e/helpers/mock-subagent.ts`)

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

Features:
- Configurable per-story results (single or array for retry sequences)
- Call log for assertion on dispatch order and count
- Default result for unconfigured stories
- Array results consumed sequentially (attempt 1 -> index 0, attempt 2 -> index 1)

### 6.2 Mini Implementation Map (`tests/node/e2e/helpers/mini-implementation-map.ts`)

Builds a synthetic 5-story, 3-phase markdown string:

```
Phase 0: story-0001 (no deps)
Phase 1: story-0002 (blocked by story-0001), story-0003 (blocked by story-0001)
Phase 2: story-0004 (blocked by story-0002, story-0003), story-0005 (blocked by story-0003)
```

This DAG exercises:
- Root node (story-0001)
- Fan-out (story-0001 blocks story-0002 and story-0003)
- Fan-in (story-0004 blocked by both story-0002 and story-0003)
- Critical path (story-0001 -> story-0003 -> story-0004 or story-0005)
- Transitive block propagation (if story-0001 fails, all 4 others are blocked)

The function returns valid IMPLEMENTATION-MAP.md markdown that passes `parseImplementationMap`.

### 6.3 Scenario Runner (`tests/node/e2e/helpers/scenario-runner.ts`)

Orchestration harness that:
1. Creates a temp directory
2. Writes mini implementation map to disk
3. Parses the map using `parseImplementationMap`
4. Creates a checkpoint
5. Runs the orchestration loop with configurable mock dispatch
6. Supports `--resume`, `--phase`, `--dry-run` modes
7. Returns `{ state: ExecutionState, output: string[], callLog: CallLogEntry[] }`

This avoids duplicating orchestration wiring in each test scenario.

---

## 7. Generator Integration Approach (Dual Copy Registration)

### 7.1 Current State (Already Registered)

The `x-dev-epic-implement` skill is **already fully registered** in the generator:

**Claude skills (auto-discovery):**
- `SkillsAssembler.selectCoreSkills()` scans `resources/skills-templates/core/` and discovers `x-dev-epic-implement/` directory
- No code change needed -- the directory scan is automatic

**GitHub skills (explicit registration):**
- `GithubSkillsAssembler.SKILL_GROUPS.dev` already contains `"x-dev-epic-implement"` at line 31 of `github-skills-assembler.ts`
- The template exists at `resources/github-skills-templates/dev/x-dev-epic-implement.md`

**Codex/Agents skills:**
- `CodexSkillsAssembler` uses same directory scan pattern as `SkillsAssembler`

### 7.2 Template Registration

| Template | Location | Assembler |
|----------|----------|-----------|
| `_TEMPLATE-EXECUTION-STATE.json` | `resources/templates/` | `EpicReportAssembler` copies to `docs/epic/`, `.claude/templates/`, `.github/templates/` |
| `_TEMPLATE-EPIC-EXECUTION-REPORT.md` | `resources/templates/` | `EpicReportAssembler` copies to `docs/epic/`, `.claude/templates/`, `.github/templates/` |

Both templates are already handled by `EpicReportAssembler` (registered in `pipeline.ts` as assembler #22).

### 7.3 What Needs Verification (Not Implementation)

- Golden file byte-for-byte test (`tests/node/integration/byte-for-byte.test.ts`) already covers all 8 profiles
- The skill appears in all 8 golden file sets (verified via glob)
- The `_TEMPLATE-EXECUTION-STATE.json` template validation is covered in `tests/node/checkpoint/acceptance.test.ts`
- The `_TEMPLATE-EPIC-EXECUTION-REPORT.md` template is validated in `tests/node/content/epic-execution-report-content.test.ts`

### 7.4 Remaining Generator Work

Only the `CLAUDE.md` file at the project root needs updating to include `x-dev-epic-implement` in the skills index table. This is a documentation update, not a code change.

---

## 8. Golden File Test Approach

### 8.1 Existing Coverage (Already Sufficient)

The byte-for-byte golden file tests at `tests/node/integration/byte-for-byte.test.ts` already cover:
- All 8 profiles: `go-gin`, `java-quarkus`, `java-spring`, `kotlin-ktor`, `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`
- Pipeline execution + verification against golden files
- Missing/extra file detection
- Content mismatch detection with diff preview

The `x-dev-epic-implement/SKILL.md` is included in golden files for all profiles at:
- `.claude/skills/x-dev-epic-implement/SKILL.md`
- `.agents/skills/x-dev-epic-implement/SKILL.md`
- `.github/skills/x-dev-epic-implement/SKILL.md`

### 8.2 Content Validation (Already Sufficient)

The `tests/node/content/x-dev-epic-implement-content.test.ts` file (627 lines) already validates:
- YAML frontmatter (name, description, allowed-tools, argument-hint)
- Global output policy (English ONLY)
- All 4 phases (Phase 0-3) with substantive content assertions
- Phase 1 content: checkpoint integration, map parser integration, branch management, critical path, context isolation, subagent dispatch, result validation, status values, extension placeholders, rule markers, minimum subsections, logical order
- Phase 2 content: tech lead review, report generation, PR creation, partial completion handling, checkpoint finalization
- Phase 3 content: DoD checklist, final status, test/coverage verification
- Dual copy consistency: 23 critical terms verified in both Claude and GitHub copies
- Partial execution section: mutual exclusivity, phase/story flow, error specs
- Integrity gate section: compile/test/coverage commands, regression diagnosis, git revert, updateIntegrityGate
- Resume workflow: reclassification table, branch recovery, reevaluation

### 8.3 Additional Golden File Tests (If Needed)

The E2E tests will produce their own verification of template correctness by exercising the full pipeline. No additional golden file tests are required beyond what already exists.

---

## 9. E2E Test Scenarios (Detailed Design)

### 9.1 Happy Path (all 5 stories SUCCESS)

**Setup:**
- Mini implementation map: 5 stories, 3 phases
- Mock dispatch: all stories return `{ status: "SUCCESS", commitSha: "sha-xxx", findingsCount: 0, summary: "ok" }`

**Assertions:**
- Final `execution-state.json` has 5/5 stories with status SUCCESS
- 3 integrity gates (one per phase) with status PASS
- Metrics: `storiesCompleted: 5`, `storiesTotal: 5`, `storiesFailed: 0`, `storiesBlocked: 0`
- Progress output contains PHASE_START, STORY_START, STORY_COMPLETE, GATE_RESULT, EPIC_COMPLETE events
- Stories executed in phase order: phase 0 first, then phase 1, then phase 2
- Critical path stories dispatched first within each phase

### 9.2 Failure Path (retry + block propagation)

**Setup:**
- Mock dispatch: story-0001 returns FAILED on attempts 1, 2, and 3 (exhausting MAX_RETRIES=2)
- All other stories return SUCCESS

**Assertions:**
- story-0001: status FAILED, retries: 2
- story-0002, story-0003, story-0004, story-0005: status BLOCKED (transitive block from story-0001)
- Each blocked story has `blockedBy` array containing the blocking chain
- Metrics: `storiesFailed: 1`, `storiesBlocked: 4`
- Mock dispatch called 3 times for story-0001 (initial + 2 retries)
- Mock dispatch NOT called for stories 0002-0005 (blocked before dispatch)
- Progress output contains RETRY events and BLOCK events

### 9.3 Resume Path (continue from where it stopped)

**Setup:**
- Pre-create a checkpoint with story-0001 SUCCESS, story-0002 IN_PROGRESS, story-0003/0004/0005 PENDING
- Mock dispatch: all remaining stories return SUCCESS

**Assertions:**
- story-0001 NOT re-dispatched (already SUCCESS)
- story-0002 reclassified from IN_PROGRESS to PENDING, then dispatched
- story-0003, story-0004, story-0005 dispatched in phase order
- Final state: 5/5 SUCCESS
- Mock dispatch called exactly 4 times (story-0002 through story-0005)

### 9.4 Partial Path (`--phase 2` executes only phase 2)

**Setup:**
- Pre-create checkpoint with phase 0 and phase 1 all SUCCESS
- Execute with `--phase 2` mode
- Mock dispatch: phase 2 stories return SUCCESS

**Assertions:**
- Only story-0004 and story-0005 (phase 2) are dispatched
- story-0001, story-0002, story-0003 NOT dispatched
- Integrity gate recorded for phase 2 only
- Mock dispatch called exactly 2 times

### 9.5 Dry-Run Path (`--dry-run` shows plan without executing)

**Setup:**
- Mini implementation map
- Execute with `--dry-run` mode

**Assertions:**
- Mock dispatch NEVER called (0 calls)
- No checkpoint file created
- Output contains formatted plan with all 5 stories and 3 phases
- Output contains story IDs, phase numbers, dependency info
- Plan mode is "full"

### 9.6 Parallel Path (`--parallel` mock dispatch)

**Setup:**
- Mini implementation map
- Execute with `parallelMode: true` and mock dispatch
- Mock dispatch: all stories return SUCCESS

**Assertions:**
- Within each phase, independent stories are identified as parallelizable
- Mock dispatch called for all 5 stories
- Final state: 5/5 SUCCESS
- Progress output contains parallel execution indicators

---

## 10. Risk Assessment

### 10.1 Low Risk

| Risk | Mitigation |
|------|-----------|
| Existing golden files already pass | Verified via glob -- all 8 profiles have `x-dev-epic-implement/SKILL.md` |
| Generator registration already done | `SkillsAssembler` auto-discovers from directory; `GithubSkillsAssembler.SKILL_GROUPS.dev` already contains entry |
| Template validation already covered | `acceptance.test.ts` validates `_TEMPLATE-EXECUTION-STATE.json`; `epic-execution-report-content.test.ts` validates report template |
| Byte-for-byte tests already pass | `byte-for-byte.test.ts` covers all profiles |

### 10.2 Medium Risk

| Risk | Mitigation |
|------|-----------|
| Scenario runner complexity | Keep orchestration loop simple -- delegate to existing functions, don't reinvent. Use same patterns as existing checkpoint acceptance tests |
| Test flakiness from timing | Use captured `WriteFn` (no real stdout). Use `Date.now()` ranges for timestamp assertions. Mock all I/O timing |
| Implementation map type mismatches | `ParsedMap` in `domain/implementation-map/types.ts` uses `ReadonlyMap<number, readonly string[]>` for phases, while `dry-run/types.ts` has its own `ParsedMap` stub with `readonly number[]`. The scenario runner must use the real `ParsedMap` type from `implementation-map/types.ts` |
| Memory pressure from Vitest forks | E2E tests are not CPU-intensive (mock dispatch). Pool config `maxForks: 3` should handle fine. Monitor with `--reporter=verbose` |

### 10.3 High Risk

| Risk | Mitigation |
|------|-----------|
| Dry-run `ParsedMap` type divergence | `domain/dry-run/types.ts` defines its own `StoryNode`, `ParsedMap`, `StoryStatus`, `ExecutionState` stubs that differ from `domain/implementation-map/types.ts`. The E2E tests must either: (a) use adapters to convert between the two type systems, or (b) the dry-run test scenario should use the dry-run planner's own types directly. Recommend option (b) for the dry-run scenario since it tests the dry-run subsystem in isolation |
| Two `ExecutionState` types exist | `checkpoint/types.ts` uses `Record<string, StoryEntry>` while `domain/implementation-map/types.ts` uses `Record<string, { readonly status: StoryStatus }>`. The scenario runner must use `checkpoint` types for checkpoint operations and `implementation-map` types for DAG operations. These are compatible since `StoryEntry` includes `status` |

---

## 11. Implementation Order

Following TPP (Transformation Priority Premise) and inner-layers-first:

```
Step 1: Create test helpers
  1a. mini-implementation-map.ts  (pure data, no I/O)
  1b. mock-subagent.ts            (pure function, no I/O)
  1c. scenario-runner.ts          (composes all modules, uses tmpDir for I/O)

Step 2: Implement E2E scenarios (in TPP order)
  2a. Dry-run path               (degenerate case -- no execution)
  2b. Happy path                  (simplest real execution)
  2c. Failure path                (retry + block propagation)
  2d. Resume path                 (pre-populated checkpoint)
  2e. Partial path                (--phase filter)
  2f. Parallel path               (parallel mode flag)

Step 3: Documentation
  3a. Update CLAUDE.md skills index

Step 4: Verification
  4a. Run full test suite with coverage
  4b. Verify existing golden file tests still pass
  4c. Verify coverage >= 95% line, >= 90% branch for new code
```

---

## 12. File Summary

### New Files (4-5)

| File | Lines (est.) |
|------|-------------|
| `tests/node/e2e/orchestrator-e2e.test.ts` | ~400-500 |
| `tests/node/e2e/helpers/mock-subagent.ts` | ~60-80 |
| `tests/node/e2e/helpers/mini-implementation-map.ts` | ~80-100 |
| `tests/node/e2e/helpers/scenario-runner.ts` | ~150-200 |

### Modified Files (1)

| File | Change Size |
|------|------------|
| `CLAUDE.md` | ~2 lines (add skill to table) |

### Total Estimated New Lines: ~700-880

---

## 13. Vitest Configuration Notes

- E2E tests should use a dedicated `describe` block with `{ timeout: 30000 }` (30s budget per story DoD)
- Tests use temp directories with `beforeEach`/`afterEach` cleanup (same pattern as checkpoint tests)
- Pool configuration (`forks`, `maxForks: 3`) is adequate for these tests
- No sequential requirement unless tests share state (they should not)
