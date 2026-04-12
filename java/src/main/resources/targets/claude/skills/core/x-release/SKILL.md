---
name: x-release
description: "Orchestrates complete release flow using Git Flow release branches with approval gate, PR-flow (gh CLI) and deep validation: version bump (auto-detect or explicit), release branch creation from develop, deep validation (coverage, golden files, version consistency), version file updates, changelog generation, release commit, release PR via gh (optionally reviewed by x-review-pr), human approval gate with persistent state file, tag on main after merged PR, back-merge PR to develop with conflict detection, and cleanup. Supports hotfix releases from main, dry-run mode, resume via --continue-after-merge, in-session pause via --interactive, GPG-signed tags, skip-review opt-out, and custom state file path."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, Agent, Skill, AskUserQuestion
argument-hint: "[major|minor|patch|version] [--dry-run] [--skip-tests] [--no-publish] [--hotfix] [--continue-after-merge] [--interactive] [--signed-tag] [--skip-review] [--state-file <path>]"
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
| `--skip-review` | No | Skip the fire-and-forget `x-review-pr` invocation at the end of Phase `OPEN-RELEASE-PR`. The release PR is still opened against `main`; only the automated specialist review is suppressed. |
| `--state-file <path>` | No | Override the state file path (default: `plans/release-state-<X.Y.Z>.json`). |

## Workflow

```
 0. RESUME-DETECT   -> Verify gh/jq, load or create state file, detect resume mode
 1. DETERMINE       -> Parse argument and calculate target version
 2. VALIDATE-DEEP   -> Deep validation (8+1 checks: workdir, branch, changelog, build, coverage, golden, hardcoded, version consistency)
 3. BRANCH          -> Create release/X.Y.Z branch from develop (or hotfix/* from main)
 4. UPDATE          -> Update version in project-specific files (strip SNAPSHOT)
 5. CHANGELOG       -> Generate/update CHANGELOG.md via x-release-changelog
 6. COMMIT          -> Create release commit on release branch
 7. OPEN-RELEASE-PR -> Push release branch and open PR to main via gh pr create
 8. TAG             -> Create annotated/signed git tag on main (after merged PR)
 9. MERGE-BACK      -> Merge release branch back into develop with --no-ff
10. PUBLISH         -> Push the release tag to remote (main/develop go via PR flow)
11. CLEANUP         -> Delete the release branch
    DRY-RUN         -> If --dry-run, show plan and exit (no changes)
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

### Step 2 — Phase VALIDATE-DEEP

Phase VALIDATE-DEEP replaces the former simple pre-condition check with 8
mandatory checks plus 1 conditional check. Each check has a unique error
code. Checks execute sequentially — the first failure aborts the release.

On success, the state file advances to `phase: VALIDATED`.

#### Check Execution Table

| # | Check | Logic | Abort If | Error Code | Skippable |
|---|-------|-------|----------|------------|-----------|
| 1 | Working dir clean | `git status --porcelain` | output non-empty | `VALIDATE_DIRTY_WORKDIR` | No (always-mandatory) |
| 2 | Correct branch | `git branch --show-current` | not develop/main(hotfix) | `VALIDATE_WRONG_BRANCH` | No (always-mandatory) |
| 3 | `[Unreleased]` non-empty | parse CHANGELOG.md | section empty | `VALIDATE_EMPTY_UNRELEASED` | No (always-mandatory) |
| 4 | Build + tests | `{{BUILD_COMMAND}}` | exit ≠ 0 | `VALIDATE_BUILD_FAILED` | Yes (`--skip-tests`) |
| 5a | Line coverage threshold | parse coverage report | line < `{{COVERAGE_LINE_THRESHOLD}}`% | `VALIDATE_COVERAGE_LINE` | Yes (`--skip-tests`) |
| 5b | Branch coverage threshold | parse coverage report | branch < `{{COVERAGE_BRANCH_THRESHOLD}}`% | `VALIDATE_COVERAGE_BRANCH` | Yes (`--skip-tests`) |
| 6 | Golden file consistency | `{{GOLDEN_TEST_COMMAND}}` | exit ≠ 0 | `VALIDATE_GOLDEN_DRIFT` | Yes (`--skip-tests`) |
| 7 | Hardcoded version strings | `grep -rn $CURRENT_VERSION` | matches outside allowed paths | `VALIDATE_HARDCODED_VERSION` | No (always-mandatory) |
| 8 | Cross-file version consistency | pom ↔ target ↔ branch | mismatch | `VALIDATE_VERSION_MISMATCH` | No (always-mandatory) |
| 9 | Generation dry-run (conditional) | `{{GENERATION_COMMAND}} --dry-run` | diff non-empty | `VALIDATE_GENERATION_DRIFT` | Conditional |

#### `--skip-tests` Behavior

When `--skip-tests` is passed, ONLY checks 4, 5, 6 are skipped. Checks 1,
2, 3, 7, 8 are always-mandatory because they do not depend on running
tests.

```
if --skip-tests:
    echo "WARNING: --skip-tests skips checks 4, 5, 6 (build, coverage, golden files)"
    # Checks 1, 2, 3, 7, 8 still execute
