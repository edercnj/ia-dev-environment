---
name: x-internal-phase-gate
description: "Validates phase transitions for orchestrators under Rule 25. Four modes: --mode pre (assert predecessor phases completed before entering phase N), --mode post (assert all child tasks of phase N are completed AND all expected artifacts exist on disk), --mode wave (post-Batch-B verification of parallel wave completeness: N child TaskUpdate completed + N artifacts exist), --mode final (terminal gate composing with x-internal-epic-integrity-gate). Reads execution-state.json.taskTracking.phaseGateResults and TaskList task state; writes back the gate result. Emits a single-line JSON envelope {passed, mode, skill, phase, expectedTasks, completedTasks, missingTasks, expectedArtifacts, missingArtifacts, wallclockMs, timestamp}. Exit 0 on passed, 12 on failure, 13 on malformed args, 14 on task-resolution timeout. First skill in the x-internal-* convention authored by EPIC-0055; eighth overall (after status-update, report-write, args-normalize, story-load-context, story-build-plan, story-verify, story-resume, epic-build-plan, epic-integrity-gate, epic-branch-ensure, story-report) and the eighth under internal/plan/."
model: haiku
visibility: internal
user-invocable: false
allowed-tools: Read, Bash
argument-hint: "--mode pre|post|wave|final [--skill <name>] [--phase <Phase N>] [--parent-task-id <id>] [--expected-tasks id1,id2] [--expected-artifacts path1,path2] [--timeout-s <N>] [--emit-tracker true|false] [--state-file plans/epic-XXXX/execution-state.json]"
category: internal-plan
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL**
> Esta skill é invocada apenas por outras skills (orquestradores).
> NÃO é destinada a invocação direta pelo usuário.
> Callers principais: `x-epic-implement`, `x-story-implement`,
> `x-task-implement`, `x-release`, `x-epic-orchestrate`, `x-review`,
> `x-review-pr`, `x-pr-merge-train` (após retrofits em stories
> 0055-0003 a 0055-0010). Oitava skill no subdir `internal/plan/`
> (após `x-internal-story-load-context`, `x-internal-story-build-plan`,
> `x-internal-story-verify`, `x-internal-story-resume`,
> `x-internal-epic-build-plan`, `x-internal-epic-integrity-gate`,
> `x-internal-story-report`). Primeira skill introduzida por EPIC-0055.

# Skill: x-internal-phase-gate

## Purpose

Validates phase transitions in orchestrators governed by Rule 25. A phase gate is a synchronous, fail-fast check that runs around each numbered `## Phase N` section: a PRE gate ensures the predecessor phases are cleanly `completed`; a POST gate ensures every child task of the current phase is `completed` AND every artifact the phase was supposed to produce exists on disk. Rule 25 moves enforcement from "Stop hook notices afterwards" (Rule 24 defense-in-depth) to **sync gate before the phase is marked completed** — missing tasks or artifacts abort the lifecycle with exit 12.

Responsibilities (single):

1. Parse arguments against the 4-mode matrix (`pre` / `post` / `wave` / `final`).
2. For PRE mode: scan predecessor phases' `phaseGateResults` in `execution-state.json` and `TaskList` for any non-`completed` sibling task.
3. For POST / WAVE / FINAL mode: intersect `--expected-tasks` against `TaskList` looking for `completed` status on each, AND `stat`-check every path in `--expected-artifacts`.
4. Atomically append a `phaseGateResults[]` entry to `execution-state.json.taskTracking` via `x-internal-status-update` (delegated).
5. Emit a single-line JSON envelope on stdout.
6. Translate every failure class to the exit-code catalogue.

Non-responsibilities (explicit):

- Does NOT mutate task state (no `TaskUpdate(status: ...)`). The caller owns task state transitions.
- Does NOT run the PostToolUse hook. The hook (`verify-phase-gates.sh`, story-0055-0002) reads `phaseGateResults[]` this skill writes.
- Does NOT emit `TaskCreate` unless `--mode wave --emit-tracker true` is set (Rule 25 Invariant 6 exception — one tracker task to surface wave wall-clock).
- Does NOT run tests, build, or any project-level validation. `x-internal-story-verify` / `x-internal-epic-integrity-gate` own those — this gate composes with them via `--mode final`.

