# ADR-0014 — Task Hierarchy & Phase Gate Enforcement

**Status:** Accepted
**Date:** 2026-04-24
**Supersedes:** —
**Superseded by:** —
**Related:** ADR-0010 (Interactive Gates Convention), Rule 13 (Skill Invocation Protocol), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity), Rule 19 (Backward Compatibility)

> **Note on numbering:** The parent story (`story-0055-0001`) originally specified `ADR-0013`. That number was already taken by `ADR-0013-knowledge-packs-dedicated-directory.md` (pre-existing, unrelated). ADR-0014 is the next available sequential number and supersedes the story's numbering reference. Rule 25 and `x-internal-phase-gate` SKILL.md have been updated to reference ADR-0014.

## Context

Before EPIC-0055, the orchestrator skills in this repository — `x-epic-implement`, `x-story-implement`, `x-task-implement`, `x-release`, `x-epic-orchestrate`, `x-review`, `x-review-pr`, `x-pr-merge-train` — silently delegated sub-work via the `Skill(...)` tool. Only `x-review` emitted structured `TaskCreate` / `TaskUpdate` calls. Every other orchestrator was a black box to the operator:

- No visibility into which phase was running.
- No visibility into which member of a parallel wave (6 planning subagents, 9 specialist reviewers) had completed.
- No visibility into which TDD cycle inside a task was in flight.
- No formal gate preventing a skill from skipping a phase and jumping ahead.

EPIC-0049 ("Thin Orchestrators") compressed ~2000 lines of inline shell into concise delegations, which made the black-box problem worse. The operator had no way to tell whether a stuck run was blocked on a real sub-skill call, the LLM inlining instead of delegating (Rule 24 — Execution Integrity), or a silent failure mid-wave.

An operator-initiated `Ctrl+C` on a 20-minute run typically revealed no information about where the 20 minutes were spent. Debugging required `grep`-ing telemetry NDJSON after the fact.

## Decision

Introduce **4-level hierarchical task tracking** with **synchronous phase gates** across all 8 canonical orchestrators, enforced by **4 defense-in-depth layers** (normative, Stop hook, PreToolUse hook, CI audit).

### Shape

1. **Rule 25** (`.claude/rules/25-task-hierarchy.md`) codifies:
   - `TaskCreate` emission per phase, per wave member, per sequential iteration.
   - Hierarchical `subject` contract using `›` (U+203A) separator, max depth 4.
   - `metadata` convention carrying `expectedArtifacts` consumed by the gate.
   - `execution-state.json.taskTracking.{enabled, phaseGateResults[]}` extension.

2. **`x-internal-phase-gate`** (new skill, `internal/plan/`, model `haiku`) provides four modes:
   - `--mode pre` — predecessor phases cleanly `completed`.
   - `--mode post` — all child tasks `completed` AND all `--expected-artifacts` exist on disk.
   - `--mode wave` — post-Batch-B parallel wave completeness.
   - `--mode final` — terminal gate composing with `x-internal-story-verify` and `x-internal-epic-integrity-gate`, adds Rule-24 mandatory-artifact scan.

3. **Four enforcement layers** (paralleling Rule 24):
   - **1 — Normative.** Rule 25 + CLAUDE.md top-level block guides LLM behavior.
   - **2 — Stop hook.** `.claude/hooks/verify-phase-gates.sh` reads `phaseGateResults[]` on `Stop` event; missing/failed gates → stderr WARNING + exit 2.
   - **3 — PreToolUse hook.** `.claude/hooks/enforce-phase-sequence.sh` blocks `Skill(...)` invocation of an orchestrator when its predecessor phase has no `passed=true` record.
   - **4 — CI audit.** `scripts/audit-task-hierarchy.sh` (exit 25) + `scripts/audit-phase-gates.sh` (exit 26) fail PRs missing `TaskCreate` / gate invocations.

4. **Rule 19 backward-compat.** `taskTracking.enabled` defaults to `false` when absent; legacy epics (flowVersion `"1"` or `"2"` pre-EPIC-0055) bypass gates as no-ops. Deprecation window: 2 releases.

### Scope — affected orchestrators (8)

| Orchestrator | Retrofit story |
| :--- | :--- |
| `x-task-implement` | story-0055-0003 |
| `x-story-implement` | story-0055-0004 |
| `x-epic-implement` | story-0055-0005 |
| `x-review` | story-0055-0006 |
| `x-review-pr` | story-0055-0007 |
| `x-release` | story-0055-0008 |
| `x-epic-orchestrate` | story-0055-0009 |
| `x-pr-merge-train` | story-0055-0010 |

Internal skills (`x-internal-*`) are exempt — their calling orchestrator owns the task boundary.

## Consequences

### Positive

