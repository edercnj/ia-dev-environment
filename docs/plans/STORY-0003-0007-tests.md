# Test Plan -- STORY-0003-0007: x-test-plan -- Promotion to Implementation Driver with TPP

## Summary

- Affected template files: 2 (`resources/skills-templates/core/x-test-plan/SKILL.md`, `resources/github-skills-templates/testing/x-test-plan.md`)
- Affected golden files: 24 (8 profiles x 3 targets: `.claude/`, `.agents/`, `.github/`)
- TypeScript source changes: 0
- Total test methods: 33 (8 existing golden file integration + 25 new content validation)
- Categories: Golden File Integration (8), Content Validation (17), Dual Copy Consistency (8)
- Coverage targets: >= 95% line, >= 90% branch (maintained -- no new production code)

---

## 1. Test File Locations and Naming

### Existing (unchanged)

**Path:** `tests/node/integration/byte-for-byte.test.ts`

**Rationale:** The existing byte-for-byte test suite (`describe.sequential.each` over all 8 `CONFIG_PROFILES`) automatically validates that pipeline output matches golden files. No code changes to this file -- only 24 golden files are updated to reflect the new Double-Loop + TPP format.

### New

**Path:** `tests/node/content/x-test-plan-double-loop-tpp.test.ts`

**Rationale:** Content validation tests that verify the structural integrity of the restructured x-test-plan skill. These are unit-level tests that read the source template files and assert section presence, ordering, TPP markers, and dependency metadata. Separated from byte-for-byte tests because they validate semantic content, not binary equality.

**Naming convention:** `[sectionUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

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

const CLAUDE_TEMPLATE = resolve(RESOURCES_DIR, "skills-templates", "core", "x-test-plan", "SKILL.md");
const GITHUB_TEMPLATE = resolve(RESOURCES_DIR, "github-skills-templates", "testing", "x-test-plan.md");
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

1. `{profile}/.claude/skills/x-test-plan/SKILL.md`
2. `{profile}/.agents/skills/x-test-plan/SKILL.md`

The GitHub template is processed by `GithubSkillsAssembler` with `{language_name}` placeholder resolution, producing:

3. `{profile}/.github/skills/x-test-plan/SKILL.md`

Golden files live at `tests/golden/{profile}/` for all 8 profiles defined in `tests/helpers/integration-constants.ts`.

---

## 3. Acceptance Tests (Outer Loop)

Each acceptance test maps directly to a Gherkin scenario from the story. These are the "outer loop" validations -- they describe the end-to-end behavior the story must deliver. They remain RED until all supporting unit tests pass.

### AT-1: Test plan generates acceptance tests derived from Gherkin

- **Gherkin**: "Cenario: Test plan gera acceptance tests derivados do Gherkin"
- **Status**: RED until UT-5, UT-6 complete
- **Components**: Claude template (`resources/skills-templates/core/x-test-plan/SKILL.md`), GitHub template (`resources/github-skills-templates/testing/x-test-plan.md`)
- **Acceptance Criteria**: The updated template instructs the skill to produce an "Acceptance Tests (Outer Loop)" section containing `AT-N` entries derived from Gherkin scenarios, each referencing the original scenario and listing components under test.
- **Validation**: Content validation tests verify the Claude template contains `## Acceptance Tests (Outer Loop)` or equivalent H2/H3 headers, `AT-` ID pattern, `Gherkin` field, `Status` field, and `Components` field. GitHub template contains equivalent condensed content.

### AT-2: Test plan generates unit tests in TPP order

- **Gherkin**: "Cenario: Test plan gera unit tests em ordem TPP"
- **Status**: RED until UT-7, UT-8, UT-9 complete
- **Components**: Claude template Step 2 section
- **Acceptance Criteria**: The template instructs unit tests to be ordered by TPP level (1-6), with the first UT always being a degenerate case (TPP Level 1), subsequent UTs following increasing complexity, and each UT indicating its TPP Level.
- **Validation**: Content validation tests verify TPP Level sub-sections (1 through 6) exist in order, and the field table includes `TPP Level` as a mandatory field.

### AT-3: Each scenario includes dependency markers

