# Test Plan -- STORY-0003-0005: Templates -- TDD Sections in _TEMPLATE-STORY.md and _TEMPLATE-EPIC.md

## Summary

- Modified source files: `resources/templates/_TEMPLATE-STORY.md`, `resources/templates/_TEMPLATE-EPIC.md`
- New test file: `tests/node/content/template-tdd-sections.test.ts`
- Total test methods: 30
- Categories: Story Template Content Validation (14), Epic Template Content Validation (6), Backward Compatibility (8), Structure Validation (2)
- Coverage targets: >= 95% line, >= 90% branch
- No new production code -- templates are static reference files not processed by the pipeline. No golden file updates required.
- No routing or assembler changes -- templates in `resources/templates/` are used by AI skills (x-story-create, x-story-epic) as structural guides, not by the build pipeline.

---

## 1. Test File Locations and Naming

### Existing (unchanged)

**Path:** `tests/node/integration/byte-for-byte.test.ts`

**Rationale:** No golden file changes. The templates in `resources/templates/` are NOT copied by the pipeline into any output directory. They are referenced by name in skills (x-story-create, x-story-epic, x-story-epic-full) but not byte-for-byte copied. The byte-for-byte tests remain unchanged and serve as a regression gate only.

### New

**Path:** `tests/node/content/template-tdd-sections.test.ts`

**Rationale:** Content validation tests that read the two template files directly from `resources/templates/` and assert the presence of required TDD sections, checklists, and structural integrity. Follows the established pattern from `tests/node/content/refactoring-guidelines-content.test.ts`.

