> Returns to [slim body](../SKILL.md) after reading the required operation.

# x-git-worktree — Full Protocol

## Operation 1: create

### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--branch` | string | Yes | — | Branch name for the worktree |
| `--base` | string | No | `develop` | Base branch to create new branch from |
| `--id` | string | No | derived | Worktree directory identifier |

### Workflow

```
1. VALIDATE    -> Check branch not protected (main/develop) → PROTECTED_BRANCH
2. VALIDATE    -> Check branch not already an active worktree → BRANCH_CONFLICT
3. FETCH       -> git fetch origin
4. BRANCH      -> Create branch from --base if absent: git branch <branch> <base>
5. WORKTREE    -> git worktree add .claude/worktrees/<id>/ <branch>
6. VERIFY      -> Confirm .claude/worktrees/<id>/ and <id>/.git exist
7. OUTPUT      -> Print absolute path of worktree
```

### Identifier Derivation

When `--id` is absent, strip common prefixes from `--branch`:

```bash
derive_id() {
  local id="${1#feat/}"; id="${id#feature/}"; id="${id#fix/}"
  id="${id#hotfix/}"; id="${id#refactor/}"; echo "$id"
}
```

---

## Operation 2: list

### Status Classification

| Status | Condition |
|--------|-----------|
| `ACTIVE` | Branch exists, last commit ≤ 7 days old, not merged |
| `STALE` | Branch exists, last commit > 7 days old |
| `MERGED` | Branch merged into target (`develop` or `main`) |
| `ORPHAN` | Branch does not exist locally or on remote |

### Output Format

```
| ID                 | Branch                         | Status | Last Commit      | Age  |
| task-0049-0001-001 | feat/task-0049-0001-001-domain | ACTIVE | 2026-04-07 10:30 | 2h   |
```

### Implementation

```bash
for dir in ".claude/worktrees"/*/; do
  [ -d "$dir" ] || continue
  ID=$(basename "$dir")
  BRANCH=$(git -C "$dir" rev-parse --abbrev-ref HEAD 2>/dev/null)
  LAST_COMMIT=$(git -C "$dir" log -1 --format="%ci" 2>/dev/null)
  STATUS=$(classify_status "$BRANCH" "$LAST_COMMIT")
  echo "| $ID | $BRANCH | $STATUS | $LAST_COMMIT | $(age "$LAST_COMMIT") |"
done
```

---

## Operation 3: remove

```bash
WORKTREE_DIR=".claude/worktrees/$IDENTIFIER"

# 1. Verify worktree exists
[ -d "$WORKTREE_DIR" ] || { echo "Worktree '$IDENTIFIER' not found" >&2; exit 1; }

# 2. Remove via git worktree
git worktree remove "$WORKTREE_DIR" --force

# 3. Prune stale references
git worktree prune

# 4. Verify removal
[ ! -d "$WORKTREE_DIR" ] && echo "Worktree '$IDENTIFIER' removed" \
  || { echo "Failed to remove '$IDENTIFIER'" >&2; exit 1; }
```

---

## Operation 4: cleanup

### Cleanup Criteria

| Criterion | Condition | Action |
|-----------|-----------|--------|
| `MERGED` | Branch merged into develop or main | Remove worktree + delete local branch |
| `STALE` | Last commit > 7 days | Remove worktree (preserve branch) |
| `ORPHAN` | Branch absent locally and remotely | Remove worktree |

### Detection Helpers

```bash
is_merged() { git branch --merged "${2:-develop}" | grep -q "$1"; }

is_stale() {
  local last_epoch=$(git log -1 --format="%ct" "$1" 2>/dev/null)
  [ $(($(date +%s) - last_epoch)) -gt $((7 * 24 * 3600)) ]
}

is_orphan() {
  git rev-parse --verify "$1" >/dev/null 2>&1 && return 1
  git ls-remote --heads origin "$1" | grep -q "$1" && return 1
  return 0
}
```

### Dry-run Output

