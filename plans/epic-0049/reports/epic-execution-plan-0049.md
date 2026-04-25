# Epic Execution Plan — EPIC-0049

> **Epic ID:** EPIC-0049
> **Title:** Refatoração do Fluxo de Épico
> **Date:** 2026-04-22
> **Total Stories:** 22
> **Total Phases:** 4
> **Mode:** execute
> **Merge Mode:** auto (`--auto-merge`)
> **Parallelism:** sequential (`--sequential`)
> **Base Branch:** develop
> **Template Version:** inline-fallback

## Configuration

| Flag | Value |
| --- | --- |
| `--sequential` | true |
| `--auto-merge` | true |
| `--skip-review` | false |
| `--single-pr` | false |
| `--auto-approve-pr` | false |
| `--skip-pr-comments` | false |
| `--strict-overlap` | false |

## Story Execution Order (Phase-by-Phase, Sequential Within Phase)

### Phase 0 — Primitives Git/PR + Standalone Internals (10 stories)

| Order | Story ID | Title | Dependencies | Status |
| --- | --- | --- | --- | --- |
| 1 | story-0049-0001 | x-git-branch | — | Pending |
| 2 | story-0049-0002 | x-git-merge | — | Pending |
| 3 | story-0049-0003 | x-pr-merge | — | Pending |
| 4 | story-0049-0004 | x-planning-commit | — | Pending |
| 5 | story-0049-0005 | x-internal-status-update (PILOTO) | — | Pending |
| 6 | story-0049-0006 | x-internal-report-write | — | Pending |
| 7 | story-0049-0007 | x-internal-args-normalize | — | Pending |
| 8 | story-0049-0011 | x-internal-story-load-context | — | Pending |
| 9 | story-0049-0012 | x-internal-story-build-plan | — | Pending |
| 10 | story-0049-0014 | x-internal-story-verify | — | Pending |

### Phase 1 — Composites + Extensions + Rules (8 stories)

| Order | Story ID | Title | Dependencies | Status |
| --- | --- | --- | --- | --- |
| 11 | story-0049-0008 | x-internal-epic-branch-ensure | 0001 | Pending |
| 12 | story-0049-0009 | x-internal-epic-build-plan | 0006 | Pending |
| 13 | story-0049-0010 | x-internal-epic-integrity-gate | 0006 | Pending |
| 14 | story-0049-0013 | x-internal-story-resume | 0005 | Pending |
| 15 | story-0049-0015 | x-internal-story-report | 0006 | Pending |
| 16 | story-0049-0016 | Extensão x-pr-create | 0003 | Pending |
| 17 | story-0049-0017 | Extensão x-task-plan (--no-commit) | — | Pending |
| 18 | story-0049-0020 | Rules 09/14/19/20/21 | — | Pending |

### Phase 2 — Critical Refactors (2 stories)

| Order | Story ID | Title | Dependencies | Status |
| --- | --- | --- | --- | --- |
| 19 | story-0049-0018 | Refator x-epic-implement (CRÍTICA) | 0005, 0007, 0008, 0009, 0010, 0016 | Pending |
| 20 | story-0049-0019 | Refator x-story-implement (CRÍTICA) | 0005, 0007, 0011, 0012, 0013, 0014, 0015, 0016 | Pending |

### Phase 3 — Planning Skills Versioning (2 stories)

| Order | Story ID | Title | Dependencies | Status |
| --- | --- | --- | --- | --- |
| 21 | story-0049-0021 | VCS: x-epic-create/decompose/map | 0004, 0008 | Pending |
| 22 | story-0049-0022 | VCS: x-epic-orchestrate/story-create/plan/task-plan | 0004, 0008, 0017 | Pending |

## Critical Path

S6 (report-write) → S9 (epic-build-plan) / S10 (epic-integrity) → S18 (refator x-epic-implement) → FIM

S3 (pr-merge) → S16 (pr-create ext) → S18 & S19

## Integrity Gates

| Phase | Gate Type | Trigger |
| --- | --- | --- |
| 0 | Compile + Test + Coverage + Smoke | After all 10 stories merged to develop |
| 1 | Compile + Test + Coverage + Smoke | After all 8 stories merged to develop |
| 2 | Compile + Test + Coverage + Smoke | After both critical refactors merged |
| 3 | Compile + Test + Coverage + Smoke | After both versioning stories merged |

Each gate MUST PASS before advancing to the next phase (EPIC-0042: smoke gate is mandatory).

## Notes

- All 22 stories are platform-level (no external-domain blockers).
- Story 0049-0020 (Rules) can be executed at any time; listed in Phase 1 for grouping.
- Stories 0049-0018 and 0049-0019 are the critical bottlenecks: 6 and 8 upstream deps respectively.
- With `--sequential`, Phase 0.5 preflight conflict analysis is skipped.
- With `--auto-merge`, story PRs are auto-merged after reviews approve; dependent stories wait for `prMergeStatus == MERGED` before dispatch.
- After each phase integrity gate passes, an automatic semantic-version bump runs on `develop` (RULE-013).
