# Test Plan -- STORY-0003-0001: Testing KP -- TDD Workflow & Transformation Priority Premise

## Summary

- Affected source file: `resources/core/03-testing-philosophy.md`
- Affected golden files: 16 files across 8 profiles (`.claude/` + `.agents/` copies)
- Total test methods: 28
- Categories: Golden File Integration (8), Content Validation (12), Backward Compatibility (8)
- Coverage targets: >= 95% line, >= 90% branch
- No new source code modules -- this is a content-only change verified by existing byte-for-byte infrastructure plus new content assertions

---

## 1. Test File Locations and Naming

### Existing (modified)

**Path:** `tests/node/integration/byte-for-byte.test.ts`

**Rationale:** Golden files for all 8 profiles must be updated to include the 4 new TDD sections. The existing byte-for-byte test suite automatically validates that pipeline output matches golden files. No code changes to this file -- only golden file updates.

### New

**Path:** `tests/node/content/testing-philosophy-tdd-sections.test.ts`

**Rationale:** Content validation tests that verify the structural integrity of the 4 new TDD sections. These are unit-level tests that read the source file and assert section presence, ordering, and content. Separated from byte-for-byte tests because they validate semantic content, not binary equality.

**Naming convention:** `[sectionUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Source File Path

```typescript
import { resolve, dirname } from "node:path";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, it, expect, beforeAll } from "vitest";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

const SOURCE_FILE = resolve(RESOURCES_DIR, "core", "03-testing-philosophy.md");
```

### 2.2 Content Loading

```typescript
let content: string;

beforeAll(() => {
  content = readFileSync(SOURCE_FILE, "utf-8");
});
```

### 2.3 Golden File Paths

The pipeline copies `resources/core/03-testing-philosophy.md` to two locations per profile:

1. `{profile}/.claude/skills/testing/references/testing-philosophy.md`
2. `{profile}/.agents/skills/testing/references/testing-philosophy.md`

Golden files live at `tests/golden/{profile}/` for all 8 profiles defined in `tests/helpers/integration-constants.ts`:

- `go-gin`
- `java-quarkus`
- `java-spring`
- `kotlin-ktor`
- `python-click-cli`
- `python-fastapi`
- `rust-axum`
- `typescript-nestjs`

---

## 3. Test Groups

### Group 1: Golden File Integration (8 tests -- existing suite)

The existing `byte-for-byte.test.ts` runs `describe.sequential.each` over all 8 `CONFIG_PROFILES`. Each profile runs 5 assertions (pipeline success, golden match, no missing files, no extra files, total files > 0). The golden files must be regenerated to include the new TDD sections.

**Action required:** Update all 16 golden `testing-philosophy.md` files (8 profiles x 2 copies each) to match the updated `resources/core/03-testing-philosophy.md`.

| # | Test Name (existing) | Profile | Assertion |
|---|---------------------|---------|-----------|
| 1 | `pipelineMatchesGoldenFiles_go-gin` | go-gin | `verifyOutput()` returns `success: true` |
| 2 | `pipelineMatchesGoldenFiles_java-quarkus` | java-quarkus | `verifyOutput()` returns `success: true` |
| 3 | `pipelineMatchesGoldenFiles_java-spring` | java-spring | `verifyOutput()` returns `success: true` |
| 4 | `pipelineMatchesGoldenFiles_kotlin-ktor` | kotlin-ktor | `verifyOutput()` returns `success: true` |
| 5 | `pipelineMatchesGoldenFiles_python-click-cli` | python-click-cli | `verifyOutput()` returns `success: true` |
| 6 | `pipelineMatchesGoldenFiles_python-fastapi` | python-fastapi | `verifyOutput()` returns `success: true` |
| 7 | `pipelineMatchesGoldenFiles_rust-axum` | rust-axum | `verifyOutput()` returns `success: true` |
| 8 | `pipelineMatchesGoldenFiles_typescript-nestjs` | typescript-nestjs | `verifyOutput()` returns `success: true` |

**Verification logic:** `verifyOutput()` in `src/verifier.ts` compares two directory trees byte-for-byte using `Buffer.equals()`. Any difference in the golden `testing-philosophy.md` files will produce a `FileDiff` mismatch and fail the test. The `formatVerificationFailures()` helper in `tests/helpers/integration-constants.ts` renders up to 500 chars of unified diff for debugging.

#### Execution

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

---

### Group 2: Content Validation -- TDD Workflow Section (3 tests)

Verify the "## TDD Workflow" section exists with the required RED, GREEN, REFACTOR sub-sections.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 9 | `tddWorkflow_sectionExists_containsH2Header` | `content` contains `## TDD Workflow` |
| 10 | `tddWorkflow_phases_containsRedGreenRefactor` | `content` contains all three sub-section markers: `**RED**`, `**GREEN**`, `**REFACTOR**` |
| 11 | `tddWorkflow_coreRule_containsNeverWriteProductionCodeWithoutFailingTest` | `content` contains the rule "NEVER write production code without a failing test" (case-insensitive match) |

