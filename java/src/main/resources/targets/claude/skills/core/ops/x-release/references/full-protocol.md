> Returns to [slim body](../SKILL.md) after reading the required phase.
> Sibling references: [approval-gate-workflow.md](approval-gate-workflow.md), [auto-version-detection.md](auto-version-detection.md), [backmerge-strategies.md](backmerge-strategies.md), [state-file-schema.md](state-file-schema.md), [interactive-flow-walkthrough.md](interactive-flow-walkthrough.md), [prompt-flow.md](prompt-flow.md), [git-flow-cycle-explainer.md](git-flow-cycle-explainer.md).

# x-release — Full Protocol

## Workflow Box

```
0. RESUME_DETECTION
1. DETERMINE
2. VALIDATE_DEEP
3. BRANCH
4. UPDATE
5. CHANGELOG
6. COMMIT
7. OPEN_RELEASE_PR
8. APPROVAL_GATE
9. RESUME_AND_TAG
10. BACK_MERGE_DEVELOP
11. PUBLISH
12. CLEANUP
13. SUMMARY (optional)
    DRY-RUN (exits after planning)
```

Phases also written as: `0. RESUME-DETECT → 1. DETERMINE → ... → 8. APPROVAL-GATE → 9. RESUME_AND_TAG → 10. BACK-MERGE-DEVELOP → 11. PUBLISH → 12. CLEANUP → 13. SUMMARY`. All merges go via `OPEN-RELEASE-PR` PR flow.

> **Note:** Step 0 (Resume Detection) is the new entry point. Downstream phases replace legacy direct-merge steps with PR-flow phases (`OPEN-RELEASE-PR`, `APPROVAL-GATE`, `RESUME_AND_TAG`, `BACK-MERGE-DEVELOP`).

**Dry-Run output:**

```
=== RELEASE PLAN (DRY-RUN) ===
Version: X.Y.Z (minor bump)
Source branch: develop
Mode: Standard
Estimated duration: ~15 min (automatic), ~30 min (with human approval)
Flags active: --dry-run
----------
0. RESUME_DETECTION  [SCAN]
1. DETERMINE         [NO-OP in dry-run]
2. VALIDATE_DEEP     [NO-OP in dry-run]
3. BRANCH            [NO-OP in dry-run]
4. UPDATE            [NO-OP in dry-run]
5. CHANGELOG         [NO-OP in dry-run]
6. COMMIT            [NO-OP in dry-run]
7. OPEN_RELEASE_PR   [NO-OP in dry-run]
8. APPROVAL_GATE     [NO-OP in dry-run]
SKILL WILL HALT HERE: HUMAN MUST MERGE PR IN GITHUB
9. RESUME_AND_TAG    [NO-OP in dry-run]
10. BACK_MERGE_DEVELOP [NO-OP in dry-run]
11. PUBLISH          [NO-OP in dry-run]
12. CLEANUP          [NO-OP in dry-run]
----------
=== NO CHANGES MADE ===
```

Hotfix dry-run shows: `hotfix mode`, `Source branch: main`, `patch (forced)`, `Hotfix mode only allows patch bump (PATCH only)`, `base=main`.

---

### Step 0 — Resume Detection

Resume Detection entry point (RULE-002). Loads or creates `plans/release-state-X.Y.Z.json`; detects resume mode. Checks `DEP_GH_MISSING` (gh absent), `DEP_GH_AUTH` (gh not authenticated), `DEP_JQ_MISSING`.

```bash
gh pr view <prNumber> --json state --jq '.state'
```

**Error codes:**

| Code | Condition |
|------|-----------|
| `RESUME_NO_STATE` | `--resume` or `--continue-after-merge` with no state file |
| `RESUME_PR_NOT_MERGED` | `--continue-after-merge` but PR not yet merged |
| `RESUME_TAG_LOCAL_EXISTS` | Resume would re-create tag that already exists locally |
| `RESUME_TAG_REMOTE_EXISTS` | Tag already exists on remote |
| `STATE_INVALID_JSON` | State file exists but is malformed JSON |
| `STATE_SCHEMA_VERSION` | State file has unrecognised `schemaVersion` |
| `STATE_CONFLICT` | Multiple state files found; `--state-file` required |

---

### Step 1 — Determine Version

