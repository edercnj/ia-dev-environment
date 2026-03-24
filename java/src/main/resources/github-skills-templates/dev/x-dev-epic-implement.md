---
name: x-dev-epic-implement
description: >
  Orchestrates the implementation of an entire epic by executing stories
  sequentially or in parallel via worktrees. Parses epic ID and flags,
  validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story
  files), then delegates story execution to x-dev-lifecycle subagents.
---

# Skill: Epic Implementation Orchestrator

## When to Use

- Full epic implementation spanning multiple stories
- Multi-story orchestration with dependency-aware execution order
- Resumable epic execution after interruption
- Parallel story execution via worktrees

## Input Parsing

**Positional (required):** `EPIC-ID` — 4-digit zero-padded epic identifier (e.g., `0042`).

**Optional flags:**

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `--phase N` | number | (all) | Execute only phase N (0-3) |
| `--story story-XXXX-YYYY` | string | (all) | Execute only a specific story |
| `--skip-review` | boolean | `false` | Skip review phases in subagents |
| `--dry-run` | boolean | `false` | Generate plan without executing |
| `--resume` | boolean | `false` | Continue from last checkpoint (execution-state.json) |
| `--sequential` | boolean | `false` | Disable parallel worktrees, execute stories one at a time |

Missing epic ID aborts with: `ERROR: Epic ID is required.`

## Prerequisites Check

1. `docs/stories/epic-XXXX/` directory exists — if not found, suggest `/x-story-epic-full`
2. `EPIC-XXXX.md` exists — if not found, suggest `/x-story-epic`
3. `IMPLEMENTATION-MAP.md` exists — if not found, suggest `/x-story-map`
4. At least one `story-XXXX-YYYY.md` file exists
5. If `--resume`: `execution-state.json` exists — if missing, suggest running without `--resume`

Abort on first failure with clear error message.

## Partial Execution

The `--phase` and `--story` flags enable partial execution of an epic.
These flags are **mutually exclusive** — providing both aborts with:

```
ERROR: --phase and --story are mutually exclusive
```

### Mode: `--phase N`

Execute only stories belonging to phase N.

1. Read checkpoint (or verify existing code if no checkpoint)
2. Validate that phases 0..N-1 are complete (all stories have status SUCCESS)
3. If validation fails, abort:
   - Phase out of range: `Phase {N} does not exist. Max phase is {M}.`
   - Prior phases incomplete: `Phases 0..{N-1} must be complete before phase {N}`
4. Filter stories to phase N only
5. Execute core loop for phase N stories
6. Run integrity gate at end of phase N
7. Update checkpoint

Phase 0 requires no prerequisite validation (no prior phases to check).

### Mode: `--story story-XXXX-YYYY`

Execute a single story in isolation.

1. Read checkpoint (required for single story mode)
2. Validate that ALL dependencies of the story have status SUCCESS
3. If validation fails, abort:
   - Story not in map: `Story {storyId} not found in implementation map`
   - Dependencies not met: `Dependencies not satisfied: [{list}]`
4. Dispatch subagent for the specific story
5. Collect result and update checkpoint
6. Do **not** run integrity gate (single story execution has no integrity gate)

## Phase 0 — Preparation

1. Parse arguments (epic ID + flags)
2. Run all prerequisite checks
3. Read IMPLEMENTATION-MAP.md for dependency graph
4. Read EPIC-XXXX.md for context
5. Glob story files, determine execution order
6. Create branch: `git checkout -b feat/epic-{epicId}-implementation`
7. If `--dry-run`: output plan and stop
8. If `--resume`: run the Resume Workflow (see below) before delegation
9. Delegate per-story execution to x-dev-lifecycle

## Resume Workflow

When `--resume` is set, load `execution-state.json` and apply two-pass reclassification before re-entering the execution loop.

### Reclassification Table

| Current Status | New Status | Condition |
|----------------|------------|-----------|
| IN_PROGRESS | PENDING | Always (interrupted) |
| SUCCESS | SUCCESS | Preserved |
| FAILED (retries < MAX_RETRIES) | PENDING | Retry candidate |
| FAILED (retries >= MAX_RETRIES) | FAILED | Budget exhausted |
| PARTIAL | PENDING | Treat as interrupted |
| BLOCKED | BLOCKED | Deferred to reevaluation |
| PENDING | PENDING | No change |

### Branch Recovery

Checkout the branch from checkpoint: `git checkout {state.branch}`. If not found locally, try `git checkout -b {state.branch} origin/{state.branch}`.

