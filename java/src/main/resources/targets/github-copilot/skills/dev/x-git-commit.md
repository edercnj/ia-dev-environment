---
name: x-git-commit
description: >
  Creates Conventional Commits with Task ID in scope and pre-commit chain
  (format -> lint -> compile). Central commit point in the task-centric
  workflow with TDD tag support.
  Reference: `.github/skills/x-commit/SKILL.md`
---

# Skill: Conventional Commit with Task ID

## Purpose

Creates standardized commits for {{PROJECT_NAME}} with Task ID in the scope, enforces the complete pre-commit chain (RULE-007: `x-format -> x-lint -> compile -> commit`), and annotates commits with TDD tags (RULE-008). This skill is the central commit creation point in the task-centric workflow.

## Triggers

- `/x-commit --task TASK-XXXX-YYYY-NNN --type feat --subject "add detection logic"` -- commit with task ID
- `/x-commit --task TASK-XXXX-YYYY-NNN --type test --subject "add unit tests" --tdd RED` -- commit with TDD tag
- `/x-commit --task TASK-XXXX-YYYY-NNN --type feat --subject "implement handler" --tdd GREEN` -- green phase commit
- `/x-commit --task TASK-XXXX-YYYY-NNN --type refactor --subject "extract method" --tdd REFACTOR` -- refactor phase commit
- `/x-commit --task TASK-XXXX-YYYY-NNN --type chore --subject "update config" --skip-chain` -- skip pre-commit chain

## Arguments

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `--task` | String | Yes | Task ID in format `TASK-XXXX-YYYY-NNN` |
| `--type` | String | Yes | Conventional Commits type: feat, fix, test, refactor, docs, chore, perf |
| `--subject` | String | Yes | Commit subject, imperative mood, max 72 characters |
| `--tdd` | String | No | TDD tag: RED, GREEN, REFACTOR, or TDD |
| `--body` | String | No | Commit body (multi-line, additional context) |
| `--skip-chain` | Flag | No | Skip pre-commit chain (emergency use only) |
| `--amend` | Flag | No | Amend last commit instead of creating new |

## Commit Message Format

```
<type>(<TASK-XXXX-YYYY-NNN>): <subject> [TDD:TAG]

[optional body]
```

### TDD Tags

| Tag | Phase | When |
|-----|-------|------|
| `[TDD:RED]` | Red | After writing a failing test |
| `[TDD:GREEN]` | Green | After making the test pass |
| `[TDD:REFACTOR]` | Refactor | After refactoring without changing tests |
| `[TDD]` | Generic | When specific phase does not apply |

## Workflow

```
1. VALIDATE    -> Check parameters (task ID, type, subject, tdd tag)
2. CHECK-STAGE -> Verify staged files exist
3. PRE-COMMIT  -> Run chain: x-format -> x-lint -> compile (unless --skip-chain)
4. BUILD-MSG   -> Construct commit message with task ID and TDD tag
5. COMMIT      -> Execute git commit (or git commit --amend)
6. REPORT      -> Output commit summary
```

### Pre-Commit Chain (RULE-007)

1. **x-format**: Format code, re-stage modified files
2. **x-lint**: Analyze code, abort on ERRORs
3. **compile**: Run `{{COMPILE_COMMAND}}`, abort on failure
4. **commit**: Create commit with standardized message

### Validations

- Task ID must match `TASK-XXXX-YYYY-NNN` pattern
- Subject must be 72 characters or fewer
- Type must be a valid Conventional Commits type
- TDD tag must be RED, GREEN, REFACTOR, or TDD
- Working tree must have staged files

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid task ID | ABORT with format hint |
| Subject too long | ABORT with character count |
| Invalid type | ABORT with valid types list |
| No staged files | ABORT with "No staged files for commit" |
| Pre-commit chain fails | ABORT with step name and error details |

## Integration Notes

- Invokes x-format and x-lint as pre-commit steps
- Used by x-dev-lifecycle to create task-level commits
- Follows Conventional Commits for changelog generation
- TDD tags enable TDD compliance auditing
