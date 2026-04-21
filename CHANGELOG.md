# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- **story-0047-0004 (EPIC-0047):** Compression sweep across the 5 largest
  knowledge packs — `click-cli-patterns`, `k8s-helm`, `axum-patterns`,
  `iac-terraform`, `dotnet-patterns`. Each `SKILL.md` was rewritten as a
  slim narrative + Patterns Index pointing to
  `references/examples-<pattern>.md` companions (one file per carved
  numbered section), per RULE-047-02 / RULE-047-05. Combined hot-path
  reduction: **4,729 → 275 lines (−94.2% across the 5 KPs)**; individual
  SKILL.md line counts are now 64 (click-cli, was 1222), 47 (k8s-helm, was
  944), 59 (axum, was 888), 46 (iac-terraform, was 861), 59 (dotnet, was
  814) — all ≤ 250-line story-0047-0004 target. Thirty-three new
  `references/examples-*.md` files (7 + 7 + 7 + 6 + 6) hold the complete
  code samples byte-identical to the original inline blocks. The post-
  sweep corpus (SKILL.md hot-path only) stands at **45,743 lines**
  (−8.9% vs v3.9.0 baseline of 50,191); remaining gap to the −40%
  epic target (< 30,115 lines) stays assigned to stories 0047-0002 (Slim
  Mode retirement + flipped orientation) and Bucket C. Goldens regenerated
  across the profile matrix; `audits/skill-size-baseline.txt` updated to
  remove the 5 now-compliant entries (25 → 20 brownfield exemptions).
  `Epic0047CompressionSmokeTest.smoke_kpsHaveCarvedExamples` validates
  `SKILL.md ≤ 250 lines + references/examples-*.md present` across every
  generated profile (stack-gated: missing KP on a profile skips silently).
  Epic §6 updated with the post-sweep delta row.

### Added

- **story-0047-0003 (EPIC-0047):** `SkillSizeLinter` guard-rail enforcing
  RULE-047-04 (500-line cap per `SKILL.md` without a non-empty
  `references/` sibling). New classes under
  `java/src/main/java/dev/iadev/quality/`: `LintFinding` record (6-field
  data contract per story §5.1), `Severity` enum (INFO / WARN / ERROR
  tiers per §5.2), and `SkillSizeLinter` static helper exposing
  `lint(Path)` and `errorFindings(List)`. Brownfield-safe rollout: new
  test `SkillSizeLinterAcceptanceTest` runs in `mvn test` default scope
  against the real source-of-truth tree and fails ONLY on NEW offenders;
  the 25 existing oversized SKILL.md files at the time this story landed
  are enumerated in `audits/skill-size-baseline.txt` and will be carved
  out by stories 0047-0002 (flipped orientation) and 0047-0004 (KP
  sweep). Companion test `SkillCorpusSizeAudit` asserts the corpus
  total stays below the RULE-047-07 cap (30,000 lines) with a soft-warn
  default and opt-in hard-fail via `-Dskill.corpus.audit.enforce=true`.
  Unit coverage: `LintFindingTest` (5 tests), `SkillSizeLinterTest` (17
  tests including 3 boundary scenarios and `_shared/` exclusion). Full
  lint + audit suite completes in < 1 s over ~130 SKILL.md.
- **story-0046-0007 (EPIC-0046):** CI enforcement — `LifecycleIntegrityAuditTest` (Maven CI-blocking) scans every `SKILL.md` under `java/src/main/resources/targets/claude/skills/` and detects regressions across three Rule 22 dimensions: `ORPHAN_PHASE` (documented numbered sub-section not referenced elsewhere in the skill), `WRITE_WITHOUT_COMMIT` (write to `plans/epic-*/reports/` not followed within 20 lines by `x-git-commit`), and `SKIP_IN_HAPPY_PATH` (`--skip-verification`/`--skip-status-sync` used outside `## Recovery` / `## Error Handling` sections). Audit respects `<!-- audit-exempt -->` escape hatch, YAML frontmatter, and inline "Recovery-only" markers. Baseline file at `audits/lifecycle-integrity-baseline.txt` tolerates currently-accepted TOC-style sub-sections; any NEW violation fails the build with `LIFECYCLE_AUDIT_REGRESSION`. New classes: `LifecycleAuditRunner` (production detection logic replacing the story-0046-0001 skeleton), `LifecycleAuditCli` (standalone CLI, exit 0 / 11 / 2). 19 new tests: 9 unit detection tests (`LifecycleAuditRunnerDetectionTest`), 4 CLI tests (`LifecycleAuditCliTest`), 2 E2E smoke (`LifecycleAuditRegressionSmokeTest` — injects 3 synthetic regressions), 1 CI audit integration (`LifecycleIntegrityAuditTest`, < 2s over ~130 SKILL.md), plus existing skeleton contract tests. Performance budget: ≤ 2s over 40+ SKILL.md.
- **story-0046-0005 (EPIC-0046):** `ReportCommitMessageBuilder` — canonical
  builder of Conventional Commit messages for atomic epic report commits
  (`docs(epic-XXXX): add execution plan` and `docs(epic-XXXX): add phase-N
  report`). V2-gated retrofit of `x-epic-implement/SKILL.md` wires
  `Skill(skill: "x-git-commit", ...)` via Rule 13 Pattern 1 INLINE-SKILL
  immediately after each write to `plans/epic-XXXX/reports/`, closing the
  window where reports were orphaned on the working tree and triggered
  false positives in `x-release` `VALIDATE_DIRTY_WORKDIR`. Fails loudly
  with exit `REPORT_COMMIT_FAILED` (21) on commit rejection (RULE-046-08);
  forbids `--no-verify` fallback. Rule 19 backward compatibility preserved
  — v1 epics remain untouched. New tests: `ReportCommitMessageBuilderTest`
  (12), `ExecutionPlanCommitSmokeTest` (6), `PhaseReportCommitsSmokeTest`
  (6), `EpicImplementReleaseCompatTest` (3), `ReportCommitFailLoudTest`
  (3), `EpicV1NoReportCommitTest` (4).
- **story-0046-0004 (EPIC-0046):** Story/Epic end-of-life status retrofit — Phase 3 unskippable + Phase 1.7 cabeada. `x-story-implement` Phase 3.8 gains explicit sub-steps 3.8.1–3.8.5 (read Status → validateOrThrow → writeStatus(Concluída) → update IMPLEMENTATION-MAP row → atomic `docs(story-*)` commit); each step is fail-loud with exit `STATUS_SYNC_FAILED` (Rule 22 RULE-046-08). `x-epic-implement` promotes orphan Section 1.6b to Phase 1.7 (Markdown Status Sync, wired into Core Loop step 6e) and adds new Phase 5 (Epic Finalization) after the last wave with all stories SUCCESS. `--skip-verification` documented as recovery-only (violates Rule 22 RULE-046-04 on happy path). New helper `EpicMapRowUpdater` for atomic Status-column rewrite in epic `IMPLEMENTATION-MAP.md` (distinct 6-column schema from task-map). 4 smoke tests (`StoryImplementFinalizeSmokeTest`, `EpicImplementFinalizeSmokeTest`, `EpicFinalizeFailLoudTest`, `EpicFinalizeIdempotencyTest`) cover happy-path, fail-loud (missing epic/map files), and idempotency (re-run byte-stable). V2-gated (Rule 19 backward-compat). Golden files regenerated for all profiles.
- **story-0046-0003 (EPIC-0046):** Phase 3.5 (Task-Level Status Transition) retrofit in `x-task-implement` — between Phase 3 (output-contract verification) and Phase 4 (atomic commit). V2-gated via `SchemaVersionResolver`; v1 epics skip silently. New application helper `TaskMapRowUpdater` (pure-function `rewriteRow` + atomic-rename `updateRow`) and CLI wrapper `TaskMapRowUpdaterCli` with exit codes `0` / `20 STATUS_SYNC_FAILED` / `40 INVALID_ARGS`. Rule 18 (1 commit per task) preserved — helpers stage artefacts onto the index; the single Phase 4 `x-git-commit` absorbs the status + map-row delta together with the TDD diff. Coalesced pairs (Rule 15) update BOTH partner task files and BOTH map rows inside the same commit. New error codes `STATUS_SYNC_FAILED` and `STATUS_TRANSITION_INVALID` surface fail-loud at the skill boundary (RULE-046-08). Smoke coverage: `TaskImplementStatusSmokeTest`, `CoalescedTaskStatusTest`, `TaskStatusFailLoudTest`, `TaskAtomicCommitAuditTest` (reflection-based Rule 18 API-boundary guard). Goldens regenerated for 19 stack profiles + 2 platform variants.
- **story-0045-0001 (EPIC-0045):** New skill `x-pr-watch-ci` for polling PR CI checks + Copilot review detection with 8 stable exit codes (`SUCCESS/0`, `CI_PENDING_PROCEED/10`, `CI_FAILED/20`, `TIMEOUT/30`, `PR_ALREADY_MERGED/40`, `NO_CI_CONFIGURED/50`, `PR_CLOSED/60`, `PR_NOT_FOUND/70`). Encapsulates the `gh pr checks` / `statusCheckRollup` polling loop with configurable global timeout (default 1800s), Copilot-specific sub-timeout (default 900s), and atomic state-file for session resume. Introduced `PrWatchExitCode` enum and `PrWatchStatusClassifier` (zero-I/O, fully testable, covers all 8 codes via `@ParameterizedTest`).
- **story-0045-0002 (EPIC-0045):** Rule 21 (CI-Watch) formalizes `x-pr-watch-ci` as the default CI gate in schema v2 orchestrators; no-op in schema v1 (Rule 19 backward-compat). Specifies opt-out via `--no-ci-watch`, fallback matrix (V1 no-op / V2 active / V2 skipped), and regression audit script `scripts/audit-rule-20.sh`. Adds `RulesAssemblerCiWatchTest` (3 tests) verifying rule is copied, has mandatory sections, and contains canonical identifiers. Golden files regenerated for all profiles.
- **story-0045-0005 (EPIC-0045):** `x-release --ci-watch` flag enables opt-in CI gate between `x-pr-create` and the Phase 8 APPROVAL-GATE. When enabled, invokes `x-pr-watch-ci` via Rule 13 Pattern 1 INLINE-SKILL; aborts on `CI_FAILED` or `TIMEOUT`, proceeds on `SUCCESS` / `CI_PENDING_PROCEED` / `PR_ALREADY_MERGED` / `NO_CI_CONFIGURED`. `ReleaseSkillTest` gains 9 tests covering the `--ci-watch` phase block.
- **story-0045-0006 (EPIC-0045):** `Epic0045SmokeTest` validates the end-to-end CI-Watch integration contract: 8 stable exit codes (RULE-045-05), `PrWatchStatusClassifier` coverage of all 8 codes, `x-pr-watch-ci` SKILL.md existence and content, golden file regeneration, and `SkillsAssembler` discoverability. SMOKE_E2E=true path creates a live PR and exercises the full flow.

### Changed

- **story-0045-0003 (EPIC-0045):** `x-story-implement` Phase 2.2.8.5: new step between PR_CREATED and the interactive APPROVAL GATE. Invokes `x-pr-watch-ci` via Rule 13 INLINE-SKILL and waits for CI checks + Copilot review to complete before presenting the gate menu. Forces the interactive menu (instead of auto-proceed) on `CI_FAILED` or `TIMEOUT`; proceeds transparently on `SUCCESS` / `CI_PENDING_PROCEED`. Skipped when `planningSchemaVersion == "1.0"` (Rule 19) or `--no-ci-watch` flag present.
- **story-0045-0004 (EPIC-0045):** `x-task-implement --worktree` Step 4.5: waits for CI checks via `x-pr-watch-ci` before the approval gate when running standalone (non-orchestrated). Skipped when invoked by a parent orchestrator (`x-story-implement`, `x-epic-implement`) to avoid double-waiting; skipped when `--no-ci-watch` flag present.

- **story-0043-0001 (EPIC-0043):** Convention — Interactive Gates (ADR-0010 + Rule 20).
  Establishes the canonical 3-option gate menu (`PROCEED` / `FIX-PR` / `ABORT`) as the
  default behavior for all orchestrating skills that pause for human approval
  (`x-release`, `x-story-implement`, `x-epic-implement`, `x-review-pr`).
  - [ADR-0010 — Interactive Gates Convention](adr/ADR-0010-interactive-gates-convention.md):
    documents decision rationale, FIX-PR loop-back with 3-attempt guard-rail, uniform
    state file schema, deprecation of opt-in flags, and grep-based audit enforcement.
  - [Rule 20 — Interactive Gates](`.claude/rules/20-interactive-gates.md`):
    normative rule with 10 mandatory sections — Scope, Canonical Option Menu, State File
    Schema, Default Behavior, FIX-PR Loop-Back, Deprecation of Opt-In Flags, Forbidden,
    Audit Command, Rationale. Source at
    `java/src/main/resources/targets/claude/rules/20-interactive-gates.md`.
  - Golden files regenerated for all 18 profiles; `20-interactive-gates.md` present in
    every golden `.claude/rules/` output.
  - `RulesAssemblerInteractiveGatesTest` (3 tests) verifies rule is copied, has ≥ 10
    sections, and contains canonical slot labels and state schema fields.

