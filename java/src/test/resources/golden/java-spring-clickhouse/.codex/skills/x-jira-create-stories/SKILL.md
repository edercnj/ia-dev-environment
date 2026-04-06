---
name: x-jira-create-stories
description: >
  Creates Jira Stories from existing local story markdown files. Reads all story files
  in an epic directory, maps fields to Jira, creates issues with parent epic link,
  creates dependency links between stories, and syncs Jira keys back to local files.
  Use when the user has existing story files and wants to create them in Jira, or when
  the user says "create stories in Jira", "sync stories to Jira", or "push stories to Jira".
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion
argument-hint: "[EPIC_DIR_PATH or EPIC_ID]"
---

## Global Output Policy

- **Language**: English ONLY for technical output. User-facing content (including prompts, summaries, and reports) may use pt-BR.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Create Jira Stories from Local Files

## When to Use

- User has existing `story-XXXX-YYYY.md` files and wants to create them in Jira
- User says "create stories in Jira", "sync stories to Jira", "push stories to Jira"
- After running `/x-story-create` or `/x-story-epic-full` without Jira integration
- To retroactively sync locally-created stories to Jira

## Prerequisites

Read the field mapping reference before creating issues:
- `.claude/skills/x-jira-create-epic/references/jira-field-mapping.md`

## Workflow

### Step 1 — Input & Discovery

1. Accept the epic directory path or epic ID as argument. If not provided, ask:
   ```
   question: "Informe o caminho do diretório do épico ou o ID (ex: plans/epic-0012 ou 0012)"
   header: "Epic Directory"
   ```
2. If only an ID was given (e.g., `0012`), construct the path: `plans/epic-{ID}/`
3. Verify the directory exists. If not:
   ```
   ERROR: Directory {path} not found. Ensure the epic was created first.
   ```
4. Glob for `story-XXXX-*.md` files in the directory
5. If no story files found:
   ```
   ERROR: No story files found in {path}. Run /x-story-create first.
   ```

### Step 2 — Parse All Stories

For each story file, extract:
- **Title**: From `# História: <título>` (line 1)
- **Local ID**: From `**ID:**` field (e.g., `story-0012-0001`)
- **Existing Jira Key**: From `**Chave Jira:**` field
- **Description**: Section 3 text (user story paragraph + technical context)
- **Value Delivery**: Section 3.5 (Entrega de Valor) — Valor Principal, Métrica de Sucesso, Impacto
- **Dependencies**: Section 1 (Blocked By column)

### Step 3 — Filter & Confirm

1. Classify stories:
   - **Already in Jira**: Stories with a real Jira key (not `<CHAVE-JIRA>` or `—`)
   - **To create**: Stories without a Jira key

2. If all stories already have Jira keys:
   ```
   All {N} stories already have Jira keys. Nothing to create.
   ```
   Exit.

3. Report and confirm:
   ```
   question: "Confirma a criação das histórias no Jira?"
   header: "Criar Stories no Jira"
   description: |
     Histórias a criar: {count_to_create}
     Histórias já no Jira (pular): {count_existing}

     Stories a criar:
     - {story-ID}: {title}
     - {story-ID}: {title}
     ...
   options:
     - label: "Sim, criar todas"
       description: "Criar {count_to_create} stories no Jira"
     - label: "Não, cancelar"
       description: "Não criar nada no Jira"
   ```
   If "Não": exit.

### Step 4 — Epic Link Discovery

1. Read `epic-XXXX.md` from the same directory
2. Extract the epic's `**Chave Jira:**` value
3. If the epic has no Jira key (or has `<CHAVE-JIRA>` / `—`):
   ```
   question: "O épico não tem chave Jira. O que deseja fazer?"
   header: "Epic Link"
   options:
     - label: "Criar stories sem vínculo ao épico"
       description: "Stories serão criadas sem parent link"
     - label: "Informar chave do épico manualmente"
       description: "Informe a chave do épico no Jira (ex: PROJ-123)"
     - label: "Cancelar — criar épico primeiro"
       description: "Execute /x-jira-create-epic antes"
   ```
   - If "Criar sem vínculo": proceed without `parent` field
   - If "Informar chave": ask for the key and use it as `parent`
   - If "Cancelar": exit

### Step 5 — Project Selection

1. Call `mcp__atlassian__getAccessibleAtlassianResources` to discover available Atlassian sites
2. Use the first available site's `id` as `cloudId`. If no sites returned:
   ```
   ERROR: No accessible Atlassian sites found. Check your Atlassian credentials.
   ```
   Abort.

