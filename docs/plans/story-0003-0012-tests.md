# Test Plan -- STORY-0003-0012: x-dev-implement -- Red-Green-Refactor Implementation

## Summary

- Affected template files: 2 (`resources/skills-templates/core/x-dev-implement/SKILL.md`, `resources/github-skills-templates/dev/x-dev-implement.md`)
- Affected golden files: 24 (8 profiles x 3 targets: `.claude/`, `.agents/`, `.github/`)
- TypeScript source changes: 0
- Total test methods: 35 (8 existing golden file integration + 19 new content validation + 8 dual copy consistency)
- Categories: Golden File Integration (8), Content Validation (19), Dual Copy Consistency (8)
- Coverage targets: >= 95% line, >= 90% branch (maintained -- no new production code)

---

## 1. Test File Locations and Naming

### Existing (unchanged)

**Path:** `tests/node/integration/byte-for-byte.test.ts`

**Rationale:** The existing byte-for-byte test suite (`describe.sequential.each` over all 8 `CONFIG_PROFILES`) automatically validates that pipeline output matches golden files. No code changes to this file -- only 24 golden files are updated to reflect the new TDD Loop structure.

### Planned

**Planned path:** `tests/node/content/x-dev-implement-tdd-loop.test.ts`

**Rationale:** Content validation tests that verify the structural integrity of the restructured x-dev-implement skill. These unit-level tests read the source template files and assert section presence, ordering, TDD workflow markers, and backward compatibility. They are separated from byte-for-byte tests because they validate semantic content, not binary equality.

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

const CLAUDE_TEMPLATE = resolve(RESOURCES_DIR, "skills-templates", "core", "x-dev-implement", "SKILL.md");
const GITHUB_TEMPLATE = resolve(RESOURCES_DIR, "github-skills-templates", "dev", "x-dev-implement.md");
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

1. `{profile}/.claude/skills/x-dev-implement/SKILL.md`
2. `{profile}/.agents/skills/x-dev-implement/SKILL.md`

The GitHub template is processed by `GithubSkillsAssembler`, producing:

3. `{profile}/.github/skills/x-dev-implement/SKILL.md`

Golden files live at `tests/golden/{profile}/` for all 8 profiles defined in `tests/helpers/integration-constants.ts`.

### 2.4 Profile-Specific Differences

| Target | Profile-specific? | Reason |
|--------|-------------------|--------|
| `.claude/` | No -- all 8 profiles identical | Template has no single-brace `{placeholder}` resolved per profile |
| `.agents/` | No -- mirrors `.claude/` exactly | Byte-for-byte copy via `CodexSkillsAssembler` |
| `.github/` | No -- all 8 profiles identical | Template has no single-brace `{placeholder}` that differ per profile; only `{{DOUBLE_BRACE}}` which are preserved |

---

## 3. Acceptance Tests (Outer Loop)

Each acceptance test maps directly to a Gherkin scenario from the story. These are the "outer loop" validations -- they describe the end-to-end behavior the story must deliver. They remain RED until all supporting unit tests pass.

### AT-1: Skill requires test plan as input

- **Gherkin**: "Cenario: Skill exige test plan como input"
- **Status**: RED until UT-5, UT-6 complete
- **Components**: Claude template Step 1 (subagent prompt), GitHub template Step 1
- **Acceptance Criteria**: Step 1 subagent prompt includes a new step to read the test plan as a MANDATORY input. If no test plan exists, a warning is emitted suggesting `/x-test-plan` first, with a fallback to the old workflow.
- **Validation**: Content validation tests verify the Claude template contains a test plan reading step marked as mandatory, and a fallback warning block. GitHub template contains equivalent content.

### AT-2: Acceptance test written before unit tests (Double-Loop)

- **Gherkin**: "Cenario: Acceptance test escrito antes de unit tests"
- **Status**: RED until UT-7, UT-8 complete
- **Components**: Claude template Step 2 section 2.0, GitHub template Step 2
- **Acceptance Criteria**: Step 2 begins with writing acceptance tests FIRST (section 2.0), before any unit test cycles. The acceptance test starts RED and stays RED until inner loop completes.
- **Validation**: Content validation tests verify section 2.0 exists with Double-Loop instructions, including the AT-first pattern and RED status assertion.

