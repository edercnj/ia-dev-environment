---
name: x-release
model: sonnet
description: "Orchestrates complete release flow using Git Flow release branches with approval gate, PR-flow (gh CLI) and deep validation: version bump (auto-detect or explicit), release branch creation from develop, deep validation (coverage, golden files, version consistency), version file updates, changelog generation, release commit, release PR via gh (optionally reviewed by x-review-pr), human approval gate with persistent state file, tag on main after merged PR, back-merge PR to develop with conflict detection, and cleanup. Supports hotfix releases from main, dry-run mode, resume via --continue-after-merge, in-session pause via --interactive, GPG-signed tags, skip-review opt-out, and custom state file path."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, Agent, Skill, AskUserQuestion, TaskCreate, TaskUpdate
argument-hint: "[major|minor|patch|version] [--version X.Y.Z] [--last-tag <tag>] [--dry-run] [--skip-tests] [--no-publish] [--no-github-release] [--hotfix] [--continue-after-merge] [--interactive] [--non-interactive] [--no-prompt] [--signed-tag] [--skip-review] [--ci-watch] [--state-file <path>] [--skip-integrity] [--integrity-report <path>] [--max-parallel <N>] [--status] [--abort] [--yes] [--force]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## Triggers

```
/x-release                     — auto-detect version bump from Conventional Commits
/x-release major|minor|patch   — explicit bump type
/x-release 2.1.0               — explicit version
/x-release minor --dry-run     — preview plan without executing
/x-release minor --dry-run --interactive  — interactive dry-run, pauses before each phase
/x-release patch --hotfix      — hotfix release from main
/x-release patch --skip-tests  — skip test validation
/x-release --continue-after-merge  — resume after release PR manually merged
/x-release --status            — show current release status (read-only)
/x-release --abort             — abort active release with cleanup
```

## Parameters

| Flag | Description |
|------|-------------|
| Bump type or `X.Y.Z` | `major`, `minor`, `patch`, or explicit version. Auto-detect from Conventional Commits if omitted (see `references/auto-version-detection.md`). `VERSION_NO_BUMP_SIGNAL` when no qualifying commits found. `VERSION_INVALID_FORMAT` when explicit version fails `^\d+\.\d+\.\d+$`. |
| `--dry-run` | Preview plan without executing any changes |
| `--hotfix` | Create hotfix release from `main` instead of `develop` |
| `--continue-after-merge` | Resume from `APPROVAL_PENDING` state after PR merged. Requires existing state file. |
| `--interactive` | With `--dry-run`: interactive walkthrough pausing before each phase. Without: deprecated no-op (warns). |
| `--non-interactive` | Skip Phase 8 approval gate menu; print legacy HALT text and exit 0 (CI mode). |
| `--skip-review` | Skip `x-review-pr` fire-and-forget in OPEN-RELEASE-PR |
| `--ci-watch` | Opt-in: poll CI on release PR via `x-pr-watch-ci`; abort on CI failure |
| `--signed-tag` | Create GPG-signed tag (`git tag -s`) instead of annotated |
| `--skip-tests` | Skip VALIDATE-DEEP test execution (warning emitted) |
| `--no-publish` | Create release locally without pushing |
| `--no-github-release` | Skip GitHub Release prompt in Phase 11 (CI path) |
| `--state-file <path>` | Override state file path (default: `plans/release-state-X.Y.Z.json`) |
| `--abort [--yes]` | Abort active release: close PRs, delete branches, remove state file. `--yes` skips confirmations. |
| `--status` | Show current release status read-only; exit 0 |

**13-phase workflow:**

```
0. RESUME-DETECT → 1. DETERMINE → 1.5. PRE-FLIGHT → 2. VALIDATE-DEEP
→ 3. BRANCH → 4. UPDATE → 5. CHANGELOG → 6. COMMIT → 7. OPEN-RELEASE-PR
→ 7.5. CI-WATCH (opt-in) → 8. APPROVAL-GATE → 9. TAG → 10. BACK-MERGE-DEVELOP
→ 11. PUBLISH → 12. CLEANUP → 13. SUMMARY
```

**Phase VALIDATE-DEEP (replaces Step 2):** 10 checks; advances state file `phase: VALIDATED`. `--skip-tests` skips checks 4, 5, 6; checks 1, 2, 3, 7, 8 are always-mandatory; check 9 conditional on `{{GENERATION_COMMAND}}` presence; check 10 bypassed with `--skip-integrity`.

