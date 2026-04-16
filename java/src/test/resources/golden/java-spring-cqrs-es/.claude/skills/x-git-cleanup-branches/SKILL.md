---
name: x-git-cleanup-branches
description: "Cleans local git state in one pass: fetches origin with prune, removes all non-main worktrees (any path), and deletes all local branches except main/master/develop. Destructive by default with an interactive y/N confirmation gate; supports --dry-run (preview) and --yes (non-interactive)."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "[--dry-run] [--yes]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Local Git Cleanup (branches + worktrees + fetch)

## Purpose

Centralizes the "reset local git state" workflow for {{PROJECT_NAME}}: after a batch of merged PRs, stale worktrees and orphan local branches accumulate. This skill does the combined pass in one invocation — fetch with prune, remove every non-main worktree, delete every local branch outside the protected set (`main`, `master`, `develop`).

Destructive by design (the user explicitly asked for a sweep). Safety comes from the confirmation gate (`y/N`) before any deletion and from `--dry-run` preview mode.

## When to Use

- After merging a batch of feature PRs — flush remote-tracking refs and delete the merged locals.
- Before starting a large refactor — clean slate local branches / worktrees.
- When `git worktree list` or `git branch -l` gets noisy.
- NOT for surgical removal — use `/x-git-worktree remove --id <id>` or `git branch -D <name>` directly.

## Triggers

- `/x-git-cleanup-branches` — execute with interactive confirmation
- `/x-git-cleanup-branches --dry-run` — preview candidates, no changes
- `/x-git-cleanup-branches --yes` — execute, skip confirmation (CI / scripted use)

## Protected Set (Hard-Coded)

| Name | Why protected |
|------|---------------|
| `main` | Production branch (Rule 09) |
| `master` | Legacy production alias |
| `develop` | Integration branch (Rule 09) |

The currently checked-out branch (HEAD) is **NOT** in the protected set. If HEAD points at a candidate branch, the skill checks out `develop` (fallback: `main`) before deletion.

## Workflow

```
1.  PARSE FLAGS       -> validate --dry-run / --yes mutual exclusion
2.  DETECT CONTEXT    -> abort if running inside .claude/worktrees/*
3.  RESOLVE HEAD      -> capture current branch (empty if detached)
4.  FETCH             -> git fetch --prune origin (skip if no origin)
5.  ENUMERATE WTS     -> list non-main worktrees via git worktree list --porcelain
6.  ENUMERATE BRS     -> list local branches minus protected set
7.  PRINT PLAN        -> human-readable candidate table
8.  CONFIRM GATE      -> y/N prompt unless --yes / --dry-run
9.  SWITCH IF NEEDED  -> checkout develop/main if HEAD is a candidate
10. REMOVE WORKTREES  -> git worktree remove --force + git worktree prune
11. DELETE BRANCHES   -> git branch -D per candidate
12. REPORT SUMMARY    -> counts + exit 0
```

### Step 1 — Parse Flags

```bash
DRY_RUN=false
ASSUME_YES=false
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    --yes|-y)  ASSUME_YES=true ;;
    -h|--help) echo "Usage: x-git-cleanup-branches [--dry-run] [--yes]"; exit 0 ;;
    *) echo "ERROR: unknown flag: $arg" >&2; exit 2 ;;
  esac
done

if [ "$DRY_RUN" = "true" ] && [ "$ASSUME_YES" = "true" ]; then
  echo "ERROR: --dry-run and --yes are mutually exclusive" >&2
  exit 2
fi
```

### Step 2 — Detect Worktree Context (abort if inside one)

This skill MUST run from the main repository. Running from inside a worktree would attempt to remove the host worktree while executing — unsafe.

This skill extends the canonical `detect_worktree_context()` check from `x-git-worktree` (Rule 14, non-nesting invariant). The canonical snippet only recognises worktrees under `.claude/worktrees/*`; because this skill enumerates and removes **all** non-main worktrees via `git worktree list --porcelain` (any path), the guard also compares `git rev-parse --show-toplevel` to the main worktree path and inspects `git rev-parse --git-dir` for a `worktrees/` suffix, so a linked worktree in any location triggers the abort.

