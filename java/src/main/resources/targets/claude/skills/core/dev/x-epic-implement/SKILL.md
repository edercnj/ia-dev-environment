---
name: x-epic-implement
description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via explicit git worktrees (per ADR-0004 §D2 and Rule 14). Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-story-implement. EPIC-0038 simplification: epic orchestrator handles ONLY story-level concerns (phase order, story PR management, epic-level verification). Task management (TDD cycles, atomic commits per task, coalesced handling) is fully delegated to x-story-implement's v2 wave dispatcher. Tasks are invisible at the epic level."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent, AskUserQuestion, TaskCreate, TaskUpdate
argument-hint: "[EPIC-ID] [--phase N] [--story story-XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--sequential] [--single-pr] [--auto-merge] [--no-merge] [--interactive-merge] [--strict-overlap] [--skip-pr-comments] [--auto-approve-pr] [--batch-approval] [--manual-batch-approval] [--non-interactive] [--task-tracking] [--dry-run-only-comments] [--revert-on-failure]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## CONTEXT MANAGEMENT

Do NOT read full files into context when partial data suffices.
Use targeted reads (offset/limit) or grep for specific fields.

- **Checkpoint reads**: Use selective checkpoint reads. When checking story status in `execution-state.json`, grep for the specific story ID and extract only `status` and `prMergeStatus` fields. Do NOT load the entire `execution-state.json` into context.
- **Phase reports**: Delegate phase completion report generation to a dedicated subagent. The orchestrator receives only `{ "status": "GENERATED", "path": "plans/epic-{epicId}/reports/phase-{N}-completion-{epicId}.md" }`. Full report content stays in the subagent context and is written to file.
- **Review dashboards**: Reference review outputs by file path, not by inline content. After a review skill completes, record only the path and score summary.
- **Story results**: After each story dispatch, record only the SubagentResult JSON (status, commitSha, summary, prUrl, prNumber). Do NOT echo full implementation logs.

# Skill: Epic Implementation (Orchestrator)

## Purpose

Orchestrate the implementation of an entire epic by executing stories sequentially or in parallel via explicit git worktrees. Parse epic ID and flags, validate prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), delegate story execution to `x-story-implement` subagents running inside orchestrator-provisioned worktrees (per [ADR-0004](../../../adr/ADR-0004-worktree-first-branch-creation-policy.md) §D2 and [Rule 14](../../rules/14-worktree-lifecycle.md)), manage checkpoints, integrity gates, retry/block propagation, resume, partial execution, dry-run, and progress reporting.

> **Branch Creation Policy (ADR-0004 + Rule 14).** For parallel story execution this skill creates an explicit git worktree per dispatched story via the `x-git-worktree` skill BEFORE launching the subagent. The dispatched subagent runs `x-story-implement`, whose Phase 0 Step 6a detects `inWorktree == true` and selects Mode 1 (REUSE) — no nested worktree is created (Rule 14 §3). The orchestrator is the creator and OWNS removal per Rule 14 §5. The legacy `Agent(isolation:"worktree")` harness parameter is DEPRECATED (ADR-0004 §D1) and MUST NOT be used anywhere in this skill.

## When to Use

- Full epic implementation spanning multiple stories
- Multi-story orchestration with dependency-aware execution order
- Resumable epic execution after interruption
- Parallel story execution via explicit git worktrees (ADR-0004 §D2, Rule 14)

## Workflow Overview

```
Phase 0:   PREPARATION        -> Parse args, validate prerequisites, generate execution plan (inline)
Phase 0.5: PRE-FLIGHT         -> Conflict analysis for parallel stories (conditional, inline)
Phase 1:   EXECUTION LOOP     -> Provision per-story worktrees (x-git-worktree create), dispatch subagents, auto-rebase, remove worktrees on merge (x-git-worktree remove); integrity gates (inline)
Phase 2:   PROGRESS REPORT    -> Consolidate results, generate epic execution report (inline)
Phase 3:   VERIFICATION       -> Epic-level test suite, DoD checklist, final status (inline)
Phase 4:   PR COMMENTS        -> Remediate PR review comments across all story PRs (optional, inline)
```

## Context Budget Decision Logic

Before invoking any skill inline via the `Skill` tool, evaluate the accumulated context budget:

1. Check the `context-budget` field in the target skill's frontmatter (light/medium/heavy)
2. Track the accumulated budget of all skills loaded inline in the current conversation
3. Apply the delegation rule:
   - If the accumulated budget is `heavy` and the next skill is `medium` or `heavy`: delegate via `Agent` tool (subagent) instead of `Skill` tool (inline)
   - If the accumulated budget is `medium` and the next skill is `heavy`: delegate via `Agent` tool
   - Otherwise: invoke inline via `Skill` tool
4. When delegating due to budget: log `"Context budget exceeded ({accumulated}). Delegating {skill-name} to subagent."`

> **Note:** The `context-budget` field is informational only — it does not affect how Claude Code loads the skill. The delegation decision is made by this orchestrator.

## Input Parsing

### Positional Argument (Required)

| Argument | Format | Required | Description |
|----------|--------|----------|-------------|
| `EPIC-ID` | `XXXX` (4-digit zero-padded) | **Mandatory** | The epic identifier, e.g., `0042` |

The epic ID is a required positional argument. If missing, abort immediately:

```
ERROR: Epic ID is required. Usage: /x-epic-implement [EPIC-ID] [flags]
```

### Optional Flags

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `--phase N` | number | (all phases) | Execute only phase N (0-4) |
| `--story story-XXXX-YYYY` | string | (all stories) | Execute only a specific story by ID |
| `--skip-review` | boolean | `false` | Skip review phases in x-story-implement subagents |
| `--dry-run` | boolean | `false` | Generate execution plan without executing |
| `--resume` | boolean | `false` | Continue from last checkpoint (execution-state.json) |
| `--sequential` | boolean | `false` | Disable parallel worktrees, execute stories one at a time |
| ~~`--skip-smoke-gate`~~ | — | — | **REMOVED (EPIC-0042).** Smoke gate is now mandatory when `{{SMOKE_COMMAND}}` is configured. Use `--resume` after fixing smoke failures. |
| `--single-pr` | boolean | `false` | Preserve legacy flow: epic branch + rebase-before-merge + single mega-PR (RULE-009) |
| `--auto-merge` | boolean | `true` | Auto-merge story PRs via `gh pr merge` after reviews approve (RULE-004). This is the DEFAULT behavior (EPIC-0042). Mutually exclusive with `--no-merge` and `--interactive-merge`. |
| `--no-merge` | boolean | `false` | Explicit opt-out: create PRs but skip merge and merge-wait. Dependencies are satisfied by `status == SUCCESS` alone (PR merge not required). Mutually exclusive with `--auto-merge` and `--interactive-merge`. Use for repos with branch protection rules requiring multiple approvers. |
| `--interactive-merge` | boolean | `false` | Opt-in to interactive merge mode: prompt the user at phase boundaries with 3 options (merge all, pause for manual merge, skip merge). Mutually exclusive with `--auto-merge` and `--no-merge`. |
| `--strict-overlap` | boolean | `false` | When set, stories with `code-overlap-high` or `unpredictable` are demoted to sequential queue (original behavior). Without flag, pre-flight is advisory-only (RULE-005). |
| `--skip-pr-comments` | boolean | `false` | Skip PR comment remediation phase (Phase 4). When set, Phase 4 is skipped entirely with log message. |
| `--auto-approve-pr` | boolean | `false` | Propagate to x-story-implement dispatches. Each story creates a parent branch and task PRs auto-merge into it. Parent branches require human review before merging to develop (RULE-004). |
| `--batch-approval` | boolean | `true` | Enable/disable batch approval for parallel story task PRs (RULE-013). When enabled, consolidates pending PRs from parallel stories into a single approval prompt. |
| `--task-tracking` | boolean | `true` | Enable/disable task-level tracking in execution-state.json. When enabled, individual task progress is tracked with PR fields (prUrl, prNumber, branch). |
| `--non-interactive` | boolean | `false` | Skip all interactive gates (AskUserQuestion menus). Auto-approves batch PR gate and passes `--non-interactive` to all `x-story-implement` dispatches. For CI/automation. |
| `--manual-batch-approval` | boolean | `false` | **DEPRECATED (EPIC-0043).** No-op — the gate menu is now the default. Emits one-time warning: `"[DEPRECATED] --manual-batch-approval is no longer needed; the gate menu is now the default."` |
| `--dry-run-only-comments` | boolean | `false` | Suppress auto-apply of PR comment fixes. When set, Phase 4 generates the dry-run report but does not apply fixes without confirmation (EPIC-0042). |
| `--revert-on-failure` | boolean | `false` | Skip agent-assisted regression fix and revert directly on integrity gate failure (EPIC-0042). When set, the original revert behavior is used without attempting auto-remediation. |

## Prerequisites Check

Before execution, validate all prerequisites in order. Abort on first failure.

### 1. Epic Directory

Check that `plans/epic-XXXX/` exists (where XXXX is the parsed epic ID).

```
ERROR: Directory plans/epic-{epicId}/ not found. Run /x-epic-decompose first.
```

### 2. Epic File

Check that `EPIC-XXXX.md` exists in the epic directory.

```
ERROR: EPIC-{epicId}.md not found in plans/epic-{epicId}/. Run /x-epic-create first.
```

### 3. Implementation Map

Check that `IMPLEMENTATION-MAP.md` exists in the epic directory.

```
ERROR: IMPLEMENTATION-MAP.md not found. Run /x-epic-map first.
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
2. Validate that phases 0..N-1 are complete:
   - If `mergeMode == "no-merge"`: all stories must have `status == SUCCESS` (prMergeStatus not checked)
   - Otherwise: all stories must have `status == SUCCESS AND prMergeStatus == "MERGED"`
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
2. Validate that ALL dependencies of the story are satisfied:
   - If `mergeMode == "no-merge"`: dependencies must have `status == SUCCESS` (prMergeStatus not checked)
   - Otherwise: dependencies must have `status == SUCCESS AND prMergeStatus == "MERGED"`
3. If validation fails, abort:
   - Story not in map: `Story {storyId} not found in implementation map`
   - Dependencies not met: `Dependencies not satisfied: [{list}]`
4. Dispatch subagent for the specific story
5. Collect result and update checkpoint
6. Do **not** run integrity gate (single story execution has no integrity gate)

## Phase 0 — Preparation (Orchestrator — Inline)

1. **Parse arguments**: Extract epic ID from positional argument and all optional flags
1b. **Flag validation**: If more than one of `--auto-merge`, `--no-merge`, and `--interactive-merge` are set, abort:
    ```
    ERROR: --auto-merge, --no-merge, and --interactive-merge are mutually exclusive. Use only one.
    ```
    Determine `mergeMode` from flags (EPIC-0042):
    - `--auto-merge` → `mergeMode = "auto"` (this is the DEFAULT when no flag is specified)
    - `--interactive-merge` → `mergeMode = "interactive"`
    - `--no-merge` → `mergeMode = "no-merge"` (explicit opt-out)
    - Neither → `mergeMode = "auto"` (default — EPIC-0042, changed from "no-merge")
    If `--single-pr` is set with `--auto-merge`, `--no-merge`, or `--interactive-merge`, log warning:
    `"WARNING: --single-pr overrides merge mode flags. Per-story PR logic is skipped."`
1c. **Auto-approve-pr propagation**: If `--auto-approve-pr` is set:
    - Record `autoApprovePr = true` in the execution state
    - Each story dispatch will include `--auto-approve-pr` flag to `x-story-implement`
    - Each story creates a parent branch `feat/story-XXXX-YYYY-desc` from `develop`
    - Task PRs within each story target the parent branch (not `develop`)
    - Task PRs are auto-merged into the parent branch after passing CI
    - Parent branches are **never** auto-merged to `develop` — they require human review
    - If `--auto-approve-pr` is set with `--single-pr`, log warning and ignore:
      `"WARNING: --auto-approve-pr is incompatible with --single-pr. Ignoring --auto-approve-pr."`
    - Log: `"Auto-approve mode: task PRs will auto-merge into parent branches. Parent branches require human review."`
1d. **Batch approval config**: Record `batchApproval` flag (default `true`). When `--batch-approval=false`, disable consolidated prompts.
1e. **Task tracking config**: Record `taskTracking` flag (default `true`). When `--task-tracking=false`, task-level fields are omitted from execution-state.json.
2. **Run prerequisites checks**: Execute all 5 prerequisite validations (abort on first failure)
3. **Read IMPLEMENTATION-MAP.md**: Extract the story dependency graph and execution order
4. **Read EPIC-XXXX.md**: Load epic context (title, description, acceptance criteria)
5. **Discover story files**: Glob `story-XXXX-*.md` to collect all story files in the epic directory
6. **Determine execution order**: Use the dependency graph from IMPLEMENTATION-MAP.md to order stories; stories without dependencies run in parallel via worktrees by default; use `--sequential` to disable
7. **Single-PR guard**: If `--single-pr` is set, enter legacy flow: create branch `feat/epic-{epicId}-full-implementation`, use rebase-before-merge strategy, create single mega-PR at the end. Skip all per-story PR logic. The legacy flow is preserved unchanged from the pre-epic-0021 behavior.
8. **Create reports directory**: Create `plans/epic-{epicId}/reports/` if it does not exist. Log: `"Created reports/ directory for EPIC-{epicId}"`. If directory already exists, skip silently.
9. **Generate execution plan**: Run the Execution Plan Persistence workflow (see below). This saves a human-readable execution plan to `plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md` BEFORE any story executes.
10. **Dry-run exit**: If `--dry-run` is set, the execution plan was already saved in step 9. Log: `"Dry-run: execution plan saved to plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md. No stories executed."` and stop. No stories are dispatched.
11. **Resume handling**: If `--resume` is set, run the Resume Workflow (see below) before delegation
12. **Delegate**: For each story in execution order, invoke `/x-story-implement` with appropriate flags. Branching is delegated to `x-story-implement` — each story creates its own branch `feat/{storyId}-description` targeting `develop`. When `--auto-approve-pr` is set, propagate the flag to each `x-story-implement` dispatch so task PRs target the story's parent branch and auto-merge into it.

### Execution Plan Persistence

Before any story executes, the orchestrator persists a human-readable execution plan.
This plan complements the technical `execution-state.json` with an auditable artifact
that Product Owners and reviewers can inspect before authorizing execution.

#### Pre-check (RULE-002 — Idempotency)

1. Compute plan path: `plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md`
2. Check if the plan file exists:
   - If NOT found: generate a new plan. Log: `"Generating execution plan for EPIC-{epicId}"`
   - If found, compare modification times:
     - `mtime(IMPLEMENTATION-MAP.md) <= mtime(execution-plan)` → reuse existing plan. Log: `"Reusing existing execution plan from {date}"`
     - `mtime(IMPLEMENTATION-MAP.md) > mtime(execution-plan)` → regenerate. Log: `"Regenerating execution plan (implementation map modified)"`

#### Plan Generation

1. Read template at `.claude/templates/_TEMPLATE-EPIC-EXECUTION-PLAN.md` for required output format (RULE-007)
2. If template is found: generate the plan following the template structure, filling all `{{PLACEHOLDER}}` tokens with real data from the epic context, implementation map, and story files
3. If template is NOT found (RULE-012 — graceful fallback): log `"WARNING: Template _TEMPLATE-EPIC-EXECUTION-PLAN.md not found, using inline format"` and generate the plan with the following inline format:

```markdown
# Epic Execution Plan — EPIC-{epicId}