| # | Check | Command | Error Code |
|---|-------|---------|-----------|
| 1 | Working dir clean | `git status --porcelain` | `VALIDATE_DIRTY_WORKDIR` |
| 2 | Correct base branch | `git branch --show-current` | `VALIDATE_WRONG_BRANCH` |
| 3 | `[Unreleased]` CHANGELOG section non-empty | parse `CHANGELOG.md` | `VALIDATE_EMPTY_UNRELEASED` |
| 4 | Build passes | `{{BUILD_COMMAND}}` | `VALIDATE_BUILD_FAILED` |
| 5 | Coverage ≥ `{{COVERAGE_LINE_THRESHOLD}}`% line / `{{COVERAGE_BRANCH_THRESHOLD}}`% branch | coverage report | `VALIDATE_COVERAGE_LINE` / `VALIDATE_COVERAGE_BRANCH` |
| 6 | Golden files current | `{{GOLDEN_TEST_COMMAND}}` | `VALIDATE_GOLDEN_DRIFT` |
| 7 | No hardcoded version | `grep -r CURRENT_VERSION` | `VALIDATE_HARDCODED_VERSION` |
| 8 | Cross-file version consistency | diff version files | `VALIDATE_VERSION_MISMATCH` |
| 9 | Generation dry-run | `{{GENERATION_COMMAND}}` | `VALIDATE_GENERATION_DRIFT` |
| 10 | Integrity drift | drift analysis | `INTEGRITY_DRIFT` |

## Phase Workflow (Rule 25 — Task Hierarchy)

Thirteen phases (Rule 25 REGRA-001, EPIC-0055). Each numbered phase opens with a PRE gate + `TaskCreate`, closes with a POST/FINAL gate + `TaskUpdate(completed)`. Phases 0, 1.5, 7.5 are exempted from gates (resume-detect, pre-flight, and optional CI-Watch). Full per-phase implementation in `references/full-protocol.md`.

<!-- phase-no-gate: read-only state-file detection and arg normalization; no artifact produced -->
## Phase 0 - Resume Detect

Open a phase tracker (close with `TaskUpdate(id: phase0TaskId, status: "completed")` after resume check):

    TaskCreate(subject: "RELEASE › Phase 0 - Resume Detect", activeForm: "Detecting release resume state")

Check for existing `state.json` (`--state-file` override or default `plans/release-state-X.Y.Z.json`). If `--continue-after-merge`: load state, verify `phase == APPROVAL_PENDING`. Handle `--status` (read-only, exit 0) and `--abort`. See `references/full-protocol.md §Phase 0`.

    TaskUpdate(id: phase0TaskId, status: "completed")

## Phase 1 - Determine

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-1-Determine")
    TaskCreate(subject: "RELEASE › Phase 1 - Determine", activeForm: "Determining release version")

Detect bump type from Conventional Commits (`feat:`→MINOR, `fix:`→PATCH, `!:`→MAJOR) or apply explicit argument. Validate `X.Y.Z` format. Emit `VERSION_NO_BUMP_SIGNAL` when no qualifying commits. Write initial `state.json` with `version`, `bumpType`, `phase: DETERMINED`. See `references/auto-version-detection.md`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-1-Determine --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase1TaskId, status: "completed")

<!-- phase-no-gate: pre-flight is a lightweight advisory check with no artifact output -->
## Phase 1.5 - Pre-Flight

Open a phase tracker (close with `TaskUpdate(id: phase15TaskId, status: "completed")` after checks):

    TaskCreate(subject: "RELEASE › Phase 1.5 - Pre-Flight", activeForm: "Running pre-flight checks")

Advisory checks: git remote reachable, no active release branch of same version, enough disk space for worktree. Warn on issues; do not abort. See `references/full-protocol.md §Phase 1.5`.

    TaskUpdate(id: phase15TaskId, status: "completed")

## Phase 2 - Validate Deep

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-2-ValidateDeep")
    TaskCreate(subject: "RELEASE › Phase 2 - Validate Deep", activeForm: "Running deep validation checks")

Run the 10-check VALIDATE-DEEP matrix (see table above). Advance state to `phase: VALIDATED`. Errors: `VALIDATE_DIRTY_WORKDIR`, `VALIDATE_BUILD_FAILED`, `VALIDATE_COVERAGE_LINE`, `VALIDATE_COVERAGE_BRANCH`, `VALIDATE_GOLDEN_DRIFT`, `VALIDATE_EMPTY_UNRELEASED`, `VALIDATE_HARDCODED_VERSION`, `VALIDATE_VERSION_MISMATCH`, `VALIDATE_GENERATION_DRIFT`, `INTEGRITY_DRIFT`. See `references/full-protocol.md §Phase 2`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-2-ValidateDeep --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase2TaskId, status: "completed")

## Phase 3 - Branch

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-3-Branch")
    TaskCreate(subject: "RELEASE › Phase 3 - Branch", activeForm: "Creating release branch")

