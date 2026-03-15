# Task Decomposition -- STORY-0003-0007: x-test-plan -- Promotion to Implementation Driver with TPP

**Status:** PENDING
**Date:** 2026-03-15
**Story:** story-0003-0007
**Blocked By:** story-0003-0001 (Testing KP with TDD/TPP -- DONE)
**Blocks:** story-0003-0008, story-0003-0012, story-0003-0014

---

## G1 -- Template Design (Claude Copy)

**Purpose:** Read and analyze the current `x-test-plan` Claude template, understand the structure to be replaced, and design the new Step 2 section with Double-Loop + TPP structure, the new output format, and updated quality checks.
**Dependencies:** None
**Compiles independently:** N/A -- research and design only, no file changes.

### T1.1 -- Read current Claude template

- **File:** `resources/skills-templates/core/x-test-plan/SKILL.md` (read-only)
- **What to analyze:**
  - YAML frontmatter (lines 1-6): unchanged
  - Global Output Policy (lines 8-12): unchanged
  - Title, Purpose, Input (lines 14-24): unchanged
  - Execution Flow (lines 26-32): Step 2 description text updated
  - Step 1: Subagent (lines 34-63): **FULLY PRESERVED** (RULE-009) -- character-for-character identical
  - Step 2: Generate Test Scenarios (lines 65-94): **TO BE REPLACED**
  - Step 3: Estimate & Validate (lines 96-114): Quality Checks section **TO BE UPDATED**
  - Output format (lines 116-149): **TO BE REPLACED**
  - Anti-Patterns (lines 151-158): Extended with TPP anti-pattern
  - Integration Notes (lines 160-164): unchanged
- **Dependencies on other tasks:** None
- **Estimated complexity:** XS

### T1.2 -- Design new Step 2 structure

- **What to design:**
  - Section 2.1: Acceptance Tests (Outer Loop) -- field table with AT-N ID, Gherkin ref, Status RED, Components, Test Type
  - Section 2.2: Unit Tests (Inner Loop -- TPP Order) -- 6 TPP level sub-sections (Degenerate, Unconditional, Simple Conditions, Complex Conditions, Iterations, Edge Cases), followed by a field table with UT-N metadata
  - Section 2.3: Integration Tests (Cross-Component) -- field table with IT-N ID, Components, Depends on, Parallel
  - Section 2.4: CRUD-Only Story Optimization -- guidance to limit UTs to Levels 1-2 for pure CRUD
- **Key constraint:** The step opening paragraph must change from "generate scenarios by category" to "generate a Double-Loop TDD test plan. Organize scenarios by implementation order (TPP), NOT by test category."
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** M

### T1.3 -- Design new output format

- **What to design:**
  - Summary section: acceptance tests count, unit tests count (TPP order), integration tests count, estimated line/branch coverage
  - Acceptance Tests section: AT-N entries with Gherkin ref, Status RED, Components, Acceptance Criteria
  - Unit Tests section: UT-N entries with Test name, Implementation hint, Transform, Components, Depends on, Parallel
  - Integration Tests section: IT-N entries
  - Coverage Estimation table (preserved)
  - Risks and Gaps section (preserved)
- **Key constraint:** Old category-based output (Happy Path / Error Path / Boundary / Parametrized tables) is completely removed.
- **Dependencies on other tasks:** T1.2
- **Estimated complexity:** S

### T1.4 -- Design updated quality checks

- **What to design:**
  - 10 quality checks replacing the current 7:
    1. Every Gherkin scenario maps to >= 1 AT
    2. Every acceptance criterion maps to >= 1 UT chain
    3. UT-1 is ALWAYS a degenerate case (TPP Level 1)
    4. UTs follow non-decreasing TPP level order
    5. Every exception has >= 1 error path test
    6. Boundary values use triplet pattern (at-min, at-max, past-max)
    7. Dependency markers are complete (no orphan UTs)
    8. Estimated coverage meets thresholds (>= 95% line, >= 90% branch)
    9. Test naming follows convention: `[method]_[scenario]_[expected]`
    10. No unnecessary UTs for CRUD-only stories (max Level 3 unless justified)
- **Dependencies on other tasks:** T1.2
- **Estimated complexity:** S

### Verification checkpoint G1

Design review: all new sections drafted in plan, no file modifications yet.

---

## G2 -- Template Implementation (Claude Copy)

**Purpose:** Modify the Claude template source file to replace Step 2, the output format, and quality checks with the Double-Loop + TPP structure designed in G1.
**Dependencies:** G1 (design must be finalized)
**Compiles independently:** N/A -- markdown template file, no TypeScript changes.

