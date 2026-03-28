---
name: x-dev-epic-implement
description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[EPIC-ID] [--phase N] [--story story-XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--sequential] [--skip-smoke-gate]"
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

## Prerequisites Check

Before execution, validate all prerequisites in order. Abort on first failure.

### 1. Epic Directory

Check that `docs/stories/epic-XXXX/` exists (where XXXX is the parsed epic ID).

```
ERROR: Directory docs/stories/epic-{epicId}/ not found. Run /x-story-epic-full first.
```

### 2. Epic File

Check that `EPIC-XXXX.md` exists in the epic directory.

```
ERROR: EPIC-{epicId}.md not found in docs/stories/epic-{epicId}/. Run /x-story-epic first.
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
7. **Create branch**: `git checkout -b feat/epic-{epicId}-implementation`
8. **Dry-run exit**: If `--dry-run` is set, output the execution plan (story order, dependencies, estimated phases) and stop
9. **Resume handling**: If `--resume` is set, run the Resume Workflow (see below) before delegation
10. **Delegate**: For each story in execution order, invoke `/x-dev-lifecycle` with appropriate flags

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
| REBASING | PENDING | Interrupted during rebase — retry from scratch |
| REBASE_SUCCESS | PENDING | Rebase completed but merge not done — retry merge |
| REBASE_FAILED | PENDING | Rebase failed — retry candidate (counts as retry) |

`MAX_RETRIES` defaults to 2. All other story fields (phase, commitSha, retries, summary, duration, findingsCount) are preserved.

### Step 2 — Reevaluate BLOCKED Stories

After reclassification, evaluate each BLOCKED story:

- If `blockedBy` is **undefined** → keep BLOCKED (conservative: unknown dependencies)
- If `blockedBy` is **empty array** → reclassify to PENDING (no dependencies = vacuously satisfied)
- If **all** dependencies in `blockedBy` have status SUCCESS → reclassify to PENDING
- If **any** dependency is non-SUCCESS or missing from the stories map → keep BLOCKED

This is a **single-pass** evaluation (no cascade). Stories unblocked in this pass will not trigger further unblocking of stories that depend on them.

### Step 3 — Branch Recovery

Checkout the branch recorded in the checkpoint: `git checkout {state.branch}`. If the branch does not exist locally, attempt `git checkout -b {state.branch} origin/{state.branch}`.

### Step 4 — Resume Execution

After reclassification and branch recovery, feed the updated state into `getExecutableStories()` to determine which stories are ready for execution. Only stories with status PENDING proceed to the execution loop.

## Phase 0.5 — Pre-flight Conflict Analysis

At the start of **each phase N**, before dispatching any stories for that phase, the
orchestrator performs a pre-flight analysis to detect file-level overlaps between stories
in the same phase. Stories with high code overlap are demoted to sequential execution
within phase N, preventing costly merge conflicts during parallel dispatch (Section 1.4b).
The results are written to `preflight-analysis-phase-{N}.md`, which the core loop
consumes when deciding per-story parallel vs sequential scheduling.

**Skip condition:** When `--sequential` is set, Phase 0.5 is skipped entirely. Log:
`"Pre-flight analysis skipped (sequential mode)"` and proceed directly to Phase 1.
In sequential mode there is no parallel dispatch, so conflict analysis adds no value.

### 0.5.1 Read Implementation Plans

For each story in the current phase N, attempt to read its implementation plan:

1. Compute plan path: `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
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
  "planPath": "docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md",
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

**Output file:** Save the analysis to `docs/stories/epic-XXXX/plans/preflight-analysis-phase-N.md`
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
   - `branch`: `feat/epic-{epicId}-full-implementation`
   - `stories`: Array of `{ id, phase }` from step 3
   - `mode`: `{ parallel: true, skipReview: <from flags> }` (default; set to `false` when `--sequential` is passed)
