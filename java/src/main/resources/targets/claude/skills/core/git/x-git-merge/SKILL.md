---
name: x-git-merge
description: "Merges a source branch into a target branch locally with configurable strategy (merge/squash/rebase), automatic conflict detection + rollback, and idempotent no-op when target already contains source HEAD. Centralizes the ~120 lines of inline Bash previously in x-epic-implement Phase 1.4e auto-rebase."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "--source <branch> --target <branch> [--strategy merge|squash|rebase] [--message <msg>] [--no-push]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Local Git Merge with Conflict Rollback (x-git-merge)

## Purpose

Single, idempotent entry point for merging one local branch into another with three strategies (`merge` / `squash` / `rebase`), automatic conflict detection and rollback, and a structured result payload. Replaces ~120 lines of inline Bash in `x-epic-implement` Phase 1.4e (auto-rebase between parallel stories + `develop → epic/XXXX` sync). Callers receive either `{mergeSha, conflicts:false}` or `{conflicts:true, rolledBack:true, conflictedFiles:[...]}` — never a half-merged working tree.

## Triggers

- `/x-git-merge --source develop --target epic/0049` — default `merge` strategy, push after success
- `/x-git-merge --source develop --target epic/0049 --strategy squash --message "sync develop"` — squash with custom message
- `/x-git-merge --source feat/foo --target develop --strategy rebase` — linear history via rebase
- `/x-git-merge --source develop --target epic/0049 --no-push` — merge locally only

## Parameters

| Argument | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `--source` | string | Yes | — | Source branch to merge from. Must exist locally. |
| `--target` | string | Yes | — | Target branch that receives the merge. Must exist locally. |
| `--strategy` | enum | No | `merge` | One of `merge` (default, `git merge --no-ff`), `squash` (`git merge --squash` + commit), `rebase` (`git rebase`). |
| `--message` | string | No | auto | Commit message for `merge` / `squash`. Ignored for `rebase`. Max 255 chars. |
| `--no-push` | boolean | No | `false` | Skip `git push origin <target>` after a successful merge. |

## Workflow

```
1. PARSE FLAGS     -> extract --source, --target, --strategy, --message, --no-push
2. PRE-CHECKS      -> working tree clean; source exists; target exists
3. CHECKOUT TARGET -> git checkout <target>
4. IDEMPOTENCY     -> git merge-base --is-ancestor <source> <target> → noOp
5. ATTEMPT MERGE   -> dispatch on strategy (merge | squash | rebase)
6. DECIDE          -> success → capture mergeSha; conflict → abort + collect paths
7. PUSH (OPT)      -> git push origin <target> when not --no-push and not noOp
8. EMIT RESULT     -> structured JSON: mergeSha, conflicts, conflictedFiles, rolledBack, noOp
```

### Step 1 — Parse Flags

```bash
SOURCE=""; TARGET=""; STRATEGY="merge"; MESSAGE=""; NO_PUSH="false"
while [ $# -gt 0 ]; do
  case "$1" in
    --source)   SOURCE="$2";   shift 2 ;;
    --target)   TARGET="$2";   shift 2 ;;
    --strategy) STRATEGY="$2"; shift 2 ;;
    --message)  MESSAGE="$2";  shift 2 ;;
    --no-push)  NO_PUSH="true"; shift ;;
    *) echo "ERROR: unknown flag: $1" >&2; exit 1 ;;
  esac
done
[ -z "$SOURCE" ] && { echo "ERROR: --source is required" >&2; exit 1; }
[ -z "$TARGET" ] && { echo "ERROR: --target is required" >&2; exit 1; }
case "$STRATEGY" in
  merge|squash|rebase) ;;
  *) echo "ERROR: INVALID_STRATEGY — must be merge|squash|rebase" >&2; exit 1 ;;
esac
```

### Step 2 — Pre-Checks

