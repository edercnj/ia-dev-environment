# Test Plan -- story-0005-0004: Implementation Map Parser

**Story:** `story-0005-0004.md`
**Architecture Plan:** `architecture-story-0005-0004.md`
**Implementation Plan:** `plan-story-0005-0004.md`

**Framework:** Vitest (pool: forks, maxForks: 3, maxConcurrency: 5)
**Coverage Targets:** >= 95% line, >= 90% branch
**Test Naming:** `[functionUnderTest]_[scenario]_[expectedBehavior]`

---

## Test File Structure

```
tests/
  domain/
    implementation-map/
      markdown-parser.test.ts       # UT-01 through UT-12
      dag-builder.test.ts           # UT-13 through UT-18
      dag-validator.test.ts         # UT-19 through UT-28
      phase-computer.test.ts        # UT-29 through UT-34
      critical-path.test.ts         # UT-35 through UT-40
      executable-stories.test.ts    # UT-41 through UT-48
      parse-implementation-map.test.ts  # AT-01 through AT-09, IT-01 through IT-03
  fixtures/
    implementation-maps/
      empty-map.md                  # Empty dependency matrix
      single-story.md              # One root story, no deps
      linear-chain.md             # A -> B -> C linear chain
      parallel-roots.md           # Two roots, one dependent
      asymmetric-map.md           # A blocks B, but B missing A in blockedBy
      cyclic-map.md               # A -> B -> C -> A cycle
      five-stories-three-phases.md # 5 stories in 3 phases for executable tests
      epic-0005-map.md            # Copy of real docs/stories/epic-0005/IMPLEMENTATION-MAP.md
```

---

## 1. Acceptance Tests (AT-N) -- Outer Loop

These acceptance tests map 1:1 to the Gherkin scenarios in Section 7 of the story. They are written first and remain RED until all inner-loop unit tests drive the implementation to completion. All acceptance tests call `parseImplementationMap()` (the facade) or `getExecutableStories()`.

**Test File:** `tests/domain/implementation-map/parse-implementation-map.test.ts`

| ID | Gherkin Scenario | Description | Input/Setup | Expected Result | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| AT-01 | Empty map | `parseImplementationMap` with empty dependency matrix returns empty ParsedMap | Fixture: `empty-map.md` -- markdown with Section 1 header but no data rows | `stories.size === 0`, `phases.size === 0`, `totalPhases === 0`, `criticalPath === []`, `warnings === []` | TASK-types | yes |
| AT-02 | Single root story | `parseImplementationMap` with one story, no dependencies | Fixture: `single-story.md` -- one row: `story-0042-0001`, no blockedBy, no blocks | `stories.size === 1`, story has `phase: 0`, `blockedBy: []`, `phases === {0: ["story-0042-0001"]}`, `criticalPath === ["story-0042-0001"]` | TASK-types | yes |
| AT-03 | Linear dependencies | `parseImplementationMap` with chain 0001 -> 0002 -> 0003 | Fixture: `linear-chain.md` -- three rows forming a linear chain | `phases === {0: ["0001"], 1: ["0002"], 2: ["0003"]}`, `totalPhases === 3`, `criticalPath === ["0001", "0002", "0003"]` | TASK-types | yes |
| AT-04 | Parallelism | `parseImplementationMap` with two roots and a shared dependent | Fixture: `parallel-roots.md` -- 0001 (no deps), 0002 (no deps), 0003 (blocked by 0001, 0002) | `phases[0]` contains 0001 and 0002, `phases[1]` contains 0003, `criticalPath` contains 0003 | TASK-types | yes |
| AT-05 | Asymmetric dependency warning | `parseImplementationMap` with inconsistent blocks/blockedBy | Fixture: `asymmetric-map.md` -- 0001 lists blocks 0002, but 0002 does NOT list 0001 in blockedBy | `warnings.length >= 1`, warning type is `"asymmetric-dependency"`, 0002.blockedBy includes 0001 (auto-corrected) | TASK-types | yes |
| AT-06 | Cycle detection | `parseImplementationMap` with cyclic dependencies throws | Fixture: `cyclic-map.md` -- 0001 -> 0002 -> 0003 -> 0001 | Throws `CircularDependencyError` with message containing `"0001"`, `"0002"`, `"0003"` | TASK-types | yes |
| AT-07 | Executable stories with partial state | `getExecutableStories` filters by satisfied dependencies | Fixture: `five-stories-three-phases.md` + ExecutionState `{0001: SUCCESS, 0002: SUCCESS, 0003: PENDING, 0004: PENDING, 0005: PENDING}` where 0003 depends on 0001, 0004 depends on 0002, 0005 depends on 0003 | Returns `["0003", "0004"]` (deps satisfied). 0005 NOT included (0003 still PENDING) | TASK-types | yes |
| AT-08 | Critical path prioritization | `getExecutableStories` returns critical-path stories first | Same fixture as AT-07, but 0003 is on critical path, 0004 is not | 0003 appears before 0004 in result array | TASK-types | yes |
| AT-09 | Real map parsing (epic-0005) | `parseImplementationMap` with real epic-0005 map | Fixture: `epic-0005-map.md` (copy of `docs/stories/epic-0005/IMPLEMENTATION-MAP.md`) | 14 stories extracted, 6 phases computed, critical path includes `story-0005-0001`, `story-0005-0004`, `story-0005-0005`, `story-0005-0007`, `story-0005-0010`, `story-0005-0014` | TASK-types | yes |

