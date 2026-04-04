# Task Breakdown -- story-0005-0004: Implementation Map Parser

**Story:** `story-0005-0004.md`
**Architecture Plan:** `architecture-story-0005-0004.md`
**Implementation Plan:** `plan-story-0005-0004.md`

---

## Summary

25 tasks organized into 9 groups (A through I) following TDD Red-Green-Refactor cycles.
All groups are **sequential** because each builds on types and patterns established by the previous group.

**Legend:**
- **RED** = Write failing tests only (no production code)
- **GREEN** = Write minimum production code to make tests pass
- **REFACTOR** = Improve design without changing behavior
- **SETUP** = Infrastructure/scaffolding (no tests needed)

---

## Group A: Setup + Types

### TASK-1: Create directory structure and placeholder files
- **Type:** SETUP
- **File(s):** `src/domain/implementation-map/` (directory), `tests/domain/implementation-map/` (directory), `tests/fixtures/implementation-maps/` (directory)
- **Description:** Create the three directories. Create empty placeholder files for all 8 source files (`types.ts`, `markdown-parser.ts`, `dag-builder.ts`, `dag-validator.ts`, `phase-computer.ts`, `critical-path.ts`, `executable-stories.ts`, `index.ts`) so imports resolve during development.
- **Depends On:** none
- **Parallel:** no
- **Acceptance Criteria:**
  - All 8 source files exist under `src/domain/implementation-map/`
  - Test and fixture directories exist
  - `npx tsc --noEmit` passes (files can be empty or have minimal exports)

### TASK-2: Define all types, interfaces, enums, and custom errors in types.ts
- **Type:** SETUP
- **File(s):** `src/domain/implementation-map/types.ts`
- **Description:** Implement the complete `types.ts` with: `StoryStatus` enum (inline stub with `TODO(story-0005-0001)`), `ExecutionState` interface (inline stub), `DependencyMatrixRow`, `PhaseSummaryRow`, `DagNode`, `DagWarning`, `ParsedMap` interfaces, and custom error classes `CircularDependencyError`, `InvalidDagError`, `MapParseError` (all extending `Error`). All types must use `readonly` modifiers as specified in the architecture plan.
- **Depends On:** TASK-1
- **Parallel:** no
- **Acceptance Criteria:**
  - All 10 types/interfaces/enums/classes from Section 2.1 of the architecture plan are defined
  - Custom errors extend `Error` and carry contextual information
  - `StoryStatus` and `ExecutionState` have `TODO(story-0005-0001)` markers
  - `npx tsc --noEmit` passes
  - No imports from outside the module (zero external dependencies)

---

## Group B: Markdown Parser (TDD)

### TASK-3: RED -- Write failing tests for extractDependencyMatrix()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/markdown-parser.test.ts`, `tests/fixtures/implementation-maps/empty-map.md`, `tests/fixtures/implementation-maps/single-story.md`, `tests/fixtures/implementation-maps/linear-chain.md`, `tests/fixtures/implementation-maps/parallel-roots.md`
- **Description:** Write test cases following TPP ordering (simple to complex):
  1. Empty markdown (no Section 1 table) returns empty array
  2. Markdown with Section 1 containing a single story row (no dependencies)
  3. Multi-row matrix with comma-separated `blockedBy` and `blocks` values
  4. Rows with `-` (dash) representing empty dependency lists
  5. Malformed markdown (missing table header, garbled content) throws `MapParseError`
  6. Extra whitespace and inconsistent alignment are tolerated
  Create fixture `.md` files for each scenario. Tests must import `extractDependencyMatrix` from `markdown-parser.ts` and all must fail (function not yet implemented).
- **Depends On:** TASK-2
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 6 test cases exist
  - All tests fail with "not implemented" or import errors
  - Fixture files are created and used by tests
  - Tests validate structure of returned `DependencyMatrixRow[]`

