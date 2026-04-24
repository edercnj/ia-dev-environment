---
name: x-pr-merge-train
description: "Merge-train automation: discovers, validates, and merges a sequence of PRs into develop in deterministic order. Supports --prs, --epic, and --pattern discovery modes with pre-merge validation and dry-run auditing."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent
argument-hint: "[--prs N,M,...] [--epic ID] [--pattern regex] [--max-parallel N] [--dry-run] [--resume]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers. Start directly with technical information.
- **Progress lines**: Prefix with the current phase, e.g. `[Phase 0]`, `[Phase 1]`, `[Phase 2]`.

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
| `--prs` | `String` (CSV integers) | Mutually exclusive¹ | — | Literal comma-separated PR numbers; order preserved. |
| `--epic` | `String` (4-digit ID) | Mutually exclusive¹ | — | Reads `plans/epic-{ID}/execution-state.json` to resolve PRs. |
| `--pattern` | `String` (GitHub search regex) | Mutually exclusive¹ | — | Enumerates open PRs via `gh pr list --search`. Sorted by `createdAt`. |
| `--max-parallel` | `Integer` | Optional | `3` | Max concurrent rebase workers in Phase 5 (1–8). |
| `--dry-run` | Flag | Optional | `false` | Report plan and VETOs without merging. Exits after Phase 2. |
| `--resume` | Flag | Optional | `false` | Resume interrupted train from `plans/merge-train/{id}/state.json`. |

> ¹ Exactly one of `--prs`, `--epic`, or `--pattern` must be provided; zero or multiple emits `MODE_AMBIGUOUS`.

## Output Contract

| Artifact | Path | Description |
| :--- | :--- | :--- |
| `state.json` | `plans/merge-train/{trainId}/state.json` | Full train lifecycle state; survives interruptions and supports `--resume`. |
| `report.md` | `plans/merge-train/{trainId}/report.md` | Human-readable completion report: PRs merged, waves, errors, duration. |
| `worker-{pr}.log` | `plans/merge-train/{trainId}/worker-{pr}.log` | Per-PR rebase worker result: `{status, headSha, durationMs, reason?}`. |

Train state transitions: `PREPARATION → DISCOVERY → VALIDATION → SORTING → MERGING_BASE → WAVE_N_DISPATCHED → WAVE_N_RETURNED → VERIFYING → REPORTING → COMPLETED` (or `FAILED`).

## Error Envelope

| Code | Phase | Condition | Remediation |
| :--- | :--- | :--- | :--- |
| `MODE_AMBIGUOUS` | 1 | 0 or 2+ mode flags | Provide exactly one of `--prs`, `--epic`, `--pattern`. |
| `EPIC_STATE_MISSING` | 1 | `--epic N` but no `execution-state.json` | Use `--prs` or `--pattern`. |
| `PR_CLOSED` | 2 | `state != "OPEN"` | Remove from train or reopen. |
| `PR_DRAFT` | 2 | `isDraft == true` | Mark PR as Ready for review. |
| `PR_BASE_MISMATCH` | 2 | `baseRefName != "develop"` | Rebase PR against develop. |
| `PR_NOT_APPROVED` | 2 | `reviewDecision != "APPROVED"` | Request review. |
| `PR_CI_FAILING` | 2 | CI has `FAILURE`/`ERROR` check | Fix CI. |
| `PR_MERGE_CONFLICT` | 2 | `mergeable == "CONFLICTING"` | Rebase manually. |
| `NEUTERED_PARALLEL` | 3 | Code-file overlap detected (advisory, non-fatal) | `MAX_PARALLEL=1` forced; train continues serially. |
| `MERGE_REJECTED_BY_PROTECTION` | 4–5 | Branch protection blocks merge | Adjust protection rules. |
| `MERGE_POLL_TIMEOUT` | 4–5 | PR did not reach `MERGED` within timeout | Increase `--merge-timeout-seconds` or investigate CI. |
| `CODE_CONFLICT_NEEDS_HUMAN` | 5 | Rebase conflict in non-golden file | Resolve manually → `git rebase --continue` → push → `--resume`. |
| `PUSH_LEASE_REJECTED` | 5 | `--force-with-lease` rejected after retry | Fetch + rebase manually → `--resume`. |
| `GOLDENS_REGEN_FAILED` | 5 | `GoldenFileRegenerator` non-zero | Diagnose build failure; see worker log. |
| `SMOKE_TEST_FAILED` | 6 | `mvn test` fails after all merges | Diagnose; worktree preserved for diagnosis. |
| `STATE_CONFLICT` | resume | No `state.json` or ambiguous train ID | Start fresh or provide `--train-id`. |

## Full Protocol

> Full 8-phase workflow (Phase 0 Preparation → Phase 7 Report + Cleanup), `state.json` complete
> schema, atomic-write pattern, `--resume` logic, rebase-subagent canonical prompt, file-overlap
> precheck algorithm, and VETO evaluation rules in [`references/full-protocol.md`](references/full-protocol.md).
