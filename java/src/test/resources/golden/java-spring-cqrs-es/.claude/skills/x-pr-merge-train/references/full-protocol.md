<!-- Returns to [slim body](../SKILL.md) after reading the required phase. -->

# x-pr-merge-train — Full Protocol

## Phase 0 — Preparation

### Step 0.1 — Detect Worktree Context

Invoke the `x-git-worktree` skill via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-git-worktree", args: "detect-context")

Returns `{inWorktree, worktreePath, mainRepoPath}`.

| `inWorktree` | `worktreeOwnership` | Action |
| :--- | :--- | :--- |
| `true` | `REUSE_PARENT` | Reuse current worktree. Do NOT create nested (Rule 14 §3). |
| `false` | `TRAIN_OWNS_WORKTREE` | Create dedicated worktree for the train. |

Log: `"[Phase 0] worktreeOwnership={REUSE_PARENT|TRAIN_OWNS_WORKTREE}"`

### Step 0.2 — Derive trainId

| Mode | trainId pattern | Example |
| :--- | :--- | :--- |
| `--epic ID` | `{ID}-{timestamp}` | `0042-20260419T1430` |
| `--prs ...` | `manual-{timestamp}` | `manual-20260419T1430` |
| `--pattern ...` | `manual-{timestamp}` | `manual-20260419T1430` |

`{timestamp}` is UTC in `YYYYMMDDTHHmm` format.

### Step 0.3 — Initialize state.json

Create `plans/merge-train/{trainId}/` and write:

```json
{
  "schemaVersion": "1.0",
  "trainId": "{trainId}",
  "phase": "PREPARATION",
  "worktreeOwnership": "{TRAIN_OWNS_WORKTREE|REUSE_PARENT}",
  "prs": []
}
```

---

## Phase 1 — Discovery

### Step 1.0 — Mode Validation

Exactly one of `--prs`, `--epic`, or `--pattern` must be provided. Zero or multiple → `MODE_AMBIGUOUS`.

Update `state.json`: `phase = "DISCOVERY"`.

### Step 1.1 — Mode Dispatch

#### Mode A: `--prs N,M,...`
Parse comma-separated integers; preserve declared order; assign `discoveredPrs`.

#### Mode B: `--epic ID`
Resolve `plans/epic-{ID}/execution-state.json`. Traverse `stories[].tasks[].prNumber` for non-null entries. Sort by `storyId` ascending then `TASK-ID` ascending.

#### Mode C: `--pattern regex`
Execute: `gh pr list --search "{pattern}" --state open --json number,createdAt --jq '.[] | [.number, .createdAt] | @csv'`
Sort by `createdAt` ascending.

### Step 1.2 — Update state.json

Persist `discoveredPrs` as stub per-PR entries with `validationStatus: "PENDING"`.

---

## Phase 2 — Validation

### Step 2.0 — Iterate Over Discovered PRs

Update `state.json`: `phase = "VALIDATION"`.

For each PR: `gh pr view <pr> --json state,mergeable,isDraft,reviewDecision,baseRefName,headRefName,statusCheckRollup`

### Step 2.1 — VETO Evaluation (applied in order; stop at first VETO)

| Code | Condition | Message |
| :--- | :--- | :--- |
| `PR_CLOSED` | `state != "OPEN"` | `PR #N não está aberto.` |
| `PR_DRAFT` | `isDraft == true` | `PR #N está em draft.` |
| `PR_BASE_MISMATCH` | `baseRefName != "develop"` | `PR #N não tem base develop.` |
| `PR_NOT_APPROVED` | `reviewDecision != "APPROVED"` | `PR #N não foi aprovado.` |
| `PR_CI_FAILING` | `statusCheckRollup` has `FAILURE`/`ERROR` | `PR #N tem CI vermelha.` |
| `PR_MERGE_CONFLICT` | `mergeable == "CONFLICTING"` | `PR #N tem conflitos de merge pendentes.` |

### Step 2.2 — VETO Handling

**Normal mode:** any VETO → emit summary + abort. No VETOs → proceed to Phase 3.

**Dry-run mode:** regardless of VETOs, emit full audit plan and exit:
```
DRY-RUN PLAN — x-pr-merge-train
trainId: {trainId}
PR Validation Results:
  PR #374 [feat/task-0042-0001-001]: VALID
  PR #375 [feat/task-0042-0001-002]: PR_DRAFT (would abort in normal mode)
Merge order (if all valid): #374 → #375
VETOs detected: 1
```

---

## Phase 3 — Sort + File-Overlap Precheck

### Step 3.1 — Sort Validated PR List

| Discovery Mode | Sort Rule |
| :--- | :--- |
| `--prs` | Preserve explicit declaration order. |
| `--epic` | Already sorted by storyId + TASK-ID ascending from Phase 1; preserve. |
| `--pattern` | Already sorted by createdAt ascending from Phase 1; preserve. |