### T2.1 -- Replace Step 2 content

- **File:** `resources/skills-templates/core/x-test-plan/SKILL.md` (modify)
- **What to implement:**
  - Replace lines 65-94 (current Step 2 with category-based subsections 2.1-2.9)
  - New content: Step 2 opening paragraph + sections 2.1 (Acceptance Tests), 2.2 (Unit Tests with TPP Levels 1-6), 2.3 (Integration Tests), 2.4 (CRUD-Only Optimization)
  - Each section includes the field table from the plan (Section 2.2 of the architecture plan)
- **Key constraint:** The `## Step 2: Generate Test Scenarios (Orchestrator -- Inline)` heading is preserved but the opening paragraph changes.
- **Preservation check:** Step 1 content (lines 34-63) must remain character-for-character identical before and after this edit. Verify with diff.
- **Dependencies on other tasks:** T1.2
- **Estimated complexity:** M

### T2.2 -- Replace output format template

- **File:** `resources/skills-templates/core/x-test-plan/SKILL.md` (modify)
- **What to implement:**
  - Replace the markdown code block in the Output section (current lines 120-149)
  - New content: Double-Loop structured output with Summary, Acceptance Tests (AT-N), Unit Tests (UT-N with TPP levels), Integration Tests (IT-N), Coverage Estimation, Risks and Gaps
  - The `## Output` heading and save-to instruction are preserved
- **Dependencies on other tasks:** T1.3
- **Estimated complexity:** S

### T2.3 -- Update quality checks in Step 3

- **File:** `resources/skills-templates/core/x-test-plan/SKILL.md` (modify)
- **What to implement:**
  - Replace the 7 quality checks (current lines 108-114) with 10 new quality checks
  - Coverage Estimation Table is preserved as-is
- **Dependencies on other tasks:** T1.4
- **Estimated complexity:** S

### T2.4 -- Add TPP anti-pattern to Anti-Patterns section

- **File:** `resources/skills-templates/core/x-test-plan/SKILL.md` (modify)
- **What to implement:**
  - Add one new anti-pattern: "Do NOT organize test plan by category (Happy Path, Error Path, etc.) -- use TPP order"
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### T2.5 -- Verify subagent preservation (RULE-009)

- **Action:** Run a character-for-character diff of Step 1 (lines 34-63) before and after all edits.
- **Expected:** Zero differences in the subagent prompt, `allowed-tools`, and 11-point return schema.
- **Verification command:**
  ```bash
  # Compare subagent section before/after (use git diff on the specific line range)
  git diff -- resources/skills-templates/core/x-test-plan/SKILL.md | head -100
  ```
- **Dependencies on other tasks:** T2.1, T2.2, T2.3, T2.4
- **Estimated complexity:** XS

### Verification checkpoint G2

```bash
# Verify template is well-formed markdown
cat resources/skills-templates/core/x-test-plan/SKILL.md | head -5  # YAML frontmatter intact
# Verify Step 1 subagent is unchanged
# Verify new Step 2 contains TPP levels, AT/UT/IT sections
# Verify output format contains Double-Loop structure
```

---

## G3 -- Golden Files Update (.claude/)

**Purpose:** Copy the updated Claude template to all 8 `.claude/` golden file paths. Since the `.claude/` golden files contain the template as-is (no placeholder resolution -- `{{LANGUAGE}}` stays unresolved), all 8 profiles receive identical content.
**Dependencies:** G2 (template must be finalized)
**Compiles independently:** N/A -- markdown files.

### T3.1 -- Copy template to all 8 `.claude/` golden paths

- **Source:** `resources/skills-templates/core/x-test-plan/SKILL.md`
- **Destinations (8 files):**
  1. `tests/golden/go-gin/.claude/skills/x-test-plan/SKILL.md`
  2. `tests/golden/java-quarkus/.claude/skills/x-test-plan/SKILL.md`
  3. `tests/golden/java-spring/.claude/skills/x-test-plan/SKILL.md`
  4. `tests/golden/kotlin-ktor/.claude/skills/x-test-plan/SKILL.md`
  5. `tests/golden/python-click-cli/.claude/skills/x-test-plan/SKILL.md`
  6. `tests/golden/python-fastapi/.claude/skills/x-test-plan/SKILL.md`
  7. `tests/golden/rust-axum/.claude/skills/x-test-plan/SKILL.md`
  8. `tests/golden/typescript-nestjs/.claude/skills/x-test-plan/SKILL.md`
