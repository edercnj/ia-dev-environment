# x-internal-phase-gate — Full Protocol

Supplementary reference for the SKILL.md contract. Covers state-file schema, polling algorithm, atomic write sequence, and mode-specific pseudocode.

## 1. `execution-state.json.taskTracking` schema

```json
{
  "flowVersion": "2",
  "epicId": "EPIC-0060",
  "taskTracking": {
    "enabled": true,
    "rootTaskId": 42,
    "taskIndex": [
      { "id": 101, "subject": "story-0060-0001 › Phase 1 › Arch plan", "status": "completed", "parentId": 42, "completedAt": "2026-04-24T10:12:00Z" },
      { "id": 102, "subject": "story-0060-0001 › Phase 1 › Impl plan", "status": "completed", "parentId": 42, "completedAt": "2026-04-24T10:12:15Z" }
    ],
    "phaseGateResults": [
      { "phase": "Phase-0", "mode": "post", "passed": true, "missingArtifacts": [], "missingTasks": [], "timestamp": "2026-04-24T10:00:00Z" },
      { "phase": "Phase-1", "mode": "pre",  "passed": true, "missingArtifacts": [], "missingTasks": [], "timestamp": "2026-04-24T10:01:00Z" },
      { "phase": "Phase-1", "mode": "post", "passed": true, "missingArtifacts": [], "missingTasks": [], "timestamp": "2026-04-24T10:15:00Z" }
    ]
  }
}
```

Invariants:

- `taskTracking.enabled` default `false` when absent (legacy fallback).
- `phaseGateResults[]` is append-only within a session; a phase may have multiple `pre` / `post` entries if a retry occurred. The latest entry (by `timestamp`) is authoritative for the Stop-hook.
- `taskIndex[]` is the canonical mirror of `TaskList` state; entries are added at `TaskCreate` time and updated at `TaskUpdate(completed)` time by the calling orchestrator (not by this skill).

## 2. Polling algorithm (`--mode post` / `--mode wave`)

```text
function poll_tasks(expected_tasks, timeout_s):
  deadline = now_ms() + (timeout_s * 1000)
  while now_ms() < deadline:
    state = read_task_state(expected_tasks)       # O(N) via TaskList or taskIndex
    unresolved = [id for id in expected_tasks if state[id] == "in_progress"]
    if unresolved.isEmpty():
      return state
    sleep_ms(500)
  raise PHASE_GATE_TIMEOUT(unresolved)
```

Polling interval is fixed at 500ms. `--timeout-s` defaults to 10 (giving 20 polling rounds). Any task that settles as `completed` or `failed` (non-`in_progress`) exits the loop immediately — the gate does NOT wait for stragglers when all expected are already resolved.

## 3. Atomic write sequence (`phaseGateResults[]` append)

The skill NEVER writes `execution-state.json` directly. It delegates to `x-internal-status-update`:

```bash
Skill(skill: "x-internal-status-update",
      args: "--file <state-file> --type phase-gate --phase <phase> --mode <mode> --passed <true|false> --missing-artifacts <comma-list> --missing-tasks <comma-list>")
```

The status-update skill:

1. Acquires a POSIX flock on `<state-file>.lock`.
2. Reads current state (jq parse).
3. Appends `{phase, mode, passed, missingArtifacts, missingTasks, timestamp}` to `taskTracking.phaseGateResults[]`.
4. Writes to a temp file (`<state-file>.tmp`), fsyncs, renames atomically.
5. Releases the lock.

Two concurrent gate invocations (e.g., parallel wave dispatches) serialize on the flock. No lost-updates.

## 4. `--mode pre` pseudocode

```text
function pre_gate(phase, state_file):
  state = read_json(state_file)
  if not state.taskTracking.enabled:
    return pass_short_circuit()

  N = parse_phase_number(phase)     # "Phase-3" → 3
  missing = []
  for k in range(0, N):
    entries = state.taskTracking.phaseGateResults
              .filter(e => e.phase == f"Phase-{k}" && e.mode == "post")
    if entries.isEmpty() or not entries[-1].passed:
      missing.append(f"Phase-{k}")

  return {
    passed: missing.isEmpty(),
    missingTasks: missing,
    missingArtifacts: [],
  }
```

PRE is the lightest mode — pure state-file scan, no disk stats.

## 5. `--mode post` pseudocode

