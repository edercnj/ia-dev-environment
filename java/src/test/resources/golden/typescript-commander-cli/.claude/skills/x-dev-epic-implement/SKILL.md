---
name: x-dev-epic-implement
description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, AskUserQuestion
<<<<<<< HEAD
argument-hint: "[EPIC-ID] [--phase N] [--story story-XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--sequential] [--skip-smoke-gate] [--single-pr] [--auto-merge] [--no-merge] [--interactive-merge] [--strict-overlap] [--skip-pr-comments]"
context-budget: heavy
||||||| ed34d6011
argument-hint: "[EPIC-ID] [--phase N] [--story story-XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--sequential] [--skip-smoke-gate] [--single-pr] [--auto-merge] [--no-merge] [--interactive-merge] [--strict-overlap] [--skip-pr-comments]"
context-budget: medium
=======
argument-hint: "[EPIC-ID] [--phase N] [--story story-XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--sequential] [--skip-smoke-gate] [--skip-gate] [--single-pr] [--auto-merge] [--no-merge] [--interactive-merge] [--strict-overlap] [--skip-pr-comments]"
context-budget: heavy
>>>>>>> origin/develop
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
| `--skip-gate` | boolean | `false` | Skip the local integrity gate between phases. Gate is registered as `SKIPPED` (not `DEFERRED`). Use for trusted environments or when manual validation is preferred. |
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
  0. Read preflight analysis for this phase (Phase 0.5 output):
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
<<<<<<< HEAD
     e. Circuit breaker check (Section 1.7):
        - If story SUCCESS: reset consecutiveFailures to 0, stay/return to CLOSED
        - If story FAILED: increment consecutiveFailures and totalFailuresInPhase
        - Run circuit breaker threshold check (Section 1.7)
        - If threshold hit, execute corresponding action (WARNING / PAUSE / ABORT)
  7. Read references/integrity-gate.md and run integrity gate between phases
  8. Generate phase completion report (Read references/phase-reports.md)
  9. Re-read checkpoint via readCheckpoint(epicDir) for next iteration
||||||| ed34d6011
  7. Read references/integrity-gate.md and run integrity gate between phases
  8. Generate phase completion report (Read references/phase-reports.md)
  9. Re-read checkpoint via readCheckpoint(epicDir) for next iteration
=======
  7. Run integrity gate between phases (see Section 1.7 — Local Integrity Gate)
  8. Post-gate prompt: present options to user (see Section 1.7b — Post-Gate Prompt)
  9. Generate phase completion report (Read references/phase-reports.md)
  10. Re-read checkpoint via readCheckpoint(epicDir) for next iteration
>>>>>>> origin/develop
```

The loop ensures that:
- Stories are dispatched in dependency-safe order
- BLOCKED stories are never dispatched (filtered by `getExecutableStories`)
- Each phase completes before the next begins
- Pre-flight conflict analysis partitions stories into parallel and sequential groups
- Dependencies are verified at lifecycle level (SUCCESS) and optionally at PR level (MERGED) depending on `mergeMode`
- Circuit breaker (Section 1.7) pauses execution on 3 consecutive failures and aborts the phase on 5 total failures

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
  "coverageLine": N, "coverageBranch": N, "tddCycles": N, "prUrl": "...", "prNumber": N,
  "errorType": "TRANSIENT"|"CONTEXT"|"PERMANENT"|"TIMEOUT"|"INVALID_RESULT",
  "errorMessage": "...", "errorCode": "ERR-TRANSIENT-001" }

Note: `errorType`, `errorMessage`, and `errorCode` are OPTIONAL fields.
- `errorType`: Present when `status` is `FAILED`. Classifies the failure for recovery decisions.
- `errorMessage`: Optional, max 500 characters. Human-readable description of the failure.
- `errorCode`: Optional, pattern `ERR-[A-Z]+-[0-9]{3}`. References the error catalog for lookup.
```

**Sequential mode** (`--sequential`): Dispatch one story at a time via `Agent` tool.
**Parallel mode** (default): Launch ALL executable stories in a SINGLE message via `Agent` with `isolation: "worktree"`. Branch: `feat/{storyId}-short-description`. Legacy `--parallel` flag is silently ignored.

