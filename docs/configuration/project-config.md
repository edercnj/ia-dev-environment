# Project Configuration (`project-config.yaml`)

This page documents the optional configuration sections that drive
`mvn process-resources` — the pipeline that generates `.claude/`,
`.github/`, and related artefacts.

For the full list of required fields (project, architecture,
interfaces, language, framework), see the project README. The
section below covers the optional `telemetry` block introduced by
EPIC-0040 (story-0040-0004).

## `telemetry` (optional)

Controls whether the SkillTelemetry hooks are injected into the
generated `.claude/settings.json`. When enabled (default), the
pipeline writes the 5 hook entries listed below and copies the 8
supporting shell scripts into `.claude/hooks/` with `0755`
permissions.

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `telemetry.enabled` | `boolean` | `true` | Inject telemetry hooks into `.claude/settings.json` and copy telemetry shell scripts to `.claude/hooks/`. |

### Enabled (default)

When the `telemetry` block is absent or `telemetry.enabled: true`,
the following hook events are emitted:

| Event | Matcher | Script | Timeout |
| :--- | :--- | :--- | :--- |
| `SessionStart` | — | `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-session.sh` | 5 s |
| `PreToolUse` | `*` | `telemetry-pretool.sh` | 5 s |
| `PostToolUse` | `*` | `telemetry-posttool.sh` | 5 s |
| `SubagentStop` | — | `telemetry-subagent.sh` | 5 s |
| `Stop` | — | `telemetry-stop.sh` | 5 s |

Scripts copied to `.claude/hooks/`:

- `telemetry-emit.sh` (shared emit helper)
- `telemetry-lib.sh` (common helpers)
- `telemetry-phase.sh` (skill-phase markers, EPIC-0040 story-0040-0006)
- `telemetry-session.sh`
- `telemetry-pretool.sh`
- `telemetry-posttool.sh`
- `telemetry-subagent.sh`
- `telemetry-stop.sh`

### Disabled

Opt out explicitly with:

```yaml
telemetry:
  enabled: false
```

When disabled, `SettingsAssembler` skips the 5 hook entries and
`HooksAssembler` skips copying the 8 telemetry scripts (the 5
entrypoints — `telemetry-session`, `telemetry-pretool`,
`telemetry-posttool`, `telemetry-subagent`, `telemetry-stop` — plus
`telemetry-emit.sh`, `telemetry-lib.sh`, and `telemetry-phase.sh`).
The resulting `.claude/settings.json` contains no `telemetry-*`
string and the `.claude/hooks/` directory contains no telemetry files.

### Coexistence with `post-compile-check.sh`

For compiled stacks (java-maven, java-gradle, kotlin-gradle,
typescript-npm, rust, go, csharp), the legacy
`post-compile-check.sh` hook remains registered under
`PostToolUse` with matcher `Write|Edit`. When telemetry is also
enabled, the `PostToolUse` array contains two entries:

```json
"PostToolUse": [
  {"matcher": "Write|Edit", "hooks": [{"command": ".../post-compile-check.sh", "timeout": 60, ...}]},
  {"matcher": "*",          "hooks": [{"command": ".../telemetry-posttool.sh", "timeout": 5}]}
]
```

Both handlers run on every matching tool use.

### Example YAML

```yaml
project:
  name: my-service
  purpose: Example service

# ... required sections ...

telemetry:
  enabled: true   # default; include the field only when opting out
```

### Error Behaviour

| Situation | Behaviour |
| :--- | :--- |
| `telemetry` section absent | Defaults to `enabled=true`. |
| `telemetry.enabled` key absent inside section | Defaults to `true`. |
| Telemetry script missing under `targets/claude/hooks/` | Pipeline aborts with `UncheckedIOException` citing the missing path. |
| `chmod 0755` fails (non-POSIX FS) | Logged; copy succeeds without executable bit. |

### References

- EPIC-0040 — Telemetria de Execução de Skills
- story-0040-0004 — SettingsAssembler injects telemetry hooks
- story-0040-0003 — Telemetry hook shell scripts
- `HooksAssembler.TELEMETRY_SCRIPTS` — canonical file list
- `HookConfigBuilder.appendHooksSection` — JSON emission logic
