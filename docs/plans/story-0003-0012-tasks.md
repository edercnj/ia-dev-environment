# Task Decomposition -- STORY-0003-0012: x-dev-implement -- Red-Green-Refactor Implementation

**Status:** PENDING
**Date:** 2026-03-15
**Story:** story-0003-0012
**Blocked By:** story-0003-0006 (DONE), story-0003-0007 (DONE), story-0003-0008 (DONE)
**Blocks:** story-0003-0014

---

## G1 -- Foundation (Read and Understand)

**Purpose:** Read and analyze the current `x-dev-implement` templates (Claude and GitHub copies), understand the 4-step structure to be restructured, and verify that dependency stories (0003-0006, 0003-0007, 0003-0008) are already merged so this story can safely reference their outputs (TDD agent workflows, TPP test plan, TDD task decomposition).
**Dependencies:** None
**Compiles independently:** N/A -- research only, no file changes.

### T1.1 -- Read current Claude template

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (read-only)
- **What to analyze:**
  - YAML frontmatter (lines 1-6): `description` text to be updated
  - Global Output Policy (lines 8-12): PRESERVED unchanged
  - Title + When to Use table (lines 14-23): PRESERVED unchanged
  - Execution Flow (lines 25-31): Step 2 and 3 descriptions to be updated
  - Step 1: Prepare + Understand -- subagent prompt (lines 33+): subagent steps to be EXTENDED (new Step 2 for test plan reading), NOT replaced (RULE-009)
  - Step 2: Implement (current): TO BE REPLACED entirely by TDD Loop
  - Step 3: Test + Validate (current): TO BE RESTRUCTURED as TDD-specific validation
  - Step 4: Commit (current): TO BE UPDATED for atomic TDD commits
  - Integration Notes: TO BE UPDATED with x-test-plan prerequisite
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.2 -- Read current GitHub template

- **File:** `resources/github-skills-templates/dev/x-dev-implement.md` (read-only)
- **What to analyze:**
  - YAML frontmatter: `description` block scalar to be updated
  - Same 4-step structure as Claude copy but condensed format
  - Detailed References section: to be EXTENDED with x-test-plan reference
  - Identify all `{single_brace}` and `{{DOUBLE_BRACE}}` placeholders for preservation
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.3 -- Verify dependency stories are complete

- **What to verify:**
  - story-0003-0006 (Agents TDD Workflows): typescript-developer agent already has TDD workflow section
  - story-0003-0007 (x-test-plan TPP): x-test-plan already generates Double-Loop + TPP ordered test plans
  - story-0003-0008 (x-lib-task-decomposer TDD): task decomposer already supports TDD task structure
- **Files to check (read-only):**
  - `resources/agents-templates/developers/typescript-developer.md` -- confirm `## TDD Workflow` section exists
  - `resources/skills-templates/core/x-test-plan/SKILL.md` -- confirm TPP/Double-Loop output format exists
- **Dependencies on other tasks:** None
- **Estimated complexity:** XS

### T1.4 -- Identify all placeholders in both templates

- **Action:** Catalog all `{{PLACEHOLDER}}` occurrences in the Claude template and all `{placeholder}` + `{{PLACEHOLDER}}` occurrences in the GitHub template.
- **Purpose:** Ensure no placeholder is accidentally removed or corrupted during template modification.
- **Key placeholders to preserve:**
  - `{{COMPILE_COMMAND}}`, `{{TEST_COMMAND}}`, `{{COVERAGE_COMMAND}}`, `{{FRAMEWORK}}`, `{{LANGUAGE}}`
- **Dependencies on other tasks:** T1.1, T1.2
- **Estimated complexity:** XS

### Verification checkpoint G1

No file changes. Design review complete. All current content understood, placeholders cataloged, dependencies verified.

---

## G2 -- Core Template Changes (Claude/Agents Copy)

**Purpose:** Modify the primary Claude skill template to restructure the 4-step flow from implementation-first to TDD (Red-Green-Refactor) workflow. This is the source of truth (RULE-002). The `.claude/` and `.agents/` golden files are direct copies of this template.
**Dependencies:** G1 (analysis must be complete)
**Compiles independently:** N/A -- markdown template file, no TypeScript changes.

