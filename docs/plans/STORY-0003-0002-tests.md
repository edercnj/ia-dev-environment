# Test Plan -- STORY-0003-0002: Coding Standards KP -- Refactoring Guidelines

## Summary

- New source file: `resources/core/14-refactoring-guidelines.md`
- New routing entry: `src/domain/core-kp-routing.ts` (1 static route added)
- Affected golden files: 16 new files across 8 profiles (`.claude/` + `.agents/` copies)
- Modified test file: `tests/node/domain/core-kp-routing.test.ts` (assertion updates)
- New test file: `tests/node/content/refactoring-guidelines-content.test.ts`
- Total test methods: 32
- Categories: Unit (6), Golden File Integration (8), Content Validation (10), Backward Compatibility (4), Dual Copy Consistency (4)
- Coverage targets: >= 95% line, >= 90% branch
- No new business logic code -- one routing entry added plus a content file, verified by existing infrastructure and new content assertions

---

## 1. Test File Locations and Naming

### Existing (modified)

**Path:** `tests/node/domain/core-kp-routing.test.ts`

**Rationale:** The new route entry in `CORE_TO_KP_MAPPING` changes array length from 11 to 12 and shifts the last-route index. All count assertions and the `lastRoute` test must be updated.

### Existing (unchanged, but golden files updated)

**Path:** `tests/node/integration/byte-for-byte.test.ts`

**Rationale:** No code changes to this file. Golden file directories must be updated to include the new `refactoring-guidelines.md` in `.claude/skills/coding-standards/references/` and `.agents/skills/coding-standards/references/` for all 8 profiles.

### Existing (unchanged)

**Path:** `tests/node/assembler/rules-assembler.test.ts`

**Rationale:** The `routeCoreToKps` tests in this file use a minimal `resources/` setup that only creates `01-clean-code.md`, `02-solid-principles.md`, and `03-testing-philosophy.md` in the test temp directory. The assembler picks up routes generically from `getActiveRoutes()`. Since the test does not create `14-refactoring-guidelines.md` in its temp dir, the assembler will silently skip the missing file (graceful skip behavior). No changes required.

### New

**Path:** `tests/node/content/refactoring-guidelines-content.test.ts`

**Rationale:** Content validation tests that verify the structural integrity of the 3 required sub-sections in the new refactoring guidelines file. Separated from byte-for-byte tests because they validate semantic content, not binary equality.

**Naming convention:** `[sectionUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Source File Path (for content tests)

```typescript
import { resolve, dirname } from "node:path";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, it, expect, beforeAll } from "vitest";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

const SOURCE_FILE = resolve(RESOURCES_DIR, "core", "14-refactoring-guidelines.md");
```

### 2.2 Content Loading

```typescript
let content: string;

