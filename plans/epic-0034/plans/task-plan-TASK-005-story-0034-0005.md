# Task Plan -- TASK-0034-0005-005

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0005-005 |
| Story ID | story-0034-0005 |
| Epic ID | 0034 |
| Source Agent | merged(TechLead, ProductOwner) |
| Type | quality-gate + validation |
| TDD Phase | VERIFY |
| TPP Level | N/A |
| Layer | cross-cutting |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Create the final epic Pull Request from `feature/epic-0034-remove-non-claude-targets` to `develop`. Produce a comprehensive PR body summarizing all 5 stories with before/after metrics, BREAKING CHANGE notice, migration instructions, rollback procedure, and JaCoCo report link. Verify the PR is mergeable without conflicts and CI is green.

## Implementation Guide

### Step 1 - Verify branch state

```bash
cd /Users/edercnj/workspaces/ia-dev-environment
git status
# Expected: clean working tree

git branch --show-current
# Expected: feature/epic-0034-remove-non-claude-targets

git log --oneline develop..HEAD | head -30
# Expected: list of commits from all 5 stories (001..005)
```

Verify every commit on the branch follows Conventional Commits format:

```bash
git log develop..HEAD --format='%s' \
  | grep -vE '^(feat|fix|docs|refactor|test|chore|perf|style|build|ci)(\([a-z0-9/-]+\))?!?: '
# Expected: empty output (0 non-conforming commits)
```

### Step 2 - Ensure branch is up-to-date with develop

```bash
git fetch origin
git merge-base --is-ancestor origin/develop HEAD && echo "UP TO DATE" || echo "NEEDS REBASE"
```

If not up to date:

```bash
git rebase origin/develop
# Resolve conflicts if any. The Tech Lead approves conflict resolution.
```

Re-run `mvn clean verify` after any rebase to confirm the final state is still green.

### Step 3 - Compose PR body

Create a file `/tmp/epic-0034-pr-body.md`:

```markdown
## Summary

EPIC-0034 removes non-Claude targets (GitHub Copilot, Codex, Agents) from the `ia-dev-environment` generator, reducing scope to Claude Code only.

This is a **BREAKING CHANGE**. CLI `--platform` no longer accepts `copilot`, `codex`, `agents`, or `all` - only `claude-code` (also the new default).

## Stories

### story-0034-0001 - Remove GitHub Copilot
- Deleted 8 `Github*Assembler` classes + 1 `PrIssueTemplateAssembler`
- Deleted 15 test classes + 1 fixture
- Deleted `java/src/main/resources/targets/github-copilot/` (~131 files)
- Deleted `.github/` subdirs in 17 golden profiles (~2324 files; workflows preserved)
- Removed `Platform.COPILOT` and `AssemblerTarget.GITHUB`
- Cleaned 18 setup-config YAMLs

### story-0034-0002 - Remove Codex
- Deleted 7 `Codex*Assembler` classes
- Deleted 6 test classes
- Deleted `java/src/main/resources/targets/codex/` (~15 files)
- Deleted `.codex/` subdirs in 17 golden profiles (~2944 files)
- Removed `Platform.CODEX` and `AssemblerTarget.CODEX`

### story-0034-0003 - Remove Agents generic target
- Deleted 6 `Agents*Test` classes + 1 fixture (`AgentsTestFixtures`)
- Deleted any remaining `Agents*Assembler` classes
- Deleted `.agents/` subdirs in 17 golden profiles (~2910 files)
- Removed `AssemblerTarget.CODEX_AGENTS`

### story-0034-0004 - Sanitize shared code
- Deleted `ReadmeGithubCounter`
- Edited 10+ shared classes to remove multi-target conditionals
- Edited 5 smoke tests to remove nested Copilot/Codex classes
- Preserved `resources/shared/templates/` intact (RULE-004)

### story-0034-0005 - Documentation and final verification
- Reduced `CLAUDE.md` root by ~180 lines
- Cleaned residual references in `.claude/rules/`, `README.md`, `docs/`
- Added `CHANGELOG.md` `[Unreleased]` Removed, Changed, Migration, Rollback sections
- Regenerated `expected-artifacts.json` (~9500 -> ~830 entries per profile)
- Executed full E2E verification

## Metrics

### Code and fixtures

| Category | Before | After | Delta |
|---|---|---|---|
| Java main classes (generator core) | baseline | baseline - 18 | -18 |
| Java test classes | baseline | baseline - ~34 | -~34 |
| Test fixtures | baseline | baseline - 2 | -2 |
| Golden files (total) | 14285 | ~6107 | -~8178 (~57%) |
| Golden `.github/` (non-workflows) | 2324 | 0 | -2324 |
| Golden `.github/workflows/` (PROTECTED) | 95 | 95 | 0 |
| Golden `.codex/` | 2944 | 0 | -2944 |
| Golden `.agents/` | 2910 | 0 | -2910 |
| Golden `.claude/` (PROTECTED) | 5684 | 5684 | 0 |
| `resources/shared/templates/` (PROTECTED) | 57 | 57 | 0 |
| `resources/targets/github-copilot/` | 131 | 0 | -131 |
| `resources/targets/codex/` | 15 | 0 | -15 |
| `resources/targets/claude/` (PROTECTED) | 395 | 395 | 0 |
| `CLAUDE.md` (root) | 283 lines | ~100 lines | -~180 |
| `expected-artifacts.json` entries/profile | ~9500 | ~830 | -91% |

### Coverage

| Metric | Baseline | Post-Epic | Threshold (Rule 05) |
|---|---|---|---|
| Line | 95.69% | {to-fill} | >= 95% (RULE-002: <= 2pp drop) |
| Branch | 90.69% | {to-fill} | >= 90% (RULE-002: <= 2pp drop) |

### Build time

| Stage | Baseline | Post-Epic |
|---|---|---|
| `mvn clean verify` | 6:01 | {to-fill} |

### Tests

| Metric | Baseline | Post-Epic |
|---|---|---|
| Total passing | 837 | {to-fill} |
| Failures | 0 | 0 |
| Errors | 0 | 0 |
| Skipped | 0 | 0 |

## BREAKING CHANGE Notice

The CLI `--platform` flag now accepts only `claude-code`. Previous values (`copilot`, `codex`, `agents`, `all`) are rejected with a clear error message.

**Migration:**

- Scripts invoking `ia-dev-env generate --platform {copilot|codex|agents|all}` must update to `--platform claude-code` or drop the flag (the new default is `claude-code`).
- Downstream tooling consuming `.github/instructions/`, `.github/skills/`, `.github/prompts/`, `.codex/config.toml`, or `.agents/` artifacts must be retired - these outputs are no longer produced.
- `.github/workflows/` files in golden fixtures are preserved; CI/CD is unaffected (RULE-003).

Per Rule 08 and SemVer, the next release must be a **MAJOR version bump**.

## Rollback

This epic introduces no database migrations and no persistent state changes (the generator is stateless). To roll back, revert the merge commit on `develop` and re-run builds. Prior multi-target behavior is restored atomically.

## Quality Gates

- [x] `mvn clean verify` green
- [x] JaCoCo line coverage >= 95% (RULE-002: <= 2pp drop from 95.69% baseline)
- [x] JaCoCo branch coverage >= 90% (RULE-002: <= 2pp drop from 90.69% baseline)
- [x] 6 grep sanity checks all return zero
- [x] CLI rejects `copilot`, `codex`, `agents` with clear error (no stack trace - CWE-209)
- [x] CLI `--platform claude-code` and default succeed (~830 files for `java-spring`)
- [x] `.github/workflows/` preserved (95 files - RULE-003)
- [x] `resources/shared/templates/` preserved (57 files - RULE-004)
- [x] Every commit follows Conventional Commits
- [x] `BREAKING CHANGE:` footer present on breaking commits
- [x] CHANGELOG.md `[Unreleased]` populated with Removed, Changed, Migration, Rollback, Security sections
- [x] All 9 Gherkin scenarios in story-0034-0005 validated against evidence

## Test Plan

- [x] Unit + integration tests: `mvn clean verify`
- [x] Smoke tests: `PlatformDirectorySmokeTest`, `AssemblerRegressionSmokeTest`, `CliModesSmokeTest`
- [x] Manual CLI smoke: rejected platforms + accepted platforms + default
- [x] Golden file count regression: `find | wc -l`
- [x] Invariants: RULE-003 workflows, RULE-004 templates

## JaCoCo Report

Attached as CI artifact / link: {to-fill when CI completes}

## References

- EPIC: `plans/epic-0034/epic-0034.md`
- Implementation Map: `plans/epic-0034/implementation-map-0034.md`
- Baseline: `plans/epic-0034/baseline-pre-epic.md`
- Stories: `plans/epic-0034/story-0034-{0001..0005}.md`
- Final verification report: `plans/epic-0034/reports/task-005-004/verification-report.md`

---

Ref: EPIC-0034
```

