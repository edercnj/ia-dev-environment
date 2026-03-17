# Implementation Plan -- story-0005-0007: Failure Handling -- Retry + Block Propagation

**Architecture Plan:** `architecture-story-0005-0007.md`
**Story:** `story-0005-0007.md`

---

## 1. Affected Layers and Components

| Layer | Impact | Details |
|-------|--------|---------|
| `src/domain/failure/` | **NEW sub-module** | New directory with 3 files: `block-propagator.ts`, `retry-evaluator.ts`, `types.ts` |
| `src/domain/failure/index.ts` | **NEW** | Barrel export for the failure module |
| `src/domain/index.ts` | **MODIFY** | Add re-export of `failure` barrel |
| `src/checkpoint/` | **NO CHANGE** | Existing `updateStoryStatus()` already supports `status`, `retries`, and `blockedBy` fields in `StoryEntryUpdate` |
| `src/domain/implementation-map/` | **NO CHANGE** | Existing `DagNode` type consumed read-only; no modifications needed |
| `src/exceptions.ts` | **NO CHANGE** | No new custom error classes required; failure handling returns result types, not exceptions |

This story is contained within the domain layer (pure functions) with no adapter, CLI, config, or template changes. The checkpoint engine is already capable of persisting all required fields (`status: FAILED | BLOCKED`, `retries`, `blockedBy`).

---

## 2. New Classes/Interfaces to Create

### 2.1 Types (`src/domain/failure/types.ts`)

| Type | Kind | Description |
|------|------|-------------|
| `RetryContext` | interface | Context passed to the retry subagent: `{ storyId, previousError, retryNumber, branchName }` |
| `RetryDecision` | discriminated union | `{ shouldRetry: true, retryContext: RetryContext } \| { shouldRetry: false, reason: "budget_exhausted" }` |
| `BlockPropagationResult` | interface | `{ failedStory: string, blockedStories: BlockedStoryEntry[] }` |
| `BlockedStoryEntry` | interface | `{ storyId: string, blockedBy: string[] }` -- tracks which story ID(s) caused the block |
| `MAX_RETRIES` | const | Named constant `= 2` (RULE-005). Domain rule, not configuration. |

### 2.2 Functions (by file)

| File | Functions | Lines Est. |
|------|-----------|------------|
| `retry-evaluator.ts` | `evaluateRetry(storyId, currentRetries, previousError, branchName): RetryDecision` | ~25 |
| `block-propagator.ts` | `propagateBlocks(failedStoryId, dag): BlockPropagationResult` | ~45 |
| `index.ts` | Barrel re-exports of types and functions | ~10 |

### 2.3 Design Decisions

**`evaluateRetry`** is a pure function. It receives the current retry count and returns a `RetryDecision` discriminated union. It does NOT call the checkpoint engine or dispatch subagents -- the orchestrator (story-0005-0010) is responsible for acting on the decision. This keeps the domain logic free from I/O.

**`propagateBlocks`** is a pure function implementing BFS traversal on the DAG's `blocks` adjacency list. It receives a `ReadonlyMap<string, DagNode>` and a failed story ID, returns the complete list of transitively blocked stories with their `blockedBy` chains. It does NOT mutate the DAG or call the checkpoint engine.

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| `src/domain/index.ts` | Add `export * from "./failure/index.js";` | Expose the new failure module through the domain barrel |

No other existing files are modified. The checkpoint engine already supports all needed operations:
- `updateStoryStatus(epicDir, storyId, { status: "FAILED", retries: N })` -- mark story as FAILED
- `updateStoryStatus(epicDir, storyId, { status: "BLOCKED", blockedBy: [...] })` -- mark story as BLOCKED
- `StoryEntry` already has `retries: number` and `blockedBy?: readonly string[]`

---

## 4. Dependency Direction Validation

```
src/domain/failure/types.ts             --> (no imports)
src/domain/failure/retry-evaluator.ts   --> ./types.ts (within module only)
src/domain/failure/block-propagator.ts  --> ./types.ts, ../implementation-map/types.ts (DagNode, ParsedMap)
src/domain/failure/index.ts             --> all internal files
src/domain/index.ts                     --> failure/index.ts
```

**Dependency graph:**
```
domain/failure  -->  domain/implementation-map/types (DagNode read-only)
                     ^
                     |  (both domain, inward-pointing)
```

**Verification checklist:**
- [ ] No import from `src/assembler/`
- [ ] No import from `src/cli*.ts`
- [ ] No import from `src/config.ts`
- [ ] No import from `src/models.ts`
- [ ] No import from `src/checkpoint/` (domain must not import checkpoint -- checkpoint is application/infrastructure)
- [ ] No import from `node:fs` or `node:path`
- [ ] No import from any npm package
- [ ] Only standard library types used (Map, Set, Array, string, number, boolean)
- [ ] Only cross-domain import is `DagNode` / `ParsedMap` from `implementation-map/types.ts` (domain-to-domain)