> **Note on AT-09:** The story Gherkin references epic-0004 (17 stories, 4 phases). The epic-0005 map has 14 stories in 6 phases. The test plan uses epic-0005 as the primary real map since it is the project's own epic. An additional integration test (IT-02) covers epic-0004.

---

## 2. Unit Tests (UT-N) -- Inner Loop (TPP Order)

### 2.1 markdown-parser.ts -- `extractDependencyMatrix(content)`

**Test File:** `tests/domain/implementation-map/markdown-parser.test.ts`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-01 | `extractDependencyMatrix_emptyContent_returnsEmptyArray` | `content = ""` | Returns `[]` | 1 ({} -> nil) | none | yes |
| UT-02 | `extractDependencyMatrix_headerOnlyNoDataRows_returnsEmptyArray` | Content with `## 1. Matriz de Dependencias` header and table header row but no data rows | Returns `[]` | 1 ({} -> nil) | none | yes |
| UT-03 | `extractDependencyMatrix_singleRowNoDeps_returnsSingleRow` | Content with one story row: `story-0042-0001`, title, no blockedBy (`-`), no blocks (`-`), status | Returns `[{ storyId: "story-0042-0001", title: "...", blockedBy: [], blocks: [], status: "Pendente" }]` | 2 (nil -> constant) | none | yes |
| UT-04 | `extractDependencyMatrix_singleRowWithDeps_parsesCommaSeparatedIds` | Content with one row having `story-0042-0001` in blockedBy and `story-0042-0003, story-0042-0004` in blocks | Returns row with `blockedBy: ["story-0042-0001"]`, `blocks: ["story-0042-0003", "story-0042-0004"]` | 3 (constant -> constant+) | none | yes |
| UT-05 | `extractDependencyMatrix_multipleRows_returnsAllRows` | Content with 3 story rows forming a chain | Returns 3 `DependencyMatrixRow` objects with correct relationships | 4 (scalar -> collection) | none | yes |
| UT-06 | `extractDependencyMatrix_extraWhitespace_toleratesFormatting` | Content with extra spaces around `|` separators and inconsistent alignment | Returns correctly parsed rows (same as UT-05 data) | 5 (collection -> collection) | none | yes |
| UT-07 | `extractDependencyMatrix_dashSeparatorRow_skipsIt` | Content where the `| :--- | :--- |` separator row is present | Separator row is NOT included in results | 6 (unconditional -> conditional) | none | yes |
| UT-08 | `extractDependencyMatrix_statusVariations_preservesOriginal` | Content with statuses: `Pendente`, `Concluido`, `Em andamento` | Each row's `status` field matches the original text | 3 (constant -> constant+) | none | yes |

