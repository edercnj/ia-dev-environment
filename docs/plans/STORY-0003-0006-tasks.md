# Task Decomposition -- STORY-0003-0006: Agents TDD Workflows for Developer, QA and Tech Lead

**Status:** PENDING
**Date:** 2026-03-15
**Blocked By:** story-0003-0001 (KP Testing with TDD), story-0003-0002 (KP Coding Standards with refactoring)
**Blocks:** story-0003-0012, story-0003-0015, story-0003-0016

---

## G1 -- Test Setup (RED phase)

**Purpose:** Write content-validation tests that assert TDD sections exist in the 3 agent templates. These tests MUST fail initially because the TDD content has not been added yet, confirming the RED phase of the TDD cycle.
**Dependencies:** None
**Compiles independently:** Yes -- test file only, no source changes.

### T1.1 -- Create content-validation test file for agent TDD sections

- **File:** `tests/node/content/agent-tdd-sections.test.ts` (create)
- **What to implement:**
  - Read source template files and assert TDD content presence
  - 3 describe blocks: typescript-developer, qa-engineer, tech-lead
  - Tests for typescript-developer:
    - Source file contains `## TDD Workflow` section
    - Source file contains "write the test FIRST"
    - Responsibilities list starts with test-writing (items 1-3 are TDD-related)
    - Source file contains "Red-Green-Refactor" cycle description
    - Source file contains "Transformation Priority Premise"
  - Tests for qa-engineer:
    - Source file contains `### TDD Compliance (25-28)` category
    - Heading says "28-Point" (not "24-Point")
    - Contains 4 TDD items (items 25-28)
    - Contains "test-first pattern"
    - Contains "explicit refactoring"
    - Contains "Double-Loop TDD"
    - All original items 1-24 remain intact
  - Tests for tech-lead:
    - Source file contains `### TDD Process (41-45)` category
    - Heading says "45-Point" (not "40-Point")
    - Contains 5 TDD items (items 41-45)
    - Contains "Red-Green-Refactor progression"
    - Contains "Double-Loop TDD"
    - Contains "Transformation Priority Premise"
    - Contains "Atomic commits"
    - All original items 1-40 remain intact
  - Tests for dual-copy consistency:
    - Claude format and GitHub format both contain TDD sections for each agent
    - `.claude/agents/` files match `resources/agents-templates/` versions
- **Source files to read:**
  - `resources/agents-templates/developers/typescript-developer.md`
  - `resources/agents-templates/core/qa-engineer.md`
  - `resources/agents-templates/core/tech-lead.md`
  - `resources/github-agents-templates/developers/typescript-developer.md`
  - `resources/github-agents-templates/core/qa-engineer.md`
  - `resources/github-agents-templates/core/tech-lead.md`
- **Dependencies on other tasks:** None
- **Estimated complexity:** M

### Verification checkpoint G1

```bash
# Compile test file
npx tsc --noEmit

# Run tests -- ALL should FAIL (RED phase)
npx vitest run tests/node/content/agent-tdd-sections.test.ts
```

**Expected outcome:** All new tests fail because TDD content is not yet present in templates. This confirms the RED phase.

---

## G2 -- typescript-developer Agent (GREEN phase, part 1)

**Purpose:** Add TDD Workflow section and reorder responsibilities in the typescript-developer agent template (Claude format), then mirror to the GitHub format template.
**Dependencies:** G1 (tests written and failing)
**Compiles independently:** N/A -- pure resource files (Markdown templates), no TypeScript changes.

### T2.1 -- Update Claude format: `resources/agents-templates/developers/typescript-developer.md`

- **File:** `resources/agents-templates/developers/typescript-developer.md` (modify)
- **What to implement:**
  - **Reorder Responsibilities** (lines 22-29): Change from implementation-first to test-first order:
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
  - **Add new `## TDD Workflow` section** after `## Responsibilities` and before `## Implementation Standards`:
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
- **Backward compatibility:** All 8 original responsibility items preserved (reordered + 2 new TDD items prepended). Total grows from 8 to 10.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T2.2 -- Update GitHub format: `resources/github-agents-templates/developers/typescript-developer.md`