- **Gherkin**: "Cenario: Cada cenario inclui dependency markers"
- **Status**: RED until UT-10 complete
- **Components**: Claude template (field tables for AT, UT, IT sections)
- **Acceptance Criteria**: Every scenario template (AT, UT, IT) contains `Components`, `Depends on`, and `Parallel` fields.
- **Validation**: Content validation tests verify all three field names appear in the per-scenario field specification tables.

### AT-4: Degenerate case is always first unit test

- **Gherkin**: "Cenario: Degenerate case e sempre o primeiro unit test"
- **Status**: RED until UT-8 complete
- **Components**: Claude template Step 2.2 (TPP Level 1 sub-section)
- **Acceptance Criteria**: TPP Level 1 is the first sub-section under Unit Tests, explicitly described as "Degenerate Cases" with transforms `{}->nil` or `nil->constant`.
- **Validation**: Content validation tests verify TPP Level 1 appears first under UT section and contains degenerate-case keywords.

### AT-5: Test plan preserves subagent pattern

- **Gherkin**: "Cenario: Test plan preserva subagent pattern"
- **Status**: RED until UT-1, UT-2, UT-3 complete
- **Components**: Claude template Step 1 section
- **Acceptance Criteria**: Step 1 subagent prompt is character-for-character identical to the current version. The subagent's `allowed-tools` and 11-point return schema are unchanged.
- **Validation**: Content validation tests verify Step 1 section is fully preserved (subagent prompt text, 11-point schema, tool references).

### AT-6: Output compatible with x-lib-task-decomposer

- **Gherkin**: "Cenario: Test plan output compativel com x-lib-task-decomposer"
- **Status**: RED until UT-5, UT-7, UT-11 complete
- **Components**: Claude template Output Format section
- **Acceptance Criteria**: The output format uses `UT-N` IDs that can be mapped to tasks, each UT corresponds to a concrete implementation step, and the format is parseable as structured markdown.
- **Validation**: Content validation tests verify the output template contains `UT-N` sequential IDs, `AT-N` IDs, and `IT-N` IDs with clear field structure.

### AT-7: Test plan with story without conditional logic

- **Gherkin**: "Cenario: Test plan com story sem logica condicional"
- **Status**: RED until UT-12 complete
- **Components**: Claude template CRUD-Only Story Optimization section (Step 2.4)
- **Acceptance Criteria**: A CRUD-only optimization section exists that limits UTs to degenerate -> constant -> variable (max Level 2-3) and explicitly instructs against generating unnecessary conditional/iteration UTs.
- **Validation**: Content validation tests verify the CRUD optimization section exists with appropriate level-limiting instructions.

---

## 4. Unit Tests (Inner Loop -- TPP Order)

These are the concrete tests that drive implementation. They follow TPP order: degenerate cases first, then increasingly complex validations.

### UT-1: Subagent Step 1 preserved -- section header exists -- TPP Level 1

- **Test**: `step1Subagent_sectionExists_containsSubagentHeader`
- **Implementation**: Ensure the template retains `## Step 1: Gather Context (Subagent via Task)` header
- **Transform**: `{}->nil` (if section missing, fail immediately)
- **Components**: Claude template
- **Depends on**: --
- **Parallel**: yes

### UT-2: Subagent Step 1 preserved -- 11-point schema intact -- TPP Level 1

- **Test**: `step1Subagent_returnSchema_contains11NumberedItems`
- **Implementation**: Count numbered items in Step 1 blockquote matching `/^\s*>\s*\d+\.\s/m`; expect >= 11
- **Transform**: `{}->nil` (if schema items missing, fail)
- **Components**: Claude template
- **Depends on**: UT-1
- **Parallel**: no

### UT-3: Subagent Step 1 preserved -- KP references intact -- TPP Level 1

- **Test**: `step1Subagent_kpReferences_containsTestingPhilosophyAndConventionsAndArchitecture`
- **Implementation**: Verify Step 1 contains references to `testing-philosophy.md`, `testing-conventions.md`, `architecture-principles.md`
- **Transform**: `{}->nil` (if references missing, fail)
- **Components**: Claude template
- **Depends on**: UT-1
- **Parallel**: yes (parallel with UT-2)

