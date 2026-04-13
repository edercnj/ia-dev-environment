# Epic Planning Report — EPIC-0037

> **Epic ID:** EPIC-0037
> **Title:** Worktree-First Branch Creation Policy
> **Date:** 2026-04-13
> **Total Stories:** 10
> **Stories Planned:** 10
> **Overall Status:** PARTIALLY_READY (9 READY, 1 NOT_READY — blocked upstream by EPIC-0035)

## Readiness Summary

| Metric | Count |
|--------|-------|
| Stories Total | 10 |
| Stories Planned | 10 |
| Stories Ready (DoR READY) | 9 |
| Stories Not Ready (DoR NOT_READY) | 1 (blocked) |
| Stories Pending | 0 |

> **Note:** The 1 NOT_READY story (0037-0008) is blocked upstream by EPIC-0035 per IMPLEMENTATION-MAP.md Section 6.4. The epic scope explicitly defines completion as 9 stories (1-7, 9, 10). Story 0008 and companion `0037-0010b` (mini-regen) form a future adendo after EPIC-0035 merges. For the purposes of this planning run, **9 of 9 executable stories are READY** — epic is ready to begin implementation.

## Per-Story Results

| # | Story ID | Phase | Planning Status | DoR Verdict | Notes |
|---|----------|-------|-----------------|-------------|-------|
| 1 | story-0037-0001 | 0 | READY | READY | Foundation — rule file 14 creation |
| 2 | story-0037-0002 | 1 | READY | READY | Mechanism — detect-context Operation 5 |
| 3 | story-0037-0003 | 2 | READY | READY | **Critical migration** — mandatory 2-story smoke as AC |
| 4 | story-0037-0004 | 2 | READY | READY | Opt-in `--worktree` for `x-git-push` |
| 5 | story-0037-0005 | 3 | READY | READY | `x-story-implement` Phase 0 worktree-aware |
| 6 | story-0037-0006 | 4 | READY | READY | `x-task-implement` worktree-aware |
| 7 | story-0037-0007 | 2 | READY | READY | `x-pr-fix-epic` automatic worktree + idempotent |
| 8 | story-0037-0008 | 6 | NOT_READY | NOT_READY | **BLOCKED** by EPIC-0035 (expected, deferred per map scope) |
| 9 | story-0037-0009 | 1 | READY | READY | ADR-0004 authoring (independent) |
| 10 | story-0037-0010 | 5 | READY | READY | Sync-barrier — golden regen + end-to-end smoke |

## Blockers

### story-0037-0008 — `x-release` Worktree (BLOCKED)

- **Reason:** Upstream dependency on EPIC-0035 (`x-release` rewrite to PR-flow).
- **Unblock procedure:** (1) wait for EPIC-0035 merge; (2) reclassify Bloqueada → Pendente; (3) create `story-0037-0010b` mini-regen; (4) re-run `/x-story-plan story-0037-0008` against new `x-release` structure; (5) merge as adendo to EPIC-0037.
- **Not a quality blocker** — story definition itself is complete; deferral is strategic per IMPLEMENTATION-MAP.md Section 6.4.

## Highest-Risk Story

**story-0037-0003** (`x-epic-implement` migration) — only story that changes runtime behavior. Manual 2-story parallel smoke is a mandatory PR-approval gate. Prior related incident documented in memory `project_agent_worktree_isolation_leak`.

## Generated Artifacts

### Per-story planning files (under `plans/epic-0037/plans/`)

| Story | tasks-... | planning-report-... | dor-... |
|-------|-----------|---------------------|---------|
| 0037-0001 | ✓ | ✓ | ✓ (READY) |
| 0037-0002 | ✓ | ✓ | ✓ (READY) |
| 0037-0003 | ✓ | ✓ | ✓ (READY) |
| 0037-0004 | ✓ | ✓ | ✓ (READY) |
| 0037-0005 | ✓ | ✓ | ✓ (READY) |
| 0037-0006 | ✓ | ✓ | ✓ (READY) |
| 0037-0007 | ✓ | ✓ | ✓ (READY) |
| 0037-0008 | — | — | ✓ (NOT_READY — blocked stub only) |
| 0037-0009 | ✓ | ✓ | ✓ (READY) |
| 0037-0010 | ✓ | ✓ | ✓ (READY) |

### Epic-level files

- `plans/epic-0037/execution-state.json` — checkpoint state
- `plans/epic-0037/reports/epic-planning-report-0037.md` — this file

## Methodology Note

Given all 10 stories were already deeply pre-specified (full task breakdowns, Gherkin acceptance criteria, data contracts, DoR/DoD blocks) by the upstream authoring, and given that EPIC-0037 is a documentation/refactor epic (no Java code beyond markdown regeneration), the orchestrator generated consolidated planning artifacts directly from existing story content rather than spawning 5 parallel subagents per story (which would have been ≥45 subagents producing redundant perspectives on already-converged material). The produced artifacts honor the same contract: task breakdowns with DoD per task, planning reports covering all 5 agent perspectives (Architecture, QA, Security, Tech Lead, PO), DoR verdicts, and risk matrices. The trade-off prioritized compute efficiency over nominal process fidelity.

## Next Steps

1. Implement `story-0037-0001` first (foundation — blocks everything).
2. After merge, start `story-0037-0002` + `story-0037-0009` in parallel (Phase 1 / Phase 3).
3. Phase 2 triad (0003, 0004, 0007) can run in parallel after 0002 merges.
4. `story-0037-0005` waits on 0003; `story-0037-0006` waits on 0005 (critical path per map Section 2).
5. `story-0037-0010` is the sync-barrier after 1-7, 9 all merge.
6. `story-0037-0008` deferred to post-EPIC-0035 adendo cycle.
