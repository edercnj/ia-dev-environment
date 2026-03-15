# Test Plan -- STORY-0003-0010: x-story-epic -- DoD with TDD Criteria

## Summary

- Affected template files: 2 (`resources/skills-templates/core/x-story-epic/SKILL.md`, `resources/github-skills-templates/story/x-story-epic.md`)
- Affected golden files: 24 (8 profiles x 3 targets: `.claude/`, `.agents/`, `.github/`)
- TypeScript source changes: 0
- Total test methods: 29 (8 existing golden file integration + 13 content validation + 8 dual copy consistency)
- Categories: Golden File Integration (8), Content Validation (13), Dual Copy Consistency (8)
- Coverage targets: >= 95% line, >= 90% branch (maintained -- no new production code)

---

## 1. Test File Locations and Naming

### Existing (unchanged)

**Path:** `tests/node/integration/byte-for-byte.test.ts`

**Rationale:** The existing byte-for-byte test suite (`describe.sequential.each` over all 8 `CONFIG_PROFILES`) automatically validates that pipeline output matches golden files. No code changes to this file -- only 24 golden files are updated to reflect the new TDD DoD items and TDD rules extraction instructions.

### Planned (not part of this PR)

**Planned path:** `tests/node/content/x-story-epic-tdd-dod.test.ts` (to be added in a follow-up change)

**Rationale:** Future content validation tests that will verify the structural integrity and semantic correctness of the TDD additions to the x-story-epic skill. This PR only updates golden files; the content validation test file itself will be introduced separately.

**Naming convention (for the future test file):** `[sectionUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Source File Paths

```typescript
import { resolve, dirname } from "node:path";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, it, expect, beforeAll } from "vitest";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

const CLAUDE_TEMPLATE = resolve(RESOURCES_DIR, "skills-templates", "core", "x-story-epic", "SKILL.md");
const GITHUB_TEMPLATE = resolve(RESOURCES_DIR, "github-skills-templates", "story", "x-story-epic.md");
```

### 2.2 Content Loading

```typescript
let claudeContent: string;
let githubContent: string;

