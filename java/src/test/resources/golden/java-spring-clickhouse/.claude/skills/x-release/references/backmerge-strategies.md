# Back-Merge Strategies for Release Flow

> **Source of Truth:** This document describes the back-merge strategies used
> by Phase BACK-MERGE-DEVELOP (Step 10) in the `x-release` skill. It covers
> both the clean merge flow and the conflict detection flow, including Java
> SNAPSHOT advance logic and state file transitions.

## Overview

After a release is tagged on `main` (phase `TAGGED`), the changes must be
propagated back to `develop`. The back-merge is performed via a PR
(`gh pr create --base develop`) instead of a direct `git merge` to comply
with Rule 09 (PR-Flow). A dry-run merge (`git merge --no-commit --no-ff`)
detects conflicts before committing to either the clean or conflict flow.

## Decision Tree

```
Start: phase == TAGGED
  │
  ├─ phase != TAGGED → ABORT BACKMERGE_WRONG_PHASE
  │
  ▼
Create branch: chore/backmerge-v${VERSION} from origin/develop
  │
  ▼
Dry-run merge: git merge --no-commit --no-ff origin/main
  │
  ├─ exit 0 (clean)
  │    │
  │    ├─ pom.xml exists AND not hotfix
  │    │    ├─ Compute NEXT_SNAPSHOT = MAJOR.(MINOR+1).0-SNAPSHOT
  │    │    ├─ sed pom.xml → NEXT_SNAPSHOT
  │    │    └─ commit "chore: advance develop to ${NEXT_SNAPSHOT}"
  │    │
  │    ├─ pom.xml absent OR hotfix
  │    │    └─ commit "release: merge v${VERSION} back into develop"
  │    │
  │    ├─ git push -u origin $BACKMERGE_BRANCH
  │    ├─ gh pr create --base develop
  │    └─ state → BACKMERGE_OPENED
  │
  ├─ exit 1 (conflict)
  │    │
  │    ├─ Capture: git diff --name-only --diff-filter=U
  │    ├─ git merge --abort
  │    ├─ git push -u origin refs/remotes/origin/main:refs/heads/$BACKMERGE_BRANCH
  │    ├─ gh pr create --base develop (with conflict body)
  │    └─ state → BACKMERGE_CONFLICT + conflictFiles
  │
  └─ other exit code → ABORT BACKMERGE_UNEXPECTED
```

## Clean Merge Flow

When `git merge --no-commit --no-ff origin/main` exits with code 0, the
merge is clean and can proceed automatically.

### Java SNAPSHOT Advance

For Java/Maven projects (detected by presence of `pom.xml`) in non-hotfix
mode, the SNAPSHOT version is advanced to the next minor:

- **Input:** version `2.3.0`, MAJOR=2, MINOR=3
- **Output:** `2.4.0-SNAPSHOT` written to pom.xml via `sed`
- **Commit:** `chore: advance develop to 2.4.0-SNAPSHOT`

The sed command updates only the project version (not parent or dependency
versions) by excluding lines within `<parent>...</parent>` blocks.

### Hotfix and Non-Java Projects

For hotfix releases (`--hotfix` flag) or non-Java projects (no `pom.xml`),
the SNAPSHOT advance is skipped. A simple merge commit is created instead:

- **Commit:** `release: merge v${VERSION} back into develop`

### PR Creation

The backmerge branch is pushed and a PR is opened:

```bash
gh pr create \
  --base develop \
  --head "$BACKMERGE_BRANCH" \
  --title "chore(release): back-merge v${VERSION} to develop" \
  --body "Automated back-merge from main after v${VERSION} release. Clean merge."
```

## Conflict Flow

When `git merge --no-commit --no-ff origin/main` exits with code 1, a
conflict was detected. The skill captures the conflicting files, aborts
the merge, and opens a PR for human resolution.

### Why Git Cannot Commit with Unmerged Paths

Git refuses to create a commit when there are unmerged paths in the index.
Even `--no-verify` cannot override this — it only skips pre-commit hooks,
not the index validation. The strategy is:

1. Capture the list of conflicting files via `git diff --name-only --diff-filter=U`
2. Abort the merge to return to a clean working tree
3. Fetch latest main (`git fetch origin main`), then push the remote-tracking
   ref `refs/remotes/origin/main` to the backmerge branch — this avoids
   publishing a stale local `main`
4. Open a PR where GitHub shows the conflict diff for inline resolution

### Conflict PR Body

The PR body includes:

- Warning banner: "CONFLICTS DETECTED during local dry-run merge"
- List of conflicting files in a code block
- Note that SNAPSHOT advance was NOT applied
- Instructions to resolve conflicts in the PR

### State File

The state file is updated with:

- `phase: BACKMERGE_CONFLICT`
- `conflictFiles`: JSON array of conflicting file paths
- `backmergePrUrl` and `backmergePrNumber`: PR metadata

## State Transitions

| Before | After (clean) | After (conflict) |
|:---|:---|:---|
| `TAGGED` | `BACKMERGE_OPENED` | `BACKMERGE_CONFLICT` |

## Error Codes

| Code | Condition | Resolution |
|:---|:---|:---|
| `BACKMERGE_WRONG_PHASE` | Phase is not `TAGGED` | Complete tagging first |
| `BACKMERGE_UNEXPECTED` | Unknown `git merge` exit code | Inspect git state manually |

## Completion

The `COMPLETED` phase is reached only after the back-merge PR has been
merged manually by a human reviewer. The skill terminates at
`BACKMERGE_OPENED` or `BACKMERGE_CONFLICT`. Transition to `COMPLETED`
and final cleanup (delete release branch, archive state file) is handled
by Step 12 (CLEANUP).
