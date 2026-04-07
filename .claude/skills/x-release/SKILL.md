---
name: x-release
description: "Orchestrates complete release flow: version bump (auto-detect or explicit), pre-condition validation, version file updates, changelog generation, release commit, git tag, and optional publish. Supports dry-run mode for safe previewing."
user-invocable: true
argument-hint: "[major|minor|patch|version] [--dry-run] [--skip-tests] [--no-publish]"
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, Agent
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

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

```bash
# Get current version from latest tag
CURRENT=$(git describe --tags --abbrev=0 2>/dev/null | sed 's/^v//')

# If no tag exists, default to 0.0.0
if [ -z "$CURRENT" ]; then
  CURRENT="0.0.0"
fi

# Split into components
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"

# Calculate next version based on type
# major: MAJOR+1.0.0
# minor: MAJOR.MINOR+1.0
# patch: MAJOR.MINOR.PATCH+1
```

**Option B: Explicit version** (e.g., `2.1.0`)

Use the provided version directly after validating it follows Semantic Versioning format (`X.Y.Z`).

**Option C: Auto-detect from Conventional Commits**

When no argument is provided, delegate commit analysis to `x-lib-version-bump`:

1. Determine commit range: last tag to HEAD (or all commits if no tag exists)
   ```bash
   LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)
   RANGE="${LAST_TAG:-$(git rev-list --max-parents=0 HEAD)}..HEAD"
   ```
2. Invoke `x-lib-version-bump` logic with the range and `--dry-run` to get bump type and next version
3. The bump decision table (from `x-lib-version-bump`):

| Commit Pattern | Version Bump |
|---------------|-------------|
| `BREAKING CHANGE:` in body or `!` after type | **major** |
| `feat:` or `feat(scope):` | **minor** |
| `fix:`, `refactor:`, `perf:` | **patch** |
| Only `test:`, `docs:`, `chore:`, `build:`, `ci:` | **patch** (minimum for release) |

Decision order: if any commit triggers major, use major. Else if any triggers minor, use minor. Otherwise patch.

