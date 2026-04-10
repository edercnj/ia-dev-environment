# x-git-push

> Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control.

| | |
|---|---|
| **Category** | Git/Release |
| **Invocation** | `/x-git-push [branch-name or commit-message]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Standardizes the entire Git workflow following Git Flow conventions. Handles branch creation from `develop`, atomic commits using Conventional Commits format, pushing to remote, and PR creation via `gh`. Also supports hotfix workflows branching from `main` with back-merge to `develop`, and TDD-specific commit formats with `[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, and `[TDD:REFACTOR]` suffixes.

## Usage

```
/x-git-push
/x-git-push feat/story-0001-0002-add-validation
/x-git-push "feat(domain): add transaction validation"
```

## Workflow

1. **Branch** -- Create feature branch from `develop` (or hotfix from `main`)
2. **Commit** -- Stage changes and create atomic commits following Conventional Commits
3. **Build** -- Run full build to validate before push
4. **Push** -- Push branch to remote with upstream tracking
5. **PR** -- Create pull request via `gh pr create` targeting `develop`

## See Also

- [x-release](../x-release/) -- Uses the same Conventional Commits format for release commits
- [x-release-changelog](../x-release-changelog/) -- Parses commits created by this skill to generate changelogs
