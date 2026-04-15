# x-release State File Schema (schemaVersion: 2)

> **Source of Truth:** This document is the authoritative contract for the
> `plans/release-state-<X.Y.Z>.json` state file produced and consumed by the
> `x-release` skill. Every phase of the skill reads and writes this file
> atomically (write-to-temp + rename). Downstream stories and tests validate
> against this schema.

## Purpose

The state file provides **durable, cross-invocation persistence** of release
progress. It enables:

1. **Idempotency (RULE-003):** Each phase checks `phase` at the start and
   skips silently if its work is already recorded. Re-running the skill
   after a failure does not re-execute completed phases.
2. **Resume (RULE-004):** `--continue-after-merge` reads `phase:
   APPROVAL_PENDING` and resumes directly at `RESUME-AND-TAG`, without
   re-executing Steps 1–7. The resume flow still verifies PR merge status
   via `gh pr view` (defense in depth).
3. **Defense in depth:** Even when `--interactive` keeps the operator in
   the same Claude Code session, the state file is persisted so that an
   accidental session loss is recoverable.

## File Naming and Location

| Condition | Path |
|:---|:---|
| Default | `plans/release-state-<X.Y.Z>.json` |
| `--state-file <path>` | `<path>` (operator-provided absolute or relative path) |

The file is created during Step 0 (Resume Detection) with phase
`INITIALIZED`, then advances to `DETERMINED` after Step 1 and through
subsequent phases. It is removed (or moved to
`plans/release-state-<X.Y.Z>.json.done`) after `COMPLETED` by the
CLEANUP phase.

## Canonical JSON Example

```json
{
  "schemaVersion": 2,
  "version": "2.3.0",
  "previousVersion": "2.2.5",
  "bumpType": "minor",
  "phase": "APPROVAL_PENDING",
  "phasesCompleted": [
    "INITIALIZED",
    "DETERMINED",
    "VALIDATED",
    "BRANCHED",
    "UPDATED",
    "CHANGELOG_DONE",
    "COMMITTED",
    "PR_OPENED"
  ],
  "branch": "release/2.3.0",
  "baseBranch": "develop",
  "worktreePath": "/Users/dev/repo/.claude/worktrees/release-2.3.0",
  "hotfix": false,
  "dryRun": false,
  "signedTag": false,
  "interactive": true,
  "prNumber": 262,
  "prUrl": "https://github.com/acme/ia-dev-env/pull/262",
  "prTitle": "release: v2.3.0",
  "targetVersion": "2.3.0",
  "changelogEntry": "## [2.3.0] - 2026-04-11\n\n### Added\n- ...",
  "tagMessage": "Release v2.3.0\n\nChanges in this release:\n- ...",
  "startedAt": "2026-04-11T14:22:07Z",
  "lastPhaseCompletedAt": "2026-04-11T14:35:41Z",
  "nextActions": [
    {"label": "PR mergeado — continuar", "command": "/x-release"},
    {"label": "Rodar fix-pr-comments", "command": "/x-pr-fix"}
  ],
  "waitingFor": "PR_MERGE",
  "phaseDurations": {
    "VALIDATED": 142,
    "BRANCHED": 3,
    "UPDATED": 1,
    "CHANGELOG_DONE": 8,
    "COMMITTED": 2,
    "PR_OPENED": 12
  },
  "lastPromptAnsweredAt": "2026-04-11T14:35:42Z",
  "githubReleaseUrl": null
}
```

## Field Reference

### Required Fields (always present)

