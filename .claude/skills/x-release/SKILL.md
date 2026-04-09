---
name: x-release
description: "Orchestrates complete release flow using Git Flow release branches: version bump (auto-detect or explicit), release branch creation from develop, version file updates, changelog generation, release commit, dual merge (main + develop), git tag on main, and cleanup. Supports hotfix releases from main and dry-run mode."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, Agent
argument-hint: "[major|minor|patch|version] [--dry-run] [--skip-tests] [--no-publish] [--hotfix]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Release Orchestrator

## Purpose

Orchestrates the end-to-end release process for {{PROJECT_NAME}} using Git Flow release branches. Automates version bumps, release branch creation from `develop`, changelog generation, dual merge (to `main` and back to `develop`), tagging on `main`, and branch cleanup. Supports hotfix releases from `main` and dry-run mode for safe previewing.

## Triggers

- `/x-release` — auto-detect version bump from Conventional Commits since last tag
- `/x-release major` — bump major version (breaking changes)
- `/x-release minor` — bump minor version (new features)
- `/x-release patch` — bump patch version (bug fixes)
- `/x-release 2.1.0` — set explicit version
- `/x-release minor --dry-run` — preview release plan without executing
- `/x-release patch --skip-tests` — skip test validation
- `/x-release minor --no-publish` — create release locally without pushing
- `/x-release patch --hotfix` — create hotfix release from `main`

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| Bump type or version | No | `major`, `minor`, `patch`, or explicit `X.Y.Z`. Auto-detect if omitted. |
| `--dry-run` | No | Preview release plan without executing any changes |
| `--skip-tests` | No | Skip test validation (displays warning) |
| `--no-publish` | No | Create release locally without pushing to remote |
| `--hotfix` | No | Create hotfix release from `main` instead of `develop` |

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

### Step 1 — Determine Version

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

When no argument is provided, analyze commits since the last tag:

```bash
# Get commits since last tag
git log $(git describe --tags --abbrev=0 2>/dev/null)..HEAD \
    --format="%s%n%b" --no-merges
```

| Commit Pattern | Version Bump |
|---------------|-------------|
| `BREAKING CHANGE:` in body or `!` after type | **major** |
| `feat:` or `feat(scope):` | **minor** |
| `fix:`, `refactor:`, `perf:`, `docs:`, etc. | **patch** |

Decision order: if any commit triggers major, use major. Else if any triggers minor, use minor. Otherwise patch.

