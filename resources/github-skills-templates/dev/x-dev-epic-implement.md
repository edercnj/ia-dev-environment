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
| `--story XXXX-YYYY` | string | (all) | Execute only a specific story |
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

> Placeholder: Implemented in story-0005-0005.

## Phase 2 — Consolidation

> Placeholder: Implemented in story-0005-0011.

## Phase 3 — Verification

> Placeholder: Implemented in story-0005-0011.

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story), `x-story-map` (error guidance)
- All `{{PLACEHOLDER}}` tokens are runtime markers — NOT resolved during generation
