# Epic Execution Plan -- EPIC-0034

> **Epic ID:** EPIC-0034
> **Title:** Remoção de Targets Não-Claude do Gerador
> **Date:** 2026-04-10
> **Total Stories:** 5
> **Total Phases:** 5
> **Author:** Epic Orchestrator (x-dev-epic-implement)
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential (forced by map — 1 story per phase) |
| Max Parallelism | 1 |
| Checkpoint Frequency | Per-story (after each story terminal state) |
| Dry Run | YES |
| Merge Mode | no-merge (default — PRs created but not merged automatically) |
| Base Branch | develop |
| Skip Review | false |
| Single PR | false (each story creates its own PR) |

### Execution Strategy Notes

The implementation map declares exactly one story per phase with a strict linear
chain: `story-0034-0001 → 0002 → 0003 → 0004 → 0005`. No stories can run in
parallel even though the `--sequential` flag is not set, because `getExecutableStories()`
will surface only one PENDING story at a time. Phase 0.5 pre-flight conflict
analysis is effectively a no-op (single story per phase cannot overlap with itself).

The map (Section 7 — Observações Estratégicas) explicitly documents that
parallelism was rejected by design under RULE-005 (Atomic Removal per Target):
stories 0001, 0002 and 0003 all edit the same enum hotspots (`Platform.java`,
`AssemblerTarget.java`, `AssemblerFactory.java`, `PlatformConverter.java`,
`FileCategorizer.java`, `OverwriteDetector.java` and 17 `setup-config.*.yaml`
files). Sequential execution produces zero merge conflicts and surgical rollback.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Remoção GitHub Copilot | story-0034-0001 | 1 | 1–2 days | — |
| 1 | Remoção Codex | story-0034-0002 | 1 | 1–2 days | Phase 0 SUCCESS |
| 2 | Remoção Agents Genérico | story-0034-0003 | 1 | 1–2 days | Phase 1 SUCCESS |
| 3 | Higienização Shared Code | story-0034-0004 | 1 | 2–3 days | Phase 2 SUCCESS |
| 4 | Documentação e Verificação Final | story-0034-0005 | 1 | ~1 day | Phase 3 SUCCESS |

> **Total estimated duration:** ~7–12 days of focused work (source: implementation-map-0034.md §3)

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0034-0001 | Remover Suporte a GitHub Copilot | 0 | — | Yes | XL |
| 2 | story-0034-0002 | Remover Suporte a Codex | 1 | story-0034-0001 | Yes | L |
| 3 | story-0034-0003 | Remover Target Agents Genérico | 2 | story-0034-0002 | Yes | L |
| 4 | story-0034-0004 | Higienizar Classes Compartilhadas | 3 | story-0034-0003 | Yes | L |
| 5 | story-0034-0005 | Documentação e Verificação Final | 4 | story-0034-0004 | Yes | M |

> **Critical Path Legend:** All 5 stories are on the critical path (linear chain — a delay anywhere propagates to the end).
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

### Per-Story Scope Summary

| Story | Java Classes Deleted | Test Classes Deleted | Resources | Golden Files | Tasks |
|-------|----------------------|----------------------|-----------|--------------|-------|
| story-0034-0001 | 8 (Github*) | 14 | ~131 | ~2,419 | 6 |
| story-0034-0002 | 7 (Codex*) | 6 | ~15 | ~2,944 | 5 |
| story-0034-0003 | 0–2 (Agents*) | 6 + 1 fixture | 0 (if merged with Copilot/Codex) | ~2,910 | 4 |
| story-0034-0004 | 1 (ReadmeGithubCounter) + edits in 10+ shared | 5 smoke tests edited | 0 | 0 | 6 |
| story-0034-0005 | 0 (docs only) | 0 | regen expected-artifacts.json | 0 | 5 |
| **Total** | **~18** | **~29 + 2 fixtures** | **~146** | **~8,273** | **26** |

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | All 5 story files exist in `plans/epic-0034/` |
| Dependencies resolved | PASS | Linear chain verified; each story blocked by its predecessor |
| Circular dependencies | PASS | DAG is a chain (no cycles possible) |
| Implementation map valid | PASS | `implementation-map-0034.md` parsed; 5 phases, 26 tasks, cross-story deps explicit |
| DoR files present | N/A | `plans/epic-0034/plans/` does not exist — `x-epic-plan` was not run. DoR pre-check will be SKIPPED (backward-compat per RULE-001 of the skill) |
| Architecture plans present | N/A | Same as DoR — no `plan-story-*.md` artifacts. Each story subagent will plan from the story file itself |
| Baseline green | UNVERIFIED | Requires `mvn clean verify` on `develop` before first dispatch (epic DoR item) |
| Coverage baseline recorded | UNVERIFIED | Required by epic DoR (`line`, `branch` snapshot before removal) |
| `resources/shared/templates/` inventory | UNVERIFIED | RULE-004 — protected dir; no story may delete from here |
| `.github/workflows/` exclusion | UNVERIFIED | RULE-003 — CI/CD pipelines must NOT be touched by story-0034-0001 |

### Pre-flight Notes