- **Method:** Byte-for-byte copy (`cp` or equivalent). All 8 files are identical.
- **Verification:**
  ```bash
  # All 8 files must be identical to the source
  for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    diff resources/skills-templates/core/x-test-plan/SKILL.md \
         tests/golden/$p/.claude/skills/x-test-plan/SKILL.md
  done
  ```
- **Dependencies on other tasks:** T2.5
- **Estimated complexity:** XS

---

## G4 -- Golden Files Update (.agents/)

**Purpose:** Copy the `.claude/` golden files to all 8 `.agents/` golden file paths. The `.agents/` copies must be byte-for-byte identical to `.claude/` copies (produced by `CodexSkillsAssembler` mirroring mechanism).
**Dependencies:** G3 (`.claude/` golden files must be updated first)
**Compiles independently:** N/A -- markdown files.

### T4.1 -- Copy `.claude/` to all 8 `.agents/` golden paths

- **Source:** The `.claude/` golden files from G3 (or the same source template, since all are identical).
- **Destinations (8 files):**
  1. `tests/golden/go-gin/.agents/skills/x-test-plan/SKILL.md`
  2. `tests/golden/java-quarkus/.agents/skills/x-test-plan/SKILL.md`
  3. `tests/golden/java-spring/.agents/skills/x-test-plan/SKILL.md`
  4. `tests/golden/kotlin-ktor/.agents/skills/x-test-plan/SKILL.md`
  5. `tests/golden/python-click-cli/.agents/skills/x-test-plan/SKILL.md`
  6. `tests/golden/python-fastapi/.agents/skills/x-test-plan/SKILL.md`
  7. `tests/golden/rust-axum/.agents/skills/x-test-plan/SKILL.md`
  8. `tests/golden/typescript-nestjs/.agents/skills/x-test-plan/SKILL.md`
- **Method:** Byte-for-byte copy. All 8 files identical to `.claude/` counterparts.
- **Verification:**
  ```bash
  # All .agents/ must match .claude/ byte-for-byte
  for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    diff tests/golden/$p/.claude/skills/x-test-plan/SKILL.md \
         tests/golden/$p/.agents/skills/x-test-plan/SKILL.md
  done
  ```
- **Dependencies on other tasks:** T3.1
- **Estimated complexity:** XS

---

## G5 -- GitHub Template (Dual Copy)

**Purpose:** Update the GitHub skills template to mirror the Double-Loop + TPP structure in a condensed format suitable for GitHub Copilot. Verify `{language_name}` placeholders are correct.
**Dependencies:** G2 (Claude template must be finalized to mirror from)
**Compiles independently:** N/A -- markdown template file.

### T5.1 -- Replace Step 2 in GitHub template

- **File:** `resources/github-skills-templates/testing/x-test-plan.md` (modify)
- **What to implement:**
  - Replace the current Step 2 content (lines 46-57): category table with 6 rows (Unit, Integration, API, E2E, Contract, Performance)
  - New content: condensed Double-Loop + TPP structure with:
    - "Acceptance Tests (Outer Loop)" brief description
    - TPP Levels table (6 rows: Degenerate, Unconditional, Simple conditions, Complex conditions, Iterations, Edge cases) with Scenarios and Transform columns
    - "Integration Tests" brief description
  - Each UT includes: test name, implementation hint, TPP transform, dependencies, parallel flag
- **Key constraint:** The GitHub copy is condensed -- it does NOT replicate the full field tables from the Claude copy. It provides a summary table and brief descriptions.
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** M

### T5.2 -- Update Step 3 quality checks in GitHub template

- **File:** `resources/github-skills-templates/testing/x-test-plan.md` (modify)
- **What to implement:**
  - Add TPP-aware quality checks after the coverage estimation table (current lines 59-65)
  - Keep the condensed format: list the most critical TPP checks alongside existing checks
- **Dependencies on other tasks:** T2.3
- **Estimated complexity:** S

### T5.3 -- Add TPP anti-pattern to GitHub Anti-Patterns section

- **File:** `resources/github-skills-templates/testing/x-test-plan.md` (modify)
- **What to implement:**
  - Add: "Do NOT organize test plan by category -- use TPP order"
- **Dependencies on other tasks:** T5.1
- **Estimated complexity:** XS

### T5.4 -- Verify `{language_name}` placeholders

