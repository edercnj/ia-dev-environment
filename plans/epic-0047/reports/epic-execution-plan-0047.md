# Epic Execution Plan -- EPIC-0047

> **Epic ID:** EPIC-0047
> **Title:** Skill Body Compression Framework
> **Date:** 2026-04-21
> **Total Stories:** 4
> **Total Phases:** 2
> **Author:** x-epic-implement (orchestrator)
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential (one story at a time, dependency-ordered) |
| Max Parallelism | 1 (forced by `--sequential`) |
| Checkpoint Frequency | After each story status transition |
| Dry Run | **true** — no subagents dispatched; no PRs created |

**Invocation:** `/x-epic-implement epic-0047 --sequential --auto-merge --dry-run`

**Derived flags:**
- `mergeMode = "auto"` (from `--auto-merge`, default per EPIC-0042)
- `parallel = false` (from `--sequential`)
- `singlePr = false` (per-story PRs, not legacy mega-PR)
- `skipReview = false` (per-story tech lead review executes)
- `autoApprovePr = false` (task PRs target `develop`, not a parent branch)
- `batchApproval = true` (no-op in sequential mode — no concurrent PRs to consolidate)
- `taskTracking = true` (task-level fields tracked in `execution-state.json`)
- `planningSchemaVersion = "2.0"` (task-first, per epic DoR §3)

**Notes:**
- `--sequential` forces one-story-at-a-time dispatch; Phase 0.5 (pre-flight conflict analysis) is skipped.
- `--auto-merge` auto-merges each story PR via `gh pr merge {N} --merge` after reviews approve; on merge the orchestrator auto-rebases remaining open PRs in the phase (Section 1.4e).
- Version bump runs post-integrity-gate for each phase (RULE-013). With 4 stories creating diverse changes, at least one MINOR bump per phase is likely (`feat:` commits).

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Foundation + Guard-rail | story-0047-0001, story-0047-0003 | Sequential (1×1) | ~5 dias úteis | Bucket A mergeado + Sprint 2 medição (externas) |
| 1 | Conteúdo (Flip + KP sweep) | story-0047-0002, story-0047-0004 | Sequential (1×1) | ~12 dias úteis | story-0047-0001 merged |

> **Total estimated duration:** ~15–18 dias úteis (scale IMPL-MAP §3) após precondições externas resolvidas.

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0047-0001 | Diretório `_shared/` + ADR-0006 (estratégia de inclusão) | 0 | — | **Yes** (gargalo) | L |
| 2 | story-0047-0003 | CI lint `SkillSizeLinter` (limite 500 LoC + `references/` sibling) | 0 | — | No (paralela) | M |
| 3 | story-0047-0002 | Retirar pattern Slim Mode + ADR-0007 (flipped orientation) | 1 | story-0047-0001 (MERGED) | **Yes** | XL |
| 4 | story-0047-0004 | Sweep de compressão dos 5 maiores knowledge packs | 1 | story-0047-0001 (MERGED) | No | XL |

> **Ordering inside phase:** sequential dispatch respects `sortByCriticalPath()`. Story 0047-0001 is dispatched before 0047-0003 in Phase 0 because it's on the critical path. Story 0047-0002 is dispatched before 0047-0004 in Phase 1 for the same reason.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

---

## Per-Story Dispatch Preview

### Order 1 — story-0047-0001 (Phase 0)

- **Branch:** `feat/story-0047-0001-*` (created by `x-story-implement` Phase 0)
- **Base:** `develop`
- **Worktree:** OPTIONAL under `--sequential` (Section 1.4). Default: run on main checkout.
- **DoR pre-check:** `plans/epic-0047/plans/dor-story-0047-0001.md` — verdict READY (from planning checkpoint).
- **Status:** PENDING → IN_PROGRESS → SUCCESS → PR_CREATED → PR_MERGED (auto-merge)
- **Markdown sync:** story file + IMPLEMENTATION-MAP row updated to Concluída on SUCCESS.

### Order 2 — story-0047-0003 (Phase 0)

- **Branch:** `feat/story-0047-0003-*`
- **Base:** `develop` (updated after story-0047-0001 auto-merged)
- **DoR pre-check:** `plans/epic-0047/plans/dor-story-0047-0003.md` — verdict READY.
- **Dispatched after:** order 1 completes SUCCESS + auto-merge.
- **Note:** 0003 has no dependency on 0001, but sequential mode orders it after 0001 per critical path priority.

