---
name: x-git-worktree
description: "Manages git worktrees for parallel task and story execution. Operations: create, list, remove, cleanup. Follows RULE-018 (Worktree Lifecycle) naming convention under .claude/worktrees/{identifier}/."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "<create|list|remove|cleanup> [--branch <name>] [--base <base>] [--id <identifier>] [--dry-run]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Git Worktree Management

## Purpose

Centralizes all git worktree operations for {{PROJECT_NAME}}. Provides isolated working directories for parallel task and story execution without conflicts, with automatic cleanup of stale or merged worktrees.

## When to Use

- Creating isolated working directories for parallel story/task implementation
- Listing active worktrees and their status
- Removing specific worktrees after completion
- Cleaning up obsolete worktrees (merged, stale, orphan)
- Any workflow requiring parallel execution in isolated directories

## Triggers

- `/x-git-worktree create --branch <name>` -- create a new worktree
- `/x-git-worktree list` -- list all active worktrees with status
- `/x-git-worktree remove --id <identifier>` -- remove a specific worktree
- `/x-git-worktree cleanup` -- remove obsolete worktrees
- `/x-git-worktree cleanup --dry-run` -- preview cleanup without removing

## Worktree Base Directory

All worktrees are created under `.claude/worktrees/` relative to the repository root.

```
.claude/
  worktrees/
    task-0029-0001-001/       # Worktree for a task
    story-0029-0002/          # Worktree for a story
    hotfix-auth/              # Custom worktree
```

## Naming Convention (RULE-018)

| Context | Pattern | Example |
|---------|---------|---------|
| Task | `.claude/worktrees/task-XXXX-YYYY-NNN/` | `.claude/worktrees/task-0029-0001-001/` |
| Story | `.claude/worktrees/story-XXXX-YYYY/` | `.claude/worktrees/story-0029-0001/` |
| Custom | `.claude/worktrees/{identifier}/` | `.claude/worktrees/hotfix-auth/` |

When `--id` is not provided, the identifier is derived from the branch name by stripping common prefixes (`feat/`, `feature/`, `fix/`, `hotfix/`, `refactor/`).

## Operations

### Operation 1: create

Creates a new git worktree in `.claude/worktrees/{identifier}/`.

#### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--branch` | string | **Yes** | -- | Branch name for the worktree |
| `--base` | string | No | `develop` | Base branch to create the new branch from |
| `--id` | string | No | derived from branch | Worktree directory identifier |

#### Workflow

```
1. VALIDATE    -> Check branch is not protected (main, develop)
2. VALIDATE    -> Check branch is not already an active worktree
3. FETCH       -> git fetch origin to ensure up-to-date refs
4. BRANCH      -> Create branch from --base if it does not exist
5. WORKTREE    -> git worktree add .claude/worktrees/{id}/ {branch}
6. VERIFY      -> Confirm worktree was created successfully
7. OUTPUT      -> Return absolute path of the worktree
```

#### Step 1 -- Validate Protected Branches

Never create a worktree directly on `main` or `develop`. These are protected branches.

```bash
# Check for protected branches
BRANCH_NAME="$1"
if [ "$BRANCH_NAME" = "main" ] || [ "$BRANCH_NAME" = "develop" ]; then
  echo "ERROR: Cannot create worktree on protected branch ($BRANCH_NAME)"
  exit 1
fi
```

#### Step 2 -- Check Active Worktrees

```bash
# Verify the branch is not already checked out in a worktree
git worktree list --porcelain | grep -q "branch refs/heads/$BRANCH_NAME"
if [ $? -eq 0 ]; then
  echo "ERROR: Branch '$BRANCH_NAME' is already an active worktree"
  exit 1
fi
```

#### Step 3 -- Fetch Remote

```bash
git fetch origin
```

#### Step 4 -- Create Branch (if needed)

```bash
# Only create if branch does not exist locally or remotely
if ! git rev-parse --verify "$BRANCH_NAME" >/dev/null 2>&1; then
  git branch "$BRANCH_NAME" "$BASE_BRANCH"
fi
```

#### Step 5 -- Create Worktree

```bash
WORKTREE_DIR=".claude/worktrees/$IDENTIFIER"
git worktree add "$WORKTREE_DIR" "$BRANCH_NAME"
```

#### Step 6 -- Verify