> **Epic ID:** EPIC-{epicId}
> **Date:** {currentDate}
> **Total Stories:** {totalStories}
> **Total Phases:** {totalPhases}
> **Mode:** {execute|dry-run}

## Story Execution Order

| Order | Story ID | Phase | Dependencies | Status |
|-------|----------|-------|--------------|--------|
| 1 | story-{epicId}-0001 | 0 | — | Pending |
| 2 | story-{epicId}-0002 | 0 | — | Pending |
| ... | ... | ... | ... | ... |
```

4. Write the plan to `plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md`
5. The plan header MUST include: Epic ID, Date, Author (role), Template Version (RULE-011)

#### Atomic Commit — Execution Plan (V2-gated, story-0046-0005)

After the execution-plan file has been written (or confirmed reused) and
BEFORE entering the wave loop, orchestrators running a v2 epic
(`planningSchemaVersion == "2.0"`) MUST commit the report atomically so that
the working tree stays clean for downstream tools (RULE-046-05, RULE-046-06).

Fallback matrix (Rule 19 — Backward Compatibility):

| `planningSchemaVersion` | Action |
| :--- | :--- |
| absent / `"1.0"` / invalid | **V1 no-op.** Skip the commit block entirely. Reports remain optional and uncommitted (legacy behavior). |
| `"2.0"` | **V2 active.** Execute the 3-step block below. |

Three-step block (V2 only):

1. **Stage**: `git add plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md`
   (idempotent — adding an already-tracked file with no diff is a no-op and
   keeps the block safe across resume).
2. **Build message** via `ReportCommitMessageBuilder.executionPlan(epicId,
   waves, stories)` — produces the canonical subject `docs(epic-{epicId}):
   add execution plan` plus body with wave count, story count, schema tag,
   and `Refs:` line.
3. **Commit** via Rule 13 Pattern 1 INLINE-SKILL:

       Skill(skill: "x-git-commit", args: "--message \"<built message>\"")

   On non-zero exit, abort the epic with exit code
   `REPORT_COMMIT_FAILED` (21) and stream the hook's stderr so the operator
   can diagnose (RULE-046-08 — fail loud). Do NOT fall back to `--no-verify`.

Idempotence: if staging produces no diff (file already committed on a prior
run of the epic), skip the `Skill(x-git-commit)` call with log
`"Reusing committed execution plan for EPIC-{epicId}"` and proceed.

## Resume Workflow

When `--resume` is set, the orchestrator loads `execution-state.json` and applies a two-pass reclassification before re-entering the execution loop.

### Step 1 — Reclassify Story Statuses

Apply the following status transitions to every story in the checkpoint.
For stories with PR fields, verify actual PR status via GitHub CLI.

| Current Status | New Status | Condition |
|----------------|------------|-----------|
| IN_PROGRESS | PENDING | Always (interrupted work) |
| SUCCESS | SUCCESS | Preserved — never re-execute |
| PR_CREATED | PR_CREATED or SUCCESS | Verify via `gh pr view {prNumber} --json state,mergedAt`: if MERGED → SUCCESS; if OPEN → keep PR_CREATED; if not found → FAILED |
| PR_PENDING_REVIEW | PR_PENDING_REVIEW or SUCCESS | Verify via `gh pr view`: if MERGED → SUCCESS; if OPEN → keep PR_PENDING_REVIEW; if not found → FAILED |
| PR_MERGED | SUCCESS | PR merged — story is complete |
| FAILED (retries < MAX_RETRIES) | PENDING | Retry candidate (close open PR if exists) |
| FAILED (retries >= MAX_RETRIES) | FAILED | Retry budget exhausted |
| PARTIAL | PENDING | Treat as interrupted |
| BLOCKED | BLOCKED | Deferred to reevaluation step |
| PENDING | PENDING | No change |

`MAX_RETRIES` defaults to 2. All other story fields (phase, commitSha, retries, summary, duration, findingsCount, prUrl, prNumber, prMergeStatus) are preserved.

#### PR Status Verification

For each story with a `prNumber`, verify the actual PR state:

```
state = gh pr view {prNumber} --json state,mergedAt
if state == "MERGED":
  update prMergeStatus = "MERGED"
  reclassify to SUCCESS
else if state == "OPEN":
  keep current status (PR_CREATED or PR_PENDING_REVIEW)
else if PR not found (error):
  reclassify to FAILED with reason "PR not found"
```

#### Failure Handling — PR Closure

When a story transitions to FAILED and has an open PR:

```
if story.prNumber exists and story.prMergeStatus != "MERGED":
  run: gh pr close {prNumber} --comment "Story failed: {summary}"
  update: story.prMergeStatus = "CLOSED"
```

When a story is retried after failure, the lifecycle creates a new PR
(the old PR was closed). The new `prUrl` and `prNumber` replace the old values.

### Step 1b — Reclassify Task Statuses (Per-Task Resume)

After story-level reclassification (Step 1), apply task-level reclassification for stories with task data:

For each story that has a `tasks` object in `execution-state.json`:

1. **IN_PROGRESS tasks -> PENDING** (interrupted work — task was executing when interruption occurred)
2. **DONE tasks -> DONE** (preserved — completed tasks are never re-executed)
3. **PR_CREATED tasks**: verify via `gh pr view {prNumber} --json state,mergedAt`:
   - If MERGED -> DONE (PR was merged while orchestrator was interrupted)
   - If OPEN -> keep PR_CREATED (still awaiting review)
   - If CLOSED/not found -> FAILED with reason "PR closed or not found"
4. **PR_APPROVED tasks**: verify via `gh pr view`:
   - If MERGED -> DONE
   - If OPEN -> keep PR_APPROVED
   - If CLOSED/not found -> FAILED
5. **PR_MERGED tasks -> DONE** (PR merged — task is complete)
6. **BLOCKED tasks**: if all task dependencies are DONE -> PENDING; otherwise keep BLOCKED
7. **FAILED tasks**: if retries < MAX_RETRIES -> PENDING (retry); otherwise keep FAILED
8. **PENDING tasks -> PENDING** (no change)
9. **SKIPPED tasks -> SKIPPED** (no change — terminal status)

This enables resume at the task level: only incomplete tasks are re-executed, not the entire story. When a story resumes with some tasks DONE, the `x-story-implement` / `x-task-implement` subagent receives the task state and skips DONE tasks automatically.

**Backward Compatibility:** Stories without a `tasks` field in the checkpoint are unaffected by this step. The step is a no-op for stories executed in non-PRE_PLANNED mode or when `version` is `"1.0"` or absent.

### Step 2 — Reevaluate BLOCKED Stories

After reclassification, evaluate each BLOCKED story:

- If `blockedBy` is **undefined** → keep BLOCKED (conservative: unknown dependencies)
- If `blockedBy` is **empty array** → reclassify to PENDING (no dependencies = vacuously satisfied)
- If `mergeMode == "no-merge"`: if **all** dependencies in `blockedBy` have `status == SUCCESS` → reclassify to PENDING (prMergeStatus not checked)
- Otherwise: if **all** dependencies in `blockedBy` have status SUCCESS and `prMergeStatus == "MERGED"` → reclassify to PENDING
- If **any** dependency is non-SUCCESS or missing from the stories map → keep BLOCKED

This is a **single-pass** evaluation (no cascade). Stories unblocked in this pass will not trigger further unblocking of stories that depend on them.

### Step 3 — Resume Execution

After reclassification and PR verification, feed the updated state into `getExecutableStories()` to determine which stories are ready for execution. Only stories with status PENDING proceed to the execution loop. The orchestrator remains on `develop` during resume — no epic branch recovery is needed.

## Phase 0.5 — Pre-flight Conflict Analysis

At the start of **each phase N**, before dispatching any stories for that phase, the
orchestrator performs a pre-flight analysis to detect file-level overlaps between stories
in the same phase. By default (advisory mode), overlaps are logged as warnings but all
stories still execute in parallel. With `--strict-overlap`, stories with high code overlap
are demoted to sequential execution within phase N (RULE-005).
The results are written to `preflight-analysis-phase-{N}.md`, which the core loop
consumes when deciding per-story parallel vs sequential scheduling.

**Skip condition:** When `--sequential` is set, Phase 0.5 is skipped entirely. Log:
`"Pre-flight analysis skipped (sequential mode)"` and proceed directly to Phase 1.
In sequential mode there is no parallel dispatch, so conflict analysis adds no value.

### 0.5.0 Parallelism Gate via `/x-parallel-eval` (EPIC-0041 / story-0041-0006)

Before the plan-file-based overlap matrix below runs, the orchestrator executes the
**parallelism gate** powered by `/x-parallel-eval --scope=epic`. This gate consumes the
File Footprint section of every story (EPIC-0041 / story-0041-0001) to detect collision
pairs at the granularity of declared write / regen sets, which is strictly more accurate
than the implementation-plan heuristic of subsections 0.5.1–0.5.4. The gate follows the
**degrade-with-warning** semantics of RULE-005: execution never aborts; it only adjusts
parallelism per phase and persists the decision in `execution-state.json`.

**Skip condition:** When `--sequential` is set, the gate is skipped alongside the rest
of Phase 0.5 (same outer gate above).

#### 0.5.0.a Invocation

Invoke `x-parallel-eval` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-parallel-eval", args: "--scope=epic --epic <EPIC_FILE>")

The skill returns an exit code plus a Markdown report. Log the invocation line before
the call:

```
[parallelism-gate] /x-parallel-eval --scope=epic --epic plans/epic-XXXX exit=<code>
```

#### 0.5.0.b Exit Code → Action Table

| Exit Code | Classification | Orchestrator Action |
| :--- | :--- | :--- |
| `0` | No conflicts | Proceed with the planned parallelism. No changes to the phase execution plan. No entry added to `parallelismDowngrades`. |
| `1` | Warnings — footprint missing / legacy story | Proceed with the planned parallelism. Log `"[parallelism-gate] WARNING: footprint incompleto, risco residual"`. No entry added to `parallelismDowngrades` (advisory only). |
| `2` | Hard / regen conflicts | **Downgrade the affected phase to serial.** Serialize the conflicting stories (see 0.5.0.c), log a visible warning with the conflict pair table, and append an entry to `execution-state.json` field `parallelismDowngrades` (see 0.5.0.d). |
| Other (3+) | Unexpected | Treat as exit `1` (fail-open) — log WARNING and proceed. Never abort the epic on gate output. |

**Fail-open contract:** If the `x-parallel-eval` skill is not available in the project
(skill file missing under `.claude/skills/x-parallel-eval/` or the invocation errors
out before returning an exit code), log `"[parallelism-gate] gate pulado — /x-parallel-eval indisponível"`
and proceed with the planned parallelism (RULE-005 / RULE-006).

#### 0.5.0.c Downgrade Algorithm (exit code 2)

For each hard-conflict or regen-conflict pair `(storyA, storyB)` reported by
`/x-parallel-eval` inside the current phase `N`:

1. Remove `storyA` and `storyB` from the parallel batch of phase `N`.
2. Append them to a sequential queue at the end of phase `N`, preserving the critical
   path ordering of phase `N` (the story with more dependents first).
3. Every other story in phase `N` that has zero conflicts remains in the parallel batch.
4. The `adjustedSequence` persisted in `execution-state.json` is the list of batches
   produced — each batch is itself a list of story IDs that run together.

Log the decision verbatim (human-readable block):

```
[parallelism-gate] WARNING: <N> hard conflict(s) detected. Phase <N> downgraded from parallel to serial.
[parallelism-gate] Affected pair: story-XXXX-AAAA ↔ story-XXXX-BBBB (shared write: <file>)
[parallelism-gate] Continuing execution with adjusted plan.
```

#### 0.5.0.d Persistence in `execution-state.json`

Append one entry per phase-level downgrade to the top-level array
`parallelismDowngrades`. Legacy state files (without the field) MUST still parse — the
field is optional (backward compatibility, RULE-006 — see `ExecutionState.java`).

```json
{
  "parallelismDowngrades": [
    {
      "phase": 3,
      "originalGroup": ["story-XXXX-0006", "story-XXXX-0007", "story-XXXX-0008"],
      "adjustedSequence": [["story-XXXX-0008"], ["story-XXXX-0006"], ["story-XXXX-0007"]],
      "reason": "hard conflict on SettingsAssembler.java",
      "evaluatedAt": "2026-04-17T10:00:00Z"
    }
  ]
}
```

Fields:

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `phase` | integer | Phase number whose parallelism was downgraded |
| `originalGroup` | list[string] | Story IDs that were originally in the parallel batch |
| `adjustedSequence` | list[list[string]] | Sequence of batches produced by the downgrade (each inner list runs in parallel; outer list is serialized) |
| `reason` | string | Short human-readable cause (e.g., `"hard conflict on SettingsAssembler.java"`) |
| `evaluatedAt` | ISO-8601 timestamp | When the gate ran |

#### 0.5.0.e Relation to Subsections 0.5.1–0.5.5

The gate above runs **first**. After the gate adjusts the phase execution plan, the
existing subsections 0.5.1–0.5.5 still execute, but now operate on the **post-gate**
story set for phase `N`. The two mechanisms are complementary:

- **Gate (0.5.0):** authoritative; based on declared File Footprints; produces
  downgrade decisions.
- **Plan-file matrix (0.5.1–0.5.4):** advisory fallback; based on implementation-plan
  content parsing; produces warnings for stories whose File Footprint was incomplete.

When the gate already downgrades a pair, the plan-file matrix's `code-overlap-high`
classification for the same pair is redundant — it remains informational.

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

### 0.5.4 Generate Execution Plan (Dual-Mode: Advisory / Strict)

The execution plan output depends on the `--strict-overlap` flag (RULE-005):

#### Default Mode (Advisory — no `--strict-overlap`)

All stories are dispatched in parallel regardless of overlap classification.
Overlaps produce warnings but do NOT block parallel execution. With per-story PRs,
conflicts are resolved automatically via auto-rebase (Section 1.4e) after PR merge.

**Output file:** Save the analysis to `plans/epic-XXXX/plans/preflight-analysis-phase-N.md`:

```markdown
# Pre-flight Conflict Analysis — Phase {N}

