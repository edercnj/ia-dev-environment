# x-git-cleanup-branches

> Fetches with prune, removes all non-main worktrees, and deletes every local branch except `main`, `master`, `develop`.

| | |
|---|---|
| **Category** | Git/Release |
| **Invocation** | `/x-git-cleanup-branches [--dry-run] [--yes]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Collapses the "reset local git state" workflow into a single command. After a batch of PRs merges, local branches and worktrees pile up — this skill sweeps them in one destructive pass, guarded by an interactive `y/N` confirmation. Remote state is untouched: only `fetch --prune origin` is performed.

## Usage

```
/x-git-cleanup-branches
/x-git-cleanup-branches --dry-run
/x-git-cleanup-branches --yes
```

## Workflow

1. Validate flags (`--dry-run` and `--yes` are mutually exclusive).
2. Abort if running inside a `.claude/worktrees/*` directory (Rule 14 non-nesting invariant).
3. Resolve current branch (empty when HEAD is detached).
4. `git fetch --prune origin` (skipped with warning if no `origin`).
5. Enumerate non-main worktrees via `git worktree list --porcelain`.
6. Enumerate local branches excluding the protected set (`main`, `master`, `develop`).
7. Print the candidate plan; exit 0 if nothing to do or `--dry-run` is set.
8. Ask `Proceed? [y/N]` unless `--yes` is set.
9. If HEAD is a candidate, `git checkout develop` (fallback `main`) before deletion.
10. `git worktree remove --force` each candidate, then `git worktree prune`.
11. `git branch -D` each candidate branch.
12. Report removed/deleted counts and exit 0.

## Safety

- Protected set is literal: `main`, `master`, `develop`. No substring matches.
- Refuses to run from inside a `.claude/worktrees/*` directory.
- Confirmation gate unless `--yes` or `--dry-run`.
- Never pushes, force-pushes, or mutates remote state.

## See Also

- [x-git-worktree](../x-git-worktree/) — targeted worktree operations with MERGED/STALE/ORPHAN criteria
- [x-git-push](../x-git-push/) — commit + push + PR creation
- [x-git-commit](../x-git-commit/) — atomic Conventional Commits