### T2.1 -- Update YAML frontmatter description

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (modify)
- **What to implement:**
  - Replace the `description` field to reference TDD workflow, Double-Loop TDD, and test plan KPs
  - **BEFORE:** `"Implements a feature/story following project conventions. Delegates preparation to a subagent that reads architecture and coding KPs, then implements layer-by-layer with intermediate compilation checks."`
  - **AFTER:** `"Implements a feature/story using TDD (Red-Green-Refactor) workflow. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements test-first with Double-Loop TDD, layer-by-layer with compile checks after each cycle."`
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** XS

### T2.2 -- Update Execution Flow overview

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (modify)
- **What to implement:**
  - Update the 4-step flow description text:
    - Step 1: Add "+ test plan" to subagent description, add "TDD implementation plan" output
    - Step 2: Change from "IMPLEMENT -> Orchestrator writes code layer-by-layer" to "TDD LOOP -> For each scenario (TPP order): RED -> GREEN -> REFACTOR -> compile check"
    - Step 3: Change from "TEST + VALIDATE" to "VALIDATE -> Coverage thresholds, all acceptance tests GREEN"
    - Step 4: Change from generic commits to "Atomic TDD commits: one per Red-Green-Refactor cycle"
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### T2.3 -- Extend Step 1 subagent prompt with test plan reading

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (modify)
- **What to implement:**
  - Insert a new Step 2 within the subagent prompt (between current Steps 1 and 2, pushing subsequent steps down by 1):
    - Read test plan (MANDATORY): look for test plan file, extract AT-N, UT-N (TPP order), IT-N
    - Identify outer loop (acceptance tests) and inner loop (unit tests in TPP sequence)
    - If NO test plan found: emit WARNING and suggest running `/x-test-plan` first
  - Update the final "Produce implementation plan" step with TDD-specific items:
    - TDD cycle mapping: which UT-N scenarios apply to which layer
    - Acceptance test identification: AT-N entries
  - Add Fallback Mode warning block (RULE-003 backward compatibility)
- **Key constraint:** Existing subagent steps are EXTENDED, not replaced. The `allowed-tools`, KP reading steps, and return schema structure are preserved (RULE-009).
- **Preservation check:** Verify all existing subagent steps remain intact (content-preserved, only renumbered).
- **Dependencies on other tasks:** T1.1, T1.4
- **Estimated complexity:** M

### T2.4 -- Replace Step 2 with TDD Loop

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (modify)
- **What to implement:**
  - Replace the entire current Step 2 ("Implement -- Orchestrator Inline") with the new TDD Loop structure:
    - **2.0 Write Acceptance Test First (Double-Loop -- Outer Loop):** Write AT-N, run it (must be RED), stays RED through inner loop
    - **2.1 Inner Loop: Red-Green-Refactor per Unit Test (TPP Order):**
      - RED: Write failing test for UT-N, verify it fails
      - GREEN: Implement minimum production code, respect layer order, verify all tests pass
      - REFACTOR: Evaluate extract method/DRY/naming, verify tests still pass
      - Compile Check: `{{COMPILE_COMMAND}}` after each cycle
    - **2.2 Cycle Completion:** Run AT-N after all related UT-N cycles, should now be GREEN
    - **2.3 Code Conventions:** Named constants, method/class limits, self-documenting code, no null returns, constructor injection, immutable DTOs (moved from old Step 2)
    - **2.4 Fallback Mode:** Layer-by-layer implementation when no test plan, test-with instead of test-first
  - All `{{TEST_COMMAND}}` and `{{COMPILE_COMMAND}}` placeholders must appear in the correct code blocks
- **Dependencies on other tasks:** T2.3
- **Estimated complexity:** L

### T2.5 -- Update Step 3 (Validate) with TDD-specific DoD

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (modify)
- **What to implement:**
  - Replace the generic "Test + Validate" content with TDD-specific validation
  - Add Definition of Done table with criteria:
    - All acceptance tests (AT-N) GREEN
    - All unit tests (UT-N) GREEN
    - Line coverage >= 95%, Branch coverage >= 90%
    - Code compiles cleanly (`{{COMPILE_COMMAND}}`)
    - Tests written BEFORE implementation (verify test-first per cycle)
    - Refactoring evaluated per cycle
    - Thread-safe (if applicable)
  - Preserve `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}` placeholders