### 1.4b Subagent Recovery

When a subagent returns `SubagentResult` with `status == "FAILED"`, apply error-type-based recovery before marking the story as permanently failed.

#### Recovery Strategy Table

| errorType | Recovery Action | Max Retries | Prompt Modification |
|-----------|----------------|-------------|---------------------|
| `TRANSIENT` | Re-dispatch with same prompt | 2 | None |
| `CONTEXT` | Re-dispatch with reduced prompt | 1 | Add `"CONTEXT PRESSURE: minimize output, skip optional sections"` |
| `TIMEOUT` | Re-dispatch with `--skip-verification` | 1 | Add `"--skip-verification"` to args |
| `PERMANENT` | No recovery, mark FAILED | 0 | N/A |
| `INVALID_RESULT` | No recovery, mark FAILED | 0 | N/A |
| No `errorType` | Treat as PERMANENT | 0 | N/A |

#### Recovery Algorithm

```
After receiving SubagentResult with status FAILED:
  1. Check if errorType is present
     - If absent: treat as PERMANENT, mark FAILED, no retry
  2. Look up recovery strategy from table above
  3. If retryable and retryCount < maxRetries:
     a. Log: "Subagent recovery: {errorType}. Retry {n}/{max} for {storyId}..."
     b. Apply prompt modification if any
     c. Re-dispatch subagent
     d. Increment retryCount
  4. If retryCount >= maxRetries:
     a. Mark story as FAILED
     b. Log: "Subagent recovery exhausted for {storyId}: {errorType}"
```

#### Escalation Rule

If **3 consecutive** subagent dispatches fail (same or different stories), escalate to the user via `AskUserQuestion` with options:

```
3 consecutive subagent failures detected.
Last error: {errorType} — {errorMessage}
Affected stories: {storyId1}, {storyId2}, {storyId3}

Options:
1. Retry last failed story
2. Skip failed story and continue
3. Abort epic execution
```

The consecutive failure counter resets on any successful dispatch.

### 1.4c Conflict Resolution & Auto-Rebase

**Auto-rebase (RULE-011):** After each PR merge (or story SUCCESS in no-merge mode), rebase remaining open PRs onto updated `develop` in critical path order. Skip when `--sequential`. Push with `--force-with-lease` (NEVER `--force`).

**Conflict resolution (RULE-012):** On rebase conflict, dispatch a `general-purpose` subagent with branch names and conflict file list. Max `MAX_REBASE_RETRIES` (3) attempts. On exhaustion: abort rebase, mark story FAILED, close PR.

**Worktree cleanup:** SUCCESS stories cleaned automatically. FAILED stories preserved for diagnostics.

### 1.5 Result Validation (RULE-008)

After receiving the subagent response, validate the `SubagentResult` contract:

**Required fields (all statuses):**

1. **`status` field**: MUST be present, MUST be one of: `SUCCESS`, `FAILED`, `PARTIAL`
2. **`findingsCount` field**: MUST be present and be a number
3. **`summary` field**: MUST be present and be a string

**Conditional required fields (SUCCESS only):**

4. **`commitSha` field**: If `status === "SUCCESS"`, MUST be present and be a string
5. **`prUrl` field**: If `status === "SUCCESS"`, MUST be present and be a valid GitHub PR URL string
6. **`prNumber` field**: If `status === "SUCCESS"`, MUST be present and be a positive integer

**Optional fields (FAILED status -- used by recovery, Section 1.4b):**

7. **`errorType` field**: Optional. If present, MUST be one of: `TRANSIENT`, `CONTEXT`, `PERMANENT`, `TIMEOUT`, `INVALID_RESULT`. Used by Section 1.4b to determine recovery strategy.
8. **`errorMessage` field**: Optional. If present, MUST be a string with max 500 characters. Truncate if longer.
9. **`errorCode` field**: Optional. If present, MUST match pattern `ERR-[A-Z]+-[0-9]{3}`. References the error catalog.

