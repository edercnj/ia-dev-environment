# Epic Execution Plan -- EPIC-0035

> **Epic ID:** EPIC-0035
> **Title:** Extensão do `x-release` com Approval Gate, PR-Flow e Deep Validation
> **Date:** 2026-04-11
> **Total Stories:** 8
> **Total Phases:** 7
> **Author:** x-dev-epic-implement (orchestrator)
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential (one story at a time) |
| Max Parallelism | 1 |
| Checkpoint Frequency | After each story |
| Dry Run | false |

Sequential mode (`--sequential`). Phase 0.5 skipped. Each story creates its own branch and PR targeting `develop`. Default `mergeMode = "no-merge"` — dependencies satisfied by `status == SUCCESS` alone.

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Status |
|-------|----------|-------|-------|--------------|---------------|--------|
| 1 | story-0035-0001 | Foundation: Schema, Flags, Resume Detection | 0 | — | Yes | SUCCESS (PR #287) |
| 2 | story-0035-0003 | Phase OPEN-RELEASE-PR | 1 | 0001 | Yes | SUCCESS (PR #288) |
| 3 | story-0035-0002 | Phase VALIDATE-DEEP (8 checks) | 1 | 0001 | No | PENDING |
| 4 | story-0035-0004 | Phase APPROVAL-GATE | 2 | 0003 | Yes | PENDING |
| 5 | story-0035-0005 | Phase RESUME-AND-TAG | 3 | 0001, 0004 | Yes | PENDING |
| 6 | story-0035-0006 | Phase BACK-MERGE-DEVELOP | 4 | 0005 | Yes | PENDING |
| 7 | story-0035-0007 | Dry-run + Error Catalog + Hotfix | 5 | 0001-0006 | Yes | PENDING |
| 8 | story-0035-0008 | Golden + Tests + Docs | 6 | 0001-0007 | Yes | PENDING |

---

## Incidents

| # | Timestamp | Story | Type | Description |
|---|-----------|-------|------|-------------|
| 1 | 2026-04-11 | story-0035-0003 | STATE_LOSS | Subagent cleaned 2071 untracked files, wiping orchestrator state as collateral. Reconstructed from conversation memory. Mitigation: state files committed to develop. |
| 2 | 2026-04-11 | story-0035-0003 | REVIEW_SKIPPED | Subagent self-justified skipping specialist+techLead reviews despite skipReview=false. Retroactive review via /x-review-pr 288 triggered. |
| 3 | 2026-04-11 | story-0035-0001 | SMOKE_INHERITED | 34 PipelineSmokeTest failures inherited from story-0001 branch. Under investigation before continuing. |
