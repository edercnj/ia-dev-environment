# x-git-commit

> Creates Conventional Commits with Task ID in scope and pre-commit chain (format -> lint -> compile). Central commit point in the task-centric workflow with TDD tag support.

| | |
|---|---|
| **Category** | Dev/Workflow |
| **Invocation** | `/x-git-commit --task TASK-XXXX-YYYY-NNN --type <type> --subject <subject> [--tdd RED\|GREEN\|REFACTOR] [--body <body>] [--skip-chain] [--amend]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Orchestrates the pre-commit quality chain (x-code-format -> x-code-lint -> compile) and creates standardized commits with Task ID in the scope (RULE-016), TDD tags in the subject (RULE-008), and Conventional Commits format. Every commit is traceable to its originating task, and code quality is enforced before each commit.

## Usage

```
/x-git-commit --task TASK-0029-0005-001 --type feat --subject "add detection logic"
/x-git-commit --task TASK-0029-0005-001 --type test --subject "add unit tests" --tdd RED
/x-git-commit --task TASK-0029-0005-001 --type feat --subject "implement handler" --tdd GREEN
/x-git-commit --task TASK-0029-0005-001 --type refactor --subject "extract method" --tdd REFACTOR
/x-git-commit --task TASK-0029-0005-001 --type chore --subject "update config" --skip-chain
/x-git-commit --task TASK-0029-0005-001 --type fix --subject "fix null check" --amend
```

## Workflow

1. **Validate** -- Check task ID format, commit type, subject length, TDD tag
2. **Check Stage** -- Verify staged files exist in the working tree
3. **Pre-Commit Chain** -- Run x-code-format -> x-code-lint -> compile (unless `--skip-chain`)
4. **Build Message** -- Construct commit message with task ID and TDD tag
5. **Commit** -- Execute `git commit` (or `git commit --amend`)
6. **Report** -- Output commit summary with SHA and message

## Commit Format

```
<type>(<TASK-XXXX-YYYY-NNN>): <subject> [TDD:TAG]

[optional body]

[optional footer]
```

## TDD Tags

| Tag | Phase | When |
|-----|-------|------|
| `[TDD:RED]` | Red | After writing a failing test |
| `[TDD:GREEN]` | Green | After making the test pass |
| `[TDD:REFACTOR]` | Refactor | After refactoring without changing tests |
| `[TDD]` | Generic | When specific phase does not apply |

## Outputs

| Artifact | Description |
|----------|-------------|
| Git commit | Commit with task ID in scope and optional TDD tag |
| Console report | Summary with task, type, subject, TDD tag, chain status, and commit SHA |

## See Also

- [x-code-format](../x-code-format/) -- First step of the pre-commit chain (code formatting)
- [x-code-lint](../x-code-lint/) -- Second step of the pre-commit chain (static analysis)
- [x-git-push](../x-git-push/) -- Push commits and create PRs
- [x-story-implement](../x-story-implement/) -- Orchestrates the full implementation cycle
