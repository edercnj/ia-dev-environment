# x-git-worktree

> Manages git worktrees for parallel task and story execution. Operations: create, list, remove, cleanup, detect-context. Follows Rule 14 (Worktree Lifecycle) naming convention under .claude/worktrees/{identifier}/.

| | |
|---|---|
| **Category** | Git/Workflow |
| **Invocation** | `/x-git-worktree <create\|list\|remove\|cleanup\|detect-context> [options]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Centralizes git worktree lifecycle management for parallel development workflows. Creates isolated working directories under `.claude/worktrees/` for tasks and stories, tracks their status (ACTIVE, STALE, MERGED, ORPHAN), and provides automatic cleanup of obsolete worktrees based on three criteria: branch merged, inactivity exceeding 7 days, or branch deletion.

## Usage

```
/x-git-worktree create --branch feat/task-0029-0001-001-domain --base develop
/x-git-worktree list
/x-git-worktree remove --id task-0029-0001-001
/x-git-worktree cleanup --dry-run
/x-git-worktree cleanup
/x-git-worktree detect-context
```

## Operations

1. **create** -- Create a new worktree with branch validation and protected branch enforcement
2. **list** -- Display all worktrees with ID, branch, status, last commit, and age
3. **remove** -- Remove a specific worktree by identifier
4. **cleanup** -- Remove obsolete worktrees (MERGED, STALE, ORPHAN) with optional dry-run
5. **detect-context** -- Read-only probe that returns the current worktree context as JSON (`{inWorktree, worktreePath, mainRepoPath}`); used by skills that must decide whether to create a new worktree or reuse the current one (Rule 14 §3)

## See Also

- [x-epic-implement](../x-epic-implement/) -- Uses worktrees for parallel story execution
- [x-story-implement](../x-story-implement/) -- Executes within worktree directories
- [x-git-push](../x-git-push/) -- Branch creation and push from within worktrees
