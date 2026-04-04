# Test Plan — story-0004-0005

## Summary

This story adds a new **Phase 3 — Documentation** to the `x-dev-lifecycle` skill templates, positioned between Phase 2 (Implementation) and the former Phase 3 (Review, now Phase 4). All subsequent phases are renumbered +1, total phase count changes from 8 (0-7) to 9 (0-8), and the "NEVER stop before Phase N" rule updates accordingly. Testing validates template content via unit-level substring/regex checks, dual copy consistency (RULE-001), and byte-for-byte golden file parity across all 8 profiles.

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (Claude source) | Verify Documentation phase, renumbering, dispatch, changelog in `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | YES | `tests/node/content/x-dev-lifecycle-doc-phase.test.ts` |
| Content validation (GitHub source) | Same validations against `resources/github-skills-templates/dev/x-dev-lifecycle.md` | YES | `tests/node/content/x-dev-lifecycle-doc-phase.test.ts` (separate describe block) |
| Dual copy consistency | Both sources contain semantically identical Documentation phase content | YES | `tests/node/content/x-dev-lifecycle-doc-phase.test.ts` |
| Golden file integration | Pipeline output matches updated golden files | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |

---

## 2. Test Scenarios

### AT-1: Lifecycle SKILL.md contains new Phase 3 — Documentation section

> **Category:** Acceptance Test (integration-level)
> **Depends On:** none
> **Parallel:** no (outer-loop acceptance test)

**Description:** After modifying both source templates and regenerating golden files, running the full pipeline for any profile produces a `x-dev-lifecycle/SKILL.md` output that contains the new Documentation phase, correct phase count (9), and renumbered phases.

**Verification:**
- Run `npx vitest run tests/node/content/x-dev-lifecycle-doc-phase.test.ts` -- all content tests pass
- Run `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass
- Combined: confirms end-to-end that the new phase is correctly generated and distributed

---

### UT-1: Phase header "## Phase 3 — Documentation" exists in template

> **Category:** Unit Test — Content Validation (Level 1: Degenerate)
> **Depends On:** none
> **Parallel:** yes (independent of UT-2 through UT-11)

**Claude source:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

| # | Test Name | Assertion |
|---|-----------|-----------|
| 1 | `claudeSource_phase3_containsDocumentationHeading` | `expect(claudeContent).toMatch(/## Phase 3 .*Documentation/)` |