### 2.2 markdown-parser.ts -- `extractPhaseSummary(content)`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-09 | `extractPhaseSummary_emptyContent_returnsEmptyArray` | `content = ""` | Returns `[]` | 1 ({} -> nil) | none | yes |
| UT-10 | `extractPhaseSummary_singlePhaseRow_returnsSingleRow` | Content with Section 5 header and one phase row: `0, 0001/0002/0003, Foundation, 3 paralelas, -` | Returns `[{ phase: 0, stories: ["0001", "0002", "0003"], layer: "Foundation", parallelism: "3 paralelas", prerequisite: "-" }]` | 2 (nil -> constant) | none | yes |
| UT-11 | `extractPhaseSummary_multiplePhaseRows_returnsAllPhases` | Content with 3 phase rows (phases 0, 1, 2) | Returns 3 `PhaseSummaryRow` objects with correct phase numbers and story lists | 4 (scalar -> collection) | none | yes |
| UT-12 | `extractPhaseSummary_commaSeparatedStories_splitsCorrectly` | Content with phase row: `3, 0006/0007/0008/0009/0011/0013, Extensions, 6 paralelas, Fase 2` (comma-separated) | `stories` array contains all 6 story IDs | 5 (collection -> collection) | none | yes |

### 2.3 dag-builder.ts -- `buildDag(rows)`

**Test File:** `tests/domain/implementation-map/dag-builder.test.ts`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-13 | `buildDag_emptyRows_returnsEmptyMap` | `rows = []` | Returns `Map` with size 0 | 1 ({} -> nil) | none | yes |
| UT-14 | `buildDag_singleRootRow_returnsMapWithOneNode` | `rows = [{ storyId: "0001", ..., blockedBy: [], blocks: [] }]` | Map has 1 entry. Node has `storyId: "0001"`, `blockedBy: []`, `blocks: []`, `phase: -1` (uncomputed), `isOnCriticalPath: false` | 2 (nil -> constant) | none | yes |
| UT-15 | `buildDag_twoStoriesWithDependency_setsBlockedByAndBlocks` | `rows = [root 0001 blocks 0002, dependent 0002 blockedBy 0001]` | Map has 2 entries. 0001.blocks includes "0002". 0002.blockedBy includes "0001" | 3 (constant -> constant+) | none | yes |
| UT-16 | `buildDag_linearChainThreeStories_buildsCorrectAdjacency` | `rows = [0001 -> 0002 -> 0003]` (3-story chain) | Map has 3 entries with correct adjacency: 0001.blocks=[0002], 0002.blockedBy=[0001]/blocks=[0003], 0003.blockedBy=[0002] | 4 (scalar -> collection) | none | yes |
| UT-17 | `buildDag_parallelRootsWithSharedDependent_handlesMultipleDeps` | `rows = [0001 no deps blocks 0003, 0002 no deps blocks 0003, 0003 blockedBy 0001+0002]` | Map has 3 entries. 0003.blockedBy has length 2 containing both 0001 and 0002 | 5 (collection -> collection) | none | yes |
| UT-18 | `buildDag_preservesTitleFromRows_titlesMatchInput` | `rows = [{ storyId: "0001", title: "Execution State", ... }]` | Node at key "0001" has `title: "Execution State"` | 3 (constant -> constant+) | none | yes |

### 2.4 dag-validator.ts -- `validateSymmetry(dag)`

**Test File:** `tests/domain/implementation-map/dag-validator.test.ts`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-19 | `validateSymmetry_emptyDag_returnsNoWarnings` | `dag = new Map()` (empty) | Returns `[]` | 1 ({} -> nil) | none | yes |
| UT-20 | `validateSymmetry_symmetricDag_returnsNoWarnings` | DAG where 0001.blocks=[0002] and 0002.blockedBy=[0001] | Returns `[]` | 2 (nil -> constant) | none | yes |
| UT-21 | `validateSymmetry_asymmetricBlocksMissing_returnsWarningAndCorrects` | DAG where 0001.blocks=[0002] but 0002.blockedBy=[] | Returns 1 warning of type `"asymmetric-dependency"`. After validation, 0002.blockedBy includes 0001 (auto-corrected) | 4 (unconditional -> conditional) | none | yes |
| UT-22 | `validateSymmetry_asymmetricBlockedByMissing_returnsWarningAndCorrects` | DAG where 0002.blockedBy=[0001] but 0001.blocks does not include 0002 | Returns 1 warning of type `"asymmetric-dependency"`. After validation, 0001.blocks includes 0002 | 4 (unconditional -> conditional) | none | yes |
| UT-23 | `validateSymmetry_multipleAsymmetries_returnsMultipleWarnings` | DAG with 2 separate asymmetric edges | Returns 2 warnings, each describing the specific asymmetry | 5 (collection -> collection) | none | yes |