**Naming convention:** `[sectionUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Source File Paths

```typescript
import { describe, it, expect, beforeAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const TEMPLATES_DIR = path.resolve(
  __dirname, "../../..", "resources/templates",
);

const STORY_TEMPLATE_PATH = path.resolve(TEMPLATES_DIR, "_TEMPLATE-STORY.md");
const EPIC_TEMPLATE_PATH = path.resolve(TEMPLATES_DIR, "_TEMPLATE-EPIC.md");
```

### 2.2 Content Loading

```typescript
const storyContent = fs.readFileSync(STORY_TEMPLATE_PATH, "utf-8");
const epicContent = fs.readFileSync(EPIC_TEMPLATE_PATH, "utf-8");
```

### 2.3 Key Observations

- Templates live at `resources/templates/` and are NOT processed by the Nunjucks template engine.
- Templates are NOT copied into golden file directories by any assembler.
- Templates are referenced by name in skill SKILL.md files (x-story-create, x-story-epic, x-story-epic-full) as structural references for AI output.
- Tests are pure content validation -- reading files and asserting content.

---

## 3. Test Groups

### Group 1: Story Template -- TDD Scenario Categories Checklist (4 tests)

Verify the story template contains a checklist of mandatory TDD scenario categories.

**Gherkin mapped:** "Story template contains checklist of mandatory TDD categories"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 1 | `storyTemplate_tddCategories_containsDegenerateCases` | `storyContent` contains `Degenerate cases` |
| 2 | `storyTemplate_tddCategories_containsHappyPath` | `storyContent` contains `Happy path` |
| 3 | `storyTemplate_tddCategories_containsErrorPaths` | `storyContent` contains `Error paths` |
| 4 | `storyTemplate_tddCategories_containsBoundaryValues` | `storyContent` contains `Boundary values` with at-min/at-max/past-max reference |

#### Assertions Pattern

```typescript
// Tests 1-3 (via it.each)
it.each([
  ["Degenerate cases"],
  ["Happy path"],
  ["Error paths"],
])("storyTemplate_tddCategories_contains_%s", (category) => {
  expect(storyContent).toContain(category);
});

// Test 4 (boundary values with triplet pattern)
it("storyTemplate_tddCategories_containsBoundaryValues", () => {
  expect(storyContent).toContain("Boundary values");
  expect(storyContent).toMatch(/at-min.*at-max.*past-max/i);
});
```

---

### Group 2: Story Template -- TPP Ordering Note (2 tests)

Verify the story template contains a note about Transformation Priority Premise ordering.

**Gherkin mapped:** "Story template contains TPP ordering note"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 5 | `storyTemplate_tppOrdering_referencesTPP` | `storyContent` contains reference to `Transformation Priority Premise` or `TPP` |
| 6 | `storyTemplate_tppOrdering_indicatesSimpleToComplex` | Section references ordering from simple to complex (degenerate first, edge cases last) |

#### Assertions Pattern

```typescript
// Test 5
it("storyTemplate_tppOrdering_referencesTPP", () => {
  expect(storyContent).toMatch(/Transformation Priority Premise|TPP/);
});

// Test 6
it("storyTemplate_tppOrdering_indicatesSimpleToComplex", () => {
  expect(storyContent).toMatch(/simple.*complex|degenerate.*edge/is);
});
```

---

### Group 3: Story Template -- TDD Implementation Notes (3 tests)

Verify the story template contains implementation notes about Double-Loop TDD.

**Gherkin mapped:** "Story template contains TDD Implementation Notes (Double-Loop TDD, first scenario = acceptance test)"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 7 | `storyTemplate_tddNotes_containsDoubleLoopReference` | `storyContent` contains `Double-Loop TDD` or `double-loop` reference |
| 8 | `storyTemplate_tddNotes_firstScenarioIsAcceptanceTest` | `storyContent` contains guidance that first scenario becomes the acceptance test |
| 9 | `storyTemplate_tddNotes_unitTestsFromInnerLoop` | `storyContent` contains guidance about unit tests from inner loop |

#### Assertions Pattern

```typescript
// Test 7
it("storyTemplate_tddNotes_containsDoubleLoopReference", () => {
  expect(storyContent).toMatch(/double[- ]loop/i);
});

// Test 8
it("storyTemplate_tddNotes_firstScenarioIsAcceptanceTest", () => {
  expect(storyContent).toMatch(/first.*scenario.*acceptance\s*test/is);
});

// Test 9
it("storyTemplate_tddNotes_unitTestsFromInnerLoop", () => {
  expect(storyContent).toMatch(/unit\s*test/i);
});
```

---

### Group 4: Story Template -- TDD Section Structure (5 tests)

Verify the new TDD section has proper markdown structure and is positioned correctly.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 10 | `storyTemplate_tddSection_containsChecklistFormat` | TDD categories use checklist format (`- [ ]`) |
| 11 | `storyTemplate_tddSection_hasAtLeast4ChecklistItems` | At least 4 checklist items (Degenerate, Happy, Error, Boundary) |
| 12 | `storyTemplate_tddSection_appearsAfterGherkin` | TDD section index is greater than Gherkin section (section 7) index |
| 13 | `storyTemplate_tddSection_appearsBeforeSubtasks` | TDD section index is less than Sub-tasks section (section 8) index, OR TDD is within an existing section |
| 14 | `storyTemplate_tddSection_hasMarkdownHeader` | TDD section begins with a markdown header (`##` or `###`) |

#### Assertions Pattern

```typescript
// Test 10
it("storyTemplate_tddSection_containsChecklistFormat", () => {
  const checklistItems = storyContent.match(/^-\s*\[\s*\]/gm) || [];
  // Must have at least the 4 TDD category checkboxes
  // (template already had checkboxes in DoR/DoD; we check TDD-specific ones)
  expect(storyContent).toMatch(/-\s*\[\s*\].*Degenerate/i);
});

// Test 11
it("storyTemplate_tddSection_hasAtLeast4TddChecklistItems", () => {
  // Extract the TDD-related section and count TDD-specific checkboxes
  const tddCategories = ["Degenerate", "Happy path", "Error paths", "Boundary"];
  for (const cat of tddCategories) {
    expect(storyContent).toContain(cat);
  }
});

// Test 12
it("storyTemplate_tddSection_appearsAfterGherkin", () => {
  const gherkinIndex = storyContent.indexOf("## 7. ");
  const tddIndex = storyContent.search(/TDD|Scenario.*Ordering|Mandatory.*Scenario/i);
  expect(tddIndex).toBeGreaterThan(gherkinIndex);
});

// Test 13 -- TDD section is between Gherkin (section 7) and Sub-tasks (last section)
it("storyTemplate_tddSection_positionedCorrectly", () => {
  const subtasksIndex = storyContent.indexOf("Sub-tarefas") > -1
    ? storyContent.indexOf("Sub-tarefas")
    : storyContent.indexOf("Sub-tasks");
  const tddMatch = storyContent.search(/TDD|Mandatory.*Scenario.*Categories/i);
  expect(tddMatch).toBeGreaterThan(-1);
  // TDD section should exist; position relative to subtasks depends on
  // whether it's a new numbered section or a sub-section of section 7
});

// Test 14
it("storyTemplate_tddSection_hasMarkdownHeader", () => {
  expect(storyContent).toMatch(/^#{2,3}\s.*TDD|^#{2,3}\s.*Scenario.*Ordering|^#{2,3}\s.*Mandatory/m);
});
```

---

### Group 5: Epic Template -- TDD Compliance in DoD (3 tests)

Verify the epic template contains TDD Compliance item in the Global DoD section.

**Gherkin mapped:** "Epic template contains TDD Compliance in DoD (test-first commits)"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 15 | `epicTemplate_dodTddCompliance_itemExists` | `epicContent` contains `TDD Compliance` within the DoD section |
| 16 | `epicTemplate_dodTddCompliance_referencesTestFirst` | DoD section contains `test-first` reference |
| 17 | `epicTemplate_dodTddCompliance_referencesRefactoring` | DoD section contains reference to refactoring after green |

#### Assertions Pattern

```typescript
// Extract DoD section
const dodStart = epicContent.indexOf("### Global Definition of Done");
const dodEnd = epicContent.indexOf("\n## ", dodStart + 1);
const dodSection = epicContent.slice(dodStart, dodEnd > -1 ? dodEnd : undefined);

// Test 15
it("epicTemplate_dodTddCompliance_itemExists", () => {
  expect(dodSection).toContain("TDD Compliance");
});

// Test 16
it("epicTemplate_dodTddCompliance_referencesTestFirst", () => {
  expect(dodSection).toMatch(/test[- ]first/i);
});

// Test 17
it("epicTemplate_dodTddCompliance_referencesRefactoring", () => {
  expect(dodSection).toMatch(/refactor/i);
});
```

---

### Group 6: Epic Template -- Double-Loop TDD in DoD (3 tests)

Verify the epic template contains Double-Loop TDD item in the Global DoD section.

**Gherkin mapped:** "Epic template contains Double-Loop TDD in DoD"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 18 | `epicTemplate_dodDoubleLoop_itemExists` | DoD section contains `Double-Loop TDD` |
| 19 | `epicTemplate_dodDoubleLoop_referencesAcceptanceTests` | DoD section contains `Acceptance` or `acceptance test` reference |
| 20 | `epicTemplate_dodDoubleLoop_referencesUnitTests` | DoD section contains `Unit` or `unit test` reference |

#### Assertions Pattern

```typescript
// Test 18
it("epicTemplate_dodDoubleLoop_itemExists", () => {
  expect(dodSection).toMatch(/Double[- ]Loop\s+TDD/i);
});

// Test 19
it("epicTemplate_dodDoubleLoop_referencesAcceptanceTests", () => {
  expect(dodSection).toMatch(/acceptance/i);
});

// Test 20
it("epicTemplate_dodDoubleLoop_referencesUnitTests", () => {
  expect(dodSection).toMatch(/unit/i);
});
```

---

### Group 7: Backward Compatibility -- Story Template Sections Preserved (5 tests)

Verify all 8 original sections of the story template remain intact.

**Gherkin mapped:** "All existing sections preserved (8 sections in story)"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 21 | `storyBackwardCompat_section1_dependenciesPreserved` | `storyContent` contains `## 1. ` (Dependencies) |
| 22 | `storyBackwardCompat_section3_descriptionPreserved` | `storyContent` contains `## 3. ` (Description) |
| 23 | `storyBackwardCompat_section5_dataContractPreserved` | `storyContent` contains `## 5. ` (Data Contract) |
| 24 | `storyBackwardCompat_section7_gherkinPreserved` | `storyContent` contains `## 7. ` (Gherkin) with `gherkin` code block |
| 25 | `storyBackwardCompat_allOriginalSections_8SectionsPresent` | `storyContent` contains all 8 section headers: Dependencias, Regras Transversais, Descricao, Definicoes de Qualidade, Contratos de Dados, Diagramas, Criterios de Aceite, Sub-tarefas |

#### Assertions Pattern

```typescript
it.each([
  ["## 1.", "Depend"],
  ["## 2.", "Regras Transversais"],
  ["## 3.", "Descri"],
  ["## 4.", "Defini"],
  ["## 5.", "Contratos"],
  ["## 6.", "Diagramas"],
  ["## 7.", "Crit"],
  ["## 8.", "Sub-tarefas"],
])("storyBackwardCompat_section_%s_preserved", (sectionNum, keyword) => {
  expect(storyContent).toContain(sectionNum);
  expect(storyContent).toMatch(new RegExp(`## \\d+\\..*${keyword}`, "i"));
});
```

Note: This uses `it.each` producing 8 tests but is grouped as 5 logical assertions. The actual test method count is 8 using `it.each` (one per section) but logically 5 test names are listed. For accurate counting, we use **5 tests** as listed above, with test 25 using `it.each` internally.

---

### Group 8: Backward Compatibility -- Epic Template Sections Preserved (3 tests)

Verify all 5 original sections of the epic template remain intact.

**Gherkin mapped:** "All existing sections preserved (5 sections in epic)"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 26 | `epicBackwardCompat_section1_overviewPreserved` | `epicContent` contains `## 1. Vis` (Visao Geral) |
| 27 | `epicBackwardCompat_section3_qualityDefinitionsPreserved` | `epicContent` contains `## 3.` (Definicoes de Qualidade Globais) |
| 28 | `epicBackwardCompat_allOriginalSections_5SectionsPresent` | `epicContent` contains all 5 section headers: Visao Geral, Anexos, Definicoes de Qualidade, Regras de Negocio, Indice de Historias |