## File Overlap Matrix

| Story A | Story B | Overlapping Files | Classification |
|---------|---------|-------------------|----------------|
| story-XXXX-0001 | story-XXXX-0002 | pom.xml | config-only |
| story-XXXX-0001 | story-XXXX-0003 | UserService.java, UserRepository.java, UserController.java | code-overlap-high |
| story-XXXX-0002 | story-XXXX-0003 | — | no-overlap |

## Advisory Warnings
- WARNING: code-overlap-high between story-XXXX-0001 and story-XXXX-0003 (3 files: UserService.java, UserRepository.java, UserController.java). Auto-rebase will execute after the first PR of the phase merges.
- WARNING: story-XXXX-0004 has no implementation plan (classified as unpredictable). Monitor PR for conflicts.

## Execution Plan
All stories execute in parallel (advisory warnings above do not block execution).
```

#### Strict Mode (`--strict-overlap`)

When `--strict-overlap` is set, partition stories into two groups (original behavior):

- **Parallel Batch:** Stories with `no-overlap`, `config-only`, or `code-overlap-low`.
- **Sequential Queue:** Stories with `code-overlap-high` or `unpredictable`.
  Sequential order respects critical path priority (RULE-007).

**Output file (strict mode):**

```markdown
# Pre-flight Conflict Analysis — Phase {N}

## File Overlap Matrix
(same table as advisory mode)

## Adjusted Execution Plan
### Parallel Batch
- story-XXXX-0002 (no-overlap)
### Sequential Queue
1. story-XXXX-0001 (code-overlap-high with story-XXXX-0003)
2. story-XXXX-0003 (code-overlap-high with story-XXXX-0001)
3. story-XXXX-0004 (unpredictable — no implementation plan)
```

> **Precedence:** `--sequential` > `--strict-overlap` > default advisory.
> If `--sequential` is set, all stories execute sequentially regardless of
> `--strict-overlap`. If only `--strict-overlap` is set, the partitioning
> applies. If neither is set, all stories execute in parallel with warnings.

### 0.5.5 Integration with Core Loop (Section 1.3)

The execution plan produced by Phase 0.5 is consumed by the Core Loop:

**Default mode (advisory):**
1. The core loop treats all executable stories as parallel-eligible
2. If preflight analysis exists, warnings are logged but do NOT affect dispatch
3. All stories are dispatched via worktree parallel dispatch (Section 1.4a)

**Strict mode (`--strict-overlap`):**
1. Before calling `getExecutableStories()`, the orchestrator reads the preflight
   analysis for the current phase from `preflight-analysis-phase-N.md`
2. Stories in the **Parallel Batch** are dispatched via worktree parallel dispatch
   (Section 1.4a) as normal
3. Stories in the **Sequential Queue** are removed from the parallel batch and
   enqueued for sequential dispatch (Section 1.4) after the parallel batch completes
4. The sequential queue ordering respects critical path priority (RULE-007)

**Common rules:**
5. If no preflight analysis exists for a phase (e.g., Phase 0.5 was skipped or
   this is a `--resume` run), all executable stories default to parallel dispatch
6. With per-story PRs, conflicts from parallel overlap are resolved automatically
   via auto-rebase (Section 1.4e). The `--strict-overlap` mode is recommended for
   epics with many stories editing the same files.

## Phase 1 — Execution Loop

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-1-Execution-Loop`

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
   - `mode`: `{ parallel: true, skipReview: <from flags>, singlePr: <from flags>, mergeMode: "auto"|"no-merge"|"interactive" }` (default; `parallel` set to `false` when `--sequential` is passed; `mergeMode` derived from `--auto-merge`/`--interactive-merge`/`--no-merge` flags or defaults to `"auto"` per EPIC-0042)
5. The returned `ExecutionState` tracks all story statuses, metrics, and integrity gates

**Per-story `StoryEntry` schema in `execution-state.json`:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | Story ID (e.g., `story-0042-0001`) |
| `phase` | Integer | Yes | Phase number |
| `status` | String | Yes | `PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `PARTIAL`, `BLOCKED`, `PR_CREATED`, `PR_PENDING_REVIEW`, `PR_MERGED` |
| `commitSha` | String | When SUCCESS | Last commit SHA |
| `findingsCount` | Integer | Yes | Number of review findings |
| `summary` | String | Yes | Brief description |
| `retries` | Integer | Yes | Number of retry attempts |
| `duration` | Number | When completed | Execution duration in seconds |
| `blockedBy` | String[] | When BLOCKED | IDs of blocking stories |
| `prUrl` | String | When PR created | URL of the story PR |
| `prNumber` | Integer | When PR created | GitHub PR number |
| `prMergeStatus` | String | When PR created | `OPEN`, `MERGED`, `CLOSED` |
| `parentBranch` | String | When auto-approve-pr | Parent branch name (e.g., `feat/story-XXXX-YYYY-desc`). Present only in auto-approve mode. |
| `tasks` | Map<String, TaskEntry> | Optional | Per-task state map (see Section 1.1c). Optional for backward compatibility (RULE-010). |

> See Section 1.4e for additional per-story rebase tracking fields (`rebaseStatus`, `lastRebaseSha`, `rebaseAttempts`).

**Top-level `execution-state.json` fields (in addition to per-story entries):**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `version` | String | Yes | `"2.0"` | Schema version. `"1.0"` or absent = legacy (no task tracking). `"2.0"` = task-centric. |
| `baseBranch` | String | Yes | `"develop"` | Base branch for PRs, auto-rebase, and resume. Used by all stories in the epic. |
| `autoApprovePr` | Boolean | No | `false` | Whether `--auto-approve-pr` is active for this epic execution. |
| `batchApproval` | Boolean | No | `true` | Whether batch approval is enabled for parallel story PRs. |
| `taskTracking` | Boolean | No | `true` | Whether task-level tracking is enabled in execution-state.json. |

### 1.1b DoR Pre-check (Before Story Dispatch)

Before dispatching a story to `x-story-implement`, verify its Definition of Ready:

1. Compute DoR path: `plans/epic-{epicId}/plans/dor-{storyId}.md`
2. Check if the DoR file exists:
   - **File does NOT exist:** Proceed without DoR check (backward compatible, RULE-001). Log: `"No DoR file found, proceeding without DoR check (backward compatible)"`
   - **File exists:** Read the `## Final Verdict` section
     - If verdict == `READY`: Proceed with implementation. Log: `"DoR check PASSED for {storyId}"`
     - If verdict == `NOT_READY`: Mark story as BLOCKED with reason `"DoR not satisfied: {failed_checks}"`. Log: `"DoR check FAILED for {storyId}: {failed_checks}"`. Do NOT dispatch the subagent.

This check is NON-BLOCKING when DoR files don't exist (backward compatibility with epics planned before `x-story-plan` / `x-epic-orchestrate` existed).

The DoR pre-check integrates into the Core Loop (Section 1.3) at step 6a, before `updateStoryStatus`:

```
6. For each dispatched story (parallel or sequential):
   a0. Run DoR Pre-check (Section 1.1b):
       - If DoR file missing: log and proceed
       - If DoR verdict READY: log and proceed
       - If DoR verdict NOT_READY: mark BLOCKED, skip dispatch, continue to next story
   a. updateStoryStatus(epicDir, storyId, { status: "IN_PROGRESS" })
   b. Dispatch subagent (see 1.4 or 1.4a)
   c. Validate result (see 1.5)
   d. Update checkpoint (see 1.6)
```

### 1.1c Per-Task Checkpoint

When a story is being executed in PRE_PLANNED mode (tasks available from `plans/epic-{epicId}/plans/tasks-{storyId}.md`), the `execution-state.json` tracks individual task progress within each story entry:

```json
{
  "version": "2.0",
  "stories": {
    "story-XXXX-YYYY": {
      "status": "IN_PROGRESS",
      "parentBranch": "feat/story-XXXX-YYYY-desc",
      "tasks": {
        "TASK-001": { "status": "DONE", "agent": "architect", "type": "DEV", "commitSha": "abc123", "duration": 45000, "prUrl": "https://github.com/org/repo/pull/50", "prNumber": 50, "branch": "task/TASK-001-domain-model" },
        "TASK-002": { "status": "PR_CREATED", "agent": "qa-engineer", "type": "TEST", "prUrl": "https://github.com/org/repo/pull/51", "prNumber": 51, "branch": "task/TASK-002-unit-tests" },
        "TASK-003": { "status": "PENDING", "agent": "security-engineer", "type": "SEC" }
      }
    }
  }
}
```

**Task Status Values:**

| Status | Meaning | Transitions |
|--------|---------|------------|
| PENDING | Task not started | -> IN_PROGRESS |
| IN_PROGRESS | Task being executed | -> DONE, -> PR_CREATED, -> PENDING (on resume) |
| PR_CREATED | Task PR created (pending review) | -> PR_APPROVED, -> PR_MERGED, -> FAILED |
| PR_APPROVED | Task PR approved | -> PR_MERGED, -> FAILED |
| PR_MERGED | Task PR merged into parent branch | -> DONE |
| DONE | Task completed (PR merged or commit verified) | Terminal |
| BLOCKED | Task blocked by dependency | -> PENDING (when dep DONE) |
| FAILED | Task execution or PR failed | -> PENDING (on retry) |
| SKIPPED | Task not applicable | Terminal |

**Per-Task Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | String (Enum) | Yes | `PENDING`, `IN_PROGRESS`, `PR_CREATED`, `PR_APPROVED`, `PR_MERGED`, `DONE`, `BLOCKED`, `FAILED`, `SKIPPED` |
| `agent` | String | Yes | Agent persona executing the task (e.g., `architect`, `qa-engineer`) |
| `type` | String | Yes | Task type from task breakdown (e.g., `DEV`, `TEST`, `SEC`, `REFACTOR`) |
| `commitSha` | String | When DONE | Commit SHA produced by the task |
| `duration` | Number | When DONE | Execution duration in milliseconds |
| `prUrl` | String | When PR_CREATED | URL of the task PR |
| `prNumber` | Integer | When PR_CREATED | GitHub PR number |
| `branch` | String | When IN_PROGRESS | Branch name for the task |

**Backward Compatibility (RULE-010):** The `tasks` field is OPTIONAL in the `StoryEntry` schema. Stories executed without PRE_PLANNED mode (no task breakdown file) will not have this field. All existing checkpoint logic continues to work without `tasks`. Epics with `version` field absent or set to `"1.0"` are treated as legacy and do not use task-level tracking.

**Schema Version:** The top-level `execution-state.json` includes a `version` field:
- `"1.0"` (or absent): Legacy schema — no task-level tracking
- `"2.0"`: Task-centric schema — `tasks` field is populated when `--task-tracking` is enabled

### 1.2 Branch Management

The orchestrator does NOT create a branch. Each story creates its own branch via
`x-story-implement` Phase 0, targeting `develop`. The orchestrator remains on `develop`
and monitors story PRs.

1. Ensure a clean starting point:
   ```
   git checkout develop && git pull origin develop
   ```
2. The orchestrator stays on `develop` for the entire execution. No epic branch is created.

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
  1. Call getExecutableStories(parsedMap, executionState, mergeMode)
     → Returns stories sorted by critical path priority (RULE-007)
     → Only PENDING stories with all dependencies SUCCESS are returned
     → When mergeMode != "no-merge": also requires prMergeStatus == "MERGED" for dependencies
     → Stories without dependencies (phase 0) skip the PR merge check
  1a. If stories have dependencies with status SUCCESS but prMergeStatus != "MERGED":
     → Enter PR Merge Decision Mechanism (see below) based on mergeMode
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
     e. Markdown Status Sync (Phase 1.7 — see Section 1.7 below): propagate the
        updated status to `story-XXXX-YYYY.md` and to the IMPLEMENTATION-MAP row
        via the helpers from story-0046-0001 / story-0046-0004. V2-gated and
        unskippable on the happy path (Rule 22 RULE-046-04 — non-skippable
        status transition). Failure aborts the loop with exit STATUS_SYNC_FAILED.
     f. Circuit breaker check (Section 1.7b):
        - If story SUCCESS: reset consecutiveFailures to 0
        - If story FAILED: increment consecutiveFailures and totalFailuresInPhase
  7. Run integrity gate between phases (Section 1.7c — Local Integrity Gate)
  8. Post-gate prompt: present options to user (Section 1.7d)
  9. Generate phase completion report (Read references/phase-reports.md)
  10. Re-read checkpoint via readCheckpoint(epicDir) for next iteration