**On validation failure:** Mark the story as FAILED. Set summary to: `"Invalid subagent result: missing {field} field"`. When `status` is `FAILED` and optional error fields are absent, proceed to recovery (Section 1.4b) with `errorType` treated as absent (i.e., PERMANENT -- no retry).

### 1.6 Checkpoint Update (RULE-002)

After each story completes (success or failure), persist the result:

1. Call `updateStoryStatus(epicDir, storyId, update)` with status, commitSha, findingsCount, summary, prUrl, prNumber, prMergeStatus
2. Update metrics: increment `storiesCompleted` counter
3. The checkpoint is persisted atomically to `execution-state.json`

### 1.6b Markdown Status Sync

After checkpoint update, propagate status to markdown files (story file, IMPLEMENTATION-MAP) and Jira (non-blocking). Status mapping: SUCCESS→Concluída, FAILED→Falha, PARTIAL→Parcial, IN_PROGRESS→Em Andamento, BLOCKED→Bloqueada, PENDING→Pendente. On all stories SUCCESS: update epic status to `Concluído`.

<<<<<<< HEAD
### 1.7 Circuit Breaker

The circuit breaker detects systemic failure patterns and escalates to the user before wasting resources on executions that are likely to fail. It is evaluated after every story completion (step 6e in the Core Loop).

#### Thresholds

| Consecutive Failures | Action |
|---------------------|--------|
| 1 | Log `"WARNING: 1 consecutive failure"`, continue execution |
| 2 | Log `"WARNING: 2 consecutive failures. Pattern: {analysis}"` with pattern analysis (see below), continue |
| 3 | **PAUSE**: `AskUserQuestion` — `"3 consecutive failures detected. Pattern: {analysis}. Options: Continue / Skip phase / Abort epic"` |
| 5 total in phase | **ABORT** phase with diagnostic report: `"Circuit breaker OPEN: phase aborted (5 total failures)"` |

#### State Machine

The circuit breaker operates with three states:

```
CLOSED (normal) → 1-2 failures: stay CLOSED with WARNING
CLOSED → 3 consecutive failures: transition to OPEN
OPEN → User chooses "Continue": transition to HALF_OPEN
OPEN → --resume flag: full reset to CLOSED
HALF_OPEN → next story SUCCESS: transition to CLOSED (reset counters)
HALF_OPEN → next story FAILED: transition back to OPEN
```

**State transitions:**

| From | Event | To | Side Effects |
|------|-------|----|--------------|
| CLOSED | Story FAILED (consecutive < 3) | CLOSED | Increment counters, log WARNING |
| CLOSED | Story FAILED (consecutive == 3) | OPEN | Pause execution, prompt user |
| CLOSED | Story SUCCESS | CLOSED | Reset `consecutiveFailures` to 0 |
| OPEN | User chooses "Continue" | HALF_OPEN | Reset `consecutiveFailures` to 0, keep `totalFailuresInPhase` |
| OPEN | User chooses "Skip phase" | CLOSED | Skip remaining stories in phase, advance to next phase |
| OPEN | User chooses "Abort epic" | CLOSED | Abort epic execution with diagnostic report |
| OPEN | `--resume` flag | CLOSED | Full reset: `consecutiveFailures = 0`, `totalFailuresInPhase = 0` |
| HALF_OPEN | Story SUCCESS | CLOSED | Reset all failure counters |
| HALF_OPEN | Story FAILED | OPEN | Re-prompt user with updated failure count |

#### Reset Conditions

- **Story completes with SUCCESS**: `consecutiveFailures = 0`, stay/return to CLOSED
- **`--resume` used**: Full reset of all counters (`consecutiveFailures = 0`, `totalFailuresInPhase = 0`, `status = CLOSED`)
- **User chooses "Continue" at OPEN prompt**: Move to HALF_OPEN (`consecutiveFailures` reset to 0, `totalFailuresInPhase` preserved)
- **New phase starts**: `consecutiveFailures = 0`, `totalFailuresInPhase = 0`, `status = CLOSED` (each phase has independent counters)

#### Pattern Analysis

