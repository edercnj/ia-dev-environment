# Implementation Plan: STORY-0003-0005 — TDD Sections in _TEMPLATE-STORY.md and _TEMPLATE-EPIC.md

## Summary

Add TDD-specific sections to two static markdown template files used by AI skills (`x-story-create`, `x-story-epic`) for story and epic generation. Specifically: (1) add a new section 8 "TDD Scenarios" to `_TEMPLATE-STORY.md` (after section 7 Gherkin, renumbering current section 8 to 9), and (2) add two TDD items to the Global DoD in `_TEMPLATE-EPIC.md`. Create a new content-validation test file to assert these templates contain the required TDD content. No pipeline, assembler, golden file, or routing changes are needed -- these are static reference documents.

---

## 1. Affected Files

| File | Type | Impact |
|------|------|--------|
| `resources/templates/_TEMPLATE-STORY.md` | Static template | **Modify**: Add new section 8 (TDD Scenarios), renumber old section 8 to 9 |
| `resources/templates/_TEMPLATE-EPIC.md` | Static template | **Modify**: Add 2 TDD items to Global DoD (section 3) |
| `tests/node/content/template-tdd-content.test.ts` | Test | **New file**: Content-validation tests for TDD sections |

**Total: 2 modified files + 1 new file = 3 files**

---

## 2. Changes to `_TEMPLATE-STORY.md`

### Current Structure (8 sections, 115 lines)

```
1. Dependencias
2. Regras Transversais Aplicaveis
3. Descricao
4. Definicoes de Qualidade Locais
5. Contratos de Dados (Data Contract)
6. Diagramas
7. Criterios de Aceite (Gherkin)
8. Sub-tarefas
```

### Target Structure (9 sections)

```
1. Dependencias                          (unchanged)
2. Regras Transversais Aplicaveis        (unchanged)
3. Descricao                             (unchanged)
4. Definicoes de Qualidade Locais        (unchanged)
5. Contratos de Dados (Data Contract)    (unchanged)
6. Diagramas                             (unchanged)
7. Criterios de Aceite (Gherkin)         (unchanged)
8. TDD Scenarios                         (NEW)
9. Sub-tarefas                           (renumbered from 8)
```

### New Section 8 Content

Insert after the closing ``` of the Gherkin block (line 106) and before the current `## 8. Sub-tarefas`:

```markdown
## 8. TDD Scenarios

### Scenario Ordering (TPP — Transformation Priority Premise)

> Order scenarios from simplest transformation to most complex. Follow Kent Beck's TPP:
> `{} -> nil -> constant -> constant+ -> scalar -> scalar+ -> collection -> collection+ -> recursion`
>
> Each new scenario should require exactly ONE transformation to pass.

### Mandatory Scenario Categories

- [ ] **Degenerate cases**: null input, empty collections, zero-value, missing fields
- [ ] **Happy path**: standard successful flow with valid data
- [ ] **Error paths**: invalid input, business rule violations, infrastructure failures
- [ ] **Boundary values**: min/max limits, off-by-one, type limits, empty-to-one transitions

### TDD Implementation Notes (Double-Loop TDD)

> **Outer loop (Acceptance):** Write one failing acceptance test from a Gherkin scenario above.
> **Inner loop (Unit):** Implement using Red-Green-Refactor micro-cycles, following TPP ordering.
>
> - Each commit must be test-first: test written BEFORE production code.
> - Refactoring happens ONLY when all tests are GREEN.
> - Every scenario in section 7 must have a corresponding acceptance test.
```

### Renumbering

Change `## 8. Sub-tarefas` to `## 9. Sub-tarefas`.

### Final Line Count

Current: 115 lines. After addition: ~135 lines (approximately +20 lines for the new section).

---

## 3. Changes to `_TEMPLATE-EPIC.md`

### Current Global DoD (section 3, lines 27-34)

