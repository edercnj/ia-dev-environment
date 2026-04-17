---
name: x-jira-create-epic
description: "Create a Jira Epic from an existing local epic markdown file. Read the epic file, map fields to Jira, create the issue via MCP, and sync the Jira key back to the local file."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion
argument-hint: "[EPIC_FILE_PATH]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Create Jira Epic

## Purpose

Create a Jira Epic issue from an existing local `epic-XXXX.md` file. Parse the epic markdown, map fields to Jira issue attributes, create the Epic via MCP, and sync the returned Jira key back to the local file for bidirectional traceability.

## Triggers

- `/x-jira-create-epic <epic_file_path>` — create Jira Epic from the specified file
- User says "create epic in Jira", "sync epic to Jira", or "push epic to Jira"
- After running `/x-epic-create` or `/x-epic-decompose` without Jira integration

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `EPIC_FILE_PATH` | Path | No | — | Path to the epic markdown file (prompted if omitted) |

## Prerequisites

Read the field mapping reference before creating issues:
- `.claude/skills/x-jira-create-epic/references/jira-field-mapping.md`

## Workflow

### Step 1 — Input and Parse

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-jira-create-epic Phase-1-Read-Markdown`

1. Accept the epic file path as argument. If not provided, ask:
   ```
   question: "Qual o caminho do arquivo do epico? (ex: plans/epic-0012/epic-0012.md)"
   header: "Epic File"
   ```
2. Read the epic file completely
3. Extract:
   - **Title**: From `# Epico: <titulo>` (line 1)
   - **Local ID**: From the directory name (`epic-XXXX`)
   - **Overview**: From Section 1 (Visao Geral) — all text after `**Chave Jira:**` line until Section 2
   - **Status**: From the header `**Status:**` field
   - **Existing Jira Key**: From `**Chave Jira:**` field

### Step 2 — Pre-checks

1. Verify `mcp__atlassian__createJiraIssue` is available. If not:
   ```
   ERROR: MCP tool mcp__atlassian__createJiraIssue not available.
   Ensure the Atlassian MCP server is configured in .claude/settings.json.
   ```
   Abort.

2. Check existing Jira key. If `**Chave Jira:**` has a real value (not `<CHAVE-JIRA>` and not `—`):
   ```
   question: "Este epico ja tem chave Jira: {KEY}. O que deseja fazer?"
   header: "Epico ja vinculado ao Jira"
   options:
     - label: "Criar novo epico no Jira"
       description: "Ignorar a chave existente e criar um novo issue"
     - label: "Pular — manter a chave existente"
       description: "Nao criar no Jira, manter o vinculo atual"
   ```
   If "Pular": exit with message "Epico mantido com chave {KEY}."

### Step 3 — Project Selection

1. Call `mcp__atlassian__getAccessibleAtlassianResources` to discover available Atlassian sites
2. Use the first available site's `id` as `cloudId`. If no sites returned:
   ```
   ERROR: No accessible Atlassian sites found. Check your Atlassian credentials.
   ```
   Abort.

3. Call `mcp__atlassian__getVisibleJiraProjects` with the discovered `cloudId`
4. Present the project list to the user:
   ```
   question: "Selecione o projeto Jira para criar o epico:"
   header: "Projeto Jira"
   options:
     - label: "{PROJECT_KEY} — my-fastapi-timescale"
       description: "Project key: {PROJECT_KEY}"
     (one option per project)
   ```
5. Capture the selected `projectKey`

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-jira-create-epic Phase-1-Read-Markdown ok`

### Step 4 — Create Epic in Jira

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-jira-create-epic Phase-2-MCP-Call`

Wrap the Jira MCP call with mcp-start / mcp-end markers (story-0040-0008
§3.2) so the `tool.call` event carries `tool=mcp__atlassian__createJiraIssue`
and a measured `durationMs`:

<!-- TELEMETRY: tool.call mcp-start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh mcp-start x-jira-create-epic createJiraIssue`

Call `mcp__atlassian__createJiraIssue` with:
- `cloudId`: discovered cloudId
- `projectKey`: selected project key
- `issueTypeName`: "Epic"
- `summary`: epic title (extracted from `# Epico: <titulo>`)
- `description`: Section 1 (Visao Geral) text content
- `contentFormat`: "markdown"
- `additional_fields`:
  ```json
  {
    "labels": [
      { "name": "generated-by-ia-dev-env" },
      { "name": "epic-XXXX" }
    ]
  }
  ```

Where `epic-XXXX` is the local epic ID (e.g., `epic-0012`).

<!-- TELEMETRY: tool.call mcp-end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh mcp-end x-jira-create-epic createJiraIssue ok`

(On MCP failure, invoke the mcp-end marker with status `failed` instead of
`ok` so the telemetry record reflects the real outcome — §5.3.)

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-jira-create-epic Phase-2-MCP-Call ok`

### Step 5 — Sync Jira Key Back to Local File

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-jira-create-epic Phase-3-Sync-Back`

1. Capture the returned Jira issue key (e.g., `PROJ-123`)
2. Read the epic markdown file
3. Replace the `**Chave Jira:**` value:
   - If current value is `<CHAVE-JIRA>`: replace with the actual key
   - If current value is `—`: replace with the actual key
   - If creating a new epic (user chose to ignore existing key): replace with the new key
4. Write the updated file

### Step 6 — Report

Output:
```
Epico criado no Jira: {JIRA_KEY}
Projeto: {PROJECT_KEY}
Arquivo local atualizado: {EPIC_FILE_PATH}
Label de sync: epic-XXXX
```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-jira-create-epic Phase-3-Sync-Back ok`

## Error Handling

| Scenario | Action |
|----------|--------|
| MCP tool not available | Abort with clear message about MCP configuration |
| No Atlassian sites found | Abort with credential check message |
| Issue creation fails | Report the error, do NOT update the local file |
| File write fails | Report error, mention Jira key was created but local file not updated |
| Epic file not found | Abort with message: "Epic file not found at {path}" |
| Epic file unparseable | Abort with message: "Cannot parse epic file — verify format" |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-epic-create | reads | Reads the epic file generated by this skill |
| x-epic-decompose | called-by | Orchestrator may invoke this in Phase B |
| x-jira-create-stories | calls | Creates Jira stories linked to the epic |

## ID Synchronization Strategy

Bidirectional lookup is enabled by:
- **Local to Jira**: The local ID (`epic-XXXX`) is stored as a Jira label
- **Jira to Local**: The Jira key (e.g., `PROJ-123`) is stored in the `**Chave Jira:**` field
- **JQL Lookup**: `labels = "epic-XXXX" AND labels = "generated-by-ia-dev-env"`