#### Assertions Pattern

```typescript
// Test 9
expect(content).toContain("## TDD Workflow");

// Test 10
expect(content).toContain("**RED**");
expect(content).toContain("**GREEN**");
expect(content).toContain("**REFACTOR**");

// Test 11
expect(content.toLowerCase()).toContain(
  "never write production code without a failing test"
);
```

---

### Group 3: Content Validation -- Double-Loop TDD Section (3 tests)

Verify the "## Double-Loop TDD" section exists with outer/inner loop descriptions and a diagram.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 12 | `doubleLoopTdd_sectionExists_containsH2Header` | `content` contains `## Double-Loop TDD` |
| 13 | `doubleLoopTdd_loops_containsOuterAndInnerDescriptions` | `content` contains `Acceptance Test` (outer loop) and `Unit Test` (inner loop) references |
| 14 | `doubleLoopTdd_diagram_containsMermaidOrAsciiDiagram` | `content` contains a diagram block (either `sequenceDiagram` or ASCII art showing loop interaction) |

#### Assertions Pattern

```typescript
// Test 12
expect(content).toContain("## Double-Loop TDD");

// Test 13
expect(content).toMatch(/acceptance\s+test/i);
expect(content).toMatch(/unit\s+test/i);

// Test 14
const hasMermaid = content.includes("sequenceDiagram");
const hasCodeBlock = content.includes("```");
expect(hasMermaid || hasCodeBlock).toBe(true);
```

---

### Group 4: Content Validation -- Transformation Priority Premise Section (3 tests)

Verify the "## Transformation Priority Premise" section exists with at least 7 ordered transformations.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 15 | `tpp_sectionExists_containsH2Header` | `content` contains `## Transformation Priority Premise` |
| 16 | `tpp_transformations_containsAtLeast7OrderedItems` | Section contains at least 7 numbered list items (matching `/^\s*\d+\.\s/m` pattern) between the TPP H2 and the next H2 |
| 17 | `tpp_boundaries_firstIsNilLastIsMutatedValue` | First transformation references `nil`/`null`/`undefined`; last transformation references `mutated value` or value transformation |

#### Assertions Pattern

```typescript
// Test 15
expect(content).toContain("## Transformation Priority Premise");

// Test 16 -- extract TPP section and count numbered items
const tppStart = content.indexOf("## Transformation Priority Premise");
const tppEnd = content.indexOf("\n## ", tppStart + 1);
const tppSection = content.slice(tppStart, tppEnd > -1 ? tppEnd : undefined);
const numberedItems = tppSection.match(/^\s*\d+\.\s/gm) || [];
expect(numberedItems.length).toBeGreaterThanOrEqual(7);

// Test 17
expect(tppSection).toMatch(/nil|null|undefined/i);
expect(tppSection).toMatch(/mutated?\s*value/i);
```

---

### Group 5: Content Validation -- Test Scenario Ordering Section (3 tests)