```markdown
### Global Definition of Done (DoD)

- **Cobertura:** <Meta de cobertura -- ex: >= 95% Line, >= 90% Branch>
- **Testes Automatizados:** <Tipos de testes exigidos e cenarios obrigatorios>
- **Relatorio de Cobertura:** <Formato e granularidade esperada>
- **Documentacao:** <Artefatos de documentacao que devem estar atualizados>
- **Persistencia:** <Criterio de integridade de dados, se aplicavel>
- **Performance:** <SLO de latencia/throughput>
```

### Target Global DoD

Append two new items after the existing `- **Performance:**` line:

```markdown
- **TDD Compliance:** Commits follow test-first discipline; refactoring occurs only after green; scenario ordering follows TPP (Transformation Priority Premise)
- **Double-Loop TDD:** Acceptance tests derived from Gherkin scenarios (outer loop); unit tests from TPP-ordered scenarios (inner loop)
```

### Final Line Count

Current: 52 lines. After addition: 54 lines (+2 lines).

---

## 4. New Test File: `tests/node/content/template-tdd-content.test.ts`

### Rationale

These templates are NOT processed by the Nunjucks engine and are NOT part of the pipeline output. Therefore:
- `codex-templates.test.ts` (Nunjucks rendering) -- NOT applicable.
- `byte-for-byte.test.ts` (golden file comparison) -- NOT applicable.
- A **content-validation test** (like `refactoring-guidelines-content.test.ts`) is the correct pattern.

### Test Structure

The test reads the raw file content via `fs.readFileSync` and asserts the presence of required sections and keywords. Follows the exact pattern from `tests/node/content/refactoring-guidelines-content.test.ts`.

```typescript
// tests/node/content/template-tdd-content.test.ts
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const TEMPLATES_DIR = path.resolve(__dirname, "../../..", "resources/templates");

const storyContent = fs.readFileSync(
  path.join(TEMPLATES_DIR, "_TEMPLATE-STORY.md"),
  "utf-8",
);
const epicContent = fs.readFileSync(
  path.join(TEMPLATES_DIR, "_TEMPLATE-EPIC.md"),
  "utf-8",
);
```

### Test Cases for `_TEMPLATE-STORY.md`

| Test Name | Assertion |
|-----------|-----------|
| `storyTemplate_containsTddScenariosSection` | Contains `## 8. TDD Scenarios` |
| `storyTemplate_containsScenarioOrderingSubsection` | Contains `### Scenario Ordering (TPP` |
| `storyTemplate_containsTransformationPriorityPremise` | Contains `Transformation Priority Premise` |
| `storyTemplate_containsTppSequence` | Contains `nil -> constant -> constant+` |
| `storyTemplate_containsMandatoryScenarioCategoriesSubsection` | Contains `### Mandatory Scenario Categories` |
| `storyTemplate_containsDegenerateCasesCategory` | Contains `Degenerate cases` |
| `storyTemplate_containsHappyPathCategory` | Contains `Happy path` |
| `storyTemplate_containsErrorPathsCategory` | Contains `Error paths` |
| `storyTemplate_containsBoundaryValuesCategory` | Contains `Boundary values` |
| `storyTemplate_containsTddImplementationNotesSubsection` | Contains `### TDD Implementation Notes (Double-Loop TDD)` |
| `storyTemplate_containsOuterLoopReference` | Contains `Outer loop (Acceptance)` |
| `storyTemplate_containsInnerLoopReference` | Contains `Inner loop (Unit)` |
| `storyTemplate_containsTestFirstCommitRule` | Contains `test-first` |
| `storyTemplate_containsRefactoringOnlyOnGreen` | Contains `Refactoring happens ONLY when all tests are GREEN` |
| `storyTemplate_preservesGherkinSection` | Contains `## 7. Criterios de Aceite (Gherkin)` |
| `storyTemplate_subtasksRenumberedToNine` | Contains `## 9. Sub-tarefas` |
| `storyTemplate_preservesAllOriginalSections` | Contains sections 1-7 headings (parameterized) |
| `storyTemplate_tddSectionAfterGherkin` | Index of `## 8. TDD` > index of `## 7. Criterios` |
| `storyTemplate_tddSectionBeforeSubtasks` | Index of `## 8. TDD` < index of `## 9. Sub-tarefas` |

### Test Cases for `_TEMPLATE-EPIC.md`