At 2 or more consecutive failures, analyze the `errorType` from recent failed stories to determine if the issue is systemic:

```
If all recent failures have same errorType:
  → "Systemic: repeated {errorType} failures"
If errorTypes differ:
  → "Intermittent: mixed failure types ({type1}, {type2})"
If errorType is not available:
  → "Unknown: error types not classified"
```

The pattern analysis result is stored in `lastFailurePattern` and included in the WARNING log and `AskUserQuestion` prompt.

#### Phase Abort (5 Total Failures)

When `totalFailuresInPhase` reaches 5 (regardless of consecutive count), abort the current phase:

1. Log: `"Circuit breaker OPEN: phase aborted (5 total failures)"`
2. Mark all remaining PENDING stories in the phase as BLOCKED with reason `"Phase aborted by circuit breaker"`
3. Generate a diagnostic report summarizing all failures in the phase (story IDs, error types, error messages)
4. Advance to the integrity gate (the gate will likely FAIL, triggering its own reporting)

#### Checkpoint Integration

The circuit breaker state is persisted in `execution-state.json` under the `circuitBreaker` field:

```json
"circuitBreaker": {
  "consecutiveFailures": 0,
  "totalFailuresInPhase": 0,
  "lastFailureAt": null,
  "lastFailurePattern": null,
  "status": "CLOSED"
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `consecutiveFailures` | Integer | Yes | `0` | Counter of consecutive story failures (resets on SUCCESS) |
| `totalFailuresInPhase` | Integer | Yes | `0` | Counter of total failures in current phase (resets per phase) |
| `lastFailureAt` | String (ISO-8601) | Optional | `null` | Timestamp of most recent failure |
| `lastFailurePattern` | String | Optional | `null` | Pattern analysis result: `"Systemic: ..."` or `"Intermittent: ..."` |
| `status` | String (Enum) | Yes | `"CLOSED"` | Circuit breaker state: `CLOSED`, `OPEN`, `HALF_OPEN` |

> **Schema details:** See `references/checkpoint-schema.md` for the full `execution-state.json` schema including `circuitBreaker`.

||||||| ed34d6011
=======
### 1.7 Local Integrity Gate (RULE-006)

> **Reference:** Read `references/integrity-gate.md` for gate subagent prompt, regression diagnosis, version bump, and checkpoint schema details. This section overrides the DEFERRED default for `--no-merge` mode described in that reference.

The integrity gate MUST execute by default between phases — **never DEFERRED**. Even in `--no-merge` mode (the default), a local gate validates cross-story integration using a temporary branch.

#### Gate Result Values

| Value | Meaning |
|-------|---------|
| `PASS` | All compile, test, and coverage checks passed on the merged code |
| `FAIL` | One or more checks failed (merge conflict, compilation error, test failure, coverage below threshold) |
| `SKIPPED` | Gate explicitly skipped via `--skip-gate` flag (conscious opt-out) |

> **`DEFERRED` is no longer a default outcome.** The `DEFERRED` value is removed from the default gate behavior. Gates always produce `PASS`, `FAIL`, or `SKIPPED`.

#### Skip Gate (`--skip-gate`)

When `--skip-gate` is set:
1. Log: `"Integrity gate skipped (--skip-gate) for phase {N}"`
2. Record: `integrityGate.status = "SKIPPED"` in checkpoint
3. Skip directly to post-gate prompt (Section 1.7b)
4. No temporary branch is created

#### Local Gate Algorithm (No-Merge Mode)

When `mergeMode == "no-merge"` (default) and `--skip-gate` is NOT set:

```
1. Filter stories: collect all stories in current phase with status == SUCCESS
   - If no SUCCESS stories exist:
     Log: "No SUCCESS stories in phase {N}, skipping gate"
     Record: integrityGate.status = "SKIPPED"
     Return early (proceed to post-gate prompt)

2. Create temporary branch:
   git checkout develop && git pull origin develop
   git checkout -b temp/gate-phase-{N}-{timestamp}