- **Dependencies on other tasks:** T2.4
- **Estimated complexity:** S

### T2.6 -- Update Step 4 (Commit) with atomic TDD commits

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (modify)
- **What to implement:**
  - Replace generic commit instructions with TDD-specific atomic commit pattern:
    - Per TDD cycle: `git add [test + impl]`, commit with RED/GREEN/REFACTOR details
    - For acceptance tests: separate commit when AT turns GREEN
    - Commit ordering reflects TDD progression: AT commit (RED) first, UT+impl commits in TPP order, AT GREEN commit last
  - Commit message format follows Conventional Commits with TDD annotations
- **Dependencies on other tasks:** T2.5
- **Estimated complexity:** S

### T2.7 -- Update Integration Notes

- **File:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (modify)
- **What to implement:**
  - Add x-test-plan as prerequisite reference
  - Add note that developer agent already includes TDD workflow rules (story-0003-0006)
  - Preserve existing integration notes about x-dev-lifecycle, x-test-run, x-git-push
- **Dependencies on other tasks:** T2.6
- **Estimated complexity:** XS

### T2.8 -- Verify placeholder preservation and subagent integrity

- **Action:** After all edits, verify:
  1. All `{{PLACEHOLDER}}` occurrences from T1.4 catalog are still present
  2. Subagent prompt in Step 1 retains all original KP reading steps (renumbered but content-preserved)
  3. `allowed-tools: Read, Write, Edit, Bash, Grep, Glob` unchanged in frontmatter
  4. `argument-hint` unchanged
- **Verification command:**
  ```bash
  grep -c '{{COMPILE_COMMAND}}' resources/skills-templates/core/x-dev-implement/SKILL.md
  grep -c '{{TEST_COMMAND}}' resources/skills-templates/core/x-dev-implement/SKILL.md
  grep -c '{{COVERAGE_COMMAND}}' resources/skills-templates/core/x-dev-implement/SKILL.md
  grep -c '{{FRAMEWORK}}' resources/skills-templates/core/x-dev-implement/SKILL.md
  ```
- **Dependencies on other tasks:** T2.1 through T2.7
- **Estimated complexity:** XS

### Verification checkpoint G2

```bash
# Verify template is well-formed markdown
head -6 resources/skills-templates/core/x-dev-implement/SKILL.md  # YAML frontmatter intact
# Verify key TDD sections exist
grep 'TDD Loop' resources/skills-templates/core/x-dev-implement/SKILL.md
grep 'Double-Loop' resources/skills-templates/core/x-dev-implement/SKILL.md
grep 'Red-Green-Refactor' resources/skills-templates/core/x-dev-implement/SKILL.md
grep 'Fallback Mode' resources/skills-templates/core/x-dev-implement/SKILL.md
```

---

## G3 -- Dual Copy (GitHub Template)

**Purpose:** Update the GitHub skills template to mirror the TDD restructuring from G2 in a condensed format suitable for GitHub Copilot. Verify structural parity with Claude copy (RULE-001).
**Dependencies:** G2 (Claude template must be finalized to mirror from)
**Compiles independently:** N/A -- markdown template file.

### T3.1 -- Update YAML frontmatter description

- **File:** `resources/github-skills-templates/dev/x-dev-implement.md` (modify)
- **What to implement:**
  - Update the `description` block scalar to reference TDD workflow, Double-Loop, and test plan
  - Mirror the semantic content from T2.1 but in GitHub's block scalar YAML format
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### T3.2 -- Update Execution Flow overview

- **File:** `resources/github-skills-templates/dev/x-dev-implement.md` (modify)
- **What to implement:**
  - Mirror the same 4-step flow description changes from T2.2
- **Dependencies on other tasks:** T2.2
- **Estimated complexity:** XS

### T3.3 -- Extend Step 1 subagent prompt with test plan reading

- **File:** `resources/github-skills-templates/dev/x-dev-implement.md` (modify)
- **What to implement:**
  - Mirror test plan reading step from T2.3 but with GitHub-specific paths (`.github/skills/` instead of `skills/`)
  - Include fallback warning
  - Condensed format appropriate for GitHub Copilot
- **Dependencies on other tasks:** T2.3
- **Estimated complexity:** M