### TASK-4: GREEN -- Implement extractDependencyMatrix()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/markdown-parser.ts`
- **Description:** Implement `extractDependencyMatrix(content: string): DependencyMatrixRow[]` using regex-based line-by-line parsing:
  - Locate Section 1 by `## 1.` header pattern
  - Parse table rows by splitting on `|`
  - Skip header separator rows (`---`)
  - Trim cells, handle `-` as empty, split comma-separated story IDs
  - Throw `MapParseError` on malformed input with line number context
  - Tolerate whitespace variations
- **Depends On:** TASK-3
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-3 tests pass
  - Function is pure (no side effects, no I/O)
  - Imports only from `./types.js`
  - Function length <= 25 lines (extract helpers as needed)

### TASK-5: RED -- Write failing tests for extractPhaseSummary()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/markdown-parser.test.ts`, `tests/fixtures/implementation-maps/` (additional fixtures if needed)
- **Description:** Add test cases for `extractPhaseSummary()` following TPP:
  1. Empty markdown (no Section 5 table) returns empty array
  2. Single phase with one story
  3. Multiple phases with multiple stories per phase
  4. Phase rows with layer and parallelism fields
  5. Malformed Section 5 throws `MapParseError`
- **Depends On:** TASK-4
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 5 test cases exist for `extractPhaseSummary()`
  - All new tests fail
  - Tests validate structure of returned `PhaseSummaryRow[]`

### TASK-6: GREEN -- Implement extractPhaseSummary()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/markdown-parser.ts`
- **Description:** Implement `extractPhaseSummary(content: string): PhaseSummaryRow[]`:
  - Locate Section 5 by `## 5.` header pattern
  - Parse table rows similarly to dependency matrix
  - Extract phase number, story list, layer, parallelism, prerequisite
  - Throw `MapParseError` on malformed input
- **Depends On:** TASK-5
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-5 tests pass
  - All TASK-3 tests still pass (no regression)
  - Function is pure, imports only from `./types.js`

### TASK-7: REFACTOR -- Markdown parser cleanup
- **Type:** REFACTOR
- **File(s):** `src/domain/implementation-map/markdown-parser.ts`
- **Description:** Extract shared table-parsing logic into a private helper (e.g., `parseMarkdownTable()`). Eliminate duplication between `extractDependencyMatrix` and `extractPhaseSummary`. Ensure no function exceeds 25 lines. Improve naming if needed. Verify all regex patterns are readable with named capture groups or comments.
- **Depends On:** TASK-6
- **Parallel:** no
- **Acceptance Criteria:**
  - All existing tests still pass (no behavior change)
  - No function exceeds 25 lines
  - Shared table-parsing logic extracted to a helper
  - Code follows naming conventions from Rule 03

---

## Group C: DAG Builder (TDD)

### TASK-8: RED -- Write failing tests for buildDag()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/dag-builder.test.ts`
- **Description:** Write test cases following TPP:
  1. Empty rows produce empty `Map<string, DagNode>`
  2. Single root story (no dependencies) produces DAG with one node, phase -1 (not yet computed), `isOnCriticalPath: false`
  3. Linear chain A -> B -> C produces 3 nodes with correct `blockedBy` and `blocks`
  4. Parallel roots with shared dependent: A and B both block C
  5. Verify that `DagNode.title` is preserved from input rows
  Tests use `DependencyMatrixRow` objects as input (not markdown).
- **Depends On:** TASK-7
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 5 test cases exist
  - All tests fail (function not yet implemented)
  - Tests validate the `Map<string, DagNode>` structure

### TASK-9: GREEN -- Implement buildDag()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/dag-builder.ts`
- **Description:** Implement `buildDag(rows: DependencyMatrixRow[]): Map<string, DagNode>`:
  - Iterate rows, create `DagNode` entries in a `Map`
  - Set `phase` to -1 (placeholder, computed later by phase-computer)
  - Set `isOnCriticalPath` to `false` (placeholder, set later by critical-path)
  - Preserve `title`, `blockedBy`, `blocks` from input rows
