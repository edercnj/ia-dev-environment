# Task Decomposition -- STORY-0003-0002: Refactoring Guidelines for Coding Standards KP

**Status:** PENDING
**Date:** 2026-03-15
**Blocked By:** None (story-0003-0001 is a sibling, not a blocker)
**Blocks:** story-0003-0003 (Code Smells Catalog), story-0003-0006

---

## G1 -- Foundation (Create `resources/core/14-refactoring-guidelines.md`)

**Purpose:** Create the single source-of-truth content file containing all refactoring guidelines. This is the core deliverable -- a new Markdown resource file with three subsections: Refactoring Triggers, Prioritized Techniques, and Safety Rules.
**Dependencies:** None
**Compiles independently:** N/A -- pure resource file, no TypeScript changes.
**Risk:** LOW

### T1.1 -- Create `resources/core/14-refactoring-guidelines.md`

- **File:** `resources/core/14-refactoring-guidelines.md` (create)
- **What to implement:**
  1. `## Refactoring Guidelines` -- main heading
  2. `### Refactoring Triggers` -- criteria for when to refactor:
     - Function > 25 lines -> Extract Method (references existing Hard Limit)
     - Class > 250 lines -> Extract Class (references existing Hard Limit)
     - Method used once with no readability benefit -> Inline Method
     - Name does not reveal intent (CC-01) -> Rename
     - Duplicated code 3+ lines (CC-05) -> Extract shared function
     - Conditional logic growing -> Replace Conditional with Polymorphism
  3. `### Prioritized Techniques (TDD Frequency Order)` -- ordered list:
     1. Extract Method
     2. Rename Variable/Method/Class
     3. Replace Magic Number with Named Constant
     4. Extract Interface (DIP)
     5. Move Method (SRP)
     6. Replace Conditional with Polymorphism
     - Each technique must include context for when to apply
  4. `### Safety Rules` -- 5 non-negotiable rules:
     1. ALL tests must be GREEN before starting any refactoring
     2. ALL tests must remain GREEN after each refactoring step
     3. NEVER add behavior during refactoring
     4. Refactoring is a sequence of small, safe steps -- each independently reversible
     5. If any test breaks during refactoring, UNDO the last step immediately
  5. Language: English only (RULE-012)
  6. Content must NOT duplicate existing CC-01 to CC-10 or SOLID rules -- cross-reference them instead
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Verification checkpoint G1

```bash
# Verify file exists and contains required sections
test -f resources/core/14-refactoring-guidelines.md && echo "OK" || echo "MISSING"
grep -c "## Refactoring Guidelines" resources/core/14-refactoring-guidelines.md    # 1
grep -c "### Refactoring Triggers" resources/core/14-refactoring-guidelines.md     # 1
grep -c "### Prioritized Techniques" resources/core/14-refactoring-guidelines.md   # 1
grep -c "### Safety Rules" resources/core/14-refactoring-guidelines.md             # 1
```

---

## G2 -- Routing (Add route entry in `core-kp-routing.ts`)

**Purpose:** Register the new content file in the domain routing table so the assembler pipeline picks it up and copies it to `.claude/skills/coding-standards/references/` and `.agents/skills/coding-standards/references/` during generation.
**Dependencies:** G1 (source file must exist for the route to reference a real file)
**Compiles independently:** Yes
**Risk:** LOW

### T2.1 -- Add route to `CORE_TO_KP_MAPPING`

- **File:** `src/domain/core-kp-routing.ts` (modify)
- **What to implement:**
  1. Append one entry to the `CORE_TO_KP_MAPPING` array (after the `13-story-decomposition.md` line):
     ```typescript
     { sourceFile: "14-refactoring-guidelines.md", kpName: "coding-standards", destFile: "refactoring-guidelines.md" },
     ```
  2. Update the JSDoc comment from `/** 11 static routes ... */` to `/** 12 static routes ... */`
  3. The new route maps to the `coding-standards` KP, grouping it with `01-clean-code.md` and `02-solid-principles.md`
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** XS

### Verification checkpoint G2

```bash
# Verify compilation passes
npx --no-install tsc --noEmit
# Verify route count in source
grep -c "14-refactoring-guidelines.md" src/domain/core-kp-routing.ts   # 1
```

---

## G3 -- Test Updates (Fix route count assertions)

**Purpose:** Update the unit test assertions in `core-kp-routing.test.ts` to reflect the new route count (11 -> 12 static routes, and downstream active route counts).
**Dependencies:** G2 (routing change must be in place)
**Compiles independently:** Yes
**Risk:** LOW

### T3.1 -- Update `contains_11_staticRoutes` assertion

- **File:** `tests/node/domain/core-kp-routing.test.ts` (modify)
- **What to implement:**
  1. Change `toHaveLength(11)` to `toHaveLength(12)` in the `contains_11_staticRoutes` test
  2. Optionally rename the test to `contains_12_staticRoutes` for clarity
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### T3.2 -- Update `lastRoute_isStoryDecomposition` test

