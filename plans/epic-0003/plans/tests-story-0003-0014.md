# Test Plan — story-0003-0014

## Summary

This story modifies the `x-dev-lifecycle` Markdown skill templates to restructure all 8 phases for TDD: promoting Phase 1B to mandatory driver, changing Phase 1C input source, restructuring Phase 2 for Red-Green-Refactor loops, and adding TDD references to Phases 3-7. No TypeScript source code changes. Testing relies on content validation of the 3 modified source files, dual copy consistency checks, and byte-for-byte golden file parity across all 8 profiles x 3 output directories (24 golden files).

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (Claude source) | Verify TDD restructure sections exist in `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | YES | `tests/node/content/x-dev-lifecycle-content.test.ts` |
| Content validation (GitHub source) | Verify TDD restructure sections exist in `resources/github-skills-templates/dev/x-dev-lifecycle.md` | YES | `tests/node/content/x-dev-lifecycle-content.test.ts` (same file, separate describe block) |
| Dual copy consistency | Verify both sources contain semantically identical TDD content | YES | `tests/node/content/x-dev-lifecycle-content.test.ts` |
| Backward compatibility | Verify G1-G7 fallback path preserved, phase structure unchanged | YES | `tests/node/content/x-dev-lifecycle-content.test.ts` |
| Golden file integration | Verify pipeline output matches updated golden files | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Assembler unit tests | Verify copy logic works for x-dev-lifecycle | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |

---

## 2. New Tests — Content Validation

### 2.1 File: `tests/node/content/x-dev-lifecycle-content.test.ts`

This new test file validates the TDD restructure content in both source-of-truth templates. It follows the pattern established in `tests/node/content/template-tdd-sections.test.ts` and `tests/node/content/x-story-create-content.test.ts`.

#### 2.1.1 Claude Source Template — Phase 0 (Test Plan Check)

**Source file under test:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 1 | `claudeSource_phase0_containsTestPlanExistenceCheck` | Phase 0 includes a step to check if test plan exists at `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` |
| 2 | `claudeSource_phase0_setsTestPlanAvailableFlag` | Phase 0 documents a flag or condition indicating test plan availability (drives Phase 1B/2 behavior) |

#### 2.1.2 Claude Source Template — Phase 1B (Promoted to Driver)

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 3 | `claudeSource_phase1B_containsMandatoryDriverAnnotation` | Phase 1B is annotated as "MANDATORY DRIVER" for Phase 2 |
| 4 | `claudeSource_phase1B_referencesTestPlanAsImplementationRoadmap` | Phase 1B output is described as the implementation roadmap (not just documentation) |
| 5 | `claudeSource_phase1B_mentionsGateForPhase2` | Phase 1B includes a gate: if Phase 1B fails, Phase 2 must use fallback mode |
| 6 | `claudeSource_phase1B_mentionsDoubleLoopFormat` | Phase 1B references Double-Loop TDD format in its output description |
| 7 | `claudeSource_phase1B_mentionsTPPOrdering` | Phase 1B mentions TPP ordering in the test plan output |
| 8 | `claudeSource_phase1B_mentionsDependencyMarkers` | Phase 1B output includes dependency and/or parallelism markers |

#### 2.1.3 Claude Source Template — Phase 1C (Changed Input)

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 9 | `claudeSource_phase1C_derivesFromTestPlan` | Phase 1C notes that task decomposition is driven by the test plan (scenario-driven), not solely by the architecture plan |
| 10 | `claudeSource_phase1C_mentionsTDDTaskStructure` | Phase 1C output includes TDD task structure (RED/GREEN/REFACTOR) |
| 11 | `claudeSource_phase1C_mentionsAutoDetection` | Phase 1C documents that `x-lib-task-decomposer` auto-detects decomposition mode |
| 12 | `claudeSource_phase1C_mentionsFallbackToG1G7` | Phase 1C documents fallback to G1-G7 layer-based decomposition when no test plan exists |

#### 2.1.4 Claude Source Template — Phase 2 (TDD Implementation)

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 13 | `claudeSource_phase2_headingIncludesTDD` | Phase 2 heading includes "TDD" (replacing or augmenting "Group-Based") |
| 14 | `claudeSource_phase2_headingIncludesFallback` | Phase 2 heading mentions "Fallback" or "G1-G7" to indicate backward compatibility |
| 15 | `claudeSource_phase2_containsRedGreenRefactorLoop` | Phase 2 contains Red-Green-Refactor cycle description |
| 16 | `claudeSource_phase2_containsAcceptanceTestFirst` | Phase 2 starts with acceptance test (AT-1 or AT-N) written first, staying RED |
| 17 | `claudeSource_phase2_containsUnitTestInTPPOrder` | Phase 2 iterates unit tests (UT-N) in TPP order |
| 18 | `claudeSource_phase2_containsCompileCheckPerCycle` | Phase 2 includes compile check (`{{COMPILE_COMMAND}}` or `tsc --noEmit`) after each TDD cycle |
| 19 | `claudeSource_phase2_containsAtomicCommitPerCycle` | Phase 2 mandates atomic commit after each TDD cycle |
| 20 | `claudeSource_phase2_containsCoverageTargets` | Phase 2 specifies coverage targets (line >= 95%, branch >= 90%) |
| 21 | `claudeSource_phase2_readsTestPlan` | Phase 2 Step 1 includes reading the test plan as MANDATORY context |
| 22 | `claudeSource_phase2_referencesXDevImplement` | Phase 2 references `x-dev-implement` for TDD workflow details |

#### 2.1.5 Claude Source Template — Phase 2 Parallelism

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 23 | `claudeSource_phase2_containsParallelismSection` | Phase 2 includes a parallelism section for independent test scenarios |
| 24 | `claudeSource_phase2_parallelism_usesDependencyMarkers` | Parallelism decisions use `Parallel` markers from test plan and task breakdown |
| 25 | `claudeSource_phase2_parallelism_singleMessageLaunch` | Parallel subagents MUST be launched in a SINGLE message (RULE-009) |
| 26 | `claudeSource_phase2_parallelism_sequentialForDependencies` | Dependent scenarios run sequentially |

#### 2.1.6 Claude Source Template — Phase 2 Fallback

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 27 | `claudeSource_phase2_containsFallbackMode` | Phase 2 documents a fallback mode when no test plan is available |
| 28 | `claudeSource_phase2_fallback_emitsWarning` | Fallback mode emits a warning about missing TDD test plan |
| 29 | `claudeSource_phase2_fallback_preservesG1G7` | Fallback preserves the G1-G7 group-based implementation workflow |
| 30 | `claudeSource_phase2_fallback_mentionsBackwardCompatibility` | Fallback references backward compatibility (RULE-003 or equivalent) |

#### 2.1.7 Claude Source Template — Phases 3, 4, 5, 6, 7 (Updated References)

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 31 | `claudeSource_phase3_referencesUpdatedXReview` | Phase 3 mentions TDD checklist availability in x-review (story-0003-0015) |
| 32 | `claudeSource_phase3_backwardCompatibleGuard` | Phase 3 includes backward-compatible guard: if TDD checklist not available, existing criteria apply |
| 33 | `claudeSource_phase4_tddDisciplineForFixes` | Phase 4 instructs test-first for each fix |
| 34 | `claudeSource_phase4_atomicTDDCommitsForFixes` | Phase 4 uses atomic TDD commits for fixes |
| 35 | `claudeSource_phase5_prBodyIncludesTDDCompliance` | Phase 5 PR body includes TDD compliance section |
| 36 | `claudeSource_phase6_referencesUpdatedXReviewPr` | Phase 6 mentions TDD criteria availability in x-review-pr (story-0003-0016) |
| 37 | `claudeSource_phase6_backwardCompatibleGuard` | Phase 6 includes backward-compatible guard: if TDD criteria not available, existing checklist applies |
| 38 | `claudeSource_phase7_containsTDDDoDItems` | Phase 7 DoD checklist includes TDD-specific items |
| 39 | `claudeSource_phase7_tddDod_testFirstPattern` | Phase 7 checks for test-first pattern in commits |
| 40 | `claudeSource_phase7_tddDod_acceptanceTestsGreen` | Phase 7 checks for acceptance tests existing and passing |
| 41 | `claudeSource_phase7_tddDod_tppOrdering` | Phase 7 checks for TPP ordering in tests |

#### 2.1.8 Claude Source Template — Integration Notes

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 42 | `claudeSource_integrationNotes_includesXDevImplement` | Integration Notes list includes `x-dev-implement` |
| 43 | `claudeSource_integrationNotes_clarifiesGroupVerifierFallback` | Integration Notes clarify `x-lib-group-verifier` is used in fallback mode |

#### 2.1.9 Claude Source Template — Structural Preservation

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 44 | `claudeSource_preserves8PhaseCount` | "8 phases (0-7)" wording is preserved |
| 45 | `claudeSource_preservesCriticalExecutionRule` | "NEVER stop before Phase 7" rule is preserved |
| 46 | `claudeSource_preservesCompleteFlowDiagram` | Complete Flow block (Phase 0-7 listing) is present |
| 47 | `claudeSource_preservesRolesAndModelsTable` | Roles and Models table structure is preserved |
| 48 | `claudeSource_preservesPhase1ArchitectSubagent` | Phase 1 architect subagent prompt is unchanged |
| 49 | `claudeSource_preservesPhase1DEventSchema` | Phase 1D (Event Schema Design) is unchanged |
| 50 | `claudeSource_preservesPhase1ECompliance` | Phase 1E (Compliance Assessment) is unchanged |
| 51 | `claudeSource_preservesAllPlaceholderTokens` | All `{{PLACEHOLDER}}` tokens remain (PROJECT_NAME, LANGUAGE, COMPILE_COMMAND, TEST_COMMAND, COVERAGE_COMMAND, LANGUAGE_VERSION) |

---

### 2.2 GitHub Source Template Validation

**Source file under test:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`