- **File:** `resources/github-agents-templates/developers/typescript-developer.md` (modify)
- **What to implement:**
  - **Reorder Responsibilities** (lines 33-39): Change from implementation-first to test-first order:
    1. Write failing tests FIRST for each behavior (Red phase)
    2. Implement the minimum code to make tests pass (Green phase)
    3. Refactor while keeping tests green (Refactor phase)
    4. Follow the architect's plan precisely
    5. Write production code using modern TypeScript idioms
    6. Follow {{FRAMEWORK}} conventions
    7. Apply strict type checking throughout
    8. Apply Clean Code principles
  - **Add new `## TDD Workflow` section** after `## Responsibilities` and before `## Rules`:
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
- **Backward compatibility:** All 6 original responsibility items preserved (reordered + 2 new TDD items prepended). Total grows from 6 to 8.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Verification checkpoint G2

```bash
# Run typescript-developer tests only
npx vitest run tests/node/content/agent-tdd-sections.test.ts -t "typescript-developer"
```

**Expected outcome:** typescript-developer tests pass. qa-engineer and tech-lead tests still fail.

---

## G3 -- qa-engineer Agent (GREEN phase, part 2)

**Purpose:** Add TDD Compliance category (4 items) to the qa-engineer agent template in both Claude and GitHub formats.
**Dependencies:** G1 (tests written and failing)
**Compiles independently:** N/A -- pure resource files (Markdown templates).

### T3.1 -- Update Claude format: `resources/agents-templates/core/qa-engineer.md`

- **File:** `resources/agents-templates/core/qa-engineer.md` (modify)
- **What to implement:**
  - **Update heading** on line 26: Change `## 24-Point QA Checklist` to `## 28-Point QA Checklist`
  - **Add new category** after `### Fixtures & Organization (21-24)` section (after line 60), before `## Output Format`:
    ```markdown
    ### TDD Compliance (25-28)
    25. Commits show test-first pattern (test file modified before production code)
    26. Explicit refactoring commits exist after green phase (no behavior changes in refactoring)
    27. Tests are incremental — progression from simple to complex (Transformation Priority Premise)
    28. Acceptance tests exist for end-to-end scenarios before unit tests (Double-Loop TDD)
    ```
- **Backward compatibility:** Items 1-24 remain untouched in content and numbering. New items 25-28 appended after existing content.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T3.2 -- Update GitHub format: `resources/github-agents-templates/core/qa-engineer.md`

- **File:** `resources/github-agents-templates/core/qa-engineer.md` (modify)
- **What to implement:**
  - **Update heading** on line 40: Change `## 24-Point QA Checklist` to `## 28-Point QA Checklist`
  - **Add new condensed summary line** after `- **Fixtures & Organization (21-24):**` (after line 46), before `## Output Format`:
    ```markdown
    - **TDD Compliance (25-28):** Test-first commits, refactoring phases, incremental progression, acceptance tests
    ```
- **Backward compatibility:** Existing 5 summary lines unchanged. One new line appended.
- **Dependencies on other tasks:** None
- **Estimated complexity:** XS

### Verification checkpoint G3

```bash
# Run qa-engineer tests only
npx vitest run tests/node/content/agent-tdd-sections.test.ts -t "qa-engineer"
```

**Expected outcome:** qa-engineer tests pass. tech-lead tests still fail.

---

## G4 -- tech-lead Agent (GREEN phase, part 3)

**Purpose:** Add TDD Process category (5 items) to the tech-lead agent template in both Claude and GitHub formats.
**Dependencies:** G1 (tests written and failing)
**Compiles independently:** N/A -- pure resource files (Markdown templates).

### T4.1 -- Update Claude format: `resources/agents-templates/core/tech-lead.md`

- **File:** `resources/agents-templates/core/tech-lead.md` (modify)
- **What to implement:**
  - **Update heading** on line 25: Change `## 40-Point Holistic Checklist` to `## 45-Point Holistic Checklist`
  - **Add new category** after `### Operational Readiness (38-40)` section (after line 77), before `## Output Format`:
    ```markdown
    ### TDD Process (41-45)
    41. Git history shows Red-Green-Refactor progression (test commit precedes implementation commit)
    42. Double-Loop TDD: acceptance test precedes unit tests for each feature
    43. Transformation Priority Premise ordering visible in test progression (simple to complex)
    44. Refactoring phases do not add new behavior (tests unchanged during refactor commits)
    45. Atomic commits — one behavior per Red-Green-Refactor cycle
    ```
- **Backward compatibility:** Items 1-40 remain untouched in content and numbering. New items 41-45 appended after existing content.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T4.2 -- Update GitHub format: `resources/github-agents-templates/core/tech-lead.md`