### Phase 0 boundary

- **Integrity gate** on `develop` (compile + test + coverage ≥95%/90% + smoke).
- **Version bump** (RULE-013): likely MINOR if `feat:` commits from 0001 or 0003; PATCH for `fix:` only; NONE if only chores/docs.
- **Phase completion report** written to `plans/epic-0047/reports/phase-0-completion-0047.md`.

### Order 3 — story-0047-0002 (Phase 1)

- **Branch:** `feat/story-0047-0002-*`
- **Base:** `develop` (after Phase 0 merged + version bumped)
- **DoR pre-check:** `plans/epic-0047/plans/dor-story-0047-0002.md` — verdict READY.
- **Dependencies:** story-0047-0001 must have `status == SUCCESS AND prMergeStatus == MERGED` (enforced by `getExecutableStories()` under `mergeMode=auto`).
- **Risk:** XL effort (5 skills flipped to slim + full-protocol carve-out); high likelihood of golden file regeneration.

### Order 4 — story-0047-0004 (Phase 1)

- **Branch:** `feat/story-0047-0004-*`
- **Base:** `develop` (after story-0047-0002 auto-merged + auto-rebase applied)
- **DoR pre-check:** `plans/epic-0047/plans/dor-story-0047-0004.md` — verdict READY.
- **Risk:** XL effort (5 KPs with code examples migrated to `references/`); high file overlap with 0002 possible (both touch `skills/**`) — auto-rebase of 0004 is expected after 0002 merges.

### Phase 1 boundary

- Integrity gate + version bump + phase completion report (same as Phase 0).

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | OK | 4 story files discovered (`story-0047-0001..0004.md`) |
| Dependencies resolved | OK | DAG: {0001, 0003} → Phase 0; {0002, 0004} → Phase 1 (both depend on 0001) |
| Circular dependencies | OK | None detected |
| Implementation map valid | OK | `IMPLEMENTATION-MAP.md` parses cleanly; totalPhases=2 |
| Phase 0.5 gate | SKIPPED | `--sequential` mode skips pre-flight conflict analysis (no parallel dispatch) |
| DoR files | OK | All 4 stories have `dor-story-*.md` with verdict READY (from `x-epic-orchestrate` checkpoint dated 2026-04-21) |
| Task plans | OK | All 4 stories have `tasks-story-*.md` and `planning-report-story-*.md` (PRE_PLANNED mode eligible) |
| External preconditions | **REQUIRES HUMAN CONFIRMATION** | Epic DoR §3 requires Bucket A of `mellow-mixing-rainbow.md` merged + Sprint 2 measurement run before 0002 executes. **Not auto-verifiable by orchestrator.** |

