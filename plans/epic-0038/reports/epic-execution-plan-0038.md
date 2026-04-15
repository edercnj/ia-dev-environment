# Epic Execution Plan -- EPIC-0038

> **Epic ID:** EPIC-0038
> **Title:** Task-First Planning & Execution Architecture
> **Date:** 2026-04-14
> **Total Stories:** 10
> **Total Phases:** 8 (0..7)
> **Author:** Epic Orchestrator (x-epic-implement)
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential (per-phase, single-story dispatch) |
| Max Parallelism | 1 |
| Checkpoint Frequency | After every story completion + after every phase integrity gate |
| Dry Run | true |

**Flags applied:** `--dry-run --sequential`
**Merge mode:** `no-merge` (default). PRs are created but not merged; dependencies satisfied by `status == SUCCESS`. Dependent stories merge upstream branches at start of their lifecycle.
**Review:** enabled (specialist + tech lead per story).
**Auto-approve PR:** disabled.
**Smoke gate:** enabled (runs between phases on `develop` after PRs merge — DEFERRED in no-merge mode; per-story smoke still runs inside `x-story-implement`).
**Phase 4 (PR comment remediation):** enabled.
**Starting branch:** `develop` @ d2ea86369.

> **Note on `--sequential`:** Phase 0.5 (pre-flight conflict analysis) is skipped entirely. Phases 1 and 2 (which allow 2 parallel stories each in the map) are executed one story at a time instead. Trade-off: no merge-conflict surface, but wallclock doubles for those phases.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Foundation: Task schema | story-0038-0001 | 1 | 45-90 min | — |
| 1 | Map schema + `x-task-plan` callable | story-0038-0002, story-0038-0003 | 1 (forced serial) | 90-180 min | Phase 0 |
| 2 | Planning wiring + execution read | story-0038-0004, story-0038-0005 | 1 (forced serial) | 90-180 min | Phase 1 (0004←0003, 0005←0001+0002) |
| 3 | Story orchestrator refactor | story-0038-0006 | 1 | 60-120 min | story-0038-0005 |
| 4 | Epic orchestrator simplification | story-0038-0007 | 1 | 45-90 min | story-0038-0006 |
| 5 | Migration + backward compat | story-0038-0008 | 1 | 60-120 min | story-0038-0007 |
| 6 | Documentation, templates, rules | story-0038-0009 | 1 | 60-120 min | story-0038-0008 |
| 7 | E2E integration + dogfood | story-0038-0010 | 1 | 60-120 min | story-0038-0009 |

> **Total estimated wallclock duration:** ~8-15 hours of agent execution time (sequential). Actual varies significantly with review cycle feedback and TDD iteration count per task.

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0038-0001 | Task como artefato primário | 0 | — | Yes (bottleneck) | L |
| 2 | story-0038-0002 | Task-implementation-map per story | 1 | story-0038-0001 | Yes | M |
| 3 | story-0038-0003 | `x-task-plan` refatorada como skill callable | 1 | story-0038-0001 | No (leaf branch) | M |
| 4 | story-0038-0004 | `x-story-plan` invoca `x-task-plan` | 2 | story-0038-0003 | No (leaf) | M |
| 5 | story-0038-0005 | `x-task-implement` refatorada (lê task contracts) | 2 | story-0038-0001, story-0038-0002 | Yes (convergence point) | L |
| 6 | story-0038-0006 | `x-story-implement` orquestra tasks via map | 3 | story-0038-0005 | Yes (architectural checkpoint) | L |
| 7 | story-0038-0007 | `x-epic-implement` simplificado | 4 | story-0038-0006 | Yes | M |
| 8 | story-0038-0008 | Migration path + backward compat | 5 | story-0038-0007 | Yes | M |
| 9 | story-0038-0009 | Documentação, templates e 5 RULEs | 6 | story-0038-0008 | Yes | M |
| 10 | story-0038-0010 | E2E integration tests + dogfood verification | 7 | story-0038-0009 | Yes (terminal leaf) | M |