Fill in the `{to-fill}` placeholders using the evidence gathered in TASK-004.

### Step 4 - Push branch

```bash
git push -u origin feature/epic-0034-remove-non-claude-targets
```

Note: if the branch was previously pushed, use `git push` (no `-u`).

### Step 5 - Create PR via gh CLI

```bash
gh pr create \
  --base develop \
  --head feature/epic-0034-remove-non-claude-targets \
  --title "feat(cli)!: remove non-Claude targets from generator (EPIC-0034)" \
  --body-file /tmp/epic-0034-pr-body.md \
  --label 'breaking-change,epic-0034,refactor'
```

Capture the PR URL returned.

### Step 6 - Monitor CI

```bash
gh pr checks $(gh pr view --json number -q .number) --watch
```

Wait until all checks are green.

If any check fails, analyze logs:

```bash
gh pr checks $(gh pr view --json number -q .number)
gh run list --branch feature/epic-0034-remove-non-claude-targets --limit 5
```

### Step 7 - Confirm mergeable

```bash
gh pr view --json mergeable,mergeStateStatus
```

Expected: `mergeable = MERGEABLE`, `mergeStateStatus = CLEAN`.

If conflicts appear, rebase on develop, push, and re-verify.

### Step 8 - No merge

DO NOT merge the PR in this task. Merging is a manual human-reviewer step. This task concludes when:

- PR is open against `develop`
- CI is green
- PR is mergeable
- PR body is complete
- Reviewers have been requested

## Definition of Done

- [ ] Branch state clean (no uncommitted changes)
- [ ] All commits on branch conform to Conventional Commits
- [ ] Branch up to date with `origin/develop` (rebased if needed)
- [ ] `mvn clean verify` still green after any rebase
- [ ] PR title: `feat(cli)!: remove non-Claude targets from generator (EPIC-0034)`
- [ ] PR target branch: `develop` (NOT `main`)
- [ ] PR body contains all 5 story summaries
- [ ] PR body contains complete metrics tables (code, coverage, build time, tests)
- [ ] PR body contains BREAKING CHANGE notice with migration instructions
- [ ] PR body contains rollback procedure
- [ ] PR body contains JaCoCo report link or artifact reference
- [ ] PR body references baseline, epic, implementation map, stories, verification report
- [ ] Labels applied: `breaking-change`, `epic-0034`, `refactor`
- [ ] CI green on PR
- [ ] PR is mergeable without conflicts
- [ ] PR NOT merged (manual human step)
- [ ] PR URL recorded in final output

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0005-004 | All E2E verification must be complete and evidence archived before PR creation so the PR body can reference concrete metrics |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Merge conflicts with `develop` due to unrelated PRs merged during the epic | Medium | Medium | Step 2 checks up-to-dateness. Rebase and re-verify. Document any non-trivial conflict resolution in the PR description. |
| CI fails due to environment drift (new flaky test, timeout) | Medium | Medium | Step 6 monitors checks. Known-flaky tests documented in the repo's CI docs. Retry once; if still failing, root-cause. |
| PR body references wrong branch (`main` instead of `develop`) | Low | High | Step 5 explicit `--base develop`. Rule 09 forbids PRs directly to main from feature branches. |
| Placeholder `{to-fill}` values left in the PR body | Medium | Medium | Step 3 requires filling values from TASK-004 evidence before creating the PR. Reviewer catches as low-effort rework. |
| gh CLI not authenticated or missing | Low | Low | Pre-check `gh auth status` before Step 5. |
| Epic PR accidentally merged in this task | Low | High | Step 8 explicitly forbids merging. This task ends at PR open + mergeable. |
