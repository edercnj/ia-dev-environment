# Epic Execution Plan -- EPIC-0050

> **Epic ID:** EPIC-0050
> **Title:** Model Selection Enforcement & Token Optimization
> **Date:** 2026-04-23
> **Total Stories:** 10
> **Total Phases:** 5 (Phase 0..Phase 4)
> **Author:** x-epic-implement (thin orchestrator, dry-run)
> **Template Version:** _TEMPLATE-EPIC-EXECUTION-PLAN.md
> **flowVersion:** 2 (new EPIC-0049 defaults)

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential (EPIC-0049 new default) |
| Max Parallelism | 1 (phase-sequential, story-sequential) |
| Checkpoint Frequency | On every story transition (`x-internal-status-update`) |
| Dry Run | **YES** (this plan was produced with `--dry-run`; no side effects) |

Opt-in for higher throughput: re-invoke with `--parallel` to run all 6 Phase-1 stories (S2, S3, S4, S5, S7, S8) concurrently via worktrees. With `--parallel` the wall-time estimate drops from ~9-10 days of sequential work to ~4-5 days (IMPLEMENTATION-MAP §5). Your project-memory entry `feedback_subagent_scope_creep_push.md` applies to the parallel path — every worktree dispatch must carry explicit SCOPE LOCK.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Governança (Foundation) | S1 | 1 | 1 day | — |
| 1 | Enforcement Técnico | S2, S3, S4, S5, S7, S8 | up to 6 (parallel-capable) | 1-2 days parallel / 6 days sequential | Phase 0 complete |
| 2 | Padrão estendido de Agent() | S6 | 1 | 0.5 day | S5 complete |
| 3 | CI Audit Enforcement | S9 | 1 | 1-2 days | S2..S8 complete |
| 4 | Medição Pós-Deploy | S10 | 1 | 1 day + wait | S9 complete + EPIC-0040 merged |

> **Total estimated duration:** ~9-10 days sequential / ~4-5 days with `--parallel` (IMPLEMENTATION-MAP.md §5).

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0050-0001 | Rule nova — Model Selection Strategy (foundation) | 0 | — | **Yes** | M |
| 2 | story-0050-0002 | Frontmatter `model: sonnet` em 4 orquestradores pesados | 1 | S1 | No | S |
| 3 | story-0050-0003 | Frontmatter `model: sonnet` em 4 orquestradores secundários | 1 | S1 | No | S |
| 4 | story-0050-0004 | Frontmatter `model: haiku` em 10 skills utilitárias e KPs | 1 | S1 | No | M |
| 5 | story-0050-0005 | `Agent(...)` com `model:` em x-story-plan (5 subagents) | 1 | S1 | **Yes** | M |
| 6 | story-0050-0007 | `Skill(...)` com `model:` param em orquestradores | 1 | S1 | No | M |
| 7 | story-0050-0008 | Agent metadata determinístico (substituir Adaptive) | 1 | S1 | No | S |
| 8 | story-0050-0006 | `Agent(...)` com `model:` em x-arch-plan + x-test-plan | 2 | S5 | **Yes** | S |
| 9 | story-0050-0009 | CI audit script de model selection | 3 | S2..S8 | **Yes** | M |
| 10 | story-0050-0010 | Medição pós-deploy via telemetria (EPIC-0040) | 4 | S9 | **Yes** | S |