```

#### Check 1 — Working Directory Clean

```bash
if [ -n "$(git status --porcelain)" ]; then
  echo "ABORT VALIDATE_DIRTY_WORKDIR: Uncommitted changes in working directory"
  # List dirty files for diagnosis
  git status --short
  exit 1
fi
```

#### Check 2 — Correct Branch

```bash
BRANCH=$(git branch --show-current)
if [ "$HOTFIX" = true ]; then
  if [ "$BRANCH" != "main" ]; then
    echo "ABORT VALIDATE_WRONG_BRANCH: Not on main (required for hotfix). Current: $BRANCH"
    exit 1
  fi
else
  if [[ "$BRANCH" != "develop" && ! "$BRANCH" =~ ^release/ ]]; then
    echo "ABORT VALIDATE_WRONG_BRANCH: not on develop/release (or main for hotfix). Current: $BRANCH"
    exit 1
  fi
fi
```

#### Check 3 — CHANGELOG [Unreleased] Non-Empty

```bash
# Parse CHANGELOG.md for [Unreleased] section content
UNRELEASED=$(sed -n '/## \[Unreleased\]/,/## \[/p' CHANGELOG.md | sed '1d;$d' | grep -v '^$' | grep -v '^###')
if [ -z "$UNRELEASED" ]; then
  echo "ABORT VALIDATE_EMPTY_UNRELEASED: CHANGELOG [Unreleased] section empty"
  echo "Add entries to the [Unreleased] section before releasing."
  exit 1
fi
```

#### Check 4 — Build + Tests (skippable via `--skip-tests`)

```bash
if [ "$SKIP_TESTS" != true ]; then
  echo "Check 4: Running build + tests..."
  if ! {{BUILD_COMMAND}}; then
    echo "ABORT VALIDATE_BUILD_FAILED: Build or tests failed"
    exit 1
  fi
else
  echo "WARNING: Check 4 skipped (--skip-tests)"
fi
```

#### Check 5 — Coverage Thresholds (skippable via `--skip-tests`)

```bash
if [ "$SKIP_TESTS" != true ]; then
  echo "Check 5: Validating coverage thresholds..."
  # Parse coverage report (e.g., jacoco.xml for Java)
  LINE_COV=$(extract_line_coverage)   # project-specific
  BRANCH_COV=$(extract_branch_coverage)

  if (( $(echo "$LINE_COV < {{COVERAGE_LINE_THRESHOLD}}" | bc -l) )); then
    echo "ABORT VALIDATE_COVERAGE_LINE: Line coverage ${LINE_COV}% below threshold {{COVERAGE_LINE_THRESHOLD}}%"
    exit 1
  fi

  if (( $(echo "$BRANCH_COV < {{COVERAGE_BRANCH_THRESHOLD}}" | bc -l) )); then
    echo "ABORT VALIDATE_COVERAGE_BRANCH: Branch coverage ${BRANCH_COV}% below threshold {{COVERAGE_BRANCH_THRESHOLD}}%"
    exit 1
  fi

  echo "Coverage OK: line=${LINE_COV}%, branch=${BRANCH_COV}%"
else
  echo "WARNING: Check 5 skipped (--skip-tests)"
fi
```

#### Check 6 — Golden File Consistency (skippable via `--skip-tests`)

```bash
if [ "$SKIP_TESTS" != true ]; then
  if [ -n "{{GOLDEN_TEST_COMMAND}}" ]; then
    echo "Check 6: Running golden file tests..."
    if ! {{GOLDEN_TEST_COMMAND}}; then
      echo "ABORT VALIDATE_GOLDEN_DRIFT: Golden files out of sync"
      echo "Run golden file regeneration before releasing."
      exit 1
    fi
  else
    echo "Check 6: Skipped (no GOLDEN_TEST_COMMAND configured)"
  fi
else
  echo "WARNING: Check 6 skipped (--skip-tests)"
fi
```

#### Check 7 — Hardcoded Version Strings (always-mandatory)

```bash
echo "Check 7: Scanning for hardcoded version strings..."
CURRENT_VERSION=$(get_current_version)  # from pom.xml, package.json, etc.