Same 51 tests as Section 2.1, but targeting the GitHub copy. Use `describe.each` or a shared test factory to avoid duplication. Each test name prefixed with `githubSource_` instead of `claudeSource_`.

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 52-102 | Same as #1-#51 but with `githubSource_` prefix | Same validations against GitHub template, with GitHub-specific path references |

**Key differences from Claude tests:**
- Test #22 (references x-dev-implement): should verify the GitHub-equivalent path `.github/skills/x-dev-implement/SKILL.md`
- Test #48 (Phase 1 architect): should reference `.github/skills/architecture/SKILL.md` instead of `skills/architecture/references/architecture-principles.md`
- Test #44-#51 (structural): identical semantic validations

---

### 2.3 Dual Copy Consistency (RULE-001)

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 103 | `dualCopy_bothContainTDDPhase2Heading` | Both sources contain a Phase 2 heading with "TDD" |
| 104 | `dualCopy_bothContainRedGreenRefactorLoop` | Both sources describe Red-Green-Refactor cycle |
| 105 | `dualCopy_bothContainAcceptanceTestFirst` | Both sources document acceptance test written first |
| 106 | `dualCopy_bothContainG1G7Fallback` | Both sources preserve the G1-G7 fallback path |
| 107 | `dualCopy_bothContainPhase1BMandatoryDriver` | Both sources annotate Phase 1B as mandatory driver |
| 108 | `dualCopy_bothContainParallelismInstructions` | Both sources include Phase 2 parallelism instructions |
| 109 | `dualCopy_bothContainAtomicCommitInstructions` | Both sources mandate atomic TDD commits |
| 110 | `dualCopy_bothContainTPPOrdering` | Both sources reference TPP ordering |
| 111 | `dualCopy_bothContainCompileCheckPerCycle` | Both sources include compile check after each TDD cycle |
| 112 | `dualCopy_bothContainBackwardCompatibleGuards` | Both sources have backward-compatible guards for Phases 3 and 6 |
| 113 | `dualCopy_bothContainTDDDoDItems` | Both sources include TDD-specific DoD items in Phase 7 |
| 114 | `dualCopy_phaseCount_identical` | Both sources declare "8 phases (0-7)" |
| 115 | `dualCopy_pathDifferences_onlyExpected` | The only differences between the two templates are expected path references (`skills/` vs `.github/skills/`), frontmatter format, Global Output Policy, and Detailed References section |