## Convention Anchors (x-internal-* — EPIC-0055)

| Aspect | Value | Rationale |
| :--- | :--- | :--- |
| Path | `internal/plan/x-internal-phase-gate/` | Read-and-compute carve-out, co-located with other story/epic gate skills. |
| Frontmatter `visibility` | `internal` | Generator filters from `/help` menu. |
| Frontmatter `user-invocable` | `false` | Declarative complement. |
| Frontmatter `model` | `haiku` | Zero-reasoning lookup (RULE-023 utility tier). |
| Body marker | `> 🔒 **INTERNAL SKILL**` | Rule 22 §Body marker. |
| Allowed tools | `Read, Bash` | Read for artifact stat; Bash for jq + state-file I/O via `x-internal-status-update`. |

## Triggers

Bare-slash form intentionally omitted — never invoked by a user. All invocations follow Rule 13 Pattern 1 (INLINE-SKILL):

```markdown
Skill(skill: "x-internal-phase-gate",
      args: "--mode pre --skill x-story-implement --phase Phase-1 --state-file plans/epic-0060/execution-state.json")
```

```markdown
Skill(skill: "x-internal-phase-gate",
      args: "--mode post --skill x-story-implement --phase Phase-1 --expected-tasks 101,102,103,104,105,106 --expected-artifacts plans/epic-0060/plans/arch-story-0060-0001.md,plans/epic-0060/plans/plan-story-0060-0001.md --state-file plans/epic-0060/execution-state.json")
```

```markdown
Skill(skill: "x-internal-phase-gate",
      args: "--mode wave --skill x-review --phase Phase-2 --expected-tasks 201,202,203,204,205,206,207,208,209 --state-file plans/epic-0060/execution-state.json")
```

## Parameters

| Flag | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--mode <tier>` | M | — | One of `pre`, `post`, `wave`, `final`. Case-insensitive; normalized to lowercase. |
| `--skill <name>` | M | — | Calling orchestrator name (recorded in the envelope and state file). |
| `--phase <Phase N>` | M | — | Exact phase label (matches `## Phase N — Name` header). |
| `--parent-task-id <id>` | O | — | Integer ID of the parent `TaskCreate` that owns the phase. Used to discover children via `TaskList`. |
| `--expected-tasks <ids>` | O | — | Comma-separated integer task IDs the gate must see as `completed`. Required for `post`, `wave`, `final`. |
| `--expected-artifacts <paths>` | O | — | Comma-separated relative paths that MUST exist on disk. Required for `post` and `final`. Optional for `wave`. |
| `--timeout-s <N>` | O | `10` | Integer seconds to retry-poll if any `--expected-tasks` is still `in_progress`. Exceeded → exit 14. |
| `--emit-tracker <bool>` | O | `false` | Only legal with `--mode wave`. Emits a single tracker `TaskCreate` with `subject` = `<skill> › <phase> › wave-tracker` for CLI visibility. |
| `--state-file <path>` | O | auto | Path to `execution-state.json`. Auto-derived from `$CWD/plans/epic-XXXX/execution-state.json` when epic is unambiguous. |

## Response Contract

On success (exit 0) and on failure (exit 12) the skill writes a single-line JSON object to stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `passed` | `Boolean` | yes | `true` when all checks passed. |
| `mode` | `String` | yes | Echo of `--mode`. |
| `skill` | `String` | yes | Echo of `--skill`. |
| `phase` | `String` | yes | Echo of `--phase`. |
| `expectedTasks` | `Array<Integer>` | yes | Echo of `--expected-tasks` (`[]` if absent). |
| `completedTasks` | `Array<Integer>` | yes | Subset of `expectedTasks` confirmed `completed` in `TaskList`. |
| `missingTasks` | `Array<Integer>` | yes | `expectedTasks \ completedTasks`. Empty when `passed=true`. |
| `expectedArtifacts` | `Array<String>` | yes | Echo of `--expected-artifacts`. |
| `missingArtifacts` | `Array<String>` | yes | Subset of `expectedArtifacts` that do NOT exist on disk. Empty when `passed=true`. |
| `wallclockMs` | `Integer` | yes | Gate execution time in milliseconds (monotonic clock). |
| `timestamp` | `String` (ISO-8601) | yes | Gate completion timestamp in UTC. |