> **Critical Path:** 0001 → 0002 → 0005 → 0006 → 0007 → 0008 → 0009 → 0010 (8 of 10 stories). Stories 0003 and 0004 form the planning leaf branch and do not gate downstream work.

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 10/10 story-0038-*.md files discovered in `plans/epic-0038/` |
| Dependencies resolved | PASS | All 10 stories map to nodes in IMPLEMENTATION-MAP.md §1; no unresolved references |
| Circular dependencies | PASS | Topological sort succeeds; 8-phase DAG confirmed |
| Implementation map valid | PASS | IMPLEMENTATION-MAP.md §1 matrix, §2 Mermaid, §3 critical path all consistent |
| Cross-epic hard dependency | VERIFY | EPIC-0036 stories 0036-0001..0006 must be merged to `develop` before story-0038-0005 starts (rename `x-dev-implement` → `x-task-implement`). Verified at story-level pre-flight inside `x-story-implement`. |
| Phase 0.5 (overlap analysis) | SKIP | Skipped per `--sequential` flag |

**Notes:**
- The `implementation-map-0038.md` → `IMPLEMENTATION-MAP.md` rename was committed in d2ea86369 to satisfy the prerequisite check.
- Reports directory `plans/epic-0038/reports/` created at plan generation time.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | 2-5M | Aggregate across all subagents (10 stories × ~200-500k per story-implement cycle including reviews) |
| Estimated wall time | 8-15 hours | Sequential mode; real time dominated by per-story TDD cycles and review feedback loops |
| Max parallel subagents | 1 | Forced by `--sequential`. The orchestrator dispatches one `x-story-implement` subagent at a time. |
| Peak memory estimate | Low | One subagent context at a time; orchestrator keeps only metadata per story |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Schema of Task artifact (story-0038-0001) is incorrect — cascades retrabalho to 0002, 0003, 0005, 0009 (templates derivados) | Critical | Possible | Pause after Phase 0 PASS and inspect schema + parser + migrated example before dispatching Phase 1. Multi-agent review (architect+QA+tech-lead) inside `x-story-implement`. |
| Cross-epic hard dep EPIC-0036 stories 0036-0001..0006 not merged to `develop` | High | Unlikely | Story-level pre-flight in `x-story-implement` greps for `x-task-implement` skill presence; story-0038-0005 aborts with clear error if missing. |
| Bootstrap recursion: agent tries to self-host EPIC-0038 under v2 mid-execution | High | Possible | Spec §8.2 forbids self-hosting; EPIC-0038 remains v1 throughout. If agent generates v2 artifacts for 0038's own stories, treat as bug. |
| `x-story-implement` Phase 6 creates PR but `--no-merge` blocks downstream dependency in 0005 (waits on 0001+0002 code) | Medium | Likely | Dependent stories merge upstream branches (`origin/feat/story-0038-NNNN-*`) at start of their lifecycle per `--no-merge` contract (RULE-003). |
| Integration gate detects regression but cannot attribute to specific story (sequential mode lowers this risk but doesn't eliminate it) | Medium | Unlikely | Per-story commits are atomic; `git revert` + mark story FAILED + block propagation. |
| Story-0038-0006 (architectural checkpoint) fails under real workload | High | Possible | Spec §3 recommends synthetic smoke story before declaring 0006 done. Do NOT advance to Phase 4 until 0006 passes smoke. |
| Markdown drift between IMPLEMENTATION-MAP, story files, and Jira (no Jira keys on this epic) | Low | Possible | Section 1.6b orchestrator sync updates status on SUCCESS; Jira sync is no-op (no keys). |

**Risk profile summary:** This epic is **meta** (refactors its own planning infrastructure) and **high-risk at Phase 0** (single point of failure). The `--sequential --dry-run` combination maximizes inspection opportunities at each boundary. Recommendation: after dry-run approval, execute `--phase 0` first, inspect artifacts, then resume.

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | After every story status transition |
| Save on phase completion | Yes (integrity gate result persisted) |
| Save on story completion | Yes (SUCCESS/FAILED/PARTIAL + commitSha + prUrl) |
| Save on integrity gate failure | Yes (gate result recorded with failedTests, regressionSource) |
| State file location | `plans/epic-0038/execution-state.json` |

### Recovery Procedures

- **On subagent failure (retries < 2):** story reclassified to PENDING on next orchestrator run, re-dispatched with same metadata (clean context).
- **On retry budget exhaustion:** story stays FAILED; block propagation marks downstream dependents BLOCKED.
- **On integrity gate FAIL with regression identified:** `git revert <commitSha>` on the offending story, mark story FAILED, propagate block to dependents.
- **On integrity gate FAIL without attribution:** orchestrator pauses and reports to user for manual intervention.
- **On conflict resolution subagent exhaustion (3 attempts in `x-story-implement`):** story FAILED, PR closed via `gh pr close`, block propagation triggered.

### Resume Behavior

- `--resume` loads `execution-state.json`, reclassifies IN_PROGRESS → PENDING, verifies PR_CREATED/PR_PENDING_REVIEW via `gh pr view`, preserves SUCCESS, reevaluates BLOCKED stories with the latest dependency status.
- In `no-merge` mode, dependency satisfaction is checked by `status == SUCCESS` only (PR merge status ignored for dispatch gating).
- Execution-state schema `version == "2.0"` when stories run in PRE_PLANNED mode (task-level tracking enabled); else `"1.0"` / absent.

---

## Dry-Run Summary

```
Epic Execution Plan (DRY RUN)
=============================
Epic: EPIC-0038 — Task-First Planning & Execution Architecture
Mode: per-story PR, sequential dispatch
Stories: 10 total across 8 phases
Starting branch: develop (d2ea86369)

Phase 0 (Foundation — sequential, 1 story):
  - story-0038-0001: Task como artefato primário
      Branch: feat/story-0038-0001-* → PR → develop
      Gate: integrity (DEFERRED in no-merge) + smoke per-story

Phase 1 (Map schema + planning skill — sequential, 2 stories):
  - story-0038-0002: Task-implementation-map per story
      Deps: story-0038-0001 (SUCCESS required; branch merged at story start per --no-merge)
  - story-0038-0003: x-task-plan callable
      Deps: story-0038-0001 (SUCCESS required)

Phase 2 (Planning wiring + execution read — sequential, 2 stories):
  - story-0038-0004: x-story-plan invoca x-task-plan
      Deps: story-0038-0003 (SUCCESS)
  - story-0038-0005: x-task-implement lê contratos I/O
      Deps: story-0038-0001, story-0038-0002 (both SUCCESS)
      Cross-epic: EPIC-0036 rename must be merged to develop

Phase 3 (Story orchestrator — sequential, 1 story):
  - story-0038-0006: x-story-implement orquestra tasks via map
      Deps: story-0038-0005 (SUCCESS)
      Architectural checkpoint — pause recommended for smoke run

Phase 4 (Epic orchestrator simplification — sequential, 1 story):
  - story-0038-0007: x-epic-implement simplificado
      Deps: story-0038-0006 (SUCCESS)

Phase 5 (Migration & backward compat — sequential, 1 story):
  - story-0038-0008: planningSchemaVersion + legacy loader
      Deps: story-0038-0007 (SUCCESS)

Phase 6 (Docs, templates, rules — sequential, 1 story):
  - story-0038-0009: templates + 5 RULEs + CLAUDE.md
      Deps: story-0038-0008 (SUCCESS)

Phase 7 (E2E + dogfood — sequential, 1 story, terminal leaf):
  - story-0038-0010: E2E test + dogfood next epic
      Deps: story-0038-0009 (SUCCESS)

Flags: --dry-run=true, --sequential=true, --auto-merge=false, --no-merge=true (default),
       --interactive-merge=false, --single-pr=false, --skip-review=false,
       --strict-overlap=false, --skip-smoke-gate=false, --skip-pr-comments=false,
       --auto-approve-pr=false, --batch-approval=true, --task-tracking=true
Merge mode: no-merge (PRs created but not merged; deps satisfied by SUCCESS)
```

No stories were dispatched. No PRs were created. No commits were made (except the rename normalization in d2ea86369 which preceded this dry-run).