| Field | Type | Constraints | Description |
|:---|:---|:---|:---|
| `schemaVersion` | integer | MUST equal `2` for this schema revision | State file format version. Any other value (including legacy `1`) aborts with `STATE_SCHEMA_VERSION`. |
| `version` | string | SemVer `X.Y.Z` | Target release version. |
| `phase` | string | One of the 14 enum values (see below) | Current phase the skill has completed (inclusive). |
| `branch` | string | Non-empty | Release or hotfix branch name (e.g., `release/2.3.0`). |
| `baseBranch` | string | `develop` or `main` | Source branch the release branched from. |
| `hotfix` | boolean | — | `true` if the release was started with `--hotfix`. |
| `dryRun` | boolean | — | `true` if the release is executing in dry-run mode (state file is still created for consistency, but no remote mutations occur). |
| `signedTag` | boolean | — | `true` if `--signed-tag` was passed. |
| `interactive` | boolean | — | `true` if `--interactive` was passed. |
| `startedAt` | string | ISO-8601 UTC (`YYYY-MM-DDTHH:MM:SSZ`) | Timestamp of the first `INITIALIZED` write. |
| `lastPhaseCompletedAt` | string | ISO-8601 UTC | Timestamp of the most recent phase transition. |
| `phasesCompleted` | array of strings | Ordered, no duplicates, subset of the 14 phases | Every phase name that has already been completed. Used for idempotency checks. |

### Optional / Derived Fields

| Field | Type | Populated After | Description |
|:---|:---|:---|:---|
| `previousVersion` | string \| null | `DETERMINED` | Version detected from the latest tag (`null` for first release). |
| `bumpType` | string \| null | `DETERMINED` | `major`, `minor`, `patch`, or `explicit`. |
| `targetVersion` | string | `DETERMINED` | Equal to `version`; kept separate to match the Gherkin contract. |
| `prNumber` | integer \| null | `PR_OPENED` | Number of the release PR returned by `gh pr create`. |
| `prUrl` | string \| null | `PR_OPENED` | Full URL of the release PR. |
| `prTitle` | string \| null | `PR_OPENED` | Title of the release PR (`release: v<X.Y.Z>`). |
| `changelogEntry` | string \| null | `CHANGELOG_DONE` | Raw Markdown block generated by `x-release-changelog` for the release. Cached so that the tag message and PR body stay consistent after resume. |
| `tagMessage` | string \| null | `CHANGELOG_DONE` | Derived tag annotation text (used by both `git tag -a` and `git tag -s`). |
| `worktreePath` | string \| null | `BRANCHED` | Canonical absolute path of the release/hotfix worktree under `.claude/worktrees/release-X.Y.Z/` or `.claude/worktrees/hotfix-<slug>/`. Populated by Phase BRANCH (Step 3.3) after `x-git-worktree create` succeeds. Cleared (`null`) by Phase CLEANUP-WORKTREE after `x-git-worktree remove` succeeds. Optional for backward compatibility with state files written before EPIC-0037. |

### Schema v2 Fields

Introduced by EPIC-0039 story-0039-0002 to support interactive prompts,
smart resume, and telemetry for downstream stories (S07, S08, S10, S12).
All v2 fields are optional on read (absent => `null`/empty) but the
canonical writer always emits every key.

| Field | Type | Origin | When Set | Description |
|:---|:---|:---|:---|:---|
| `nextActions` | `Array<{label: String, command: String}>` \| null | skill orchestrator | Any halt phase | Ordered list of suggested slash-commands to present to the operator on the next prompt. `command` MUST match the allowlist regex `^/[a-z\-]+`. |
| `waitingFor` | enum `{NONE, PR_REVIEW, PR_MERGE, BACKMERGE_REVIEW, BACKMERGE_MERGE, USER_CONFIRMATION}` \| null | skill orchestrator | Any halt phase | Classifies the current halt. `NONE` when the skill is actively executing. |
| `phaseDurations` | `Map<String, Long>` \| null | skill orchestrator | Updated after each phase completes | Wall-clock duration in seconds per phase name. Empty map (`{}`) is valid (before the first phase completes). |
| `lastPromptAnsweredAt` | ISO-8601 UTC string \| null | prompt handler | After every operator prompt | Telemetry capturing human-response time; used by downstream observability stories. |
| `githubReleaseUrl` | string \| null | `PUBLISH` (S06) | After `gh release create` | URL of the GitHub Release created by the publish phase. Remains `null` until then. |

### Field Preservation After Resume

When `--continue-after-merge` resumes the flow, all fields described above
MUST be read unmodified from the persisted state file. The skill MUST NOT
recompute `version`, `prNumber`, `prUrl`, `changelogEntry`, or `tagMessage`
from scratch — doing so would break cross-session consistency and allow
silent drift between the approved PR and the tag that is ultimately pushed.

