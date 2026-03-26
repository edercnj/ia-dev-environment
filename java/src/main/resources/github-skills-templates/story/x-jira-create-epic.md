---
name: x-jira-create-epic
description: >
  Creates a Jira Epic from an existing local epic markdown file. Reads the epic file,
  maps fields to Jira, creates the issue, and syncs the Jira key back to the local file.
  Use when the user has an existing epic file and wants to create it in Jira, or when
  the user says "create this epic in Jira", "sync epic to Jira", or "push epic to Jira".
---

# Create Jira Epic from Local File

This skill creates a Jira Epic from an existing local epic markdown file. It reads the
epic file, maps fields to Jira API parameters, creates the issue, and syncs the Jira
key back to the local file for bidirectional traceability.

## When to Use

- User has an existing `epic-XXXX.md` file and wants to create it in Jira
- User says "create epic in Jira", "sync epic to Jira", "push epic to Jira"
- After running `/x-story-epic` or `/x-story-epic-full` without Jira integration

## Prerequisites

Read the field mapping reference before creating issues:
- `.github/skills/x-jira-create-epic/references/jira-field-mapping.md`

## Workflow

### Step 1 — Input & Parse

1. Accept the epic file path as argument. If not provided, ask the user
2. Read the epic file completely
3. Extract:
   - **Title**: From `# Épico: <título>` (line 1)
   - **Local ID**: From the directory name (`epic-XXXX`)
   - **Overview**: From Section 1 (Visão Geral)
   - **Existing Jira Key**: From `**Chave Jira:**` field

### Step 2 — Pre-checks

1. Verify Atlassian MCP tools are available (`mcp__atlassian__createJiraIssue`). If not, abort
2. Check existing Jira key. If already linked, ask user whether to create new or skip

### Step 3 — Project Selection

1. Call `mcp__atlassian__getAccessibleAtlassianResources` to discover Atlassian sites
2. Call `mcp__atlassian__getVisibleJiraProjects` with the discovered `cloudId`
3. Present project list and capture selected `projectKey`

### Step 4 — Create Epic in Jira

Call `mcp__atlassian__createJiraIssue` with:
- `cloudId`: discovered cloudId
- `projectKey`: selected project key
- `issueTypeName`: "Epic"
- `summary`: epic title
- `description`: Section 1 (Visão Geral) text content
- `contentFormat`: "markdown"
- `additional_fields`: labels `["generated-by-ia-dev-env", "epic-XXXX"]`

### Step 5 — Sync Jira Key Back

1. Capture the returned Jira issue key
2. Update the `**Chave Jira:**` value in the epic markdown file
3. Write the updated file

### Step 6 — Report

Output the created Jira key, project, updated file path, and sync label.

## Error Handling

- **MCP not available**: Abort with clear message
- **No Atlassian sites**: Abort with credential check message
- **Issue creation fails**: Report error, do NOT update local file
- **File write fails**: Report error, mention Jira key was created but file not updated

## ID Synchronization

- **Local → Jira**: Local ID stored as Jira label for JQL lookup
- **Jira → Local**: Jira key stored in `**Chave Jira:**` field
