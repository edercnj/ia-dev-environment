---
name: x-review-perf
description: >
  Performance specialist review: validates N+1 queries,
  connection pools, async patterns, pagination, caching,
  timeouts, circuit breakers, and resource cleanup.
  Reference: `.github/skills/x-review-perf/SKILL.md`
---

# Skill: Performance Specialist Review

## Purpose

Performs a performance-focused code review for {{PROJECT_NAME}}, identifying N+1 query patterns, connection pool misuse, missing pagination, cache inefficiencies, improper timeouts, and resource leaks.

## Triggers

- `/x-review-perf` -- review current branch changes
- `/x-review-perf 123` -- review PR #123
- `/x-review-perf --files src/main/` -- review specific source files

## Review Checklist

| # | Check | Severity |
|---|-------|----------|
| 1 | No N+1 query patterns | CRITICAL |
| 2 | Connection pool sizing configured | HIGH |
| 3 | Async operations use non-blocking I/O | HIGH |
| 4 | Pagination for collection endpoints | HIGH |
| 5 | Caching strategy for hot paths | MEDIUM |
| 6 | Timeouts on all external calls | HIGH |
| 7 | Circuit breakers for external services | MEDIUM |
| 8 | Resource cleanup in finally/try-with | HIGH |
| 9 | No unbounded collections in memory | MEDIUM |
| 10 | Batch operations for bulk writes | MEDIUM |

## Output Format

Produces a structured findings report with severity classification, file references, and fix suggestions.

## Integration Notes

- Invoked by x-review as a specialist review agent
- Reads performance-engineering knowledge pack
- Findings feed into the consolidated review dashboard
