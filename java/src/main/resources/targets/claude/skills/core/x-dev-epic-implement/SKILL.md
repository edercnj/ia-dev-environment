---
name: x-dev-epic-implement
description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, AskUserQuestion
argument-hint: "[EPIC-ID] [--phase N] [--story story-XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--sequential] [--skip-smoke-gate] [--single-pr] [--auto-merge] [--no-merge] [--interactive-merge] [--strict-overlap] [--skip-pr-comments]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Epic Implementation (Orchestrator)

## Purpose

Orchestrate the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parse epic ID and flags, validate prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), delegate story execution to `x-dev-lifecycle` subagents, manage checkpoints, integrity gates, retry/block propagation, resume, partial execution, dry-run, and progress reporting.

## When to Use

- Full epic implementation spanning multiple stories
- Multi-story orchestration with dependency-aware execution order
- Resumable epic execution after interruption
- Parallel story execution via worktrees

## On-Demand References

This skill uses a core + references architecture. Load references only when needed:

| Condition | Reference File | Content |
|-----------|---------------|---------|
| `--resume` flag is set | `references/resume-workflow.md` | Resume detection, reclassification, PR verification |
| Phase 0.5 (parallel dispatch) | `references/preflight-analysis.md` | Conflict analysis, overlap matrix, classification |
| Merge mode decision needed | `references/merge-modes.md` | Auto-merge, no-merge, interactive-merge mechanisms |
| Between phases (integrity gate) | `references/integrity-gate.md` | Gate preconditions, subagent prompt, regression diagnosis, version bump |
| Checkpoint schema details needed | `references/checkpoint-schema.md` | execution-state.json schema, per-task fields, story entry schema |
| Phase completion or epic report | `references/phase-reports.md` | Phase completion report generation, epic progress report |

> **RULE-002 (Graceful Degradation):** If a reference file is not found, log `"WARNING: Reference {filename} not found"` and continue execution without that reference. The core workflow is self-sufficient for basic execution.

## Workflow Overview

```
Phase 0:   PREPARATION        -> Parse args, validate prerequisites, generate execution plan (inline)
Phase 0.5: PRE-FLIGHT         -> Conflict analysis for parallel stories (conditional, inline)
                                  → Read references/preflight-analysis.md
Phase 1:   EXECUTION LOOP     -> Dispatch stories via worktrees or sequential, integrity gates (inline)
                                  → Read references/integrity-gate.md (between phases)
Phase 2:   PROGRESS REPORT    -> Consolidate results, generate epic execution report (inline)
                                  → Read references/phase-reports.md
Phase 3:   VERIFICATION       -> Epic-level test suite, DoD checklist, final status (inline)
Phase 4:   PR COMMENTS        -> Remediate PR review comments across all story PRs (optional, inline)
```

## Input Parsing

### Positional Argument (Required)

| Argument | Format | Required | Description |
|----------|--------|----------|-------------|
| `EPIC-ID` | `XXXX` (4-digit zero-padded) | **Mandatory** | The epic identifier, e.g., `0042` |

The epic ID is a required positional argument. If missing, abort immediately:

```
ERROR: Epic ID is required. Usage: /x-dev-epic-implement [EPIC-ID] [flags]
```

