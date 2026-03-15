# Task Decomposition: STORY-0003-0015 -- x-review QA Engineer TDD Checklist

## Overview

Add 6 TDD checklist items to the QA Engineer specialist in the x-review skill. Update item count from 12 to 18, scoring from /24 to /36. Update QA KP paths to reference TDD sections. No TypeScript code changes.

**Total affected files:** 26 (2 source templates + 24 golden files)

---

## G1: Source Templates -- Edit Both x-review Templates

**Dependencies:** None
**Purpose:** Apply the 3 changes (QA KP path, QA checklist, consolidation table) to both source templates.

### G1-T1: Edit Claude source template -- QA KP path

- **File:** `resources/skills-templates/core/x-review/SKILL.md`
- **Line:** 104
- **Action:** Update QA row in Engineer -> Knowledge Pack Mapping table

**Current:**
```
| QA | `skills/testing/references/testing-philosophy.md`, `skills/testing/references/testing-conventions.md` |
```

**Updated:**
```
| QA | `skills/testing/references/testing-philosophy.md`, `skills/testing/references/testing-conventions.md` (focus on TDD Workflow, Double-Loop TDD, and Transformation Priority Premise sections) |
```

### G1-T2: Edit Claude source template -- QA checklist

- **File:** `resources/skills-templates/core/x-review/SKILL.md`
- **Line:** 116
- **Action:** Change `12 items, /24` to `18 items, /36` and append 6 TDD items

**Current:**
```
**QA (12 items, /24):** Test exists for each AC, line coverage ≥95%, branch coverage ≥90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API.
```

**Updated:**
```
**QA (18 items, /36):** Test exists for each AC, line coverage ≥95%, branch coverage ≥90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.
```

**6 new items appended (items 13-18):**

| # | Short Name | Checklist Text |
|---|-----------|---------------|
| 13 | test-first commits | commits show test-first pattern |
| 14 | refactoring after green | explicit refactoring after green |
| 15 | TPP progression | tests follow TPP progression |
| 16 | no test-after | no test written after implementation |
| 17 | acceptance tests | acceptance tests validate E2E behavior |
| 18 | TDD coverage | TDD coverage thresholds maintained |

### G1-T3: Edit Claude source template -- consolidation table

- **File:** `resources/skills-templates/core/x-review/SKILL.md`
- **Line:** 141
- **Action:** Change QA score denominator from 24 to 36

**Current:**
```
| QA            | XX/24 | Rejected           |
```

**Updated:**
```
| QA            | XX/36 | Rejected           |
```

### G1-T4: Edit GitHub source template -- QA KP path

- **File:** `resources/github-skills-templates/review/x-review.md`
- **Line:** 100
- **Action:** Update QA row in Engineer -> Knowledge Pack Mapping table

**Current:**
```
| QA | `.github/skills/testing/SKILL.md` |
```

**Updated:**
```
| QA | `.github/skills/testing/SKILL.md` (focus on TDD Workflow, Double-Loop TDD, and Transformation Priority Premise sections) |
```

### G1-T5: Edit GitHub source template -- QA checklist

- **File:** `resources/github-skills-templates/review/x-review.md`
- **Line:** 112
- **Action:** Change `12 items, /24` to `18 items, /36` and append 6 TDD items (using `>=` not `≥`)

**Current:**
```
**QA (12 items, /24):** Test exists for each AC, line coverage >=95%, branch coverage >=90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API.
```

**Updated:**
```
**QA (18 items, /36):** Test exists for each AC, line coverage >=95%, branch coverage >=90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.
```

### G1-T6: Edit GitHub source template -- consolidation table

- **File:** `resources/github-skills-templates/review/x-review.md`
- **Line:** 137
- **Action:** Change QA score denominator from 24 to 36

**Current:**
```
| QA            | XX/24 | Rejected           |
```

**Updated:**
```
| QA            | XX/36 | Rejected           |
```

### G1-T7: Verify dual copy consistency (RULE-001)

- **Action:** Confirm that the 3 changes are semantically equivalent across both templates while preserving encoding differences
- **Encoding rules:**
  - Claude template: em-dash `—`, Unicode `≥`, arrow `→`
  - GitHub template: double-dash `--`, ASCII `>=`, arrow `->`
- **Command:**
  ```bash
  diff <(sed -n '/^### Engineer Checklists/,/^## Phase 3/p' resources/skills-templates/core/x-review/SKILL.md) \
       <(sed -n '/^### Engineer Checklists/,/^## Phase 3/p' resources/github-skills-templates/review/x-review.md)
  ```
- **Expected:** Only encoding differences (no semantic divergence in the QA items)

### G1-T8: Verify no existing content was modified

- **Command:** `git diff resources/skills-templates/core/x-review/SKILL.md resources/github-skills-templates/review/x-review.md`
- **Expected:** Only the 3 changes per file (6 total). All existing QA items, all other engineer checklists, and all other sections remain unchanged. No deletions -- additions only.

### Checkpoint: Source templates pass manual review

---

## G2: Golden Files -- Update All 24 Golden Files

**Dependencies:** G1 completed
**Strategy:** Apply the same 3 changes to each golden file. `.claude/` and `.agents/` files mirror the Claude template (using `≥`). `.github/` files mirror the GitHub template (using `>=`).

### G2-T1: Update 16 `.claude/` and `.agents/` golden files

- **Source pattern:** Claude template (using `≥`)
- **3 changes per file** (same as G1-T1, G1-T2, G1-T3):
  - Line 104: QA KP path -- add TDD focus instruction
  - Line 116: QA checklist -- `12 items, /24` -> `18 items, /36`, append 6 items
  - Line 141: Consolidation -- `XX/24` -> `XX/36`
