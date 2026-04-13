# x-pr-fix-comments

> Reads PR review comments and fixes actionable ones automatically. Detects PR from argument or branch, classifies comments (actionable/suggestion/question/praise), implements fixes, and commits with proper conventional commit messages.

| | |
|---|---|
| **Category** | Review |
| **Invocation** | `/x-pr-fix-comments [PR-number]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Automates the process of addressing PR review comments by fetching all review comments from a pull request, classifying them by type (actionable, suggestion, question, praise, resolved), and implementing fixes for actionable feedback. After each fix, it verifies compilation and tests, replies to comment threads with a summary of the action taken, and commits changes with conventional commit messages.

## Usage

```
/x-pr-fix-comments
/x-pr-fix-comments 123
```

## Workflow

1. **Detect** -- Identify PR from argument or current branch
2. **Fetch** -- Get all review comments via GitHub CLI
3. **Classify** -- Categorize each comment (actionable/suggestion/question/praise/resolved)
4. **Fix** -- Implement fixes for actionable comments, file-by-file top-to-bottom
5. **Verify** -- Compile and test after each fix; revert if broken
6. **Reply** -- Reply to each comment thread with fix summary or rejection reason
7. **Commit** -- Commit with conventional commit message referencing the PR

## See Also

- [x-pr-fix-epic-comments](../x-pr-fix-epic-comments/) -- Batch version for all PRs in an epic
- [x-review-pr](../x-review-pr/) -- Tech Lead review that may generate comments to fix
- [x-review](../x-review/) -- Specialist reviews that may generate comments to fix