beforeAll(() => {
  content = readFileSync(SOURCE_FILE, "utf-8");
});
```

### 2.3 Golden File Paths

The pipeline copies `resources/core/14-refactoring-guidelines.md` to two locations per profile:

1. `{profile}/.claude/skills/coding-standards/references/refactoring-guidelines.md`
2. `{profile}/.agents/skills/coding-standards/references/refactoring-guidelines.md`

Golden files live at `tests/golden/{profile}/` for all 8 profiles defined in `tests/helpers/integration-constants.ts`:

- `go-gin`
- `java-quarkus`
- `java-spring`
- `kotlin-ktor`
- `python-click-cli`
- `python-fastapi`
- `rust-axum`
- `typescript-nestjs`

Note: `.github/skills/coding-standards/` only contains `SKILL.md` (no `references/` directory). The `GithubSkillsAssembler` does not copy reference files. Only `.claude/` and `.agents/` carry references. Confirmed by golden file inspection.

---

## 3. Test Groups

### Group 1: Unit Tests -- `core-kp-routing.ts` (6 tests -- existing file, modified)

**Path:** `tests/node/domain/core-kp-routing.test.ts`

These tests validate the routing table after adding the new entry for `14-refactoring-guidelines.md`.

| # | Test Name | Current Assertion | New Assertion | Change |
|---|-----------|-------------------|---------------|--------|
| 1 | `contains_11_staticRoutes` -> `contains_12_staticRoutes` | `toHaveLength(11)` | `toHaveLength(12)` | Rename + update count |
| 2 | `firstRoute_isCleanCode` | unchanged | unchanged | No change |
| 3 | `lastRoute_isStoryDecomposition` -> `lastRoute_isRefactoringGuidelines` | index `[10]`, source `13-story-decomposition.md` | index `[11]`, source `14-refactoring-guidelines.md` | Update to new last route |
| 4 | `microservice_includes12Routes` -> `microservice_includes13Routes` | `toHaveLength(12)` | `toHaveLength(13)` | Update count |
| 5 | `library_excludesCloudNative_returns11Routes` -> `library_excludesCloudNative_returns12Routes` | `toHaveLength(11)` | `toHaveLength(12)` | Update count |
| 6 | `monolith_includesCloudNative` | `toHaveLength(12)` | `toHaveLength(13)` | Update count |

**Additional validation for the new route entry:**

The updated `lastRoute` test must assert:

```typescript
it("lastRoute_isRefactoringGuidelines", () => {
  const last = CORE_TO_KP_MAPPING[11]!;
  expect(last.sourceFile).toBe("14-refactoring-guidelines.md");
  expect(last.kpName).toBe("coding-standards");
  expect(last.destFile).toBe("refactoring-guidelines.md");
});
```

**Decision: Insertion position.** Per the implementation plan, the new route is appended at the end of `CORE_TO_KP_MAPPING` (after `13-story-decomposition.md`). This makes `14-refactoring-guidelines.md` the new last element at index `[11]`.

If preserving the `story-decomposition` test is desired, an alternative approach is to keep the existing `lastRoute_isStoryDecomposition` test and add a new `refactoringGuidelines_routeExists` test. The recommended approach (per plan) is to update the `lastRoute` test to validate the new last entry.

#### Execution

```bash
npx vitest run tests/node/domain/core-kp-routing.test.ts
```

---

### Group 2: Golden File Integration (8 tests -- existing suite, no code changes)

The existing `byte-for-byte.test.ts` runs `describe.sequential.each` over all 8 `CONFIG_PROFILES`. Each profile runs 5 assertions (pipeline success, golden match, no missing files, no extra files, total files > 0). The golden files must include the new `refactoring-guidelines.md` file.

**Action required:** Add `refactoring-guidelines.md` to all 16 golden directories (8 profiles x 2 copy locations each).

| # | Test Name (existing) | Profile | Assertion |
|---|---------------------|---------|-----------|
| 7 | `pipelineMatchesGoldenFiles_go-gin` | go-gin | `verifyOutput()` returns `success: true` |
| 8 | `pipelineMatchesGoldenFiles_java-quarkus` | java-quarkus | `verifyOutput()` returns `success: true` |
| 9 | `pipelineMatchesGoldenFiles_java-spring` | java-spring | `verifyOutput()` returns `success: true` |
| 10 | `pipelineMatchesGoldenFiles_kotlin-ktor` | kotlin-ktor | `verifyOutput()` returns `success: true` |
| 11 | `pipelineMatchesGoldenFiles_python-click-cli` | python-click-cli | `verifyOutput()` returns `success: true` |
| 12 | `pipelineMatchesGoldenFiles_python-fastapi` | python-fastapi | `verifyOutput()` returns `success: true` |
| 13 | `pipelineMatchesGoldenFiles_rust-axum` | rust-axum | `verifyOutput()` returns `success: true` |
| 14 | `pipelineMatchesGoldenFiles_typescript-nestjs` | typescript-nestjs | `verifyOutput()` returns `success: true` |

**Verification logic:** `verifyOutput()` in `src/verifier.ts` compares two directory trees byte-for-byte using `Buffer.equals()`. If a golden `refactoring-guidelines.md` is missing, the test will report `EXTRA` files (pipeline generates it but golden doesn't have it). If golden file content differs from pipeline output, the test will report a `MISMATCH`.

**Key difference from STORY-0003-0001:** This story adds a NEW file to the golden directories (not modifying an existing one). The `noExtraFiles` and `noMissingFiles` assertions will catch any discrepancy between the expected and actual file set.

#### Execution

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

---

### Group 3: Content Validation -- Refactoring Guidelines Section (3 tests)

**Path:** `tests/node/content/refactoring-guidelines-content.test.ts`

Verify the top-level `## Refactoring Guidelines` section exists and contains the required 3 sub-sections.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 15 | `refactoringGuidelines_sectionExists_containsH2Header` | `content` contains `## Refactoring Guidelines` |
| 16 | `refactoringGuidelines_subSections_containsTriggersAndTechniquesAndSafetyRules` | `content` contains all 3 sub-section headers: `### Refactoring Triggers`, `### Prioritized Techniques`, `### Safety Rules` |
| 17 | `refactoringGuidelines_language_contentIsEnglishOnly` | `content` does NOT contain Portuguese keywords (per RULE-012); all section headers are in English |