3. Merge story branches into temporary branch:
   For each story with status SUCCESS in the current phase:
     git merge origin/feat/{storyId} --no-edit
     If merge conflict:
       Log: "MERGE CONFLICT: story {storyId} conflicts on temp gate branch"
       Log conflict file list: git diff --name-only --diff-filter=U
       Record: integrityGate.status = "FAIL"
       Record: integrityGate.failReason = "merge-conflict"
       Record: integrityGate.conflictStory = storyId
       Abort merge: git merge --abort
       Jump to step 6 (cleanup)

4. Run validation on temporary branch:
   a. Compile: {{COMPILE_COMMAND}}
   b. Test: {{TEST_COMMAND}}
   c. Coverage: {{COVERAGE_COMMAND}}
   d. Smoke tests (unless --skip-smoke-gate):
      {{SMOKE_COMMAND}}

5. Evaluate results:
   - If compilation fails: integrityGate.status = "FAIL"
   - If any tests fail: integrityGate.status = "FAIL", correlate with stories
   - If line coverage < 95% or branch coverage < 90%: integrityGate.status = "FAIL"
   - If smoke tests fail (and not --skip-smoke-gate): integrityGate.status = "FAIL"
   - Otherwise: integrityGate.status = "PASS"

6. Cleanup temporary branch (ALWAYS, even on failure):
   git checkout develop
   git branch -D temp/gate-phase-{N}-{timestamp}

7. Record gate result in checkpoint via updateIntegrityGate()
   (See references/integrity-gate.md for full schema)

8. If FAIL: block next phase, present failure details to user
   If PASS: proceed to post-gate prompt (Section 1.7b)
```

#### Gate in Non-No-Merge Modes

When `mergeMode != "no-merge"` (auto or interactive with PRs already merged), the gate runs directly on `develop` as described in `references/integrity-gate.md` (PRs are already merged, so no temporary branch is needed). The same `PASS`/`FAIL`/`SKIPPED` result values apply — `DEFERRED` is never used.

#### Gate Failure Handling

On `integrityGate.status == "FAIL"`, present options to the user via `AskUserQuestion`:

```
Phase {N} integrity gate: FAIL
Reason: {failReason} (merge-conflict | test-failure | coverage-below-threshold | compilation-error)
Details: {details}

Options:
1. Fix and retry gate (re-run after manual fixes)
2. Skip gate for this phase (--skip-gate)
3. Abort epic execution
```

### 1.7b Post-Gate Prompt

After a gate `PASS` (or `SKIPPED`), present options to the user via `AskUserQuestion`:

```
Phase {N} integrity gate: {PASS|SKIPPED}
Stories completed: {successCount}/{totalCount}
{if PASS: Coverage: {lineCoverage}% line, {branchCoverage}% branch}

