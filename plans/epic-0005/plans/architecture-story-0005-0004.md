# Architecture Plan -- story-0005-0004: Implementation Map Parser

## 1. Component Overview

The Implementation Map Parser is a **pure domain module** that reads `IMPLEMENTATION-MAP.md` markdown content and produces a structured, validated DAG (Directed Acyclic Graph) with computed phases and critical path. It fits into the existing codebase as a new sub-module under `src/domain/`.

**Placement in `src/` structure:**

```
src/
  domain/
    index.ts                    # <-- add re-exports
    implementation-map/         # <-- NEW module directory
      types.ts                  # types, interfaces, enums
      markdown-parser.ts        # extract tables from markdown
      dag-builder.ts            # build DAG from parsed rows
      dag-validator.ts          # symmetry, cycles, roots
      phase-computer.ts         # compute phases from DAG
      critical-path.ts          # longest path algorithm
      executable-stories.ts     # filter runnable stories
      index.ts                  # barrel export
```

The parser is a **read-only, stateless module** -- it takes a markdown string as input and returns typed structures. No I/O, no side effects, no framework dependencies.

## 2. New Files to Create

| File | Responsibility | Estimated Lines |
|------|---------------|-----------------|
| `src/domain/implementation-map/types.ts` | All interfaces, enums, and type aliases | ~80 |
| `src/domain/implementation-map/markdown-parser.ts` | Extract dependency matrix (Section 1) and phase summary (Section 5) from markdown | ~100 |
| `src/domain/implementation-map/dag-builder.ts` | Build adjacency list `Map<storyId, DagNode>` from parsed rows | ~60 |
| `src/domain/implementation-map/dag-validator.ts` | Validate symmetry, detect cycles (DFS), verify roots exist | ~80 |
| `src/domain/implementation-map/phase-computer.ts` | Compute implementation phases from validated DAG | ~50 |
| `src/domain/implementation-map/critical-path.ts` | Find longest path in DAG (critical path) | ~60 |
| `src/domain/implementation-map/executable-stories.ts` | `getExecutableStories(parsedMap, executionState)` with critical path priority sorting | ~40 |
| `src/domain/implementation-map/index.ts` | Barrel re-exports + `parseImplementationMap()` facade function | ~30 |

**Total: 8 new files, ~500 lines estimated.**

## 3. Interfaces and Types

### 3.1 Core Types (types.ts)

```typescript
/** Minimal stub for StoryStatus from story-0005-0001 (not yet implemented). */
export enum StoryStatus {
  PENDING = "PENDING",
  IN_PROGRESS = "IN_PROGRESS",
  SUCCESS = "SUCCESS",
  FAILED = "FAILED",
  BLOCKED = "BLOCKED",
  PARTIAL = "PARTIAL",
}

/** Minimal stub for ExecutionState from story-0005-0001 (not yet implemented). */
export interface ExecutionState {
  readonly epicId: string;
  readonly stories: Readonly<Record<string, { readonly status: StoryStatus }>>;
}

/** A single row from the dependency matrix (Section 1). */
export interface DependencyMatrixRow {
  readonly storyId: string;
  readonly title: string;
  readonly blockedBy: readonly string[];
  readonly blocks: readonly string[];
  readonly status: string;
}

/** A single row from the phase summary (Section 5). */
export interface PhaseSummaryRow {
  readonly phase: number;
  readonly stories: readonly string[];
  readonly layer: string;
  readonly parallelism: string;
  readonly prerequisite: string;
}

/** Node in the dependency DAG. */
export interface DagNode {
  readonly storyId: string;
  readonly title: string;
  readonly blockedBy: readonly string[];
  readonly blocks: readonly string[];
  readonly phase: number;
  readonly isOnCriticalPath: boolean;
}

/** Validation warning emitted during DAG construction. */
export interface DagWarning {
  readonly type: "asymmetric-dependency" | "missing-story-reference";
  readonly message: string;
}

/** Complete result of parsing an implementation map. */
export interface ParsedMap {
  readonly stories: ReadonlyMap<string, DagNode>;
  readonly phases: ReadonlyMap<number, readonly string[]>;
  readonly criticalPath: readonly string[];
  readonly totalPhases: number;
  readonly warnings: readonly DagWarning[];
}
```

### 3.2 Function Signatures

```typescript
// markdown-parser.ts
function extractDependencyMatrix(content: string): DependencyMatrixRow[];
function extractPhaseSummary(content: string): PhaseSummaryRow[];

// dag-builder.ts
function buildDag(rows: DependencyMatrixRow[]): Map<string, DagNode>;

// dag-validator.ts
function validateSymmetry(dag: Map<string, DagNode>): DagWarning[];
function detectCycles(dag: Map<string, DagNode>): void; // throws on cycle
function validateRoots(dag: Map<string, DagNode>): void; // throws if no roots

// phase-computer.ts
function computePhases(dag: Map<string, DagNode>): Map<number, string[]>;

// critical-path.ts
function findCriticalPath(dag: Map<string, DagNode>, phases: Map<number, string[]>): string[];

// executable-stories.ts
function getExecutableStories(parsedMap: ParsedMap, executionState: ExecutionState): string[];

// index.ts (facade)
function parseImplementationMap(content: string): ParsedMap;
```

