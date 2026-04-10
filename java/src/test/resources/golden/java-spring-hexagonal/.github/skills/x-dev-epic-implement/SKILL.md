---
name: x-dev-epic-implement
description: >
  Orchestrates the implementation of an entire epic by executing stories
  sequentially or in parallel via worktrees. Parses epic ID and flags,
  validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story
  files), then delegates story execution to x-dev-story-implement subagents.
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
| `--auto-approve-pr` | boolean | `false` | Propagate to x-dev-story-implement: task PRs auto-merge into parent branches. Parent branches require human review. |
| `--batch-approval` | boolean | `true` | Consolidate pending PRs from parallel stories into a single approval prompt (RULE-013). |
| `--task-tracking` | boolean | `true` | Enable task-level tracking in execution-state.json with PR fields (prUrl, prNumber, branch). |

Missing epic ID aborts with: `ERROR: Epic ID is required.`

## Prerequisites Check

1. `plans/epic-XXXX/` directory exists — if not found, suggest `/x-story-epic-full`
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
7. Create `plans/epic-{epicId}/reports/` directory if it does not exist
8. Generate execution plan (see Execution Plan Persistence below)
9. If `--dry-run`: execution plan was saved in step 8. Log: `"Dry-run: execution plan saved to {path}. No stories executed."` and stop
10. If `--resume`: run the Resume Workflow (see below) before delegation
11. Delegate per-story execution to x-dev-story-implement

### Execution Plan Persistence

Before any story executes, persist a human-readable execution plan.

**Pre-check (RULE-002):** Check if `plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md` exists:
- Not found → generate new plan. Log: `"Generating execution plan for EPIC-{epicId}"`
- Found and `mtime(IMPLEMENTATION-MAP.md) <= mtime(plan)` → reuse. Log: `"Reusing existing execution plan from {date}"`
- Found and `mtime(IMPLEMENTATION-MAP.md) > mtime(plan)` → regenerate. Log: `"Regenerating execution plan (implementation map modified)"`

**Template (RULE-007):** Read template at `.claude/templates/_TEMPLATE-EPIC-EXECUTION-PLAN.md` for required output format.

**Fallback (RULE-012):** If template not found, log `"WARNING: Template _TEMPLATE-EPIC-EXECUTION-PLAN.md not found, using inline format"` and generate with inline format containing story list ordered by phase.

**Header (RULE-011):** Include Epic ID, Date, Author, Template Version.

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
| REBASING | PENDING | Interrupted during rebase — retry from scratch |
| REBASE_SUCCESS | PENDING | Rebase completed but merge not done — retry merge |
| REBASE_FAILED | PENDING | Rebase failed — retry candidate (counts as retry) |

### Branch Recovery

Checkout the branch from checkpoint: `git checkout {state.branch}`. If not found locally, try `git checkout -b {state.branch} origin/{state.branch}`.

### Task-Level Reclassification (Step 1b — Per-Task Resume)

After story-level reclassification, apply task-level reclassification for stories with task data:

1. **IN_PROGRESS tasks -> PENDING** (interrupted work)
2. **DONE tasks -> DONE** (preserved)
3. **PR_CREATED/PR_APPROVED tasks**: verify via `gh pr view`: MERGED -> DONE; OPEN -> keep; not found -> FAILED
4. **PR_MERGED tasks -> DONE**
5. **BLOCKED tasks**: if all deps DONE -> PENDING; otherwise keep BLOCKED
6. **FAILED tasks**: if retries < MAX_RETRIES -> PENDING; otherwise keep FAILED

**Backward Compatibility:** Stories without `tasks` field are unaffected. No-op for legacy schema (`version` absent or `"1.0"`).

### BLOCKED Reevaluation

After reclassification, reevaluate each BLOCKED story:

- `blockedBy` undefined → keep BLOCKED (conservative)
- `blockedBy` empty → reclassify to PENDING (vacuously satisfied)
- All deps SUCCESS → reclassify to PENDING
- Any dep non-SUCCESS or missing → keep BLOCKED

Single-pass evaluation (no cascade). After reevaluation, feed updated state into `getExecutableStories()` — only PENDING stories enter the execution loop.

## Phase 0.5 — Pre-flight Conflict Analysis

At the start of **each phase N**, before dispatching any stories for that phase, analyze
file-level overlaps between stories in the same phase. Stories with high code overlap are
demoted to sequential execution within phase N, preventing costly merge conflicts during
parallel dispatch. The results are written to `preflight-analysis-phase-{N}.md`, which the
core loop consumes when deciding per-story parallel vs sequential scheduling.

**Skip condition:** When `--sequential` is set, Phase 0.5 is skipped entirely. Log:
`"Pre-flight analysis skipped (sequential mode)"`.

### 0.5.1 Read Implementation Plans

For each story in the current phase N, read `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
and extract affected files. Stories without plans are classified as `unpredictable`.

### 0.5.2 Build File Overlap Matrix

For each pair of stories (A, B), compute the intersection of their affected file sets.
The matrix is symmetric: `overlap(A, B) == overlap(B, A)`.

### 0.5.3 Classify Overlaps

| Classification | Criteria | Action |
|----------------|----------|--------|
| `unpredictable` | One or both stories have no plan | Demote to sequential |
| `config-only` | ALL overlapping files are config (`*.yaml`, `*.json`, `*.properties`, `*.toml`, `*.env`, `pom.xml`, `build.gradle`, `package.json`) | Allow parallel + smart merge |
| `code-overlap-low` | 1–2 code files (`.ts`, `.java`, `.py`, `.go`, `.rs`, `.kt`) overlap | Allow parallel with WARNING |
| `code-overlap-high` | 3+ code files overlap | Demote to sequential |
| `no-overlap` | Zero overlapping files | Allow parallel |

### 0.5.4 Generate Adjusted Execution Plan

- **Parallel Batch:** Stories with no overlaps, config-only, or code-overlap-low
- **Sequential Queue:** Stories with code-overlap-high or unpredictable (ordered by critical path priority)
- Output saved to `plans/epic-XXXX/plans/preflight-analysis-phase-N.md` for audit

### 0.5.5 Integration with Core Loop (Section 1.3)

The Core Loop reads the preflight analysis and partitions stories:
- Parallel Batch dispatched via worktree dispatch (Section 1.4a)
- Sequential Queue dispatched one at a time (Section 1.4) after parallel batch completes
- If no preflight analysis exists, all stories default to parallel dispatch

## Phase 1 — Execution Loop

### 1.1 Initialize Execution State

- Read `IMPLEMENTATION-MAP.md`, call `parseImplementationMap(content)` to get `ParsedMap`
- Build stories array with `{ id, phase }` from the parsed map
- Call `createCheckpoint(epicDir, input)` to create initial `ExecutionState`

### 1.2 Branch Management

- `git checkout develop && git pull origin develop`
- Create branch: `git checkout -b feat/epic-{epicId}-full-implementation`
- Resume mode: checkout existing branch if it already exists

### 1.3 Core Loop Algorithm

- For each phase, read preflight analysis (Phase 0.5 output) if it exists
- Partition executable stories into parallel batch and sequential queue based on preflight analysis
- Call `getExecutableStories(parsedMap, executionState)` sorted by critical path priority
- Dispatch parallel batch via worktree dispatch (1.4a), then sequential queue one at a time (1.4)
- For each dispatched story: mark `IN_PROGRESS`, dispatch subagent, validate result, update checkpoint
- BLOCKED stories are never dispatched (filtered by `getExecutableStories`)
- If no preflight analysis exists, all stories default to parallel dispatch

### 1.3a Cross-Story Task Dependency Enforcement

Before dispatching a story, verify that all cross-story task dependencies are satisfied. If story-B has a task depending on a specific task in story-A, that task must be DONE before story-B is dispatched. This is more granular than story-level dependencies.

### 1.3b Batch Approval for Parallel Stories (RULE-013)

When `--batch-approval` is enabled and multiple stories execute in parallel, consolidate pending task PRs into a single approval prompt: "N task PRs pending across M stories" with options: Approve all / Review individually / Pause all. Skipped when `--auto-approve-pr` is set (task PRs auto-merge).

### 1.4 Subagent Dispatch (Sequential Mode — When `--sequential` Is Set)

- When `--sequential` flag is set, use sequential dispatch
- Use `Agent` tool to launch a clean context subagent (RULE-001 context isolation)
- Subagent executes x-dev-story-implement logic and returns `SubagentResult`
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

### 1.4b Merge Strategy — Rebase-Before-Merge (After Parallel Dispatch)

Sequential merge of worktree branches into epic branch using rebase-before-merge
strategy, ordered by critical path priority (RULE-007). Each branch (except the first)
is rebased onto the updated epic branch before merging, eliminating spurious conflicts
from stale base commits.

1. Sort SUCCESS stories by critical path priority; init `alreadyMergedStories = []`, `alreadyMergedCommits = []`
2. **First story (no rebase):** `git merge --ff-only feat/epic-{epicId}-{storyId}` (fall back to `git merge` if ff fails)
3. **Subsequent stories (rebase before merge):**
   a. `updateStoryStatus(epicDir, storyId, { status: "REBASING" })`
   b. `git checkout feat/epic-{epicId}-{storyId}` then `git rebase feat/epic-{epicId}-full-implementation`
   c. On rebase success: `updateStoryStatus(..., { status: "REBASE_SUCCESS" })`, switch back, `git merge --ff-only`
   d. On rebase conflict: dispatch conflict resolution subagent (1.4c) with `alreadyMergedStories`, `alreadyMergedCommits`, `rebaseSourceBranch`
   e. On resolution failure: `git rebase --abort`, `updateStoryStatus(..., { status: "REBASE_FAILED" })` then FAILED, propagate blocks
4. On merge success: `updateStoryStatus(epicDir, storyId, { status: "SUCCESS", commitSha })` (RULE-002); track in `alreadyMergedStories`/`alreadyMergedCommits`
5. FAILED or PARTIAL stories from dispatch: do NOT merge/rebase; first persist result with `updateStoryStatus(epicDir, storyId, { status: "FAILED" | "PARTIAL", summary, lastAttemptSha })`, then delegate to failure handling (story-0005-0007)
6. Checkpoint updated after EACH state transition (REBASING → REBASE_SUCCESS → SUCCESS, or REBASING → REBASE_FAILED → FAILED), not in batch

**Checkpoint States:** `REBASING` (rebase in progress), `REBASE_SUCCESS` (rebase done, merge pending), `REBASE_FAILED` (rebase failed, resolution attempted or story FAILED)

### 1.4c Conflict Resolution Subagent

- Dispatch `Agent` subagent with conflict file list, epic/worktree branch, conflict type (`"rebase"` or `"merge"`), and already-merged context (`alreadyMergedStories`, `alreadyMergedCommits`, `rebaseSourceBranch`)
- Subagent considers already-merged stories as intentional, integrates current story on top
- For rebase conflicts: `git add` resolved files (do NOT commit; `rebase --continue` handles it)
- For merge conflicts: `git add` resolved files and commit the merge resolution
- Returns `{ status: "SUCCESS" | "FAILED", summary }` — on FAILED:
  - For rebase conflicts: `git rebase --abort`, mark story FAILED via REBASE_FAILED
  - For merge conflicts: mark story FAILED directly
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

### Phase Completion Reports

After the integrity gate finishes for a phase, generate a phase completion report.

**Template (RULE-007):** Read template at `.claude/templates/_TEMPLATE-PHASE-COMPLETION-REPORT.md` for required output format.

**Fallback (RULE-012):** If template not found, log `"WARNING: Template _TEMPLATE-PHASE-COMPLETION-REPORT.md not found, using inline format"` and generate with inline format.

**Content:** Story statuses, durations, integrity gate results, findings summary, TDD compliance, coverage delta, blockers, next phase readiness.

**Output:** `plans/epic-{epicId}/reports/phase-{N}-completion-{epicId}.md`

**Header (RULE-011):** Include Epic ID, Phase Number, Date, Author, Template Version.

**Timing:** Generated AFTER the integrity gate completes (PASS or FAIL) to include gate results.

## Phase 2 — Consolidation (Two-Wave)

After all stories complete (or reach terminal state), the orchestrator runs a
two-wave consolidation via clean-context subagents (RULE-001). Wave 1 launches
independent subagents in parallel; Wave 2 waits for both results before creating the PR.

**Skip condition:** If NO stories have status SUCCESS, skip consolidation entirely.

### Wave 1 — Parallel Review + Report (SINGLE message)

**CRITICAL:** 2.1 and 2.2 MUST be launched in a SINGLE message (RULE-003).
When `--skip-review` is set, Wave 1 launches only 2.2.

#### 2.1 Tech Lead Review Subagent

- Dispatch subagent that executes `x-review-pr` logic on full epic diff (branch vs develop)
- Input: branch name, base branch (develop)
- Returns `ReviewResult`: `{ score, decision (GO/NO-GO), findings }`
- On SUCCESS: record ReviewResult in checkpoint atomically (RULE-002)
- On subagent failure: log warning, continue (review is informational)

#### 2.2 Report Generation Subagent

- Dispatch subagent to generate `epic-execution-report.md`
- Reads `_TEMPLATE-EPIC-EXECUTION-REPORT.md` and `execution-state.json`
- Resolves all `{{PLACEHOLDER}}` tokens with real data from checkpoint
- `{{FINDINGS_SUMMARY}}` populated with `"Pending review"` placeholder (replaced by Wave 2)
- `{{TDD_COMPLIANCE_TABLE}}`: For each story, read `tddCompliance` from phase entry in checkpoint. Populate per-story row with TDD Commits, Total Commits, TDD % (rounded integer), TPP Progression (OK/WARNING/N/A), Status (PASS >= 80%, WARNING >= 50%, FAIL < 50%). If no data available (legacy epic), fill all columns with N/A
- `{{TDD_SUMMARY}}`: Aggregated metrics — total TDD/total commits, aggregate TDD %, stories by status count, Epic Status (PASS if zero FAIL, else FAIL). If no data: `N/A — no TDD compliance data available (legacy epic without integrity gate)`
- Validates no unresolved `{{...}}` placeholders remain (excluding expected `"Pending review"`)
- Writes to `plans/epic-{epicId}/epic-execution-report.md`
- On FAILURE: log ERROR, PR created without report in Wave 2

### Wave 1 Result Handling

| 2.1 Result | 2.2 Result | Action |
|------------|------------|--------|
| SUCCESS | SUCCESS | Replace placeholder with actual findings; create PR with full report |
| FAILURE | SUCCESS | Replace placeholder with `"Review unavailable"`; PR without review score |
| SUCCESS | FAILURE | PR with minimalist body from checkpoint; include review score |
| FAILURE | FAILURE | PR with minimalist body from checkpoint directly |

### Wave 2 — PR Creation (after Wave 1 completes)

#### 2.3 PR Creation

- Replace `"Pending review"` in report with actual ReviewResult from 2.1 (or `"Review unavailable"` if 2.1 failed)
- If 2.2 failed: create minimalist PR body from checkpoint data
- Push: `git push -u origin feat/epic-{epicId}-full-implementation`
- Title: `feat(epic): implement EPIC-{epicId} — {title}`
- If completion < 100%: title includes `[PARTIAL]`
- Body: summary with stories completed/failed/blocked, tech lead score, coverage, report link
- Create: `gh pr create --title "..." --body "..." --base develop`
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
- Tech lead review executed (Wave 1) or skipped; report generated (Wave 1); placeholder replaced and PR created (Wave 2)

### 3.3 Final Status Determination

- **COMPLETE**: all stories SUCCESS and all DoD items pass
- **PARTIAL**: some stories FAILED/BLOCKED but critical path succeeded
- **FAILED**: critical path stories failed
- Persist to checkpoint: `updateCheckpoint(epicDir, { finalStatus })`

### 3.4 Completion Output

- Display: epic status, stories completed/failed/blocked, coverage, tech lead score, PR link, report path, elapsed time
- Return to develop: `git checkout develop && git pull origin develop`

## Auto-Approve PR Propagation

When `--auto-approve-pr` is set:
- Flag is propagated to each `x-dev-story-implement` dispatch
- Each story creates a parent branch `feat/story-XXXX-YYYY-desc` from develop
- Task PRs target the parent branch (not develop)
- Task PRs are auto-merged into the parent branch after CI passes
- Parent branches are NEVER auto-merged to develop (require human review)
- Completion output lists parent branches as "Pending human review"

## Task-Level State Tracking

When `--task-tracking` is enabled (default), execution-state.json includes:
- `version: "2.0"` (schema version for backward compatibility)
- Per-story `tasks` map with TaskEntry per task: `status`, `agent`, `type`, `commitSha`, `duration`, `prUrl`, `prNumber`, `branch`
- Task statuses: PENDING, IN_PROGRESS, PR_CREATED, PR_APPROVED, PR_MERGED, DONE, BLOCKED, FAILED, SKIPPED
- `parentBranch` per story (present only in auto-approve mode)

**Backward Compatibility (RULE-010):** Epics with `version` absent or `"1.0"` use legacy schema without task-level tracking. The `tasks` field is optional.

## Integration Notes

- Invokes: `x-dev-story-implement` (per-story), `x-story-map` (error guidance)
- Invokes: `x-review-pr` (tech lead review on full epic diff, Phase 2.1 — Wave 1 parallel)
- Uses: `gh pr create` (PR creation with summary body, Phase 2.3 — Wave 2 sequential)
- Phase 2 uses Two-Wave consolidation: Wave 1 dispatches 2.1 + 2.2 in parallel (SINGLE message, RULE-003); Wave 2 (2.3) runs after both complete
- Reads: `_TEMPLATE-EPIC-EXECUTION-REPORT.md` (report template), `execution-state.json` (checkpoint data)
- Reads: `_TEMPLATE-EPIC-EXECUTION-PLAN.md` (execution plan template, Phase 0 Step 8)
- Reads: `_TEMPLATE-PHASE-COMPLETION-REPORT.md` (phase completion report template)
- Reads: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (implementation plans for pre-flight analysis, Phase 0.5)
- Writes: `plans/epic-XXXX/reports/epic-execution-plan-{epicId}.md` (execution plan, Phase 0 Step 8)
- Writes: `plans/epic-XXXX/reports/phase-{N}-completion-{epicId}.md` (phase completion report)
- Writes: `plans/epic-XXXX/plans/preflight-analysis-phase-N.md` (pre-flight analysis output, Phase 0.5)
- Creates: `plans/epic-XXXX/reports/` directory (Phase 0 Step 7)
- Phase 0.5 is skipped when `--sequential` is set
- Execution plan uses idempotency pre-check (RULE-002): compares mtime of IMPLEMENTATION-MAP.md vs execution plan
- Templates referenced via RULE-007; graceful fallback to inline format (RULE-012)
- All `{{PLACEHOLDER}}` tokens are runtime markers — NOT resolved during generation