### Optional Flags

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `--phase N` | number | (all phases) | Execute only phase N (0-4) |
| `--story story-XXXX-YYYY` | string | (all stories) | Execute only a specific story by ID |
| `--skip-review` | boolean | `false` | Skip review phases in x-dev-lifecycle subagents |
| `--dry-run` | boolean | `false` | Generate execution plan without executing |
| `--resume` | boolean | `false` | Continue from last checkpoint (execution-state.json) |
| `--sequential` | boolean | `false` | Disable parallel worktrees, execute stories one at a time |
| `--skip-smoke-gate` | boolean | `false` | Skip smoke tests in the integrity gate between phases |
| `--single-pr` | boolean | `false` | Preserve legacy flow: epic branch + rebase-before-merge + single mega-PR (RULE-009) |
| `--auto-merge` | boolean | `false` | Auto-merge story PRs via `gh pr merge` after reviews approve (RULE-004). Mutually exclusive with `--no-merge` and `--interactive-merge`. |
| `--no-merge` | boolean | `false` | Explicit no-merge flag (same as default behavior). Create PRs but skip merge and merge-wait. Dependencies are satisfied by `status == SUCCESS` alone (PR merge not required). Mutually exclusive with `--auto-merge` and `--interactive-merge`. Use for repos with branch protection rules requiring multiple approvers. |
| `--interactive-merge` | boolean | `false` | Opt-in to interactive merge mode: prompt the user at phase boundaries with 3 options (merge all, pause for manual merge, skip merge). Mutually exclusive with `--auto-merge` and `--no-merge`. |
| `--strict-overlap` | boolean | `false` | When set, stories with `code-overlap-high` or `unpredictable` are demoted to sequential queue (original behavior). Without flag, pre-flight is advisory-only (RULE-005). |
| `--skip-pr-comments` | boolean | `false` | Skip PR comment remediation phase (Phase 4). When set, Phase 4 is skipped entirely with log message. |

## Prerequisites Check

Validate in order, abort on first failure:

1. **Epic Directory**: `plans/epic-XXXX/` must exist
2. **Epic File**: `EPIC-XXXX.md` must exist in epic directory
3. **Implementation Map**: `IMPLEMENTATION-MAP.md` must exist
4. **Story Files**: At least one `story-XXXX-*.md` must exist
5. **Resume Checkpoint** (if `--resume`): `execution-state.json` must exist

## Partial Execution

The `--phase` and `--story` flags enable partial execution of an epic.
These flags are **mutually exclusive** — providing both aborts with:

```
ERROR: --phase and --story are mutually exclusive
```

### Mode: `--phase N`

Execute only phase N stories. Validates phases 0..N-1 are complete (SUCCESS + MERGED when applicable). Phase 0 has no prerequisites. Runs integrity gate at end.

### Mode: `--story story-XXXX-YYYY`

Execute a single story. Validates all dependencies satisfied. No integrity gate.

## Phase 0 — Preparation (Orchestrator — Inline)

1. **Parse arguments** and validate flags (merge mode flags are mutually exclusive)
2. **Determine mergeMode**:
    - `--auto-merge` → `mergeMode = "auto"`
    - `--interactive-merge` → `mergeMode = "interactive"`
    - `--no-merge` → `mergeMode = "no-merge"` (same as default)
    - Neither → `mergeMode = "no-merge"` (default — RULE-003)
3. **Run prerequisites checks** (abort on first failure)
4. **Read IMPLEMENTATION-MAP.md** and **EPIC-XXXX.md**
5. **Discover story files** and **determine execution order** from dependency graph
6. **Single-PR guard**: If `--single-pr`, enter legacy flow (epic branch + mega-PR)
7. **Create reports directory**: `plans/epic-{epicId}/reports/` (skip if exists)
8. **Generate execution plan** (see Execution Plan Persistence below)
9. **Dry-run exit**: If `--dry-run`, log plan path and stop
10. **Resume handling**: If `--resume`, Read `references/resume-workflow.md` and execute Resume Workflow
11. **Delegate**: For each story, invoke `/x-dev-lifecycle`. Each story creates its own branch targeting `develop`.

### Execution Plan Persistence

Persist a human-readable execution plan before any story executes.

**Idempotency (RULE-002):** If plan exists and `mtime(IMPLEMENTATION-MAP.md) <= mtime(plan)`, reuse. Otherwise regenerate.

**Generation:** Read template `_TEMPLATE-EPIC-EXECUTION-PLAN.md` (RULE-007). If missing, use inline markdown with story execution order table (RULE-012 fallback). Write to `plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md`.

## Phase 0.5 — Pre-flight Conflict Analysis

> **Reference:** Read `references/preflight-analysis.md` for full conflict analysis algorithm.

At the start of each phase N, before dispatching stories, perform pre-flight analysis to detect file-level overlaps. By default (advisory mode), overlaps are logged as warnings but stories still execute in parallel. With `--strict-overlap`, high-overlap stories are demoted to sequential execution.

