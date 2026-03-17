---
name: x-dev-epic-implement
description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[EPIC-ID] [--phase N] [--story XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--parallel]"
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
| `--story XXXX-YYYY` | string | (all stories) | Execute only a specific story by ID |
| `--skip-review` | boolean | `false` | Skip review phases in x-dev-lifecycle subagents |
| `--dry-run` | boolean | `false` | Generate execution plan without executing |
| `--resume` | boolean | `false` | Continue from last checkpoint (execution-state.json) |
| `--parallel` | boolean | `false` | Enable parallel worktrees (default: sequential) |

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

## Phase 0 — Preparation (Orchestrator — Inline)

1. **Parse arguments**: Extract epic ID from positional argument and all optional flags
2. **Run prerequisites checks**: Execute all 5 prerequisite validations (abort on first failure)
3. **Read IMPLEMENTATION-MAP.md**: Extract the story dependency graph and execution order
4. **Read EPIC-XXXX.md**: Load epic context (title, description, acceptance criteria)
5. **Discover story files**: Glob `story-XXXX-*.md` to collect all story files in the epic directory
6. **Determine execution order**: Use the dependency graph from IMPLEMENTATION-MAP.md to order stories; stories without dependencies can run in parallel if `--parallel` is set
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

## Phase 1 — Execution Loop

> **Placeholder**: This phase will be implemented in story-0005-0005.
> It will contain the story-by-story execution loop, worktree management,
> checkpoint persistence, and error handling/retry logic.

## Phase 2 — Consolidation

> **Placeholder**: This phase will be implemented in story-0005-0011.
> It will contain cross-story consolidation, branch merging, conflict
> resolution, and epic-level documentation generation.

## Phase 3 — Verification

> **Placeholder**: This phase will be implemented in story-0005-0011.
> It will contain epic-level verification, cross-story integration
> testing, final DoD checklist, and epic completion reporting.

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story execution), `x-story-map` (if map missing, via error guidance)
- All `{{PLACEHOLDER}}` tokens are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
