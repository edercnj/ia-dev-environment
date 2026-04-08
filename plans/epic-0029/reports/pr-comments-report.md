# PR Comments Report — EPIC-0029

## Summary

| Metric | Count |
|--------|-------|
| Total raw comments | 95 |
| After deduplication | 91 |
| Actionable | 52 |
| Suggestions | 37 |
| Questions | 1 |
| Praise | 1 |
| Actionable with suggestion blocks | 42 |
| Actionable (keyword-matched only) | 10 |
| Source file fixes | 40 |
| Golden file fixes (need regeneration) | 12 |
| PRs analyzed | #204-#221 (18 PRs) |

## Recurring Themes

| # | Theme | Occurrences | PRs |
|---|-------|-------------|-----|
| 1 | Task ID format standardization | 33 | #204, #205, #206, #207, #208, #209, #210, #211, #212, #213, #214, #215, #217, #218, #219, #220, #221 |
| 2 | RULE-ID conflicts/collisions | 7 | #204, #218 |
| 3 | Execution state schema consistency | 7 | #209, #213, #219, #220, #221 |
| 4 | Language consistency (English-only policy) | 6 | #205, #207, #208, #211, #219 |
| 5 | Branch naming convention mismatch (feat/ vs feature/) | 5 | #214, #217, #221 |
| 6 | Task plan file path inconsistency | 1 | #221 |

## Actionable Findings

### A01 — PR #204 `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md:179`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The heading references `RULE-002`, but in this repo `RULE-002` is an epic-specific rule ID (and also widely used elsewhere for idempotency/artifact reuse), not a stable identifier for “testability patterns”. To avoid conflicting meanings, reference only `SD-12` here (or introduce a dedicated SD-* su...

### A02 — PR #204 `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md:176`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** `RULE-011` is referenced as “sizing constraints”, but `RULE-*` IDs are defined per-epic (and `RULE-011` is already used in other docs for unrelated concepts). Consider removing the `RULE-011` reference here and treating sizing as part of `SD-12` (or define an explicit SD-* identifier for sizing cons...

### A03 — PR #204 `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md:208`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** The **Layer** hint implies a single value, but the SD-13 example uses combined layers (e.g., `Port + Adapter`) and the SD-12 patterns include `Migration`. Update the template guidance to allow multi-layer values (or rename to something like “Scope/Layers”) and include `Migration` (or clarify which e...

### A04 — PR #204 `java/src/main/resources/knowledge/core/13-story-decomposition.md:274`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The “Example violations” snippet is Java-specific (`PaymentPort.java`, `JpaPaymentAdapter.java`), but this guide is used across multiple language profiles (golden files include TS/Python/Rust). Consider making the example language-agnostic (e.g., `payment_port.*`, `payment_adapter.*`) or providing p...

### A05 — PR #204 `.claude/skills/story-planning/references/story-decomposition.md:274`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The example in SD-13 is Java-specific (`PaymentPort.java`, `JpaPaymentAdapter.java`) even though this reference is shipped into multiple language profiles (TS/Python/Rust). Consider using language-neutral filenames/placeholders or splitting the example by profile to keep the reference consistent wit...

### A06 — PR #205 `java/src/test/java/dev/iadev/golden/GoldenFileRegenerator.java:177`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** `postVisitDirectory` ignores the `exc` parameter. If one of the children fails to delete, `exc` will be non-null and the more actionable error can be lost or replaced by a secondary failure (e.g., `DirectoryNotEmptyException`). Prefer checking `exc` and rethrowing it before attempting to delete the ...

### A07 — PR #207 `java/src/test/resources/golden/java-spring-hexagonal/.claude/skills/x-dev-lifecycle/SKILL.md:673`

- **Has suggestion block:** No
- **Golden file:** Yes (needs regeneration)
- **Description:** The newly added instruction uses Portuguese ('Concluída') in an otherwise English skill spec, which makes the output inconsistent and harder to apply across teams. Consider using a single language consistently (e.g., 'Completed') or explicitly defining the expected story-file vocabulary if it must r...