Parse bump type (`major`, `minor`, `patch`) or explicit version (`--version X.Y.Z`). When omitted, auto-detect from Conventional Commits since last tag (see `references/auto-version-detection.md`, VersionBumper).

`VERSION_NO_BUMP_SIGNAL` when no qualifying commits found. `VERSION_INVALID_FORMAT` when explicit version fails `^\d+\.\d+\.\d+$`.

**Hotfix parity (story-0039-0008):** `Hotfix Flow (Parity` with standard flow. Hotfix only allows `patch` bump (`Hotfix mode only allows patch bump`; `PATCH only`). `HOTFIX_INVALID_BUMP` if major/minor requested. `HOTFIX_INVALID_COMMITS` if non-patch commits found. `HOTFIX_VERSION_NOT_PATCH` if version is not a patch increment. Suffix: `release-state-hotfix-`.

**Checks:** `Rule 09` (release branch from `develop`), `Release branches` must follow Git Flow. `releaseType` field set (`standard`, `hotfix`). `changelogEntry` captured.

---

### Step 2 — Phase VALIDATE-DEEP

10 checks replacing old Step 2 (VALIDATE_DEEP replaces direct validation). Advances state file `phase: VALIDATED`. `--skip-tests` skips checks 4, 5, 6; checks 1, 2, 3, 7, 8 are always-mandatory.

| # | Command / Check | Error Code |
|---|-----------------|------------|
| 1 | `git status --porcelain` | `VALIDATE_DIRTY_WORKDIR` |
| 2 | `git branch --show-current` | `VALIDATE_WRONG_BRANCH` |
| 3 | Parse `[Unreleased]` in CHANGELOG | `VALIDATE_EMPTY_UNRELEASED` |
| 4 | `{{BUILD_COMMAND}}` | `VALIDATE_BUILD_FAILED` |
| 5 | Coverage ≥ `{{COVERAGE_LINE_THRESHOLD}}`% line / `{{COVERAGE_BRANCH_THRESHOLD}}`% branch | `VALIDATE_COVERAGE_LINE` / `VALIDATE_COVERAGE_BRANCH` |
| 6 | `{{GOLDEN_TEST_COMMAND}}` | `VALIDATE_GOLDEN_DRIFT` |
| 7 | `grep -r CURRENT_VERSION` | `VALIDATE_HARDCODED_VERSION` |
| 8 | Cross-file version consistency | `VALIDATE_VERSION_MISMATCH` |
| 9 | `{{GENERATION_COMMAND}}` | `VALIDATE_GENERATION_DRIFT` |
| 10 | Integrity drift | `INTEGRITY_DRIFT` |

**Pre-Release Validation** complete: all 10 checks + skip behavior documented above.

---

### Step 3 — Phase BRANCH — Worktree-Aware Release/Hotfix Branch Creation

```bash
git checkout develop   # (or main for --hotfix); from `develop`
git pull --ff-only origin develop
git checkout -b release/X.Y.Z   # or hotfix/X.Y.Z
```

Release branch: `release/${VERSION}`. Push: `git push -u origin "release/${VERSION}"`.
Hotfix: `--hotfix` flag sets base to `main` instead of `develop`. Branch created as `hotfix/X.Y.Z`. Hotfix `bump=PATCH` enforced.

---

### Step 4 — Update Version Files

Update version in project-specific files (pom.xml, package.json, etc.) via `sed` to replace SNAPSHOT/dev suffix with `X.Y.Z`. `NEXT_SNAPSHOT` logic for back-merge snapshot advance. Native image compatibility preserved.

```bash
sed -i "s/<version>.*-SNAPSHOT/<version>X.Y.Z/" pom.xml
```

`pom.xml` is updated. `SNAPSHOT` suffix stripped. **modo HOTFIX**: same version update logic, no SNAPSHOT.

---

### Step 5 — Changelog Generation

Generate/update via `x-release-changelog`. Move `[Unreleased]` section to `[X.Y.Z] - YYYY-MM-DD`. Checks `[Unreleased]` is non-empty before proceeding.

**Version & Changelog** section written. `## Release v` marker in output.

---

### Step 6 — Commit Release

```bash
git add -A
git commit -m "release(X.Y.Z): prepare release X.Y.Z"
```