- The epic is currently marked **`Em Refinamento`** in `epic-0034.md` Section 1 (not `Aprovado`/`Pronto`). Executing an in-refinement epic is a policy deviation.
- The working tree is on `feature/epic-0035-release-branch-flow` with 10 modified files in `.claude/` and untracked `plans/epic-0035/`. The skill default requires starting from a clean `develop`. The orchestrator will fail step 1.2 of the core loop (`git checkout develop && git pull origin develop`) if those changes are not resolved first.
- Several DoR items from `epic-0034.md` Section 3 are unverifiable from the dry-run: baseline coverage snapshot, baseline file count per profile, `ExpectedArtifactsGenerator` availability, green `mvn clean verify` on `develop`.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | 1.5M–2.5M total | Story 0001 alone consumes ~40% due to volume of test deletions and enum edits |
| Estimated wall time | 7–12 working days | Dominated by review loops and CI on per-story PRs |
| Max parallel subagents | 1 (story dispatch) | Sequential chain by design. Phase 0.5 pre-flight is a no-op. |
| Peak memory estimate | Low | Deletions and small edits; no heavyweight codegen. `mvn clean verify` is the peak (~2–4 GB JVM) |

### Notes

- **Token cost driver:** story-0034-0001 deletes ~2,419 golden files and edits 17 YAMLs + 6 shared enum/config classes. Each subagent that reads the golden list into context pays a heavy price. The orchestrator mitigates via `x-dev-story-implement` inline delegation of golden-file work to `general-purpose` subagents with worktree isolation.
- **CI cost driver:** each per-story PR triggers `mvn clean verify` in CI. For 5 PRs this is 5 full builds (~3–6 min each depending on runner).

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Accidental deletion of `.github/workflows/` (CI/CD pipelines) | Critical | Possible | RULE-003 enforcement in story-0034-0001 DoR + explicit grep sanity check at story completion |
| Accidental edit/deletion in `resources/shared/templates/` (Claude templates) | High | Possible | RULE-004 enforcement in story-0034-0004 DoR + review checklist |
| Coverage regression beyond 2pp threshold | High | Likely | JaCoCo report attached to every PR; proportional deletion rule in RULE-002; story-0034-0005 performs final verification |
| Build breakage between stories (RULE-001 violation) | High | Possible | Each story runs `mvn clean verify` as last TDD step before PR; merge blocked on red build |
| Version bump missed before release | Medium | Likely | Integrity gate triggers `x-lib-version-bump` after each phase PASS; `--no-merge` mode defers this, so story-0034-0005 explicitly writes CHANGELOG + bump |
| Golden file regeneration produces drift | Medium | Possible | `ExpectedArtifactsGenerator` is canonical; story-0034-0005 regenerates and diffs |
| Enum edit conflict across parallel work | Low | Unlikely | Sequential execution eliminates this. Only risk is if the user manually parallelizes via `--phase` |
| Non-Claude target users blocked by breaking change | Medium | Likely | Documented in epic §6 (Communication of Breaking Change); CHANGELOG Migration section |
| Orchestrator runs from wrong base branch | High | **Currently Active** | Working tree is on `feature/epic-0035-release-branch-flow` with uncommitted changes; must resolve before real execution |
| Epic marked `Em Refinamento` executed as if Ready | Medium | **Currently Active** | Epic status in `epic-0034.md` should be promoted to `Pronto`/`Aprovado` before dispatch |

### Risk Assessment Notes

Two risks are **currently active** in the repository state (not hypothetical) and
must be resolved before the orchestrator can run real execution:

1. **Wrong base branch + dirty tree:** commit/stash/discard epic-0035 work and
   return to a clean `develop` checkout.
2. **Epic refinement status:** promote `epic-0034.md` status field from
   `Em Refinamento` to a ready state, or explicitly accept the policy deviation.

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | Per-story (after each terminal state) + per-phase integrity gate |
| Save on phase completion | Yes |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | `plans/epic-0034/execution-state.json` |

### Recovery Procedures

If the orchestrator is interrupted (manual stop, crash, CI timeout, circuit
breaker trip), the checkpoint engine persists `execution-state.json` after every
state transition. Recovery is:

1. Inspect `plans/epic-0034/execution-state.json` for the last known story statuses
2. Re-invoke with `/x-dev-epic-implement epic-0034 --resume`
3. The Resume Workflow (SKILL.md Phase 0 step 11) reclassifies statuses:
   - `IN_PROGRESS` → `PENDING` (interrupted work is retried)
   - `SUCCESS` preserved (never re-executed)
   - `FAILED` with `retries < MAX_RETRIES` → `PENDING` (retry candidate)
   - `FAILED` with `retries >= MAX_RETRIES` → stays `FAILED` (manual intervention)
   - `PR_CREATED` / `PR_PENDING_REVIEW` → verified via `gh pr view`; MERGED → SUCCESS
4. BLOCKED stories with all dependencies now SUCCESS are promoted to PENDING
5. The loop re-enters `getExecutableStories()` and dispatches the next ready story

### Resume Behavior

- **Merge mode is preserved** from the original checkpoint; a warning is logged
  if a different mode is passed on `--resume`.
- **Per-task resume (Phase 0 step 11 / Section 1.1c):** if any story has a
  `tasks` object, individual task states are reclassified the same way —
  `DONE` tasks are never re-executed; `IN_PROGRESS` tasks become `PENDING`.
  This epic's stories do not yet have `tasks` objects in the checkpoint because
  no execution has occurred, so per-task resume is a no-op on first run.
- **Rebase tracking:** not applicable to this epic because sequential execution
  means only one story PR is open at a time; `auto-rebase` (Section 1.4e) is a
  no-op when there are no other open PRs in the same phase.
- **Markdown sync on resume:** `story-0034-XXXX.md` Status field and
  `implementation-map-0034.md` Status column are synced on every checkpoint
  update (SKILL.md Section 1.6b).