| Test Name | Assertion |
|-----------|-----------|
| `epicTemplate_containsTddComplianceDoD` | Contains `TDD Compliance` |
| `epicTemplate_containsTestFirstInTddCompliance` | Contains `test-first` |
| `epicTemplate_containsTppInTddCompliance` | Contains `TPP` |
| `epicTemplate_containsDoubleLoopTddDoD` | Contains `Double-Loop TDD` |
| `epicTemplate_containsAcceptanceTestReference` | Contains `Acceptance tests` |
| `epicTemplate_containsGherkinReference` | Contains `Gherkin` |
| `epicTemplate_preservesAllOriginalSections` | Contains sections 1-5 headings (parameterized) |
| `epicTemplate_preservesOriginalDodItems` | Contains `Cobertura`, `Testes Automatizados`, `Performance` |
| `epicTemplate_tddItemsInsideDodSection` | TDD items appear between `### Global Definition of Done (DoD)` and `## 4.` |

### Estimated Test Count

**28 test cases** across 4 describe blocks.

---

## 5. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Backward compatibility: AI skills depend on section numbering | **Medium** | **Medium** | Verify `x-story-create` and `x-story-epic` skills reference section headings by name, not number. If they use regex on `## 8.`, the renumbering could break them. Mitigated by searching all skills for numeric section references. |
| Template language mismatch (Portuguese vs English) | **Low** | **Minor** | Existing template uses Portuguese section headings. New TDD section uses English (per project RULE-012 for generated content). This is consistent with the template being a reference scaffold, not generated output. The TDD content describes methodology in English; existing structure headings remain in Portuguese. |
| No pipeline impact (no golden file changes) | **None** | **None** | These files are static references, not processed by the Nunjucks engine. No assembler, pipeline, or golden file changes required. Verified by checking that no `*.njk` template or assembler references `_TEMPLATE-STORY.md` or `_TEMPLATE-EPIC.md`. |
| Test coverage impact | **None** | **None** | The new test file tests static file content (not source code branches). Source code coverage is unaffected since no `.ts` source files are modified. |
| Section ordering assumption | **Low** | **Low** | The plan places TDD Scenarios (section 8) between Gherkin (section 7) and Sub-tasks (old section 8, now 9). This is the natural location: TDD scenarios are derived from Gherkin acceptance criteria and inform the sub-task breakdown. |

### Pre-Implementation Verification

Before modifying the templates, run:

```bash
# Verify no skill or agent references section numbers by digit
grep -r "## 8\." .claude/skills/ .claude/agents/ resources/
```

If any matches reference `## 8. Sub-tarefas` by number, those references must be updated to `## 9.`.

---

## 6. Dependency Direction Validation

```
resources/templates/_TEMPLATE-STORY.md   (static markdown, no code dependency)
resources/templates/_TEMPLATE-EPIC.md    (static markdown, no code dependency)
         |
         v
.claude/skills/x-story-create/          (reads template at runtime via AI skill)
.claude/skills/x-story-epic/            (reads template at runtime via AI skill)
         |
         v
tests/node/content/template-tdd-content.test.ts  (reads file, asserts content)
```

No inward dependency violations. Templates are leaf nodes -- nothing depends on them at compile time. Skills reference them as runtime context. Tests validate content only.

---

## 7. Database / API / Event / Configuration Changes

None. This story modifies only static markdown templates and adds a content-validation test.

---

## 8. Implementation Order

1. **Search for section-number references**: Verify no skill/agent hardcodes `## 8.` for Sub-tarefas
2. **Modify `_TEMPLATE-STORY.md`**: Add section 8 (TDD Scenarios), renumber old section 8 to 9
3. **Modify `_TEMPLATE-EPIC.md`**: Append TDD Compliance and Double-Loop TDD items to Global DoD
4. **Create test file**: `tests/node/content/template-tdd-content.test.ts`
5. **Run new tests**: `npx vitest run tests/node/content/template-tdd-content.test.ts`
6. **Run full test suite**: `npx vitest run` to confirm no regressions
7. **Verify coverage**: Ensure >= 95% line, >= 90% branch coverage is maintained
