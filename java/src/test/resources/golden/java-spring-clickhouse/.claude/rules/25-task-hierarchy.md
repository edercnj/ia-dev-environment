# Rule 25 — Task Hierarchy & Phase Gate Contract

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity), Rule 19 (Backward Compatibility).
> **Introduced by:** EPIC-0055 (Task Hierarchy & Phase Gate Enforcement).
> **ADR:** ADR-0014 (numbering note: the parent story proposed ADR-0013, which was already taken; ADR-0014 is the next free number).

## Purpose

Every orchestrator that declares numbered `## Phase N` sections in its `SKILL.md` MUST expose its execution granularity to the operator as a structured, hierarchical task list, and MUST wrap each phase with a `x-internal-phase-gate` invocation. Before EPIC-0055 only `x-review` emitted `TaskCreate`/`TaskUpdate` — every other orchestrator delegated silently via `Skill(...)` calls, leaving the operator blind to which phase failed, which subagent of a wave was still running, or which TDD cycle was in flight. Rule 25 restores 4-level visibility (Root › Story › Phase › Wave/Cycle) and makes phase transitions explicit, auditable, and blockable.

## Scope

Applies to the 8 canonical orchestrators (the **Anexo B** set of EPIC-0055):

| Orchestrator | Layer |
| :--- | :--- |
| `x-epic-implement` | Epic (6 phases) |
| `x-story-implement` | Story (4 phases, 10 sub-phases) |
| `x-task-implement` | Task (5 steps + 4 numbered interludes) |
| `x-release` | Release (10+ phases) |
| `x-epic-orchestrate` | Epic planning loop |
| `x-review` | Specialist review wave |
| `x-review-pr` | Tech-lead 45-point review |
| `x-pr-merge-train` | Sequential PR loop |

Internal skills (`x-internal-*`) are **exempt** (see Invariant 6 below) — they are invoked by orchestrators that already own the task tracking boundary.

## Invariants

1. **`TaskCreate` per phase.** Every orchestrator MUST emit exactly one `TaskCreate(...)` upon entering each numbered `## Phase N` section, and a matching `TaskUpdate(status: "completed")` upon leaving it.
2. **`TaskCreate` per wave member.** When a phase dispatches a parallel wave (Batch A/B pattern — Rule 13 Pattern 2 used to launch N sibling subagents in one assistant message), the orchestrator MUST emit one `TaskCreate` per wave member in Batch A and one `TaskUpdate(status: "completed")` per member in Batch B.
3. **`TaskCreate` per sequential iteration.** When a phase iterates sequentially over items (stories, tasks, PRs), the orchestrator MUST emit one `TaskCreate` per iteration and chain them via `TaskUpdate(addBlockedBy: [previousId])` so the CLI renders "blocked by #N" between iterations.
4. **Phase gates PRE/POST mandatory.** Every numbered phase MUST invoke `Skill(skill: "x-internal-phase-gate", args: "--mode pre --phase N …")` before dispatch and a POST-family gate (`--mode post`, `--mode wave`, or `--mode final`) after completion. `wave` and `final` are reinforced POST variants (see §Integration with Rule 24 below): both validate everything `post` does and add extra checks (wave = Batch-B parallel completeness; final = Rule-24 mandatory-artifact scan). A phase that carries any one of the three satisfies the POST requirement. Exceptions MUST be marked with `<!-- phase-no-gate: <reason> -->` on the line immediately preceding the phase header.
5. **`subject` hierarchy.** Every `TaskCreate` `subject` MUST use the triangle separator `›` (U+203A) to express hierarchy. Maximum depth: 4 levels. Contract regex in §3 below.
6. **Internal skills DO NOT emit tasks.** Skills under `x-internal-*` are silent by design — their calling orchestrator owns the task boundary. Single exception: `x-internal-phase-gate --mode wave --emit-tracker true` MAY emit one tracker task to surface wave-level timing.
7. **Gate failure aborts with exit 12.** `x-internal-phase-gate` returns exit `12` (`PHASE_GATE_FAILED`) on any failed gate. The calling orchestrator propagates via its already-documented exit code (e.g., `x-story-implement` → `VERIFY_FAILED`; `x-epic-implement` → `INTEGRITY_GATE_FAILED`). Exit 12 is reserved for the gate sub-skill itself.

## `subject` Contract

### Canonical regex

