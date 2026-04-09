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
| `version` | String | Yes | `"3.0"` | Schema version. New checkpoints use `"3.0"`. Existing `"2.0"` or absent version continues to work (backward compat). |
| `baseBranch` | String | Yes | `"develop"` | Base branch for PRs, auto-rebase, and resume. Used by all stories in the epic. |
| `errorHistory` | Array | No | `[]` | Chronological array of error entries recorded during execution. See Error History Entry schema below. |
| `circuitBreaker` | Object | No | `{ "status": "CLOSED" }` | Circuit breaker state tracking consecutive and total failures. See Circuit Breaker State schema below. |
| `contextPressure` | Object | No | `{ "currentLevel": 0 }` | Context window pressure tracking for degradation decisions. See Context Pressure State schema below. |

## Schema Version (v3.0)

New checkpoints MUST be created with `"version": "3.0"`. The version field enables forward-compatible schema evolution:

- **`"3.0"`**: Full schema with `errorHistory`, `circuitBreaker`, and `contextPressure` fields.
- **`"2.0"` or absent**: Legacy schema. New fields are treated as empty/default when not present. Version is NOT auto-upgraded.

## Error History Entry

Each entry in the `errorHistory` array records a single error occurrence:

```json
{
  "timestamp": "2026-04-08T14:30:00Z",
  "storyId": "story-0042-0003",
  "taskId": "TASK-0042-0003-002",
  "errorCode": "ERR-TRANSIENT-001",
  "errorMessage": "Claude API overloaded",
  "phase": "2.2.5",
  "retryCount": 2,
  "resolution": "SUCCESS_AFTER_RETRY"
}
```

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `timestamp` | String | Yes | ISO-8601 UTC | When the error occurred |
| `storyId` | String | Yes | `story-XXXX-YYYY` | Story being executed when error occurred |
| `taskId` | String | No | `TASK-XXXX-YYYY-NNN` | Task being executed (if applicable) |
| `errorCode` | String | Yes | `ERR-[A-Z]+-[0-9]{3}` | Standardized error code from the error catalog |
| `errorMessage` | String | Yes | Max 500 chars | Human-readable error description |
| `phase` | String | Yes | — | Execution phase when error occurred (e.g., `2.2.5`) |
| `retryCount` | Integer | Yes | >= 0 | Number of retry attempts before resolution |
| `resolution` | String | Yes | Enum | Final outcome of the error |

**Resolution Values:**

| Value | Meaning |
|-------|---------|
| `SUCCESS_AFTER_RETRY` | Error was resolved after one or more retries |
| `FAILED` | All retries exhausted, error was not resolved |
| `ESCALATED` | Error was escalated (e.g., to user, to a different strategy) |

## Circuit Breaker State

Tracks failure patterns to enable threshold-based escalation (see Section 1.7 in SKILL.md):

```json
{
  "consecutiveFailures": 0,
  "totalFailuresInPhase": 0,
  "lastFailureAt": null,
  "lastFailurePattern": null,
  "status": "CLOSED"
}
```

| Field | Type | Required | Default | Validation | Description |
|-------|------|----------|---------|------------|-------------|
| `consecutiveFailures` | Integer | Yes | `0` | `>= 0` | Number of consecutive failures without a success. Resets to 0 on any SUCCESS. |
| `totalFailuresInPhase` | Integer | Yes | `0` | `>= 0` | Total failures in the current execution phase. Resets per phase and on `--resume`. |
| `lastFailureAt` | String | No | `null` | ISO-8601 UTC | Timestamp of the most recent failure |
| `lastFailurePattern` | String | No | `null` | See values below | Pattern analysis result from 2+ consecutive failures |
| `status` | String | Yes | `"CLOSED"` | Enum | Circuit breaker status: `CLOSED`, `OPEN`, `HALF_OPEN` |

**Circuit Breaker Status Values:**

| Status | Meaning | Entry Condition |
|--------|---------|-----------------|
| `CLOSED` | Normal operation, errors tracked but threshold not reached | Initial state; SUCCESS after HALF_OPEN; `--resume`; user "Skip phase" |
| `OPEN` | 3+ consecutive failures, execution paused for user decision | 3 consecutive failures from CLOSED; FAILED from HALF_OPEN |
| `HALF_OPEN` | Testing recovery after user chose "Continue" | User chooses "Continue" from OPEN |

**`lastFailurePattern` Values:**

| Pattern | Meaning |
|---------|---------|
| `"Systemic: repeated {errorType} failures"` | All recent failures have the same `errorType` |
| `"Intermittent: mixed failure types ({type1}, {type2})"` | Recent failures have different `errorType` values |
| `"Unknown: error types not classified"` | `errorType` not available in the failure results |
| `null` | No failures recorded yet |

**Reset Rules:**

| Event | `consecutiveFailures` | `totalFailuresInPhase` | `status` |
|-------|----------------------|----------------------|----------|
| Story SUCCESS | Reset to 0 | Unchanged | Stay/return to CLOSED |
| `--resume` | Reset to 0 | Reset to 0 | Reset to CLOSED |
| User "Continue" at OPEN | Reset to 0 | Unchanged | HALF_OPEN |
| New phase starts | Reset to 0 | Reset to 0 | Reset to CLOSED |

## Context Pressure State

Tracks context window pressure for graceful degradation:

```json
{
  "currentLevel": 0,
  "degradationActivatedAt": null,
  "phasesCompletedInConversation": 0
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `currentLevel` | Integer | Yes | `0` | Current pressure level (0 = normal, 1 = elevated, 2 = high, 3 = critical) |
| `degradationActivatedAt` | String | No | `null` | ISO-8601 UTC timestamp when degradation was first activated |
| `phasesCompletedInConversation` | Integer | Yes | `0` | Number of phases completed in the current conversation |

## Backward Compatibility

Files with `version` set to `"2.0"` or with the `version` field absent continue to work without modification:

1. **Missing `errorHistory`**: Treated as an empty array (`[]`). No errors are recorded unless the orchestrator explicitly initializes the field.
2. **Missing `circuitBreaker`**: Treated as default closed state (`{ "status": "CLOSED", "consecutiveFailures": 0, "totalFailuresInPhase": 0 }`).
3. **Missing `contextPressure`**: Treated as default no-pressure state (`{ "currentLevel": 0, "phasesCompletedInConversation": 0 }`).
4. **Version is NOT auto-upgraded**: A `"2.0"` checkpoint remains `"2.0"` even after new fields are added during execution. Only new checkpoints are created with `"3.0"`.
5. **All existing checkpoint logic** (story status, per-task tracking, rebase tracking, PR comment remediation) continues to work unchanged.

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