```bash
# Working tree must be clean (RULE-004 preserves history; dirty tree risks loss)
if [ -n "$(git status --porcelain)" ]; then
  echo "ERROR: WORKING_TREE_DIRTY — Working tree must be clean before merge" >&2
  exit 1
fi

# Source branch must exist locally
if ! git rev-parse --verify --quiet "$SOURCE" >/dev/null; then
  echo "ERROR: SOURCE_NOT_FOUND — Source branch '$SOURCE' not found" >&2
  exit 2
fi

# Target branch must exist locally
if ! git rev-parse --verify --quiet "$TARGET" >/dev/null; then
  echo "ERROR: TARGET_NOT_FOUND — Target branch '$TARGET' not found" >&2
  exit 3
fi
```

### Step 3 — Checkout Target

```bash
git checkout "$TARGET"
```

### Step 4 — Idempotency (No-Op Check)

```bash
# If target already contains source HEAD, nothing to merge
if git merge-base --is-ancestor "$SOURCE" "$TARGET"; then
  printf '{"mergeSha":null,"conflicts":false,"conflictedFiles":[],"rolledBack":false,"noOp":true}\n'
  exit 0
fi
```

### Step 5 — Attempt Merge (Dispatch on Strategy)

```bash
CONFLICTS="false"
case "$STRATEGY" in
  merge)
    if [ -n "$MESSAGE" ]; then
      git merge --no-ff --no-edit -m "$MESSAGE" "$SOURCE" 2>/tmp/merge.err || CONFLICTS="true"
    else
      git merge --no-ff --no-edit "$SOURCE" 2>/tmp/merge.err || CONFLICTS="true"
    fi
    ;;
  squash)
    if ! git merge --squash "$SOURCE" 2>/tmp/merge.err; then
      CONFLICTS="true"
    else
      MSG="${MESSAGE:-squash: merge $SOURCE into $TARGET}"
      git commit -m "$MSG" 2>/tmp/merge.err || CONFLICTS="true"
    fi
    ;;
  rebase)
    git rebase "$SOURCE" 2>/tmp/merge.err || CONFLICTS="true"
    ;;
esac
```

### Step 6 — Decide (Success or Conflict Rollback)

```bash
if [ "$CONFLICTS" = "true" ]; then
  # Capture unmerged paths BEFORE aborting (diff-filter=U)
  FILES=$(git diff --name-only --diff-filter=U 2>/dev/null | tr '\n' ' ' | sed 's/ $//')
  CONFLICTED_FILES=$(printf '%s' "$FILES" | awk 'BEGIN{printf "["} \
    {for(i=1;i<=NF;i++){if(i>1)printf ",";printf "\"%s\"",$i}} END{print "]"}')

  # Abort the partial operation
  ABORT_CMD="git merge --abort"
  [ "$STRATEGY" = "rebase" ] && ABORT_CMD="git rebase --abort"
  if ! $ABORT_CMD 2>/tmp/abort.err; then
    echo "ERROR: ROLLBACK_FAILED — $ABORT_CMD failed; manual cleanup needed" >&2
    cat /tmp/abort.err >&2
    exit 11
  fi
  printf '{"mergeSha":null,"conflicts":true,"conflictedFiles":%s,"rolledBack":true,"noOp":false}\n' \
    "$CONFLICTED_FILES"
  exit 10
fi

MERGE_SHA=$(git rev-parse HEAD)
```

### Step 7 — Optional Push

```bash
if [ "$NO_PUSH" != "true" ]; then
  git push origin "$TARGET" || {
    echo "WARN: push failed; merge is committed locally but not pushed" >&2
  }
fi
```

### Step 8 — Emit Structured Result

```bash
printf '{"mergeSha":"%s","conflicts":false,"conflictedFiles":[],"rolledBack":false,"noOp":false}\n' \
  "$MERGE_SHA"
```

## Outputs

| Field | Type | Always | Description |
|-------|------|--------|-------------|
| `mergeSha` | string(40) \| null | Yes | SHA of the resulting merge / squash / rebase-HEAD commit; `null` on conflict or no-op |
| `conflicts` | boolean | Yes | `true` when any conflict was detected during the merge attempt |
| `conflictedFiles` | string[] | Yes | List of unmerged paths captured before rollback; `[]` on success / no-op |
| `rolledBack` | boolean | Yes | `true` when `git merge --abort` / `git rebase --abort` executed successfully |
| `noOp` | boolean | Yes | `true` when target already contained source HEAD (ancestor check) |

