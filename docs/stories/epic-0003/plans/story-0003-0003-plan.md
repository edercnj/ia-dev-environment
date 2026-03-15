# Implementation Plan: story-0003-0003

**Story:** Rules 03 & 05 -- TDD Practices and TDD Compliance
**Author:** Senior Architect
**Date:** 2026-03-15

---

## 1. Affected Layers and Components

This story modifies **static content resources only** -- no TypeScript source code changes are needed. The pipeline assembly logic (`RulesAssembler.copyCoreRules`, `GithubInstructionsAssembler.generateContextual`, `CodexAgentsMdAssembler`) already copies/renders these files with placeholder replacement. Adding new sections to the source files propagates automatically through the existing pipeline.

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources | `resources/core-rules/03-coding-standards.md` | Add `## TDD Practices` section |
| Resources | `resources/core-rules/05-quality-gates.md` | Add 5 TDD items to merge checklist + `## TDD Compliance` section |
| Resources | `resources/codex-templates/sections/coding-standards.md.njk` | Add `### TDD Practices` subsection |
| Resources | `resources/codex-templates/sections/quality-gates.md.njk` | Add TDD checklist items + `### TDD Compliance` subsection |
| Resources | `resources/github-instructions-templates/coding-standards.md` | Add `## TDD Practices` section |
| Resources | `resources/github-instructions-templates/quality-gates.md` | Add TDD checklist items + `## TDD Compliance` section |
| Tests | 16 golden files for `.claude/rules/` (8 profiles x 2 rules) | Update to match new content |
| Tests | 8 golden files for `.github/instructions/` coding-standards | Update to match new content |
| Tests | 8 golden files for `.github/instructions/` quality-gates | Update to match new content |
| Tests | 8 golden files for `AGENTS.md` (Codex) | Update Coding Standards and Quality Gates sections |
| Source | None | No pipeline code changes needed |

---

## 2. New Content to Add

### 2.1 Rule 03 -- TDD Practices Section

Append after the `## Forbidden` section, before `## Language-Specific Conventions`:

```markdown
## TDD Practices

- **Red-Green-Refactor** is mandatory for all production code
  1. **Red**: Write a failing test that defines the expected behavior
  2. **Green**: Write the minimum code to make the test pass
  3. **Refactor**: Improve design without changing behavior
- Refactoring criteria: extract method when > 25 lines, eliminate duplication, improve naming
- Refactoring NEVER adds behavior -- if behavior changes, write a new failing test first
- Test-first commits: test must appear in git history before or in the same commit as its implementation

> **Full TDD reference:** Read `skills/testing/SKILL.md` for Double-Loop TDD, Transformation Priority Premise, and advanced TDD patterns.
```

### 2.2 Rule 05 -- Merge Checklist TDD Items

Append 5 items to the existing `## Merge Checklist` section, after the existing items:

```markdown
- [ ] Commits show test-first pattern (test precedes implementation in git log)
- [ ] Explicit refactoring after green
- [ ] Tests are incremental (simple to complex via TPP)
- [ ] No test written AFTER implementation
- [ ] Acceptance tests exist and validate end-to-end behavior
```

### 2.3 Rule 05 -- TDD Compliance Section

Append after `## Forbidden` section (end of file):

```markdown
## TDD Compliance

- **Double-Loop TDD**: Outer loop (acceptance test, failing) drives inner loop (unit tests, Red-Green-Refactor)
- **Transformation Priority Premise (TPP)**: Order tests from simple to complex -- `{} -> nil -> constant -> constant+ -> scalar -> collection -> ...`
- **Atomic TDD commits**: Each Red-Green-Refactor cycle produces one or more atomic commits with Conventional Commits format
- Coverage thresholds (see above) are NOT a substitute for TDD -- high coverage with test-after is insufficient
```

### 2.4 Codex Template Sections (Nunjucks)

For `coding-standards.md.njk`, append before `### Language-Specific`:

```markdown
### TDD Practices

- **Red-Green-Refactor** is mandatory for all production code
- Refactoring criteria: extract method when > 25 lines, eliminate duplication, improve naming
- Refactoring NEVER adds behavior
- Full TDD reference: `skills/testing/SKILL.md`
```

For `quality-gates.md.njk`, append TDD items to `### Merge Checklist` and add:

```markdown
### TDD Compliance

- **Double-Loop TDD**: Outer loop (acceptance test) drives inner loop (unit tests)
- **TPP**: Order tests from simple to complex
- **Atomic TDD commits**: Each cycle produces atomic commits
```

### 2.5 GitHub Instructions Templates

Mirror the same content as core-rules, adapted to GitHub instructions format (H2 headers, `{placeholder}` style instead of `{{placeholder}}`).

---

## 3. Existing Content to Preserve (Backward Compatibility -- RULE-003)

### Rule 03 Sections That Must Remain Unchanged

| Section | Status |
|---------|--------|
| `## Hard Limits` (table: 25 lines, 250 lines, 4 params, 120 chars) | PRESERVE |
| `## Naming` (4 bullets) | PRESERVE |
| `## SOLID (one-liners)` (5 bullets) | PRESERVE |
| `## Error Handling` (4 bullets) | PRESERVE |
| `## Forbidden` (6 bullets) | PRESERVE |
| `## Language-Specific Conventions` (reference block) | PRESERVE |

### Rule 05 Sections That Must Remain Unchanged

| Section | Status |
|---------|--------|
| `## Coverage Thresholds` (95% line, 90% branch) | PRESERVE -- NOT duplicated in TDD section |
| `## Test Categories` (7 items) | PRESERVE |
| `## Test Naming` (pattern) | PRESERVE |
| `## Merge Checklist` (existing 6 items) | PRESERVE -- new items appended, existing items unchanged |
| `## Forbidden` (5 items) | PRESERVE |

### Critical Invariant

Coverage thresholds (95%/90%) MUST NOT be duplicated in the `## TDD Compliance` section. The TDD section references them with "see above" to avoid drift.

---

## 4. Files Requiring Modification (Complete List)

### 4.1 Source of Truth (2 files)

| # | File | Change |
|---|------|--------|
| 1 | `resources/core-rules/03-coding-standards.md` | Add `## TDD Practices` before `## Language-Specific Conventions` |
| 2 | `resources/core-rules/05-quality-gates.md` | Add 5 TDD items to merge checklist + `## TDD Compliance` at end |

### 4.2 Codex Templates (2 files)

| # | File | Change |
|---|------|--------|
| 3 | `resources/codex-templates/sections/coding-standards.md.njk` | Add `### TDD Practices` before `### Language-Specific` |
| 4 | `resources/codex-templates/sections/quality-gates.md.njk` | Add TDD checklist items + `### TDD Compliance` at end |

### 4.3 GitHub Instructions Templates (2 files)

| # | File | Change |
|---|------|--------|
| 5 | `resources/github-instructions-templates/coding-standards.md` | Add `## TDD Practices` at end |
| 6 | `resources/github-instructions-templates/quality-gates.md` | Add TDD checklist items + `## TDD Compliance` at end |

### 4.4 Golden Files -- `.claude/rules/` (16 files)

| # | File |
|---|------|
| 7 | `tests/golden/go-gin/.claude/rules/03-coding-standards.md` |
| 8 | `tests/golden/go-gin/.claude/rules/05-quality-gates.md` |
| 9 | `tests/golden/java-quarkus/.claude/rules/03-coding-standards.md` |
| 10 | `tests/golden/java-quarkus/.claude/rules/05-quality-gates.md` |
| 11 | `tests/golden/java-spring/.claude/rules/03-coding-standards.md` |
| 12 | `tests/golden/java-spring/.claude/rules/05-quality-gates.md` |
| 13 | `tests/golden/kotlin-ktor/.claude/rules/03-coding-standards.md` |
| 14 | `tests/golden/kotlin-ktor/.claude/rules/05-quality-gates.md` |
| 15 | `tests/golden/python-click-cli/.claude/rules/03-coding-standards.md` |
| 16 | `tests/golden/python-click-cli/.claude/rules/05-quality-gates.md` |
| 17 | `tests/golden/python-fastapi/.claude/rules/03-coding-standards.md` |
| 18 | `tests/golden/python-fastapi/.claude/rules/05-quality-gates.md` |
| 19 | `tests/golden/rust-axum/.claude/rules/03-coding-standards.md` |
| 20 | `tests/golden/rust-axum/.claude/rules/05-quality-gates.md` |
| 21 | `tests/golden/typescript-nestjs/.claude/rules/03-coding-standards.md` |
| 22 | `tests/golden/typescript-nestjs/.claude/rules/05-quality-gates.md` |