**Skip condition:** When `--sequential` is set, Phase 0.5 is skipped entirely.

## Phase 1 — Execution Loop

### 1.1 Initialize Execution State

1. Read `IMPLEMENTATION-MAP.md` content from the epic directory
2. Call `parseImplementationMap(content)` to obtain a `ParsedMap` containing:
   - `stories`: Map of story IDs to `DagNode` (with phase, dependencies, critical path flag)
   - `phases`: Map of phase numbers to story ID arrays
   - `criticalPath`: Ordered array of story IDs on the critical path
   - `totalPhases`: Number of execution phases
3. Build the stories array with `{ id, phase }` entries from the `ParsedMap`
4. Call `createCheckpoint(epicDir, input)` where input is:
   - `epicId`: The parsed epic ID
   - `stories`: Array of `{ id, phase }` from step 3
   - `baseBranch`: `"develop"` (default — RULE-004; used for PR targets, auto-rebase, and resume)
   - `mode`: `{ parallel: true, skipReview: <from flags>, singlePr: <from flags>, mergeMode: "auto"|"no-merge"|"interactive" }` (default; `parallel` set to `false` when `--sequential` is passed; `mergeMode` derived from `--auto-merge`/`--interactive-merge` flags or defaults to `"no-merge"`)
5. The returned `ExecutionState` tracks all story statuses, metrics, and integrity gates

> **Schema details:** Read `references/checkpoint-schema.md` for full `execution-state.json` schema.

### 1.1b DoR Pre-check (Before Story Dispatch)

Before dispatching a story to `x-dev-lifecycle`, verify its Definition of Ready:

1. Compute DoR path: `plans/epic-{epicId}/plans/dor-{storyId}.md`
2. Check if the DoR file exists:
   - **File does NOT exist:** Proceed without DoR check (backward compatible, RULE-001). Log: `"No DoR file found, proceeding without DoR check (backward compatible)"`
   - **File exists:** Read the `## Final Verdict` section
     - If verdict == `READY`: Proceed with implementation. Log: `"DoR check PASSED for {storyId}"`
     - If verdict == `NOT_READY`: Mark story as BLOCKED with reason `"DoR not satisfied: {failed_checks}"`. Log: `"DoR check FAILED for {storyId}: {failed_checks}"`. Do NOT dispatch the subagent.

### 1.2 Branch Management

The orchestrator does NOT create a branch. Each story creates its own branch via
`x-dev-lifecycle` Phase 0, targeting `develop`. The orchestrator remains on `develop`
and monitors story PRs.

1. Ensure a clean starting point:
   ```
   git checkout develop && git pull origin develop
   ```
2. The orchestrator stays on `develop` for the entire execution. No epic branch is created.

> **`--single-pr` guard (RULE-009):** When `--single-pr` is set, the legacy flow is
> activated instead: create branch `feat/epic-{epicId}-full-implementation`, use
> rebase-before-merge strategy, and create a single mega-PR at the end. All per-story
> PR logic is skipped.

### 1.3 Core Loop Algorithm

Execute stories phase-by-phase in dependency order:

```
For each phase in (0..totalPhases-1):
  0a. Context pressure check (Section 1.7):
      → Evaluate current pressure signals against thresholds
      → If signals detected at level > currentLevel: advance ONE level, apply actions
      → If currentLevel >= 3: save execution-state.json, log exit message, stop
  0b. Read preflight analysis for this phase (Phase 0.5 output):
      → Load preflight-analysis-phase-{N}.md if it exists
      → Extract parallelBatch and sequentialQueue story lists
      → If no preflight analysis exists, treat all stories as parallel-eligible
  1. Call getExecutableStories(parsedMap, executionState, mergeMode)
     → Returns stories sorted by critical path priority (RULE-007)
     → Only PENDING stories with all dependencies SUCCESS are returned
     → When mergeMode != "no-merge": also requires prMergeStatus == "MERGED" for dependencies
     → Stories without dependencies (phase 0) skip the PR merge check
  1a. If stories have dependencies with status SUCCESS but prMergeStatus != "MERGED":
     → Read references/merge-modes.md for PR Merge Decision Mechanism based on mergeMode
  2. If no executable stories and some remain PENDING:
     → Phase is blocked; log warning and advance to next phase
  3. Partition executable stories using preflight analysis:
     a. parallelStories = stories in parallelBatch (or all, if no preflight)
     b. sequentialStories = stories in sequentialQueue (or empty, if no preflight)
  4. Dispatch parallelStories via worktree dispatch (Section 1.4a)
  5. After parallel batch completes, dispatch sequentialStories one at a time
     via sequential dispatch (Section 1.4), in critical path priority order
  6. For each dispatched story (parallel or sequential):
     a0. Run DoR Pre-check (Section 1.1b):
         - If DoR file missing: log and proceed
         - If DoR verdict READY: log and proceed
         - If DoR verdict NOT_READY: mark BLOCKED, skip dispatch, continue to next story
     a. updateStoryStatus(epicDir, storyId, { status: "IN_PROGRESS" })
     b. Dispatch subagent (see 1.4 or 1.4a)
     c. Validate result (see 1.5)
     d. Update checkpoint (see 1.6)
  7. Read references/integrity-gate.md and run integrity gate between phases
  8. Generate phase completion report (Read references/phase-reports.md)
  9. Increment contextPressure.phasesCompletedInConversation in checkpoint
  10. Re-read checkpoint via readCheckpoint(epicDir) for next iteration
```

The loop ensures that:
- Stories are dispatched in dependency-safe order
- BLOCKED stories are never dispatched (filtered by `getExecutableStories`)
- Each phase completes before the next begins
- Pre-flight conflict analysis partitions stories into parallel and sequential groups
- Dependencies are verified at lifecycle level (SUCCESS) and optionally at PR level (MERGED) depending on `mergeMode`

#### `getExecutableStories()` Algorithm (RULE-003)

```
function getExecutableStories(parsedMap, executionState, mergeMode):
  for each storyNode in parsedMap.stories:
    storyState = executionState.stories[storyNode.id]
    if storyState.status != PENDING: continue
    for each dep in storyNode.dependencies:
      depState = executionState.stories[dep]
      if depState.status != SUCCESS: skip story
      if mergeMode != "no-merge":
        if depState.prMergeStatus != "MERGED": skip story
    add storyNode to executableList
  return sortByCriticalPath(executableList)
```

### 1.4 Subagent Dispatch

**Context isolation (RULE-001):** Pass ONLY metadata to subagents — never source code, KPs, or diffs.

**Prompt Template for Subagent:**
```
You are implementing story {storyId} for epic {epicId}.
Story file: plans/epic-{epicId}/story-{storyId}.md
Branch: {branchName} | Phase: {currentPhase} | Skip review: {skipReview}

CRITICAL: Invoke the /x-dev-lifecycle skill using the Skill tool:
  Skill(skill: "x-dev-lifecycle", args: "{storyId}")

The /x-dev-lifecycle skill orchestrates ALL phases: planning, TDD, reviews, commits, and PR creation.
Do NOT manually perform these steps. Let the skill handle all orchestration.

If /x-dev-lifecycle is unavailable (Skill tool error), fall back to manual execution:
1. Read story -> 2. Plan -> 3. TDD (Red-Green-Refactor) -> 4. Test + coverage
5. Commit (Conventional Commits) -> 6. Create PR targeting `develop`

PR MUST include "Part of EPIC-{epicId}" in body (RULE-008).
Version bump: DEFERRED (orchestrator handles at integrity gate).
CONTEXT ISOLATION: Pass only metadata to nested invocations, never source code or diffs.

Return SubagentResult JSON:
{ "status": "SUCCESS"|"FAILED"|"PARTIAL", "commitSha": "...", "findingsCount": N,
  "summary": "...", "reviewsExecuted": { "specialist": true|false, "techLead": true|false },
  "reviewScores": { "specialist": "N/M", "techLead": "N/M" },
  "coverageLine": N, "coverageBranch": N, "tddCycles": N, "prUrl": "...", "prNumber": N }
```

