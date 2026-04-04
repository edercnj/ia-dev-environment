# Implementation Plan -- story-0005-0004: Implementation Map Parser

**Architecture Plan:** `architecture-story-0005-0004.md`
**Story:** `story-0005-0004.md`

---

## 1. Affected Layers and Components

| Layer | Impact | Details |
|-------|--------|---------|
| `src/domain/` | **NEW sub-module** | New `implementation-map/` directory with 8 files |
| `src/domain/index.ts` | **MODIFY** | Add re-export of `implementation-map` barrel |
| `src/exceptions.ts` | **NO CHANGE** | Custom errors co-located in the new module |

This story is contained entirely within the domain layer. No assembler, CLI, config, or template changes.

---

## 2. New Classes/Interfaces to Create

### 2.1 Types (`src/domain/implementation-map/types.ts`)

| Type | Kind | Description |
|------|------|-------------|
| `StoryStatus` | enum | Inline stub (`PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `BLOCKED`, `PARTIAL`). TODO: replace with import from story-0005-0001. |
| `ExecutionState` | interface | Minimal stub: `{ epicId: string, stories: Record<string, { status: StoryStatus }> }`. |
| `DependencyMatrixRow` | interface | Parsed row from Section 1 table. |
| `PhaseSummaryRow` | interface | Parsed row from Section 5 table. |
| `DagNode` | interface | Node in the dependency graph with phase and critical path flag. |
| `DagWarning` | interface | Warning emitted during validation (non-fatal). |
| `ParsedMap` | interface | Complete parser output: stories, phases, criticalPath, totalPhases, warnings. |
| `CircularDependencyError` | class | Custom error thrown when cycle detected. Extends `Error`. |
| `InvalidDagError` | class | Custom error thrown when DAG has no roots. Extends `Error`. |
| `MapParseError` | class | Custom error thrown on malformed markdown. Extends `Error`. |

### 2.2 Functions (by file)

| File | Functions | Lines Est. |
|------|-----------|------------|
| `markdown-parser.ts` | `extractDependencyMatrix(content)`, `extractPhaseSummary(content)` | ~100 |
| `dag-builder.ts` | `buildDag(rows)` | ~60 |
| `dag-validator.ts` | `validateSymmetry(dag)`, `detectCycles(dag)`, `validateRoots(dag)` | ~80 |
| `phase-computer.ts` | `computePhases(dag)` | ~50 |
| `critical-path.ts` | `findCriticalPath(dag, phases)`, `markCriticalPath(dag, path)` | ~60 |
| `executable-stories.ts` | `getExecutableStories(parsedMap, executionState)` | ~40 |
| `index.ts` | `parseImplementationMap(content)` (facade) | ~30 |

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| `src/domain/index.ts` | Add `export * from "./implementation-map/index.js";` | Expose the new module through the domain barrel |

No other existing files are modified.

---

## 4. Dependency Direction Validation

```
src/domain/implementation-map/types.ts      --> (no imports)
src/domain/implementation-map/*.ts          --> types.ts (within module only)
src/domain/implementation-map/index.ts      --> all internal files
src/domain/index.ts                         --> implementation-map/index.ts
```

**Verification checklist:**
- [ ] No import from `src/assembler/`
- [ ] No import from `src/cli*.ts`
- [ ] No import from `src/config.ts`
- [ ] No import from `src/models.ts`
- [ ] No import from `node:fs` or `node:path`
- [ ] No import from any npm package
- [ ] Only standard library types used (Map, Set, Array, string, number, boolean)

---

## 5. Integration Points

| Consumer | How It Integrates | When |
|----------|-------------------|------|
| story-0005-0005 (Core Loop) | Calls `parseImplementationMap(content)` and `getExecutableStories(map, state)` | After this story is complete |
| story-0005-0012 (Dry-run) | Calls `parseImplementationMap(content)` to display execution plan | After this story is complete |
| story-0005-0001 (Execution State) | This story stubs `StoryStatus` and `ExecutionState`; when 0001 lands, replace stubs with imports | Retroactive refactor |

---

## 6. Database Changes

N/A -- This module is pure computation with no persistence.

---

## 7. API Changes

N/A -- This module has no HTTP/CLI interface. It exports TypeScript functions consumed by other domain modules.

---

## 8. Event Changes

N/A -- No events are produced or consumed.

---

## 9. Configuration Changes

N/A -- No environment variables or config files. The parser is configuration-free.

---

## 10. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| story-0005-0001 types not yet available | Medium | Certain | Define inline stubs with `TODO(story-0005-0001)` marker. Mechanical refactor when 0001 lands (replace import path). |
| Template format changes break parser | Low | Low | Test against real maps (epic-0003, epic-0004, epic-0005). Template is version-controlled. |
| Edge cases in markdown parsing (unusual formatting) | Medium | Medium | Build regex patterns that tolerate whitespace variations. Include fuzzy-format test cases. |
| Performance with large maps (50+ stories) | Low | Low | All algorithms are O(V+E). Requirement is <100ms for 50 stories. Linear scan of markdown is trivially fast. |

---

## 11. Implementation Order (TDD)

Implementation follows inner-to-outer order, with tests written first (Red-Green-Refactor).

### Phase A: Types and Errors
1. Create `types.ts` with all interfaces, enums, and custom errors.
2. No tests needed (type definitions only).

### Phase B: Markdown Parser (Red-Green-Refactor)
1. **Test:** Empty markdown returns empty arrays.
2. **Test:** Single-row dependency matrix.
3. **Test:** Multi-row dependency matrix with comma-separated dependencies.
4. **Test:** Phase summary table extraction.
5. **Test:** Malformed markdown throws `MapParseError`.
6. **Implement:** `extractDependencyMatrix()` and `extractPhaseSummary()`.

### Phase C: DAG Builder (Red-Green-Refactor)
1. **Test:** Empty rows produce empty DAG.
2. **Test:** Single root story.
3. **Test:** Linear chain A -> B -> C.
4. **Test:** Parallel roots with shared dependent.
5. **Implement:** `buildDag()`.

### Phase D: DAG Validator (Red-Green-Refactor)
1. **Test:** Symmetric DAG produces no warnings.
2. **Test:** Asymmetric DAG produces warning and auto-corrects.
3. **Test:** Cyclic DAG throws `CircularDependencyError` with cycle chain.
4. **Test:** DAG with no roots throws `InvalidDagError`.
5. **Implement:** `validateSymmetry()`, `detectCycles()`, `validateRoots()`.

### Phase E: Phase Computer (Red-Green-Refactor)
1. **Test:** Single phase (all roots).
2. **Test:** Linear chain produces N phases.
3. **Test:** Diamond dependency produces correct phases.
4. **Implement:** `computePhases()`.

### Phase F: Critical Path (Red-Green-Refactor)
1. **Test:** Single story is its own critical path.
2. **Test:** Linear chain is the critical path.
3. **Test:** Diamond graph selects longest branch.
4. **Implement:** `findCriticalPath()`.

### Phase G: Executable Stories (Red-Green-Refactor)
1. **Test:** All PENDING with no deps returns all stories.
2. **Test:** PENDING with unsatisfied deps excluded.
3. **Test:** Critical path stories sorted first.
4. **Test:** No executable stories returns empty array.
5. **Implement:** `getExecutableStories()`.

### Phase H: Facade + Integration Tests
1. **Test:** `parseImplementationMap()` end-to-end with synthetic map.
2. **Test:** `parseImplementationMap()` with epic-0005 real map.
3. **Implement:** `parseImplementationMap()` facade in `index.ts`.
4. **Modify:** `src/domain/index.ts` to re-export the new module.

### Phase I: Refinement
1. Verify coverage >= 95% line, >= 90% branch.
2. Run `npx tsc --noEmit` for type checking.
3. Review all `TODO(story-0005-0001)` markers.

---

## 12. Test File Structure

```
tests/
  domain/
    implementation-map/
      markdown-parser.test.ts
      dag-builder.test.ts
      dag-validator.test.ts
      phase-computer.test.ts
      critical-path.test.ts
      executable-stories.test.ts
      parse-implementation-map.test.ts   # facade integration test
  fixtures/
    implementation-maps/
      empty-map.md
      single-story.md
      linear-chain.md
      parallel-roots.md
      cyclic-map.md
      asymmetric-map.md
      epic-0005-map.md                   # copy of real IMPLEMENTATION-MAP.md
```

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario | Test File | Phase |
|-----------------|-----------|-------|
| Empty map | `parse-implementation-map.test.ts` | H |
| Single root story | `dag-builder.test.ts`, `parse-implementation-map.test.ts` | C, H |
| Linear dependencies | `phase-computer.test.ts`, `critical-path.test.ts` | E, F |
| Parallelism | `phase-computer.test.ts` | E |
| Asymmetric dependency warning | `dag-validator.test.ts` | D |
| Cycle detection | `dag-validator.test.ts` | D |
| Executable stories with partial state | `executable-stories.test.ts` | G |
| Critical path prioritization | `executable-stories.test.ts` | G |
| Real map (epic-0005) | `parse-implementation-map.test.ts` | H |
