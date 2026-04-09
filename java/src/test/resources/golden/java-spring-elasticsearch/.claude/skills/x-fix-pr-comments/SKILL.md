---
name: x-fix-pr-comments
description: "Reads PR review comments and fixes actionable ones automatically. Detects PR from argument or branch, classifies comments (actionable/suggestion/question/praise), implements fixes, and commits with proper conventional commit messages."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[PR-number]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Fix PR Comments

## Purpose

Automates the process of addressing PR review comments for {{PROJECT_NAME}}. Reads all review comments from a pull request, classifies them by type, and implements fixes for actionable feedback.

## Triggers

- `/x-fix-pr-comments` — fix comments on current branch's PR
- `/x-fix-pr-comments 123` — fix comments on PR #123

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `PR-number` | No | PR number to process. If omitted, detect from current branch. |

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

### Step 1 — Detect PR

Determine which PR to process:

```bash
# If argument is a number, use it directly
PR_NUMBER=$1

# If no argument, detect from current branch
if [ -z "$PR_NUMBER" ]; then
  gh pr view --json number --jq '.number'
fi
```

If no PR found, abort: `No PR found for current branch. Provide a PR number as argument.`

### Step 2 — Fetch Review Comments

```bash
# Get all review comments (not issue comments)
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/comments \
  --jq '.[] | {id: .id, path: .path, line: .line, body: .body, user: .user.login, created_at: .created_at}'

# Also get PR review threads for context
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/reviews \
  --jq '.[] | {id: .id, state: .state, body: .body, user: .user.login}'
```

### Step 3 — Classify Comments

For each comment, classify into one of:

| Type | Description | Action |
|------|-------------|--------|
| **Actionable** | Clear request to change code (fix bug, rename, refactor) | Implement the fix |
| **Suggestion** | Optional improvement with rationale | Implement if aligns with project standards |
| **Question** | Reviewer asking for clarification | Skip (requires human response) |
| **Praise** | Positive feedback | Skip |
| **Resolved** | Already addressed or outdated | Skip |

**Classification rules:**
- Contains "please change", "should be", "must", "fix", "bug", "wrong" — Actionable
- Contains "consider", "maybe", "could", "suggestion", "nit" — Suggestion
- Contains "?", "why", "what", "how does" — Question
- Contains "LGTM", "nice", "good", "great" — Praise
- Thread is marked as resolved — Resolved

### Step 4 — Implement Fixes

For each actionable/suggestion comment:

1. Read the file at the specified path and line:
   ```bash
   # Comment metadata provides path and line
   cat -n {path} | sed -n '{line-5},{line+5}p'
   ```

2. Understand the reviewer's request

3. Implement the fix using Edit tool

4. If the fix requires understanding broader context, read surrounding code first

**Ordering:** Process comments file-by-file to minimize context switching. Within a file, process top-to-bottom to avoid line number shifts.

### Step 5 — Verify

After each fix (or batch of fixes per file):

```bash
{{COMPILE_COMMAND}}
{{TEST_COMMAND}}
```

If compilation fails, revert the last change and try an alternative approach.
If tests fail, analyze the failure and adjust the fix.

### Step 6 — Reply to Comments (PT-BR)

After each comment is processed (fixed, skipped, or rejected), reply to the review comment thread **in Portuguese (pt-BR)** explaining the action taken.

**Reply command:**
```bash
# Reply to an inline review comment thread
gh api repos/{owner}/{repo}/pulls/{PR_NUMBER}/comments/{comment_id}/replies \
  -f body="{message_in_portuguese}"
```

**Reply templates by classification:**

| Classification | Reply Template (PT-BR) |
|---------------|----------------------|
| **Actionable (fixed)** | `Corrigido. {descricao da alteracao feita}. Commit: {short_hash}` |
| **Suggestion (accepted)** | `Sugestao aceita. {descricao da alteracao feita}. Commit: {short_hash}` |
| **Suggestion (rejected)** | `Sugestao analisada, mas nao aplicada. Motivo: {razao tecnica pela qual nao faz sentido no contexto atual}` |
| **Actionable (rejected)** | `Observacao analisada, mas nao faz sentido neste contexto. Motivo: {explicacao tecnica detalhada}` |
| **Actionable (failed)** | `Tentei corrigir, mas a alteracao causou falha na compilacao/testes. Necessita intervencao manual.` |
| **Question** | _(no reply — requires human response)_ |
| **Praise** | _(no reply)_ |
| **Resolved** | _(no reply — already resolved)_ |

**Rules:**
- ALL replies MUST be in Portuguese (pt-BR)
- Be specific: describe exactly what was changed and why
- When rejecting, provide technical justification (reference project rules, architecture constraints, or coding standards)
- Include commit hash when a fix was applied
- Never reply to praise or already-resolved comments

**Examples:**

Fix applied:
```
Corrigido. Renomeei a variavel `d` para `elapsedTimeInMs` conforme padrao de naming do projeto. Commit: abc1234
```

Suggestion rejected:
```
Sugestao analisada, mas nao aplicada. Motivo: o metodo precisa ser publico porque implementa a interface Assembler, que define o contrato do pipeline. Tornar package-private quebraria o polimorfismo.
```

Comment doesn't make sense:
```
Observacao analisada, mas nao faz sentido neste contexto. Motivo: o null check sugerido e desnecessario aqui — o parametro vem de um Optional.orElseThrow() na linha 42, garantindo que nunca sera null neste ponto.
```

### Step 7 — Commit

After all fixes for a logical group are verified:

```bash
git add {modified-files}
git commit -m "fix({scope}): address PR review comments

- {summary of change 1}
- {summary of change 2}

Addresses review comments on PR #{PR_NUMBER}"
```

**Scope** should match the module/layer affected (e.g., `domain`, `api`, `config`).

### Step 8 — Report

Output a summary table:

```markdown
## PR Review Comments — Fix Report

**PR:** #{PR_NUMBER}
**Total comments:** N
**Processed:** M

| # | File | Line | Type | Action | Status | Reply |
|---|------|------|------|--------|--------|-------|
| 1 | src/main/Foo.java | 42 | Actionable | Renamed variable | Fixed | Replied |
| 2 | src/main/Bar.java | 15 | Suggestion | Added null check | Fixed | Replied |
| 3 | src/main/Baz.java | 20 | Actionable | — | Rejected | Replied |
| 4 | src/main/Baz.java | 88 | Question | — | Skipped | — |
| 5 | src/main/Qux.java | 3 | Praise | — | Skipped | — |

### Questions Requiring Human Response

- **src/main/Baz.java:88** (@reviewer): "Why is this method public?"
```

## Error Handling

| Scenario | Action |
|----------|--------|
| PR not found | Abort with message |
| No comments on PR | Report "No review comments found" |
| Comment references deleted file | Skip with warning |
| Fix causes compilation failure | Revert and report as "Unable to fix automatically" |
| Fix causes test failure | Revert and report as "Fix caused regression" |
| API rate limit | Wait and retry (max 3 attempts) |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-dev-lifecycle` | called-by | Invoked during Phase 4 (fix review comments) |
| `x-fix-epic-pr-comments` | called-by | Batch mode processes multiple PRs from an epic |
| `x-git-push` | calls | Uses Conventional Commits format for fix commits |
| `x-review` | reads | Processes comments produced by specialist reviews |