The envelope shape is authoritative. `phaseGateResults[]` in `execution-state.json` receives a slim projection: `{phase, mode, passed, missingArtifacts, missingTasks, timestamp}`.

## Exit Codes

| Code | Name | Condition |
| :--- | :--- | :--- |
| 0 | `OK` | `passed=true` |
| 12 | `PHASE_GATE_FAILED` | `passed=false` — at least one missing task or missing artifact |
| 13 | `PHASE_GATE_MALFORMED` | Unknown flag, missing required flag, `--emit-tracker true` without `--mode wave`, `--expected-tasks` on `--mode pre`, etc. |
| 14 | `PHASE_GATE_TIMEOUT` | An expected task is still `in_progress` after `--timeout-s` seconds of polling |
| 64 | `EX_USAGE` | `--mode` value not in {pre, post, wave, final} or `--phase` fails Rule-25 `subject` regex |

No partial-success state: the first non-recoverable error aborts the run and yields a non-zero exit.

## Workflow

### Step 1 — Arg parse + validation

Parse arguments with the shared Rule-14 `while (($#))` loop. Normalize `--mode` to lowercase. Validate:

- `--mode ∈ {pre, post, wave, final}` — else exit 64.
- `--phase` matches Rule-25 subject regex — else exit 64.
- For `--mode post|wave|final`: `--expected-tasks` MUST be present.
- For `--mode post|final`: `--expected-artifacts` MUST be present.
- For `--mode pre`: `--expected-tasks` / `--expected-artifacts` MUST be absent (checks come from state file only) — else exit 13.
- `--emit-tracker true` requires `--mode wave` — else exit 13.

### Step 2 — Resolve `--state-file`

When `--state-file` absent, search upward from `$PWD` for `plans/epic-*/execution-state.json`. Exactly one match → proceed. Zero or multiple → exit 13 with `STATE_FILE_AMBIGUOUS`.

### Step 3 — Short-circuit on `taskTracking.enabled=false`

Read `taskTracking.enabled` from the state file. Default is `true` when the field is absent (Rule 19 fallback). Only when the value is **explicitly** `false`, emit:

```json
{"passed":true,"mode":"<mode>","skill":"<skill>","phase":"<phase>","expectedTasks":[],"completedTasks":[],"missingTasks":[],"expectedArtifacts":[],"missingArtifacts":[],"wallclockMs":<N>,"timestamp":"<t>","note":"taskTracking disabled (legacy mode)"}
```

and exit 0. This is the Rule-19 backward-compat path — legacy epics treat gates as no-ops.

### Step 4 — Mode-specific validation

#### 4a. `--mode pre`

Read `execution-state.json.taskTracking.phaseGateResults[]`. Let `N = int(phase.replace("Phase-",""))`. For every `k ∈ [0, N-1]`, at least one entry with `phase="Phase-k"` AND `mode="post"` AND `passed=true` MUST be present. Missing entries → `missingTasks=[<Phase-k-placeholder>]`, `passed=false`, exit 12.

No artifact checks in PRE mode.

#### 4b. `--mode post`

- **Tasks:** For each `id ∈ --expected-tasks`, retrieve its status. Use `TaskList` via Bash (e.g., reading from `~/.claude/state/tasks/<session>/tasks.json` when available, falling back to `execution-state.json.taskTracking.taskIndex[]`). `completed` → add to `completedTasks`; else add to `missingTasks`. If any task is `in_progress`, poll every 500ms up to `--timeout-s`; persistent → exit 14.
- **Artifacts:** For each `path ∈ --expected-artifacts`, `[[ -e "$path" ]]` — present → drop; absent → append to `missingArtifacts`.
- `passed = missingTasks.isEmpty() && missingArtifacts.isEmpty()`.

#### 4c. `--mode wave`

Same as POST, but:
- Semantics: ALL wave members must be `completed` simultaneously (no ordering assumption).
- `--expected-artifacts` is optional — waves that only produce task commits (no files) are valid.
- When `--emit-tracker true`, emit exactly one `TaskCreate(subject: "<skill> › <phase> › wave-tracker")` upfront and `TaskUpdate(status: "completed")` at the end, regardless of result.

#### 4d. `--mode final`