- **4-level visibility during execution.** Operator sees `EPIC-0061 › Phase 3 › story-0061-0001 › Phase 1 › wave of 6 planners · 8m 17s` during `/x-epic-implement` — a change of category, not degree, over the current opacity.
- **Synchronous Rule-24 enforcement.** `--mode final` moves mandatory-artifact verification from "Stop hook after the fact" to "gate before the phase is marked completed". Reduces the window where a missing review or missing verify-envelope goes undetected.
- **Composable primitives.** The gate is a thin wrapper (~150 lines of bash + JSON envelope). Future orchestrators inherit the contract by construction.
- **Debugging time reduction.** Epic failures resolve from ~30min of `grep`-ing logs to < 2min of reading the task list.

### Negative

- **Maintenance surface.** Rule 25, one new skill, two new hooks, two new CI scripts, one new audit baseline. Every new orchestrator added post-EPIC-0055 must follow the contract or justify a `<!-- phase-no-gate -->` exemption.
- **Overhead.** ~47ms per gate invocation. Across an epic with ~30 gates, aggregate cost is ~1.4 seconds — well below the 2% wall-clock DoD threshold, but not zero.
- **Retrofit scope.** 8 orchestrator SKILL.md files touched (stories 0055-0003 to 0055-0010). Each retrofit is bounded, but they're sequential for the critical path (`x-task-implement` → `x-story-implement` → `x-epic-implement` must complete before the extension retrofits can parallelize).
- **Deprecation window management.** The `taskTracking` field default-absent fallback lasts 2 releases. CI needs to track the counter and fail closed after the window.

### Neutral

- Rule 25 REGRA-006 carves an exception for `x-internal-phase-gate --mode wave --emit-tracker true`. This is the single place where an internal skill emits a task of its own — intentional, tracked in the audit.

## Alternatives considered

### (a) Status quo — 1-level visibility only

Keep orchestrators as black boxes. Operators rely on telemetry NDJSON post-hoc.

**Rejected:** The stated problem IS the opacity. Telemetry is analytical, not operational — it doesn't help the operator reading the live CLI during a run.

### (b) 5+ levels of nesting

Expand hierarchy depth beyond 4 (e.g., cycle-level sub-tasks per UT within Red phase).

**Rejected:** Cost-benefit inverts past depth 4. `TASK-... › Step 2 › Cycle 1 › Red` already carries enough information. A fifth level fragments the CLI rendering without proportional gain.

### (c) Retrofit only `x-epic-implement` and `x-story-implement`

Skip the extension orchestrators (`x-release`, `x-review`, `x-review-pr`, `x-epic-orchestrate`, `x-pr-merge-train`).

**Rejected:** The black-box problem applies uniformly. Partial coverage would create "silent" phases that appear skipped in the task list, confusing the contract. Better to cover all 8 or none.

### (d) Post-hoc reconstruction instead of live emission

Infer hierarchy from telemetry NDJSON + artifact mtimes at the end of the run.

**Rejected:** Solves debugging but not live visibility. Operators watching a long run get no feedback until the run ends.

### (e) Use `TodoWrite` / `TodoRead` (legacy) instead of `TaskCreate`

Tap into the legacy todo API instead of introducing the new task hierarchy.

**Rejected:** `TodoWrite` has no parent/child relation and no `addBlockedBy` chain. It also doesn't support `metadata.expectedArtifacts`, which the gate consumes. Rule 25 explicitly forbids mixing the two (Invariant 6).

## Implementation notes

- **Skill model tier:** `haiku` — zero-reasoning lookup + file stats.
- **Skill path:** `java/src/main/resources/targets/claude/skills/core/internal/plan/x-internal-phase-gate/`.
- **Delegation:** All `phaseGateResults[]` writes go through `x-internal-status-update` (flock-protected). The gate itself NEVER writes `execution-state.json` directly.
- **Baseline file:** `audits/task-hierarchy-baseline.txt` seeds with the current orchestrators during the deprecation window. Immutable after EPIC-0055 merges.
- **Telemetry:** Internal skills do NOT emit `phase.start` / `phase.end`; the calling orchestrator owns the telemetry wrapper. Passive hooks still capture `tool.call`.

## References

- Rule 25 — `.claude/rules/25-task-hierarchy.md`
- Rule 24 — `.claude/rules/24-execution-integrity.md` (composition via `--mode final`)
- Rule 22 — `.claude/rules/22-skill-visibility.md` (`x-internal-*` convention)
- Rule 19 — `.claude/rules/19-backward-compatibility.md` (`taskTracking` fallback matrix)
- ADR-0010 — `adr/ADR-0010-interactive-gates-convention.md` (exempts internal skills from the 3-option menu)
- ADR-0012 — `adr/ADR-0012-skill-body-slim-by-default.md` (SKILL.md < 250 lines guideline; full protocol in `references/`)
- EPIC-0055 spec — `plans/epic-0055/spec-task-granularity-phase-gates.md`
- EPIC-0055 story index — `plans/epic-0055/epic-0055.md`