- **story-0043-0005 (EPIC-0043):** Retrofit `x-review-pr` exhausted-retry gate — default interactive menu after auto-remediation exhaustion.
  When the Tech Lead review returns NO-GO and all 2 auto-remediation retry cycles exhaust without convergence, `x-review-pr` now
  presents the canonical 3-option gate menu (`PROCEED` / `FIX-PR` / `ABORT`) by default instead of silently returning NO-GO.
  `--non-interactive` is the CI/automation opt-out (preserves legacy HALT text + exit 0).
  - **PROCEED (slot 1):** re-dispatches auto-remediation agents (+2 loops); description contextualises the action per Rule 20 note on `x-review-pr`.
  - **FIX-PR loop-back (slot 2):** invokes `Skill(skill: "x-pr-fix", args: "<PR>")` via Rule 13 Pattern 1 INLINE-SKILL; re-presents menu on return.
  - **Guard-rail:** 3 consecutive PROCEED/FIX-PR attempts without converging to GO trigger `REVIEW_FIX_LOOP_EXCEEDED` auto-terminate.
  - **State file (opt-in):** `plans/review/<pr>/state.json` written only on FIX-PR selection (Rule 20 §5.1 schema); enables `--resume-review <pr>` re-entry.
  - **New error codes:** `REVIEW_REMEDIATION_EXHAUSTED` (ABORT selection), `REVIEW_FIX_LOOP_EXCEEDED` (guard-rail), `GATE_SCHEMA_INVALID` (corrupt state file).
  - **`allowed-tools` updated:** `AskUserQuestion`, `Skill` added per Rule 20 Forbidden clause.
  - Golden files regenerated for all 19 profiles. Rule 13 audit: zero bare-slash violations. All 6066 tests pass.
  - TASK-0043-0005-001 (PR #481), TASK-0043-0005-002 (PR #482), TASK-0043-0005-003 (PR #483).

- **story-0043-0004 (EPIC-0043):** Retrofit `x-epic-implement` batch PR consolidation gate — default interactive menu.
  The batch consolidation gate (§1.3b) now **always** opens the canonical 3-option menu (`PROCEED` / `FIX-PR` / `ABORT`) by default.
  `--non-interactive` is the CI/automation opt-out (auto-approves without prompting). `--manual-batch-approval` is deprecated (no-op, one-time warning).
  - **FIX-PR loop-back:** option 2 invokes `Skill(skill: "x-pr-fix-epic", args: "--epic {EPIC_ID}")` via Rule 13 Pattern 1 INLINE-SKILL,
    records the attempt in `batchGate.fixAttempts[]`, and re-presents the menu on return.
  - **Guard-rail:** 3 consecutive FIX-PR attempts trigger `EPIC_BATCH_FIX_LOOP_EXCEEDED` (auto-terminate, state preserved for `--resume`).
  - **State file extension:** `batchGate` sub-object added to `plans/epic-<ID>/execution-state.json` with fields
    `lastGateDecision` (Enum|null), `fixAttempts[]` (with `attemptNumber`, `delegateSkill`, `invokedAt`, `outcome`),
    `waveIndex` (Integer|null, which wave triggered the gate), and `schemaVersion`. Silent migration for legacy state files.
  - **Golden files** regenerated for all 19 profiles. `ApiFirstPhaseTest` updated to check for new AskUserQuestion gate.
  - TASK-0043-0004-001 was COALESCED with TASK-0043-0003-004 (PR #477); TASK-0043-0004-002 (PR #479); TASK-0043-0004-003 (PR #480).

- **story-0043-0002 (EPIC-0043):** Retrofit `x-release` Phase 8 APPROVAL-GATE — default interactive menu.
  Phase 8 now **always** opens the canonical 3-option gate menu (`PROCEED` / `FIX-PR` / `ABORT`) by default,
  without any flag. `--non-interactive` is the new CI/automation opt-out (prints the legacy HALT text + exits 0).
  `--interactive` (without `--dry-run`) is deprecated as gate opt-in; emits a deprecation warning and is a no-op.
  `--interactive --dry-run` interactive dry-run sub-modality is **preserved unchanged**.
  - **FIX-PR loop-back:** option 2 invokes `Skill(skill: "x-pr-fix", args: "<PR_NUMBER>")` via Rule 13 Pattern 1
    INLINE-SKILL, records the attempt in `fixAttempts[]`, and re-presents the menu on return.
  - **Guard-rail:** 3 consecutive FIX-PR attempts trigger `RELEASE_FIX_LOOP_EXCEEDED` (exit 1, state preserved).
  - **State file extension:** `lastGateDecision` (String|null) and `fixAttempts[]` (Array<FixAttempt>) added to
    `plans/release-state-{VERSION}.json`. Silent migration for legacy state files (≤3.6.0) that lack these fields:
    emits `WARN [RELEASE_STATE_SCHEMA_LEGACY]` and initializes fields to `null`/`[]` on first write.
  - **Reference docs updated:** `references/approval-gate-workflow.md` completely rewritten with Mermaid sequence
    diagram, FIX-PR loop-back decision tree, and Historical Behavior (pre-EPIC-0043) section.
    `references/state-file-schema.md` gains a "Gate Fields (EPIC-0043)" section.
  - Golden files regenerated for all 18 profiles. `ReleaseApprovalGateTest.stepEight_optionTwoFixPr` validates
    the new option 2 semantics (was `stepEight_optionTwoHalt` for the pre-EPIC-0043 menu).

- **story-0042-0003 (EPIC-0042):** `x-pr-merge-train` skill completed with Phases 6–7, full state.json schema, atomic-write pattern, `--resume` entry logic, 16-code Error Handling table, Integration Notes, and ≥ 4 Examples.
  - **Phase 6 — Final Verification:** fetches + pulls `develop`, runs `mvn compile` + `mvn test` smoke checks after all merges, and asserts each merged PR reached `MERGED` state via GitHub API. Any test failure sets `phase = FAILED` with `reason = SMOKE_TEST_FAILED` and preserves the worktree for diagnosis (Rule 14 §4).
  - **Phase 7 — Report + Cleanup:** writes `plans/merge-train/<trainId>/report.md` (PRs Merged, Waves, Errors Observed tables), conditionally removes the worktree via `x-git-worktree` INLINE-SKILL (Rule 13 Pattern 1) when `TRAIN_OWNS_WORKTREE && phase != FAILED`, and finalizes `state.json` with `phase = COMPLETED`.
  - **state.json Complete Schema:** documents all 13 fields including `schemaVersion`, `lastPhaseCompletedAt`, `neuteredParallel`, `waves[]`, and all 14 `phase` enum values, with a full JSON example.
  - **Atomic State Writes:** `.tmp` + `mv` rename pattern documented to prevent `state.json` corruption on SIGKILL mid-write.
  - **`--resume` Entry Logic:** `STATE_CONFLICT` abort when no `state.json` exists; `--train-id` mandatory when multiple state files exist; resumes from next incomplete phase preserving `prsMergedOk[]` and `waves[]`.
  - **Error Handling Table:** 16 error codes with Phase, Condition, and Remediation columns covering all failure modes from `MODE_AMBIGUOUS` through `STATE_CONFLICT`.
  - **Integration Notes Table:** 4 rows documenting relationships with `x-git-worktree`, `x-git-commit`, `x-pr-fix-epic`, and `x-story-implement`.
  - **Examples:** 4 invocations with context (explicit `--prs`, `--epic` auto-discover, `--dry-run` audit, `--resume --train-id` post-conflict recovery).
  - Four TDD test classes added: `MergeTrainSkillPhase6Test` (×2), `MergeTrainSkillSchemaTest` (×1), `MergeTrainSkillErrorHandlingTest` (×1), `MergeTrainSkillExamplesTest` (×1).

- **story-0042-0002 (EPIC-0042):** `x-pr-merge-train` gains Phases 3, 4, and 5 — merge orchestration and parallel rebase subagents.
  - **Phase 3 — Sort + File-Overlap Precheck:** reorders the validated PR list by `createdAt` ascending (or preserves explicit `--prs` order), then computes file-set intersections for every PR pair via `gh pr view --json files`. Any overlap outside `golden/**` forces `MAX_PARALLEL=1` and logs `NEUTERED_PARALLEL` to `state.json` (telemetry only, not an error). The first PR becomes `BASE_PR`; the rest form `TAIL[]`.
  - **Phase 4 — Base PR Merge:** triggers `gh pr merge <BASE_PR> --squash --auto --delete-branch` and polls every 60 s until `state == MERGED` (default 30-minute timeout). Aborts with `MERGE_POLL_TIMEOUT` on timeout or `MERGE_REJECTED_BY_PROTECTION` on branch-protection rejection. Updates `state.json` fields `phase`, `prsMergedOk[]`, and `prsFailed[]`.
  - **Phase 5 — Parallel Tail Orchestration:** wave dispatcher loop dispatches up to `MAX_PARALLEL` rebase workers per wave as sibling `Agent(subagent_type: "general-purpose", ...)` calls in a single assistant message (Rule 13 Pattern 2). Each worker executes the canonical rebase subagent prompt, which embeds the golden-regen block verbatim from `README.md:810-818` (RULE-005), resolves golden conflicts with `--ours` + regen (RULE-004), and aborts with `CODE_CONFLICT_NEEDS_HUMAN` on code conflicts. Workers write structured `worker-<pr>.log` JSON. After all workers in a wave return, OK PRs are merged serially; any `CODE_CONFLICT_NEEDS_HUMAN` or `PUSH_LEASE_REJECTED` failure aborts the train with worktree preserved for diagnosis.
  - `state.json` extended with `neuteredParallel`, `prsMergedOk[]`, `prsFailed[]`, and `waves[]` fields.
  - Four TDD tests added: `MergeTrainSkillPhase3Test`, `MergeTrainSkillPhase4Test` (×1), `MergeTrainSkillPhase5Test` (×2).

- **story-0042-0004 (EPIC-0042):** `x-story-implement` now auto-fixes task PR review comments after Tech Lead GO. New Step 3.6.5 gates on `decision=GO`, discovers task PRs from `execution-state.json`, checks per-PR review comment count via `gh api`, and invokes `Skill(skill: "x-pr-fix", ...)` for each PR with comments. Compile-regression guard aborts with `PR_FIX_COMPILE_REGRESSION` if a fix breaks the build, skipping Step 3.7 without auto-retry (RULE-007 single-pass). Integration Notes and Error Handling tables updated accordingly.

- **story-0045-0002 (EPIC-0045):** Rule 21 — CI-Watch (RULE-045-01) + `scripts/audit-rule-20.sh` regression guard.
  Introduces the normative rule for the CI-Watch requirement: skills that invoke `x-pr-create` via
  `Skill(skill: "x-pr-create", ...)` MUST also invoke `Skill(skill: "x-pr-watch-ci", ...)` immediately after,
  unless the caller declares `--no-ci-watch` as the sole opt-out token.
  - **Fallback Matrix (3 rows):** V1 no-op (`planningSchemaVersion` absent or `"1.0"` — Rule 19 backward compat),
    V2 active (default for `"2.0"`), V2 skipped (`--no-ci-watch` opt-out).
  - **`scripts/audit-rule-20.sh`:** grep-based CI guard; scans every `SKILL.md` under the skills source tree,
    exits 0 (PASS) when all files invoking `x-pr-create` also invoke `x-pr-watch-ci` or declare `--no-ci-watch`;
    exits 1 (FAIL) with per-file violation messages otherwise. Supports `--skills-dir <path>` for test isolation.
    Fixed macOS `grep -F` flag-parsing bug: opt-out pattern matched via `grep -qE -- '--no-ci-watch'`.
  - **`RulesAssemblerCiWatchTest`** (3 tests): verifies `21-ci-watch.md` is copied into assembled output,
    has all 5 mandatory sections, and contains canonical identifiers (`RULE-045-01`, fallback variants,
    `x-pr-watch-ci`, `x-pr-create`, `audit-rule-20.sh`).
  - **`Rule20AuditTest`** (5 tests via `ProcessBuilder`): compliant SKILL.md exits 0; `--no-ci-watch`
    opt-out exits 0; missing CI-Watch exits 1 with mention of `x-pr-create`; no `x-pr-create` exits 0;
    prose-only mention exits 0.
  - Golden files regenerated for all 19 profiles; rule count 20 → 21; `21-ci-watch.md` entry added to
    every `.claude/README.md` golden output.
  - `CLAUDE.md` updated with EPIC-0045 "In progress" block.

## [3.9.0] - 2026-04-19

### Added

- **EPIC-0041 — File-Conflict-Aware Parallelism Analysis (8 stories).** Introduces planning-time detection of hotspot collisions between stories/tasks scheduled in the same wave, ending the "topological-only parallelism" anti-pattern that produced merge conflicts on `SettingsAssembler.java`, `HooksAssembler.java`, `CLAUDE.md`, `CHANGELOG.md`, `pom.xml`, and the golden-file tree across EPIC-0036..EPIC-0040. Architecture recorded in [ADR-0006](adr/ADR-0006-file-conflict-aware-parallelism.md). _Slot ADR-0005 was taken by telemetry (EPIC-0040) before this epic shipped, hence ADR-0006._
  - **story-0041-0001:** New knowledge pack `parallelism-heuristics` — catalogues collision categories (hard / regen / soft), hotspot list, and degrade-with-warning policy. Single reference for every downstream planning skill.
  - **story-0041-0002:** `x-task-plan` now emits a mandatory `## File Footprint` block (`### write:` / `### read:` / `### regen:`) on every `plan-task-TASK-*.md`. New `FileFootprintParser` rejects unknown headings to keep parsing deterministic.
  - **story-0041-0003:** `x-story-plan` Phase 6 aggregates task footprints into a canonical `## Story File Footprint` on each `plan-story-*.md`. New `StoryFootprintAggregator` unions the sub-sections with alphabetic deduplication.
  - **story-0041-0004:** New skill `/x-parallel-eval --scope=epic|story|task` (Java domain + CLI + SKILL.md). Emits a deterministic collision matrix and a reagrupment recommendation; backed by `dev.iadev.parallelism.*` (23 tests). Output ordering is alphabetic so `GoldenFileCoverageTest` can pin the shape.
  - **story-0041-0005:** `x-epic-map` Step 8.5 invokes `/x-parallel-eval --scope=epic` and renders a new "8.5 Restrições de Paralelismo" section on the Implementation Map; `_TEMPLATE-IMPLEMENTATION-MAP.md` gains the matching placeholder. `EpicMapStep85IT` covers the happy path + zero-collision fallback.
  - **story-0041-0006:** Parallelism gate in `x-epic-implement` Phase 0.5.0 and `x-story-implement` Phase 1.5. On collision, the executor **degrades the wave to serial and logs a visible warning** listing the conflicting pairs — never aborts (RULE-005). Downgrades persisted on `ExecutionState.parallelismDowngrades` for audit.
  - **story-0041-0007:** Retroactive `/x-parallel-eval` reports + `.diff` patches for epics 0036..0040 under `plans/epic-0041/migrations/`. No map was auto-edited (human-review gate). EPIC-0040 flagged HIGH — hard conflict on `telemetry-phase.sh`.
  - **story-0041-0008 (this story, docs):** ADR-0006 publication, `CLAUDE.md` executive-summary note for EPIC-0041, and this CHANGELOG entry. Release tasks (version bump + Git Flow release branch + tag) are delivered by a follow-up `/x-release` invocation per Rule 09.

### Changed

- **`ExecutionState`** gained optional `parallelismDowngrades` field capturing every wave demoted from parallel to serial by the new gate (pair of plan IDs + hotspot path + timestamp) — lets post-mortems answer "did parallelism degrade today, and on which pair?" without re-running the analysis.
- **Planning skill contracts** — `x-task-plan` and `x-story-plan` now require the new footprint sections; legacy plans without a footprint are handled as "unknown" (warn, do not block) per RULE-006.
- **Root `CLAUDE.md`** gained an "EPIC-0041 (Concluded)" executive-summary block with links to ADR-0006, the migrations directory, and `/x-parallel-eval`.

### Removed

- **EPIC-0044 / story-0044-0001 — Removed 4 deprecated symbols from `StackMapping`** (`forRemoval = true`, originally introduced in EPIC-0023). All remaining consumers were migrated in this story (`PermissionCollector`); substitutes with identical signatures already exist in `DatabaseSettingsMapping`, so no API break for external callers.
  - Removed `StackMapping.DATABASE_SETTINGS_MAP` — use `DatabaseSettingsMapping.DATABASE_SETTINGS_MAP`.
  - Removed `StackMapping.CACHE_SETTINGS_MAP` — use `DatabaseSettingsMapping.CACHE_SETTINGS_MAP`.
  - Removed `StackMapping.getDatabaseSettingsKey(String)` — use `DatabaseSettingsMapping.getDatabaseSettingsKey(String)`.
  - Removed `StackMapping.getCacheSettingsKey(String)` — use `DatabaseSettingsMapping.getCacheSettingsKey(String)`.
- **EPIC-0044 / story-0044-0002 — Removed 2 deprecated `resolveResourcesRoot` overloads from `ResourceResolver`** (`forRemoval = true`). All 23 assemblers + 9 test helpers migrated to depth-free `resolveResourceDir(String)`; `depth` parameter eliminated. Callers that need the resources root obtain it via `resolveResourceDir("shared").getParent()`.
  - Removed `dev.iadev.util.ResourceResolver.resolveResourcesRoot(String)` — use `ResourceResolver.resolveResourceDir(String)`.
  - Removed `dev.iadev.util.ResourceResolver.resolveResourcesRoot(String, int)` — use `ResourceResolver.resolveResourceDir(String)` (depth parameter eliminated).

## [3.8.0] - 2026-04-17

### Added
- **EPIC-0040 — Telemetria de Execução de Skills (11 implementation stories + docs story).** Hybrid telemetry architecture that captures skill executions, phase boundaries, subagent lifecycles, and tool calls as NDJSON under `plans/epic-*/telemetry/events.ndjson`. Enabled by default; opt-out via `CLAUDE_TELEMETRY_DISABLED=1` or `telemetryEnabled: false`. Architecture recorded in [ADR-0005](adr/ADR-0005-telemetry-architecture.md); privacy contract in [Rule 20 — Telemetry Privacy](.claude/rules/20-telemetry-privacy.md).
  - **story-0040-0001 (PR #408):** NDJSON event schema + storage spec. New reference `_TEMPLATE-TELEMETRY-EVENT.json` documents the canonical event shape shared by the shell and Java layers.
  - **story-0040-0002 (PR #411):** Java domain package `dev.iadev.telemetry` — `TelemetryEvent` record, `TelemetryWriter` (append-only NDJSON with `flock`/mkdir lock), `TelemetryReader` (streaming parser tolerant of partial lines), zero-framework imports (Rule 04).
  - **story-0040-0003 (PR #410):** Six Bash hook scripts under `java/src/main/resources/targets/claude/hooks/` — `telemetry-session.sh`, `telemetry-pretool.sh`, `telemetry-posttool.sh`, `telemetry-subagent.sh`, `telemetry-stop.sh`, and the helper `telemetry-emit.sh`. Fail-open (`set +e`), 5 s stdin timeout, scrubbing on the write path.
  - **story-0040-0004 (PR #414):** `SettingsAssembler` injects telemetry hook registrations into the generated `settings.json`; `HooksAssembler` copies the source-of-truth scripts to `.claude/hooks/`. Zero-touch adoption for projects regenerated on 3.8.0.
  - **story-0040-0005 (PR #413):** `TelemetryScrubber` (PII / secret scrubbing) + Rule 20 — Telemetry Privacy. Scrubber is on the Java writer's write path; shell scripts share the same regex subset in `telemetry-emit.sh`.
  - **story-0040-0006 (PR #415):** Phase markers in implementation skills (`x-epic-implement`, `x-story-implement`, `x-task-implement`).
  - **story-0040-0007 (PR #416):** Phase + subagent markers in planning skills (`x-task-plan`, `x-story-plan`, `x-arch-plan`, `x-epic-map`, `x-test-plan`).
  - **story-0040-0008 (PR #418):** Phase + MCP markers in creation skills (`x-epic-create`, `x-story-create`, `x-epic-decompose`, `x-jira-create-epic`, `x-jira-create-stories`).
  - **story-0040-0009 (PR #419):** `_TEMPLATE-SKILL.md` gained a "Telemetry (Optional)" section with copy-paste-ready `telemetry-phase.sh start|end`, `telemetry-phase.sh subagent-start|subagent-end`, and `telemetry-phase.sh mcp-start|mcp-end` snippets.
  - **story-0040-0010 (PR #420):** New skill `/x-telemetry-analyze` — point-in-time report with per-skill / phase / tool aggregates, Mermaid Gantt timeline, and optional JSON/CSV export (`ops/` category).
  - **story-0040-0011 (PR #422):** New skill `/x-telemetry-trend` — cross-epic P95 regression detector with top-10 slowest skills ranking; single-responsibility partner of `/x-telemetry-analyze`.
  - **story-0040-0012 (this story, docs):** ADR-0005 publication, `CLAUDE.md` + `readme-template.md` "Telemetry" section, and this CHANGELOG entry. Release tasks 004-006 (version bump, release branch, tag `v3.8.0`, back-merge to `develop` with `3.9.0-SNAPSHOT` bump) are delivered by a follow-up `/x-release 3.8.0` invocation per Rule 09 (Git Flow).
- **Storage layout:** `plans/epic-XXXX/telemetry/events.ndjson` (per-epic, committed) and `.claude/telemetry/index.json` (cross-epic cache, gitignored).
- **Rule 20 — Telemetry Privacy (`.claude/rules/20-telemetry-privacy.md`).** Mandates scrubbing through `TelemetryScrubber` (or the shell regex chain) before any write to `events.ndjson`. Committed NDJSON is safe to republish by policy.

### Changed
- **`ExecutionState`** gained optional `telemetryPath` field pointing to the active `events.ndjson` location for the current epic.
- **`_TEMPLATE-SKILL.md`** gained a "Telemetry (Optional)" section (story-0040-0009) with plug-and-play phase / subagent / MCP helper calls to keep new skills instrumented without hand-rolling the wiring.
- **`ProjectConfig.telemetryEnabled`** — new boolean field (default `true`); when set to `false`, `SettingsAssembler` omits the telemetry hook registrations from the generated `settings.json`.
- **Root `CLAUDE.md` and `readme-template.md`** gained a "Telemetry" executive-summary section with links to ADR-0005, Rule 20, `/x-telemetry-analyze`, and `/x-telemetry-trend`.

### Security
- **Rule 20 (Telemetry Privacy)** introduces the scrubber-on-write-path invariant. Pattern catalog covers AWS access keys, JWT-shaped tokens, and `Bearer` authorization headers (shell layer); the Java `TelemetryScrubber` extends the catalog with additional PII / secret patterns.

## [3.6.0] - 2026-04-16

### Added
- **EPIC-0042-A — Enforce Quality Gates, Auto-Remediation, and Continuous Execution (9 stories, ~209 golden files regenerated across 17 profiles + 2 platform variants).** Closes 9 critical gaps in the development lifecycle around test enforcement, smoke gate coverage, continuous execution, NO-GO auto-remediation, and review report visibility. _Note: renamed from EPIC-0042 to EPIC-0042-A to disambiguate from the in-flight `EPIC-0042 — Merge-Train Automation + Auto PR-Fix Hook` (under `plans/epic-0042/`); the nested story IDs were also renamed from `STORY-0042-*` to `story-0042-A-*` (lowercase, matching the EPIC-0039 / EPIC-0038 convention) as part of the same disambiguation; see `feature/epic-0042-merge-train-automation` branch._
  - **story-0042-A-0001 — Mandatory Test Execution in x-review-pr.** Tech Lead review (`x-review-pr`) now runs `{{TEST_COMMAND}}` + `{{COVERAGE_COMMAND}}` + `{{SMOKE_COMMAND}}` before emitting GO/NO-GO. Any test failure, coverage below 95% line / 90% branch, or smoke failure forces **automatic NO-GO** (overrides rubric score). Rubric section I (Tests & Execution) expanded from 3 to 6 points.
  - **story-0042-A-0002 — Smoke Test Execution in Review Skills.** QA review (`x-review-qa`) gains items QA-19 (smoke test existence) and QA-20 (smoke test pass) plus a dedicated Smoke Test Verification step. Max score moves from /36 → /40. Smoke failure forces `STATUS: Rejected`. When `testing.smoke_tests == false`, QA-19/QA-20 are automatically N/A.
  - **story-0042-A-0003 — Make Coverage Thresholds Blocking in x-test-tdd.** Phase 3b coverage check promoted from WARNING to BLOCK — TDD cycle cannot advance to REFACTOR when coverage falls below 95% line or 90% branch. New `--warn-only-coverage` flag preserves legacy non-blocking behavior for migration.
  - **story-0042-A-0004 — Enforce Smoke Tests as Hard Gate in x-story-implement.** Phase 3 Step 3.8 reclassifies smoke test failures: Health Check / Critical Path failures **fail Phase 3** (hard gate); Response Time / Error Rate remain advisory. New `--skip-smoke` flag is the only explicit escape hatch.
  - **story-0042-A-0005 — Remove --skip-smoke-gate and Make Epic Integrity Gate Mandatory.** The `--skip-smoke-gate` flag has been removed entirely from `x-epic-implement`. Smoke gate is mandatory whenever `{{SMOKE_COMMAND}}` is configured. Phase 3 test failures block epic completion. `--no-merge` deferred gates now emit explicit WARNING about pending validation.
  - **story-0042-A-0006 — Default to Continuous Execution — Auto-Execute with Opt-Out.** Procedural `AskUserQuestion` calls removed in favor of flag-based opt-out across 6+ skills: `--no-auto-fix-story` (x-review), `--manual-contract-approval` / `--manual-task-approval` (x-story-implement), `--manual-batch-approval` (x-epic-implement), `--no-merge` as opt-out from new auto-merge default, `--dry-run-only-comments` (x-epic-implement PR fix), `--jira <KEY>` / `--no-jira` (x-epic-create, x-epic-decompose, x-story-create). CRITICAL security findings still pause for human review.
  - **story-0042-A-0007 — NO-GO Auto-Remediation Agent Dispatch.** When review fails, the orchestrator now classifies findings (TEST_FAILURE / COVERAGE_GAP / CODE_QUALITY) and dispatches a `general-purpose` agent via `Agent(...)` to fix the failing tests / add missing coverage / resolve code quality issues, then re-runs the review automatically (max 2 cycles). Applied in `x-review-pr` Step 8, `x-review` Phase 4b, `x-story-implement` Phase 3 Step 3.5, and `x-epic-implement` integrity gate (agent-assisted fix attempted before `git revert`). New flags `--no-auto-remediation` and `--revert-on-failure` preserve manual control.
  - **story-0042-A-0008 — Review Report Visibility and Terminal Progress Display.** All review artifacts now use explicit `Write(file_path: "...", content: "...")` instructions to guarantee disk persistence. `TodoWrite` trackers expose per-specialist progress during parallel `x-review` dispatch and per-sub-step progress during `x-story-implement` Phase 3 reviews. Consolidated summary boxes print to terminal after specialist reviews complete (mirroring the Tech Lead box format). Epic-level Review Summary table aggregates per-story results.
  - **story-0042-A-0009 — Golden File Regeneration and Cross-Skill Consistency.** All goldens regenerated via `GoldenFileRegenerator`; `mvn verify` passes (813 tests / 0 failures / coverage green). CLI flag tables and CHANGELOG cross-audited for consistency across 19 source skill files.
- **EPIC-0039 — `x-release` interactive flow with auto-versioning, smart resume, and observability (15 stories, 15 story PRs).** Reshapes the release skill around interactive operator ergonomics, reduces pre-release surprises, and adds per-phase telemetry. All additions preserve the existing `--continue-after-merge` / `--no-prompt` CI paths (RULE-005 behaviour preservation).
  - **story-0039-0001 — Auto-detect next version from Conventional Commits.** New pure-domain pipeline `GitTagReader` → `ConventionalCommitsParser` → `VersionBumper` derives MAJOR/MINOR/PATCH from commits since the last `v*` tag when no positional bump or `--version` is supplied. 4 new exit codes (`VERSION_NO_BUMP_SIGNAL`, `VERSION_NO_COMMITS`, `VERSION_INVALID_FORMAT`, `VERSION_TAG_NOT_FOUND`). Reference: `auto-version-detection.md`.
  - **story-0039-0003 — Pre-commit integrity checks in VALIDATE-DEEP.** New sub-check 10 (`VALIDATE_INTEGRITY_DRIFT`) detects cross-file drift before the release branch is created: empty `[Unreleased]` in CHANGELOG, version misalignment between `pom.xml` and `README.md`/`CLAUDE.md` badges, and new `TODO`/`FIXME`/`HACK`/`XXX` markers in the commit window (WARN only). New pure-domain `IntegrityChecker` (3 checks) + `IntegrityReport` aggregator + `DiffTodoScanner` + `VersionExtractor` (regex-based, no XML parser to avoid XXE) + `RepoFileReader` adapter (UTF-8, path-traversal rejected). Flags `--skip-integrity` (not recommended) and `--integrity-report <path>`.
  - **story-0039-0004 — Parallelization of VALIDATE-DEEP.** New `dev.iadev.release.validate.ParallelCheckExecutor` (domain-pure, 97% line / 100% branch) with `CheckSpec`/`CheckOutcome`/`CheckResult`/`AggregatedResult`/`Severity` dispatches the 7 post-build checks in parallel via shell `&` + `wait $PID`. Alphabetic sort of `VALIDATE_*` codes for deterministic abort, per-check telemetry `[VALIDATE-DEEP] {name}: {seconds}s {severity}`, and new `--max-parallel <N>` (1..16, default `min(CPU,4)`). Benchmark test (`ValidateDeepBenchmarkTest`) enforces ≥40% wall-clock reduction; smoke (`ValidateDeepParallelSmokeTest`) validates abort on forced failure. All original `VALIDATE_*` codes preserved (RULE-005).
  - **story-0039-0005 — Phase 13 SUMMARY with Git Flow diagram.** New `SummaryRenderer` produces a post-release cycle explainer: ASCII Git Flow diagram (`develop`/`release/X.Y.Z`/`main` lanes, back-merge arrows), PR numbers, tag, GitHub Release link, total duration, and Top-3 slowest phases vs historical median (joined with telemetry JSONL when available). `--no-summary` suppresses the block for CI. Reference: `git-flow-cycle-explainer.md`.
  - **story-0039-0006 — GitHub Release auto-creation with confirmation.** New Phase 11 prompt asks the operator whether to invoke `gh release create v<V> --notes-file <changelog-slice>`. `--no-github-release` skips the prompt (CI path). Errors (`gh` auth, rate-limit, API) surface as `PUBLISH_GH_RELEASE_FAILED` warn-only — tag push never blocks on Release creation.
  - **story-0039-0007 — Interactive prompts at every halt point.** New `PromptEngine` (`dev.iadev.release.prompt`) resolves 3 halt points (`APPROVAL_GATE`, `BACKMERGE_MERGE`, `RECOVERABLE_FAILURE`) via `AskUserQuestion`; `waitingFor`/`nextActions` persisted to state file before every prompt. `--no-prompt` disables all prompts; combined with `--interactive` raises `INTERACTIVE_REQUIRES_DRYRUN` (exit 1). Reference: `prompt-flow.md`.
  - **story-0039-0008 — Smart Resume of orphaned state files.** When `/x-release` is invoked on a stale state file (`phase != COMPLETED`) and `--no-prompt` is absent, the skill now prompts `[1] Resume | [2] Abort | [3] Start new` (Start-new offered only when new commits exist since the orphaned state was written). Replaces the blunt `STATE_CONFLICT` abort; that code is still emitted in `--no-prompt` for CI parity. New `SmartResumeOrchestrator` + `StateFileDetector` + smoke.
  - **story-0039-0009 — Pre-flight dashboard before branching.** New Step 1.5 (`PRE-FLIGHT`) renders a consolidated dashboard (target version, commit counts, CHANGELOG preview with `--preflight-changelog-lines <N>`, integrity results, execution plan) and prompts `[1] Prosseguir | [2] Editar versão | [3] Abortar`. Integrity FAIL blocks without prompt (`PREFLIGHT_INTEGRITY_FAIL`); `--no-preflight` skips the step entirely. New `PreflightDashboardRenderer` (pure function over `DashboardData`) with CWE-sanitization of version strings and CHANGELOG body (OWASP A05).
  - **story-0039-0010 — Operational commands `--status` and `--abort`.** `--status` reads the state file and renders version, phase, PR URL, last-activity age, waiting-for state, and suggested next actions. Safe from any branch (read-only). `--abort` performs double-confirmation cleanup: closes open PRs via `gh pr close`, deletes local + remote branches, removes state file. Individual cleanup failures are warn-only (exit 0). New error codes: `STATUS_PARSE_FAILED`, `ABORT_NO_RELEASE`, `ABORT_USER_CANCELLED`, `ABORT_PR_CLOSE_FAILED`, `ABORT_BRANCH_DELETE_FAILED`. `--yes`/`--force` bypasses both prompts with a "FORCE MODE" warning.
  - **story-0039-0011 — Handoff to `/x-pr-fix`.** `PromptEngine` exposes a second option at `APPROVAL_GATE` and `BACKMERGE_MERGE` that hands control to `/x-pr-fix <PR#>` and comes back to the same halt point on completion. New `HandoffOrchestrator` + `HandoffLoopSmokeTest` confirm state is preserved across the round-trip.
  - **story-0039-0012 — Per-phase telemetry to `plans/release-metrics.jsonl`.** New `ReleaseTelemetryWriter` appends a JSONL line per phase with `{phase, version, releaseType, ts, ...}`. Phase 13 SUMMARY's Top-3 benchmark reads this file via `BenchmarkAnalyzer` and degrades gracefully when it's absent. `--telemetry off` installs `NoopTelemetrySink` (CI privacy / debug).
  - **story-0039-0013 — Interactive dry-run for onboarding.** `/x-release --dry-run --interactive` pauses before each of the 13 phases and records outcomes without invoking `git`, `mvn`, or `gh` (zero side effects). Passing `--interactive` without `--dry-run` aborts with `INTERACTIVE_REQUIRES_DRYRUN`. New `DryRunInteractiveSmokeTest`.
  - **story-0039-0014 — Hotfix parity with release flow.** `--hotfix` now runs the same interactive lifecycle (auto-detect, prompts, smart resume, pre-flight, summary, telemetry) with three parametric deviations driven by `ReleaseContext.forHotfix()`: PATCH-only guard (`HOTFIX_INVALID_COMMITS`, `HOTFIX_VERSION_NOT_PATCH`), `main` base branch, hotfix state-file naming (`plans/release-state-hotfix-<V>.json`). `VersionDetector`, `StateFileDetector`, `PreflightDashboardRenderer`, `SummaryRenderer`, and `ReleaseTelemetryWriter` are now context-aware. `HotfixInteractiveSmokeTest` covers 6 Gherkin scenarios. `releaseType=hotfix` is derived from the context — no redundant state-file field.
  - **story-0039-0015 — Doc + golden regen final consolidation.** New `references/interactive-flow-walkthrough.md` with end-to-end example sessions for normal release and hotfix. `SKILL.md` gets a "Reference Documents" index linking all reference files to their story origins. 3 new plan templates registered in `PlanTemplateDefinitions` (`_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md`): `TEMPLATE_COUNT` bumped 17 → 20. 17 profiles regenerated in a single consolidated pass (RULE-008).

### Changed
- **State file schema: v1 → v2 (breaking).** New required fields `nextActions`, `waitingFor`, `phaseDurations`, `lastPromptAnsweredAt`, and `githubReleaseUrl`. Bumped via `schemaVersion: 2`; v1 state files emit `STATE_SCHEMA_VERSION` and must be recreated. Documented in `state-file-schema.md`.
- **Golden files across 17 profiles regenerated** to reflect: rewritten `x-release/SKILL.md`, 7 x-release references (new `interactive-flow-walkthrough.md` + 6 rewritten from earlier stories), and 3 new plan templates in `.claude/templates/`. Byte-for-byte parity verified across all profiles.
- **`PlanTemplateDefinitions.TEMPLATE_COUNT`** bumped 17 → 20 (3 new templates registered).

### Removed
- **`STATE_CONFLICT` abort in interactive mode** replaced by the Smart Resume prompt (story-0039-0008). The code is still emitted in `--no-prompt` for CI parity — operators running interactively no longer see a blunt abort when picking up an orphaned release.

## [3.5.0] - 2026-04-15

### Added
- **EPIC-0038 — Task-First Planning & Execution Architecture (10 stories, 25 task PRs).** Inverts the planning paradigm from top-down (Epic → Story → Task-as-sub-section) to bottom-up (Task atomic & testable → Story as value aggregation → Epic as cross-cutting grouper). Tasks become first-class artifacts with own files, explicit I/O contracts, per-task DoD, formal testability declaration, and atomic commits.
  - **story-0038-0001** (PR #345): `task-TASK-XXXX-YYYY-NNN.md` schema + `TaskFileParser` (99% line/96% branch) + `TaskValidator` with 6 SRP rules (TF-SCHEMA-001..006, 100% line/100% branch) + migrated exemplar + smoke + golden stability test.
  - **story-0038-0002** (PR #353): `task-implementation-map-STORY-*.md` schema + domain model (`TaskGraph`, `TaskNode`, `Wave`, `Edge`) + `TopologicalSorter` (Kahn + coalesced collapse) + 4 exceptions + `TaskMapMarkdownWriter` + `TaskImplementationMapGenerator` use case + `task-map-gen` CLI (picocli) + 7-task golden fixture with byte-for-byte parity + wallclock < 200ms E2E.
  - **story-0038-0003** (PR #355): `x-task-plan` refactored as dual-mode callable skill (task-file-first for EPIC-0038 + legacy story-scoped for epics 0025-0037). Dedicated 4-phase flow (Validate → Extract Contracts → Generate TDD Cycles → Write Plan) with 4 exit codes.
  - **story-0038-0004** (PR #357): `x-story-plan` v2 extensions — schema-aware flow adding Phases 4a-4c (task breakdown with I/O contracts, parallel `x-task-plan` invocation, task-implementation-map generation) + per-task DoR extensions. v1 legacy flow preserved.
  - **story-0038-0005** (PR #359): `x-task-implement` v2 extensions — task-first execution path reading task-file + plan + map; pre-execution gates (dependencies / testability / schema); per-cycle TDD loop in TPP order; post-execution output verification via grep/assert/test; atomic commit via `x-git-commit` with `task(TASK-NNN)` scope.
  - **story-0038-0006** (PR #361): `x-story-implement` wave dispatcher — reads `task-implementation-map` and invokes `x-task-implement` per task, honouring declared parallelism; integration verification after each wave; coalesced wave handling; story-level aggregation report.
  - **story-0038-0007** (PR #363): `x-epic-implement` simplification — scope reduction (task management fully delegated to `x-story-implement`); epic orchestrator retains only story-level concerns; execution-state task fields treated read-only from epic perspective.
  - **story-0038-0008** (PR #365): `planningSchemaVersion` gate + `SchemaVersionResolver` (domain class) — soft-fallback matrix (NO_FILE / MISSING_FIELD / INVALID_VALUE → V1; `"1.0"` → V1 no fallback; `"2.0"` → V2; unparseable JSON → hard fail). Compatibility matrix documented in `x-epic-implement` SKILL.md.
  - **story-0038-0009** (PR #367): Two new templates (`_TEMPLATE-TASK.md`, `_TEMPLATE-TASK-IMPLEMENTATION-MAP.md`) + 5 new rule files in slots 15-19 (RULE-TF-01 Task Testability; RULE-TF-02 I/O Contracts; RULE-TF-03 Topological Execution; RULE-TF-04 Atomic Task Commits; RULE-TF-05 Backward Compatibility). `expected-artifacts.json` bumped (claude-rules: 13 → 18).
  - **story-0038-0010** (PR #369): `TaskFirstE2EIntegrationTest` exercising the full Java contract pipeline (schema resolver → parser → validator → generator → CLI) against a 3-task synthetic epic; anti-pattern grep sanity ensuring no rule file endorses the legacy "task embedded in story" shape.

### Changed
- **Golden files across 17 profiles + 2 platform variants regenerated** to reflect: 5 new rule files (15-19), `x-task-plan` / `x-story-plan` / `x-task-implement` / `x-story-implement` / `x-epic-implement` SKILL.md v2 appendices, and the two new task-first templates.
- **`expected-artifacts.json`** updated: `claude-rules: 18`, `claude-templates: 18` (was 13 and 16 respectively).

### Fixed
- **PR #370 (post-merge fix):** `_TEMPLATE-TASK.md` and `_TEMPLATE-TASK-IMPLEMENTATION-MAP.md` were shipped in story-0038-0009 but never registered in `PlanTemplateDefinitions` — the generator silently skipped them. Now registered with mandatory sections; `TEMPLATE_COUNT` bumped 15 → 17. Verified byte-for-byte parity across 5 sampled profiles.

### Quality gates
- `mvn clean verify -P all-tests`: **770 tests, 0 failures, 0 errors, ~4m31s**
- JaCoCo: **all coverage checks met** (≥95% line / ≥90% branch per project)
- 318 classes analyzed
- Byte-for-byte golden parity across 17 profiles

### Backward compatibility
EPIC-0038 itself runs in `planningSchemaVersion = "1.0"` per spec §8.2 bootstrap rule. Legacy epics (0025-0037) resolve to V1 with `NO_FILE` / `MISSING_FIELD` fallback and run unchanged — zero regression. The v2 task-first flow activates only when an epic explicitly declares `"2.0"`. First dogfood target is the next epic after EPIC-0038.

## [3.4.0] - 2026-04-14

### Added
- **EPIC-0037 — Closing story 0037-0008 (`x-release` worktree-aware Phase BRANCH + Phase CLEANUP-WORKTREE):** the `x-release` skill now provisions a dedicated git worktree under `.claude/worktrees/release-X.Y.Z/` (or `hotfix-{slug}/`) for the entire release flow — created in Phase BRANCH (3 substeps: detect-context → idempotent create → persist `worktreePath` to state file) and removed in the new Phase CLEANUP-WORKTREE after `RESUME-AND-TAG`. Worktree persists through `APPROVAL-GATE` (which can take hours) so the main checkout stays free for concurrent work. Includes security hardening: `HOTFIX_SLUG` regex validation with explicit `\r\n` rejection + whole-string anchoring (`grep -Eqx`), branch-match check on idempotent reuse, and canonical path + prefix assertion. EPIC-0037 closes at 10/10 stories merged.

### Changed
- **State file schema (`x-release/references/state-file-schema.md`) extended additively (schemaVersion=1 unchanged):**
  - New optional field `worktreePath` (string, abs path; populated in Phase BRANCH, cleared in Phase CLEANUP-WORKTREE)
  - New `phase` enum value `WORKTREE_CLEANED`
  - 4 new error codes: `WT_RELEASE_CREATE_FAILED`, `WT_RELEASE_REMOVE_FAILED`, `WT_SLUG_INVALID`, `WT_RELEASE_BRANCH_MISMATCH`
  - 2 new validation rules (#11 — schema additivity audit; #12 — slug security validation)
- **Dependency bumps (Dependabot):**
  - `org.graalvm.buildtools:native-maven-plugin` → 1.0.0
  - `org.jacoco:jacoco-maven-plugin` → 0.8.14
  - `org.apache.maven.plugins:maven-surefire-plugin` → 3.5.5
  - `jackson.version` → 2.21.2 (from 2.18.2)
  - `picocli.version` → 4.7.7 (from 4.7.6)
  - `actions/checkout` → 6 (from 4)

## [3.3.0] - 2026-04-14

### Added
- **EPIC-0037 — Worktree-First Branch Creation Policy (9 stories merged: 0001–0007, 0009–0010; story 0008 deferred pending EPIC-0035-driven follow-up):**
  - New rule file `.claude/rules/14-worktree-lifecycle.md` (Rule 14 — Worktree Lifecycle) — codifies naming convention, protected branches, non-nesting invariant, lifecycle, and the Creator-Owns-Removal matrix that all branch-creating skills MUST follow.
  - New ADR-0004 — Worktree-First Branch Creation Policy (orthogonal but related to ADR-0003); accepted 2026-04-13. Five sub-decisions D1–D5 covering the deprecation of `Agent(isolation:"worktree")`, the standalone-vs-orchestrator opt-in matrix, the non-nesting invariant, Creator-Owns-Removal, and Rule 14 as first-class.
  - New `x-git-worktree` Operation 5 (`detect-context`): read-only probe that returns `{inWorktree, worktreePath, mainRepoPath}` JSON. Used by every branch-creating skill to decide whether to REUSE an existing worktree or CREATE a new one. Includes CWE-116 hardening on JSON-emitted paths.
  - `--worktree` opt-in flag added to `x-story-implement` and `x-task-implement` for standalone parallel use; `x-epic-implement` always provisions a worktree per dispatched story (orchestrator-automatic per ADR-0004 §D2).
  - `STORY_OWNS_WORKTREE` and `TASK_OWNS_WORKTREE` boolean state bridges between Phase 0 (mode selection) and the late-phase cleanup steps; eliminates the prior coupling of "branch creation mode" and "removal decision".
- **Destructive prune in `SkillsAssembler` (EPIC-0036 follow-up):** `SkillsAssembler.assemble()` now removes top-level directories under `{output}/skills/` that do not appear in the generated set, so renames or deletions in the source of truth (`java/src/main/resources/targets/claude/skills/`) are reflected in `.claude/skills/` on the next regeneration. `knowledge-packs/` and `database-patterns/` are explicitly protected because they are written by `RulesAssembler` earlier in the pipeline. Covered by `SkillsAssemblerPruneTest` (7 scenarios).

### Changed
- **EPIC-0037 — Branch creation across orchestrators and standalone skills migrated to explicit `Skill(skill: "x-git-worktree", ...)` calls (Rule 13 Pattern 1):**
  - `x-epic-implement` parallel dispatch now provisions a worktree per story via `x-git-worktree create` BEFORE launching each subagent, and removes via `x-git-worktree remove` after PR merge (or after SUCCESS in `--no-merge` mode). Sequential mode follows the same cleanup criteria.
  - `x-story-implement` Phase 0 Step 6 reorganised into substeps 6a–6e (detect-context → mode selection → execution → logging → record state); Phase 3 Step 10 is mode-aware per Rule 14 §2 (no `develop` checkout inside a worktree).
  - `x-task-implement` Step 0.5 mirrors the same pattern at task level; new Step 0.5f resolves the `--base` argument from current HEAD (story branch when on `feat/story-XXXX-YYYY-...`, else `develop`); Step 5 is mode-aware.
  - `x-git-worktree` SKILL.md error table now standardises `PROTECTED_BRANCH` exit code (Rule 14 §2 mandate); Integration Notes rewritten to reflect the post-EPIC-0037 reality (no more "Pending" entries).

### Deprecated
- **`Agent(isolation:"worktree")` harness parameter (EPIC-0037 / ADR-0004 §D1):** the harness-native worktree isolation is replaced by explicit `x-git-worktree create` / `remove` calls. Zero active uses remain in the source of truth (`java/src/main/resources/targets/`); remaining grep matches are anti-pattern notes documenting the deprecation.

### Removed
- **14 stale EPIC-0036 output directories from `.claude/skills/`:** `x-dev-implement`, `x-dev-story-implement`, `x-dev-epic-implement`, `x-dev-architecture-plan`, `x-dev-arch-update`, `x-dev-adr-automation`, `x-epic-plan`, `x-story-epic`, `x-story-epic-full`, `x-story-map`, `x-pr-fix-comments`, `x-pr-fix-epic-comments`, `x-runtime-protection`, `run-e2e`. These legacy directories were renamed in EPIC-0036 but persisted in the committed output because the previous assembler was additive-only. The new prune step prevents regressions.

## [3.2.0] - 2026-04-13

### Added
- **CI guard against old skill names (EPIC-0036 / STORY-0036-0006):** New `scripts/check-old-skill-names.sh` scans the repository for any of the 19 OLD EPIC-0036 skill names and fails the build on an unexpected occurrence. Allow-list covers historical artifacts (`plans/`, `adr/`, `CHANGELOG.md`, `docs/release-notes/`), generated outputs (`.claude/`, `java/src/test/resources/golden/`), and build directories. Wired into `.github/workflows/skill-name-guard.yml`, which runs on every push and pull request to `develop` and `main`. Regression tests live in `tests/guard/test-skill-name-guard.sh`.
- **Recursive traversal for hierarchical skills SoT (EPIC-0036):** `SkillsAssembler` now recursively walks the categorised skills directory tree (`targets/claude/skills/{category}/{skill}/`), enabling the new hierarchical SoT layout.
- **Release notes for EPIC-0036 (`docs/release-notes/EPIC-0036-skill-renames.md`):** dedicated migration document with the complete 19-row old → new table, breaking-change notice, mechanical migration recipe, and references to ADR-0003 and the staging document.

### Changed
- **Skill Taxonomy Refactor — Primary Cluster (EPIC-0036 / STORY-0036-0004):** 10 skills renamed to eliminate the `x-story-*` / `x-epic-*` / `x-task-*` / `x-dev-*` ambiguity. Mappings:
  - `x-story-epic` → `x-epic-create`
  - `x-story-epic-full` → `x-epic-decompose`
  - `x-story-map` → `x-epic-map`
  - `x-epic-plan` → `x-epic-orchestrate`
  - `x-dev-implement` → `x-task-implement`
  - `x-dev-story-implement` → `x-story-implement`
  - `x-dev-epic-implement` → `x-epic-implement`
  - `x-dev-architecture-plan` → `x-arch-plan`
  - `x-dev-arch-update` → `x-arch-update`
  - `x-dev-adr-automation` → `x-adr-generate`

  All cross-references, rules, templates, Java tests, and golden files updated. No backward compatibility for old names (RULE-005). See `adr/ADR-0003-skill-taxonomy-and-naming.md` for rationale.

- **Skill Taxonomy Refactor — `run-*` Unification and Pointwise Renames (EPIC-0036 / STORY-0036-0005):** 9 additional skills renamed to complete the taxonomy. `run-*` skills moved to the `x-test-*` family; pointwise simplifications applied to PR and security categories. Mappings:
  - `run-e2e` → `x-test-e2e`
  - `run-smoke-api` → `x-test-smoke-api`
  - `run-smoke-socket` → `x-test-smoke-socket`
  - `run-contract-tests` → `x-test-contract`
  - `run-perf-test` → `x-test-perf`
  - `x-pr-fix-comments` → `x-pr-fix`
  - `x-pr-fix-epic-comments` → `x-pr-fix-epic`
  - `x-runtime-protection` → `x-runtime-eval`
  - `x-security-secret-scan` → `x-security-secrets`

  Total renames across STORY-0036-0004 and STORY-0036-0005: **19**. All cross-references, golden files, and tests updated. No backward compatibility (RULE-005).

### Removed
- **`SkillGroupRegistry.java` (EPIC-0036 / STORY-0036-0003):** the static skill-group registry was deleted; categorisation now lives in the directory layout (`targets/claude/skills/{category}/{skill}/`) under EPIC-0036.

### Fixed
- **Stale generated resources before `process-resources` (build):** Maven build now cleans stale generated outputs prior to the `process-resources` phase, preventing leftover files from previous runs from polluting the build.

## [3.1.0] - 2026-04-13

### Added
- EPIC-0035: `x-release` skill extended with approval gate, PR-flow, and deep validation
  - New flags: `--continue-after-merge`, `--interactive`, `--signed-tag`, `--state-file`
  - State persistence in `plans/release-state-<X.Y.Z>.json` with versioned JSON schema
  - New references: `approval-gate-workflow.md`, `state-file-schema.md`, `backmerge-strategies.md`

### Changed
- EPIC-0035: `x-release` Phase VALIDATE-DEEP now runs 8 checks (tests, coverage, golden files, hardcoded version strings, cross-file consistency) in addition to the basic build
- EPIC-0035: `x-release` Phase OPEN-RELEASE-PR replaces direct `git merge` to main with `gh pr create --base main` + human approval gate
- EPIC-0035: `x-release` Phase BACK-MERGE-DEVELOP replaces direct `git merge` to develop with `gh pr create --base develop` + conflict detection
- EPIC-0035: Hotfix workflow updated to use PR-flow while preserving all existing semantics (patch only, back-merge to develop + active release/*)

### Fixed
- EPIC-0035: `x-release` no longer violates Rule 09 (Branching Model) -- all merges to `main` and `develop` now go through PRs

## [3.0.0] - 2026-04-11

### Added

- **EPIC-0037 plans (docs):** Epic plan, implementation map, and 10 story files for the upcoming "worktree-first branch creation policy" initiative. Planning artifacts only in this release; execution deferred to a future version.

### Removed

- **BREAKING:** `Platform.COPILOT` enum value (generator no longer emits `.github/` Copilot artifacts). (EPIC-0034 / story-0034-0001)
- **BREAKING:** `Platform.CODEX` enum value (generator no longer emits `.codex/` artifacts). (EPIC-0034 / story-0034-0002)
- **BREAKING:** `AssemblerTarget.GITHUB` — `.github/` assembler target retired. (EPIC-0034 / story-0034-0001)
- **BREAKING:** `AssemblerTarget.CODEX` — `.codex/` assembler target retired. (EPIC-0034 / story-0034-0002)
- **BREAKING:** `AssemblerTarget.CODEX_AGENTS` — `.agents/` (shared Codex-agents) target retired. (EPIC-0034 / story-0034-0003)
- **BREAKING:** 18 Java assembler classes deleted across three stories (8 Copilot + 7 Codex + 2 Agents + `ReadmeGithubCounter`), plus ~34 test classes and 2 test fixtures. (EPIC-0034 / stories 0034-0001..0004)
- **BREAKING:** Golden file fixtures under `.github/` (non-workflows), `.codex/`, and `.agents/` — ~8178 files total. The `.github/workflows/` fixtures are **preserved** (RULE-003). (EPIC-0034 / stories 0034-0001..0003)
- **BREAKING:** Resource directories `java/src/main/resources/targets/github-copilot/` (131 files) and `java/src/main/resources/targets/codex/` (15 files). `java/src/main/resources/shared/templates/` is preserved (RULE-004). (EPIC-0034 / stories 0034-0001..0002)

### Changed

- **BREAKING:** CLI `--platform` flag now accepts only `claude-code` (and the backward-compatibility keyword `all`, which is now functionally equivalent to `claude-code`). Previous values `copilot`, `codex`, and `agents` are rejected with a clear error message that lists accepted values. (EPIC-0034 / story-0034-0001)
- **BREAKING (effective):** The `--platform` CLI default string is still declared as `all` in `GenerateCommand`, but `all` now produces `claude-code` output because it is the only remaining platform. Users who previously relied on `--platform all` for multi-target generation now get a single-target claude-code build. (EPIC-0034 / story-0034-0001)
- Generator output per profile reduced substantially after retiring non-Claude targets (verified example: `java-spring` went from ~9500 manifest entries to 343 actual generated files). The `expected-artifacts.json` smoke manifest was regenerated to match the verified claude-only outputs. (EPIC-0034 / story-0034-0005)
- `CLAUDE.md` at repo root reduced from 289 to ~115 lines by removing multi-target documentation sections. (EPIC-0034 / story-0034-0005)
- `readme-template.md` cleaned of `.github/`, `.codex/`, and `.agents/` directory descriptions; artifact-conventions table now lists only Claude artifacts. (EPIC-0034 / story-0034-0005)
- `README.md` at repo root updated: tagline, overview, CLI reference, "What's Generated" tree, and project-structure diagram all reflect single-target (Claude Code) scope. (EPIC-0034 / story-0034-0005)

### Migration

Users with automated scripts or CI pipelines invoking `ia-dev-env` must update as follows:

- Replace `--platform copilot`, `--platform codex`, or `--platform agents` with `--platform claude-code`, **OR** drop the flag entirely (the CLI default is still declared as `all`, which now produces `claude-code` output because it is the only remaining platform). `--platform all` remains accepted and now means "generate claude-code only".
- Remove any downstream tooling that consumes `.github/instructions/`, `.github/skills/`, `.github/prompts/`, `.codex/config.toml`, `.codex/requirements.toml`, or `.agents/skills/` artifacts — these are no longer produced by the generator.
- `.github/workflows/` files in generated projects are unaffected; CI/CD pipelines continue to work without changes (RULE-003).
- Claude Code users with existing `.claude/` output: no action required. Regenerate with the same command you used before, minus any platform flag.
- Per Rule 08 Semantic Versioning, the next release of this tool is a **MAJOR version bump** (2.x → 3.0.0).

### Rollback

This epic introduces no database migrations and no persistent state changes — the generator is stateless. To roll back, revert the merge commit for EPIC-0034 on `develop` and rebuild. Prior multi-target behavior is restored atomically with no data migration needed.

### Security

- CLI error messages for rejected platform values contain no class names, stack traces, or file paths (CWE-209 compliance verified via `plans/epic-0034/reports/task-005-004/` evidence).
- `ReadmeGithubCounter` class and all GitHub-specific readme-generation paths removed (attack surface reduction).
- `ExpectedArtifactsGenerator` output path verified as a compile-time constant (CWE-22 path traversal risk check, story-0034-0005).

## [2.3.0] - 2026-04-10

### Added
- **Rule 13 — Skill Invocation Protocol (EPIC-0033 / STORY-0033-0001):** New canonical rule at `java/src/main/resources/targets/claude/rules/13-skill-invocation-protocol.md` defining three permitted delegation patterns (INLINE-SKILL, SUBAGENT-GENERAL, SUBAGENT-RESEARCH) and forbidding the bare-slash `Invoke /x-foo` anti-pattern in delegation contexts. The slash form remains permitted in user-facing `## Triggers` and `## Examples` sections. Referenced 30+ times across `skills/core/` and in `CLAUDE.md` (rules index updated; gaps at 10/11/12 reserved for conditional rules `10-anti-patterns`, `11-security-pci`, `12-security-anti-patterns`). Documented in **ADR-0002**.
- **TaskCreate/TaskUpdate Level 2 visibility (EPIC-0033 / STORY-0033-0002):** `x-dev-epic-implement` now emits `TaskCreate("Story {id}: {title}")` per story dispatch and closes it after the subagent returns. `x-dev-story-implement` emits per-phase tracking tasks (Phase 0-3) plus per-task entries inside the Phase 2 inner loop. Description format follows CR-02. FAILED/BLOCKED terminal states surface via `"(FAILED) "` / `"(BLOCKED) "` description prefix. `execution-state.json` remains authoritative for resume/checkpoint logic (CR-04).
- **Planning subagent Level 3 visibility (EPIC-0033 / STORY-0033-0003):** Each of the 7 active planners in Phase 1 (1A Architecture, 1B Impl Plan, 1B Test Plan, 1C Task Decomposer, 1D Event Schema, 1E Security primary+fallback, 1F Compliance) appears individually in the Claude Code task list. Two strategies: orchestrator-managed (around `Skill(...)` calls) and subagent-managed (FIRST/LAST ACTION in subagent prompt). Skipped planners do NOT emit TaskCreate (AC-4).
- **Per-specialist tracking in `x-review` Phase 2 — Level 3 (EPIC-0033 audit hardening):** One `TaskCreate("Review: {specialist} — Story {STORY_ID}")` per active specialist (9 possible: qa, perf, db, obs, devops, data-modeling, security, api, events) as siblings with the `Skill(...)` calls in a single Batch A message. Batch B closes them after reviews return.
- **Explicit `--orchestrated` flag in `x-test-tdd` (EPIC-0033 / STORY-0033-0003):** Replaces the fragile implicit detection for compact-vs-full output format. `x-dev-story-implement` Phase 2 step 2.2.5 passes `--orchestrated`; direct user invocations omit it and get full multi-line format.
- **Parallelism + tracking batching documentation (EPIC-0033 audit hardening):** New section at the top of Phase 1B-1F in `x-dev-story-implement/SKILL.md` (with enumeration table of orchestrator-managed vs subagent-managed planners) and `planning-phases.md`. Documents the 3-step Batch A → Wait → Batch B pattern.
- **ADR-0002 — Skill Delegation Protocol:** Documents the architectural decision behind Rule 13, context, three permitted patterns, parallelism strategy, observability, trade-offs, and cross-references to the EPIC-0033 PR chain.
- **EPIC-0034 plans (docs):** Epic plan, story files, and implementation map for removing non-Claude targets.

### Changed
- **Skill naming standardization (EPIC-0032):** 25 skills renamed in the source of truth (`java/src/main/resources/targets/`) to follow the `x-{category}-{action}` convention (RULE-001). 15 core skills: `x-dev-lifecycle` → `x-dev-story-implement` (consolidation), `x-commit` → `x-git-commit`, `x-tdd` → `x-test-tdd`, `x-format` → `x-code-format`, `x-lint` → `x-code-lint`, `x-changelog` → `x-release-changelog`, `x-docs` → `x-doc-generate`, `x-codebase-audit` → `x-code-audit`, `x-spec-drift-check` → `x-spec-drift`, `x-setup-dev-environment` → `x-setup-env`, `x-ci-cd-generate` → `x-ci-generate`, `x-worktree` → `x-git-worktree`, `x-plan-task` → `x-task-plan`, `x-fix-pr-comments` → `x-pr-fix-comments`, `x-fix-epic-pr-comments` → `x-pr-fix-epic-comments`. 10 conditional skills: `x-sast-scan`/`x-dast-scan`/`x-secret-scan`/`x-container-scan`/`x-infra-scan`/`x-sonar-gate` grouped under `x-security-*`, `x-pentest` → `x-security-pentest`, `x-contract-lint` → `x-test-contract-lint`, `setup-environment` → `x-setup-stack`, `instrument-otel` → `x-obs-instrument`. All cross-references, Java source (`SkillGroupRegistry`, `SkillsSelection`, `SecurityBaselineWriter`), and documentation updated. No backward compatibility for old names (RULE-004).
- **Source of Truth rule (RULE-002):** `CLAUDE.md` and `AGENTS.md` now explicitly declare `java/src/main/resources/targets/` as the authoritative location for skills, knowledge packs, agents, rules and templates. Direct edits to generated directories (`.claude/`, `.github/`, `.codex/`, `.agents/`, `src/test/resources/golden/`) are forbidden (story-0032-0001).
- **13 logical delegation routes converted (24 physical locations) (EPIC-0033 / STORY-0033-0001):** All skill-to-skill delegations in `skills/core/` now use explicit `Skill(skill: "...", args: "...")` calls following Rule 13. Affected files: `x-test-tdd/SKILL.md` (4 RED/GREEN/REFACTOR/Slim Mode), `x-git-commit/SKILL.md` (2 format/lint), `x-review/SKILL.md` (9 specialists), `x-dev-story-implement/SKILL.md` + `references/verification-phase.md` (5 synced pairs), `planning-phases.md` (1A), `x-dev-epic-implement/SKILL.md` (2 Phase 4), `x-owasp-scan/SKILL.md` (1 A06). Audit grep confirms 0 bare-slash matches remaining.
- **`allowed-tools` hardened (EPIC-0033):** `x-test-tdd` += `Skill`; `x-git-commit` += `Skill`; `x-dev-epic-implement` += `Skill, Agent, TaskCreate, TaskUpdate`; `x-dev-story-implement` += `Skill, Agent, TaskCreate, TaskUpdate`; `x-review` += `TaskCreate, TaskUpdate`.
- **`x-review` Phase 2 restructured (EPIC-0033 / STORY-0033-0001):** Nine specialist dispatches converted from bullet list of bare-slash commands to nine explicit `Skill(...)` calls in a single Batch A message. Activation conditions moved to a bullet list before the code block so tool-call lines are comment-free and copy-safe.
- **Step 1E (Security Assessment) aligned between SKILL.md and planning-phases.md (EPIC-0033 audit hardening):** Pre-existing drift resolved — both files describe 1E identically as `Skill(x-threat-model)` primary + inline Security Engineer subagent fallback. Orchestrator explicitly closes `securityTaskId` before launching fallback.
- **Subagent launches in Phase 1 converted to explicit `Agent(...)` calls (EPIC-0033 audit hardening):** Phase 1B Impl Plan, 1D Event Schema, 1E fallback, and 1F Compliance now show canonical `Agent(subagent_type: "general-purpose", description: "...", prompt: "...")` tool calls in both `x-dev-story-implement/SKILL.md` and `planning-phases.md`.
- **Rule 13 Pattern 2 renamed from SUBAGENT-SKILL to SUBAGENT-GENERAL (EPIC-0033 audit hardening):** Now covers (a) subagent invokes another skill, (b) subagent produces an artifact, (c) subagent performs complex analysis. All three use the same `Agent(...)` call shape.
- **Non-Claude target removal (EPIC-0034):** `targets/github-copilot/` and `targets/codex/` directories removed from the source of truth. `targets/claude/` is now the sole authoritative target. Generator, tests, and golden fixtures updated accordingly.

### Fixed
- **Orphan reference to non-existent `x-test-contract-lint` removed (EPIC-0033 / STORY-0033-0001):** `x-dev-story-implement/SKILL.md:269` referenced a skill that does not exist in `skills/core/`. Replaced with inline linter guidance (redocly/cli, buf lint, spectral lint) plus a follow-up note.
- **CLAUDE.md rules index backfill (EPIC-0033 / STORY-0033-0001):** Root `CLAUDE.md` rules table previously listed only rules 01-06 — stale since 07-09 had been added without updating the index. Refreshed to include Rule 13 with an explanatory note about the reserved gap at 10-12 for conditional rules.
- **3 additional delegation routes using "Invoke skill X" phrasing (EPIC-0033 / STORY-0033-0003 drive-by):** Pre-existing Story 0001 misses at `x-dev-story-implement/SKILL.md:335` (Step 1A), line 399 (1B Test Plan), line 413 (1C Task Decomposition). The original audit grep did not match the `"Invoke skill X"` variant.
- **`x-test-tdd` implicit mode detection removed (EPIC-0033 / STORY-0033-0003):** Replaced fragile `"if this skill was invoked via Skill tool"` check with explicit `--orchestrated` flag.
- **`TaskUpdate(id: {placeholder})` disambiguation (EPIC-0033 audit — PR #258 review):** Removed curly-brace placeholders from 5 `TaskUpdate` calls in `x-dev-story-implement/SKILL.md` and added explanatory prose clarifying that each token is a runtime reference to the numeric integer returned by `TaskCreate`.
- **FAILED vs BLOCKED signaling split (EPIC-0033 audit — PR #258 review):** Phase 2 inner-loop closeout (step 2.2.11) previously conflated FAILED and BLOCKED. Split into distinct prefixes with CR-04 reference updated to `SUCCESS/FAILED/BLOCKED`.
- **1E fallback `securityTaskId` orphaning (EPIC-0033 audit — PR #259 review):** Added explicit `TaskUpdate(id: securityTaskId)` before the fallback `Agent(...)` launch so the orchestrator's tracking task doesn't stay open forever when `x-threat-model` is unavailable.
- **Trailing inline comments on `Skill(...)` calls in `x-review` Phase 2 (EPIC-0033 audit — PR #257 review):** Removed trailing `# only if ...` comments and moved activation conditions to a bullet list preceding the tool-call block.
- **Weak `XReviewSkillTemplateTest` assertions strengthened (EPIC-0033 audit — PR #257 review):** Three `phase2_references*Skill` test methods now use `containsPattern` with regex that verifies both skill name and args payload (`args: "{STORY_ID}"`), allowing whitespace variation.
- **Rule numbered 13, not 10, due to conditional rule collision (EPIC-0033 audit — PR #257 review):** Slots 10 (`10-anti-patterns`), 11 (`11-security-pci`), and 12 (`12-security-anti-patterns`) were already reserved. Renamed to `13-skill-invocation-protocol.md` with a gap note in the `CLAUDE.md` rules index.

## [2.2.2] - 2026-04-10

### Fixed
- **Installer (`java/install.sh`) — version detection from `pom.xml`**: replaced the hardcoded `VERSION`/`JAR_NAME` constants with a `resolve_version()` step that parses the version from `java/pom.xml` at install time. Restores the documented `--jar=PATH` flow by deriving the version from the JAR filename pattern `ia-dev-env-VERSION.jar` (with `unknown` fallback) when a pre-built JAR is supplied, so the script no longer fails with "pom.xml not found" outside the source tree.
- **Installer success banner — ANSI escape codes**: color variables migrated from `'\033[...]'` to bash ANSI-C quoting `$'\033[...]'` so the actual ESC byte is stored. Both the `cat <<EOF` heredocs in `print_success()` / `usage()` and the `printf`-based log helpers now render colors consistently — previously users saw raw `\033[0;32m` text in the success banner.
- **Installer Quick Start — list every bundled stack**: a new `list_bundled_stacks()` helper introspects the installed fat JAR via the JDK `jar` tool (with `unzip` fallback), discovering every `shared/config-templates/setup-config.<name>.yaml` resource. The success banner now prints one `ia-dev-env generate --stack <name> --output my-project/` example per bundled stack instead of only `java-quarkus`, and stays in sync automatically as profiles are added or removed.
- **CLI version single-sourced from `pom.xml` (`IaDevEnvApplication`)**: replaced the hardcoded `@Command(version = "2.0.0")` with `versionProvider = CliVersionProvider.class`. The new provider reads from `dev/iadev/version.properties`, a Maven-filtered resource seeded from `${project.version}` at build time, so `ia-dev-env --version` now reflects the actual `pom.xml` version (`ia-dev-env 2.2.2`).
- **Dev wrapper (`java/bin/ia-dev-env`) — JAR discovery**: dropped the stale hardcoded `JAR_NAME="ia-dev-env-2.0.0-SNAPSHOT.jar"` (which no longer matched any built artifact) and replaced it with a glob over `target/ia-dev-env-*.jar` that picks the newest match. Removes the last `SNAPSHOT` reference from the dev tooling.
- **Tests**: `IaDevEnvApplicationTest`, `DistributionTest`, and `WrapperScriptTest` updated to assert against a SemVer regex / glob pattern instead of pinning a specific version literal. New `CliVersionProviderTest` covers the happy path (single line, program name prefix, SemVer body, no `unknown` fallback, no leaked `${project.version}` placeholder).

## [2.2.1] - 2026-04-09

### Fixed
- Resolve merge conflict markers committed to develop across 5 source templates and ~265 golden files. Conflicts between EPIC-0030 (context isolation, output compaction, lazy-kp loading) and EPIC-0031 (circuit breaker, error classification, graceful degradation) caused 27 test failures.
- Restore EPIC-0030 content lost during EPIC-0031 merges: CONTEXT MANAGEMENT sections, CONTEXT ISOLATION markers, specific KP reference paths (`security/references/application-security.md`, `compliance/references/`), and subagent role prompts (Senior Architect, Event Engineer, Security Engineer) in `x-dev-story-implement` and `x-dev-epic-implement`.
- Reposition CONTEXT ISOLATION markers after role names in subagent prompts to satisfy `assertPromptSectionContains` test assertions.
- Fix `x-dev-epic-implement` subagent dispatch to use explicit `Skill(skill: "x-dev-story-implement")` invocation instead of descriptive workflow steps.
- Update expected-artifacts manifest to reflect restored orchestrator template structure.
- Regenerate golden files for all 17 profiles + 2 platform-specific outputs.

## [2.2.0] - 2026-04-09

### Added
- **Error Catalog & Standardized Error Responses (EPIC-0031):** New `references/error-catalog.md` with 13 standardized error codes across 4 categories (TRANSIENT, CONTEXT, PERMANENT, CIRCUIT). Detection patterns and prescribed actions for each code. Error Classification sections added to `x-dev-epic-implement` and `x-dev-story-implement`.
- **Transient Error Retry with Exponential Backoff (EPIC-0031):** Automatic retry for transient errors (overloaded, rate limit, timeout) with exponential backoff (2s, 4s, 8s). Tool calls retry 3x, subagent dispatch retries 2x. Permanent errors fail immediately — never retried.
- **Local Integrity Gate Between Phases (EPIC-0031):** Integrity gate now executes by default in `--no-merge` mode (never DEFERRED). Creates temp branch, merges SUCCESS stories, runs compile+test+coverage, deletes temp branch. New `--skip-gate` flag for explicit opt-out (SKIPPED, not DEFERRED). Post-gate AskUserQuestion prompt with 3 options.
- **Subagent Failure Recovery (EPIC-0031):** SubagentResult expanded with `errorType`, `errorMessage`, `errorCode` fields. Recovery strategies by error type: TRANSIENT (2 retries), CONTEXT (1 retry with reduced prompt), TIMEOUT (1 retry with --skip-verification). Escalation after 3 consecutive failures.
- **Circuit Breaker for Epic Execution (EPIC-0031):** 3-state machine (CLOSED/OPEN/HALF_OPEN) with threshold escalation: 1 failure = WARNING, 2 = pattern analysis, 3 consecutive = PAUSE with AskUserQuestion, 5 total in phase = ABORT. Resets on SUCCESS or `--resume`.
- **Checkpoint Error History Schema v3.0 (EPIC-0031):** New top-level fields: `errorHistory` (array of error entries with timestamp, errorCode, resolution), `circuitBreaker` (state tracking), `contextPressure` (degradation level). Backward compatible with v2.0. Resume shows error summary.
- **Graceful Degradation on Context Pressure (EPIC-0031):** 3-level progressive degradation: Level 1 (reduce verbosity, skip optional phases), Level 2 (force delegation to subagents), Level 3 (save state, suggest --resume). Progressive advancement rule — never skips levels.

### Fixed
- Standardized `===`/`!==` operators across all pseudocode in orchestrator skills (was inconsistent `==`/`===` mix).
- Circuit breaker state transition table now includes "5 total failures → phase abort" row.
- `circuitBreaker` and `contextPressure` checkpoint defaults now include all required fields.
- `lastFailurePattern` uses clean enum values (TRANSIENT/CONTEXT/PERMANENT/MIXED) instead of template strings.
- Local integrity gate scope clarified for `--no-merge` mode in reference documentation.

## [2.1.0] - 2026-04-09

### Added
- **Skill Naming Standardization Epic (EPIC-0032):** Epic, 8 stories, and implementation map for renaming 25 skills to follow consistent `x-{category}-{action}` convention.
- **Skill Delegation Fix Epic (EPIC-0033):** Epic, 4 stories, and implementation map addressing broken Skill tool chain, zero TaskCreate visibility, planning subagent black boxes, and x-dev-story-implement/x-dev-story-implement naming inconsistency.

## [Unreleased]

### Added
- **Platform filter (EPIC-0025)**: Add `--platform` flag to `generate` command for targeted AI platform generation (claude-code, copilot, codex, all). Supports YAML config via `platform:` section. Default: all (backward-compatible).
- **Artifact Persistence & Standardization (EPIC-0024):** 12 new plan and review templates providing standardized output formats for all planning and review artifacts. Templates: Implementation Plan, Test Plan, Architecture Plan, Task Breakdown, Security Assessment, Compliance Assessment, Specialist Review, Tech Lead Review, Consolidated Review Dashboard, Review Remediation, Epic Execution Plan, and Phase Completion Report.
- **PlanTemplatesAssembler (EPIC-0024):** New assembler that copies 12 templates verbatim to both `.claude/templates/` and `.github/templates/` with mandatory section validation (RULE-004 dual-target, RULE-010 validation).
- **Pre-checks in 8 skills (EPIC-0024):** Idempotency pre-checks added to x-dev-story-implement, x-test-plan, x-dev-architecture-plan, x-lib-task-decomposer, x-review, x-review-pr, x-dev-epic-implement, and x-dev-implement. Skills verify artifact staleness before regenerating (RULE-002).
- **Consolidated Review Dashboard (EPIC-0024):** Cumulative dashboard created by x-review and updated by x-review-pr with parseable scores (XX/YY format) and review history (RULE-005, RULE-006).
- **Remediation Tracking (EPIC-0024):** Review remediation template with findings tracker, deferred justifications, and re-review results for systematic issue resolution.
- **Epic Execution Plan (EPIC-0024):** Execution plan template saved before epic execution begins, enabling human audit of strategy, phase timeline, and resource requirements.
- **Phase Completion Reports (EPIC-0024):** Per-phase reports documenting stories completed, integrity gate results, coverage delta, and next phase readiness.

### Changed
- **x-story-implement (EPIC-0024):** Now produces 6 artifact types with pre-checks (previously 2). Generates implementation plan, test plan, architecture plan, task breakdown, security assessment, and compliance assessment with staleness verification. (Renamed from `x-dev-story-implement` in EPIC-0036.)
- **x-task-implement (EPIC-0024):** Consumes existing plans as context when available, ensuring consistency between x-story-implement planning and x-task-implement execution. (Renamed from `x-dev-implement` in EPIC-0036.)
- **x-review (EPIC-0024):** Generates consolidated review dashboard with parseable specialist scores. Dashboard is cumulative across review rounds.
- **x-review-pr (EPIC-0024):** Updates consolidated review dashboard with Tech Lead review round, providing complete quality visibility in a single file.
- Restructured project directories to adopt SDD (Spec-Driven Development) layout
  - `docs/` replaced by `steering/`, `specs/`, `plans/`, `results/`, `contracts/`, `adr/`
  - Java assembler output paths updated to match new structure
  - Skill templates updated with new directory references

### Added
- **Output Directory Cleanup + Overwrite Protection (story-0005-0015):** Removed redundant legacy `docs/epic/` output from `EpicReportAssembler` — epic template now only emits to `.claude/templates/` and `.github/templates/`. Added `--force` flag to `generate` command with overwrite protection: when the output directory already contains generated artifacts (`.claude/`, `.github/`, `steering/`, `specs/`, `plans/`, `results/`, `contracts/`, `adr/`; and legacy `docs/` when present), the CLI exits with an error listing conflicting directories unless `--force` is provided. `--dry-run` bypasses the overwrite check. New `src/overwrite-detector.ts` module with `checkExistingArtifacts()` and `formatConflictMessage()`. Golden files updated for all 8 profiles (removed legacy `docs/epic/`). 20 new tests (8 unit + 7 CLI + 5 integration).
- **E2E Tests + Generator Integration (story-0005-0014):** Capstone story for epic-0005. Orchestrator E2E test suite with 14 tests covering 6 scenarios: dry-run (no execution), happy path (5/5 SUCCESS), failure path (retry exhaustion + transitive block propagation), resume path (continue from checkpoint), partial execution (--phase filter), and parallel mode. Test infrastructure: configurable mock subagent dispatch, synthetic 5-story/3-phase implementation map, scenario runner composing all orchestrator modules (parser, checkpoint, retry, blocks, progress). Generator integration verified: `x-dev-epic-implement` skill already auto-discovered by SkillsAssembler and registered in GithubSkillsAssembler. Golden file tests already pass for all 8 profiles. CLAUDE.md updated with skill entry.
- **Parallel Execution with Worktrees (story-0005-0010):** Added parallel worktree dispatch capability to the `x-dev-epic-implement` SKILL.md template. When `--parallel` flag is active, executable stories in the same phase are dispatched concurrently in a SINGLE message via `Agent` tool with `isolation: "worktree"`. After all subagents complete, worktree branches are merged sequentially into the epic branch (critical path first per RULE-007). Includes conflict resolution subagent for automatic merge conflict handling and worktree cleanup rules. New sections: 1.4a (Parallel Worktree Dispatch), 1.4b (Merge Strategy), 1.4c (Conflict Resolution Subagent), 1.4d (Worktree Cleanup). 14 new content tests, 5 new dual-copy consistency terms, golden files regenerated for all 8 profiles. Extension point placeholder for story-0005-0010 removed from Section 1.7.
- **Progress Reporting + Execution Metrics (story-0005-0013):** New `src/progress/` module with real-time progress event emission and execution metrics tracking for the epic orchestrator. Features: 7 typed progress events (PHASE_START, STORY_START, STORY_COMPLETE, GATE_RESULT, RETRY, BLOCK, EPIC_COMPLETE) as a discriminated union, pure formatting functions for terminal output, pure metrics calculation (average duration, estimated remaining time), factory-based stateful reporter with injectable output sink, and checkpoint-persisted execution metrics. Extended `ExecutionMetrics` with 7 new optional fields (storiesFailed, storiesBlocked, elapsedMs, estimatedRemainingMs, averageStoryDurationMs, storyDurations, phaseDurations). All checkpoint changes are backward-compatible. 59 new tests (24 formatter, 13 calculator, 22 reporter), zero regressions on existing 3051 tests.
- **Partial Execution (story-0005-0009):** New `src/domain/implementation-map/partial-execution.ts` module with 4 pure functions for epic partial execution: `parsePartialExecutionMode` (mutual exclusivity validation), `validatePhasePrerequisites` (phases 0..N-1 completeness check), `validateStoryPrerequisites` (dependency satisfaction check), `getStoriesForPhase` (phase story filter). New types: `PartialExecutionMode` discriminated union, `PrerequisiteResult`. New error class: `PartialExecutionError`. SKILL.md templates updated with Partial Execution section (both Claude and GitHub copies). 23 unit tests + 12 content assertions, 100% line coverage, 96.77% branch coverage.
- **Resume Reclassification Engine (story-0005-0008):** New `src/checkpoint/resume.ts` module with pure functions for epic resume workflow. `reclassifyStories()` applies status transition rules (IN_PROGRESS→PENDING, FAILED with retries<MAX_RETRIES→PENDING, SUCCESS preserved). `reevaluateBlocked()` resolves BLOCKED stories when all dependencies reach SUCCESS. `prepareResume()` composes both operations. `MAX_RETRIES` constant (2) and `ReclassificationEntry` type added to checkpoint types. Resume Workflow section added to `x-dev-epic-implement` SKILL.md (both Claude and GitHub copies) documenting reclassification table, branch recovery, and BLOCKED reevaluation. 36 unit tests, 100% line/branch coverage.
- **Failure Handling: Retry + Block Propagation (story-0005-0007):** New `src/domain/failure/` module with pure-function retry evaluation and transitive block propagation. `evaluateRetry()` enforces RULE-005 (max 2 retries per story) with error context passthrough to retry subagents. `propagateBlocks()` performs BFS on the dependency DAG to transitively mark all dependents as BLOCKED (RULE-006). Discriminated union return types (`RetryDecision`, `BlockPropagationResult`) — no exceptions, no I/O. 26 tests, 100% line and branch coverage.
- **Consolidation Final — Review + Report + PR (story-0005-0011):** Replaced Phase 2 (Consolidation) and Phase 3 (Verification) placeholders in `x-dev-epic-implement` SKILL.md with full implementation content. Phase 2 dispatches tech lead review via `x-review-pr` subagent, generates `epic-execution-report.md` from template with all 18 placeholders resolved, creates PR via `gh pr create` with `[PARTIAL]` handling for incomplete epics. Phase 3 covers epic-level test suite validation, DoD checklist, final status determination (COMPLETE/PARTIAL/FAILED), and completion output. 28 new content tests, 7 new dual-copy consistency terms, golden files regenerated for all 8 profiles. Extension point placeholder for story-0005-0011 removed from Phase 1.
- **Integrity Gate Between Phases (story-0005-0006):** Extended `IntegrityGateEntry` with `branchCoverage` (optional number) and `regressionSource` (optional string). Added integrity gate section to `x-dev-epic-implement` SKILL.md with gate subagent prompt, regression diagnosis, gate result registration, and RULE-004 enforcement. 25 new tests. Golden files updated for all 8 profiles.
- **Dry-run mode (story-0005-0012):** Domain module `src/domain/dry-run/` implementing
  execution plan simulation for the epic orchestrator. Pure functions `buildDryRunPlan()`
  and `formatPlan()`/`formatStoryDetail()` compute and render the plan without side effects.
  Supports flag combinations: `--resume` (checkpoint status merge), `--phase N` (filter),
  `--parallel` (concurrency indicators), `--story XXXX-YYYY` (single story detail).
  35 tests (28 planner + 7 formatter), 98.94% line / 98.55% branch coverage.
- **Orchestrator Core Loop + Sequential Dispatcher (story-0005-0005):** Replaced Phase 1 placeholder in `x-dev-epic-implement` SKILL.md with full execution loop logic. Core loop iterates phases from the dependency DAG, dispatches subagents sequentially per story with critical path priority (RULE-007), validates SubagentResult contracts (RULE-008), updates checkpoint after each story (RULE-002), and maintains context isolation (RULE-001). Includes 7 extension point placeholders for downstream stories (integrity gate, retry, resume, partial execution, parallel worktrees, consolidation, progress reporting). 13 new content tests, dual-copy consistency verified, golden files regenerated for all 8 profiles.
- **Implementation Map Parser (story-0005-0004):** New `src/domain/implementation-map/` module that parses `IMPLEMENTATION-MAP.md` files and builds a dependency DAG. Features: markdown table extraction, DAG construction with adjacency lists, symmetry validation with auto-correction, cycle detection (DFS three-color), phase computation, critical path identification (topological-sort longest path), and `getExecutableStories()` with critical path priority sorting. Pure-function design with zero external dependencies. 77 tests, 100% line coverage, 92.36% branch coverage.
- **x-dev-epic-implement skill skeleton (story-0005-0003):** New core skill template `x-dev-epic-implement` with YAML frontmatter, input parsing (epic ID + 6 optional flags), 5 prerequisite checks with error messages, Phase 0 preparation flow, and Phase 1-3 placeholders for future stories. Registered in GitHub skills assembler. Golden files updated for all 8 profiles.
- **Epic Execution Report Template (story-0005-0002):** New `_TEMPLATE-EPIC-EXECUTION-REPORT.md` with 8 sections and 18 runtime placeholders for epic orchestration. `EpicReportAssembler` copies the template verbatim to `docs/epic/`, `.claude/templates/`, and `.github/templates/`. Pipeline now has 23 assemblers.
- **Execution State Schema** (`src/checkpoint/types.ts`) -- Typed interfaces for epic execution state: `ExecutionState`, `StoryEntry`, `IntegrityGateEntry`, `ExecutionMetrics`, `SubagentResult`, `StoryStatus` enum. Foundation for orchestrator resumability.
- **Checkpoint Engine** (`src/checkpoint/engine.ts`) -- CRUD operations for `execution-state.json` with atomic write (tmp file + rename). Functions: `createCheckpoint`, `readCheckpoint`, `updateStoryStatus`, `updateIntegrityGate`, `updateMetrics`.
- **Schema Validation** (`src/checkpoint/validation.ts`) -- Hand-written validation for checkpoint JSON: field presence, type checks, enum guards. `validateExecutionState` returns typed state or throws `CheckpointValidationError`.
- **Checkpoint Error Classes** (`src/exceptions.ts`) -- `CheckpointValidationError` (field + detail) and `CheckpointIOError` (path + operation) for structured error handling.
- **Execution State Template** (`resources/templates/_TEMPLATE-EXECUTION-STATE.json`) -- Reference JSON template for execution state with example values.
- **GitHub Actions CI** -- Lint, build, test workflow with Node.js 20/22 matrix.
  Coverage upload on Node 22. Pack verification job.
- **npm packaging** -- `files` field, `prepublishOnly` script for publish gating.
- **Framework knowledge packs (8 new):** NestJS, Express, FastAPI, Django, Gin, Ktor, Axum, .NET knowledge packs with DI, data access, web/HTTP, configuration, testing, and anti-pattern sections. All frameworks now have dedicated knowledge packs matching the Quarkus/Spring Boot reference format.
- **Infrastructure knowledge packs (7 new):** k8s-deployment (pod specs, resource sizing, probes), k8s-kustomize (base/overlays, patches), k8s-helm (chart structure, GitOps), dockerfile (multi-stage per language), container-registry (tagging, scanning, retention), iac-terraform (modules, state, CI/CD), iac-crossplane (XRD, Composition, Claims).
- **Rules consolidation strategy:** All protocols consolidated into single `13-protocol-conventions.md`, all patterns into `14-architecture-patterns.md`, security into max 2 files (`15-security-principles.md` + `16-compliance-requirements.md`), framework rules into max 3 files (core, data, operations). Target: ≤30 rule files for any configuration.
- **Context audit in setup.sh:** Automatic post-generation audit reports total rule file count and size, warns if limits exceeded (>30 files or >200KB).
- **Knowledge pack selection logic:** All 10 frameworks, 7 infrastructure tools, and cloud providers now have conditional knowledge pack selection in setup.sh.
- **Comprehensive Restructuring (v3 config):** Rewrite `setup-config.example.yaml` from flat v2 (`project.type`) to semantic v3 (`architecture.style`, `interfaces[]`, `data.message_broker`, `observability`, `testing`) with backward-compatible v2 migration.
- **Cloud-Native Principles (`core/12`):** 12-Factor compliance checklist, Kubernetes health probes, graceful shutdown, configuration hierarchy, container best practices, service mesh awareness. Cross-references rules 08/09/10 without duplication.
- **Patterns Directory (22 files):** Architectural (hexagonal-architecture, cqrs, event-sourcing, modular-monolith), Microservice (saga, outbox, api-gateway, service-discovery, bulkhead, strangler-fig, idempotency), Resilience (circuit-breaker, retry-with-backoff, timeout-patterns, dead-letter-queue), Data (repository-pattern, unit-of-work, cache-aside, event-store), Integration (anti-corruption-layer, backend-for-frontend, adapter-pattern).
- **Protocols Directory (8 files):** REST (rest-conventions, openapi-conventions), gRPC (grpc-conventions, grpc-versioning), GraphQL (graphql-conventions), WebSocket (websocket-conventions), Event-Driven (event-conventions, broker-patterns).
- **Setup.sh v3 support:** Pattern/protocol assembly based on architecture style and interface types, new interactive prompts (architecture style, DDD, event-driven, interfaces, message broker, testing), backward-compatible v2 config migration with warnings.
- **Cross-references:** core/05 links to hexagonal-architecture pattern, core/06 links to all 6 protocol directories, core/09 links to 5 resilience pattern files.
- **Security layer expansion (10 files):** Application security (OWASP Top 10), cryptography (encryption/hashing/key management), pentest readiness, and 6 compliance frameworks (PCI-DSS, PCI-SSF, LGPD, GDPR, HIPAA, SOX). Base security rules always included; compliance frameworks conditionally selected via `security.compliance[]`.
- **Cloud provider knowledge packs (4 files):** AWS, Azure, GCP, OCI service mapping references. Not rules — reference documents mapping boilerplate concepts to provider-specific services. Selected via `cloud.provider`.
- **Infrastructure patterns expansion (12 files):** Kubernetes deployment/kustomize/helm patterns, Dockerfile multi-stage builds per language, container registry patterns, Terraform and Crossplane IaC references, API gateway patterns (Kong, Istio, AWS APIGW, Traefik).
- **API Gateway pattern:** Generic API gateway pattern document plus 4 implementation-specific knowledge packs.
- **Domain knowledge templates (10 files):** Open Banking (PIX/BACEN), Healthcare FHIR, Telecom TMF, Insurance ACORD, IoT Telemetry — each with domain-rules.md and domain-template.md.
- **YAML configuration expansion:** New sections for `security` (compliance, encryption, pentest), `cloud` (provider), `infrastructure` (templating, IaC, registry, API gateway, service mesh), and `domain` (template selection). Backward compatible with existing format.
- **setup.sh updates:** New assembly phases for security rules, cloud knowledge packs, infrastructure knowledge packs. Domain template selection from templates/domains/. Two new conditional skills (security-compliance-review, review-gateway). Auto-enforcement: PCI-DSS/HIPAA compliance forces encryption at rest.
- **Database References (22 files):** SQL (PostgreSQL, Oracle, MySQL/MariaDB), NoSQL (MongoDB, Cassandra/ScyllaDB), Cache (Redis, Dragonfly, Memcached). Each with types-and-conventions, migration-patterns, query-optimization. Shared common principles per category.
- **Cache support in config:** New `stack.cache.type` field supporting `redis`, `dragonfly`, `memcached`, or `none`.
- **Oracle and Cassandra database types:** `stack.database.type` now supports `oracle` and `cassandra` in addition to existing options.
- **Mongock migration tool:** `stack.database.migration` now supports `mongock` for MongoDB projects.
- **Settings fragments:** `database-oracle.json`, `database-mongodb.json`, `database-cassandra.json`, `cache-redis.json`, `cache-dragonfly.json`, `cache-memcached.json`.
- **Layer templates:** MongoDB Document, MongoDB Repository, Cassandra Entity, and Cache Adapter templates added to the `layer-templates` knowledge pack.
- **Database Engineer agent:** Expanded from 20-point to 30-point checklist covering SQL, NoSQL, and Cache-specific validations. Now activates when `database != "none"` OR `cache != "none"`.
- **Database patterns knowledge pack:** Restructured as a hub referencing `references/` directory. References are auto-selected by `setup.sh` based on database and cache type.
- **Version matrix:** Consolidated cross-reference of all databases, caches, and framework integrations.

### Fixed
- **macOS awk compatibility:** `parse_yaml_nested()` rewritten from BSD-awk-incompatible syntax to pure bash `while read` loop. Fixes nested YAML parsing on macOS.

## [0.1.0] - 2026-02-18

### Added
- **Core Layer (11 files):** Universal engineering principles — clean code, SOLID, testing, git workflow, hexagonal architecture, API design, security, observability, resilience, infrastructure, database. All technology-agnostic with pseudocode examples.
- **Profile: java21-quarkus (9 files):** CDI, Panache, RESTEasy Reactive, MicroProfile Fault Tolerance, SmallRye Health, OpenTelemetry direct, `@ConfigMapping`, `@RegisterForReflection`, Quarkus native build.
- **Profile: java21-spring-boot (9 files):** Spring DI, Spring Data JPA, `@RestController`/`@ControllerAdvice`, Resilience4j, Spring Boot Actuator, Micrometer + OTel bridge, `@ConfigurationProperties`, `@RegisterReflectionForBinding`, Spring AOT native build.
- **Templates (2 files):** Project identity template and domain template with placeholder syntax.
- **Domain Examples (3):** ISO 8583 authorizer (5 files), e-commerce API (2 files), SaaS multi-tenant (2 files).
- **Generator (`setup.sh`):** Interactive and config-file modes, assembles core + profile + domain template.
- **Documentation (4 files):** README, CONTRIBUTING, ANATOMY-OF-A-RULE, FAQ.