### T3.4 -- Replace Step 2 with TDD Loop (condensed)

- **File:** `resources/github-skills-templates/dev/x-dev-implement.md` (modify)
- **What to implement:**
  - Condensed version of T2.4 TDD Loop, preserving key structure:
    - 2.0 Acceptance Test First (Double-Loop)
    - 2.1 Inner Loop (Red-Green-Refactor per UT with TPP ordering)
    - 2.2 Cycle Completion
    - 2.3 Code Conventions
    - 2.4 Fallback Mode
  - All `{{TEST_COMMAND}}` and `{{COMPILE_COMMAND}}` placeholders preserved
- **Dependencies on other tasks:** T2.4
- **Estimated complexity:** M

### T3.5 -- Update Steps 3-4 (Validate and Commit)

- **File:** `resources/github-skills-templates/dev/x-dev-implement.md` (modify)
- **What to implement:**
  - Mirror TDD-specific validation from T2.5 (condensed DoD table)
  - Mirror atomic TDD commit pattern from T2.6
- **Dependencies on other tasks:** T2.5, T2.6
- **Estimated complexity:** S

### T3.6 -- Update Integration Notes and Detailed References

- **File:** `resources/github-skills-templates/dev/x-dev-implement.md` (modify)
- **What to implement:**
  - Mirror integration notes from T2.7
  - Extend Detailed References section with `.github/skills/x-test-plan/SKILL.md` reference
- **Dependencies on other tasks:** T2.7
- **Estimated complexity:** XS

### T3.7 -- Verify structural parity with Claude copy (RULE-001)

- **Action:** After all edits, verify structural parity:
  1. Both templates have the same 4 step names (Prepare, TDD Loop, Validate, Commit)
  2. Both templates reference test plan as mandatory input
  3. Both templates include fallback warning
  4. Both templates describe Red-Green-Refactor cycle
  5. Both templates describe acceptance test first (Double-Loop)
  6. Both templates describe atomic TDD commits
  7. All `{{PLACEHOLDER}}` occurrences preserved
- **Permitted differences:** Global Output Policy (Claude only), KP path references, Detailed References section (GitHub only), verbosity level
- **Dependencies on other tasks:** T3.1 through T3.6
- **Estimated complexity:** XS

### Verification checkpoint G3

```bash
# Verify key TDD sections exist in GitHub copy
grep 'TDD Loop' resources/github-skills-templates/dev/x-dev-implement.md
grep 'Double-Loop' resources/github-skills-templates/dev/x-dev-implement.md
grep 'Red-Green-Refactor' resources/github-skills-templates/dev/x-dev-implement.md
grep 'Fallback Mode' resources/github-skills-templates/dev/x-dev-implement.md
# Verify no unresolved {single_brace} placeholders were accidentally introduced
grep -c '{language_name}' resources/github-skills-templates/dev/x-dev-implement.md
```

---

## G4 -- Golden Files (.claude/ and .agents/)

**Purpose:** Copy the updated Claude template to all 8 `.claude/` golden file paths and all 8 `.agents/` golden file paths. Since these golden files contain the template as-is (no placeholder resolution -- `{{LANGUAGE}}` stays unresolved), all 8 profiles receive identical content. The `.agents/` copies are byte-for-byte mirrors of `.claude/` (produced by `CodexSkillsAssembler`).
**Dependencies:** G2 (Claude template must be finalized)
**Compiles independently:** N/A -- markdown files.

### T4.1 -- Copy template to all 8 `.claude/` golden paths

- **Source:** `resources/skills-templates/core/x-dev-implement/SKILL.md`
- **Destinations (8 files, all identical):**
  1. `tests/golden/go-gin/.claude/skills/x-dev-implement/SKILL.md`
  2. `tests/golden/java-quarkus/.claude/skills/x-dev-implement/SKILL.md`
  3. `tests/golden/java-spring/.claude/skills/x-dev-implement/SKILL.md`
  4. `tests/golden/kotlin-ktor/.claude/skills/x-dev-implement/SKILL.md`
  5. `tests/golden/python-click-cli/.claude/skills/x-dev-implement/SKILL.md`
  6. `tests/golden/python-fastapi/.claude/skills/x-dev-implement/SKILL.md`
  7. `tests/golden/rust-axum/.claude/skills/x-dev-implement/SKILL.md`
  8. `tests/golden/typescript-nestjs/.claude/skills/x-dev-implement/SKILL.md`