#### Assertions Pattern

```typescript
// Test 15
expect(content).toContain("## Refactoring Guidelines");

// Test 16
expect(content).toContain("### Refactoring Triggers");
expect(content).toContain("### Prioritized Techniques");
expect(content).toContain("### Safety Rules");

// Test 17 -- verify English-only content (RULE-012)
const portuguesePatterns = /\b(quando|técnica|regra|seção|nunca)\b/i;
expect(content).not.toMatch(portuguesePatterns);
```

---

### Group 4: Content Validation -- Refactoring Triggers (3 tests)

Verify the triggers sub-section documents the refactoring criteria from the story acceptance criteria.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 18 | `refactoringTriggers_functionLimit_references25Lines` | Triggers section references `25 line` threshold (the existing Hard Limit for function length) |
| 19 | `refactoringTriggers_classLimit_references250Lines` | Triggers section references `250 line` threshold (the existing Hard Limit for class length) |
| 20 | `refactoringTriggers_naming_referencesIntentRevealing` | Triggers section references naming/intent criteria (CC-01 cross-reference) |

#### Assertions Pattern

```typescript
const triggersStart = content.indexOf("### Refactoring Triggers");
const triggersEnd = content.indexOf("\n### ", triggersStart + 1);
const triggersSection = content.slice(triggersStart, triggersEnd > -1 ? triggersEnd : undefined);

// Test 18
expect(triggersSection).toMatch(/25\s*line/i);

// Test 19
expect(triggersSection).toMatch(/250\s*line/i);

// Test 20
expect(triggersSection).toMatch(/intent|naming|CC-01/i);
```

---

### Group 5: Content Validation -- Prioritized Techniques (2 tests)

Verify the techniques sub-section lists refactoring techniques in TDD frequency order.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 21 | `prioritizedTechniques_firstTechnique_isExtractMethod` | "Extract Method" is the first technique listed (per story: most common in TDD) |
| 22 | `prioritizedTechniques_count_containsAtLeast6Techniques` | Section contains at least 6 techniques (as specified in story section 3.2) |

#### Assertions Pattern

```typescript
const techStart = content.indexOf("### Prioritized Techniques");
const techEnd = content.indexOf("\n### ", techStart + 1);
const techSection = content.slice(techStart, techEnd > -1 ? techEnd : undefined);

// Test 21
const firstTechniqueMatch = techSection.match(/\d+\.\s*(.+)/);
expect(firstTechniqueMatch).not.toBeNull();
expect(firstTechniqueMatch![1]).toMatch(/extract\s*method/i);

// Test 22
const numberedItems = techSection.match(/^\s*\d+\.\s/gm) || [];
expect(numberedItems.length).toBeGreaterThanOrEqual(6);
```

---

### Group 6: Content Validation -- Safety Rules (2 tests)