beforeAll(() => {
  claudeContent = readFileSync(CLAUDE_TEMPLATE, "utf-8");
  githubContent = readFileSync(GITHUB_TEMPLATE, "utf-8");
});
```

### 2.3 Golden File Paths

The pipeline copies the Claude template to two locations per profile (identical content):

1. `{profile}/.claude/skills/x-story-epic/SKILL.md`
2. `{profile}/.agents/skills/x-story-epic/SKILL.md`

The GitHub template is processed by `GithubSkillsAssembler`, producing:

3. `{profile}/.github/skills/x-story-epic/SKILL.md`

Golden files live at `tests/golden/{profile}/` for all 8 profiles defined in `tests/helpers/integration-constants.ts`.

---

## 3. Acceptance Tests (Outer Loop)

Each acceptance test maps directly to a Gherkin scenario from the story. These are the "outer loop" validations -- they describe the end-to-end behavior the story must deliver.

### AT-1: Epic generated contains TDD Compliance in DoD

- **Gherkin**: "Cenario: Epic gerado contem TDD Compliance no DoD"
- **Status**: RED until UT-3, UT-4 complete
- **Components**: Claude template Step 4 (`resources/skills-templates/core/x-story-epic/SKILL.md`), GitHub template Step 4 (`resources/github-skills-templates/story/x-story-epic.md`)
- **Acceptance Criteria**: The updated template instructs the skill to include "TDD Compliance" as a DoD item, mentioning test-first commits, explicit refactoring, and TPP.
- **Validation**: Content validation tests verify the Claude template contains "TDD Compliance" in the DoD section with keywords "test-first", "refactoring", and "TPP". GitHub template contains equivalent content.

### AT-2: Epic generated contains Double-Loop TDD in DoD

- **Gherkin**: "Cenario: Epic gerado contem Double-Loop TDD no DoD"
- **Status**: RED until UT-5, UT-6 complete
- **Components**: Claude template Step 4, GitHub template Step 4
- **Acceptance Criteria**: The updated template instructs the skill to include "Double-Loop TDD" as a DoD item, mentioning acceptance tests from Gherkin and unit tests guided by TPP.
- **Validation**: Content validation tests verify "Double-Loop TDD" appears in the DoD section with keywords "acceptance tests" and "unit tests".

### AT-3: Epic extracts TDD rules as cross-cutting rules

- **Gherkin**: "Cenario: Epic extrai regras TDD como regras transversais"
- **Status**: RED until UT-7, UT-8, UT-9, UT-10 complete
- **Components**: Claude template Step 2, GitHub template Step 2
- **Acceptance Criteria**: Step 2 instructs the skill to extract TDD-related cross-cutting rules when applicable, including Red-Green-Refactor, Atomic TDD Commits, and Gherkin Completeness.
- **Validation**: Content validation tests verify Step 2 contains TDD rules extraction guidance with all three rule categories.

### AT-4: Coverage thresholds maintained in DoD

- **Gherkin**: "Cenario: Coverage thresholds mantidos no DoD"
- **Status**: RED until UT-1, UT-2 complete
- **Components**: Claude template Step 4, GitHub template Step 4
- **Acceptance Criteria**: Existing DoD items (coverage targets, test types, documentation, SLOs, persistence) are preserved. TDD items are additional.
- **Validation**: Content validation tests verify existing DoD bullet points remain unchanged alongside the new TDD items.

### AT-5: Existing DoD preserved

- **Gherkin**: "Cenario: DoD existente preservado"
- **Status**: RED until UT-1, UT-2 complete
- **Components**: Claude template Step 4, GitHub template Step 4
- **Acceptance Criteria**: All original DoD items from the pre-change template remain present. The TDD items are appended, not substituted.
- **Validation**: Content validation tests verify all 5 original DoD items (coverage, test types, documentation, SLOs, persistence) exist in the template after modification.

---

## 4. Unit Tests (Inner Loop -- TPP Order)

These are the concrete tests that drive implementation. They follow TPP order: degenerate cases first, then increasingly complex validations.

### UT-1: Existing DoD items preserved -- coverage targets present -- TPP Level 1

- **Test**: `step4DoD_existingItems_containsCoverageTargets`
- **Implementation**: Verify the DoD section still contains "Coverage targets (line, branch)"
- **Transform**: `{}->nil` (if coverage item missing, fail immediately)
- **Components**: Claude template Step 4
- **Depends on**: --
- **Parallel**: yes

### UT-2: Existing DoD items preserved -- all 5 original items present -- TPP Level 1

- **Test**: `step4DoD_existingItems_containsAll5OriginalBulletPoints`
- **Implementation**: Verify the DoD section contains all 5 pre-existing items: "Coverage targets", "Required test types", "Documentation requirements", "Performance SLOs", "Persistence/data integrity"
- **Transform**: `{}->nil` (if any item missing, fail)
- **Components**: Claude template Step 4
- **Depends on**: UT-1
- **Parallel**: no

### UT-3: TDD Compliance DoD item present -- TPP Level 2

- **Test**: `step4DoD_tddCompliance_containsTDDComplianceItem`
- **Implementation**: Verify the DoD section contains a bullet point with "TDD Compliance"
- **Transform**: `constant->variable` (new item appears in template)
- **Components**: Claude template Step 4
- **Depends on**: UT-2
- **Parallel**: no

### UT-4: TDD Compliance DoD item -- keywords present -- TPP Level 3

- **Test**: `step4DoD_tddCompliance_containsTestFirstRefactoringTPP`
- **Implementation**: Verify the TDD Compliance item mentions "test-first" (or "test precedes implementation"), "refactoring", and "Transformation Priority Premise" (or "TPP")
- **Transform**: `unconditional->conditional` (checking multiple keyword conditions)
- **Components**: Claude template Step 4
- **Depends on**: UT-3
- **Parallel**: no

### UT-5: Double-Loop TDD DoD item present -- TPP Level 2

- **Test**: `step4DoD_doubleLoopTDD_containsDoubleLoopItem`
- **Implementation**: Verify the DoD section contains a bullet point with "Double-Loop TDD"
- **Transform**: `constant->variable` (new item appears in template)
- **Components**: Claude template Step 4
- **Depends on**: UT-2
- **Parallel**: yes (parallel with UT-3)

### UT-6: Double-Loop TDD DoD item -- keywords present -- TPP Level 3

- **Test**: `step4DoD_doubleLoopTDD_containsAcceptanceAndUnitTestRefs`
- **Implementation**: Verify the Double-Loop TDD item mentions "acceptance tests" (or "Acceptance tests") and "unit tests" (or "Unit tests"), and references "Gherkin" and "Transformation Priority Premise" (or "TPP")
- **Transform**: `unconditional->conditional` (checking multiple keyword conditions)
- **Components**: Claude template Step 4
- **Depends on**: UT-5
- **Parallel**: no

### UT-7: Step 2 TDD rules extraction section exists -- TPP Level 2

- **Test**: `step2Rules_tddExtraction_containsTDDRulesSubsection`
- **Implementation**: Verify Step 2 contains a subsection or paragraph about TDD cross-cutting rules extraction (e.g., "TDD cross-cutting rules")
- **Transform**: `constant->variable` (new subsection appears in Step 2)
- **Components**: Claude template Step 2
- **Depends on**: --
- **Parallel**: yes

### UT-8: Step 2 TDD rules -- Red-Green-Refactor mentioned -- TPP Level 3

- **Test**: `step2Rules_tddExtraction_containsRedGreenRefactorRule`
- **Implementation**: Verify the TDD rules extraction section mentions "Red-Green-Refactor" as one of the extractable rules
- **Transform**: `unconditional->conditional`
- **Components**: Claude template Step 2
- **Depends on**: UT-7
- **Parallel**: yes

### UT-9: Step 2 TDD rules -- Atomic TDD Commits mentioned -- TPP Level 3

- **Test**: `step2Rules_tddExtraction_containsAtomicTDDCommitsRule`
- **Implementation**: Verify the TDD rules extraction section mentions "Atomic TDD Commits" as one of the extractable rules
- **Transform**: `unconditional->conditional`
- **Components**: Claude template Step 2
- **Depends on**: UT-7
- **Parallel**: yes (parallel with UT-8)

### UT-10: Step 2 TDD rules -- Gherkin Completeness mentioned -- TPP Level 3

- **Test**: `step2Rules_tddExtraction_containsGherkinCompletenessRule`
- **Implementation**: Verify the TDD rules extraction section mentions "Gherkin Completeness" as one of the extractable rules
- **Transform**: `unconditional->conditional`
- **Components**: Claude template Step 2
- **Depends on**: UT-7
- **Parallel**: yes (parallel with UT-8, UT-9)

### UT-11: GitHub template -- TDD DoD items present -- TPP Level 2

- **Test**: `githubTemplate_step4DoD_containsTDDComplianceAndDoubleLoop`
- **Implementation**: Verify the GitHub template contains "TDD Compliance" and "Double-Loop TDD" in its DoD section
- **Transform**: `constant->variable`
- **Components**: GitHub template Step 4
- **Depends on**: --
- **Parallel**: yes

### UT-12: GitHub template -- TDD rules extraction present -- TPP Level 2

- **Test**: `githubTemplate_step2Rules_containsTDDRulesExtraction`
- **Implementation**: Verify the GitHub template contains TDD rules extraction guidance in Step 2 (Red-Green-Refactor, Atomic TDD Commits, Gherkin Completeness)
- **Transform**: `constant->variable`
- **Components**: GitHub template Step 2
- **Depends on**: --
- **Parallel**: yes (parallel with UT-11)

### UT-13: Step 2 existing content preserved -- cross-cutting rules guidance intact -- TPP Level 1

- **Test**: `step2Rules_existingContent_containsOriginalCrossCuttingGuidance`
- **Implementation**: Verify Step 2 still contains the original "What qualifies as a cross-cutting rule" and "What stays in individual stories" subsections
- **Transform**: `{}->nil` (if existing content missing, fail)
- **Components**: Claude template Step 2
- **Depends on**: --
- **Parallel**: yes

---

## 5. Golden File Integration Tests (Existing Suite -- Updated Golden Files)

The existing `byte-for-byte.test.ts` runs `describe.sequential.each` over all 8 `CONFIG_PROFILES`. Each profile runs 5 assertions. Golden files must be regenerated to include the new TDD DoD items and TDD rules extraction instructions.

**Action required:** Update all 24 golden `x-story-epic/SKILL.md` files (8 profiles x 3 targets) to match the updated templates.

### Group 1: Pipeline Parity (8 profiles x 3 targets = 24 file comparisons)

| # | Test Name (existing) | Profile | Assertion |
|---|---------------------|---------|-----------|
| 1 | `pipelineMatchesGoldenFiles_go-gin` | go-gin | `verifyOutput()` returns `success: true` |
| 2 | `pipelineMatchesGoldenFiles_java-quarkus` | java-quarkus | `verifyOutput()` returns `success: true` |
| 3 | `pipelineMatchesGoldenFiles_java-spring` | java-spring | `verifyOutput()` returns `success: true` |
| 4 | `pipelineMatchesGoldenFiles_kotlin-ktor` | kotlin-ktor | `verifyOutput()` returns `success: true` |
| 5 | `pipelineMatchesGoldenFiles_python-click-cli` | python-click-cli | `verifyOutput()` returns `success: true` |
| 6 | `pipelineMatchesGoldenFiles_python-fastapi` | python-fastapi | `verifyOutput()` returns `success: true` |
| 7 | `pipelineMatchesGoldenFiles_rust-axum` | rust-axum | `verifyOutput()` returns `success: true` |
| 8 | `pipelineMatchesGoldenFiles_typescript-nestjs` | typescript-nestjs | `verifyOutput()` returns `success: true` |

**Verification logic:** `verifyOutput()` in `src/verifier.ts` compares two directory trees byte-for-byte using `Buffer.equals()`. Any difference in the golden `x-story-epic/SKILL.md` files will produce a `FileDiff` mismatch and fail the test. The `formatVerificationFailures()` helper renders up to 500 chars of unified diff for debugging.

---

## 6. Dual Copy Consistency Tests

Verify RULE-001 compliance: `.claude/` and `.agents/` copies must be byte-for-byte identical for all 8 profiles. This is validated transitively by the byte-for-byte suite (both are generated from the same template), but explicit validation adds defense-in-depth.

**Path:** `tests/node/content/x-story-epic-tdd-dod.test.ts`

```typescript
import { CONFIG_PROFILES, GOLDEN_DIR } from "../../helpers/integration-constants.js";

