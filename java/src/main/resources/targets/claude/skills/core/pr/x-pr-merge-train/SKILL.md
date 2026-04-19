---
name: x-pr-merge-train
description: "Merge-train automation: discovers, validates, and merges a sequence of PRs into develop in deterministic order. Supports --prs, --epic, and --pattern discovery modes with pre-merge validation and dry-run auditing."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent
argument-hint: "[--prs N,M,...] [--epic ID] [--pattern regex] [--max-parallel N] [--dry-run] [--resume]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers. Start directly with technical information.
- **Progress lines**: Prefix with the current phase, e.g. `[Phase 0]`, `[Phase 1]`, `[Phase 2]`.

## Purpose

Automate the sequential merge of a pre-validated list of pull requests into `develop`. The skill discovers the PR list from one of three modes (`--prs`, `--epic`, `--pattern`), validates each PR against six hard criteria before touching any branch, and then merges them in order — aborting on the first failure to prevent partial-state corruption.

A `--dry-run` invocation reports the full plan (order, status, VETO codes) without merging anything, providing an auditable preview before committing to a potentially irreversible operation.

## Triggers

```
/x-pr-merge-train --epic 0042 --dry-run
/x-pr-merge-train --prs 374,375,376
/x-pr-merge-train --pattern "feat/task-0042-" --max-parallel 2
/x-pr-merge-train --epic 0042 --resume
```

## Parameters

| Flag | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `--prs` | `String` (CSV of integers) | Mutually exclusive¹ | — | Literal comma-separated PR numbers. Order preserved as declared. Example: `374,375,376` |
| `--epic` | `String` (4-digit ID) | Mutually exclusive¹ | — | Reads `plans/epic-{ID}/execution-state.json` to resolve PR numbers from task entries. |
| `--pattern` | `String` (GitHub search regex) | Mutually exclusive¹ | — | Enumerates open PRs via `gh pr list --search`. Sorted by `createdAt` ascending. |
| `--max-parallel` | `Integer` | Optional | `3` | Maximum number of concurrent rebase workers in Phase 4+ (1–8). Does not affect Phases 0–2. |
| `--dry-run` | Flag | Optional | `false` | Report plan and VETOs without merging. Skill exits after Phase 2 with full audit output. |
| `--resume` | Flag | Optional | `false` | Resume a previously interrupted train. Requires an existing `plans/merge-train/{id}/state.json`. |

> ¹ Exactly one of `--prs`, `--epic`, or `--pattern` must be provided. Providing zero or more than one emits `MODE_AMBIGUOUS` and aborts.

## Workflow Overview

```
Phase 0: Preparation   — detect worktree context, initialize state.json, derive trainId
Phase 1: Discovery     — resolve PR list from --prs / --epic / --pattern
Phase 2: Validation    — validate each PR against 6 VETO criteria; abort or report
Phase 3: Rebase        — rebase each PR branch onto latest develop (story-0042-0002)
Phase 4: Merge         — merge rebased branches into develop in order (story-0042-0002)
Phase 5: Verification  — post-merge CI check + state.json finalization (story-0042-0003)
Phase 6: Cleanup       — remove train worktree if TRAIN_OWNS_WORKTREE=true (story-0042-0003)
Phase 7: Report        — emit final summary with merge SHAs and timings (story-0042-0003)
```

> Phases 3–7 are implemented in stories 0042-0002 and 0042-0003. This skill (story-0042-0001) delivers Phases 0–2 as the walking skeleton.

## Phase 0 — Preparation

### Step 0.1 — Detect Worktree Context

Invoke the `x-git-worktree` skill via the Skill tool (Rule 13 — INLINE-SKILL pattern) to classify the current execution context:

    Skill(skill: "x-git-worktree", args: "detect-context")

The skill returns a JSON envelope:

```json
{
  "inWorktree": true,
  "worktreePath": "/abs/path/.claude/worktrees/story-XXXX-YYYY",
  "mainRepoPath": "/abs/path/to/repo"
}
```

Record `inWorktree` to derive `worktreeOwnership`:

| `inWorktree` | `worktreeOwnership` | Action |
| :--- | :--- | :--- |
| `true` | `REUSE_PARENT` | Reuse current worktree. Do NOT create a nested one (Rule 14 §3). |
| `false` | `TRAIN_OWNS_WORKTREE` | Create a dedicated worktree for the train (not implemented until story-0042-0002). |

Log: `"[Phase 0] worktreeOwnership={REUSE_PARENT|TRAIN_OWNS_WORKTREE}"`

### Step 0.2 — Derive trainId

Compute a deterministic `trainId` from the input arguments:

| Mode | trainId pattern | Example |
| :--- | :--- | :--- |
| `--epic ID` | `{ID}-{timestamp}` | `0042-20260419T1430` |
| `--prs ...` | `manual-{timestamp}` | `manual-20260419T1430` |
| `--pattern ...` | `manual-{timestamp}` | `manual-20260419T1430` |

`{timestamp}` is UTC in `YYYYMMDDTHHmm` format.

### Step 0.3 — Initialize state.json

Create directory `plans/merge-train/{trainId}/` and write a stub `state.json`:

```json
{
  "schemaVersion": "1.0",
  "trainId": "{trainId}",
  "phase": "PREPARATION",
  "worktreeOwnership": "{TRAIN_OWNS_WORKTREE|REUSE_PARENT}",
  "prs": []
}
```

Log: `"[Phase 0] state.json initialized at plans/merge-train/{trainId}/state.json"`