5. The returned `ExecutionState` tracks all story statuses, metrics, and integrity gates

### 1.2 Branch Management

1. Ensure a clean starting point:
   ```
   git checkout main && git pull origin main
   ```
2. Create the epic branch:
   ```
   git checkout -b feat/epic-{epicId}-full-implementation
   ```
3. If the branch already exists (resume scenario):
   ```
   git checkout feat/epic-{epicId}-full-implementation
   ```
   [Placeholder: resume from checkpoint — story-0005-0008]

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

Story file: docs/stories/epic-{epicId}/story-{storyId}.md
Branch: {branchName}
Phase: {currentPhase}
Skip review: {skipReview}

Execute the x-dev-lifecycle workflow:
1. Read the story file for requirements
2. Create implementation plan
3. Implement following TDD (Red-Green-Refactor)
4. Run tests and verify coverage
5. Commit changes with Conventional Commits

Return a JSON result with this exact structure (SubagentResult):
{
  "status": "SUCCESS" | "FAILED" | "PARTIAL",
  "commitSha": "<git commit SHA if SUCCESS>",
  "findingsCount": <number of review findings>,
  "summary": "<brief description of what was done>"
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

**Branch Naming:** Each worktree operates on branch `feat/epic-{epicId}-{storyId}`.

**Context Isolation (RULE-001):** Each worktree subagent receives clean context,
identical to sequential mode. The orchestrator passes ONLY metadata (story ID,
branch, phase, flags). The `isolation: "worktree"` parameter ensures each subagent
works on an isolated copy of the repository.

4. Wait for ALL subagents to complete before proceeding to merge (Section 1.4b)
5. Validate each `SubagentResult` using Section 1.5 rules

### 1.4b Merge Strategy — Rebase-Before-Merge (After Parallel Dispatch)

After all parallel subagents complete, merge their worktree branches sequentially
into the epic branch, ordered by critical path priority (RULE-007). Each branch
(except the first) is rebased onto the updated epic branch before merging. This
eliminates spurious conflicts caused by stale base commits when multiple worktree
branches were created from the same starting point.

**Rationale:** All worktree branches originate from the same base commit. After
merging story A, the epic branch has advanced, but story B's branch still points
to the old base. A direct `git merge` of story B may produce spurious conflicts
on shared files (e.g., `pom.xml`, `build.gradle`, `CHANGELOG.md`). Rebasing
story B onto the updated epic branch gives git full context of what was already
merged, drastically reducing conflict rates in epics with 4+ parallel stories.

**Merge Algorithm (Rebase-Before-Merge):**

1. Collect all stories with `status: "SUCCESS"` from parallel dispatch results
2. Sort by critical path priority: `sortByCriticalPath(successStories, parsedMap)`
3. Initialize tracking: `alreadyMergedStories = []`, `alreadyMergedCommits = []`
4. For each SUCCESS story (in critical path order):

   **Case A — First story (no rebase needed):**
   a. Attempt fast-forward merge: `git merge --ff-only feat/epic-{epicId}-{storyId}`
   b. If fast-forward succeeds:
      - Call `updateStoryStatus(epicDir, storyId, { status: "SUCCESS", commitSha })` (RULE-002)
      - Append storyId to `alreadyMergedStories`, append commitSha to `alreadyMergedCommits`
   c. If fast-forward fails (divergent history):
      - Fall back to: `git merge feat/epic-{epicId}-{storyId}`
      - If merge succeeds: update checkpoint as SUCCESS, append to tracking arrays
      - If merge conflict detected: dispatch conflict resolution subagent (Section 1.4c)
        with `alreadyMergedStories=[]` (first story, no prior merges)
      - If resolution succeeds: commit merge, update checkpoint as SUCCESS
      - If resolution fails: mark story as FAILED, trigger failure handling

   **Case B — Subsequent stories (rebase before merge):**
   a. Update checkpoint: `updateStoryStatus(epicDir, storyId, { status: "REBASING" })`
   b. Checkout the story branch: `git checkout feat/epic-{epicId}-{storyId}`
   c. Rebase onto updated epic branch: `git rebase feat/epic-{epicId}-full-implementation`
   d. If rebase succeeds:
      - Update checkpoint: `updateStoryStatus(epicDir, storyId, { status: "REBASE_SUCCESS" })`
      - Switch back: `git checkout feat/epic-{epicId}-full-implementation`
      - Merge (fast-forward only): `git merge --ff-only feat/epic-{epicId}-{storyId}`
      - Update checkpoint: `updateStoryStatus(epicDir, storyId, { status: "SUCCESS", commitSha })` (RULE-002)
      - Append storyId to `alreadyMergedStories`, append commitSha to `alreadyMergedCommits`
   e. If rebase conflicts detected:
      - Checkpoint already shows `REBASING` status
      - Dispatch conflict resolution subagent (Section 1.4c) with additional context:
        - `alreadyMergedStories`: IDs of stories already merged into epic branch
        - `alreadyMergedCommits`: SHAs of commits already in epic branch
        - `rebaseSourceBranch`: the story branch being rebased
        - `conflictFiles`: list of files with conflicts
      - If resolution succeeds:
        - `git rebase --continue`
        - Switch back: `git checkout feat/epic-{epicId}-full-implementation`
        - Merge (fast-forward only): `git merge --ff-only feat/epic-{epicId}-{storyId}`
        - Update checkpoint as SUCCESS, append to tracking arrays
      - If resolution fails:
        - `git rebase --abort` (restore branch to pre-rebase state)
        - Update checkpoint: `updateStoryStatus(epicDir, storyId, { status: "REBASE_FAILED", summary })`
        - Then: `updateStoryStatus(epicDir, storyId, { status: "FAILED", summary })`
        - Trigger block propagation for dependent stories (per story-0005-0007)

5. For stories with `status: "FAILED"` or `PARTIAL` from subagent dispatch:
   - Do NOT attempt merge or rebase
   - Persist failure to checkpoint before delegating:
     `updateStoryStatus(epicDir, storyId, { status: "FAILED", summary, lastAttemptSha })`
     (RULE-002). This MUST move the story out of `IN_PROGRESS` so it is not left stuck
     in the checkpoint from the 1.4a dispatch phase.
   - Then delegate to failure handling (retry + block propagation per story-0005-0007)

**Checkpoint States for Rebase-Before-Merge:**

| State | Description |
|-------|-------------|
| `REBASING` | Rebase in progress for this story onto the updated epic branch |
| `REBASE_SUCCESS` | Rebase completed successfully, merge pending |
| `REBASE_FAILED` | Rebase failed, conflict resolution attempted or story marked FAILED |

These intermediate states enable precise resume behavior: if the orchestrator crashes
during a rebase, the checkpoint indicates exactly where the process was interrupted.

**Checkpoint Timing (RULE-002):** The checkpoint is updated after each state
transition (REBASING → REBASE_SUCCESS → SUCCESS, or REBASING → REBASE_FAILED → FAILED),
ensuring atomic persistence. If the orchestrator crashes mid-rebase or mid-merge,
the checkpoint reflects the last completed state transition.

### 1.4c Conflict Resolution Subagent

When a merge or rebase conflict is detected during the rebase-before-merge strategy
(Section 1.4b), dispatch a conflict resolution subagent to attempt automatic resolution.

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): pass only branch names, conflict file list, and
  already-merged context (stories and commits already integrated into the epic branch)

