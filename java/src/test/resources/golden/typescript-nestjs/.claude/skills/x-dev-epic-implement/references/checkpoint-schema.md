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
| `contextPressure` | Object | No | `{ "currentLevel": 0 }` | Context pressure degradation state tracking pressure level and phases completed. See Context Pressure State schema below. |

## Context Pressure State

Tracks context window pressure to enable progressive degradation (see Section 1.7 in SKILL.md):

```json
"contextPressure": {
  "currentLevel": 0,
  "degradationActivatedAt": null,
  "phasesCompletedInConversation": 0
}
```

| Field | Type | Required | Default | Validation | Description |
|-------|------|----------|---------|------------|-------------|
| `currentLevel` | Integer | Yes | `0` | `enum: [0, 1, 2, 3]` | Current degradation level: 0 (normal), 1 (warning), 2 (critical), 3 (emergency) |
| `degradationActivatedAt` | String | No | `null` | ISO-8601 UTC | Timestamp when degradation first activated (Level 0 → Level 1) |
| `phasesCompletedInConversation` | Integer | Yes | `0` | `>= 0` | Number of phases completed in the current conversation. Resets on `--resume` (new conversation). |

**Context Pressure Level Values:**

| Level | Name | Entry Condition | Actions |
|-------|------|-----------------|---------|
| 0 | Normal | Initial state; no pressure signals detected | Full execution, full logging |
| 1 | Warning | Truncated output; "output too large"; `phasesCompletedInConversation >= 3` | Reduce verbosity; skip optional phases; slim mode for reviews |
| 2 | Critical | System compression; `ERR-CONTEXT-001`/`ERR-CONTEXT-002`; token limit errors | Force delegation; skip reviews; pressure header in prompts |
| 3 | Emergency | 3+ consecutive tool failures; incoherent output; lost instructions | Save state; suggest `--resume`; stop execution |

**Progressive Advancement:** Levels MUST advance sequentially (0→1→2→3). Even if Level 3 signals are detected at Level 0, the orchestrator advances one level at a time, applying each level's actions before proceeding.

**Reset Rules:**

| Event | `currentLevel` | `phasesCompletedInConversation` | `degradationActivatedAt` |
|-------|---------------|-------------------------------|-------------------------|
| `--resume` (new conversation) | Preserved | Reset to 0 | Preserved |
| Normal operation | Unchanged | Incremented per phase | Unchanged |

**Backward Compatibility:** The `contextPressure` field is OPTIONAL. When not present, it is treated as default normal state (`{ "currentLevel": 0, "degradationActivatedAt": null, "phasesCompletedInConversation": 0 }`). Existing checkpoints without this field continue to work unchanged.

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