### AT-3: Unit tests executed in TPP order

- **Gherkin**: "Cenario: Unit tests executados em ordem TPP"
- **Status**: RED until UT-9, UT-10 complete
- **Components**: Claude template Step 2 section 2.1, GitHub template Step 2
- **Acceptance Criteria**: Section 2.1 (Inner Loop) describes Red-Green-Refactor per unit test in strict TPP order. The TPP ordering is explicitly referenced as mandatory.
- **Validation**: Content validation tests verify the inner loop section references TPP order and contains RED, GREEN, REFACTOR sub-sections.

### AT-4: Each Red-Green-Refactor cycle is complete

- **Gherkin**: "Cenario: Cada ciclo Red-Green-Refactor completo"
- **Status**: RED until UT-9, UT-10, UT-11 complete
- **Components**: Claude template Step 2 section 2.1 (RED, GREEN, REFACTOR sub-sections)
- **Acceptance Criteria**: Each cycle contains: (1) write test BEFORE implementation, (2) implement MINIMUM to pass, (3) refactor evaluated (may be noop), (4) compile check after cycle.
- **Validation**: Content validation tests verify all four sub-sections (RED, GREEN, REFACTOR, Compile Check) exist within the inner loop section.

### AT-5: Compile check after each cycle

- **Gherkin**: "Cenario: Compile check apos cada ciclo"
- **Status**: RED until UT-11 complete
- **Components**: Claude template Step 2 (Compile Check sub-section)
- **Acceptance Criteria**: A compile check step (`{{COMPILE_COMMAND}}`) appears after each Red-Green-Refactor cycle, expecting zero errors and zero warnings.
- **Validation**: Content validation tests verify `{{COMPILE_COMMAND}}` appears within the TDD loop section (not just in the old Step 3).

### AT-6: Atomic commit per TDD cycle

- **Gherkin**: "Cenario: Atomic commit por ciclo TDD"
- **Status**: RED until UT-14, UT-15 complete
- **Components**: Claude template Step 4, GitHub template Step 4
- **Acceptance Criteria**: Step 4 describes atomic commits per TDD cycle (not per layer). Each commit contains the test AND its implementation. Commit format follows Conventional Commits with RED/GREEN/REFACTOR annotations.
- **Validation**: Content validation tests verify Step 4 references TDD cycle commits with test+implementation bundling and Conventional Commits format.

### AT-7: Coverage validated at the end

- **Gherkin**: "Cenario: Coverage validada ao final"
- **Status**: RED until UT-12, UT-13 complete
- **Components**: Claude template Step 3, GitHub template Step 3
- **Acceptance Criteria**: Step 3 validates that all acceptance tests are GREEN, coverage >= 95% line / >= 90% branch, and all tests pass. The DoD table includes TDD-specific criteria (test-first pattern, refactoring per cycle).
- **Validation**: Content validation tests verify Step 3 contains TDD-specific DoD entries beyond the original generic validation.

---

## 4. Unit Tests (Inner Loop -- TPP Order)

These are the concrete tests that drive implementation. They follow TPP order: degenerate cases first, then increasingly complex validations.

### TPP Level 1 -- Degenerate Cases (section exists or not)

#### UT-1: YAML frontmatter description updated -- TPP Level 1

- **Test**: `yamlFrontmatter_description_containsTDDWorkflow`
- **Implementation**: Verify the YAML `description` field contains "TDD" and "Red-Green-Refactor"
- **Transform**: `{}->nil` (if description unchanged, fail immediately)
- **Components**: Claude template (YAML frontmatter)
- **Depends on**: --
- **Parallel**: yes

#### UT-2: Execution flow overview updated -- TPP Level 1

- **Test**: `executionFlow_step2Name_containsTDDLoop`
- **Implementation**: Verify the execution flow block contains "TDD LOOP" (not "IMPLEMENT") as Step 2
- **Transform**: `{}->nil` (if old step name persists, fail)
- **Components**: Claude template (execution flow block)
- **Depends on**: --
- **Parallel**: yes

#### UT-3: Allowed-tools preserved in YAML frontmatter -- TPP Level 1