- **Action:** Inspect the updated template for `{language_name}` occurrences.
- **Current occurrences (2 locations, unchanged):**
  - Line 22: `Knowledge of {language_name} test frameworks and conventions`
  - Line 27: `{language_name}-specific test frameworks, naming conventions`
- **Expected:** These 2 occurrences remain. No new `{language_name}` placeholders needed (the new TPP content is language-agnostic).
- **Verification:** The `replacePlaceholders()` method in `TemplateEngine` resolves `{language_name}` to the profile's language name. Unknown keys like `{STORY_PATH}` are preserved verbatim.
- **Dependencies on other tasks:** T5.1
- **Estimated complexity:** XS

### Verification checkpoint G5

```bash
# Verify {language_name} appears exactly 2 times (unchanged)
grep -c '{language_name}' resources/github-skills-templates/testing/x-test-plan.md  # expect: 2
```

---

## G6 -- Golden Files Update (.github/)

**Purpose:** Update all 8 `.github/` golden files with the modified GitHub template content, applying profile-specific `{language_name}` substitution.
**Dependencies:** G5 (GitHub template must be finalized)
**Compiles independently:** N/A -- markdown files.

### T6.1 -- Update `.github/` golden files for all 8 profiles

- **Source:** `resources/github-skills-templates/testing/x-test-plan.md`
- **Method:** Either (a) run the pipeline for each profile and copy the output, or (b) manually apply `{language_name}` substitution per profile.
- **Profile-to-language mapping:**

  | Profile | `{language_name}` resolves to |
  |---------|-------------------------------|
  | go-gin | `go` |
  | java-quarkus | `java` |
  | java-spring | `java` |
  | kotlin-ktor | `kotlin` |
  | python-click-cli | `python` |
  | python-fastapi | `python` |
  | rust-axum | `rust` |
  | typescript-nestjs | `typescript` |

- **Destinations (8 files):**
  1. `tests/golden/go-gin/.github/skills/x-test-plan/SKILL.md`
  2. `tests/golden/java-quarkus/.github/skills/x-test-plan/SKILL.md`
  3. `tests/golden/java-spring/.github/skills/x-test-plan/SKILL.md`
  4. `tests/golden/kotlin-ktor/.github/skills/x-test-plan/SKILL.md`
  5. `tests/golden/python-click-cli/.github/skills/x-test-plan/SKILL.md`
  6. `tests/golden/python-fastapi/.github/skills/x-test-plan/SKILL.md`
  7. `tests/golden/rust-axum/.github/skills/x-test-plan/SKILL.md`
  8. `tests/golden/typescript-nestjs/.github/skills/x-test-plan/SKILL.md`

- **Recommended approach:** Run the pipeline for each profile to ensure full parity:
  ```bash
  for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    npx tsx src/cli.ts generate \
      --config resources/config-templates/setup-config.$profile.yaml \
      --output /tmp/golden-$profile \
      --resources resources
    cp /tmp/golden-$profile/.github/skills/x-test-plan/SKILL.md \
       tests/golden/$profile/.github/skills/x-test-plan/SKILL.md
  done
  ```
- **Note:** `java-quarkus` and `java-spring` will produce identical content (both resolve to `java`). Similarly, `python-click-cli` and `python-fastapi` both resolve to `python`.
- **Dependencies on other tasks:** T5.4
- **Estimated complexity:** S

### Verification checkpoint G6

```bash
# Verify language substitution is correct in each profile
grep 'Knowledge of go' tests/golden/go-gin/.github/skills/x-test-plan/SKILL.md
grep 'Knowledge of java' tests/golden/java-spring/.github/skills/x-test-plan/SKILL.md
grep 'Knowledge of kotlin' tests/golden/kotlin-ktor/.github/skills/x-test-plan/SKILL.md
grep 'Knowledge of python' tests/golden/python-fastapi/.github/skills/x-test-plan/SKILL.md
grep 'Knowledge of rust' tests/golden/rust-axum/.github/skills/x-test-plan/SKILL.md
grep 'Knowledge of typescript' tests/golden/typescript-nestjs/.github/skills/x-test-plan/SKILL.md
# Verify no unresolved {language_name} placeholders remain
grep -r '{language_name}' tests/golden/*/.github/skills/x-test-plan/SKILL.md  # expect: 0 matches
```

---

## G7 -- Verification

**Purpose:** Run compilation check, full test suite, and verify coverage thresholds. Since this story modifies 0 TypeScript files, compilation should be a no-op pass. The byte-for-byte integration tests validate all 24 golden files automatically.
**Dependencies:** G3, G4, G6 (all 24 golden files must be updated)