```
Cleanup candidates:
| ID                 | Status | Reason                     |
| task-0049-0001-001 | MERGED | Branch merged into develop  |
| task-0049-0003-002 | STALE  | No commits for 12 days     |

Total: 2 worktrees would be removed (omit --dry-run to execute)
```

---

## Operation 5: detect-context

Read-only context probe used by orchestrators to decide REUSE | CREATE | LEGACY (Rule 14 §3 Non-Nesting Invariant).

### Canonical Bash Snippet (copy-pasteable)

```bash
detect_worktree_context() {
  local toplevel main_repo wt_path in_wt="false"
  toplevel=$(git rev-parse --show-toplevel 2>/dev/null) || {
    echo '{"error":"NOT_A_REPO"}' >&2; return 1
  }
  json_escape() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'; }

  if printf '%s' "$toplevel" | grep -q "/\.claude/worktrees/"; then
    in_wt="true"
    wt_path=$(json_escape "$toplevel")
    if ! main_repo=$(git worktree list --porcelain 2>/dev/null \
        | awk '/^worktree/{print $2; exit}') || [ -z "$main_repo" ]; then
      main_repo="$toplevel"
    fi
    main_repo=$(json_escape "$main_repo")
    printf '{"inWorktree":%s,"worktreePath":"%s","mainRepoPath":"%s"}\n' \
      "$in_wt" "$wt_path" "$main_repo"
  else
    main_repo=$(json_escape "$toplevel")
    printf '{"inWorktree":%s,"worktreePath":null,"mainRepoPath":"%s"}\n' \
      "$in_wt" "$main_repo"
  fi
}
```

### Security Notes

- **CWE-116:** `json_escape()` escapes `\` and `"` in all path strings before JSON interpolation.
- **CWE-209:** Consumers that log `worktreePath`/`mainRepoPath` MUST redact home-directory prefixes (`/Users/<name>`, `/home/<name>`) to avoid leaking usernames.
- **Symlinks:** `git rev-parse --show-toplevel` returns canonical resolved paths — consistent with Rule 14's `/.claude/worktrees/` convention.

### Sample Outputs

```json
// Main repo:   {"inWorktree":false,"worktreePath":null,"mainRepoPath":"/dev/repo"}
// In worktree: {"inWorktree":true,"worktreePath":"/dev/repo/.claude/worktrees/story-0037","mainRepoPath":"/dev/repo"}
```

---

## Git Flow Integration (RULE-005)

| Context | Base Branch | Branch Prefix |
|---------|-------------|---------------|
| Task (from develop) | `develop` | `feat/task-XXXX-YYYY-NNN-description` |
| Story (from develop) | `develop` | `feat/story-XXXX-YYYY-description` |
| Hotfix | `main` | `hotfix/description` |

Protected branches (`main`, `develop`) cannot host worktrees.

### Post-Merge Lifecycle

1. PR merged → worktree becomes `MERGED` cleanup candidate.
2. `cleanup` removes worktree + deletes local branch.
3. GitHub deletes remote branch after merge (if auto-delete configured).

---

## Worktree Lifecycle (Rule 14 §5 — Creator-Owns-Removal)

| Who created | Who may remove |
|-------------|----------------|
| `x-story-implement` (standalone `--worktree`) | `x-story-implement` Phase 3 |
| `x-task-implement` (standalone `--worktree`) | `x-task-implement` Step 5 |
| `x-epic-implement` (parallel story dispatch) | `x-epic-implement` post-merge |
| Human operator | Human operator or `x-git-cleanup-branches` |

Nested worktree creation is forbidden — `detect-context` must return `inWorktree=false` before any `create` call. Failure to preserve this invariant causes `.git` resolution failures.

---

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-epic-implement` | caller | Parallel story dispatch: explicit create/remove per story (ADR-0004 §D2). |
| `x-story-implement` | caller | Phase 0 detect-context + three-way REUSE/CREATE/LEGACY mode; Phase 3 optional cleanup. |
| `x-task-implement` | caller | Step 0.5 detect-context + three-way mode; Step 5 optional cleanup. |
| `x-git-cleanup-branches` | peer | Cleans ALL non-main worktrees in one pass; designed for full repo reset, not task-level lifecycle. |