```bash
detect_worktree_context() {
  local toplevel git_dir main_repo wt_path in_wt="false"
  toplevel=$(git rev-parse --show-toplevel 2>/dev/null) || {
    echo '{"error":"NOT_A_REPO"}' >&2
    return 1
  }
  git_dir=$(git rev-parse --git-dir 2>/dev/null) || {
    echo '{"error":"NOT_A_REPO"}' >&2
    return 1
  }
  json_escape() {
    printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
  }

  # Resolve main repo path (first `worktree` entry, stripping the
  # `worktree ` prefix so paths containing spaces are preserved).
  if ! main_repo=$(git worktree list --porcelain 2>/dev/null \
              | sed -n 's/^worktree //p' | head -n 1) \
       || [ -z "$main_repo" ]; then
    main_repo="$toplevel"
  fi

  # Classifier 1 — Rule 14 non-nesting invariant (substring check).
  if printf '%s' "$toplevel" | grep -q "/\.claude/worktrees/"; then
    in_wt="true"
  fi
  # Classifier 2 — git-dir of a linked worktree lives under
  # `<main>/.git/worktrees/<id>/`.
  case "$git_dir" in
    */worktrees/*|.git/worktrees/*) in_wt="true" ;;
  esac
  # Classifier 3 — toplevel differs from the main repo path.
  if [ "$toplevel" != "$main_repo" ]; then
    in_wt="true"
  fi

  if [ "$in_wt" = "true" ]; then
    wt_path=$(json_escape "$toplevel")
    main_repo=$(json_escape "$main_repo")
    printf '{"inWorktree":%s,"worktreePath":"%s","mainRepoPath":"%s"}\n' \
      "$in_wt" "$wt_path" "$main_repo"
  else
    main_repo=$(json_escape "$main_repo")
    printf '{"inWorktree":%s,"worktreePath":null,"mainRepoPath":"%s"}\n' \
      "$in_wt" "$main_repo"
  fi
}

CONTEXT_JSON=$(detect_worktree_context) || exit 1
IN_WT=$(printf '%s' "$CONTEXT_JSON" | grep -o '"inWorktree":[^,]*' | cut -d: -f2)

if [ "$IN_WT" = "true" ]; then
  echo "ERROR: IN_WORKTREE_UNSAFE — must run from main repo, not a worktree" >&2
  exit 1
fi
```

### Step 3 — Resolve Current Branch

```bash
CURRENT_BRANCH=$(git symbolic-ref --short -q HEAD || true)
# Empty string => detached HEAD (safe; no switch needed later)
```

### Step 4 — Fetch With Prune

```bash
if git remote | grep -q '^origin$'; then
  echo "→ git fetch --prune origin"
  git fetch --prune origin || echo "WARNING: fetch failed, continuing"
else
  echo "WARNING: no 'origin' remote configured, skipping fetch"
fi
```

### Step 5 — Enumerate Non-Main Worktrees

The first `worktree <path>` entry in `git worktree list --porcelain` is always the main repository. All subsequent entries are removal candidates. Paths in that output can legally contain spaces, so extract them by stripping the literal `worktree ` prefix (nine characters) rather than by whitespace-splitting — `sed` preserves the full path.

```bash
WORKTREE_CANDIDATES=$(git worktree list --porcelain \
  | sed -n 's/^worktree //p' \
  | tail -n +2)
```

### Step 6 — Enumerate Candidate Branches

```bash
PROTECTED_REGEX='^(main|master|develop)$'
BRANCH_CANDIDATES=$(git for-each-ref --format='%(refname:short)' refs/heads/ \
  | grep -Ev "$PROTECTED_REGEX" || true)
```

`grep -Ev … || true` prevents a non-match (exit 1) from aborting the script under `set -e` style shells.

### Step 7 — Print Plan

```bash
echo ""
echo "=== Cleanup Plan ==="
echo ""
echo "Worktrees to remove (main worktree preserved):"
if [ -z "$WORKTREE_CANDIDATES" ]; then
  echo "  (none)"
else
  while IFS= read -r wt; do
    [ -n "$wt" ] || continue
    printf '  - %s\n' "$wt"
  done <<EOF
$WORKTREE_CANDIDATES
EOF
fi

echo ""
echo "Local branches to delete (protected: main, master, develop):"
if [ -z "$BRANCH_CANDIDATES" ]; then
  echo "  (none)"
else
  while IFS= read -r br; do
    [ -n "$br" ] || continue
    printf '  - %s\n' "$br"
  done <<EOF
$BRANCH_CANDIDATES
EOF
fi

if [ -z "$WORKTREE_CANDIDATES" ] && [ -z "$BRANCH_CANDIDATES" ]; then
  echo ""
  echo "Nothing to clean. Exiting."
  exit 0
fi

if [ "$DRY_RUN" = "true" ]; then
  echo ""
  echo "Dry-run complete — no changes applied."
  exit 0
fi
```

### Step 8 — Confirmation Gate

```bash
if [ "$ASSUME_YES" != "true" ]; then
  echo ""
  read -r -p "Proceed with deletion? [y/N] " ANS
  case "$ANS" in
    y|Y|yes|YES) ;;
    *) echo "Aborted by user."; exit 0 ;;
  esac
fi
```

### Step 9 — Switch Away From a Candidate HEAD

