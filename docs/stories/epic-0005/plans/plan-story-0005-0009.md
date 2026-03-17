# Implementation Plan -- story-0005-0009: Partial Execution (`--phase N`, `--story XXXX-YYYY`)

**Story:** `story-0005-0009.md`
**Depends on:** story-0005-0005 (Core Loop)
**Blocks:** story-0005-0014

---

## 1. Affected Layers and Components

| Layer | Impact | Details |
|-------|--------|---------|
| `src/domain/implementation-map/` | **NEW file** | New `partial-execution.ts` with precondition validation functions |
| `src/domain/implementation-map/types.ts` | **MODIFY** | Add `PartialExecutionMode` discriminated union and `PrerequisiteResult` interface |
| `src/domain/implementation-map/index.ts` | **MODIFY** | Re-export new `partial-execution.ts` functions |
| `src/exceptions.ts` | **MODIFY** | Add `PartialExecutionError` class |
| `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | **MODIFY** | Add Partial Execution section documenting `--phase` and `--story` behavior |
| `resources/github-skills-templates/dev/x-dev-epic-implement.md` | **MODIFY** | Mirror the Partial Execution section (dual-copy consistency RULE-001) |

This story is pure domain logic (precondition validation) plus template content updates. No assembler, CLI, config, or adapter changes.

---

## 2. New Classes/Interfaces to Create

### 2.1 Types (`src/domain/implementation-map/types.ts` -- additions)

| Type | Kind | Description |
|------|------|-------------|
| `PhaseExecutionMode` | interface | `{ kind: "phase"; phase: number }` -- execute a single phase |
| `StoryExecutionMode` | interface | `{ kind: "story"; storyId: string }` -- execute a single story |
| `FullExecutionMode` | interface | `{ kind: "full" }` -- execute all phases (default) |
| `PartialExecutionMode` | discriminated union | `PhaseExecutionMode \| StoryExecutionMode \| FullExecutionMode` |
| `PrerequisiteResult` | interface | `{ valid: boolean; error?: string; unsatisfiedDeps?: string[] }` |

### 2.2 Functions (`src/domain/implementation-map/partial-execution.ts` -- NEW)

| Function | Signature | Lines Est. | Description |
|----------|-----------|------------|-------------|
| `parsePartialExecutionMode` | `(phase?: number, storyId?: string) => PartialExecutionMode` | ~15 | Validates mutual exclusivity; returns typed mode discriminant. Throws `PartialExecutionError` if both provided. |
| `validatePhasePrerequisites` | `(phase: number, parsedMap: ParsedMap, executionState: ExecutionState) => PrerequisiteResult` | ~25 | Validates: (1) phase exists in parsedMap, (2) all stories in phases 0..N-1 have SUCCESS status. Returns `{ valid: true }` or `{ valid: false, error: "..." }`. |
| `validateStoryPrerequisites` | `(storyId: string, parsedMap: ParsedMap, executionState: ExecutionState) => PrerequisiteResult` | ~25 | Validates: (1) story exists in parsedMap, (2) all `blockedBy` deps have SUCCESS status. Returns `{ valid: true }` or `{ valid: false, error: "...", unsatisfiedDeps: [...] }`. |
| `getStoriesForPhase` | `(phase: number, parsedMap: ParsedMap) => string[]` | ~10 | Extracts story IDs for a specific phase from parsedMap.phases. |

### 2.3 Error Class (`src/exceptions.ts` -- addition)

| Class | Extends | Fields | Description |
|-------|---------|--------|-------------|
| `PartialExecutionError` | `Error` | `code: string`, `context: Record<string, unknown>` | Thrown for all partial execution precondition failures. Codes: `MUTUAL_EXCLUSIVITY`, `INVALID_PHASE`, `INCOMPLETE_PHASES`, `STORY_NOT_FOUND`, `UNSATISFIED_DEPS`. |

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| `src/domain/implementation-map/types.ts` | Add 4 type definitions at end of file | `PartialExecutionMode` union and `PrerequisiteResult` interface |
| `src/domain/implementation-map/index.ts` | Add `export * from "./partial-execution.js";` and barrel exports for new types | Expose partial execution functions through the domain barrel |
| `src/exceptions.ts` | Add `PartialExecutionError` class | New error class for precondition failures |
| `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | Add "Partial Execution" section between Input Parsing and Prerequisites | Document `--phase` / `--story` behavior, preconditions, error messages |
| `resources/github-skills-templates/dev/x-dev-epic-implement.md` | Mirror partial execution content | Dual-copy consistency (RULE-001) |

