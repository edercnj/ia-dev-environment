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

If this skill has numbered phases, emit phase markers via the shared helper.
Insert at the start AND end of each phase:

```bash
$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start {{SKILL_NAME}} <phase-name>
$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end {{SKILL_NAME}} <phase-name>
```

For parallel subagents, use `subagent-start` / `subagent-end`:

```bash
$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start {{SKILL_NAME}} <subagent-name>
$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end {{SKILL_NAME}} <subagent-name>
```

For MCP calls, use `mcp-start` / `mcp-end`:

```bash
$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh mcp-start {{SKILL_NAME}} <mcp-operation>
$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh mcp-end {{SKILL_NAME}} <mcp-operation>
```

Reference: `.claude/rules/13-skill-invocation-protocol.md` (section "Telemetry Markers").
Canonical example: `x-dev-story-implement` (see its phase markers for a working reference).
