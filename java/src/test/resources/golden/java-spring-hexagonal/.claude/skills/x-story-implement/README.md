# x-story-implement

> Thin orchestrator (~340 lines — story-0049-0019 refactor) that drives a story end-to-end via 4 delegated phases. Zero inline git/gh/mvn calls; every substantive responsibility is delegated to specialized sub-skills.

| | |
|---|---|
| **Category** | Orchestrator (thin — ADR-0012 + EPIC-0049) |
| **Invocation** | `/x-story-implement STORY-ID [--target-branch <branch>] [--auto-merge <strategy>] [--epic-id <XXXX>] ...` |
| **Delegates to** | `x-internal-args-normalize`, `x-internal-story-load-context`, `x-internal-story-resume`, `x-internal-story-build-plan`, `x-task-implement`, `x-pr-create`, `x-pr-watch-ci`, `x-parallel-eval`, `x-review`, `x-review-pr`, `x-pr-fix`, `x-internal-story-verify`, `x-internal-story-report`, `x-internal-status-update`, `x-git-worktree` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## Overview

Runs the full story implementation lifecycle as 4 delegated phases. Every substantive responsibility — argv parsing, story loading, planning, TDD, verification, reporting — is delegated to a sub-skill. The orchestrator's inline work is limited to `Read`/`Glob` for local file discovery and `Skill`/`Agent` for delegation.

EPIC-0049 introduced three OO-style flags (`--target-branch`, `--auto-merge`, `--epic-id`) that propagate downward to `x-task-implement` and `x-pr-create`. When the flags are absent, the orchestrator preserves EPIC-0048 behavior exactly (target=develop, auto-merge=none).

## Execution Flow

```mermaid
flowchart TD
    START(["/x-story-implement STORY-ID"]) --> P0["Phase 0: Args, Context & Resume"]
    P0 --> P0A["0.1 x-internal-args-normalize"]
    P0A --> P0B["0.2 x-internal-story-load-context"]
    P0B --> P0C{--resume?}
    P0C -->|yes| P0D["0.4 x-internal-story-resume"]
    P0C -->|no| P1
    P0D --> P1

    P1["Phase 1: Plan"] --> P1A{PRE_PLANNED?}
    P1A -->|yes| P2
    P1A -->|no| P1B["x-internal-story-build-plan"]
    P1B --> P1C["x-parallel-eval"]
    P1C --> P2

    P2["Phase 2: Task Execution Loop"] --> P2A["x-task-implement per task"]
    P2A --> P2B["x-pr-watch-ci"]
    P2B --> P2C["x-pr-create"]
    P2C --> P2D{More tasks?}
    P2D -->|yes| P2A
    P2D -->|no| P3

    P3["Phase 3: Verify, Report & Cleanup"] --> P3A["3.1 x-internal-story-verify"]
    P3A --> P3B["3.2 x-review + x-review-pr"]
    P3B --> P3C["3.3 x-internal-story-report"]
    P3C --> P3D["3.4 Status finalize"]
    P3D --> P3E["3.5 x-git-worktree remove (Mode 2)"]
    P3E --> DONE(["Lifecycle Complete"])

    style DONE fill:#2d6a4f,color:#fff
```

## Phases

| # | Phase | Description | Delegated To |
|---|-------|-------------|--------------|
| 0 | Args, Context & Resume | Parse argv, load story, detect resume point, worktree decision | `x-internal-args-normalize`, `x-internal-story-load-context`, `x-internal-story-resume`, `x-git-worktree detect-context` |
| 1 | Plan | Parallel planning (arch + test + decomposition + security + compliance), parallelism gate | `x-internal-story-build-plan`, `x-parallel-eval` |
| 2 | Task Execution Loop | Per-task TDD + CI watch + PR create (flags propagate OO-style) | `x-task-implement`, `x-pr-watch-ci`, `x-pr-create`, `x-internal-status-update` |
| 3 | Verify, Report & Cleanup | Verify gate, specialist + TL reviews, final report, status finalize, worktree cleanup | `x-internal-story-verify`, `x-review`, `x-review-pr`, `x-pr-fix`, `x-internal-story-report`, `x-internal-status-update`, `x-git-worktree remove` |

## EPIC-0049 Flag Propagation

| Flag | Default | Propagation |
| :--- | :--- | :--- |
| `--target-branch <branch>` | `develop` | → `x-task-implement --target-branch`, `x-pr-create --target-branch` |
| `--auto-merge <strategy>` | `none` | → `x-pr-create --auto-merge` (requires `--target-branch` when not `none`) |
| `--epic-id <XXXX>` | auto-derived | → `x-pr-create --epic-id` (adds `epic-XXXX` label) |

With all three flags absent, behavior is identical to EPIC-0048 (backward compat — RULE-008).

## Prerequisites

- Story file exists with acceptance criteria and sub-tasks
- Predecessor stories (dependencies) are complete (validated by `x-internal-story-load-context`)
- Epic directory structure: `plans/epic-XXXX/plans/`, `plans/epic-XXXX/reports/`
- Git working tree is clean on the base branch

## Outputs

| Artifact | Path | Producer |
|----------|------|----------|
| Architecture / Implementation / Test / Task / Security / Compliance Plans | `plans/epic-XXXX/plans/*-story-XXXX-YYYY.md` | `x-internal-story-build-plan` (Phase 1) |
| Story Completion Report | `plans/epic-XXXX/reports/story-completion-report-STORY-ID.md` | `x-internal-story-report` (Phase 3.3) |
| Review Dashboard | `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` | `x-review` (Phase 3.2) |
| Per-task PRs | GitHub (targeting `--target-branch` or `develop`) | `x-pr-create` (Phase 2) |
| Story-level PR (`--auto-approve-pr` only) | GitHub (parent `feat/story-...` → `--target-branch`) | `x-pr-create` (Phase 2.2) |

## See Also

- [x-epic-implement](../x-epic-implement/) -- Epic-level orchestrator that dispatches this skill per story
- [x-task-implement](../x-task-implement/) -- TDD implementation engine used in Phase 2
- [x-internal-story-load-context](../../internal/plan/x-internal-story-load-context/) -- Story load + artifact pre-checks
- [x-internal-story-build-plan](../../internal/plan/x-internal-story-build-plan/) -- Parallel planning (Phase 1)
- [x-internal-story-verify](../../internal/plan/x-internal-story-verify/) -- Verify gate (Phase 3.1)
- [x-internal-story-report](../../internal/plan/x-internal-story-report/) -- Final report (Phase 3.3)
- [x-internal-story-resume](../../internal/plan/x-internal-story-resume/) -- Resume-point detection (Phase 0.4)
- [x-internal-args-normalize](../../internal/ops/x-internal-args-normalize/) -- Argv parsing (Phase 0.1)