`BASE_PR` = first PR in ordered list. `TAIL[]` = all remaining.

### Step 3.2 — File-Overlap Precheck

For each PR: `gh pr view <pr> --json files --jq '.files[].path'`

For every pair `(PR_i, PR_j)`: compute `intersection = files(PR_i) ∩ files(PR_j)`.

| Path | Classification |
| :--- | :--- |
| `golden/**` or `java/src/test/resources/golden/**` | `GOLDEN_OVERLAP` — safe; regen handles it. |
| Any other path | `CODE_OVERLAP` — forces serial execution. |

Any `CODE_OVERLAP` → force `MAX_PARALLEL = 1`, set `neuteredParallel = true`.
`NEUTERED_PARALLEL` is informational only — train proceeds serially.

---

## Phase 4 — Base PR Merge

### Step 4.1 — Trigger Auto-Merge

Update `state.json`: `phase = "MERGING_BASE"`.

```bash
gh pr merge <BASE_PR> --squash --auto --delete-branch
```

Branch-protection rejection (required checks, merge queue) → `MERGE_REJECTED_BY_PROTECTION` → abort train.

### Step 4.2 — Poll for Merge Completion

Poll every 60 seconds (default timeout 30 minutes = 1800s; override via `--merge-timeout-seconds`):

```bash
gh pr view <BASE_PR> --json state,mergeStateStatus --jq '[.state, .mergeStateStatus] | @csv'
```

| Observed State | Action |
| :--- | :--- |
| `state == "MERGED"` | Break poll loop; proceed. |
| `mergeStateStatus == "BLOCKED"` | Abort with `MERGE_REJECTED_BY_PROTECTION`. |
| Timeout reached | Abort with `MERGE_POLL_TIMEOUT`. |
| Any other | Continue polling. |

### Step 4.3 — Update state.json

```json
{"phase": "MERGING_BASE_DONE", "prsMergedOk": [374]}
```

---

## Phase 5 — Parallel Tail Orchestration

### Step 5.1 — Canonical Rebase Subagent Prompt

The following prompt MUST be sent verbatim to each rebase worker subagent, with `<PR>`, `<HEAD>`, and `<TRAIN_ID>` substituted at dispatch time.

---

**CANONICAL REBASE SUBAGENT PROMPT (embed verbatim — RULE-005)**

You are a rebase worker for merge-train `<TRAIN_ID>`. Your task is to rebase PR `#<PR>` (branch `<HEAD>`) onto the latest `origin/develop` and push the result. Then write your result to `plans/merge-train/<TRAIN_ID>/worker-<PR>.log`.

**Procedure:**

1. `git fetch origin`
2. `git checkout <HEAD>`
3. `git rebase origin/develop`
4. For each conflict:
   - If path matches `golden/**` or `java/src/test/resources/golden/**`:
     ```bash
     git checkout --ours <file> && git add <file> && git rebase --continue
     cd java && mvn compile test-compile
     java -cp target/classes:target/test-classes:$(mvn dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout) dev.iadev.golden.GoldenFileRegenerator
     mvn test
     git add java/src/test/resources/golden/ && git rebase --continue
     ```
   - If path does NOT match `golden/**`: `git rebase --abort` + write FAILED log.
5. `git push --force-with-lease origin <HEAD>` (one retry on rejection)
6. On success: write `{"status":"OK","headSha":"<sha>","durationMs":<ms>}` to `worker-<PR>.log`.
7. On failure: write `{"status":"FAILED","reason":"<CODE>","file":"<path?>","headSha":"<sha>","durationMs":<ms>}`.

**Worker Log Schema:** `status` (OK|FAILED), `reason` (CODE_CONFLICT_NEEDS_HUMAN|PUSH_LEASE_REJECTED|GOLDENS_REGEN_FAILED), `file?`, `headSha`, `durationMs`.

---

### Step 5.2 — Wave Dispatcher Loop

```
WAVE_INDEX = 1
while TAIL[] is not empty:
  1. Take min(MAX_PARALLEL, len(TAIL[])) PRs from head of TAIL[] → WAVE_PRS[]
  2. Update state.json: phase = "WAVE_{N}_DISPATCHED"
  3. Dispatch ALL WAVE_PRS in ONE message as sibling Agent calls (Rule 13 Pattern 2):
       Agent(subagent_type: "general-purpose", description: "Rebase+regen+push PR #<PR>",
             prompt: "<CANONICAL_REBASE_PROMPT with PR/HEAD/TRAIN_ID substituted>")
  4. Wait for ALL subagents; read worker-<PR>.log for each
  5. Update state.json: phase = "WAVE_{N}_RETURNED"
  6. On FAILED_PRS: add to prsFailed[], log, abort train (worktree preserved — Rule 14 §4)
  7. Serial merge of OK_PRS: gh pr merge <pr> --squash --auto --delete-branch + poll (same as Phase 4)
  8. Remove merged PRs from TAIL[]
  9. WAVE_INDEX += 1
```

