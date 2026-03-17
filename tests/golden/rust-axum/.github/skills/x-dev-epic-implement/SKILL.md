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
| `--parallel` | boolean | `false` | Enable parallel worktrees |

Missing epic ID aborts with: `ERROR: Epic ID is required.`

## Prerequisites Check

1. `docs/stories/epic-XXXX/` directory exists — if not found, suggest `/x-story-epic-full`
2. `EPIC-XXXX.md` exists — if not found, suggest `/x-story-epic`
3. `IMPLEMENTATION-MAP.md` exists — if not found, suggest `/x-story-map`
4. At least one `story-XXXX-YYYY.md` file exists
5. If `--resume`: `execution-state.json` exists — if missing, suggest running without `--resume`

Abort on first failure with clear error message.

## Phase 0 — Preparation

1. Parse arguments (epic ID + flags)
2. Run all prerequisite checks
3. Read IMPLEMENTATION-MAP.md for dependency graph
4. Read EPIC-XXXX.md for context
5. Glob story files, determine execution order
6. Create branch: `git checkout -b feat/epic-{epicId}-implementation`
7. If `--dry-run`: output plan and stop
8. If `--resume`: read execution-state.json, skip completed stories
9. Delegate per-story execution to x-dev-lifecycle

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

### 1.4 Subagent Dispatch

- Use `Agent` tool to launch a clean context subagent (RULE-001 context isolation)
- Subagent executes x-dev-lifecycle logic and returns `SubagentResult`
- Result fields: `status` (`SUCCESS`/`FAILED`/`PARTIAL`), `commitSha`, `findingsCount`, `summary`

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
- [Placeholder: parallel worktree dispatch — story-0005-0010]
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
