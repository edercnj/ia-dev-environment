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

## Phase 1 — Discovery

### Step 1.0 — Mode Validation

Exactly one of `--prs`, `--epic`, or `--pattern` must be provided. Count the number of mode flags present:

| Count | Action |
| :--- | :--- |
| 0 | Abort with `MODE_AMBIGUOUS`: `"Informe exatamente um de --prs, --epic ou --pattern."` |
| 1 | Continue to Step 1.1 |
| 2+ | Abort with `MODE_AMBIGUOUS`: `"Informe exatamente um de --prs, --epic ou --pattern."` |

Update `state.json`: set `phase = "DISCOVERY"`.

### Step 1.1 — Mode Dispatch

Dispatch exclusively to the matching mode handler:

#### Mode A: `--prs N,M,...`

1. Parse the comma-separated string into a list of positive integers.
   - Validation: every token must be a positive integer; non-integer tokens abort with `MODE_AMBIGUOUS`.
2. Preserve the declared order — do NOT sort.
3. Assign `discoveredPrs = [N, M, ...]`.

Log: `"[Phase 1] --prs mode: discovered {count} PR(s): {list}"`

#### Mode B: `--epic ID`

1. Resolve path: `plans/epic-{ID}/execution-state.json`.
2. If the file does not exist, abort with `EPIC_STATE_MISSING`:
   `"execution-state.json de epic-{ID} não encontrado. Use --prs ou --pattern."`
3. Parse the JSON. Traverse `stories[storyId].tasks[TASK-ID].prNumber` for every entry where `prNumber` is non-null and non-zero.
4. Sort the collected PR numbers deterministically:
   - Primary sort: `storyId` ascending (lexicographic).
   - Secondary sort: `TASK-ID` ascending (lexicographic).
5. Assign `discoveredPrs = [sorted list]`.

Log: `"[Phase 1] --epic mode: resolved {count} PR(s) from epic-{ID} execution-state"`

#### Mode C: `--pattern regex`

1. Execute: `gh pr list --search "{pattern}" --state open --json number,createdAt --jq '.[] | [.number, .createdAt] | @csv'`
2. Parse output into `(number, createdAt)` pairs.
3. Sort by `createdAt` ascending (oldest first).
4. Assign `discoveredPrs = [sorted list of numbers]`.

Log: `"[Phase 1] --pattern mode: discovered {count} open PR(s) matching \"{pattern}\""`

### Step 1.2 — Update state.json

Persist the discovered list to `state.json` with stub per-PR entries:

```json
{
  "schemaVersion": "1.0",
  "trainId": "{trainId}",
  "phase": "DISCOVERY",
  "worktreeOwnership": "{value}",
  "prs": [
    {
      "number": 374,
      "headRefName": null,
      "baseRefName": null,
      "mergeable": null,
      "reviewDecision": null,
      "isDraft": null,
      "state": null,
      "validationStatus": "PENDING"
    }
  ]
}
```

Log: `"[Phase 1] Discovery complete. {count} PR(s) queued for validation."`

## Phase 2 — Validation

### Step 2.0 — Iterate Over Discovered PRs

Update `state.json`: set `phase = "VALIDATION"`.

For each PR number in `discoveredPrs` (in order), fetch its metadata:

```bash
gh pr view <pr> --json state,mergeable,isDraft,reviewDecision,baseRefName,headRefName,statusCheckRollup
```

### Step 2.1 — VETO Evaluation

Apply the following six VETO checks **in order** for each PR. Stop at the first VETO:

| Code | Condition | Message |
| :--- | :--- | :--- |
| `PR_CLOSED` | `state != "OPEN"` | `PR #N não está aberto.` |
| `PR_DRAFT` | `isDraft == true` | `PR #N está em draft.` |
| `PR_BASE_MISMATCH` | `baseRefName != "develop"` | `PR #N não tem base develop.` |
| `PR_NOT_APPROVED` | `reviewDecision != "APPROVED"` | `PR #N não foi aprovado.` |
| `PR_CI_FAILING` | `statusCheckRollup` contains any entry with `state == "FAILURE"` or `state == "ERROR"` | `PR #N tem CI vermelha.` |
| `PR_MERGE_CONFLICT` | `mergeable == "CONFLICTING"` | `PR #N tem conflitos de merge pendentes.` |

Update the matching PR entry in `state.json`:
- `headRefName`, `baseRefName`, `mergeable`, `reviewDecision`, `isDraft`, `state` — set from API response
- `validationStatus` — set to `"VALID"` if no VETO; set to the VETO code string if vetoed (e.g., `"PR_DRAFT"`)

Log each result: `"[Phase 2] PR #{N}: {VALID|VETO_CODE}"`

### Step 2.2 — VETO Handling

After evaluating all PRs, apply the mode-specific VETO policy:

#### Normal mode (no `--dry-run`)

If **any** PR has a VETO:
1. Update `state.json`: persist all VETO codes.
2. Emit a summary of all VETOs:
   ```
   TRAIN ABORTED — validation failed:
     PR #374: PR_DRAFT — PR #374 está em draft.
     PR #376: PR_CI_FAILING — PR #376 tem CI vermelha.
   Fix the above issues and re-run x-pr-merge-train.
   ```
3. Exit — do NOT proceed to Phase 3.

If **no** PRs have VETOs:
1. Update `state.json`: all PRs have `validationStatus = "VALID"`.
2. Log: `"[Phase 2] Validation complete. All {count} PR(s) passed. Ready for Phase 3."`
3. Proceed to Phase 3 (implemented in story-0042-0002).

#### Dry-run mode (`--dry-run`)

Regardless of VETO presence:
1. Update `state.json`: persist all validation results.
2. Emit a full audit plan:
   ```
   DRY-RUN PLAN — x-pr-merge-train
   trainId: {trainId}

   PR Validation Results:
     PR #374 [feat/task-0042-0001-001]: VALID
     PR #375 [feat/task-0042-0001-002]: PR_DRAFT (would abort in normal mode)
     PR #376 [feat/task-0042-0001-003]: VALID

   Merge order (if all valid): #374 → #375 → #376
   VETOs detected: 1 (would abort before Phase 3 in normal mode)
   ```
3. Exit — do NOT proceed to Phase 3 in dry-run mode.

Log: `"[Phase 2] Dry-run complete. {vetoed} VETO(s) detected across {count} PR(s)."`