```bash
if [ -d "$WORKTREE_DIR" ] && [ -f "$WORKTREE_DIR/.git" ]; then
  echo "Worktree created at: $(cd "$WORKTREE_DIR" && pwd)"
else
  echo "ERROR: Worktree creation failed"
  exit 1
fi
```

#### Derive Identifier from Branch Name

When `--id` is not specified, derive the identifier by stripping known prefixes:

```bash
derive_id() {
  local branch="$1"
  # Strip common branch prefixes
  local id="${branch#feat/}"
  id="${id#feature/}"
  id="${id#fix/}"
  id="${id#hotfix/}"
  id="${id#refactor/}"
  echo "$id"
}
```

### Operation 2: list

Lists all active worktrees with status information.

#### Output Format

```
| ID                    | Branch                              | Status   | Last Commit       | Age    |
| :---                  | :---                                | :---     | :---              | :---   |
| task-0029-0001-001    | feat/task-0029-0001-001-domain      | ACTIVE   | 2026-04-07 10:30  | 2h     |
| story-0029-0002       | feat/story-0029-0002-task-model     | ACTIVE   | 2026-04-07 08:00  | 4h30m  |
| task-0029-0003-002    | feat/task-0029-0003-002-lint        | STALE    | 2026-03-31 15:00  | 7d+    |
| hotfix-auth           | hotfix/auth-fix                     | MERGED   | 2026-04-06 12:00  | 1d     |
```

#### Status Classification

| Status | Condition |
|--------|-----------|
| `ACTIVE` | Branch exists, last commit within 7 days, not merged |
| `STALE` | Branch exists, last commit older than 7 days |
| `MERGED` | Branch has been merged into target (develop or main) |
| `ORPHAN` | Branch no longer exists locally or on remote |

#### Implementation

```bash
# List worktrees under .claude/worktrees/
WORKTREE_BASE=".claude/worktrees"
for dir in "$WORKTREE_BASE"/*/; do
  [ -d "$dir" ] || continue
  ID=$(basename "$dir")
  BRANCH=$(git -C "$dir" rev-parse --abbrev-ref HEAD 2>/dev/null)
  LAST_COMMIT=$(git -C "$dir" log -1 --format="%ci" 2>/dev/null)
  STATUS=$(classify_status "$BRANCH" "$LAST_COMMIT")
  echo "| $ID | $BRANCH | $STATUS | $LAST_COMMIT | $(age "$LAST_COMMIT") |"
done
```

### Operation 3: remove

Removes a specific worktree by identifier.

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--id` | string | **Yes** | Worktree identifier to remove |

#### Workflow

```bash
WORKTREE_DIR=".claude/worktrees/$IDENTIFIER"

# 1. Verify worktree exists
if [ ! -d "$WORKTREE_DIR" ]; then
  echo "ERROR: Worktree '$IDENTIFIER' not found"
  exit 1
fi

# 2. Remove via git worktree
git worktree remove "$WORKTREE_DIR" --force

# 3. Prune stale worktree references
git worktree prune

# 4. Verify removal
if [ ! -d "$WORKTREE_DIR" ]; then
  echo "Worktree '$IDENTIFIER' removed successfully"
else
  echo "ERROR: Failed to remove worktree '$IDENTIFIER'"
  exit 1
fi
```

### Operation 4: cleanup

Removes obsolete worktrees based on three criteria.

#### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--dry-run` | boolean | No | `false` | List candidates without removing |

#### Cleanup Criteria

| Criterion | Condition | Action |
|-----------|-----------|--------|
| `MERGED` | Branch merged into target (develop or main) | Remove worktree + delete local branch |
| `STALE` | Last commit older than 7 days | Remove worktree (preserve branch) |
| `ORPHAN` | Branch does not exist locally or on remote | Remove worktree |

#### Workflow

```
1. LIST        -> Enumerate all worktrees in .claude/worktrees/
2. CLASSIFY    -> Determine status of each worktree
3. FILTER      -> Select worktrees matching cleanup criteria
4. DRY-RUN     -> If --dry-run, report and stop
5. REMOVE      -> Execute git worktree remove for each candidate
6. CLEAN       -> Delete local branches for MERGED worktrees
7. PRUNE       -> git worktree prune
8. REPORT      -> Summary of removed worktrees
```