Verify the safety rules sub-section contains the 5 rules from the story.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 23 | `safetyRules_neverAddBehavior_rulePresent` | Section contains the rule "never add behavior during refactoring" (case-insensitive) |
| 24 | `safetyRules_count_containsAtLeast5Rules` | Section contains at least 5 numbered or bulleted rules |

#### Assertions Pattern

```typescript
const safetyStart = content.indexOf("### Safety Rules");
const safetyEnd = content.indexOf("\n## ", safetyStart + 1);
const safetySection = content.slice(safetyStart, safetyEnd > -1 ? safetyEnd : undefined);

// Test 23
expect(safetySection.toLowerCase()).toContain("never add behavior");

// Additional safety rule assertions
expect(safetySection.toLowerCase()).toContain("all tests");
expect(safetySection.toLowerCase()).toMatch(/undo|revert/);

// Test 24
const ruleItems = safetySection.match(/^\s*(\d+\.|[-*])\s/gm) || [];
expect(ruleItems.length).toBeGreaterThanOrEqual(5);
```

---

### Group 7: Backward Compatibility -- Existing KP Content Preserved (4 tests)

Verify that existing content in the `coding-standards` knowledge pack is not affected by the addition. The new file (`refactoring-guidelines.md`) is a separate file -- it does NOT modify existing files like `clean-code.md` or `solid-principles.md`. These tests ensure the existing reference files are still present and unchanged.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 25 | `backwardCompat_cleanCode_fileStillPresent` | Golden file `clean-code.md` exists in `.claude/skills/coding-standards/references/` for all profiles |
| 26 | `backwardCompat_solidPrinciples_fileStillPresent` | Golden file `solid-principles.md` exists in `.claude/skills/coding-standards/references/` for all profiles |
| 27 | `backwardCompat_routingTable_existingRoutesPreserved` | All 11 original routes in `CORE_TO_KP_MAPPING` are unchanged (first route is clean-code, routes include all original source files) |
| 28 | `backwardCompat_existingGoldenFileCount_notReduced` | Each profile's `.claude/skills/coding-standards/references/` directory has MORE files than before (no files removed) |

#### Assertions Pattern

```typescript
// Test 25-26 (in content test file)
import { CONFIG_PROFILES, GOLDEN_DIR } from "../../helpers/integration-constants.js";
import { existsSync } from "node:fs";

for (const profile of CONFIG_PROFILES) {
  expect(existsSync(resolve(GOLDEN_DIR, profile,
    ".claude/skills/coding-standards/references/clean-code.md"))).toBe(true);
  expect(existsSync(resolve(GOLDEN_DIR, profile,
    ".claude/skills/coding-standards/references/solid-principles.md"))).toBe(true);
}

// Test 27 (in core-kp-routing.test.ts)
const sourceFiles = CORE_TO_KP_MAPPING.map(r => r.sourceFile);
expect(sourceFiles).toContain("01-clean-code.md");
expect(sourceFiles).toContain("02-solid-principles.md");
expect(sourceFiles).toContain("13-story-decomposition.md");

// Test 28
// Existing profiles have at least: clean-code.md, solid-principles.md, coding-conventions.md
// After this story, they should also have refactoring-guidelines.md
// The byte-for-byte tests (Group 2) catch any missing or extra files
```

---

### Group 8: Dual Copy Consistency (4 tests)

Verify RULE-001 compliance: `.claude/` and `.agents/` copies of `refactoring-guidelines.md` must be byte-identical. Tested on a representative subset of profiles.

| # | Test Name | Profile | Assertion |
|---|-----------|---------|-----------|
| 29 | `dualCopy_goGin_claudeAndAgentsCopiesMatch` | go-gin | `.claude/` copy equals `.agents/` copy |
| 30 | `dualCopy_javaSpring_claudeAndAgentsCopiesMatch` | java-spring | `.claude/` copy equals `.agents/` copy |
| 31 | `dualCopy_pythonFastapi_claudeAndAgentsCopiesMatch` | python-fastapi | `.claude/` copy equals `.agents/` copy |
| 32 | `dualCopy_typescriptNestjs_claudeAndAgentsCopiesMatch` | typescript-nestjs | `.claude/` copy equals `.agents/` copy |