---

## 3. Existing Tests — No Changes Needed

### 3.1 Golden File Integration Tests

- **File:** `tests/node/integration/byte-for-byte.test.ts`
- **What it validates:** Pipeline output matches golden files byte-for-byte for all 8 profiles
- **How it covers this story:** After updating both source templates and regenerating 24 golden files, the pipeline will produce output identical to the updated golden files
- **Expected result:** All 8 profiles pass (40 test assertions: 5 per profile)
- **Test logic unchanged:** The test infrastructure is generic and works with any content

### 3.2 Assembler Unit Tests

- **File:** `tests/node/assembler/skills-assembler.test.ts` -- Tests `SkillsAssembler` copy logic for `.claude/` output
- **File:** `tests/node/assembler/codex-skills-assembler.test.ts` -- Tests `CodexSkillsAssembler` mirror logic for `.agents/` output
- **File:** `tests/node/assembler/github-skills-assembler.test.ts` -- Tests `GithubSkillsAssembler` copy logic for `.github/` output
- **Impact:** None -- assembler logic unchanged; these tests continue to validate the copy mechanism works

---

## 4. Golden Files Requiring Update

**Total: 24 golden files** (8 profiles x 3 output directories)

### 4.1 `.claude/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` |

### 4.2 `.agents/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md` |