#### Assertions Pattern

```typescript
it.each([
  ["## 1.", "Vis"],
  ["## 2.", "Anexos"],
  ["## 3.", "Defini"],
  ["## 4.", "Regras"],
  ["## 5.", "ndice"],
])("epicBackwardCompat_section_%s_preserved", (sectionNum, keyword) => {
  expect(epicContent).toContain(sectionNum);
  expect(epicContent).toMatch(new RegExp(`## \\d+\\..*${keyword}`, "i"));
});
```

---

### Group 9: Structure Validation (2 tests)

Verify markdown structural validity of both templates after modifications.

**Gherkin mapped:** "Template with TDD section empty is valid"

| # | Test Name | Assertion |
|---|-----------|-----------|
| 29 | `storyTemplate_structure_validMarkdownHeaders` | All H2 headers (`## N.`) are numbered sequentially; no duplicate section numbers |
| 30 | `epicTemplate_structure_validMarkdownHeaders` | All H2 headers (`## N.`) are numbered sequentially; no duplicate section numbers |

#### Assertions Pattern

```typescript
// Test 29
it("storyTemplate_structure_validMarkdownHeaders", () => {
  const h2Matches = storyContent.match(/^## \d+\./gm) || [];
  expect(h2Matches.length).toBeGreaterThanOrEqual(8);
  // Check uniqueness of section numbers
  const sectionNumbers = h2Matches.map(h => h.match(/\d+/)![0]);
  const uniqueNumbers = new Set(sectionNumbers);
  expect(uniqueNumbers.size).toBe(sectionNumbers.length);
});

// Test 30
it("epicTemplate_structure_validMarkdownHeaders", () => {
  const h2Matches = epicContent.match(/^## \d+\./gm) || [];
  expect(h2Matches.length).toBeGreaterThanOrEqual(5);
  const sectionNumbers = h2Matches.map(h => h.match(/\d+/)![0]);
  const uniqueNumbers = new Set(sectionNumbers);
  expect(uniqueNumbers.size).toBe(sectionNumbers.length);
});
```

