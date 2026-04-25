---
name: x-pr-merge-train
description: "Merge-train automation: discovers, validates, and merges a sequence of PRs into develop in deterministic order. Supports --prs, --epic, and --pattern discovery modes with pre-merge validation and dry-run auditing."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent, TaskCreate, TaskUpdate
argument-hint: "[--prs N,M,...] [--epic ID] [--pattern regex] [--max-parallel N] [--dry-run] [--resume]"
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

## Phase Overview (Rule 25 — Task Hierarchy)

Eight phases (Rule 25 REGRA-001, EPIC-0055). Each opens with a PRE gate + `TaskCreate`, closes with a POST/FINAL gate + `TaskUpdate(completed)`. Phase 0 is exempted from gates (read-only setup). Full per-phase implementation in `references/full-protocol.md`.

```
0. PREPARATION  → detect state file, parse args, validate mode flag (inline)
1. DISCOVERY    → enumerate PRs for the chosen mode (inline + gh CLI)
2. VALIDATION   → VETO check per PR — open, non-draft, approved, CI pass (inline)
3. SORTING      → topological sort + overlap pre-check (inline)
4. BASE-MERGE   → merge base wave into develop (x-pr-merge per PR)
5. REBASE-WAVE  → parallel rebase workers per wave (subagents)
6. SMOKE-VERIFY → mvn test after all merges (inline)
7. REPORT       → write report.md + cleanup (inline)
```

State machine: `PREPARATION → DISCOVERY → VALIDATION → SORTING → MERGING_BASE → WAVE_N_DISPATCHED → WAVE_N_RETURNED → VERIFYING → REPORTING → COMPLETED` (or `FAILED`).

<!-- phase-no-gate: read-only arg parsing and state-file detection; no artifact produced -->
## Phase 0 - Preparation

Open a phase tracker (close with `TaskUpdate(id: phase0TaskId, status: "completed")` at end of setup):

    TaskCreate(subject: "PR-MERGE › Phase 0 - Preparation", activeForm: "Preparing merge-train setup")

Resolve `--resume` (load existing `state.json`) vs fresh run. Validate exactly one of `--prs`, `--epic`, `--pattern`. Produce `trainId` (derived from mode + epoch). Errors: `MODE_AMBIGUOUS`, `EPIC_STATE_MISSING`, `STATE_CONFLICT`.

See `references/full-protocol.md §Phase 0` for full implementation.

    TaskUpdate(id: phase0TaskId, status: "completed")

## Phase 1 - Discovery

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-pr-merge-train --phase Phase-1-Discovery")
    TaskCreate(subject: "PR-MERGE › Phase 1 - Discovery", activeForm: "Discovering PRs for merge train")

Enumerate PRs per chosen mode:
- `--prs`: parse CSV, preserve order.
- `--epic`: read `plans/epic-{ID}/execution-state.json`, collect merged story PR URLs.
- `--pattern`: `gh pr list --search {pattern} --state open --json number,createdAt | sort by createdAt`.

Write initial `state.json` at `plans/merge-train/{trainId}/state.json` with `phase: DISCOVERY` and the discovered PR list. Advance state to `VALIDATION`.

See `references/full-protocol.md §Phase 1` for full implementation.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-pr-merge-train --phase Phase-1-Discovery --expected-artifacts plans/merge-train/{trainId}/state.json")
    TaskUpdate(id: phase1TaskId, status: "completed")

## Phase 2 - Validation

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-pr-merge-train --phase Phase-2-Validation")
    TaskCreate(subject: "PR-MERGE › Phase 2 - Validation", activeForm: "Validating PR merge eligibility")

For each discovered PR, query `gh pr view {N} --json state,isDraft,baseRefName,reviewDecision,mergeable,statusCheckRollup` and apply VETO rules:
- `PR_CLOSED`: `state != OPEN`
- `PR_DRAFT`: `isDraft == true`
- `PR_BASE_MISMATCH`: `baseRefName != develop`
- `PR_NOT_APPROVED`: `reviewDecision != APPROVED`
- `PR_CI_FAILING`: any check with `FAILURE` or `ERROR`
- `PR_MERGE_CONFLICT`: `mergeable == CONFLICTING`

If `--dry-run`: print VETO report and exit after Phase 2. Non-vetoed PRs advance to Phase 3.

See `references/full-protocol.md §Phase 2` for full implementation.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-pr-merge-train --phase Phase-2-Validation --expected-artifacts plans/merge-train/{trainId}/state.json")
    TaskUpdate(id: phase2TaskId, status: "completed")

