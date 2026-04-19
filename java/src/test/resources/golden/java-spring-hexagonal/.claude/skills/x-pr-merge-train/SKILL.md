---
name: x-pr-merge-train
description: "Merge-train automation: discovers, validates, and merges a sequence of PRs into develop in deterministic order. Supports --prs, --epic, and --pattern discovery modes with pre-merge validation and dry-run auditing."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent
argument-hint: "[--prs N,M,...] [--epic ID] [--pattern regex] [--max-parallel N] [--dry-run] [--resume]"
context-budget: heavy
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
Phase 0: Preparation              — detect worktree context, initialize state.json, derive trainId
Phase 1: Discovery                — resolve PR list from --prs / --epic / --pattern
Phase 2: Validation               — validate each PR against 6 VETO criteria; abort or report
Phase 3: Sort + File-Overlap Precheck — reorder by createdAt, detect file overlap, set MAX_PARALLEL
Phase 4: Base PR Merge            — merge BASE_PR with auto-merge + 60s poll; emit MERGE_POLL_TIMEOUT on timeout
Phase 5: Parallel Tail Orchestration — dispatch TAIL[] as sibling Agent() waves; serial merge after each wave
Phase 6: Final Verification       — git fetch + pull + mvn compile + mvn test after all merges
Phase 7: Report + Cleanup         — emit report.md, conditional worktree removal, state.phase=COMPLETED
```

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

## Phase 3 — Sort + File-Overlap Precheck

### Step 3.1 — Sort Validated PR List

Update `state.json`: set `phase = "SORTING"`.

Determine merge order based on discovery mode:

| Discovery Mode | Sort Rule |
| :--- | :--- |
| `--prs N,M,...` | Preserve the explicit declaration order from Phase 1. |
| `--epic ID` | Already sorted by `storyId` + `TASK-ID` ascending from Phase 1; preserve. |
| `--pattern regex` | Already sorted by `createdAt` ascending from Phase 1; preserve. |

The first PR in the ordered list is `BASE_PR`. All remaining PRs form the `TAIL[]` list.

Log: `"[Phase 3] Sort complete. BASE_PR={N}, TAIL={list}"`

### Step 3.2 — File-Overlap Precheck

For each PR in the full validated list, fetch the set of files touched:

```bash
gh pr view <pr> --json files --jq '.files[].path'
```

For every pair `(PR_i, PR_j)` where `i < j`, compute the intersection of their file sets:

```
intersection = files(PR_i) ∩ files(PR_j)
```

Classify each intersecting path:

| Path | Classification |
| :--- | :--- |
| `golden/**` or `java/src/test/resources/golden/**` | `GOLDEN_OVERLAP` — safe; regen handles it |
| Any other path | `CODE_OVERLAP` — forces serial execution |

If **any** pair has a `CODE_OVERLAP` intersection (at least one path outside `golden/**`):

1. Force `MAX_PARALLEL = 1` for Phase 5 (regardless of `--max-parallel` argument).
2. Set `neuteredParallel = true` in `state.json`.
3. Log: `"[Phase 3] NEUTERED_PARALLEL — overlap detected between PRs {i} and {j} on file {file}. Forcing MAX_PARALLEL=1."`

> `NEUTERED_PARALLEL` is **not an error**. The train proceeds normally; Phase 5 simply dispatches one PR at a time. This event is recorded in `state.json` for telemetry and post-run analysis.

If **no** `CODE_OVERLAP` is found (all intersections are golden-only or empty):

1. Keep `MAX_PARALLEL` at the value provided by `--max-parallel` (default `3`).
2. Set `neuteredParallel = false` in `state.json`.
3. Log: `"[Phase 3] No code-file overlap detected. MAX_PARALLEL={value}"`

### Step 3.3 — Update state.json

```json
{
  "phase": "SORTING",
  "neuteredParallel": true,
  "basePr": 374,
  "tail": [375, 376, 377]
}
```

Log: `"[Phase 3] Precheck complete. neuteredParallel={true|false}. Proceeding to Phase 4."`

## Phase 4 — Base PR Merge

### Step 4.1 — Trigger Auto-Merge

Update `state.json`: set `phase = "MERGING_BASE"`.

Execute:

```bash
gh pr merge <BASE_PR> --squash --auto --delete-branch
```

- `--squash`: collapse all commits into a single squash commit on `develop`.
- `--auto`: GitHub waits for required status checks to pass before merging.
- `--delete-branch`: delete the head branch after successful merge.

If the command exits non-zero with a message indicating branch protection rejection
(e.g., `required status checks`, `required reviews`, `merge queue`):

1. Log: `"[Phase 4] MERGE_REJECTED_BY_PROTECTION — PR #{N}: {reason}"`
2. Update `state.json`: add `{pr: N, reason: "MERGE_REJECTED_BY_PROTECTION"}` to `prsFailed[]`.
3. **Abort the train immediately** — do NOT proceed to Phase 5.

### Step 4.2 — Poll for Merge Completion

Poll every 60 seconds until `state == "MERGED"` or the configured timeout expires
(default: 30 minutes = 1800 seconds; override via `--merge-timeout-seconds`):

```bash
gh pr view <BASE_PR> --json state,mergeStateStatus \
  --jq '[.state, .mergeStateStatus] | @csv'
```

| Observed State | Action |
| :--- | :--- |
| `state == "MERGED"` | Break the poll loop and proceed. |
| `mergeStateStatus == "BLOCKED"` | Abort immediately with `MERGE_REJECTED_BY_PROTECTION`. |
| Timeout reached | Abort with `MERGE_POLL_TIMEOUT`. |
| Any other state | Continue polling. |

**On `MERGE_POLL_TIMEOUT`:**

1. Log: `"[Phase 4] MERGE_POLL_TIMEOUT — PR #{N} did not reach MERGED within {timeout}s."`
2. Update `state.json`: add `{pr: N, reason: "MERGE_POLL_TIMEOUT"}` to `prsFailed[]`.
3. **Abort the train** — do NOT start Phase 5. Preserve state.json for `--resume`.

### Step 4.3 — Update state.json After Successful Merge

```json
{
  "phase": "MERGING_BASE_DONE",
  "prsMergedOk": [374]
}
```

Log: `"[Phase 4] BASE_PR #{N} merged successfully. Proceeding to Phase 5."`

## Phase 5 — Parallel Tail Orchestration

### Step 5.1 — Canonical Rebase Subagent Prompt

The following prompt MUST be sent verbatim to each rebase worker subagent, with
`<PR>`, `<HEAD>`, and `<TRAIN_ID>` substituted at dispatch time.

---

**CANONICAL REBASE SUBAGENT PROMPT (embed verbatim — RULE-005)**

You are a rebase worker for merge-train `<TRAIN_ID>`. Your task is to rebase
PR `#<PR>` (branch `<HEAD>`) onto the latest `origin/develop` and push the
result. Then write your result to `plans/merge-train/<TRAIN_ID>/worker-<PR>.log`.

**Context:**
- PR number: `<PR>`
- Branch head: `<HEAD>`
- Base branch: `develop`
- Train ID: `<TRAIN_ID>`

**Procedure:**

1. Fetch the latest remote state:
   ```bash
   git fetch origin
   ```

2. Check out the PR branch:
   ```bash
   git checkout <HEAD>
   ```

3. Start the interactive rebase:
   ```bash
   git rebase origin/develop
   ```

4. **For each conflict encountered during rebase:**

   - Check the conflicting file path.
   - If the path matches `golden/**` or `java/src/test/resources/golden/**`:
     ```bash
     git checkout --ours <file>
     git add <file>
     git rebase --continue
     ```
     Then regenerate goldens using the canonical regen block (RULE-004 — RULE-005):
     ```bash
     cd java
     mvn compile test-compile
     java -cp target/classes:target/test-classes:$(mvn dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout) \
       dev.iadev.golden.GoldenFileRegenerator
     mvn test
     ```
     Stage regenerated golden files and continue:
     ```bash
     git add java/src/test/resources/golden/
     git rebase --continue
     ```
   - If the path does NOT match `golden/**`:
     ```bash
     git rebase --abort
     ```
     Write failure log and stop immediately:
     ```json
     {"status": "FAILED", "reason": "CODE_CONFLICT_NEEDS_HUMAN", "file": "<conflicting-file>", "headSha": "<current-sha>", "durationMs": <elapsed>}
     ```
     Write to `plans/merge-train/<TRAIN_ID>/worker-<PR>.log` and exit.

5. Push the rebased branch with lease protection (one retry on rejection):
   ```bash
   git push --force-with-lease origin <HEAD>
   ```
   If the push is rejected (remote moved since fetch):
   ```bash
   git fetch origin
   git rebase origin/develop
   git push --force-with-lease origin <HEAD>
   ```
   If the second push is also rejected:
   Write failure log:
   ```json
   {"status": "FAILED", "reason": "PUSH_LEASE_REJECTED", "headSha": "<current-sha>", "durationMs": <elapsed>}
   ```
   Write to `plans/merge-train/<TRAIN_ID>/worker-<PR>.log` and exit.

6. On success, record the final HEAD SHA and write the result log:
   ```bash
   HEAD_SHA=$(git rev-parse HEAD)
   ```
   ```json
   {"status": "OK", "headSha": "<HEAD_SHA>", "durationMs": <elapsed>}
   ```
   Write to `plans/merge-train/<TRAIN_ID>/worker-<PR>.log`.

**Worker Log Schema (`worker-<PR>.log`):**

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `status` | `String` (enum: `OK`, `FAILED`) | Mandatory | Final outcome |
| `reason` | `String` | Mandatory if FAILED | One of: `CODE_CONFLICT_NEEDS_HUMAN`, `PUSH_LEASE_REJECTED`, `GOLDENS_REGEN_FAILED` |
| `file` | `String` | Optional | Relative path of conflicting file; present only on `CODE_CONFLICT_NEEDS_HUMAN` |
| `headSha` | `String` (40 hex chars) | Mandatory | Branch HEAD SHA after rebase (or last known SHA on failure) |
| `durationMs` | `Long` | Mandatory | Elapsed milliseconds (≥ 0) |

---

### Step 5.2 — Wave Dispatcher Loop

The wave dispatcher repeatedly pulls up to `MAX_PARALLEL` PRs from `TAIL[]` and
dispatches them as sibling `Agent(...)` calls in a **single assistant message**
(Rule 13 Pattern 2 — SUBAGENT-GENERAL). It continues until `TAIL[]` is empty.

**Loop:**

```
WAVE_INDEX = 1
while TAIL[] is not empty:
    1. Take min(MAX_PARALLEL, len(TAIL[])) PRs from head of TAIL[] → WAVE_PRS[]
    2. Update state.json: phase = "WAVE_{N}_DISPATCHED",
       waves[] += {index: WAVE_INDEX, prs: WAVE_PRS, dispatchedAt: <ISO-8601>}
    3. Dispatch ALL WAVE_PRS in ONE message as sibling Agent calls:
         Agent(
           subagent_type: "general-purpose",
           description: "Rebase+regen+push PR #<PR>",
           prompt: "<CANONICAL_REBASE_PROMPT with PR=<PR>, HEAD=<headRefName>, TRAIN_ID=<trainId>>"
         )
       — one Agent(...) call per PR, all emitted as siblings in the same assistant message
    4. Wait for ALL subagents to return (read worker-<PR>.log for each PR in WAVE_PRS)
    5. Update state.json: phase = "WAVE_{N}_RETURNED",
       waves[last].returnedAt = <ISO-8601>
    6. Consolidate results:
         OK_PRS = [pr for pr in WAVE_PRS if worker-<pr>.log.status == "OK"]
         FAILED_PRS = [pr for pr in WAVE_PRS if worker-<pr>.log.status == "FAILED"]
    7. If FAILED_PRS is non-empty:
         - For each failed PR: add {pr, reason, file?} to state.json.prsFailed[]
         - Log: "[Phase 5] Wave {N}: {count} PR(s) failed rebase. Aborting train."
         - Preserve worktree for diagnosis (Rule 14 §4 — failed tasks must not be auto-removed)
         - Abort the train; do NOT process remaining TAIL[] entries
    8. Serial merge of OK_PRS (in wave order):
         for pr in OK_PRS:
             gh pr merge <pr> --squash --auto --delete-branch
             Poll every 60s until state == "MERGED" or timeout (same logic as Phase 4)
             On success: append pr to state.json.prsMergedOk[]
             On MERGE_POLL_TIMEOUT or MERGE_REJECTED_BY_PROTECTION:
               add {pr, reason} to state.json.prsFailed[]
               Abort train
    9. Remove OK_PRS (and any merged) from TAIL[]
   10. WAVE_INDEX += 1
```

**Rule 13 compliance:** ZERO bare-slash patterns (`/x-foo`) in Phase 5 delegation
context. All subagent dispatch uses the `Agent(subagent_type: "general-purpose", ...)`
form shown above.

### Step 5.3 — Completion

When `TAIL[]` is empty and all merges succeeded:

1. Update `state.json`: `phase = "TAIL_MERGED_DONE"`.
2. Log: `"[Phase 5] All {count} tail PR(s) rebased and merged. Proceeding to Phase 6."`

## Phase 6 — Final Verification

### Step 6.1 — Update state.json

Update `state.json`: set `phase = "VERIFYING"`.

### Step 6.2 — Fetch and Update develop

```bash
git fetch origin develop
git checkout develop
git pull --ff-only
```

### Step 6.3 — Compile Check

```bash
cd java && mvn compile
```

Expect exit code `0`. On failure:

1. Update `state.json`: `phase = "FAILED"`, `reason = "SMOKE_TEST_FAILED"`.
2. Log: `"[Phase 6] SMOKE_TEST_FAILED — mvn compile failed after all merges. Worktree preserved."`
3. Preserve worktree for diagnosis (Rule 14 §4 — failed tasks must not be auto-removed).
4. Abort — do NOT proceed to Phase 7.

### Step 6.4 — Test Check

```bash
mvn test
```

Expect exit code `0`. On failure:

1. Update `state.json`: `phase = "FAILED"`, `reason = "SMOKE_TEST_FAILED"`.
2. Log: `"[Phase 6] SMOKE_TEST_FAILED — mvn test failed after all merges. Worktree preserved at plans/merge-train/<trainId>/"`
3. Preserve worktree for diagnosis (Rule 14 §4).
4. Abort — do NOT proceed to Phase 7.

### Step 6.5 — PR State Assertion

For each PR in `prsMergedOk[]`, assert that GitHub reports the PR as merged:

```bash
gh pr view <pr> --json state --jq '.state'
```

Assert the output equals `"MERGED"`. Any mismatch logs a WARNING but does NOT abort:

```
[Phase 6] WARNING: PR #<N> expected MERGED but got <state>
```

### Step 6.6 — Finalize Phase

Update `state.json`:

```json
{
  "phase": "VERIFYING_DONE",
  "lastPhaseCompletedAt": "<now ISO-8601>"
}
```

Log: `"[Phase 6] Final verification complete. develop is GREEN. Proceeding to Phase 7."`

## Phase 7 — Report + Cleanup

### Step 7.1 — Update state.json

Update `state.json`: `phase = "REPORTING"`.

### Step 7.2 — Write report.md

Write `plans/merge-train/<trainId>/report.md` with the following sections:

```
# Merge Train Report — <trainId>

**Started:** <startedAt>
**Ended:** <now>
**Duration:** <elapsed human-readable>

## PRs Merged

| PR # | Role | Wave | Duration | Merge SHA |
| :--- | :--- | :--- | :--- | :--- |
| 374 | BASE | — | 42s | abc1234 |
| 375 | TAIL | 1 | 87s | def5678 |
| 376 | TAIL | 1 | 91s | ghi9012 |

## Waves

| Wave | PRs | Dispatched | Returned | Workers |
| :--- | :--- | :--- | :--- | :--- |
| 1 | #375, #376 | 2026-04-19T14:32:00Z | 2026-04-19T14:33:31Z | 2 |

## Errors Observed

| Code | PR # | Message |
| :--- | :--- | :--- |
| (none) | — | — |

## Final Phase

COMPLETED
```

If no errors occurred, the Errors Observed table contains a single `(none)` row.
If errors occurred, each entry from `prsFailed[]` is listed.

### Step 7.3 — Conditional Worktree Cleanup

**Case A — TRAIN_OWNS_WORKTREE and phase != "FAILED":**

Invoke the `x-git-worktree` skill via the Skill tool (Rule 13 Pattern 1 — INLINE-SKILL):

    Skill(skill: "x-git-worktree", args: "remove --id <trainId>")

Log: `"[Phase 7] Worktree removed for trainId <trainId>."`

**Case B — phase == "FAILED":**

Do NOT remove the worktree. Preserve it for diagnosis (Rule 14 §4 — failed tasks must not be auto-removed).

Log: `"[Phase 7] Train FAILED — worktree preserved at .claude/worktrees/<trainId> for diagnosis."`

**Case C — worktreeOwnership == "REUSE_PARENT":**

Skip cleanup. The orchestrator (parent skill) owns the worktree and is responsible for removal (Rule 14 §5 — creator owns removal).

Log: `"[Phase 7] REUSE_PARENT — worktree cleanup skipped (orchestrator owns the worktree)."`

### Step 7.4 — Finalize state.json

Update `state.json`:

```json
{
  "phase": "COMPLETED",
  "lastPhaseCompletedAt": "<now ISO-8601>"
}
```

Log: `"[Phase 7] Train <trainId> complete. Report: plans/merge-train/<trainId>/report.md"`


