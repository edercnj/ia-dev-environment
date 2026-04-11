---
name: x-release
description: "Orchestrates complete release flow using Git Flow release branches with approval gate, PR-flow (gh CLI) and deep validation: version bump (auto-detect or explicit), release branch creation from develop, deep validation (coverage, golden files, version consistency), version file updates, changelog generation, release commit, release PR via gh, human approval gate with persistent state file, tag on main after merged PR, back-merge PR to develop with conflict detection, and cleanup. Supports hotfix releases from main, dry-run mode, resume via --continue-after-merge, in-session pause via --interactive, GPG-signed tags, and custom state file path."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, Agent, Skill, AskUserQuestion
argument-hint: "[major|minor|patch|version] [--dry-run] [--skip-tests] [--no-publish] [--hotfix] [--continue-after-merge] [--interactive] [--signed-tag] [--state-file <path>]"
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
| `--continue-after-merge` | No | Resume flow from RESUME-AND-TAG after the release PR has been merged manually. Requires an existing state file with `phase: APPROVAL_PENDING`. |
| `--interactive` | No | Activate in-session pause at APPROVAL-GATE via `AskUserQuestion` instead of exiting the skill. The state file is still persisted for defense in depth. |
| `--signed-tag` | No | Create a GPG-signed git tag (`git tag -s`) instead of an annotated tag (`git tag -a`). |
| `--state-file <path>` | No | Override the state file path (default: `plans/release-state-<X.Y.Z>.json`). |

## Workflow

```
 0. RESUME-DETECT -> Verify gh/jq, load or create state file, detect resume mode
 1. DETERMINE     -> Parse argument and calculate target version
 2. VALIDATE      -> Check pre-conditions (branch, working directory)
 3. BRANCH        -> Create release/X.Y.Z branch from develop (or hotfix/* from main)
 4. UPDATE        -> Update version in project-specific files (strip SNAPSHOT)
 5. CHANGELOG     -> Generate/update CHANGELOG.md via x-release-changelog
 6. COMMIT        -> Create release commit on release branch
 7. MERGE-MAIN    -> Merge release branch into main with --no-ff
 8. TAG           -> Create annotated git tag on main
 9. MERGE-BACK    -> Merge release branch back into develop with --no-ff
10. PUBLISH       -> Push main, develop, and tag to remote (unless --no-publish)
11. CLEANUP       -> Delete the release branch
    DRY-RUN       -> If --dry-run, show plan and exit (no changes)
```

> **Note:** Step 0 (Resume Detection) is the new entry point introduced by
> EPIC-0035. It MUST execute before Step 1 on every invocation. Downstream
> stories (0002–0008) replace Steps 7–9 with PR-flow phases
> (`OPEN-RELEASE-PR`, `APPROVAL-GATE`, `RESUME-AND-TAG`,
> `BACK-MERGE-DEVELOP`). Steps 1, 3, 4, 5, 6 and 11 are preserved verbatim
> per RULE-002 (behaviour preservation).

### Step 0 — Resume Detection

This step is the mandatory entry point for every invocation of the skill.
It verifies external dependencies, decides whether the current run is a
fresh start or a resume, and either creates or loads the state file. The
full schema of the state file is documented in
`references/state-file-schema.md` — read it before changing any code in
this section.

#### Step 0.1 — Verify External Dependencies (RULE-008)

The extended workflow depends on `gh` CLI (>= 2.0) and `jq`. Abort
immediately with an actionable error code if either is missing or if
`gh` is not authenticated.

```bash
# 1. gh CLI must be installed
if ! command -v gh >/dev/null 2>&1; then
  echo "ABORT DEP_GH_MISSING: gh CLI not installed."
  echo "See https://cli.github.com/ for installation."
  exit 1
fi

# 2. jq must be installed (used to read/write the state file)
if ! command -v jq >/dev/null 2>&1; then
  echo "ABORT DEP_JQ_MISSING: jq not installed."
  echo "Install via your package manager (brew install jq, apt install jq)."
  exit 1
fi

# 3. gh must be authenticated
if ! gh auth status >/dev/null 2>&1; then
  echo "ABORT DEP_GH_AUTH: gh not authenticated."
  echo "Run 'gh auth login' before executing /x-release."
  exit 1
fi
```

