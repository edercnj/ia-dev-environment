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
 2. VALIDATE        -> Check pre-conditions (branch, working directory)
 3. BRANCH          -> Create release/X.Y.Z branch from develop (or hotfix/* from main)
 4. UPDATE          -> Update version in project-specific files (strip SNAPSHOT)
 5. CHANGELOG       -> Generate/update CHANGELOG.md via x-release-changelog
 6. COMMIT          -> Create release commit on release branch
 7. OPEN-RELEASE-PR -> Push release branch and open PR to main via gh pr create
 8. APPROVAL-GATE   -> Persist APPROVAL_PENDING, print instructions, halt (or interactive)
 9. TAG             -> Create annotated/signed git tag on main (after merged PR)
10. BACK-MERGE-DEVELOP -> Open PR to develop with conflict detection (clean or conflict flow)
11. PUBLISH         -> Push the release tag to remote (main/develop go via PR flow)
12. CLEANUP         -> Delete the release branch
    DRY-RUN         -> If --dry-run, show plan and exit (no changes)
```

> **Note:** Step 0 (Resume Detection) is the new entry point introduced by
> EPIC-0035. It MUST execute before Step 1 on every invocation. Downstream
> stories (0002–0008) replace Steps 7–10 with PR-flow phases
> (`OPEN-RELEASE-PR`, `APPROVAL-GATE`, `RESUME-AND-TAG`,
> `BACK-MERGE-DEVELOP`). Steps 1, 3, 4, 5, 6 and 12 are preserved verbatim
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

### Step 8 — Approval Gate

> **RULE-003 (Idempotência via State File):** This phase writes
> `APPROVAL_PENDING` to the state file and halts. Re-invocation without
> `--continue-after-merge` detects the in-flight state in Step 0 and
> aborts with `STATE_CONFLICT`, preventing double execution.

> **Reference:** See `references/approval-gate-workflow.md` for the
> complete workflow diagram, state transitions, and interactive mode
> decision tree.

The Approval Gate is the safety checkpoint between opening the release PR
(Step 7) and applying irreversible actions (tag, back-merge). Nothing
irreversible has happened yet — the operator can abort by closing the PR.

#### Step 8.1 — Persist APPROVAL_PENDING State

```bash
# Atomic state update: PR_OPENED -> APPROVAL_PENDING
TMP="${STATE_FILE}.tmp.$$"
jq --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  '.phase = "APPROVAL_PENDING"
   | .phasesCompleted += ["APPROVAL_GATE_REACHED"]
   | .lastPhaseCompletedAt = $ts' \
  "$STATE_FILE" > "$TMP"
mv "$TMP" "$STATE_FILE"
```

#### Step 8.2 — Print Human-Readable Instructions

```bash
PR_URL=$(jq -r '.prUrl' "$STATE_FILE")
PR_NUMBER=$(jq -r '.prNumber' "$STATE_FILE")

cat <<EOF
============================================================
APPROVAL GATE — RELEASE v${VERSION}
============================================================

PR opened: ${PR_URL}
PR number: #${PR_NUMBER}

NEXT STEPS (MANUAL):
  1. Open the PR in GitHub: ${PR_URL}
  2. Ensure all CI checks pass
  3. Collect required code review approvals
  4. Merge the PR via GitHub UI (prefer merge commit)
  5. Re-run this skill:
     /x-release ${VERSION} --continue-after-merge

State saved to: ${STATE_FILE}
The skill will now exit. Nothing irreversible has happened yet.
To cancel: close the PR and delete state file + release branch.
============================================================
EOF
```

#### Step 8.3 — Branch on `--interactive` Flag

If `--interactive` is **not** set, the skill exits immediately:

```bash
if [ "$INTERACTIVE" != "true" ]; then
  echo "Skill halted at APPROVAL GATE. Resume with --continue-after-merge."
  exit 0
fi
```

If `--interactive` **is** set, the skill uses `AskUserQuestion` with three
options instead of exiting:

```
AskUserQuestion:
  question: "Release v${VERSION} — PR #${PR_NUMBER} opened. Choose an action:"
  options:
    1. "PR merged, continue to tag (Recommended)"
    2. "Halt — resume later with --continue-after-merge"
    3. "Cancel release entirely"
```

##### Option 1 — Continue to Tag (Defense in Depth)

Verify via `gh pr view` that the PR is actually merged before proceeding.
This prevents the operator from accidentally claiming "merged" when the PR
is still open.

```bash
PR_STATE=$(gh pr view "$PR_NUMBER" --json state --jq '.state')
if [ "$PR_STATE" != "MERGED" ]; then
  echo "ABORT [APPROVAL_PR_STILL_OPEN]: PR #${PR_NUMBER} is still ${PR_STATE}."
  echo "Merge the PR first, then choose option 1 again."
  # Re-present the 3 options (loop back to AskUserQuestion)
  exit 1
fi
# PR confirmed merged — proceed to Step 9 (TAG) in-session
```

##### Option 2 — Halt

Identical to the default (non-interactive) behavior:

```bash
echo "Halt selected. Resume later with:"
echo "  /x-release ${VERSION} --continue-after-merge"
exit 0
```

##### Option 3 — Cancel Release

Requires double confirmation to prevent accidental cancellation:

```
AskUserQuestion:
  question: "CONFIRM: Cancel release v${VERSION}? This will delete the state file.
             The release PR and branch must be closed/deleted manually."
  options:
    1. "Yes, cancel the release"
    2. "No, go back"
```

If confirmed:

```bash
# Delete the state file
rm -f "$STATE_FILE"

echo "============================================================"
echo "RELEASE CANCELLED — v${VERSION}"
echo "============================================================"
echo ""
echo "State file deleted: ${STATE_FILE}"
echo ""
echo "MANUAL CLEANUP REQUIRED:"
echo "  gh pr close ${PR_NUMBER}"
echo "  git push origin --delete release/${VERSION}"
echo "  git branch -d release/${VERSION}"
echo "============================================================"

exit 2  # APPROVAL_CANCELLED
```

If not confirmed, loop back to the 3 options.

#### Step 8.4 — Error Catalog (local to APPROVAL-GATE)

| Condition | Code | Message template |
|:---|:---|:---|
| Interactive option 1 but PR state != MERGED | `APPROVAL_PR_STILL_OPEN` | `PR #${PR_NUMBER} is still ${PR_STATE}. Merge first.` |
| Interactive option 3 confirmed | `APPROVAL_CANCELLED` | `Release cancelled by user.` (exit 2) |

The codes above extend the catalog in
`references/state-file-schema.md`. Operators triage approval-gate
failures using the same vocabulary as the rest of the skill.

> **Idempotency (RULE-003):** If the skill is re-invoked without
> `--continue-after-merge` and the state file has `phase:
> APPROVAL_PENDING`, Step 0 detects the in-flight state and aborts
> with `STATE_CONFLICT`. The APPROVAL-GATE phase never executes
> twice for the same release.

### Step 9 — Tag Creation

Create an annotated git tag on `main` (after the merge):

```bash
# Create annotated tag on main
git tag -a "v${VERSION}" -m "Release v${VERSION}

Changes in this release:
$(git log $(git describe --tags --abbrev=0 HEAD~1 2>/dev/null)..HEAD~1 \
    --format='- %s' --no-merges)"
```

### Step 10 — Back-Merge Develop (Phase BACK-MERGE-DEVELOP)

> **RULE-001 (PR-Flow):** `develop` MUST NOT receive a direct `git merge` from
> the skill. This phase replaces the legacy Step 10 MERGE-BACK with
> **Phase BACK-MERGE-DEVELOP**, which detects conflicts via a dry-run merge,
> then opens a PR via `gh pr create --base develop`. Clean merges include
> Java SNAPSHOT advance; conflict merges produce an explanatory PR for human
> resolution.

> **Reference:** See `references/backmerge-strategies.md` for the complete
> workflow diagram and decision tree for both clean and conflict flows.

#### Step 10.1 — Verify Phase Prerequisite

```bash
# Verify phase == TAGGED
PHASE=$(jq -r .phase "$STATE_FILE")
if [ "$PHASE" != "TAGGED" ]; then
  echo "ABORT [BACKMERGE_WRONG_PHASE]: expected TAGGED, got $PHASE"
  exit 1
fi
```

#### Step 10.2 — Create Backmerge Branch and Dry-Run Merge

```bash
BACKMERGE_BRANCH="chore/backmerge-v${VERSION}"

# Create backmerge branch from develop
git fetch origin develop
git checkout -b "$BACKMERGE_BRANCH" origin/develop

# Dry-run merge to detect conflicts
git merge --no-commit --no-ff origin/main 2>/dev/null
MERGE_EXIT=$?
```

#### Step 10.3 — Clean Merge Flow (exit 0)

```bash
if [ $MERGE_EXIT -eq 0 ]; then
  # Preserve Java SNAPSHOT advance
  if [ -f pom.xml ] && [ "$HOTFIX" != "true" ]; then
    NEXT_MINOR=$((MINOR + 1))
    NEXT_SNAPSHOT="${MAJOR}.${NEXT_MINOR}.0-SNAPSHOT"
    sed -i '' '/<parent>/,/<\/parent>/!{
      0,/<version>.*<\/version>/s|<version>.*</version>|<version>'"$NEXT_SNAPSHOT"'</version>|
    }' pom.xml
    git add pom.xml
    git commit -m "chore: advance develop to ${NEXT_SNAPSHOT}"
  else
    git commit -m "release: merge v${VERSION} back into develop"
  fi

  git push -u origin "$BACKMERGE_BRANCH"

  BACKMERGE_PR_URL=$(gh pr create \
    --base develop \
    --head "$BACKMERGE_BRANCH" \
    --title "chore(release): back-merge v${VERSION} to develop" \
    --body "Automated back-merge from main after v${VERSION} release. Clean merge.")

  BACKMERGE_PR_NUMBER="${BACKMERGE_PR_URL##*/}"

  # State: BACKMERGE_OPENED
  TMP="${STATE_FILE}.tmp.$$"
  jq --arg url "$BACKMERGE_PR_URL" \
     --arg num "$BACKMERGE_PR_NUMBER" \
     --arg ts  "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    '.phase = "BACKMERGE_OPENED"
     | .backmergePrUrl = $url
     | .backmergePrNumber = ($num | tonumber)
     | .lastPhaseCompletedAt = $ts
     | .phasesCompleted += ["BACK_MERGE_DEVELOP"]' \
    "$STATE_FILE" > "$TMP"
  mv "$TMP" "$STATE_FILE"
