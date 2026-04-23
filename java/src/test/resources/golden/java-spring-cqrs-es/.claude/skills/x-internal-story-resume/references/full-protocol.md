# x-internal-story-resume — Full Protocol

> Depth reference for `x-internal-story-resume`. The SKILL.md body is
> the normative contract; this document expands the workflow
> internals that orchestrators do not need in their working context
> but that implementers and auditors must be able to consult.

## 1. Argument-Parser Rejection Matrix

The parser is a tight single-file loop (`while (($#)); case "$1" in …`)
to keep the SKILL.md within the SkillSizeLinter 500-line threshold
without delegating to `x-internal-args-normalize` (that skill is a
peer, not a dependency — see Rule 14).

| Input | Result | Exit |
| :--- | :--- | :--- |
| `--story-id story-0049-0013 --epic-id 0049` | accepted | 0 |
| `--story-id story-0049-0013` (epic-id missing) | `usage: --epic-id is required` | 64 |
| `--epic-id 0049` (story-id missing) | `usage: --story-id is required` | 64 |
| `--story-id STORY-0049-0013 --epic-id 0049` | accepted (ID normalised to lowercase) | 0 |
| `--story-id story-0049-0013 --epic-id 49` | accepted (zero-padded to `0049`) | 0 |
| `--story-id story-49-13 --epic-id 49` | `usage: --story-id must match story-NNNN-NNNN` | 64 |
| `--unknown-flag` | `usage: unknown flag --unknown-flag` | 64 |
| `--story-id='' --epic-id=0049` | `usage: --story-id must not be empty` | 64 |
| `--help` | print banner + usage; exit 0 | 0 |

The banner is intentionally terse (< 20 lines) so sourcing the skill
from a parent script that accidentally passes `--help` does not flood
the caller's stdout.

## 2. execution-state.json Schema Contract

The skill consumes the following paths inside the state file; any
other schema evolution is safe:

| jq path | Expected type | Used for |
| :--- | :--- | :--- |
| `.stories` | `object` | map of `story-XXXX-YYYY` → story node |
| `.stories[<id>]` | `object` | presence check — exit 2 when missing |
| `.stories[<id>].tasks` | `object` (ordered map) | task classification; `keys_unsorted` preserves insertion order |
| `.stories[<id>].tasks[<taskId>].status` | `string` | bucket into `tasksCompleted` / `tasksPending` |
| `.stories[<id>].tasks[<taskId>].commitSha` | `string \| null` | surfaces into `tasksCompleted[].commitSha` and `lastCommitSha` |
| `.stories[<id>].tasks[<taskId>].completedAt` | `string` (ISO-8601) | staleness comparison (Step 5) |

Supported status strings (case-insensitive, trimmed):

| Success synonyms | Pending / non-terminal |
| :--- | :--- |
| `DONE`, `MERGED`, `COMPLETE`, `Concluída`, `Concluida`, `Done`, `Merged` | `PENDING`, `IN_PROGRESS`, `PR_CREATED`, `PR_APPROVED`, `PR_MERGED`, `FAILED`, `BLOCKED`, `UNKNOWN`, everything else |

`PR_MERGED` intentionally appears in the pending bucket: in the
`x-story-implement` lifecycle, `PR_MERGED` means the PR landed on
`develop` but `execution-state.json` has not yet been transitioned
to `DONE` by `x-internal-status-update`. The caller
(`x-story-implement`) is the sole authority on that transition;
classifying `PR_MERGED` as DONE here would pre-empt the
orchestrator's own lifecycle bookkeeping.

## 3. Task-Order Invariant Edge Cases

The baseline algorithm in Step 4 assumes DONE tasks precede PENDING
tasks in task-order (the invariant upheld by `x-story-implement`
Phase 2 wave dispatch, which never retries a PENDING task before an
earlier DONE one). When that invariant is violated, the skill
degrades gracefully:

| Task sequence (task-order) | `tasksCompleted` | `tasksPending` | `resumePoint` |
| :--- | :--- | :--- | :--- |
| DONE, DONE, PENDING, PENDING | 2 | 2 | `phase-2-task-3` |
| DONE, PENDING, DONE, PENDING | 2 | 2 | `phase-2-task-2` (first non-DONE wins) |
| PENDING, DONE, DONE, PENDING | 2 | 2 | `phase-2-task-1` (ditto) |
| DONE, DONE, DONE, DONE | 4 | 0 | `all-done` |
| PENDING, PENDING, PENDING, PENDING | 0 | 4 | `fresh-start` |
| (empty tasks map) | 0 | 0 | `fresh-start` |

