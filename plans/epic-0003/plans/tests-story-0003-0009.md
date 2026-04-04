# Test Plan — story-0003-0009

## Summary

This story modifies two Markdown skill template files to add enriched Gherkin instructions (mandatory scenario categories, TPP ordering, minimum validation, boundary value triplet pattern, and new common mistakes). No TypeScript source code changes. Testing relies on content validation of the modified source files and byte-for-byte golden file parity across all 8 profiles x 3 output directories (24 golden files).

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (Claude source) | Verify enriched Gherkin sections exist in `resources/skills-templates/core/x-story-create/SKILL.md` | YES | `tests/node/content/x-story-create-content.test.ts` |
| Content validation (GitHub source) | Verify enriched Gherkin sections exist in `resources/github-skills-templates/story/x-story-create.md` | YES | `tests/node/content/x-story-create-content.test.ts` (same file, separate describe block) |
| Dual copy consistency | Verify both sources contain semantically identical enriched content | YES | `tests/node/content/x-story-create-content.test.ts` |
| Golden file integration | Verify pipeline output matches updated golden files | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Assembler unit tests | Verify copy logic works for x-story-create | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |
| Routing tests | Verify x-story-create is in skill registry | NO (existing) | `tests/node/domain/core-kp-routing.test.ts` |

---

## 2. New Tests — Content Validation

### 2.1 File: `tests/node/content/x-story-create-content.test.ts`

This new test file validates the enriched Gherkin content in both source-of-truth templates. It follows the pattern established in `tests/node/content/refactoring-guidelines-content.test.ts`.

#### 2.1.1 Claude Source Template Validation

**Source file under test:** `resources/skills-templates/core/x-story-create/SKILL.md`

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 1 | `claudeSource_containsGherkinCompletenessPrerequisite` | The Prerequisites section includes a reference to Rule 13 (SD-02) for Gherkin completeness |
| 2 | `claudeSource_containsStoryDecompositionRulePath` | The prerequisite references `.claude/rules/13-story-decomposition.md` or `.claude/skills/story-planning/references/story-decomposition.md` (whichever path the implementation uses) |
| 3 | `claudeSource_containsDegenerateCasesCategory` | Section 7 required scenarios include "Degenerate cases" as a mandatory category |
| 4 | `claudeSource_containsHappyPathCategory` | Section 7 required scenarios include "Happy path" |
| 5 | `claudeSource_containsErrorPathsCategory` | Section 7 required scenarios include "Error paths" |
| 6 | `claudeSource_containsBoundaryValuesCategory` | Section 7 required scenarios include "Boundary values" |
| 7 | `claudeSource_containsComplexEdgeCasesCategory` | Section 7 required scenarios include "Complex edge cases" (if applicable) |
| 8 | `claudeSource_containsTPPOrderingSection` | A "Scenario Ordering (TPP)" or equivalent section header exists |
| 9 | `claudeSource_tppOrdering_degenerateFirst` | TPP ordering lists degenerate cases as item 1 (first) |
| 10 | `claudeSource_tppOrdering_happyPathSecond` | TPP ordering lists happy path after degenerate cases |
| 11 | `claudeSource_tppOrdering_errorPathsThird` | TPP ordering lists error paths after happy path |
| 12 | `claudeSource_tppOrdering_boundaryValuesFourth` | TPP ordering lists boundary values after error paths |
| 13 | `claudeSource_containsMinimumScenarioValidation` | A "Minimum Scenario Validation" section or instruction exists |
| 14 | `claudeSource_minimumScenarios_requiresAtLeast4` | The minimum validation explicitly states "at least 4" scenarios |
| 15 | `claudeSource_containsBoundaryTripletPattern` | Content mentions the triplet pattern: at-minimum, at-maximum, past-maximum (or equivalent wording) |
| 16 | `claudeSource_sizingHeuristic_tooSmall_4scenarios` | "Too small" section references "4 Gherkin scenarios" (updated from 2) |
| 17 | `claudeSource_commonMistakes_missingDegenerateCases` | Common Mistakes section includes entry about missing degenerate cases |
| 18 | `claudeSource_commonMistakes_happyPathFirstOrdering` | Common Mistakes section includes entry about happy-path-first ordering violation |
| 19 | `claudeSource_commonMistakes_boundaryWithoutTriplet` | Common Mistakes section includes entry about boundary values without triplet |
| 20 | `claudeSource_commonMistakes_fewerThan4Scenarios` | Common Mistakes section includes entry about fewer than 4 scenarios |

#### 2.1.2 GitHub Source Template Validation

**Source file under test:** `resources/github-skills-templates/story/x-story-create.md`

Same 20 tests as Section 2.1.1, but targeting the GitHub copy. Use `describe.each` or a shared test factory to avoid duplication. Each test name prefixed with `githubSource_` instead of `claudeSource_`.

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 21-40 | Same as #1-#20 but with `githubSource_` prefix | Same validations against GitHub template, with GitHub-specific path references |

**Key difference:** Test #2 should verify the GitHub copy references the GitHub-equivalent path for Rule 13 (e.g., `resources/core/13-story-decomposition.md` instead of the `.claude/` path).

