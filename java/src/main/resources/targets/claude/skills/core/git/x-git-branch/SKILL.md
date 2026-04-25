---
name: x-git-branch
description: "Creates a bare git branch (no worktree) from a configurable base with naming validation and idempotency. Single source of truth for branch creation logic consumed by orchestrators (x-internal-epic-branch-ensure, x-story-implement) and users."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "--name <branch> [--base <branch>] [--push] [--dry-run]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Create Bare Git Branch (x-git-branch)

## Purpose

Single, idempotent entry point for creating a bare git branch (no worktree) from a configurable base. Centralizes three concerns previously duplicated across orchestrators: naming validation (Rule 09), idempotency (same-SHA no-op), and structured error codes. Replaces ad-hoc `git checkout -b` blocks. `x-git-worktree create` is the only other place branch creation is allowed (it additionally runs `git worktree add`).

## Triggers

- `/x-git-branch --name feat/my-feature` — create branch from `develop` (default)
- `/x-git-branch --name epic/0049 --base main` — create epic branch from `main`
- `/x-git-branch --name feat/my-feature --push` — create and push with upstream tracking
- `/x-git-branch --name feat/my-feature --dry-run` — preview without executing

## Parameters

| Argument | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `--name` | string | Yes | — | Target branch name. Must match `^(feat\|fix\|hotfix\|chore\|docs\|refactor\|release\|epic\|planning\|integration)/[a-z0-9-]+$` |
| `--base` | string | No | `develop` | Base branch. Must exist locally. |
| `--push` | boolean | No | `false` | Push `-u origin <name>` after creation |
| `--dry-run` | boolean | No | `false` | Preview only; no side effects |

### Canonical Prefix Set

`feat`, `fix`, `hotfix`, `chore`, `docs`, `refactor`, `release`, `epic`, `planning`, `integration`

These mirror the prefixes documented in Rule 09 (Branching Model) plus three additions used by the epic/planning flow (`epic`, `planning`, `integration`).

## Workflow

```
1. PARSE FLAGS    -> extract --name, --base, --push, --dry-run
2. VALIDATE NAME  -> regex match against canonical prefix set
3. VALIDATE BASE  -> git rev-parse --verify <base>
4. RESOLVE BASE   -> capture baseSha for idempotency check
5. CHECK EXISTING -> git rev-parse --verify <name> (may fail — that is OK)
6. DECIDE         -> new / idempotent no-op / diverged error
7. CREATE BRANCH  -> git branch <name> <base>    (skipped in dry-run)
8. PUSH (OPT)     -> git push -u origin <name>   (when --push, not dry-run)
9. EMIT RESULT    -> structured output: branchName, baseSha, created, alreadyExisted, pushed
```

### Step 1 — Parse Flags

```bash
BRANCH_NAME=""
BASE_BRANCH="develop"
PUSH="false"
DRY_RUN="false"

while [ $# -gt 0 ]; do
  case "$1" in
    --name)    BRANCH_NAME="$2"; shift 2 ;;
    --base)    BASE_BRANCH="$2"; shift 2 ;;
    --push)    PUSH="true"; shift ;;
    --dry-run) DRY_RUN="true"; shift ;;
    *) echo "ERROR: unknown flag: $1" >&2; exit 1 ;;
  esac
done

if [ -z "$BRANCH_NAME" ]; then
  echo "ERROR: --name is required" >&2
  exit 1
fi
```

### Step 2 — Validate Branch Name

Canonical regex (one line, POSIX ERE):

```
^(feat|fix|hotfix|chore|docs|refactor|release|epic|planning|integration)/[a-z0-9-]+$
```

```bash
VALID_REGEX='^(feat|fix|hotfix|chore|docs|refactor|release|epic|planning|integration)/[a-z0-9-]+$'
if ! printf '%s' "$BRANCH_NAME" | grep -Eq "$VALID_REGEX"; then
  echo "ERROR: INVALID_NAME — Branch name must follow <prefix>/<rest> pattern" >&2
  echo "       name='$BRANCH_NAME'" >&2
  exit 1
fi
```

Max length: 100 characters (Rule 09).

### Step 3 — Validate Base Branch Exists

```bash
if ! git rev-parse --verify --quiet "$BASE_BRANCH" >/dev/null; then
  echo "ERROR: BASE_NOT_FOUND — Base branch '$BASE_BRANCH' not found" >&2
  exit 2
fi
BASE_SHA=$(git rev-parse --verify "$BASE_BRANCH")
```

### Step 4 — Check Existing Branch (Idempotency)

```bash
ALREADY_EXISTED="false"
CREATED="true"

if git rev-parse --verify --quiet "$BRANCH_NAME" >/dev/null; then
  ALREADY_EXISTED="true"
  EXISTING_SHA=$(git rev-parse --verify "$BRANCH_NAME")
  if [ "$EXISTING_SHA" = "$BASE_SHA" ]; then
    # Idempotent no-op: branch exists and points at the same SHA as base
    CREATED="false"
  else
    echo "ERROR: BRANCH_EXISTS_DIFFERENT_BASE — Branch '$BRANCH_NAME' exists with different base SHA" >&2
    echo "       existing=$EXISTING_SHA base($BASE_BRANCH)=$BASE_SHA" >&2
    exit 3
  fi
fi
```