- **Method:** Byte-for-byte copy (`cp`). All 8 files are identical.
- **Verification:**
  ```bash
  for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    diff resources/skills-templates/core/x-dev-implement/SKILL.md \
         tests/golden/$p/.claude/skills/x-dev-implement/SKILL.md
  done
  ```
- **Dependencies on other tasks:** T2.8
- **Estimated complexity:** XS

### T4.2 -- Copy template to all 8 `.agents/` golden paths

- **Source:** `resources/skills-templates/core/x-dev-implement/SKILL.md` (or any `.claude/` golden file, since all are identical)
- **Destinations (8 files, all identical, byte-for-byte match with `.claude/`):**
  1. `tests/golden/go-gin/.agents/skills/x-dev-implement/SKILL.md`
  2. `tests/golden/java-quarkus/.agents/skills/x-dev-implement/SKILL.md`
  3. `tests/golden/java-spring/.agents/skills/x-dev-implement/SKILL.md`
  4. `tests/golden/kotlin-ktor/.agents/skills/x-dev-implement/SKILL.md`
  5. `tests/golden/python-click-cli/.agents/skills/x-dev-implement/SKILL.md`
  6. `tests/golden/python-fastapi/.agents/skills/x-dev-implement/SKILL.md`
  7. `tests/golden/rust-axum/.agents/skills/x-dev-implement/SKILL.md`
  8. `tests/golden/typescript-nestjs/.agents/skills/x-dev-implement/SKILL.md`
- **Method:** Byte-for-byte copy. All 8 files identical to `.claude/` counterparts.
- **Verification:**
  ```bash
  for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    diff tests/golden/$p/.claude/skills/x-dev-implement/SKILL.md \
         tests/golden/$p/.agents/skills/x-dev-implement/SKILL.md
  done
  ```
- **Dependencies on other tasks:** T4.1
- **Estimated complexity:** XS

### Verification checkpoint G4

```bash
# All 16 .claude/ and .agents/ golden files must match the source template
for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  diff resources/skills-templates/core/x-dev-implement/SKILL.md \
       tests/golden/$p/.claude/skills/x-dev-implement/SKILL.md
  diff resources/skills-templates/core/x-dev-implement/SKILL.md \
       tests/golden/$p/.agents/skills/x-dev-implement/SKILL.md
done
# Expected: zero differences for all 16 comparisons
```

---

## G5 -- Golden Files (.github/)

**Purpose:** Update all 8 `.github/` golden files with the modified GitHub template content. Since the `x-dev-implement` GitHub template contains no `{single_brace}` placeholders that vary per profile (only `{{DOUBLE_BRACE}}` which are preserved), all 8 profiles produce identical output.
**Dependencies:** G3 (GitHub template must be finalized)
**Compiles independently:** N/A -- markdown files.

### T5.1 -- Update `.github/` golden files for all 8 profiles

- **Source:** `resources/github-skills-templates/dev/x-dev-implement.md`
- **Method:** Either (a) run the pipeline for each profile and copy the output, or (b) manually copy since no profile-specific `{placeholder}` substitution occurs for this template.
- **Recommended approach:** Run the pipeline for reliability:
  ```bash
  for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    npx tsx src/cli.ts generate \
      --config resources/config-templates/setup-config.$profile.yaml \
      --output /tmp/golden-$profile \
      --resources resources
    cp /tmp/golden-$profile/.github/skills/x-dev-implement/SKILL.md \
       tests/golden/$profile/.github/skills/x-dev-implement/SKILL.md
  done
  ```
- **Destinations (8 files, all identical):**
  1. `tests/golden/go-gin/.github/skills/x-dev-implement/SKILL.md`
  2. `tests/golden/java-quarkus/.github/skills/x-dev-implement/SKILL.md`
  3. `tests/golden/java-spring/.github/skills/x-dev-implement/SKILL.md`
  4. `tests/golden/kotlin-ktor/.github/skills/x-dev-implement/SKILL.md`
  5. `tests/golden/python-click-cli/.github/skills/x-dev-implement/SKILL.md`
  6. `tests/golden/python-fastapi/.github/skills/x-dev-implement/SKILL.md`
  7. `tests/golden/rust-axum/.github/skills/x-dev-implement/SKILL.md`
  8. `tests/golden/typescript-nestjs/.github/skills/x-dev-implement/SKILL.md`
