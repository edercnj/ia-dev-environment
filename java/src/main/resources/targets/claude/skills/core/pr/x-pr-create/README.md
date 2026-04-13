# x-pr-create

> Task-level PR creation with formatted title, automatic labels, structured body, and target branch logic. Creates standardized PRs for individual tasks with Task ID traceability.

| | |
|---|---|
| **Category** | Git/Release |
| **Invocation** | `/x-pr-create TASK-XXXX-YYYY-NNN [--auto-approve-pr] [--draft] [--description "desc"]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Creates standardized Pull Requests for individual tasks with Task ID in the title (`feat(TASK-XXXX-YYYY-NNN): description`), automatic labels (`task`, `story-XXXX-YYYY`, `epic-XXXX`), structured body with task details table, commit list, and review checklist. Validates branch naming, runs tests before PR creation, and supports draft mode and auto-approve targeting.

## Usage

```
/x-pr-create TASK-0029-0001-001
/x-pr-create TASK-0029-0001-001 --draft
/x-pr-create TASK-0029-0001-001 --auto-approve-pr
/x-pr-create TASK-0029-0001-001 --description "add user validation"
```

## Flags

| Flag | Description |
|------|-------------|
| `--auto-approve-pr` | Target parent story branch instead of `develop` |
| `--draft` | Create PR as draft with `[DRAFT]` banner |
| `--description` | Override PR title description (default: derived from branch name) |

## PR Title Format

```
feat(TASK-XXXX-YYYY-NNN): short description
```

- Maximum 70 characters
- Truncated with `...` if exceeding limit
- Follows Conventional Commits format

## Labels

| Label | Color | Description |
|-------|-------|-------------|
| `task` | `#0075ca` | Individual task PR |
| `story-XXXX-YYYY` | `#e4e669` | Parent story reference |
| `epic-XXXX` | `#d73a4a` | Parent epic reference |

Labels are auto-created via `gh label create` if they do not exist.

## Target Branch Logic

| Mode | Target Branch |
|------|---------------|
| Default | `develop` |
| `--auto-approve-pr` | Parent story branch (`feat/story-XXXX-YYYY-*`) |

## Workflow

1. **Validate** -- Parse task ID and verify branch naming convention
2. **Pre-check** -- Run `{{TEST_COMMAND}}` to ensure tests pass
3. **Title** -- Format PR title (<=70 chars, Conventional Commits)
4. **Body** -- Generate structured body with task details, commits, checklist
5. **Labels** -- Create missing labels via `gh label create`
6. **Create** -- Create PR via `gh pr create` with all metadata

## See Also

- [x-git-commit](../x-git-commit/) -- Creates commits with Task ID in scope
- [x-git-push](../x-git-push/) -- General git workflow (branch, commit, push, PR)
- [x-story-implement](../x-story-implement/) -- Full feature lifecycle that delegates PR creation