**Alternative: Test all 8 profiles.** For thoroughness, iterate over `CONFIG_PROFILES`:

```typescript
import { CONFIG_PROFILES, GOLDEN_DIR } from "../../helpers/integration-constants.js";

describe("dual copy consistency -- refactoring-guidelines", () => {
  for (const profile of CONFIG_PROFILES) {
    it(`dualCopy_${profile}_claudeAndAgentsCopiesMatch`, () => {
      const claudeCopy = readFileSync(
        resolve(GOLDEN_DIR, profile,
          ".claude/skills/coding-standards/references/refactoring-guidelines.md"),
        "utf-8",
      );
      const agentsCopy = readFileSync(
        resolve(GOLDEN_DIR, profile,
          ".agents/skills/coding-standards/references/refactoring-guidelines.md"),
        "utf-8",
      );
      expect(claudeCopy).toBe(agentsCopy);
    });
  }
});
```

This approach adds 8 tests instead of 4. The byte-for-byte integration tests (Group 2) also validate this transitively, so the explicit dual-copy test provides defense-in-depth.

---

## 4. Section Ordering Validation

The new file `14-refactoring-guidelines.md` is a standalone reference file (not appended to an existing file). Ordering validation is therefore about the routing table, not section ordering within a file.

```typescript
it("refactoringGuidelines_routePosition_appendedAfterStoryDecomposition", () => {
  const storyDecompIndex = CORE_TO_KP_MAPPING.findIndex(
    r => r.sourceFile === "13-story-decomposition.md"
  );
  const refactoringIndex = CORE_TO_KP_MAPPING.findIndex(
    r => r.sourceFile === "14-refactoring-guidelines.md"
  );
  expect(refactoringIndex).toBeGreaterThan(storyDecompIndex);
});
```

---

## 5. Rules Assembler Impact Analysis

### No changes required to `tests/node/assembler/rules-assembler.test.ts`

The `rules-assembler.test.ts` file creates a minimal temp `resources/` directory with only 3 core files (`01-clean-code.md`, `02-solid-principles.md`, `03-testing-philosophy.md`). The assembler calls `getActiveRoutes()` which now returns 12 (or 13) routes, but the assembler silently skips any route whose source file is missing in the resources directory. This is already tested by `missingCoreSourceFile_skipsGracefully` and `missingCoreDir_skipsGracefully`.

The existing test `routesCoreFilesToKnowledgePacks` validates that `clean-code.md` arrives at `skills/coding-standards/references/clean-code.md`. This indirectly validates the routing mechanism that `14-refactoring-guidelines.md` will also use -- the same `routeCoreToKps()` code path.

---

## 6. Coverage Strategy

### 6.1 Line Coverage

The only new production code is one line added to `CORE_TO_KP_MAPPING` (a static array entry). This line is covered by all `getActiveRoutes()` tests since the array is spread into the result. Coverage impact: one additional line covered by existing tests.

### 6.2 Branch Coverage

No new branches. The new route is a static entry (no conditional logic). The only conditional routing is in `CONDITIONAL_CORE_KP` which is unchanged.

### 6.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | One new array entry, covered by existing `getActiveRoutes()` tests |
| Branch | >= 90% | No new branches; existing coverage maintained |

---

## 7. Test Matrix Summary