- **File:** `tests/node/domain/core-kp-routing.test.ts` (modify)
- **What to implement:**
  1. The new route is appended at the end of `CORE_TO_KP_MAPPING`, so it becomes the new last route
  2. **Option A (recommended):** Update the existing test to check the new last route:
     - Change index from `[10]` to `[11]`
     - Change expected values to `sourceFile: "14-refactoring-guidelines.md"`, `kpName: "coding-standards"`, `destFile: "refactoring-guidelines.md"`
     - Rename test to `lastRoute_isRefactoringGuidelines`
  3. **Option B:** Keep existing test at index `[10]` (still valid for story-decomposition) and add a new test for index `[11]`
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### T3.3 -- Update `getActiveRoutes` length assertions

- **File:** `tests/node/domain/core-kp-routing.test.ts` (modify)
- **What to implement:**
  1. `microservice_includes12Routes`: change `toHaveLength(12)` to `toHaveLength(13)` (12 static + 1 conditional)
  2. `library_excludesCloudNative_returns11Routes`: change `toHaveLength(11)` to `toHaveLength(12)` (12 static, cloud-native excluded)
  3. `monolith_includesCloudNative`: change `toHaveLength(12)` to `toHaveLength(13)` (12 static + 1 conditional)
  4. Optionally rename the test descriptions to match the new counts
- **Dependencies on other tasks:** T2.1
- **Estimated complexity:** XS

### Verification checkpoint G3

```bash
# Run routing tests only
npx vitest run tests/node/domain/core-kp-routing.test.ts
```

---

## G4 -- Golden Files (Regenerate for all 8 profiles)

**Purpose:** Regenerate golden files for all 8 profiles so byte-for-byte integration tests pass. The new route causes the pipeline to produce a `refactoring-guidelines.md` file in two output directories per profile: `.claude/skills/coding-standards/references/` and `.agents/skills/coding-standards/references/`. Total: 16 new golden files across 8 profiles.
**Dependencies:** G2 (route must be registered so the pipeline produces the new file)
**Compiles independently:** N/A -- file copies
**Risk:** MEDIUM (must regenerate ALL 8 profiles to avoid byte-for-byte test failures)

### T4.1 -- Regenerate golden files for all 8 profiles

- **Profiles:** go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs
- **What to implement:**
  1. For each profile, run the pipeline with the profile's config and copy the output to the golden directory:
     ```bash
     for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
       tmpdir=$(mktemp -d)
       npx tsx src/index.ts generate \
         --config "resources/config-templates/setup-config.${profile}.yaml" \
         --output-dir "${tmpdir}/output"
       # Copy generated output to golden directory
       rm -rf "tests/golden/${profile}"
       cp -r "${tmpdir}/output" "tests/golden/${profile}"
       rm -rf "${tmpdir}"
     done
     ```
  2. Alternatively, copy ONLY the new file to each golden directory (faster, lower risk of unintended changes):
     ```bash
     for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
       cp resources/core/14-refactoring-guidelines.md \
         "tests/golden/${profile}/.claude/skills/coding-standards/references/refactoring-guidelines.md"
       cp resources/core/14-refactoring-guidelines.md \
         "tests/golden/${profile}/.agents/skills/coding-standards/references/refactoring-guidelines.md"
     done
     ```
  3. **Recommended approach:** Use the full pipeline regeneration (option 1) to ensure byte-for-byte parity, then verify with `git diff` that only the expected new files appear and no existing files changed unexpectedly.
- **New golden files (16 total):**
  - `tests/golden/{profile}/.claude/skills/coding-standards/references/refactoring-guidelines.md` (x8)
  - `tests/golden/{profile}/.agents/skills/coding-standards/references/refactoring-guidelines.md` (x8)
- **Dependencies on other tasks:** T2.1 (route must exist), T1.1 (source file must exist)
- **Estimated complexity:** S

### Verification checkpoint G4

```bash
# Verify all 16 golden files exist
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  test -f "tests/golden/${profile}/.claude/skills/coding-standards/references/refactoring-guidelines.md" && echo "OK: ${profile} .claude" || echo "MISSING: ${profile} .claude"
  test -f "tests/golden/${profile}/.agents/skills/coding-standards/references/refactoring-guidelines.md" && echo "OK: ${profile} .agents" || echo "MISSING: ${profile} .agents"
done
```

---

## G5 -- Validation (Full test suite + coverage verification)

**Purpose:** Run the complete test suite to confirm zero regressions, byte-for-byte parity with regenerated golden files, and coverage thresholds are maintained.
**Dependencies:** G1-G4 (all changes must be in place)
**Compiles independently:** N/A (verification only)
**Risk:** LOW

### T5.1 -- Full compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors across the entire project.
- **Dependencies on other tasks:** G1-G4

### T5.2 -- Run routing unit tests

- **Command:** `npx vitest run tests/node/domain/core-kp-routing.test.ts`
- **Expected:** All assertions pass with updated counts (12 static, 13 active for microservice/monolith, 12 for library).
- **Dependencies on other tasks:** G3

### T5.3 -- Run byte-for-byte integration tests

- **Command:** `npx vitest run tests/node/integration/byte-for-byte.test.ts`
- **Expected:** All 8 profiles pass with zero mismatches, zero missing files, zero extra files.
- **Dependencies on other tasks:** G4