### T7.1 -- TypeScript compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors. No TypeScript files were modified.
- **Dependencies on other tasks:** G6
- **Estimated complexity:** XS

### T7.2 -- Run byte-for-byte parity tests

- **Command:** `npx vitest run tests/node/integration/byte-for-byte.test.ts`
- **Expected:** All 8 profiles pass. The `byte-for-byte.test.ts` integration test runs the pipeline for each profile and compares output against golden files.
- **Test mechanism:** `tests/node/integration/byte-for-byte.test.ts` uses `describe.sequential.each` over 8 profiles. For each profile:
  1. Loads config from `resources/config-templates/setup-config.{profile}.yaml`
  2. Runs `runPipeline()` to a temp directory
  3. Runs `verifyOutput()` comparing pipeline output against `tests/golden/{profile}/`
  4. Asserts: `pipelineSuccessForProfile`, `pipelineMatchesGoldenFiles`, `noMissingFiles`, `noExtraFiles`, `totalFilesGreaterThanZero`
- **What this validates:**
  - `.claude/skills/x-test-plan/SKILL.md` -- copied from source template, must match golden
  - `.agents/skills/x-test-plan/SKILL.md` -- mirrored from `.claude/`, must match golden
  - `.github/skills/x-test-plan/SKILL.md` -- rendered from GitHub template with placeholder resolution, must match golden
- **Dependencies on other tasks:** T7.1
- **Estimated complexity:** S

### T7.3 -- Run full test suite

- **Command:** `npm test`
- **Expected:** All 1,384+ tests pass. Zero regressions.
- **Dependencies on other tasks:** T7.2
- **Estimated complexity:** S

### T7.4 -- Verify coverage thresholds

- **Command:** `npm test -- --coverage` (or equivalent)
- **Expected:** >= 95% line coverage, >= 90% branch coverage.
- **Note:** Since no TypeScript code was modified, coverage should remain at the existing level (~99.6% lines, ~97.84% branches).
- **Dependencies on other tasks:** T7.3
- **Estimated complexity:** XS

### T7.5 -- Acceptance criteria verification

- **What to verify against story DoD:**
  - [ ] x-test-plan generates scenarios in TPP order (not by category) -- verified by reading updated template
  - [ ] Output separated into Acceptance Tests and Unit Tests (Double-Loop) -- verified by reading updated template
  - [ ] Each scenario includes dependency markers and parallelism indicators -- verified by reading field tables
  - [ ] Output format updated with AT and UT sections -- verified by reading output format template
  - [ ] Both copies updated (RULE-001) -- verified by `.claude/` + `.github/` golden file parity
  - [ ] Subagent pattern preserved (RULE-009) -- verified by T2.5 diff
  - [ ] Golden file tests updated and passing -- verified by T7.2
  - [ ] Coverage >= 95% line, >= 90% branch -- verified by T7.4
- **Dependencies on other tasks:** T7.1, T7.2, T7.3, T7.4
- **Estimated complexity:** XS

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Complexity |
|-------|---------|----------------|----------------|-------|------------|
| G1 | Template Design | 0 | 0 (read-only) | 4 | XS-M |
| G2 | Template Implementation (Claude) | 0 | 1 template | 5 | XS-M |
| G3 | Golden Files (.claude/) | 0 | 8 golden files | 1 | XS |
| G4 | Golden Files (.agents/) | 0 | 8 golden files | 1 | XS |
| G5 | GitHub Template (Dual Copy) | 0 | 1 template | 4 | XS-M |
| G6 | Golden Files (.github/) | 0 | 8 golden files | 1 | S |
| G7 | Verification | 0 | 0 | 5 | XS-S |
| **Total** | | **0 new files** | **26 modified files** | **21 tasks** | |

## Dependency Graph

```
G1: TEMPLATE DESIGN (read + design -- no file changes)
 |
 v
G2: TEMPLATE IMPLEMENTATION (modify Claude template)
 |
 +-------> G3: GOLDEN FILES .claude/ (copy to 8 profiles)
 |          |
 |          v
 |         G4: GOLDEN FILES .agents/ (copy from .claude/ to 8 profiles)
 |
 +-------> G5: GITHUB TEMPLATE (modify GitHub template)
            |
            v
           G6: GOLDEN FILES .github/ (resolve + copy to 8 profiles)
            |
            v
           G7: VERIFICATION (compile, test, coverage)
                ^
                |
           G3 + G4 (must also be complete)
```