Verify the "## Test Scenario Ordering" section exists with 6 complexity levels.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 18 | `scenarioOrdering_sectionExists_containsH2Header` | `content` contains `## Test Scenario Ordering` |
| 19 | `scenarioOrdering_levels_contains6Levels` | Section contains references to 6 levels (matching pattern `Level 1` through `Level 6` or numbered list with 6 items) |
| 20 | `scenarioOrdering_boundaries_level1IsDegenerateCasesLevel6IsEdgeCases` | Level 1 references "Degenerate cases"; Level 6 references "Edge cases" |

#### Assertions Pattern

```typescript
// Test 18
expect(content).toContain("## Test Scenario Ordering");

// Test 19 -- extract section and count levels
const orderStart = content.indexOf("## Test Scenario Ordering");
const orderEnd = content.indexOf("\n## ", orderStart + 1);
const orderSection = content.slice(orderStart, orderEnd > -1 ? orderEnd : undefined);
const levelMatches = orderSection.match(/level\s*\d/gi) || [];
expect(levelMatches.length).toBeGreaterThanOrEqual(6);

// Test 20
expect(orderSection).toMatch(/level\s*1.*degenerate\s*cases/is);
expect(orderSection).toMatch(/level\s*6.*edge\s*cases/is);
```

---

### Group 6: Backward Compatibility -- Existing Sections Preserved (8 tests)

Verify that all pre-existing sections in `03-testing-philosophy.md` remain intact after the TDD additions.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 21 | `backwardCompat_coverageThresholds_sectionPreserved` | `content` contains `## Coverage Thresholds` with `>= 95%` and `>= 90%` |
| 22 | `backwardCompat_testCategories_all8CategoriesPresent` | `content` contains all 8 category headers: `### 1. Unit Tests`, `### 2. Integration Tests`, `### 3. API Tests`, `### 4. Protocol Tests`, `### 5. Contract Tests`, `### 6. End-to-End Tests`, `### 7. Performance Tests`, `### 8. Smoke Tests` |
| 23 | `backwardCompat_namingConvention_sectionPreserved` | `content` contains `## Naming Convention` and the pattern `[methodUnderTest]_[scenario]_[expectedBehavior]` |
| 24 | `backwardCompat_testFixtures_sectionPreserved` | `content` contains `## Test Fixtures` |
| 25 | `backwardCompat_dataUniqueness_sectionPreserved` | `content` contains `## Data Uniqueness in Tests` |
| 26 | `backwardCompat_asyncHandling_sectionPreserved` | `content` contains `## Asynchronous Resource Handling` |
| 27 | `backwardCompat_prohibitions_sectionPreserved` | `content` contains `## Prohibitions` with `NEVER mock domain logic` |
| 28 | `backwardCompat_dbStrategy_sectionPreserved` | `content` contains `## When to Use Real Database vs In-Memory` |

#### Assertions Pattern

```typescript
// Test 21
expect(content).toContain("## Coverage Thresholds");
expect(content).toContain(">= 95%");
expect(content).toContain(">= 90%");

// Test 22
const categories = [
  "### 1. Unit Tests",
  "### 2. Integration Tests",
  "### 3. API Tests",
  "### 4. Protocol Tests",
  "### 5. Contract Tests",
  "### 6. End-to-End Tests",
  "### 7. Performance Tests",
  "### 8. Smoke Tests",
];
for (const category of categories) {
  expect(content).toContain(category);
}

// Test 23
expect(content).toContain("## Naming Convention");
expect(content).toContain("[methodUnderTest]_[scenario]_[expectedBehavior]");

// Test 24
expect(content).toContain("## Test Fixtures");

// Test 25
expect(content).toContain("## Data Uniqueness in Tests");

// Test 26
expect(content).toContain("## Asynchronous Resource Handling");

// Test 27
expect(content).toContain("## Prohibitions");
expect(content).toContain("NEVER mock domain logic");

// Test 28
expect(content).toContain("## When to Use Real Database vs In-Memory");
```

---

## 4. Section Ordering Validation

The 4 new TDD sections MUST appear AFTER all existing sections (additive change per story description). This is validated implicitly by backward compatibility tests (existing sections intact) and explicitly by:

```typescript
it("tddSections_ordering_newSectionsAppearAfterExistingSections", () => {
  const lastExistingSection = content.indexOf("## When to Use Real Database vs In-Memory");
  const firstNewSection = content.indexOf("## TDD Workflow");
  expect(firstNewSection).toBeGreaterThan(lastExistingSection);
});
```

This is an additional structural test that can be included in Group 5 or as a standalone assertion.

---

## 5. Dual Copy Consistency

The story requires RULE-001 compliance: both copy locations must contain identical TDD content. The pipeline handles this automatically by copying from `resources/core/` to both output locations. The byte-for-byte tests (Group 1) validate this transitively -- if golden files contain the new sections and the pipeline output matches golden files, both copies are correct.

**Explicit validation (optional, in content tests):**

```typescript
import { CONFIG_PROFILES, GOLDEN_DIR } from "../../helpers/integration-constants.js";

describe("dual copy consistency", () => {
  for (const profile of CONFIG_PROFILES) {
    it(`dualCopy_${profile}_claudeAndAgentsCopiesMatch`, () => {
      const claudeCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".claude/skills/testing/references/testing-philosophy.md"),
        "utf-8",
      );
      const agentsCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".agents/skills/testing/references/testing-philosophy.md"),
        "utf-8",
      );
      expect(claudeCopy).toBe(agentsCopy);
    });
  }
});
```

---

## 6. Coverage Strategy

### 6.1 Line Coverage

No new production code is added -- this is a content file change. Coverage impact is zero. Existing coverage (99.6% lines, 97.84% branches) is maintained.

### 6.2 Branch Coverage

The `verifyOutput()` function in `src/verifier.ts` exercises all branches through the golden file comparison:
- `Buffer.equals()` returns `true` for matching files (happy path)
- `FileDiff` produced for mismatching files (detected during development if golden files are stale)
- `missingFiles` / `extraFiles` arrays populated if file sets differ

### 6.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | No new code paths; existing coverage maintained |
| Branch | >= 90% | Existing verifier branches already covered |

---

## 7. Test Matrix Summary

| Group | Description | Test Count | Type |
|-------|-------------|------------|------|
| G1: Golden File Integration | Byte-for-byte parity across 8 profiles | 8 | Integration (existing) |
| G2: TDD Workflow | Section existence, phases, core rule | 3 | Content Validation |
| G3: Double-Loop TDD | Section existence, loop descriptions, diagram | 3 | Content Validation |
| G4: TPP | Section existence, 7+ transformations, boundaries | 3 | Content Validation |
| G5: Test Scenario Ordering | Section existence, 6 levels, boundaries | 3 | Content Validation |
| G6: Backward Compatibility | All 8 existing sections preserved | 8 | Content Validation |
| **Total** | | **28** | |

---

## 8. Execution Commands

### Run Content Validation Tests Only

```bash
npx vitest run tests/node/content/testing-philosophy-tdd-sections.test.ts
```

### Run Golden File Tests Only

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

### Run Both

```bash
npx vitest run tests/node/content/testing-philosophy-tdd-sections.test.ts tests/node/integration/byte-for-byte.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 9. Golden File Update Procedure

### Step 1: Modify Source

Edit `resources/core/03-testing-philosophy.md` to add the 4 new TDD sections at the end.

### Step 2: Regenerate Golden Files

Run the pipeline for each profile and copy outputs to golden directories:

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  npx ts-node src/cli.ts generate \
    --config resources/config-templates/setup-config.${profile}.yaml \
    --output /tmp/golden-${profile}
  cp /tmp/golden-${profile}/.claude/skills/testing/references/testing-philosophy.md \
     tests/golden/${profile}/.claude/skills/testing/references/testing-philosophy.md
  cp /tmp/golden-${profile}/.agents/skills/testing/references/testing-philosophy.md \
     tests/golden/${profile}/.agents/skills/testing/references/testing-philosophy.md
done
```

### Step 3: Verify

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 8 profiles must pass with `success: true`.

---

## 10. Dependencies and Prerequisites

### Prerequisites