Superset of POST. Additionally scans the Rule-24 mandatory artifact set (when `--skill = x-story-implement`): verify-envelope, review-story, techlead-review, story-completion-report. Acts as the synchronous Rule-24 gate the Stop-hook normally enforces asynchronously.

### Step 5 — Write `phaseGateResults[]` entry

Atomically append to `execution-state.json.taskTracking.phaseGateResults[]` via:

```markdown
Skill(skill: "x-internal-status-update",
      args: "--file <state-file> --type phase-gate --phase <phase> --mode <mode> --passed <true|false> --missing-artifacts <comma-list> --missing-tasks <comma-list>")
```

The status-update skill owns the flock-protected read-modify-write.

### Step 6 — Emit envelope

Single-line JSON via `jq -nc` with all fields. Exit 0 on success, 12 on failure.

## Idempotency Contract

The skill is idempotent by design:

- Re-invoking the same gate (same `--mode`, `--skill`, `--phase`) is a replay: it re-reads task state + artifact state, writes a fresh `phaseGateResults[]` entry, and returns the current result. Previous entries remain for audit history.
- Concurrent invocations serialize on `x-internal-status-update`'s flock (file-level).

## Performance Contract

| Mode | Budget |
| :--- | :--- |
| `pre` | < 50ms (single file read + jq projection) |
| `post` | < 100ms + artifact stat × N (usually < 150ms total) |
| `wave` | same as post |
| `final` | < 200ms (adds Rule-24 scan) |

Target global overhead across an epic with ~30 gates: < 5 seconds, i.e., well below the EPIC-0055 DoD threshold of "< 2% wall-clock overhead".

## Examples

### Example 1 — Happy path, PRE mode

```bash
Skill(skill: "x-internal-phase-gate",
      args: "--mode pre --skill x-story-implement --phase Phase-2 --state-file plans/epic-0060/execution-state.json")
```

Output:
```json
{"passed":true,"mode":"pre","skill":"x-story-implement","phase":"Phase-2","expectedTasks":[],"completedTasks":[],"missingTasks":[],"expectedArtifacts":[],"missingArtifacts":[],"wallclockMs":12,"timestamp":"2026-04-24T10:30:00Z"}
```
Exit: 0.

### Example 2 — POST mode all green

```bash
Skill(skill: "x-internal-phase-gate",
      args: "--mode post --skill x-story-implement --phase Phase-1 --expected-tasks 101,102,103,104,105,106 --expected-artifacts plans/epic-0060/plans/arch-story-0060-0001.md,plans/epic-0060/plans/plan-story-0060-0001.md")
```

Output:
```json
{"passed":true,"mode":"post","skill":"x-story-implement","phase":"Phase-1","expectedTasks":[101,102,103,104,105,106],"completedTasks":[101,102,103,104,105,106],"missingTasks":[],"expectedArtifacts":["plans/epic-0060/plans/arch-story-0060-0001.md","plans/epic-0060/plans/plan-story-0060-0001.md"],"missingArtifacts":[],"wallclockMs":47,"timestamp":"2026-04-24T10:35:00Z"}
```
Exit: 0.

### Example 3 — POST mode, missing artifacts

```bash
Skill(skill: "x-internal-phase-gate",
      args: "--mode post --skill x-story-implement --phase Phase-1 --expected-tasks 101,102 --expected-artifacts plans/epic-0060/plans/arch-story-0060-0001.md,plans/epic-0060/plans/missing.md")
```

Output:
```json
{"passed":false,"mode":"post","skill":"x-story-implement","phase":"Phase-1","expectedTasks":[101,102],"completedTasks":[101,102],"missingTasks":[],"expectedArtifacts":["plans/epic-0060/plans/arch-story-0060-0001.md","plans/epic-0060/plans/missing.md"],"missingArtifacts":["plans/epic-0060/plans/missing.md"],"wallclockMs":38,"timestamp":"2026-04-24T10:40:00Z"}
```
Exit: 12.

### Example 4 — WAVE mode, one member still running

With `--timeout-s 2` and task 203 in `in_progress`:

```bash
Skill(skill: "x-internal-phase-gate",
      args: "--mode wave --skill x-review --phase Phase-2 --expected-tasks 201,202,203,204,205,206,207,208,209 --timeout-s 2")
```