---

## 5. Integration Points

| Consumer | How It Integrates | When |
|----------|-------------------|------|
| story-0005-0010 (Orchestrator) | Calls `evaluateRetry()` after subagent failure; calls `propagateBlocks()` when retries exhausted; uses result to call `updateStoryStatus()` on checkpoint engine | After this story is complete |
| story-0005-0006 (Integrity Gate) | Calls `propagateBlocks()` when integrity gate identifies regression source and reverts a story | After this story is complete |
| story-0005-0005 (Core Loop) | The core loop uses `getExecutableStories()` from implementation-map, which already excludes BLOCKED stories (status !== PENDING). No changes needed. | Existing behavior |
| `src/checkpoint/engine.ts` | Orchestrator calls `updateStoryStatus()` with `{ status: "FAILED" }` and `{ status: "BLOCKED", blockedBy: [...] }` -- existing API, no changes | Existing API |

### Integration contract between failure module and orchestrator:

1. Orchestrator detects subagent returned `status: "FAILED"`
2. Orchestrator calls `evaluateRetry(storyId, currentEntry.retries, error, branch)`
3. If `shouldRetry: true`: re-dispatch subagent with `retryContext`, increment retries via `updateStoryStatus`
4. If retry succeeds: `updateStoryStatus(id, { status: "SUCCESS" })`
5. If retry fails and `shouldRetry: false` (budget exhausted):
   - `updateStoryStatus(id, { status: "FAILED", retries: currentRetries })`
   - Call `propagateBlocks(id, parsedMap.stories)`
   - For each entry in `blockedStories`: `updateStoryStatus(entry.storyId, { status: "BLOCKED", blockedBy: entry.blockedBy })`
6. Continue with `getExecutableStories()` which naturally excludes FAILED and BLOCKED stories

---

## 6. Database Changes

N/A -- This module is pure computation with no persistence. The checkpoint engine already persists `status`, `retries`, and `blockedBy` fields via `execution-state.json`.

---

## 7. API Changes

N/A -- This module has no HTTP/CLI interface. It exports TypeScript functions consumed by the orchestrator module (story-0005-0010).

---

## 8. Event Changes

N/A -- No events are produced or consumed.

---

## 9. Configuration Changes

N/A -- The retry budget is a domain constant (`MAX_RETRIES = 2`, RULE-005), not a configuration value. No environment variables or config files are introduced.

---

## 10. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| `DagNode` type diverges between implementation-map and failure module expectations | Medium | Low | Block propagator depends only on `blocks: string[]` field of `DagNode`, which is stable. Import type directly, no duplication. |
| Block propagation infinite loop on malformed DAG | High | Very Low | BFS uses a `visited` Set to prevent revisiting nodes. DAG is already validated for cycles by `detectCycles()` in implementation-map. Add defensive guard regardless. |
| Retry context grows unbounded with large error messages | Low | Medium | Truncate `previousError` to a reasonable limit (e.g., 4096 chars) in the `RetryContext` factory. |
| Type mismatch between `StoryStatus` in `implementation-map/types.ts` (enum) vs `checkpoint/types.ts` (const object) | Medium | Certain | The failure module imports from `implementation-map/types.ts` (domain layer). The orchestrator must handle the mapping when calling `updateStoryStatus()`. Both define the same string literal values. |
| Orchestrator integration not tested until story-0005-0010 | Low | Certain | Failure module is fully unit-tested with pure functions. Integration test with checkpoint engine deferred to story-0005-0010 but can be validated by manual composition test. |
| Performance regression on large DAGs | Low | Low | BFS is O(V+E). Requirement is <100ms for <50 stories. Even with 50 stories and dense edges, traversal completes in microseconds. |

---

## 11. Implementation Order (TDD)

Implementation follows inner-to-outer order, with tests written first (Red-Green-Refactor).

### Phase A: Types

1. Create `src/domain/failure/types.ts` with `MAX_RETRIES`, `RetryContext`, `RetryDecision`, `BlockPropagationResult`, `BlockedStoryEntry`.
2. No tests needed (type definitions and a constant only).

### Phase B: Retry Evaluator (Red-Green-Refactor)