**Sequential mode** (`--sequential`): Dispatch one story at a time via `Agent` tool.
**Parallel mode** (default): Launch ALL executable stories in a SINGLE message via `Agent` with `isolation: "worktree"`. Branch: `feat/{storyId}-short-description`. Legacy `--parallel` flag is silently ignored.

### 1.4c Conflict Resolution & Auto-Rebase

**Auto-rebase (RULE-011):** After each PR merge (or story SUCCESS in no-merge mode), rebase remaining open PRs onto updated `develop` in critical path order. Skip when `--sequential`. Push with `--force-with-lease` (NEVER `--force`).

**Conflict resolution (RULE-012):** On rebase conflict, dispatch a `general-purpose` subagent with branch names and conflict file list. Max `MAX_REBASE_RETRIES` (3) attempts. On exhaustion: abort rebase, mark story FAILED, close PR.

**Worktree cleanup:** SUCCESS stories cleaned automatically. FAILED stories preserved for diagnostics.

### 1.5 Result Validation (RULE-008)

After receiving the subagent response, validate the `SubagentResult` contract:

1. **`status` field**: MUST be present, MUST be one of: `SUCCESS`, `FAILED`, `PARTIAL`
2. **`findingsCount` field**: MUST be present and be a number
3. **`summary` field**: MUST be present and be a string
4. **`commitSha` field**: If `status === "SUCCESS"`, MUST be present and be a string
5. **`prUrl` field**: If `status === "SUCCESS"`, MUST be present and be a valid GitHub PR URL string
6. **`prNumber` field**: If `status === "SUCCESS"`, MUST be present and be a positive integer

**On validation failure:** Mark the story as FAILED. Set summary to: `"Invalid subagent result: missing {field} field"`

### 1.6 Checkpoint Update (RULE-002)

After each story completes (success or failure), persist the result:

1. Call `updateStoryStatus(epicDir, storyId, update)` with status, commitSha, findingsCount, summary, prUrl, prNumber, prMergeStatus
2. Update metrics: increment `storiesCompleted` counter
3. The checkpoint is persisted atomically to `execution-state.json`

### 1.6b Markdown Status Sync

After checkpoint update, propagate status to markdown files (story file, IMPLEMENTATION-MAP) and Jira (non-blocking). Status mapping: SUCCESS→Concluída, FAILED→Falha, PARTIAL→Parcial, IN_PROGRESS→Em Andamento, BLOCKED→Bloqueada, PENDING→Pendente. On all stories SUCCESS: update epic status to `Concluído`.

### 1.7 Graceful Degradation

When the context window approaches its limit during long-running epic executions, the orchestrator degrades progressively to preserve progress. Degradation MUST advance through levels sequentially — NEVER skip from Level 0 directly to Level 3.

#### Pressure Signals

| Level | Signal | Detection |
|-------|--------|-----------|
| Level 1 (Warning) | Output truncation, 3+ phases completed | Subagent returns truncated output; tool call returns "output too large"; `phasesCompletedInConversation >= 3` |
| Level 2 (Critical) | Context compression, context errors | System compression detected (messages shortened); subagent fails with `ERR-CONTEXT-001`/`ERR-CONTEXT-002`; tool calls with token limit errors |
| Level 3 (Emergency) | Multiple consecutive tool failures | 3+ consecutive tool calls failing; responses losing instructions; incoherent output |

#### Degradation Actions

| Level | Actions |
|-------|---------|
| Level 1 | Reduce log verbosity (status lines only); skip optional phases (Phase 0.5 pre-flight, Phase 4 PR comments); use slim mode for review skills |
| Level 2 | Force ALL remaining work to subagents (delegate everything); skip Phase 3 reviews implicitly; add `"CONTEXT PRESSURE: minimize output"` to all subagent prompts |
| Level 3 | Save `execution-state.json` immediately; log: `"CONTEXT PRESSURE Level 3: saving state and suggesting resume"`; suggest `--resume` in a new conversation; stop execution gracefully |