- **Test**: `yamlFrontmatter_allowedTools_containsReadWriteEditBashGrepGlob`
- **Implementation**: Verify YAML frontmatter contains `allowed-tools: Read, Write, Edit, Bash, Grep, Glob`
- **Transform**: `{}->nil` (if tools missing, fail)
- **Components**: Claude template (YAML frontmatter)
- **Depends on**: --
- **Parallel**: yes

#### UT-4: Step 1 subagent prompt preserved (original Steps 1, 3, 4) -- TPP Level 1

- **Test**: `step1Subagent_originalSteps_preservedStoryConventionsExistingCode`
- **Implementation**: Verify Step 1 still contains: "Read the story/requirements", "Read project conventions" (with KP references to architecture-principles.md, coding-conventions.md, version-features.md, layer-templates), "Review existing code", and branch creation instructions
- **Transform**: `{}->nil` (if subagent prompt removed or broken, fail)
- **Components**: Claude template Step 1
- **Depends on**: --
- **Parallel**: yes

### TPP Level 2 -- Unconditional Paths (new section appears)

#### UT-5: Test plan reading step added to subagent prompt -- TPP Level 2

- **Test**: `step1Subagent_testPlanStep_containsMandatoryTestPlanReading`
- **Implementation**: Verify Step 1 subagent prompt contains a new step for reading the test plan, marked as "MANDATORY" or "mandatory". Must reference a test plan path pattern.
- **Transform**: `constant->variable` (new step added to subagent prompt)
- **Components**: Claude template Step 1
- **Depends on**: UT-4
- **Parallel**: no

#### UT-6: Fallback warning when test plan absent -- TPP Level 2

- **Test**: `step1Subagent_fallbackWarning_containsWarningAndSuggestion`
- **Implementation**: Verify the template contains a fallback mode section with a WARNING message suggesting `/x-test-plan` and describing degraded behavior (layer-by-layer, test-with instead of test-first)
- **Transform**: `constant->variable` (new fallback block appears)
- **Components**: Claude template Step 1 or Step 2
- **Depends on**: UT-5
- **Parallel**: no

#### UT-7: Step 2 header renamed to TDD Loop -- TPP Level 2

- **Test**: `step2Header_sectionTitle_containsTDDLoopNotImplement`
- **Implementation**: Verify the template contains `## Step 2: TDD Loop` (or equivalent) and does NOT contain `## Step 2: Implement`
- **Transform**: `constant->variable` (section header replaced)
- **Components**: Claude template Step 2
- **Depends on**: UT-2
- **Parallel**: no

#### UT-8: Acceptance test first section (2.0) exists -- TPP Level 2

- **Test**: `step2_acceptanceTestFirst_containsDoubleLoopOuterLoop`
- **Implementation**: Verify Step 2 contains a section 2.0 (or equivalent) for writing acceptance tests FIRST. Must reference "Double-Loop", "RED", and "acceptance test"
- **Transform**: `constant->variable` (new sub-section appears)
- **Components**: Claude template Step 2 section 2.0
- **Depends on**: UT-7
- **Parallel**: no

### TPP Level 3 -- Simple Conditions (multiple sub-sections with distinct content)

#### UT-9: Inner loop section (2.1) with RED sub-section -- TPP Level 3

- **Test**: `step2InnerLoop_redPhase_containsFailingTestInstructions`
- **Implementation**: Verify section 2.1 contains a RED sub-section with instructions to write a failing test, run it, and confirm failure. Must reference `{{TEST_COMMAND}}`.
- **Transform**: `unconditional->conditional` (branching: RED phase has distinct content from GREEN/REFACTOR)
- **Components**: Claude template Step 2 section 2.1
- **Depends on**: UT-7
- **Parallel**: yes (parallel with UT-10, UT-11)

#### UT-10: Inner loop section (2.1) with GREEN sub-section -- TPP Level 3

- **Test**: `step2InnerLoop_greenPhase_containsMinimumImplementationInstructions`
- **Implementation**: Verify section 2.1 contains a GREEN sub-section with instructions to write MINIMUM production code, respect layer order, and run ALL tests. Must reference `{{TEST_COMMAND}}`.
- **Transform**: `unconditional->conditional` (branching: GREEN phase content)
- **Components**: Claude template Step 2 section 2.1
- **Depends on**: UT-7
- **Parallel**: yes (parallel with UT-9, UT-11)