- **Files (16 total):**

| Profile | `.claude/` file | `.agents/` file |
|---------|----------------|----------------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-review/SKILL.md` | `tests/golden/go-gin/.agents/skills/x-review/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-review/SKILL.md` | `tests/golden/java-quarkus/.agents/skills/x-review/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-review/SKILL.md` | `tests/golden/java-spring/.agents/skills/x-review/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-review/SKILL.md` | `tests/golden/kotlin-ktor/.agents/skills/x-review/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-review/SKILL.md` | `tests/golden/python-click-cli/.agents/skills/x-review/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-review/SKILL.md` | `tests/golden/python-fastapi/.agents/skills/x-review/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-review/SKILL.md` | `tests/golden/rust-axum/.agents/skills/x-review/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-review/SKILL.md` | `tests/golden/typescript-nestjs/.agents/skills/x-review/SKILL.md` |

- **Batch approach:** Since `.claude/` and `.agents/` golden files are byte-identical to the Claude source template, apply the same 3 edits to all 16 files using `replace_all` or scripted find/replace.

### G2-T2: Update 8 `.github/` golden files

- **Source pattern:** GitHub template (using `>=`)
- **3 changes per file** (same as G1-T4, G1-T5, G1-T6):
  - Line 100: QA KP path -- add TDD focus instruction
  - Line 112: QA checklist -- `12 items, /24` -> `18 items, /36`, append 6 items
  - Line 137: Consolidation -- `XX/24` -> `XX/36`
- **Files (8 total):**

| Profile | `.github/` file |
|---------|----------------|
| go-gin | `tests/golden/go-gin/.github/skills/x-review/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-review/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-review/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-review/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-review/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-review/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-review/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-review/SKILL.md` |

### G2-T3: Verify golden file consistency

- **Action:** Confirm all `.claude/` and `.agents/` golden files are byte-identical. Confirm all `.github/` golden files are byte-identical.
- **Command:**
  ```bash
  md5 tests/golden/*/.claude/skills/x-review/SKILL.md tests/golden/*/.agents/skills/x-review/SKILL.md
  md5 tests/golden/*/.github/skills/x-review/SKILL.md
  ```
- **Expected:** One hash for all 16 `.claude/`+`.agents/` files, another hash for all 8 `.github/` files.

### Checkpoint: All 26 files edited, hashes verified

---

## G3: Verification -- Run Tests

**Dependencies:** G2 completed

### G3-T1: Run TypeScript compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors. No TypeScript files were modified -- this is a sanity check.

### G3-T2: Run byte-for-byte integration tests

- **Command:** `npx vitest run tests/node/integration/byte-for-byte.test.ts`
- **Expected:** All 8 profiles pass `pipelineMatchesGoldenFiles` assertions. This confirms the golden files match what the pipeline would generate from the updated source templates.

### G3-T3: Run full test suite with coverage

- **Command:** `npx vitest run --coverage`
- **Expected:** All 1,384+ tests pass. Coverage >= 95% line, >= 90% branch (unchanged since no TypeScript code was modified).

### G3-T4: Final git diff review

- **Command:** `git diff --stat`
- **Expected:** Exactly 26 files changed (2 source templates + 24 golden files). All changes are modifications to existing lines (no new files, no deletions of lines beyond the replaced text).

### Checkpoint: All tests green, coverage maintained

---

## Dependency Graph

```
G1 (Source Templates: edit both x-review templates)
 |
 v
G2 (Golden Files: update all 24 golden files)
 |
 v
G3 (Verification: compile, test, coverage)
```

All groups are sequential. No parallelism between groups. Within G2, T1 and T2 can run in parallel (independent file sets).

---

## Change Summary Per File

### 3 Change Points (applied to all 26 files)

| Change | Location (Claude/agents) | Location (GitHub) | Description |
|--------|------------------------|-------------------|-------------|
| A: QA KP path | Line 104 | Line 100 | Add TDD section focus instruction |
| B: QA checklist | Line 116 | Line 112 | 12->18 items, /24->/36, append 6 TDD items |
| C: Consolidation | Line 141 | Line 137 | XX/24 -> XX/36 |

### Character Encoding Matrix

| Variant | Files | Comparison | Dash | Arrow |
|---------|-------|-----------|------|-------|
| Claude source + `.claude/` + `.agents/` golden | 1 + 16 = 17 | `≥` | `—` | `→` |
| GitHub source + `.github/` golden | 1 + 8 = 9 | `>=` | `--` | `->` |

---

## Risk Mitigations

| Risk | Mitigation |
|------|------------|
| Existing 12 QA items accidentally modified | Append-only change; verify with `git diff` showing no deletions |
| Dual copy encoding inconsistency | Separate edits per variant; G1-T7 verifies semantic equivalence |
| Golden file hash mismatch | G2-T3 md5 verification before running tests |
| Item count mismatch (header vs actual) | Count all 18 comma-separated items in updated line |
| Consolidation table not updated | G1-T3/G1-T6 explicitly target the table row |

---

## Summary Table

| Group | Tasks | Files Modified | Estimated Effort |
|-------|-------|---------------|-----------------|
| G1 | 8 (6 edits + 2 verification) | 2 source templates | Small |
| G2 | 3 (2 batch edits + 1 verification) | 24 golden files | Mechanical (scripted) |
| G3 | 4 (compile + tests + coverage + diff) | 0 | Automated (tests) |
| **Total** | **15** | **26** | **Small** |