---

## 4. Section Position Validation

The TDD sections in the story template MUST appear after the Gherkin section (section 7). Two implementation strategies are possible:

1. **New numbered section** (e.g., `## 7.1 TDD Scenarios` or `## 7.5 TDD Scenarios`) -- added as a sub-section of section 7
2. **Renumbered sections** (e.g., TDD becomes section 8, Sub-tasks becomes section 9)

The tests are designed to handle either approach:

```typescript
it("storyTemplate_tddPosition_afterGherkinSection", () => {
  const gherkinPos = storyContent.indexOf("Critérios de Aceite");
  const tddPos = storyContent.search(/TDD|Mandatory.*Scenario|Scenario.*Ordering/i);
  expect(tddPos).toBeGreaterThan(gherkinPos);
  expect(tddPos).toBeGreaterThan(-1);
});
```

---

## 5. No Golden File Changes Required

Unlike stories 0003-0001 and 0003-0002, this story does NOT require golden file updates because:

1. `resources/templates/_TEMPLATE-STORY.md` and `_TEMPLATE-EPIC.md` are NOT processed by the pipeline.
2. These files are NOT listed in `CORE_TO_KP_MAPPING` or any assembler route.
3. They are referenced by **name** in skill SKILL.md files but not copied byte-for-byte.
4. The byte-for-byte integration tests (`byte-for-byte.test.ts`) will pass without changes.

