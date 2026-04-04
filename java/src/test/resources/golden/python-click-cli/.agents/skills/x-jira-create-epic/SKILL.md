---
name: x-jira-create-epic
description: >
  Creates a Jira Epic from an existing local epic markdown file. Reads the epic file,
  maps fields to Jira, creates the issue, and syncs the Jira key back to the local file.
  Use when the user has an existing epic file and wants to create it in Jira, or when
  the user says "create this epic in Jira", "sync epic to Jira", or "push epic to Jira".
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion
argument-hint: "[EPIC_FILE_PATH]"
---

## Global Output Policy

- **Language**: English ONLY for technical output. User-facing content (including prompts, summaries, and reports) may use pt-BR.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Create Jira Epic from Local File

## When to Use

- User has an existing `epic-XXXX.md` file and wants to create it in Jira
- User says "create epic in Jira", "sync epic to Jira", "push epic to Jira"
- After running `/x-story-epic` or `/x-story-epic-full` without Jira integration

## Prerequisites

Read the field mapping reference before creating issues:
- `.claude/skills/x-jira-create-epic/references/jira-field-mapping.md`

## Workflow

### Step 1 — Input & Parse

1. Accept the epic file path as argument. If not provided, ask:
   ```
   question: "Qual o caminho do arquivo do épico? (ex: plans/epic-0012/epic-0012.md)"
   header: "Epic File"
   ```
2. Read the epic file completely
3. Extract:
   - **Title**: From `# Épico: <título>` (line 1)
   - **Local ID**: From the directory name (`epic-XXXX`)
   - **Overview**: From Section 1 (Visão Geral) — all text after `**Chave Jira:**` line until Section 2
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
   question: "Este épico já tem chave Jira: {KEY}. O que deseja fazer?"
   header: "Épico já vinculado ao Jira"
   options:
     - label: "Criar novo épico no Jira"
       description: "Ignorar a chave existente e criar um novo issue"
     - label: "Pular — manter a chave existente"
       description: "Não criar no Jira, manter o vínculo atual"
   ```
   If "Pular": exit with message "Épico mantido com chave {KEY}."

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
   question: "Selecione o projeto Jira para criar o épico:"
   header: "Projeto Jira"
   options:
     - label: "{PROJECT_KEY} — my-cli-tool"
       description: "Project key: {PROJECT_KEY}"
     (one option per project)
   ```
5. Capture the selected `projectKey`

### Step 4 — Create Epic in Jira

Call `mcp__atlassian__createJiraIssue` with:
- `cloudId`: discovered cloudId
- `projectKey`: selected project key
- `issueTypeName`: "Epic"
- `summary`: epic title (extracted from `# Épico: <título>`)
- `description`: Section 1 (Visão Geral) text content
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

### Step 5 — Sync Jira Key Back to Local File

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
Épico criado no Jira: {JIRA_KEY}
Projeto: {PROJECT_KEY}
Arquivo local atualizado: {EPIC_FILE_PATH}
Label de sync: epic-XXXX
```

## Error Handling

- **MCP not available**: Abort with clear message
- **No Atlassian sites**: Abort with credential check message
- **Issue creation fails**: Report the error, do NOT update the local file
- **File write fails**: Report the error, mention the Jira key was created but local file not updated

## ID Synchronization Strategy

Bidirectional lookup is enabled by:
- **Local → Jira**: The local ID (`epic-XXXX`) is stored as a Jira label
- **Jira → Local**: The Jira key (e.g., `PROJ-123`) is stored in the `**Chave Jira:**` field
- **JQL Lookup**: `labels = "epic-XXXX" AND labels = "generated-by-ia-dev-env"`