**Prompt Template:**
```
You are resolving a {conflictType} conflict for epic {epicId}.

Epic branch: feat/epic-{epicId}-full-implementation
Worktree branch: feat/epic-{epicId}-{storyId}
Conflict type: {conflictType}  (one of: "rebase" | "merge")
Conflict files: {conflictFileList}

Already merged context:
- Stories already merged into epic branch: {alreadyMergedStories}
- Commit SHAs already in epic branch: {alreadyMergedCommits}
- Branch being rebased (if rebase conflict): {rebaseSourceBranch}

Instructions:
1. Analyze the diff from both branches for each conflict file
2. Consider the already-merged stories — their changes are intentional and should
   be preserved. The current story's changes should be integrated on top of them.
3. For rebase conflicts: resolve each conflicting hunk so that the rebased branch
   cleanly applies on top of the updated epic branch
4. For merge conflicts: resolve preserving the intent of both branches
5. Stage resolved files:
   - For rebase conflicts: `git add <resolved files>` (do NOT commit; rebase --continue handles it)
   - For merge conflicts: `git add <resolved files>` and commit the merge resolution
6. Return a JSON result:
{
  "status": "SUCCESS" | "FAILED",
  "summary": "<description of resolution or reason for failure>"
}

If the conflict is irresolvable (semantic contradictions, incompatible changes),
return status FAILED with a clear explanation.
```

