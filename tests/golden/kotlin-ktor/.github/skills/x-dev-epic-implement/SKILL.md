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

### Mode: `--story XXXX-YYYY`

Execute a single story in isolation.

1. Read checkpoint (required for single story mode)
2. Validate that ALL dependencies of the story have status SUCCESS
3. If validation fails, abort:
   - Story not in map: `Story {XXXX-YYYY} not found in implementation map`
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
8. If `--resume`: read execution-state.json, skip completed stories
9. Delegate per-story execution to x-dev-lifecycle

## Phase 1 — Execution Loop

> Placeholder: Implemented in story-0005-0005.

## Phase 2 — Consolidation

> Placeholder: Implemented in story-0005-0011.

## Phase 3 — Verification

> Placeholder: Implemented in story-0005-0011.

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story), `x-story-map` (error guidance)
- All `{{PLACEHOLDER}}` tokens are runtime markers — NOT resolved during generation
