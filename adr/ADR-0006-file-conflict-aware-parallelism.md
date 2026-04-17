---
status: Accepted
date: 2026-04-17
deciders:
  - Eder Celeste Nunes Junior
story-ref: "story-0041-0008"
---

# ADR-0006: File-Conflict-Aware Parallelism Analysis

> **Note on numbering:** EPIC-0041 originally scoped this decision record as
> `ADR-0005`, but that slot was assigned to
> [ADR-0005 — Telemetry Architecture](ADR-0005-telemetry-architecture.md)
> during EPIC-0040 (merged before EPIC-0041 Phase 6). The record is therefore
> filed as ADR-0006 to preserve monotonic numbering.

## Status

Accepted | 2026-04-17

## Context

`x-epic-map` computes story/task parallelism purely from the dependency DAG
(`Blocked By` edges). Two stories without a declared dependency are grouped in
the same phase even when both write to the same hotspot file — historically
`SettingsAssembler.java`, `HooksAssembler.java`, `CLAUDE.md`, `CHANGELOG.md`,
`pom.xml`, and the golden-file tree under `src/test/resources/golden/**`.

When `x-epic-implement` / `x-story-implement` dispatched those stories as
parallel worktrees, merge conflicts occurred on the hotspot files at
integration time. The cost surfaced across EPIC-0036..EPIC-0040: at least one
hotspot collision per epic, sometimes blocking the integration branch for
hours while the operator hand-resolved the diff.

Topological analysis alone cannot see this class of conflict — the DAG is
correct, but "no dependency" is not the same as "no contention". What was
missing was a **file-level footprint** declared up-front on every task/story
plan, and a gate that consumes those footprints to demote conflicting pairs
inside the same phase to serial execution.

## Decision

Introduce **file-conflict-aware parallelism analysis** as a first-class gate
across the planning → mapping → execution pipeline. Concretely:

1. **Structured File Footprint on plans (RULE-001, RULE-002).** Every
   `plan-task-*.md` carries a `## File Footprint` block with three
   sub-sections: `### write:`, `### read:`, `### regen:`. Every
   `plan-story-*.md` carries a `## Story File Footprint` block with the
   same schema, resulting from the union of its tasks' footprints. Paths are
   repo-relative; `**` wildcards are permitted. The heading set is
   canonical — unknown headings are rejected by the parser so that regex
   parsing stays deterministic.
2. **Collision categories (RULE-003).** (a) **Hard conflict** — two plans
   list the same path under `write:`. (b) **Regen conflict** — one plan
   lists a path under `write:` and another lists it under `regen:` (golden
   regeneration is serialized by the build tool). (c) **Soft conflict** —
   overlap only under `read:`; ignored. (a) and (b) block paralleism; (c)
   does not.
3. **Hotspot catalog (RULE-004).** Historically conflicting files
   (`SettingsAssembler.java`, `HooksAssembler.java`, `CLAUDE.md`,
   `.gitignore`, `CHANGELOG.md`, `pom.xml`, `src/test/resources/golden/**`)
   are treated as **exclusive-write**: any pair that touches the same
   hotspot serializes, even across waves.
4. **New skill `/x-parallel-eval --scope=epic|story|task`.** Standalone
   Java-backed skill (under `dev.iadev.parallelism.*`) that parses footprints
   and emits a collision matrix + reagrupment recommendation. Output is
   deterministic (alphabetic ordering, no embedded timestamps) so golden
   tests can pin its behaviour (RULE-008).
5. **`x-epic-map` Step 8.5.** Invokes `/x-parallel-eval --scope=epic` and
   annotates the Implementation Map with a new section "8.5 Restrições de
   Paralelismo" listing the pairs that must be serialized and the reason.
6. **Execution gate in `x-epic-implement` (Phase 0.5.0) and
   `x-story-implement` (Phase 1.5).** Before any parallel worktree
   dispatch, the gate re-runs `/x-parallel-eval`. On collision, the
   executor **degrades the wave to serial and logs a visible warning** with
   the conflicting pairs (RULE-005). It does NOT abort. The downgrade is
   persisted on `ExecutionState.parallelismDowngrades` for audit.