This follows Semantic Versioning (https://semver.org/spec/v2.0.0.html) rules.

**SNAPSHOT handling (Maven projects):** When the current pom.xml version is `X.Y.Z-SNAPSHOT`,
strip the `-SNAPSHOT` suffix before applying the increment. The release version is `X.Y.Z`
(no SNAPSHOT). After release, a post-release commit prepares the next development version
(see Step 5.5).

### Step 2 -- VALIDATE Pre-conditions

Before making any changes, validate:

```bash
# 1. No uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
  echo "ABORT: uncommitted changes in working directory"
  exit 1
fi

# 2. Current branch is main/master or release branch
BRANCH=$(git branch --show-current)
if [[ "$BRANCH" != "main" && "$BRANCH" != "master" && \
      ! "$BRANCH" =~ ^release/ ]]; then
  echo "WARNING: not on main/master/release branch"
fi

# 3. Run tests (unless --skip-tests)
# Use project's build command to run tests
{{BUILD_COMMAND}}
```

If `--skip-tests` flag is present:
- Skip test execution
- Display warning: "WARNING: Skipping test validation"
- Continue with release

### Step 3 -- UPDATE Version Files

Delegate version file update to `x-lib-version-bump` logic.

The release version does NOT include `-SNAPSHOT` suffix. Pass `--snapshot false` to
`x-lib-version-bump` to ensure the release version is clean (e.g., `2.1.0`, not `2.1.0-SNAPSHOT`).

**Supported project types** (detected automatically by `x-lib-version-bump`):

| Language | File | Pattern |
|----------|------|---------|
| Java (Maven) | `pom.xml` | `<version>X.Y.Z</version>` |
| Java (Gradle) | `build.gradle` | `version = 'X.Y.Z'` |
| TypeScript/JavaScript | `package.json` | `"version": "X.Y.Z"` |
| Python | `pyproject.toml` | `version = "X.Y.Z"` |
| Rust | `Cargo.toml` | `version = "X.Y.Z"` |
| Go | _(no version file)_ | Git tags only |

Invoke `x-lib-version-bump` with `--no-commit --snapshot false` to update the file without
committing (the release commit is handled in Step 5). The `x-lib-version-bump` utility
handles project type detection and version file update logic (only updating the project
version, not dependency versions).

### Step 4 -- CHANGELOG Generation

Delegate changelog generation to the `x-changelog` skill:

```
Use Agent tool to invoke x-changelog with the target version.
The x-changelog skill will:
1. Parse Conventional Commits since last tag
2. Group by type (Added, Changed, Fixed, etc.)
3. Generate or update CHANGELOG.md
```

Reference: `skills/x-changelog/SKILL.md`

### Step 5 -- COMMIT Release

Create a release commit following Conventional Commits format:

```bash
# Stage all version-related changes
git add -A

# Create release commit
git commit -m "release: v${VERSION}"
```

The commit message format is `release: v{version}` — this follows `x-git-push` Conventional Commits patterns.

### Step 5.5 -- Prepare Next Development Version (Maven/SNAPSHOT projects)

For projects that use `-SNAPSHOT` convention (Maven), prepare the next development version
after the release commit:

1. Determine next development version: increment MINOR from release version, append `-SNAPSHOT`
   - Example: released `2.1.0` → next development version is `2.2.0-SNAPSHOT`
   - If released a PATCH (e.g., `2.1.1`) → next development version is `2.1.2-SNAPSHOT`
2. Update pom.xml with the next development version (using `x-lib-version-bump` logic with `--no-commit`)
3. Commit: `chore(version): prepare for next development iteration [X.Y.Z-SNAPSHOT]`

This follows standard Maven release practice (similar to `maven-release-plugin`).
The release tag points to the clean version (`v2.1.0`), and the next commit on `main`
immediately restores the SNAPSHOT convention for ongoing development.

> **Non-SNAPSHOT projects** (npm, Python, Rust, Go): This step is skipped. The version
> in the build file remains at the released version until the next release.

### Step 6 -- TAG Creation

Create an annotated git tag with release notes:

```bash
# Create annotated tag
git tag -a "v${VERSION}" -m "Release v${VERSION}

Changes in this release:
$(git log $(git describe --tags --abbrev=0 HEAD~1)..HEAD~1 \
    --format='- %s' --no-merges)"
```

### Step 7 -- DRY-RUN Mode

If `--dry-run` flag is present, show the complete release plan without executing any changes:

```
=== RELEASE PLAN (DRY-RUN) ===

Current version:  1.2.3
Target version:   1.3.0
Bump type:        minor

Files to update:
  - pom.xml (version: 1.2.3 -> 1.3.0)

Changelog preview:
  ## [1.3.0] - 2024-01-15
  ### Added
  - feat(api): add pagination support
  - feat(auth): add OAuth2 provider
  ### Fixed
  - fix(db): resolve connection pool leak

Commit message:
  release: v1.3.0

Tag:
  v1.3.0

=== NO CHANGES MADE ===
```

**Important:** In dry-run mode, Steps 2-6 are only simulated. No files are modified, no commits are created, no tags are made.

### Step 8 -- PUBLISH

If `--no-publish` flag is NOT present, push the release:

```bash
# Push commit and tag
git push origin $(git branch --show-current)
git push origin "v${VERSION}"
```

If `--no-publish` flag IS present:
- Skip push
- Display: "Release created locally. Run 'git push' manually when ready."

## Pre-Release Validation Checklist

Before executing the release, validate against the release-checklist template (`_TEMPLATE-RELEASE-CHECKLIST.md`):

- [ ] All tests passing (or --skip-tests acknowledged)
- [ ] Coverage meets threshold
- [ ] No uncommitted changes
- [ ] On correct branch (main/master/release)
- [ ] Version follows SemVer
- [ ] CHANGELOG.md will be updated
- [ ] No breaking changes without major bump

## Integration Notes

- **x-lib-version-bump**: Delegates commit analysis (Step 1 Option C) and version file update (Step 3) to the shared version bump utility
- **x-changelog**: Delegates changelog generation via Agent tool
- **x-git-push**: Uses same Conventional Commits format for release commit
- **release-management KP**: References SemVer rules, branching strategies, and registry patterns from `skills/release-management/SKILL.md`
- **Release Checklist**: Validates against `_TEMPLATE-RELEASE-CHECKLIST.md` for completeness
- **SNAPSHOT lifecycle**: Step 3 strips SNAPSHOT for release; Step 5.5 re-adds SNAPSHOT for next development version (Maven projects only)

## Error Handling

| Scenario | Action |
|----------|--------|
| No tags found | Assume version 0.0.0, first release |
| Uncommitted changes | ABORT with error message |
| Tests fail (no --skip-tests) | ABORT with test output |
| Invalid version format | ABORT with SemVer format hint |
| No Conventional Commits found | Default to patch bump |
| Already tagged version | ABORT with "version already released" |
| Not on main/master | Warning, proceed if on release branch |
| Push fails | Report error, release is local only |
