---
name: x-pr-create
description: "Task-level PR creation with formatted title, automatic labels, structured body, and target branch logic. Creates standardized PRs for individual tasks with Task ID traceability."
user-invocable: true
allowed-tools: Bash, Read, Grep, Glob
argument-hint: "TASK-XXXX-YYYY-NNN [--auto-approve-pr] [--draft] [--description \"short desc\"]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Task PR Creation

## Purpose

Creates standardized Pull Requests for individual tasks in {{PROJECT_NAME}} with Task ID in the title, automatic labels (`task`, `story-XXXX-YYYY`, `epic-XXXX`), structured body with review checklist, and correct target branch. Ensures every task delivery is traceable and independently reviewable.

## Triggers

- `/x-pr-create TASK-0029-0001-001` -- create PR for the specified task
- `/x-pr-create TASK-0029-0001-001 --draft` -- create as draft PR
- `/x-pr-create TASK-0029-0001-001 --auto-approve-pr` -- target parent story branch instead of develop
- `/x-pr-create TASK-0029-0001-001 --description "add user validation"` -- override PR title description

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `task-id` | String | Yes | Task ID in format `TASK-XXXX-YYYY-NNN` |
| `--auto-approve-pr` | Flag | No | Target parent story branch instead of `develop` |
| `--draft` | Flag | No | Create PR as draft |
| `--description` | String | No | Short description for PR title (overrides branch-derived description) |

## Workflow

```
Phase 0  VALIDATE    -> Parse task ID, validate branch naming
Phase 1  PRE-CHECK   -> Run {{TEST_COMMAND}} to verify tests pass
Phase 2  TITLE       -> Format PR title (<=70 chars, Conventional Commits)
Phase 3  BODY        -> Generate structured PR body
Phase 4  LABELS      -> Create labels if missing, collect label list
Phase 5  CREATE      -> Create PR via gh pr create
```

### Phase 0 -- Validate Branch and Arguments

1. Parse task ID from arguments: extract `TASK-XXXX-YYYY-NNN` format
2. Validate task ID matches pattern: `TASK-\d{4}-\d{4}-\d{3}`
3. Verify current branch follows naming convention:

```bash
CURRENT_BRANCH=$(git branch --show-current)
# Must match: feat/task-XXXX-YYYY-NNN-*
PATTERN="^feat/task-[0-9]{4}-[0-9]{4}-[0-9]{3}-"
if [[ ! "$CURRENT_BRANCH" =~ $PATTERN ]]; then
  echo "ABORT: Current branch does not match task naming convention: feat/task-XXXX-YYYY-NNN-*"
  echo "Current branch: $CURRENT_BRANCH"
  exit 1
fi
```

4. Extract epic ID (`XXXX`), story ID (`XXXX-YYYY`), and task number (`NNN`) from the task ID
5. If `--auto-approve-pr` is set, verify parent branch exists:

```bash
STORY_ID="story-XXXX-YYYY"
PARENT_BRANCH=$(git branch -a | grep "feat/${STORY_ID}" | head -1 | tr -d ' ')
if [[ -z "$PARENT_BRANCH" ]]; then
  echo "ABORT: Parent branch not found for ${STORY_ID}"
  exit 1
fi
```

### Phase 1 -- Pre-Check (Tests)

Run the project test command before creating the PR:

```bash
{{TEST_COMMAND}}
if [[ $? -ne 0 ]]; then
  echo "ABORT: Tests failed. Fix tests before creating PR."
  exit 1
fi
```

If tests fail, abort with message: `"Tests failed. Fix tests before creating PR."`

### Phase 2 -- Format PR Title

Format the PR title following Conventional Commits:

1. Determine the commit type from the task type or default to `feat`
2. Build title: `<type>(TASK-XXXX-YYYY-NNN): <description>`
3. If `--description` is provided, use it; otherwise derive from branch name:

```bash
# Extract description from branch name
# feat/task-0029-0001-001-add-validation -> add validation
DESC=$(echo "$CURRENT_BRANCH" | sed 's/feat\/task-[0-9]*-[0-9]*-[0-9]*-//' | tr '-' ' ')
```

4. Truncate title to 70 characters maximum:

```bash
TITLE="feat(TASK-XXXX-YYYY-NNN): ${DESC}"
if [[ ${#TITLE} -gt 70 ]]; then
  TITLE="${TITLE:0:67}..."
fi
```