This follows Semantic Versioning (https://semver.org/spec/v2.0.0.html) rules.

### Step 2 — Validate Pre-conditions

Before making any changes, validate:

```bash
# 1. No uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
  echo "ABORT: uncommitted changes in working directory"
  exit 1
fi

# 2. Current branch MUST be develop or release/* (NOT main)
BRANCH=$(git branch --show-current)
if [[ "$BRANCH" != "develop" && ! "$BRANCH" =~ ^release/ ]]; then
  echo "WARNING: not on develop/release branch"
  echo "Release workflow requires starting from 'develop'."
  echo "Switch to develop before running /x-release."
  # For hotfix mode, main is allowed (see Hotfix section)
fi

# 3. Run tests (unless --skip-tests)
# Use project's build command to run tests
{{BUILD_COMMAND}}
```

If `--skip-tests` flag is present:
- Skip test execution
- Display warning: "WARNING: Skipping test validation"
- Continue with release

**Branch validation rules:**
- Standard release: MUST start from `develop` or an existing `release/*` branch
- Hotfix release (`--hotfix`): MUST start from `main`
- Starting from any other branch (e.g., `feature/*`) is a WARNING

### Step 3 — Branch Creation

Create a release branch from the appropriate source:

**Standard release (from develop):**

```bash
# Ensure develop is up to date
git checkout develop
git pull origin develop

# Create release branch
git checkout -b "release/${VERSION}"
```

**Hotfix release (from main):**

```bash
# Ensure main is up to date
git checkout main
git pull origin main

# Create hotfix branch
git checkout -b "hotfix/${DESCRIPTION}"
```

### Step 4 — Update Version Files

Update version in the appropriate project files based on language/build tool.

**SNAPSHOT handling:**
- On the release branch, **strip** the `-SNAPSHOT` suffix (e.g., `1.2.0-SNAPSHOT` becomes `1.2.0`)
- After release, on `develop`, **advance** to the next `-SNAPSHOT` version (e.g., `1.3.0-SNAPSHOT`)

| Language | File | Pattern | SNAPSHOT |
|----------|------|---------|----------|
| Java (Maven) | `pom.xml` | `<version>X.Y.Z</version>` | Strip `-SNAPSHOT` on release, add on develop |
| Java (Gradle) | `build.gradle` | `version = 'X.Y.Z'` | Strip `-SNAPSHOT` on release, add on develop |
| TypeScript/JavaScript | `package.json` | `"version": "X.Y.Z"` | N/A |
| Python | `pyproject.toml` | `version = "X.Y.Z"` | N/A |
| Rust | `Cargo.toml` | `version = "X.Y.Z"` | N/A |
| Go | _(no version file)_ | Git tags only | N/A |

**Detection strategy:**

```bash
# Detect project type and update version
if [ -f "pom.xml" ]; then
  # Maven: update <version> in pom.xml
  # Only update the project version, not dependency versions
  # Strip -SNAPSHOT suffix for release
  sed -i '' '/<parent>/,/<\/parent>/!{
    0,/<version>.*<\/version>/s/<version>.*<\/version>/<version>'"$VERSION"'<\/version>/
  }' pom.xml

elif [ -f "build.gradle" ]; then
  # Gradle: update version property (strip -SNAPSHOT)
  sed -i '' "s/version = '.*'/version = '$VERSION'/" build.gradle

elif [ -f "package.json" ]; then
  # npm/yarn: use npm version (no git tag)
  npm version "$VERSION" --no-git-tag-version

elif [ -f "pyproject.toml" ]; then
  # Python: update version in pyproject.toml
  sed -i '' "s/version = \".*\"/version = \"$VERSION\"/" pyproject.toml

elif [ -f "Cargo.toml" ]; then
  # Rust: update version in Cargo.toml
  sed -i '' "s/^version = \".*\"/version = \"$VERSION\"/" Cargo.toml

elif [ -f "go.mod" ]; then
  # Go: no version file to update, tags only
  echo "Go project: version managed via git tags only"
fi
```

### Step 5 — Changelog Generation

Delegate changelog generation to the `x-changelog` skill via the Skill tool:

```
Skill(skill: "x-changelog", args: "{version}")
```

The skill handles: parsing Conventional Commits since last tag, grouping by type (Added, Changed, Fixed, etc.), and generating/updating CHANGELOG.md.
Do NOT manually perform these steps. Let the skill handle all changelog generation.

If `/x-changelog` is unavailable via the Skill tool, fall back to the Agent tool:
```
Agent(prompt: "/x-changelog {version}")
```

Reference: `skills/x-changelog/SKILL.md`

### Step 6 — Commit Release

Create a release commit on the release branch following Conventional Commits format:

```bash
# Stage all version-related changes
git add -A

# Create release commit on release branch
git commit -m "release: v${VERSION}"
```

The commit message format is `release: v{version}` — this follows `x-git-push` Conventional Commits patterns.

### Step 7 — Merge to Main

Merge the release branch into `main` using a merge commit (no fast-forward):

```bash
# Switch to main
git checkout main
git pull origin main

# Merge release branch with merge commit
git merge "release/${VERSION}" --no-ff \
    -m "release: merge release/${VERSION} into main"
```

**Important:** The `--no-ff` flag ensures a merge commit is always created, preserving the release branch history in the commit graph.

### Step 8 — Tag Creation

Create an annotated git tag on `main` (after the merge):

```bash
# Create annotated tag on main
git tag -a "v${VERSION}" -m "Release v${VERSION}

Changes in this release:
$(git log $(git describe --tags --abbrev=0 HEAD~1 2>/dev/null)..HEAD~1 \
    --format='- %s' --no-merges)"
```

### Step 9 — Merge Back to Develop

Merge the release branch back into `develop` to propagate any release fixes:

```bash
# Switch to develop
git checkout develop
git pull origin develop

# Merge release branch back into develop
git merge "release/${VERSION}" --no-ff \
    -m "release: merge release/${VERSION} back into develop"
```

**SNAPSHOT version advance (Java/Gradle only):**

After merging back to `develop`, advance the version to the next SNAPSHOT:

```bash
# Example: if releasing 1.2.0, develop becomes 1.3.0-SNAPSHOT
NEXT_MINOR=$((MINOR + 1))
NEXT_SNAPSHOT="${MAJOR}.${NEXT_MINOR}.0-SNAPSHOT"

# Update pom.xml or build.gradle with SNAPSHOT version
# Commit the SNAPSHOT advance
git add -A
git commit -m "chore: advance develop to ${NEXT_SNAPSHOT}"
```

### Step 10 — Publish

If `--no-publish` flag is NOT present, push all branches and the tag:

```bash
# Push main with the merge commit and tag
git push origin main
git push origin "v${VERSION}"

# Push develop with the back-merge
git push origin develop
```

If `--no-publish` flag IS present:
- Skip push
- Display: "Release created locally. Run 'git push origin main develop v{VERSION}' manually when ready."

### Step 11 — Cleanup

Delete the release branch after successful merge:

```bash
# Delete local release branch
git branch -d "release/${VERSION}"

# Delete remote release branch (if pushed)
git push origin --delete "release/${VERSION}" 2>/dev/null || true
```

### Dry-Run Mode

If `--dry-run` flag is present, show the complete release plan without executing any changes:

```
=== RELEASE PLAN (DRY-RUN) ===

Current version:  1.2.3
Target version:   1.3.0
Bump type:        minor
Source branch:    develop

Steps:
  1. Create branch:    release/1.3.0 (from develop)
  2. Update version:   pom.xml (1.2.3-SNAPSHOT -> 1.3.0)
  3. Generate:         CHANGELOG.md
  4. Commit:           release: v1.3.0
  5. Merge to main:    git merge release/1.3.0 --no-ff
  6. Tag:              v1.3.0 (on main)
  7. Merge to develop: git merge release/1.3.0 --no-ff
  8. Advance develop:  1.4.0-SNAPSHOT
  9. Push:             main, develop, v1.3.0
 10. Cleanup:          delete release/1.3.0

=== NO CHANGES MADE ===
```

**Important:** In dry-run mode, all steps are only simulated. No files are modified, no commits are created, no tags are made, no branches are created.

## Hotfix Release

Hotfix releases follow a similar workflow but branch from `main` instead of `develop`:

### Hotfix Workflow

```
1. VALIDATE     -> Must be on main
2. BRANCH       -> git checkout -b hotfix/description from main
3. FIX          -> Apply minimal fix (PATCH version bump only)
4. UPDATE       -> Update version files (PATCH increment only)
5. CHANGELOG    -> Generate changelog
6. COMMIT       -> Commit fix on hotfix branch
7. MERGE-MAIN   -> git checkout main && git merge hotfix/description --no-ff
8. TAG          -> git tag -a vX.Y.Z on main
9. MERGE-BACK   -> git checkout develop && git merge hotfix/description --no-ff
                   (or merge into active release/* branch if one exists)
10. PUBLISH     -> Push main, develop (or release/*), and tag
11. CLEANUP     -> git branch -d hotfix/description
```

### Hotfix Rules

- **Origin branch:** Always `main` (never `develop`)
- **Version bump:** PATCH only (e.g., `1.2.0` -> `1.2.1`)
- **Merge targets:** `main` AND `develop` (or active `release/*` if one exists)
- **Tag location:** `main` (after merge)
- MAJOR or MINOR bumps are forbidden in hotfix releases

### Hotfix Merge Target Selection

```bash
# Check if an active release branch exists
RELEASE_BRANCH=$(git branch --list 'release/*' | head -1 | tr -d ' ')

if [ -n "$RELEASE_BRANCH" ]; then
  # Merge into active release branch instead of develop
  git checkout "$RELEASE_BRANCH"
  git merge "hotfix/${DESCRIPTION}" --no-ff
else
  # Merge into develop
  git checkout develop
  git merge "hotfix/${DESCRIPTION}" --no-ff
fi
```

## Pre-Release Validation Checklist

Before executing the release, validate against the release-checklist template (`_TEMPLATE-RELEASE-CHECKLIST.md`):

- [ ] All tests passing (or --skip-tests acknowledged)
- [ ] Coverage meets threshold
- [ ] No uncommitted changes
- [ ] On correct branch (develop for release, main for hotfix)
- [ ] Version follows SemVer
- [ ] CHANGELOG.md will be updated
- [ ] No breaking changes without major bump

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
| Push fails | Report error, release is local only |
| Merge conflict (main) | ABORT, resolve manually |
| Merge conflict (develop) | Warning, resolve and continue |
| Active release branch exists (hotfix) | Merge hotfix into release branch instead of develop |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-changelog` | calls | Delegates changelog generation via Agent tool |
| `x-git-push` | reads | Uses same Conventional Commits format for release commit |

- **release-management KP**: References SemVer rules, branching strategies, and registry patterns from `skills/release-management/SKILL.md`
- **Release Checklist**: Validates against `_TEMPLATE-RELEASE-CHECKLIST.md` for completeness
- **Rule 09 (Branching Model)**: Follows Git Flow branch types and merge direction rules