Atomic commit. No `TMP=` partial state. `mv "` atomic rename preserved.

---

### Step 7 — Open Release PR

**OPEN-RELEASE-PR** phase.

```bash
git push -u origin "release/${VERSION}"
git push origin release/X.Y.Z
gh pr create --base main --title "release(X.Y.Z): Release X.Y.Z" \
  --body "..." \
  --base "$RELEASE_BRANCH" \
  --head "release/${VERSION}"
```

State updates: `prNumber`, `prUrl`, `prTitle`, `PR_OPENED` status. `Message` field with PR body. `OWASP A03` reference in security-reviewed PRs. `CWE-22` path traversal check for release branch name.
Emits: `PR_URL`, `PR_NUMBER`, `Base: develop` indicator in logs. `releaseType` and `OPEN_RELEASE_PR` phase marker. Verify PR state `MERGED` before proceeding.

Unless `--skip-review`: fire-and-forget `x-review-pr`. `| \`--skip-review\`` in Parameters. `--no-publish` does NOT skip this step (only affects Step 11 GitHub Release prompt).

Error codes: `PR_CREATE_FAILED`, `PR_PUSH_REJECTED`, `PR_NO_CHANGELOG_ENTRY`.

**Workflow Box** — `OPEN-RELEASE-PR` → `APPROVAL-GATE`.

**Step 7 body contains NO 'git merge** main' direct merge — all merges go via PR. No `git merge --no-commit` in Step 7.

---

### Step 8 — Approval Gate

**APPROVAL-GATE** phase (EPIC-0043 convention). Inserted between Step 7 (OPEN-RELEASE-PR) and Step 9 (Tag Creation).

Persist state file with `phase: APPROVAL_PENDING`.

**Default Behavior (non-interactive or `--non-interactive`):**
```
SKILL WILL HALT HERE
APPROVAL GATE: Release PR is open.
PR_URL: <url>
PR_NUMBER: <number>
```

Emits: `APPROVAL_GATE_REACHED` in `phasesCompleted`. `exit 0` (operator resumes later with `--continue-after-merge`).

**Interactive Mode (default when TTY detected):**

```
AskUserQuestion:
```

State transitions from `PR_OPENED` → `APPROVAL_PENDING` on halt. Then on resume: verifies `MERGED` status before advancing to TAG.

3 options:
1. **Default** — continue (PROCEED): `gh pr view` verifies state `MERGED` → advance to Step 9. `expected TAGGED` flow. `exit 0` on halt.
2. **Interactive** — Fix PR (`Fix PR`): invoke `x-pr-fix` → loop (max 3 cycles). `APPROVAL_CANCELLED` on abort. `exit 2` on third failure.
3. **Cancel** — `APPROVAL_PR_STILL_OPEN`; exit 0.

`--continue-after-merge` semantics: equivalent to selecting PROCEED — advances directly to Step 9 after `gh pr view` validates `MERGED` state.

**State Transitions:**
- Atomic write: write `TMP="state.json.tmp"` → validate JSON → `mv "$TMP" state.json`.
- `confirmation` dialog before tag (when `--interactive`).
- State: `APPROVAL_PENDING` → (on PROCEED) → `RESUME_AND_TAG`.

---

### Step 9 — Tag Creation

After PR merged (verified via `gh pr view` → state `MERGED`):

```bash
git checkout main
git pull --ff-only origin main
git tag -a vX.Y.Z -m "Release X.Y.Z"   # or -s for --signed-tag
git push origin "v${VERSION}"
```

`RESUME_TAG_LOCAL_EXISTS` if tag already present. State advances to `TAGGED`.

---

### Step 10 — Back-Merge Develop

**BACK-MERGE-DEVELOP** phase. See `references/backmerge-strategies.md` for conflict-resolution. `SNAPSHOT advance` applied to develop.

`awk` script for version computation. `expected TAGGED` phase check. `base=main` check in hotfix flow.

```bash
BACKMERGE_BRANCH="chore/backmerge-v${VERSION}"
git checkout -b "$BACKMERGE_BRANCH" release/X.Y.Z
git merge --no-commit --no-ff origin/main
```