**GitHub source:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`

| # | Test Name | Assertion |
|---|-----------|-----------|
| 2 | `githubSource_phase3_containsDocumentationHeading` | `expect(githubContent).toMatch(/## Phase 3 .*Documentation/)` |

---

### UT-2: Header says "9 phases (0-8)" instead of "8 phases (0-7)"

> **Category:** Unit Test — Content Validation (Level 2: Unconditional)
> **Depends On:** none
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 3 | `claudeSource_criticalRule_contains9Phases0to8` | `expect(claudeContent).toMatch(/9 phases \(0-8\)/)` |
| 4 | `claudeSource_criticalRule_doesNotContain8Phases0to7` | `expect(claudeContent).not.toMatch(/8 phases \(0-7\)/)` |
| 5 | `githubSource_criticalRule_contains9Phases0to8` | `expect(githubContent).toMatch(/9 phases \(0-8\)/)` |
| 6 | `githubSource_criticalRule_doesNotContain8Phases0to7` | `expect(githubContent).not.toMatch(/8 phases \(0-7\)/)` |

---

### UT-3: "NEVER stop before Phase 8" rule present (was Phase 7)

> **Category:** Unit Test — Content Validation (Level 2: Unconditional)
> **Depends On:** none
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 7 | `claudeSource_criticalRule_neverStopBeforePhase8` | `expect(claudeContent).toContain("NEVER stop before Phase 8")` |
| 8 | `claudeSource_criticalRule_doesNotReferenceOldPhase7Stop` | `expect(claudeContent).not.toContain("NEVER stop before Phase 7")` |
| 9 | `githubSource_criticalRule_neverStopBeforePhase8` | `expect(githubContent).toContain("NEVER stop before Phase 8")` |
| 10 | `githubSource_criticalRule_doesNotReferenceOldPhase7Stop` | `expect(githubContent).not.toContain("NEVER stop before Phase 7")` |

---

### UT-4: Phase renumbering — old Phase 3 (Review) is now Phase 4

> **Category:** Unit Test — Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1 (new Phase 3 must exist first)
> **Parallel:** yes (with UT-5 through UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 11 | `claudeSource_phase4_containsReviewHeading` | `expect(claudeContent).toMatch(/## Phase 4 .*Review/)` |
| 12 | `claudeSource_noOldPhase3Review` | `expect(claudeContent).not.toMatch(/## Phase 3 .*Review/)` |
| 13 | `githubSource_phase4_containsReviewHeading` | `expect(githubContent).toMatch(/## Phase 4 .*Review/)` |

---

### UT-5: Phase renumbering — old Phase 4 (Fixes) is now Phase 5

> **Category:** Unit Test — Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-4, UT-6 through UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 14 | `claudeSource_phase5_containsFixesHeading` | `expect(claudeContent).toMatch(/## Phase 5 .*Fixes/)` |
| 15 | `claudeSource_noOldPhase4Fixes` | `expect(claudeContent).not.toMatch(/## Phase 4 .*Fixes/)` |
| 16 | `githubSource_phase5_containsFixesHeading` | `expect(githubContent).toMatch(/## Phase 5 .*Fixes/)` |

---

### UT-6: Phase renumbering — old Phase 5 (Commit) is now Phase 6

> **Category:** Unit Test — Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-4, UT-5, UT-7, UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 17 | `claudeSource_phase6_containsCommitHeading` | `expect(claudeContent).toMatch(/## Phase 6 .*Commit/)` |
| 18 | `claudeSource_noOldPhase5Commit` | `expect(claudeContent).not.toMatch(/## Phase 5 .*Commit/)` |
| 19 | `githubSource_phase6_containsCommitHeading` | `expect(githubContent).toMatch(/## Phase 6 .*Commit/)` |

---

### UT-7: Phase renumbering — old Phase 6 (Tech Lead) is now Phase 7

> **Category:** Unit Test — Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-4 through UT-6, UT-8)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 20 | `claudeSource_phase7_containsTechLeadHeading` | `expect(claudeContent).toMatch(/## Phase 7 .*Tech Lead/)` |
| 21 | `claudeSource_noOldPhase6TechLead` | `expect(claudeContent).not.toMatch(/## Phase 6 .*Tech Lead/)` |
| 22 | `githubSource_phase7_containsTechLeadHeading` | `expect(githubContent).toMatch(/## Phase 7 .*Tech Lead/)` |

---

### UT-8: Phase renumbering — old Phase 7 (Verification) is now Phase 8

> **Category:** Unit Test — Content Validation (Level 3: Simple Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-4 through UT-7)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 23 | `claudeSource_phase8_containsVerificationHeading` | `expect(claudeContent).toMatch(/## Phase 8 .*Verification/)` |
| 24 | `claudeSource_noOldPhase7Verification` | `expect(claudeContent).not.toMatch(/## Phase 7 .*Verification/)` |
| 25 | `claudeSource_phase8IsOnlyStoppingPoint` | `expect(claudeContent).toMatch(/Phase 8 is the ONLY legitimate stopping point/)` |
| 26 | `claudeSource_noOldPhase7StoppingPoint` | `expect(claudeContent).not.toContain("Phase 7 is the ONLY legitimate stopping point")` |
| 27 | `githubSource_phase8_containsVerificationHeading` | `expect(githubContent).toMatch(/## Phase 8 .*Verification/)` |

---

### UT-9: Documentation phase includes interface dispatch mechanism

> **Category:** Unit Test — Content Validation (Level 4: Complex Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-10)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 28 | `claudeSource_phase3_containsInterfacesFieldRead` | `expect(claudeContent).toMatch(/interfaces.*field|Read.*interfaces/i)` (in Phase 3 section) |
| 29 | `claudeSource_phase3_containsRestDispatch` | `expect(claudeContent).toContain("rest")` and appears within Phase 3 context alongside `OpenAPI` or `Swagger` |
| 30 | `claudeSource_phase3_containsGrpcDispatch` | `expect(claudeContent).toMatch(/grpc.*doc|gRPC.*generator/i)` within Phase 3 context |
| 31 | `claudeSource_phase3_containsCliDispatch` | `expect(claudeContent).toMatch(/cli.*doc|CLI.*generator/i)` within Phase 3 context |
| 32 | `claudeSource_phase3_containsEventDispatch` | `expect(claudeContent).toMatch(/websocket|event.*driven|event-consumer|event-producer/i)` within Phase 3 context |
| 33 | `claudeSource_phase3_containsNoInterfaceSkipLog` | `expect(claudeContent).toMatch(/[Nn]o documentable interfaces/)` |
| 34 | `githubSource_phase3_containsInterfaceDispatchMechanism` | `expect(githubContent).toMatch(/interfaces.*field|Read.*interfaces/i)` |
| 35 | `githubSource_phase3_containsNoInterfaceSkipLog` | `expect(githubContent).toMatch(/[Nn]o documentable interfaces/)` |

---

### UT-10: Documentation phase includes changelog generation

> **Category:** Unit Test — Content Validation (Level 4: Complex Condition)
> **Depends On:** UT-1
> **Parallel:** yes (with UT-9)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 36 | `claudeSource_phase3_containsChangelogGeneration` | `expect(claudeContent).toMatch(/changelog.*entry|CHANGELOG\.md/i)` within Phase 3 context |
| 37 | `claudeSource_phase3_changelogAlwaysGenerated` | `expect(claudeContent).toMatch(/ALWAYS.*regardless|always.*regardless/i)` or semantically equivalent assertion that changelog runs independent of interfaces |
| 38 | `claudeSource_phase3_changelogUsesConventionalCommits` | `expect(claudeContent).toMatch(/[Cc]onventional [Cc]ommits/)` within Phase 3 context |
| 39 | `claudeSource_phase3_changelogReadsGitLog` | `expect(claudeContent).toMatch(/git log|commits since/)` within Phase 3 context |
| 40 | `githubSource_phase3_containsChangelogGeneration` | `expect(githubContent).toMatch(/changelog.*entry|CHANGELOG\.md/i)` |
| 41 | `githubSource_phase3_changelogAlwaysGenerated` | `expect(githubContent).toMatch(/ALWAYS.*regardless|always.*regardless/i)` |

---

### UT-11: Complete Flow block lists all 9 phases correctly

> **Category:** Unit Test — Content Validation (Level 5: Iteration/collection)
> **Depends On:** UT-1, UT-4 through UT-8
> **Parallel:** no (depends on all renumbering tests)

| # | Test Name | Assertion |
|---|-----------|-----------|
| 42 | `claudeSource_completeFlow_containsPhase0Preparation` | `expect(claudeContent).toMatch(/Phase 0:.*Preparation/)` |
| 43 | `claudeSource_completeFlow_containsPhase1Planning` | `expect(claudeContent).toMatch(/Phase 1:.*Planning/)` |
| 44 | `claudeSource_completeFlow_containsPhase1BParallel` | `expect(claudeContent).toMatch(/Phase 1B-1E:.*Parallel/)` |
| 45 | `claudeSource_completeFlow_containsPhase2Implementation` | `expect(claudeContent).toMatch(/Phase 2:.*Implementation/)` |
| 46 | `claudeSource_completeFlow_containsPhase3Documentation` | `expect(claudeContent).toMatch(/Phase 3:.*Documentation/)` |
| 47 | `claudeSource_completeFlow_containsPhase4Review` | `expect(claudeContent).toMatch(/Phase 4:.*Review/)` |
| 48 | `claudeSource_completeFlow_containsPhase56FixesPR` | `expect(claudeContent).toMatch(/Phase 5-6:.*Fixes.*PR|Phase 5.*Fixes|Phase 6.*Commit/)` |
| 49 | `claudeSource_completeFlow_containsPhase7TechLead` | `expect(claudeContent).toMatch(/Phase 7:.*Tech Lead/)` |
| 50 | `claudeSource_completeFlow_containsPhase8Verification` | `expect(claudeContent).toMatch(/Phase 8:.*Verification/)` |
| 51 | `claudeSource_completeFlow_noOldPhase3Review` | `expect(claudeContent).not.toMatch(/Phase 3:.*Review/)` (within Complete Flow code block) |
| 52 | `githubSource_completeFlow_containsPhase3Documentation` | `expect(githubContent).toMatch(/Phase 3:.*Documentation/)` |
| 53 | `githubSource_completeFlow_containsPhase8Verification` | `expect(githubContent).toMatch(/Phase 8:.*Verification/)` |

---

### UT-12: Cross-references updated (Phase N/8 progress messages)

> **Category:** Unit Test — Content Validation (Level 5: Iteration)
> **Depends On:** UT-2, UT-3
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 54 | `claudeSource_progressMessage_phaseNof8` | `expect(claudeContent).toMatch(/Phase N\/8/)` |
| 55 | `claudeSource_progressMessage_noOldPhaseNof7` | `expect(claudeContent).not.toMatch(/Phase N\/7/)` |
| 56 | `githubSource_progressMessage_phase8of8Completed` | `expect(githubContent).toMatch(/Phase 8\/8 completed/)` |
| 57 | `githubSource_progressMessage_noOldPhase7of7` | `expect(githubContent).not.toMatch(/Phase 7\/7 completed/)` |
| 58 | `githubSource_afterEachPhases0to7` | `expect(githubContent).toMatch(/Phases 0.7:/)` (was 0-6) |

---

### UT-13: Roles table updated with renumbered phase references

> **Category:** Unit Test — Content Validation (Level 5: Iteration)
> **Depends On:** UT-4, UT-7
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 59 | `claudeSource_rolesTable_reviewPhase4` | `expect(claudeContent).toMatch(/Specialist Reviews.*Phase 4/i)` or `expect(claudeContent).toMatch(/Phase 4.*Adaptive/)` |
| 60 | `claudeSource_rolesTable_techLeadPhase7` | `expect(claudeContent).toMatch(/Tech Lead.*Phase 7/)` |
| 61 | `claudeSource_rolesTable_noOldPhase3Review` | `expect(claudeContent).not.toMatch(/Specialist Reviews.*Phase 3/)` |
| 62 | `claudeSource_rolesTable_noOldPhase6TechLead` | `expect(claudeContent).not.toMatch(/Tech Lead.*Phase 6\b/)` where `\b` avoids false positive with "Phase 6" in other contexts |

---

### UT-14: Phase 3 positioned between Phase 2 and Phase 4 in document order

> **Category:** Unit Test — Content Validation (Level 6: Edge Case — ordering)
> **Depends On:** UT-1, UT-4
> **Parallel:** no

| # | Test Name | Assertion |
|---|-----------|-----------|
| 63 | `claudeSource_phase3_afterPhase2` | Phase 3 heading index > Phase 2 heading index |
| 64 | `claudeSource_phase3_beforePhase4` | Phase 3 heading index < Phase 4 heading index |
| 65 | `githubSource_phase3_afterPhase2` | Phase 3 heading index > Phase 2 heading index |
| 66 | `githubSource_phase3_beforePhase4` | Phase 3 heading index < Phase 4 heading index |

---

### UT-15: Structural preservation — unchanged elements intact

> **Category:** Unit Test — Content Validation (Level 6: Edge Case — backward compatibility)
> **Depends On:** none
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 67 | `claudeSource_preservesPhase0Preparation` | `expect(claudeContent).toMatch(/## Phase 0 .*Preparation/)` |
| 68 | `claudeSource_preservesPhase1Architecture` | `expect(claudeContent).toMatch(/## Phase 1 .*Architecture/)` |
| 69 | `claudeSource_preservesPhase2TDDImplementation` | `expect(claudeContent).toMatch(/## Phase 2 .*TDD Implementation/)` |
| 70 | `claudeSource_preservesPhase1BTestPlanning` | `expect(claudeContent).toContain("1B: Test Planning")` |
| 71 | `claudeSource_preservesPhase1CTaskDecomposition` | `expect(claudeContent).toContain("1C: Task Decomposition")` |
| 72 | `claudeSource_preservesPhase1DEventSchema` | `expect(claudeContent).toContain("1D: Event Schema Design")` |
| 73 | `claudeSource_preservesPhase1ECompliance` | `expect(claudeContent).toContain("1E: Compliance Assessment")` |
| 74 | `claudeSource_preservesG1G7Fallback` | `expect(claudeContent).toContain("G1-G7 Fallback")` |
| 75 | `claudeSource_preservesAllPlaceholderTokens` | Each of `{{PROJECT_NAME}}`, `{{LANGUAGE}}`, `{{LANGUAGE_VERSION}}`, `{{COMPILE_COMMAND}}`, `{{TEST_COMMAND}}`, `{{COVERAGE_COMMAND}}` present |
| 76 | `claudeSource_preservesIntegrationNotes` | `expect(claudeContent).toContain("## Integration Notes")` |
| 77 | `claudeSource_preservesFrontmatter` | `expect(claudeContent).toContain("name: x-dev-lifecycle")` |

---

### UT-16: Dual copy consistency — Documentation phase present in both (RULE-001)

> **Category:** Unit Test — Dual Copy Consistency
> **Depends On:** UT-1
> **Parallel:** yes

| # | Test Name | Assertion |
|---|-----------|-----------|
| 78 | `dualCopy_bothContainPhase3DocumentationHeading` | Both contain `## Phase 3` with `Documentation` |
| 79 | `dualCopy_bothContain9Phases0to8` | Both contain `9 phases (0-8)` |
| 80 | `dualCopy_bothContainNeverStopBeforePhase8` | Both contain `NEVER stop before Phase 8` |
| 81 | `dualCopy_bothContainPhase8VerificationHeading` | Both contain `## Phase 8` with `Verification` |
| 82 | `dualCopy_bothContainPhase8OnlyStoppingPoint` | Both contain `Phase 8 is the ONLY legitimate stopping point` |
| 83 | `dualCopy_bothContainInterfaceDispatch` | Both contain `interfaces` dispatch mechanism |
| 84 | `dualCopy_bothContainChangelogGeneration` | Both contain `changelog` or `CHANGELOG.md` within Phase 3 |
| 85 | `dualCopy_bothContainNoInterfaceSkipLog` | Both contain `No documentable interfaces` skip log |
| 86 | `dualCopy_bothContainPhase3InCompleteFlow` | Both contain `Phase 3:.*Documentation` in Complete Flow block |
| 87 | `dualCopy_phaseCountIdentical` | Both declare same total phase count |

---

### IT-1: Golden file test — all 8 profiles generate lifecycle with new phase

> **Category:** Integration Test — Golden File Parity
> **Depends On:** UT-1 through UT-16 (all content tests must pass before golden files can be regenerated)
> **Parallel:** no (sequential per profile, as per existing `describe.sequential.each` pattern)

**Description:** The existing `tests/node/integration/byte-for-byte.test.ts` test runs the pipeline for all 8 profiles and verifies byte-for-byte parity with golden files. After updating the source templates and regenerating golden files, this test validates that the pipeline correctly distributes the new Documentation phase to all output directories.

**Profiles:**

| Profile | Golden File Paths (3 per profile) |
|---------|----------------------------------|
| go-gin | `.claude/skills/x-dev-lifecycle/SKILL.md`, `.agents/skills/x-dev-lifecycle/SKILL.md`, `.github/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | (same 3 paths) |
| java-spring | (same 3 paths) |
| kotlin-ktor | (same 3 paths) |
| python-click-cli | (same 3 paths) |
| python-fastapi | (same 3 paths) |
| rust-axum | (same 3 paths) |
| typescript-nestjs | (same 3 paths) |

**Total golden files affected:** 24 (8 profiles x 3 output directories)

**Test assertions per profile (existing):**
1. `pipelineSuccessForProfile_{profile}` -- pipeline runs without error
2. `pipelineMatchesGoldenFiles_{profile}` -- byte-for-byte match
3. `noMissingFiles_{profile}` -- no expected files absent
4. `noExtraFiles_{profile}` -- no unexpected files generated
5. `totalFilesGreaterThanZero_{profile}` -- at least one file generated

---

### IT-2: GitHub template also contains Documentation phase

> **Category:** Integration Test — Content Validation (GitHub-specific)
> **Depends On:** UT-1
> **Parallel:** yes (with IT-1)

**Description:** Validates that the GitHub Copilot template (`resources/github-skills-templates/dev/x-dev-lifecycle.md`) contains the same Documentation phase with GitHub-specific formatting differences.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 88 | `githubSource_containsPhase3Documentation` | `expect(githubContent).toMatch(/## Phase 3 .*Documentation/)` |
| 89 | `githubSource_phase8CompletionMessage` | `expect(githubContent).toMatch(/Phase 8\/8 completed.*[Ll]ifecycle complete/)` |
| 90 | `githubSource_afterPhasesRange_0to7` | `expect(githubContent).toMatch(/After each of Phases 0.7/)` (updated from 0-6) |
| 91 | `githubSource_afterPhase8_lifecycleComplete` | `expect(githubContent).toMatch(/After Phase 8:/)` (updated from "After Phase 7:") |
| 92 | `githubSource_detailedReferencesPreserved` | `expect(githubContent).toContain("## Detailed References")` |

---

## 3. Golden Files Requiring Update

### 3.1 Golden File Update Strategy

After editing both source templates, regenerate all 24 golden files:

```bash
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

## 4. Suggested Test Implementation Pattern

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

describe("x-dev-lifecycle Claude source — Phase 3 Documentation", () => {
  // UT-1: Phase header exists
  // UT-9: Interface dispatch
  // UT-10: Changelog generation
});

describe("x-dev-lifecycle Claude source — Phase count and stop rule", () => {
  // UT-2: 9 phases (0-8)
  // UT-3: NEVER stop before Phase 8
});

describe("x-dev-lifecycle Claude source — Phase renumbering", () => {
  // UT-4: Review -> Phase 4
  // UT-5: Fixes -> Phase 5
  // UT-6: Commit -> Phase 6
  // UT-7: Tech Lead -> Phase 7
  // UT-8: Verification -> Phase 8
});

describe("x-dev-lifecycle Claude source — Complete Flow block", () => {
  // UT-11: All 9 phases listed
  // UT-12: Progress messages
  // UT-13: Roles table
  // UT-14: Phase ordering
});

describe("x-dev-lifecycle Claude source — Structural preservation", () => {
  // UT-15: Unchanged elements intact
});

describe("x-dev-lifecycle GitHub source — Documentation phase", () => {
  // IT-2: GitHub-specific validations
});

describe("x-dev-lifecycle dual copy consistency (RULE-001)", () => {
  // UT-16: Both copies consistent
});
```

---

## 5. TDD Execution Order

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-dev-lifecycle-doc-phase.test.ts`) with all ~92 assertions | RED (source files not yet modified) |
| 2 | Edit Claude source template — insert Phase 3, renumber Phases 3-7 to 4-8, update header/footer | Partial GREEN (Claude tests pass, GitHub tests RED) |
| 3 | Edit GitHub source template — same changes adapted for GitHub format | GREEN (all content + consistency tests pass) |
| 4 | Update deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) to match Claude source | N/A (deployed copy updated) |
| 5 | Regenerate 24 golden files (script from Section 3.1) | N/A (golden files updated) |
| 6 | Run byte-for-byte integration tests | GREEN (golden file parity confirmed) |
| 7 | Run full test suite (`npx vitest run`) | GREEN (all existing + new tests pass) |

---

## 6. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation — Claude source | ~50 | 0 |
| Content validation — GitHub source | ~25 | 0 |
| Dual copy consistency (RULE-001) | 10 | 0 |
| Cross-reference / progress messages | 5 | 0 |
| Roles table renumbering | 4 | 0 |
| Phase ordering (edge case) | 4 | 0 |
| Structural preservation | 11 | 0 |
| Golden file integration (IT-1) | 0 | 40 (8 profiles x 5 assertions) |
| **Total** | **~92** | **~40** |

---

## 7. Backward Compatibility Verification (RULE-003)

These tests ensure no existing functionality is removed:

| Verification | Test(s) |
|--------------|---------|
| Phase 0-2 structure preserved | UT-15 (#67-#69) |
| Phase 1B-1E parallel planning preserved | UT-15 (#70-#73) |
| G1-G7 fallback path preserved | UT-15 (#74) |
| Placeholder tokens intact | UT-15 (#75) |
| Integration Notes section present | UT-15 (#76) |
| Frontmatter structure preserved | UT-15 (#77) |

---

## 8. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after source edit | Mechanical copy script eliminates drift; byte-for-byte tests catch any mismatch |
| Content tests too brittle | Use `toContain()` for substrings and `toMatch()` for regex; test semantic presence, not exact formatting |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (#78-#87) verify both copies have equivalent Documentation phase content |
| Renumbering introduces stale references | Negative assertions (e.g., `not.toMatch(/## Phase 3 .*Review/)`) catch leftover old phase numbers |
| Phase ordering wrong | UT-14 tests document-order of Phase 3 relative to Phase 2 and Phase 4 via index comparison |
| Deployed copy diverges from source | Verification checklist: deployed copy must match Claude source exactly |

---

## 9. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-lifecycle-doc-phase.test.ts` -- all ~92 new content tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` -- full suite passes (1,384+ existing tests + ~92 new tests)
- [ ] Coverage remains >= 95% line, >= 90% branch (no TypeScript code changes, so coverage unaffected)
- [ ] No compiler/linter warnings introduced
- [ ] Deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) matches Claude source template exactly
