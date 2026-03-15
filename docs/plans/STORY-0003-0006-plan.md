# Implementation Plan: STORY-0003-0006 — Agents TDD Workflows for Developer, QA and Tech Lead

## 1. Summary

Add TDD workflow sections to 3 agent templates (typescript-developer, qa-engineer, tech-lead)
across both template directories (`.claude` format and `.github` format), update generated
`.claude/agents/` copies, and update all golden test files for byte-for-byte parity.

---

## 2. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources | `resources/agents-templates/` | Source of truth for `.claude/agents/` format |
| Resources | `resources/github-agents-templates/` | Source of truth for `.github/agents/` format |
| Generated | `.claude/agents/` | Project's own agents (copy of agents-templates) |
| Tests | `tests/golden/*/` | Golden files for byte-for-byte integration tests |
| Pipeline | `src/assembler/agents-assembler.ts` | No code changes needed (template copy logic unchanged) |
| Pipeline | `src/assembler/github-agents-assembler.ts` | No code changes needed (template copy logic unchanged) |

**No source code changes** are required in the assembler pipeline. The pipeline performs template
copy + placeholder replacement. Since TDD content is static text (no new placeholders), only
template files and their golden copies need updating.

---

## 3. Files to Modify

### 3.1 Source Templates — agents-templates/ (Claude format, 3 files)

| # | File | Change |
|---|------|--------|
| 1 | `resources/agents-templates/developers/typescript-developer.md` | Add TDD Workflow section, reorder responsibilities |
| 2 | `resources/agents-templates/core/qa-engineer.md` | Add TDD Compliance category (4 items) to 24-point checklist |
| 3 | `resources/agents-templates/core/tech-lead.md` | Add TDD Process category (5 items) to 40-point checklist |

### 3.2 Source Templates — github-agents-templates/ (GitHub format, 3 files)

| # | File | Change |
|---|------|--------|
| 4 | `resources/github-agents-templates/developers/typescript-developer.md` | Add TDD Workflow section, reorder responsibilities |
| 5 | `resources/github-agents-templates/core/qa-engineer.md` | Add TDD Compliance category (4 items) to checklist |
| 6 | `resources/github-agents-templates/core/tech-lead.md` | Add TDD Process category (5 items) to checklist |

### 3.3 Generated Agents — .claude/agents/ (3 files)

These are identical copies of `resources/agents-templates/`. They must be updated to match.

| # | File | Change |
|---|------|--------|
| 7 | `.claude/agents/typescript-developer.md` | Mirror changes from file #1 |
| 8 | `.claude/agents/qa-engineer.md` | Mirror changes from file #2 |
| 9 | `.claude/agents/tech-lead.md` | Mirror changes from file #3 |

### 3.4 Golden Files — .claude/agents/ format (qa-engineer: 8, tech-lead: 8, typescript-developer: 1)

**qa-engineer.md** — 8 profiles (core agent, identical across all profiles):

| # | File |
|---|------|
| 10 | `tests/golden/go-gin/.claude/agents/qa-engineer.md` |
| 11 | `tests/golden/java-quarkus/.claude/agents/qa-engineer.md` |
| 12 | `tests/golden/java-spring/.claude/agents/qa-engineer.md` |
| 13 | `tests/golden/kotlin-ktor/.claude/agents/qa-engineer.md` |
| 14 | `tests/golden/python-click-cli/.claude/agents/qa-engineer.md` |
| 15 | `tests/golden/python-fastapi/.claude/agents/qa-engineer.md` |
| 16 | `tests/golden/rust-axum/.claude/agents/qa-engineer.md` |
| 17 | `tests/golden/typescript-nestjs/.claude/agents/qa-engineer.md` |

**tech-lead.md** — 8 profiles (core agent, identical across all profiles):

| # | File |
|---|------|
| 18 | `tests/golden/go-gin/.claude/agents/tech-lead.md` |
| 19 | `tests/golden/java-quarkus/.claude/agents/tech-lead.md` |
| 20 | `tests/golden/java-spring/.claude/agents/tech-lead.md` |
| 21 | `tests/golden/kotlin-ktor/.claude/agents/tech-lead.md` |
| 22 | `tests/golden/python-click-cli/.claude/agents/tech-lead.md` |
| 23 | `tests/golden/python-fastapi/.claude/agents/tech-lead.md` |
| 24 | `tests/golden/rust-axum/.claude/agents/tech-lead.md` |
| 25 | `tests/golden/typescript-nestjs/.claude/agents/tech-lead.md` |

**typescript-developer.md** — 1 profile (developer agent, only for typescript-nestjs):

| # | File |
|---|------|
| 26 | `tests/golden/typescript-nestjs/.claude/agents/typescript-developer.md` |

