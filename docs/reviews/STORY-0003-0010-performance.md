```
ENGINEER: Performance
STORY: STORY-0003-0010
SCORE: 26/26
STATUS: Approved
---
PASSED:
- [1] No N+1 queries (2/2) — N/A — no runtime code changes
- [2] Connection pool sized (2/2) — N/A — no runtime code changes
- [3] Async where applicable (2/2) — N/A — no runtime code changes
- [4] Pagination on collections (2/2) — N/A — no runtime code changes
- [5] Caching strategy (2/2) — N/A — no runtime code changes
- [6] No unbounded lists (2/2) — N/A — no runtime code changes
- [7] Timeout on external calls (2/2) — N/A — no runtime code changes
- [8] Circuit breaker on external (2/2) — N/A — no runtime code changes
- [9] Thread safety (2/2) — N/A — no runtime code changes
- [10] Resource cleanup (2/2) — N/A — no runtime code changes
- [11] Lazy loading (2/2) — N/A — no runtime code changes
- [12] Batch operations (2/2) — N/A — no runtime code changes
- [13] Index usage (2/2) — N/A — no runtime code changes
FAILED:
(none)
PARTIAL:
(none)
```

**Summary:** This change adds TDD-related instructions (Red-Green-Refactor, Atomic TDD Commits, Gherkin Completeness, Double-Loop TDD) to the `x-story-epic` skill template and propagates them to 24 golden file copies plus the GitHub skills template. All 26 modified files are markdown. There are zero TypeScript changes, zero runtime behavior changes, and zero configuration changes. All 13 performance checklist items are not applicable.
