# Telemetry Event Contract

> Canonical reference for the telemetry events emitted by Claude Code hooks,
> skills, and subagents in the `ia-dev-environment` project.
> Source of truth: `_TEMPLATE-TELEMETRY-EVENT.json` (JSON Schema Draft 2020-12).
> Published by **EPIC-0040 / story-0040-0001**.

## 1. Scope

Every telemetry record written by the project MUST conform to this schema. The
schema is the single contract shared between:

- Shell hooks (story-0040-0003) that append NDJSON lines
- Java ingestion / analysis code (story-0040-0002, story-0040-0005)
- Downstream reporting tools

Violations of the schema are **not permitted** — readers MUST reject events
that fail validation so that the data remains machine-processable.

## 2. Field Reference

| Field | Type | Req. | Constraints | Example |
|-------|------|:----:|-------------|---------|
| `schemaVersion` | string (SemVer) | **M** | regex `^\d+\.\d+\.\d+$` | `"1.0.0"` |
| `eventId` | string (UUIDv4) | **M** | canonical UUIDv4 | `"a3b2..."` |
| `timestamp` | string (ISO-8601) | **M** | `YYYY-MM-DDTHH:mm:ss[.s{1,9}]Z` (fractional seconds optional) | `"2026-04-13T12:34:56.789Z"` |
| `sessionId` | string | **M** | 1..128 chars | `"claude-sess-abc123"` |
| `epicId` | string \| null | O | `EPIC-NNNN` or `unknown` | `"EPIC-0040"` |
| `storyId` | string \| null | O | `story-NNNN-NNNN` | `"story-0040-0001"` |
| `taskId` | string \| null | O | `TASK-NNNN-NNNN-NNN` | `"TASK-0040-0001-001"` |
| `type` | string (enum) | **M** | see §3 | `"tool.call"` |
| `skill` | string \| null | O | kebab-case, max 64 | `"x-story-implement"` |
| `phase` | string \| null | O | max 64 chars | `"Phase-2-Implementation"` |
| `tool` | string \| null | O | max 64 chars | `"Bash"` |
| `durationMs` | integer \| null | O | `>= 0` | `12345` |
| `status` | string \| null | O | `ok \| failed \| skipped` | `"ok"` |
| `failureReason` | string \| null | O | max 256 chars, scrubbed | `"timeout"` |
| `metadata` | object \| null | O | whitelist (RULE-003) | `{"retryCount": 0}` |

**Required (5):** `schemaVersion`, `eventId`, `timestamp`, `sessionId`, `type`.
All other fields are optional and MAY be `null`.

## 3. Event Type Enum

11 canonical values — adding a value requires a MINOR `schemaVersion` bump.

| Value | Emitter | Terminal? |
|-------|---------|:---------:|
| `session.start` | Claude Code session init | — |
| `session.end` | Claude Code session shutdown | terminal |
| `skill.start` | Skill tool invocation | — |
| `skill.end` | Skill tool return | terminal |
| `phase.start` | Skill phase marker (e.g., Phase 0) | — |
| `phase.end` | Skill phase closure | terminal |
| `tool.call` | Any Claude Code tool dispatch | — |
| `tool.result` | Tool result return | terminal |
| `subagent.start` | Task/Agent dispatch | — |
| `subagent.end` | Task/Agent return | terminal |
| `error` | Unexpected failure | terminal |

Terminal events SHOULD carry `durationMs` and `status`. Start-style /
non-terminal events SHOULD NOT set those fields unless they represent an
immediately known failure and no corresponding terminal event will be emitted
(for example, a failed `tool.call` that never produces `tool.result`).

## 4. Storage Layout

Two locations share the same per-event schema:

| Path | Committed? | Purpose |
|------|:----------:|---------|
| `plans/epic-XXXX/telemetry/events.ndjson` | **yes** | Canonical append-only log per epic. One event per line (RULE-002). |
| `plans/epic-XXXX/telemetry/sessions/{sessionId}.ndjson` | yes (optional) | Optional per-session shard for high-volume epics. |
| `.claude/telemetry/index.json` | **no** — gitignored | Local cache of aggregated metadata; rebuildable from the committed NDJSON. |

The project `.gitignore` excludes `.claude/telemetry/index.json` so the cache
never leaks into commits. The `plans/epic-*/telemetry/` directory structure IS
versioned.

## 5. Canonical Examples

Three fixtures ship alongside this contract under
`src/test/resources/fixtures/telemetry/` and drive automated validation:

### 5.1 `session-start-minimal.json` — minimal valid event

```json
{
  "schemaVersion": "1.0.0",
  "eventId": "11111111-1111-4111-8111-111111111111",
  "timestamp": "2026-04-16T12:34:56.789Z",
  "sessionId": "claude-sess-abc123",
  "type": "session.start"
}
```

### 5.2 `skill-end-with-duration.json` — happy skill termination

```json
{
  "schemaVersion": "1.0.0",
  "eventId": "22222222-2222-4222-8222-222222222222",
  "timestamp": "2026-04-16T12:36:01.500Z",
  "sessionId": "claude-sess-abc123",
  "epicId": "EPIC-0040",
  "storyId": "story-0040-0001",
  "taskId": "TASK-0040-0001-001",
  "type": "skill.end",
  "skill": "x-story-implement",
  "phase": "Phase-2-Implementation",
  "durationMs": 12345,
  "status": "ok",
  "metadata": { "retryCount": 0 }
}
```

### 5.3 `tool-call-failed.json` — tool call with failure

```json
{
  "schemaVersion": "1.0.0",
  "eventId": "33333333-3333-4333-8333-333333333333",
  "timestamp": "2026-04-16T12:37:15.001Z",
  "sessionId": "claude-sess-abc123",
  "epicId": "EPIC-0040",
  "storyId": "story-0040-0001",
  "type": "tool.call",
  "tool": "Bash",
  "durationMs": 678,
  "status": "failed",
  "failureReason": "timeout",
  "metadata": { "retryCount": 2 }
}
```

Two additional fixtures intentionally fail validation and exercise boundary /
error paths: `invalid-type.json` (type not in enum) and `negative-duration.json`
(durationMs = -1).

## 6. Evolution Policy (RULE-001)

- **PATCH** bump for docs-only / comment changes.
- **MINOR** bump for new optional field OR new enum value.
- **MAJOR** bump for removal / renaming of a required field OR breaking enum
  change.
- Readers MUST support the current MAJOR and the previous MAJOR (N-1).

## 7. References

- Story: `plans/epic-0040/story-0040-0001.md`
- Epic: `plans/epic-0040/epic-0040.md`
- Schema: `_TEMPLATE-TELEMETRY-EVENT.json` (sibling file)
- Tests: `java/src/test/java/dev/iadev/telemetry/schema/`