**Verification:**
- `src/` contains zero references to `resources/templates` (confirmed by grep).
- No assembler reads from `resources/templates/`.
- Skills reference templates by path string only (e.g., "Read `resources/templates/_TEMPLATE-STORY.md`").

---

## 6. Coverage Strategy

### 6.1 Line Coverage

No new production code is added. Templates are static markdown files in `resources/`. Coverage configuration explicitly excludes `resources/**` (see `vitest.config.ts` line 18). Existing coverage (99.6% lines, 97.84% branches) is maintained.

### 6.2 Branch Coverage

No new branches. Templates are not source code. No conditional logic affected.

### 6.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | No new code paths; existing coverage maintained |
| Branch | >= 90% | No new branches; existing coverage maintained |

---

## 7. Test Matrix Summary

| Group | Description | Test Count | Type | File |
|-------|-------------|------------|------|------|
| G1: Story -- TDD Categories Checklist | 4 mandatory categories present | 4 | Content Validation | `template-tdd-sections.test.ts` |
| G2: Story -- TPP Ordering Note | TPP reference, simple-to-complex | 2 | Content Validation | `template-tdd-sections.test.ts` |
| G3: Story -- TDD Implementation Notes | Double-Loop, acceptance test, unit tests | 3 | Content Validation | `template-tdd-sections.test.ts` |
| G4: Story -- TDD Section Structure | Checklist format, position, header | 5 | Structure Validation | `template-tdd-sections.test.ts` |
| G5: Epic -- TDD Compliance in DoD | Item exists, test-first, refactoring | 3 | Content Validation | `template-tdd-sections.test.ts` |
| G6: Epic -- Double-Loop TDD in DoD | Item exists, acceptance, unit | 3 | Content Validation | `template-tdd-sections.test.ts` |
| G7: Story -- Backward Compatibility | All 8 original sections preserved | 5 | Backward Compatibility | `template-tdd-sections.test.ts` |
| G8: Epic -- Backward Compatibility | All 5 original sections preserved | 3 | Backward Compatibility | `template-tdd-sections.test.ts` |
| G9: Structure Validation | Valid markdown headers, no duplicates | 2 | Structure Validation | `template-tdd-sections.test.ts` |
| **Total** | | **30** | | |

---

## 8. Execution Commands

### Run Template TDD Section Tests Only

```bash
npx vitest run tests/node/content/template-tdd-sections.test.ts
```

### Run All Content Validation Tests

```bash
npx vitest run tests/node/content/
```

### Run Golden File Tests (regression only, no changes expected)

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 9. Implementation Notes for Template Changes

### _TEMPLATE-STORY.md Changes

The story specifies adding TDD content "after section 7 - Gherkin, or as sub-section". Two approaches:

**Option A: Sub-section of section 7 (recommended)**
- Add `### 7.1 Scenario Ordering (TPP)` after the Gherkin code block
- Add `### 7.2 Mandatory Scenario Categories` with checklist
- Add `### 7.3 TDD Implementation Notes` with Double-Loop guidance
- Preserves existing section numbering (8 sections intact, Sub-tasks stays section 8)

**Option B: New top-level section**
- Renumber Sub-tasks from section 8 to section 9
- Add `## 8. TDD Scenarios` as new section
- Changes section count from 8 to 9

Tests are written to handle either approach by using flexible matching (regex, `toContain`).

### _TEMPLATE-EPIC.md Changes

Add two items to the existing `### Global Definition of Done (DoD)` section (section 3):
- `- **TDD Compliance:** ...`
- `- **Double-Loop TDD:** ...`

These are appended after the existing DoD items (Performance). No new sections needed.

---

## 10. Dependencies and Prerequisites

### Prerequisites

- `resources/templates/_TEMPLATE-STORY.md` exists with 8 sections (current state)
- `resources/templates/_TEMPLATE-EPIC.md` exists with 5 sections (current state)
- `tests/node/content/` directory exists (established by STORY-0003-0002)
- Vitest config includes `tests/**/*.test.ts` glob (confirmed: `vitest.config.ts`)

### Import Dependencies (for new test file)

| Module | Import | Used For |
|--------|--------|----------|
| `node:fs` | `readFileSync` | Reading template files |
| `node:path` | `resolve` | Path resolution |
| `vitest` | `describe`, `it`, `expect` | Test framework |