#### UT-11: Inner loop section (2.1) with REFACTOR + Compile Check sub-sections -- TPP Level 3

- **Test**: `step2InnerLoop_refactorAndCompile_containsDesignImprovementAndCompileCheck`
- **Implementation**: Verify section 2.1 contains: (1) a REFACTOR sub-section (extract method, DRY, naming; no new behavior), and (2) a Compile Check sub-section with `{{COMPILE_COMMAND}}`. Both must appear after GREEN.
- **Transform**: `unconditional->conditional` (two distinct sub-sections after GREEN)
- **Components**: Claude template Step 2 section 2.1
- **Depends on**: UT-7
- **Parallel**: yes (parallel with UT-9, UT-10)

#### UT-12: Step 3 Validate contains TDD-specific DoD table -- TPP Level 3

- **Test**: `step3Validate_dodTable_containsTDDSpecificCriteria`
- **Implementation**: Verify Step 3 DoD table contains TDD-specific entries: "Tests written BEFORE implementation" and "Refactoring evaluated per cycle". These are new entries beyond the original 6-row table.
- **Transform**: `unconditional->conditional` (new rows in DoD table)
- **Components**: Claude template Step 3
- **Depends on**: UT-2
- **Parallel**: yes

#### UT-13: Step 3 Validate references acceptance tests GREEN -- TPP Level 3

- **Test**: `step3Validate_dodTable_containsAcceptanceTestsGreen`
- **Implementation**: Verify Step 3 DoD table contains "acceptance tests" and "GREEN" entries, confirming the Double-Loop outer loop completion is part of the DoD.
- **Transform**: `unconditional->conditional` (new acceptance test row in DoD)
- **Components**: Claude template Step 3
- **Depends on**: UT-12
- **Parallel**: no

#### UT-14: Step 4 Commit describes atomic TDD commits -- TPP Level 3

- **Test**: `step4Commit_commitPattern_containsTDDCycleCommits`
- **Implementation**: Verify Step 4 references "TDD cycle" or "Red-Green-Refactor cycle" commits, not "per layer/feature" commits. Must contain examples with RED/GREEN/REFACTOR annotations in the commit message.
- **Transform**: `unconditional->conditional` (old commit pattern replaced with TDD pattern)
- **Components**: Claude template Step 4
- **Depends on**: UT-2
- **Parallel**: yes

#### UT-15: Step 4 Commit includes acceptance test commit ordering -- TPP Level 3

- **Test**: `step4Commit_commitOrdering_containsATFirstThenUTsInTPPOrder`
- **Implementation**: Verify Step 4 describes commit ordering: acceptance test commit first, then unit test + implementation commits in TPP order.
- **Transform**: `unconditional->conditional` (new ordering constraint)
- **Components**: Claude template Step 4
- **Depends on**: UT-14
- **Parallel**: no

### TPP Level 4 -- Complex Conditions (cross-section validation, GitHub parity)

#### UT-16: Integration Notes reference x-test-plan as prerequisite -- TPP Level 4

- **Test**: `integrationNotes_prerequisite_containsXTestPlanReference`
- **Implementation**: Verify the Integration Notes section references `/x-test-plan` as a prerequisite.
- **Transform**: `constant->conditional` (new prerequisite added to existing section)
- **Components**: Claude template Integration Notes
- **Depends on**: UT-5
- **Parallel**: yes

#### UT-17: Old Step 2 "Implement" content removed -- TPP Level 4

- **Test**: `step2_oldContent_doesNotContainLayerByLayerAsMainWorkflow`
- **Implementation**: Verify the template does NOT contain `## Step 2: Implement` as a section header. The old "5. Tests (written alongside or test-first)" entry should be absent from the main workflow (it may exist in fallback mode only).
- **Transform**: `unconditional->conditional` (verifying absence)
- **Components**: Claude template Step 2
- **Depends on**: UT-7
- **Parallel**: yes

#### UT-18: GitHub template -- TDD Loop structure present -- TPP Level 4