- **Depends On:** TASK-8
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-8 tests pass
  - Function is pure, imports only from `./types.js`
  - Function length <= 25 lines

### TASK-10: REFACTOR -- DAG builder cleanup
- **Type:** REFACTOR
- **File(s):** `src/domain/implementation-map/dag-builder.ts`
- **Description:** Review `buildDag()` for clarity. Extract helper functions if the mapping logic is dense. Ensure immutable patterns (use `Object.freeze` or spread for readonly enforcement). Verify naming conventions.
- **Depends On:** TASK-9
- **Parallel:** no
- **Acceptance Criteria:**
  - All existing tests still pass
  - Code adheres to SOLID principles (SRP in particular)
  - No mutable state leaks

---

## Group D: DAG Validator (TDD)

### TASK-11: RED -- Write failing tests for validateSymmetry(), detectCycles(), validateRoots()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/dag-validator.test.ts`, `tests/fixtures/implementation-maps/cyclic-map.md`, `tests/fixtures/implementation-maps/asymmetric-map.md`
- **Description:** Write test cases organized in three `describe` blocks:
  **validateSymmetry:**
  1. Symmetric DAG produces empty warnings array
  2. Asymmetric DAG (A blocks B, but B does not list A in blockedBy) produces a `DagWarning` with type `"asymmetric-dependency"`
  3. Auto-correction: after `validateSymmetry`, the missing edge is added to the DAG
  **detectCycles:**
  4. Acyclic DAG does not throw
  5. Simple cycle (A -> B -> A) throws `CircularDependencyError` with cycle chain in message
  6. Complex cycle (A -> B -> C -> A) throws with full chain
  **validateRoots:**
  7. DAG with roots does not throw
  8. DAG where every node has dependencies throws `InvalidDagError`
- **Depends On:** TASK-10
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 8 test cases across the three functions
  - All tests fail
  - Tests verify error types, error messages (containing cycle chain), and warning structures

### TASK-12: GREEN -- Implement validateSymmetry(), detectCycles(), validateRoots()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/dag-validator.ts`
- **Description:** Implement three validation functions:
  - `validateSymmetry(dag)`: Scan all nodes. For each `A.blocks` entry B, verify B has A in `blockedBy` (and vice versa). Emit `DagWarning` for mismatches. Auto-correct by creating a new `DagNode` with the missing edge added (maintain immutability -- return a corrected DAG or mutate the Map).
  - `detectCycles(dag)`: DFS with three-color marking (WHITE/GRAY/BLACK). On back-edge detection, reconstruct cycle chain from DFS stack. Throw `CircularDependencyError` with message format `"Circular dependency detected: A -> B -> C -> A"`.
  - `validateRoots(dag)`: Check that at least one node has empty `blockedBy`. Throw `InvalidDagError` if none found.
- **Depends On:** TASK-11
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-11 tests pass
  - All previous tests still pass
  - Cycle detection is O(V+E)
  - Functions are pure (except for controlled Map mutation in symmetry correction)

### TASK-13: REFACTOR -- Validator cleanup
- **Type:** REFACTOR
- **File(s):** `src/domain/implementation-map/dag-validator.ts`
- **Description:** Extract DFS traversal into a named helper. Ensure each public function is <= 25 lines. Verify error messages carry sufficient context. Consider extracting the three-color enum as a private const.
- **Depends On:** TASK-12
- **Parallel:** no
- **Acceptance Criteria:**
  - All existing tests still pass
  - No function exceeds 25 lines
  - DFS logic is isolated in a named helper

---

## Group E: Phase Computer (TDD)

### TASK-14: RED -- Write failing tests for computePhases()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/phase-computer.test.ts`
- **Description:** Write test cases following TPP:
  1. Single story (root) is in phase 0
  2. All stories are roots -> all in phase 0
  3. Linear chain A -> B -> C produces phases 0, 1, 2
  4. Diamond: A (root), B depends on A, C depends on A, D depends on B and C -> A=0, B=0+1=1, C=0+1=1, D=max(1,1)+1=2
  5. Verify returned `Map<number, string[]>` groups stories correctly per phase