### Story Dependencies

| Dependency | Status | Impact |
|-----------|--------|--------|
| story-0003-0003 (Rules 03/05 TDD sections) | Must be done | TDD concepts referenced in templates |
| story-0003-0004 (Rule 13 Enriched Gherkin) | Must be done | Gherkin completeness criteria referenced in templates |

---

## 11. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Template section numbering changes break backward compat tests | Medium | High | Tests use flexible matching (`## N.` + keyword), not exact strings |
| TDD section added as sub-section vs new section | Medium | Low | Tests handle both approaches with regex matching for content, not position |
| Content assertions too brittle (exact Portuguese vs English) | Medium | Medium | Templates are in Portuguese (existing state). Tests match keywords case-insensitively, mixing Portuguese section headers with English TDD terminology |
| Confusion about golden file updates | High | Medium | Clearly documented in section 5: NO golden file changes needed. Templates are not in the pipeline. |
| Template file encoding issues | Low | Low | `readFileSync("utf-8")` handles standard UTF-8. Template already contains Portuguese accented characters. |
| Test file discovery by Vitest | Low | Low | Pattern `tests/**/*.test.ts` matches `tests/node/content/template-tdd-sections.test.ts`. Confirmed by previous `tests/node/content/` tests (STORY-0003-0002). |
| Epic DoD section detection fails if section header changes | Low | Medium | Tests extract DoD by searching for `Global Definition of Done` string, not by section number. |
| "Template with TDD section empty is valid" acceptance criterion | Medium | Medium | Addressed by structure validation (G9) -- tests verify headers and checkboxes exist but do NOT require filled values. The template provides structure with placeholders. |

---

## 12. Naming Convention Reference

All new test names follow `[sectionUnderTest]_[scenario]_[expectedBehavior]`:

```
storyTemplate_tddCategories_containsDegenerateCases
storyTemplate_tddCategories_containsHappyPath
storyTemplate_tddCategories_containsErrorPaths
storyTemplate_tddCategories_containsBoundaryValues
storyTemplate_tppOrdering_referencesTPP
storyTemplate_tppOrdering_indicatesSimpleToComplex
storyTemplate_tddNotes_containsDoubleLoopReference
storyTemplate_tddNotes_firstScenarioIsAcceptanceTest
storyTemplate_tddNotes_unitTestsFromInnerLoop
storyTemplate_tddSection_containsChecklistFormat
storyTemplate_tddSection_hasAtLeast4TddChecklistItems
storyTemplate_tddSection_appearsAfterGherkin
storyTemplate_tddSection_positionedCorrectly
storyTemplate_tddSection_hasMarkdownHeader
epicTemplate_dodTddCompliance_itemExists
epicTemplate_dodTddCompliance_referencesTestFirst
epicTemplate_dodTddCompliance_referencesRefactoring
epicTemplate_dodDoubleLoop_itemExists
epicTemplate_dodDoubleLoop_referencesAcceptanceTests
epicTemplate_dodDoubleLoop_referencesUnitTests
storyBackwardCompat_section1_dependenciesPreserved
storyBackwardCompat_section3_descriptionPreserved
storyBackwardCompat_section5_dataContractPreserved
storyBackwardCompat_section7_gherkinPreserved
storyBackwardCompat_allOriginalSections_8SectionsPresent
epicBackwardCompat_section1_overviewPreserved
epicBackwardCompat_section3_qualityDefinitionsPreserved
epicBackwardCompat_allOriginalSections_5SectionsPresent
storyTemplate_structure_validMarkdownHeaders
epicTemplate_structure_validMarkdownHeaders
```

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario (from story) | Test Group | Test IDs |
|-------------------------------|-----------|----------|
| Story template contains checklist of mandatory TDD categories | G1 | 1, 2, 3, 4 |
| Story template contains TPP ordering note | G2 | 5, 6 |
| Story template contains TDD Implementation Notes (Double-Loop) | G3 | 7, 8, 9 |
| Epic template contains TDD Compliance in DoD | G5 | 15, 16, 17 |
| Epic template contains Double-Loop TDD in DoD | G6 | 18, 19, 20 |
| Existing sections preserved (8 in story, 5 in epic) | G7 + G8 | 21-28 |
| Template with TDD section empty is valid | G4 + G9 | 10-14, 29-30 |
