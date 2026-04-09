# Epic Execution Plan -- EPIC-0029

> **Epic ID:** EPIC-0029
> **Title:** Task-Centric Workflow Overhaul
> **Date:** 2026-04-08
> **Total Stories:** 18
> **Total Phases:** 7
> **Author:** Epic Orchestrator
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Parallel worktree dispatch (default) |
| Max Parallelism | 7 (Phase 0) |
| Checkpoint Frequency | Per-story completion |
| Dry Run | false |

Per-story PR model: each story creates its own branch targeting `develop`. Merge mode: `no-merge` (default). Dependencies satisfied by `status == SUCCESS` alone. Integrity gates deferred (code not merged to develop during execution).

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Dependencies |
|-------|------|---------|-------------|--------------|
| 0 | Foundation | 0001, 0002, 0003, 0004, 0006, 0010, 0011 | 7 parallel | -- |
| 1 | Core Skills | 0005, 0007, 0012 | 3 parallel | Phase 0 (partial: specific deps) |
| 2 | Workers | 0008, 0009, 0013 | 3 parallel | Phase 1 (partial) |
| 3 | Compositions | 0014, 0017 | 2 parallel | Phase 2 (partial) |
| 4 | Lifecycle Rewrite | 0015 | 1 (bottleneck) | Phases 0-2 complete + 0012 |
| 5 | Epic Orchestrator | 0016 | 1 (bottleneck) | Phase 4 + 0006 |
| 6 | Validation | 0018 | 1 (final) | ALL prior stories |

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0029-0001 | Formal Task Definition & Story Template Update | 0 | -- | No | M |
| 2 | story-0029-0002 | Task Status Model & Execution State Schema | 0 | -- | No | M |
| 3 | story-0029-0003 | x-format -- Code Formatting Skill | 0 | -- | Yes | M |
| 4 | story-0029-0004 | x-lint -- Code Linting Skill | 0 | -- | Yes | M |
| 5 | story-0029-0006 | x-worktree -- Git Worktree Management Skill | 0 | -- | No | M |
| 6 | story-0029-0010 | x-docs -- Documentation Skill | 0 | -- | No | M |
| 7 | story-0029-0011 | Individual Review Skills Extraction | 0 | -- | No | L |
| 8 | story-0029-0005 | x-commit -- Conventional Commit Skill | 1 | 0003, 0004 | Yes | M |
| 9 | story-0029-0007 | x-plan-task -- Task Planning Skill | 1 | 0001 | Yes | M |
| 10 | story-0029-0012 | x-review Orchestrator Refactor | 1 | 0011 | No | M |
| 11 | story-0029-0008 | x-tdd -- TDD Execution Skill | 2 | 0005, 0007 | Yes | L |
| 12 | story-0029-0009 | x-pr-create -- Task PR Creation Skill | 2 | 0005 | No | M |
| 13 | story-0029-0013 | x-story-create -- Testable Tasks & Value Delivery | 2 | 0001, 0007 | No | M |
| 14 | story-0029-0014 | x-story-map -- Task-Level Dependency Graph | 3 | 0001, 0013 | No | M |
| 15 | story-0029-0017 | x-git-push -- Task Branch Naming & Conventions | 3 | 0005, 0009 | No | M |
| 16 | story-0029-0015 | x-dev-lifecycle -- Task-Centric Workflow | 4 | 0002, 0005, 0007, 0008, 0009, 0012 | Yes | XL |
| 17 | story-0029-0016 | x-dev-epic-implement -- Auto-Approve & Task Tracking | 5 | 0002, 0006, 0015 | Yes | XL |
| 18 | story-0029-0018 | Golden File Regeneration & Integration Tests | 6 | ALL prior stories | Yes | L |

> **Critical Path Legend:** `Yes` = story is on the critical path (delay impacts epic deadline); `No` = story has slack.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 18/18 story files found |
| Dependencies resolved | PASS | All dependency references resolve to existing stories |
| Circular dependencies | PASS | No circular dependencies detected |
| Implementation map valid | PASS | implementation-map-0029.md parsed successfully |

No pre-flight issues detected. All stories have valid dependency chains.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Max parallel subagents | 7 | Phase 0 launches 7 worktree agents |
| Peak memory estimate | 14-28 GB | 7 parallel subagents x 2-4 GB each |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| story-0029-0015 bottleneck (6 deps, XL size) | High | Likely | Prioritize critical path stories; ensure quality in upstream deps |
| Parallel Phase 0 memory pressure (7 agents) | Medium | Possible | Monitor system memory; use --sequential if needed |
| story-0029-0018 golden file drift | Medium | Possible | Run full regeneration and byte-for-byte comparison |
| Cross-story conflicts in SKILL.md files | Low | Possible | Pre-flight advisory warnings; auto-rebase after PR merge |

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | Per-story completion |
| Save on phase completion | Yes |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | plans/epic-0029/execution-state.json |

### Recovery Procedures

1. On interruption: `--resume` flag reloads checkpoint, reclassifies IN_PROGRESS to PENDING
2. On story failure: retry up to MAX_RETRIES (2), then mark FAILED and propagate blocks
3. On integrity gate failure: pause for operator decision

### Resume Behavior

Resume reclassifies story statuses: IN_PROGRESS -> PENDING, FAILED (retries < 2) -> PENDING, SUCCESS preserved. BLOCKED stories reevaluated based on dependency status.
