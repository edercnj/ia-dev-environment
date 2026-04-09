# Checkpoint Schema Reference

> **Context:** This reference details the execution-state.json schema and per-task fields.
> Part of x-dev-epic-implement skill.

## Per-story `StoryEntry` Schema in `execution-state.json`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | Story ID (e.g., `story-0042-0001`) |
| `phase` | Integer | Yes | Phase number |
| `status` | String | Yes | `PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `PARTIAL`, `BLOCKED`, `PR_CREATED`, `PR_PENDING_REVIEW`, `PR_MERGED` |
| `commitSha` | String | When SUCCESS | Last commit SHA |
| `findingsCount` | Integer | Yes | Number of review findings |
| `summary` | String | Yes | Brief description |
| `retries` | Integer | Yes | Number of retry attempts |
| `duration` | Number | When completed | Execution duration in seconds |
| `blockedBy` | String[] | When BLOCKED | IDs of blocking stories |
| `prUrl` | String | When PR created | URL of the story PR |
| `prNumber` | Integer | When PR created | GitHub PR number |
| `prMergeStatus` | String | When PR created | `OPEN`, `MERGED`, `CLOSED` |

> See auto-rebase section in integrity-gate.md for additional per-story rebase tracking fields (`rebaseStatus`, `lastRebaseSha`, `rebaseAttempts`).

**Top-level `execution-state.json` fields (in addition to per-story entries):**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `baseBranch` | String | Yes | `"develop"` | Base branch for PRs, auto-rebase, and resume. Used by all stories in the epic. |

## Per-Task Checkpoint

When a story is being executed in PRE_PLANNED mode (tasks available from `plans/epic-{epicId}/plans/tasks-{storyId}.md`), the `execution-state.json` tracks individual task progress within each story entry:

```json
{
  "stories": {
    "story-XXXX-YYYY": {
      "status": "IN_PROGRESS",
      "tasks": {
        "TASK-001": { "status": "DONE", "agent": "architect", "type": "DEV", "commitSha": "abc123", "duration": 45000 },
        "TASK-002": { "status": "IN_PROGRESS", "agent": "qa-engineer", "type": "TEST" },
        "TASK-003": { "status": "PENDING", "agent": "security-engineer", "type": "SEC" }
      }
    }
  }
}
```

**Task Status Values:**

| Status | Meaning | Transitions |
|--------|---------|------------|
| PENDING | Task not started | -> IN_PROGRESS |
| IN_PROGRESS | Task being executed | -> DONE, -> PENDING (on resume) |
| DONE | Task completed with commit | Terminal |
| BLOCKED | Task blocked by dependency | -> PENDING (when dep DONE) |
| SKIPPED | Task not applicable | Terminal |

**Per-Task Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | String (Enum) | Yes | `PENDING`, `IN_PROGRESS`, `DONE`, `BLOCKED`, `SKIPPED` |
| `agent` | String | Yes | Agent persona executing the task (e.g., `architect`, `qa-engineer`) |
| `type` | String | Yes | Task type from task breakdown (e.g., `DEV`, `TEST`, `SEC`, `REFACTOR`) |
| `commitSha` | String | When DONE | Commit SHA produced by the task |
| `duration` | Number | When DONE | Execution duration in milliseconds |

**Backward Compatibility:** The `tasks` field is OPTIONAL in the `StoryEntry` schema. Stories executed without PRE_PLANNED mode (no task breakdown file) will not have this field. All existing checkpoint logic continues to work without `tasks`.

## Rebase Tracking Fields (Per-Story)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `rebaseStatus` | String (Enum) | Optional | `PENDING`, `REBASING`, `REBASE_SUCCESS`, `REBASE_FAILED` |
| `lastRebaseSha` | String | Optional | SHA-1 hex (40 chars) of develop used for last rebase |
| `rebaseAttempts` | Integer | Optional | Number of rebase attempts (0 to MAX_REBASE_RETRIES) |

> **Note:** `rebaseStatus` is a sub-field within each story entry, NOT a primary
> story status. The primary story status remains: PENDING, IN_PROGRESS, SUCCESS,
> FAILED, BLOCKED, PARTIAL, PR_CREATED, PR_PENDING_REVIEW, PR_MERGED.

## PR Comment Remediation Schema

The `prCommentRemediation` field is added to `execution-state.json` at the top level:

```json
{
  "prCommentRemediation": {
    "status": "COMPLETE | SKIPPED | DRY_RUN",
    "fixPrUrl": "https://github.com/{owner}/{repo}/pull/159",
    "fixPrNumber": 159,
    "fixesApplied": 8,
    "reportPath": "plans/epic-{epicId}/reports/pr-comments-report.md"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | String (Enum) | Yes | `COMPLETE`, `SKIPPED`, or `DRY_RUN` |
| `fixPrUrl` | String | When COMPLETE | URL of the correction PR |
| `fixPrNumber` | Integer | When COMPLETE | GitHub PR number of the correction PR |
| `fixesApplied` | Integer | Yes | Number of fixes applied (0 when SKIPPED or DRY_RUN) |
| `reportPath` | String | When COMPLETE or DRY_RUN | Path to the generated findings report |

## Circuit Breaker Schema

The `circuitBreaker` field is a top-level field in `execution-state.json` that tracks failure patterns per phase:

```json
{
  "circuitBreaker": {
    "consecutiveFailures": 0,
    "totalFailuresInPhase": 0,
    "lastFailureAt": null,
    "lastFailurePattern": null,
    "status": "CLOSED"
  }
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `consecutiveFailures` | Integer | Yes | `0` | Number of consecutive story failures without an intervening SUCCESS |
| `totalFailuresInPhase` | Integer | Yes | `0` | Total number of story failures in the current phase |
| `lastFailureAt` | String (ISO-8601) | No | `null` | Timestamp of the most recent failure |
| `lastFailurePattern` | String (Enum) | No | `null` | `TRANSIENT`, `CONTEXT`, `PERMANENT`, `MIXED`, or `null` |
| `status` | String (Enum) | Yes | `"CLOSED"` | `CLOSED` (normal), `OPEN` (tripped — execution paused or phase aborted) |

**State transitions:**
- Story SUCCESS → `consecutiveFailures = 0`, `status` remains `CLOSED`
- Story FAILED → increment `consecutiveFailures` and `totalFailuresInPhase`
- `consecutiveFailures >= 3` → `status = "OPEN"` (phase paused)
- `totalFailuresInPhase >= 5` → `status = "OPEN"` (phase aborted)
- `--resume` → reset `consecutiveFailures = 0`, `status = "CLOSED"`

The circuit breaker resets at the start of each new phase (`consecutiveFailures = 0`, `totalFailuresInPhase = 0`, `status = "CLOSED"`).

## Context Pressure Schema

The `contextPressure` field is a top-level field in `execution-state.json` that tracks context window usage:

```json
{
  "contextPressure": {
    "currentLevel": 0,
    "degradationActivatedAt": null,
    "phasesCompletedInConversation": 0
  }
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `currentLevel` | Integer | Yes | `0` | Current context pressure level (0 = none, 1 = low, 2 = medium, 3 = high) |
| `degradationActivatedAt` | String (ISO-8601) | No | `null` | Timestamp when graceful degradation was activated (level >= 2) |
| `phasesCompletedInConversation` | Integer | Yes | `0` | Number of phases completed in the current conversation (used to estimate remaining capacity) |