### BLOCKED Reevaluation

After reclassification, reevaluate each BLOCKED story:

- `blockedBy` undefined → keep BLOCKED (conservative)
- `blockedBy` empty → reclassify to PENDING (vacuously satisfied)
- All deps SUCCESS → reclassify to PENDING
- Any dep non-SUCCESS or missing → keep BLOCKED

Single-pass evaluation (no cascade). After reevaluation, feed updated state into `getExecutableStories()` — only PENDING stories enter the execution loop.

## Phase 1 — Execution Loop

### 1.1 Initialize Execution State

- Read `IMPLEMENTATION-MAP.md`, call `parseImplementationMap(content)` to get `ParsedMap`
- Build stories array with `{ id, phase }` from the parsed map
- Call `createCheckpoint(epicDir, input)` to create initial `ExecutionState`

### 1.2 Branch Management

- `git checkout main && git pull origin main`
- Create branch: `git checkout -b feat/epic-{epicId}-full-implementation`
- Resume mode: checkout existing branch if it already exists

### 1.3 Core Loop Algorithm

- For each phase, call `getExecutableStories(parsedMap, executionState)` sorted by critical path priority
- For each executable story: mark `IN_PROGRESS`, dispatch subagent, validate result, update checkpoint
- BLOCKED stories are never dispatched (filtered by `getExecutableStories`)

### 1.4 Subagent Dispatch (Sequential Mode — When `--sequential` Is Set)

- When `--sequential` flag is set, use sequential dispatch
- Use `Agent` tool to launch a clean context subagent (RULE-001 context isolation)
- Subagent executes x-dev-lifecycle logic and returns `SubagentResult`
- Result fields: `status` (`SUCCESS`/`FAILED`/`PARTIAL`), `commitSha`, `findingsCount`, `summary`

### 1.4a Parallel Worktree Dispatch (Default Behavior)

Default behavior. When `--sequential` is NOT set, dispatch all executable stories
in the current phase concurrently in a SINGLE message via `Agent` with `isolation: "worktree"`.

- Call `getExecutableStories(parsedMap, executionState)` for the current phase
- Mark all as `IN_PROGRESS`, then launch in SINGLE message with `isolation: "worktree"`
- Each worktree operates on branch `feat/epic-{epicId}-{storyId}`
- Context isolation (RULE-001): each worktree subagent gets clean context
- Only when `--sequential` flag is set, the sequential dispatch in Section 1.4 is used instead
- Wait for ALL subagents to complete before merge

> **Legacy flag:** If `--parallel` is passed, it is silently ignored (no error). The
> parallel behavior is already the default.

### 1.4b Merge Strategy (After Parallel Dispatch)

Sequential merge of worktree branches into epic branch, ordered by critical path priority (RULE-007):

1. Sort SUCCESS stories by critical path priority
2. For each: `git merge feat/epic-{epicId}-{storyId}` into epic branch
3. On success: `updateStoryStatus(epicDir, storyId, { status: "SUCCESS", commitSha })` (RULE-002)
4. On conflict: dispatch conflict resolution subagent (1.4c)
5. FAILED or PARTIAL stories from dispatch: do NOT merge; first persist result with `updateStoryStatus(epicDir, storyId, { status: "FAILED" | "PARTIAL", summary, lastAttemptSha })`, then delegate to failure handling (story-0005-0007)
6. Checkpoint updated after EACH merge or FAILED/PARTIAL status update, not in batch

### 1.4c Conflict Resolution Subagent

- Dispatch `Agent` subagent with conflict file list, epic branch, and worktree branch
- Subagent analyzes diff from both sides and resolves preserving intent of both stories
- Returns `{ status: "SUCCESS" | "FAILED", summary }` — on FAILED, mark story FAILED
- On irresolvable conflict: trigger block propagation for dependents (story-0005-0007)

### 1.4d Worktree Cleanup

- SUCCESS + merged: worktree cleaned up automatically after merge
- FAILED stories: worktree preserved for diagnostic investigation
- No-change worktrees: Agent tool auto-cleanup when no changes were made

### 1.5 Result Validation (RULE-008)

- Validate `SubagentResult` contract: `status`, `findingsCount`, `summary` required
- If `status === "SUCCESS"`, `commitSha` must be present
- On invalid result: mark story as `FAILED` with descriptive summary

### 1.6 Checkpoint Update (RULE-002)

