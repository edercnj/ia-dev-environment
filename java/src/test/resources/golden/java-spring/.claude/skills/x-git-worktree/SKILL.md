---
name: x-git-worktree
model: haiku
description: "Manages git worktrees for parallel task and story execution. Operations: create, list, remove, cleanup, detect-context. Follows Rule 14 (Worktree Lifecycle) naming convention under .claude/worktrees/{identifier}/."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "<create|list|remove|cleanup|detect-context> [--branch <name>] [--base <base>] [--id <identifier>] [--dry-run]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## Triggers

```
/x-git-worktree create --branch feat/task-0049-0001-001-domain [--base develop] [--id task-0049-0001-001]
/x-git-worktree list
/x-git-worktree remove --id task-0049-0001-001
/x-git-worktree cleanup [--dry-run]
/x-git-worktree detect-context
```

## Parameters

| Operation | Parameter | Type | Required | Default | Description |
|-----------|-----------|------|----------|---------|-------------|
| `create` | `--branch` | string | Yes | — | Branch name for the worktree |
| `create` | `--base` | string | No | `develop` | Base branch to create the new branch from |
| `create` | `--id` | string | No | derived from branch (strips `feat/`, `fix/`, `hotfix/`, `refactor/` prefixes) | Worktree directory identifier |
| `remove` | `--id` | string | Yes | — | Worktree identifier to remove |
| `cleanup` | `--dry-run` | boolean | No | `false` | Preview candidates without removing |
| `detect-context` | (none) | — | — | — | Returns JSON: `{inWorktree, worktreePath, mainRepoPath}` |

All worktrees are placed under `.claude/worktrees/{identifier}/`. Protected branches (`main`, `develop`) cannot host worktrees. Creator-owns-removal invariant enforced per Rule 14 §5 — only the skill that called `create` may call `remove` for the same worktree.

## Output Contract

| Operation | Output |
|-----------|--------|
| `create` | Prints absolute worktree path on success; exits 1 on protected branch or conflict |
| `list` | Markdown table: ID, Branch, Status (`ACTIVE`/`STALE`/`MERGED`/`ORPHAN`), Last Commit, Age |
| `remove` | Confirmation message on success; exits 1 if `--id` not found |
| `cleanup` | Summary table of removed worktrees; `--dry-run` prints candidates only |
| `detect-context` | Single-line JSON: `{"inWorktree":bool,"worktreePath":string|null,"mainRepoPath":string}` |

**detect-context** is read-only and safe to call from any context — used by orchestrators to decide REUSE | CREATE | LEGACY branching (Rule 14 §3 Non-Nesting Invariant).

## Error Envelope

| Code | Condition | Message |
|------|-----------|---------|
| `PROTECTED_BRANCH` | `create` on `main` or `develop` | `Cannot create worktree on protected branch (<name>)` |
| `BRANCH_CONFLICT` | Branch already checked out in another worktree | `Branch '<name>' is already an active worktree` |
| `WORKTREE_EXISTS` | Target directory already exists | `Worktree directory already exists: .claude/worktrees/<id>` |
| `NOT_FOUND` | `remove --id` not found | `Worktree '<id>' not found` |
| `GIT_ADD_FAILED` | `git worktree add` non-zero | Git error detail; suggest manual cleanup |
| `GIT_REMOVE_FAILED` | `git worktree remove` non-zero (retried with `--force`) | Git error detail |
| `NOT_A_REPO` | `detect-context` run outside a git repo | `{"error":"NOT_A_REPO"}` to stderr; exit 1 |
| `NO_OBSOLETE` | `cleanup` finds nothing | `No obsolete worktrees found` (informational, exit 0) |

## Full Protocol

> Complete per-operation Bash implementations (create 7-step workflow, list status classification, cleanup criteria — MERGED/STALE/ORPHAN, detect-context canonical snippet with CWE-116/CWE-209 hardening and JSON-escape), Git Flow integration table (task/story/hotfix base-branch rules), post-merge lifecycle, naming convention derivation, and Integration Notes (x-epic-implement/x-story-implement/x-task-implement callers) in [`references/full-protocol.md`](references/full-protocol.md).