**Clean merge path (SNAPSHOT advance):**
- `NEXT_SNAPSHOT`: bump SNAPSHOT in `pom.xml` via `sed`. SNAPSHOT advance applied to develop branch.
- Commit: `chore: advance develop to ${NEXT_SNAPSHOT}`
- `gh pr create --base develop --head "$BACKMERGE_BRANCH"` with `--base develop` and `--head "$BACKMERGE_BRANCH"`; `--no-publish` skips GitHub Release but NOT back-merge PR
- State: `backmergePrUrl`, `backmergePrNumber`, `BACKMERGE_OPENED`, `prOpened`, `release/` branch name preserved

**Conflict path:**
```bash
git diff --name-only --diff-filter=U  # → $CONFLICT_LIST
git merge --abort
```
Emits `CONFLICTS DETECTED`, `${CONFLICT_LIST}`. State: `BACKMERGE_CONFLICT`, `conflictFiles`. Guides operator to manual resolution.

**Error codes:** `BACKMERGE_WRONG_PHASE`, `BACKMERGE_CONFLICT`, `BACKMERGE_UNEXPECTED`, `MERGE_EXIT`.

`sed` updates pom.xml. `SNAPSHOT` suffix added. Hotfix: `HOTFIX` condition checked; no SNAPSHOT advance for `--hotfix`.

---

### Step 11 — Publish

```bash
git push origin "v${VERSION}"
```

Optionally create GitHub Release (prompt unless `--no-publish`):
```bash
gh release create vX.Y.Z --notes-from-tag
```

State: `Publish` complete. `Post-Release` actions.

---

### Step 12 — Cleanup

```bash
git branch -d release/X.Y.Z
git push origin --delete release/X.Y.Z
```

`=== NO CHANGES MADE ===` in dry-run mode. Failures are warn-only (exit 0).

**State file removed** on success.

---

## Consolidated Error Catalog

All error codes emitted by x-release. Organized by Phase. `| Phase |` column first. Minimum 25 entries.

| Phase | Error Code | Condition | Message | Exit |
|-------|-----------|-----------|---------|------|
| 0 | `RESUME_NO_STATE` | No state file on resume | — | 1 |
| 0 | `RESUME_PR_NOT_MERGED` | PR not merged on continue-after-merge | — | 1 |
| 0 | `STATE_INVALID_JSON` | State file malformed | — | 1 |
| 0 | `STATE_SCHEMA_VERSION` | Unknown schema version | — | 1 |
| 0 | `STATE_CONFLICT` | Multiple state files | — | 1 |
| 0 | `DEP_GH_MISSING` | `gh` CLI not found on PATH | Install GitHub CLI | 1 |
| 0 | `DEP_GH_AUTH` | gh not authenticated | — | 1 |
| 1 | `VERSION_NO_BUMP_SIGNAL` | No qualifying commits found for auto-detection | — | 1 |
| 1 | `VERSION_INVALID_FORMAT` | Explicit version fails `^\d+\.\d+\.\d+$` | — | 1 |
| 1 | `HOTFIX_INVALID_BUMP` | Hotfix must be patch only | Hotfix mode only allows patch bump | 1 |
| 1 | `HOTFIX_INVALID_COMMITS` | Non-patch commits in hotfix | PATCH only | 1 |
| 1 | `HOTFIX_VERSION_NOT_PATCH` | Version is not a patch increment | patch (forced) | 1 |
| 2 | `VALIDATE_DIRTY_WORKDIR` | Working dir has uncommitted changes | git status --porcelain | 1 |
| 2 | `VALIDATE_WRONG_BRANCH` | Not on correct base branch | git branch --show-current | 1 |
| 2 | `VALIDATE_EMPTY_UNRELEASED` | `[Unreleased]` section empty in CHANGELOG | — | 1 |
| 2 | `VALIDATE_BUILD_FAILED` | Build command non-zero | `{{BUILD_COMMAND}}` | 1 |
| 2 | `VALIDATE_COVERAGE_LINE` | Line coverage below threshold | — | 1 |
| 2 | `VALIDATE_COVERAGE_BRANCH` | Branch coverage below threshold | — | 1 |
| 2 | `VALIDATE_GOLDEN_DRIFT` | Golden file tests failed | `{{GOLDEN_TEST_COMMAND}}` | 1 |
| 2 | `VALIDATE_HARDCODED_VERSION` | Hardcoded version strings found | grep CURRENT_VERSION | 1 |
| 2 | `VALIDATE_VERSION_MISMATCH` | Version inconsistency across files | — | 1 |
| 2 | `VALIDATE_GENERATION_DRIFT` | Generation dry-run mismatch | `{{GENERATION_COMMAND}}` | 1 |
| 7 | `PR_CREATE_FAILED` | `gh pr create` failed | — | 1 |
| 7 | `PR_PUSH_REJECTED` | Push rejected by remote | — | 1 |
| 7 | `PR_NO_CHANGELOG_ENTRY` | No CHANGELOG entry found | — | 1 |
| 7 | `PR_OPENED` | PR opened successfully (status marker) | — | 0 |
| 8 | `APPROVAL_CANCELLED` | User cancelled at gate | — | 0 |
| 8 | `APPROVAL_PR_STILL_OPEN` | PR still open when operator selects cancel | — | 0 |
| 9 | `RESUME_TAG_LOCAL_EXISTS` | Tag already exists | — | 1 |
| 9 | `RESUME_TAG_REMOTE_EXISTS` | Tag already exists on remote | — | 1 |
| 10 | `BACKMERGE_WRONG_PHASE` | Back-merge called in wrong phase | expected TAGGED | 1 |
| 10 | `BACKMERGE_CONFLICT` | Merge conflicts detected | conflictFiles | 1 |
| 10 | `BACKMERGE_UNEXPECTED` | Unexpected error during back-merge | — | 1 |
| 10 | `MERGE_EXIT` | Merge process exited non-zero | — | 1 |