#### Progressive Advancement Rule (CRITICAL)

Degradation MUST be progressive. Even if Level 3 signals are detected while at Level 0, the orchestrator advances through Level 1 → Level 2 → Level 3 sequentially. Each level's actions MUST be applied before advancing to the next.

```
Level 0 (Normal) → detect any pressure signal → Level 1
Level 1          → detect Level 2+ signal     → Level 2
Level 2          → detect Level 3 signal      → Level 3
```

NEVER: Level 0 → Level 3 (forbidden, even if Level 3 signals are present).

#### Context Pressure Check (Core Loop Integration)

At the start of each phase iteration (before dispatching stories for phase N in Section 1.3), evaluate context pressure:

```
Before dispatching stories for phase N:
  1. Evaluate current pressure signals
  2. If new signals detected at level > currentLevel:
     a. Advance to next level (currentLevel + 1, NOT directly to detected level)
     b. Apply that level's actions
     c. Update contextPressure in checkpoint
     d. Log: "CONTEXT PRESSURE Level {N}: {actions_summary}"
  3. If currentLevel >= 3: save state and exit gracefully
  4. Increment phasesCompletedInConversation after phase completes
```

#### Checkpoint Integration

The context pressure state is persisted in `execution-state.json` under the `contextPressure` field:

```json
"contextPressure": {
  "currentLevel": 0,
  "degradationActivatedAt": null,
  "phasesCompletedInConversation": 0
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `currentLevel` | Integer | Yes | `0` | Current degradation level: `0` (normal), `1` (warning), `2` (critical), `3` (emergency) |
| `degradationActivatedAt` | String (ISO-8601) | Optional | `null` | Timestamp when degradation first activated (Level 0 → Level 1) |
| `phasesCompletedInConversation` | Integer | Yes | `0` | Number of phases completed in the current conversation (not persisted across resume) |

**Backward Compatibility:** The `contextPressure` field is OPTIONAL. When not present, it is treated as default normal state (`{ "currentLevel": 0, "degradationActivatedAt": null, "phasesCompletedInConversation": 0 }`). Existing checkpoints without this field continue to work unchanged.

> **Schema details:** See `references/checkpoint-schema.md` for the full `execution-state.json` schema including `contextPressure`.

## Phase 3 — Verification

Final verification on `develop` after all story PRs are handled.

1. Run full test suite: `git checkout develop && git pull origin develop && {{TEST_COMMAND}}`
2. Coverage thresholds: >=95% line, >=90% branch
3. DoD checklist: all story PRs merged/documented, integrity gates passed, coverage met, reviews executed, report generated
4. Final status: **COMPLETE** (all SUCCESS), **PARTIAL** (critical path succeeded), or **FAILED**
5. Display completion output with story PR table, coverage, and elapsed time

## Phase 4 — PR Comment Remediation (Optional)

Skip when `--skip-pr-comments` or `--single-pr` is set. Otherwise:
1. Scan story PRs for unresolved review comments
2. Run `/x-fix-epic-pr-comments {epicId} --dry-run` first
3. Ask for confirmation (bypass with `--auto-merge`)
4. If approved: run `/x-fix-epic-pr-comments {epicId}` to apply fixes

## Error Handling

| Scenario | Action |
|----------|--------|
| Epic ID missing from arguments | Abort: `ERROR: Epic ID is required. Usage: /x-dev-epic-implement [EPIC-ID] [flags]` |
| Epic directory not found | Abort: `ERROR: Directory plans/epic-{epicId}/ not found. Run /x-story-epic-full first.` |
| IMPLEMENTATION-MAP.md missing | Abort: `ERROR: IMPLEMENTATION-MAP.md not found. Run /x-story-map first.` |
| No story files found | Abort: `ERROR: No story files found matching story-{epicId}-*.md.` |
| `--phase` and `--story` both provided | Abort: `ERROR: --phase and --story are mutually exclusive` |
| Mutually exclusive merge flags | Abort: `ERROR: --auto-merge, --no-merge, and --interactive-merge are mutually exclusive. Use only one.` |
| Subagent returns invalid result | Mark story as FAILED with summary: `Invalid subagent result: missing {field} field` |
| Integrity gate FAIL with regression | Revert commit, mark story FAILED, trigger block propagation |
| Integrity gate FAIL without regression | Pause execution, report to user |
| Rebase conflict fails after MAX_REBASE_RETRIES | Abort rebase, mark story FAILED, close PR |
| Context pressure Level 1 detected | Reduce log verbosity, skip optional phases, use slim mode for reviews (Section 1.7) |
| Context pressure Level 2 detected | Force delegation to subagents, skip Phase 3 reviews, add pressure header to prompts (Section 1.7) |
| Context pressure Level 3 detected | Save `execution-state.json` immediately, suggest `--resume`, stop execution gracefully (Section 1.7) |
| Template file not found (RULE-012) | Log warning, use inline format as fallback |
| Reference file not found (RULE-002) | Log warning, continue without reference |

## Template Fallback

Templates referenced by this skill follow RULE-012:

- `_TEMPLATE-EPIC-EXECUTION-PLAN.md` — inline markdown format used for execution plan
- `_TEMPLATE-PHASE-COMPLETION-REPORT.md` — inline markdown format used for phase reports
- `_TEMPLATE-EPIC-EXECUTION-REPORT.md` — inline markdown format used for progress report

## Idempotency (RULE-002)

| Check | Skip Condition | Override |
|-------|---------------|----------|
| Execution plan | `mtime(IMPLEMENTATION-MAP.md) <= mtime(execution-plan)` | Regenerate when map is newer |
| Resume checkpoint | `execution-state.json` exists with valid state | `--resume` flag loads checkpoint |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-dev-lifecycle` | Invokes (per story) | Story execution with PR creation, reviews in Phases 4/7 |
| `x-fix-epic-pr-comments` | Invokes (Phase 4) | PR comment remediation |
| `x-epic-plan` | References | Produces DoR files consumed by DoR pre-check |
| `x-story-map` | References | Error guidance when map is missing |
| `x-lib-version-bump` | Invokes (post-gate) | Version bump on `develop` after integrity gate PASS |
| `gh pr view` | Uses | PR merge status verification |
| `gh pr merge` | Uses | Auto-merge when `--auto-merge` is set |
| `gh pr close` | Uses | Close PR on story failure |
| `_TEMPLATE-EPIC-EXECUTION-PLAN.md` | Reads | Execution plan format |
| `_TEMPLATE-PHASE-COMPLETION-REPORT.md` | Reads | Phase completion report format |
| `_TEMPLATE-EPIC-EXECUTION-REPORT.md` | Reads | Progress report template |
| `execution-state.json` | Reads/Writes | Checkpoint data for resume, status tracking |