### Step 5 — Create Branch (Unless Dry-Run or Idempotent)

```bash
if [ "$DRY_RUN" = "true" ]; then
  echo "[dry-run] git branch $BRANCH_NAME $BASE_BRANCH"
elif [ "$CREATED" = "true" ]; then
  git branch "$BRANCH_NAME" "$BASE_BRANCH"
fi
```

Branch is created **without checkout** (bare). Callers that need a checked-out working tree must use `x-git-worktree create` instead.

### Step 6 — Optional Push

```bash
PUSHED="false"
if [ "$PUSH" = "true" ] && [ "$DRY_RUN" != "true" ]; then
  if ! git push -u origin "$BRANCH_NAME" 2>/tmp/push.err; then
    echo "ERROR: PUSH_FAILED — Push to origin failed" >&2
    cat /tmp/push.err >&2
    exit 4
  fi
  PUSHED="true"
elif [ "$PUSH" = "true" ] && [ "$DRY_RUN" = "true" ]; then
  echo "[dry-run] git push -u origin $BRANCH_NAME"
fi
```

### Step 7 — Emit Structured Result

```bash
printf '{"branchName":"%s","baseSha":"%s","created":%s,"alreadyExisted":%s,"pushed":%s}\n' \
  "$BRANCH_NAME" "$BASE_SHA" "$CREATED" "$ALREADY_EXISTED" "$PUSHED"
```

## Outputs

| Field | Type | Description |
|-------|------|-------------|
| `branchName` | string | The branch name that was requested |
| `baseSha` | string (40) | SHA of `<base>` at the moment the skill ran |
| `created` | boolean | `true` if a new branch was created; `false` for idempotent no-op |
| `alreadyExisted` | boolean | `true` if the branch already existed locally |
| `pushed` | boolean | `true` if `git push -u origin` succeeded |

## Error Codes

| Exit | Code | Condition | Message |
|------|------|-----------|---------|
| 1 | `INVALID_NAME` | `--name` missing or fails regex | `Branch name must follow <prefix>/<rest> pattern` |
| 2 | `BASE_NOT_FOUND` | `--base` does not exist locally | `Base branch '<name>' not found` |
| 3 | `BRANCH_EXISTS_DIFFERENT_BASE` | branch exists at a different SHA | `Branch exists with different base SHA` |
| 4 | `PUSH_FAILED` | `git push -u origin` failed | `Push to origin failed: <stderr>` |

## Error Handling

| Scenario | Action |
|----------|--------|
| `--name` not provided | exit 1 `INVALID_NAME` with usage hint |
| `--name` fails regex | exit 1 `INVALID_NAME`; print offending name |
| `--base` missing locally | exit 2 `BASE_NOT_FOUND`; suggest `git fetch origin` |
| Branch exists, same SHA | exit 0, `created=false`, `alreadyExisted=true` (idempotent) |
| Branch exists, different SHA | exit 3 `BRANCH_EXISTS_DIFFERENT_BASE`; do NOT force |
| `git push` fails (no remote / auth / diverged) | exit 4 `PUSH_FAILED`; stderr carried verbatim |
| `--dry-run` with any flag | preview only, exit 0, `created` reflects the would-be action |

## Examples

```
# Feature branch from develop (default base)
/x-git-branch --name feat/auth-refresh
# -> {"branchName":"feat/auth-refresh","baseSha":"<sha>","created":true,"alreadyExisted":false,"pushed":false}

# Epic branch from main
/x-git-branch --name epic/0049 --base main

# Idempotent re-creation (exit 0, created=false)
/x-git-branch --name feat/auth-refresh

# Dry-run
/x-git-branch --name feat/my-branch --dry-run
# [dry-run] git branch feat/my-branch develop

# Create + push
/x-git-branch --name feat/my-branch --push
```

## Rule References

- **Rule 09** (Branching Model) — canonical prefixes and `<prefix>/<rest>` pattern.
- **RULE-005** (Thin orchestrator) from EPIC-0049 — orchestrators delegate branch creation here; no inline `git checkout -b`.
- **RULE-010** (Skills internas pequenas) from EPIC-0049 — this SKILL.md stays under 250 lines.

## Integration Notes

| Skill | Relationship | Context |
|-------|--------------|---------|
| `x-internal-epic-branch-ensure` | caller (story-0049-0008) | Idempotent ensure of `epic/XXXX`; delegates creation here |
| `x-story-implement` | caller (future refactor — story-0049-0019) | Phase 0 branch creation for standalone stories |
| `x-epic-implement` | caller (future refactor — story-0049-0018) | Epic/planning branch creation |
| `x-git-worktree` | related | `create` operation wraps branch creation + `git worktree add`; uses its own inline logic because worktree add requires non-checkout-conflicting branch state |
| `x-git-push` | related | Pushes an existing branch; this skill optionally invokes `git push` directly for the single-shot create+push case |