3. Call `mcp__atlassian__getVisibleJiraProjects` with the discovered `cloudId`
4. Present the project list to the user:
   ```
   question: "Selecione o projeto Jira para criar as stories:"
   header: "Projeto Jira"
   options:
     - label: "{PROJECT_KEY} — my-spring-clickhouse"
       description: "Project key: {PROJECT_KEY}"
     (one option per project)
   ```
5. Capture the selected `projectKey`

### Step 6 — Create Stories in Jira

Process stories in dependency order (stories with no dependencies first, then those that depend
on already-created stories). This ensures parent epic links and dependency links can be created.

Initialize a mapping: `storyIdToJiraKey = {}`

For each story (in dependency order):

1. **Build description** for Jira by concatenating:
   ```markdown
   {Section 3 — User story paragraph and technical context}

   ---

   ## Entrega de Valor

   - **Valor Principal:** {value from Section 3.5}
   - **Métrica de Sucesso:** {metric from Section 3.5}
   - **Impacto no Negócio:** {impact from Section 3.5}
   ```

2. **Call** `mcp__atlassian__createJiraIssue`:
   - `cloudId`: discovered cloudId
   - `projectKey`: selected project key
   - `issueTypeName`: "Story"
   - `summary`: story title
   - `description`: constructed description (above)
   - `contentFormat`: "markdown"
   - `parent` (optional): epic Jira key (if available from Step 4)
   - `additional_fields`:
     ```json
     {
       "labels": [
         { "name": "generated-by-ia-dev-env" },
         { "name": "story-XXXX-YYYY" }
       ]
     }
     ```

3. **Capture** the returned Jira key
4. **Store** mapping: `storyIdToJiraKey[storyId] = jiraKey`
5. **Update** the story markdown file:
   - Replace `**Chave Jira:** <CHAVE-JIRA>` with `**Chave Jira:** {jiraKey}`
   - Or replace `**Chave Jira:** —` with `**Chave Jira:** {jiraKey}`

6. **On failure**: Log warning, set `<CHAVE-JIRA>` to `—`, continue with next story

### Step 7 — Dependency Linking (Second Pass)

After ALL stories are created and have Jira keys, perform a second pass to create
dependency links:

For each story's "Blocked By" list:
1. Look up the blocker's Jira key in `storyIdToJiraKey` (or from the story file if it was pre-existing)
2. If both the current story and the blocker have Jira keys:
   - Call `mcp__atlassian__createIssueLink`:
     - `cloudId`: discovered cloudId
     - `type`: "Blocks"
     - `inwardIssue`: blocker's Jira key (the issue that blocks)
     - `outwardIssue`: current story's Jira key (the issue that is blocked)
3. If linking fails: log warning, continue (non-blocking, best-effort)

### Step 8 — Update Implementation Map

If `IMPLEMENTATION-MAP.md` exists in the epic directory:
1. Read the file
2. For each created story, find its row in the Section 1 dependency matrix
3. Update the `Chave Jira` column with the actual Jira key
4. Write the updated file

### Step 9 — Report

Output summary:
```
## Resultado da Criação no Jira

| Story | Título | Chave Jira | Status |
|-------|--------|------------|--------|
| story-XXXX-0001 | {title} | PROJ-101 | Criada |
| story-XXXX-0002 | {title} | PROJ-102 | Criada |
| story-XXXX-0003 | {title} | — | Falha: {error} |

**Resumo:**
- Stories criadas: {success_count}/{total_count}
- Links de dependência criados: {link_count}
- Falhas: {failure_count}
- Implementation Map atualizado: {yes/no}
```

## Error Handling

- **MCP not available**: Abort with clear message
- **No Atlassian sites**: Abort with credential check message
- **Individual story creation fails**: Log warning, continue with remaining stories
- **Link creation fails**: Log warning, continue (best-effort)
- **File write fails**: Log warning, mention Jira key was created but file not updated

## ID Synchronization Strategy

Bidirectional lookup is enabled by:
- **Local → Jira**: The local ID (`story-XXXX-YYYY`) is stored as a Jira label
- **Jira → Local**: The Jira key is stored in the `**Chave Jira:**` field
- **JQL Lookup**: `labels = "story-XXXX-YYYY" AND labels = "generated-by-ia-dev-env"`