### 3.5 Golden Files — .github/agents/ format (qa-engineer: 8, tech-lead: 8, typescript-developer: 1)

**qa-engineer.agent.md** — 8 profiles:

| # | File |
|---|------|
| 27 | `tests/golden/go-gin/.github/agents/qa-engineer.agent.md` |
| 28 | `tests/golden/java-quarkus/.github/agents/qa-engineer.agent.md` |
| 29 | `tests/golden/java-spring/.github/agents/qa-engineer.agent.md` |
| 30 | `tests/golden/kotlin-ktor/.github/agents/qa-engineer.agent.md` |
| 31 | `tests/golden/python-click-cli/.github/agents/qa-engineer.agent.md` |
| 32 | `tests/golden/python-fastapi/.github/agents/qa-engineer.agent.md` |
| 33 | `tests/golden/rust-axum/.github/agents/qa-engineer.agent.md` |
| 34 | `tests/golden/typescript-nestjs/.github/agents/qa-engineer.agent.md` |

**tech-lead.agent.md** — 8 profiles:

| # | File |
|---|------|
| 35 | `tests/golden/go-gin/.github/agents/tech-lead.agent.md` |
| 36 | `tests/golden/java-quarkus/.github/agents/tech-lead.agent.md` |
| 37 | `tests/golden/java-spring/.github/agents/tech-lead.agent.md` |
| 38 | `tests/golden/kotlin-ktor/.github/agents/tech-lead.agent.md` |
| 39 | `tests/golden/python-click-cli/.github/agents/tech-lead.agent.md` |
| 40 | `tests/golden/python-fastapi/.github/agents/tech-lead.agent.md` |
| 41 | `tests/golden/rust-axum/.github/agents/tech-lead.agent.md` |
| 42 | `tests/golden/typescript-nestjs/.github/agents/tech-lead.agent.md` |

**typescript-developer.agent.md** — 1 profile:

| # | File |
|---|------|
| 43 | `tests/golden/typescript-nestjs/.github/agents/typescript-developer.agent.md` |

### 3.6 File Count Summary

| Category | Files |
|----------|-------|
| Source templates (agents-templates/) | 3 |
| Source templates (github-agents-templates/) | 3 |
| Generated agents (.claude/agents/) | 3 |
| Golden files (.claude/agents/ format) | 17 |
| Golden files (.github/agents/ format) | 17 |
| **Total files to modify** | **43** |

---

## 4. Content Changes Per Agent

### 4.1 typescript-developer — TDD Workflow Section

**Claude format** (`resources/agents-templates/developers/typescript-developer.md`):

Add a new `## TDD Workflow` section after `## Responsibilities` and reorder responsibilities
to put test-writing before implementation.

#### Responsibilities Reorder

Change from:
```
1. Implement features following the architect's plan precisely
2. Write strictly typed code (no `any`, no `as` casts unless justified)
3. Follow {{FRAMEWORK}} conventions (decorators, modules, dependency injection)
4. Create comprehensive tests (unit, integration, e2e)
5. Write database migrations when schema changes are needed
6. Configure environment variables and validation schemas
7. Apply Clean Code principles adapted to TypeScript idioms
8. Ensure proper error handling with typed error classes
```

Change to (test-first order):
```
1. Write failing tests FIRST for each behavior (Red phase)
2. Implement the minimum code to make tests pass (Green phase)
3. Refactor while keeping tests green (Refactor phase)
4. Follow the architect's plan precisely
5. Write strictly typed code (no `any`, no `as` casts unless justified)
6. Follow {{FRAMEWORK}} conventions (decorators, modules, dependency injection)
7. Write database migrations when schema changes are needed
8. Configure environment variables and validation schemas
9. Apply Clean Code principles adapted to TypeScript idioms
10. Ensure proper error handling with typed error classes
```

#### New TDD Workflow Section

Insert after Responsibilities:
```markdown
## TDD Workflow

You ALWAYS follow the Red-Green-Refactor cycle for every behavior you implement:

1. **RED** — Write a failing test that defines the expected behavior
2. **GREEN** — Write the minimum production code to make the test pass
3. **REFACTOR** — Improve code structure while all tests remain green
4. **COMMIT** — Create an atomic commit after each complete cycle

### TDD Rules
- You ALWAYS write the test FIRST, then implement the minimum code to make it pass
- After each GREEN, you evaluate refactoring opportunities before moving to the next behavior
- You commit after each complete Red-Green-Refactor cycle
- Tests progress from simple to complex (Transformation Priority Premise)
- When implementing a feature with multiple behaviors, write one test at a time
```

**GitHub format** (`resources/github-agents-templates/developers/typescript-developer.md`):

