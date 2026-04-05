---
name: x-dev-epic-implement
description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[EPIC-ID] [--phase N] [--story story-XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--sequential] [--skip-smoke-gate] [--single-pr]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Epic Implementation Orchestrator

## When to Use

- Full epic implementation spanning multiple stories
- Multi-story orchestration with dependency-aware execution order
- Resumable epic execution after interruption
- Parallel story execution via worktrees

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
| `--phase N` | number | (all phases) | Execute only phase N (0-3) |
| `--story story-XXXX-YYYY` | string | (all stories) | Execute only a specific story by ID |
| `--skip-review` | boolean | `false` | Skip review phases in x-dev-lifecycle subagents |
| `--dry-run` | boolean | `false` | Generate execution plan without executing |
| `--resume` | boolean | `false` | Continue from last checkpoint (execution-state.json) |
| `--sequential` | boolean | `false` | Disable parallel worktrees, execute stories one at a time |
| `--skip-smoke-gate` | boolean | `false` | Skip smoke tests in the integrity gate between phases |
| `--single-pr` | boolean | `false` | Preserve legacy flow: epic branch + rebase-before-merge + single mega-PR (RULE-009) |

## Prerequisites Check

Before execution, validate all prerequisites in order. Abort on first failure.

### 1. Epic Directory

Check that `plans/epic-XXXX/` exists (where XXXX is the parsed epic ID).

```
ERROR: Directory plans/epic-{epicId}/ not found. Run /x-story-epic-full first.
```

### 2. Epic File

Check that `EPIC-XXXX.md` exists in the epic directory.

```
ERROR: EPIC-{epicId}.md not found in plans/epic-{epicId}/. Run /x-story-epic first.
```

### 3. Implementation Map

Check that `IMPLEMENTATION-MAP.md` exists in the epic directory.

```
ERROR: IMPLEMENTATION-MAP.md not found. Run /x-story-map first.
```

### 4. Story Files

Glob for `story-XXXX-*.md` files in the epic directory. At least one must exist.

```
ERROR: No story files found matching story-{epicId}-*.md.
```

### 5. Resume Checkpoint (conditional)

If `--resume` flag is set, check that `execution-state.json` exists in the epic directory.

```
ERROR: No checkpoint found (execution-state.json missing). Cannot resume. Run without --resume.
```

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

## Phase 0 — Preparation (Orchestrator — Inline)

1. **Parse arguments**: Extract epic ID from positional argument and all optional flags
2. **Run prerequisites checks**: Execute all 5 prerequisite validations (abort on first failure)
3. **Read IMPLEMENTATION-MAP.md**: Extract the story dependency graph and execution order
4. **Read EPIC-XXXX.md**: Load epic context (title, description, acceptance criteria)
5. **Discover story files**: Glob `story-XXXX-*.md` to collect all story files in the epic directory
6. **Determine execution order**: Use the dependency graph from IMPLEMENTATION-MAP.md to order stories; stories without dependencies run in parallel via worktrees by default; use `--sequential` to disable
7. **Single-PR guard**: If `--single-pr` is set, enter legacy flow: create branch `feat/epic-{epicId}-full-implementation`, use rebase-before-merge strategy, create single mega-PR at the end. Skip all per-story PR logic. The legacy flow is preserved unchanged from the pre-epic-0021 behavior.
8. **Dry-run exit**: If `--dry-run` is set, output the execution plan (story order, dependencies, estimated phases) and stop
9. **Resume handling**: If `--resume` is set, run the Resume Workflow (see below) before delegation
10. **Delegate**: For each story in execution order, invoke `/x-dev-lifecycle` with appropriate flags. Branching is delegated to `x-dev-lifecycle` — each story creates its own branch `feat/{storyId}-description` targeting `main`.

## Resume Workflow

When `--resume` is set, the orchestrator loads `execution-state.json` and applies a two-pass reclassification before re-entering the execution loop.

### Step 1 — Reclassify Story Statuses

Apply the following status transitions to every story in the checkpoint:

| Current Status | New Status | Condition |
|----------------|------------|-----------|
| IN_PROGRESS | PENDING | Always (interrupted work) |
| SUCCESS | SUCCESS | Preserved — never re-execute |
| FAILED (retries < MAX_RETRIES) | PENDING | Retry candidate |
| FAILED (retries >= MAX_RETRIES) | FAILED | Retry budget exhausted |
| PARTIAL | PENDING | Treat as interrupted |
| BLOCKED | BLOCKED | Deferred to reevaluation step |
| PENDING | PENDING | No change |

`MAX_RETRIES` defaults to 2. All other story fields (phase, commitSha, retries, summary, duration, findingsCount) are preserved.

### Step 2 — Reevaluate BLOCKED Stories

After reclassification, evaluate each BLOCKED story:

- If `blockedBy` is **undefined** → keep BLOCKED (conservative: unknown dependencies)
- If `blockedBy` is **empty array** → reclassify to PENDING (no dependencies = vacuously satisfied)
- If **all** dependencies in `blockedBy` have status SUCCESS → reclassify to PENDING
- If **any** dependency is non-SUCCESS or missing from the stories map → keep BLOCKED

This is a **single-pass** evaluation (no cascade). Stories unblocked in this pass will not trigger further unblocking of stories that depend on them.

### Step 3 — Resume Execution (formerly Step 4)

After reclassification and branch recovery, feed the updated state into `getExecutableStories()` to determine which stories are ready for execution. Only stories with status PENDING proceed to the execution loop.