The non-contiguous-DONE rows (#2 and #3) surface via a stderr
warning:

```text
warn: non-contiguous DONE tasks detected; resume point computed from first non-DONE position
```

so operators can reconcile the state file with `x-status-reconcile`
if the divergence is systematic. The envelope itself stays
well-formed.

`lastCommitSha` always tracks the LAST element of `tasksCompleted` in
task-order, regardless of the invariant. When the invariant holds,
this is the SHA of the most recently merged task; when violated, it
is the last DONE-labelled task in the state file's insertion order
— still a useful anchor for resuming, and matches the semantics of
"last checkpoint" that the caller expects.

## 4. ISO-8601 Timestamp Parser Portability

`completedAt` is written by `x-internal-status-update` as
`date -u +%Y-%m-%dT%H:%M:%SZ` (GNU and BSD both accept this form).
Step 5's staleness comparison reads it back via:

| Platform | Primary invocation | Fallback |
| :--- | :--- | :--- |
| BSD (macOS) | `date -j -f '%Y-%m-%dT%H:%M:%SZ' "${v}" +%s` | — |
| GNU (Linux) | `date -d "${v}" +%s` | — |
| Busybox (Alpine) | `date -u -D '%Y-%m-%dT%H:%M:%SZ' -d "${v}" +%s` | — |

The skill probes by trying the BSD form first (exit-on-failure), then
the GNU form, then the Busybox form. When all three fail, the
`completedAt` for that task is treated as "unknown" and its stale
check is silently skipped — a failed date parse is not a fatal
error; it is a hint that the state file was written by an unusual
toolchain, which the skill tolerates.

## 5. Concurrency Contract

- The skill does NOT open `<state_file>.lock` directly. It delegates
  the locked read to `x-internal-status-update --read-only`, which
  internally acquires `flock -s` (shared lock). The delegation
  consolidates all lock bookkeeping into the pilot skill and avoids
  duplicate lock-acquisition code paths.
- When running in degraded fallback mode (pilot skill unavailable),
  the skill calls `jq -c` directly on the state file without any
  lock. This is deliberate — the fallback is reserved for bootstrap
  scenarios in which no parallel writer exists (the pilot skill is
  itself being generated).
- Step 5 (staleness detection) runs lock-free: it reads the story
  file's mtime via `stat`, not the state file. No lock upgrade is
  required.
- The `x-internal-status-update --read-only` delegation also validates
  the state-file schema as a side effect; parse failures surface as
  its own exit 3, which this skill re-maps to exit 2
  (`STORY_NOT_IN_STATE`) when the cause is a missing story node.

## 6. Performance Profile

Measured on the `plans/epic-0049/` fixture (22 stories × ~5 tasks
per story in `execution-state.json`):

| Step | Median time | Dominated by |
| :--- | :--- | :--- |
| 1 | 4 ms | argument parsing |
| 2 | 32 ms | `Skill(x-internal-status-update)` cold start + `flock -s` + `jq` |
| 3 | 8 ms | `jq` iteration over tasks |
| 4 | 1 ms | arithmetic |
| 5 | 12 ms | 1× `stat` + per-task `date -f` |
| 6 | 22 ms | `jq -n` envelope assembly |
| **Total** | **~80 ms** | well under the 200 ms DoD budget |

The skill is CPU-bound on `jq`; the dominant factor is the cold-start
cost of the delegate `x-internal-status-update` invocation (~32 ms).
Inlining the `jq -c .stories[…]` read would shave ~20 ms but forfeit
the shared-lock contract, so the delegation is kept.

## 7. Failure Envelope Examples

Envelope for exit 1 (STATE_FILE_MISSING):

```text
execution-state.json not found
```

Envelope for exit 2 (STORY_NOT_IN_STATE):

```text
Story not in execution-state.json
```

Envelope for exit 64 (usage error):

```text
usage: x-internal-story-resume --story-id <id> --epic-id <id>
```

Envelope for exit 127 (dependency missing):

```text
dependency missing: jq
```

No JSON is written to stdout on any non-zero exit — callers
distinguish success from failure by exit code, not by parsing
stdout.

## 8. Why Delegate the State Read to x-internal-status-update?

Unlike `x-internal-story-load-context` (which reads `stories.<id>.status`
for dependency validation via a direct `jq` call), this skill
delegates its state read to the pilot skill. The reasons diverge:

| Axis | `x-internal-story-load-context` | `x-internal-story-resume` |
| :--- | :--- | :--- |
| Frequency of invocation | Once per story at Phase 0 entry | Once per story at Phase 0 resume-detection step |
| Concurrent writers expected? | No (Phase 0 runs before any `x-story-implement` or `x-task-implement` writes) | Yes (resume can be invoked while a parallel task is in-flight, writing `status=IN_PROGRESS`) |
| Data volume | Single `status` string | Full `tasks.*` sub-tree |
| Lock requirement | Best-effort (dependency check is idempotent) | Shared-lock REQUIRED (reading mid-write of `.tasks[…]` yields partial JSON) |

The second row is the decisive factor: a resume-detection read
concurrent with a task-status write can observe half-updated JSON
(e.g., `status` updated but `commitSha` not yet), which would
produce a DONE task with `commitSha=null` — then `lastCommitSha`
would be wrongly `null` on the returned envelope. Delegating to
`x-internal-status-update --read-only` pins the read behind its
shared lock and eliminates the race.

The tradeoff is a ~20 ms cold-start cost, well within the 200 ms DoD
budget.

## 9. Downstream Consumer Contract (story-0049-0019)

`story-0049-0019` will delete the ~120-line inline resume-detection
block from `x-story-implement` Phase 0 and replace it with a single
`Skill(x-internal-story-resume …)` invocation. The consumer contract
for that downstream refactor:

1. `resumePoint == "fresh-start"` → proceed to Phase 2 task-1 with a
   new branch.
2. `resumePoint == "all-done"` → skip Phase 2 entirely; proceed to
   Phase 3 verification with `lastCommitSha` as the anchor for the
   story branch HEAD.
3. `resumePoint == "phase-2-task-<N>"` → resume Phase 2 from the
   `<N>`-th task; the previous N-1 tasks are DONE and their branches
   are already merged (per `tasksCompleted`).
4. `staleWarnings` non-empty → emit an operator-visible WARN log line
   but do NOT block; the staleness heuristic is advisory (a docs-only
   tweak to the story file after a code task DONE is routine).
5. Exit 2 (`STORY_NOT_IN_STATE`) → initialise a new story node via
   `x-internal-status-update --initialize` and restart from (1).
6. Exit 1 (`STATE_FILE_MISSING`) → same as (5); the state file is
   created by the pilot skill's `--initialize` mode.

This contract is stable across the EPIC-0049 rollout and is pinned
here so the consumer story-0049-0019 does not need to rediscover it.

## 10. Rationale: Why "phase-2-task-<N>" Instead of Task ID?

An early draft returned the first-PENDING task's ID directly (e.g.,
`resumePoint="TASK-0049-0013-004"`). That was rejected because:

1. The caller (`x-story-implement` Phase 0 → Phase 2 wiring) uses
   `resumePoint` to decide WHICH orchestrator phase to enter. A
   task ID couples the envelope to the wave-dispatch algorithm; a
   phase marker (`phase-2-…`) keeps the consumer independent of
   task-implementation-map details.
2. The consumer cross-references `tasksPending[0]` for the actual
   task ID when it needs one — the envelope already carries it.
   Duplicating the ID in `resumePoint` would be redundant.
3. The three-valued vocabulary (`fresh-start` / `all-done` /
   `phase-2-task-<N>`) maps cleanly to the three orchestrator
   branches; a task-ID-valued variant would be one-of-thousands,
   forcing the consumer to parse it.

The 1-based index in `phase-2-task-<N>` is unambiguous: it is the
position of the first non-DONE task in task-order, NOT the numeric
suffix of the task ID. Gaps (TASK-001 DONE, TASK-002 skipped,
TASK-003 PENDING) produce `phase-2-task-2` — the second position
in the state file's iteration order — not `phase-2-task-3`.
