---
name: x-release
model: sonnet
description: "Orchestrates complete release flow using Git Flow release branches with approval gate, PR-flow (gh CLI) and deep validation: version bump (auto-detect or explicit), release branch creation from develop, deep validation (coverage, golden files, version consistency), version file updates, changelog generation, release commit, release PR via gh (optionally reviewed by x-review-pr), human approval gate with persistent state file, tag on main after merged PR, back-merge PR to develop with conflict detection, and cleanup. Supports hotfix releases from main, dry-run mode, resume via --continue-after-merge, in-session pause via --interactive, GPG-signed tags, skip-review opt-out, and custom state file path."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, Agent, Skill, AskUserQuestion
argument-hint: "[major|minor|patch|version] [--version X.Y.Z] [--last-tag <tag>] [--dry-run] [--skip-tests] [--no-publish] [--no-github-release] [--hotfix] [--continue-after-merge] [--interactive] [--non-interactive] [--no-prompt] [--signed-tag] [--skip-review] [--ci-watch] [--state-file <path>] [--skip-integrity] [--integrity-report <path>] [--max-parallel <N>] [--status] [--abort] [--yes] [--force]"
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

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-release Phase-Approval-Gate`

**Phase 8 — APPROVAL-GATE:** Persist state with `phase: APPROVAL_PENDING`. Present EPIC-0043 gate menu (PROCEED / FIX-PR / ABORT) unless `--non-interactive`. On PROCEED: advance to Phase 9 (TAG). On FIX-PR: `Skill(skill: "x-pr-fix", args: "<prNumber>")` then loop (max 3 cycles). See `references/approval-gate-workflow.md`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-release Phase-Approval-Gate ok`

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
