---
name: x-release
description: >
  Orchestrates complete release flow: version bump (auto-detect or explicit),
  pre-condition validation, version file updates, changelog generation,
  release commit, git tag, and optional publish. Supports dry-run mode.
  Reference: `.github/skills/x-release/SKILL.md`
---

# Skill: Release Orchestrator

## Purpose

Orchestrates the end-to-end release process for {{PROJECT_NAME}}, automating version bumps, changelog generation, commit creation, tagging, and publishing following Semantic Versioning and Conventional Commits standards.

## Triggers

- `/x-release` -- auto-detect version bump from Conventional Commits since last tag
- `/x-release major` -- bump major version (breaking changes)
- `/x-release minor` -- bump minor version (new features)
- `/x-release patch` -- bump patch version (bug fixes)
- `/x-release 2.1.0` -- set explicit version
- `/x-release minor --dry-run` -- preview release plan without executing
- `/x-release patch --skip-tests` -- skip test validation
- `/x-release minor --no-publish` -- create release locally without pushing

## Workflow

```
1. DETERMINE   -> Parse argument and calculate target version
2. VALIDATE    -> Check pre-conditions (tests, branch, working directory)
3. UPDATE      -> Update version in project-specific files
4. CHANGELOG   -> Generate/update CHANGELOG.md via x-changelog
5. COMMIT      -> Create release commit (Conventional Commits format)
6. TAG         -> Create annotated git tag
7. DRY-RUN     -> If --dry-run, show plan and exit (no changes)
8. PUBLISH     -> Push commit and tag to remote (unless --no-publish)
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
- Current branch is main/master or release branch
- All tests pass (unless `--skip-tests`)
- Coverage meets threshold

### Step 3 -- UPDATE Version Files

| Language | File | Pattern |
|----------|------|---------|
| Java (Maven) | `pom.xml` | `<version>X.Y.Z</version>` |
| Java (Gradle) | `build.gradle` | `version = 'X.Y.Z'` |
| TypeScript/JavaScript | `package.json` | `"version": "X.Y.Z"` |
| Python | `pyproject.toml` | `version = "X.Y.Z"` |
| Rust | `Cargo.toml` | `version = "X.Y.Z"` |
| Go | _(no version file)_ | Git tags only |

### Step 4 -- CHANGELOG Generation

Delegate changelog generation to the `x-changelog` skill to parse Conventional Commits and update CHANGELOG.md.

### Step 5 -- COMMIT Release

Create release commit: `release: v{version}` (Conventional Commits format, following x-git-push patterns).

### Step 6 -- TAG Creation

Create annotated git tag: `git tag -a v{version} -m "Release v{version}"`

### Step 7 -- DRY-RUN Mode

If `--dry-run` flag is present, show complete release plan (version, files to update, changelog preview, commit message, tag) without executing any changes.

### Step 8 -- PUBLISH

Push commit and tag to remote (unless `--no-publish`).

## Integration Notes

- **x-changelog**: Delegates changelog generation
- **x-git-push**: Uses same Conventional Commits format for release commit
- **release-management KP**: References SemVer rules and branching strategies
- **Release Checklist**: Validates against release-checklist template

## Error Handling

| Scenario | Action |
|----------|--------|
| No tags found | Assume version 0.0.0, first release |
| Uncommitted changes | ABORT with error message |
| Tests fail (no --skip-tests) | ABORT with test output |
| Invalid version format | ABORT with SemVer format hint |
| No Conventional Commits found | Default to patch bump |
| Already tagged version | ABORT with "version already released" |
