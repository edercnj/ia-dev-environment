---
name: x-git-worktree
description: >
  Manages git worktrees for parallel task and story execution.
  Operations: create, list, remove, cleanup. Follows RULE-018
  (Worktree Lifecycle) naming convention under
  .claude/worktrees/{identifier}/.
  Reference: `.github/skills/x-git-worktree/SKILL.md`
---

# Skill: Git Worktree Management

## Purpose

Manages git worktrees for {{PROJECT_NAME}} to enable parallel task and story execution. Provides create, list, remove, and cleanup operations following the RULE-018 worktree lifecycle convention.

## Triggers

- `/x-git-worktree create --branch feat/story-XXXX --base develop` -- create a worktree
- `/x-git-worktree list` -- list all active worktrees
- `/x-git-worktree remove --id agent-abc123` -- remove a specific worktree
- `/x-git-worktree cleanup` -- remove all stale worktrees
- `/x-git-worktree create --branch feat/story-XXXX --dry-run` -- preview without creating

## Arguments

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `<operation>` | String | Yes | One of: create, list, remove, cleanup |
| `--branch` | String | No | Branch name for the worktree (create only) |
| `--base` | String | No | Base branch to create from (default: develop) |
| `--id` | String | No | Worktree identifier (remove only) |
| `--dry-run` | Flag | No | Preview operation without executing |

## Workflow

```
1. PARSE      -> Validate operation and arguments
2. EXECUTE    -> Perform git worktree operation
3. VALIDATE   -> Verify worktree state
4. REPORT     -> Output operation summary
```

## Naming Convention (RULE-018)

Worktrees are created under `.claude/worktrees/{identifier}/` where `{identifier}` is a unique agent or task identifier.

## Error Handling

| Scenario | Action |
|----------|--------|
| Branch already exists | ABORT with branch name hint |
| Worktree path occupied | ABORT with path conflict details |
| Invalid identifier | ABORT with naming convention hint |
| Base branch not found | ABORT with available branches |

## Integration Notes

- Used by x-epic-implement for parallel story execution
- Used by x-story-implement for isolated task implementation
- Follows Git Flow branching model (RULE-009)
