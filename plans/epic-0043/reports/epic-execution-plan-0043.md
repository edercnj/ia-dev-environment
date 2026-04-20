# Epic Execution Plan -- EPIC-0043

> **Epic ID:** EPIC-0043
> **Title:** Standardize Interactive Gates with Fixed-Option Menus
> **Date:** 2026-04-19
> **Total Stories:** 6
> **Total Phases:** 2
> **Author:** Epic Orchestrator
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential (`--sequential`) |
| Max Parallelism | 1 |
| Checkpoint Frequency | After each story |
| Dry Run | false |

> Sequential mode enabled: all 6 stories execute one at a time. Phase 0.5 pre-flight conflict analysis is skipped (no parallel dispatch). Stories in Phase 1 are executed in critical-path priority order.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Convention | story-0043-0001 | 1 (sequential) | M | — |
| 1 | Retrofits + Audit | story-0043-0002, 0003, 0004, 0005, 0006 | 1 (sequential) | L–XL | Phase 0 complete |

> **Total estimated duration:** XL (6 stories, 25 tasks, sequential execution)

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0043-0001 | Convention — ADR-0005 + Rule 20 — Interactive Gates | 0 | — | Yes | M |
| 2 | story-0043-0002 | Retrofit `x-release` Phase 8 APPROVAL-GATE | 1 | story-0043-0001 | No | M |
| 3 | story-0043-0003 | Retrofit `x-story-implement` (Task PR + Contract Gates) | 1 | story-0043-0001 | Yes (COALESCED) | M |
| 4 | story-0043-0004 | Retrofit `x-epic-implement` Batch PR Gate | 1 | story-0043-0001, COALESCED w/ 0003 | Yes (COALESCED) | M |
| 5 | story-0043-0005 | Retrofit `x-review-pr` Exhausted-Retry Gate | 1 | story-0043-0001 | No | S |
| 6 | story-0043-0006 | CI Audit — Rule 20 Enforcement | 1 | story-0043-0001 | No | M |

> **Critical Path Legend:** `Yes` = story is on the critical path; `No` = story has slack.
> **COALESCED Note:** TASK-0043-0003-004 ↔ TASK-0043-0004-001 share `x-epic-implement/SKILL.md`. In sequential mode, story-0043-0003 runs fully before story-0043-0004 begins — the COALESCED commit lands within story-0043-0003 first, then story-0043-0004 augments it. Golden regen runs in TASK-0043-0004-003.

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 6 story files found: story-0043-{0001..0006}.md |
| Dependencies resolved | PASS | 0001 → {0002,0003,0004,0005,0006}; no cycle detected |
| Circular dependencies | PASS | No cycles in DAG |
| Implementation map valid | PASS | IMPLEMENTATION-MAP.md present with 2-phase plan |

> Pre-flight conflict analysis SKIPPED (`--sequential` mode). COALESCED pair (0003-004 ↔ 0004-001) noted — sequential execution prevents conflict.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | ~600k–900k | 6 stories × avg ~120k tokens each (SKILL.md + golden diffs heavy) |
| Estimated wall time | 60–120 min | Sequential; story-0001 and story-0003/0004 are largest |
| Max parallel subagents | 1 | Sequential mode |
| Peak memory estimate | Low | One subagent at a time |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| COALESCED tasks (0003-004 ↔ 0004-001) land in wrong order | High | Possible | Sequential: 0003 runs first, full commit; 0004 starts after 0003 SUCCESS |
| Golden regen drift between 0003 and 0004 | High | Likely | TASK-0043-0004-003 owns the final golden regen after both COALESCED tasks land |
| `mvn process-resources` not run before regen | Medium | Likely | Each story must run `mvn process-resources` before `GoldenFileRegenerator` |
| Rule 20 slot collision with existing rules | Low | Unlikely | Slot 20 confirmed available (slots 13-19 occupied, 10-12 reserved) |
| Audit script breaking other CI checks | Medium | Possible | story-0043-0006 isolates audit in its own test class `InteractiveGatesAuditTest` |

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | After each story |
| Save on phase completion | Yes |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | `plans/epic-0043/execution-state.json` |

### Recovery Procedures

If execution is interrupted, run:
```
/x-epic-implement 0043 --sequential --resume
```

The resume workflow will:
1. Load `execution-state.json`
2. Reclassify IN_PROGRESS → PENDING
3. Verify PR status for any PRs created before interruption
4. Re-enter execution loop from first PENDING story

### Resume Behavior

- Stories with status `SUCCESS` are preserved and never re-executed
- Stories with status `IN_PROGRESS` at interruption are reset to `PENDING`
- Stories with status `FAILED` (retries < 2) are reset to `PENDING`
- Phase ordering is maintained; Phase 1 does not start until Phase 0 is SUCCESS