- **Note:** All 8 profiles produce identical content because the template has no profile-specific `{placeholder}` substitution.
- **Dependencies on other tasks:** T3.7
- **Estimated complexity:** S

### Verification checkpoint G5

```bash
# Verify all 8 .github/ golden files are identical to each other
for p in java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  diff tests/golden/go-gin/.github/skills/x-dev-implement/SKILL.md \
       tests/golden/$p/.github/skills/x-dev-implement/SKILL.md
done
# Verify no unresolved {single_brace} placeholders remain
grep -r '{language_name}' tests/golden/*/.github/skills/x-dev-implement/SKILL.md  # expect: 0 matches
```

---

## G6 -- Test Validation

**Purpose:** Run compilation check, byte-for-byte integration tests, and full test suite to validate that all 24 golden files match pipeline output. Since this story modifies 0 TypeScript files, no new test files are created -- the existing `byte-for-byte.test.ts` automatically validates golden file parity.
**Dependencies:** G4, G5 (all 24 golden files must be updated)

### T6.1 -- TypeScript compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors. No TypeScript files were modified.
- **Dependencies on other tasks:** G4, G5
- **Estimated complexity:** XS

### T6.2 -- Run byte-for-byte parity tests

- **Command:** `npx vitest run tests/node/integration/byte-for-byte.test.ts`
- **Expected:** All 8 profiles pass. The `byte-for-byte.test.ts` integration test runs the pipeline for each profile and compares output against golden files.
- **Test mechanism:** `tests/node/integration/byte-for-byte.test.ts` uses `describe.sequential.each` over 8 profiles. For each profile:
  1. Loads config from `resources/config-templates/setup-config.{profile}.yaml`
  2. Runs `runPipeline()` to a temp directory
  3. Runs `verifyOutput()` comparing pipeline output against `tests/golden/{profile}/`
  4. Asserts: `pipelineSuccessForProfile`, `pipelineMatchesGoldenFiles`, `noMissingFiles`, `noExtraFiles`, `totalFilesGreaterThanZero`
- **What this validates for this story:**
  - `.claude/skills/x-dev-implement/SKILL.md` -- copied from source template, must match golden
  - `.agents/skills/x-dev-implement/SKILL.md` -- mirrored from `.claude/`, must match golden
  - `.github/skills/x-dev-implement/SKILL.md` -- rendered from GitHub template, must match golden
- **Dependencies on other tasks:** T6.1
- **Estimated complexity:** S

### T6.3 -- Run full test suite

- **Command:** `npm test`
- **Expected:** All 1,384+ tests pass. Zero regressions.
- **Dependencies on other tasks:** T6.2
- **Estimated complexity:** S

### Verification checkpoint G6

```bash
npx tsc --noEmit && npx vitest run tests/node/integration/byte-for-byte.test.ts && npm test
```

**Expected outcome:** Zero compilation errors, all byte-for-byte tests pass, full test suite passes.

---

## G7 -- Documentation and Final Verification

**Purpose:** Verify coverage thresholds, validate all acceptance criteria from the story, and verify cross-cutting rules compliance.
**Dependencies:** G6 (all tests must be passing)

### T7.1 -- Verify coverage thresholds

- **Command:** `npm test -- --coverage`
- **Expected:** >= 95% line coverage, >= 90% branch coverage.
- **Note:** Since no TypeScript code was modified, coverage should remain at the existing level (~99.6% lines, ~97.84% branches).
- **Dependencies on other tasks:** T6.3
- **Estimated complexity:** XS

### T7.2 -- Validate acceptance criteria against story DoD

- **What to verify against story-0003-0012 DoD:**

