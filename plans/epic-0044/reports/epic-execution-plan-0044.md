# Epic Execution Plan — EPIC-0044

> **Epic ID:** EPIC-0044
> **Title:** Remoção de Código Deprecated (`forRemoval = true`)
> **Date:** 2026-04-16
> **Author:** x-epic-implement (orchestrator)
> **Template Version:** inline (RULE-012 fallback — `_TEMPLATE-EPIC-EXECUTION-PLAN.md` not present)
> **Total Stories:** 2
> **Total Phases:** 1
> **Mode:** execute (default — paralelo + auto-merge)
> **Schema Version:** 2.0 (task-first projection — orchestrator delegates task management to `x-story-implement`)
> **Planning Schema (epic-level):** v1 (no `execution-state.json` pre-existing → fallback per Rule 19)

## Flag Resolution

| Flag | Value | Source |
| :--- | :--- | :--- |
| `--phase` | (all) | default |
| `--story` | (all) | default |
| `--skip-review` | false | default |
| `--dry-run` | false | default |
| `--resume` | false | default |
| `--sequential` | false | default (parallel via worktrees) |
| `--single-pr` | false | default (per-story PRs) |
| `--auto-merge` | true | default (EPIC-0042) |
| `--no-merge` | false | default |
| `--interactive-merge` | false | default |
| `--strict-overlap` | false | default (advisory) |
| `--auto-approve-pr` | false | default |
| `--batch-approval` | true | default |
| `--task-tracking` | true | default |
| `--manual-batch-approval` | false | default |
| `--skip-pr-comments` | false | default |
| `--dry-run-only-comments` | false | default |
| `--revert-on-failure` | false | default |
| **Resolved `mergeMode`** | **`auto`** | derived from `--auto-merge` (EPIC-0042 default) |

## Story Execution Order

| Order | Story ID | Phase | Dependencies | Branch (target = `develop`) | Worktree | Status |
| :---: | :--- | :---: | :--- | :--- | :--- | :--- |
| 1 | story-0044-0001 | 0 | — | `feat/story-0044-0001-remove-stackmapping-deprecated` | `.claude/worktrees/story-0044-0001/` | Pending |
| 2 | story-0044-0002 | 0 | — | `feat/story-0044-0002-remove-resolveResourcesRoot` | `.claude/worktrees/story-0044-0002/` | Pending |

**Both stories run in parallel within Phase 0** (no dependencies between them; disjoint files except `CHANGELOG.md` which resolves via trivial rebase per IMPLEMENTATION-MAP §7).

## Per-Story Scope

### story-0044-0001 — Remove StackMapping deprecated symbols (XS — 3 tasks)

| Task | Layer | Size | Files |
| :--- | :--- | :--- | :--- |
| TASK-0044-0001-001 | Application | S | `PermissionCollector.java` (migrate 2 calls) |
| TASK-0044-0001-002 | Test | M | `StackMappingTest.java`, `StackMappingSearchTest.java`, `TimeseriesKnowledgeTest.java`, `NewsqlSettingsAndMappingTest.java`, `Epic0023IntegrationTest.java`, `DatabaseSettingsMappingTest.java` |
| TASK-0044-0001-003 | Domain | S | `StackMapping.java` (remove 4 symbols), `CHANGELOG.md`, new `StackMappingDeprecatedRemovedTest.java` |

**Removed symbols (4):** `DATABASE_SETTINGS_MAP`, `CACHE_SETTINGS_MAP`, `getDatabaseSettingsKey`, `getCacheSettingsKey`.

### story-0044-0002 — Remove ResourceResolver deprecated overloads (M — 6 tasks)