1. **Test:** `evaluateRetry` with retries=0 returns `shouldRetry: true` with correct `RetryContext`.
2. **Test:** `evaluateRetry` with retries=1 returns `shouldRetry: true` with `retryNumber: 2`.
3. **Test:** `evaluateRetry` with retries=2 (budget exhausted) returns `shouldRetry: false`.
4. **Test:** `evaluateRetry` with retries > MAX_RETRIES returns `shouldRetry: false`.
5. **Test:** `RetryContext` includes `previousError`, `storyId`, `branchName`, `retryNumber`.
6. **Implement:** `evaluateRetry()` function.

### Phase C: Block Propagator (Red-Green-Refactor)

1. **Test:** Single failed story with no dependents returns empty `blockedStories`.
2. **Test:** Single direct dependent is blocked with `blockedBy: [failedStoryId]`.
3. **Test:** Transitive chain: A fails, B depends on A, C depends on B -- both B and C blocked.
4. **Test:** Diamond: A fails, B and C depend on A, D depends on B and C -- all three blocked.
5. **Test:** Non-dependent stories are not included in `blockedStories`.
6. **Test:** Already-failed or already-blocked stories in the path are still traversed (propagation continues through them).
7. **Test:** Story not in DAG returns empty `blockedStories` (defensive).
8. **Test:** `blockedBy` chains are correct: direct dependents have `[failedId]`, transitive dependents have `[intermediateId]`.
9. **Implement:** `propagateBlocks()` using BFS on `DagNode.blocks`.

### Phase D: Barrel Exports + Domain Integration

1. Create `src/domain/failure/index.ts` with all exports.
2. Modify `src/domain/index.ts` to add `export * from "./failure/index.js";`.
3. Verify `npx tsc --noEmit` passes.

### Phase E: Refinement

1. Verify coverage >= 95% line, >= 90% branch.
2. Run `npx tsc --noEmit` for type checking.
3. Verify all Gherkin scenarios from the story are covered.

---

## 12. Test File Structure

```
tests/
  domain/
    failure/
      retry-evaluator.test.ts       # Pure function tests for retry logic
      block-propagator.test.ts       # Pure function tests for BFS propagation
      helpers.ts                     # Test factories (reuses createDag from implementation-map helpers)
```

Test helpers should reuse `createDag` and `createDagNode` from `tests/domain/implementation-map/helpers.ts` or duplicate the factories locally if cross-directory import is undesirable. The existing `createDag()` helper produces `Map<string, DagNode>` which is exactly what `propagateBlocks()` consumes.

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario | Test File | Phase |
|-----------------|-----------|-------|
| Retry com SUCCESS na primeira tentativa | `retry-evaluator.test.ts` | B |
| Retry falha duas vezes e marca FAILED | `retry-evaluator.test.ts` | B |
| Retry nao ultrapassa budget de 2 (RULE-005) | `retry-evaluator.test.ts` | B |
| Block propagation direta -- dependente imediato | `block-propagator.test.ts` | C |
| Block propagation transitiva -- dependente indireto (RULE-006) | `block-propagator.test.ts` | C |
| Continuacao com stories nao-bloqueadas | `block-propagator.test.ts` (verifies non-dependents excluded) + existing `executable-stories.test.ts` (already excludes non-PENDING) | C |
| Retry recebe erro anterior como contexto | `retry-evaluator.test.ts` | B |
| Block propagation nao afeta stories sem dependencia | `block-propagator.test.ts` | C |

**Note:** Full integration scenarios (retry -> block -> checkpoint update -> continue) will be tested in story-0005-0010 when the orchestrator is implemented. This story tests the pure domain logic in isolation.

---

## 14. Key Observations

### 14.1 Two StoryStatus Definitions

The codebase currently has two `StoryStatus` definitions:
- `src/checkpoint/types.ts`: `const StoryStatus = { ... } as const` with type extraction
- `src/domain/implementation-map/types.ts`: `enum StoryStatus { ... }` with a `TODO(story-0005-0001)` marker

Both define the same 6 values (`PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `BLOCKED`, `PARTIAL`). The failure module should import from `implementation-map/types.ts` (same domain layer) to maintain dependency direction. The orchestrator (story-0005-0010) will bridge between the two when calling checkpoint functions.

### 14.2 Checkpoint Already Supports All Needed Fields

`StoryEntryUpdate` in `src/checkpoint/types.ts` already includes:
- `status?: StoryStatus` -- for FAILED and BLOCKED
- `retries?: number` -- for retry count
- `blockedBy?: readonly string[]` -- for block propagation chain

No changes to the checkpoint module are needed.

### 14.3 getExecutableStories Already Handles BLOCKED

The existing `getExecutableStories()` in `src/domain/implementation-map/executable-stories.ts` checks `storyState.status !== StoryStatus.PENDING` and excludes non-PENDING stories. BLOCKED and FAILED stories are automatically excluded from execution without any code changes.