---

## 4. Dependency Direction Validation

```
src/domain/implementation-map/partial-execution.ts
  --> ./types.ts (PartialExecutionMode, PrerequisiteResult, ParsedMap, ExecutionState, StoryStatus)
  --> ../exceptions.ts? NO -- PartialExecutionError lives in src/exceptions.ts

src/exceptions.ts
  --> (no imports, standalone)
```

**Revised dependency approach:** `partial-execution.ts` is a pure domain module. It must NOT import from `src/exceptions.ts` (which is an infrastructure-level module shared across layers). Instead, the validation functions return `PrerequisiteResult` objects (value objects with error details). The **caller** (orchestrator in the SKILL.md template) decides whether to throw or display the error.

This means `PartialExecutionError` goes in `src/exceptions.ts` for use by consumers, but `partial-execution.ts` itself only returns result objects -- no throws.

**Exception:** `parsePartialExecutionMode` could throw for mutual exclusivity since that is an argument parsing concern. However, to keep the domain module pure, it will also return a result-style value. The caller handles error display.

**Final dependency graph:**

```
src/domain/implementation-map/partial-execution.ts
  --> ./types.ts (within module only)

src/domain/implementation-map/index.ts
  --> ./partial-execution.ts (re-export)

src/exceptions.ts
  --> (standalone, PartialExecutionError added for consumer use)
```

**Verification checklist:**
- [ ] `partial-execution.ts` imports ONLY from `./types.ts`
- [ ] No import from `src/assembler/`
- [ ] No import from `src/cli*.ts`
- [ ] No import from `src/config.ts`
- [ ] No import from `src/models.ts`
- [ ] No import from `node:fs` or `node:path`
- [ ] No import from any npm package
- [ ] Only standard library types used (Map, Set, Array, string, number, boolean)
- [ ] `PartialExecutionError` in `src/exceptions.ts` has no domain imports

---

## 5. Integration Points

| Consumer | How It Integrates | When |
|----------|-------------------|------|
| SKILL.md template (orchestrator) | Calls `parsePartialExecutionMode()` to determine execution mode, then calls `validatePhasePrerequisites()` or `validateStoryPrerequisites()`. On invalid result, emits error and aborts. On valid, uses `getStoriesForPhase()` or dispatches single story. | At SKILL.md template invocation time (runtime, AI agent) |
| story-0005-0005 (Core Loop) | Core loop already uses `getExecutableStories()`. Phase mode filters stories via `getStoriesForPhase()` before entering the loop. Story mode bypasses the loop entirely and dispatches a single subagent. | Integration happens in the SKILL.md template logic |
| story-0005-0014 | Blocked by this story; will extend partial execution with additional modes or error recovery. | After this story is complete |
| Checkpoint Engine (`src/checkpoint/`) | `validatePhasePrerequisites` reads `ExecutionState.stories` to check status. `validateStoryPrerequisites` reads `ExecutionState.stories` for dependency status. Both use the checkpoint types but access them through `implementation-map/types.ts` stubs. | Read-only dependency on checkpoint state |

**Note on type stubs:** The `implementation-map/types.ts` file already defines `StoryStatus` and `ExecutionState` as inline stubs (with `TODO(story-0005-0001)` markers). The new validation functions use these existing stubs. No new stub is needed.

---

## 6. Configuration Changes

N/A -- No environment variables, config files, or template variables. The partial execution logic is configuration-free. The `--phase` and `--story` flags are parsed from the user's command input at AI agent runtime, not from application configuration.

---