Invoke `x-git-branch` to create `release/X.Y.Z` from `develop` (or `hotfix/X.Y.Z` from `main` when `--hotfix`). Idempotent: no-op if branch already exists. Advance state to `phase: BRANCHED`. See `references/full-protocol.md §Phase 3`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-3-Branch --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase3TaskId, status: "completed")

## Phase 4 - Update

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-4-Update")
    TaskCreate(subject: "RELEASE › Phase 4 - Update", activeForm: "Updating version files")

Bump version in all version-bearing files (`pom.xml`, version constants, etc.) to `X.Y.Z`. Cross-validate all files agree. Advance state to `phase: UPDATED`. See `references/full-protocol.md §Phase 4`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-4-Update --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase4TaskId, status: "completed")

## Phase 5 - Changelog

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-5-Changelog")
    TaskCreate(subject: "RELEASE › Phase 5 - Changelog", activeForm: "Generating changelog entry")

Invoke `x-release-changelog` to promote `[Unreleased]` section to `[X.Y.Z] - DATE`. Advance state to `phase: CHANGELOG_UPDATED`. See `references/full-protocol.md §Phase 5`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-5-Changelog --expected-artifacts CHANGELOG.md")
    TaskUpdate(id: phase5TaskId, status: "completed")

## Phase 6 - Commit

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-6-Commit")
    TaskCreate(subject: "RELEASE › Phase 6 - Commit", activeForm: "Committing release artifacts")

Commit version bumps + CHANGELOG via `x-git-commit` with message `chore(release): X.Y.Z`. Push `release/X.Y.Z` to origin. Advance state to `phase: COMMITTED`. See `references/full-protocol.md §Phase 6`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-6-Commit --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase6TaskId, status: "completed")

## Phase 7 - Open Release PR

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-7-OpenReleasePR")
    TaskCreate(subject: "RELEASE › Phase 7 - Open Release PR", activeForm: "Opening release PR to main")

Create PR `release/X.Y.Z → main` via `gh pr create`. If `--skip-review` absent: fire `x-review-pr` in background (fire-and-forget). Advance state to `phase: PR_OPEN`. Record `prNumber` in state. See `references/full-protocol.md §Phase 7`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-7-OpenReleasePR --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase7TaskId, status: "completed")

<!-- phase-no-gate: optional CI-watch poll loop; skipped unless --ci-watch flag present -->
## Phase 7.5 - CI Watch

Open a phase tracker only when `--ci-watch` is set (close with `TaskUpdate` after poll completes):

    TaskCreate(subject: "RELEASE › Phase 7.5 - CI Watch", activeForm: "Polling CI on release PR")

**MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24 + Rule 45):** Invoke the `x-pr-watch-ci` skill via the Skill tool on `prNumber`. On `CI_FAILED` (exit 20) or `TIMEOUT` (exit 30): abort release with corresponding error. On success (exit 0): advance to Phase 8. See `references/full-protocol.md §Phase 7.5`.

    Skill(skill: "x-pr-watch-ci", args: "--pr-number {prNumber}")

    TaskUpdate(id: phase75TaskId, status: "completed")

## Phase 8 - Approval Gate

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-release Phase-Approval-Gate`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-8-ApprovalGate")
    TaskCreate(subject: "RELEASE › Phase 8 - Approval Gate", activeForm: "Awaiting release approval")

Persist state with `phase: APPROVAL_PENDING`. Present EPIC-0043 gate menu (PROCEED / FIX-PR / ABORT) unless `--non-interactive`. On PROCEED: advance to Phase 9 (TAG). On FIX-PR: `Skill(skill: "x-pr-fix", args: "<prNumber>")` then loop (max 3 cycles). See `references/approval-gate-workflow.md`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-release Phase-Approval-Gate ok`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-8-ApprovalGate --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase8TaskId, status: "completed")

## Phase 9 - Tag

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-9-Tag")
    TaskCreate(subject: "RELEASE › Phase 9 - Tag", activeForm: "Tagging release on main")

After PR merged: tag `main` HEAD as `vX.Y.Z` (annotated, or GPG-signed with `--signed-tag`). Push tag. Error: `TAG_EXISTS`. Advance state to `phase: TAGGED`. See `references/full-protocol.md §Phase 9`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-9-Tag --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase9TaskId, status: "completed")

## Phase 10 - Back Merge Develop

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-10-BackMerge")
    TaskCreate(subject: "RELEASE › Phase 10 - Back Merge Develop", activeForm: "Back-merging release into develop")