### 2.5 dag-validator.ts -- `detectCycles(dag)`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-24 | `detectCycles_emptyDag_doesNotThrow` | `dag = new Map()` | Does not throw | 1 ({} -> nil) | none | yes |
| UT-25 | `detectCycles_acyclicDag_doesNotThrow` | DAG: 0001 -> 0002 -> 0003 (linear, no cycles) | Does not throw | 2 (nil -> constant) | none | yes |
| UT-26 | `detectCycles_directCycleTwoNodes_throwsWithCycleChain` | DAG: 0001 -> 0002 -> 0001 | Throws `CircularDependencyError` with message containing both "0001" and "0002" | 4 (unconditional -> conditional) | none | yes |
| UT-27 | `detectCycles_indirectCycleThreeNodes_throwsWithFullChain` | DAG: 0001 -> 0002 -> 0003 -> 0001 | Throws `CircularDependencyError` with message containing "0001", "0002", "0003" in cycle format | 5 (collection -> collection) | none | yes |

### 2.6 dag-validator.ts -- `validateRoots(dag)`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-28a | `validateRoots_emptyDag_doesNotThrow` | `dag = new Map()` | Does not throw (vacuously valid) | 1 ({} -> nil) | none | yes |
| UT-28b | `validateRoots_dagWithRoots_doesNotThrow` | DAG with 0001 (no blockedBy) as root | Does not throw | 2 (nil -> constant) | none | yes |
| UT-28c | `validateRoots_dagWithNoRoots_throwsInvalidDagError` | DAG where every node has at least one blockedBy (no roots exist) | Throws `InvalidDagError` with descriptive message | 4 (unconditional -> conditional) | none | yes |

### 2.7 phase-computer.ts -- `computePhases(dag)`

**Test File:** `tests/domain/implementation-map/phase-computer.test.ts`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-29 | `computePhases_emptyDag_returnsEmptyMap` | `dag = new Map()` | Returns `Map` with size 0 | 1 ({} -> nil) | none | yes |
| UT-30 | `computePhases_singleRoot_returnsSinglePhaseZero` | DAG with one node (no deps) | Returns `Map { 0 => ["0001"] }` | 2 (nil -> constant) | none | yes |
| UT-31 | `computePhases_twoRoots_bothInPhaseZero` | DAG with 0001 and 0002, both no deps | Returns `Map { 0 => ["0001", "0002"] }` | 3 (constant -> constant+) | none | yes |
| UT-32 | `computePhases_linearChain_incrementalPhases` | DAG: 0001 -> 0002 -> 0003 | Returns `Map { 0 => ["0001"], 1 => ["0002"], 2 => ["0003"] }` | 4 (scalar -> collection) | none | yes |
| UT-33 | `computePhases_diamondDependency_correctPhaseAssignment` | DAG: 0001 -> 0002, 0001 -> 0003, 0002 -> 0004, 0003 -> 0004 | Returns `Map { 0 => ["0001"], 1 => ["0002", "0003"], 2 => ["0004"] }` | 5 (collection -> collection) | none | yes |
| UT-34 | `computePhases_updatesNodePhaseField_nodesReflectPhase` | DAG: 0001 (root) -> 0002 | After `computePhases`, node 0001 has `phase: 0`, node 0002 has `phase: 1` | 5 (collection -> collection) | none | yes |

### 2.8 critical-path.ts -- `findCriticalPath(dag, phases)`

**Test File:** `tests/domain/implementation-map/critical-path.test.ts`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-35 | `findCriticalPath_emptyDag_returnsEmptyArray` | `dag = new Map()`, `phases = new Map()` | Returns `[]` | 1 ({} -> nil) | none | yes |
| UT-36 | `findCriticalPath_singleNode_returnsSingleElementPath` | DAG with one root node, `phases = { 0: ["0001"] }` | Returns `["0001"]` | 2 (nil -> constant) | none | yes |
| UT-37 | `findCriticalPath_linearChain_returnsFullChain` | DAG: 0001 -> 0002 -> 0003, `phases = { 0: ["0001"], 1: ["0002"], 2: ["0003"] }` | Returns `["0001", "0002", "0003"]` | 4 (scalar -> collection) | none | yes |
| UT-38 | `findCriticalPath_twoParallelBranchesUnequalLength_selectsLongerBranch` | DAG: 0001 -> 0002 -> 0004 (long), 0001 -> 0003 (short). Phases: {0:[0001], 1:[0002,0003], 2:[0004]} | Returns path through the longer branch: `["0001", "0002", "0004"]` | 5 (collection -> collection) | none | yes |
| UT-39 | `findCriticalPath_diamondGraph_selectsLongestPath` | DAG: 0001 -> 0002 -> 0004, 0001 -> 0003 -> 0004. Phases: {0:[0001], 1:[0002,0003], 2:[0004]} | Returns `["0001", "0002", "0004"]` or `["0001", "0003", "0004"]` (either valid since same length). Critical path length === 3. | 5 (collection -> collection) | none | yes |
| UT-40 | `findCriticalPath_marksNodesOnCriticalPath_isOnCriticalPathTrue` | Same DAG as UT-37 (linear chain) | All nodes in the returned path have `isOnCriticalPath: true`. Nodes NOT on the path have `isOnCriticalPath: false`. | 6 (unconditional -> conditional) | none | yes |