## 7. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| `ExecutionState` stub diverges from checkpoint engine types | Medium | Likely | The `implementation-map/types.ts` stub has a simpler `ExecutionState` than `checkpoint/types.ts`. Validation functions only need `stories[id].status`, which both define. Add a `TODO(story-0005-0001)` note. After story-0005-0001 merges, mechanical refactor to use real import. |
| Phase numbering off-by-one | Medium | Medium | Phases are 0-indexed in `ParsedMap.phases`. Tests must cover boundary: phase 0 (no prereqs needed), phase N-1 (last valid phase), phase N (invalid). |
| Story ID format mismatch | Low | Low | Story IDs in `ParsedMap.stories` keys use the format from IMPLEMENTATION-MAP.md (e.g., `0042-0003`). Ensure `--story` argument matches exactly. Validation uses strict equality lookup in the Map. |
| Incomplete checkpoint (no checkpoint file for `--story` mode) | Medium | Medium | `--story` mode requires a checkpoint. If no checkpoint exists, `validateStoryPrerequisites` returns `{ valid: false, error: "Checkpoint required for --story mode" }`. This is documented in the story requirements. |
| SKILL.md content changes break existing tests | Low | Certain | The test file `x-dev-epic-implement-content.test.ts` checks for section presence and keyword existence. New content adds sections; existing sections are not removed. Tests should pass as-is. New tests will cover the added sections. |
| Dual-copy drift between Claude and GitHub templates | Medium | Medium | Update both templates in the same commit. Add test assertions for partial execution keywords in the dual-copy consistency test block. |

---

## 8. Implementation Order (TDD)

Implementation follows inner-to-outer order, with tests written first (Red-Green-Refactor).

### Phase A: Types (no tests needed)

1. Add `PhaseExecutionMode`, `StoryExecutionMode`, `FullExecutionMode`, `PartialExecutionMode` union, and `PrerequisiteResult` to `src/domain/implementation-map/types.ts`.
2. Add `PartialExecutionError` to `src/exceptions.ts`.
3. Run `npx tsc --noEmit` to verify types compile.

### Phase B: `parsePartialExecutionMode` (Red-Green-Refactor)

1. **Test:** Neither flag provided returns `{ kind: "full" }`.
2. **Test:** `--phase 2` returns `{ kind: "phase", phase: 2 }`.
3. **Test:** `--story 0042-0003` returns `{ kind: "story", storyId: "0042-0003" }`.
4. **Test:** Both flags provided returns error result (mutual exclusivity).
5. **Implement:** `parsePartialExecutionMode()`.

### Phase C: `validatePhasePrerequisites` (Red-Green-Refactor)

1. **Test:** Phase 0 always returns `{ valid: true }` (no prior phases to check).
2. **Test:** Phase 2 with phases 0 and 1 all SUCCESS returns `{ valid: true }`.
3. **Test:** Phase 2 with phase 1 having PENDING story returns `{ valid: false, error: "Phases 0..1 must be complete before phase 2" }`.
4. **Test:** Phase N exceeding max phase returns `{ valid: false, error: "Phase 5 does not exist. Max phase is 3." }`.
5. **Test:** Phase 2 with phase 0 SUCCESS but phase 1 having FAILED story returns invalid.
6. **Implement:** `validatePhasePrerequisites()`.

### Phase D: `validateStoryPrerequisites` (Red-Green-Refactor)

1. **Test:** Story with no dependencies returns `{ valid: true }`.
2. **Test:** Story with all deps SUCCESS returns `{ valid: true }`.
3. **Test:** Story with one dep PENDING returns `{ valid: false, unsatisfiedDeps: ["0042-0002"] }`.
4. **Test:** Story not found in map returns `{ valid: false, error: "Story 0042-9999 not found in implementation map" }`.
5. **Test:** Story with multiple unsatisfied deps lists all in `unsatisfiedDeps`.
6. **Implement:** `validateStoryPrerequisites()`.

### Phase E: `getStoriesForPhase` (Red-Green-Refactor)

1. **Test:** Valid phase returns story IDs from `parsedMap.phases`.
2. **Test:** Invalid phase returns empty array.
3. **Test:** Phase with multiple stories returns all IDs.
4. **Implement:** `getStoriesForPhase()`.

### Phase F: Index Barrel Update

1. Add exports to `src/domain/implementation-map/index.ts`.
2. Run `npx tsc --noEmit` to verify.

### Phase G: SKILL.md Template Updates