Stderr:
```
PHASE_GATE_TIMEOUT — task 203 still in_progress after 2s
```
Exit: 14.

### Example 5 — Malformed: wave without expected-tasks

```bash
Skill(skill: "x-internal-phase-gate",
      args: "--mode wave --skill x-review --phase Phase-2")
```

Stderr:
```
PHASE_GATE_MALFORMED — --mode wave requires --expected-tasks
```
Exit: 13.

### Example 6 — Legacy short-circuit

When `execution-state.json.taskTracking.enabled = false`:

```bash
Skill(skill: "x-internal-phase-gate",
      args: "--mode post --skill x-story-implement --phase Phase-1 --expected-tasks 101 --expected-artifacts foo.md")
```

Output:
```json
{"passed":true,"mode":"post","skill":"x-story-implement","phase":"Phase-1","expectedTasks":[],"completedTasks":[],"missingTasks":[],"expectedArtifacts":[],"missingArtifacts":[],"wallclockMs":3,"timestamp":"2026-04-24T10:45:00Z","note":"taskTracking disabled (legacy mode)"}
```
Exit: 0.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| `--mode` value invalid | `PHASE_GATE_MALFORMED`; exit 13 |
| `--phase` fails regex | Exit 64 |
| Required flag missing per mode | `PHASE_GATE_MALFORMED`; exit 13 |
| State file absent AND no `--state-file` override | `STATE_FILE_AMBIGUOUS`; exit 13 |
| `taskTracking.enabled=false` | Short-circuit; `passed=true`; exit 0 |
| Expected task `in_progress` beyond timeout | `PHASE_GATE_TIMEOUT`; exit 14 |
| At least one expected task missing / non-completed | `PHASE_GATE_FAILED`; exit 12 |
| At least one expected artifact absent | `PHASE_GATE_FAILED`; exit 12 |
| `x-internal-status-update` delegation fails | Propagate stderr; exit 13 with `STATE_UPDATE_FAILED` |

## Generator Filter Contract

The `ia-dev-env` generator excludes skills with `visibility: internal` from:

1. `.claude/README.md` inventory table.
2. `/help` menu.
3. User-facing autocomplete.

Still copied to `.claude/skills/` (flat layout) so other skills can invoke via `Skill(...)`.

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers — the calling orchestrator owns telemetry wrapping. Passive hooks capture `tool.call` events for the underlying `Bash`/`Read` invocations.

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-internal-status-update` | delegate | Atomic flock-protected write of `phaseGateResults[]` entry. |
| `x-epic-implement`, `x-story-implement`, `x-task-implement`, `x-release`, `x-epic-orchestrate`, `x-review`, `x-review-pr`, `x-pr-merge-train` | callers | Retrofit in stories 0055-0003 → 0055-0010. |
| `x-internal-epic-integrity-gate` | composition | `--mode final` of `x-epic-implement` Phase 4 composes with integrity gate (gate runs first; integrity-gate is subsequent). |
| `x-internal-story-verify` | composition | `--mode final` of `x-story-implement` Phase 3 composes with story-verify (verify runs first; gate confirms its outputs). |
| `.claude/hooks/verify-phase-gates.sh` | consumer (Stop hook) | Reads `phaseGateResults[]` this skill writes; emits WARNING + exit 2 on gate failure at end of LLM turn. |
| `.claude/hooks/enforce-phase-sequence.sh` | consumer (PreToolUse hook) | Reads `phaseGateResults[]` to block `Skill(...)` of an orchestrator whose predecessor phase has no `passed=true` entry. |

## Rule References

- Rule 25 — the specification this skill enforces.
- Rule 22 — Skill Visibility convention (`x-internal-*`, visibility: internal).
- Rule 23 — Model Selection (haiku tier for utility skills).
- Rule 13 — Skill Invocation Protocol (INLINE-SKILL pattern).
- Rule 19 — Backward Compatibility (legacy short-circuit behavior).
- Rule 24 — Execution Integrity (composition via `--mode final`).

## Full Protocol

Per-mode state-file schema, full flock + `x-internal-status-update` interaction pseudocode, polling algorithm for `in_progress` tasks, and the `phaseGateResults[]` append-only invariant live in [`references/full-protocol.md`](references/full-protocol.md).