```
^(?P<root>(?:[A-Z][A-Z0-9-]+|epic-[0-9]{4}|story-[0-9]{4}-[0-9]{4}|task-[0-9]{4}-[0-9]{4}(?:-[0-9]{3})?|Phase [0-9]+))(?: › (?P<levelN>[A-Za-z0-9_\-\.:() ]+))*$
```

- **Root:** either UPPERCASE identifier (`EPIC-0060`, `TASK-0060-0001-003`, `QA`), lowercase canonical (`epic-0060`, `story-0060-0001`, `task-0060-0001-003`), or the literal `Phase N`.
- **Separator:** exactly ` › ` (space, U+203A, space). ASCII `>` is invalid.
- **Depth:** `root` counts as level 1. Max 4 levels total — deeper nesting collapses subject tokens.

### Valid examples

- `story-0060-0001 › Phase 1 › Arch plan` (3 levels)
- `TASK-0060-0001-003 › Step 2 › Cycle 1 › Red` (4 levels, max depth)
- `EPIC-0060 › Phase 3 › story-0060-0001` (3 levels, epic→story transition)
- `QA › Review story-0060-0001` (2 levels, x-review legacy pattern — tolerated via Rule 19)

### Invalid examples

- `QA review` — no root prefix → REGEX mismatch
- `EPIC-0060 > Phase 3 > story-0060-0001` — uses ASCII `>` instead of `›`
- `EPIC-0060 › Phase 3 › story-0060-0001 › Phase 1 › Arch` — depth 5

## `activeForm` Convention

`activeForm` is the gerund of `subject` without the root prefix, truncated to < 40 characters. It drives the CLI spinner text.

| `subject` | `activeForm` |
| :--- | :--- |
| `story-0060-0001 › Phase 1 › Arch plan` | `Planning arch for story-0060-0001` |
| `TASK-0060-0001-003 › Red cycle › UT-2` | `Running Red cycle UT-2` |
| `EPIC-0060 › Phase 4 › Integrity gate` | `Running integrity gate` |

## `metadata` Convention

The `TaskCreate` `metadata` field carries structured context that `x-internal-phase-gate` reads via `TaskGet(taskId)`:

```json
{
  "phase": "Phase 1",
  "parentSkill": "x-story-implement",
  "storyId": "story-0060-0001",
  "epicId": "EPIC-0060",
  "expectedArtifacts": [
    "plans/epic-0060/plans/arch-story-0060-0001.md",
    "plans/epic-0060/plans/plan-story-0060-0001.md",
    "plans/epic-0060/plans/tests-story-0060-0001.md",
    "plans/epic-0060/plans/tasks-story-0060-0001.md",
    "plans/epic-0060/plans/security-story-0060-0001.md",
    "plans/epic-0060/plans/compliance-story-0060-0001.md"
  ]
}
```

| Key | Required? | Description |
| :--- | :--- | :--- |
| `phase` | yes | Exact phase string (matches `## Phase N — <Name>`). |
| `parentSkill` | yes | Orchestrator name emitting this task. |
| `storyId` / `epicId` / `taskId` | context-dependent | Whichever is relevant for the phase. |
| `expectedArtifacts` | yes for POST gates | Relative paths from repo root. Gate verifies each exists. |

## Interaction with `execution-state.json`

Adds two optional sub-objects (backward-compatible per Rule 19 — absence means legacy mode, gates are no-ops):

```json
{
  "flowVersion": "2",
  "epicId": "EPIC-0060",
  "taskTracking": {
    "enabled": true,
    "rootTaskId": 42,
    "phaseGateResults": [
      { "phase": "Phase 1", "mode": "post", "passed": true, "missingArtifacts": [], "missingTasks": [] },
      { "phase": "Phase 2", "mode": "post", "passed": true, "missingArtifacts": [], "missingTasks": [] }
    ]
  }
}
```

`taskTracking.enabled` defaults to `false` when the key is absent. Rule 19's fallback matrix treats that as legacy behavior — orchestrators DO NOT emit tasks and phase-gate calls become no-ops.

## Enforcement Layers

Four layers — a violation caught by any layer fails the lifecycle.