1. Add "Partial Execution" section to `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`.
2. Mirror content in `resources/github-skills-templates/dev/x-dev-epic-implement.md`.
3. Content includes:
   - Mutual exclusivity rule for `--phase` and `--story`
   - `--phase N` flow: validate phases 0..N-1, filter stories, run integrity gate
   - `--story XXXX-YYYY` flow: validate deps, dispatch single subagent, no integrity gate
   - Error message specifications for all failure modes

### Phase H: Content Tests Update

1. Add test assertions for partial execution keywords in `tests/node/content/x-dev-epic-implement-content.test.ts`.
2. Add new describe block: `"x-dev-epic-implement SKILL.md -- partial execution"`.
3. Verify dual-copy consistency for new terms: `"mutually exclusive"`, `"--phase"`, `"--story"`, `"integrity gate"`.

### Phase I: Refinement

1. Verify coverage >= 95% line, >= 90% branch.
2. Run `npx tsc --noEmit` for type checking.
3. Verify all existing tests still pass (no regressions).

---

## 9. Test File Structure

```
tests/
  domain/
    implementation-map/
      partial-execution.test.ts          # Unit tests for all 4 functions
  node/
    content/
      x-dev-epic-implement-content.test.ts  # Modified: add partial execution assertions
```

Estimated new test count: ~20 unit tests in `partial-execution.test.ts` + ~6 content assertions in the existing content test file.

---

## 10. Acceptance Criteria Traceability

| Gherkin Scenario | Test Location | Implementation Phase |
|-----------------|---------------|---------------------|
| Phase 2 with phases 0-1 complete | `partial-execution.test.ts` (validatePhasePrerequisites) | C |
| Phase N with prior phase incomplete | `partial-execution.test.ts` (validatePhasePrerequisites) | C |
| Story with deps satisfied | `partial-execution.test.ts` (validateStoryPrerequisites) | D |
| Story with dep not satisfied | `partial-execution.test.ts` (validateStoryPrerequisites) | D |
| `--phase` and `--story` mutually exclusive | `partial-execution.test.ts` (parsePartialExecutionMode) | B |
| `--phase` with invalid number | `partial-execution.test.ts` (validatePhasePrerequisites) | C |
| `--story` with nonexistent ID | `partial-execution.test.ts` (validateStoryPrerequisites) | D |
| SKILL.md updated with partial execution | `x-dev-epic-implement-content.test.ts` | H |
| No integrity gate for single story | SKILL.md content (documented), content test assertion | G, H |

---

## 11. Function Signatures (Detailed)

### `parsePartialExecutionMode`

```typescript
function parsePartialExecutionMode(
  phase: number | undefined,
  storyId: string | undefined,
): PartialExecutionMode
```

Returns the appropriate mode variant. If both `phase` and `storyId` are defined, returns a special error indicator (the caller handles the error display). Since `PartialExecutionMode` is a discriminated union, the function can return `{ kind: "full" }`, `{ kind: "phase", phase }`, or `{ kind: "story", storyId }`.

For mutual exclusivity, two design options:
- **Option A (Recommended):** Throw `PartialExecutionError` -- this is an argument parsing error, not a domain precondition. Argument validation is a reasonable place for throwing.
- **Option B:** Return a fourth variant `{ kind: "error", message: string }`.

**Decision: Option A.** `parsePartialExecutionMode` throws `PartialExecutionError` with code `MUTUAL_EXCLUSIVITY` when both are provided. This keeps the return type clean (3 variants, no error variant) and the throw is justified since this is input validation, not domain logic.

### `validatePhasePrerequisites`

```typescript
function validatePhasePrerequisites(
  phase: number,
  parsedMap: ParsedMap,
  executionState: ExecutionState,
): PrerequisiteResult
```

Pure function. No side effects. Returns `{ valid: true }` or `{ valid: false, error: "..." }`.

### `validateStoryPrerequisites`

```typescript
function validateStoryPrerequisites(
  storyId: string,
  parsedMap: ParsedMap,
  executionState: ExecutionState,
): PrerequisiteResult
```

Pure function. No side effects. Returns `{ valid: true }` or `{ valid: false, error: "...", unsatisfiedDeps: [...] }`.

### `getStoriesForPhase`

```typescript
function getStoriesForPhase(
  phase: number,
  parsedMap: ParsedMap,
): readonly string[]
```

Pure function. Returns story IDs for the given phase, or empty array if phase does not exist.