### UT-4: Subagent allowed-tools preserved -- TPP Level 1

- **Test**: `step1Subagent_allowedTools_containsReadGrepGlob`
- **Implementation**: Verify YAML frontmatter contains `allowed-tools: Read, Grep, Glob`
- **Transform**: `{}->nil`
- **Components**: Claude template (YAML frontmatter)
- **Depends on**: --
- **Parallel**: yes

### UT-5: Acceptance Tests section exists in Claude template -- TPP Level 2

- **Test**: `acceptanceTests_sectionExists_containsOuterLoopHeader`
- **Implementation**: Add `## Acceptance Tests (Outer Loop)` or `### 2.1 Acceptance Tests (Outer Loop)` to Step 2
- **Transform**: `constant->variable` (new section appears in template)
- **Components**: Claude template Step 2
- **Depends on**: UT-1
- **Parallel**: yes

### UT-6: Acceptance Tests field table -- AT-N ID, Gherkin, Status, Components -- TPP Level 2

- **Test**: `acceptanceTests_fieldTable_containsRequiredFields`
- **Implementation**: Verify the AT section contains field table with `ID`, `Gherkin`, `Status`, `Components`
- **Transform**: `constant->variable`
- **Components**: Claude template Step 2.1
- **Depends on**: UT-5
- **Parallel**: no

### UT-7: Unit Tests section exists with TPP ordering -- TPP Level 2

- **Test**: `unitTests_sectionExists_containsInnerLoopTPPHeader`
- **Implementation**: Add `## Unit Tests (Inner Loop -- TPP Order)` or `### 2.2 Unit Tests (Inner Loop -- TPP Order)` to Step 2
- **Transform**: `constant->variable`
- **Components**: Claude template Step 2
- **Depends on**: UT-1
- **Parallel**: yes (parallel with UT-5)

### UT-8: TPP Levels 1-6 present as sub-sections -- TPP Level 3

- **Test**: `unitTests_tppLevels_contains6LevelSubsections`
- **Implementation**: Verify Step 2 UT section contains sub-sections or references for TPP Level 1 through Level 6 (Degenerate, Unconditional, Simple Conditions, Complex Conditions, Iterations, Edge Cases)
- **Transform**: `unconditional->conditional` (branching: each level has distinct content)
- **Components**: Claude template Step 2.2
- **Depends on**: UT-7
- **Parallel**: no

### UT-9: UT field table -- ID, Test, Implementation, Transform, TPP Level, Components, Depends on, Parallel -- TPP Level 3

- **Test**: `unitTests_fieldTable_containsAllRequiredFields`
- **Implementation**: Verify UT field specification table contains all 8 fields: ID, Test, Implementation, Transform, TPP Level, Components, Depends on, Parallel
- **Transform**: `unconditional->conditional`
- **Components**: Claude template Step 2.2
- **Depends on**: UT-7
- **Parallel**: yes (parallel with UT-8)

### UT-10: Dependency markers present in all scenario types -- TPP Level 3

- **Test**: `dependencyMarkers_allSections_containComponentsDependsOnParallel`
- **Implementation**: Verify `Components`, `Depends on`, and `Parallel` fields appear in AT, UT, and IT field tables
- **Transform**: `unconditional->conditional` (checking across 3 sections)
- **Components**: Claude template Step 2.1, 2.2, 2.3
- **Depends on**: UT-5, UT-7
- **Parallel**: no

### UT-11: Integration Tests section exists with dependency markers -- TPP Level 2

- **Test**: `integrationTests_sectionExists_containsCrossComponentHeader`
- **Implementation**: Add `### 2.3 Integration Tests (Cross-Component)` to Step 2 with IT-N IDs and dependency fields
- **Transform**: `constant->variable`
- **Components**: Claude template Step 2.3
- **Depends on**: UT-7
- **Parallel**: yes

### UT-12: CRUD-only optimization section exists -- TPP Level 3

- **Test**: `crudOptimization_sectionExists_containsLevelLimitingInstructions`
- **Implementation**: Add `### 2.4 CRUD-Only Story Optimization` with instructions to limit UTs to Level 1-2 for pure CRUD stories
- **Transform**: `unconditional->conditional` (conditional behavior based on story type)
- **Components**: Claude template Step 2.4
- **Depends on**: UT-8
- **Parallel**: no