- **Test**: `githubTemplate_step2_containsTDDLoopStructure`
- **Implementation**: Verify GitHub template contains: "TDD Loop" in Step 2 header, "acceptance test" in section 2.0, "Red-Green-Refactor" in section 2.1, and "Compile Check" in the inner loop.
- **Transform**: `scalar->collection` (checking multiple structural elements across GitHub copy)
- **Components**: GitHub template
- **Depends on**: --
- **Parallel**: yes

#### UT-19: GitHub template -- Detailed References includes x-test-plan -- TPP Level 4

- **Test**: `githubTemplate_detailedReferences_containsXTestPlanReference`
- **Implementation**: Verify the GitHub template's "Detailed References" section includes `.github/skills/x-test-plan/SKILL.md` as a reference.
- **Transform**: `scalar->collection` (new reference added to existing list)
- **Components**: GitHub template (Detailed References section)
- **Depends on**: --
- **Parallel**: yes

---

## 5. Golden File Integration Tests (Existing Suite -- Updated Golden Files)

The existing `byte-for-byte.test.ts` runs `describe.sequential.each` over all 8 `CONFIG_PROFILES`. Each profile runs 5 assertions. Golden files must be regenerated to include the new TDD Loop format.

**Action required:** Update all 24 golden `x-dev-implement/SKILL.md` files (8 profiles x 3 targets) to match the updated templates.

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

**Verification logic:** `verifyOutput()` in `src/verifier.ts` compares two directory trees byte-for-byte using `Buffer.equals()`. Any difference in the golden `x-dev-implement/SKILL.md` files will produce a `FileDiff` mismatch and fail the test. The `formatVerificationFailures()` helper renders up to 500 chars of unified diff for debugging.

---

## 6. Dual Copy Consistency Tests

Verify RULE-001 compliance: `.claude/` and `.agents/` copies must be byte-for-byte identical for all 8 profiles. This is validated transitively by the byte-for-byte suite (both are generated from the same template), but explicit validation adds defense-in-depth.

**Path:** Include in `tests/node/content/x-dev-implement-tdd-loop.test.ts`

```typescript
import { CONFIG_PROFILES, GOLDEN_DIR } from "../../helpers/integration-constants.js";

describe("dual copy consistency -- x-dev-implement", () => {
  for (const profile of CONFIG_PROFILES) {
    it(`dualCopy_${profile}_claudeAndAgentsCopiesIdentical`, () => {
      const claudeCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".claude/skills/x-dev-implement/SKILL.md"),
        "utf-8",
      );
      const agentsCopy = readFileSync(
        resolve(GOLDEN_DIR, profile, ".agents/skills/x-dev-implement/SKILL.md"),
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
| G1: Frontmatter & Flow | YAML description, execution flow, allowed-tools | 3 | Content Validation | Level 1 |
| G2: Subagent Preservation | Step 1 original steps preserved (story, conventions, code review) | 1 | Content Validation | Level 1 |
| G3: Test Plan Input | Mandatory test plan step in subagent, fallback warning | 2 | Content Validation | Level 2 |
| G4: TDD Loop Structure | Step 2 header, acceptance test first (2.0) | 2 | Content Validation | Level 2 |
| G5: Inner Loop Phases | RED, GREEN, REFACTOR + Compile Check sub-sections | 3 | Content Validation | Level 3 |
| G6: Validate Step | TDD-specific DoD table, acceptance tests GREEN | 2 | Content Validation | Level 3 |
| G7: Commit Step | Atomic TDD commits, AT-first ordering | 2 | Content Validation | Level 3 |
| G8: Cross-Section | Integration Notes prerequisite, old content removed | 2 | Content Validation | Level 4 |
| G9: GitHub Template | TDD Loop structure, x-test-plan reference | 2 | Content Validation | Level 4 |
| G10: Golden File Integration | Byte-for-byte parity across 8 profiles | 8 | Integration (existing) | -- |
| G11: Dual Copy Consistency | `.claude/` == `.agents/` for all 8 profiles | 8 | Content Validation | -- |
| **Total** | | **35** | | |

---

## 8. Acceptance Criteria Traceability

| Gherkin Scenario | AT ID | Supporting UT IDs | Test Group(s) |
|------------------|-------|-------------------|---------------|
| Skill exige test plan como input | AT-1 | UT-5, UT-6 | G3 |
| Acceptance test escrito antes de unit tests | AT-2 | UT-7, UT-8 | G4 |
| Unit tests executados em ordem TPP | AT-3 | UT-9, UT-10 | G5 |
| Cada ciclo Red-Green-Refactor completo | AT-4 | UT-9, UT-10, UT-11 | G5 |
| Compile check apos cada ciclo | AT-5 | UT-11 | G5 |
| Atomic commit por ciclo TDD | AT-6 | UT-14, UT-15 | G7 |
| Coverage validada ao final | AT-7 | UT-12, UT-13 | G6 |
| Golden file parity (implicit DoD) | -- | All golden file tests | G10 |
| Dual copy consistency (RULE-001) | -- | All dual copy tests | G11 |

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

1. Edit `resources/skills-templates/core/x-dev-implement/SKILL.md` -- restructure Steps 1-4 for TDD workflow with Double-Loop, TPP ordering, and atomic commits
2. Edit `resources/github-skills-templates/dev/x-dev-implement.md` -- mirror structural changes in condensed form with GitHub-specific paths

### Step 2: Regenerate Golden Files

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  npx tsx src/cli.ts generate \
    --config resources/config-templates/setup-config.${profile}.yaml \
    --output /tmp/golden-${profile} \
    --resources resources

  cp /tmp/golden-${profile}/.claude/skills/x-dev-implement/SKILL.md \
     tests/golden/${profile}/.claude/skills/x-dev-implement/SKILL.md
  cp /tmp/golden-${profile}/.agents/skills/x-dev-implement/SKILL.md \
     tests/golden/${profile}/.agents/skills/x-dev-implement/SKILL.md
  cp /tmp/golden-${profile}/.github/skills/x-dev-implement/SKILL.md \
     tests/golden/${profile}/.github/skills/x-dev-implement/SKILL.md
done
```

