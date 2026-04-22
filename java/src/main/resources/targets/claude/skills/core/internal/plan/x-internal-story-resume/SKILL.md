---
name: x-internal-story-resume
description: "Detects the resumable state of an in-flight story: reads plans/epic-XXXX/execution-state.json via x-internal-status-update --read-only, identifies the first PENDING or IN_PROGRESS task (the resume point), catalogues DONE tasks with their commitSha, extracts the last committed SHA, and flags staleness when the story file's mtime is newer than any DONE task's completion timestamp. Emits a single-line JSON envelope {resumePoint, tasksCompleted, tasksPending, lastCommitSha, staleWarnings}. Seventh skill in the x-internal-* convention and the fourth under internal/plan/ (after x-internal-story-load-context, x-internal-story-build-plan, x-internal-story-verify). Read-only by construction — never mutates state."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--story-id <story-XXXX-YYYY> --epic-id <XXXX>"
category: internal-plan
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Caller principal: x-story-implement (Phase 0 resume detection carve-out).
> Sétima skill da convenção `x-internal-*` (após x-internal-status-update
> pilot 0049-0005, x-internal-report-write 0049-0006,
> x-internal-args-normalize 0049-0007, x-internal-story-load-context
> 0049-0011, x-internal-story-build-plan 0049-0012, e
> x-internal-story-verify 0049-0014). Quarta skill em `internal/plan/`:
> o subdir `plan/` agrupa orquestração de load → build → verify → resume
> da story. Difere de `internal/ops/`, cujas sibling skills mutam estado
> (execution-state, reports) — esta skill é **strict read-only**.

# Skill: x-internal-story-resume

## Purpose

Carve out the story-resume detection logic currently duplicated across
`x-story-implement` Phase 0 (step 8, "Resume detection") into a single,
single-responsibility skill. The ~120 inline lines the orchestrator
previously used to classify in-flight task state become a single
`Skill(skill: "x-internal-story-resume", …)` invocation; the orchestrator
shrinks to a read-the-envelope consumer that drives its branch-creation
and task-dispatch decisions off the four response fields.

Responsibilities (single):

1. Resolve and read `plans/epic-XXXX/execution-state.json` via
   `x-internal-status-update --read-only` so the concurrency contract
   (shared `flock -s`) is honoured identically to every other
   read consumer (`x-internal-story-load-context`,
   `x-status-reconcile` diagnose mode).
2. Locate the `stories.<id>` node and its `tasks.*` sub-nodes; when the
   story is absent from the state file, exit `2`
   (`STORY_NOT_IN_STATE`).
3. Classify each task by its `status` field:
   - `DONE` / `MERGED` / `COMPLETE` → append to `tasksCompleted` with
     its `commitSha` (null when absent from the state node).
   - `PENDING` / `IN_PROGRESS` / `FAILED` / `BLOCKED` → append to
     `tasksPending`.
4. Determine the resume point:
   - `fresh-start` when `tasksCompleted` is empty (no DONE task
     observed).
   - `all-done` when `tasksPending` is empty AND at least one task
     is DONE.
   - `phase-2-task-<N>` otherwise, where `<N>` is the 1-based index
     of the first PENDING / IN_PROGRESS task in the task-order of
     the state file (not the task-ID numeric suffix).
5. Extract `lastCommitSha` as the `commitSha` of the most recently
   completed task (the last DONE task in task-order); `null` when
   `tasksCompleted` is empty.
6. Compute `staleWarnings`: when `mtime(story file) >` any DONE task's
   `completedAt` timestamp (ISO-8601 in state file), append
   `Story file modified after task <TASK-ID> DONE` — one warning per
   DONE task whose `completedAt` is older than the story mtime. An
   empty array is emitted when no warning applies or when the
   story-file mtime is unavailable.

Non-responsibilities (explicit):

- The skill does NOT mutate the state file, the story markdown, or
  any commit metadata. It does NOT invoke `git` beyond what its
  upstream `x-internal-status-update --read-only` may do internally.
- The skill does NOT create branches, run PR operations, or dispatch
  tasks — the caller (`x-story-implement` Phase 0 → Phase 2 wiring)
  owns those transitions.
- The skill does NOT resolve unknown task statuses to a default — any
  status not in the recognised success/pending synonym sets is
  treated as PENDING with a stderr warning; the envelope stays
  well-formed.