| # | Acceptance Criterion | Validation Method |
|---|---------------------|-------------------|
| AC-1 | Step 2 restructured for TDD Loop (Red-Green-Refactor) | Read updated template, verify TDD Loop section |
| AC-2 | Test plan is input obrigatorio (with fallback warning) | Read Step 1 subagent prompt, verify test plan reading step + fallback |
| AC-3 | Acceptance test written first (Double-Loop) | Read Step 2.0, verify AT-first instruction |
| AC-4 | Layer order preserved within each cycle | Read Step 2.1 GREEN phase, verify layer order instruction |
| AC-5 | Compile check after each cycle | Read Step 2.1, verify `{{COMPILE_COMMAND}}` after each cycle |
| AC-6 | Atomic commits per cycle | Read Step 4, verify per-cycle commit pattern |
| AC-7 | Both copies updated (RULE-001) | Structural parity verified in T3.7 |
| AC-8 | Golden file tests updated and passing | byte-for-byte tests passing in T6.2 |
| AC-9 | Coverage >= 95% line, >= 90% branch | Coverage report in T7.1 |
| AC-10 | Backward compatibility -- fallback mode (RULE-003) | Read Step 2.4, verify fallback when no test plan |
| AC-11 | Subagent pattern preserved (RULE-009) | Verified in T2.8 |

- **Dependencies on other tasks:** T7.1
- **Estimated complexity:** XS

### Verification checkpoint G7

All acceptance criteria validated. Story ready for review.

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Complexity Range |
|-------|---------|----------------|----------------|-------|-----------------|
| G1 | Foundation (read + analyze) | 0 | 0 (read-only) | 4 | XS-S |
| G2 | Core Template Changes (Claude) | 0 | 1 template | 8 | XS-L |
| G3 | Dual Copy (GitHub template) | 0 | 1 template | 7 | XS-M |
| G4 | Golden Files (.claude/ + .agents/) | 0 | 16 golden files | 2 | XS |
| G5 | Golden Files (.github/) | 0 | 8 golden files | 1 | S |
| G6 | Test Validation | 0 | 0 | 3 | XS-S |
| G7 | Documentation + Final Verification | 0 | 0 | 2 | XS |
| **Total** | | **0 new files** | **26 modified files** | **27 tasks** | |

## Dependency Graph

```
G1: FOUNDATION (read + analyze -- no file changes)
 |
 v
G2: CORE TEMPLATE CHANGES (modify Claude template)
 |
 +-------> G3: DUAL COPY (modify GitHub template)
 |          |
 |          v
 |         G5: GOLDEN FILES .github/ (copy/generate to 8 profiles)
 |
 +-------> G4: GOLDEN FILES .claude/ + .agents/ (copy to 16 files)
            |
            v
           G6: TEST VALIDATION (compile, byte-for-byte, full suite)
            ^
            |
           G5 (must also be complete)
            |
            v
           G7: DOCUMENTATION + FINAL VERIFICATION (coverage, DoD)
```

- G1 must be done first (analysis phase).
- G2 depends on G1 (implements the Claude template changes).
- G3 depends on G2 (mirrors Claude changes to GitHub format).
- G4 depends on G2 (copies Claude template to 16 golden files).
- G5 depends on G3 (copies/generates GitHub template to 8 golden files).
- G6 depends on G4 AND G5 (all 24 golden files must be updated before running tests).
- G7 depends on G6 (tests must pass before final verification).

**Parallelism:** G3 and G4 can execute in parallel after G2 completes (they modify independent file sets). G5 depends on G3 but is independent of G4. G6 waits for both G4 and G5.

## File Inventory

### Template source files (2 modified)

| File | Action | Group |
|------|--------|-------|
| `resources/skills-templates/core/x-dev-implement/SKILL.md` | MODIFY -- Restructure for TDD workflow | G2 |
| `resources/github-skills-templates/dev/x-dev-implement.md` | MODIFY -- Mirror TDD structure (condensed) | G3 |

### Golden files -- `.claude/` (8 modified, all identical)

| Profile | Path | Group |
|---------|------|-------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-implement/SKILL.md` | G4 |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-implement/SKILL.md` | G4 |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-implement/SKILL.md` | G4 |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-implement/SKILL.md` | G4 |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-implement/SKILL.md` | G4 |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-implement/SKILL.md` | G4 |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-implement/SKILL.md` | G4 |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-implement/SKILL.md` | G4 |

### Golden files -- `.agents/` (8 modified, byte-for-byte identical to `.claude/`)

