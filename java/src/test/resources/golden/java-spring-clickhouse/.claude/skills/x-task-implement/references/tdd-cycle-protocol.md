<!-- Returns to [slim body](../SKILL.md) after reading this section. -->

# x-task-implement — TDD Cycle Tracking Protocol (Rule 25)

Canonical protocol for Phase 2 (TDD Cycles) of `x-task-implement`.
Supersedes inline Step 2 notes in `full-protocol.md` for Rule 25 tracking.

## Overview

Phase 2 implements Double-Loop TDD (outer AT-N, inner UT-N) with full task
visibility per cycle (Red → Green → Refactor). Each acceptance test drives
one or more unit test cycles. All cycle task trackers are created in Batch A
before execution begins, then updated sequentially.

## Subject Hierarchy (Rule 25 §3)

```
{TASK_ID} › Step 2 - TDD Cycles         (level 2 — step tracker)
{TASK_ID} › Step 2 › Cycle N › Red      (level 4 — cycle phase, max depth)
{TASK_ID} › Step 2 › Cycle N › Green    (level 4)
{TASK_ID} › Step 2 › Cycle N › Refactor (level 4)
```

`{TASK_ID}` is the full task identifier (e.g., `TASK-0060-0001-003`).
Use ` › ` (space + U+203A + space) as the separator — never ASCII `>`.
Depth 4 is the Rule 25 maximum; do not add further nesting.

## Batch A — Create All Cycle Trackers

Emit ALL TaskCreate calls for ALL cycles in ONE assistant message before
starting the execution loop. This enables the wave gate to track completion.

```
[One assistant message — all TaskCreate calls as sibling tool calls]

TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 1 › Red",      activeForm: "RED cycle 1")
TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 1 › Green",    activeForm: "GREEN cycle 1")
TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 1 › Refactor", activeForm: "REFACTOR cycle 1")

TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 2 › Red",      activeForm: "RED cycle 2")
TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 2 › Green",    activeForm: "GREEN cycle 2")
TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 2 › Refactor", activeForm: "REFACTOR cycle 2")

[... repeat for Cycle 3..N ...]
```

Store returned IDs: `tddCycleTaskIds[N] = {red: id, green: id, refactor: id}`.

## Execution Loop — Per-Cycle Sequential Updates

For each cycle N = 1..M (sequential, one cycle at a time):

```
# --- RED ---
TaskUpdate(id: tddCycleTaskIds[N].red, status: "in_progress")

Write failing test (name: [method]_[scenario]_[expected]).
Run: {{TEST_COMMAND}} — MUST fail (exit non-zero). If test passes, throw RED_NOT_OBSERVED.

Skill(skill: "x-git-commit", model: "haiku",
      args: "--type test --scope {scope} --subject \"add failing test UT-N (RED)\"")

TaskUpdate(id: tddCycleTaskIds[N].red, status: "completed")

# --- GREEN ---
TaskUpdate(id: tddCycleTaskIds[N].green, status: "in_progress")

Write minimum production code to make the test pass.
Run: {{COMPILE_COMMAND}} && {{TEST_COMMAND}} — ALL must pass.

Skill(skill: "x-git-commit", model: "haiku",
      args: "--type feat --scope {scope} --subject \"implement UT-N (GREEN)\"")

TaskUpdate(id: tddCycleTaskIds[N].green, status: "completed")

# --- REFACTOR ---
TaskUpdate(id: tddCycleTaskIds[N].refactor, status: "in_progress")

Improve design without adding behavior:
  - Extract method when > 25 lines
  - Eliminate duplication
  - Improve naming
Run: {{TEST_COMMAND}} — MUST stay GREEN. If tests fail, throw REFACTOR_BROKE_TESTS.

Skill(skill: "x-git-commit", model: "haiku",
      args: "--type refactor --scope {scope} --subject \"improve UT-N\"")

TaskUpdate(id: tddCycleTaskIds[N].refactor, status: "completed")
```

## Wave Gate — POST validation after all cycles complete

After the loop completes all N cycles, invoke the wave gate to verify every
cycle task (Red/Green/Refactor × N) has status `completed`:

```
Skill(skill: "x-internal-phase-gate", model: "haiku",
      args: "--mode wave --skill x-task-implement --phase Phase-2-TDD
             --expected-tasks {comma-separated list of all tddCycleTaskIds}")
```

The `--expected-tasks` argument receives the flat list of all cycle task IDs
(red1, green1, refactor1, red2, green2, refactor2, ..., redN, greenN, refactorN).

Then close the step tracker:

```
TaskUpdate(id: phase2TaskId, status: "completed")
```

## Error Codes (Phase 2)

| Code | Condition |
|------|-----------|
| `RED_NOT_OBSERVED` | RED phase test passed unexpectedly (not failing as required) |
| `REFACTOR_BROKE_TESTS` | Refactor phase caused previously-GREEN tests to fail |

## TPP Order for Cycle Sequencing

Order unit tests from simplest to most complex per the Transformation
Priority Premise (TPP):

```
{} → nil → constant → constant+ → scalar → unconditional → if → while → assign → recurse → iterate → map
```

Earlier cycles handle trivial inputs (nil, empty, zero). Later cycles handle
edge cases, errors, and complex scenarios. This ensures each RED → GREEN →
REFACTOR step is the smallest safe increment.

## Notes

- DO NOT emit TaskCreate for cancelled or skipped cycles (no `<!-- audit-exempt -->` needed for cycles that are never started).
- DO NOT emit phase markers (`phase.start`/`phase.end`) inside the cycle loop — the TELEMETRY markers on Phase 2 in `SKILL.md` cover the entire loop.
- The `tddCycleTaskIds` map is in-memory; it does not need to be persisted to `execution-state.json` for Phase 2.
