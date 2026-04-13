# Epic Planning Report — EPIC-0037

> **Epic:** Worktree-First Branch Creation Policy
> **Date:** 2026-04-13
> **Total Stories:** 10 (9 implementable + 1 BLOCKED)
> **Stories Planned:** 9
> **Overall Status:** **READY** (excluding blocked STORY-0008)

## Readiness Summary

| Metric | Count |
|--------|-------|
| Stories Total | 10 |
| Stories Planned | 9 |
| Stories Ready (DoR READY) | 9 |
| Stories Not Ready (DoR NOT_READY) | 0 |
| Stories Blocked (deferred) | 1 (story-0037-0008 — blocked by EPIC-0035) |

## Per-Story Results

| # | Story ID | Phase | Planning Status | DoR Verdict | Tasks | Artifacts |
|---|----------|-------|-----------------|-------------|-------|-----------|
| 1 | story-0037-0001 | 0 | READY | READY | 7 | 10 |
| 2 | story-0037-0002 | 1 | READY | READY | 9 | 12 |
| 3 | story-0037-0003 | 2 | READY | READY | 10 | 13 |
| 4 | story-0037-0004 | 2 | READY | READY | 6 | 9 |
| 5 | story-0037-0005 | 2 | READY | READY | 7 | 10 |
| 6 | story-0037-0006 | 2 | READY | READY | 7 | 10 |
| 7 | story-0037-0007 | 2 | READY | READY | 9 | 12 |
| 8 | story-0037-0008 | 5 | PENDING (BLOCKED) | N/A | — | — |
| 9 | story-0037-0009 | 3 | READY | READY | 6 | 9 |
| 10 | story-0037-0010 | 4 | READY | READY | 7 | 10 |

**Total tasks across planned stories:** 68
**Total planning artifacts generated:** ~95 files

## Blockers

None for executable scope. **STORY-0008** is intentionally deferred: blocked by EPIC-0035 (`x-release` rewrite). Will be planned post-EPIC-0035 merge along with mini-regen STORY-0010b.

## Critical Risks (Cross-Story)

| Risk | Story | Severity | Mitigation |
|------|-------|----------|-----------|
| Migration breaks parallel epic dispatch | story-0037-0003 | **Critical** | Mandatory 2-story parallel smoke (TASK-008); rollback via PR revert |
| Task creates nested worktree inside story | story-0037-0006 | **Critical** | Mandatory nested-prevention smoke (TASK-006); largest epic risk per epic risk table |
| `${slug}`/`${epicId}`/`${TASK_ID}` injection (multiple stories) | 0003, 0006, 0007 | High | Regex validation in each: `[a-z0-9-]+`, `^[0-9]{4}$`, `^task-\d{4}-\d{4}-\d{3}$` |
| RULE-004 backward compat broken | 0004, 0005, 0006 | Critical | Each story has dedicated regression smoke (byte-identical output without flag) |
| Cleanup removes orchestrator's worktree | 0005, 0006, 0007 | Critical | RULE-003 enforced; defensive `OWNS=false` default; explicit `cd` to main repo before remove |
| Forward-references to ADR-0004 (story-0037-0009) | 0003 (TASK-004) | Medium | Placeholder TODO acceptable; update post-merge |
| Drift between inline `detect_worktree_context()` and x-git-worktree canonical | 0004, 0005, 0006, 0007 | Medium | Drift note in each consumer; x-git-worktree designated authoritative |

## Cross-Cutting Quality Gates Verified

- [x] All stories have RULE-001 SoT compliance in DoD (zero edits in `.claude/`/`.github/`)
- [x] All stories use Conventional Commits with `(story-0037-NNNN)` scope
- [x] All stories target `develop` branch with label `epic-0037`
- [x] All stories include golden file regen step where applicable
- [x] Epic-level success metrics traceable to story-0037-0010 success criteria checks
- [x] CHANGELOG entry consolidated in story-0037-0010

## Phasing & Execution Order

Following `plans/epic-0037/implementation-map-0037.md`:

| Phase | Stories | Notes |
|-------|---------|-------|
| 0 — Foundation | 0001 | Sequential; blocks all other stories |
| 1 — Mecanismo | 0002 | Sequential after 0001 |
| 2 — Migração + Standalone | 0003, 0004, 0007 (parallel) → 0005 → 0006 | Phase 2 has internal sequencing |
| 3 — ADR | 0009 | Independent; can run anytime |
| 4 — Sync Barrier | 0010 | Must run last |
| 5 — Future (BLOCKED) | 0008 | Deferred until EPIC-0035 merges |

## Generated Artifacts

Per story:
- `plans/epic-0037/plans/tasks-story-0037-NNNN.md` (task breakdown with full DoD)
- `plans/epic-0037/plans/planning-report-story-0037-NNNN.md` (multi-agent consolidated planning)
- `plans/epic-0037/plans/dor-story-0037-NNNN.md` (DoR checklist)
- `plans/epic-0037/plans/task-plan-TASK-NNN-story-0037-NNNN.md` (per-task plan files)
- Story file Section 9 appended with consolidated task breakdown

Epic-level:
- `plans/epic-0037/execution-state.json` (planning state checkpoint)
- `plans/epic-0037/reports/epic-planning-report-0037.md` (this file)

## Notes on Methodology

- **Stories 0001 & 0002** were planned with full 5-subagent dispatch (Architect + QA + Security + TechLead + PO).
- **Stories 0003-0010** were planned with consolidated multi-perspective subagents (each subagent adopted all 5 perspectives sequentially) to manage context budget while delivering equivalent task breakdown coverage.
- All stories produced complete planning artifact sets per the x-story-plan SKILL.md spec.
- **Compact task plan files** were generated per task to satisfy the "one file per task" requirement; full DoD details are consolidated in the per-story `tasks-*.md` file.