After the Core Loop finishes (last phase completes and all stories are SUCCESS),
the orchestrator enters Phase 5 — Epic Finalization (see Section 5 below) to
transition the epic Status to `Concluído` and atomically commit the epic-closing
diff. Phase 5 is V2-gated and fail-loud (Rule 22 RULE-046-08).
```

The loop ensures that:
- Stories are dispatched in dependency-safe order
- BLOCKED stories are never dispatched (filtered by `getExecutableStories`)
- Each phase completes before the next begins (parallel dispatch is default; sequential when `--sequential` is set)
- Pre-flight conflict analysis partitions stories into parallel and sequential groups to minimize merge conflicts
- Dependencies are verified at lifecycle level (SUCCESS) and optionally at PR level (MERGED) depending on `mergeMode`
- Cross-story task dependencies are enforced before dispatch (Section 1.3a)
- Batch approval consolidates pending PRs from parallel stories (Section 1.3b)
- Circuit breaker (Section 1.7) pauses on 3 consecutive failures and aborts the phase on 5 total failures

#### 1.3a Cross-Story Task Dependency Enforcement

Before dispatching a story, verify that all cross-story task dependencies are satisfied:

```
function checkCrossStoryTaskDeps(storyId, executionState):
  story = executionState.stories[storyId]
  if story.tasks is undefined: return true  // no task-level deps (backward compat)

  for each task in story.tasks:
    if task has crossStoryDeps:
      for each dep in task.crossStoryDeps:
        depStory = executionState.stories[dep.storyId]
        if depStory is undefined: return false
        if depStory.tasks is undefined:
          // Legacy story without tasks — check story-level status
          if depStory.status != SUCCESS: return false
        else:
          depTask = depStory.tasks[dep.taskId]
          if depTask is undefined or depTask.status != "DONE":
            mark storyId as BLOCKED
            reason = "Waiting for {dep.taskId} (status: {depTask.status})"
            return false
  return true
```

This enables fine-grained dependency management: story-B can start as soon as the specific task it depends on in story-A is DONE, without waiting for all of story-A to complete.

#### 1.3b Batch Approval for Parallel Stories (RULE-013) (EPIC-0043)

**Deprecated flag:** If `--manual-batch-approval` is present, emit one-time warning
`"[DEPRECATED] --manual-batch-approval is no longer needed; the gate menu is now the default."`
and continue (no-op).

**Trigger:** After parallel story dispatch completes (Section 1.4a step 4), collect all task PRs
with status `PR_CREATED` or `PR_APPROVED` across the parallel stories.

**`--non-interactive` mode (auto-approve):** When `--non-interactive` is present, auto-approve
all pending PRs without prompting. For each pending PR, execute `gh pr merge {prNumber} --merge`.
Update task status to `PR_MERGED` then `DONE`. Log:
`"Batch auto-approve (--non-interactive): {N} PRs across {M} stories"`
If any merge fails, log warning and retry once. If retry fails, mark the specific task as FAILED.

**Default behavior (interactive menu):** When execution reaches this gate WITHOUT `--non-interactive`,
present the operator with `AskUserQuestion` (Rule 20 — Canonical Option Menu, PR variant).
Display the PR List Table before the menu.

**PR List Table (displayed before options):**

| Story ID | Task ID | PR # | PR URL | Title | Changed Files |
|----------|---------|------|--------|-------|---------------|

**AskUserQuestion call shape:**

```markdown
AskUserQuestion(
  question: "{N} task PRs pending across {M} stories. Review and choose an action.",
  options: [
    {
      header: "Proceed",
      label: "Continue (Recommended)",
      description: "Merge all {N} pending task PRs and continue epic execution."
    },
    {
      header: "Fix PR",
      label: "Run x-pr-fix-epic and retry",
      description: "Invokes x-pr-fix-epic on all epic PRs; reapresents this menu on return."
    },
    {
      header: "Abort",
      label: "Cancel the operation",
      description: "Save execution state and exit. Resume with --resume after manual review."
    }
  ]
)
```

**On PROCEED:** For each pending PR, execute `gh pr merge {prNumber} --merge`. Update task status
to `PR_MERGED` then `DONE`. If any merge fails, log warning and fall back to individual retry.

**On FIX-PR:** Record the attempt in `batchGate.fixAttempts[]` (see schema below). Invoke fix skill
via Rule 13 INLINE-SKILL:

    Skill(skill: "x-pr-fix-epic", args: "--epic {EPIC_ID}")

On return: update `outcome` in the last `FixAttempt`. Reapresent the full gate menu (loop-back).
Guard-rail: if `batchGate.fixAttempts.size() == 3` before selecting FIX-PR, emit
`EPIC_BATCH_FIX_LOOP_EXCEEDED` and terminate automatically:
`"Loop de fix excedeu 3 tentativas no epic {EPIC_ID}; gate encerrado automaticamente.
  Retomar via --resume com --non-interactive ou edição manual do state file."`
No 4th option is presented (total option count remains exactly 3, per RULE-002).

**On ABORT:** Save execution state. Set `batchGate.lastGateDecision = "ABORT"`. Exit the skill.
Resume with `--resume` after manual review.

**`execution-state.json` schema extension (EPIC-0043):**

The top-level `execution-state.json` gains a `batchGate` sub-object:

```json
{
  "batchGate": {
    "lastGateDecision": null,
    "fixAttempts": [
      {
        "attemptNumber": 1,
        "delegateSkill": "x-pr-fix-epic",
        "invokedAt": "2026-04-19T12:00:00Z",
        "outcome": "applied"
      }
    ],
    "waveIndex": 2,
    "schemaVersion": "1.0"
  }
}
```

**Field reference:**

| Field | Type | M/O | Description |
| :--- | :--- | :--- | :--- |
| `batchGate.lastGateDecision` | `Enum \| null` | M | `null` before first interaction; then `PROCEED` \| `FIX_PR` \| `ABORT` |
| `batchGate.fixAttempts` | `List<FixAttempt>` | O (default `[]`) | Each FIX-PR selection appends one entry; max 3 |
| `batchGate.fixAttempts[].attemptNumber` | `Integer` | M | 1-based attempt counter |
| `batchGate.fixAttempts[].delegateSkill` | `String` | M | Always `"x-pr-fix-epic"` |
| `batchGate.fixAttempts[].invokedAt` | `ISO-8601` | M | Timestamp of the invocation |
| `batchGate.fixAttempts[].outcome` | `String` | M | `"applied"` \| `"no-op"` \| `"error"` |
| `batchGate.waveIndex` | `Integer \| null` | O (≥ 0) | Wave of the epic at which the gate was reached; `null` when not wave-partitioned |
| `batchGate.schemaVersion` | `String` | M | `"1.0"` |

**Error code:**

| Code | Condition | Message |
| :--- | :--- | :--- |
| `EPIC_BATCH_FIX_LOOP_EXCEEDED` | 3 consecutive FIX-PR attempts at the batch gate | `"Loop de fix excedeu 3 tentativas no epic {EPIC_ID} wave {WAVE}; gate encerrado automaticamente. Retomar via --resume com --non-interactive ou edição manual do state file."` |

Legacy files (without `batchGate`) are read as `null` and initialized on first write. Log on first
migration: `"EPIC_BATCH_GATE_SCHEMA_LEGACY: execution-state.json sem batchGate; inicializando"`

**Skip condition:** When `--batch-approval=false` or `--auto-approve-pr` is set (task PRs
auto-merge into parent branches), skip batch approval entirely. When `--sequential` is set,
there are no parallel PRs to consolidate.

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
        if depState.prMergeStatus != "MERGED": skip story  // PR merge check (RULE-003)
      // When mergeMode == "no-merge": skip PR merge check entirely
      // Dependencies satisfied by status == SUCCESS alone
    add storyNode to executableList
  return sortByCriticalPath(executableList)
```

#### PR Merge Decision Mechanism (RULE-004)

When stories have dependencies with `status == SUCCESS` but `prMergeStatus != "MERGED"`,
the orchestrator behavior depends on the `mergeMode`:

**1. Auto-merge mode (`mergeMode == "auto"`, via `--auto-merge`):**

For each dependency with an unmerged PR and approved reviews, execute
`gh pr merge {prNumber} --merge`. Merge order follows `sortByCriticalPath()` (RULE-007).
If merge fails (conflict, failing checks), log warning and fall through to polling
(60s interval, 24h timeout). On timeout: mark dependent stories as `BLOCKED` with
reason `"PR merge timeout"`.

**2. No-merge mode (`mergeMode == "no-merge"`, via `--no-merge`):**

Skip PR merge wait entirely. Dependencies are satisfied by `status == SUCCESS` alone.
Log: `"--no-merge: skipping merge wait for PR #{prNumber} (story-{id}). Dependency satisfied by SUCCESS status."`
Proceed immediately to dispatch dependent stories.

When `--no-merge` is active and a dependent story has dependencies with `prMergeStatus == "OPEN"`,
the dependent story's branch must incorporate the dependency's code. Before dispatching the
dependent story, the orchestrator instructs the subagent to merge dependency branches:

```
Before starting implementation, merge dependency branches into your story branch:
  git fetch origin
  for each dependency branch where prMergeStatus == "OPEN":
    git merge origin/feat/{dep-branch} --no-edit
This ensures your story has access to dependency code that has not yet been merged to develop.
```

**3. Interactive mode (`mergeMode == "interactive"`, via `--interactive-merge`):**

After all stories in the current phase complete with `status == SUCCESS`, prompt the user
using `AskUserQuestion`:

```
question: "Phase {N} complete. {count} PR(s) created. How would you like to proceed with merging?"
header: "PR Merge"
options:
  - label: "Merge all and continue"
    description: "Auto-merge all open PRs in this phase via gh pr merge, then proceed to next phase"
  - label: "I will merge manually — pause"
    description: "Pause execution. I will merge PRs manually. Resume with --resume after merging."
  - label: "Skip merge — continue without merging"
    description: "Proceed to next phase without merging. Dependencies satisfied by SUCCESS status only."
multiSelect: false
```

- **"Merge all and continue"**: Execute `gh pr merge {prNumber} --merge` for each open PR
  in the phase (critical path order). On success, update `prMergeStatus = "MERGED"`.
  On failure, log error and fall back to "I will merge manually" behavior.
- **"I will merge manually — pause"**: Save checkpoint and pause execution. The user
  runs `--resume` after manually merging PRs. On resume, PR status is verified via
  `gh pr view`.
- **"Skip merge — continue without merging"**: Behave as `mergeMode == "no-merge"` for
  this phase only. Log warning. Proceed without PR merge check for dependent stories.
  Dependent stories will merge dependency branches as described in mode 2.

### 1.4 Subagent Dispatch (Sequential Mode — When `--sequential` Is Set)

When `--sequential` flag is set, use sequential dispatch. For each executable story, launch a clean-context subagent using the `Agent` tool:

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): The orchestrator passes ONLY metadata to the subagent.
  Never pass source code, knowledge packs, or diffs. The subagent is born with
  clean context and dies after completion.
- **Worktree policy (ADR-0004 §D2, Rule 14):** In sequential mode the orchestrator MAY provision a single worktree per dispatched story via `Skill(skill: "x-git-worktree", args: "create --branch feat/{storyId}-{shortDesc} --base develop --id {storyId}")` and pass the worktree path to the subagent prompt (same pattern as Section 1.4a step 2.6). This is OPTIONAL for `--sequential` — the default behavior is to let the subagent's `x-story-implement` run on the main checkout of the repository. If a worktree IS provisioned, the orchestrator MUST remove it via `Skill(skill: "x-git-worktree", args: "remove --id {storyId}")` using the same criteria as the parallel path (Section 1.4d): when `mergeMode == "no-merge"` (default), remove on `status == SUCCESS` after auto-rebase of any remaining PRs in the phase completes; when `mergeMode != "no-merge"`, remove after the story's PR is merged (Rule 14 §5). The deprecated `Agent(isolation:"worktree")` harness parameter MUST NOT be used (ADR-0004 §D1).

**Prompt Template for Subagent:**
```
You are implementing story {storyId} for epic {epicId}.

CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
Do NOT expect source code, diffs, or knowledge pack content in this prompt.

Story file: plans/epic-{epicId}/story-{storyId}.md
Branch: {branchName}
Phase: {currentPhase}
Skip review: {skipReview}
Auto-approve PR: {autoApprovePr}

CRITICAL: Invoke the /x-story-implement skill using the Skill tool:
  Skill(skill: "x-story-implement", args: "{storyId} --non-interactive")

The /x-story-implement skill orchestrates ALL phases: planning, TDD, reviews, commits, and PR creation.
Do NOT manually perform these steps. Let the skill handle all orchestration.
Note: --non-interactive is always passed by x-epic-implement so x-story-implement's interactive
gates (Phase 0.5, Phase 2.2.9) auto-approve without pausing (Rule 20 — EPIC-0043).
When x-epic-implement itself is called with --non-interactive, also propagate to x-story-implement dispatches.

If /x-story-implement is unavailable (Skill tool error), fall back to manual execution:
1. Read story -> 2. Plan -> 3. TDD (Red-Green-Refactor) -> 4. Test + coverage
5. Commit (Conventional Commits) -> 6. Create PR targeting `develop`

PR MUST include "Part of EPIC-{epicId}" in body (RULE-008).

When --auto-approve-pr is set:
- Create a parent branch `feat/{storyId}-desc` from develop
- Task PRs target the parent branch (not develop)
- Task PRs are auto-merged into the parent branch after CI passes
- The parent branch itself is NOT auto-merged to develop (requires human review)