- **File:** `resources/github-agents-templates/core/tech-lead.md` (modify)
- **What to implement:**
  - **Update heading** on line 40: Change `## 40-Point Checklist Categories` to `## 45-Point Checklist Categories`
  - **Update description** in YAML frontmatter (line 4): Change `40-point review checklist` to `45-point review checklist`
  - **Add new condensed summary line** after `- **Operational Readiness (38-40):**` (after line 47), before `## Output Format`:
    ```markdown
    - **TDD Process (41-45):** Red-Green-Refactor history, Double-Loop TDD, TPP ordering, atomic commits
    ```
- **Backward compatibility:** Existing 6 summary lines unchanged. One new line appended. YAML frontmatter description updated only for point count.
- **Dependencies on other tasks:** None
- **Estimated complexity:** XS

### Verification checkpoint G4

```bash
# Run all content tests -- ALL should pass now
npx vitest run tests/node/content/agent-tdd-sections.test.ts
```

**Expected outcome:** All content-validation tests pass (GREEN phase complete for all 3 agents).

---

## G5 -- Generated Outputs (`.claude/agents/` copies)

**Purpose:** Update the project's own `.claude/agents/` files to mirror the changes from `resources/agents-templates/`. These files are verbatim copies of the agents-templates directory (no placeholder replacement, as this project uses `{{FRAMEWORK}}` placeholders directly).
**Dependencies:** G2 (T2.1), G3 (T3.1), G4 (T4.1)
**Compiles independently:** N/A -- pure Markdown files.

### T5.1 -- Update `.claude/agents/typescript-developer.md`

- **File:** `.claude/agents/typescript-developer.md` (modify)
- **What to implement:** Copy the exact content of `resources/agents-templates/developers/typescript-developer.md` (after G2 changes) into this file. The content must be byte-for-byte identical.
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### T5.2 -- Update `.claude/agents/qa-engineer.md`

- **File:** `.claude/agents/qa-engineer.md` (modify)
- **What to implement:** Copy the exact content of `resources/agents-templates/core/qa-engineer.md` (after G3 changes) into this file. The content must be byte-for-byte identical.
- **Dependencies on other tasks:** T3.1
- **Estimated complexity:** XS

### T5.3 -- Update `.claude/agents/tech-lead.md`

- **File:** `.claude/agents/tech-lead.md` (modify)
- **What to implement:** Copy the exact content of `resources/agents-templates/core/tech-lead.md` (after G4 changes) into this file. The content must be byte-for-byte identical.
- **Dependencies on other tasks:** T4.1
- **Estimated complexity:** XS

### Verification checkpoint G5

```bash
# Diff to verify byte-for-byte match with source templates
diff resources/agents-templates/developers/typescript-developer.md .claude/agents/typescript-developer.md
diff resources/agents-templates/core/qa-engineer.md .claude/agents/qa-engineer.md
diff resources/agents-templates/core/tech-lead.md .claude/agents/tech-lead.md
```

**Expected outcome:** All 3 diffs produce no output (files are identical).

---

## G6 -- Golden Files

**Purpose:** Update all golden test files to reflect the changes made to source templates. The byte-for-byte integration test (`tests/node/integration/byte-for-byte.test.ts`) compares pipeline-generated output against golden files. Golden files must match what the pipeline produces after template changes.
**Dependencies:** G2, G3, G4 (all source templates updated)
**Compiles independently:** N/A -- pure Markdown files.

### T6.1 -- Update golden `.claude/agents/typescript-developer.md` (1 file)

- **File:** `tests/golden/typescript-nestjs/.claude/agents/typescript-developer.md` (modify)
- **What to implement:** Apply the same changes from T2.1 (reordered responsibilities, TDD Workflow section). Content is identical to the source template because no placeholder replacement occurs for this file.
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### T6.2 -- Update golden `.claude/agents/qa-engineer.md` (8 files)

- **Files (all identical content):**
  - `tests/golden/go-gin/.claude/agents/qa-engineer.md`
  - `tests/golden/java-quarkus/.claude/agents/qa-engineer.md`
  - `tests/golden/java-spring/.claude/agents/qa-engineer.md`
  - `tests/golden/kotlin-ktor/.claude/agents/qa-engineer.md`
  - `tests/golden/python-click-cli/.claude/agents/qa-engineer.md`
  - `tests/golden/python-fastapi/.claude/agents/qa-engineer.md`
  - `tests/golden/rust-axum/.claude/agents/qa-engineer.md`
  - `tests/golden/typescript-nestjs/.claude/agents/qa-engineer.md`
