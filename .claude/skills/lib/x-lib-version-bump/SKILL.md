---
name: x-lib-version-bump
description: "Shared version bump utility: detects current version from build files, analyzes Conventional Commits for bump type, calculates next SemVer version, and updates version files. Supports SNAPSHOT convention for Java/Maven projects."
user-invocable: false
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[commit-range] [--dry-run]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.

# Skill: Version Bump Utility (Shared Library)

## Purpose

Shared utility that centralizes semantic version bump logic. Detects current version from build files, analyzes Conventional Commits in a git range to determine bump type (MAJOR/MINOR/PATCH/NONE), calculates the next version, updates the version file, and creates a version commit.

Consumed by:
- `x-dev-lifecycle` (Phase 6 — standalone mode)
- `x-dev-epic-implement` (Integrity Gate — post-phase version bump)
- `x-dev-implement` (Step 4 — optional local bump)
- `x-release` (Steps 1 and 3 — version detection and file update)

## When to Use

- **Never invoked directly by the user** (user-invocable: false)
- Called by other skills that need version bump logic
- Provides a DRY implementation of version analysis and file update

## Input Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `commit-range` | Yes | Git log range for commit analysis (e.g., `main..HEAD`, `v1.0.0..HEAD`, `abc123..def456`) |
| `--dry-run` | No | Output the bump plan without modifying files or creating commits |
| `--no-commit` | No | Update version file but do not create a git commit |
| `--snapshot` | No | Append `-SNAPSHOT` suffix to the new version (Java/Maven convention). Default: `true` for Maven projects |
| `--commit-suffix` | No | Optional suffix for the commit message (e.g., `[phase-0]`) |

## Procedure

### STEP 1 — Detect Project Type and Current Version

Detect the build system and extract the current version:

```bash
# Priority-ordered detection
if [ -f "pom.xml" ]; then
  PROJECT_TYPE="maven"
  # Extract version, stripping -SNAPSHOT if present
  CURRENT_RAW=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
  SNAPSHOT_SUFFIX=""
  if [[ "$CURRENT_RAW" == *-SNAPSHOT ]]; then
    SNAPSHOT_SUFFIX="-SNAPSHOT"
    CURRENT=$(echo "$CURRENT_RAW" | sed 's/-SNAPSHOT//')
  else
    CURRENT="$CURRENT_RAW"
  fi

elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
  PROJECT_TYPE="gradle"
  CURRENT=$(grep "^version" build.gradle* | head -1 | sed "s/.*['\"]\(.*\)['\"].*/\1/")

elif [ -f "package.json" ]; then
  PROJECT_TYPE="npm"
  CURRENT=$(grep '"version"' package.json | head -1 | sed 's/.*: *"\(.*\)".*/\1/')

elif [ -f "pyproject.toml" ]; then
  PROJECT_TYPE="python"
  CURRENT=$(grep '^version' pyproject.toml | head -1 | sed 's/.*= *"\(.*\)".*/\1/')

elif [ -f "Cargo.toml" ]; then
  PROJECT_TYPE="rust"
  CURRENT=$(grep '^version' Cargo.toml | head -1 | sed 's/.*= *"\(.*\)".*/\1/')

else
  echo "WARNING: No recognized build file found. Version bump skipped."
  exit 0
fi
```

**SNAPSHOT handling (Maven convention):**
- Development versions use `X.Y.Z-SNAPSHOT` (indicates "heading toward X.Y.Z")
- The base version for bump calculation is always the stripped version (without -SNAPSHOT)
- After bump, `-SNAPSHOT` is re-added for Maven projects (unless invoked by x-release for a release)

**Version parsing:**
```
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"
```

### STEP 2 — Analyze Commits for Bump Type

Scan commits in the provided range to determine the highest-priority bump:

```bash
# Get commit subjects and bodies
COMMITS=$(git log ${COMMIT_RANGE} --format="%s%n%b" --no-merges)
```

**Bump Decision Table (priority order — highest wins):**

| Priority | Pattern | Bump Type | Example Commit |
|----------|---------|-----------|----------------|
| 1 (highest) | `BREAKING CHANGE:` in body OR `!` after type (e.g., `feat!:`) | **MAJOR** | `feat!: redesign API authentication` |
| 2 | `feat:` or `feat(scope):` | **MINOR** | `feat(auth): add OAuth2 support` |
| 3 | `fix:`, `refactor:`, `perf:` | **PATCH** | `fix(db): resolve connection pool leak` |
| 4 (lowest) | Only `test:`, `docs:`, `chore:`, `build:`, `ci:`, `style:` | **NONE** | `test(auth): add unit tests` |

**Detection algorithm:**

```bash
BUMP_TYPE="NONE"

# Check for BREAKING CHANGE (highest priority)
if echo "$COMMITS" | grep -qE '(^[a-z]+(\([^)]*\))?!:|BREAKING CHANGE:)'; then
  BUMP_TYPE="MAJOR"
# Check for feat: (minor)
elif echo "$COMMITS" | grep -qE '^feat(\([^)]*\))?:'; then
  BUMP_TYPE="MINOR"
# Check for fix:/refactor:/perf: (patch)
elif echo "$COMMITS" | grep -qE '^(fix|refactor|perf)(\([^)]*\))?:'; then
  BUMP_TYPE="PATCH"
fi
```