### 4.3 `.github/` golden files (8 files, identical to GitHub source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md` |

### 4.4 Golden File Update Strategy

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/dev/x-dev-lifecycle.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

---

## 5. Content Verification — Key Sections That Must Appear

The following sections and keywords MUST be present in the updated templates. Content tests use `toContain()` or `toMatch()` for substring/regex matching (not brittle exact line matching).

### 5.1 Phase 0 — New Step (Test Plan Check)

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `tests-story-XXXX-YYYY.md` or `test plan` | References the test plan file |
| `TEST_PLAN_AVAILABLE` or conditional flag wording | Documents availability flag |

### 5.2 Phase 1B — Mandatory Driver

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `MANDATORY` (case-insensitive) | Marks Phase 1B as mandatory |
| `driver` or `roadmap` | Describes test plan as implementation driver |
| `Double-Loop` | References Double-Loop TDD format |
| `TPP` or `Transformation Priority Premise` | References TPP ordering |
| `dependency` or `parallel` marker | References dependency/parallelism markers in output |
| `fallback` or `fail` + `G1-G7` | Gate: fallback if Phase 1B fails |

### 5.3 Phase 1C — Changed Input

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `test plan` + `scenario` or `test-driven` | Input derived from test plan |
| `RED` + `GREEN` + `REFACTOR` | TDD task structure |
| `auto-detect` or `mode` | Auto-detection of decomposition mode |

### 5.4 Phase 2 — TDD Implementation

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `TDD Implementation` or `TDD` in heading | Phase 2 heading |
| `Red-Green-Refactor` or `RED.*GREEN.*REFACTOR` | TDD cycle |
| `acceptance test` + `RED` or `AT-` | Acceptance test written first |
| `UT-` or `unit test.*TPP` | Unit tests in TPP order |
| `{{COMPILE_COMMAND}}` or `compile check` | Compile check per cycle |
| `atomic commit` | Atomic commits |
| `95%` + `90%` | Coverage targets |
| `x-dev-implement` | Reference to implementation skill |

### 5.5 Phase 2 — Parallelism

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `independent` + `parallel` | Independent scenarios can run in parallel |
| `SINGLE message` | RULE-009 compliance |
| `Depends On` or `dependency` + `sequential` | Dependent scenarios run sequentially |

### 5.6 Phase 2 — Fallback

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `fallback` + `G1-G7` | Fallback to group-based implementation |
| `warning` or `Warning` | Emit warning when no test plan |
| `backward` or `compatible` | Backward compatibility reference |

### 5.7 Phases 3, 6 — Updated References

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `TDD checklist` or `TDD criteria` | TDD validation in reviews |
| `story-0003-0015` or `if.*available` (Phase 3) | Conditional TDD checklist reference |
| `story-0003-0016` or `if.*available` (Phase 6) | Conditional TDD criteria reference |
| `existing criteria` or `existing checklist` | Backward-compatible guard |

### 5.8 Phase 4-5 — Minor Updates

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `test.*first` or `TDD discipline` (Phase 4) | Test-first for fixes |
| `TDD Compliance` or `TDD` (Phase 5 PR body) | TDD in PR description |

### 5.9 Phase 7 — TDD DoD Items

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `test-first pattern` | Git log shows tests before implementation |
| `acceptance test` + `pass` or `GREEN` | AT-N pass check |
| `TPP ordering` or `simple to complex` | TPP in test ordering |

---

## 6. Backward Compatibility Verification

These tests ensure no existing functionality is removed (RULE-003):

### 6.1 Phase Structure Preservation

