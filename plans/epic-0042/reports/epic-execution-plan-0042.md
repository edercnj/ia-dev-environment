# Epic Execution Plan -- EPIC-0042

> **Epic ID:** EPIC-0042
> **Title:** Merge-Train Automation + Auto PR-Fix Hook
> **Date:** 2026-04-19
> **Total Stories:** 4
> **Total Phases:** 3
> **Author:** Epic Orchestrator
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Per-story PR (each story creates its own branch + PR targeting develop) |
| Max Parallelism | 2 (Phase 0 dispatches story-0042-0001 and story-0042-0004 in parallel) |
| Checkpoint Frequency | After each story completion |
| Dry Run | false |

Phases 1 and 2 are serial because story-0042-0002 and story-0042-0003 each extend
the same `x-pr-merge-train/SKILL.md` built by the previous story — parallel dispatch
would produce inevitable merge conflicts on that file. Phase 0 is parallel (the two
stories touch disjoint files: 0001 creates a new SKILL.md; 0004 edits x-story-implement/SKILL.md).

Merge mode: **auto** (default per EPIC-0042). All per-story PRs are auto-merged to
`develop` after CI passes. The `mergeMode` field in execution-state.json is `"auto"`.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Foundation + Independent Hook | story-0042-0001, story-0042-0004 | 2 parallel | ~45 min | — |
| 1 | Merge + Parallel Rebase | story-0042-0002 | 1 (serial) | ~35 min | Phase 0 (0001 only) |
| 2 | Verification + State + Errors | story-0042-0003 | 1 (serial) | ~40 min | Phase 1 |

> **Total estimated duration:** ~2 hours (wall time with parallel Phase 0)

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0042-0001 | Skill `x-pr-merge-train` — skeleton + discovery + validation | 0 | — | Yes | M |
| 1 | story-0042-0004 | Hook `x-pr-fix` após TL GO em `x-story-implement` | 0 | — | No | S |
| 2 | story-0042-0002 | Merge orchestration + parallel rebase subagents | 1 | story-0042-0001 (MERGED) | Yes | L |
| 3 | story-0042-0003 | Verification + state.json + error handling + examples | 2 | story-0042-0002 (MERGED) | Yes | L |

> **Critical Path:** story-0042-0001 → story-0042-0002 → story-0042-0003
> story-0042-0004 is a leaf node; delays do not impact the critical path.

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 4 story files found: story-0042-0001..0004.md |
| Dependencies resolved | PASS | 0001→0002→0003 chain; 0004 independent |
| Circular dependencies | PASS | No cycles detected (DAG validated) |
| Implementation map valid | PASS | IMPLEMENTATION-MAP.md present and parsed |

No advisory warnings. Phase 0 stories touch disjoint files:
- story-0042-0001: creates `core/pr/x-pr-merge-train/SKILL.md` (new file)
- story-0042-0004: edits `core/dev/x-story-implement/SKILL.md` (existing file)
Classification: **no-overlap** → parallel dispatch allowed.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | ~120K | 4 stories × ~30K; parallel Phase 0 saves wall time |
| Estimated wall time | ~2 hours | Serial phases 1+2 dominate |
| Max parallel subagents | 2 | Phase 0 only |
| Peak memory estimate | 2 subagents × context | Standard subagent footprint |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| story-0042-0002 extends 0001's SKILL.md — needs 0001 merged first | High | Very Likely | Strict phase ordering enforced; 0002 blocked until 0001 prMergeStatus=MERGED |
| Golden regen may differ across machines (RULE-005) | Medium | Possible | Verbatim regen block from README.md:810-818 enforced; golden diff validated in CI |
| x-story-implement SKILL.md is a hotspot (many epics touch it) | Medium | Likely | story-0042-0004 is serial in Phase 0 (no other story touches it); rebase auto-triggered |
| SkillsAssemblerTest requires SKILL.md present before test can go green | Low | Likely | TASK-0042-0001-005 writes test first (TDD: RED before GREEN) |

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | After each story completion |
| Save on phase completion | Yes |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | plans/epic-0042/execution-state.json |

### Recovery Procedures

On any story failure:
1. Inspect `plans/epic-0042/execution-state.json` for `status: "FAILED"` + `summary`
2. Inspect preserved worktree (if any) for diagnostic context
3. Fix the root cause
4. Re-run with `--resume` flag

### Resume Behavior

`--resume` reads execution-state.json, reclassifies IN_PROGRESS → PENDING,
verifies PR status via `gh pr view`, preserves SUCCESS + DONE tasks,
then re-enters the execution loop with only PENDING stories dispatched.