## Error Codes

| Exit | Code | Condition | Message |
|------|------|-----------|---------|
| 1 | `WORKING_TREE_DIRTY` | `git status --porcelain` non-empty | `Working tree must be clean before merge` |
| 1 | `INVALID_STRATEGY` | `--strategy` not in {merge, squash, rebase} | `--strategy must be one of: merge, squash, rebase` |
| 2 | `SOURCE_NOT_FOUND` | source branch missing locally | `Source branch '<name>' not found` |
| 3 | `TARGET_NOT_FOUND` | target branch missing locally | `Target branch '<name>' not found` |
| 10 | `MERGE_CONFLICT_ROLLED_BACK` | conflict detected, rollback succeeded | `Conflict in N files; merge aborted` |
| 11 | `ROLLBACK_FAILED` | `git merge --abort` / `git rebase --abort` failed | `Rollback failed; manual cleanup needed` |

## Error Handling

| Scenario | Action |
|----------|--------|
| `--source` or `--target` missing | exit 1; print usage hint |
| `--strategy` invalid | exit 1 `INVALID_STRATEGY`; print accepted values |
| Dirty working tree | exit 1 `WORKING_TREE_DIRTY`; no side effects |
| Source/target branch missing | exit 2 / 3 with suggestion `git fetch origin` |
| Target already contains source HEAD | exit 0 with `noOp:true`; no commit, no push |
| Conflict during merge/squash/rebase | capture `conflictedFiles`, abort, exit 10 `MERGE_CONFLICT_ROLLED_BACK` |
| `git merge --abort` / `git rebase --abort` fails | exit 11 `ROLLBACK_FAILED`; stderr carried verbatim; manual cleanup |
| `git push` fails | WARN only; local merge is preserved; caller can retry push |

## Examples

```
# Happy path — merge develop into epic/0049, push after
/x-git-merge --source develop --target epic/0049
# -> {"mergeSha":"abc123...","conflicts":false,...,"noOp":false}

# No-op — target already contains source HEAD
/x-git-merge --source develop --target epic/0049
# -> {"mergeSha":null,...,"noOp":true}

# Conflict with automatic rollback (exit 10)
/x-git-merge --source develop --target epic/0049
# -> {"mergeSha":null,"conflicts":true,"conflictedFiles":["a.md"],"rolledBack":true,...}

# Squash with custom commit message
/x-git-merge --source feat/foo --target develop --strategy squash --message "feat(foo): batch"

# Rebase feature branch onto develop (linear history, no push)
/x-git-merge --source develop --target feat/foo --strategy rebase --no-push
```

## Rule References

- **Rule 09** (Branching Model) — merge-direction rules and target-branch conventions.
- **RULE-004** (EPIC-0049 — Estratégia de merge: preserva history) — default strategy `merge` with `--no-ff` preserves per-task TDD commits for bisect.
- **RULE-005** (EPIC-0049 — Thin orchestrator) — `x-epic-implement` and `x-story-implement` delegate local merges here; no inline `git merge` blocks.
- **RULE-010** (EPIC-0049 — Skills internas pequenas) — this SKILL.md stays under 250 lines.

## Integration Notes

| Skill | Relationship | Context |
|-------|--------------|---------|
| `x-epic-implement` | caller (future refactor — story-0049-0018) | Phase 1.4e auto-rebase between parallel stories; `develop → epic/XXXX` sync |
| `x-story-implement` | caller (future refactor — story-0049-0019) | Optional auto-sync of story branch with parent epic branch |
| `x-internal-epic-branch-ensure` | related (story-0049-0008) | Ensures `epic/XXXX` exists before this skill merges `develop` into it |
| `x-pr-merge` | sibling (story-0049-0003) | Remote PR merge via `gh pr merge`; this skill handles local-only merges |
| `x-git-branch` | sibling (story-0049-0001) | Bare branch creation; this skill assumes both branches exist |