- G1 must be done first (design phase).
- G2 depends on G1 (implements the design).
- G3 depends on G2 (copies Claude template to golden files).
- G4 depends on G3 (mirrors `.claude/` to `.agents/`).
- G5 depends on G2 (mirrors Claude changes to GitHub format).
- G6 depends on G5 (copies GitHub template to golden files with substitution).
- G7 depends on G3, G4, and G6 (all 24 golden files must be updated before running tests).

## File Inventory

### Template source files (2 modified)

| File | Action |
|------|--------|
| `resources/skills-templates/core/x-test-plan/SKILL.md` | MODIFY -- Replace Step 2, output format, quality checks |
| `resources/github-skills-templates/testing/x-test-plan.md` | MODIFY -- Mirror Double-Loop + TPP structure (condensed) |

### Golden files -- `.claude/` (8 modified)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-test-plan/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-test-plan/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-test-plan/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-test-plan/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-test-plan/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-test-plan/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-test-plan/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-test-plan/SKILL.md` |

### Golden files -- `.agents/` (8 modified, byte-for-byte identical to `.claude/`)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-test-plan/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-test-plan/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-test-plan/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-test-plan/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-test-plan/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-test-plan/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-test-plan/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-test-plan/SKILL.md` |

### Golden files -- `.github/` (8 modified, `{language_name}` resolved per profile)

| Profile | Path | `{language_name}` |
|---------|------|--------------------|
| go-gin | `tests/golden/go-gin/.github/skills/x-test-plan/SKILL.md` | `go` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-test-plan/SKILL.md` | `java` |
| java-spring | `tests/golden/java-spring/.github/skills/x-test-plan/SKILL.md` | `java` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-test-plan/SKILL.md` | `kotlin` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-test-plan/SKILL.md` | `python` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-test-plan/SKILL.md` | `python` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-test-plan/SKILL.md` | `rust` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-test-plan/SKILL.md` | `typescript` |

### Files NOT modified

| File | Reason |
|------|--------|
| `src/assembler/skills-assembler.ts` | No change -- copies core skill templates as-is |
| `src/assembler/codex-skills-assembler.ts` | No change -- mirrors `.claude/skills/` to `.agents/skills/` |
| `src/assembler/github-skills-assembler.ts` | No change -- reads template, applies placeholder resolution |
| `src/template-engine.ts` | No change -- placeholder resolution unchanged |
| `tests/node/integration/byte-for-byte.test.ts` | No change -- existing test validates golden files automatically |
| `resources/skills-templates/knowledge-packs/testing/SKILL.md` | No change -- KP entry point unaffected |
| `resources/core/03-testing-philosophy.md` | No change -- already contains TDD/TPP (story-0003-0001) |

## Key Implementation Notes

1. **RULE-009 (Subagent Preservation):** Step 1 of the Claude template (lines 34-63) must remain character-for-character identical. The subagent prompt, `allowed-tools: Read, Grep, Glob`, and 11-point return schema are NOT modified. Only Step 2 and Step 3 are changed.

2. **RULE-001 (Dual Copy Consistency):** Both the Claude template (`resources/skills-templates/core/x-test-plan/SKILL.md`) and the GitHub template (`resources/github-skills-templates/testing/x-test-plan.md`) must reflect the same structural changes. The GitHub copy is a condensed version but must contain the same Double-Loop + TPP concepts.

3. **RULE-002 (Source of Truth):** The source files in `resources/` are the source of truth. Golden files are derived outputs that must be regenerated after template changes.

4. **Placeholder handling:**
   - `.claude/` and `.agents/` golden files contain `{{LANGUAGE}}` unresolved (Nunjucks double-brace syntax, not processed by `replacePlaceholders()`).
   - `.github/` golden files have `{language_name}` resolved to the profile's language name by `TemplateEngine.replacePlaceholders()`.
   - The new TPP/Double-Loop content introduces NO new placeholders.

5. **Backward compatibility:** The output format is intentionally restructured from category-based to TPP-based. This is a non-backward-compatible change to the test plan output. The `x-lib-task-decomposer` skill consumes the architect's plan (not the test plan), so there is no parsing dependency on the old format. The test plan is consumed as free-form markdown by downstream skills.

6. **Golden file update strategy:** For `.claude/` and `.agents/`, a simple file copy suffices (all 8 profiles are identical). For `.github/`, run the pipeline per profile or manually substitute `{language_name}` -- the pipeline approach is more reliable and recommended.