### UT-13: Updated quality checks include TPP validation -- TPP Level 3

- **Test**: `qualityChecks_step3_containsTPPOrderingValidation`
- **Implementation**: Verify Step 3 Quality Checks include TPP-specific rules (e.g., "UT-1 is ALWAYS a degenerate case", "UTs follow non-decreasing TPP level order")
- **Transform**: `unconditional->conditional`
- **Components**: Claude template Step 3
- **Depends on**: UT-1
- **Parallel**: yes

### UT-14: Output format uses Double-Loop structure -- TPP Level 3

- **Test**: `outputFormat_template_containsDoubleLoopStructure`
- **Implementation**: Verify the Output section template contains `## Acceptance Tests (Outer Loop)`, `## Unit Tests (Inner Loop -- TPP Order)`, `## Integration Tests (Cross-Component)`, `## Coverage Estimation`, and `## Risks and Gaps`
- **Transform**: `unconditional->conditional`
- **Components**: Claude template Output section
- **Depends on**: UT-5, UT-7
- **Parallel**: no

### UT-15: Old category-based sections removed -- TPP Level 3

- **Test**: `step2_oldSections_removedHappyPathErrorPathBoundary`
- **Implementation**: Verify Claude template does NOT contain `### 2.1 Happy Path`, `### 2.2 Error Path`, `### 2.3 Boundary Tests` as separate category-based sections
- **Transform**: `unconditional->conditional` (verifying absence)
- **Components**: Claude template Step 2
- **Depends on**: UT-5, UT-7
- **Parallel**: yes

### UT-16: GitHub template -- Double-Loop structure present -- TPP Level 2

- **Test**: `githubTemplate_step2_containsDoubleLoopTPPStructure`
- **Implementation**: Verify GitHub template contains "Acceptance Tests (Outer Loop)" and "Unit Tests (Inner Loop -- TPP Order)" references
- **Transform**: `constant->variable`
- **Components**: GitHub template
- **Depends on**: --
- **Parallel**: yes

### UT-17: GitHub template -- TPP level table present -- TPP Level 3

- **Test**: `githubTemplate_step2_containsTPPLevelTable`
- **Implementation**: Verify GitHub template contains a table with TPP Level entries (1 -- Degenerate through 6 -- Edge cases)
- **Transform**: `unconditional->conditional`
- **Components**: GitHub template
- **Depends on**: UT-16
- **Parallel**: no

---

## 5. Golden File Integration Tests (Existing Suite -- Updated Golden Files)

The existing `byte-for-byte.test.ts` runs `describe.sequential.each` over all 8 `CONFIG_PROFILES`. Each profile runs 5 assertions. Golden files must be regenerated to include the new Double-Loop + TPP format.

**Action required:** Update all 24 golden `x-test-plan/SKILL.md` files (8 profiles x 3 targets) to match the updated templates.

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

**Verification logic:** `verifyOutput()` in `src/verifier.ts` compares two directory trees byte-for-byte using `Buffer.equals()`. Any difference in the golden `x-test-plan/SKILL.md` files will produce a `FileDiff` mismatch and fail the test. The `formatVerificationFailures()` helper renders up to 500 chars of unified diff for debugging.

---

## 6. Dual Copy Consistency Tests

Verify RULE-001 compliance: `.claude/` and `.agents/` copies must be byte-for-byte identical for all 8 profiles. This is validated transitively by the byte-for-byte suite (both are generated from the same template), but explicit validation adds defense-in-depth.

**Path:** Include in `tests/node/content/x-test-plan-double-loop-tpp.test.ts`