## `phase` Enum (15 values)

| Value | Meaning | Next valid phase |
|:---|:---|:---|
| `INITIALIZED` | State file created, dependencies verified | `DETERMINED` |
| `DETERMINED` | Step 1 complete — target version computed | `VALIDATED` |
| `VALIDATED` | `VALIDATE-DEEP` passed (coverage, golden, consistency) | `BRANCHED` |
| `BRANCHED` | Release (or hotfix) branch created inside worktree `.claude/worktrees/<id>/`; `worktreePath` populated | `UPDATED` |
| `UPDATED` | Version files updated and staged | `CHANGELOG_DONE` |
| `CHANGELOG_DONE` | `CHANGELOG.md` generated via `x-release-changelog` | `COMMITTED` |
| `COMMITTED` | Release commit created on the release branch | `PR_OPENED` |
| `PR_OPENED` | `OPEN-RELEASE-PR` complete — release PR opened via `gh pr create` | `APPROVAL_PENDING` |
| `APPROVAL_PENDING` | `APPROVAL-GATE` reached — skill paused waiting for the operator | `MERGED` (via `--continue-after-merge`) |
| `MERGED` | Release PR confirmed `MERGED` by `gh pr view` | `TAGGED` |
| `TAGGED` | Tag created on `main` (annotated or signed) and pushed | `BACKMERGE_OPENED` or `BACKMERGE_CONFLICT` |
| `BACKMERGE_OPENED` | `BACK-MERGE-DEVELOP` opened a back-merge PR | `WORKTREE_CLEANED` or `COMPLETED` |
| `BACKMERGE_CONFLICT` | Back-merge hit a conflict, blocked PR opened for manual resolution | `WORKTREE_CLEANED` or `COMPLETED` (after manual fix) |
| `WORKTREE_CLEANED` | `CLEANUP-WORKTREE` removed the release worktree via `x-git-worktree remove`; `worktreePath` cleared to `null`. Skipped silently on state files written before EPIC-0037 or when `worktreePath` is absent. | `COMPLETED` |
| `COMPLETED` | CLEANUP finished — release branch deleted, state file archived | terminal |

### Valid Transitions

Transitions are strictly forward. A phase MUST NOT be rolled back by the
skill itself. Any recovery from a failure state (e.g., `BACKMERGE_CONFLICT`)
happens manually, and the operator then re-invokes the skill, which
recognises the persisted phase and resumes at the correct place.

```
INITIALIZED
    -> DETERMINED
    -> VALIDATED
    -> BRANCHED
    -> UPDATED
    -> CHANGELOG_DONE
    -> COMMITTED
    -> PR_OPENED
    -> APPROVAL_PENDING
    -> MERGED
    -> TAGGED
    -> { BACKMERGE_OPENED | BACKMERGE_CONFLICT }
    -> WORKTREE_CLEANED   (skipped if worktreePath is absent / null)
    -> COMPLETED
```

## Atomic Write Protocol

Every write to the state file MUST follow the write-to-temp + rename
pattern to guarantee atomicity on POSIX filesystems:

```bash
TMP="${STATE_FILE}.tmp.$$"
jq --arg phase "$NEW_PHASE" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '.phase = $phase | .lastPhaseCompletedAt = $ts
    | .phasesCompleted += [$phase]' \
   "$STATE_FILE" > "$TMP"
mv "$TMP" "$STATE_FILE"
```

Direct in-place edits are forbidden because a mid-write crash can produce
truncated JSON, which subsequent invocations would reject with
`STATE_INVALID_JSON`.

## Error Codes Emitted While Reading the State File

