# Task Breakdown — story-0005-0007: Failure Handling — Retry + Block Propagation

## Decomposition Mode: Test-Driven (TDD with TPP markers)

Tasks follow Red-Green-Refactor cycles derived from the test plan TPP ordering.

---

## TASK-1: Domain types and constants

**Type:** GREEN (foundational types)
**Parallel:** yes
**Files:**
- `src/domain/failure/types.ts` — `MAX_RETRIES`, `RetryContext`, `RetryDecision`, `BlockPropagationResult`, `BlockedStoryEntry`
- `src/domain/failure/index.ts` — barrel exports

**Acceptance:** Types compile, exports resolve.

---

## TASK-2: RetryEvaluator — RED/GREEN/REFACTOR cycles

**Type:** TDD
**Parallel:** yes (independent of TASK-3)
**Depends On:** TASK-1

### Cycle 2.1: Budget exhausted (UT-1, UT-2, UT-9) — TPP L1-L2

- **RED:** Write tests: `evaluateRetry_retriesAtMax_returnsShouldRetryFalse`, `evaluateRetry_retriesAboveMax_returnsShouldRetryFalse`, `evaluateRetry_budgetExhausted_reasonIsBudgetExhausted`
- **GREEN:** Implement `evaluateRetry` with `if (currentRetries >= MAX_RETRIES)` branch returning `{ shouldRetry: false, reason: "budget_exhausted" }`
- **REFACTOR:** Extract constant check if needed

### Cycle 2.2: Retry allowed with context (UT-3 through UT-8) — TPP L3-L4

- **RED:** Write tests: `evaluateRetry_retriesZero_returnsShouldRetryTrue`, `evaluateRetry_retriesOne_returnsShouldRetryTrue`, context field tests (UT-5 through UT-8)
- **GREEN:** Implement `else` branch returning `{ shouldRetry: true, retryContext: { storyId, previousError, retryNumber: currentRetries + 1, branchName } }`
- **REFACTOR:** Simplify if needed

### Cycle 2.3: Acceptance test (AT-1) — Parametrized

- **RED:** Write parametrized test `retryBudget_parametrized_enforcesMaxRetries` with rows [0,1,2,3]
- **GREEN:** Already passing from cycles 2.1-2.2
- **REFACTOR:** Verify no duplication

---

## TASK-3: BlockPropagator — RED/GREEN/REFACTOR cycles

**Type:** TDD
**Parallel:** yes (independent of TASK-2)
**Depends On:** TASK-1

### Cycle 3.1: Degenerate cases (UT-10, UT-11) — TPP L1

- **RED:** Write tests: `propagateBlocks_storyNotInDag_returnsEmptyBlockedStories`, `propagateBlocks_storyWithNoDependents_returnsEmptyBlockedStories`
- **GREEN:** Implement `propagateBlocks` with early returns for missing node and empty `blocks` array
- **REFACTOR:** N/A

### Cycle 3.2: Direct dependent (UT-12, UT-13, UT-19) — TPP L3-L4

- **RED:** Write tests: single direct dependent blocked, blockedBy contains failed story, failedStoryId in result
- **GREEN:** Implement BFS first level — queue, visited set, push direct dependents
- **REFACTOR:** Extract helper if needed

### Cycle 3.3: Transitive chain (UT-14, UT-15) — TPP L5

- **RED:** Write tests: chain A→B→C all blocked, blockedBy chain correctness (B→[A], C→[B])
- **GREEN:** Complete BFS loop — process queue until empty
- **REFACTOR:** Ensure O(n) complexity with visited set

### Cycle 3.4: Complex topologies (UT-16, UT-17, UT-18) — TPP L5 edge

- **RED:** Write tests: diamond DAG, fan-out, unrelated stories not affected
- **GREEN:** Already passing from cycle 3.3 BFS implementation
- **REFACTOR:** Final cleanup

### Cycle 3.5: Acceptance test (AT-2) — Complex DAG

- **RED:** Write `blockPropagation_transitiveGraph_blocksAllDependentsCorrectly`
- **GREEN:** Already passing from cycles 3.1-3.4
- **REFACTOR:** Final review

---

## TASK-4: Barrel exports and re-exports

**Type:** GREEN
**Parallel:** no
**Depends On:** TASK-2, TASK-3

**Files:**
- `src/domain/failure/index.ts` — ensure all exports correct
- Verify `src/domain/index.ts` exists and consider adding re-export (only if index.ts exists and has pattern)

**Acceptance:** `import { evaluateRetry, propagateBlocks, MAX_RETRIES } from "./domain/failure/index.js"` resolves.

---

## TASK-5: Final validation

**Type:** Verification
**Parallel:** no
**Depends On:** TASK-2, TASK-3, TASK-4

- Run full test suite: `npx vitest run`
- Verify coverage: ≥ 95% line, ≥ 90% branch
- Run TypeScript compiler: `npx tsc --noEmit`
- Commit all changes

---

## Execution Order

```
TASK-1 ──┐
         ├── TASK-2 (parallel) ──┐
         ├── TASK-3 (parallel) ──┤── TASK-4 ── TASK-5
         └───────────────────────┘
```
