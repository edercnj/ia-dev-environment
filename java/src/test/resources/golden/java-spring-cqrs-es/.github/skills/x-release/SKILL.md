---
name: x-release
description: >
  Orchestrates complete release flow using Git Flow release branches: version
  bump (auto-detect or explicit), release branch creation from develop, version
  file updates, changelog generation, release commit, dual merge (main +
  develop), git tag on main, and cleanup. Supports hotfix releases and dry-run.
  Reference: `.github/skills/x-release/SKILL.md`
---

# Skill: Release Orchestrator

## Purpose

Orchestrates the end-to-end release process for {{PROJECT_NAME}} using Git Flow release branches. Automates version bumps, release branch creation from `develop`, changelog generation, dual merge (to `main` and back to `develop`), tagging on `main`, and branch cleanup. Supports hotfix releases from `main` and dry-run mode for safe previewing.

## Triggers

- `/x-release` -- auto-detect version bump from Conventional Commits since last tag
- `/x-release major` -- bump major version (breaking changes)
- `/x-release minor` -- bump minor version (new features)
- `/x-release patch` -- bump patch version (bug fixes)
- `/x-release 2.1.0` -- set explicit version
- `/x-release minor --dry-run` -- preview release plan without executing
- `/x-release patch --skip-tests` -- skip test validation
- `/x-release minor --no-publish` -- create release locally without pushing
- `/x-release patch --hotfix` -- create hotfix release from `main`

## Workflow

```
 1. DETERMINE    -> Parse argument and calculate target version
 2. VALIDATE     -> Check pre-conditions (branch, working directory)
 3. BRANCH       -> Create release/X.Y.Z branch from develop (or hotfix/* from main)
 4. UPDATE       -> Update version in project-specific files (strip SNAPSHOT)
 5. CHANGELOG    -> Generate/update CHANGELOG.md via x-changelog
 6. COMMIT       -> Create release commit on release branch
 7. MERGE-MAIN   -> Merge release branch into main with --no-ff
 8. TAG          -> Create annotated git tag on main
 9. MERGE-BACK   -> Merge release branch back into develop with --no-ff
10. PUBLISH      -> Push main, develop, and tag to remote (unless --no-publish)
11. CLEANUP      -> Delete the release branch
    DRY-RUN      -> If --dry-run, show plan and exit (no changes)
```

### Step 1 -- DETERMINE Version

Parse the argument to determine the target version:

**Option A: Increment type** (`major`, `minor`, `patch`)

Calculate next version from current tag using Semantic Versioning rules.

**Option B: Explicit version** (e.g., `2.1.0`)

Use the provided version directly after validating SemVer format.

**Option C: Auto-detect from Conventional Commits**

When no argument is provided, analyze commits since the last tag:

| Commit Pattern | Version Bump |
|---------------|-------------|
| `BREAKING CHANGE:` in body or `!` after type | **major** |
| `feat:` or `feat(scope):` | **minor** |
| `fix:`, `refactor:`, `perf:`, `docs:`, etc. | **patch** |

This follows Semantic Versioning (https://semver.org/spec/v2.0.0.html) rules.

### Step 2 -- VALIDATE Pre-conditions

- No uncommitted changes in working directory
- Current branch MUST be `develop` or `release/*` (NOT `main`)
- For hotfix: current branch MUST be `main`
- All tests pass (unless `--skip-tests`)
- Coverage meets threshold

### Step 3 -- BRANCH Creation

**Standard release:** `git checkout -b release/X.Y.Z` from `develop`

**Hotfix release:** `git checkout -b hotfix/description` from `main`

### Step 4 -- UPDATE Version Files

Update version and strip SNAPSHOT suffix on release branch:

| Language | File | Pattern | SNAPSHOT |
|----------|------|---------|----------|
| Java (Maven) | `pom.xml` | `<version>X.Y.Z</version>` | Strip on release, add on develop |
| Java (Gradle) | `build.gradle` | `version = 'X.Y.Z'` | Strip on release, add on develop |
| TypeScript/JavaScript | `package.json` | `"version": "X.Y.Z"` | N/A |
| Python | `pyproject.toml` | `version = "X.Y.Z"` | N/A |
| Rust | `Cargo.toml` | `version = "X.Y.Z"` | N/A |
| Go | _(no version file)_ | Git tags only | N/A |

### Step 5 -- CHANGELOG Generation

Delegate changelog generation to the `x-changelog` skill to parse Conventional Commits and update CHANGELOG.md.

### Step 6 -- COMMIT Release

Create release commit on release branch: `release: v{version}` (Conventional Commits format, following x-git-push patterns).

### Step 7 -- MERGE TO MAIN

Merge release branch into `main` with merge commit: `git merge release/X.Y.Z --no-ff`

### Step 8 -- TAG Creation

Create annotated git tag on `main` after merge: `git tag -a v{version} -m "Release v{version}"`

### Step 9 -- MERGE BACK TO DEVELOP

Merge release branch back into `develop`: `git merge release/X.Y.Z --no-ff`

After merge, advance `develop` to next SNAPSHOT version (Java/Gradle only).

### Step 10 -- PUBLISH

Push `main`, `develop`, and tag to remote (unless `--no-publish`).

### Step 11 -- CLEANUP

Delete the release branch: `git branch -d release/X.Y.Z`

### DRY-RUN Mode

If `--dry-run` flag is present, show complete release plan (branch creation, version update, changelog preview, dual merge, tag, cleanup) without executing any changes.

## Hotfix Release

- **Origin branch:** Always `main` (never `develop`)
- **Version bump:** PATCH only
- **Merge targets:** `main` AND `develop` (or active `release/*` if one exists)
- **Tag location:** `main` (after merge)

## Integration Notes

- **x-changelog**: Delegates changelog generation
- **x-git-push**: Uses same Conventional Commits format for release commit
- **release-management KP**: References SemVer rules and branching strategies
- **Release Checklist**: Validates against release-checklist template
- **Rule 09 (Branching Model)**: Follows Git Flow branch types and merge direction rules

## Error Handling

| Scenario | Action |
|----------|--------|
| No tags found | Assume version 0.0.0, first release |
| Uncommitted changes | ABORT with error message |
| Tests fail (no --skip-tests) | ABORT with test output |
| Invalid version format | ABORT with SemVer format hint |
| No Conventional Commits found | Default to patch bump |
| Already tagged version | ABORT with "version already released" |
| Not on develop (standard) | Warning, suggest switching to develop |
| Not on main (hotfix) | ABORT, hotfix must start from main |
| Merge conflict | ABORT, resolve manually |