Error codes emitted here match the catalog in
`references/state-file-schema.md` (section "Error Codes Emitted While
Reading the State File").

#### Step 0.2 — Resolve State File Path

```bash
# Default: plans/release-state-<X.Y.Z>.json
# Override: --state-file <path>
if [ -n "$STATE_FILE_OVERRIDE" ]; then
  STATE_FILE="$STATE_FILE_OVERRIDE"
else
  # If the version was passed explicitly, use it directly.
  # Otherwise defer to Step 1 to compute the version before re-entering
  # this step — but only for fresh starts (no resume).
  STATE_FILE="plans/release-state-${VERSION}.json"
fi
```

The state file path is resolved **once** and cached for the rest of the
invocation. Every phase that reads or writes state uses `$STATE_FILE`.

#### Step 0.3 — Branch on Invocation Mode

The decision tree below is the single source of truth for Step 0 outcomes.

```
if --continue-after-merge:
    if state file missing:
        ABORT RESUME_NO_STATE
    if jq . "$STATE_FILE" fails:
        ABORT STATE_INVALID_JSON
    if .schemaVersion != 1:
        ABORT STATE_SCHEMA_VERSION
    if .phase != "APPROVAL_PENDING":
        ABORT (invalid phase for resume — expected APPROVAL_PENDING)
    MODE = RESUME
    JUMP to Phase 9 (RESUME-AND-TAG, introduced by story-0035-0005)
else:
    if state file exists:
        if jq . "$STATE_FILE" fails:
            ABORT STATE_INVALID_JSON
        if .schemaVersion != 1:
            ABORT STATE_SCHEMA_VERSION
        if .phase != "COMPLETED":
            ABORT STATE_CONFLICT
        # else: a previous run completed cleanly — proceed to overwrite
    # Create fresh state file (atomic write)
    TMP="${STATE_FILE}.tmp.$$"
    jq -n \
      --arg version "$VERSION" \
      --arg branch  "$BRANCH" \
      --arg base    "$BASE_BRANCH" \
      --argjson hotfix     "$HOTFIX" \
      --argjson dryRun     "$DRY_RUN" \
      --argjson signedTag  "$SIGNED_TAG" \
      --argjson interactive "$INTERACTIVE" \
      --arg startedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
      '{
         schemaVersion: 1,
         version: $version,
         phase: "INITIALIZED",
         phasesCompleted: ["INITIALIZED"],
         branch: $branch,
         baseBranch: $base,
         hotfix: $hotfix,
         dryRun: $dryRun,
         signedTag: $signedTag,
         interactive: $interactive,
         startedAt: $startedAt,
         lastPhaseCompletedAt: $startedAt
       }' > "$TMP"
    mv "$TMP" "$STATE_FILE"
    MODE = START
    PROCEED to Step 1 (DETERMINE)
```

#### Step 0.4 — Error Catalog (summary)

| Condition | Code | Source |
|:---|:---|:---|
| `gh` missing | `DEP_GH_MISSING` | Step 0.1 |
| `jq` missing | `DEP_JQ_MISSING` | Step 0.1 |
| `gh` unauthenticated | `DEP_GH_AUTH` | Step 0.1 |
| State file invalid JSON | `STATE_INVALID_JSON` | Step 0.3 |
| Unknown `schemaVersion` | `STATE_SCHEMA_VERSION` | Step 0.3 |
| `--continue-after-merge` with no state | `RESUME_NO_STATE` | Step 0.3 |
| State exists, `phase != COMPLETED` | `STATE_CONFLICT` | Step 0.3 |

The full human-readable messages live in
`references/state-file-schema.md`. All subsequent phases reuse the same
error-code vocabulary so that operators can triage failures consistently.

#### Step 0.5 — Fresh Start vs. Resume Summary

| Scenario | State file before | `MODE` | Next step |
|:---|:---|:---|:---|
| Fresh invocation, no state | absent | `START` | Step 1 |
| Fresh invocation, completed state | `phase: COMPLETED` | `START` | Step 1 (state overwritten) |
| Fresh invocation, in-flight state | `phase != COMPLETED` | ABORT | `STATE_CONFLICT` |
| `--continue-after-merge`, valid | `phase: APPROVAL_PENDING` | `RESUME` | Phase 9 (`RESUME-AND-TAG`) |
| `--continue-after-merge`, missing | absent | ABORT | `RESUME_NO_STATE` |
| `--continue-after-merge`, wrong phase | any other `phase` | ABORT | invalid phase error |

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

Delegate changelog generation to the `x-release-changelog` skill:

```
Use Agent tool to invoke x-release-changelog with the target version.
The x-release-changelog skill will:
1. Parse Conventional Commits since last tag
2. Group by type (Added, Changed, Fixed, etc.)
3. Generate or update CHANGELOG.md
```

Reference: `skills/x-release-changelog/SKILL.md`

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
| `x-release-changelog` | calls | Delegates changelog generation via Agent tool |
| `x-git-push` | reads | Uses same Conventional Commits format for release commit |

- **release-management KP**: References SemVer rules, branching strategies, and registry patterns from `skills/release-management/SKILL.md`
- **Release Checklist**: Validates against `_TEMPLATE-RELEASE-CHECKLIST.md` for completeness
- **Rule 09 (Branching Model)**: Follows Git Flow branch types and merge direction rules