## Phase 3 - Sorting

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-pr-merge-train --phase Phase-3-Sorting")
    TaskCreate(subject: "PR-MERGE › Phase 3 - Sorting", activeForm: "Sorting and overlap-checking PRs")

Topological sort validated PRs by `createdAt` (or explicit `--prs` order). Run file-overlap pre-check against each pair: if code-file overlap detected, emit `NEUTERED_PARALLEL` advisory and force `MAX_PARALLEL=1` (serial). Update `state.json` with sorted order and parallelism decision.

See `references/full-protocol.md §Phase 3` for full implementation.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-pr-merge-train --phase Phase-3-Sorting --expected-artifacts plans/merge-train/{trainId}/state.json")
    TaskUpdate(id: phase3TaskId, status: "completed")

## Phase 4 - Base Merge

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-pr-merge-train --phase Phase-4-BaseMerge")
    TaskCreate(subject: "PR-MERGE › Phase 4 - Base Merge", activeForm: "Merging base PRs into develop")

For each PR in sorted order, invoke `x-pr-merge`:

    Skill(skill: "x-pr-merge", args: "--pr {N} --strategy squash --target develop")

On `MERGE_REJECTED_BY_PROTECTION` or `MERGE_POLL_TIMEOUT`: abort train with state `FAILED`. Record failed PR in `state.json`. See `references/full-protocol.md §Phase 4`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-pr-merge-train --phase Phase-4-BaseMerge --expected-artifacts plans/merge-train/{trainId}/state.json")
    TaskUpdate(id: phase4TaskId, status: "completed")

## Phase 5 - Rebase Wave

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-pr-merge-train --phase Phase-5-RebaseWave")
    TaskCreate(subject: "PR-MERGE › Phase 5 - Rebase Wave", activeForm: "Rebasing PR wave onto develop")

Dispatch rebase workers in parallel (up to `--max-parallel` sibling agents per wave). Each worker:
1. Creates worktree via `x-git-worktree`.
2. Rebases branch onto develop HEAD.
3. On `GOLDENS_REGEN_FAILED`: runs `GoldenFileRegenerator` and re-pushes.
4. On `CODE_CONFLICT_NEEDS_HUMAN`: preserves worktree, marks PR `FAILED`, continues.
5. Writes result to `plans/merge-train/{trainId}/worker-{pr}.log`.

Collect results; any non-fatal failure marks that PR skipped but continues the train.

See `references/full-protocol.md §Phase 5` for full implementation.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-pr-merge-train --phase Phase-5-RebaseWave --expected-artifacts plans/merge-train/{trainId}/state.json")
    TaskUpdate(id: phase5TaskId, status: "completed")

## Phase 6 - Smoke Verify

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-pr-merge-train --phase Phase-6-SmokeVerify")
    TaskCreate(subject: "PR-MERGE › Phase 6 - Smoke Verify", activeForm: "Running smoke verification after merges")

After all wave merges, run:
```bash
mvn test
```
On failure: set state `FAILED`, emit `SMOKE_TEST_FAILED`, preserve worktree for diagnosis.
On success: advance to Phase 7.

See `references/full-protocol.md §Phase 6` for full implementation.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-pr-merge-train --phase Phase-6-SmokeVerify --expected-artifacts plans/merge-train/{trainId}/state.json")
    TaskUpdate(id: phase6TaskId, status: "completed")

## Phase 7 - Report

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-pr-merge-train --phase Phase-7-Report")
    TaskCreate(subject: "PR-MERGE › Phase 7 - Report", activeForm: "Writing merge-train completion report")

Write `plans/merge-train/{trainId}/report.md` with: PRs merged, waves executed, errors encountered, total duration. Update `state.json` to `phase: COMPLETED`. Cleanup worktrees of successfully merged PRs via `x-git-worktree cleanup`.

See `references/full-protocol.md §Phase 7` for full implementation.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode final --skill x-pr-merge-train --phase Phase-7-Report --expected-artifacts plans/merge-train/{trainId}/report.md,plans/merge-train/{trainId}/state.json")
    TaskUpdate(id: phase7TaskId, status: "completed")

## Full Protocol

> Full 8-phase workflow (Phase 0 Preparation → Phase 7 Report + Cleanup), `state.json` complete
> schema, atomic-write pattern, `--resume` logic, rebase-subagent canonical prompt, file-overlap
> precheck algorithm, and VETO evaluation rules in [`references/full-protocol.md`](references/full-protocol.md).