| Verification | How Tested |
|--------------|-----------|
| 8 phases (0-7) count preserved | Test #44: regex match `8 phases.*0-7` |
| "NEVER stop before Phase 7" preserved | Test #45: exact substring match |
| Complete Flow diagram (Phase 0-7) present | Test #46: all phase entries in code block |
| Roles and Models table intact | Test #47: table with Architect, Developer, Tech Lead rows |

### 6.2 Unchanged Phases

| Phase | Verification | How Tested |
|-------|-------------|-----------|
| Phase 1 | Architect subagent prompt unchanged | Test #48: Senior Architect subagent text preserved |
| Phase 1D | Event Schema conditional preserved | Test #49: Event Engineer subagent text preserved |
| Phase 1E | Compliance conditional preserved | Test #50: Security Engineer subagent text preserved |

### 6.3 G1-G7 Fallback Path

| Verification | How Tested |
|--------------|-----------|
| G1-G7 terminology present in fallback | Tests #27-#30: fallback section content |
| Fallback triggers when no test plan | Test #27: fallback mode documented |
| G1-G7 implementation instructions preserved | Test #29: G1-G7 workflow in fallback |

### 6.4 Placeholder Tokens

| Token | How Tested |
|-------|-----------|
| `{{PROJECT_NAME}}` | Test #51: substring match |
| `{{LANGUAGE}}` | Test #51: substring match |
| `{{LANGUAGE_VERSION}}` | Test #51: substring match |
| `{{COMPILE_COMMAND}}` | Test #51: substring match |
| `{{TEST_COMMAND}}` | Test #51: substring match |
| `{{COVERAGE_COMMAND}}` | Test #51: substring match |

---

## 7. Dual Copy Consistency — Detailed Checks

The dual copy consistency tests (Section 2.3) verify RULE-001 by comparing both source templates:

### 7.1 Semantic Equivalence

Both copies must contain the same TDD instructions with only path references differing:

| Content | Claude Path | GitHub Path |
|---------|-------------|-------------|
| Architecture KP | `skills/architecture/references/architecture-principles.md` | `.github/skills/architecture/SKILL.md` |
| Layer templates KP | `skills/layer-templates/SKILL.md` | `.github/skills/layer-templates/SKILL.md` |
| Coding standards KP | `skills/coding-standards/references/coding-conventions.md` + `version-features.md` | `.github/skills/coding-standards/SKILL.md` |
| Protocols KP | `skills/protocols/references/event-driven-conventions.md` | `.github/skills/protocols/SKILL.md` |
| Security KP | `skills/security/SKILL.md` | `.github/skills/security/SKILL.md` |
| Compliance KP | `skills/compliance/SKILL.md` | `.github/skills/compliance/SKILL.md` |

### 7.2 Expected Structural Differences (NOT treated as inconsistencies)

| Aspect | Claude Code Template | GitHub Copilot Template |
|--------|---------------------|------------------------|
| Frontmatter | `allowed-tools`, `argument-hint` | `name`, `description` only |
| Global Output Policy | Present | Absent |
| Phase 0 steps | 5 steps (includes epic ID extraction + mkdir) | 3 steps (simpler) — **verify GitHub retains its simpler form** |
| Integration Notes | `placeholders resolved from project configuration` | `{{PLACEHOLDER}} tokens are runtime markers...` + Detailed References section |
| Detailed References | Absent | Present (links to `.github/skills/x-dev-*`) |
| Phase 7 completion message | `>>> Phase N/7 completed. Proceeding to Phase N+1...` | Distinct Phase 7 message: `>>> Phase 7/7 completed. Lifecycle complete.` |

---

## 8. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

describe("x-dev-lifecycle Claude source — Phase 0 TDD check", () => {
  // Tests #1-#2
});

describe("x-dev-lifecycle Claude source — Phase 1B mandatory driver", () => {
  // Tests #3-#8
});

describe("x-dev-lifecycle Claude source — Phase 1C changed input", () => {
  // Tests #9-#12
});

describe("x-dev-lifecycle Claude source — Phase 2 TDD implementation", () => {
  // Tests #13-#22
});

describe("x-dev-lifecycle Claude source — Phase 2 parallelism", () => {
  // Tests #23-#26
});

describe("x-dev-lifecycle Claude source — Phase 2 fallback", () => {
  // Tests #27-#30
});

