---
name: x-pr-create
description: >
  Task-level PR creation with formatted title, automatic labels, structured
  body, and target branch logic. Creates standardized PRs for individual
  tasks with Task ID traceability.
  Reference: `.github/skills/x-pr-create/SKILL.md`
---

# Skill: Task PR Creation

## Purpose

Creates standardized Pull Requests for individual tasks in {{PROJECT_NAME}} with Task ID in the title, automatic labels (`task`, `story-XXXX-YYYY`, `epic-XXXX`), structured body with review checklist, and correct target branch.

## Triggers

- `/x-pr-create TASK-XXXX-YYYY-NNN` -- create PR for the specified task
- `/x-pr-create TASK-XXXX-YYYY-NNN --draft` -- create as draft PR
- `/x-pr-create TASK-XXXX-YYYY-NNN --auto-approve-pr` -- target parent story branch
- `/x-pr-create TASK-XXXX-YYYY-NNN --description "desc"` -- override title description

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `task-id` | String | Yes | Task ID: `TASK-XXXX-YYYY-NNN` |
| `--auto-approve-pr` | Flag | No | Target parent story branch instead of `develop` |
| `--draft` | Flag | No | Create PR as draft |
| `--description` | String | No | Short description for PR title |

## PR Title Format

```
feat(TASK-XXXX-YYYY-NNN): short description
```

Maximum 70 characters. Truncated with `...` if exceeding limit.

## Labels

| Label | Color | Description |
|-------|-------|-------------|
| `task` | `#0075ca` | Individual task PR |
| `story-XXXX-YYYY` | `#e4e669` | Parent story reference |
| `epic-XXXX` | `#d73a4a` | Parent epic reference |

## Target Branch

- **Default**: `develop`
- **`--auto-approve-pr`**: parent story branch (`feat/story-XXXX-YYYY-*`)

## Workflow

1. Validate task ID and branch naming (`feat/task-XXXX-YYYY-NNN-*`)
2. Run `{{TEST_COMMAND}}` to verify tests pass
3. Format PR title (<=70 chars, Conventional Commits)
4. Generate body (summary, task details, commits, review checklist)
5. Create/verify labels via `gh label create`
6. Create PR via `gh pr create`

## Body Template

```markdown
## Summary
{task description}

## Task Details
| Field | Value |
|-------|-------|
| Task ID | TASK-XXXX-YYYY-NNN |
| Story | story-XXXX-YYYY |
| Epic | epic-XXXX |

## Changes
{commit list from git log}

## Review Checklist
- [ ] Tests pass locally
- [ ] Coverage thresholds met
- [ ] TDD commits present
- [ ] No TODO/FIXME/HACK comments
- [ ] Conventional Commits format followed
```