### A08 — PR #207 `java/src/test/resources/golden/java-spring-hexagonal/.claude/skills/x-dev-implement/SKILL.md:97`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** This introduces a new 'fresh' concept for task plans but does not define freshness criteria here (while `x-dev-lifecycle` defines a concrete `mtime(...)` comparison for artifacts). To keep behavior consistent and implementable, consider referencing the same freshness rule (or duplicating the precise...

### A09 — PR #207 `N/A:?`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** ## Pull request overview

> [!NOTE]
> Copilot was unable to run its full agentic suite in this review.

Adds the new `x-docs` documentation automation skill as a unified entry point and updates registries/tests and golden fixtures accordingly, including a golden traversal fix to exclude platform-spe...

### A10 — PR #208 `java/src/test/resources/golden/java-spring-event-driven/.codex/skills/x-dev-lifecycle/SKILL.md:674`

- **Has suggestion block:** No
- **Golden file:** Yes (needs regeneration)
- **Description:** This section mixes languages (\"Concluída\") in otherwise English documentation. Please replace with an English status label (e.g., \"Completed\") or explicitly define the expected canonical status text if it must remain localized.

### A11 — PR #208 `java/src/main/java/dev/iadev/application/assembler/SkillsSelection.java:218`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** These feature-gate checks are case-sensitive (unlike `architecture.style`, which is normalized to lower-case). If config values can vary in casing, gates like `databaseName != \"none\"` may mis-fire (e.g., \"None\"). Consider normalizing these config values similarly (lower-casing with `Locale.ROOT`...

### A12 — PR #209 `java/src/main/java/dev/iadev/checkpoint/ResumeHandler.java:217`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** `reevaluateBlockedTasksInStory(...)` is currently a no-op: it iterates BLOCKED tasks but never updates `result`, so BLOCKED tasks will never be reclassified to PENDING on resume despite the Javadoc stating that behavior. Either implement the dependency check and status update (e.g., based on parsed ...

### A13 — PR #209 `java/src/main/java/dev/iadev/checkpoint/CheckpointValidation.java:125`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** Tasks are described as "keyed by task ID", but validation currently only checks `taskEntry.taskId()` format and does not ensure it matches the map key (`taskId`). This can allow inconsistent checkpoints (key says one task, payload says another). Consider adding a validation error when `!taskId.equal...

### A14 — PR #209 `java/src/main/resources/shared/templates/_TEMPLATE-EXECUTION-STATE.json:20`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The template uses `"TASK-XXXX-0001-001"` as both the task map key and `taskId`, but `TaskEntry` validation requires digits (`^TASK-\d{4}-\d{4}-\d{3}$`). This makes the provided v2.0 template invalid if copied into a real checkpoint. Update the placeholder to a digit-based example (e.g., `TASK-0000-0...

### A15 — PR #210 `java/src/test/java/dev/iadev/golden/GoldenFileRegenerator.java:163`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** `postVisitDirectory` receives a non-null `IOException e` when visiting the directory failed. Ignoring it can mask traversal failures (and can lead to partial deletes without a clear error). Handle `e` explicitly (e.g., rethrow it before deleting, or decide on a clear policy such as terminating the w...

### A16 — PR #210 `java/src/test/java/dev/iadev/golden/GoldenFileRegenerator.java:132`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** Avoid embedding path separators in a string passed to `Path.resolve(...)`. Use `goldenRoot.resolve(profile).resolve("platform-claude-code")` to keep path handling OS-agnostic and clearer.

### A17 — PR #210 `N/A:?`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** ## Pull request overview

> [!NOTE]
> Copilot was unable to run its full agentic suite in this review.

Adds an `x-format` skill to the dev skill group and regenerates golden outputs so generated project templates include the new formatter skill across supported platforms/languages.

**Changes:**
- ...

### A18 — PR #211 `java/src/test/java/dev/iadev/golden/GoldenFileTest.java:130`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** The test excludes `platform-claude-code` when collecting golden paths but not when collecting generated paths. If the pipeline ever emits platform output during this test run, the comparison will fail even though the intent (per the new EXCLUDED_DIRS javadoc) is to validate platform output elsewhere...

### A19 — PR #211 `java/src/test/resources/golden/java-spring-cqrs-es/.codex/skills/x-dev-lifecycle/SKILL.md:440`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** The ‘atomic commit’ example doesn’t match the PR’s stated convention (`<type>(<TASK-XXXX-YYYY-NNN>): ...` with the Task ID in scope). Here it uses `feat(scope)` and a shortened `TASK-NNN`. Update the documentation examples to use the full task ID in the scope (or explicitly instruct to use `/x-commi...

### A20 — PR #211 `java/src/test/resources/golden/java-spring-cqrs-es/.codex/skills/x-dev-implement/SKILL.md:280`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** This commit command example conflicts with the new `x-commit` conventions described in this PR (task ID in scope and standardized commit formatting). Consider changing the guidance to use `/x-commit` (so the pre-commit chain runs and the message format is enforced) or at least update the example com...

### A21 — PR #212 `java/src/main/resources/targets/claude/skills/conditional/x-review-obs/SKILL.md:23`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The activation condition uses a config key path (`observability.tool`) that doesn’t match the way the code reads configuration (e.g., `ProjectConfig.observabilityTool()` in `SkillsSelection`). To avoid confusion and keep docs consistent with the actual config/schema, update the activation condition ...

### A22 — PR #212 `java/src/test/resources/golden/java-spring-cqrs-es/.claude/skills/x-review-events/SKILL.md:19`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** The Purpose section wording switches to third-person ('Reviews... Covers...'). If other skills use an imperative style ('Review... Cover...'), consider aligning this phrasing for consistency across skill templates.

### A23 — PR #213 `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md:180`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The template removes the previously documented `8.1 Detailed Tasks (Multi-Agent Planning)` structure, but x-dev-lifecycle and x-story-plan docs in this PR still explicitly reference updating/reading 'Section 8.1'. With this template, those instructions become impossible to follow and will break PRE_...

### A24 — PR #214 `java/src/main/resources/targets/claude/skills/core/x-pr-create/SKILL.md:61`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** Branch naming in this skill uses `feat/...` (e.g., `feat/task-...`, `feat/story-...`), but Rule 09 defines branch prefixes as `feature/*` (and `fix/*` for bugfixes). This mismatch will cause the validation logic and examples here to conflict with the documented branching model; align the expected pa...

### A25 — PR #214 `java/src/main/resources/targets/claude/skills/core/x-pr-create/SKILL.md:74`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** `PARENT_BRANCH` is derived from `git branch -a` and can include entries like `remotes/origin/...`; passing that directly to `gh pr create --base` is likely invalid, and `head -1` can select an arbitrary match if multiple story branches exist. Prefer resolving a single, unambiguous *branch name* (wit...

### A26 — PR #214 `java/src/main/resources/targets/claude/skills/core/x-pr-create/SKILL.md:134`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** In auto-approve mode the PR base is a story branch, but the body template hard-codes the commit list to `git log --oneline develop..HEAD`. This will produce an incorrect change list when the base is not `develop`; generate the commit list relative to the actual chosen base branch.

### A27 — PR #214 `java/src/main/resources/targets/github-copilot/skills/git-troubleshooting/x-pr-create.md:55`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** This skill template documents task/story branch naming as `feat/task-...` and `feat/story-...`, which conflicts with the repository branching model (Rule 09) that uses `feature/*` (and `fix/*` for bugfixes). Update the documented patterns in this template (and the referenced skill spec) to match the...

### A28 — PR #215 `java/src/main/resources/targets/github-copilot/skills/story/x-story-create.md:96`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The bullet labels for Section 3.5 value delivery don’t match the story template. `_TEMPLATE-STORY.md` uses “Métrica de Sucesso” and “Impacto no Negócio” (with accents), but this skill requires “Metrica de Sucesso” / “Impacto no Negocio”. Since this section is described as having an exact 3-bullet st...

### A29 — PR #215 `java/src/main/resources/targets/github-copilot/skills/story/x-story-create.md:312`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** This skill states the story output must follow `_TEMPLATE-STORY.md` as the “exact structure to follow”, but the required Section 8 format here (formal task decomposition table with TASK-XXXX-YYYY-NNN IDs, testability patterns, LOC constraints) doesn’t match the current template’s Section 8 (“Sub-tar...

### A30 — PR #215 `java/src/main/resources/targets/claude/skills/core/x-story-create/SKILL.md:336`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** The skill requires a new formal Section 8 task decomposition (TASK-XXXX-YYYY-NNN IDs, table columns, testability patterns, sizing constraints), but earlier it instructs following `_TEMPLATE-STORY.md` as the exact output structure. The current template’s Section 8 is a different format (“Sub-tarefas”...

### A31 — PR #217 `java/src/main/resources/targets/github-copilot/skills/git-troubleshooting/x-git-push.md:47`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** This template mostly assumes a `main`-only workflow (branch strategy and examples start from `main`), but this section says the parent story branch is created from `develop` and even suggests `develop` as a starting point. Please align the parent/task branch guidance with the rest of this document (...

### A32 — PR #217 `java/src/main/resources/targets/github-copilot/skills/git-troubleshooting/x-git-push.md:191`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The PR body template links to `plans/epic-XXXX/plans/task-plan-XXXX-YYYY-NNN.md`, but other planning skills generate task plans using the `task-plan-TASK-NNN-story-XXXX-YYYY.md` naming (see `x-story-plan`, e.g. `java/src/main/resources/targets/claude/skills/core/x-story-plan/SKILL.md:592`). Please u...

### A33 — PR #217 `java/src/main/resources/targets/claude/skills/core/x-git-push/SKILL.md:98`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** Branch description validation says "lowercase, hyphens only", but the branch rules explicitly allow digits (`[a-z0-9/-]`) and the examples include digits (e.g. `implement-phase2`). Please clarify the validation rule to match the allowed character set (e.g., "lowercase letters/digits with hyphen sepa...

### A34 — PR #217 `java/src/main/resources/targets/claude/skills/core/x-git-push/SKILL.md:274`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The task PR body template links to `plans/epic-XXXX/plans/task-plan-XXXX-YYYY-NNN.md`, but `x-story-plan` documents generating per-task plans at `plans/epic-XXXX/plans/task-plan-TASK-NNN-story-XXXX-YYYY.md` (see `java/src/main/resources/targets/claude/skills/core/x-story-plan/SKILL.md:592`). Please ...

### A35 — PR #217 `java/src/main/resources/targets/claude/skills/core/x-git-push/SKILL.md:408`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** `x-git-push` now claims it delegates to `x-commit` and `x-pr-create`, but those skills don’t appear to exist in the current skill set (no `x-commit/` or `x-pr-create/` under `java/src/main/resources/targets/claude/skills/core/` or `.claude/skills/`). Please either add those skills in this PR or remo...

### A36 — PR #218 `java/src/main/resources/targets/claude/skills/core/x-story-map/SKILL.md:181`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The step gate requires global task IDs in the form `TASK-XXXX-YYYY-NNN`, but the existing `/x-story-plan` spec and story templates commonly use story-scoped IDs like `TASK-001` (which will collide across stories). As written, this likely causes Step 8 to be skipped in most real outputs or makes cros...

### A37 — PR #218 `java/src/main/resources/targets/claude/skills/core/x-story-map/SKILL.md:278`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** Step 8A/8C assume every `Depends On` reference resolves to a known task node, but the Error Handling table doesn’t define what to do when a task dependency points to a missing/unknown task ID (and the PR description mentions a “missing task IDs” error). Add an explicit ERROR case for unresolved task...

### A38 — PR #218 `java/src/main/resources/targets/github-copilot/skills/story/x-story-map.md:176`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** This step requires globally unique task IDs like `TASK-XXXX-YYYY-NNN`, but the established `/x-story-plan` output format uses story-scoped IDs (e.g., `TASK-001`). Without aligning these formats, cross-story dependency extraction/topological sort will either be skipped or produce collisions between t...

### A39 — PR #218 `java/src/main/resources/shared/templates/_TEMPLATE-IMPLEMENTATION-MAP.md:173`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The template labels this validation as `RULE-012`, but `RULE-012` is already used elsewhere in the codebase for unrelated semantics (template fallback / overwrite detection). Reusing the same RULE number here will confuse readers and downstream skills; rename this to a new rule identifier (or descri...

### A40 — PR #219 `.claude/skills/x-dev-lifecycle/SKILL.md:192`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** Global Output Policy states "Language: English ONLY", but the Resume Detection section immediately below uses Portuguese column headers (e.g., "Status Anterior", "Novo Status"). Either translate that table to English or relax the global policy to avoid conflicting instructions.

### A41 — PR #219 `.claude/skills/x-dev-lifecycle/SKILL.md:188`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** Resume Detection says to look for `execution-state.json` "in the story's plan directory", but later Phase 2.1 creates/updates it at `plans/epic-XXXX/execution-state.json`. Please make the file location consistent (and update both sections accordingly), otherwise resume logic may look in the wrong pl...

### A42 — PR #219 `.claude/skills/x-dev-lifecycle/SKILL.md:534`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** Coverage behavior is contradictory: Step 3.1 says below-threshold coverage only emits a WARNING, but the Error Handling table says coverage below thresholds should fail Phase 3.1 and require adding tests. Please pick one behavior (fail or warn) and align both sections.

### A43 — PR #219 `.claude/skills/x-dev-lifecycle/SKILL.md:672`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The doc says Phase 3 is optional via `--skip-verification`, but later states "Phase 3 is the ONLY legitimate stopping point." If verification can be skipped, stopping after Phase 2 must be allowed in that mode—please clarify this rule so it is not self-contradictory.

### A44 — PR #219 `java/src/test/java/dev/iadev/smoke/TaskCentricLifecycleTest.java:41`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** This test runs the full AssemblerPipeline (including multi-profile runs) but the filename/class name ends with `Test`, so it will run under Surefire (unit test phase). The project convention is to run these pipeline/smoke validations under Failsafe by naming them `*SmokeTest` (see pom excludes/inclu...

### A45 — PR #220 `java/src/main/resources/targets/claude/skills/core/x-dev-epic-implement/SKILL.md:535`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** `version` is marked as required in the top-level `execution-state.json` schema table, but the description also says it may be absent (treated as legacy/"1.0"). This is internally inconsistent—either mark `version` as optional (and document defaulting to legacy when missing) or remove the “absent” be...

### A46 — PR #220 `java/src/main/resources/targets/claude/skills/core/x-dev-epic-implement/SKILL.md:71`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** This document now labels batch approval as "RULE-013", but the same file already uses "RULE-013" for the post-integrity-gate version bump section. Reusing the same rule identifier for unrelated behaviors makes the rule references ambiguous—please assign a distinct rule ID for batch approval (and upd...

### A47 — PR #220 `N/A:?`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** ## Pull request overview

This PR updates the **x-dev-epic-implement** skill documentation (and its generated golden copies across multiple profiles/targets) to support epic orchestration enhancements: propagating auto-approve behavior to lifecycle dispatches, consolidating PR approvals, and trackin...

### A48 — PR #221 `java/src/main/java/dev/iadev/checkpoint/ResumeHandler.java:216`

- **Has suggestion block:** No
- **Golden file:** No (direct fix)
- **Description:** reevaluateBlockedTasksInStory is effectively a no-op (it never updates any BLOCKED task), but reevaluateBlockedTasks() calls it and the surrounding Javadoc claims blocked tasks will be reevaluated/unblocked. Either implement the unblocking logic (based on explicit dependency data) or remove this met...

### A49 — PR #221 `java/src/test/resources/golden/go-gin/.agents/skills/x-dev-implement/SKILL.md:280`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** This commit example conflicts with the task-centric git rules introduced elsewhere in this PR: scope should be the full `TASK-XXXX-YYYY-NNN`, and the subject should not reference the task ID in the message body if it's already in the scope. Consider updating to the standardized format shown in x-git...

### A50 — PR #221 `.claude/templates/_TEMPLATE-TASK-BREAKDOWN.md:39`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** This template uses placeholder task IDs like TASK-1/TASK-2, but the rest of the PR standardizes on `TASK-XXXX-YYYY-NNN`. Updating the template to the formal format (or at least to a placeholder that preserves the shape, e.g., `TASK-{{EPIC_ID}}-{{STORY_ID}}-{{NNN}}`) will reduce downstream inconsiste...

### A51 — PR #221 `java/src/main/resources/targets/github-copilot/skills/dev/x-worktree.md:23`

- **Has suggestion block:** Yes
- **Golden file:** No (direct fix)
- **Description:** The trigger examples reference `feature/story-XXXX`, but the branch naming conventions introduced/used throughout this PR are `feat/story-...` and `feat/task-...`. Align these examples with the documented naming scheme to avoid teaching users an invalid prefix.

### A52 — PR #221 `java/src/test/resources/golden/go-gin/.agents/skills/x-fix-epic-pr-comments/SKILL.md:574`

- **Has suggestion block:** Yes
- **Golden file:** Yes (needs regeneration)
- **Description:** This rewording makes the example less concrete than the previous `{{PROJECT_NAME}}` form. Consider restoring an explicit double-brace example inline (e.g., `{{PROJECT_NAME}}`) to reduce ambiguity about what the literal match is detecting.

## Suggestion Findings

### S01 — PR #204 `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md:209`
- **Description:** The allowed values for **Test Type** omit `Acceptance`, but the table above includes the `UseCase + AT` pattern with Test Type = `Acceptance`. Add `Acceptance` to the allowed list (or align the patter

### S02 — PR #204 `java/src/main/resources/knowledge/core/13-story-decomposition.md:251`
- **Description:** This section labels task sizing as `RULE-011`, but `RULE-*` IDs are epic-specific and `RULE-011` is used elsewhere in the repo for different meanings. Consider removing the `RULE-011` reference and ma

### S03 — PR #204 `.claude/skills/story-planning/references/story-decomposition.md:251`
- **Description:** This guide references task sizing as `RULE-011`, but `RULE-*` IDs are epic-specific and `RULE-011` is already used elsewhere for unrelated concepts. Prefer referencing `SD-12` (or a dedicated SD-* sub

### S04 — PR #204 `N/A:?`
- **Description:** ## Pull request overview  This PR formalizes story task decomposition by introducing structured, testability-driven task blocks in the story template and by adding SD-12/SD-13 guidance to the story de

### S05 — PR #205 `java/src/test/java/dev/iadev/golden/GoldenFileRegenerator.java:74`
- **Description:** The `PipelineOptions` constructor is called with multiple positional booleans/null, which is very hard to read and easy to misuse. Prefer a named factory/builder (or, if that’s not available) introduc

### S06 — PR #205 `java/src/test/java/dev/iadev/golden/GoldenFileRegenerator.java:119`
- **Description:** If an `IOException` occurs while preserving platform directories, it’s wrapped in a `RuntimeException` and any already-created temp backup directories may be leaked (not deleted) because they’re not t

### S07 — PR #205 `java/src/test/resources/golden/java-spring-fintech-pci/.claude/skills/x-dev-lifecycle/SKILL.md:673`
- **Description:** This newly introduced instruction mixes languages (“Concluída”) within otherwise English guidance. If the canonical skill spec is intended to be English, update the upstream template/source to use a c

### S08 — PR #205 `N/A:?`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  This PR introduces a new core `x-worktree` skill to standardize git worktree lifecycle managemen

### S09 — PR #206 `java/src/test/java/dev/iadev/golden/GoldenFileTest.java:270`
- **Description:** `Path#getFileName()` can be `null` for certain paths (e.g., filesystem root), which would cause a NullPointerException at `d.getFileName().toString()`. Consider guarding against `null` (treat as CONTI

### S10 — PR #206 `N/A:?`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  Adds a new `x-lint` skill to the pre-commit workflow and updates golden artifacts/tests to refle

### S11 — PR #207 `java/src/test/java/dev/iadev/golden/GoldenFileTest.java:249`
- **Description:** As written, this will also skip traversal when the *root* `dir` itself is named `platform-*` (because `preVisitDirectory` is invoked for the starting directory). If `collectRelativePaths(...)` is ever

### S12 — PR #208 `java/src/test/resources/golden/java-spring-event-driven/.codex/skills/x-dev-lifecycle/SKILL.md:138`
- **Description:** The PR description is focused on extracting and gating review skills, but the golden files include substantial new/expanded lifecycle behavior documentation (planning mode detection, task-by-task exec

### S13 — PR #208 `N/A:?`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  This PR updates skill selection to support newly extracted, individually-gated review skills and

### S14 — PR #209 `java/src/main/java/dev/iadev/checkpoint/CheckpointValidation.java:113`
- **Description:** `validateSingleTask(...)` dereferences `taskEntry` without a null check. If a checkpoint JSON contains a `tasks` entry with a null value, validation will throw a NullPointerException instead of return

### S15 — PR #209 `N/A:?`
- **Description:** ## Pull request overview  Adds per-task execution tracking to the checkpoint model (v2.0 schema) so a story can persist individual task lifecycle state and resume behavior can operate at task granular

### S16 — PR #211 `java/src/test/java/dev/iadev/golden/GoldenFileRegenerator.java:111`
- **Description:** The final summary count can be incorrect when failures occur in the main `PROFILES` loop. `success` only counts successful entries from `PROFILES`, while `failed` includes failures from both loops; th

### S17 — PR #211 `java/src/test/resources/golden/java-spring-cqrs-es/.codex/skills/x-dev-lifecycle/SKILL.md:674`
- **Description:** This instruction mixes Portuguese (‘Concluída’) into otherwise English workflow docs. If story templates/fields are expected to be English, rename this to ‘Completed’ (or the exact expected status lab

### S18 — PR #211 `java/src/test/java/dev/iadev/application/assembler/SkillGroupRegistryTest.java:57`
- **Description:** PR description says the dev-group assertion was updated from 13 -> 15, but this diff updates it from 12 -> 15. Please align the PR description (or re-check the expected prior size) so reviewers/users 

### S19 — PR #211 `N/A:?`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  Adds a new `x-commit` skill (with `x-format`/`x-lint`) to standardize Conventional Commits with 

### S20 — PR #212 `java/src/main/java/dev/iadev/application/assembler/SkillsSelection.java:222`
- **Description:** The logic repeats the sentinel string check (`\"none\"`) across multiple config fields and duplicates the `databaseName()` check. Consider normalizing these config values once (e.g., lowercasing and/o

### S21 — PR #212 `N/A:?`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  Refactors the x-review system to delegate reviews to dedicated specialist skills and updates gen

### S22 — PR #213 `java/src/test/java/dev/iadev/application/assembler/GithubSkillsAssemblerTest.java:61`
- **Description:** The test method name `assemble_storyGroup_hasNineSkills` no longer matches the assertion and display name (now 10). Rename the method to reflect the updated expectation (e.g., `assemble_storyGroup_has

### S23 — PR #213 `N/A:?`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  Adds the new `x-plan-task` skill and updates planning/implementation workflow docs so existing s

### S24 — PR #214 `N/A:?`
- **Description:** ## Pull request overview  Adds a new `x-pr-create` skill to the skills catalog to standardize task-level pull request creation (title/body/labels/target branch) and wires it into the `git-troubleshoot

### S25 — PR #215 `N/A:?`
- **Description:** ## Pull request overview  This PR updates the `x-story-create` skill (Claude + GitHub Copilot targets) to require a new mandatory **Section 3.5 (Value Delivery)** and a more formal, testable **Section

### S26 — PR #217 `N/A:?`
- **Description:** ## Pull request overview  Updates the `x-git-push` skill documentation/templates to support a task-centric workflow (task branches, task-scoped commits, and task PR templates), and refreshes golden sn

### S27 — PR #218 `java/src/main/resources/targets/claude/skills/core/x-story-map/SKILL.md:196`
- **Description:** `RULE-012` is already used elsewhere in the repo for other concepts (e.g., overwrite detection and template fallback). Reusing `RULE-012` here to mean cross-story dependency consistency makes the rule

### S28 — PR #218 `java/src/main/resources/targets/claude/skills/core/x-story-map/SKILL.md:236`
- **Description:** The “cycle through” color snippet uses `style story-1`, `style story-2`, etc., but those node/subgraph IDs don’t match the IDs used elsewhere in the same example (`story-XXXX-0001`, `story-XXXX-0002`)

### S29 — PR #218 `java/src/main/resources/targets/github-copilot/skills/story/x-story-map.md:193`
- **Description:** `RULE-012` is already used across the repo for other concepts (notably template fallback / overwrite detection). Using `RULE-012` here for cross-story dependency consistency makes rule references ambi

### S30 — PR #218 `N/A:?`
- **Description:** ## Pull request overview  Adds a new “Task-Level Dependency Graph” section to the `x-story-map` skill so it can extract cross-story task dependencies, validate consistency, compute a merge order, and 

### S31 — PR #219 `.claude/skills/x-dev-lifecycle/SKILL.md:430`
- **Description:** Task status state machine looks internally inconsistent: the schema lists `PR_MERGED` and `DONE`, but the resume table maps "PR merged" to `DONE`, while auto-approve sets `PR_MERGED`. Also the loop sk

### S32 — PR #219 `.claude/skills/x-dev-lifecycle/SKILL.md:628`
- **Description:** Status values for story/IMPLEMENTATION-MAP updates are written as `Concluida` (no accent), but the repo convention is `Concluída` (e.g., `.claude/skills/x-dev-epic-implement/SKILL.md` status sync at l

### S33 — PR #219 `.claude/skills/x-dev-lifecycle/SKILL.md:472`
- **Description:** Phase 2 instructs to present an `AskUserQuestion` approval gate, but the skill frontmatter `allowed-tools` does not include `AskUserQuestion` (currently: Read/Write/Edit/Bash/Grep/Glob/Skill). Either 

### S34 — PR #219 `N/A:?`
- **Description:** ## Pull request overview  This PR rewrites the `x-dev-lifecycle` skill to a task-centric workflow where each task gets its own branch/PR/approval gate, followed by consolidated story-level verificatio

### S35 — PR #221 `java/src/main/java/dev/iadev/checkpoint/CheckpointValidation.java:125`
- **Description:** validateSingleTask dereferences taskEntry without a null check. If the tasks map contains a null value (or deserialization yields a null task entry), this will throw NPE and prevent checkpoint validat

### S36 — PR #221 `java/src/main/java/dev/iadev/checkpoint/ResumeHandler.java:116`
- **Description:** The Javadoc describes BLOCKED dependency reevaluation as part of reclassification, but reclassifySingleTask only handles IN_PROGRESS -> PENDING. Update the documentation to match actual behavior, or i

### S37 — PR #221 `N/A:?`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  This PR expands EPIC-0029’s task-centric workflow and review foundations by adding new skill tem

## Questions

### Q01 — PR #216 `N/A`
- **Description:** ## Pull request overview  > [!NOTE] > Copilot was unable to run its full agentic suite in this review.  Adds a new `x-tdd` skill (spec + README) and regenerates golden artifacts across multiple profil

## Praise

- PR #208 `java/src/main/java/dev/iadev/application/assembler/SkillsSelection.java`: The helper name `isHexagonalOrDdd` is misleading because the accepted styles include `cqrs` and `clean` as well. Rename to something that matches the 