### Step 3: Verify

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 8 profiles must pass with `success: true`.

### Shortcut: Manual Copy for `.claude/` and `.agents/`

Since the Claude template has no profile-specific placeholders resolved at the `.claude/` and `.agents/` level (only `{{DOUBLE_BRACE}}` which stays unresolved), the source template can be copied directly to all 16 golden paths:

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  cp resources/skills-templates/core/x-dev-implement/SKILL.md \
     tests/golden/${profile}/.claude/skills/x-dev-implement/SKILL.md
  cp resources/skills-templates/core/x-dev-implement/SKILL.md \
     tests/golden/${profile}/.agents/skills/x-dev-implement/SKILL.md
done
```

For `.github/` golden files, the same shortcut applies since x-dev-implement has no profile-specific `{placeholder}` resolution. However, using the pipeline is safer as it validates the full generation path.

---

## 11. Execution Commands

### Run Content Validation Tests Only

```bash
npx vitest run tests/node/content/x-dev-implement-tdd-loop.test.ts
```

### Run Golden File Tests Only

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

### Run Both

```bash
npx vitest run tests/node/content/x-dev-implement-tdd-loop.test.ts tests/node/integration/byte-for-byte.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 12. Naming Convention Reference

All new test names follow `[sectionUnderTest]_[scenario]_[expectedBehavior]`:

```
yamlFrontmatter_description_containsTDDWorkflow
executionFlow_step2Name_containsTDDLoop
yamlFrontmatter_allowedTools_containsReadWriteEditBashGrepGlob
step1Subagent_originalSteps_preservedStoryConventionsExistingCode
step1Subagent_testPlanStep_containsMandatoryTestPlanReading
step1Subagent_fallbackWarning_containsWarningAndSuggestion
step2Header_sectionTitle_containsTDDLoopNotImplement
step2_acceptanceTestFirst_containsDoubleLoopOuterLoop
step2InnerLoop_redPhase_containsFailingTestInstructions
step2InnerLoop_greenPhase_containsMinimumImplementationInstructions
step2InnerLoop_refactorAndCompile_containsDesignImprovementAndCompileCheck
step3Validate_dodTable_containsTDDSpecificCriteria
step3Validate_dodTable_containsAcceptanceTestsGreen
step4Commit_commitPattern_containsTDDCycleCommits
step4Commit_commitOrdering_containsATFirstThenUTsInTPPOrder
integrationNotes_prerequisite_containsXTestPlanReference
step2_oldContent_doesNotContainLayerByLayerAsMainWorkflow
githubTemplate_step2_containsTDDLoopStructure
githubTemplate_detailedReferences_containsXTestPlanReference
dualCopy_{profile}_claudeAndAgentsCopiesIdentical  (x8 profiles)
```

