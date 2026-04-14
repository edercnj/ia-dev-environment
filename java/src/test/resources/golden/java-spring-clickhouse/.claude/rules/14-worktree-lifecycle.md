# Rule 14 — Worktree Lifecycle

> **Scope:** Every skill that creates branches via `git checkout -b` or that delegates to `/x-git-worktree`.
> **Ownership:** Platform Team.
> **Related:** Rule 09 (Branching Model / Git Flow), ADR-0004 (Worktree Ownership).

## 1. Naming Convention

| Context | Pattern | Example |
| :--- | :--- | :--- |
| Task | `.claude/worktrees/task-XXXX-YYYY-NNN/` | `.claude/worktrees/task-0037-0003-001/` |
| Story | `.claude/worktrees/story-XXXX-YYYY/` | `.claude/worktrees/story-0037-0003/` |
| Epic Fix | `.claude/worktrees/fix-epic-XXXX/` | `.claude/worktrees/fix-epic-0037/` |
| Release | `.claude/worktrees/release-X.Y.Z/` | `.claude/worktrees/release-2.4.0/` |
| Hotfix | `.claude/worktrees/hotfix-{slug}/` | `.claude/worktrees/hotfix-auth-bug/` |
| Custom | `.claude/worktrees/{identifier}/` | (any unambiguous label) |

When a skill does not receive an explicit `--id`, the identifier MUST be derived from the branch name by stripping common prefixes (`feat/`, `feature/`, `fix/`, `hotfix/`, `refactor/`). The final directory name MUST match one of the patterns above.

## 2. Protected Branches

- `main` and `develop` MUST NEVER be checked out directly in a worktree.
- `/x-git-worktree create` MUST abort with error code `PROTECTED_BRANCH` when `--branch main` or `--branch develop` is passed.
- Worktrees always track a short-lived branch (`feat/*`, `fix/*`, `hotfix/*`, `release/*`, `chore/*`) that will be merged via PR and then removed.

## 3. Non-Nesting Invariant

> **Rule:** No skill MUST create a worktree while it is already executing inside a worktree.

Creating a worktree inside a worktree corrupts the `.git` directory layout, produces misleading `git worktree list` output, and leaks branches that the outer orchestrator believes it owns. Every branch-creating skill MUST detect the current context before invoking `/x-git-worktree create`.

### Canonical Detection Mechanism

The canonical detection routine is exposed by `/x-git-worktree detect-context` (Operation 5 of the `x-git-worktree` skill). Its implementation is:

```bash
TOPLEVEL=$(git rev-parse --show-toplevel)
if printf '%s' "$TOPLEVEL" | grep -F -q "/.claude/worktrees/"; then
  IN_WORKTREE=true
else
  IN_WORKTREE=false
fi
```

> **Fixed-string matching is required:** use `grep -F` so that `.` is treated literally (not as a regex metacharacter) and so that `$TOPLEVEL` is never interpreted as a pattern. Classification relies ONLY on this substring check — do NOT cross-reference `git worktree list` for classification. That matches the normative algorithm documented in `x-git-worktree/SKILL.md` Operation 5 (which uses `git worktree list --porcelain` only to resolve `mainRepoPath` when already classified as inside a worktree).

When `IN_WORKTREE=true`, the caller MUST reuse the existing worktree instead of creating a nested one. Skills dispatched by an orchestrator (e.g., `x-story-implement` dispatched by `x-epic-implement`) MUST always call `detect-context` before any `git checkout -b` or `/x-git-worktree create`.

## 4. Lifecycle

```
[create] ──► [use] ──► [branch merged via PR] ──► [remove]
                                                    ▲
                                                    │
                         [story failed] ──► [preserve for diagnosis]
```

- A worktree is created when the creator skill enters its execution phase.
- The worktree is used through the full TDD / review / PR lifecycle of the artifact it hosts.
- After the backing branch is merged (or explicitly abandoned), the worktree becomes a cleanup candidate.
- Worktrees of failed stories / tasks MUST NOT be auto-removed — they are preserved for diagnosis until a human invokes `/x-git-worktree remove --id <identifier>` or `/x-git-worktree cleanup`.

## 5. Creator Owns Removal

The skill that creates a worktree is the skill responsible for removing it. Subordinate skills that reuse an orchestrator's worktree MUST NOT remove it.

| Skill | Creates worktree? | Who removes? | When? |
| :--- | :--- | :--- | :--- |
| `x-epic-implement` | Yes — one per story (orchestrator) | `x-epic-implement` | After the story PR is merged and auto-rebase finishes |
| `x-story-implement` standalone (`--worktree`) | Yes | `x-story-implement` | End of Phase 3 on success |
| `x-story-implement` orchestrated (invoked by epic) | No — reuses the epic's worktree | `x-epic-implement` | (creator owns) |
| `x-task-implement` standalone (`--worktree`) | Yes | `x-task-implement` | End of task execution |
| `x-task-implement` inside a story | No — reuses the story's worktree | `x-story-implement` or `x-epic-implement` | (creator owns) |
| `x-pr-fix-epic` | Yes | `x-pr-fix-epic` | After the consolidated fix PR is merged |
| `x-release` (post-EPIC-0035) | Yes | `x-release` | After `RESUME-AND-TAG` confirms the git tag |

## 6. When to Use Worktrees

```
┌─ Am I an orchestrator (I launch subagents)?
│   └─ Yes → Create explicit worktrees via /x-git-worktree create BEFORE dispatching subagents.
│
├─ Am I a standalone skill that may run in parallel with other instances?
│   └─ Yes → Expose --worktree as an opt-in flag. Create a worktree only when the flag is present.
│
└─ Am I a subagent inside an orchestrator?
    └─ Yes → Call /x-git-worktree detect-context FIRST. Reuse the orchestrator's worktree.
             NEVER create a nested worktree.
```

Skills that only run sequentially in the main working tree (e.g., `x-code-format`, `x-code-lint`, `x-git-commit`) MUST NOT create worktrees.

## 7. Anti-Patterns

- `Agent(isolation:"worktree")` — **DEPRECATED** (ADR-0004). Use `/x-git-worktree create` explicitly and pass the resulting path to the subagent prompt.
- Calling `git checkout -b` directly in a branch-creating skill without going through `/x-git-worktree` — violates this rule file.
- Creating a worktree on `main` or `develop` — violates Section 2.
- Creating a nested worktree without calling `detect-context` first — violates Section 3.
- Skipping cleanup on standalone success — leaks worktrees under `.claude/worktrees/` and pollutes `git worktree list`.
- A subordinate skill removing the orchestrator's worktree — violates Section 5 (creator owns removal).
- Auto-removing worktrees of failed stories / tasks — violates Section 4 (preserve for diagnosis).