7. **Backward compatibility (RULE-006).** Plans generated before this epic
   do not carry a footprint. `/x-parallel-eval` treats a missing footprint
   as "unknown", emits a warning, and does NOT block execution. Re-planning
   with `--force` is the recommended migration path.
8. **Retroactive re-evaluation.** `plans/epic-0041/migrations/` contains
   `.diff` patches for epics 0036..0040 for human review — no map was
   auto-edited. EPIC-0040 was flagged HIGH (hard conflict on
   `telemetry-phase.sh`) in this pass.

## Alternatives Considered

### A1 — Distributed lock-file between worktrees at runtime

A runtime lock-file keyed by path would serialize the actual writes. Rejected
because (a) it punts the problem to integration time instead of planning
time — the operator still waits for conflicts before discovering them,
(b) lock coordination across worktrees is OS-dependent and fragile, (c) it
gives no up-front signal to the map.

### A2 — Static analysis at the class / method level

A scanner that reads the Java AST and produces write-sets per task would be
more precise than path-level tracking. Rejected for v1 because (a) it
requires a compile-ready workspace during planning (often the plan precedes
any code), (b) it does not cover non-Java artifacts (markdown, YAML, golden
files, `CLAUDE.md`, `pom.xml`) where most real conflicts actually happen,
(c) the engineering cost dominates the hotspot-centric benefit. RULE-002
keeps this option open for a later iteration — path granularity is a
subset of class granularity, so an AST-backed scanner can augment
`/x-parallel-eval` without breaking the contract.

### A3 — Granularity by Java class only (skip non-code hotspots)

A lighter version of A2 that groups files by class. Rejected because the
observed hotspots (`CLAUDE.md`, `CHANGELOG.md`, goldens) are not classes; a
class-only model would miss the majority of real conflicts.

## Consequences

### Positive

- Planning-time detection of hotspot collisions — operators see the
  constraint on the map before a single worktree is spawned.
- Audit trail: every downgrade is persisted on `ExecutionState`, so a post-
  mortem can tell "did parallelism degrade today, and on which pair?"
  without re-running the analysis.
- Deterministic output lets golden tests lock the matrix shape; regressions
  in the parser or the hotspot catalog are caught at CI time, not at integration.
- Retroactive `.diff` patches for epics 0036..0040 document the cost that
  was paid before the feature shipped, making the ROI visible.

### Negative

- Planning skills gain mandatory new sections. Hand-written plans that
  predate this epic must be re-planned with `--force` (mitigated by the
  backward-compatibility warning path — no hard break).
- Hotspot list in RULE-004 is static and must be curated as the codebase
  evolves. A stale catalog under-reports conflicts. A follow-up story may
  promote this to config.
- False positives on `**` wildcards: a task that writes
  `src/test/resources/golden/**/foo.txt` conflicts with every other task
  that touches the tree. The operator can override with `--force-parallel`
  on the executor, but this is intentionally friction-heavy.

### Neutral

- `/x-parallel-eval` is additive — no existing skill changes its user-facing
  contract. Existing `x-epic-map` callers continue to work; Step 8.5 is
  rendered only when footprints are available.
- The feature is orthogonal to EPIC-0038 (task-first flow) and EPIC-0040
  (telemetry). Telemetry captures the downgrade events via the existing
  `ExecutionState` writer — no new NDJSON schema needed.

## Related ADRs

- [ADR-0004 — Worktree-First Branch Creation Policy](ADR-0004-worktree-first-branch-creation-policy.md)
  — the parallelism gate fires before `/x-git-worktree create`, so
  downgraded waves never spawn competing worktrees in the first place.
- [ADR-0005 — Telemetry Architecture](ADR-0005-telemetry-architecture.md)
  — parallelism downgrades are surfaced through the same
  `ExecutionState`/NDJSON pipeline.

## Story Reference

- EPIC-0041 — File-Conflict-Aware Parallelism Analysis (stories 0001–0008).
- This ADR is authored by story-0041-0008 and accepts the design executed
  in stories 0041-0001 through 0041-0007.