## 4. Dependency Direction

```
src/domain/implementation-map/
  types.ts          <-- no imports from outside (self-contained types)
  markdown-parser   <-- imports types.ts only
  dag-builder       <-- imports types.ts only
  dag-validator     <-- imports types.ts only
  phase-computer    <-- imports types.ts only
  critical-path     <-- imports types.ts only
  executable-stories <-- imports types.ts only
  index.ts          <-- imports all of the above, re-exports
```

**Rules:**
- The module depends on NOTHING outside `src/domain/implementation-map/`.
- No imports from `src/assembler/`, `src/cli.ts`, `src/models.ts`, or `src/config.ts`.
- No Node.js I/O imports (`fs`, `path`) -- the module receives a string, not a file path.
- The facade function `parseImplementationMap(content: string)` is the single public entry point.
- `getExecutableStories()` is a separate public function that takes `ParsedMap` + `ExecutionState`.

When story-0005-0001 lands, the inline `StoryStatus` and `ExecutionState` stubs will be replaced with imports from the checkpoint engine module.

## 5. Key Design Decisions

### 5.1 Pure Functions over Classes

All operations are **pure functions** (input -> output, no mutation). Rationale:
- The parser has no state to manage between calls.
- Pure functions are trivially testable.
- Aligns with the existing `src/domain/` pattern (e.g., `validator.ts`, `resolver.ts`).

### 5.2 Facade Pattern for Public API

The `parseImplementationMap()` function orchestrates the full pipeline:
1. `extractDependencyMatrix()` -- parse Section 1
2. `buildDag()` -- create adjacency list
3. `validateSymmetry()` -- collect warnings, auto-fix asymmetry
4. `detectCycles()` -- throw if cyclic
5. `validateRoots()` -- throw if no roots
6. `computePhases()` -- assign phase numbers
7. `findCriticalPath()` -- identify longest path
8. Mark `isOnCriticalPath` on each DagNode

Internal functions are exported for granular unit testing but the facade is the canonical entry point.

### 5.3 Error Handling

- **Cycles:** Throw `CircularDependencyError` (custom error extending `Error`) with the cycle chain in the message.
- **No roots:** Throw `InvalidDagError` with a descriptive message.
- **Asymmetric dependencies:** Do NOT throw. Emit a `DagWarning` and auto-correct (add the missing edge). This matches the Gherkin acceptance criterion.
- **Malformed markdown:** Throw `MapParseError` with line number context.

Custom errors will be added to `src/domain/implementation-map/types.ts` (co-located with the module, not in `src/exceptions.ts`).

### 5.4 Markdown Parsing Strategy

Use **regex-based line-by-line parsing** (no external dependency):
- Identify section headers by `## N. ` prefix.
- Parse table rows by splitting on `|`.
- Trim cells, handle `---` separator rows, empty cells (`-`), and comma-separated story lists.
- Tolerate formatting variations (extra spaces, inconsistent alignment).

### 5.5 Cycle Detection Algorithm

Standard DFS with three-color marking (`WHITE`, `GRAY`, `BLACK`):
- `GRAY` node encountered during DFS = back-edge = cycle.
- When a cycle is detected, reconstruct the cycle chain from the DFS stack.
- O(V + E) time complexity.

### 5.6 Critical Path Algorithm

Topological-sort-based longest path:
1. Topological sort of the DAG (guaranteed acyclic after validation).
2. Initialize distances to 0 for all nodes.
3. Process nodes in topological order; for each node, update distances of dependents.
4. The longest distance determines the critical path.
5. Backtrack from the node with the longest distance to reconstruct the path.

## 6. Risk Assessment

### 6.1 Dependency on story-0005-0001 Types (MEDIUM)

`getExecutableStories()` needs `StoryStatus` enum and `ExecutionState` interface. Since story-0005-0001 is not yet implemented:

**Mitigation:** Define minimal inline stubs in `types.ts` with a `TODO(story-0005-0001)` marker. The stubs contain only the fields this module reads (`status` field per story). When story-0005-0001 lands, replace stubs with proper imports. The refactoring is mechanical (search-and-replace import path).

### 6.2 Markdown Format Stability (LOW)

The parser depends on the `IMPLEMENTATION-MAP.md` template format. If the template changes, the parser breaks.

**Mitigation:** The template (`resources/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`) is versioned in this repo. The parser will be tested against real implementation maps (epic-0003, epic-0004, epic-0005) as integration tests, providing regression coverage.

### 6.3 Module Size (LOW)

8 files for a single module may seem like over-splitting. However, each file is under 100 lines, single-responsibility, and independently testable. The barrel `index.ts` provides a clean facade.