---

## Phase 6 — Final Verification

```bash
git fetch origin develop && git checkout develop && git pull --ff-only
cd java && mvn compile   # → SMOKE_TEST_FAILED on non-zero
mvn test                 # → SMOKE_TEST_FAILED on non-zero
```

For each merged PR: `gh pr view <pr> --json state --jq '.state'` → assert `"MERGED"` (WARNING on mismatch, non-fatal).

Update `state.json`: `phase = "VERIFYING_DONE"`.

---

## Phase 7 — Report + Cleanup

### Step 7.2 — Write report.md

```
# Merge Train Report — <trainId>
**Started:** <startedAt>  **Ended:** <now>  **Duration:** <elapsed>

## PRs Merged
| PR # | Role | Wave | Duration | Merge SHA |
| 374  | BASE | —    | 42s      | abc1234   |

## Waves
| Wave | PRs  | Dispatched           | Returned             |
| 1    | #375 | 2026-04-19T14:32:00Z | 2026-04-19T14:33:31Z |

## Errors Observed
| Code  | PR # | Message |
| (none)| —    | —       |

## Final Phase
COMPLETED
```

### Step 7.3 — Conditional Worktree Cleanup

| Case | Action |
| :--- | :--- |
| `TRAIN_OWNS_WORKTREE` and `phase != FAILED` | `Skill(skill: "x-git-worktree", args: "remove --id <trainId>")` |
| `phase == FAILED` | Preserve worktree (Rule 14 §4 — failed tasks must not be auto-removed). |
| `worktreeOwnership == REUSE_PARENT` | Skip cleanup; orchestrator owns the worktree. |

---

## state.json — Complete Schema

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `schemaVersion` | `String` | M | `"1.0"` |
| `trainId` | `String` | M | `epic-NNNN-TIMESTAMP` or `manual-TIMESTAMP` |
| `startedAt` | `String` (ISO-8601) | M | UTC timestamp of train start |
| `lastPhaseCompletedAt` | `String` (ISO-8601) | M | Updated at end of each phase |
| `phase` | `Enum` | M | Current phase name |
| `worktreeOwnership` | `Enum` | M | `TRAIN_OWNS_WORKTREE` or `REUSE_PARENT` |
| `neuteredParallel` | `Boolean` | M | `true` if Phase 3 forced `MAX_PARALLEL=1` |
| `maxParallel` | `Integer` | M | Effective max-parallel value |
| `dryRun` | `Boolean` | M | Whether `--dry-run` was passed |
| `prs` | `List<PrEntry>` | M | Per-PR state: `number`, `headRefName`, `baseRefName`, `mergeable`, `reviewDecision`, `isDraft`, `state`, `validationStatus`, `role` (BASE/TAIL) |
| `prsMergedOk` | `List<Integer>` | M | Append-only merged PR numbers |
| `prsFailed` | `List<{pr,reason}>` | M | Failed PRs with error code |
| `waves` | `List<WaveEntry>` | M | `{index, prs, dispatchedAt, returnedAt}` |

**Atomic write pattern:** write to `state.json.tmp` → validate JSON → `mv state.json.tmp state.json`. Prevents corruption on SIGKILL.

---

## Resume Entry Logic (`--resume`)

**Prerequisites:** existing `plans/merge-train/<trainId>/state.json`. Multiple state files → `--train-id` mandatory; missing → `STATE_CONFLICT`.

Example: `/x-pr-merge-train --resume --train-id epic-0042-20260415-143022`

**Behaviour:**
1. Load `state.json`; determine last completed phase.
2. Skip completed phases; re-enter next incomplete phase.
3. Preserve `prsMergedOk[]` and `waves[]` — do NOT re-merge already-merged PRs.
4. On `CODE_CONFLICT_NEEDS_HUMAN`: human resolves conflict → `git rebase --continue` → push → `--resume` continues from next wave.
5. On `SMOKE_TEST_FAILED`: diagnose, fix → `--resume` re-runs Phase 6.

---

## Error Handling

| Code | Phase | Condition | Remediation |
|------|-------|-----------|-------------|
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

---

## Integration Notes

| Skill | Relationship | When |
| :--- | :--- | :--- |
| `x-git-worktree` | Invoked (INLINE-SKILL, Phase 0.1 + Phase 7.3) | Detect context; cleanup when `TRAIN_OWNS_WORKTREE`. |
| `x-git-commit` | Not called directly | Commits made by rebase-worker subagents via git CLI. |
| `x-pr-fix-epic` | Manual invocation by operator | After `--resume` following `CODE_CONFLICT_NEEDS_HUMAN` to fix PR review comments. |
| `x-story-implement` | Orthogonal | Merge-train operates on already-open PRs; not called and does not call. |