**Additional notes:**
- Phase 0.5 is skipped when `--sequential` is set
- Phase 0.5 defaults to advisory mode; use `--strict-overlap` for blocking partitioning
- All `{{PLACEHOLDER}}` tokens are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
- Integrity gate runs on `develop` after all phase PRs are merged (RULE-006)
- Auto-rebase (Section 1.4e, RULE-011) triggers after each PR merge
- `--single-pr` preserves legacy flow: epic branch + single mega-PR
- `--no-merge`, `--auto-merge`, and `--interactive-merge` are mutually exclusive; default is no-merge mode
- Phase 4 (PR Comment Remediation) is optional, skipped with `--skip-pr-comments` or `--single-pr`
- DoR pre-check is NON-BLOCKING when DoR files don't exist
- Per-task checkpoint tracks individual task progress within PRE_PLANNED stories
- Task-level resume reclassifies IN_PROGRESS tasks to PENDING on `--resume`
- Graceful degradation (Section 1.7) activates progressively on context pressure: Level 1 (reduce verbosity) → Level 2 (force delegation) → Level 3 (save state and exit)
- Context pressure check runs at the start of each phase iteration (step 0a in Core Loop)
- Degradation NEVER skips levels — always advances one level at a time
- `contextPressure` checkpoint field is OPTIONAL and backward compatible