| Layer | Mechanism | Trigger | Reaction |
| :--- | :--- | :--- | :--- |
| **1 — Normative** | This rule + CLAUDE.md "EXECUTION INTEGRITY" block | Loaded every conversation | LLM guided to emit `TaskCreate` + invoke the gate |
| **2 — Runtime (Stop hook)** | `.claude/hooks/verify-phase-gates.sh` | `Stop` event at end of LLM turn | Reads `taskTracking.phaseGateResults`; missing/failed gate → stderr WARNING + exit 2 |
| **3 — Runtime (PreToolUse hook)** | `.claude/hooks/enforce-phase-sequence.sh` | `PreToolUse` on `Skill(...)` targeting an orchestrator | Blocks invocation when a predecessor phase has no `passed=true` record |
| **4 — CI audit** | `scripts/audit-task-hierarchy.sh` (exit 25) + `scripts/audit-phase-gates.sh` (exit 26) | PR to `develop` or `epic/*` | Fails build with `TASK_HIERARCHY_VIOLATION` / `PHASE_GATE_VIOLATION` |

## Audit Contract (Layer 4)

`scripts/audit-task-hierarchy.sh` (story-0055-0002) scans every `SKILL.md` under `java/src/main/resources/targets/claude/skills/` and fails when:

1. An orchestrator in the Scope table above lacks a `TaskCreate(` inside any `## Phase N` section.
2. A `TaskCreate(` has no matching `TaskUpdate(..., status: "completed")` downstream in the same file (unless `<!-- audit-exempt -->` precedes it).
3. A `## Phase N` section lacks a `--mode pre` invocation OR any POST-family gate (`--mode post`, `--mode wave`, or `--mode final`) of `x-internal-phase-gate` (unless `<!-- phase-no-gate: <reason> -->` precedes it).
4. A `subject:` literal does not match the regex in §3.

Escape hatches:

- **Baseline** (`audits/task-hierarchy-baseline.txt`) grandfathers orchestrators merged before Rule 25 was introduced. Immutable after EPIC-0055 merges.
- **Per-phase exemption** (`<!-- phase-no-gate: <reason> -->`) marks intentional skips — e.g., purely prose phases that produce no artifact.
- **Per-task exemption** (`<!-- audit-exempt -->`) allows a `TaskCreate` without a completion partner — rare, reviewed case.

## Integration with Rule 24 (Execution Integrity)

The `--mode post` gate of the **last evidence-producing phase** of each orchestrator MUST include the four Rule 24 mandatory artifacts in `--expected-artifacts`:

| Orchestrator | Phase | Required artifacts |
| :--- | :--- | :--- |
| `x-story-implement` | Phase 3 | `verify-envelope-STORY-ID.json`, `review-story-STORY-ID.md`, `techlead-review-story-STORY-ID.md`, `story-completion-report-STORY-ID.md` |

This promotes Rule 24 enforcement from "Stop-hook notices afterwards" to **synchronous gate before the phase is marked completed**. Stop-hook + CI audit remain as defense in depth (Layers 2+3).

## Backward Compatibility

- Epics created before EPIC-0055 merged have `taskTracking` absent from `execution-state.json`. Orchestrators treat absence as `taskTracking.enabled=false` — no task emission, gate calls become no-ops. Behavior identical to pre-Rule-25.
- `--legacy-flow` on `x-epic-implement` forces `taskTracking.enabled=false` even on new epics.
- Deprecation window: 2 releases after EPIC-0055 merges into `main`. After the window, missing `taskTracking` field on a `flowVersion="2"` state file fails fast with `TASK_TRACKING_MISSING`.

## Forbidden

- Emitting `TaskCreate` from within an `x-internal-*` skill (breaks Invariant 6).
- Using ASCII `>` or `-` instead of `›` as hierarchy separator (breaks Invariant 5 + subject regex).
- Using `TodoWrite`/`TodoRead` (legacy) in a skill that already emits `TaskCreate` — redundant and drifts the task list.
- Skipping a phase gate to "speed up" execution — the gate's 47ms average latency is below the telemetry-measurable floor.
- Adding entries to `audits/task-hierarchy-baseline.txt` after Rule 25 merges — file is immutable by CI check.

## Audit

The audit itself is self-verified: `scripts/audit-task-hierarchy.sh --self-check` asserts that this rule file, the baseline file (`audits/task-hierarchy-baseline.txt`), and the skills root directory are present. If any of those expected paths are missing, the build fails with `RULE_25_ENFORCEMENT_BROKEN`. Deeper wiring assertions (CLAUDE.md references, `x-internal-phase-gate` presence) are intentionally out of scope for the self-check — they live inside the full audit pass.
