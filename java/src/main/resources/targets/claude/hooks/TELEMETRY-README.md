# Telemetry Hook Scripts — EPIC-0040

This directory ships the Phase 1 shell hooks that capture Claude Code
tool/session activity as NDJSON telemetry events. The scripts are the
**source of truth** (RULE-008); `HooksAssembler` copies them to
`.claude/hooks/` at generation time (story-0040-0004).

## Files

| File | Hook Event | Emits | Notes |
|------|-----------|-------|-------|
| `telemetry-emit.sh` | — (helper) | — | stdin -> NDJSON append + scrubbing |
| `telemetry-lib.sh` | — (lib) | — | `resolve_context`, `build_event`, `now_iso` |
| `telemetry-session.sh` | `SessionStart` | `session.start` | |
| `telemetry-pretool.sh` | `PreToolUse` | (no event) | Writes `$TMPDIR/claude-telemetry/$id.start` |
| `telemetry-posttool.sh` | `PostToolUse` | `tool.call` | Reads start file, computes `durationMs`, sets `status` |
| `telemetry-subagent.sh` | `SubagentStop` | `subagent.end` | |
| `telemetry-stop.sh` | `Stop` | `session.end` | Cleans up `$TMPDIR/claude-telemetry/` |

## Storage (RULE-007)

Events are appended to:

```
${CLAUDE_PROJECT_DIR}/plans/epic-XXXX/telemetry/events.ndjson   # per-epic, committed
${CLAUDE_PROJECT_DIR}/plans/unknown/telemetry/events.ndjson     # fallback
```

One JSON event per line. Each line validates against
`java/src/main/resources/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json`
(published by story-0040-0001). The append is serialised with `flock(1)`
where available and an `mkdir`-based advisory lock otherwise (RULE-002).

## Fail-Open (RULE-004)

- All scripts use `set +e` (errors never abort the parent) and `set -u`
  (catches programmer bugs in unset variables).
- Any write failure logs to stderr and exits `0`.
- Read of stdin is bounded to 5 s when `timeout(1)` is available.

## Opt-Out (RULE-006)

Set `CLAUDE_TELEMETRY_DISABLED=1` in the shell env to suppress every
emission. The guard short-circuits before any I/O, so no side effects
remain (verified by the `disableFlag_suppressesAllEmission` smoke test).

## Context Resolution (RULE-005)

`resolve_context` (in `telemetry-lib.sh`) populates the three context
variables in this order:

1. `CLAUDE_TELEMETRY_CONTEXT` JSON env var with `epicId`/`storyId`/`taskId`.
2. Current Git branch: `feat/story-NNNN-MMMM-*` or
   `feat/task-NNNN-MMMM-NNN-*` or `feature/epic-NNNN-*` (regex match).
3. Any `plans/epic-*/execution-state.json` with `currentPhase != null`
   (newest matching file wins).
4. Fallback: `epicId = "unknown"`, story/task empty.

## Scrubbing (RULE-003, partial)

`telemetry-emit.sh` applies regex scrubbing BEFORE the append:

| Pattern | Replacement |
|---------|-------------|
| `AKIA[0-9A-Z]{16}` | `AKIA***REDACTED***` |
| `eyJ[A-Za-z0-9._-]{10,}` | `eyJ***REDACTED***` |
| `([Bb]earer )[A-Za-z0-9._~+/-]+=*` | `$1 ***REDACTED***` |

This is the shell-layer defence. Full PII scrubbing lives in
`TelemetryScrubber` (story-0040-0005).

## Validation

Run the integration smoke test:

```bash
cd java
mvn -Dtest='dev.iadev.telemetry.hooks.HooksSmokeIT' test
```

The test requires `bash` and `jq` on `PATH`; it is skipped gracefully when
either is missing (e.g., Windows developer machines).

`shellcheck -S warning` is enforced in CI (see `.github/workflows`).

## Dependencies

| Tool | Required | Notes |
|------|----------|-------|
| `bash` (>=4) | yes | All scripts use Bash arrays / `[[ ]]` |
| `jq` | yes | JSON parsing + NDJSON shaping |
| `flock` | optional | Preferred lock primitive; mkdir fallback |
| `timeout` | optional | Bounds stdin reads at 5 s |
| `uuidgen` | optional | UUIDv4 generation (procfs fallback) |
| `perl` or `python3` | optional | ms-precision timestamp fallback (macOS) |

All "optional" tools degrade gracefully.