## Phase 0.5 — Pre-flight Conflict Analysis

At the start of **each phase N**, before dispatching any stories for that phase, the
orchestrator performs a pre-flight analysis to detect file-level overlaps between stories
in the same phase. Stories with high code overlap are demoted to sequential execution
within phase N, preventing costly merge conflicts during parallel dispatch.
The results are written to `preflight-analysis-phase-{N}.md`, which the core loop
consumes when deciding per-story parallel vs sequential scheduling.

**Skip condition:** When `--sequential` is set, Phase 0.5 is skipped entirely. Log:
`"Pre-flight analysis skipped (sequential mode)"` and proceed directly to Phase 1.
In sequential mode there is no parallel dispatch, so conflict analysis adds no value.

### 0.5.1 Read Implementation Plans

For each story in the current phase N, attempt to read its implementation plan:

1. Compute plan path: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
2. Read the plan file and extract the list of affected files:
   - Look for sections titled "Affected files", "Existing classes to modify", or
     "New classes/interfaces to create"
   - Collect all file paths referenced in those sections
3. If the plan file does not exist for a story:
   - Mark the story as `unpredictable`
   - Log: `"WARNING: No implementation plan for {storyId} — classified as unpredictable"`
   - An `unpredictable` story is treated as a potential conflict with any other story

**Per-story data structure:**
```json
{
  "storyId": "story-XXXX-YYYY",
  "planPath": "plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md",
  "affectedFiles": ["src/main/java/.../File.java", "pom.xml"],
  "hasPlan": true
}
```

### 0.5.2 Build File Overlap Matrix

For each pair of stories (A, B) in the same phase, compute the intersection of their
affected file sets:

1. Intersect `affectedFiles(A)` with `affectedFiles(B)`
2. Record the overlap count: `overlapCount = |intersection|`
3. The matrix is symmetric: `overlap(A, B) == overlap(B, A)` — compute each pair once

### 0.5.3 Classify Overlaps

For each pair with `overlapCount > 0` (or involving an `unpredictable` story), apply
the classification rules in priority order:

| Classification | Criteria | Action |
|----------------|----------|--------|
| `unpredictable` | One or both stories have no implementation plan (`hasPlan: false`) | Demote to sequential execution (conservative) |
| `config-only` | ALL overlapping files are configuration files (`*.yaml`, `*.json`, `*.properties`, `*.toml`, `*.env`, `pom.xml`, `build.gradle`, `package.json`) | Allow parallel dispatch + smart merge (config files are generally merge-friendly) |
| `code-overlap-low` | 1–2 overlapping files are code files (`.ts`, `.java`, `.py`, `.go`, `.rs`, `.kt`) | Allow parallel dispatch with WARNING logged: `"WARNING: Low code overlap ({N} file(s)) between {storyA} and {storyB}"` |
| `code-overlap-high` | 3+ overlapping files are code files | Demote to sequential execution |
| `no-overlap` | Zero overlapping files and both stories have plans | Allow parallel dispatch (no action needed) |

**Per-pair data structure:**
```json
{
  "storyA": "story-XXXX-YYYY",
  "storyB": "story-XXXX-ZZZZ",
  "overlappingFiles": ["UserService.java", "UserRepository.java"],
  "overlapCount": 2,
  "classification": "code-overlap-low",
  "action": "parallel-with-warning"
}
```

### 0.5.4 Generate Adjusted Execution Plan

Based on the classification results, partition stories into two groups:

- **Parallel Batch:** Stories with no overlaps, `config-only` overlaps, or
  `code-overlap-low` overlaps. These are dispatched concurrently via worktrees.
- **Sequential Queue:** Stories involved in `code-overlap-high` or `unpredictable`
  pairs. These are dispatched one at a time after the parallel batch completes.
  The sequential order respects critical path priority (RULE-007).

**Output file:** Save the analysis to `plans/epic-XXXX/plans/preflight-analysis-phase-N.md`
for audit purposes. The file follows this structure:

```markdown
# Pre-flight Conflict Analysis — Phase {N}

## File Overlap Matrix

| Story A | Story B | Overlapping Files | Classification |
|---------|---------|-------------------|----------------|
| story-XXXX-0001 | story-XXXX-0002 | pom.xml | config-only |
| story-XXXX-0001 | story-XXXX-0003 | UserService.java, UserRepository.java, UserController.java | code-overlap-high |
| story-XXXX-0002 | story-XXXX-0003 | — | no-overlap |

## Adjusted Execution Plan

### Parallel Batch
- story-XXXX-0002 (no overlaps)

### Sequential Queue (after parallel batch)
1. story-XXXX-0001 (code-overlap-high with story-XXXX-0003)
2. story-XXXX-0003 (code-overlap-high with story-XXXX-0001)

## Warnings
- story-XXXX-0004: no implementation plan found (classified as unpredictable)
```

### 0.5.5 Integration with Core Loop (Section 1.3)

The adjusted execution plan produced by Phase 0.5 is consumed by the Core Loop:

1. Before calling `getExecutableStories()`, the orchestrator reads the preflight
   analysis for the current phase from `preflight-analysis-phase-N.md`
2. Stories in the **Parallel Batch** are dispatched via worktree parallel dispatch
   (Section 1.4a) as normal
3. Stories in the **Sequential Queue** are removed from the parallel batch and
   enqueued for sequential dispatch (Section 1.4) after the parallel batch completes