### 4.5 Golden Files -- `.github/instructions/` (16 files)

| # | File |
|---|------|
| 23 | `tests/golden/go-gin/.github/instructions/coding-standards.instructions.md` |
| 24 | `tests/golden/go-gin/.github/instructions/quality-gates.instructions.md` |
| 25 | `tests/golden/java-quarkus/.github/instructions/coding-standards.instructions.md` |
| 26 | `tests/golden/java-quarkus/.github/instructions/quality-gates.instructions.md` |
| 27 | `tests/golden/java-spring/.github/instructions/coding-standards.instructions.md` |
| 28 | `tests/golden/java-spring/.github/instructions/quality-gates.instructions.md` |
| 29 | `tests/golden/kotlin-ktor/.github/instructions/coding-standards.instructions.md` |
| 30 | `tests/golden/kotlin-ktor/.github/instructions/quality-gates.instructions.md` |
| 31 | `tests/golden/python-click-cli/.github/instructions/coding-standards.instructions.md` |
| 32 | `tests/golden/python-click-cli/.github/instructions/quality-gates.instructions.md` |
| 33 | `tests/golden/python-fastapi/.github/instructions/coding-standards.instructions.md` |
| 34 | `tests/golden/python-fastapi/.github/instructions/quality-gates.instructions.md` |
| 35 | `tests/golden/rust-axum/.github/instructions/coding-standards.instructions.md` |
| 36 | `tests/golden/rust-axum/.github/instructions/quality-gates.instructions.md` |
| 37 | `tests/golden/typescript-nestjs/.github/instructions/coding-standards.instructions.md` |
| 38 | `tests/golden/typescript-nestjs/.github/instructions/quality-gates.instructions.md` |

### 4.6 Golden Files -- `AGENTS.md` (Codex) (8 files)

| # | File |
|---|------|
| 39 | `tests/golden/go-gin/AGENTS.md` |
| 40 | `tests/golden/java-quarkus/AGENTS.md` |
| 41 | `tests/golden/java-spring/AGENTS.md` |
| 42 | `tests/golden/kotlin-ktor/AGENTS.md` |
| 43 | `tests/golden/python-click-cli/AGENTS.md` |
| 44 | `tests/golden/python-fastapi/AGENTS.md` |
| 45 | `tests/golden/rust-axum/AGENTS.md` |
| 46 | `tests/golden/typescript-nestjs/AGENTS.md` |

**Total files to modify: 46**

---

## 5. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Coverage threshold duplication | Medium | TDD Compliance section references "see above" instead of repeating values |
| Golden file drift | Low | All 8 profiles share identical core-rules content (verified); batch update is safe |
| Codex template heading level mismatch | Low | Codex uses H3 (subsections of H2 "Coding Standards" / "Quality Gates"); core-rules use H2 |
| Nunjucks variable in quality-gates template | Low | TDD section has no `{{ }}` variables; only existing sections use `{{ coverage_line }}` / `{{ coverage_branch }}` |
| Merge checklist ordering | Low | New TDD items are appended after existing items, preserving backward compatibility |
| GitHub instructions template uses `{placeholder}` (Python-style) not `{{placeholder}}` (Nunjucks) | Medium | Content added to GitHub instructions templates must NOT contain `{{ }}` -- only `{placeholder}` for coverage values. TDD sections have no placeholders, so no risk |
| Byte-for-byte test failures | Expected | Golden files MUST be updated in the same commit as source changes; this is the validation mechanism |
| AGENTS.md golden file structure | Medium | The Coding Standards and Quality Gates sections inside AGENTS.md are rendered from Nunjucks templates; updating the `.njk` files will change AGENTS.md output, requiring all 8 AGENTS.md golden files to be updated |

---

## 6. Implementation Order

Following RULE-005 (Red-Green-Refactor) and RULE-008 (Atomic TDD Commits):

### Phase 1: RED -- Write Failing Tests (update golden files first)

**Step 1.1:** Update all 16 `.claude/rules/` golden files (8 profiles x 2 rules) with the expected new content. Run `npm test` -- byte-for-byte tests will FAIL because source-of-truth files have not changed yet.