Version bump: DEFERRED. Do NOT modify pom.xml version in Phase 6.
The epic orchestrator handles version bumps at the integrity gate.

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
  "prNumber": <PR number if SUCCESS>,
  "parentBranch": "<parent branch name when --auto-approve-pr>"
}
```

### 1.4a Parallel Worktree Dispatch (Default Behavior)

Default behavior. When `--sequential` is NOT set, all executable stories in the
current phase are launched concurrently using **explicit git worktrees** provisioned
via the `x-git-worktree` skill BEFORE each subagent dispatch. This implements the
Worktree-First Branch Creation Policy (ADR-0004 §D2, Rule 14).

**Activation:** Default behavior. Only when `--sequential` flag is set, the sequential
dispatch in Section 1.4 is used instead.

> **Legacy flag:** If `--parallel` is passed, it is silently ignored (no error). The
> parallel behavior is already the default. This graceful handling ensures backward
> compatibility for at least 1 version cycle.

> **Anti-pattern (DO NOT USE):** `Agent(isolation:"worktree")` is DEPRECATED per
> [ADR-0004](../../../adr/ADR-0004-worktree-first-branch-creation-policy.md) §D1 and
> [Rule 14](../../rules/14-worktree-lifecycle.md) §7. The harness-native isolation
> parameter has been replaced by explicit `x-git-worktree create` / `x-git-worktree remove`
> calls so the worktree lifecycle is visible in logs, resilient to subagent failure, and
> recoverable on partial execution. This skill MUST NOT use `isolation:"worktree"` anywhere.

**Dispatch Algorithm:**

1. Call `getExecutableStories(parsedMap, executionState)` to get all executable stories for the current phase
2. For each executable story, mark `IN_PROGRESS` via `updateStoryStatus(epicDir, storyId, { status: "IN_PROGRESS" })`
2.5 **Story-level tracking (Story 0033-0002, Level 2 visibility):** For each executable story, create a tracking task via TaskCreate. Store the returned IDs indexed by `storyId` in an in-memory map `storyTrackingTasks`:

       TaskCreate(description: "Story {storyId}: {storyTitle}")

    These tasks surface real-time story progress in the Claude Code task list. They are closed in step 4.5 via TaskUpdate after each subagent returns. execution-state.json remains the authoritative record of SUCCESS/FAILED per CR-04 of EPIC-0033.
2.6 **Provision per-story worktrees (Rule 14 §2 — Atomicity Invariant).** For each executable story, invoke the `x-git-worktree` skill via the Skill tool (Rule 13 Pattern 1 — INLINE-SKILL) to create a worktree + branch atomically BEFORE dispatching the subagent:

       Skill(skill: "x-git-worktree", args: "create --branch feat/{storyId}-{shortDesc} --base develop --id {storyId}")

    Capture the returned absolute worktree path from stdout (the `create` operation prints the path on success; the exit status indicates success/failure) and store it in an in-memory map `storyWorktreePaths` indexed by `storyId`. On any provisioning failure (non-zero exit or missing path), do NOT dispatch the subagent for that story — mark the story FAILED with reason `"Worktree provisioning failed"` and skip to the next story. The orchestrator is the creator of each worktree and OWNS its removal (Rule 14 §5).

3. Launch ALL stories in a SINGLE message using the `Agent` tool WITHOUT `isolation`. Each subagent executes inside the worktree provisioned in step 2.6 (the path is passed in the prompt so `x-story-implement` Phase 0 Step 6a detects `inWorktree == true` and selects Mode 1 — REUSE):

```
For each story in executableStories:
  Agent(
    subagent_type: "general-purpose",
    description: "Implement story {storyId}",
    prompt: "<same prompt template as Section 1.4, with story-specific metadata PLUS the worktree path from storyWorktreePaths[storyId]>"
  )
```

The subagent's prompt MUST include the following additional lines so the nested `x-story-implement` invocation resolves to REUSE (Mode 1) instead of creating a second worktree:

```
Worktree path: {storyWorktreePaths[storyId]}

CRITICAL: Before invoking /x-story-implement, change your working directory to the worktree path above (e.g., `cd {storyWorktreePaths[storyId]}`). All subsequent git operations and skill invocations MUST run from inside that worktree. x-story-implement Phase 0 Step 6a will detect the worktree context and REUSE it (Rule 14 §3 — Non-Nesting). Do NOT invoke `x-git-worktree create` again for this story.
```

Each subagent uses the same prompt template as Section 1.4, including the PR creation instructions: PR targets `develop`, PR body includes "Part of EPIC-{epicId}" (RULE-008), and `SubagentResult` includes `prUrl` and `prNumber`.

**Branch Naming:** Each worktree operates on branch `feat/{storyId}-short-description` (standard story branch pattern, matching `x-story-implement` Phase 0). Branch + worktree are created atomically by `x-git-worktree create` (Rule 14 §2).

**Context Isolation (RULE-001):** Each subagent receives clean context, identical to sequential mode. The orchestrator passes ONLY metadata (story ID, branch, phase, flags, worktree path). Filesystem isolation is provided by the explicit git worktree — not by the deprecated `isolation:"worktree"` harness parameter.

4. Wait for ALL subagents to complete
4.5 **Close story-level tracking tasks (Story 0033-0002):** For each returned `SubagentResult`, update the corresponding tracking task created in step 2.5 via TaskUpdate:

       TaskUpdate(id: storyTrackingTasks[storyId], status: "completed")

    If the SubagentResult indicates FAILED, first call an additional TaskUpdate to prefix the description with "(FAILED) " so the failure surfaces in the Claude Code task list. The `status` field is always "completed" for visibility purposes — the authoritative SUCCESS/FAILED remains in execution-state.json (CR-04).
5. Validate each `SubagentResult` using Section 1.5 rules
6. Each story's PR targets `develop` directly — no merge into an epic branch is needed

### 1.4c Conflict Resolution Subagent (RULE-012)

When auto-rebase (Section 1.4e) detects conflicts during `git rebase origin/develop`,
a conflict resolution subagent is dispatched to resolve them automatically.

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): pass only branch names, conflict file list, and
  metadata — never source code inline

**Prompt Template:**
```
You are a Conflict Resolution Specialist resolving rebase conflicts.

CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
Do NOT expect source code, diffs, or knowledge pack content in this prompt.

Conflict type: rebase
Story branch: {storyBranch}
Conflict files: {conflictFiles}
Merged stories this phase: {mergedStories}
Merged PRs: {mergedPRs}
Main SHA before phase: {mainShaBeforePhase}

Steps:
1. For each conflict file, analyze the diff from both branches
2. The changes from merged stories are intentional — preserve them
3. Resolve each conflicting hunk respecting the intent of both branches
4. Run: git add <resolved files> (do NOT commit — rebase handles the commit)
5. Return JSON: { "status": "SUCCESS" | "FAILED", "summary": "..." }
```

**Post-resolution flow:**
- If SUCCESS: `git rebase --continue && git push --force-with-lease origin {storyBranch}`
- If FAILED and `rebaseAttempts < MAX_REBASE_RETRIES` (3):
  `git rebase --abort`, log WARNING, retry on next merge event
- If FAILED and `rebaseAttempts >= MAX_REBASE_RETRIES`:
  `git rebase --abort`, mark story FAILED, close PR:
  `gh pr close {prNumber} --comment "Rebase conflict resolution failed after {MAX_REBASE_RETRIES} attempts"`
  Trigger block propagation for dependent stories.

**Constants:**

| Constant | Type | Default | Description |
|----------|------|---------|-------------|
| `MAX_REBASE_RETRIES` | Integer | 3 | Maximum conflict resolution attempts per story |

### 1.4d Worktree Cleanup (Creator Owns Removal — Rule 14 §5)

The orchestrator is the creator of every worktree provisioned in Section 1.4a step 2.6 and is therefore responsible for removing each one. Cleanup is explicit — invoked via the `x-git-worktree` skill — and occurs at well-defined lifecycle points:

- **SUCCESS + PR merged (or `--no-merge` + SUCCESS + auto-rebase of remaining PRs in the phase completed):** Invoke `x-git-worktree` via the Skill tool (Rule 13 Pattern 1 — INLINE-SKILL) to remove the worktree:

      Skill(skill: "x-git-worktree", args: "remove --id {storyId}")

  Drop the entry for `{storyId}` from `storyWorktreePaths` after a successful removal. This call MUST happen AFTER any auto-rebase triggered by the merge (Section 1.4e) has finished for every other remaining PR in the phase, so the worktree remains available as a fallback working copy if a rebase needs to reinspect the story's history.

- **FAILED stories (Rule 14 §4 — Failure Preservation):** Do NOT remove the worktree. Preserve it for diagnostic investigation. Log the preserved worktree `id` (i.e., `{storyId}`) at WARN level — NEVER the absolute path to shared logs, since `x-git-worktree detect-context` explicitly warns that absolute paths can leak usernames / home directory prefixes (CWE-209). Operators can run `Skill(skill: "x-git-worktree", args: "list")` after triage to recover the local path and then run `Skill(skill: "x-git-worktree", args: "remove --id {storyId}")` to clean up (the `remove` operation internally passes `--force` to `git worktree remove`; no user-facing `--force` flag is documented). The orchestrator's in-memory `storyWorktreePaths` entry is retained until the end of Phase 1 and then surfaced in the Phase 2 progress report — that report is generated locally and MAY include the full path for diagnostics, but shared-log emission MUST redact home-directory prefixes.

- **Worktree provisioning failure (Section 1.4a step 2.6):** No worktree exists — nothing to remove. The story is marked FAILED and cleanup is a no-op.

- **Epic-level abort or user interrupt:** All still-registered entries in `storyWorktreePaths` are preserved (treat as FAILED for cleanup purposes). The operator is responsible for running `x-git-worktree cleanup` after triage.

> **Anti-pattern (DO NOT USE):** Do NOT rely on the deprecated `Agent(isolation:"worktree")` harness mechanism to clean up worktrees automatically (ADR-0004 §D1). The orchestrator MUST invoke `x-git-worktree remove` explicitly as described above.

### 1.4e Auto-Rebase After PR Merge (RULE-011)

After each PR merge within a phase, the orchestrator automatically rebases
remaining open PRs in the same phase onto the updated `develop`.

**Trigger:**
- When `mergeMode != "no-merge"`: a story's `prMergeStatus` transitions to `"MERGED"`
- When `mergeMode == "no-merge"`: a story's `status` transitions to `SUCCESS` (since PRs are not merged, rebase triggers on completion to keep branches current against `origin/develop`)

**Skip conditions:**
- `--sequential` is set (stories execute one at a time, no parallel PRs) —
  log: `"Auto-rebase skipped (--sequential mode)"`
- No remaining open PRs in the phase
- All PRs in the phase are already merged

**Algorithm:**

1. Detect remaining open PRs in the phase: stories where `prMergeStatus != "MERGED"`
2. Order by critical path priority: `sortByCriticalPath()` (RULE-007)
3. For each remaining story:
   a. `git fetch origin develop && git checkout {story-branch}`
   b. `git rebase origin/develop`
   c. If rebase succeeds (no conflicts):
      - `git push --force-with-lease origin {story-branch}`
      - Update checkpoint: `rebaseStatus = "REBASE_SUCCESS"`, `lastRebaseSha = {SHA}`
   d. If rebase has conflicts:
      - Dispatch Conflict Resolution Subagent (Section 1.4c)
      - On resolution success: `git rebase --continue && git push --force-with-lease`
      - On resolution failure: increment `rebaseAttempts`, handle per Section 1.4c rules
   e. Return to `develop`: `git checkout develop`

**Push strategy:** Always `--force-with-lease` (NEVER `--force`) to protect against
concurrent pushes. If push fails (branch updated by another process), re-fetch and
retry the rebase.

**Checkpoint fields per story (rebase tracking):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `rebaseStatus` | String (Enum) | Optional | `PENDING`, `REBASING`, `REBASE_SUCCESS`, `REBASE_FAILED` |
| `lastRebaseSha` | String | Optional | SHA-1 hex (40 chars) of develop used for last rebase |
| `rebaseAttempts` | Integer | Optional | Number of rebase attempts (0 to MAX_REBASE_RETRIES) |

> **Note:** `rebaseStatus` is a sub-field within each story entry, NOT a primary
> story status. The primary story status remains: PENDING, IN_PROGRESS, SUCCESS,
> FAILED, BLOCKED, PARTIAL, PR_CREATED, PR_PENDING_REVIEW, PR_MERGED.

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
   - `prUrl`: URL of the PR created by the lifecycle (if status is `SUCCESS`)
   - `prNumber`: PR number (if status is `SUCCESS`)
   - `prMergeStatus`: Initial value `"OPEN"` (updated to `"MERGED"` when PR is merged)
2. Update metrics: increment `storiesCompleted` counter
3. The checkpoint is persisted atomically to `execution-state.json` via the checkpoint engine
4. Between story completions, the checkpoint always reflects the current execution state

### 1.7 Phase 1.7 — Markdown Status Sync (was Section 1.6b, promoted to Phase 1.7 by story-0046-0004)

Phase 1.7 is cabled explicitly into the Core Loop step 6e (Section 1.3). It runs
after every story checkpoint update and is the single source of truth for
propagating a story's end-of-life status from `execution-state.json` to the
markdown artifacts (`story-XXXX-YYYY.md` + IMPLEMENTATION-MAP row). V2-gated:
when `planningSchemaVersion == "2.0"`, Phase 1.7 is unskippable on the happy
path (Rule 22 RULE-046-04). Each helper call is fail-loud — any
`StatusSyncException` exits the orchestrator with code `STATUS_SYNC_FAILED` and
stderr carrying the offending path (Rule 22 RULE-046-08). V1 epics retain the
legacy in-place rewrite below (Rule 19 — backward compatibility).

Implementation uses the helpers shipped by story-0046-0001 and story-0046-0004:

- `StatusFieldParser.readStatus(path)` / `.writeStatus(path, status)` — atomic
  Status header read/write for story and epic markdown files.
- `LifecycleTransitionMatrix.validateOrThrow(current, target)` — rejects illegal
  transitions (e.g., PENDENTE → CONCLUIDA without passing EM_ANDAMENTO).
- `EpicMapRowUpdater.updateRow(mapFile, storyId, newStatus)` — atomic rewrite of
  the Status cell of a single row in `IMPLEMENTATION-MAP.md`.

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

**Epic-level completion check (legacy inline shortcut — v1 only):**

Retained for backward compatibility with schema v1 epics (Rule 19). When
`planningSchemaVersion == "2.0"`, epic finalization is handled by **Phase 5 —
Epic Finalization** (Section 5 below), which is the normative path.

After updating story status to SUCCESS, check if ALL stories in the epic have status SUCCESS
in the checkpoint. If yes (v1 only):
1. Read `plans/epic-{epicId}/EPIC-{epicId}.md`
2. Update the `**Status:**` field from `Em Andamento` to `Concluído`
3. If the epic has a Jira key (not `—` or `<CHAVE-JIRA>`):
   - Call `mcp__atlassian__getTransitionsForJiraIssue` with the epic's Jira key
   - Find the transition to "Done"
   - Call `mcp__atlassian__transitionJiraIssue`
   - If transition fails: log warning, continue (non-blocking)

### 5. Phase 5 — Epic Finalization (V2-gated, added by story-0046-0004)

Phase 5 is the end-of-epic status-finalize block. It executes **once**, after
the Core Loop finishes the last phase with all stories reaching SUCCESS, and
before the orchestrator returns to the caller. V2-gated — only active when
`planningSchemaVersion == "2.0"`. For v1 epics, the legacy inline shortcut
above is used instead (Rule 19 fallback).

**Preconditions.** Every story in `execution-state.json` MUST be in `SUCCESS`
state. If any story is still `PENDING`, `IN_PROGRESS`, `FAILED`, `PARTIAL`, or
`BLOCKED`, Phase 5 is a no-op and the orchestrator returns with the existing
mixed state (Rule 22 — source-of-truth invariant — prevents false epic
closure).

**Sub-steps (all fail-loud — any failure exits STATUS_SYNC_FAILED per
Rule 22 RULE-046-08).**

1. **Read epic Status.** Call `StatusFieldParser.readStatus(epicFilePath)`
   where `epicFilePath = plans/epic-{epicId}/epic-{epicId}.md`. The expected
   current value is `EM_ANDAMENTO` or `EM_REFINAMENTO` (legacy label —
   tolerated by Rule 22 matrix, treated as EM_ANDAMENTO for transition
   purposes). If the file is missing: exit `STATUS_SYNC_FAILED` with the path
   in stderr (fail-loud scenario covered by story-0046-0004 AC).

2. **Validate transition.** `LifecycleTransitionMatrix.validateOrThrow(current,
   CONCLUIDA)`. Rejection → exit `STATUS_SYNC_FAILED`.

3. **Write epic Status = Concluído.** `StatusFieldParser.writeStatus(
   epicFilePath, LifecycleStatus.CONCLUIDA)`. Atomic write via
   temp-file-plus-rename. IOException → `StatusSyncException` → exit
   `STATUS_SYNC_FAILED`.

4. **Atomic commit.** Stage exactly the epic file and invoke x-git-commit via
   the Skill tool (Rule 13 Pattern 1 — INLINE-SKILL):

       Skill(skill: "x-git-commit", args: "--type chore --scope epic-{epicId} --subject \"finalize status to Concluído\" --body \"Epic Status: Em Andamento → Concluído. Implementation map columns updated for all stories.\"")

   The pre-commit chain (format → lint → compile) MUST succeed.

5. **Idempotency.** If Step 1 reads `CONCLUIDA`, log
   `"Phase 5: epic {epicId} already Concluído — no-op"` and return. No commit
   is created (Rule 22 RULE-046-06 — clean workdir invariant after re-run).

6. **Clean-workdir verification.** After the commit (or no-op return), assert
   `git status --porcelain` is empty. A dirty workdir exits
   `STATUS_SYNC_FAILED` — this catches orphan `*.tmp` files from a partially
   failed atomic move (Rule 22 RULE-046-06).

7. **Jira epic transition (optional, non-blocking).** If the epic has a Jira
   key, call `mcp__atlassian__transitionJiraIssue` to move the epic to Done.
   Failures log a warning and continue — Jira is an external system outside
   the Rule 22 fail-loud contract.

**Gherkin contract.** Phase 5 satisfies the following acceptance criteria from
story-0046-0004 §7:

- "Épico v2 happy path — story e epic concluídos" — 3 commits exist: 2
  story-finalize (Phase 3.8.5) + 1 epic-finalize (this phase).
- "Falha de status update no epic-finalize (fail loud)" — deleting the epic
  file before Phase 5 triggers exit `STATUS_SYNC_FAILED` with the path in
  stderr.
- "Clean workdir após x-epic-implement" — Step 6 assertion.
- "Idempotência — re-rodar x-epic-implement em épico já concluído" — Step 5
  short-circuit.

### 1.7 Extension Points

The following sections are placeholders for downstream stories:

- [Placeholder: integrity gate between phases — story-0005-0006]
- [Placeholder: retry + block propagation — story-0005-0007]
- [Placeholder: resume from checkpoint — story-0005-0008]
- Circuit breaker (Section 1.7) pauses on 3 consecutive failures and aborts the phase on 5 total failures
- [Placeholder: progress reporting — story-0005-0013]

### Integrity Gate (Between Phases) (RULE-006)

After ALL stories in a phase complete AND all their PRs are merged to `develop`,
dispatch an integrity gate subagent before advancing to the next phase.

The gate runs on `develop` to validate the integrated code from all merged PRs.

#### Pre-Phase SHA Capture

At the **start** of each phase, before dispatching any stories:

1. Capture: `mainShaBeforePhase[N] = git rev-parse develop`
2. Persist to checkpoint: `updateCheckpoint(epicDir, { mainShaBeforePhase: { [N]: sha } })`
3. On `--resume`: recover `mainShaBeforePhase[N]` from checkpoint (do NOT recalculate,
   since stories from the phase may already be merged)

#### Gate Preconditions

The gate behavior depends on `mergeMode`:

**When `mergeMode != "no-merge"` (auto or interactive with PRs merged):**

Before running the gate, verify all PRs from the phase are merged:

```
for each story in currentPhase:
  assert story.prMergeStatus == "MERGED"
