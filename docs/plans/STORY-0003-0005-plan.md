# Implementation Plan: STORY-0003-0005 — TDD Sections in _TEMPLATE-STORY.md and _TEMPLATE-EPIC.md

## Summary

Add TDD-specific sections to two static markdown template files used by AI skills (`x-story-create`, `x-story-epic`) for story and epic generation. Specifically: (1) add TDD-focused subsections (7.1, 7.2, 7.3) under section 7 "Criterios de Aceite (Gherkin)" in `_TEMPLATE-STORY.md` (keeping the existing section 8 "Sub-tarefas" and its numbering unchanged), and (2) add two TDD items to the Global DoD in `_TEMPLATE-EPIC.md`. Create a new content-validation test file to assert these templates contain the required TDD content. No pipeline, assembler, golden file, or routing changes are needed -- these are static reference documents.

---

## 1. Affected Files

| File | Type | Impact |
|------|------|--------|
| `resources/templates/_TEMPLATE-STORY.md` | Static template | **Modify**: Add TDD subsections 7.1-7.3 under section 7, keep section 8 "Sub-tarefas" unchanged |
| `resources/templates/_TEMPLATE-EPIC.md` | Static template | **Modify**: Add 2 TDD items to Global DoD (section 3) |
| `tests/node/content/template-tdd-sections.test.ts` | Test | **New file**: Content-validation tests for TDD sections |

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

### Target Structure (8 sections, with TDD subsections under 7)

```
1. Dependencias                          (unchanged)
2. Regras Transversais Aplicaveis        (unchanged)
3. Descricao                             (unchanged)
4. Definicoes de Qualidade Locais        (unchanged)
5. Contratos de Dados (Data Contract)    (unchanged)
6. Diagramas                             (unchanged)
7. Criterios de Aceite (Gherkin)         (now includes subsections 7.1-7.3 for TDD content)
8. Sub-tarefas                           (unchanged)
```

### New TDD Subsections under Section 7

Insert new TDD-specific subsections 7.1-7.3 within section 7 "Criterios de Aceite (Gherkin)", after the closing ``` of the Gherkin block (line 106) and before `## 8. Sub-tarefas`, keeping the subsequent section 8 header and its numbering unchanged:

```markdown
### 7.1 Scenario Ordering (TPP)

> Scenarios MUST follow the Transformation Priority Premise (TPP) order, from simplest to most
> complex: degenerate -> unconditional -> conditions -> iterations -> edge cases.
> This ordering ensures incremental complexity in both tests and implementation.

### 7.2 Mandatory Scenario Categories

Every story MUST include scenarios covering all of the following categories:

- [ ] Degenerate cases (null, empty, zero)
- [ ] Happy path (basic success)
- [ ] Error paths (each error type)
- [ ] Boundary values (at-min, at-max, past-max)

### 7.3 TDD Implementation Notes

- **Double-Loop TDD**: The primeiro cenario Gherkin becomes the acceptance test (outer loop).
  Subsequent scenarios guide unit tests (inner loop).
- The first scenario defines the walking skeleton -- the simplest end-to-end path.
- Unit tests are driven by TPP: start with the simplest transformation and progress to more
  complex ones.
```

### Final Line Count

Current: 115 lines. After addition: ~138 lines (approximately +23 lines for the new subsections).

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
- **TDD Compliance:** Commits show test-first pattern. Explicit refactoring after green. Tests are incremental (from simple to complex via TPP -- Transformation Priority Premise).
- **Double-Loop TDD:** Acceptance tests derived from Gherkin scenarios (outer loop). Unit tests guided by TPP (inner loop).
```

### Final Line Count

Current: 52 lines. After addition: 54 lines (+2 lines).

---

## 4. New Test File: `tests/node/content/template-tdd-sections.test.ts`

### Rationale

These templates are NOT processed by the Nunjucks engine and are NOT part of the pipeline output. Therefore:
- `codex-templates.test.ts` (Nunjucks rendering) -- NOT applicable.
- `byte-for-byte.test.ts` (golden file comparison) -- NOT applicable.
- A **content-validation test** (like `refactoring-guidelines-content.test.ts`) is the correct pattern.

### Test Structure

The test reads the raw file content via `fs.readFileSync` and asserts the presence of required sections and keywords. Follows the exact pattern from `tests/node/content/refactoring-guidelines-content.test.ts`.

### Test Groups (41 tests across 10 describe blocks)

| Group | Description | Count |
|-------|-------------|-------|
| Story — TDD mandatory scenario categories | Checks 4 categories + checkbox syntax + boundary specifics | 6 |
| Story — TPP ordering note | TPP reference + ordering guidance | 2 |
| Story — TDD Implementation Notes | Double-Loop + acceptance test + unit test references | 3 |
| Story — TDD section structure | Heading exists + after Gherkin + before Sub-tarefas | 3 |
| Epic — TDD Compliance in DoD | Compliance item + test-first + refactoring + TPP | 4 |
| Epic — Double-Loop TDD in DoD | Double-Loop item + acceptance + unit tests | 3 |
| Story — backward compatibility | All 8 original sections + section count | 9 |
| Epic — backward compatibility | All 5 original sections + section count | 6 |
| Structure validation | Valid markdown headings for both templates | 2 |
| Empty TDD section validity | Parseable markdown + unchecked defaults + placeholder text | 3 |
| **Total** | | **41** |

---

## 5. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Backward compatibility: AI skills depend on section numbering | **None** | **None** | Subsection approach (7.1-7.3) preserves all original section numbers. Section 8 "Sub-tarefas" is unchanged. No renumbering required. |
| Template language mismatch (Portuguese vs English) | **Low** | **Minor** | Existing template uses Portuguese section headings. New TDD subsections use English (per project RULE-012 for generated content). This is consistent with the template being a reference scaffold. |
| No pipeline impact (no golden file changes) | **None** | **None** | These files are static references, not processed by the Nunjucks engine. No assembler, pipeline, or golden file changes required. |
| Test coverage impact | **None** | **None** | The new test file tests static file content (not source code branches). Source code coverage is unaffected since no `.ts` source files are modified. |

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
tests/node/content/template-tdd-sections.test.ts  (reads file, asserts content)
```

No inward dependency violations. Templates are leaf nodes -- nothing depends on them at compile time. Skills reference them as runtime context. Tests validate content only.

---

## 7. Database / API / Event / Configuration Changes

None. This story modifies only static markdown templates and adds a content-validation test.

---

## 8. Implementation Order

1. **Modify `_TEMPLATE-STORY.md`**: Add subsections 7.1, 7.2, 7.3 under section 7 (Gherkin)
2. **Modify `_TEMPLATE-EPIC.md`**: Append TDD Compliance and Double-Loop TDD items to Global DoD
3. **Create test file**: `tests/node/content/template-tdd-sections.test.ts`
4. **Run new tests**: `npx vitest run tests/node/content/template-tdd-sections.test.ts`
5. **Run full test suite**: `npx vitest run` to confirm no regressions
6. **Verify coverage**: Ensure >= 95% line, >= 90% branch coverage is maintained