# Allowed paths: version files, CHANGELOG, tags, state files
ALLOWED_PATTERN="pom.xml|package.json|Cargo.toml|pyproject.toml|build.gradle|CHANGELOG.md|release-state-"

MATCHES=$(grep -rn "$CURRENT_VERSION" . \
  --include="*.sh" --include="*.md" --include="*.yaml" --include="*.yml" \
  --include="*.properties" --include="*.env" \
  | grep -v -E "$ALLOWED_PATTERN" \
  | grep -v "node_modules" \
  | grep -v ".git/")

if [ -n "$MATCHES" ]; then
  echo "ABORT VALIDATE_HARDCODED_VERSION: Hardcoded version string found"
  echo "$MATCHES"
  exit 1
fi
```

#### Check 8 — Cross-File Version Consistency (always-mandatory)

```bash
echo "Check 8: Validating cross-file version consistency..."
# Extract version from each source
POM_VERSION=$(extract_pom_version)
CHANGELOG_VERSION=$(extract_latest_changelog_version)
BRANCH_VERSION=$(extract_branch_version)  # from release/X.Y.Z

# Compare: all must match the target VERSION
MISMATCH=""
if [ "$POM_VERSION" != "$VERSION" ]; then
  MISMATCH="$MISMATCH pom.xml=$POM_VERSION"
fi
if [ -n "$CHANGELOG_VERSION" ] && [ "$CHANGELOG_VERSION" != "$VERSION" ]; then
  MISMATCH="$MISMATCH CHANGELOG=$CHANGELOG_VERSION"
fi
if [ -n "$BRANCH_VERSION" ] && [ "$BRANCH_VERSION" != "$VERSION" ]; then
  MISMATCH="$MISMATCH branch=$BRANCH_VERSION"
fi

if [ -n "$MISMATCH" ]; then
  echo "ABORT VALIDATE_VERSION_MISMATCH: pom.xml version mismatch"
  echo "Expected: $VERSION, found:$MISMATCH"
  exit 1
fi
```

#### Check 9 — Generation Dry-Run (conditional)

This check only executes when `{{GENERATION_COMMAND}}` is configured
(non-empty). It runs the generator in dry-run mode and compares output
against current files.

```bash
if [ -n "{{GENERATION_COMMAND}}" ]; then
  echo "Check 9: Running generation dry-run..."
  if ! {{GENERATION_COMMAND}} --dry-run; then
    echo "ABORT VALIDATE_GENERATION_DRIFT: Generator output differs from baseline"
    echo "Run the generator before releasing."
    exit 1
  fi
else
  echo "Check 9: Skipped (no GENERATION_COMMAND configured)"
fi
```

#### State File Update on Success

After all checks pass, update the state file to advance the phase:

```bash
jq '.phase = "VALIDATED"
    | .phasesCompleted += ["VALIDATED"]
    | .lastPhaseCompletedAt = now | todate' \
  "$STATE_FILE" > "${STATE_FILE}.tmp.$$"
mv "${STATE_FILE}.tmp.$$" "$STATE_FILE"