- Call `updateStoryStatus(epicDir, storyId, result)` after each story
- Checkpoint persisted atomically to `execution-state.json`

### 1.7 Extension Points

- [Placeholder: integrity gate — story-0005-0006]
- [Placeholder: retry + block propagation — story-0005-0007]
- [Placeholder: resume from checkpoint — story-0005-0008]
- [Placeholder: partial execution filter — story-0005-0009]
- [Placeholder: progress reporting — story-0005-0013]

### Integrity Gate (Between Phases)

After all stories in a phase complete, dispatch an integrity gate subagent:

1. **Compile**: `{{COMPILE_COMMAND}}`
2. **Test**: `{{TEST_COMMAND}}` (full suite)
3. **Coverage**: `{{COVERAGE_COMMAND}}` (thresholds: >= 95% line, >= 90% branch)

**Result**: `{ status: PASS|FAIL, testCount, coverage, branchCoverage?, failedTests?, regressionSource? }`

**On PASS**: Advance to next phase
**On FAIL + regression identified**: `git revert` culprit story, mark FAILED, propagate blocks
**On FAIL + unidentified**: Pause execution, report to user

Gate result stored via `updateIntegrityGate(epicDir, phase, result)`. Mandatory per RULE-004.

## Phase 2 — Consolidation

After all stories complete (or reach terminal state), the orchestrator dispatches
three sequential consolidation actions via clean-context subagents (RULE-001).

### 2.1 Tech Lead Review Subagent

- Dispatch subagent that executes `x-review-pr` logic on full epic diff (branch vs main)
- Input: branch name, base branch (main)
- Returns `ReviewResult`: `{ score, decision (GO/NO-GO), findings }`
- On subagent failure: log warning, continue (review is informational)

### 2.2 Report Generation Subagent

- Dispatch subagent to generate `epic-execution-report.md`
- Reads `_TEMPLATE-EPIC-EXECUTION-REPORT.md` and `execution-state.json`
- Resolves all `{{PLACEHOLDER}}` tokens with real data from checkpoint
- Validates no unresolved `{{...}}` placeholders remain in output
- Writes to `docs/stories/epic-{epicId}/epic-execution-report.md`

### 2.3 PR Creation

- Push: `git push -u origin feat/epic-{epicId}-full-implementation`
- Title: `feat(epic): implement EPIC-{epicId} — {title}`
- If completion < 100%: title includes `[PARTIAL]`
- Body: summary with stories completed/failed/blocked, tech lead score, coverage, report link
- Create: `gh pr create --title "..." --body "..." --base main`
- On push failure: log error, generate report without PR, persist failure in checkpoint

### 2.4 Partial Completion Handling

- Consolidation executes regardless of story failures
- FAILED stories listed with failure reasons; BLOCKED stories listed with unsatisfied dependencies
- PR body indicates partial implementation; PR title includes `[PARTIAL]`

### 2.5 Checkpoint Finalization

- Register PR URL and report path in checkpoint via `updateCheckpoint(epicDir, ...)`
- Set `finishedAt` timestamp; persist final `execution-state.json`

## Phase 3 — Verification

Final verification validates the epic as a whole before declaring completion.

### 3.1 Epic-Level Test Suite

- Run full test suite: all unit, integration, and API tests
- Coverage thresholds: >=95% line, >=90% branch (non-negotiable)
- Record coverage results in checkpoint

### 3.2 DoD Checklist Validation

- All stories completed or documented as FAILED/BLOCKED in report
- Coverage thresholds met
- Zero compiler/linter warnings
- Tech lead review executed; report generated; PR created

### 3.3 Final Status Determination

- **COMPLETE**: all stories SUCCESS and all DoD items pass
- **PARTIAL**: some stories FAILED/BLOCKED but critical path succeeded
- **FAILED**: critical path stories failed
- Persist to checkpoint: `updateCheckpoint(epicDir, { finalStatus })`

### 3.4 Completion Output

- Display: epic status, stories completed/failed/blocked, coverage, tech lead score, PR link, report path, elapsed time
- Return to main: `git checkout main && git pull origin main`

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story), `x-story-map` (error guidance)
- Invokes: `x-review-pr` (tech lead review on full epic diff, Phase 2.1)
- Uses: `gh pr create` (PR creation with summary body, Phase 2.3)
- Reads: `_TEMPLATE-EPIC-EXECUTION-REPORT.md` (report template), `execution-state.json` (checkpoint data)
- All `{{PLACEHOLDER}}` tokens are runtime markers — NOT resolved during generation