#### 2.1.3 Dual Copy Consistency (RULE-001)

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 41 | `dualCopy_bothContainDegenerateCasesCategory` | Both sources contain the degenerate cases category |
| 42 | `dualCopy_bothContainTPPOrderingSection` | Both sources contain a TPP ordering section |
| 43 | `dualCopy_bothContainMinimumScenarioValidation` | Both sources contain minimum scenario validation |
| 44 | `dualCopy_bothContainBoundaryTripletPattern` | Both sources reference the triplet pattern |
| 45 | `dualCopy_bothContain5RequiredCategories` | Both sources list 5 required scenario categories (degenerate, happy, error, boundary, complex edge) |
| 46 | `dualCopy_commonMistakesCount_matchesBetweenCopies` | The number of common mistake entries is equal in both sources |
| 47 | `dualCopy_pathDifferences_onlyExpected` | The only path-level differences between the two templates are the expected ones: `.claude/` vs GitHub paths, Portuguese vs English section headers |

#### 2.1.4 Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-story-create/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/story/x-story-create.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

describe("x-story-create Claude source — enriched Gherkin", () => {
  // Tests #1-#20 here
});

describe("x-story-create GitHub source — enriched Gherkin", () => {
  // Tests #21-#40 here (mirror of Claude tests)
});

describe("x-story-create dual copy consistency (RULE-001)", () => {
  // Tests #41-#47 here
});
```

---

## 3. Existing Tests — No Changes Needed

### 3.1 Golden File Integration Tests

- **File:** `tests/node/integration/byte-for-byte.test.ts`
- **What it validates:** Pipeline output matches golden files byte-for-byte for all 8 profiles
- **How it covers this story:** After updating both source templates and copying to 24 golden files, the pipeline will produce output identical to the updated golden files
- **Expected result:** All 8 profiles pass (40 test assertions: 5 per profile)
- **Test logic unchanged:** The test infrastructure is generic and works with any content

### 3.2 Assembler Unit Tests

- **File:** `tests/node/assembler/skills-assembler.test.ts` — Tests `SkillsAssembler` copy logic for `.claude/` output
- **File:** `tests/node/assembler/codex-skills-assembler.test.ts` — Tests `CodexSkillsAssembler` mirror logic for `.agents/` output
- **File:** `tests/node/assembler/github-skills-assembler.test.ts` — Tests `GithubSkillsAssembler` copy logic for `.github/` output
- **Impact:** None — assembler logic unchanged; these tests continue to validate the copy mechanism works

### 3.3 Routing Tests

- **File:** `tests/node/domain/core-kp-routing.test.ts`
- **Impact:** None — `x-story-create` is a core skill, not a routed core rule file

---

## 4. Golden Files Requiring Update

**Total: 24 golden files** (8 profiles x 3 output directories)

### 4.1 `.claude/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-story-create/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-story-create/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-story-create/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-story-create/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-story-create/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-story-create/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-story-create/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-story-create/SKILL.md` |

### 4.2 `.agents/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-story-create/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-story-create/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-story-create/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-story-create/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-story-create/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-story-create/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-story-create/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-story-create/SKILL.md` |

### 4.3 `.github/` golden files (8 files, identical to GitHub source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.github/skills/x-story-create/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-story-create/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-story-create/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-story-create/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-story-create/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-story-create/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-story-create/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-story-create/SKILL.md` |

### 4.4 Golden File Update Strategy

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-story-create/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/story/x-story-create.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-story-create/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-story-create/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-story-create/SKILL.md"
done
```

---

## 5. TDD Execution Order

Following test-first approach:

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-story-create-content.test.ts`) with all 47 test cases | RED (tests fail because source files not yet modified) |
| 2 | Edit Claude source template (`resources/skills-templates/core/x-story-create/SKILL.md`) | Partial GREEN (Claude tests pass, GitHub tests still RED) |
| 3 | Edit GitHub source template (`resources/github-skills-templates/story/x-story-create.md`) | GREEN (all content tests pass) |
| 4 | Copy sources to 24 golden files (script from Section 4.4) | N/A (golden files updated) |
| 5 | Run byte-for-byte integration tests | GREEN (golden file parity confirmed) |
| 6 | Run full test suite (`npx vitest run`) | GREEN (all 1,384+ existing tests pass, plus ~47 new tests) |

---

## 6. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-story-create-content.test.ts` — all 47 content validation tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` — all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` — full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch (no TypeScript code changes, so coverage unaffected)
- [ ] No compiler/linter warnings introduced

---

## 7. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after source edit | Mechanical copy script (Section 4.4) eliminates drift; byte-for-byte tests catch any mismatch immediately |
| Content test too brittle (exact string matching) | Use `toContain()` for substring checks instead of exact line matching; test semantic presence, not formatting |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (#41-#47) verify both copies have equivalent enriched content |
| Path references wrong in either copy | Dedicated test (#2 for Claude, #22 for GitHub) validates the correct path reference for each ecosystem |

---

## 8. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation (Claude) | 20 | 0 |
| Content validation (GitHub) | 20 | 0 |
| Dual copy consistency | 7 | 0 |
| Golden file integration | 0 | 40 (8 profiles x 5 assertions) |
| Assembler unit tests | 0 | ~50 (across 3 assembler test files) |
| **Total** | **47** | **~90** |