**Edge cases:**
- Empty commit range (no commits): `BUMP_TYPE = "NONE"`
- Merge commits excluded (`--no-merges`)
- `chore(version):` commits from previous bumps: excluded from analysis (they are `chore:` → NONE)

### STEP 3 — Calculate Next Version

Apply SemVer increment based on bump type:

```bash
case "$BUMP_TYPE" in
  MAJOR)
    NEXT_MAJOR=$((MAJOR + 1))
    NEXT_VERSION="${NEXT_MAJOR}.0.0"
    ;;
  MINOR)
    NEXT_MINOR=$((MINOR + 1))
    NEXT_VERSION="${MAJOR}.${NEXT_MINOR}.0"
    ;;
  PATCH)
    NEXT_PATCH=$((PATCH + 1))
    NEXT_VERSION="${MAJOR}.${MINOR}.${NEXT_PATCH}"
    ;;
  NONE)
    echo "No version-impacting changes detected. Skipping version bump."
    exit 0
    ;;
esac
```

**SNAPSHOT suffix:**
- Maven projects: append `-SNAPSHOT` by default → `NEXT_VERSION="${NEXT_VERSION}-SNAPSHOT"`
- Override with `--snapshot false` when called by x-release for a release commit (strips SNAPSHOT)
- Non-Maven projects: no suffix

### STEP 4 — Update Version File

Update the version in the appropriate build file:

```bash
case "$PROJECT_TYPE" in
  maven)
    # Update ONLY the project <version>, NOT parent or dependency versions
    # Strategy: skip <parent> block, then update the first <version> occurrence
    sed -i '' '/<parent>/,/<\/parent>/!{
      0,/<version>.*<\/version>/s/<version>.*<\/version>/<version>'"$NEXT_VERSION"'<\/version>/
    }' pom.xml
    ;;
  gradle)
    sed -i '' "s/version = ['\"].*['\"]/version = '${NEXT_VERSION}'/" build.gradle*
    ;;
  npm)
    npm version "$NEXT_VERSION" --no-git-tag-version
    ;;
  python)
    sed -i '' "s/version = \".*\"/version = \"${NEXT_VERSION}\"/" pyproject.toml
    ;;
  rust)
    sed -i '' "s/^version = \".*\"/version = \"${NEXT_VERSION}\"/" Cargo.toml
    ;;
esac
```

**Validation:** After update, re-read the file to confirm the version was changed correctly.

### STEP 5 — Create Version Commit (unless `--no-commit` or `--dry-run`)

```bash
# Stage only the version file
case "$PROJECT_TYPE" in
  maven) git add pom.xml ;;
  gradle) git add build.gradle* ;;
  npm) git add package.json package-lock.json ;;
  python) git add pyproject.toml ;;
  rust) git add Cargo.toml Cargo.lock ;;
esac

# Commit with optional suffix
COMMIT_MSG="chore(version): bump to ${NEXT_VERSION}"
if [ -n "$COMMIT_SUFFIX" ]; then
  COMMIT_MSG="${COMMIT_MSG} ${COMMIT_SUFFIX}"
fi

git commit -m "$COMMIT_MSG"
```

### STEP 6 — Output Result

Return structured result for the calling skill:

```
VERSION BUMP RESULT
===================
Project type:     {PROJECT_TYPE}
Previous version: {CURRENT_RAW}
New version:      {NEXT_VERSION}
Bump type:        {BUMP_TYPE}
Commit SHA:       {SHA} (or "none" if --no-commit/--dry-run)
Commits analyzed: {count}
Contributing types: feat: {N}, fix: {N}, refactor: {N}, ...
```

## Dry-Run Output

When `--dry-run` is set, no files are modified and no commits are created:

```
VERSION BUMP PLAN (DRY-RUN)
============================
Project type:     maven
Current version:  2.0.0-SNAPSHOT (base: 2.0.0)
Bump type:        MINOR
Next version:     2.1.0-SNAPSHOT
File to update:   pom.xml
Commit message:   chore(version): bump to 2.1.0-SNAPSHOT

Commits in range (main..HEAD):
  - feat(auth): add JWT token validation
  - test(auth): add unit tests for JWT
  - fix(db): handle null values in mapper

Highest priority: feat: → MINOR

=== NO CHANGES MADE ===
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No build file found | Log warning, exit 0 (no-op) |
| Invalid version format in file | ABORT with error: "Cannot parse version: {raw}" |
| Empty commit range | BUMP_TYPE = NONE, exit 0 |
| Git not available | ABORT with error |
| sed update fails | ABORT with error, do not commit |
| pom.xml has no `<version>` | ABORT with error |

## Integration Notes

- **x-dev-lifecycle Phase 6**: Calls with `commit-range=main..HEAD`, `--snapshot` (default true for Maven)
- **x-dev-epic-implement Integrity Gate**: Calls with `commit-range=mainShaBeforePhase[N]..main`, `--commit-suffix=[phase-N]`
- **x-dev-implement Step 4**: Calls with `commit-range=main..HEAD`, `--snapshot`
- **x-release Step 1**: Calls with `--dry-run` to determine bump type and next version
- **x-release Step 3**: Calls with `--snapshot false` to set release version (no SNAPSHOT suffix)
- Commit format `chore(version):` follows Conventional Commits (x-git-push) and maps to NONE bump (avoids recursive bumping)