describe("dual copy consistency -- x-story-epic", () => {
  for (const profile of CONFIG_PROFILES) {
    it(`dualCopy_${profile}_claudeAndAgentsCopiesIdentical`, () => {
      const claudeCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".claude/skills/x-story-epic/SKILL.md"),
        "utf-8",
      );
      const agentsCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".agents/skills/x-story-epic/SKILL.md"),
        "utf-8",
      );
      expect(claudeCopy).toBe(agentsCopy);
    });
  }
});
```

| # | Test Name | Profile | Assertion |
|---|-----------|---------|-----------|
| 1 | `dualCopy_go-gin_claudeAndAgentsCopiesIdentical` | go-gin | `.claude/` == `.agents/` |
| 2 | `dualCopy_java-quarkus_claudeAndAgentsCopiesIdentical` | java-quarkus | `.claude/` == `.agents/` |
| 3 | `dualCopy_java-spring_claudeAndAgentsCopiesIdentical` | java-spring | `.claude/` == `.agents/` |
| 4 | `dualCopy_kotlin-ktor_claudeAndAgentsCopiesIdentical` | kotlin-ktor | `.claude/` == `.agents/` |
| 5 | `dualCopy_python-click-cli_claudeAndAgentsCopiesIdentical` | python-click-cli | `.claude/` == `.agents/` |
| 6 | `dualCopy_python-fastapi_claudeAndAgentsCopiesIdentical` | python-fastapi | `.claude/` == `.agents/` |
| 7 | `dualCopy_rust-axum_claudeAndAgentsCopiesIdentical` | rust-axum | `.claude/` == `.agents/` |
| 8 | `dualCopy_typescript-nestjs_claudeAndAgentsCopiesIdentical` | typescript-nestjs | `.claude/` == `.agents/` |

---

## 7. Test Matrix Summary

| Group | Description | Test Count | Type | TPP Level |
|-------|-------------|------------|------|-----------|
| G1: Backward Compatibility (DoD) | Existing DoD items preserved (coverage, test types, docs, SLOs, persistence) | 2 | Content Validation | Level 1 |
| G2: Backward Compatibility (Step 2) | Existing cross-cutting rules guidance preserved | 1 | Content Validation | Level 1 |
| G3: TDD Compliance DoD | TDD Compliance item present with keywords | 2 | Content Validation | Level 2-3 |
| G4: Double-Loop TDD DoD | Double-Loop TDD item present with keywords | 2 | Content Validation | Level 2-3 |
| G5: TDD Rules Extraction | Step 2 TDD subsection with Red-Green-Refactor, Atomic Commits, Gherkin | 4 | Content Validation | Level 2-3 |
| G6: GitHub Template | TDD DoD items + TDD rules extraction in GitHub copy | 2 | Content Validation | Level 2 |
| G7: Golden File Integration | Byte-for-byte parity across 8 profiles | 8 | Integration (existing) | -- |
| G8: Dual Copy Consistency | `.claude/` == `.agents/` for all 8 profiles | 8 | Content Validation | -- |
| **Total** | | **29** | | |

---

## 8. Acceptance Criteria Traceability

| Gherkin Scenario | AT ID | Supporting UT IDs | Test Group(s) |
|------------------|-------|-------------------|---------------|
| Epic gerado contem TDD Compliance no DoD | AT-1 | UT-3, UT-4 | G3 |
| Epic gerado contem Double-Loop TDD no DoD | AT-2 | UT-5, UT-6 | G4 |
| Epic extrai regras TDD como regras transversais | AT-3 | UT-7, UT-8, UT-9, UT-10 | G5 |
| Coverage thresholds mantidos no DoD | AT-4 | UT-1, UT-2 | G1 |
| DoD existente preservado | AT-5 | UT-1, UT-2 | G1 |
| GitHub template parity (RULE-001) | -- | UT-11, UT-12 | G6 |
| Golden file parity (implicit DoD) | -- | All golden file tests | G7 |
| Dual copy consistency (RULE-001) | -- | All dual copy tests | G8 |

---

## 9. Coverage Strategy

### 9.1 Line Coverage

No new production code is added -- this is a template-only change. Coverage impact is zero. Existing coverage (99.6% lines, 97.84% branches) is maintained.

### 9.2 Branch Coverage

The `verifyOutput()` function in `src/verifier.ts` exercises all branches through golden file comparison:
- `Buffer.equals()` returns `true` for matching files (happy path)
- `FileDiff` produced for mismatching files (detected during development if golden files are stale)
- `missingFiles` / `extraFiles` arrays populated if file sets differ

### 9.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | No new code paths; existing coverage maintained |
| Branch | >= 90% | Existing verifier branches already covered |

---

## 10. Golden File Update Procedure

### Step 1: Modify Source Templates

1. Edit `resources/skills-templates/core/x-story-epic/SKILL.md` -- add TDD DoD items to Step 4 + TDD rules extraction to Step 2
2. Edit `resources/github-skills-templates/story/x-story-epic.md` -- mirror TDD changes in GitHub copy

### Step 2: Copy to Golden Files

Since the x-story-epic skill has **no profile-specific placeholder substitution** (verified: all 8 profiles are byte-for-byte identical for all 3 targets), the most efficient approach is direct copy.

**For `.claude/` and `.agents/` golden files (16 files):**

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  cp resources/skills-templates/core/x-story-epic/SKILL.md \
     tests/golden/${profile}/.claude/skills/x-story-epic/SKILL.md
  cp resources/skills-templates/core/x-story-epic/SKILL.md \
     tests/golden/${profile}/.agents/skills/x-story-epic/SKILL.md
done
```