| Group | Description | Test Count | Type | File |
|-------|-------------|------------|------|------|
| G1: Unit -- Routing Table | Route count, new entry, position | 6 | Unit (modified) | `core-kp-routing.test.ts` |
| G2: Golden File Integration | Byte-for-byte parity across 8 profiles | 8 | Integration (existing, golden files updated) | `byte-for-byte.test.ts` |
| G3: Refactoring Guidelines Sections | H2 header, 3 sub-sections, English-only | 3 | Content Validation | `refactoring-guidelines-content.test.ts` |
| G4: Refactoring Triggers | 25-line limit, 250-line limit, naming | 3 | Content Validation | `refactoring-guidelines-content.test.ts` |
| G5: Prioritized Techniques | Extract Method first, 6+ techniques | 2 | Content Validation | `refactoring-guidelines-content.test.ts` |
| G6: Safety Rules | Never add behavior, 5+ rules | 2 | Content Validation | `refactoring-guidelines-content.test.ts` |
| G7: Backward Compatibility | Existing files preserved, routes preserved | 4 | Content Validation + Unit | Mixed |
| G8: Dual Copy Consistency | `.claude/` == `.agents/` for representative profiles | 4 (or 8) | Content Validation | `refactoring-guidelines-content.test.ts` |
| **Total** | | **32** (or **36** if all 8 profiles in G8) | | |

---

## 8. Execution Commands

### Run Unit Tests (Routing Table) Only

```bash
npx vitest run tests/node/domain/core-kp-routing.test.ts
```

### Run Content Validation Tests Only

```bash
npx vitest run tests/node/content/refactoring-guidelines-content.test.ts
```

### Run Golden File Tests Only

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

### Run All Story-Related Tests

```bash
npx vitest run tests/node/domain/core-kp-routing.test.ts tests/node/content/refactoring-guidelines-content.test.ts tests/node/integration/byte-for-byte.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 9. Golden File Update Procedure

### Step 1: Create Source File

Create `resources/core/14-refactoring-guidelines.md` with the content specified in the implementation plan (section "Content Specification").

### Step 2: Add Route

Add the routing entry to `src/domain/core-kp-routing.ts`:

```typescript
{ sourceFile: "14-refactoring-guidelines.md", kpName: "coding-standards", destFile: "refactoring-guidelines.md" },
```

### Step 3: Regenerate Golden Files

Run the pipeline for each profile and copy the new file to golden directories:

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  npx ts-node src/cli.ts generate \
    --config resources/config-templates/setup-config.${profile}.yaml \
    --output-dir /tmp/golden-${profile}
  cp /tmp/golden-${profile}/.claude/skills/coding-standards/references/refactoring-guidelines.md \
     tests/golden/${profile}/.claude/skills/coding-standards/references/refactoring-guidelines.md
  cp /tmp/golden-${profile}/.agents/skills/coding-standards/references/refactoring-guidelines.md \
     tests/golden/${profile}/.agents/skills/coding-standards/references/refactoring-guidelines.md
done
```

### Step 4: Verify

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 8 profiles must pass with `success: true`.

---

## 10. Dependencies and Prerequisites

### Prerequisites

- `resources/core/14-refactoring-guidelines.md` created (Step 1 of implementation)
- Routing entry added to `src/domain/core-kp-routing.ts` (Step 2)
- All 8 profile config templates exist in `resources/config-templates/`
- Golden file directories exist for all 8 profiles under `tests/golden/`
- `tests/helpers/integration-constants.ts` exports `CONFIG_PROFILES`, `GOLDEN_DIR`, `RESOURCES_DIR`
- `tests/fixtures/project-config.fixture.ts` exports `aDomainTestConfig`

### Import Dependencies (for new content test file)

| Module | Import | Used For |
|--------|--------|----------|
| `node:fs` | `readFileSync`, `existsSync` | Reading source file and golden files |
| `node:path` | `resolve`, `dirname` | Path resolution |
| `node:url` | `fileURLToPath` | ESM `__dirname` equivalent |
| `vitest` | `describe`, `it`, `expect`, `beforeAll` | Test framework |
| `tests/helpers/integration-constants.ts` | `CONFIG_PROFILES`, `GOLDEN_DIR` | Profile list and golden paths (for dual copy tests) |

### Import Dependencies (for modified routing test file)

