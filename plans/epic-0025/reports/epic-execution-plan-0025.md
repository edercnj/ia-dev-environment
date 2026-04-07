# Epic Execution Plan -- EPIC-0025

> **Epic ID:** EPIC-0025
> **Title:** Platform Target Filter — Geração Seletiva por Plataforma de IA
> **Date:** 2026-04-06
> **Total Stories:** 8
> **Total Phases:** 4
> **Author:** Epic Implementation Orchestrator
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Per-story PR (each story creates its own PR targeting main) |
| Max Parallelism | 4 (Phase 2 has 4 independent stories) |
| Checkpoint Frequency | After each story completion |
| Dry Run | No |

Default parallel worktree dispatch. Stories within the same phase execute concurrently via isolated worktrees. Each story creates its own branch and PR targeting `main`.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Dependencies |
|-------|------|---------|-------------|--------------|
| 0 | Foundation (Domain Model) | story-0025-0001 | 1 | — |
| 1 | Core (Application/Pipeline) | story-0025-0002 | 1 | Phase 0 complete |
| 2 | Extensions (CLI, Config, Templates, Display) | story-0025-0003, story-0025-0004, story-0025-0005, story-0025-0006 | 4 parallel | Phase 1 complete |
| 3 | Quality & Documentation | story-0025-0007, story-0025-0008 | 2 parallel | Phase 2 complete |

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0025-0001 | Platform Enum e Mapeamento de Assemblers | 0 | — | Yes | M |
| 2 | story-0025-0002 | Filtro de Assemblers no Pipeline | 1 | story-0025-0001 | Yes | M |
| 3 | story-0025-0003 | Flag CLI `--platform` | 2 | story-0025-0002 | Yes | M |
| 4 | story-0025-0004 | Suporte `platform:` no YAML Config | 2 | story-0025-0002 | Yes | M |
| 5 | story-0025-0005 | Contagem Dinâmica de Artefatos no README e CLAUDE.md | 2 | story-0025-0002 | Yes | M |
| 6 | story-0025-0006 | Verbose e Dry-Run com Awareness de Plataforma | 2 | story-0025-0002 | Yes | M |
| 7 | story-0025-0007 | Atualização de Testes e Golden Files | 3 | story-0025-0003, story-0025-0004, story-0025-0005, story-0025-0006 | Yes | L |
| 8 | story-0025-0008 | Documentação e Help Text | 3 | story-0025-0005 | No | S |

> **Critical Path Legend:** `Yes` = story is on the critical path (delay impacts epic deadline); `No` = story has slack.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 8/8 story files found in plans/epic-0025/ |
| Dependencies resolved | PASS | All dependency references resolve to existing story files |
| Circular dependencies | PASS | No circular dependencies detected |
| Implementation map valid | PASS | implementation-map-0025.md parsed successfully (4 phases, 8 stories) |

All prerequisites validated. Epic is ready for execution.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Max parallel subagents | 4 | Phase 2 launches 4 worktree subagents simultaneously |
| Peak memory estimate | 8-16 GB | 4 parallel Node.js subagents at 2-4 GB each |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| story-0025-0002 design issues propagate to Phase 2 | High | Possible | Extra review on pipeline filter design before Phase 2 starts |
| Phase 2 parallel stories editing shared files (pom.xml, config) | Medium | Likely | Auto-rebase after PR merge resolves config-only conflicts |
| Golden file updates in story-0025-0007 break existing tests | Medium | Possible | Existing golden files preserved (RULE-001); new golden files added alongside |
| 14 profile template updates in story-0025-0004 cause merge conflicts | Low | Possible | Config-only changes are merge-friendly |

> **Severity levels:** Critical, High, Medium, Low.
> **Likelihood levels:** Very Likely, Likely, Possible, Unlikely.

story-0025-0002 is the single bottleneck — it blocks 4 downstream stories and indirectly all remaining work. Quality investment here pays off across the entire epic.

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | After each story completion |
| Save on phase completion | Yes |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | plans/epic-0025/execution-state.json |

### Recovery Procedures

- `--resume` flag reloads execution-state.json
- IN_PROGRESS stories reclassified to PENDING (interrupted work)
- FAILED stories with retries < 2 reclassified to PENDING (retry candidate)
- PR status verified via `gh pr view` before reclassification

### Resume Behavior

- Stories with status SUCCESS are never re-executed
- BLOCKED stories are reevaluated after dependency reclassification
- The orchestrator remains on `main` during resume — no epic branch recovery needed
