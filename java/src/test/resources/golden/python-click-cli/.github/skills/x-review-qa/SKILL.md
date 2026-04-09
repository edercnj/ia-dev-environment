---
name: x-review-qa
description: >
  QA specialist review: validates test coverage, TDD compliance,
  test naming, fixtures, parametrized tests, and acceptance
  criteria coverage.
  Reference: `.github/skills/x-review-qa/SKILL.md`
---

# Skill: QA Specialist Review

## Purpose

Performs a QA-focused code review for {{PROJECT_NAME}}, validating test coverage, TDD compliance (Red-Green-Refactor), test naming conventions, shared fixtures, parametrized tests, and acceptance criteria coverage.

## Triggers

- `/x-review-qa` -- review current branch changes
- `/x-review-qa 123` -- review PR #123
- `/x-review-qa --files src/test/` -- review specific test files

## Review Checklist

| # | Check | Severity |
|---|-------|----------|
| 1 | Line coverage >= 95% | CRITICAL |
| 2 | Branch coverage >= 90% | CRITICAL |
| 3 | Test naming follows convention | HIGH |
| 4 | TDD Red-Green-Refactor evidence | HIGH |
| 5 | No mocking of domain logic | HIGH |
| 6 | Parametrized tests for business rules | MEDIUM |
| 7 | Shared fixtures (no duplicate records) | MEDIUM |
| 8 | Acceptance tests for Gherkin scenarios | HIGH |
| 9 | No sleep-based synchronization | MEDIUM |
| 10 | No weak assertions (isNotNull alone) | MEDIUM |

## Output Format

Produces a structured findings report with severity classification, file references, and fix suggestions.

## Integration Notes

- Invoked by x-review as a specialist review agent
- Reads testing knowledge pack for project-specific patterns
- Findings feed into the consolidated review dashboard