| Condition | Code | Message template |
|:---|:---|:---|
| `gh` CLI missing from `PATH` | `DEP_GH_MISSING` | `gh CLI not installed. See https://cli.github.com/` |
| `jq` missing from `PATH` | `DEP_JQ_MISSING` | `jq not installed. Install via your package manager.` |
| `gh auth status` non-zero | `DEP_GH_AUTH` | `gh not authenticated. Run 'gh auth login'.` |
| File exists but `jq .` fails | `STATE_INVALID_JSON` | `State file exists but is not valid JSON: <path>` |
| `schemaVersion` is unknown (includes legacy v1) | `STATE_SCHEMA_VERSION` | `State file v<N> is no longer supported. Abort the active release via /x-release --abort and start a new one.` |
| `waitingFor` value is not one of the 6 enum values | `STATE_INVALID_ENUM` | `Unknown waitingFor value: <value>. Expected one of: NONE, PR_REVIEW, PR_MERGE, BACKMERGE_REVIEW, BACKMERGE_MERGE, USER_CONFIRMATION.` |
| `nextActions[].command` does not match `^/[a-z\-]+` | `STATE_INVALID_ACTION` | `nextActions[].command must match ^/[a-z\-]+ (got: <value>)` |
| `--continue-after-merge` with no state file | `RESUME_NO_STATE` | `No release in progress. Run /x-release <version> first.` |
| State file exists with `phase != COMPLETED` and no resume flag | `STATE_CONFLICT` | `Release in progress for v<X.Y.Z>. Use --continue-after-merge or delete state.` |
| Hotfix slug fails `^[a-z0-9][a-z0-9-]{0,62}$` | `WT_SLUG_INVALID` | `Hotfix slug must match ^[a-z0-9][a-z0-9-]{0,62}$ (got length <n>)` |
| Reused release worktree points at wrong branch | `WT_RELEASE_BRANCH_MISMATCH` | `Existing worktree <id> is on '<actual>', expected '<expected>'` |
| `x-git-worktree create` returned non-zero during Phase BRANCH, or path escapes `.claude/worktrees/` prefix | `WT_RELEASE_CREATE_FAILED` | `Could not create release worktree <id>` |
| `x-git-worktree remove` returned non-zero during Phase CLEANUP-WORKTREE, or `cd` back to main repo failed | `WT_RELEASE_REMOVE_FAILED` | `Could not remove release worktree <id>; left in place for inspection` |

## Validation Rules (enforced by `ReleaseStateFileSchemaTest`, story 0008)

1. `schemaVersion` MUST be the integer `2`. Legacy state files with `schemaVersion: 1` are rejected without silent upgrade (story-0039-0002 §3.2).
2. `version` MUST match `^[0-9]+\.[0-9]+\.[0-9]+$`.
3. `phase` MUST be one of the 15 enum values.
4. `phasesCompleted` MUST be an array of valid phase names with no
   duplicates.
5. Timestamps (`startedAt`, `lastPhaseCompletedAt`) MUST parse as
   ISO-8601 UTC with trailing `Z`.
6. `hotfix`, `dryRun`, `signedTag`, `interactive` MUST be booleans.
7. `prNumber` (when present) MUST be a positive integer.
8. When `phase >= PR_OPENED`, `prNumber` and `prUrl` MUST be non-null.
9. When `phase >= CHANGELOG_DONE`, `changelogEntry` MUST be non-null.
10. The sequence of `phasesCompleted` MUST be a prefix of the valid
    transition chain documented above.
11. `worktreePath` (when present and non-null) MUST be an absolute path
    whose canonical form lies under `<repo>/.claude/worktrees/`. When
    `phase == BRANCHED`, `worktreePath` MUST be non-null. State files
    where `worktreePath` is absent (legacy / pre-EPIC-0037) are accepted
    only for `phase` values that pre-date the BRANCHED introduction of
    worktree provisioning.
12. When `phase == WORKTREE_CLEANED`, `worktreePath` MUST be `null`.
13. `nextActions` (when present and non-null) MUST be an array whose every entry has a non-null `command` matching `^/[a-z\-]+`. Violations emit `STATE_INVALID_ACTION`.
14. `waitingFor` (when present and non-null) MUST be one of the 6 enum values declared in Schema v2 Fields. Unknown values emit `STATE_INVALID_ENUM`.
15. `phaseDurations` (when present) MUST be a map of phase-name -> non-negative integer seconds. Empty map (`{}`) is valid.
