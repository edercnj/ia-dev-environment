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
- [Placeholder: consolidation + verification — story-0005-0011]
- [Placeholder: progress reporting — story-0005-0013]

## Phase 2 — Consolidation

> Placeholder: Implemented in story-0005-0011.

## Phase 3 — Verification

> Placeholder: Implemented in story-0005-0011.

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story), `x-story-map` (error guidance)
- All `{{PLACEHOLDER}}` tokens are runtime markers — NOT resolved during generation
