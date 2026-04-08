# x-jira-create-stories

> Creates Jira Stories from existing local story markdown files. Reads all story files in an epic directory, maps fields to Jira, creates issues with parent epic link, creates dependency links between stories, and syncs Jira keys back to local files.

| | |
|---|---|
| **Category** | Jira Integration |
| **Invocation** | `/x-jira-create-stories [EPIC_DIR_PATH or EPIC_ID]` |
| **Reads** | jira-field-mapping |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Creates Jira Stories from all local `story-XXXX-YYYY.md` files in an epic directory. Processes stories in dependency order, links each to its parent epic, creates Jira issue links for inter-story dependencies (Blocks relationship), and syncs Jira keys back to local files and the implementation map. Skips stories that already have Jira keys and supports partial execution on failure.

## Usage

```
/x-jira-create-stories plans/epic-0012
/x-jira-create-stories 0012
```

## Workflow

1. **Discover** -- Accept epic directory or ID, glob for story files
2. **Parse** -- Extract title, ID, description, value delivery, and dependencies from each story
3. **Filter** -- Classify stories as already-in-Jira or to-create, confirm with user
4. **Epic Link** -- Resolve parent epic Jira key for linking
5. **Project** -- Discover Atlassian sites and prompt user to select a Jira project
6. **Create** -- Create stories in dependency order via MCP, sync keys to local files
7. **Link** -- Second pass to create Blocks dependency links between stories

## Outputs

| Artifact | Path |
|----------|------|
| Updated story files | `plans/epic-XXXX/story-XXXX-YYYY.md` (Jira key synced) |
| Updated implementation map | `plans/epic-XXXX/IMPLEMENTATION-MAP.md` (Jira keys added) |

## See Also

- [x-jira-create-epic](../x-jira-create-epic/) -- Creates the parent epic in Jira first
- [x-story-create](../x-story-create/) -- Generates the local story files from an epic
- [x-story-map](../x-story-map/) -- Generates the implementation map with dependency graph
