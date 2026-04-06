---
name: x-fix-pr-comments
description: >
  Reads PR review comments and fixes actionable ones automatically. Detects PR
  from argument or branch, classifies comments (actionable/suggestion/question/praise),
  implements fixes, and commits with proper conventional commit messages.
  Reference: `.github/skills/x-fix-pr-comments/SKILL.md`
---

# Skill: Fix PR Comments

## Purpose

Automates the process of addressing PR review comments for {{PROJECT_NAME}}. Reads all review comments from a pull request, classifies them by type, and implements fixes for actionable feedback.

## Triggers

- `/x-fix-pr-comments` -- fix comments on current branch's PR
- `/x-fix-pr-comments 123` -- fix comments on PR #123

## Workflow

```
1. DETECT     -> Identify PR (argument or branch)
2. FETCH      -> Get all review comments via GitHub CLI
3. CLASSIFY   -> Categorize each comment (actionable/suggestion/question/praise/resolved)
4. FIX        -> Implement fixes for actionable comments
5. VERIFY     -> Compile and test after each fix
6. REPLY      -> Reply to each comment thread in PT-BR (fix summary or rejection reason)
7. COMMIT     -> Commit with conventional commit message
8. REPORT     -> Summarize actions taken
```

### Step 1 -- Detect PR

```bash
# If argument is a number, use it directly
PR_NUMBER=$1

# If no argument, detect from current branch
gh pr view --json number --jq '.number'
```

If no PR found, abort: `No PR found for current branch.`

### Step 2 -- Fetch Review Comments

```bash
# Get all review comments
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/comments \
  --jq '.[] | {id: .id, path: .path, line: .line, body: .body, user: .user.login}'

# Get PR reviews for context
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/reviews \
  --jq '.[] | {id: .id, state: .state, body: .body, user: .user.login}'
```

### Step 3 -- Classify Comments

| Type | Description | Action |
|------|-------------|--------|
| **Actionable** | Clear request to change code | Implement the fix |
| **Suggestion** | Optional improvement | Implement if aligns with standards |
| **Question** | Clarification request | Skip (needs human) |
| **Praise** | Positive feedback | Skip |
| **Resolved** | Already addressed | Skip |

### Step 4 -- Implement Fixes

For each actionable comment:
1. Read the file at the comment's path and line
2. Understand the reviewer's request
3. Implement the fix
4. Process file-by-file, top-to-bottom to avoid line shifts

### Step 5 -- Verify

```bash
{{COMPILE_COMMAND}}
{{TEST_COMMAND}}
```

If compilation or tests fail, revert and try alternative approach.

### Step 6 -- Reply to Comments (PT-BR)

After each comment is processed, reply to the thread **in Portuguese (pt-BR)**:

```bash
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/comments/{comment_id}/replies \
  -f body="{message_in_portuguese}"
```

**Reply templates:**

| Classification | Reply (PT-BR) |
|---------------|--------------|
| **Fixed** | `Corrigido. {descricao da alteracao}. Commit: {hash}` |
| **Suggestion accepted** | `Sugestao aceita. {descricao}. Commit: {hash}` |
| **Suggestion rejected** | `Sugestao analisada, mas nao aplicada. Motivo: {razao tecnica}` |
| **Doesn't make sense** | `Observacao analisada, mas nao faz sentido neste contexto. Motivo: {explicacao}` |
| **Failed to fix** | `Tentei corrigir, mas a alteracao causou falha. Necessita intervencao manual.` |
| **Question/Praise** | _(no reply)_ |

**Rules:**
- ALL replies in Portuguese (pt-BR)
- Be specific: what changed and why
- When rejecting, provide technical justification
- Include commit hash for applied fixes

### Step 7 -- Commit

```bash
git add {modified-files}
git commit -m "fix({scope}): address PR review comments

Addresses review comments on PR #{PR_NUMBER}"
```

### Step 8 -- Report

```markdown
## PR Review Comments — Fix Report

**PR:** #{PR_NUMBER}
**Total comments:** N | **Processed:** M

| # | File | Line | Type | Action | Status | Reply |
|---|------|------|------|--------|--------|-------|
| 1 | path/file | 42 | Actionable | Fixed | Done | Replied |
| 2 | path/file | 20 | Actionable | — | Rejected | Replied |
| 3 | path/file | 15 | Question | — | Skipped | — |
```

## Error Handling

| Scenario | Action |
|----------|--------|
| PR not found | Abort with message |
| No comments | Report "No review comments found" |
| Fix causes failure | Revert and report "Unable to fix automatically" |