**Checkpoint migration notice:** Current `execution-state.json` uses the `x-epic-orchestrate` schema (`storyEntries` with `planningStatus`). On real execution (`--dry-run` removed), Phase 1.1 (`createCheckpoint`) will overwrite it with the `x-epic-implement` schema (`stories` map with `status`, `prMergeStatus`, etc.), preserving only `epicId`, `baseBranch`, and story→phase mapping. The planning artifacts (`tasks-*.md`, `dor-*.md`) are untouched.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | ~800k–1.4M | 4 `x-story-implement` invocations; each loads SKILL.md bodies + reads per-story tasks + TDD cycles. Number inflated by this epic's own thesis (large skill bodies). |
| Estimated wall time | ~3–5 hours | Assumes each story completes in 45–75min (TDD + review + PR + auto-merge). CI wait + review cycles dominate. |
| Max parallel subagents | 1 | `--sequential` forces single-story dispatch |
| Peak memory estimate | Low | Orchestrator runs on main checkout; no simultaneous worktrees |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Bucket A (external) not merged → 0002 baseline invalid | **Critical** | Possible | Human confirmation required BEFORE removing `--dry-run` |
| Sprint 2 measurement not captured → flip decision is a guess | High | Possible | §6 of `epic-0047.md` must contain baseline before 0002 starts |
| Golden file regen drift (`mvn process-resources` stale) | High | Likely | Stories 0002/0004 MUST call `mvn process-resources` before `GoldenFileRegenerator` (from memory: `feedback_mvn_process_resources_before_regen.md`) |
| Auto-merge conflicts with parallel work outside epic | Medium | Possible | `--auto-merge` uses `gh pr merge --merge` with force-with-lease rebase; falls back to polling on conflict |
| Subagent scope creep (push outside worktree) | High | Possible | **Project memory warning:** orchestrator MUST verify main tree state post-dispatch; add SCOPE LOCK to subagent prompts |
| Agent worktree isolation leak | High | Possible | **Project memory warning:** using explicit `x-git-worktree create/remove` (ADR-0004) — NOT the deprecated `Agent(isolation:"worktree")` harness param |
| Phase-0 integrity gate smoke failure | Medium | Possible | Smoke gate mandatory (EPIC-0042). Operator resumes via `--resume` after fixing. |
| `SkillSizeLinter` (0003) accidentally fails CI on own repo skills | Medium | Likely | Story 0003 must ship baseline exemption or ensure all existing `SKILL.md` files already under 500 LoC before merging to develop |
| Coverage regression on new Java helpers (0001 `SnippetIncluder`, 0003 `SkillSizeLinter`) | Low | Possible | Per-story Phase 2.5 coverage gate (≥95%/≥90%) blocks SUCCESS if missed |
| Epic duration (~15–18 days) causes `develop` to drift significantly | Medium | Likely | Auto-rebase Phase 1 PRs after Phase 0 merges; re-verify DoR before Phase 1 dispatch |

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | After every story status transition (PENDING → IN_PROGRESS → SUCCESS/FAILED) |
| Save on phase completion | Yes (integrity gate result + version bump recorded) |
| Save on story completion | Yes (commitSha, prUrl, prNumber, prMergeStatus, coverage) |
| Save on integrity gate failure | Yes (failedTests, regressionSource, smokeGate details) |
| State file location | `plans/epic-0047/execution-state.json` |

### Recovery Procedures

1. **Story failure within phase:** orchestrator retries up to `MAX_RETRIES=2`; on exhaustion, marks FAILED and triggers block propagation for dependents. In this epic, a FAILED 0001 blocks 0002 and 0004.
2. **Integrity gate regression identified:** agent-assisted fix attempted first (EPIC-0042); on failure, `git revert {commitSha}` + mark story FAILED. Use `--revert-on-failure` to skip the fix attempt.
3. **Integrity gate smoke failure:** phase marked FAILED; NO automatic bypass (EPIC-0042 removed `--skip-smoke-gate`). Operator fixes smoke tests on `develop` then re-runs with `--resume`.
4. **Auto-merge conflict on PR:** fall through to 60s poll, 24h timeout. On timeout: dependents marked BLOCKED.
5. **Circuit breaker:** 3 consecutive story failures pauses execution with `AskUserQuestion`; 5 total failures in phase aborts phase and marks remaining stories BLOCKED.

### Resume Behavior

Re-run with `--resume` to reclassify statuses (IN_PROGRESS→PENDING, verify PR state via `gh pr view`, reevaluate BLOCKED) and continue from first PENDING story. `mergeMode` in checkpoint is respected; if flags change on resume, a warning is logged.

---

## Human Confirmation Required Before Real Execution

Before re-running without `--dry-run`, confirm:

1. **Bucket A of `mellow-mixing-rainbow.md` merged** into `develop` + `v3.7.0` cut? (Epic DoR §3 hard precondition)
2. **Sprint 2 measurement captured** — token delta of `/x-epic-orchestrate`, `/x-story-implement`, `/x-release` documented in §6 of `epic-0047.md`? (Required before STORY-0047-0002 executes)
3. **Working tree clean** — the currently tracked modification on `plans/epic-0051/telemetry/events.ndjson` either committed or stashed so it does not contaminate subagent contexts.
4. **Repo-scope lock** — subagents MUST run within their designated branch/worktree; verify main tree state post-dispatch (project memory).

---

## Out of Scope (per `epic-0047.md` §1)

- Runtime lazy-load on `Skill()` call (blocked by Rule 13)
- Inter-file chunking (Bucket C of the plan)
- `context-budget` enforcement (deferred to EPIC-0048)
- Template-vars compression `{{COMPILE_COMMAND}}` etc. (deferred to EPIC-0049)
