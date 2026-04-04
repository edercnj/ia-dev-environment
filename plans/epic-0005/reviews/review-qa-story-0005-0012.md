# QA Review — story-0005-0012

ENGINEER: QA
STORY: story-0005-0012
SCORE: 33/36
STATUS: Rejected

## PASSED

- [1] Test exists for each AC (2/2)
- [2] Line coverage >= 95% (98.1%) (2/2)
- [3] Branch coverage >= 90% (95.31%) (2/2)
- [4] Test naming convention (2/2)
- [5] AAA pattern (2/2)
- [7] Exception paths tested (2/2)
- [8] No test interdependency (2/2)
- [9] Fixtures centralized (2/2)
- [10] Unique test data (2/2)
- [12] Integration tests N/A (2/2)
- [14] Explicit refactoring after green (2/2)
- [15] TPP progression (2/2)
- [18] TDD coverage thresholds (2/2)

## FAILED

- [16] Dead code in planner.ts:191-192 (0/2) — `if (!story) return undefined` in
  `buildStoryDetail` unreachable after `validateStoryFilter`. Remove guard or justify. [MEDIUM]

## PARTIAL

- [6] Parametrized tests for status mapping (1/2) — Use `it.each` for all 5 status values. [LOW]
- [11] Formatter edge cases (1/2) — Missing COMPLETED/FAILED status, mode variations. [LOW]
- [13] Commit granularity (1/2) — Combined RED+GREEN commits acceptable but not ideal. [LOW]
- [17] Acceptance tests E2E (1/2) — No planner->formatter integrated tests. [MEDIUM]