- `resources/core/03-testing-philosophy.md` exists and contains the 11 existing sections
- All 8 profile config templates exist in `resources/config-templates/`
- Golden file directories exist for all 8 profiles under `tests/golden/`
- `tests/helpers/integration-constants.ts` exports `CONFIG_PROFILES`, `GOLDEN_DIR`, `RESOURCES_DIR`

### Import Dependencies (for new test file)

| Module | Import | Used For |
|--------|--------|----------|
| `node:fs` | `readFileSync` | Reading source file content |
| `node:path` | `resolve`, `dirname` | Path resolution |
| `node:url` | `fileURLToPath` | ESM `__dirname` equivalent |
| `vitest` | `describe`, `it`, `expect`, `beforeAll` | Test framework |
| `tests/helpers/integration-constants.ts` | `CONFIG_PROFILES`, `GOLDEN_DIR` | Profile list and golden paths (for dual copy tests) |

---

## 11. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden files not updated before commit | High | High | CI fails immediately on byte-for-byte mismatch. Developer must regenerate golden files as part of the story. |
| Content assertions too brittle (exact string matching) | Medium | Medium | Use `toContain()` for section headers and key phrases, not full-line matching. Use case-insensitive regex for flexible assertions. |
| New sections break existing section detection | Low | High | Backward compatibility tests (Group 6) catch any accidental deletion or modification of existing content. |
| Dual copy locations diverge | Low | High | Pipeline copies from single source (`resources/core/`). Byte-for-byte tests catch divergence. Explicit dual copy test (Section 5) provides defense-in-depth. |
| Section ordering changes | Low | Medium | Ordering test (Section 4) validates new sections appear after existing ones. |
| Mermaid diagram syntax invalid | Low | Low | Content test validates diagram block presence. Mermaid rendering is a downstream concern (IDE/GitHub), not a pipeline concern. |
| Test file discovery by Vitest | Low | Low | New test file at `tests/node/content/` must match the glob pattern in `vitest.config.ts` (`tests/**/*.test.ts`). Verify pattern includes the `content/` subdirectory. |

---

## 12. Naming Convention Reference

All new test names follow `[sectionUnderTest]_[scenario]_[expectedBehavior]`:

```
tddWorkflow_sectionExists_containsH2Header
tddWorkflow_phases_containsRedGreenRefactor
tddWorkflow_coreRule_containsNeverWriteProductionCodeWithoutFailingTest
doubleLoopTdd_sectionExists_containsH2Header
doubleLoopTdd_loops_containsOuterAndInnerDescriptions
doubleLoopTdd_diagram_containsMermaidOrAsciiDiagram
tpp_sectionExists_containsH2Header
tpp_transformations_containsAtLeast7OrderedItems
tpp_boundaries_firstIsNilLastIsMutatedValue
scenarioOrdering_sectionExists_containsH2Header
scenarioOrdering_levels_contains6Levels
scenarioOrdering_boundaries_level1IsDegenerateCasesLevel6IsEdgeCases
backwardCompat_coverageThresholds_sectionPreserved
backwardCompat_testCategories_all8CategoriesPresent
backwardCompat_namingConvention_sectionPreserved
backwardCompat_testFixtures_sectionPreserved
backwardCompat_dataUniqueness_sectionPreserved
backwardCompat_asyncHandling_sectionPreserved
backwardCompat_prohibitions_sectionPreserved
backwardCompat_dbStrategy_sectionPreserved
```

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario (from story) | Test Group | Test IDs |
|-------------------------------|-----------|----------|
| KP contains TDD Workflow with Red-Green-Refactor | G2 | 9, 10, 11 |
| KP contains Double-Loop TDD | G3 | 12, 13, 14 |
| KP contains Transformation Priority Premise | G4 | 15, 16, 17 |
| KP contains Test Scenario Ordering by TPP | G5 | 18, 19, 20 |
| Existing KP content preserved | G6 | 21-28 |
| Dual copy consistency | G1 + Section 5 | 1-8 + dual copy tests |
| TDD sections informative for non-TDD projects | G6 | Backward compat ensures no prescriptive breakage |