| Module | Import | Used For |
|--------|--------|----------|
| `vitest` | `describe`, `it`, `expect` | Test framework (existing) |
| `src/domain/core-kp-routing.ts` | `CORE_TO_KP_MAPPING`, `CONDITIONAL_CORE_KP`, `getActiveRoutes` | Routing table under test (existing) |
| `tests/fixtures/project-config.fixture.ts` | `aDomainTestConfig` | Config factory (existing) |

---

## 11. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden files not created before commit | High | High | CI fails immediately -- `noExtraFiles` assertion detects pipeline output file not present in golden directory |
| Route count assertions not updated | High | High | Tests fail immediately with `expected 11 but received 12`. Developer must update all 5 count assertions. |
| `lastRoute` test index wrong | Medium | Medium | Off-by-one error if route inserted at wrong position. Test explicitly validates `CORE_TO_KP_MAPPING[11]` fields. |
| Content file uses Portuguese instead of English | Medium | Medium | Content validation test (Test 17) uses regex to detect Portuguese keywords. RULE-012 requires English-only generated content. |
| Source file number collision with future stories | Low | Low | Number `14` is verified as unused. Next available after `13-story-decomposition.md`. |
| `.github/skills/coding-standards/references/` expected to exist | Low | High | Confirmed: `.github/` does NOT have a `references/` directory for coding-standards. Only `.claude/` and `.agents/` carry references. Golden files confirm this. |
| Assembler test creates minimal resources -- new route skipped | Low | None | Expected behavior. `missingCoreSourceFile_skipsGracefully` test already validates this scenario. |
| Content assertions too brittle (exact string matching) | Medium | Medium | Use `toContain()` for section headers and key phrases, not full-line matching. Use case-insensitive regex for flexible assertions. |
| Test file discovery by Vitest | Low | Low | New test file at `tests/node/content/` must match the glob pattern in `vitest.config.ts` (`tests/**/*.test.ts`). Previous story (STORY-0003-0001) already established the `tests/node/content/` directory pattern. |

---

## 12. Naming Convention Reference

All new/modified test names follow `[sectionUnderTest]_[scenario]_[expectedBehavior]`:

### Modified (in `core-kp-routing.test.ts`)

```
contains_12_staticRoutes
lastRoute_isRefactoringGuidelines
microservice_includes13Routes
library_excludesCloudNative_returns12Routes
monolith_includesCloudNative (count updated to 13)
```

### New (in `refactoring-guidelines-content.test.ts`)

```
refactoringGuidelines_sectionExists_containsH2Header
refactoringGuidelines_subSections_containsTriggersAndTechniquesAndSafetyRules
refactoringGuidelines_language_contentIsEnglishOnly
refactoringTriggers_functionLimit_references25Lines
refactoringTriggers_classLimit_references250Lines
refactoringTriggers_naming_referencesIntentRevealing
prioritizedTechniques_firstTechnique_isExtractMethod
prioritizedTechniques_count_containsAtLeast6Techniques
safetyRules_neverAddBehavior_rulePresent
safetyRules_count_containsAtLeast5Rules
backwardCompat_cleanCode_fileStillPresent
backwardCompat_solidPrinciples_fileStillPresent
backwardCompat_routingTable_existingRoutesPreserved
backwardCompat_existingGoldenFileCount_notReduced
dualCopy_goGin_claudeAndAgentsCopiesMatch
dualCopy_javaSpring_claudeAndAgentsCopiesMatch
dualCopy_pythonFastapi_claudeAndAgentsCopiesMatch
dualCopy_typescriptNestjs_claudeAndAgentsCopiesMatch
```

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario (from story) | Test Group | Test IDs |
|-------------------------------|-----------|----------|
| KP contains Refactoring Guidelines section | G3 | 15, 16, 17 |
| Refactoring triggers include Hard Limits | G4 | 18, 19, 20 |
| Prioritized techniques in TDD frequency order | G5 | 21, 22 |
| Safety rules prevent adding behavior | G6 | 23, 24 |
| Existing KP content preserved | G7 | 25, 26, 27, 28 |
| Dual copy consistency | G2 + G8 | 7-14 + 29-32 |