### 2.9 executable-stories.ts -- `getExecutableStories(parsedMap, executionState)`

**Test File:** `tests/domain/implementation-map/executable-stories.test.ts`

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-41 | `getExecutableStories_emptyParsedMap_returnsEmptyArray` | `parsedMap` with empty stories and phases. `executionState` with empty stories. | Returns `[]` | 1 ({} -> nil) | none | yes |
| UT-42 | `getExecutableStories_singlePendingRootNoDeps_returnsIt` | `parsedMap` with one root story (no deps). `executionState = { "0001": { status: PENDING } }` | Returns `["0001"]` | 2 (nil -> constant) | none | yes |
| UT-43 | `getExecutableStories_singleStoryAlreadySuccess_returnsEmpty` | `parsedMap` with one story. `executionState = { "0001": { status: SUCCESS } }` | Returns `[]` (already completed) | 4 (unconditional -> conditional) | none | yes |
| UT-44 | `getExecutableStories_pendingWithSatisfiedDeps_returnsStory` | `parsedMap` with 0001 -> 0002. `executionState = { "0001": { status: SUCCESS }, "0002": { status: PENDING } }` | Returns `["0002"]` | 3 (constant -> constant+) | none | yes |
| UT-45 | `getExecutableStories_pendingWithUnsatisfiedDeps_excludesStory` | `parsedMap` with 0001 -> 0002. `executionState = { "0001": { status: PENDING }, "0002": { status: PENDING } }` | Returns `["0001"]` only (0002 excluded because 0001 is not SUCCESS) | 4 (unconditional -> conditional) | none | yes |
| UT-46 | `getExecutableStories_multipleSatisfied_returnsAll` | `parsedMap` with 5 stories in 3 phases. `executionState = { 0001: SUCCESS, 0002: SUCCESS, 0003: PENDING, 0004: PENDING, 0005: PENDING }`. 0003 depends on 0001, 0004 depends on 0002, 0005 depends on 0003. | Returns `["0003", "0004"]` | 5 (collection -> collection) | none | yes |
| UT-47 | `getExecutableStories_criticalPathFirst_sortedByCriticalPath` | Same setup as UT-46, but 0003 is on critical path and 0004 is not | 0003 appears before 0004 in the result | 6 (unconditional -> conditional) | none | yes |
| UT-48 | `getExecutableStories_noneExecutable_returnsEmptyArray` | `parsedMap` with 0001 -> 0002 -> 0003. `executionState = { 0001: FAILED, 0002: PENDING, 0003: PENDING }` | Returns `[]` (0002 and 0003 have unsatisfied deps, 0001 is FAILED not PENDING) | 6 (unconditional -> conditional) | none | yes |

---

## 3. Integration Tests (IT-N)

These tests use real `IMPLEMENTATION-MAP.md` files to validate end-to-end parsing against production data.

**Test File:** `tests/domain/implementation-map/parse-implementation-map.test.ts`