- **Depends On:** TASK-13
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 5 test cases exist
  - All tests fail
  - Tests use pre-built `Map<string, DagNode>` objects as input (not markdown)

### TASK-15: GREEN -- Implement computePhases()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/phase-computer.ts`
- **Description:** Implement `computePhases(dag: Map<string, DagNode>): Map<number, string[]>`:
  - Phase 0: nodes with empty `blockedBy`
  - Phase N: nodes whose ALL dependencies are in phases 0..N-1
  - Use iterative BFS-like approach: resolve all nodes whose dependencies are already resolved, then advance phase number
  - Return `Map<phase, storyId[]>`
  - Update each `DagNode.phase` in the DAG map (create new node objects to maintain immutability pattern)
- **Depends On:** TASK-14
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-14 tests pass
  - All previous tests still pass
  - Function is pure, imports only from `./types.js`
  - Function length <= 25 lines

---

## Group F: Critical Path (TDD)

### TASK-16: RED -- Write failing tests for findCriticalPath()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/critical-path.test.ts`
- **Description:** Write test cases following TPP:
  1. Single story is its own critical path: `["0001"]`
  2. Linear chain A -> B -> C: critical path is `["A", "B", "C"]`
  3. Diamond graph where one branch is longer: selects the longer branch
  4. Two branches of equal length: deterministic selection (e.g., alphabetical tie-break)
  5. Complex graph with multiple convergence points
- **Depends On:** TASK-15
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 5 test cases exist
  - All tests fail
  - Tests validate both the returned `string[]` path and its ordering

### TASK-17: GREEN -- Implement findCriticalPath()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/critical-path.ts`
- **Description:** Implement `findCriticalPath(dag: Map<string, DagNode>, phases: Map<number, string[]>): string[]`:
  - Use topological-sort-based longest path algorithm:
    1. Topological sort of DAG (guaranteed acyclic after validation)
    2. Initialize distances to 0 for all nodes
    3. Process in topological order; for each node, update distances of dependents
    4. Find node with longest distance
    5. Backtrack to reconstruct the full critical path
  - Also implement `markCriticalPath(dag, path)` to set `isOnCriticalPath: true` on nodes in the path
- **Depends On:** TASK-16
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-16 tests pass
  - All previous tests still pass
  - Algorithm is O(V+E)
  - Function is pure, imports only from `./types.js`

---

## Group G: Executable Stories (TDD)

### TASK-18: RED -- Write failing tests for getExecutableStories()
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/executable-stories.test.ts`
- **Description:** Write test cases following TPP:
  1. All stories are PENDING with no dependencies -> returns all story IDs
  2. Story with unsatisfied dependency (dep is PENDING) -> excluded
  3. Story with all dependencies SUCCESS -> included
  4. Story that is already SUCCESS or IN_PROGRESS -> excluded (not PENDING)
  5. Critical path stories sorted before non-critical-path stories
  6. No executable stories (all blocked or completed) -> returns empty array
  7. Mixed state: 5 stories, 2 executable, verify ordering matches Gherkin scenario
- **Depends On:** TASK-17
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 7 test cases exist
  - All tests fail
  - Tests construct `ParsedMap` and `ExecutionState` objects manually
  - Tests verify both content and ordering of returned `string[]`

### TASK-19: GREEN -- Implement getExecutableStories()
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/executable-stories.ts`
- **Description:** Implement `getExecutableStories(parsedMap: ParsedMap, executionState: ExecutionState): string[]`:
  - Filter stories where: status is `PENDING` in executionState AND all `blockedBy` stories have status `SUCCESS`
  - Sort results: critical path stories first (`isOnCriticalPath: true`), then alphabetically by story ID
  - Return `string[]` of executable story IDs