### Phase 3 -- Generate PR Body

Generate the structured PR body with the following sections:

```markdown
## Summary

{description derived from commits or --description flag}

## Task Details

| Field | Value |
|-------|-------|
| Task ID | TASK-XXXX-YYYY-NNN |
| Story | story-XXXX-YYYY |
| Epic | epic-XXXX |
| Task Plan | `plans/epic-XXXX/tasks/task-plan-XXXX-YYYY-NNN.md` |

## Changes

{list each commit in the branch using: git log --oneline develop..HEAD}

## Review Checklist

- [ ] Tests pass locally
- [ ] Coverage thresholds met (>=95% line, >=90% branch)
- [ ] TDD commits present (RED -> GREEN -> REFACTOR)
- [ ] No TODO/FIXME/HACK comments
- [ ] Conventional Commits format followed
```

If `--draft` is set, prepend to the body:

```markdown
> [DRAFT] This PR is not ready for review
```

### Phase 4 -- Label Auto-Creation

Ensure labels exist in the repository, creating them if missing:

```bash
# Label definitions
LABELS=(
  "task|#0075ca|Individual task PR"
  "story-XXXX-YYYY|#e4e669|Parent story reference"
  "epic-XXXX|#d73a4a|Parent epic reference"
)

for LABEL_DEF in "${LABELS[@]}"; do
  IFS='|' read -r NAME COLOR DESC <<< "$LABEL_DEF"
  # Check if label exists; create if not
  gh label list --search "$NAME" --json name -q '.[].name' | grep -qx "$NAME" || \
    gh label create "$NAME" --color "${COLOR#\#}" --description "$DESC"
done
```

### Phase 5 -- Create PR

Determine target branch and create the PR:

```bash
# Target branch logic
if [[ "$AUTO_APPROVE" == "true" ]]; then
  TARGET_BRANCH="$PARENT_BRANCH"
else
  TARGET_BRANCH="develop"
fi

# Push branch if needed
git push -u origin "$CURRENT_BRANCH"

# Build gh pr create command
DRAFT_FLAG=""
if [[ "$DRAFT" == "true" ]]; then
  DRAFT_FLAG="--draft"
fi

gh pr create \
  --base "$TARGET_BRANCH" \
  --title "$TITLE" \
  --body "$BODY" \
  --label "task,story-XXXX-YYYY,epic-XXXX" \
  $DRAFT_FLAG
```

After creation, report the result:

```
PR #42 created: https://github.com/owner/repo/pull/42
  Title:   feat(TASK-0029-0001-001): add validation
  Target:  develop
  Labels:  task, story-0029-0001, epic-0029
  Draft:   false
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid task ID format | ABORT with format explanation |
| Branch naming mismatch | ABORT: "Current branch does not match task naming convention: feat/task-XXXX-YYYY-NNN-*" |
| Tests fail | ABORT: "Tests failed. Fix tests before creating PR." |
| Parent branch not found (auto-approve mode) | ABORT: "Parent branch not found for story-XXXX-YYYY" |
| Label creation fails | WARN and continue (PR can be created without labels) |
| gh CLI not available | ABORT: "gh CLI is required. Install from https://cli.github.com" |
| PR already exists for branch | ABORT: "PR already exists for this branch. Use gh pr view to check." |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-commit` | predecessor | Commits are created before PR |
| `x-git-push` | alternative | x-git-push handles general git workflow; x-pr-create is task-specific |
| `x-dev-lifecycle` | called-by | Phase 5 (PR creation) delegates to this skill for task-level PRs |
| `x-test-run` | called-by | Phase 1 pre-check runs the test command |

## Examples

### Basic Task PR

```
/x-pr-create TASK-0029-0001-001
```

Creates PR:
- Title: `feat(TASK-0029-0001-001): add validation`
- Target: `develop`
- Labels: `task`, `story-0029-0001`, `epic-0029`

### Draft PR

```
/x-pr-create TASK-0029-0001-001 --draft
```

Creates draft PR with `[DRAFT] This PR is not ready for review` banner.

### Auto-Approve Mode

```
/x-pr-create TASK-0029-0001-001 --auto-approve-pr
```

Creates PR targeting parent story branch (`feat/story-0029-0001-*`) instead of `develop`.

### Custom Description

```
/x-pr-create TASK-0029-0001-001 --description "implement user input validation"
```

Creates PR with title: `feat(TASK-0029-0001-001): implement user input validation`