```

Then checkout `develop` with latest merges:
```
git checkout develop && git pull origin develop
```

**When `mergeMode == "no-merge"`:**

PRs are not merged to `develop`. The integrity gate is **DEFERRED**:
1. Per-story validation already runs within `x-story-implement` (compile, test, coverage per story)
2. Cross-story integration on `develop` cannot be validated (code not merged yet)
3. Log: `"--no-merge: integrity gate deferred for phase {N}. Cross-story integration will be validated after PRs are merged."`
4. Record: `integrityGate.status = "DEFERRED"` in checkpoint
5. Auto-rebase (Section 1.4e) still executes to keep branches current against `origin/develop`
6. Skip directly to phase completion report generation (no gate subagent dispatched)

#### Gate Subagent Prompt

Launch a `general-purpose` subagent:

> CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
> Do NOT expect source code, diffs, or knowledge pack content in this prompt.
>
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
> **Step 5 — Smoke Gate (MANDATORY — EPIC-0042):** Execute the full smoke test suite as a regression validation.
> - Smoke gate is ALWAYS mandatory when `{{SMOKE_COMMAND}}` is configured. There is no skip flag.
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

   **Agent-Assisted Regression Fix (EPIC-0042):**
   Before reverting, attempt to fix the regression via agent:
   1. Dispatch a general-purpose agent with the failing tests and the diff of the suspected commit:
      ```
      Agent(
        subagent_type: "general-purpose",
        description: "Fix regression in integrity gate",
        prompt: "Failing tests: [list]. Suspected commit: [sha]. Read the diff of the commit and the failing test output. Fix the implementation to make tests pass without breaking the commit's intended behavior. Run {{TEST_COMMAND}} to verify. Commit via Skill(skill: 'x-git-commit', args: '--type fix --subject \"fix regression from [sha]\"')."
      )
      ```
   2. If agent fixes successfully (tests pass): commit fix, re-run gate
   3. If agent fails: fallback to `git revert` (original behavior)
   4. Opt-out: `--revert-on-failure` flag skips agent fix attempt and reverts directly

4. If regression fix agent fails (or `--revert-on-failure`): orchestrator executes `git revert <commitSha>` for that story
5. Story is marked FAILED with summary: `"Regression detected by integrity gate"`
6. Block propagation is executed for dependents of the failed story

#### Smoke Gate Regression Diagnosis

If smoke tests fail (Step 5), the subagent:
1. Identifies which smoke tests failed (`smokeGate.failedTests` array)
2. Correlates failures with stories in the current phase by analyzing files touched by each story's commits
3. Populates `smokeGate.suspectedStories` with the story IDs most likely responsible
4. Logs: `"INTEGRITY GATE SMOKE FAILURE: Phase {N}. {count} test(s) failed. Suspected stories: [{list}]"`
5. The phase is marked as FAILED in the checkpoint
6. The operator decides: `--resume` to retry after fixing the failing smoke tests

#### Gate Result Registration

```
updateIntegrityGate(epicDir, phaseNumber, {
  status: "PASS" | "FAIL" | "DEFERRED",
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

- **PASS**: Advance to version bump (see below), then to next phase (requires both test gate and smoke gate to pass)
- **FAIL + regression identified**: revert + mark FAILED + block propagation
- **FAIL + regression unidentified**: pause execution, report to user
- **FAIL (smoke gate)**: phase marked FAILED; operator uses `--resume` after fixing the failing smoke tests (EPIC-0042: no bypass available)
- **DEFERRED** (when `mergeMode == "no-merge"`): skip gate, advance directly to phase completion report

#### Version Bump (Post-Gate) (RULE-013)

After the integrity gate **PASSES** for phase N, the orchestrator performs an automatic
semantic version bump on `develop`. This is skipped when `integrityGate.status == "DEFERRED"`.

1. Determine commit range: `mainShaBeforePhase[N]..develop`
2. Invoke `x-lib-version-bump` logic with the commit range:
   a. Analyze commits in range for highest-priority bump type (MAJOR > MINOR > PATCH > NONE)
   b. If bump type is **NONE**: skip. Log: `"No version-impacting changes in phase {N}. Version unchanged."`
   c. If bump type is MAJOR/MINOR/PATCH:
      - Read current version from pom.xml (strip -SNAPSHOT suffix for base calculation)
      - Calculate next version, append `-SNAPSHOT`
      - Update pom.xml on `develop`
      - Commit: `chore(version): bump to X.Y.Z-SNAPSHOT [phase-{N}]`
      - Push: `git push origin develop`
3. Record version bump in checkpoint:
   ```json
   "versionBump": {
     "phase": N,
     "previousVersion": "X.Y.Z-SNAPSHOT",
     "newVersion": "X.Y.Z-SNAPSHOT",
     "bumpType": "MAJOR|MINOR|PATCH|NONE",
     "commitSha": "abc123..."
   }
   ```
4. Include version bump details in the phase completion report (see Report Content below)

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

#### Gate Enforcement (RULE-006)

The integrity gate is **mandatory** — there is no bypass. Every phase transition requires a PASS gate
result. The gate runs after phase 0, 1, 2, and 3 — one gate per phase.

The smoke gate within the integrity gate is also mandatory by default. It can only be bypassed with
**EPIC-0042 change:** The `--skip-smoke-gate` flag has been removed. Smoke gate is now mandatory
when `{{SMOKE_COMMAND}}` is configured. The integrity gate always evaluates all 5 steps (compile, test, coverage, smoke).
When not set, the smoke gate (Step 5) must also pass for the overall integrity gate to pass.

> **Note:** Each story already executes its own smoke gate via `x-story-implement` (Phase 2.5).
> The integrity gate smoke tests serve as an ADDITIONAL regression validation — they ensure
> that the combination of all stories in a phase did not break the overall smoke test suite.

#### Cross-Story Consistency Gate (RULE-006)

After the integrity gate passes (compile + test + coverage + smoke), run a
cross-story consistency check on the `develop` diff for the phase:

1. Compute diff: `git diff {mainShaBeforePhase[N]}..develop`
2. Dispatch a consistency subagent with metadata only (CONTEXT ISOLATION: pass phase number, SHA range, and file list — the subagent runs `git diff` itself to read the actual diff)
3. The subagent verifies:
   - Error handling patterns are uniform across classes of the same role
   - Constructor patterns and return types are consistent within modules
   - No cross-story inconsistencies introduced by parallel development
4. Result: `{ status: "PASS"|"FAIL", findings: [...] }`
5. If FAIL: log findings, mark phase as requiring attention (advisory, not blocking)

> **`mainShaBeforePhase` field in `execution-state.json`:**
> Type: `Map<Integer, String>` — SHA-1 hex (40 chars) per phase number.
> Example: `{ "0": "abc123...", "1": "def456..." }`

### Phase Completion Reports

After all stories in a phase complete (or reach terminal state) and the integrity
gate finishes, the orchestrator delegates **phase completion report generation to a dedicated subagent**. This prevents the full report content from accumulating in the orchestrator context.

#### Report Generation (Subagent Delegation)

The orchestrator dispatches a subagent with metadata (epicId, phase number, story statuses, template path) to generate the report. The subagent:

1. Reads template at `.claude/templates/_TEMPLATE-PHASE-COMPLETION-REPORT.md` for required output format (RULE-007)
2. If template is found: generates the report following the template structure, filling all `{{PLACEHOLDER}}` tokens with real data from the checkpoint (story statuses, durations, findings, coverage, TDD metrics)
3. If template is NOT found (RULE-012 — graceful fallback): logs `"WARNING: Template _TEMPLATE-PHASE-COMPLETION-REPORT.md not found, using inline format"` and generates the report with the following inline format:

```markdown
# Phase Completion Report — EPIC-{epicId} Phase {N}

> **Epic ID:** EPIC-{epicId}
> **Phase:** {N}
> **Date:** {currentDate}

## Stories Completed

| Story ID | Status | Duration |
|----------|--------|----------|
| story-{epicId}-YYYY | SUCCESS | 3m 45s |
| ... | ... | ... |

## Summary
- Stories attempted: {count}
- Stories succeeded: {count}
- Stories failed: {count}
- Stories blocked: {count}
- Phase duration: {duration}
```

4. Writes the report to `plans/epic-{epicId}/reports/phase-{N}-completion-{epicId}.md`
5. The report header MUST include: Epic ID, Phase Number, Date, Author (role), Template Version (RULE-011)
6. Returns to orchestrator: `{ "status": "GENERATED", "path": "plans/epic-{epicId}/reports/phase-{N}-completion-{epicId}.md" }`

The orchestrator logs only the returned path and status. It does NOT read the generated report into its own context.

#### Report Content

The phase completion report contains:

- **Stories executed**: status (SUCCESS/FAILED/BLOCKED/SKIPPED), duration, commit SHA per story
- **Integrity gate results**: compilation, test, coverage, smoke gate results
- **Findings summary**: severity counts and examples from per-story reviews
- **TDD compliance**: TDD cycles, test-first commits, TPP progression per story
- **Coverage delta**: line and branch coverage before/after the phase
- **Blockers encountered**: descriptions, resolutions, impact assessments
- **Next phase readiness**: checklist and recommendation for proceeding

#### Timing

The phase completion report is generated AFTER the integrity gate completes
(whether PASS or FAIL). This ensures the gate results are included in the report.
If the gate fails, the report documents the failure and serves as a diagnostic
artifact for the operator deciding whether to resume or abort.

#### Atomic Commit — Phase Report (V2-gated, story-0046-0005)

After the phase-report subagent returns with the generated path, the
orchestrator MUST commit the report atomically before advancing to the next
wave (RULE-046-05 — reports atomically committed, RULE-046-06 — clean
workdir invariant). This eliminates the window where reports are orphaned on
the working tree and prevents false positives in
`x-release VALIDATE_DIRTY_WORKDIR`.

Fallback matrix (Rule 19 — Backward Compatibility):

| `planningSchemaVersion` | Action |
| :--- | :--- |
| absent / `"1.0"` / invalid | **V1 no-op.** Skip the commit block entirely. Phase reports remain optional and uncommitted. |
| `"2.0"` | **V2 active.** Execute the 3-step block below once per wave, immediately after Phase 1.7 (per-wave status sync from story-0046-0004). |

Three-step block (V2 only, one per wave):

1. **Stage** the phase-report path returned by the subagent:
   `git add plans/epic-{epicId}/reports/phase-{N}-completion-{epicId}.md`
   (idempotent; no-op when no diff).
2. **Build message** via `ReportCommitMessageBuilder.phaseReport(epicId,
   waveNumber, storyCount, commitCount)` — produces subject
   `docs(epic-{epicId}): add phase-{N} report` plus body summarising the
   wave outcome.
3. **Commit** via Rule 13 Pattern 1 INLINE-SKILL:

       Skill(skill: "x-git-commit", args: "--message \"<built message>\"")

   On non-zero exit, abort with exit code `REPORT_COMMIT_FAILED` (21) and
   stream the hook's stderr (RULE-046-08 — fail loud).
   Do NOT fall back to `--no-verify`.

Canonical v2 ordering (see story-0046-0005 §3.4):

```
Wave N executes
 → Phase 1.7 (per-story status sync — story 0046-0004) → commit docs(story-*)
 → Phase 1.8 (NEW: phase-report write + commit — this story) → commit docs(epic-*)
 → advance to Wave N+1
...
Phase 5 (epic finalize — story 0046-0004) → commit chore(epic-*)
```

Idempotence: if staging produces no diff, skip the commit with log
`"Reusing committed phase-{N} report for EPIC-{epicId}"` and proceed.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-1-Execution-Loop ok`

## Phase 2 — Epic Progress Report Generation

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-2-Progress-Report`

After all stories in a phase complete (or reach terminal state), the orchestrator
generates a progress report. With per-story PRs, each story already has its own
tech lead review (via `x-story-implement` Phase 7) and its own PR (via Phase 6).
Phase 2 consolidates this information into a single report.

> **Note:** The legacy two-wave consolidation (tech lead review of full diff +
> mega-PR creation) is only used when `--single-pr` is set. See the `--single-pr`
> guard in Phase 0 Step 7.

**Skip condition:** If NO stories have status SUCCESS, skip report generation entirely.
Log: `"No successful stories — skipping report generation"` and proceed to Phase 3.

### 2.1 Generate Progress Report

After all stories reach terminal state (SUCCESS, FAILED, or BLOCKED):

1. Read checkpoint to collect all story results
2. Build PR links table from `prUrl`, `prNumber`, `prMergeStatus` per story
3. Generate `epic-execution-report.md` using the template
4. Replace `{{PR_LINKS_TABLE}}` with the per-story PR table:

```markdown
| Story | PR | Status | Tech Lead Score | Merged At |
|-------|-----|--------|-----------------|-----------|
| story-{epicId}-0001 | [#41](https://github.com/org/repo/pull/41) | MERGED | 42/45 | 2026-04-01T10:30:00Z |
| story-{epicId}-0002 | [#42](https://github.com/org/repo/pull/42) | OPEN | 38/45 | — |
| story-{epicId}-0003 | — | FAILED | — | — |
```

5. Replace other `{{PLACEHOLDER}}` tokens with real data:
   - `{{EPIC_ID}}`, `{{STARTED_AT}}`, `{{FINISHED_AT}}`
   - `{{STORIES_COMPLETED}}`, `{{STORIES_FAILED}}`, `{{STORIES_BLOCKED}}`, `{{STORIES_TOTAL}}`
   - `{{COMPLETION_PERCENTAGE}}`: completed/total x 100
   - `{{PHASE_TIMELINE_TABLE}}`: phase start/end times from checkpoint
   - `{{STORY_STATUS_TABLE}}`: per-story status with commit SHAs
   - `{{COVERAGE_BEFORE}}`, `{{COVERAGE_AFTER}}`, `{{COVERAGE_DELTA}}`
   - `{{TDD_COMPLIANCE_TABLE}}`: TDD compliance per-story table
   - `{{TDD_SUMMARY}}`: TDD compliance aggregated summary
   - `{{UNRESOLVED_ISSUES}}`: findings with severity >= Medium (from per-story reviews)
6. Include summary metrics: completed/failed/blocked counts, overall completion %
7. Validate: no unresolved `{{...}}` placeholders remain in output
8. Write `epic-execution-report.md` to `plans/epic-{epicId}/`

**Result Handling:**
- On SUCCESS: report written to `plans/epic-{epicId}/epic-execution-report.md`. Update checkpoint atomically (RULE-002)
- On FAILURE: log `"ERROR: Report generation failed"`, continue to Phase 3

### 2.2 Incremental Report Updates (RULE-010)

The report is updated incrementally as each story completes, not only at the end:

1. After each story reaches a terminal state (SUCCESS, FAILED, BLOCKED):
   - Append or update the corresponding row in `{{PR_LINKS_TABLE}}`
   - Update summary metrics (completed/failed/blocked counts)
2. At the end of all phases: generate the final version with all metrics resolved
3. The incremental report allows real-time progress monitoring during epic execution

### 2.3 Epic Review Summary (EPIC-0042)

After all story reviews are collected and before checkpoint finalization, emit an aggregated review summary to the terminal:

```
============================================================
 EPIC REVIEW SUMMARY — EPIC-XXXX
============================================================

 | Story          | Specialist | Tech Lead | Tests  | Smoke  | Status |
 |----------------|------------|-----------|--------|--------|--------|
 | STORY-XXXX-001 | XX/YYY     | XX/ZZZ    | PASS   | PASS   | GO     |
 | STORY-XXXX-002 | XX/YYY     | XX/ZZZ    | PASS   | PASS   | GO     |
 | ...            | ...        | ...       | ...    | ...    | ...    |

 Overall: N/M GO | K NO-GO
============================================================
```

Replace placeholders with actual values from each story's review dashboard and tech lead report. For stories that skipped reviews (via `--skip-review`), show `SKIPPED` in the Specialist and Tech Lead columns. For stories with status FAILED or BLOCKED, show `--` for review columns and the actual status in the Status column.

This summary MUST also be included in the epic execution report saved at `plans/epic-{epicId}/epic-execution-report.md` as an "Epic Review Summary" section.

### 2.4 Checkpoint Finalization

After report generation completes, persist final state:

1. Register report path: `updateCheckpoint(epicDir, { reportPath })`
2. Set `finishedAt` timestamp
3. Persist final `execution-state.json` with all metrics

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-2-Progress-Report ok`

## Phase 3 — Verification

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-3-Verification`

Final verification validates the epic as a whole before declaring completion.
All validations run on `develop` after all story PRs are merged.

### 3.1 Epic-Level Test Suite

Run the full test suite on `develop` to validate cross-story integration:

1. Ensure on latest develop: `git checkout develop && git pull origin develop`
2. Execute: `{{TEST_COMMAND}}` (all unit, integration, and API tests)
3. Coverage thresholds (non-negotiable): >=95% line, >=90% branch
4. If any test fails: log failures, mark epic as requiring attention
5. Record coverage results in checkpoint for the report

### 3.2 DoD Checklist Validation

Verify the Definition of Done (DoD) for the epic:

- [ ] All story PRs merged to develop or documented as FAILED/BLOCKED (when `mergeMode == "no-merge"`: all story PRs created and targeting develop)
- [ ] Integrity gates passed for all phases (when `mergeMode == "no-merge"`: gates are DEFERRED — per-story validation still applies)
- [ ] Coverage thresholds met (>=95% line, >=90% branch per-story)
- [ ] Zero compiler/linter warnings (per-story, validated by lifecycle)
- [ ] Per-story tech lead reviews executed (via `x-story-implement` Phase 7) or skipped via `--skip-review`
- [ ] Epic execution report generated with PR links table (Phase 2.1)
- [ ] All findings with severity >= Medium addressed or documented

### 3.3 Final Status Determination

Compute the final epic status based on story outcomes and PR merge status:

- **COMPLETE**: All stories reached SUCCESS status and all DoD items pass. When `mergeMode != "no-merge"`: all PRs merged to `develop`. When `mergeMode == "no-merge"`: all PRs created and targeting `develop`.
- **PARTIAL**: Some stories FAILED or BLOCKED, but critical path stories succeeded. When `mergeMode != "no-merge"`: critical path PRs merged.
- **FAILED**: One or more critical path stories failed

Persist final status to checkpoint: `updateCheckpoint(epicDir, { finalStatus })`

### 3.4 Completion Output

Display the final summary to the user:

```
Epic: EPIC-{epicId} — {title}
Status: COMPLETE | PARTIAL | FAILED
Model: per-story PR (each story has its own PR targeting develop)
Auto-approve: {enabled|disabled} (when --auto-approve-pr: task PRs auto-merged to parent branches)
Stories: {completed}/{total} completed, {failed} failed, {blocked} blocked
PRs: {merged}/{total} merged, {open} open, {closed} closed (when --no-merge: "{open}/{total} open (--no-merge: merge deferred)")
Coverage: line {lineCoverage}%, branch {branchCoverage}%

Story PRs:
| Story | PR | Status | Parent Branch | Merged At |
|-------|-----|--------|---------------|-----------|
| story-{epicId}-0001 | #41 | MERGED | feat/story-{epicId}-0001-desc | 2026-04-01T10:30:00Z |
| story-{epicId}-0002 | #42 | MERGED | feat/story-{epicId}-0002-desc | 2026-04-01T11:15:00Z |
...

Parent Branches Pending Human Review (when --auto-approve-pr):
| Story | Parent Branch | Tasks Merged | Status |
|-------|---------------|--------------|--------|
| story-{epicId}-0001 | feat/story-{epicId}-0001-desc | 3/3 | Pending human review |
| story-{epicId}-0002 | feat/story-{epicId}-0002-desc | 2/2 | Pending human review |

PR Comment Remediation: COMPLETE | PR #{fixPrNumber} | {fixesApplied} fixes applied
PR Comment Remediation: SKIPPED
PR Comment Remediation: DRY_RUN
Report: plans/epic-{epicId}/epic-execution-report.md
Elapsed: {totalElapsedTime}
```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-3-Verification ok`

## Phase 4 — PR Comment Remediation (Optional)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-4-Remediation`

After Phase 3 (Verification) completes, the orchestrator offers automatic remediation
of PR review comments across all story PRs in the epic. This phase invokes
`/x-pr-fix-epic` to discover, classify, and fix actionable review comments
in a single correction PR.

**Skip conditions:**
- `--skip-pr-comments` is set: skip entirely. Log: `"PR comment remediation skipped (--skip-pr-comments)"`. Record `prCommentRemediation.status = "SKIPPED"` in checkpoint.
- `--single-pr` is set: skip (single-PR flow has no per-story PRs to scan). Log: `"PR comment remediation skipped (--single-pr mode)"`. Record `prCommentRemediation.status = "SKIPPED"`.

### 4.1 Check for PR Comments

Scan all story PRs in the epic for unresolved review comments:

1. Read `execution-state.json` to collect all story PR numbers
2. For each story with `prNumber`, check for review comments via `gh api repos/{owner}/{repo}/pulls/{prNumber}/comments`
3. Count total actionable comments across all PRs
4. If zero actionable comments found:
   - Log: `"No PR comments to remediate"`
   - Record `prCommentRemediation.status = "SKIPPED"`, `fixesApplied = 0`
   - Skip to Completion Output

### 4.2 Dry-Run + Auto-Apply (EPIC-0042)

**Default behavior (auto-apply):** When comments are found, invoke `x-pr-fix-epic` in
dry-run mode first via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-pr-fix-epic", args: "{epicId} --dry-run")

This generates a consolidated findings report at `plans/epic-{epicId}/reports/pr-comments-report.md`.
Log: `"PR comment dry-run complete: {commentCount} actionable findings across {prCount} PRs (EPIC-0042)"`

Then automatically proceed to apply fixes (Step 4.4) without user confirmation. Log:
`"Auto-applying PR comment fixes (EPIC-0042)"`

### 4.3 User Confirmation (only with `--dry-run-only-comments`) (EPIC-0042)

**Opt-out flag `--dry-run-only-comments` (EPIC-0042):** When `--dry-run-only-comments` is
present, the dry-run report is generated but fixes are NOT auto-applied. Instead, present
the report to the user and ask for confirmation using `AskUserQuestion`:

```
question: "PR comment report generated. {commentCount} actionable findings across {prCount} PRs. Apply fixes?"
header: "PR Comment Remediation"
options:
  - label: "Apply fixes"
    description: "Invoke x-pr-fix-epic to apply fixes and create a correction PR"
  - label: "Skip"
    description: "Keep the report for review but do not apply fixes"
multiSelect: false
```

- **"Apply fixes"**: proceed to Step 4.4
- **"Skip"**: Record `prCommentRemediation.status = "DRY_RUN"` (report saved, no fixes applied). Log: `"PR comment remediation: dry-run report saved, fixes not applied"`

### 4.4 Apply Fixes

Invoke `x-pr-fix-epic` via the Skill tool (Rule 13 — INLINE-SKILL pattern), without `--dry-run`, to apply fixes:

    Skill(skill: "x-pr-fix-epic", args: "{epicId}")

The skill will:
1. Classify all review comments (actionable/suggestion/question/praise)
2. Implement fixes for actionable comments
3. Create a single correction PR with all fixes

After completion, update the checkpoint:

```json
{
  "prCommentRemediation": {
    "status": "COMPLETE",
    "fixPrUrl": "https://github.com/{owner}/{repo}/pull/{N}",
    "fixPrNumber": N,
    "fixesApplied": M,
    "reportPath": "plans/epic-{epicId}/reports/pr-comments-report.md"
  }
}
```

### 4.5 Checkpoint Schema — `prCommentRemediation`

The `prCommentRemediation` field is added to `execution-state.json` at the top level:

```json
{
  "prCommentRemediation": {
    "status": "COMPLETE | SKIPPED | DRY_RUN",
    "fixPrUrl": "https://github.com/{owner}/{repo}/pull/159",
    "fixPrNumber": 159,
    "fixesApplied": 8,
    "reportPath": "plans/epic-{epicId}/reports/pr-comments-report.md"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | String (Enum) | Yes | `COMPLETE`, `SKIPPED`, or `DRY_RUN` |
| `fixPrUrl` | String | When COMPLETE | URL of the correction PR |
| `fixPrNumber` | Integer | When COMPLETE | GitHub PR number of the correction PR |
| `fixesApplied` | Integer | Yes | Number of fixes applied (0 when SKIPPED or DRY_RUN) |
| `reportPath` | String | When COMPLETE or DRY_RUN | Path to the generated findings report |

### Dry-Run Output (Phase 0, Step 10)

When `--dry-run` is set, the execution plan is saved as a persistent artifact
(see Execution Plan Persistence in Phase 0 Step 9) and the console output
displays a summary:

```
Epic Execution Plan (DRY RUN)
=============================
Epic: EPIC-{epicId}
Model: per-story PR (each story creates its own PR targeting develop)
Stories: N total across M phases

Execution plan saved to: plans/epic-{epicId}/reports/epic-execution-plan-{epicId}.md

Phase 0:
  - story-XXXX-0001: Branch feat/story-XXXX-0001-*, PR -> develop
  - story-XXXX-0002: Branch feat/story-XXXX-0002-*, PR -> develop
  Advisory warnings: [overlap warnings if any]

Phase 1:
  - story-XXXX-0003: Branch feat/story-XXXX-0003-*, PR -> develop
    Dependencies: story-XXXX-0001 (must be merged), story-XXXX-0002 (must be merged)

Flags: --auto-merge={value}, --no-merge={value}, --interactive-merge={value}, --single-pr=false, --skip-review={value}, --strict-overlap={value}
Merge mode: {auto|no-merge|interactive}
```

> **Dry-run persistence:** In dry-run mode, the execution plan is the primary output.
> It allows human review of the plan before committing to a real execution. No stories
> are dispatched and no PRs are created.

> **`--single-pr` dry-run:** When `--single-pr` is set, the dry-run output shows
> `Model: single-pr (legacy)` with the epic branch name instead of per-story branches.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-4-Remediation ok`

## Error Handling

| Scenario | Action |
|----------|--------|
| Epic ID missing from arguments | Abort: `ERROR: Epic ID is required. Usage: /x-epic-implement [EPIC-ID] [flags]` |
| Epic directory not found | Abort: `ERROR: Directory plans/epic-{epicId}/ not found. Run /x-epic-decompose first.` |
| IMPLEMENTATION-MAP.md missing | Abort: `ERROR: IMPLEMENTATION-MAP.md not found. Run /x-epic-map first.` |
| No story files found | Abort: `ERROR: No story files found matching story-{epicId}-*.md.` |
| `--phase` and `--story` both provided | Abort: `ERROR: --phase and --story are mutually exclusive` |
| Mutually exclusive merge flags | Abort: `ERROR: --auto-merge, --no-merge, and --interactive-merge are mutually exclusive. Use only one.` |
| Subagent returns invalid result (missing required fields) | Mark story as FAILED with summary: `Invalid subagent result: missing {field} field` |
| Integrity gate FAIL with identified regression | Revert commit, mark story FAILED, trigger block propagation for dependents |
| Integrity gate FAIL without identified regression | Pause execution, report to user |
| Rebase conflict resolution fails after MAX_REBASE_RETRIES (3) | Abort rebase, mark story FAILED, close PR, trigger block propagation |
| 3 consecutive story failures (circuit breaker) | Transition to OPEN, pause execution, AskUserQuestion |
| 5 total failures in phase (circuit breaker) | Abort phase, mark remaining as BLOCKED |
| `EPIC_BATCH_FIX_LOOP_EXCEEDED` — 3 consecutive FIX-PR at batch gate | Auto-terminate gate, log error code + manual resume instructions, exit skill |
| Reference file not found (RULE-002) | Log warning, continue without reference |
| Template file not found (RULE-012 — Template Fallback) | Log warning, use inline format as fallback |

## Template Fallback

Templates referenced by this skill follow RULE-012. When a template file does not exist, the skill degrades gracefully with a logged warning:

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
| `x-story-implement` | Invokes (per story) | Story execution with PR creation, reviews in Phases 4/7 |
| `x-git-worktree` | Invokes (Section 1.4a steps 2.6 and 1.4d) | Per-story worktree provisioning (`create`) before subagent dispatch and explicit removal (`remove`) after PR merge + auto-rebase completes; implements ADR-0004 §D2 and Rule 14 §5 (Creator Owns Removal) |
| `x-pr-fix-epic` | Invokes (Phase 4) | PR comment remediation; dry-run first, then apply with confirmation |
| `x-epic-orchestrate` | References | Produces DoR files (`dor-story-*.md`) consumed by DoR pre-check (Section 1.1b) |
| `x-epic-map` | References | Error guidance when map is missing |
| `x-lib-version-bump` | Invokes (post-gate) | Version bump on `develop` after integrity gate PASS (RULE-013) |
| `gh pr view` | Uses | PR merge status verification for dependency enforcement |
| `gh pr merge` | Uses | Auto-merge when `--auto-merge` is set |
| `gh pr close` | Uses | Close PR on story failure |
| `_TEMPLATE-EPIC-EXECUTION-PLAN.md` | Reads | Execution plan format (Phase 0 Step 9) |
| `_TEMPLATE-PHASE-COMPLETION-REPORT.md` | Reads | Phase completion report format (Phase 1 Step 8) |
| `_TEMPLATE-EPIC-EXECUTION-REPORT.md` | Reads | Progress report template (Phase 2) |
| `execution-state.json` | Reads/Writes | Checkpoint data for resume, status tracking |

**Additional notes:**
- Phase 0.5 is skipped when `--sequential` is set (no parallel dispatch means no conflict risk)
- Phase 0.5 defaults to advisory mode (warnings only); use `--strict-overlap` for blocking partitioning
- All `{{PLACEHOLDER}}` tokens are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
- Integrity gate runs on `develop` after all phase PRs are merged (RULE-006 — Gate Enforcement); uses `mainShaBeforePhase` for diff
- Integrity gate includes smoke tests (Step 5) as regression validation — runs `{{SMOKE_COMMAND}}`
- Smoke gate is MANDATORY (EPIC-0042); `--skip-smoke-gate` has been removed. Smoke failures must be fixed before advancing.
- Auto-rebase (Section 1.4e, RULE-011) triggers after each PR merge to keep remaining PRs up-to-date
- Conflict resolution (Section 1.4c, RULE-012) dispatches subagent for automatic rebase conflict resolution
- `--single-pr` preserves legacy flow: epic branch + single mega-PR (all per-story PR logic is skipped)
- `--no-merge`, `--auto-merge`, and `--interactive-merge` are mutually exclusive; default is no-merge mode (RULE-003)
- With `--no-merge` (default): dependency check relaxed to `status == SUCCESS` only; integrity gate uses local gate; version bump is skipped
- Interactive mode (opt-in via `--interactive-merge`) prompts user at phase boundaries with 3 options: merge all, pause for manual merge, or skip merge
- Resume workflow respects `mergeMode` from checkpoint for consistent behavior; warns if mode changed on resume
- Phase 4 (PR Comment Remediation) is optional, skipped with `--skip-pr-comments` or `--single-pr`
- Phase 4 writes `prCommentRemediation` to `execution-state.json` with status `COMPLETE`, `SKIPPED`, or `DRY_RUN`
- DoR pre-check (Section 1.1b) is NON-BLOCKING when DoR files don't exist — backward compatible with epics planned before `x-epic-orchestrate` existed
- Per-task checkpoint (Section 1.1c) tracks individual task progress within PRE_PLANNED stories; the `tasks` field is optional in the StoryEntry schema
- Task-level resume (Step 1b) reclassifies IN_PROGRESS tasks to PENDING on `--resume`, preserving DONE tasks; enables granular resume without re-executing completed tasks

---

## v2 Simplification (EPIC-0038 — Task Management Delegated)

This appendix documents the scope tightening introduced by story-0038-0007. The
pre-0038 orchestrator duplicated task-level concerns (per-task tracking, per-task PR
gates, cross-story task dependency enforcement) that properly belong to x-story-implement.
EPIC-0038 moves those concerns down one level and keeps x-epic-implement focused on
its natural scope: **stories as units of delivery**.

### Scope Reduction Matrix

| Concern | Pre-0038 (v1) | Post-0038 (v2) |
|---------|---------------|----------------|
| Parse epic, resolve stories | Epic orchestrator | Epic orchestrator (unchanged) |
| Phase order dispatch | Epic orchestrator | Epic orchestrator (unchanged) |
| Story-level PR management | Epic orchestrator | Epic orchestrator (unchanged) |
| Cross-story dependency gate | Epic orchestrator | Epic orchestrator (status-only; no task-level deps) |
| Epic integrity gate / smoke | Epic orchestrator | Epic orchestrator (unchanged) |
| **Task-level tracking** | Epic orchestrator | **x-story-implement** |
| **Per-task PR creation** | Epic orchestrator | **x-story-implement** (via x-test-tdd + x-pr-create) |
| **Coalesced task bundling** | Epic orchestrator (ad-hoc) | **x-story-implement** (via task-map COALESCED) |
| **Per-task TDD enforcement** | Epic orchestrator (best-effort) | **x-task-implement** (per-cycle RED assertion) |
| **Batch approval for tasks** | Epic orchestrator | **x-story-implement** (single story-level approval gate) |

### execution-state.json Projection (v2)

In v2, the epic orchestrator still reads `execution-state.json` but operates ONLY on
the story projection. Task-level fields (`tasks` map per story) are visible but
treated as **read-only**: the epic orchestrator never mutates task entries.

```json
{
  "version": "2.0",
  "stories": {
    "story-XXXX-YYYY": {
      "status": "SUCCESS",                 // <- epic orchestrator reads/writes
      "prNumber": 123,                      // <- epic orchestrator reads/writes
      "tasks": { /* delegated */ }          // <- READ-ONLY from epic's perspective
    }
  }
}
```

If a legacy epic still has task-level state (v1 execution-state), the orchestrator
detects this during resume (`SchemaVersionResolver`) and reverts to the legacy
task-aware code paths.

### Argument Surface (unchanged)

All CLI arguments continue to work; none is removed. The following flags were used in
v1 to configure task-level behaviour and are now **delegated** to x-story-implement
but kept for backward compatibility with scripts:

- `--batch-approval` — passed through to x-story-implement; semantic identical.
- `--task-tracking` — passed through; v2 always tracks tasks inside x-story-implement.
- `--auto-approve-pr` — passed through; story-implement decides per-task merge policy.

### Dispatcher Invocation Contract (v2)

For each story in the resolved phase order, the orchestrator invokes:

```
Skill(skill: "x-story-implement", args: "<STORY-ID> [flags inherited from epic]")
```

and collects a `StoryResult`:

```json
{
  "status": "SUCCESS" | "FAILED" | "PARTIAL",
  "storyId": "story-XXXX-YYYY",
  "prUrl": "...",
  "prNumber": 123,
  "coverageLine": 95.8,
  "coverageBranch": 91.2,
  "waveCount": N,      // new in v2 — number of waves x-story-implement dispatched
  "taskCount": M       // new in v2 — total tasks within the story
}
```

The orchestrator records `status` + `prNumber` + coverage deltas per story; never
inspects `waveCount`/`taskCount` beyond logging them in the phase report.

### v2 Benefits

- **Reduced maintenance surface**: epic orchestrator SKILL.md drops task-tracking
  tables and per-task PR flow from its phase bodies. When task semantics change
  (new testability kind, new DoD item), only x-story-implement needs an update.
- **Cleaner mental model**: the epic layer matches the domain layer in DDD — work
  units composed of stories composed of tasks. Each layer manages only its own
  concerns.
- **Failure attribution**: a story-level failure in v2 is always a real story-level
  failure (e.g., a PR conflict on merge). Task-level failures are caught and
  diagnosed inside x-story-implement before escalating.

### Compatibility Matrix — `planningSchemaVersion` (story-0038-0008)

The flag lives at the root of `plans/epic-XXXX/execution-state.json` and gates the
choice between legacy and task-first execution across the three execution skills
(x-task-implement, x-story-implement, x-epic-implement). Resolution is performed by
`dev.iadev.domain.schemaversion.SchemaVersionResolver` and the result (plus a
fallback reason, if any) is logged at the start of orchestration.

| Epic Range | `planningSchemaVersion` | Flow | Notes |
|------------|------------------------|------|-------|
| epic-0025..0037 | `"1.0"` or field absent | Legacy top-down | `x-story-plan` monolithic; tasks are story sub-sections |
| epic-0036 (rename) | `"1.0"` | Legacy top-down | Primary concern was skill taxonomy, not planning paradigm |
| epic-0038 (this) | `"1.0"` | Legacy top-down | Spec §8.2 bootstrap: the task-first epic itself runs in v1 |
| epic-0039+ | `"2.0"` | Task-first bottom-up | First dogfood of task-first; uses x-task-plan + x-task-implement |

Fallback semantics (RULE-TF-05 Backward Compatibility):

| Condition | Result | Emitted log |
|-----------|--------|-------------|
| File absent | V1 | `SCHEMA_VERSION_FALLBACK_NO_FILE` |
| Field absent | V1 | `SCHEMA_VERSION_FALLBACK_MISSING_FIELD` |
| Field malformed (`"legacy"`, `"3.0"`) | V1 | `SCHEMA_VERSION_INVALID_VALUE` |
| Field explicitly `"1.0"` | V1 | (no warning) |
| Field explicitly `"2.0"` | V2 | (no warning) |
| JSON unparseable | Hard fail (`UncheckedIOException`) | — |

**Zero-regression guarantee:** legacy epics (pre-0038) that never write the field
are treated exactly as before. The only new observable is a single-line
`schema: v1 [NO_FILE]` log at the start of orchestration.