> **Critical Path:** `S1 → S5 → S6 → S9 → S10` (5 sequential steps). Stories S2, S3, S4, S7, S8 have slack — a delay in any of them only matters if S9 blocks on the last of the group.
> **Sequential dispatch order above** respects phase boundaries and S5→S6 intra-phase dependency; within Phase 1 the order S2→S3→S4→S5→S7→S8 is advisory, and all six are independently dispatchable once S1 merges.

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 10 of 10 stories under `plans/epic-0050/` |
| Dependencies resolved | PASS | IMPLEMENTATION-MAP.md §1 matrix matches story frontmatter `Blocked By` fields |
| Circular dependencies | PASS | DAG acyclic (Kahn's algorithm applied on §1 matrix) |
| Implementation map valid | PASS | `plans/epic-0050/IMPLEMENTATION-MAP.md` present, 206 lines, includes DAG + Mermaid + critical path |
| Epic directory exists | PASS | `plans/epic-0050/` with epic, 10 stories, IMPLEMENTATION-MAP |
| Execution state | FRESH RUN | `plans/epic-0050/execution-state.json` absent → new run under flowVersion=2 |
| `epic/0050` branch | ABSENT | Neither local nor on origin — `x-internal-epic-branch-ensure` will create during Phase 2 |
| Current branch | `develop` | Clean base for branch creation |

Coordination caveat (from IMPLEMENTATION-MAP §5 note 1): **RESOLVIDO** em 2026-04-23 — EPIC-0049 totalmente mergeado (PR #421 planning + PR #582 refactor x-epic-implement + PR #583 thin orchestrator x-story-implement). Os dois alvos já estão no formato "thin orchestrator" no `develop`, portanto S2 e S7 não terão conflito de integração com EPIC-0049.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens (orchestrator only) | ~300k-600k | 10× `x-story-implement` dispatch × ~30-60k per story TDD run. Your memory `feedback_skill_body_token_cost.md` applies: every `Skill()` call re-injects the full sub-skill body. |
| Estimated wall time | ~9-10 days sequential / ~4-5 days parallel | Human-paced effort; Claude wall time is a fraction when no review is requested. |
| Max parallel subagents | 5 (bound by `x-story-plan` inside each story) | Phase-level story parallelism is separate (up to 6 under `--parallel`). |
| Peak memory estimate | N/A (stateless) | The generator is stateless; each story dispatch is a fresh worktree or sequential checkout. |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| ~~EPIC-0049 PR #421 merge conflict on `x-epic-implement` / `x-story-implement` (S2, S7)~~ | ~~High~~ | **RESOLVIDO (2026-04-23)** | EPIC-0049 100% mergeado (PRs #421 planning, #582 x-epic-implement refactor, #583 x-story-implement thin orchestrator). Risco eliminado — base segura para S2 e S7. |
| Golden-file regeneration missed after SKILL.md edits (per story) | Medium | Likely | IMPLEMENTATION-MAP §5 note 2: `mvn process-resources && mvn test -Dtest=GoldenFileRegenerator -Dgolden.regenerate=true` mandatory after every story. Your memory `feedback_mvn_process_resources_before_regen.md` applies. |
| Subagent scope creep during parallel dispatch (S2..S8) | High | Possible under `--parallel` | Your memory `feedback_subagent_scope_creep_push.md` applies — SCOPE LOCK prompt is required in every Agent() dispatch when `--parallel` is set. Sequential default avoids this risk. |
| S9 CI audit merged before S2..S8 breaks every new PR | Critical | Very Likely if dispatch order violated | DAG gating strictly enforces S2..S8 → S9; audit re-check in Phase 3 before S9 PR auto-merges. |
| S10 (post-deploy) runs with stale telemetry | Medium | Possible | IMPLEMENTATION-MAP §5 note 3: S10 requires 2 real epic executions post-merge before telemetry delta is meaningful. Do not run S10 same-day as S9 merge. |
| Cost overrun from Opus-level re-injection of 10 story runs | Medium | Likely | The very optimization this epic ships (model selection) reduces this cost ~22% per future run. Acceptable one-time investment. |

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | On every story transition (PENDING → IN_PROGRESS → PR_CREATED → SUCCESS/FAILED) |
| Save on phase completion | Yes (phase-level `completedAt` timestamp) |
| Save on story completion | Yes (each `x-story-implement` final envelope → `x-internal-status-update`) |
| Save on integrity gate failure | Yes (Phase 4 gate envelope persisted before retry/exit) |
| State file location | `plans/epic-0050/execution-state.json` |

### Recovery Procedures

- **Transient story failure:** re-invoke `/x-epic-implement 0050 --resume` — the orchestrator reads the state file, skips `SUCCESS` stories, and re-dispatches the first `PENDING`/`IN_PROGRESS`.
- **Integrity gate failure (Phase 4):** Phase 4 auto-dispatches a regression-fix agent and re-runs the gate once. On second failure → exit `INTEGRITY_GATE_FAILED` and the plan stops; manual fix + `--resume` required.
- **Final PR conflict (Phase 5.1):** `x-git-merge develop → epic/0050` conflict → orchestrator exits `FINAL_PR_CONFLICTS` with a remediation block. Resolve locally, push, then `--resume`.
- **Branch lost / corrupted:** `plans/epic-0050/execution-state.json` retains `flowVersion=2`; re-running re-enters the plan with all merged stories already marked `SUCCESS` (idempotent).

### Resume Behavior

- `flowVersion` is read from `execution-state.json`; on this first run it will be written as `"2"`.
- `--resume` auto-detects `flowVersion` and emits a visible warning if `"1"` (legacy) is found — not applicable here.
- PR-status reverification: on resume, `x-internal-epic-build-plan` fetches each story PR state and reclassifies (`PR_CREATED → SUCCESS` when merged, `IN_PROGRESS → PENDING` when branch lost).

---

## Dry-Run Summary (this invocation)

| Phase | Status | Side effects |
|-------|--------|--------------|
| Phase 0 — Args | Executed | Args parsed: epicId=0050, flowVersion=2, all flags at default. No file I/O. |
| Phase 1 — Plan | Executed (dry-run short-circuit) | This document written to `plans/epic-0050/reports/epic-execution-plan-0050.md`. No state file. No branch created. |
| Phase 2 — Branch setup | **SKIPPED** (dry-run) | `epic/0050` would be created from `develop` and pushed. |
| Phase 3 — Execution loop | **SKIPPED** (dry-run) | 10 story dispatches would run. |
| Phase 4 — Integrity gate | **SKIPPED** (dry-run) | Gate + epic-execution report render would run. |
| Phase 5 — Final PR | **SKIPPED** (dry-run) | Final `epic/0050 → develop` PR would be opened. |

---

## How to proceed

- **Execute the full run:** `/x-epic-implement 0050` (sequential, new default). Expect multi-hour wall time.
- **Execute in parallel (faster, higher risk):** `/x-epic-implement 0050 --parallel`. Review SCOPE LOCK implications first.
- **Execute one story as pilot:** `/x-epic-implement 0050 --story story-0050-0001`. Confirms the orchestrator path on the foundation story before committing to the rest.
- **Execute a single phase:** `/x-epic-implement 0050 --phase 1` (for example) — advanced; requires Phase 0 (S1) already merged.

No action has been taken by this dry-run. The epic directory, stories, and `develop` branch are untouched.