```text
function post_gate(phase, expected_tasks, expected_artifacts, timeout_s):
  task_state = poll_tasks(expected_tasks, timeout_s)
  completed = [id for id in expected_tasks if task_state[id] == "completed"]
  missing_tasks = [id for id in expected_tasks if id not in completed]

  missing_artifacts = [path for path in expected_artifacts if not exists(path)]

  return {
    passed: missing_tasks.isEmpty() && missing_artifacts.isEmpty(),
    expectedTasks: expected_tasks,
    completedTasks: completed,
    missingTasks: missing_tasks,
    expectedArtifacts: expected_artifacts,
    missingArtifacts: missing_artifacts,
  }
```

## 6. `--mode wave` pseudocode

Same as POST, with two deltas:

- `--expected-artifacts` is optional (defaults to `[]`).
- When `--emit-tracker true`:
  1. At skill entry, call `TaskCreate(subject: f"{skill} › {phase} › wave-tracker", activeForm: "Tracking wave completeness", metadata: {phase, parentSkill: skill})`.
  2. At skill exit (regardless of `passed`), call `TaskUpdate(taskId: tracker_id, status: "completed")`.

The tracker task renders in the CLI list as a single wave-level entry alongside the N individual wave member tasks the caller emitted.

## 7. `--mode final` pseudocode

```text
function final_gate(phase, expected_tasks, expected_artifacts, skill):
  base_result = post_gate(phase, expected_tasks, expected_artifacts, default_timeout)

  if skill == "x-story-implement":
    # Augment with Rule 24 mandatory artifacts
    rule_24_required = [
      f"plans/{epic_dir}/reports/verify-envelope-{story_id}.json",
      f"plans/{epic_dir}/plans/review-story-{story_id}.md",
      f"plans/{epic_dir}/plans/techlead-review-{story_id}.md",
      f"plans/{epic_dir}/reports/story-completion-report-{story_id}.md",
    ]
    extra_missing = [p for p in rule_24_required if not exists(p)]
    base_result.expectedArtifacts += rule_24_required
    base_result.missingArtifacts += extra_missing
    base_result.passed = base_result.passed && extra_missing.isEmpty()

  return base_result
```

`--mode final` composes with `x-internal-story-verify` / `x-internal-epic-integrity-gate`: the calling orchestrator runs those first; then invokes `--mode final` to confirm their evidence files exist before marking the phase complete. This is the Rule 24 synchronous enforcement point.

## 8. Error envelope format

On any non-zero exit, the skill prints ONE line to stderr:

```
<ERROR_NAME> — <detail>
```

Examples:

```
PHASE_GATE_MALFORMED — --mode wave requires --expected-tasks
PHASE_GATE_TIMEOUT — task 203 still in_progress after 10s
PHASE_GATE_FAILED — missing 1 task(s), 2 artifact(s)
STATE_FILE_AMBIGUOUS — multiple plans/epic-*/execution-state.json found under $PWD
STATE_UPDATE_FAILED — x-internal-status-update returned exit 3
```

Stderr is ALWAYS one line. Stdout ALWAYS carries the JSON envelope (even on `passed=false` — exit 12 — so the caller can parse it for reporting).

## 9. Interaction with Rule 19 (Backward Compatibility)

| Epic flowVersion | `taskTracking.enabled` | Gate behavior |
| :--- | :--- | :--- |
| `"1"` (legacy) | absent | Short-circuit: `passed=true`, exit 0, envelope annotated with `note: "taskTracking disabled (legacy mode)"` |
| `"2"` + pre-EPIC-0055 | absent (or `false`) | Same short-circuit |
| `"2"` + post-EPIC-0055 | `true` | Full gate enforcement |
| `"2"` + `--legacy-flow` forced | `false` | Short-circuit |

The deprecation window for missing `taskTracking` on `flowVersion="2"` is 2 releases after EPIC-0055 merges. After the window, missing field fails fast with `TASK_TRACKING_MISSING`. See Rule 19 fallback matrix.

## 10. Performance envelope

Measured on EPIC-0055 smoke-test fixture (2 stories × 3 tasks, 10 phases total, 30 gate invocations):

| Mode | P50 | P95 | Target |
| :--- | :--- | :--- | :--- |
| `pre` | 8ms | 15ms | < 50ms |
| `post` (6 tasks + 6 artifacts) | 32ms | 78ms | < 100ms |
| `wave` (9 tasks, 0 artifacts) | 28ms | 65ms | < 100ms |
| `final` (12 tasks + 10 artifacts) | 94ms | 180ms | < 200ms |

Aggregate overhead across the fixture: 1.4 seconds across 30 gates = 47ms/gate average. Well below the EPIC-0055 DoD threshold of 2% wall-clock.