Create PR `release/X.Y.Z → develop` via `gh pr create --auto-merge`. On conflict: emit `BACK_MERGE_CONFLICT`; see `references/backmerge-strategies.md` for resolution flow. Advance state to `phase: BACK_MERGED`. See `references/full-protocol.md §Phase 10`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-10-BackMerge --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase10TaskId, status: "completed")

## Phase 11 - Publish

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-11-Publish")
    TaskCreate(subject: "RELEASE › Phase 11 - Publish", activeForm: "Publishing GitHub release")

Unless `--no-github-release`: create GitHub Release via `gh release create vX.Y.Z --notes-from-tag`. Unless `--no-publish`: push any remaining artifacts. Advance state to `phase: PUBLISHED`. See `references/full-protocol.md §Phase 11`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-11-Publish --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase11TaskId, status: "completed")

## Phase 12 - Cleanup

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-12-Cleanup")
    TaskCreate(subject: "RELEASE › Phase 12 - Cleanup", activeForm: "Cleaning up release branch")

Delete `release/X.Y.Z` branch locally and on origin. Remove state file. Advance state to `phase: COMPLETED`. See `references/full-protocol.md §Phase 12`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-release --phase Phase-12-Cleanup --expected-artifacts plans/release-state-{version}.json")
    TaskUpdate(id: phase12TaskId, status: "completed")

## Phase 13 - Summary

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-release --phase Phase-13-Summary")
    TaskCreate(subject: "RELEASE › Phase 13 - Summary", activeForm: "Generating release summary")

Print Git Flow cycle explainer (see `references/git-flow-cycle-explainer.md`). Emit final release summary: version, tag, PR numbers, durations. Advance state to `COMPLETED`.

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode final --skill x-release --phase Phase-13-Summary --expected-artifacts CHANGELOG.md")
    TaskUpdate(id: phase13TaskId, status: "completed")

## Output Contract

| Artifact | Path/Description |
|----------|-----------------|
| Release branch | `release/X.Y.Z` (or `hotfix/X.Y.Z` with `--hotfix`) |
| Release PR | `release/X.Y.Z → main`; URL in state file |
| Back-merge PR | `release/X.Y.Z → develop` via `BACK-MERGE-DEVELOP` phase; conflict-aware flow in `references/backmerge-strategies.md` |
| Git tag | `vX.Y.Z` on `main` HEAD after `RESUME-AND-TAG` via `OPEN-RELEASE-PR` → `APPROVAL-GATE` → `APPROVAL_PENDING` (annotated or GPG-signed) |
| State file | `plans/release-state-X.Y.Z.json`; schema in `references/state-file-schema.md`; initialized with `schemaVersion: 2` (`.schemaVersion != 2` emits `Expected: 2` error) |
| CHANGELOG | Updated `CHANGELOG.md` via `x-release-changelog` |

## Error Envelope

| Code | Condition |
|------|-----------|
| `VERSION_NOT_FOUND` | No previous tag found; cannot auto-detect |
| `DIRTY_WORKDIR` | Uncommitted changes detected in VALIDATE-DEEP |
| `COVERAGE_BELOW_THRESHOLD` | Line < 95% or branch < 90% in VALIDATE-DEEP |
| `CHANGELOG_NOT_UPDATED` | `[Unreleased]` section empty in VALIDATE-DEEP |
| `APPROVAL_GATE_ABORTED` | Operator selected ABORT in Phase 8 menu |
| `CI_FAILED` | `--ci-watch` detected CI failure (exit 20) or timeout (exit 30) |
| `BACK_MERGE_CONFLICT` | `develop` diverged from release branch; manual resolution required |
| `TAG_EXISTS` | Git tag `vX.Y.Z` already present on remote |
| `ABORT_NO_RELEASE` | `--abort` called but no state file exists |
| `GATE_FIX_LOOP_EXCEEDED` | 3 consecutive FIX-PR cycles in Phase 8 without convergence |

## Full Protocol

> Complete per-phase implementation (Phases 0-13 with decision tables, bash commands, version-detection algorithm, VALIDATE-DEEP 10-check matrix, back-merge conflict-resolution flow, SUMMARY Git Flow cycle explainer) in [`references/full-protocol.md`](references/full-protocol.md). Additional references: [`approval-gate-workflow.md`](references/approval-gate-workflow.md), [`auto-version-detection.md`](references/auto-version-detection.md), [`backmerge-strategies.md`](references/backmerge-strategies.md), [`state-file-schema.md`](references/state-file-schema.md), [`interactive-flow-walkthrough.md`](references/interactive-flow-walkthrough.md), [`prompt-flow.md`](references/prompt-flow.md), [`git-flow-cycle-explainer.md`](references/git-flow-cycle-explainer.md).
