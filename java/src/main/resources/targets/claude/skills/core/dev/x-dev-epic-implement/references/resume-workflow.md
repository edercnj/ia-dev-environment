# Resume Workflow Reference

> **Context:** This reference is loaded when `--resume` flag is set.
> Part of x-dev-epic-implement skill.

## Resume Workflow

When `--resume` is set, the orchestrator loads `execution-state.json` and applies a two-pass reclassification before re-entering the execution loop.

### Step 1 — Reclassify Story Statuses

Apply the following status transitions to every story in the checkpoint.
For stories with PR fields, verify actual PR status via GitHub CLI.

| Current Status | New Status | Condition |
|----------------|------------|-----------|
| IN_PROGRESS | PENDING | Always (interrupted work) |
| SUCCESS | SUCCESS | Preserved — never re-execute |
| PR_CREATED | PR_CREATED or SUCCESS | Verify via `gh pr view {prNumber} --json state,mergedAt`: if MERGED → SUCCESS; if OPEN → keep PR_CREATED; if not found → FAILED |
| PR_PENDING_REVIEW | PR_PENDING_REVIEW or SUCCESS | Verify via `gh pr view`: if MERGED → SUCCESS; if OPEN → keep PR_PENDING_REVIEW; if not found → FAILED |
| PR_MERGED | SUCCESS | PR merged — story is complete |
| FAILED (retries < MAX_RETRIES) | PENDING | Retry candidate (close open PR if exists) |
| FAILED (retries >= MAX_RETRIES) | FAILED | Retry budget exhausted |
| PARTIAL | PENDING | Treat as interrupted |
| BLOCKED | BLOCKED | Deferred to reevaluation step |
| PENDING | PENDING | No change |

`MAX_RETRIES` defaults to 2. All other story fields (phase, commitSha, retries, summary, duration, findingsCount, prUrl, prNumber, prMergeStatus) are preserved.

#### PR Status Verification

For each story with a `prNumber`, verify the actual PR state:

```
state = gh pr view {prNumber} --json state,mergedAt
if state === "MERGED":
  update prMergeStatus = "MERGED"
  reclassify to SUCCESS
else if state === "OPEN":
  keep current status (PR_CREATED or PR_PENDING_REVIEW)
else if PR not found (error):
  reclassify to FAILED with reason "PR not found"
```

#### Failure Handling — PR Closure

When a story transitions to FAILED and has an open PR:

```
if story.prNumber exists and story.prMergeStatus !== "MERGED":
  run: gh pr close {prNumber} --comment "Story failed: {summary}"
  update: story.prMergeStatus = "CLOSED"
```

When a story is retried after failure, the lifecycle creates a new PR
(the old PR was closed). The new `prUrl` and `prNumber` replace the old values.

### Step 1b — Reclassify Task Statuses (Per-Task Resume)

After story-level reclassification (Step 1), apply task-level reclassification for stories with task data:

For each story that has a `tasks` object in `execution-state.json`:

1. **IN_PROGRESS tasks -> PENDING** (interrupted work — task was executing when interruption occurred)
2. **DONE tasks -> DONE** (preserved — completed tasks are never re-executed)
3. **PR_CREATED tasks**: verify via `gh pr view {prNumber} --json state,mergedAt`:
   - If MERGED -> DONE (PR was merged while orchestrator was interrupted)
   - If OPEN -> keep PR_CREATED (still awaiting review)
   - If CLOSED/not found -> FAILED with reason "PR closed or not found"
4. **PR_APPROVED tasks**: verify via `gh pr view`:
   - If MERGED -> DONE
   - If OPEN -> keep PR_APPROVED
   - If CLOSED/not found -> FAILED
5. **PR_MERGED tasks -> DONE** (PR merged — task is complete)
6. **BLOCKED tasks**: if all task dependencies are DONE -> PENDING; otherwise keep BLOCKED
7. **PENDING tasks -> PENDING** (no change)
8. **SKIPPED tasks -> SKIPPED** (no change — terminal status)

This enables resume at the task level: only incomplete tasks are re-executed, not the entire story. When a story resumes with some tasks DONE, the `x-dev-story-implement` / `x-dev-implement` subagent receives the task state and skips DONE tasks automatically.

**Backward Compatibility:** Stories without a `tasks` field in the checkpoint are unaffected by this step. The step is a no-op for stories executed in non-PRE_PLANNED mode.

### Step 2 — Reevaluate BLOCKED Stories

After reclassification, evaluate each BLOCKED story:

- If `blockedBy` is **undefined** → keep BLOCKED (conservative: unknown dependencies)
- If `blockedBy` is **empty array** → reclassify to PENDING (no dependencies = vacuously satisfied)
- If `mergeMode === "no-merge"`: if **all** dependencies in `blockedBy` have `status === SUCCESS` → reclassify to PENDING (prMergeStatus not checked)
- Otherwise: if **all** dependencies in `blockedBy` have status SUCCESS and `prMergeStatus === "MERGED"` → reclassify to PENDING
- If **any** dependency is non-SUCCESS or missing from the stories map → keep BLOCKED

This is a **single-pass** evaluation (no cascade). Stories unblocked in this pass will not trigger further unblocking of stories that depend on them.

### Step 2b — Reset Circuit Breaker

When `--resume` is used, fully reset the circuit breaker state to prevent stale failure counters from immediately triggering thresholds:

```
circuitBreaker.consecutiveFailures = 0
circuitBreaker.totalFailuresInPhase = 0
circuitBreaker.lastFailureAt = null (preserved for diagnostic reference, or reset)
circuitBreaker.lastFailurePattern = null
circuitBreaker.status = "CLOSED"
```

This ensures the resumed execution starts with a clean circuit breaker, regardless of the failure state that existed before the interruption.

### Step 3 — Resume Execution

After reclassification, PR verification, and circuit breaker reset, feed the updated state into `getExecutableStories()` to determine which stories are ready for execution. Only stories with status PENDING proceed to the execution loop. The orchestrator remains on `develop` during resume — no epic branch recovery is needed.