### T5.4 -- Run full test suite with coverage

- **Command:** `npx vitest run --coverage`
- **Expected:**
  1. All 1,384+ tests pass (no regressions)
  2. Line coverage >= 95%
  3. Branch coverage >= 90%
- **Dependencies on other tasks:** T5.1, T5.2, T5.3

### T5.5 -- Acceptance criteria verification

- **What to verify:**
  1. `resources/core/14-refactoring-guidelines.md` exists with all 3 subsections
  2. Route registered in `core-kp-routing.ts` (12 static routes)
  3. 16 golden files exist (8 profiles x 2 directories)
  4. Golden files are byte-identical to source (verified by byte-for-byte tests)
  5. Existing content preserved (no modifications to other core files)
  6. Coverage >= 95% line, >= 90% branch
  7. All tests passing, zero compiler warnings

### Verification checkpoint G5

```bash
# One-liner validation
npx tsc --noEmit && npx vitest run && echo "ALL CHECKS PASSED"
```

---

## G6 -- Commit

**Purpose:** Create atomic commits following Conventional Commits format.
**Dependencies:** G5 (all validations must pass)

### T6.1 -- Stage and commit all changes

- **What to commit:**
  1. `resources/core/14-refactoring-guidelines.md` (new)
  2. `src/domain/core-kp-routing.ts` (modified)
  3. `tests/node/domain/core-kp-routing.test.ts` (modified)
  4. 16 golden files in `tests/golden/{profile}/` (new)
- **Commit message:**
  ```
  feat(story-0003-0002): add refactoring guidelines to coding standards KP

  Add resources/core/14-refactoring-guidelines.md with refactoring triggers,
  prioritized techniques, and safety rules. Route to coding-standards KP.
  Update routing tests and regenerate golden files for all 8 profiles.
  ```
- **Dependencies on other tasks:** T5.4
- **Estimated complexity:** XS

---

## G7 -- N/A

No API, database, infrastructure, or event changes required. This story is purely additive content + routing + tests.

---

## Summary Table

| Group | Purpose | Files Created | Files Modified | Tasks | Complexity |
|-------|---------|--------------|----------------|-------|------------|
| G1 | Foundation: refactoring guidelines content | 1 resource | 0 | 1 | S |
| G2 | Routing: register new core file | 0 | 1 (`core-kp-routing.ts`) | 1 | XS |
| G3 | Test updates: fix count assertions | 0 | 1 (`core-kp-routing.test.ts`) | 3 | XS |
| G4 | Golden files: regenerate for all profiles | 16 golden files | 0 | 1 | S |
| G5 | Validation: full suite + coverage | 0 | 0 | 5 | S |
| G6 | Commit | 0 | 0 | 1 | XS |
| G7 | N/A | 0 | 0 | 0 | -- |
| **Total** | | **17 new files** | **2 modified** | **12 tasks** | |

## Dependency Graph

```
G1: FOUNDATION (resources/core/14-refactoring-guidelines.md)
  |
  v
G2: ROUTING (core-kp-routing.ts: append route, update JSDoc)
  |
  +----> G3: TEST UPDATES (core-kp-routing.test.ts: fix 5 assertions)
  |
  +----> G4: GOLDEN FILES (16 new files across 8 profiles x 2 dirs)
           |
           v
       G5: VALIDATION (compile, unit tests, integration tests, coverage)
           |
           v
       G6: COMMIT (atomic commit with all changes)
```

- G1 must be done first (creates the source content file).
- G2 depends on G1 (route references the source file).
- G3 and G4 can be done in parallel (both depend only on G2).
- G5 depends on G3 and G4 (all changes must be in place for validation).
- G6 depends on G5 (must pass all checks before committing).

## Key Implementation Notes

1. **File numbering:** Number 14 is the next available after 13 (story-decomposition). Numbers 04 and 12 are used by `git-workflow` and `cloud-native-principles` respectively -- only 04 is not routed (used directly as a rule, not a KP reference).
2. **Dual copy via pipeline:** The `.claude/` and `.agents/` copies are produced automatically by the assembler pipeline. `rules-assembler.ts` copies to `.claude/skills/coding-standards/references/` and `codex-skills-assembler.ts` mirrors to `.agents/skills/coding-standards/references/`. No `.github/` references copy is expected (confirmed by golden file inspection).
3. **No code changes to assemblers:** The `routeCoreToKps()` method in `rules-assembler.ts` iterates `getActiveRoutes()` generically. Adding the route to `CORE_TO_KP_MAPPING` is sufficient.
4. **Golden file strategy:** Full pipeline regeneration is safer than manual file copy because it catches any side effects. Verify with `git diff` that only the 16 new files appear.
5. **Content cross-references:** The refactoring guidelines reference CC-01 (naming) and CC-05 (DRY) but do NOT duplicate their content. They also reference the existing Hard Limits (25 lines, 250 lines) from Rule 03.
6. **Backward compatibility:** This is a purely additive change. No existing routes, content, or test assertions are removed -- only new content added and counts incremented.