describe("x-dev-lifecycle Claude source — Phases 3-7 TDD references", () => {
  // Tests #31-#41
});

describe("x-dev-lifecycle Claude source — integration notes", () => {
  // Tests #42-#43
});

describe("x-dev-lifecycle Claude source — structural preservation", () => {
  // Tests #44-#51
});

describe("x-dev-lifecycle GitHub source — TDD restructure", () => {
  // Tests #52-#102 (mirror of Claude tests #1-#51 with GitHub paths)
});

describe("x-dev-lifecycle dual copy consistency (RULE-001)", () => {
  // Tests #103-#115
});
```

---

## 9. TDD Execution Order

Following test-first approach:

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-dev-lifecycle-content.test.ts`) with all 115 test cases | RED (tests fail because source files not yet modified) |
| 2 | Edit Claude source template (`resources/skills-templates/core/x-dev-lifecycle/SKILL.md`) | Partial GREEN (Claude tests pass, GitHub tests still RED, structural preservation tests pass) |
| 3 | Edit GitHub source template (`resources/github-skills-templates/dev/x-dev-lifecycle.md`) | GREEN (all content + consistency tests pass) |
| 4 | Update deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) to match Claude source | N/A (deployed copy updated) |
| 5 | Copy sources to 24 golden files (script from Section 4.4) | N/A (golden files updated) |
| 6 | Run byte-for-byte integration tests | GREEN (golden file parity confirmed) |
| 7 | Run full test suite (`npx vitest run`) | GREEN (all existing tests pass, plus ~115 new tests) |

---

## 10. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-lifecycle-content.test.ts` -- all 115 content validation tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` -- full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch (no TypeScript code changes, so coverage unaffected)
- [ ] No compiler/linter warnings introduced
- [ ] Deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) matches Claude source template exactly

---

## 11. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after source edit | Mechanical copy script (Section 4.4) eliminates drift; byte-for-byte tests catch any mismatch immediately |
| Content test too brittle (exact string matching) | Use `toContain()` for substring checks and `toMatch()` for regex patterns; test semantic presence, not formatting |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (#103-#115) verify both copies have equivalent TDD content |
| Phase 2 fallback broken during TDD restructure | Dedicated fallback tests (#27-#30 for Claude, #78-#81 for GitHub) verify G1-G7 instructions preserved |
| Deployed copy diverges from source | Verification checklist item: deployed copy must match Claude source template exactly |
| Phase 2 subagent prompt too large | Content tests validate presence of `x-dev-implement` reference (orchestrator delegates details to the skill) |
| Parallelism instructions conflict with RULE-009 | Test #25 explicitly validates "SINGLE message" mandate for parallel subagent launches |

---

## 12. Files Summary

### 12.1 Files Modified (Source Templates + Deployed Copy)

| # | File | Description |
|---|------|-------------|
| 1 | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Claude Code source of truth (RULE-002) |
| 2 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | GitHub Copilot source of truth (RULE-002) |
| 3 | `.claude/skills/x-dev-lifecycle/SKILL.md` | Deployed copy for this project |

### 12.2 Golden Files Updated (24 files)

8 profiles x 3 output directories (`.claude/`, `.agents/`, `.github/`) = 24 golden files.

### 12.3 New Test File (1 file)

| File | Test Count |
|------|-----------|
| `tests/node/content/x-dev-lifecycle-content.test.ts` | 115 |

### 12.4 Existing Test Files (unchanged, covering this story)

| File | Test Count | Coverage |
|------|-----------|----------|
| `tests/node/integration/byte-for-byte.test.ts` | 40 (8 profiles x 5 assertions) | Golden file parity |
| `tests/node/assembler/skills-assembler.test.ts` | ~20 | Claude copy mechanism |
| `tests/node/assembler/codex-skills-assembler.test.ts` | ~15 | Agents copy mechanism |
| `tests/node/assembler/github-skills-assembler.test.ts` | ~15 | GitHub copy mechanism |

---

## 13. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation (Claude) | 51 | 0 |
| Content validation (GitHub) | 51 | 0 |
| Dual copy consistency | 13 | 0 |
| Golden file integration | 0 | 40 (8 profiles x 5 assertions) |
| Assembler unit tests | 0 | ~50 (across 3 assembler test files) |
| **Total** | **115** | **~90** |