Options:
1. Continue to Phase {N+1}
2. Merge Phase {N} PRs now (manual)
3. Pause for manual review
```

- **Option 1 (default):** Advance to the next phase immediately
- **Option 2:** Pause execution so the user can manually merge story PRs before continuing. After user confirms merges are done, resume execution.
- **Option 3:** Pause execution entirely. User resumes later with `--resume`.

> **Auto-advance:** When `--auto-merge` is set, skip the prompt and auto-select option 1 (continue to next phase).

>>>>>>> origin/develop
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

## Error Classification

When any tool call, subagent dispatch, or external command fails during execution, classify the error before deciding on recovery action.

### Error Categories

Classify every error into exactly one category using case-insensitive substring matching against the error message:

| Category | Detection Patterns (case-insensitive) | Action |
|----------|---------------------------------------|--------|
| **TRANSIENT** | `"overloaded"`, `"rate limit"`, `"429"`, `"503"`, `"504"`, `"timeout"`, `"ETIMEDOUT"`, `"capacity"`, `"502"` | Retry with exponential backoff |
| **CONTEXT** | `"context"`, `"token limit"`, `"too long"`, `"exceeded"`, `"output too large"`, `"truncated"` | Graceful degradation (defer to story-0031-0004) |
| **PERMANENT** | All errors not matching TRANSIENT or CONTEXT patterns | Fail immediately with contextual error message |

**Classification priority:** Check TRANSIENT patterns first, then CONTEXT patterns. If no pattern matches, classify as PERMANENT.
**Log format:** `"Error classified: {category} — Action: {action}"`

### Retry with Exponential Backoff

When a TRANSIENT error is detected, retry the failed operation using the following backoff schedule.

**Tool call retry (max 3 retries):**

| Retry | Delay |
|-------|-------|
| 1 | 2 seconds |
| 2 | 4 seconds |
| 3 | 8 seconds |
| After 3 failures | Mark task/story as FAILED with `errorCode` |

**Subagent dispatch retry (max 2 retries):**

When a subagent returns `status: "FAILED"` with an error message matching TRANSIENT patterns, re-dispatch the subagent:

| Retry | Delay |
|-------|-------|
| 1 | 2 seconds |
| 2 | 4 seconds |
| After 2 failures | Mark story as FAILED with `errorCode` |

**PERMANENT errors MUST NOT be retried.** If an error matches the PERMANENT category, mark the task/story as FAILED immediately. NEVER retry permanent errors.

**Retry log format:** On each retry attempt, log:
```
"Transient error detected: {error}. Retry {n}/{max} in {delay}s..."
```

On retry success, log:
```
"Retry {n}/{max} succeeded for {operation}"
```

On retry exhaustion, log:
```
"All {max} retries exhausted for {operation}. Marking as FAILED."
```

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
| Subagent FAILED with retryable errorType | Apply recovery (Section 1.4b): re-dispatch with strategy from recovery table |
| Subagent recovery exhausted | Mark story as FAILED, log: `Subagent recovery exhausted for {storyId}: {errorType}` |
| 3 consecutive subagent failures | Escalate to user via `AskUserQuestion` with Retry/Skip/Abort options |
| Integrity gate FAIL with regression | Revert commit, mark story FAILED, trigger block propagation |
| Integrity gate FAIL without regression | Pause execution, report to user |
| Rebase conflict fails after MAX_REBASE_RETRIES | Abort rebase, mark story FAILED, close PR |
| 3 consecutive story failures (circuit breaker) | Transition to OPEN, pause execution, `AskUserQuestion` with Continue/Skip phase/Abort options (Section 1.7) |
| 5 total failures in phase (circuit breaker) | Abort phase, mark remaining PENDING stories as BLOCKED, generate diagnostic report (Section 1.7) |
| Circuit breaker HALF_OPEN + story FAILED | Transition back to OPEN, re-prompt user (Section 1.7) |
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
- Integrity gate runs between phases via local gate (Section 1.7) — never DEFERRED (RULE-006)
- In no-merge mode, gate uses temporary branch to validate cross-story integration
- `--skip-gate` skips the integrity gate (records SKIPPED, not DEFERRED)
- Auto-rebase (Section 1.4e, RULE-011) triggers after each PR merge
- `--single-pr` preserves legacy flow: epic branch + single mega-PR
- `--no-merge`, `--auto-merge`, and `--interactive-merge` are mutually exclusive; default is no-merge mode
- Phase 4 (PR Comment Remediation) is optional, skipped with `--skip-pr-comments` or `--single-pr`
- DoR pre-check is NON-BLOCKING when DoR files don't exist
- Per-task checkpoint tracks individual task progress within PRE_PLANNED stories
- Task-level resume reclassifies IN_PROGRESS tasks to PENDING on `--resume`
<<<<<<< HEAD
- Circuit breaker (Section 1.7) pauses on 3 consecutive failures and aborts phase on 5 total failures
- Circuit breaker resets fully on `--resume` and per-phase (each phase starts with clean counters)
- Circuit breaker state (`CLOSED`, `OPEN`, `HALF_OPEN`) is persisted in `execution-state.json`
||||||| ed34d6011
=======
- Subagent recovery (Section 1.4b) applies error-type-based retry before marking stories as FAILED
- `errorType`, `errorMessage`, `errorCode` are optional fields in SubagentResult, used only when `status == "FAILED"`
- 3 consecutive subagent failures trigger user escalation regardless of error type
>>>>>>> origin/develop
