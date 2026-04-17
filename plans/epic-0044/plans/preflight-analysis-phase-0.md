# Pre-flight Conflict Analysis — Phase 0

> **Epic ID:** EPIC-0044
> **Phase:** 0
> **Date:** 2026-04-16
> **Mode:** advisory (default — no `--strict-overlap`)

## Plan File Discovery

| Story | Plan Path | hasPlan |
| :--- | :--- | :---: |
| story-0044-0001 | `plans/epic-0044/plans/plan-story-0044-0001.md` | **false** (not generated) |
| story-0044-0002 | `plans/epic-0044/plans/plan-story-0044-0002.md` | **false** (not generated) |

> Both stories lack `plan-story-*.md` artifacts (would normally be produced by `x-story-plan`). Per Phase 0.5 §0.5.1, both are classified as **`unpredictable`** — treated as potential conflict with any other story.

## File Overlap Matrix (declared scope from story files §3 and §8)

| Story A | Story B | Overlapping Files | Classification |
| :--- | :--- | :--- | :--- |
| story-0044-0001 | story-0044-0002 | `CHANGELOG.md` (both add to `Removed` section) | `config-only` (CHANGELOG is a documentation/text file; merge-friendly) |

> Beyond `CHANGELOG.md`, the stories operate on completely disjoint files:
> - story-0044-0001 → `StackMapping.java`, `PermissionCollector.java`, `StackMappingTest.java`, `StackMappingSearchTest.java`, `TimeseriesKnowledgeTest.java`, `NewsqlSettingsAndMappingTest.java`, `Epic0023IntegrationTest.java`, `DatabaseSettingsMappingTest.java`, new `StackMappingDeprecatedRemovedTest.java`
> - story-0044-0002 → `ResourceResolver.java`, 23 `*Assembler.java`, `ResourceResolverTest.java`, 18 `*AssemblerTest.java`, new `ResourceResolverDeprecatedRemovedTest.java`

## Effective Classification

Despite both stories being marked `unpredictable` (no plan artifact), the **declared scope** in their story files §8 (Tasks) is precise enough to confirm there is **no code-level overlap**. The only shared file is `CHANGELOG.md`, which classifies as `config-only` and is auto-merge friendly.

| Pair Final Classification | Action |
| :--- | :--- |
| `config-only` + `unpredictable` (advisory) | Allow parallel dispatch with warning |

## Advisory Warnings

- **WARNING:** story-0044-0001 has no `plan-story-0044-0001.md` (classified as `unpredictable`). Monitor PR for unexpected diffs outside declared scope.
- **WARNING:** story-0044-0002 has no `plan-story-0044-0002.md` (classified as `unpredictable`). Monitor PR for unexpected diffs outside declared scope.
- **WARNING:** `CHANGELOG.md` will be edited by both stories (concurrent `Removed` section additions). Auto-rebase (Section 1.4e, RULE-011) will execute after the first PR merges to keep the second PR's branch current.
- **NOTE:** `ConstitutionAssembler` (in story-0044-0002 wave 3) uses `depth=4` (vs `depth=3` standard). Risk of golden file divergence is highest here — TASK-0044-0002-003 isolates it explicitly with extra attention.

## Execution Plan

All stories execute in parallel via worktrees. Advisory warnings above do NOT block parallel dispatch.

| Story | Worktree Path | Branch | Mode |
| :--- | :--- | :--- | :--- |
| story-0044-0001 | `.claude/worktrees/story-0044-0001/` | `feat/story-0044-0001-remove-stackmapping-deprecated` | parallel |
| story-0044-0002 | `.claude/worktrees/story-0044-0002/` | `feat/story-0044-0002-remove-resolveResourcesRoot` | parallel |

> **Precedence:** `--sequential` not set → parallel; `--strict-overlap` not set → advisory mode.