4. The sequential queue ordering respects critical path priority (RULE-007)
5. If no preflight analysis exists for a phase (e.g., Phase 0.5 was skipped or
   this is a `--resume` run), all executable stories default to parallel dispatch

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
   - `mode`: `{ parallel: true, skipReview: <from flags>, singlePr: <from flags> }` (default; `parallel` set to `false` when `--sequential` is passed)
5. The returned `ExecutionState` tracks all story statuses, metrics, and integrity gates

### 1.2 Branch Management

The orchestrator does NOT create a branch. Each story creates its own branch via
`x-dev-lifecycle` Phase 0, targeting `main`. The orchestrator remains on `main`
and monitors story PRs.

1. Ensure a clean starting point:
   ```
   git checkout main && git pull origin main
   ```
2. The orchestrator stays on `main` for the entire execution. No epic branch is created.

> **`--single-pr` guard (RULE-009):** When `--single-pr` is set, the legacy flow is
> activated instead: create branch `feat/epic-{epicId}-full-implementation`, use
> rebase-before-merge strategy, and create a single mega-PR at the end. All per-story
> PR logic is skipped. The legacy flow is preserved unchanged from the pre-epic-0021
> behavior.

### 1.3 Core Loop Algorithm

Execute stories phase-by-phase in dependency order:

```
For each phase in (0..totalPhases-1):
  0. Read preflight analysis for this phase (Phase 0.5 output):
     → Load preflight-analysis-phase-{N}.md if it exists
     → Extract parallelBatch and sequentialQueue story lists
     → If no preflight analysis exists, treat all stories as parallel-eligible
  1. Call getExecutableStories(parsedMap, executionState)
     → Returns stories sorted by critical path priority (RULE-007)
     → Only PENDING stories with all dependencies SUCCESS are returned
  2. If no executable stories and some remain PENDING:
     → Phase is blocked; log warning and advance to next phase
  3. Partition executable stories using preflight analysis:
     a. parallelStories = stories in parallelBatch (or all, if no preflight)
     b. sequentialStories = stories in sequentialQueue (or empty, if no preflight)
  4. Dispatch parallelStories via worktree dispatch (Section 1.4a)
  5. After parallel batch completes, dispatch sequentialStories one at a time
     via sequential dispatch (Section 1.4), in critical path priority order
  6. For each dispatched story (parallel or sequential):
     a. updateStoryStatus(epicDir, storyId, { status: "IN_PROGRESS" })
     b. Dispatch subagent (see 1.4 or 1.4a)
     c. Validate result (see 1.5)
     d. Update checkpoint (see 1.6)
  7. [Placeholder: integrity gate between phases — story-0005-0006]
  8. [Placeholder: progress reporting — story-0005-0013]
  9. Re-read checkpoint via readCheckpoint(epicDir) for next iteration
```

The loop ensures that:
- Stories are dispatched in dependency-safe order
- BLOCKED stories are never dispatched (filtered by `getExecutableStories`)
- Each phase completes before the next begins (parallel dispatch is default; sequential when `--sequential` is set)
- Pre-flight conflict analysis partitions stories into parallel and sequential groups to minimize merge conflicts
- [Placeholder: partial execution filter — story-0005-0009]

### 1.4 Subagent Dispatch (Sequential Mode — When `--sequential` Is Set)

When `--sequential` flag is set, use sequential dispatch. For each executable story, launch a clean-context subagent using the `Agent` tool:

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): The orchestrator passes ONLY metadata to the subagent.
  Never pass source code, knowledge packs, or diffs. The subagent is born with
  clean context and dies after completion.

**Prompt Template for Subagent:**
```
You are implementing story {storyId} for epic {epicId}.

Story file: plans/epic-{epicId}/story-{storyId}.md
Branch: {branchName}
Phase: {currentPhase}
Skip review: {skipReview}

Execute the x-dev-lifecycle workflow:
1. Read the story file for requirements
2. Create implementation plan
3. Implement following TDD (Red-Green-Refactor)
4. Run tests and verify coverage
5. Commit changes with Conventional Commits
6. Create PR and run reviews (Phases 4-8 of x-dev-lifecycle)

The PR created by /x-dev-lifecycle Phase 6 MUST:
- Target `main` branch
- Include "Part of EPIC-{epicId}" in the PR body for traceability (RULE-008)

Include prUrl and prNumber in your SubagentResult JSON.

Return a JSON result with this exact structure (SubagentResult):
{
  "status": "SUCCESS" | "FAILED" | "PARTIAL",
  "commitSha": "<git commit SHA if SUCCESS>",
  "findingsCount": <number of review findings>,
  "summary": "<brief description of what was done>",
  "reviewsExecuted": { "specialist": true|false, "techLead": true|false },
  "reviewScores": { "specialist": "N/M", "techLead": "N/M" },
  "coverageLine": <line coverage percentage>,
  "coverageBranch": <branch coverage percentage>,
  "tddCycles": <number of Red-Green-Refactor cycles>,
  "prUrl": "<URL of the created PR if SUCCESS>",
  "prNumber": <PR number if SUCCESS>
}
```

### 1.4a Parallel Worktree Dispatch (Default Behavior)

Default behavior. When `--sequential` is NOT set, all executable stories in the
current phase are launched concurrently via worktree dispatch in a SINGLE message.

**Activation:** Default behavior. Only when `--sequential` flag is set, the sequential
dispatch in Section 1.4 is used instead.

