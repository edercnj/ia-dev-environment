# Test Plan — story-0005-0007: Failure Handling — Retry + Block Propagation

## Summary

- Total test files: 2
- Total test methods: ~18 (estimated)
- Categories covered: Unit, Parametrized (Contract)
- Estimated line coverage: ~98%
- Estimated branch coverage: ~95%

## TPP Order (Transformation Priority Premise)

Tests follow TPP progression: degenerate cases → simple → complex → boundary → edge cases.

---

## Test File 1: `tests/domain/failure/retry-evaluator.test.ts`

### Unit Tests — `evaluateRetry`

| # | ID | Test Name | Description | TPP Level | Depends On | Parallel |
|---|----|-----------|-------------|-----------|------------|----------|
| 1 | UT-1 | `evaluateRetry_retriesAtMax_returnsShouldRetryFalse` | Degenerate: budget exhausted (retries=2) returns `shouldRetry: false` | L1 (constant) | - | yes |
| 2 | UT-2 | `evaluateRetry_retriesAboveMax_returnsShouldRetryFalse` | Boundary: retries=3 still returns false | L2 (boundary) | UT-1 | yes |
| 3 | UT-3 | `evaluateRetry_retriesZero_returnsShouldRetryTrue` | Happy path: first retry allowed | L3 (simple) | UT-1 | yes |
| 4 | UT-4 | `evaluateRetry_retriesOne_returnsShouldRetryTrue` | Happy path: second retry allowed | L3 (simple) | UT-3 | yes |
| 5 | UT-5 | `evaluateRetry_retriesZero_retryContextHasRetryNumberOne` | Context: retryNumber = currentRetries + 1 | L4 (data) | UT-3 | yes |
| 6 | UT-6 | `evaluateRetry_retriesZero_retryContextContainsPreviousError` | Context: previousError echoed in output | L4 (data) | UT-3 | yes |
| 7 | UT-7 | `evaluateRetry_retriesZero_retryContextContainsStoryId` | Context: storyId echoed in output | L4 (data) | UT-3 | yes |
| 8 | UT-8 | `evaluateRetry_retriesZero_retryContextContainsBranchName` | Context: branchName echoed in output | L4 (data) | UT-3 | yes |
| 9 | UT-9 | `evaluateRetry_budgetExhausted_reasonIsBudgetExhausted` | Reason field on rejection | L4 (data) | UT-1 | yes |

### Acceptance Test — AT-1: Retry budget enforcement (RULE-005)

| # | ID | Test Name | Description |
|---|-----|-----------|-------------|
| 1 | AT-1 | `retryBudget_parametrized_enforcesMaxRetries` | Parametrized: retries=[0,1,2,3] → shouldRetry=[true,true,false,false] |

---

## Test File 2: `tests/domain/failure/block-propagator.test.ts`

### Unit Tests — `propagateBlocks`

| # | ID | Test Name | Description | TPP Level | Depends On | Parallel |
|---|----|-----------|-------------|-----------|------------|----------|
| 1 | UT-10 | `propagateBlocks_storyNotInDag_returnsEmptyBlockedStories` | Degenerate: unknown storyId | L1 (constant) | - | yes |
| 2 | UT-11 | `propagateBlocks_storyWithNoDependents_returnsEmptyBlockedStories` | Degenerate: leaf node | L1 (constant) | - | yes |
| 3 | UT-12 | `propagateBlocks_singleDirectDependent_returnsOneBlocked` | Direct block: A fails → B blocked | L3 (simple) | UT-11 | yes |
| 4 | UT-13 | `propagateBlocks_singleDirectDependent_blockedByContainsFailedStory` | Verify blockedBy field correctness | L4 (data) | UT-12 | yes |
| 5 | UT-14 | `propagateBlocks_transitiveChain_returnsAllTransitivelyBlocked` | Chain: A→B→C, all blocked (RULE-006) | L5 (composition) | UT-12 | no |
| 6 | UT-15 | `propagateBlocks_transitiveChain_blockedByChainIsCorrect` | B.blockedBy=[A], C.blockedBy=[B] | L5 (composition) | UT-14 | no |
| 7 | UT-16 | `propagateBlocks_diamondDag_returnsAllDependentsOnce` | Diamond: A→{B,C}→D, D appears once | L5 (composition) | UT-14 | no |
| 8 | UT-17 | `propagateBlocks_fanOut_returnsAllDirectDependents` | Fan-out: A→{B,C,D}, all three blocked | L4 (data) | UT-12 | yes |
| 9 | UT-18 | `propagateBlocks_unrelatedStoriesNotAffected_notIncluded` | Isolation: E has no path from A | L5 (edge) | UT-14 | yes |
| 10 | UT-19 | `propagateBlocks_failedStoryId_isInResult` | Result includes failedStory field | L4 (data) | UT-12 | yes |

### Acceptance Test — AT-2: Transitive block propagation (RULE-006)

| # | ID | Test Name | Description |
|---|-----|-----------|-------------|
| 1 | AT-2 | `blockPropagation_transitiveGraph_blocksAllDependentsCorrectly` | Complex DAG: A fails, B/C direct deps blocked, D/E transitive deps blocked, F unrelated not blocked |

---

## Coverage Estimation

| Module | Public Functions | Branches | Est. Tests | Line % | Branch % |
|--------|-----------------|----------|-----------|--------|----------|
| `retry-evaluator.ts` | 1 (`evaluateRetry`) | 2 (retries < MAX, retries >= MAX) | 10 | 100% | 100% |
| `block-propagator.ts` | 1 (`propagateBlocks`) | 4 (not in DAG, no dependents, BFS loop, visited check) | 10 | 100% | 100% |
| `types.ts` | 0 (types + constant only) | 0 | 0 | 100% (covered via usage) | N/A |
| `index.ts` | 0 (barrel re-exports) | 0 | 0 | 100% (covered via imports) | N/A |
| **Total** | **2** | **6** | **~20** | **~98%** | **~95%** |

## Quality Checks

1. [x] Every acceptance criterion maps to ≥1 test (AC-1 through AC-8 covered)
2. [x] Every error path tested (unknown storyId, budget exceeded)
3. [x] Test categories: Unit + Parametrized
4. [x] Boundary values: retries=0, 1, 2, 3 (triplet+ pattern)
5. [x] Parametrized matrices: retry budget (4 rows), DAG topologies (6 topologies)
6. [x] Estimated coverage meets thresholds (98% line, 95% branch)
7. [x] Test naming follows `[method]_[scenario]_[expected]` convention

## Risks and Gaps

- **Integration with orchestrator:** Not tested here (deferred to story-0005-0010). The orchestrator coordination between `evaluateRetry`, `propagateBlocks`, and `updateStoryStatus` is out of scope.
- **Performance:** The <100ms NFR for <50 stories is not explicitly performance-tested. A simple timing assertion could be added to AT-2 if needed.
- **DagNode type coupling:** Tests import `DagNode` from `implementation-map/types.ts`. If that type changes, these tests break. Mitigated by reusing existing `createDag`/`createDagNode` helpers.