---

## 13. Pre-conditions

### For All Tests

- story-0003-0006 completed (agents with TDD workflow)
- story-0003-0007 completed (x-test-plan with TPP)
- story-0003-0008 completed (x-lib-task-decomposer with TDD tasks)
- All 8 profile config templates exist in `resources/config-templates/`
- Golden file directories exist for all 8 profiles under `tests/golden/`
- `tests/helpers/integration-constants.ts` exports `CONFIG_PROFILES`, `GOLDEN_DIR`, `RESOURCES_DIR`

### For Content Validation Tests

- `resources/skills-templates/core/x-dev-implement/SKILL.md` has been modified with TDD Loop structure
- `resources/github-skills-templates/dev/x-dev-implement.md` has been modified with TDD Loop structure

### For Golden File Tests

- All 24 golden files updated to reflect the modified templates
- Pipeline successfully generates output for all 8 profiles

### Import Dependencies (for new test file)

| Module | Import | Used For |
|--------|--------|----------|
| `node:fs` | `readFileSync` | Reading template file content |
| `node:path` | `resolve`, `dirname` | Path resolution |
| `node:url` | `fileURLToPath` | ESM `__dirname` equivalent |
| `vitest` | `describe`, `it`, `expect`, `beforeAll` | Test framework |
| `tests/helpers/integration-constants.ts` | `CONFIG_PROFILES`, `GOLDEN_DIR` | Profile list and golden paths (for dual copy tests) |

---

## 14. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden files not updated before commit | High | High | CI fails immediately on byte-for-byte mismatch. Developer must regenerate golden files as part of the story. |
| Content assertions too brittle | Medium | Medium | Use `toContain()` for section headers and key phrases, not full-line matching. Use case-insensitive regex for flexible assertions where appropriate. |
| Step 1 subagent prompt accidentally truncated | Low | High | UT-4 validates original subagent steps (story, conventions, code review) are preserved. Use substring matching. |
| `{{PLACEHOLDER}}` accidentally removed | Low | High | UT-3 verifies allowed-tools preserved. UT-9/UT-10/UT-11 verify `{{TEST_COMMAND}}` and `{{COMPILE_COMMAND}}` in TDD loop. Byte-for-byte tests catch any unintended placeholder changes. |
| Fallback mode description ambiguous | Medium | Medium | UT-6 explicitly checks for WARNING text and fallback behavior description (layer-by-layer + test-with). |
| Old Step 2 content accidentally retained alongside TDD Loop | Medium | Medium | UT-17 verifies the old `## Step 2: Implement` header is absent. |
| GitHub template diverges structurally from Claude template | Medium | High | UT-18 validates TDD Loop structure present in GitHub copy. AT traceability ensures both copies serve the same Gherkin scenarios. |
| Vitest file discovery for new test file | Low | Low | New test at `tests/node/content/` must match `tests/**/*.test.ts` glob in vitest config. Verify pattern includes `content/` subdirectory. |

---

## 15. TPP Implementation Order for Tests

The recommended order to implement the content validation tests (following TPP progression):

| Phase | UTs | TPP Level | What Gets Validated |
|-------|-----|-----------|---------------------|
| 1 | UT-1, UT-2, UT-3, UT-4 | Level 1 | Degenerate: frontmatter updated, flow updated, tools preserved, subagent preserved |
| 2 | UT-5, UT-6, UT-7, UT-8 | Level 2 | Unconditional: test plan input, fallback, TDD Loop header, acceptance test first |
| 3 | UT-9, UT-10, UT-11, UT-12, UT-13, UT-14, UT-15 | Level 3 | Conditional: RED/GREEN/REFACTOR phases, DoD table, TDD commits |
| 4 | UT-16, UT-17, UT-18, UT-19 | Level 4 | Complex: cross-section refs, old content removed, GitHub parity |