> **Legacy flag:** If `--parallel` is passed, it is silently ignored (no error). The
> parallel behavior is already the default. This graceful handling ensures backward
> compatibility for at least 1 version cycle.

**Dispatch Algorithm:**

1. Call `getExecutableStories(parsedMap, executionState)` to get all executable stories for the current phase
2. For each executable story, mark `IN_PROGRESS` via `updateStoryStatus(epicDir, storyId, { status: "IN_PROGRESS" })`
3. Launch ALL stories in a SINGLE message using the `Agent` tool with `isolation: "worktree"`:

```
For each story in executableStories:
  Agent(
    subagent_type: "general-purpose",
    isolation: "worktree",
    prompt: "<same prompt template as Section 1.4, with story-specific metadata>"
  )
```

Each worktree subagent uses the same prompt template as Section 1.4, including
the PR creation instructions: PR targets `main`, PR body includes
"Part of EPIC-{epicId}" (RULE-008), and `SubagentResult` includes `prUrl` and `prNumber`.

**Branch Naming:** Each worktree operates on branch `feat/{storyId}-short-description` (standard story branch pattern, matching `x-dev-lifecycle` Phase 0).

**Context Isolation (RULE-001):** Each worktree subagent receives clean context,
identical to sequential mode. The orchestrator passes ONLY metadata (story ID,
branch, phase, flags). The `isolation: "worktree"` parameter ensures each subagent
works on an isolated copy of the repository.

4. Wait for ALL subagents to complete
5. Validate each `SubagentResult` using Section 1.5 rules
6. Each story's PR targets `main` directly — no merge into an epic branch is needed

### 1.4c Conflict Resolution Subagent

[Placeholder: Conflict Resolution Subagent adaptado para per-story PR — story-0021-0009]

> This section will be reimplemented by story-0021-0009 with an auto-rebase +
> automatic conflict resolution subagent adapted for the per-story PR model.
> The new implementation handles conflicts between parallel PRs targeting `main`,
> not between worktree branches and an epic branch.

### 1.4d Worktree Cleanup

After the merge phase completes, clean up worktree resources:

- **SUCCESS + merged:** Worktree is cleaned up automatically after successful merge
- **FAILED stories:** Worktree is preserved for diagnostic investigation. The branch
  and worktree path are logged for manual inspection
- **No-change worktrees:** The `Agent` tool with `isolation: "worktree"` automatically
  cleans up worktrees where no changes were made

### 1.5 Result Validation (RULE-008)

After receiving the subagent response, validate the `SubagentResult` contract:

1. **`status` field**: MUST be present, MUST be one of: `SUCCESS`, `FAILED`, `PARTIAL`
2. **`findingsCount` field**: MUST be present and be a number
3. **`summary` field**: MUST be present and be a string
4. **`commitSha` field**: If `status === "SUCCESS"`, MUST be present and be a string
5. **`prUrl` field**: If `status === "SUCCESS"`, MUST be present and be a valid GitHub PR URL string
6. **`prNumber` field**: If `status === "SUCCESS"`, MUST be present and be a positive integer

**On validation failure:**
- Mark the story as FAILED
- Set summary to: `"Invalid subagent result: missing {field} field"`
- Continue to checkpoint update (1.6)

> **Note:** `prUrl` and `prNumber` are only required when `status === "SUCCESS"`.
> When status is `FAILED` or `PARTIAL`, these fields may be null (the lifecycle
> may have failed before reaching Phase 6 PR creation).

[Placeholder: retry with error context — story-0005-0007]

### 1.6 Checkpoint Update (RULE-002)

After each story completes (success or failure), persist the result:

1. Call `updateStoryStatus(epicDir, storyId, update)` where update contains:
   - `status`: The validated status (`SUCCESS`, `FAILED`, or `PARTIAL`)
   - `commitSha`: The commit SHA (if status is `SUCCESS`)
   - `findingsCount`: Number of review findings from the subagent
   - `summary`: Brief description of what the subagent accomplished
2. Update metrics: increment `storiesCompleted` counter
3. The checkpoint is persisted atomically to `execution-state.json` via the checkpoint engine
4. Between story completions, the checkpoint always reflects the current execution state

### 1.6b Markdown Status Sync

After updating the execution-state.json checkpoint (Section 1.6), propagate the status
change to markdown files. This is executed by the orchestrator (not the subagent) to ensure
consistency across local files and external systems.

**On status SUCCESS:**

1. Read `plans/epic-{epicId}/story-{storyId}.md`
2. Update `**Status:**` field from `Pendente` (or `Em Andamento`) to `Concluída`
3. Write the updated story file

4. Read `plans/epic-{epicId}/IMPLEMENTATION-MAP.md`
5. Find the row matching `story-{storyId}` in the Section 1 dependency matrix
6. Update the Status column to `Concluída`
7. Write the updated Implementation Map

8. Jira transition (if story has a Jira key):
   a. Read the `**Chave Jira:**` field from the story file
   b. If it contains a real key (not `—` or `<CHAVE-JIRA>`):
      - Call `mcp__atlassian__getTransitionsForJiraIssue` to get available transitions
      - Find the transition to "Done" (match name containing "Done", "Concluído", "Resolved")
      - Call `mcp__atlassian__transitionJiraIssue`
      - If transition fails: log warning, continue (non-blocking)

**On status FAILED or PARTIAL:**

1. Update story file `**Status:**` to `Falha` or `Parcial` respectively
2. Update IMPLEMENTATION-MAP Status column to `Falha` or `Parcial`
3. Do NOT transition Jira issue