- **Depends On:** TASK-18
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-18 tests pass
  - All previous tests still pass
  - Function is pure, imports only from `./types.js`
  - Function length <= 25 lines

---

## Group H: Facade + Integration (TDD)

### TASK-20: RED -- Write failing tests for parseImplementationMap() facade
- **Type:** RED
- **File(s):** `tests/domain/implementation-map/parse-implementation-map.test.ts`
- **Description:** Write end-to-end test cases using markdown strings as input (not pre-parsed objects):
  1. Empty map (no stories in table) returns `ParsedMap` with empty stories, empty phases, totalPhases=0
  2. Single root story returns correct `ParsedMap` with one node in phase 0, critical path of length 1
  3. Linear chain (3 stories) returns correct phases [0,1,2], critical path of 3 stories, totalPhases=3
  4. Parallel stories with shared dependent: correct phase assignment and critical path
  5. Asymmetric dependency produces warning but still returns valid `ParsedMap`
  6. Cyclic dependency throws `CircularDependencyError`
  These tests correspond directly to the Gherkin acceptance criteria in the story.
- **Depends On:** TASK-19
- **Parallel:** no
- **Acceptance Criteria:**
  - At least 6 test cases exist covering all Gherkin scenarios (except real map, which is TASK-22)
  - All tests fail (facade not yet implemented)
  - Tests use synthetic markdown fixture strings

### TASK-21: GREEN -- Implement parseImplementationMap() facade and barrel exports
- **Type:** GREEN
- **File(s):** `src/domain/implementation-map/index.ts`
- **Description:** Implement the `parseImplementationMap(content: string): ParsedMap` facade function that orchestrates the full pipeline:
  1. `extractDependencyMatrix(content)` -- parse Section 1
  2. `buildDag(rows)` -- create adjacency list
  3. `validateSymmetry(dag)` -- collect warnings, auto-fix
  4. `detectCycles(dag)` -- throw if cyclic
  5. `validateRoots(dag)` -- throw if no roots
  6. `computePhases(dag)` -- assign phase numbers
  7. `findCriticalPath(dag, phases)` -- identify longest path
  8. `markCriticalPath(dag, path)` -- set `isOnCriticalPath` flags
  9. Assemble and return `ParsedMap`
  Also set up barrel exports for all public types, functions, and errors.
- **Depends On:** TASK-20
- **Parallel:** no
- **Acceptance Criteria:**
  - All TASK-20 tests pass
  - All previous tests still pass
  - Facade function <= 25 lines (delegates to sub-functions)
  - Barrel exports all public types, errors, `parseImplementationMap`, and `getExecutableStories`

### TASK-22: Integration test with real epic-0005 IMPLEMENTATION-MAP.md
- **Type:** GREEN
- **File(s):** `tests/domain/implementation-map/parse-implementation-map.test.ts`, `tests/fixtures/implementation-maps/epic-0005-map.md`
- **Description:** Add an integration test that reads the actual `docs/stories/epic-0005/IMPLEMENTATION-MAP.md` (copied as a fixture) and verifies:
  - All stories are extracted (verify count matches the map)
  - Phases are computed (verify totalPhases)
  - Critical path is identified (verify it is non-empty and contains expected stories)
  - No warnings (real maps should be symmetric)
  This corresponds to the Gherkin scenario "Parsing do implementation map real".
- **Depends On:** TASK-21
- **Parallel:** no
- **Acceptance Criteria:**
  - Integration test passes against the real epic-0005 map
  - Story count, phase count, and critical path assertions are correct
  - No `CircularDependencyError` or `InvalidDagError` thrown

---

## Group I: Finalization

### TASK-23: Re-export implementation-map module from src/domain/index.ts
- **Type:** SETUP
- **File(s):** `src/domain/index.ts`
- **Description:** Add `export * from "./implementation-map/index.js";` to `src/domain/index.ts` under a new comment section `// --- STORY-0005-0004: Implementation Map Parser ---`. Verify the re-export does not cause naming conflicts with existing exports.
- **Depends On:** TASK-22
- **Parallel:** no
- **Acceptance Criteria:**
  - `src/domain/index.ts` re-exports the implementation-map barrel
  - `npx tsc --noEmit` passes
  - No naming conflicts with existing domain exports