- **What to implement:** Apply the same changes from T3.1 (28-Point heading, TDD Compliance items 25-28). All 8 files get identical content since qa-engineer is a core agent with no profile-specific placeholders.
- **Dependencies on other tasks:** T3.1
- **Estimated complexity:** S (repetitive but many files)

### T6.3 -- Update golden `.claude/agents/tech-lead.md` (8 files)

- **Files (all identical content):**
  - `tests/golden/go-gin/.claude/agents/tech-lead.md`
  - `tests/golden/java-quarkus/.claude/agents/tech-lead.md`
  - `tests/golden/java-spring/.claude/agents/tech-lead.md`
  - `tests/golden/kotlin-ktor/.claude/agents/tech-lead.md`
  - `tests/golden/python-click-cli/.claude/agents/tech-lead.md`
  - `tests/golden/python-fastapi/.claude/agents/tech-lead.md`
  - `tests/golden/rust-axum/.claude/agents/tech-lead.md`
  - `tests/golden/typescript-nestjs/.claude/agents/tech-lead.md`
- **What to implement:** Apply the same changes from T4.1 (45-Point heading, TDD Process items 41-45). All 8 files get identical content since tech-lead is a core agent with no profile-specific placeholders.
- **Dependencies on other tasks:** T4.1
- **Estimated complexity:** S (repetitive but many files)

### T6.4 -- Update golden `.github/agents/typescript-developer.agent.md` (1 file)

- **File:** `tests/golden/typescript-nestjs/.github/agents/typescript-developer.agent.md` (modify)
- **What to implement:** Apply the same changes from T2.2 (reordered responsibilities, TDD Workflow section in GitHub format).
- **Dependencies on other tasks:** T2.2
- **Estimated complexity:** XS

### T6.5 -- Update golden `.github/agents/qa-engineer.agent.md` (8 files)

- **Files (all identical content):**
  - `tests/golden/go-gin/.github/agents/qa-engineer.agent.md`
  - `tests/golden/java-quarkus/.github/agents/qa-engineer.agent.md`
  - `tests/golden/java-spring/.github/agents/qa-engineer.agent.md`
  - `tests/golden/kotlin-ktor/.github/agents/qa-engineer.agent.md`
  - `tests/golden/python-click-cli/.github/agents/qa-engineer.agent.md`
  - `tests/golden/python-fastapi/.github/agents/qa-engineer.agent.md`
  - `tests/golden/rust-axum/.github/agents/qa-engineer.agent.md`
  - `tests/golden/typescript-nestjs/.github/agents/qa-engineer.agent.md`
- **What to implement:** Apply the same changes from T3.2 (28-Point heading, TDD Compliance condensed summary line). All 8 files get identical content.
- **Dependencies on other tasks:** T3.2
- **Estimated complexity:** S (repetitive but many files)

### T6.6 -- Update golden `.github/agents/tech-lead.agent.md` (8 files)

- **Files (all identical content):**
  - `tests/golden/go-gin/.github/agents/tech-lead.agent.md`
  - `tests/golden/java-quarkus/.github/agents/tech-lead.agent.md`
  - `tests/golden/java-spring/.github/agents/tech-lead.agent.md`
  - `tests/golden/kotlin-ktor/.github/agents/tech-lead.agent.md`
  - `tests/golden/python-click-cli/.github/agents/tech-lead.agent.md`
  - `tests/golden/python-fastapi/.github/agents/tech-lead.agent.md`
  - `tests/golden/rust-axum/.github/agents/tech-lead.agent.md`
  - `tests/golden/typescript-nestjs/.github/agents/tech-lead.agent.md`
- **What to implement:** Apply the same changes from T4.2 (45-Point heading, 45-point in YAML description, TDD Process condensed summary line). All 8 files get identical content.
- **Dependencies on other tasks:** T4.2
- **Estimated complexity:** S (repetitive but many files)

### Golden File Count Summary

| Agent | `.claude/agents/` | `.github/agents/` | Total |
|-------|-------------------|-------------------|-------|
| typescript-developer | 1 | 1 | 2 |
| qa-engineer | 8 | 8 | 16 |
| tech-lead | 8 | 8 | 16 |
| **Total** | **17** | **17** | **34** |

### Verification checkpoint G6