**On Resolution FAILED:**
- For rebase conflicts: execute `git rebase --abort` to restore the branch to its pre-rebase state
- Mark the story as FAILED (via REBASE_FAILED intermediate state for rebase conflicts)
- Trigger block propagation for dependent stories (per story-0005-0007)
- Preserve the worktree for manual diagnosis (see Section 1.4d)

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

**On validation failure:**
- Mark the story as FAILED
- Set summary to: `"Invalid subagent result: missing {field} field"`
- Continue to checkpoint update (1.6)

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

1. Read `docs/stories/epic-{epicId}/story-{storyId}.md`
2. Update `**Status:**` field from `Pendente` (or `Em Andamento`) to `Concluída`
3. Write the updated story file

4. Read `docs/stories/epic-{epicId}/IMPLEMENTATION-MAP.md`
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
1. Read `docs/stories/epic-{epicId}/EPIC-{epicId}.md`
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
> - Otherwise → proceed to Step 4.5
> **Step 4.5 — TDD Compliance Check:** Analyze git log for each completed story in the current phase to verify test-first commit patterns.
> For each story, run: `git log --oneline <story-range>`
> 1. **Suffix Verification:** Count commits with suffix `[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, or `[TDD:REFACTOR]`. If `tddCommitCount == 0` → status `FAIL` with message: `"No TDD commits found. Verify that commits follow the format defined in x-dev-implement Step 2."`
> 2. **Order Verification:** If fine-grained commits (`[TDD:RED]`, `[TDD:GREEN]`) exist, verify that each `[TDD:RED]` precedes its corresponding `[TDD:GREEN]`. If `[TDD:GREEN]` appears before `[TDD:RED]` → `redBeforeGreen = false`, add warning: `"Commit order violation: [TDD:GREEN] before [TDD:RED]"`
> 3. **Test-Production Ratio:** Count commits that modify test files vs production files. Compute `testProductionRatio = testCommits / totalCommits`. If `testProductionRatio < 0.4` → add warning: `"Test-production ratio below 0.4 threshold"`
> 4. **TPP Progression:** Verify that earlier test commits (UT) are simpler than later ones (heuristic based on test line count). This is advisory only — no status change.
>
> **Status determination:**
> - `FAIL`: `tddCommitCount == 0` (no TDD commits found — blocking)
> - `WARNING`: TDD commits exist but verification is partial (combined `[TDD]` format only, order violation, or low test-production ratio)
> - `PASS`: Fine-grained commits with `[TDD:RED]` before `[TDD:GREEN]` and `testProductionRatio >= 0.4`
>
> Return `tddCompliance` result (see Gate Result Registration below).
> After Step 4.5 → proceed to Step 5
> **Step 5 — Smoke Gate:** Execute the full smoke test suite as a regression validation.
> - If `--skip-smoke-gate` flag is set → log `"Integrity gate smoke tests skipped (--skip-smoke-gate)"` and record `smokeGate.status = "SKIP"` → proceed to PASS
> - Run: `{{SMOKE_COMMAND}}` (e.g., `cd java && mvn verify -P integration-tests`)
> - This runs ALL smoke tests, not just those for stories in the current phase
> - If all smoke tests pass → record `smokeGate.status = "PASS"` → overall gate is PASS
> - If any smoke test fails → correlate failures with stories in the current phase (based on files touched) → record `smokeGate.status = "FAIL"` → overall gate is FAIL
>
> Return: `{ status: "PASS"|"FAIL", testCount, coverage, branchCoverage?, failedTests?, regressionSource?, tddCompliance: { status, tddCommitCount, totalCommits, redBeforeGreen, testProductionRatio, warnings }, smokeGate?: { status, testsRun, testsFailed, failedTests?, suspectedStories? } }`

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
  tddCompliance: {
    status: "PASS" | "WARNING" | "FAIL",
    tddCommitCount: number,   // commits with [TDD], [TDD:RED], [TDD:GREEN], [TDD:REFACTOR]
    totalCommits: number,     // total commits in the story
    redBeforeGreen: boolean,  // true if fine-grained RED precedes GREEN (true if no fine-grained)
    testProductionRatio: number, // ratio of test-modifying commits to total (0.0 to 1.0)
    warnings: string[]        // list of warnings (empty if PASS)
  },
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

- **PASS**: Advance to next phase (requires test gate and smoke gate to pass; tddCompliance WARNING is non-blocking)
- **FAIL + regression identified**: revert + mark FAILED + block propagation
- **FAIL + regression unidentified**: pause execution, report to user
- **FAIL (tddCompliance)**: `tddCommitCount == 0` — phase marked FAIL with message; operator must add TDD commits and `--resume`
- **FAIL (smoke gate)**: phase marked FAILED; operator uses `--resume` after fix or `--skip-smoke-gate` to bypass

#### Checkpoint Phase Entry Format

The `tddCompliance` and `smokeGate` fields are added to each phase entry in `execution-state.json`:

```json
{
  "phases": {
    "0": {
      "status": "SUCCESS",
      "tddCompliance": {
        "status": "PASS",
        "tddCommitCount": 5,
        "totalCommits": 12,
        "redBeforeGreen": true,
        "testProductionRatio": 0.58,
        "warnings": []
      },
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

The TDD compliance check (Step 4.5) is mandatory and cannot be bypassed. A `WARNING` status is
non-blocking (gate still passes), but a `FAIL` status (zero TDD commits) blocks the phase transition.

The smoke gate within the integrity gate is also mandatory by default. It can only be bypassed with
the `--skip-smoke-gate` flag, which records `smokeGate.status = "SKIP"` in the checkpoint. When
`--skip-smoke-gate` is set, the integrity gate evaluates only Steps 1-4 and Step 4.5 (compile, test, coverage, TDD compliance).
When not set, the smoke gate (Step 5) must also pass for the overall integrity gate to pass.

> **Note:** Each story already executes its own smoke gate via `x-dev-lifecycle` (Phase 2.5).
> The integrity gate smoke tests serve as an ADDITIONAL regression validation — they ensure
> that the combination of all stories in a phase did not break the overall smoke test suite.

#### TDD Compliance Check (Step 4.5) — Detailed Specification

Step 4.5 implements **Level 2** of RULE-007 (Multi-Level Verification). It runs after Step 4 (Evaluate) and before Step 5 (Smoke Gate) to verify that commits from completed stories follow the test-first pattern defined in `x-dev-implement`.

**Input:** Git log for each story completed in the current phase.

**4 Verification Checks:**

1. **Suffix Verification:** Scan commit messages for suffixes `[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, or `[TDD:REFACTOR]`. Count matching commits as `tddCommitCount`. If `tddCommitCount == 0`, the check immediately returns `FAIL`.

2. **Order Verification:** If fine-grained commits (`[TDD:RED]`, `[TDD:GREEN]`) exist, verify that each `[TDD:RED]` precedes its corresponding `[TDD:GREEN]` in chronological order. Set `redBeforeGreen = true` if all pairs are correctly ordered; `false` if any `[TDD:GREEN]` appears before its `[TDD:RED]`.

3. **Test-Production Ratio:** For all commits in the story range, classify each as modifying test files or production files (based on file paths containing `/test/`, `/tests/`, `*_test.*`, `*.test.*`, `*.spec.*`). Compute `testProductionRatio = testCommits / totalCommits`. Values below `0.4` indicate insufficient test-first discipline.

4. **TPP Progression:** Compare line counts of earlier test commits (UT-1, UT-2) with later ones. Earlier tests should be simpler (fewer lines) than later tests, following Transformation Priority Premise ordering. This check is advisory — it produces warnings but does not change the overall status.

**Status Criteria:**

| Condition | Status | Message |
|-----------|--------|---------|
| `tddCommitCount == 0` | **FAIL** | `"No TDD commits found. Verify that commits follow the format defined in x-dev-implement Step 2."` |
| Only combined `[TDD]` commits (no `[TDD:RED]`/`[TDD:GREEN]`) | **WARNING** | `"No [TDD:RED] commits found -- using combined [TDD] format"` |
| Fine-grained with `[TDD:GREEN]` before `[TDD:RED]` | **WARNING** | `"Commit order violation: [TDD:GREEN] before [TDD:RED]"` |
| `testProductionRatio < 0.4` | **WARNING** | `"Test-production ratio below 0.4 threshold"` |
| Fine-grained with RED before GREEN and `testProductionRatio >= 0.4` | **PASS** | (no warnings) |

**Severity:** TDD compliance is **WARNING-level**, not blocking **FAIL**, except when `tddCommitCount == 0` (which is blocking FAIL). Rationale: combined `[TDD]` commits make automated verification imperfect; the definitive assessment occurs at Level 3 (Tech Lead review in `x-review-pr`).

**TDD Compliance Result Structure:**

```json
{
  "tddCompliance": {
    "status": "PASS | WARNING | FAIL",
    "tddCommitCount": 5,
    "totalCommits": 12,
    "redBeforeGreen": true,
    "testProductionRatio": 0.58,
    "warnings": []
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `tddCompliance.status` | Enum(`PASS`, `WARNING`, `FAIL`) | Yes | Overall TDD compliance status |
| `tddCompliance.tddCommitCount` | Integer | Yes | Commits with `[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, or `[TDD:REFACTOR]` suffix |
| `tddCompliance.totalCommits` | Integer | Yes | Total commits in the story |
| `tddCompliance.redBeforeGreen` | Boolean | Yes | `true` if fine-grained RED precedes GREEN (`true` if no fine-grained commits) |
| `tddCompliance.testProductionRatio` | Float | Yes | Ratio of test-modifying commits to total (0.0 to 1.0) |
| `tddCompliance.warnings` | Array[String] | Yes | List of warnings (empty if PASS) |

**Multi-Level Verification (RULE-007) Cross-Reference:**

| Level | Location | Behavior |
|-------|----------|----------|
| 1 | `x-dev-lifecycle` Phase 0 | ABORT if test plan is missing (story-0014-0002) |
| **2** | **Integrity Gate Step 4.5** | **WARNING/FAIL on TDD compliance (this step)** |
| 3 | `x-review-pr` Tech Lead review | NO-GO if TDD items fail (story-0014-0007) |

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

3a. Populate {{TDD_COMPLIANCE_TABLE}} from integrity gate data:
   For each completed story, read `tddCompliance` from the phase entry in execution-state.json.
   - If `tddCompliance` data is available for the story:
     - TDD Commits: `tddCompliance.tddCommitCount`
     - Total Commits: `tddCompliance.totalCommits`
     - TDD %: `round(tddCommitCount / totalCommits * 100)` (integer)
     - TPP Progression: evaluate commit order — OK (strictly degenerate→complex),
       WARNING (partial order), N/A (insufficient data)
     - Status: PASS (TDD % >= 80%), WARNING (TDD % >= 50% and < 80%), FAIL (TDD % < 50%)
   - If `tddCompliance` data is NOT available (legacy epic without integrity gate data):
     - Fill all columns with N/A for that story
   Format each row as: `| {storyId} | {tddCommits} | {totalCommits} | {tddPct}% | {tppProgression} | {status} |`

3b. Populate {{TDD_SUMMARY}} with aggregated metrics:
   - If TDD compliance data is available for at least one story:
     - Total TDD Commits: sum of all stories' tddCommitCount
     - Total Commits: sum of all stories' totalCommits
     - Aggregate TDD %: round(totalTddCommits / totalCommits * 100)
     - Count stories by status: N PASS / N WARNING / N FAIL
     - Epic Status: PASS if zero stories have FAIL status, FAIL otherwise
     Format as:
     `| Total | {totalTdd} | {totalCommits} | {aggPct}% | — | {epicStatus} |`
     `Stories: {passCount} PASS / {warnCount} WARNING / {failCount} FAIL`
   - If NO TDD compliance data is available for any story:
     Format as: `N/A — no TDD compliance data available (legacy epic without integrity gate)`

4. Validate: no unresolved {{...}} placeholders remain in output
5. Write epic-execution-report.md to docs/stories/epic-{epicId}/
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
- On SUCCESS: report written to `docs/stories/epic-{epicId}/epic-execution-report.md`. Update checkpoint atomically (RULE-002)
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
   - See `docs/stories/epic-{epicId}/epic-execution-report.md`
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
- [ ] TDD compliance checks passed for all phases (Step 4.5 — no FAIL status)
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
Report: docs/stories/epic-{epicId}/epic-execution-report.md
Elapsed: {totalElapsedTime}
```

Return to main branch: `git checkout main && git pull origin main`

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story execution), `x-story-map` (if map missing, via error guidance)
- Invokes: `x-review-pr` (tech lead review on full epic diff, Phase 2.1 — Wave 1 parallel)
- Uses: `gh pr create` (PR creation with summary body, Phase 2.3 — Wave 2 sequential)
- Phase 2 uses Two-Wave consolidation: Wave 1 dispatches 2.1 + 2.2 in parallel (SINGLE message, RULE-003); Wave 2 (2.3) runs after both complete
- Reads: `_TEMPLATE-EPIC-EXECUTION-REPORT.md` (report template), `execution-state.json` (checkpoint data)
- Reads: `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (implementation plans for pre-flight conflict analysis, Phase 0.5)
- Writes: `docs/stories/epic-XXXX/plans/preflight-analysis-phase-N.md` (pre-flight analysis output for audit, Phase 0.5)
- Phase 0.5 is skipped when `--sequential` is set (no parallel dispatch means no conflict risk)
- All `{{PLACEHOLDER}}` tokens are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
- Integrity gate includes smoke tests (Step 5) as regression validation after each phase — runs `{{SMOKE_COMMAND}}` (e.g., `cd java && mvn verify -P integration-tests`)
- Integrity gate includes TDD compliance check (Step 4.5) — verifies test-first commit patterns, `[TDD]` suffixes, RED-before-GREEN ordering, and test-production ratio for each completed story (RULE-007 Level 2)
- Smoke gate is bypassed with `--skip-smoke-gate` flag; result recorded as `smokeGate.status = "SKIP"` in checkpoint
- Per-story smoke tests run via `x-dev-lifecycle` Phase 2.5; integrity gate smoke tests are an additional cross-story regression check