```typescript
import { CONFIG_PROFILES, GOLDEN_DIR } from "../../helpers/integration-constants.js";

describe("dual copy consistency -- x-test-plan", () => {
  for (const profile of CONFIG_PROFILES) {
    it(`dualCopy_${profile}_claudeAndAgentsCopiesIdentical`, () => {
      const claudeCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".claude/skills/x-test-plan/SKILL.md"),
        "utf-8",
      );
      const agentsCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".agents/skills/x-test-plan/SKILL.md"),
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
| G1: Subagent Preservation | Step 1 header, 11-point schema, KP refs, tools | 4 | Content Validation | Level 1 |
| G2: Double-Loop Sections (AT) | AT header, field table, Gherkin/Status/Components | 2 | Content Validation | Level 2 |
| G3: Double-Loop Sections (UT) | UT header, TPP levels 1-6, field table (8 fields) | 3 | Content Validation | Level 2-3 |
| G4: Double-Loop Sections (IT) | IT header, dependency markers | 1 | Content Validation | Level 2 |
| G5: Dependency Markers | Components/Depends on/Parallel across AT, UT, IT | 1 | Content Validation | Level 3 |
| G6: CRUD Optimization | Section exists, level-limiting instructions | 1 | Content Validation | Level 3 |
| G7: Quality Checks | TPP-aware validation rules in Step 3 | 1 | Content Validation | Level 3 |
| G8: Output Format | Double-Loop output template structure | 1 | Content Validation | Level 3 |
| G9: Old Sections Removed | Category-based sections no longer present | 1 | Content Validation | Level 3 |
| G10: GitHub Template | Double-Loop + TPP table in condensed form | 2 | Content Validation | Level 2-3 |
| G11: Golden File Integration | Byte-for-byte parity across 8 profiles | 8 | Integration (existing) | -- |
| G12: Dual Copy Consistency | `.claude/` == `.agents/` for all 8 profiles | 8 | Content Validation | -- |
| **Total** | | **33** | | |

---

## 8. Acceptance Criteria Traceability

| Gherkin Scenario | AT ID | Supporting UT IDs | Test Group(s) |
|------------------|-------|-------------------|---------------|
| Test plan gera acceptance tests derivados do Gherkin | AT-1 | UT-5, UT-6 | G2 |
| Test plan gera unit tests em ordem TPP | AT-2 | UT-7, UT-8, UT-9 | G3 |
| Cada cenario inclui dependency markers | AT-3 | UT-10 | G5 |
| Degenerate case e sempre o primeiro unit test | AT-4 | UT-8 | G3 |
| Test plan preserva subagent pattern | AT-5 | UT-1, UT-2, UT-3, UT-4 | G1 |
| Test plan output compativel com x-lib-task-decomposer | AT-6 | UT-5, UT-7, UT-11, UT-14 | G2, G3, G4, G8 |
| Test plan com story sem logica condicional | AT-7 | UT-12 | G6 |
| Golden file parity (implicit DoD) | -- | All golden file tests | G11 |
| Dual copy consistency (RULE-001) | -- | All dual copy tests | G12 |

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

1. Edit `resources/skills-templates/core/x-test-plan/SKILL.md` -- restructure Step 2, Step 3, and Output sections for Double-Loop + TPP
2. Edit `resources/github-skills-templates/testing/x-test-plan.md` -- mirror structural changes in condensed form

### Step 2: Regenerate Golden Files

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  npx tsx src/cli.ts generate \
    --config resources/config-templates/setup-config.${profile}.yaml \
    --output /tmp/golden-${profile} \
    --resources resources

  cp /tmp/golden-${profile}/.claude/skills/x-test-plan/SKILL.md \
     tests/golden/${profile}/.claude/skills/x-test-plan/SKILL.md
  cp /tmp/golden-${profile}/.agents/skills/x-test-plan/SKILL.md \
     tests/golden/${profile}/.agents/skills/x-test-plan/SKILL.md
  cp /tmp/golden-${profile}/.github/skills/x-test-plan/SKILL.md \
     tests/golden/${profile}/.github/skills/x-test-plan/SKILL.md
done
```

### Step 3: Verify

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 8 profiles must pass with `success: true`.

### Shortcut: Manual Copy for `.claude/` and `.agents/`

Since the Claude template has no profile-specific placeholders resolved at the `.claude/` and `.agents/` level (only `{{LANGUAGE}}` which stays unresolved), the source template can be copied directly to all 16 golden paths:

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  cp resources/skills-templates/core/x-test-plan/SKILL.md \
     tests/golden/${profile}/.claude/skills/x-test-plan/SKILL.md
  cp resources/skills-templates/core/x-test-plan/SKILL.md \
     tests/golden/${profile}/.agents/skills/x-test-plan/SKILL.md