**On status IN_PROGRESS:**

1. Update story file `**Status:**` to `Em Andamento`
2. Update IMPLEMENTATION-MAP Status column to `Em Andamento`

**Status Mapping:**

| Checkpoint Status | Markdown Status | Jira Transition |
|---|---|---|
| SUCCESS | Concluída | Done |
| FAILED | Falha | — |
| PARTIAL | Parcial | — |
| IN_PROGRESS | Em Andamento | — |
| BLOCKED | Bloqueada | — |
| PENDING | Pendente | — |

**Epic-level completion check:**

After updating story status to SUCCESS, check if ALL stories in the epic have status SUCCESS
in the checkpoint. If yes:
1. Read `plans/epic-{epicId}/EPIC-{epicId}.md`
2. Update the `**Status:**` field from `Em Andamento` to `Concluído`
3. If the epic has a Jira key (not `—` or `<CHAVE-JIRA>`):
   - Call `mcp__atlassian__getTransitionsForJiraIssue` with the epic's Jira key
   - Find the transition to "Done"
   - Call `mcp__atlassian__transitionJiraIssue`
   - If transition fails: log warning, continue (non-blocking)

### 1.7 Extension Points

The following sections are placeholders for downstream stories:

- [Placeholder: integrity gate between phases — story-0005-0006]
- [Placeholder: retry + block propagation — story-0005-0007]
- [Placeholder: resume from checkpoint — story-0005-0008]
- [Placeholder: partial execution filter — story-0005-0009]
- [Placeholder: progress reporting — story-0005-0013]

### Integrity Gate (Between Phases)

After ALL stories in a phase complete, dispatch an integrity gate subagent before advancing to the next phase.

#### Gate Subagent Prompt

Launch a `general-purpose` subagent:

> You are an **Integrity Gate Validator** for {{PROJECT_NAME}}.
>
> **Step 1 — Compile:** Run `{{COMPILE_COMMAND}}` (e.g., `tsc --noEmit`).
> **Step 2 — Test:** Run `{{TEST_COMMAND}}` to execute the full test suite (not just current phase tests).
> **Step 3 — Coverage:** Run `{{COVERAGE_COMMAND}}` to collect coverage metrics.
> **Step 4 — Evaluate:**
> - If compilation fails → `{ status: "FAIL", testCount: 0, coverage: 0 }`
> - If any tests fail → correlate failed tests with commits from stories in the current phase
> - If line coverage < 95% or branch coverage < 90% → FAIL with coverage details
> - Otherwise → proceed to Step 5
> **Step 5 — Smoke Gate:** Execute the full smoke test suite as a regression validation.
> - If `--skip-smoke-gate` flag is set → log `"Integrity gate smoke tests skipped (--skip-smoke-gate)"` and record `smokeGate.status = "SKIP"` → proceed to PASS
> - Run: `{{SMOKE_COMMAND}}` (e.g., `cd java && mvn verify -P integration-tests`)
> - This runs ALL smoke tests, not just those for stories in the current phase
> - If all smoke tests pass → record `smokeGate.status = "PASS"` → overall gate is PASS
> - If any smoke test fails → correlate failures with stories in the current phase (based on files touched) → record `smokeGate.status = "FAIL"` → overall gate is FAIL
>
> Return: `{ status: "PASS"|"FAIL", testCount, coverage, branchCoverage?, failedTests?, regressionSource?, smokeGate?: { status, testsRun, testsFailed, failedTests?, suspectedStories? } }`

#### Regression Diagnosis

If tests fail, the subagent:
1. Analyzes which tests broke (`failedTests` array)
2. Correlates failed tests with commits from stories in the current phase (via `git log`)
3. Identifies the most likely story as regression source (`regressionSource`)
4. If identified: orchestrator executes `git revert <commitSha>` for that story
5. Story is marked FAILED with summary: `"Regression detected by integrity gate"`
6. Block propagation is executed for dependents of the failed story

#### Smoke Gate Regression Diagnosis

If smoke tests fail (Step 5), the subagent:
1. Identifies which smoke tests failed (`smokeGate.failedTests` array)
2. Correlates failures with stories in the current phase by analyzing files touched by each story's commits
3. Populates `smokeGate.suspectedStories` with the story IDs most likely responsible
4. Logs: `"INTEGRITY GATE SMOKE FAILURE: Phase {N}. {count} test(s) failed. Suspected stories: [{list}]"`
5. The phase is marked as FAILED in the checkpoint
6. The operator decides: `--resume` to retry after manual fix, or `--skip-smoke-gate` to bypass

#### Gate Result Registration

```
updateIntegrityGate(epicDir, phaseNumber, {
  status: "PASS" | "FAIL",
  testCount: number,
  coverage: number,        // line coverage %
  branchCoverage?: number, // branch coverage %
  failedTests?: string[],
  regressionSource?: string, // story ID
  smokeGate?: {
    status: "PASS" | "FAIL" | "SKIP",
    testsRun: number,
    testsFailed: number,
    failedTests?: string[],
    suspectedStories?: string[],
    timestamp: string        // ISO-8601
  }
});
```

- **PASS**: Advance to next phase (requires both test gate and smoke gate to pass)
- **FAIL + regression identified**: revert + mark FAILED + block propagation
- **FAIL + regression unidentified**: pause execution, report to user
- **FAIL (smoke gate)**: phase marked FAILED; operator uses `--resume` after fix or `--skip-smoke-gate` to bypass

