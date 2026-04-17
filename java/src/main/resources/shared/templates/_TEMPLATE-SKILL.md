---
name: {{SKILL_NAME}}
description: "{{SKILL_DESCRIPTION}}"
user-invocable: true
allowed-tools: Bash, Read, Write, Edit, Grep, Glob, Skill
argument-hint: "{{ARGUMENT_HINT}}"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: {{SKILL_TITLE}}

## Purpose

{{SKILL_PURPOSE}}

## Triggers

- `/{{SKILL_NAME}} {{EXAMPLE_ARG}}` -- {{EXAMPLE_DESCRIPTION}}

## Parameters

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `{{ARG_NAME}}` | {{ARG_TYPE}} | {{ARG_DEFAULT}} | {{ARG_DESCRIPTION}} |

## Workflow

1. {{STEP_1}}
2. {{STEP_2}}
3. {{STEP_3}}

## Outputs

| Artifact | Path | Description |
|----------|------|-------------|
| {{OUTPUT_NAME}} | {{OUTPUT_PATH}} | {{OUTPUT_DESCRIPTION}} |

## Error Handling

| Scenario | Action |
|----------|--------|
| {{ERROR_SCENARIO}} | {{ERROR_ACTION}} |

## Telemetry (Optional)

Use the canonical marker shape below — an HTML comment discriminator (so the
orchestrator can grep balanced pairs) immediately followed by the `Bash
command:` line that invokes the shared helper. The `*end` variants MUST carry
a status argument (`ok` | `failed` | `skipped`) so the emitted event records
the real outcome of the phase.

If this skill has numbered phases, wrap each phase with a start/end pair:

```markdown
<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start {{SKILL_NAME}} <phase-name>`

... phase body ...

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end {{SKILL_NAME}} <phase-name> ok`
```

For parallel subagent dispatch, wrap each agent with `subagent.start` /
`subagent.end` markers:

```markdown
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start {{SKILL_NAME}} <role>`

... agent work ...

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end {{SKILL_NAME}} <role> ok`
```

For MCP tool calls, wrap the call with `tool.call` markers and pass the MCP
method name as the third positional argument:

```markdown
<!-- TELEMETRY: tool.call mcp-start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh mcp-start {{SKILL_NAME}} <mcpMethod>`

... MCP call ...

<!-- TELEMETRY: tool.call mcp-end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh mcp-end {{SKILL_NAME}} <mcpMethod> ok`
```

On failure, set the final status to `failed` (or `skipped` when the phase /
subagent / MCP call was deliberately bypassed) so downstream telemetry
reflects the real outcome. Do NOT omit the status argument — the `*end`
helpers default to `ok` only as a last-resort fail-open.

Reference: `.claude/rules/13-skill-invocation-protocol.md` (section "Telemetry Markers").
Canonical example: `x-story-implement` (see its phase markers for a working reference).