Apply equivalent changes adapted to the condensed GitHub format:
- Reorder responsibilities list to test-first
- Add `## TDD Workflow` section with the same content (adapted for conciseness)

### 4.2 qa-engineer — TDD Compliance Category

**Claude format** (`resources/agents-templates/core/qa-engineer.md`):

Add a new `### TDD Compliance (25-28)` category after the existing `### Fixtures & Organization (21-24)`,
extending the checklist from 24 to 28 points:

```markdown
### TDD Compliance (25-28)
25. Commits show test-first pattern (test file modified before production code)
26. Explicit refactoring commits exist after green phase (no behavior changes in refactoring)
27. Tests are incremental — progression from simple to complex (Transformation Priority Premise)
28. Acceptance tests exist for end-to-end scenarios before unit tests (Double-Loop TDD)
```

Update the section heading from `## 24-Point QA Checklist` to `## 28-Point QA Checklist`.

**GitHub format** (`resources/github-agents-templates/core/qa-engineer.md`):

Add the condensed TDD Compliance summary line to the checklist categories:
```markdown
- **TDD Compliance (25-28):** Test-first commits, refactoring phases, incremental progression, acceptance tests
```

Update the heading from `## 24-Point QA Checklist` to `## 28-Point QA Checklist`.

### 4.3 tech-lead — TDD Process Category

**Claude format** (`resources/agents-templates/core/tech-lead.md`):

Add a new `### TDD Process (41-45)` category after the existing `### Operational Readiness (38-40)`,
extending the checklist from 40 to 45 points:

```markdown
### TDD Process (41-45)
41. Git history shows Red-Green-Refactor progression (test commit precedes implementation commit)
42. Double-Loop TDD: acceptance test precedes unit tests for each feature
43. Transformation Priority Premise ordering visible in test progression (simple to complex)
44. Refactoring phases do not add new behavior (tests unchanged during refactor commits)
45. Atomic commits — one behavior per Red-Green-Refactor cycle
```

Update the section heading from `## 40-Point Holistic Checklist` to `## 45-Point Holistic Checklist`.

**GitHub format** (`resources/github-agents-templates/core/tech-lead.md`):

Add the condensed TDD Process summary line to the checklist categories:
```markdown
- **TDD Process (41-45):** Red-Green-Refactor history, Double-Loop TDD, TPP ordering, atomic commits
```

Update the heading from `## 40-Point Checklist Categories` to `## 45-Point Checklist Categories`.

---

## 5. Backward Compatibility Validation (RULE-003)

| Agent | Original Items | Preserved? | New Total |
|-------|---------------|------------|-----------|
| typescript-developer | 8 responsibilities | Yes, all 8 preserved (reordered + 2 new TDD items prepended) | 10 responsibilities + new TDD Workflow section |
| qa-engineer | 24-point checklist (items 1-24) | Yes, items 1-24 unchanged | 28-point checklist (items 25-28 added) |
| tech-lead | 40-point checklist (items 1-40) | Yes, items 1-40 unchanged | 45-point checklist (items 41-45 added) |

**Validation approach:**
- All existing numbered items remain in their original positions with identical text
- New items are appended to the end (never inserted between existing items)
- Existing section headings and numbering ranges are preserved
- Only the top-level heading number (e.g., "24-Point" to "28-Point") changes

---

## 6. Dual Copy Consistency (RULE-001)

The two template directories serve different output formats:

| Directory | Output Format | Key Differences |
|-----------|--------------|-----------------|
| `resources/agents-templates/` | `.claude/agents/*.md` | Full prose, "Recommended Model" section, "Global Behavior" header |
| `resources/github-agents-templates/` | `.github/agents/*.agent.md` | YAML frontmatter (name, tools), condensed content, no "Recommended Model" |

**Consistency rules:**
- Both copies must contain the same TDD conceptual content
- Wording is adapted to each format's style (full vs. condensed)
- The `.claude/agents/` directory in the project root is a verbatim copy of `agents-templates/`

**Verification:** After implementation, diff the TDD sections across both formats to confirm
semantic equivalence.

---

## 7. Implementation Order

