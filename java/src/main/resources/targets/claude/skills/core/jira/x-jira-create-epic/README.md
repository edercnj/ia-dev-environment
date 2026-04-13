# x-jira-create-epic

> Creates a Jira Epic from an existing local epic markdown file. Reads the epic file, maps fields to Jira, creates the issue, and syncs the Jira key back to the local file.

| | |
|---|---|
| **Category** | Jira Integration |
| **Invocation** | `/x-jira-create-epic [EPIC_FILE_PATH]` |
| **Reads** | jira-field-mapping |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Creates a Jira Epic from an existing local `epic-XXXX.md` file by extracting the title, overview, and metadata, then calling the Atlassian MCP server to create the issue. Syncs the Jira key back to the local file for bidirectional traceability. Uses labels (`generated-by-ia-dev-env`, `epic-XXXX`) for JQL-based lookup between local and Jira identifiers.

## Usage

```
/x-jira-create-epic plans/epic-0012/epic-0012.md
/x-jira-create-epic
```

## Workflow

1. **Parse** -- Accept epic file path (or prompt for it) and extract title, ID, overview, and Jira key
2. **Pre-check** -- Verify MCP tool availability and check for existing Jira key
3. **Project** -- Discover Atlassian sites and prompt user to select a Jira project
4. **Create** -- Create Epic issue in Jira via MCP with labels for sync
5. **Sync** -- Write the returned Jira key back to the local epic file
6. **Report** -- Output creation summary with Jira key and project

## See Also

- [x-jira-create-stories](../x-jira-create-stories/) -- Creates Jira Stories linked to the epic
- [x-epic-create](../x-epic-create/) -- Generates the local epic file from a spec
- [x-epic-decompose](../x-epic-decompose/) -- Full decomposition including epic and stories
