---
name: x-task-implement
model: sonnet
description: "Implements a feature/story/task using TDD (Red-Green-Refactor) workflow. Schema-aware: v1 (legacy) runs the original Double-Loop TDD flow with story-section task extraction; v2 (task-first, EPIC-0038) reads task-TASK-XXXX-YYYY-NNN.md + plan-task-TASK-XXXX-YYYY-NNN.md, honours declared I/O contracts, respects task-implementation-map dependencies, verifies post-conditions via grep/assert, and produces a single atomic commit per task via x-git-commit."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, TaskCreate, TaskUpdate
argument-hint: "[TASK-ID (TASK-XXXX-YYYY-NNN) or STORY-ID or feature-description] [--worktree] [--no-ci-watch]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **EXECUTION INTEGRITY (Rule 24)** — Every `Skill(...)` block is a **MANDATORY TOOL CALL**. TDD cycles (`x-test-tdd`), atomic commits (`x-git-commit`), CI watch (`x-pr-watch-ci`), and PR creation (`x-pr-create`) MUST be invoked as real tool calls, not inlined. See `.claude/rules/24-execution-integrity.md`.

## Triggers

```
/x-task-implement STORY-ID          — implement a story by ID (v1 or v2 schema auto-detected)
/x-task-implement TASK-XXXX-YYYY-NNN  — implement a specific task (v2 task-file-first mode)
/x-task-implement feature-description  — implement a feature from description
/x-task-implement STORY-ID --worktree  — standalone worktree mode (ADR-0004 Mode 2)
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `STORY-ID` or `TASK-ID` or description | positional | (required) | Story ID, TASK-XXXX-YYYY-NNN, or feature description |
| `--worktree` | boolean | `false` | Create dedicated worktree (standalone Mode 2). Ignored inside existing worktree (Rule 14 §3). |
| `--no-ci-watch` | boolean | `false` | Skip Step 4.5 CI-Watch. Required for CI/automation. |

## Output Contract

**Schema dispatch:**

| `planningSchemaVersion` | Execution Mode | Input artifacts |
|--------------------------|---------------|-----------------|
| `"1.0"` (or absent) | v1 — Double-Loop TDD via story section 8 | story file, `plan-story-*.md`, `tests-story-*.md` |
| `"2.0"` | v2 — task-file-first | `task-TASK-*.md` + `plan-task-TASK-*.md` + `task-implementation-map-*.md` |

Emits structured result to caller:
```json
{"status":"DONE","taskId":"TASK-XXXX-YYYY-NNN","commitSha":"abc123","cycleCount":N,"coverageDelta":{"lineBefore":95.1,"lineAfter":95.3},"wallclockMs":12340}
```

## Task Tracking (Rule 25)

Six phases (Rule 25 REGRA-001, EPIC-0055). Each phase opens with `x-internal-phase-gate --mode pre` + `TaskCreate`, closes with `TaskUpdate(completed)` + POST/WAVE/FINAL gate. Phase 2 dispatches 3 × N `TaskCreate` calls (Red/Green/Refactor per cycle) in Batch A, then updates sequentially in the execution loop. See `references/tdd-cycle-protocol.md` for the canonical TDD cycle tracking protocol.

## Phase 0 — Setup (Steps 0 and 0.5)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Phase-0-Setup`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-task-implement --phase Phase-0-Setup")

Open phase tracker (close with `TaskUpdate(id: phase0TaskId, status: "completed")` after Step 0.5):

    TaskCreate(subject: "{TASK_ID} › Step 0 - Precheck", activeForm: "Checking plan reuse and staleness")

Resolve paths, check staleness (Step 0), detect worktree context (Step 0.5):

    Skill(skill: "x-git-worktree", model: "haiku", args: "detect-context")

See `references/full-protocol.md` §Step 0 and §Step 0.5 for full three-way mode decision (REUSE / CREATE / LEGACY).

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-task-implement --phase Phase-0-Setup")