| Task | Layer | Size | Files |
| :--- | :--- | :--- | :--- |
| TASK-0044-0002-001 | Application | M | Wave 1: 6 docs/templates assemblers |
| TASK-0044-0002-002 | Application | M | Wave 2: 6 runbooks/incidents assemblers |
| TASK-0044-0002-003 | Application | M | Wave 3: 6 rules/skills/agents assemblers (incl. `ConstitutionAssembler` depth=4 — atenção) |
| TASK-0044-0002-004 | Application | M | Wave 4: 5 protocols/patterns/cicd + factory |
| TASK-0044-0002-005 | Test | M | `ResourceResolverTest.java` + 18 `*AssemblerTest.java` + `TimeseriesKnowledgeTest.java` |
| TASK-0044-0002-006 | Domain | S | `ResourceResolver.java` (remove 2 overloads), `CHANGELOG.md`, new `ResourceResolverDeprecatedRemovedTest.java` |

**Removed symbols (2):** `resolveResourcesRoot(String)`, `resolveResourcesRoot(String, int)`.

## Phase Boundaries

| Phase | Stories | Parallel? | Integrity Gate Trigger | Auto-Rebase Trigger |
| :---: | :--- | :---: | :--- | :--- |
| 0 | story-0044-0001, story-0044-0002 | yes | After both PRs merged to `develop` | After first PR merges (rebase second) |

## Risk Assessment

| Risk | Severity | Mitigation |
| :--- | :--- | :--- |
| `CHANGELOG.md` parallel edit conflict | Low | Auto-rebase (RULE-011) handles trivial conflict; both stories add to `Removed` section |
| Golden file drift in `ConstitutionAssembler` (wave 3, depth=4) | Medium | TASK-0044-0002-003 isolates this assembler; regen via `mvn process-resources` + `GoldenFileRegenerator` |
| Coverage drop after removing test cases | Low | RULE-003 — equivalent coverage migrated to `DatabaseSettingsMappingTest` and `ResourceResolverTest` for `resolveResourceDir` paths |
| Auto-merge with no human review | Medium | EPIC-0042 default; CRITICAL findings still trigger manual batch approval gate |
| No `plan-story-*.md` files | Low | Phase 0.5 classifies both stories as `unpredictable` (advisory only in default mode) |

## Pre-flight Conflict Analysis Summary

See `plans/epic-0044/plans/preflight-analysis-phase-0.md` (generated alongside this plan).

Both stories classified as **`unpredictable`** because no `plan-story-*.md` artifacts were generated by `x-story-plan` ahead of execution. In default advisory mode, this produces warnings only — both stories still dispatch in parallel.

## Integrity Gate (Post-Phase 0)

After both PRs merge to `develop`, the orchestrator dispatches the integrity gate subagent on `develop`:

1. `mvn clean compile` — must succeed with zero `[removal]` warnings for the 6 removed symbols
2. `mvn verify` — full test suite must pass
3. Coverage: line ≥ 95%, branch ≥ 90% (RULE-003)
4. **Smoke gate (mandatory, EPIC-0042):** `mvn verify -P integration-tests`
5. Cross-story consistency: review `git diff {mainShaBeforePhase[0]}..develop`

## Version Bump (Post-Gate)

After integrity gate PASS, `x-lib-version-bump` analyzes commits in range. This epic's commits use `refactor:` prefix (RULE-005) → bump type **NONE** (no version bump). Logged as: `"No version-impacting changes in phase 0. Version unchanged."`

## Final Verification (Phase 3)

- All story PRs merged to `develop`
- Coverage thresholds met
- `epic-execution-report.md` generated in `plans/epic-0044/`
- Worktrees removed (Rule 14 §5 — creator owns removal)

## Phase 4 — PR Comment Remediation

Optional. Triggered if any story PR has actionable review comments. Default behavior (EPIC-0042): dry-run report + auto-apply unless `--dry-run-only-comments` is set.

---

**Operator note:** This plan was generated BEFORE any subagent dispatch. Modifications to story files or `IMPLEMENTATION-MAP.md` after this point will not be reflected unless the orchestrator is re-invoked.
