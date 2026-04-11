# x-review-pr

> Tech Lead holistic review with 45-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, TDD process, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge.

| | |
|---|---|
| **Category** | Review |
| **Invocation** | `/x-review-pr [PR-number or STORY-ID]` |
| **Reads** | coding-standards, architecture, testing, quality-gates |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Performs a senior-level holistic code review using a 45-point rubric across 11 dimensions including code hygiene, naming, functions, formatting, design, error handling, architecture, framework usage, tests, security, and TDD process. Reviews the full PR diff for cross-file consistency, produces a scored GO/NO-GO decision, and updates the consolidated review dashboard and remediation tracking if previous specialist reviews exist.

## Usage

```
/x-review-pr
/x-review-pr 42
/x-review-pr STORY-0024-0003
```

## Workflow

1. **Idempotency** -- Skip if report exists and code is unchanged since last review
2. **Detect** -- Identify branch, diff, and story context from argument or current branch
3. **Gather** -- Read knowledge packs and check for existing specialist review artifacts
4. **Template** -- Detect Tech Lead review template for output format
5. **Review** -- Execute 45-point checklist against full diff and source files
6. **Dashboard** -- Update consolidated dashboard with Tech Lead score
7. **Remediation** -- Update remediation tracking with FIXED/new findings

## Outputs

| Artifact | Path |
|----------|------|
| Tech Lead report | `plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md` |
| Dashboard (updated) | `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` |
| Remediation (updated) | `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md` |

## See Also

- [x-review](../x-review/) -- Parallel specialist reviews (breadth) that run before this skill
- [x-pr-fix-comments](../x-pr-fix-comments/) -- Automates fixes for PR review comments
- [x-story-implement](../x-story-implement/) -- Full development cycle that invokes this as Phase 6