#### Branch Merged Detection

```bash
is_merged() {
  local branch="$1"
  local target="${2:-develop}"
  # Check if branch is fully merged into target
  git branch --merged "$target" | grep -q "$branch"
}
```

#### Stale Detection (>7 days)

```bash
is_stale() {
  local branch="$1"
  local last_epoch=$(git log -1 --format="%ct" "$branch" 2>/dev/null)
  local now_epoch=$(date +%s)
  local seven_days=$((7 * 24 * 3600))
  [ $((now_epoch - last_epoch)) -gt $seven_days ]
}
```

#### Orphan Detection

```bash
is_orphan() {
  local branch="$1"
  # Check local
  if git rev-parse --verify "$branch" >/dev/null 2>&1; then
    return 1
  fi
  # Check remote
  if git ls-remote --heads origin "$branch" | grep -q "$branch"; then
    return 1
  fi
  return 0
}
```

#### Dry-Run Output

```
Cleanup candidates:
| ID                    | Status   | Reason                          |
| :---                  | :---     | :---                            |
| task-0029-0001-001    | MERGED   | Branch merged into develop      |
| task-0029-0003-002    | STALE    | No commits for 12 days          |
| story-0029-0005       | ORPHAN   | Branch does not exist           |

Total: 3 worktrees would be removed (use without --dry-run to execute)
```

#### Execution Output

```
Cleanup results:
| ID                    | Status   | Action                                |
| :---                  | :---     | :---                                  |
| task-0029-0001-001    | MERGED   | Removed worktree + deleted branch     |
| task-0029-0003-002    | STALE    | Removed worktree (branch preserved)   |
| story-0029-0005       | ORPHAN   | Removed worktree                      |

3 worktrees removed (1 MERGED, 1 STALE, 1 ORPHAN)
```

## Git Flow Integration (RULE-005)

| Context | Base Branch | Branch Prefix |
|---------|-------------|---------------|
| Task (from develop) | `develop` | `feat/task-XXXX-YYYY-NNN-description` |
| Story (from develop) | `develop` | `feat/story-XXXX-YYYY-description` |
| Hotfix | `main` | `hotfix/description` |
| Custom | User-specified | User-specified |

### Protected Branches

Worktrees MUST NOT be created for protected branches:
- `main` -- production branch
- `develop` -- integration branch

Attempting to create a worktree for `main` or `develop` results in an immediate error.

### Post-Merge Lifecycle

After a branch is merged via PR:
1. The worktree becomes a cleanup candidate (status: `MERGED`)
2. `/x-git-worktree cleanup` removes the worktree and deletes the local branch
3. The remote branch is deleted by GitHub after PR merge (if configured)

## Integration with Epic Execution

The `x-dev-epic-implement` orchestrator uses `x-git-worktree` for parallel story execution:

```
Orchestrator (main)              x-git-worktree                 Subagent
-----------------------          ----------                 --------
                                                            
  dispatch story ──────────────► /x-git-worktree create         
                                   --branch feat/story-...  
                                   --base develop           
                          ◄────── path: .claude/worktrees/  
                                  story-XXXX-YYYY/          
                                                            
  launch subagent ────────────────────────────────────────► works in worktree
                                                            
  story complete ◄──────────────────────────────────────── SubagentResult
                                                            
  after PR merge ──────────────► /x-git-worktree remove         
                                   --id story-XXXX-YYYY     
                          ◄────── removed                   
                                                            
  phase complete ──────────────► /x-git-worktree cleanup        
                          ◄────── N worktrees removed       
```

## Error Handling

| Scenario | Action |
|----------|--------|
| `--branch` not provided for create | Abort with usage message |
| Branch is protected (main/develop) | Abort with protection error |
| Branch already an active worktree | Abort with conflict error |
| Worktree directory already exists | Abort with existence error |
| `--id` not found for remove | Abort with not-found error |
| `git worktree add` fails | Report git error, suggest manual cleanup |
| `git worktree remove` fails | Try `--force`, report if still failing |
| No worktrees to cleanup | Report "No obsolete worktrees found" |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-dev-epic-implement` | called-by | Parallel story dispatch via worktrees (Phase 1) |
| `x-dev-story-implement` | called-by | Story execution within a worktree directory |
| `x-git-push` | related | Branch creation and push from within worktree |