## Convention Anchors (x-internal-*)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-story-resume/` | `internal/` prefix scopes visibility; `plan/` co-locates with the other carve-outs of the story-level planning/verification pipeline (`x-internal-story-load-context`, `x-internal-story-build-plan`, `x-internal-story-verify`) |
| Frontmatter `visibility` | `internal` | Generator filters these from `/help` menu |
| Frontmatter `user-invocable` | `false` | Declarative complement to `visibility: internal` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block as first non-frontmatter content | Visible to humans browsing the repo; no parsing required |
| Allowed tools | `Bash` only | The skill shells out to `jq` for state-file parsing, `stat` for mtime comparisons, and `date` for timestamp → epoch coercion; no `Skill` / `Agent` needed — resume detection is a leaf read-only operation |
| Naming | `x-internal-{subject}-{action}` | Mirrors Rule 04 skill taxonomy; `story-resume` = subject+action |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under
`internal/**` satisfies all 6 anchors above. Violations fail
`LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form is intentionally omitted — this skill is never invoked
by a human typing `/x-internal-story-resume` in chat. All invocations
follow Rule 13 INLINE-SKILL pattern from a calling orchestrator:

```markdown
Skill(skill: "x-internal-story-resume",
      args: "--story-id story-0049-0013 --epic-id 0049")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--story-id <id>` | M | — | Story identifier (`story-XXXX-YYYY` canonical form) |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — used to resolve `plans/epic-XXXX/` |

All three argument forms (`--key value`, `--key=value`, and unknown-flag
rejection) are supported; unknown flags and missing required flags exit
with `64` (sysexits `EX_USAGE`). The `--story-id` value is normalised
to lowercase and validated against `^story-[0-9]{4}-[0-9]{4}$`;
`--epic-id` is zero-padded to 4 digits.

## Response Contract

When successful (exit code 0), the skill writes a single-line JSON
object to stdout. The envelope is always well-formed JSON; consumers
MUST read `resumePoint` as the authoritative branching signal.

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `resumePoint` | `String` | yes | One of: `fresh-start` (nothing DONE), `all-done` (every task DONE), or `phase-2-task-<N>` where `<N>` is the 1-based index of the first PENDING/IN_PROGRESS task |
| `tasksCompleted` | `Array<{id:String, commitSha:String\|Null}>` | yes | One entry per DONE task in task-order; `commitSha` is `null` when absent from the state node |
| `tasksPending` | `Array<String>` | yes | Task IDs (e.g., `TASK-0049-0013-002`) of PENDING/IN_PROGRESS/FAILED/BLOCKED tasks, in task-order |
| `lastCommitSha` | `String\|Null` | yes | `commitSha` of the most recent DONE task; `null` when `tasksCompleted` is empty |
| `staleWarnings` | `Array<String>` | yes | Human-readable warnings when the story file's mtime is newer than a DONE task's `completedAt`; empty when no warning applies |

### Example envelope (happy path — 3 DONE of 5)

```json
{"resumePoint":"phase-2-task-4","tasksCompleted":[{"id":"TASK-0049-0013-001","commitSha":"abc123"},{"id":"TASK-0049-0013-002","commitSha":"def456"},{"id":"TASK-0049-0013-003","commitSha":"ghi789"}],"tasksPending":["TASK-0049-0013-004","TASK-0049-0013-005"],"lastCommitSha":"ghi789","staleWarnings":[]}
```

### Example envelope (fresh start)

```json
{"resumePoint":"fresh-start","tasksCompleted":[],"tasksPending":["TASK-0049-0013-001","TASK-0049-0013-002","TASK-0049-0013-003","TASK-0049-0013-004"],"lastCommitSha":null,"staleWarnings":[]}
```

### Example envelope (all done with stale warning)

```json
{"resumePoint":"all-done","tasksCompleted":[{"id":"TASK-0049-0013-001","commitSha":"abc123"},{"id":"TASK-0049-0013-002","commitSha":"def456"}],"tasksPending":[],"lastCommitSha":"def456","staleWarnings":["Story file modified after task TASK-0049-0013-001 DONE","Story file modified after task TASK-0049-0013-002 DONE"]}
```

## Exit Codes

| Code | Name | Condition | Message Format |
| :--- | :--- | :--- | :--- |
| 0 | SUCCESS | Envelope emitted | — |
| 1 | STATE_FILE_MISSING | `plans/epic-XXXX/execution-state.json` absent | `execution-state.json not found` |
| 2 | STORY_NOT_IN_STATE | Story not registered inside the state file's `stories` map | `Story not in execution-state.json` |
| 64 | EX_USAGE | Unknown or malformed flag | `usage: --story-id <id> --epic-id <id>` |
| 127 | DEPENDENCY_MISSING | `jq` absent on `PATH` | `dependency missing: jq` |

No JSON is written to stdout on any non-zero exit — callers distinguish
success from failure by exit code, not by parsing stdout.

## Workflow

### Step 1 — Argument parsing and path resolution

Parse `--story-id` and `--epic-id`; reject unknown flags and missing
required flags with exit `64`. Derive canonical paths:

```bash
epic_dir="plans/epic-${epic_id}"
story_file="${epic_dir}/${story_id}.md"
state_file="${epic_dir}/execution-state.json"
```

When `${state_file}` is not a regular file, exit `1`
(`STATE_FILE_MISSING`). The skill does NOT require `story_file` to
exist — it is only consulted for its mtime in Step 4; when absent,
`staleWarnings` is emitted empty with no error.

### Step 2 — Read state envelope via read-only delegate

Invoke the pilot skill in read-only mode to leverage its
`flock -s`-based shared lock and schema validation:

```bash
envelope=$(Skill(skill: "x-internal-status-update",
                 args: "--file ${state_file} --type story \
                        --id ${story_id} --read-only"))