echo "Phase VALIDATE-DEEP completed. All checks passed."
```

**Branch validation rules:**
- Standard release: MUST start from `develop` or an existing `release/*` branch
- Hotfix release (`--hotfix`): MUST start from `main`
- Starting from any other branch (e.g., `feature/*`) aborts with `VALIDATE_WRONG_BRANCH`

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

### Step 7 — Open Release PR

> **RULE-001 (PR-Flow):** `main` MUST NOT receive a direct `git merge` from
> the skill. This phase replaces the legacy Step 7 MERGE-MAIN with
> **Phase OPEN-RELEASE-PR**, which pushes the release branch and opens a
> pull request via `gh pr create --base main --head release/${VERSION}`.
> The operator (or CI) is responsible for merging the PR once reviews
> complete; the skill will then resume at Step 8 via
> `--continue-after-merge` (wired by story-0035-0005).

#### Step 7.1 — Honour --no-publish (Dry-Run for Remote)

If `--no-publish` is set, OPEN-RELEASE-PR short-circuits: no remote push,
no PR creation, no state mutation. Emit an informational block and exit
the phase so that local state reflects `COMMITTED` and operators can push
manually later.

```bash
if [ "$NO_PUBLISH" = "true" ]; then
  echo "[OPEN-RELEASE-PR] --no-publish set: skipping git push and"
  echo "                   gh pr create. Release state stays in"
  echo "                   'COMMITTED'. Push manually with:"
  echo "  git push -u origin \"release/${VERSION}\""
  echo "  gh pr create --base main --head \"release/${VERSION}\" \\"
  echo "    --title \"release: v${VERSION}\" --body \"\$(build_pr_body)\""
  exit 0
fi
```

#### Step 7.2 — Push the Release Branch

```bash
# Push the release branch so the PR has a remote head to target.
if ! git push -u origin "release/${VERSION}"; then
  echo "ABORT [PR_PUSH_REJECTED]: push rejected for release/${VERSION}."
  echo "Check branch protection and remote permissions, then retry."
  exit 1
fi
```

#### Step 7.3 — Extract the CHANGELOG Entry for this Version

```bash
# Capture the [X.Y.Z] block from CHANGELOG.md. awk prints lines from the
# target header up to the next "## [" header (exclusive), and sed drops
# the trailing delimiter line so only the current release body remains.
CHANGELOG_ENTRY=$(awk "/^## \[${VERSION}\]/,/^## \[/" CHANGELOG.md \
  | sed '$d')

if [ -z "$CHANGELOG_ENTRY" ]; then
  echo "ABORT [PR_NO_CHANGELOG_ENTRY]: no [${VERSION}] entry in"
  echo "CHANGELOG.md. Re-run Step 5 (CHANGELOG) before OPEN-RELEASE-PR."
  exit 1
fi
```

#### Step 7.4 — Build the PR Body and Invoke `gh pr create`

The PR body is the operator-facing snapshot of the release: it embeds the
CHANGELOG entry, repeats the resume hint, and lists the VALIDATE-DEEP
gates already cleared by Step 2.

```bash
build_pr_body() {
  cat <<EOF
## Release v${VERSION}

${CHANGELOG_ENTRY}

---

**Release type:** ${BUMP_TYPE}
**Previous version:** ${PREVIOUS_VERSION}

## Approval Gate

After merging this PR, re-run:

\`\`\`
/x-release ${VERSION} --continue-after-merge
\`\`\`

## Checklist (validated by VALIDATE-DEEP)

- [x] Tests passing
- [x] Coverage ≥ 95% line, ≥ 90% branch
- [x] Golden files consistent
- [x] CHANGELOG [${VERSION}] section populated
- [x] No hardcoded version strings
- [x] Cross-file version consistency
EOF
}

PR_TITLE="release: v${VERSION}"
PR_URL=$(gh pr create \
  --base main \
  --head "release/${VERSION}" \
  --title "${PR_TITLE}" \
  --body "$(build_pr_body)")

if [ -z "$PR_URL" ]; then
  echo "ABORT [PR_CREATE_FAILED]: could not capture PR URL from"
  echo "gh pr create. Inspect the gh output above and retry."
  exit 1
fi

# Derive the PR number from the URL returned by gh. This is deterministic
# and avoids the ambiguity of `gh pr view` when multiple PRs reference
# the same head branch.
PR_NUMBER="${PR_URL##*/}"
```

#### Step 7.5 — Persist PR Metadata to the State File

All writes use the atomic write-to-temp + rename protocol documented in
`references/state-file-schema.md`. The fields persisted here
(`prNumber`, `prUrl`, `prTitle`, `changelogEntry`) are the contract
consumed by `APPROVAL-GATE`, `RESUME-AND-TAG`, and `BACK-MERGE-DEVELOP`
in downstream stories.

```bash
TMP="${STATE_FILE}.tmp.$$"
jq --arg url   "$PR_URL" \
   --arg title "$PR_TITLE" \
   --arg num   "$PR_NUMBER" \
   --arg entry "$CHANGELOG_ENTRY" \
   --arg ts    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  '.phase = "PR_OPENED"
   | .prNumber = ($num | tonumber)
   | .prUrl = $url
   | .prTitle = $title
   | .changelogEntry = $entry
   | .lastPhaseCompletedAt = $ts
   | .phasesCompleted += ["OPEN_RELEASE_PR"]' \
  "$STATE_FILE" > "$TMP"
mv "$TMP" "$STATE_FILE"
```

#### Step 7.6 — Optional Specialist Review via `x-review-pr`

If `--skip-review` is absent AND the `x-review-pr` skill is available in
the project, invoke it in fire-and-forget mode so that the specialists
(Security, QA, Performance, …) post their findings to the release PR
while the operator reviews it. The skill is invoked through the
`Skill` tool so that the lifecycle orchestrator can propagate context
without spawning a nested shell.

```bash
if [ "$SKIP_REVIEW" != "true" ] && \
   [ -f ".claude/skills/x-review-pr/SKILL.md" ]; then
  # Fire-and-forget — failures of x-review-pr MUST NOT block the release.
  Skill("x-review-pr", "$PR_NUMBER") || \
    echo "WARNING: x-review-pr invocation failed; continue manually."