If the current branch is about to be deleted, git refuses `branch -D`. Switch to `develop` (fallback `main`) first.

```bash
needs_switch=false
if [ -n "$CURRENT_BRANCH" ]; then
  while IFS= read -r b; do
    [ -n "$b" ] || continue
    if [ "$b" = "$CURRENT_BRANCH" ]; then
      needs_switch=true
      break
    fi
  done <<EOF
$BRANCH_CANDIDATES
EOF
fi

if [ "$needs_switch" = "true" ]; then
  if git show-ref --verify --quiet refs/heads/develop; then
    echo "→ git checkout develop (HEAD was on a candidate branch)"
    git checkout develop
  elif git show-ref --verify --quiet refs/heads/main; then
    echo "→ git checkout main (develop missing; falling back)"
    git checkout main
  else
    echo "ERROR: NO_SAFE_FALLBACK_BRANCH — neither develop nor main exists; cannot switch away from $CURRENT_BRANCH" >&2
    exit 1
  fi
fi
```

### Step 10 — Remove Worktrees

Iterate the candidate list with `while IFS= read -r` over a heredoc, so that worktree paths containing spaces (or glob metacharacters) are preserved as a single token. A plain `for` loop would word-split them.

```bash
WT_REMOVED=0
while IFS= read -r wt; do
  [ -n "$wt" ] || continue
  echo "→ git worktree remove --force $wt"
  if git worktree remove --force "$wt"; then
    WT_REMOVED=$((WT_REMOVED + 1))
  else
    echo "WARNING: failed to remove worktree: $wt" >&2
  fi
done <<EOF
$WORKTREE_CANDIDATES
EOF
git worktree prune
```

`--force` ensures worktrees with uncommitted changes are removed. This is intentional — the Print Plan step already showed them to the user.

### Step 11 — Delete Local Branches

```bash
BR_DELETED=0
while IFS= read -r br; do
  [ -n "$br" ] || continue
  echo "→ git branch -D $br"
  if git branch -D "$br"; then
    BR_DELETED=$((BR_DELETED + 1))
  else
    echo "WARNING: failed to delete branch: $br" >&2
  fi
done <<EOF
$BRANCH_CANDIDATES
EOF
```

### Step 12 — Report Summary

```bash
echo ""
echo "=== Summary ==="
echo "Worktrees removed: $WT_REMOVED"
echo "Branches deleted:  $BR_DELETED"
exit 0
```

## Error Handling

| Scenario | Action |
|----------|--------|
| `--dry-run` and `--yes` both set | Abort with exit 2 (usage error) |
| Unknown flag passed | Abort with exit 2 (usage error) |
| Running inside any linked worktree (path anywhere, not just `.claude/worktrees/*`) | Abort with `IN_WORKTREE_UNSAFE`, exit 1 |
| Not a git repo | Abort with `NOT_A_REPO` (from detect-context), exit 1 |
| `origin` remote missing | Warn, skip fetch, continue |
| HEAD is a candidate and neither `develop` nor `main` exists | Abort with `NO_SAFE_FALLBACK_BRANCH`, exit 1 |
| Individual worktree remove fails | Warn, continue, count excludes it |
| Individual branch delete fails | Warn, continue, count excludes it |
| No candidates (worktrees empty and branches empty) | Print "Nothing to clean", exit 0 |

## Security & Safety Notes

- **Blast radius is local-only:** `git fetch` reads from origin; no `push`, no `--force-push`, no tag/remote-branch mutation. Remote state is untouched.
- **Uncommitted work:** `git worktree remove --force` discards uncommitted changes inside secondary worktrees. This is surfaced in the Print Plan step and the user can decline at the confirmation gate.
- **Protected set is literal:** filters use exact regex `^(main|master|develop)$` — no substring matches, no accidental protection of `my-develop-fix`.
- **HEAD in main worktree is preserved implicitly:** `git worktree list` reports the main worktree first and the enumeration skips it.

## Related Skills

| Skill | Relationship |
|-------|-------------|
| `x-git-worktree` | `cleanup` operation is scoped to `.claude/worktrees/*` with MERGED/STALE/ORPHAN criteria; this skill is broader and unconditional. |
| `x-git-push` | Typical upstream action after cleanup (push a fresh branch). |
| `x-git-commit` | Used to author commits — unrelated to cleanup, referenced here only for context. |

## References

- [Rule 09 — Branching Model](../../../rules/09-branching-model.md): protected branches policy
- [Rule 14 — Worktree Lifecycle](../../../rules/14-worktree-lifecycle.md): non-nesting invariant driving step 2
- [x-git-worktree/SKILL.md](../x-git-worktree/SKILL.md): source of the canonical `detect_worktree_context()` snippet