| Profile | Path | Group |
|---------|------|-------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-dev-implement/SKILL.md` | G4 |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-dev-implement/SKILL.md` | G4 |
| java-spring | `tests/golden/java-spring/.agents/skills/x-dev-implement/SKILL.md` | G4 |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-dev-implement/SKILL.md` | G4 |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-dev-implement/SKILL.md` | G4 |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-dev-implement/SKILL.md` | G4 |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-dev-implement/SKILL.md` | G4 |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-dev-implement/SKILL.md` | G4 |

### Golden files -- `.github/` (8 modified, all identical)

| Profile | Path | Group |
|---------|------|-------|
| go-gin | `tests/golden/go-gin/.github/skills/x-dev-implement/SKILL.md` | G5 |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-dev-implement/SKILL.md` | G5 |
| java-spring | `tests/golden/java-spring/.github/skills/x-dev-implement/SKILL.md` | G5 |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-dev-implement/SKILL.md` | G5 |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-dev-implement/SKILL.md` | G5 |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-dev-implement/SKILL.md` | G5 |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-dev-implement/SKILL.md` | G5 |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-dev-implement/SKILL.md` | G5 |

### Files NOT modified

| File | Reason |
|------|--------|
| `src/assembler/skills-assembler.ts` | No change -- copies core skill templates as-is |
| `src/assembler/codex-skills-assembler.ts` | No change -- mirrors `.claude/skills/` to `.agents/skills/` |
| `src/assembler/github-skills-assembler.ts` | No change -- reads template, applies placeholder resolution |
| `src/template-engine.ts` | No change -- placeholder resolution logic unaffected |
| `resources/skills-templates/core/x-test-plan/SKILL.md` | No change -- already restructured for TDD (story-0003-0007) |
| `resources/agents-templates/developers/typescript-developer.md` | No change -- already has TDD workflow (story-0003-0006) |
| `tests/node/integration/byte-for-byte.test.ts` | No change -- existing parity test validates golden files automatically |

## Key Implementation Notes

1. **RULE-009 (Subagent Preservation):** Step 1 subagent prompt is EXTENDED (new step for test plan reading), NOT replaced. All existing KP reading steps, `allowed-tools`, and return schema are preserved character-for-character (only renumbered). No new subagents are introduced.

2. **RULE-001 (Dual Copy Consistency):** Both templates (Claude and GitHub) must have the same 4-step structure (Prepare, TDD Loop, Validate, Commit), the same TDD concepts (Double-Loop, TPP, Red-Green-Refactor), the same fallback warning, and the same atomic commit pattern. Permitted differences: verbosity, KP path references, Global Output Policy, Detailed References section.

3. **RULE-002 (Source of Truth):** The source files in `resources/` are the source of truth. Golden files are derived outputs that must match pipeline-generated content.

4. **RULE-003 (Backward Compatibility):** Fallback mode ensures backward compatibility when no test plan exists: the skill degrades to the original layer-by-layer approach with a warning.

5. **Placeholder handling:**
   - `.claude/` and `.agents/` golden files contain `{{PLACEHOLDER}}` unresolved (preserved as-is by `SkillsAssembler`).
   - `.github/` golden files have `{single_brace}` resolved by `TemplateEngine.replacePlaceholders()`. For this template, no `{single_brace}` placeholders vary per profile, so all 8 outputs are identical.
   - The new TDD content introduces NO new placeholders beyond `{{TEST_COMMAND}}`, `{{COMPILE_COMMAND}}`, and `{{COVERAGE_COMMAND}}` which already exist.

6. **Golden file update strategy:** For `.claude/` and `.agents/`, a simple file copy suffices (all 8 profiles are identical). For `.github/`, running the pipeline per profile is more reliable but a direct copy also works since no profile-specific substitution occurs.

## Commit Strategy (RULE-008: Atomic TDD Commits)

| Commit | Content | Type |
|--------|---------|------|
| 1 | G2: Restructure x-dev-implement Claude template for TDD workflow | `feat:` |
| 2 | G3: Mirror TDD restructuring to GitHub x-dev-implement template | `feat:` |
| 3 | G4 + G5: Update all 24 golden files for x-dev-implement | `test:` |
| 4 | G6 + G7: Verify full suite passes, coverage thresholds met | N/A (verification only) |