```
Phase 1: Source templates (agents-templates/)
  1a. typescript-developer.md — Add TDD Workflow, reorder responsibilities
  1b. qa-engineer.md — Add TDD Compliance (25-28)
  1c. tech-lead.md — Add TDD Process (41-45)

Phase 2: Source templates (github-agents-templates/)
  2a. typescript-developer.md — Add TDD Workflow, reorder responsibilities (GitHub format)
  2b. qa-engineer.md — Add TDD Compliance (25-28) (GitHub format)
  2c. tech-lead.md — Add TDD Process (41-45) (GitHub format)

Phase 3: Generated agents (.claude/agents/)
  3a. Copy typescript-developer.md from Phase 1a
  3b. Copy qa-engineer.md from Phase 1b
  3c. Copy tech-lead.md from Phase 1c

Phase 4: Golden files update
  4a. Update 17 golden .claude/agents/ files (qa-engineer x8, tech-lead x8, typescript-developer x1)
  4b. Update 17 golden .github/agents/ files (qa-engineer x8, tech-lead x8, typescript-developer x1)

Phase 5: Verification
  5a. Run byte-for-byte integration tests: npx vitest run tests/node/integration/byte-for-byte.test.ts
  5b. Run full test suite: npm test
  5c. Verify coverage thresholds maintained
```

---

## 8. Test Strategy

### 8.1 Golden File Updates

The byte-for-byte test (`tests/node/integration/byte-for-byte.test.ts`) runs the generation
pipeline for each of the 8 profiles and compares output against golden files. After modifying
the source templates, golden files MUST be regenerated or manually updated to match.

**Efficient approach:** After updating source templates (Phases 1-2), run the pipeline once
per profile and copy the generated agent files into the corresponding golden directories.
Alternatively, since the content is known and static, manually apply the same edits to all
golden files.

### 8.2 Test Categories

| Test | File | Validates |
|------|------|-----------|
| Byte-for-byte parity | `tests/node/integration/byte-for-byte.test.ts` | Generated output matches golden files for all 8 profiles |
| E2E verification | `tests/node/integration/e2e-verification.test.ts` | Pipeline runs successfully, file counts correct |

### 8.3 No New Tests Required

The existing byte-for-byte test suite already validates that generated agents match golden files.
Since this story only modifies template content (not pipeline logic), no new test files are needed.
The updated golden files serve as the acceptance criteria.

### 8.4 Manual Verification Checklist

After implementation:
- [ ] `resources/agents-templates/developers/typescript-developer.md` contains `## TDD Workflow`
- [ ] `resources/agents-templates/core/qa-engineer.md` heading says "28-Point" and has items 25-28
- [ ] `resources/agents-templates/core/tech-lead.md` heading says "45-Point" and has items 41-45
- [ ] Same content in `resources/github-agents-templates/` equivalents
- [ ] `.claude/agents/` files are identical to `resources/agents-templates/` versions
- [ ] All 34 golden files updated
- [ ] `npm test` passes with all tests green
- [ ] Coverage >= 95% line, >= 90% branch

---

## 9. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file mismatch (missed 1+ file) | Medium | High (tests fail) | Use glob patterns to identify all 34 golden files; run tests after each batch |
| Dual copy inconsistency | Low | Medium (RULE-001 violation) | Diff both template dirs after changes; verify semantic equivalence |
| Existing checklist items altered | Low | High (RULE-003 violation) | Use precise insertions at end of file; never edit existing numbered items |
| Formatting differences break byte-for-byte | Medium | High (tests fail) | Match exact whitespace, line endings, and trailing newlines of existing files |
| Placeholder breakage (`{{FRAMEWORK}}`, etc.) | Low | Medium | TDD sections use no new placeholders; existing ones untouched |

### Key Constraints

1. **No pipeline code changes** — All changes are template-only. The assemblers copy and render
   templates without modification to their structure.
2. **No new placeholders** — TDD content is static English text. No `{{VAR}}` substitutions needed.
3. **No new checklist injection** — The checklist injection mechanism (`agents-selection.ts`) is
   config-driven and applies to conditional checklists (security, API, devops). The TDD items
   are unconditionally present in the base templates, not injected conditionally.

---

## 10. Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| story-0003-0001 (KP Testing with TDD) | Required (DoR) | TDD testing patterns must be defined before agents reference them |
| story-0003-0002 (KP Coding Standards with refactoring) | Required (DoR) | Refactoring standards must exist before agents enforce them |

---

## 11. Acceptance Criteria Cross-Reference

| Gherkin Scenario | Validated By |
|-----------------|-------------|
| Developer agent contains TDD Workflow | File #1 contains `## TDD Workflow` with "write the test FIRST" |
| Developer agent reorders responsibilities | File #1 responsibilities list starts with test-writing |
| QA agent contains TDD Compliance category | File #2 has `### TDD Compliance (25-28)` with 4 items |
| Tech Lead agent contains TDD Process category | File #3 has `### TDD Process (41-45)` with 5 items |
| Existing checklists preserved | Items 1-24 (QA) and 1-40 (TL) unchanged in content and numbering |
| Dual copy consistency | Files #1-3 and #4-6 contain equivalent TDD content |
| Golden files updated | All 34 golden files pass byte-for-byte test |
