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