| ID | Description | Input/Setup | Expected Result | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|
| IT-01 | `parseImplementationMap_epicO005RealMap_extracts14StoriesIn6Phases` | Read `docs/stories/epic-0005/IMPLEMENTATION-MAP.md` as string input | 14 stories extracted. 6 phases computed (`totalPhases === 6`). Phase 0 has 3 stories (0001, 0002, 0003). Phase 3 has 6 stories. Critical path includes: `story-0005-0001`, `story-0005-0004`, `story-0005-0005`, `story-0005-0007`, `story-0005-0010`, `story-0005-0014`. No warnings. | all unit tests | yes |
| IT-02 | `parseImplementationMap_epic0004RealMap_extracts17StoriesIn4Phases` | Read `docs/stories/epic-0004/IMPLEMENTATION-MAP.md` as string input | 17 stories extracted. 4 phases computed (`totalPhases === 4`). Phase 0 has 5 stories. `story-0004-0005` blocks 7 stories. Critical path includes `story-0004-0005`, `story-0004-0013`, `story-0004-0017`. No warnings (assuming map is symmetric). | all unit tests | yes |
| IT-03 | `parseImplementationMap_epic0005Map_getExecutableStories_initialState_returnsPhase0` | Read epic-0005 map. Build `executionState` with all 14 stories as PENDING. | `getExecutableStories` returns Phase 0 stories: `["story-0005-0001", "story-0005-0002", "story-0005-0003"]` (roots with no dependencies, sorted by critical path -- 0001 first since it is on critical path) | all unit tests | yes |

---

## 4. TPP Order Summary

The unit tests are designed to be implemented in this order within each function file, guiding the implementation through progressively more complex transformations:

### markdown-parser.ts

```
Level 1: UT-01, UT-02, UT-09        -- Empty/degenerate inputs -> return []
Level 2: UT-03, UT-10               -- Single row -> parse one item
Level 3: UT-04, UT-08               -- Comma-separated deps, status variations
Level 4: UT-05, UT-11               -- Multiple rows -> iterate
Level 5: UT-06, UT-12               -- Format tolerance, complex splitting
Level 6: UT-07                       -- Conditional: skip separator rows
```

### dag-builder.ts

```
Level 1: UT-13                       -- Empty -> empty Map
Level 2: UT-14                       -- Single node
Level 3: UT-15, UT-18               -- Two nodes with dependency, title preservation
Level 4: UT-16                       -- Three-node chain
Level 5: UT-17                       -- Multiple deps per node
```

### dag-validator.ts

```
Level 1: UT-19, UT-24, UT-28a       -- Empty DAG cases
Level 2: UT-20, UT-25, UT-28b       -- Valid DAG (no issues)
Level 4: UT-21, UT-22, UT-26, UT-28c -- Single asymmetry/cycle/no-roots detection
Level 5: UT-23, UT-27               -- Multiple asymmetries, indirect cycles
```

### phase-computer.ts

```
Level 1: UT-29                       -- Empty -> empty Map
Level 2: UT-30                       -- Single root -> phase 0
Level 3: UT-31                       -- Two roots -> both phase 0
Level 4: UT-32                       -- Linear chain -> incremental phases
Level 5: UT-33, UT-34               -- Diamond, node field updates
```

### critical-path.ts

```
Level 1: UT-35                       -- Empty -> empty array
Level 2: UT-36                       -- Single node -> single-element path
Level 4: UT-37                       -- Linear chain -> full chain
Level 5: UT-38, UT-39               -- Branch selection, diamond graph
Level 6: UT-40                       -- isOnCriticalPath flag marking
```

### executable-stories.ts

```
Level 1: UT-41                       -- Empty -> empty array
Level 2: UT-42                       -- Single PENDING root -> returns it
Level 3: UT-44                       -- PENDING with satisfied dep
Level 4: UT-43, UT-45               -- Already SUCCESS excluded, unsatisfied deps excluded
Level 5: UT-46                       -- Multiple executable stories
Level 6: UT-47, UT-48               -- Critical path sorting, none executable
```

---

## 5. Double-Loop TDD Execution Order

The implementation follows this sequence, driven by the Double-Loop interaction between acceptance tests (outer) and unit tests (inner):

### Phase A: Write ALL Acceptance Tests (AT-01 through AT-09) -- All RED

All acceptance tests are written first against the `parseImplementationMap()` facade and `getExecutableStories()` function. They all fail initially because no implementation exists.

### Phase B: Markdown Parser (drives AT-01, AT-02, AT-03 toward GREEN)

```
Inner loop: UT-01 -> UT-02 -> UT-03 -> UT-04 -> UT-05 -> UT-06 -> UT-07 -> UT-08
            UT-09 -> UT-10 -> UT-11 -> UT-12
```

After this phase: `extractDependencyMatrix()` and `extractPhaseSummary()` are complete.

### Phase C: DAG Builder (drives AT-02, AT-03, AT-04 toward GREEN)

```
Inner loop: UT-13 -> UT-14 -> UT-15 -> UT-16 -> UT-17 -> UT-18
```

After this phase: `buildDag()` is complete.

