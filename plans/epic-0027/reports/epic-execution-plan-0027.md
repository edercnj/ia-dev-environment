# Epic Execution Plan -- EPIC-0027

> **Epic ID:** EPIC-0027
> **Title:** Migração para Git Flow Branching Model
> **Date:** 2026-04-07
> **Total Stories:** 10
> **Total Phases:** 5
> **Author:** Epic Orchestrator (x-dev-epic-implement)
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Per-story PR (each story creates its own PR targeting `main`) |
| Max Parallelism | 6 (Phase 1) |
| Checkpoint Frequency | After each story completion |
| Dry Run | No |

Merge mode: **interactive** (default). At phase boundaries, the orchestrator prompts for merge decisions. Stories execute in parallel within each phase (default behavior). No `--single-pr`, `--sequential`, or `--strict-overlap` flags set.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Dependencies |
|-------|------|---------|-------------|--------------|
| 0 | Foundation | 1 | 1 | — |
| 1 | Core + Extensions | 6 | 6 (parallel) | Phase 0 complete |
| 2 | Lifecycle Integration | 1 | 1 | story-0027-0002 (Phase 1) |
| 3 | Epic Orchestrator | 1 | 1 | Phase 2 complete |
| 4 | Cross-Cutting Validation | 1 | 1 | Phases 1-3 complete |

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0027-0001 | Definição da Regra de Branching Model | 0 | — | Yes | M |
| 2 | story-0027-0002 | x-git-push — Develop como Base Default | 1 | story-0027-0001 | Yes | M |
| 3 | story-0027-0005 | x-release — Workflow de Release Branch | 1 | story-0027-0001 | No | L |
| 4 | story-0027-0006 | x-ci-cd-generate — Triggers Multi-Branch | 1 | story-0027-0001 | No | M |
| 5 | story-0027-0007 | x-fix-epic-pr-comments — Develop Base Branch | 1 | story-0027-0001 | No | S |
| 6 | story-0027-0008 | Release Management KP — Git Flow como Default | 1 | story-0027-0001 | No | S |
| 7 | story-0027-0009 | Configuração de Branching Model no YAML | 1 | story-0027-0001 | No | L |
| 8 | story-0027-0003 | x-dev-lifecycle — Integração com Develop | 2 | story-0027-0002 | Yes | M |
| 9 | story-0027-0004 | x-dev-epic-implement — Develop Base e No-Merge Default | 3 | story-0027-0003 | Yes | XL |
| 10 | story-0027-0010 | Testes de Integração e Golden Files | 4 | story-0027-0002..0009 | Yes | XL |

> **Critical Path Legend:** `Yes` = story is on the critical path (delay impacts epic deadline); `No` = story has slack.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 10/10 story files found |
| Dependencies resolved | PASS | All dependencies are satisfiable within the epic |
| Circular dependencies | PASS | No circular dependencies detected |
| Implementation map valid | PASS | 5 phases, 10 stories, valid DAG |

No pre-flight issues detected. All stories have valid dependency chains.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | High | 10 stories across 5 phases |
| Max parallel subagents | 6 | Phase 1 dispatches 6 worktree subagents |
| Peak memory estimate | 12-24 GB | 6 parallel Node.js subagents (2-4 GB each) |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Phase 1 parallel stories editing shared config files (pom.xml, profile templates) | High | Likely | Auto-rebase after each PR merge resolves conflicts |
| story-0027-0009 (YAML config) has broad impact across all assemblers | High | Possible | Scheduled in Phase 1 to surface issues early; story-0027-0010 validates |
| story-0027-0004 (epic orchestrator) is XL with 15+ reference changes | Medium | Possible | Sequential in Phase 3, full context available from prior stories |
| Golden file updates in story-0027-0010 may cascade failures | Medium | Likely | Phase 4 runs last, all other changes stable |

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | After each story completion |
| Save on phase completion | Yes |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | plans/epic-0027/execution-state.json |

### Recovery Procedures

If execution is interrupted, use `--resume` to continue from the last checkpoint. The orchestrator reclassifies IN_PROGRESS stories as PENDING and verifies PR statuses via `gh pr view`.

### Resume Behavior

On resume: IN_PROGRESS → PENDING, SUCCESS preserved, FAILED retried (up to 2 retries), BLOCKED reevaluated based on dependency status. PR statuses verified via GitHub CLI.
