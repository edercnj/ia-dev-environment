---
name: x-jira-create-stories
description: >
  Creates Jira Stories from existing local story markdown files. Reads all story files
  in an epic directory, maps fields to Jira, creates issues with parent epic link,
  creates dependency links between stories, and syncs Jira keys back to local files.
  Use when the user has existing story files and wants to create them in Jira, or when
  the user says "create stories in Jira", "sync stories to Jira", or "push stories to Jira".
---

# Create Jira Stories from Local Files

This skill creates Jira Stories from existing local story markdown files. It reads all
story files in an epic directory, maps fields to Jira API parameters, creates issues with
parent epic links, establishes dependency links between stories, and syncs Jira keys back
to local files for bidirectional traceability.

## When to Use

- User has existing `story-XXXX-YYYY.md` files and wants to create them in Jira
- User says "create stories in Jira", "sync stories to Jira", "push stories to Jira"
- After running `/x-story-create` or `/x-epic-decompose` without Jira integration

## Prerequisites

Read the field mapping reference before creating issues:
- `.github/skills/x-jira-create-epic/references/jira-field-mapping.md`

## Workflow

### Step 1 — Input & Discovery

1. Accept the epic directory path or epic ID as argument. If not provided, ask the user
2. Verify the directory exists
3. Glob for `story-XXXX-*.md` files in the directory

### Step 2 — Parse All Stories

For each story file, extract:
- **Title**: From `# História: <título>` (line 1)
- **Local ID**: From `**ID:**` field
- **Existing Jira Key**: From `**Chave Jira:**` field
- **Description**: Section 3 text (user story + technical context)
- **Value Delivery**: Section 3.5 (Entrega de Valor)
- **Dependencies**: Section 1 (Blocked By column)

### Step 3 — Filter & Confirm

1. Classify stories: already in Jira vs to create
2. If all stories already have Jira keys, exit
3. Present summary and ask for confirmation

### Step 4 — Epic Link Discovery

1. Read `epic-XXXX.md` from the same directory
2. Extract the epic's `**Chave Jira:**` value
3. If no Jira key: ask whether to create without link, provide key manually, or cancel

### Step 5 — Project Selection

1. Call `mcp__atlassian__getAccessibleAtlassianResources` to discover Atlassian sites
2. Call `mcp__atlassian__getVisibleJiraProjects` with the discovered `cloudId`
3. Present project list and capture selected `projectKey`

### Step 6 — Create Stories in Jira

Process stories in dependency order. For each story:
1. Build description by concatenating Section 3 + Section 3.5 (Entrega de Valor)
2. Call `mcp__atlassian__createJiraIssue` with Story type, parent epic link, and labels
3. Capture Jira key and update local story file

### Step 7 — Dependency Linking (Second Pass)

After all stories are created, create dependency links using `mcp__atlassian__createIssueLink`
with type "Blocks" for each Blocked By relationship.

### Step 8 — Update Implementation Map

If `IMPLEMENTATION-MAP.md` exists, update the Jira key column for each created story.

### Step 9 — Report

Output summary table with story ID, title, Jira key, and status for each story.
Include counts for created, linked, and failed stories.

## Error Handling

- **MCP not available**: Abort with clear message
- **Individual story creation fails**: Log warning, continue with remaining stories
- **Link creation fails**: Log warning, continue (best-effort)
- **File write fails**: Log warning, mention Jira key created but file not updated

## ID Synchronization

- **Local → Jira**: Local ID stored as Jira label for JQL lookup
- **Jira → Local**: Jira key stored in `**Chave Jira:**` field