fi
```

If `--skip-review` is set, emit a single-line notice and skip the
invocation entirely:

```bash
if [ "$SKIP_REVIEW" = "true" ]; then
  echo "[OPEN-RELEASE-PR] --skip-review set: x-review-pr NOT invoked."
fi
```

#### Step 7.7 — Error Catalog (local to OPEN-RELEASE-PR)

| Condition | Code | Message template |
|:---|:---|:---|
| `git push` for the release branch is rejected | `PR_PUSH_REJECTED` | `Push rejected for release/${VERSION}. Check branch protection.` |
| `awk` produces an empty CHANGELOG entry | `PR_NO_CHANGELOG_ENTRY` | `No [${VERSION}] entry in CHANGELOG.md` |
| `gh pr create` returns a non-zero exit | `PR_CREATE_FAILED` | `Failed to create PR: <stderr captured from gh>` |

The codes above extend the catalog in
`references/state-file-schema.md`. Operators triage PR-flow failures
using the same vocabulary as the rest of the skill.

> **Behavior preserved (RULE-002):** The release commit (Step 6), the
> release branch creation (Step 3), and the CHANGELOG generation
> (Step 5) are untouched. Only the merge-to-main step is replaced — the
> skill still produces the exact same local state up to `COMMITTED`.

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

> **RULE-001 reminder:** `main` and `develop` MUST NOT receive a direct
> `git push` from this phase. They are updated exclusively through the
> release PR (Step 7, `OPEN-RELEASE-PR`) and the back-merge PR
> (Step 9, introduced by story-0035-0006). The only remote mutation Step
> 10 performs is pushing the release **tag**; the release branch push
> lives inside `OPEN-RELEASE-PR` (Step 7.2).

If `--no-publish` flag is NOT present, push the release tag to the
remote:

```bash
# Push the signed/annotated release tag created in Step 8.
git push origin "v${VERSION}"
```

If `--no-publish` flag IS present:
- Skip the tag push
- Display: "Release created locally. Run 'git push origin v${VERSION}' manually when ready."

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

| Scenario | Error Code | Action |
|----------|------------|--------|
| No tags found | — | Assume version 0.0.0, first release |
| Uncommitted changes | `VALIDATE_DIRTY_WORKDIR` | ABORT: Uncommitted changes in working directory |
| Wrong branch | `VALIDATE_WRONG_BRANCH` | ABORT: Not on develop/release (or main for hotfix) |
| CHANGELOG [Unreleased] empty | `VALIDATE_EMPTY_UNRELEASED` | ABORT: CHANGELOG [Unreleased] section empty |
| Build or tests fail | `VALIDATE_BUILD_FAILED` | ABORT: Build or tests failed |
| Line coverage below threshold | `VALIDATE_COVERAGE_LINE` | ABORT: Line coverage below threshold |
| Branch coverage below threshold | `VALIDATE_COVERAGE_BRANCH` | ABORT: Branch coverage below threshold |
| Golden files out of sync | `VALIDATE_GOLDEN_DRIFT` | ABORT: Golden files out of sync |
| Hardcoded version string found | `VALIDATE_HARDCODED_VERSION` | ABORT: Hardcoded version string found |
| Version mismatch across files | `VALIDATE_VERSION_MISMATCH` | ABORT: pom.xml version mismatch |
| Generator output differs | `VALIDATE_GENERATION_DRIFT` | ABORT: Generator output differs from baseline |
| Invalid version format | — | ABORT with SemVer format hint |
| No Conventional Commits found | — | Default to patch bump |
| Already tagged version | — | ABORT with "version already released" |
| Not on main (hotfix) | `VALIDATE_WRONG_BRANCH` | ABORT, hotfix must start from main |
| Push fails | — | Report error, release is local only |
| Merge conflict (main) | — | ABORT, resolve manually |
| Merge conflict (develop) | — | Warning, resolve and continue |
| Active release branch exists (hotfix) | — | Merge hotfix into release branch instead of develop |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-release-changelog` | calls | Delegates changelog generation via Agent tool |
| `x-git-push` | reads | Uses same Conventional Commits format for release commit |

- **release-management KP**: References SemVer rules, branching strategies, and registry patterns from `skills/release-management/SKILL.md`
- **Release Checklist**: Validates against `_TEMPLATE-RELEASE-CHECKLIST.md` for completeness
- **Rule 09 (Branching Model)**: Follows Git Flow branch types and merge direction rules