### Phase D: DAG Validator (drives AT-05, AT-06 toward GREEN)

```
Inner loop: UT-19 -> UT-20 -> UT-21 -> UT-22 -> UT-23
            UT-24 -> UT-25 -> UT-26 -> UT-27
            UT-28a -> UT-28b -> UT-28c
```

After this phase: `validateSymmetry()`, `detectCycles()`, `validateRoots()` are complete.

### Phase E: Phase Computer (drives AT-03, AT-04 toward GREEN)

```
Inner loop: UT-29 -> UT-30 -> UT-31 -> UT-32 -> UT-33 -> UT-34
```

After this phase: `computePhases()` is complete.

### Phase F: Critical Path (drives AT-02, AT-03, AT-04 toward GREEN)

```
Inner loop: UT-35 -> UT-36 -> UT-37 -> UT-38 -> UT-39 -> UT-40
```

After this phase: `findCriticalPath()` is complete. AT-01 through AT-06 should now pass.

### Phase G: Executable Stories (drives AT-07, AT-08 toward GREEN)

```
Inner loop: UT-41 -> UT-42 -> UT-43 -> UT-44 -> UT-45 -> UT-46 -> UT-47 -> UT-48
```

After this phase: `getExecutableStories()` is complete. AT-07, AT-08 should now pass.

### Phase H: Facade + Integration (drives AT-09 and IT-01 through IT-03)

```
Implement: parseImplementationMap() in index.ts
Run: AT-09, IT-01, IT-02, IT-03
```

All acceptance tests GREEN. All integration tests GREEN.

---

## 6. Test Fixture Specifications

### `empty-map.md`

```markdown
## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
```

### `single-story.md`

```markdown
## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | Execution State Schema | - | - | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001 | Foundation | 1 | - |
```

### `linear-chain.md`

```markdown
## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | First Story | - | story-0042-0002 | Pendente |
| story-0042-0002 | Second Story | story-0042-0001 | story-0042-0003 | Pendente |
| story-0042-0003 | Third Story | story-0042-0002 | - | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001 | Foundation | 1 | - |
| 1 | 0002 | Core | 1 | Fase 0 |
| 2 | 0003 | Extension | 1 | Fase 1 |
```

### `parallel-roots.md`

```markdown
## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | Root A | - | story-0042-0003 | Pendente |
| story-0042-0002 | Root B | - | story-0042-0003 | Pendente |
| story-0042-0003 | Dependent | story-0042-0001, story-0042-0002 | - | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001, 0002 | Foundation | 2 paralelas | - |
| 1 | 0003 | Core | 1 | Fase 0 |
```

### `asymmetric-map.md`

```markdown
## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | Root Story | - | story-0042-0002 | Pendente |
| story-0042-0002 | Dependent Story | - | - | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001 | Foundation | 1 | - |
| 1 | 0002 | Core | 1 | Fase 0 |
```

> Note: 0001 lists 0002 in `Blocks`, but 0002 does NOT list 0001 in `Blocked By`. This asymmetry must trigger a warning and auto-correction.

### `cyclic-map.md`

```markdown
## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | Story A | story-0042-0003 | story-0042-0002 | Pendente |
| story-0042-0002 | Story B | story-0042-0001 | story-0042-0003 | Pendente |
| story-0042-0003 | Story C | story-0042-0002 | story-0042-0001 | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
```

> Note: Circular dependency 0001 -> 0002 -> 0003 -> 0001. Phase summary is empty because phases cannot be computed for cyclic graphs.

### `five-stories-three-phases.md`

```markdown
## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | Root A | - | story-0042-0003 | Pendente |
| story-0042-0002 | Root B | - | story-0042-0004 | Pendente |
| story-0042-0003 | Mid A | story-0042-0001 | story-0042-0005 | Pendente |
| story-0042-0004 | Mid B | story-0042-0002 | - | Pendente |
| story-0042-0005 | Leaf | story-0042-0003 | - | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001, 0002 | Foundation | 2 paralelas | - |
| 1 | 0003, 0004 | Core | 2 paralelas | Fase 0 |
| 2 | 0005 | Extension | 1 | Fase 1 |
```

> Note: Critical path is 0001 -> 0003 -> 0005 (3 phases, longest). Branch 0002 -> 0004 is only 2 phases. Used for AT-07 and AT-08: with 0001=SUCCESS, 0002=SUCCESS, executable stories are 0003 and 0004. Story 0003 is on critical path and should appear first.