fi
```

#### Step 10.4 — Conflict Flow (exit 1)

```bash
if [ $MERGE_EXIT -eq 1 ]; then
  # Git NEVER creates a commit while paths are unmerged, even with --no-verify.
  # Strategy: capture conflicting files from the dry-run merge, ABORT the merge
  # to return to a clean working tree, then push origin/main as-is to the backmerge
  # branch and open a PR --base develop --head main. GitHub's PR view will surface
  # the conflict and the reviewer resolves it in the PR UI (or locally).

  CONFLICT_LIST=$(git diff --name-only --diff-filter=U | head -20)

  # Return the working tree to a valid state — no commit with unmerged paths.
  git merge --abort

  # Push main's commit directly to the backmerge branch (no merge commit yet).
  git push -u origin "main:refs/heads/${BACKMERGE_BRANCH}"

  # Serialize conflict list into a JSON array for jq.
  CONFLICTS_JSON=$(printf '%s\n' "$CONFLICT_LIST" \
    | jq -R -s 'split("\n") | map(select(length > 0))')

  BACKMERGE_PR_URL=$(gh pr create \
    --base develop \
    --head "$BACKMERGE_BRANCH" \
    --title "chore(release): back-merge v${VERSION} (CONFLICTS)" \
    --body "⚠️ CONFLICTS DETECTED during local dry-run merge.

Conflicting files:
\`\`\`
${CONFLICT_LIST}
\`\`\`

Local merge was aborted because Git cannot create a commit with unmerged paths.
Resolve the conflicts in this PR (via GitHub UI) before completing the back-merge.

Java SNAPSHOT advance was NOT applied — add it manually during conflict resolution if needed.")

  BACKMERGE_PR_NUMBER="${BACKMERGE_PR_URL##*/}"

  # State: BACKMERGE_CONFLICT with conflictFiles captured
  TMP="${STATE_FILE}.tmp.$$"
  jq --argjson files "$CONFLICTS_JSON" \
     --arg url "$BACKMERGE_PR_URL" \
     --arg num "$BACKMERGE_PR_NUMBER" \
     --arg ts  "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    '.phase = "BACKMERGE_CONFLICT"
     | .conflictFiles = $files
     | .backmergePrUrl = $url
     | .backmergePrNumber = ($num | tonumber)
     | .lastPhaseCompletedAt = $ts
     | .phasesCompleted += ["BACK_MERGE_DEVELOP_CONFLICT"]' \
    "$STATE_FILE" > "$TMP"
  mv "$TMP" "$STATE_FILE"
fi
```

#### Step 10.5 — Unexpected Exit Code

```bash
if [ $MERGE_EXIT -ne 0 ] && [ $MERGE_EXIT -ne 1 ]; then
  echo "ABORT [BACKMERGE_UNEXPECTED]: git merge returned unexpected exit code: $MERGE_EXIT"
  git merge --abort 2>/dev/null || true
  exit 1
fi
```

#### Step 10.6 — Error Catalog (local to BACK-MERGE-DEVELOP)

| Condition | Code | Message template |
|:---|:---|:---|
| Phase is not `TAGGED` | `BACKMERGE_WRONG_PHASE` | `Expected TAGGED, got ${phase}` |
| `git merge` returns unexpected exit code | `BACKMERGE_UNEXPECTED` | `Unexpected exit code: ${code}` |

The codes above extend the catalog in
`references/state-file-schema.md`. Operators triage back-merge failures
using the same vocabulary as the rest of the skill.

#### Step 10.7 — Phase Completion Summary

| Merge result | State after | PR title | SNAPSHOT advance |
|:---|:---|:---|:---|
| Clean (exit 0), Java, non-hotfix | `BACKMERGE_OPENED` | `chore(release): back-merge v${VERSION} to develop` | Yes |
| Clean (exit 0), hotfix or non-Java | `BACKMERGE_OPENED` | `chore(release): back-merge v${VERSION} to develop` | No |
| Conflict (exit 1) | `BACKMERGE_CONFLICT` | `chore(release): back-merge v${VERSION} (CONFLICTS)` | No (manual) |

> **Behavior preserved (RULE-002):** The SNAPSHOT advance logic is
> identical to the legacy Step 10 — only the delivery mechanism changes
> from direct `git merge` to PR-flow. The sed command for pom.xml and
> the commit message `chore: advance develop to ${NEXT_SNAPSHOT}` are
> carried over verbatim.

### Step 11 — Publish

> **RULE-001 reminder:** `main` and `develop` MUST NOT receive a direct
> `git push` from this phase. They are updated exclusively through the
> release PR (Step 7, `OPEN-RELEASE-PR`) and the back-merge PR
> (Step 10, introduced by story-0035-0006). The only remote mutation Step
> 11 performs is pushing the release **tag**; the release branch push
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

### Step 12 — Cleanup

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

Current version:  2.2.2
Target version:   2.3.0
Bump type:        minor
Source branch:    develop
Mode:             standard (not hotfix)
State file:       plans/release-state-2.3.0.json

Phases:
  0. RESUME_DETECTION  -> check state file, verify gh/jq
                       -> will create new state with phase: INITIALIZED
  1. DETERMINE         -> bump: minor, current: 2.2.2, target: 2.3.0
  2. VALIDATE_DEEP     -> will run 8 checks:
                          [1] working dir clean
                          [2] on develop branch
                          [3] CHANGELOG [Unreleased] populated
                          [4] mvn clean verify -Pall-tests
                          [5] coverage >= 95% line, 90% branch
                          [6] golden file tests pass
                          [7] no hardcoded version strings outside pom/CHANGELOG
                          [8] cross-file version consistency
                       -> skip tests (4-6) if --skip-tests
  3. BRANCH            -> create release/2.3.0 from develop
  4. UPDATE            -> pom.xml: 2.2.2-SNAPSHOT -> 2.3.0
  5. CHANGELOG         -> invoke x-release-changelog skill
                       -> moves [Unreleased] -> [2.3.0] - 2026-04-10
  6. COMMIT            -> git commit -m "release: v2.3.0"
  7. OPEN_RELEASE_PR   -> git push origin release/2.3.0
                       -> gh pr create --base main --head release/2.3.0
                       -> PR body from CHANGELOG entry
                       -> save prNumber, prUrl to state
                       -> skip review automation if --skip-review
 ------------------------------------------------------------
  8. APPROVAL_GATE     -> SKILL WILL HALT HERE
                       -> phase: APPROVAL_PENDING persisted
                       -> exit 0, waiting for manual PR merge
                       -> with --interactive: AskUserQuestion pause instead
 ------------------------------------------------------------
 === HUMAN MUST MERGE PR IN GITHUB ===
 ------------------------------------------------------------
  9. RESUME_AND_TAG    -> requires --continue-after-merge flag
                       -> gh pr view 262 --json state,mergedAt
                       -> verify state == MERGED (defense in depth)
                       -> git checkout main, pull
                       -> git tag -a v2.3.0 (or -s if --signed-tag)
                       -> git push origin v2.3.0
 10. BACK_MERGE_DEVELOP-> git checkout -b chore/backmerge-v2.3.0 origin/develop
                       -> git merge --no-ff origin/main
                       -> if clean: SNAPSHOT advance to 2.4.0-SNAPSHOT (Java only)
                       -> gh pr create --base develop --head chore/backmerge-v2.3.0
                       -> if conflict: PR body explains, state = BACKMERGE_CONFLICT
 11. CLEANUP           -> delete release/2.3.0 (local + remote)
                       -> delete plans/release-state-2.3.0.json
                       -> print final report

Flags active: (none)
Estimated duration:
  Phases 0-7 (until halt):    ~5-10 min
  Human wait (approval):      minutes to hours
  Phases 9-11 (after resume): ~2-3 min

=== NO CHANGES MADE ===
```

For hotfix mode (`--hotfix`), the dry-run shows:
```
Source branch:    main (hotfix mode)
Bump type:        patch (forced)
Branch created:   hotfix/2.2.3 (from main)
```

**Important:** In dry-run mode, all phases are only simulated. No files are modified, no commits are created, no tags are made, no branches are created, no PRs are opened.

## Hotfix Release (with PR-Flow)

Hotfix releases follow the same phase sequence as standard releases but with
key differences. Per RULE-001, all merges use PR-flow via `gh pr create` —
no direct `git merge` to `main` or `develop`.

### Standard vs. Hotfix Differences

| Difference | Standard | Hotfix |
|---|---|---|
| Branch base | develop | main |
| Branch created | release/X.Y.Z | hotfix/X.Y.Z |
| Bump type | major/minor/patch | PATCH only (MAJOR/MINOR forbidden) |
| SNAPSHOT advance | Yes (X.(Y+1).0-SNAPSHOT) | No (skipped) |
| Back-merge target | develop | develop AND active release/* (if exists) |

### Phases Modified in Hotfix Mode

- **Phase 1 (DETERMINE)**: Forces bump = patch. MAJOR/MINOR trigger error
  `HOTFIX_INVALID_BUMP` with message: `Hotfix mode only allows patch bump. Got: <type>`.
- **Phase 2 (VALIDATE_DEEP)**: Check [2] (correct branch) expects `main`, not `develop`.
- **Phase 3 (BRANCH)**: Creates `hotfix/X.Y.Z` from `main` instead of `release/X.Y.Z`
  from `develop`.
- **Phase 7 (OPEN_RELEASE_PR)**: PR targets main via
  `gh pr create --base main --head hotfix/X.Y.Z` with title `fix: v${VERSION} (hotfix)`.
- **Phase 10 (BACK_MERGE_DEVELOP)**:
  - Always: opens PR `--base develop --head chore/backmerge-v${VERSION}`
  - If active `release/*` exists: opens additional PR to that branch
  - SNAPSHOT advance is SKIPPED for hotfix mode

### Hotfix Rules

- **Origin branch:** Always `main` (never `develop`)
- **Version bump:** PATCH only (e.g., `1.2.0` -> `1.2.1`)
- **PR targets:** `main` (Phase 7) AND `develop` (Phase 10), plus active `release/*` if exists
- **Tag location:** `main` (after PR merged, via Phase 9)
- MAJOR or MINOR bumps are forbidden in hotfix releases — enforced by `HOTFIX_INVALID_BUMP`

### Active Release Branch Detection (Phase 10, Hotfix Only)

```bash
# Detect active release/* branch on remote
RELEASE_BRANCH=$(git branch -r | grep -E 'origin/release/' | head -1 | sed 's|origin/||' | tr -d ' ')

if [ -n "$RELEASE_BRANCH" ]; then
  # Create additional PR from hotfix backmerge to active release
  gh pr create \
    --base "$RELEASE_BRANCH" \
    --head "chore/backmerge-v${VERSION}" \
    --title "chore(hotfix): merge hotfix v${VERSION} into ${RELEASE_BRANCH}" \
    --body "Propagates hotfix v${VERSION} fixes to active release branch."
fi

# Always create PR to develop (standard back-merge)
gh pr create \
  --base develop \
  --head "chore/backmerge-v${VERSION}" \
  --title "chore(hotfix): back-merge v${VERSION} to develop" \
  --body "Automated back-merge from main after hotfix v${VERSION}."
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

## Consolidated Error Catalog

All error codes emitted by the release skill, organized by phase. Each code
uses `UPPER_SNAKE_CASE` and is unique across the entire catalog. Operators
triage failures using these codes; the full human-readable messages are
printed at runtime.

| Phase | Error Code | Condition | Message | Exit |
| :--- | :--- | :--- | :--- | :--- |
| 0 | `DEP_GH_MISSING` | `gh` CLI not installed | `gh CLI not installed. See https://cli.github.com/` | 1 |
| 0 | `DEP_JQ_MISSING` | `jq` not installed | `jq not installed. Install via your package manager.` | 1 |
| 0 | `DEP_GH_AUTH` | `gh` not authenticated | `gh not authenticated. Run 'gh auth login'.` | 1 |
| 0 | `STATE_INVALID_JSON` | State file is corrupted JSON | `State file exists but is not valid JSON: <path>` | 1 |
| 0 | `STATE_SCHEMA_VERSION` | Unknown `schemaVersion` | `Unknown schemaVersion: <n>. Expected: 1.` | 1 |
| 0 | `RESUME_NO_STATE` | `--continue-after-merge` without state file | `No release in progress. Run /x-release <version> first.` | 1 |
| 0 | `STATE_CONFLICT` | State file exists with `phase != COMPLETED` | `Release in progress for v<X.Y.Z>. Use --continue-after-merge or delete state.` | 1 |
| 1 | `HOTFIX_INVALID_BUMP` | `--hotfix` with bump type != patch | `Hotfix mode only allows patch bump. Got: <type>` | 1 |
| 2 | `VALIDATE_DIRTY_WORKDIR` | `git status --porcelain` non-empty | `Uncommitted changes in working directory` | 1 |
| 2 | `VALIDATE_WRONG_BRANCH` | Not on expected branch (develop/main) | `Not on develop/release branch (or main for hotfix)` | 1 |
| 2 | `VALIDATE_EMPTY_UNRELEASED` | CHANGELOG `[Unreleased]` section empty | `CHANGELOG [Unreleased] section empty or missing entries` | 1 |
| 2 | `VALIDATE_BUILD_FAILED` | `mvn clean verify` exits non-zero | `Build or tests failed — see output above` | 1 |
| 2 | `VALIDATE_COVERAGE_LINE` | Line coverage below threshold | `Line coverage <actual>% below threshold 95%` | 1 |
| 2 | `VALIDATE_COVERAGE_BRANCH` | Branch coverage below threshold | `Branch coverage <actual>% below threshold 90%` | 1 |
| 2 | `VALIDATE_GOLDEN_DRIFT` | Golden file tests fail | `Golden files out of sync. Run GoldenFileRegenerator.` | 1 |
| 2 | `VALIDATE_HARDCODED_VERSION` | Version string in unexpected file | `Found hardcoded version <V> in: <files>` | 1 |
| 2 | `VALIDATE_VERSION_MISMATCH` | pom.xml version != target version | `pom.xml version <pom> != target version <target>` | 1 |
| 2 | `VALIDATE_GENERATION_DRIFT` | Generator output differs from golden | `Generator output differs from golden baseline` | 1 |
| 7 | `PR_NO_CHANGELOG_ENTRY` | No `[X.Y.Z]` entry in CHANGELOG.md | `No [<V>] entry found in CHANGELOG.md. Ensure Step 5 completed.` | 1 |
| 7 | `PR_PUSH_REJECTED` | `git push` rejected | `Push rejected. Check branch protection or network.` | 1 |
| 7 | `PR_CREATE_FAILED` | `gh pr create` exits non-zero | `Failed to create PR: <stderr>` | 1 |
| 8 | `APPROVAL_PR_STILL_OPEN` | Interactive option 1 but PR not merged | `PR is still OPEN. Merge first, then --continue-after-merge.` | 1 |
| 8 | `APPROVAL_CANCELLED` | Interactive option 3 confirmed by user | `Release cancelled by user. Manual cleanup required.` | 2 |
| 9 | `RESUME_GH_FAILED` | `gh pr view` command fails | `Failed to query PR #<n>. Check gh auth and network.` | 1 |
| 9 | `RESUME_PR_NOT_MERGED` | PR state is not MERGED | `PR #<n> is <state>, not MERGED. Merge first.` | 1 |
| 9 | `RESUME_PR_NO_MERGE_TIME` | `mergedAt` field is null | `PR state inconsistency — mergedAt is null.` | 1 |
| 9 | `RESUME_TAG_LOCAL_EXISTS` | Tag already exists locally | `Tag v<V> already exists locally` | 1 |
| 9 | `RESUME_TAG_REMOTE_EXISTS` | Tag already exists on remote | `Tag v<V> already exists on origin. Release already cut.` | 1 |
| 9 | `RESUME_TAG_SIGN_FAILED` | `git tag -s` GPG failure | `Failed to create signed tag. Check GPG configuration.` | 1 |
| 9 | `RESUME_TAG_PUSH_FAILED` | `git push` of tag fails (warning only) | `Tag created locally but push failed. Run 'git push origin v<V>' manually.` | — |
| 10 | `BACKMERGE_WRONG_PHASE` | Phase is not TAGGED | `Expected phase TAGGED, got <phase>` | 1 |
| 10 | `BACKMERGE_UNEXPECTED` | `git merge` returns unexpected exit code | `Unexpected git merge exit code: <n>` | 1 |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-release-changelog` | calls | Delegates changelog generation via Agent tool |
| `x-git-push` | reads | Uses same Conventional Commits format for release commit |

- **release-management KP**: References SemVer rules, branching strategies, and registry patterns from `skills/release-management/SKILL.md`
- **Release Checklist**: Validates against `_TEMPLATE-RELEASE-CHECKLIST.md` for completeness
- **Rule 09 (Branching Model)**: Follows Git Flow branch types and merge direction rules