### TASK-24: Coverage validation
- **Type:** SETUP
- **File(s):** (no new files)
- **Description:** Run `npx vitest run --coverage` scoped to the `src/domain/implementation-map/` module. Verify:
  - Line coverage >= 95%
  - Branch coverage >= 90%
  If thresholds are not met, identify uncovered branches and add targeted tests.
- **Depends On:** TASK-23
- **Parallel:** yes (can run in parallel with TASK-25)
- **Acceptance Criteria:**
  - Line coverage >= 95% for all files in `src/domain/implementation-map/`
  - Branch coverage >= 90% for all files in `src/domain/implementation-map/`
  - Coverage report generated

### TASK-25: Type checking and final validation
- **Type:** SETUP
- **File(s):** (no new files)
- **Description:** Run `npx tsc --noEmit` to verify the entire project compiles cleanly. Verify:
  - Zero compiler errors
  - Zero compiler warnings
  - All `TODO(story-0005-0001)` markers are documented and intentional
  - No imports violate the dependency direction rules (no imports from assembler, CLI, config, models, node:fs, node:path)
- **Depends On:** TASK-23
- **Parallel:** yes (can run in parallel with TASK-24)
- **Acceptance Criteria:**
  - `npx tsc --noEmit` exits with code 0
  - No dependency direction violations
  - All TODO markers reviewed and documented

---

## Dependency Graph

```
TASK-1 → TASK-2 → TASK-3 → TASK-4 → TASK-5 → TASK-6 → TASK-7
                                                           ↓
         TASK-8 → TASK-9 → TASK-10 → TASK-11 → TASK-12 → TASK-13
                                                            ↓
         TASK-14 → TASK-15 → TASK-16 → TASK-17 → TASK-18 → TASK-19
                                                              ↓
         TASK-20 → TASK-21 → TASK-22 → TASK-23 → TASK-24 (parallel)
                                                  ↓
                                                  TASK-25 (parallel with TASK-24)
```

All 25 tasks form a **single sequential chain** from TASK-1 through TASK-23, with TASK-24 and TASK-25 parallelizable at the end. This strict ordering ensures each TDD cycle builds on validated, passing code from the previous cycle.

---

## Traceability Matrix

| Gherkin Scenario | Tasks |
|-----------------|-------|
| Empty map (no stories) | TASK-3, TASK-20 |
| Single root story | TASK-3, TASK-8, TASK-14, TASK-16, TASK-20 |
| Linear dependencies | TASK-3, TASK-8, TASK-14, TASK-16, TASK-20 |
| Parallelism | TASK-3, TASK-8, TASK-14, TASK-20 |
| Asymmetric dependency warning | TASK-11, TASK-20 |
| Cycle detection | TASK-11, TASK-20 |
| Executable stories with partial state | TASK-18 |
| Critical path prioritization | TASK-18 |
| Real map (epic-0005) | TASK-22 |

---

## Estimated Effort

| Group | Tasks | Estimated Lines (prod) | Estimated Lines (test) |
|-------|-------|----------------------|----------------------|
| A: Setup + Types | 1-2 | ~80 | 0 |
| B: Markdown Parser | 3-7 | ~100 | ~150 |
| C: DAG Builder | 8-10 | ~60 | ~80 |
| D: DAG Validator | 11-13 | ~80 | ~120 |
| E: Phase Computer | 14-15 | ~50 | ~70 |
| F: Critical Path | 16-17 | ~60 | ~80 |
| G: Executable Stories | 18-19 | ~40 | ~100 |
| H: Facade + Integration | 20-22 | ~30 | ~120 |
| I: Finalization | 23-25 | ~5 | 0 |
| **Total** | **25** | **~505** | **~720** |