### `epic-0005-map.md`

Copy of `docs/stories/epic-0005/IMPLEMENTATION-MAP.md` (14 stories, 6 phases). Used for AT-09, IT-01, and IT-03.

---

## 7. Test Helper Functions

The following factory/helper functions should be created in `tests/domain/implementation-map/helpers.ts`:

```typescript
// Factory for DependencyMatrixRow
function createMatrixRow(overrides?: Partial<DependencyMatrixRow>): DependencyMatrixRow;

// Factory for DagNode
function createDagNode(overrides?: Partial<DagNode>): DagNode;

// Factory for a DAG Map from an array of node descriptions
function createDag(nodes: Array<{ id: string; blockedBy?: string[]; blocks?: string[] }>): Map<string, DagNode>;

// Factory for ExecutionState
function createExecutionState(stories: Record<string, StoryStatus>): ExecutionState;

// Factory for ParsedMap
function createParsedMap(overrides?: Partial<ParsedMap>): ParsedMap;

// Read fixture file as string
function readFixture(filename: string): string;
```

---

## 8. Coverage Strategy

| File | Expected Lines | Expected Branches | Strategy |
|:---|:---|:---|:---|
| `types.ts` | 100% | 100% | Types only; no logic branches. Custom error classes tested via throw/catch in validator tests. |
| `markdown-parser.ts` | >= 95% | >= 90% | UT-01 through UT-12 cover empty, single, multi, format tolerance, separator skip. |
| `dag-builder.ts` | >= 95% | >= 90% | UT-13 through UT-18 cover empty, single, chain, parallel, title preservation. |
| `dag-validator.ts` | >= 95% | >= 90% | UT-19 through UT-28c cover all three functions with degenerate, valid, and error paths. |
| `phase-computer.ts` | >= 95% | >= 90% | UT-29 through UT-34 cover empty, single root, multiple roots, chain, diamond. |
| `critical-path.ts` | >= 95% | >= 90% | UT-35 through UT-40 cover empty, single, chain, branch selection, diamond, flag marking. |
| `executable-stories.ts` | >= 95% | >= 90% | UT-41 through UT-48 cover empty, single, satisfied, unsatisfied, multiple, sorting, none. |
| `index.ts` | >= 95% | >= 90% | AT-01 through AT-09 + IT-01 through IT-03 exercise the full facade pipeline. |

**Total test count:** 9 acceptance + 48 unit + 3 integration = **60 test scenarios**

---

## 9. Parallelism and Execution

All tests are marked `Parallel: yes` because:
- Every function under test is a **pure function** (no shared state, no I/O, no side effects).
- Test fixtures are **read-only markdown strings** (no mutation).
- Each test creates its own input data via factory functions.
- No database, filesystem writes, or network calls.

Vitest will run all test files in parallel across forked workers (maxForks: 3). Within each file, `describe` blocks run concurrently by default.

**Estimated execution time:** < 5 seconds for all 60 tests (pure computation, no I/O).

---

## 10. Traceability Matrix

| Gherkin Scenario | AT | Unit Tests (Inner Loop) | Integration Tests |
|:---|:---|:---|:---|
| Empty map -> empty ParsedMap, totalPhases 0 | AT-01 | UT-01, UT-02, UT-09, UT-13, UT-19, UT-24, UT-28a, UT-29, UT-35, UT-41 | -- |
| Single root story -> phase 0, criticalPath = [that story] | AT-02 | UT-03, UT-14, UT-20, UT-25, UT-28b, UT-30, UT-36, UT-42 | -- |
| Linear chain -> N phases, criticalPath = full chain | AT-03 | UT-05, UT-11, UT-16, UT-32, UT-37 | -- |
| Parallelism -> roots in phase 0, dependent in phase 1 | AT-04 | UT-17, UT-31, UT-33, UT-38, UT-39 | -- |
| Asymmetric dependency -> warning + auto-correct | AT-05 | UT-21, UT-22, UT-23 | -- |
| Cycle detection -> throw with cycle chain | AT-06 | UT-26, UT-27 | -- |
| Executable stories with partial state | AT-07 | UT-44, UT-45, UT-46 | IT-03 |
| Critical path prioritization | AT-08 | UT-47 | -- |
| Real map parsing (epic-0005) | AT-09 | -- | IT-01, IT-02 |