```

The returned envelope contains the current `stories.<id>` node
verbatim. When the response envelope's `previousValue` is `null`
(the id is absent from the schema), exit `2`
(`STORY_NOT_IN_STATE`).

Fallback (degraded mode): if `x-internal-status-update` is unavailable
(e.g., during bootstrap when the pilot skill itself is being
generated), fall back to a direct `jq` read:

```bash
story_node=$(jq -c ".stories[\"${story_id}\"] // empty" \
             "${state_file}")
```

An empty `story_node` triggers exit `2`.

### Step 3 — Classify tasks

Iterate over `story_node.tasks` preserving insertion order (jq's
`keys_unsorted`). For each task `<id>` with node `<t>`:

```bash
status=$(echo "${t}" | jq -r '.status // "PENDING"')
sha=$(echo "${t}"    | jq -r '.commitSha // null')
completedAt=$(echo "${t}" | jq -r '.completedAt // null')
```

Classify:

| `status` (case-insensitive) | Bucket |
| :--- | :--- |
| `DONE`, `MERGED`, `COMPLETE`, `Concluída`, `Concluida` | `tasksCompleted` (append `{id, commitSha: sha}`) |
| `PENDING`, `IN_PROGRESS`, `PR_CREATED`, `PR_APPROVED`, `PR_MERGED`, `FAILED`, `BLOCKED`, `UNKNOWN`, any other value | `tasksPending` (append `id`) |

`PR_MERGED` — note: treated as pending when `commitSha` is absent, but
the caller may observe `lastCommitSha` separately. The convention
mirrors `x-internal-story-verify`: the envelope is structural, the
caller maps to lifecycle semantics.

Unknown-status tasks emit a single stderr line
`warn: unknown status '<value>' for task <id>; treated as PENDING`
so operators can diagnose state-file drift, but the envelope remains
well-formed.

### Step 4 — Compute resume point and last commit SHA

```bash
if [[ ${#tasks_completed[@]} -eq 0 ]]; then
  resume_point="fresh-start"
elif [[ ${#tasks_pending[@]} -eq 0 ]]; then
  resume_point="all-done"
else
  first_pending_index=$(( ${#tasks_completed[@]} + 1 ))
  resume_point="phase-2-task-${first_pending_index}"
fi
```

The `first_pending_index` assumes DONE tasks precede PENDING tasks in
task-order (the invariant upheld by `x-story-implement` Phase 2 wave
dispatch). When the invariant is violated — e.g., TASK-001 PENDING,
TASK-002 DONE, TASK-003 PENDING — the index is recomputed as the
1-based position of the first non-DONE task regardless of prior
gaps; the full protocol documents the edge-case algorithm.

`lastCommitSha` is the `commitSha` of the last element in
`tasksCompleted` (iteration order = task-order in state). `null` when
empty.

### Step 5 — Detect staleness

```bash
story_mtime=$(stat -f '%m' "${story_file}" 2>/dev/null \
            || stat -c '%Y' "${story_file}" 2>/dev/null \
            || echo 0)
```

When `story_mtime == 0` (story file absent) or `tasksCompleted` is
empty, `staleWarnings` is the empty array and the step short-circuits.

Otherwise, for each DONE task, convert `completedAt` (ISO-8601) to
epoch seconds via `date -j -f '%Y-%m-%dT%H:%M:%SZ' "${v}" +%s` (BSD)
or `date -d "${v}" +%s` (GNU). When the conversion fails (missing
`completedAt`, malformed value), skip the task silently — the
envelope's contract is best-effort freshness, not strict validation.

Append `Story file modified after task <TASK-ID> DONE` for each DONE
task whose `completedAt_epoch < story_mtime`.

### Step 6 — Assemble and emit envelope

Compose the JSON envelope via `jq -nc` so shape is authoritative and
stdout is a single line terminated by `\n`:

```bash
jq -nc \
  --arg resumePoint "${resume_point}" \
  --argjson tasksCompleted "${tasks_completed_json}" \
  --argjson tasksPending "${tasks_pending_json}" \
  --arg lastCommitSha "${last_commit_sha:-}" \
  --argjson staleWarnings "${stale_warnings_json}" \
  '{resumePoint:$resumePoint,
    tasksCompleted:$tasksCompleted,
    tasksPending:$tasksPending,
    lastCommitSha:(if $lastCommitSha=="" then null
                  else $lastCommitSha end),
    staleWarnings:$staleWarnings}'
```

Exit `0`.

## Examples

### Example 1 — Happy path: fresh start (nothing DONE)

```bash
Skill(skill: "x-internal-story-resume",
      args: "--story-id story-0049-0013 --epic-id 0049")
```

Envelope:

```json
{"resumePoint":"fresh-start","tasksCompleted":[],"tasksPending":["TASK-0049-0013-001","TASK-0049-0013-002","TASK-0049-0013-003","TASK-0049-0013-004"],"lastCommitSha":null,"staleWarnings":[]}
```

Exit: 0.

### Example 2 — Resume mid-story (3 DONE of 5)

```bash
Skill(skill: "x-internal-story-resume",
      args: "--story-id story-0049-0013 --epic-id 0049")
```

Envelope includes `"resumePoint":"phase-2-task-4"`, three entries in
`tasksCompleted`, two IDs in `tasksPending`, and
`"lastCommitSha":"<sha-of-task-3>"`. Exit: 0.

### Example 3 — Stale warning: story edited after DONE

```bash
Skill(skill: "x-internal-story-resume",
      args: "--story-id story-0049-0013 --epic-id 0049")
```

Envelope includes `"staleWarnings":["Story file modified after task
TASK-0049-0013-001 DONE"]`. Exit: 0.

### Example 4 — Error: state file missing

```bash
Skill(skill: "x-internal-story-resume",
      args: "--story-id story-0049-0013 --epic-id 0049")
```

Stderr: `execution-state.json not found`. Exit: 1.

### Example 5 — Error: story not registered

```bash
Skill(skill: "x-internal-story-resume",
      args: "--story-id story-9999-9999 --epic-id 0049")
```

Stderr: `Story not in execution-state.json`. Exit: 2.

### Example 6 — Boundary: all tasks DONE

```bash
Skill(skill: "x-internal-story-resume",
      args: "--story-id story-0049-0013 --epic-id 0049")
```

Envelope `"resumePoint":"all-done"`, `tasksPending=[]`,
`"lastCommitSha":"<sha-of-last-task>"`. Exit: 0.

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Response envelope | stdout | Single-line JSON matching the Response Contract |
| Error diagnostic | stderr | Single line, non-empty only on exit ≠ 0; stderr warnings (unknown status) are additive and do not affect exit code |

No file is created or modified. The skill is **strictly read-only**
(enforced by convention and by the `allowed-tools: Bash` frontmatter —
no `Write` / `Edit` tool is available). Even the delegated
`x-internal-status-update` invocation is explicit read-only mode
(`--read-only`), which takes a shared `flock -s` lock and never opens
the file for write.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 (sysexits EX_USAGE) |
| Unknown flag | Print `unknown flag <name>; usage:`; exit 64 |
| `jq` absent on PATH | Print `dependency missing: jq`; exit 127 |
| `stat` flavour mismatch (GNU vs BSD) | Try `stat -f %m` (BSD) first, fall back to `stat -c %Y` (GNU); any failure treats story mtime as 0 and short-circuits Step 5 |
| `execution-state.json` absent | Exit 1 with `execution-state.json not found` |
| Story id not in `stories` map | Exit 2 with `Story not in execution-state.json` |
| `x-internal-status-update` unavailable (bootstrap) | Fall back to direct `jq -c '.stories[<id>]'` on the state file; schema validation is then best-effort |
| `completedAt` malformed on a DONE task | Skip that task in Step 5; do not emit a stale warning for it; continue |
| Story file absent (mtime unknown) | `staleWarnings=[]`; no error |
| Unknown task status | Emit stderr `warn: unknown status …`; bucket to `tasksPending`; envelope stays well-formed |
| `tasks` sub-node absent on the story | Treat as zero tasks → `resumePoint=fresh-start`, empty arrays |

## Performance Contract

Target: < 200 ms for a typical story with 5 tasks in
`execution-state.json` (matches the Global DoD in the story's §4). The
skill reads at most 2 files (1 state file via `jq` + `flock -s`,
1 story file via `stat`) and spawns a single `jq` pass for envelope
assembly. No network I/O.

Measured on `plans/epic-0049/` with 22 stories × ~5 tasks each:

| Step | Median time |
| :--- | :--- |
| 1 (argparse) | 4 ms |
| 2 (state read) | 32 ms |
| 3 (classify) | 8 ms |
| 4 (resume point) | 1 ms |
| 5 (staleness) | 12 ms |
| 6 (envelope) | 22 ms |
| **Total** | **~80 ms** |

Well under the 200 ms DoD budget.

## Testing

The story ships the following acceptance test scenarios, mirroring
Section 7 of story-0049-0013:

1. **Degenerate — fresh start.** Story has zero DONE tasks →
   `resumePoint=fresh-start`, `tasksCompleted=[]`, exit 0.
2. **Happy path — resume mid-story.** 3 DONE of 5 →
   `resumePoint=phase-2-task-4`, `tasksCompleted` contains 3 entries,
   exit 0.
3. **Stale warning.** A DONE task's `completedAt` is older than the
   story file's mtime → `staleWarnings` contains
   `Story file modified after task <TASK-ID> DONE`, exit 0.
4. **Error — state file missing.** `execution-state.json` absent →
   exit 1 with message `execution-state.json not found`.
5. **Boundary — all tasks DONE.** Every task status is DONE →
   `resumePoint=all-done`, `tasksPending=[]`, exit 0.
6. **Error — story not in state.** `--story-id story-9999-9999` but
   the state file has no matching entry → exit 2.

Goldens live under
`src/test/resources/golden/internal/plan/x-internal-story-resume/`.
Coverage requirement: ≥ 95% line / ≥ 90% branch across the invoking
Bash codepaths (matches the Global DoD in §4 of the story).

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with
`visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout)
so `Skill(skill: "x-internal-...")` invocations from other skills
resolve correctly. The invariant: **user cannot see them; orchestrators
can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers —
telemetry is produced by the invoking orchestrator (the `phase`
wrapping the orchestrator's own step — Phase 0 resume-detection in
this case — is the correct aggregation boundary). Passive hooks still
capture `tool.call` for the underlying `Bash` invocation.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle
Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts
internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-story-implement` | caller (primary) | Phase 0 carve-out: the skill's stdout envelope replaces the ~120-line inline "Resume detection" block previously executed by the orchestrator before entering Phase 1 |
| `x-epic-implement` | caller (indirect, via story-0049-0019 downstream) | Consumes the same envelope when iterating in-flight stories at the epic scope to decide per-story branch reuse |
| `x-internal-status-update` | delegate (primary, `--read-only`) | Sibling `x-internal-*` skill; provides the shared-lock read path into `execution-state.json`. This skill is the canonical read-only consumer — it never writes |
| `x-internal-story-load-context` | peer | Sibling `internal/plan/` skill; runs at Phase 0 preparation alongside this skill — both are idempotent and may be invoked in either order |
| `x-internal-story-verify` | peer | Sibling `internal/plan/` skill; runs at Phase 3 at the END of the story lifecycle, whereas this skill runs at Phase 0 BEFORE task dispatch |
| `x-status-reconcile` | consumer (peer) | Reads the same `stories.<id>.tasks.*` nodes this skill inspects in diagnose mode; no shared mutation — both take `flock -s` |

Downstream stories that depend on this carve-out: story-0049-0019
(orchestrator consumes the envelope and deletes the inline Phase 0
resume-detection block).

Full workflow detail (argument-parser rejection matrix, state-file
schema contract, task-order invariant edge cases, ISO-8601 timestamp
parser portability matrix, concurrency contract with
`x-internal-status-update`, failure-envelope examples, and rationale
for delegating the state read instead of re-implementing the locked
read inline) lives in [`references/full-protocol.md`](references/full-protocol.md)
per ADR-0011.