**Step 1.2:** Update all 16 `.github/instructions/` golden files (8 profiles x 2 rules) with expected new content.

**Step 1.3:** Update all 8 `AGENTS.md` golden files with expected new Coding Standards and Quality Gates sections.

**Commit:** `test: update golden files with expected TDD sections for rules 03 and 05`

### Phase 2: GREEN -- Update Source-of-Truth Files

**Step 2.1:** Edit `resources/core-rules/03-coding-standards.md` -- add `## TDD Practices` section before `## Language-Specific Conventions`.

**Step 2.2:** Edit `resources/core-rules/05-quality-gates.md` -- add 5 TDD items to `## Merge Checklist` and add `## TDD Compliance` section at end.

**Step 2.3:** Edit `resources/codex-templates/sections/coding-standards.md.njk` -- add `### TDD Practices` before `### Language-Specific`.

**Step 2.4:** Edit `resources/codex-templates/sections/quality-gates.md.njk` -- add TDD checklist items to `### Merge Checklist` and add `### TDD Compliance` at end.

**Step 2.5:** Edit `resources/github-instructions-templates/coding-standards.md` -- add `## TDD Practices` at end.

**Step 2.6:** Edit `resources/github-instructions-templates/quality-gates.md` -- add TDD checklist items and `## TDD Compliance` at end.

**Step 2.7:** Run `npm test` -- all byte-for-byte tests should now PASS.

**Commit:** `feat: add TDD Practices to Rule 03 and TDD Compliance to Rule 05`

### Phase 3: REFACTOR -- Verify and Clean Up

**Step 3.1:** Run full test suite with coverage: `npm run test:coverage`

**Step 3.2:** Verify coverage thresholds still meet 95% line / 90% branch.

**Step 3.3:** Review all 46 files for consistency across the three output channels (core-rules, Nunjucks, GitHub instructions).

**Commit (if needed):** `refactor: normalize TDD section wording across output channels`

---

## Appendix A: Content Placement Reference

### Rule 03 Section Order (after changes)

```
# Rule 03 -- Coding Standards (Quick Reference)
> Full reference...
## Hard Limits           (existing, unchanged)
## Naming                (existing, unchanged)
## SOLID (one-liners)    (existing, unchanged)
## Error Handling        (existing, unchanged)
## Forbidden             (existing, unchanged)
## TDD Practices         <<< NEW
## Language-Specific Conventions (existing, unchanged)
```

### Rule 05 Section Order (after changes)

```
# Rule 05 -- Quality Gates
> Full reference...
## Coverage Thresholds   (existing, unchanged)
## Test Categories       (existing, unchanged)
## Test Naming           (existing, unchanged)
## Merge Checklist       (existing items unchanged; 5 new items appended)
## Forbidden             (existing, unchanged)
## TDD Compliance        <<< NEW
```

### Codex Template Section Order

Mirrors the core-rules but with one heading level down (H3 instead of H2), since they are subsections within an H2 container.

### GitHub Instructions Section Order

Mirrors the core-rules exactly (H2 headers), with `{placeholder}` syntax for coverage values.

---

## Appendix B: Verification Checklist

- [ ] `resources/core-rules/03-coding-standards.md` contains `## TDD Practices`
- [ ] `resources/core-rules/05-quality-gates.md` contains 5 new TDD checklist items
- [ ] `resources/core-rules/05-quality-gates.md` contains `## TDD Compliance`
- [ ] Coverage thresholds (95%/90%) NOT duplicated in TDD Compliance section
- [ ] All existing sections in both rules remain byte-for-byte identical
- [ ] `resources/codex-templates/sections/coding-standards.md.njk` contains `### TDD Practices`
- [ ] `resources/codex-templates/sections/quality-gates.md.njk` contains TDD items + `### TDD Compliance`
- [ ] `resources/github-instructions-templates/coding-standards.md` contains `## TDD Practices`
- [ ] `resources/github-instructions-templates/quality-gates.md` contains TDD items + `## TDD Compliance`
- [ ] All 16 `.claude/rules/` golden files updated
- [ ] All 16 `.github/instructions/` golden files updated
- [ ] All 8 `AGENTS.md` golden files updated
- [ ] `npm test` passes (all 1,384+ tests)
- [ ] Coverage >= 95% line, >= 90% branch
- [ ] No compiler/linter warnings