TaskUpdate(id: phase0TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Phase-0-Setup ok`

## Phase 1 — Prepare and Understand (Step 1)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Phase-1-Prepare`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-task-implement --phase Phase-1-Prepare")

Open phase tracker (close with `TaskUpdate(id: phase1TaskId, status: "completed")` after Step 1):

    TaskCreate(subject: "{TASK_ID} › Step 1 - Prepare", activeForm: "Loading knowledge packs and building TDD plan")

Dispatch a preparation subagent (Rule 13 Pattern 2 — SUBAGENT-GENERAL) that reads KPs and produces the TDD implementation plan. See `references/full-protocol.md` §Step 1.

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-task-implement --phase Phase-1-Prepare")

TaskUpdate(id: phase1TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Phase-1-Prepare ok`

## Phase 2 — TDD Cycles (Step 2)

See `references/tdd-cycle-protocol.md` for the canonical TDD cycle tracking protocol with Batch A/B dispatch and wave gate.

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Phase-2-TDD`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-task-implement --phase Phase-2-TDD")

Open phase tracker (close with `TaskUpdate(id: phase2TaskId, status: "completed")` after wave gate):

    TaskCreate(subject: "{TASK_ID} › Step 2 - TDD Cycles", activeForm: "Running TDD Red-Green-Refactor cycles")

**Batch A — emit all TDD cycle task trackers in ONE assistant message (Red/Green/Refactor per UT-N):**

    TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 1 › Red", activeForm: "RED cycle 1")
    TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 1 › Green", activeForm: "GREEN cycle 1")
    TaskCreate(subject: "{TASK_ID} › Step 2 › Cycle 1 › Refactor", activeForm: "REFACTOR cycle 1")
    [... repeat for Cycle 2..N in the same message ...]

Store returned IDs: `tddCycleTaskIds[N] = {red: id, green: id, refactor: id}`.

**Per-cycle execution (sequential):**

For each cycle N = 1..M:

    TaskUpdate(id: tddCycleTaskIds[N].red, status: "in_progress")
    [RED: write failing test → run → MUST fail]
    Skill(skill: "x-git-commit", model: "haiku", args: "--type test --scope {scope} --subject \"add failing test UT-N (RED)\"")
    TaskUpdate(id: tddCycleTaskIds[N].red, status: "completed")

    TaskUpdate(id: tddCycleTaskIds[N].green, status: "in_progress")
    [GREEN: minimum code → all tests MUST pass]
    Skill(skill: "x-git-commit", model: "haiku", args: "--type feat --scope {scope} --subject \"implement UT-N (GREEN)\"")
    TaskUpdate(id: tddCycleTaskIds[N].green, status: "completed")

    TaskUpdate(id: tddCycleTaskIds[N].refactor, status: "in_progress")
    [REFACTOR: improve design → tests MUST stay GREEN]
    Skill(skill: "x-git-commit", model: "haiku", args: "--type refactor --scope {scope} --subject \"improve UT-N\"")
    TaskUpdate(id: tddCycleTaskIds[N].refactor, status: "completed")

**Wave gate — all TDD cycle tasks completed:**

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode wave --skill x-task-implement --phase Phase-2-TDD --expected-tasks {all-tdd-task-ids}")

TaskUpdate(id: phase2TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Phase-2-TDD ok`

## Phase 3 — Validate and Status Transition (Steps 3 and 3.5)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Phase-3-Validate`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-task-implement --phase Phase-3-Validate")

Open phase tracker (close with `TaskUpdate(id: phase3TaskId, status: "completed")` after Step 3.5):

    TaskCreate(subject: "{TASK_ID} › Step 3 - Validate", activeForm: "Validating coverage and acceptance criteria")

Run all AT-N acceptance tests; coverage check (line ≥ 95%, branch ≥ 90%). See `references/full-protocol.md` §Step 3.

v2 only — Status transition (Step 3.5):

    TaskCreate(subject: "{TASK_ID} › Step 3.5 - Status sync", activeForm: "Syncing task status to Concluida")

Write `**Status:** Concluída` to task file and map row via `TaskMapRowUpdaterCli`. See `references/full-protocol.md` §Phase 3.5.

    TaskUpdate(id: step35TaskId, status: "completed")

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-task-implement --phase Phase-3-Validate")

TaskUpdate(id: phase3TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Phase-3-Validate ok`

## Phase 4 — Commit and CI Watch (Steps 4 and 4.5)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Phase-4-Commit`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-task-implement --phase Phase-4-Commit")

Open phase tracker (close with `TaskUpdate(id: phase4TaskId, status: "completed")` after Step 4.5):

    TaskCreate(subject: "{TASK_ID} › Step 4 - Commit", activeForm: "Creating TDD atomic commit")

Invoke atomic commit via `x-git-commit`. See `references/full-protocol.md` §Step 4.

    Skill(skill: "x-git-commit", model: "haiku", args: "--type feat --scope {scope} --subject \"implement {task-description}\"")

CI Watch (Step 4.5, conditional — v2 + `--worktree` standalone only; see decision table in `references/full-protocol.md` §Step 4.5):

    TaskCreate(subject: "{TASK_ID} › Step 4.5 - CI Watch", activeForm: "Polling CI checks after PR creation")

    Skill(skill: "x-pr-watch-ci", args: "--pr-number {N} --poll-interval-seconds 60 --timeout-minutes 30 --require-copilot-review=false")

    TaskUpdate(id: step45TaskId, status: "completed")

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-task-implement --phase Phase-4-Commit")

TaskUpdate(id: phase4TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Phase-4-Commit ok`

## Phase 5 — Cleanup (Step 5)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-task-implement Phase-5-Cleanup`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-task-implement --phase Phase-5-Cleanup")

Open phase tracker (close with `TaskUpdate(id: phase5TaskId, status: "completed")` at final gate):

    TaskCreate(subject: "{TASK_ID} › Step 5 - Cleanup", activeForm: "Cleaning up worktree")

Mode-aware cleanup (REUSE: no-op; CREATE: remove worktree; LEGACY: checkout develop). See `references/full-protocol.md` §Step 5.

    Skill(skill: "x-git-worktree", model: "haiku", args: "remove --id {task-id}")

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode final --skill x-task-implement --phase Phase-5-Cleanup")

TaskUpdate(id: phase5TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-task-implement Phase-5-Cleanup ok`

## Error Envelope

| Code | Condition |
|------|-----------|
| `TASK_ARTIFACT_NOT_FOUND` | task / plan / map file missing (v2) |
| `UNMET_DEPENDENCY` | declared `Depends on` TASK-ID not DONE |
| `SCHEMA_VIOLATION` | task file fails ERROR-level schema validation |
| `OUTPUT_CONTRACT_VIOLATION` | declared output failed post-exec verification (v2 Phase 3) |
| `RED_NOT_OBSERVED` | RED phase test didn't fail as expected |
| `REFACTOR_BROKE_TESTS` | refactor made previously-green tests fail |
| `STATUS_SYNC_FAILED` | Phase 3.5 (v2) failed to update `**Status:**` header or map row |
| Coverage below threshold | Add missing test scenarios; no bypass |

## Full Protocol

> Complete step-by-step instructions for each phase (Step 0 plan-reuse + staleness check, Step 0.5 worktree three-way mode decision, Step 1 subagent KP loading, Step 2 Double-Loop TDD + TPP ordering, Step 3 coverage + AC validation, Step 3.5 status transition, Step 4 atomic commit conventions, Step 4.5 CI-Watch decision table, Step 5 mode-aware cleanup), v2 extensions (Phase 0c schema detection, Phase 0d–0e pre-execution gates, Phase 5 status report), CI-Watch state-file schema, and all knowledge pack references in [`references/full-protocol.md`](references/full-protocol.md). Canonical TDD cycle tracking protocol (Batch A/B dispatch, wave gate, cycle IDs) in [`references/tdd-cycle-protocol.md`](references/tdd-cycle-protocol.md).