#### Checkpoint Smoke Gate Format

The `smokeGate` field is added to each phase entry in `execution-state.json`:

```json
{
  "phases": {
    "0": {
      "status": "SUCCESS",
      "smokeGate": {
        "status": "PASS",
        "testsRun": 45,
        "testsFailed": 0,
        "failedTests": [],
        "suspectedStories": [],
        "timestamp": "2026-03-25T14:30:00Z"
      }
    }
  }
}
```

#### Gate Enforcement (RULE-004)

The integrity gate is **mandatory** — there is no bypass. Every phase transition requires a PASS gate
result. The gate runs after phase 0, 1, 2, and 3 — one gate per phase.

The smoke gate within the integrity gate is also mandatory by default. It can only be bypassed with
the `--skip-smoke-gate` flag, which records `smokeGate.status = "SKIP"` in the checkpoint. When
`--skip-smoke-gate` is set, the integrity gate evaluates only Steps 1-4 (compile, test, coverage).
When not set, the smoke gate (Step 5) must also pass for the overall integrity gate to pass.

> **Note:** Each story already executes its own smoke gate via `x-dev-lifecycle` (Phase 2.5).
> The integrity gate smoke tests serve as an ADDITIONAL regression validation — they ensure
> that the combination of all stories in a phase did not break the overall smoke test suite.

## Phase 2 — Consolidation (Two-Wave)

After all stories complete (or reach terminal state), the orchestrator runs a
two-wave consolidation. Wave 1 launches independent subagents in parallel;
Wave 2 waits for both results before creating the PR. Each action is dispatched
to a clean-context subagent (RULE-001) to keep the orchestrator's context lightweight.

**Skip condition:** If NO stories have status SUCCESS, skip consolidation entirely.
Log: `"No successful stories — skipping consolidation"` and proceed to Phase 3.

### Wave 1 — Parallel Review + Report (SINGLE message)

**CRITICAL:** Both subagents 2.1 and 2.2 MUST be launched in a SINGLE message (RULE-003).
They are independent: 2.1 reads the git diff (branch vs main), while 2.2 reads the
checkpoint on disk (`execution-state.json` + template). No data dependency exists between them.

When `--skip-review` is set, Wave 1 launches ONLY subagent 2.2 (Report Generation).
No Tech Lead Review subagent is launched. The `{{FINDINGS_SUMMARY}}` field in the
report is populated with `"Review skipped by user"`.

#### 2.1 Tech Lead Review Subagent

Dispatch a subagent that executes `x-review-pr` logic on the full epic diff:

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): pass only branch name and base branch

**Prompt Template:**
```
You are a Tech Lead reviewing the full diff for epic {epicId}.

Branch: feat/epic-{epicId}-full-implementation
Base: main

Execute the x-review-pr review logic on the complete diff (all commits
from all stories). Return a JSON ReviewResult:
{
  "score": "XX/40",
  "decision": "GO" | "NO-GO",
  "findings": [{ "item": "...", "severity": "...", "suggestion": "..." }]
}
```

**Result Handling:**
- On SUCCESS: record `ReviewResult` in checkpoint atomically (RULE-002)
- On subagent failure: log `"WARNING: Tech Lead Review failed — continuing without review"`, continue (review is informational, not blocking)
- The review covers the COMPLETE epic diff (branch vs main), not individual stories

#### 2.2 Report Generation Subagent

Dispatch a subagent that generates `epic-execution-report.md`:

**Subagent Prompt:**
```
You are generating the epic execution report for EPIC-{epicId}.

1. Read template: _TEMPLATE-EPIC-EXECUTION-REPORT.md
2. Read checkpoint: execution-state.json from the epic directory
3. Resolve ALL {{PLACEHOLDER}} tokens with real data:
   - {{EPIC_ID}}, {{BRANCH}}, {{STARTED_AT}}, {{FINISHED_AT}}
   - {{STORIES_COMPLETED}}, {{STORIES_FAILED}}, {{STORIES_BLOCKED}}, {{STORIES_TOTAL}}
   - {{COMPLETION_PERCENTAGE}}: completed/total × 100
   - {{PHASE_TIMELINE_TABLE}}: phase start/end times from checkpoint
   - {{STORY_STATUS_TABLE}}: per-story status with commit SHAs
   - {{FINDINGS_SUMMARY}}: set to "Pending review" (replaced by Wave 2 with actual data)
   - {{COVERAGE_BEFORE}}, {{COVERAGE_AFTER}}, {{COVERAGE_DELTA}}
   - {{TDD_COMPLIANCE_TABLE}}: TDD compliance per-story table (see step 3a)
   - {{TDD_SUMMARY}}: TDD compliance aggregated summary (see step 3b)
   - {{COMMIT_LOG}}: git log main..HEAD --oneline
   - {{UNRESOLVED_ISSUES}}: findings with severity >= Medium
   - {{PR_LINK}}: populated after PR creation (or "Pending")

3a. Populate {{TDD_COMPLIANCE_TABLE}} with per-story metrics:
   For each completed story, derive TDD compliance data at the story level (do NOT reuse a single
   phase-level object for multiple stories):
   - Prefer story-scoped integrity gate data, if available (e.g., a `tddComplianceByStory[storyId]`
     entry in execution-state.json that is explicitly keyed to that story).
   - If no story-scoped data is available, compute metrics directly from git history:
     - Identify commits associated with the story (commits on the story branch or whose messages
       include the story ID).
     - Classify each commit as "TDD" or "non-TDD" using the same rules as the integrity gate.
     - TDD Commits: count of commits classified as TDD (`tddCommitCount`).
     - Total Commits: total number of commits associated with that story (`totalCommits`).
   - From the story-level counts:
     - TDD %:
       - If `totalCommits > 0`: `round(tddCommitCount / totalCommits * 100)` (integer)
       - If `totalCommits == 0`: `N/A` (do NOT perform the division)
     - TPP Progression:
       - If `totalCommits > 0`: evaluate commit order — OK (strictly degenerate→complex),
         WARNING (partial order), N/A (insufficient data)
       - If `totalCommits == 0`: `N/A (no commits)`
     - Status:
       - If TDD % is numeric: PASS (TDD % >= 80%), WARNING (TDD % >= 50% and < 80%), FAIL (TDD % < 50%)
       - If TDD % is `N/A` due to `totalCommits == 0`: WARNING (missing commit data)
   - If no reliable TDD data can be computed for a story (legacy epic or insufficient commit metadata):
     - Fill all columns with N/A for that story
   Format each row as: `| {storyId} | {tddCommits} | {totalCommits} | {tddPct} | {tppProgression} | {status} |`

3b. Populate {{TDD_SUMMARY}} with aggregated metrics:
   - If TDD compliance data is available for at least one story (at least one non-N/A row):
     - Total TDD Commits: sum of all stories' tddCommitCount
     - Total Commits: sum of all stories' totalCommits
     - Aggregate TDD %:
       - If `totalCommits > 0`: `round(totalTddCommits / totalCommits * 100)`
       - If `totalCommits == 0`: `N/A` (do NOT perform the division)
     - Count stories by status: N PASS / N WARNING / N FAIL (excluding N/A rows)
     - Epic Status:
       - PASS if zero stories have FAIL status and Aggregate TDD % is numeric
       - FAIL if any story has FAIL status or Aggregate TDD % is N/A due to zero total commits
     Format as:
     `| Total | {totalTdd} | {totalCommits} | {aggPct} | — | {epicStatus} |`
     `Stories: {passCount} PASS / {warnCount} WARNING / {failCount} FAIL`
   - If NO TDD compliance data is available for any story:
     Format as: `N/A — no TDD compliance data available (legacy epic without integrity gate or insufficient data)`

4. Validate: no unresolved {{...}} placeholders remain in output
5. Write epic-execution-report.md to plans/epic-{epicId}/
```

**Parallel-mode note:** Because 2.2 runs concurrently with 2.1 in Wave 1, the
`ReviewResult` may not yet be available when 2.2 populates `{{FINDINGS_SUMMARY}}`.
Therefore, 2.2 MUST populate `{{FINDINGS_SUMMARY}}` with the placeholder value
`"Pending review"`. Wave 2 (Section 2.3) replaces this placeholder with the actual
review findings after both subagents complete.

**Validation:** After generation, scan the output file for any remaining `{{...}}`
patterns (excluding the expected `"Pending review"` in `{{FINDINGS_SUMMARY}}`).
If found, log a warning with the unresolved placeholder names.

**Result Handling:**
- On SUCCESS: report written to `plans/epic-{epicId}/epic-execution-report.md`. Update checkpoint atomically (RULE-002)
- On FAILURE: log `"ERROR: Report generation failed"`, continue to Wave 2 (PR created without report)

### Wave 1 Result Handling

After both subagents in Wave 1 complete (or the single subagent when `--skip-review`
is set), collect and evaluate results before proceeding to Wave 2:

| 2.1 Result | 2.2 Result | Action |
|------------|------------|--------|
| SUCCESS | SUCCESS | Replace `"Pending review"` in report with actual findings; create PR with full report |
| FAILURE | SUCCESS | Log warning; replace `"Pending review"` with `"Review unavailable"`; create PR without review score in title |
| SUCCESS | FAILURE | Log ERROR; create PR with minimalist body extracted from checkpoint; include review score |
| FAILURE | FAILURE | Log ERROR for both; create PR with minimalist body from checkpoint data directly |
| SKIPPED (`--skip-review`) | SUCCESS | `{{FINDINGS_SUMMARY}}` already contains `"Review skipped by user"`; create PR without review section |
| SKIPPED (`--skip-review`) | FAILURE | Log ERROR; create PR with minimalist body; no review section |

**Checkpoint timing (RULE-002):** Each subagent's result is recorded in the checkpoint
atomically as soon as it completes. The checkpoint is the single source of truth for
Wave 2 to determine what data is available.

### Wave 2 — PR Creation (after Wave 1 completes)

Wave 2 executes ONLY after all Wave 1 subagents have completed. It collects the
results from the checkpoint and creates the PR.

#### 2.3 PR Creation

Before creating the PR, replace the `"Pending review"` placeholder in the report
with the actual review data from Wave 1:

1. **Replace placeholder in report:** If 2.1 succeeded, read `ReviewResult` from
   checkpoint and replace `"Pending review"` in `epic-execution-report.md` with the
   actual `{{FINDINGS_SUMMARY}}` content (consolidated findings from the review).
   If 2.1 failed, replace with `"Review unavailable"`.
   If `--skip-review` was set, the value is already `"Review skipped by user"` (no replacement needed).
2. **Handle missing report:** If 2.2 failed (no report file on disk), create a
   minimalist PR body directly from checkpoint data (story counts, coverage metrics,
   review score if available). Skip report-related steps.

Push the epic branch and create a PR via `gh` CLI:

3. **Push:** `git push -u origin feat/epic-{epicId}-full-implementation`
4. **Title format:**
   - Full completion: `feat(epic): implement EPIC-{epicId} — {title}`
   - Partial completion: `[PARTIAL] feat(epic): implement EPIC-{epicId} — {title}`
   - Include `[PARTIAL]` when completion percentage < 100%
   - When 2.1 failed or was skipped, omit review score from title
5. **Body structure:**
   ```
   ## Summary
   - Stories completed: {completed}/{total}
   - Stories failed: {failed}
   - Stories blocked: {blocked}
   - Completion: {percentage}%

   ## Tech Lead Review
   - Score: {score} ({decision}: GO or NO-GO)

   ## Metrics
   - Line coverage: {lineCoverage}%
   - Branch coverage: {branchCoverage}%

   ## Report
   - See `plans/epic-{epicId}/epic-execution-report.md`
   ```
6. **Create:** `gh pr create --title "{title}" --body "{body}" --base main`

**Error handling:** If `git push` fails (e.g., remote not accessible), log the error,
generate the report without a PR link, and persist the failure in checkpoint.

### 2.4 Partial Completion Handling

Consolidation executes regardless of whether all stories succeeded:

- If stories are FAILED: the report lists them with failure reasons and summaries
- If stories are BLOCKED: the report lists them with unsatisfied dependency chains
- The PR body explicitly indicates partial implementation with counts
- The PR title includes `[PARTIAL]` when any story is not SUCCESS
- The tech lead review still runs on whatever code was produced

### 2.5 Checkpoint Finalization

After consolidation actions complete, persist final state:

1. Register PR URL in checkpoint: `updateCheckpoint(epicDir, { prUrl })`
2. Register report path: `updateCheckpoint(epicDir, { reportPath })`
3. Set `finishedAt` timestamp
4. Persist final `execution-state.json` with all metrics

## Phase 3 — Verification

Final verification validates the epic as a whole before declaring completion.

### 3.1 Epic-Level Test Suite

Run the full test suite on the epic branch to validate cross-story integration:

1. Execute: `{{TEST_COMMAND}}` (all unit, integration, and API tests)
2. Coverage thresholds (non-negotiable): >=95% line, >=90% branch
3. If any test fails: log failures, mark epic as requiring attention
4. Record coverage results in checkpoint for the report

### 3.2 DoD Checklist Validation

Verify the Definition of Done (DoD) for the epic:

- [ ] All stories completed (or documented as FAILED/BLOCKED in report)
- [ ] Coverage thresholds met (>=95% line, >=90% branch)
- [ ] Zero compiler/linter warnings
- [ ] Tech lead review executed (Phase 2.1 — Wave 1) or skipped via `--skip-review`
- [ ] Epic execution report generated with no unresolved placeholders (Phase 2.2 — Wave 1)
- [ ] `"Pending review"` placeholder replaced with actual findings (Phase 2.3 — Wave 2)
- [ ] PR created or failure documented (Phase 2.3 — Wave 2)
- [ ] All findings with severity >= Medium addressed or documented

### 3.3 Final Status Determination

Compute the final epic status based on story outcomes:

- **COMPLETE**: All stories reached SUCCESS status and all DoD items pass
- **PARTIAL**: Some stories FAILED or BLOCKED, but critical path stories succeeded
- **FAILED**: One or more critical path stories failed

Persist final status to checkpoint: `updateCheckpoint(epicDir, { finalStatus })`

### 3.4 Completion Output

Display the final summary to the user:

```
Epic: EPIC-{epicId} — {title}
Status: COMPLETE | PARTIAL | FAILED
Stories: {completed}/{total} completed, {failed} failed, {blocked} blocked
Coverage: line {lineCoverage}%, branch {branchCoverage}%
Tech Lead: {score} ({decision})
PR: {prUrl}
Report: plans/epic-{epicId}/epic-execution-report.md
Elapsed: {totalElapsedTime}
```

Return to main branch: `git checkout main && git pull origin main`

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story execution), `x-story-map` (if map missing, via error guidance)
- Invokes: `x-review-pr` (tech lead review on full epic diff, Phase 2.1 — Wave 1 parallel)
- Uses: `gh pr create` (PR creation with summary body, Phase 2.3 — Wave 2 sequential)
- Phase 2 uses Two-Wave consolidation: Wave 1 dispatches 2.1 + 2.2 in parallel (SINGLE message, RULE-003); Wave 2 (2.3) runs after both complete
- Reads: `_TEMPLATE-EPIC-EXECUTION-REPORT.md` (report template), `execution-state.json` (checkpoint data)
- Reads: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (implementation plans for pre-flight conflict analysis, Phase 0.5)
- Writes: `plans/epic-XXXX/plans/preflight-analysis-phase-N.md` (pre-flight analysis output for audit, Phase 0.5)
- Phase 0.5 is skipped when `--sequential` is set (no parallel dispatch means no conflict risk)
- All `{{PLACEHOLDER}}` tokens are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
- Integrity gate includes smoke tests (Step 5) as regression validation after each phase — runs `{{SMOKE_COMMAND}}` (e.g., `cd java && mvn verify -P integration-tests`)
- Smoke gate is bypassed with `--skip-smoke-gate` flag; result recorded as `smokeGate.status = "SKIP"` in checkpoint
- Per-story smoke tests run via `x-dev-lifecycle` Phase 2.5; integrity gate smoke tests are an additional cross-story regression check