```bash
# Run byte-for-byte integration tests
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

**Expected outcome:** All 8 profiles pass byte-for-byte parity (no mismatches, no missing files, no extra files).

---

## G7 -- Verification (full suite)

**Purpose:** Run the complete test suite and verify all acceptance criteria, coverage thresholds, and cross-cutting rules.
**Dependencies:** G1-G6 (all changes complete)
**Compiles independently:** N/A -- verification only.

### T7.1 -- Run full test suite

- **Command:**
  ```bash
  npm test
  ```
- **Expected outcome:** All 1,384+ tests pass (existing tests + new content-validation tests from G1).
- **Dependencies on other tasks:** G1-G6

### T7.2 -- Verify coverage thresholds

- **Command:**
  ```bash
  npm test -- --coverage
  ```
- **Expected outcome:** Line coverage >= 95%, branch coverage >= 90%.
- **Dependencies on other tasks:** T7.1

### T7.3 -- Verify TypeScript compilation

- **Command:**
  ```bash
  npx tsc --noEmit
  ```
- **Expected outcome:** Zero compilation errors.
- **Dependencies on other tasks:** G1 (test file must compile)

### T7.4 -- Validate acceptance criteria

Manual / automated checks:

| # | Acceptance Criterion | Validation |
|---|---------------------|------------|
| AC-1 | typescript-developer contains `## TDD Workflow` with "write the test FIRST" | Content test in G1 |
| AC-2 | typescript-developer responsibilities are test-first ordered | Content test in G1 |
| AC-3 | qa-engineer has `### TDD Compliance (25-28)` with 4 items | Content test in G1 |
| AC-4 | tech-lead has `### TDD Process (41-45)` with 5 items | Content test in G1 |
| AC-5 | Existing checklist items preserved (24 for QA, 40 for TL) | Content test in G1 |
| AC-6 | Dual copy consistency (agents-templates + github-agents-templates) | Content test in G1 |
| AC-7 | `.claude/agents/` match `resources/agents-templates/` | Diff check in G5 |
| AC-8 | All 34 golden files updated | Byte-for-byte tests in G6 |
| AC-9 | Coverage >= 95% line, >= 90% branch | Coverage report in T7.2 |

### Verification checkpoint G7

```bash
# Full verification sequence
npx tsc --noEmit && npm test -- --coverage
```

**Expected outcome:** Zero errors, all tests pass, coverage thresholds met.

---

## Dependency Graph

```
G1 (Test Setup - RED)
 |
 +---> G2 (typescript-developer) ---> G5.T5.1 (.claude/agents copy)
 |                                |-> G6.T6.1 (golden .claude 1 file)
 |                                +-> G6.T6.4 (golden .github 1 file)
 |
 +---> G3 (qa-engineer) -----------> G5.T5.2 (.claude/agents copy)
 |                                |-> G6.T6.2 (golden .claude 8 files)
 |                                +-> G6.T6.5 (golden .github 8 files)
 |
 +---> G4 (tech-lead) ------------> G5.T5.3 (.claude/agents copy)
 |                                |-> G6.T6.3 (golden .claude 8 files)
 |                                +-> G6.T6.6 (golden .github 8 files)
 |
 G5 + G6 (all complete)
 |
 +---> G7 (Verification)
```

**Parallelism:** G2, G3, and G4 can be executed in parallel after G1 completes. G5 and G6 depend on their respective source template groups but can also execute in parallel across agents.

---

## File Count Summary

| Category | Files | Group |
|----------|-------|-------|
| New test file | 1 | G1 |
| Source templates (agents-templates/) | 3 | G2, G3, G4 |
| Source templates (github-agents-templates/) | 3 | G2, G3, G4 |
| Generated agents (.claude/agents/) | 3 | G5 |
| Golden files (.claude/agents/ format) | 17 | G6 |
| Golden files (.github/agents/ format) | 17 | G6 |
| **Total files to create/modify** | **44** | |

---

## Commit Strategy (RULE-008: Atomic TDD Commits)

| Commit | Content | Type |
|--------|---------|------|
| 1 | G1: Add content-validation tests for agent TDD sections (RED) | `test:` |
| 2 | G2: Add TDD Workflow to typescript-developer agent templates | `feat:` |
| 3 | G3: Add TDD Compliance category to qa-engineer agent templates | `feat:` |
| 4 | G4: Add TDD Process category to tech-lead agent templates | `feat:` |
| 5 | G5: Update .claude/agents/ generated copies | `feat:` |
| 6 | G6: Update golden files for all 8 profiles | `test:` |
| 7 | G7: Verify full suite passes (no file changes -- verification only) | N/A |