done
```

For `.github/` golden files, use the pipeline (not manual copy) because `{language_name}` placeholders are resolved per profile.

---

## 11. Profile-Specific Differences

| Target | Profile-specific? | Placeholder | Update Method |
|--------|-------------------|-------------|---------------|
| `.claude/` | No -- all 8 profiles identical | `{{LANGUAGE}}` stays unresolved | Direct copy from source template |
| `.agents/` | No -- mirrors `.claude/` exactly | `{{LANGUAGE}}` stays unresolved | Direct copy from source template |
| `.github/` | Yes -- `{language_name}` resolved per profile | `{language_name}` -> "typescript", "go", "java", etc. | Run pipeline per profile |

---

## 12. Execution Commands

### Run Content Validation Tests Only

```bash
npx vitest run tests/node/content/x-test-plan-double-loop-tpp.test.ts
```

### Run Golden File Tests Only

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

### Run Both

```bash
npx vitest run tests/node/content/x-test-plan-double-loop-tpp.test.ts tests/node/integration/byte-for-byte.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 13. Naming Convention Reference

All new test names follow `[sectionUnderTest]_[scenario]_[expectedBehavior]`:

```
step1Subagent_sectionExists_containsSubagentHeader
step1Subagent_returnSchema_contains11NumberedItems
step1Subagent_kpReferences_containsTestingPhilosophyAndConventionsAndArchitecture
step1Subagent_allowedTools_containsReadGrepGlob
acceptanceTests_sectionExists_containsOuterLoopHeader
acceptanceTests_fieldTable_containsRequiredFields
unitTests_sectionExists_containsInnerLoopTPPHeader
unitTests_tppLevels_contains6LevelSubsections
unitTests_fieldTable_containsAllRequiredFields
dependencyMarkers_allSections_containComponentsDependsOnParallel
integrationTests_sectionExists_containsCrossComponentHeader
crudOptimization_sectionExists_containsLevelLimitingInstructions
qualityChecks_step3_containsTPPOrderingValidation
outputFormat_template_containsDoubleLoopStructure
step2_oldSections_removedHappyPathErrorPathBoundary
githubTemplate_step2_containsDoubleLoopTPPStructure
githubTemplate_step2_containsTPPLevelTable
dualCopy_{profile}_claudeAndAgentsCopiesIdentical  (x8 profiles)
```

---

## 14. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden files not updated before commit | High | High | CI fails immediately on byte-for-byte mismatch. Developer must regenerate golden files as part of the story. |
| Content assertions too brittle | Medium | Medium | Use `toContain()` for section headers and key phrases, not full-line matching. Use case-insensitive regex for flexible assertions. |
| Step 1 subagent prompt accidentally modified | Low | High | UT-1/UT-2/UT-3 validate subagent preservation character-for-character. Use exact string matching. |
| `{{LANGUAGE}}` placeholders broken in `.claude/` copy | Low | Medium | These are intentionally unresolved; byte-for-byte tests catch any change. |
| `{language_name}` resolution broken in `.github/` copy | Medium | Medium | Run full pipeline for all 8 profiles; do not manually edit `.github/` goldens. |
| New content exceeds 250-line class limit | Low | Low | Template is markdown, not a class. Rule 03 limits apply to code, not templates. |
| x-lib-task-decomposer incompatible with new format | Medium | High | Read task decomposer skill before implementation; verify no hard dependency on old section headers (Happy Path, Error Path, etc.). |
| Vitest file discovery for new test file | Low | Low | New test at `tests/node/content/` must match `tests/**/*.test.ts` glob in vitest config. Verify pattern includes `content/` subdirectory. |

---

## 15. Dependencies and Prerequisites

### Prerequisites

- `story-0003-0001` completed (TDD/TPP sections in `resources/core/03-testing-philosophy.md`)
- `resources/skills-templates/core/x-test-plan/SKILL.md` exists with current category-based format
- `resources/github-skills-templates/testing/x-test-plan.md` exists with current condensed format
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