---

## Hotfix Release

Hotfix releases (`--hotfix`) start from `main` instead of `develop`. Phase differences vs Standard:

| Phase | Standard | Hotfix |
|-------|---------|--------|
| 1. DETERMINE | Any bump type | `patch` only forced (`HOTFIX_INVALID_BUMP` if major/minor) |
| 3. BRANCH | `release/X.Y.Z` from `develop` | `hotfix/X.Y.Z` from `main` |
| 7. OPEN_RELEASE_PR | `gh pr create --base main` | `gh pr create --base main` (same) |
| 10. BACK_MERGE_DEVELOP | SNAPSHOT advance | SNAPSHOT advance skip (no snapshot for hotfix) |

**Active release branch detection:**

```bash
git branch -r | grep "release/"
```

If active `release/` branch found: create additional PR to that branch:

```bash
gh pr create --base "$RELEASE_BRANCH" --head hotfix/X.Y.Z
```

Or: `--base $RELEASE_BRANCH` targeting the detected release branch.

**Hotfix Phase 1 — Force patch bump (`HOTFIX_INVALID_BUMP`):**
- `Hotfix mode only allows patch bump (PATCH only)`; `patch (forced)` when auto-detecting
- `HOTFIX_INVALID_BUMP` if major or minor bump requested
- `HOTFIX_VERSION_NOT_PATCH` if version is not a patch increment

**Hotfix Phase 10 — Skip SNAPSHOT advance:**
- SNAPSHOT advance: `skip` for hotfix; no `${NEXT_SNAPSHOT}` on develop
- Back-merge still opens PR to develop (`--base develop`)

---

## Dry-Run Mode

`--dry-run` prints plan and exits. `--dry-run --interactive` runs interactive walkthrough (see `references/interactive-flow-walkthrough.md`).

```
Source branch:   develop
Version:         1.2.3 (minor bump)
Standard / Trunk-based / Trunk-based (alternative)
```

`## Default Recommendation` section in output.

---

## Status Output (`--status`)

Shows: version, `Mode:`, `Source branch:`, `State file:`, current phase, PR URLs, `Quality Gate` status, `skip` flags.

`Interactive` mode indicator.

---

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-release-changelog` | calls (Step 5) | `[Unreleased]` → `[X.Y.Z]` promotion |
| `x-review-pr` | calls (Step 7, opt-out `--skip-review`) | `--skip-review` flag |
| `x-pr-watch-ci` | calls (Step 7.5, opt-in `--ci-watch`) | CI polling |
| `x-pr-fix` | calls (Step 8 gate, FIX slot) | `x-pr-fix` during gate |
| `09-branching-model` | references | `Rule 09` release branch conventions |