**For `.github/` golden files (8 files):**

Since the GitHub template also has no profile-specific placeholders for this skill (verified: all 8 are identical), direct copy is also valid:

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  cp resources/github-skills-templates/story/x-story-epic.md \
     tests/golden/${profile}/.github/skills/x-story-epic/SKILL.md
done
```

### Step 3: Verify

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 8 profiles must pass with `success: true`.

---

## 11. Profile-Specific Differences

| Target | Profile-specific? | Placeholder | Update Method |
|--------|-------------------|-------------|---------------|
| `.claude/` | No -- all 8 profiles identical | None resolved | Direct copy from source template |
| `.agents/` | No -- mirrors `.claude/` exactly | None resolved | Direct copy from source template |
| `.github/` | No -- all 8 profiles identical for this skill | None resolved | Direct copy from GitHub source template |

---

## 12. Execution Commands

### Run Content Validation Tests Only

```bash
npx vitest run tests/node/content/x-story-epic-tdd-dod.test.ts
```

### Run Golden File Tests Only

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

### Run Both

```bash
npx vitest run tests/node/content/x-story-epic-tdd-dod.test.ts tests/node/integration/byte-for-byte.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 13. Naming Convention Reference

All new test names follow `[sectionUnderTest]_[scenario]_[expectedBehavior]`:

```
step4DoD_existingItems_containsCoverageTargets
step4DoD_existingItems_containsAll5OriginalBulletPoints
step4DoD_tddCompliance_containsTDDComplianceItem
step4DoD_tddCompliance_containsTestFirstRefactoringTPP
step4DoD_doubleLoopTDD_containsDoubleLoopItem
step4DoD_doubleLoopTDD_containsAcceptanceAndUnitTestRefs
step2Rules_tddExtraction_containsTDDRulesSubsection
step2Rules_tddExtraction_containsRedGreenRefactorRule
step2Rules_tddExtraction_containsAtomicTDDCommitsRule
step2Rules_tddExtraction_containsGherkinCompletenessRule
githubTemplate_step4DoD_containsTDDComplianceAndDoubleLoop
githubTemplate_step2Rules_containsTDDRulesExtraction
step2Rules_existingContent_containsOriginalCrossCuttingGuidance
dualCopy_{profile}_claudeAndAgentsCopiesIdentical  (x8 profiles)
```

---

## 14. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden files not updated before commit | High | High | CI fails immediately on byte-for-byte mismatch. Developer must update all 24 golden files as part of the story. |
| Content assertions too brittle | Medium | Medium | Use `toContain()` for section headers and key phrases, not full-line matching. |
| Existing DoD items accidentally removed | Low | High | UT-1 and UT-2 explicitly validate that all 5 original DoD items are preserved. Runs before TDD-specific tests. |
| RULE-001 dual copy inconsistency | Low | Medium | UT-11 and UT-12 validate GitHub template has equivalent TDD content. Dual copy tests verify `.claude/` == `.agents/` for all 8 profiles. |
| Step 2 existing guidance accidentally overwritten | Low | High | UT-13 validates original "What qualifies as a cross-cutting rule" and "What stays in individual stories" subsections are preserved. |
| New content breaks downstream consumers (x-story-create) | Low | Medium | Changes are purely additive. No section headers are renamed or removed. x-story-create reads DoD items as free-form bullets. |
| Vitest file discovery for new test file | Low | Low | New test at `tests/node/content/` must match `tests/**/*.test.ts` glob in vitest config. Verify pattern includes `content/` subdirectory. |

---

## 15. Dependencies and Prerequisites

### Prerequisites

- `story-0003-0005` completed (Templates with TDD sections -- DONE)
- `resources/skills-templates/core/x-story-epic/SKILL.md` exists with current format
- `resources/github-skills-templates/story/x-story-epic.md` exists with current format
- All 8 profile config templates exist in `resources/config-templates/`
- Golden file directories exist for all 8 profiles under `tests/golden/`
- `tests/helpers/integration-constants.ts` exports `CONFIG_PROFILES`, `GOLDEN_DIR`, `RESOURCES_DIR`

### Import Dependencies (for new test file)

| Module | Import | Used For |
|--------|--------|----------|
| `node:fs` | `readFileSync` | Reading template file content |
| `node:path` | `resolve`, `dirname` | Path resolution |
| `node:url` | `fileURLToPath` | ESM `__dirname` equivalent |
| `vitest` | `describe`, `it`, `expect`, `beforeAll` | Test framework |
| `tests/helpers/integration-constants.ts` | `CONFIG_PROFILES`, `GOLDEN_DIR` | Profile list and golden paths (for dual copy tests) |
