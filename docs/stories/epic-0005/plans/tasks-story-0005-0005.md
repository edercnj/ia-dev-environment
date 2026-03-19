# Task Breakdown -- story-0005-0005: Orchestrator Core Loop + Sequential Dispatcher

**Story:** `story-0005-0005.md`
**Architecture Plan:** `architecture-story-0005-0005.md`
**Implementation Plan:** `plan-story-0005-0005.md`
**Test Plan:** `tests-story-0005-0005.md`

---

## Summary

23 tasks organized into 7 groups (A through G) following TDD Red-Green-Refactor cycles.
This is a **template-only** change -- no new TypeScript source files. All tasks modify SKILL.md templates,
content tests, or golden files.

**Legend:**
- **RED** = Write failing tests only (no template changes)
- **GREEN** = Write minimum template content to make tests pass
- **REFACTOR** = Improve content/tests without changing behavior; regenerate golden files
- **SETUP** = Infrastructure/scaffolding (no tests needed)

---

## Group A: Test Infrastructure -- Split Existing Placeholder Test

### TASK-1: RED -- Split `skillMd_phases1to3_arePlaceholders` into Phase 1 and Phase 2+3 assertions
- **Type:** RED
- **Test:** UT-2 (`skillMd_phases2And3_remainPlaceholders`)
- **Depends On:** none
- **Parallel:** no
- **Description:** Refactor the existing test `skillMd_phases1to3_arePlaceholders` in `x-dev-epic-implement-content.test.ts`. Split it into two tests:
  1. `skillMd_phases2And3_remainPlaceholders` -- asserts Phase 2 and Phase 3 still match `/placeholder|story-0005|TODO|implemented in|extended by/i` (this test passes immediately since Phase 2 and 3 are unchanged).
  2. Remove the Phase 1 placeholder assertion from the existing test (Phase 1 will no longer be a placeholder after implementation).
  The test file now no longer asserts that Phase 1 is a placeholder, preparing for Phase 1 content addition.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Original `skillMd_phases1to3_arePlaceholders` test is replaced by `skillMd_phases2And3_remainPlaceholders`
  - Phase 2 and Phase 3 placeholder assertions pass
  - Phase 1 is no longer asserted as a placeholder
  - All existing tests still pass (no regression)

### TASK-2: RED -- Add acceptance test: Phase 1 is not a placeholder
- **Type:** RED
- **Test:** AT-1 (`skillMd_phase1_isNotPlaceholder_containsSubstantiveContent`)
- **Depends On:** TASK-1
- **Parallel:** no
- **Description:** Add a new test `skillMd_phase1_isNotPlaceholder_containsSubstantiveContent` that:
  - Extracts Phase 1 content (between "Phase 1" and "Phase 2" headings)
  - Asserts it does NOT match `/^\s*>\s*\*?\*?Placeholder/im` (not just a placeholder blockquote)
  - Asserts it contains at least 50 lines of content (not a stub)
  This test will FAIL because Phase 1 is currently a 4-line placeholder.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and fails (RED state)
  - Test correctly extracts Phase 1 content between headings
  - The "at least 50 lines" assertion fails on current placeholder

---

## Group B: Level 1+2 Unit Tests -- Foundation Keywords (RED)

### TASK-3: RED -- Phase 1 heading exists
- **Type:** RED
- **Test:** UT-1 (`skillMd_phase1_headingExists`)
- **Depends On:** TASK-2
- **Parallel:** no
- **Description:** Add test `skillMd_phase1_headingExists` that asserts `content.indexOf("Phase 1") > -1`. This test passes immediately (the heading already exists in the template). It serves as the foundation assertion for all subsequent Phase 1 content tests.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and passes (GREEN immediately -- heading already present)

### TASK-4: RED -- Phase 1 mentions checkpoint integration
- **Type:** RED
- **Test:** UT-3 (`skillMd_phase1_containsCheckpointIntegration`)
- **Depends On:** TASK-3
- **Parallel:** no
- **Description:** Add test `skillMd_phase1_containsCheckpointIntegration` that:
  - Extracts Phase 1 content (between "Phase 1" and "Phase 2" headings)
  - Asserts it contains `createCheckpoint` AND `updateStoryStatus`
  Test will FAIL because Phase 1 placeholder does not contain these keywords.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and fails (RED state)

### TASK-5: RED -- Phase 1 mentions map parser integration
- **Type:** RED
- **Test:** UT-4 (`skillMd_phase1_containsMapParserIntegration`)
- **Depends On:** TASK-3
- **Parallel:** yes (with TASK-4)
- **Description:** Add test `skillMd_phase1_containsMapParserIntegration` that:
  - Extracts Phase 1 content
  - Asserts it contains `parseImplementationMap` AND `getExecutableStories`
  Test will FAIL.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and fails (RED state)

### TASK-6: RED -- Phase 1 mentions subagent dispatch
- **Type:** RED
- **Test:** UT-5 (`skillMd_phase1_containsSubagentDispatch`)
- **Depends On:** TASK-3
- **Parallel:** yes (with TASK-4, TASK-5)
- **Description:** Add test `skillMd_phase1_containsSubagentDispatch` that:
  - Extracts Phase 1 content
  - Asserts it contains `Agent` AND `SubagentResult` AND `x-dev-lifecycle`
  Test will FAIL.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and fails (RED state)

---

## Group C: First GREEN -- Write Phase 1 Core Content in SKILL.md

### TASK-7: GREEN -- Write Phase 1 core content in Claude SKILL.md template
- **Type:** GREEN
- **Test:** AT-1, UT-3, UT-4, UT-5 (make them pass)
- **Depends On:** TASK-4, TASK-5, TASK-6
- **Parallel:** no
- **Description:** Replace the Phase 1 placeholder (lines 105-109) in `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` with the core loop content. Write sections 1.1 through 1.4 as specified in the implementation plan Section 14:
  - **1.1 Initialize Execution State** -- instructions for `parseImplementationMap()` and `createCheckpoint()`
  - **1.2 Branch Management** -- `git checkout` instructions for creating/resuming `feat/epic-{epicId}-full-implementation` branch
  - **1.3 Core Loop Algorithm** -- phase-by-phase iteration using `getExecutableStories()`, `IN_PROGRESS` status marking via `updateStoryStatus()`
  - **1.4 Subagent Dispatch (Sequential Mode)** -- Agent tool dispatch with prompt template containing storyId, storyPath, branchName, currentPhase, skipReview, epicId; subagent executes `x-dev-lifecycle` logic with clean context; returns `SubagentResult`
  This is the minimum content to make AT-1, UT-3, UT-4, and UT-5 pass.
- **Files:**
  - `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- **Acceptance:**
  - AT-1 passes (Phase 1 is no longer a placeholder, has >= 50 lines)
  - UT-3 passes (`createCheckpoint`, `updateStoryStatus` present)
  - UT-4 passes (`parseImplementationMap`, `getExecutableStories` present)
  - UT-5 passes (`Agent`, `SubagentResult`, `x-dev-lifecycle` present)
  - All existing tests still pass (no regression)

---

## Group D: Level 3+4 Unit Tests -- Collection and Composite Keywords (RED then GREEN)

### TASK-8: RED -- Phase 1 mentions result validation fields (RULE-008)
- **Type:** RED
- **Test:** UT-6 (`skillMd_phase1_containsResultValidation`)
- **Depends On:** TASK-7
- **Parallel:** no
- **Description:** Add test `skillMd_phase1_containsResultValidation` that:
  - Extracts Phase 1 content
  - Asserts it contains ALL of: `status`, `findingsCount`, `summary`, `commitSha`
  - Asserts it matches `/RULE-008|result contract|Result Validation/i`
  Test may pass if TASK-7 already included these terms, or may fail if content is incomplete.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists
  - If it fails, it is in RED state waiting for TASK-11

### TASK-9: RED -- Phase 1 mentions branch management
- **Type:** RED
- **Test:** UT-7 (`skillMd_phase1_containsBranchManagement`)
- **Depends On:** TASK-7
- **Parallel:** yes (with TASK-8)
- **Description:** Add test `skillMd_phase1_containsBranchManagement` that:
  - Extracts Phase 1 content
  - Asserts it contains `feat/epic-` AND matches `/git checkout|branch/i`
  Test may pass if TASK-7 already included branch content.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists
  - If it fails, it is in RED state waiting for TASK-11

### TASK-10: RED -- Phase 1 mentions critical path priority (RULE-007)
- **Type:** RED
- **Test:** UT-8 (`skillMd_phase1_containsCriticalPathPriority`)
- **Depends On:** TASK-7
- **Parallel:** yes (with TASK-8, TASK-9)
- **Description:** Add test `skillMd_phase1_containsCriticalPathPriority` that:
  - Extracts Phase 1 content
  - Asserts it matches `/critical.?path/i`
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists
  - If it fails, it is in RED state waiting for TASK-11

### TASK-11: GREEN -- Expand Phase 1 with result validation, branch detail, and critical path
- **Type:** GREEN
- **Test:** UT-6, UT-7, UT-8 (make them pass)
- **Depends On:** TASK-8, TASK-9, TASK-10
- **Parallel:** no
- **Description:** Expand Phase 1 in `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` with sections 1.5 and 1.6 as specified in the implementation plan:
  - **1.5 Result Validation (RULE-008)** -- check `status` field (SUCCESS | FAILED | PARTIAL), `findingsCount` (number), `summary` (string), `commitSha` (required if SUCCESS); on invalid result mark FAILED with summary "Invalid subagent result: missing {field} field"
  - **1.6 Checkpoint Update (RULE-002)** -- `updateStoryStatus()` call with status, commitSha, findingsCount, summary; increment `storiesCompleted` metric
  Also ensure section 1.2 (Branch Management) explicitly mentions `feat/epic-` and `git checkout`, and section 1.3 mentions `critical path` priority in story ordering.
- **Files:**
  - `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- **Acceptance:**
  - UT-6 passes (result validation fields and RULE-008 reference)
  - UT-7 passes (branch management keywords)
  - UT-8 passes (critical path mention)
  - All previous tests still pass

---

## Group E: Level 4+5 -- Context Isolation, Extension Points, Status Values, Dual-Copy (RED then GREEN)

### TASK-12: RED -- Phase 1 mentions context isolation (RULE-001)
- **Type:** RED
- **Test:** UT-9 (`skillMd_phase1_containsContextIsolation`)
- **Depends On:** TASK-11
- **Parallel:** no
- **Description:** Add test `skillMd_phase1_containsContextIsolation` that:
  - Extracts Phase 1 content
  - Asserts it matches `/RULE-001|context isolation|clean context/i`
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists
  - If it fails, it is in RED state waiting for TASK-16

### TASK-13: RED -- Phase 1 contains extension point placeholders
- **Type:** RED
- **Test:** UT-10 (`skillMd_phase1_containsExtensionPlaceholders`)
- **Depends On:** TASK-11
- **Parallel:** yes (with TASK-12)
- **Description:** Add test `skillMd_phase1_containsExtensionPlaceholders` that:
  - Extracts Phase 1 content
  - Asserts it contains at least 3 of: `story-0005-0006`, `story-0005-0007`, `story-0005-0008`, `story-0005-0010`, `story-0005-0011`, `story-0005-0013`
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists
  - If it fails, it is in RED state waiting for TASK-16

### TASK-14: RED -- Phase 1 mentions status values
- **Type:** RED
- **Test:** UT-11 (`skillMd_phase1_containsStatusValues`)
- **Depends On:** TASK-11
- **Parallel:** yes (with TASK-12, TASK-13)
- **Description:** Add test `skillMd_phase1_containsStatusValues` that:
  - Extracts Phase 1 content
  - Asserts it contains ALL of: `IN_PROGRESS`, `SUCCESS`, `FAILED`
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists
  - If it fails, it is in RED state waiting for TASK-16

### TASK-15: RED -- Phase 1 does not contain source code imports (RULE-001 compliance)
- **Type:** RED
- **Test:** UT-13 (`skillMd_phase1_doesNotContainSourceImports`)
- **Depends On:** TASK-11
- **Parallel:** yes (with TASK-12, TASK-13, TASK-14)
- **Description:** Add test `skillMd_phase1_doesNotContainSourceImports` that:
  - Extracts Phase 1 content
  - Asserts it does NOT match `/^import .* from/m`
  - Asserts it does NOT match `/require\(/`
  This test should pass immediately since SKILL.md is Markdown, not TypeScript.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and passes (GREEN immediately)

### TASK-16: GREEN -- Finalize Phase 1 with extension points, status values, context isolation
- **Type:** GREEN
- **Test:** UT-9, UT-10, UT-11 (make them pass)
- **Depends On:** TASK-12, TASK-13, TASK-14, TASK-15
- **Parallel:** no
- **Description:** Add section 1.7 (Extension Points) to Phase 1 in `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`:
  - `[Placeholder: integrity gate -- story-0005-0006]`
  - `[Placeholder: retry + block propagation -- story-0005-0007]`
  - `[Placeholder: resume from checkpoint -- story-0005-0008]`
  - `[Placeholder: partial execution filter -- story-0005-0009]`
  - `[Placeholder: parallel worktrees -- story-0005-0010]`
  - `[Placeholder: consolidation + verification -- story-0005-0011]`
  - `[Placeholder: progress reporting -- story-0005-0013]`
  Also ensure:
  - Section 1.4 explicitly mentions `clean context` or `RULE-001` for context isolation
  - Sections 1.3 and 1.5 explicitly use `IN_PROGRESS`, `SUCCESS`, `FAILED` status values
- **Files:**
  - `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- **Acceptance:**
  - UT-9 passes (context isolation)
  - UT-10 passes (extension placeholders)
  - UT-11 passes (status values)
  - UT-13 still passes (no source imports)
  - All previous tests still pass

### TASK-17: RED -- Phase 1 section ordering is logical
- **Type:** RED
- **Test:** UT-14 (`skillMd_phase1_subsectionsInLogicalOrder`)
- **Depends On:** TASK-16
- **Parallel:** no
- **Description:** Add test `skillMd_phase1_subsectionsInLogicalOrder` that:
  - Extracts Phase 1 content
  - Measures positions of "Initialize", "Branch", "Core Loop", "Dispatch", "Validation"
  - Asserts "Initialize" appears before "Core Loop"
  - Asserts "Core Loop" appears before "Dispatch" or "Dispatch" is within "Core Loop"
  This test should pass immediately if the content was structured correctly in TASK-7/TASK-11/TASK-16.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and passes (verifies section ordering)

### TASK-18: RED -- Acceptance test: Phase 1 contains all required subsections
- **Type:** RED
- **Test:** AT-2 (`skillMd_phase1_containsAllRequiredSubsections`)
- **Depends On:** TASK-16
- **Parallel:** yes (with TASK-17)
- **Description:** Add test `skillMd_phase1_containsAllRequiredSubsections` that:
  - Extracts Phase 1 content
  - Asserts it contains ALL of: `Initialize`, `Branch`, `Core Loop`, `Dispatch`, `Validation`, `Checkpoint`, `Extension`
  This test should pass since all subsections were added in TASK-7, TASK-11, and TASK-16.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Test exists and passes (all 7 subsections present)

---

## Group F: Dual-Copy Consistency -- GitHub Mirror

### TASK-19: RED -- Add new critical terms to dual-copy consistency test
- **Type:** RED
- **Test:** UT-12 (`bothContainTerm_%s_dualCopyConsistency` -- extended CRITICAL_TERMS)
- **Depends On:** TASK-18
- **Parallel:** no
- **Description:** Extend the `CRITICAL_TERMS` array in the dual-copy consistency `describe` block to include new Phase 1 terms:
  - `getExecutableStories`
  - `SubagentResult`
  - `IN_PROGRESS`
  - `createCheckpoint`
  These terms exist in the Claude SKILL.md (added in TASK-7/TASK-11/TASK-16) but NOT yet in the GitHub mirror. Tests for new terms will FAIL.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts`
- **Acceptance:**
  - Extended CRITICAL_TERMS array includes 4 new terms (total: 14 terms)
  - New term tests fail for the GitHub mirror (RED state)
  - Existing term tests still pass

### TASK-20: GREEN -- Update GitHub mirror with abbreviated Phase 1 content
- **Type:** GREEN
- **Test:** UT-12, AT-3 (dual-copy consistency passes)
- **Depends On:** TASK-19
- **Parallel:** no
- **Description:** Replace the Phase 1 placeholder (line 60-61) in `resources/github-skills-templates/dev/x-dev-epic-implement.md` with an abbreviated version of the Phase 1 content. The abbreviated version must:
  - Be 30-50 lines (consistent with the abbreviated style of existing Phase 0)
  - Contain all critical terms: `getExecutableStories`, `SubagentResult`, `IN_PROGRESS`, `createCheckpoint`, `parseImplementationMap`, `updateStoryStatus`, `feat/epic-`, `git checkout`, `RULE-008`, `x-dev-lifecycle`, `critical path`, `RULE-001`, `SUCCESS`, `FAILED`
  - Reference extension point stories (at least `story-0005-0006`, `story-0005-0007`, `story-0005-0010`)
  - NOT be a copy-paste of the full Claude SKILL.md content
- **Files:**
  - `resources/github-skills-templates/dev/x-dev-epic-implement.md`
- **Acceptance:**
  - All dual-copy consistency tests pass (both old and new critical terms)
  - GitHub mirror Phase 1 is between 30-50 lines
  - AT-3 effectively passes (dual-copy terms verified)

---

## Group G: Refactoring and Golden Files

### TASK-21: REFACTOR -- Review Phase 1 content for clarity and compliance
- **Type:** REFACTOR
- **Test:** All existing tests must remain green
- **Depends On:** TASK-20
- **Parallel:** no
- **Description:** Review the Phase 1 content in both templates for:
  - Line count within 300-line NFR target for Phase 1 section
  - Consistent placeholder naming: `[Placeholder: {name} -- story-XXXX-YYYY]`
  - No accidental `{{PLACEHOLDER}}` runtime tokens introduced
  - No function/method exceeds documentation conventions
  - Section headings use consistent numbering (1.1, 1.2, ..., 1.7)
  - Remove any duplication between sections
  - Verify all Gherkin acceptance criteria from the story are addressed by the template content
- **Files:**
  - `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
  - `resources/github-skills-templates/dev/x-dev-epic-implement.md`
- **Acceptance:**
  - Phase 1 section is < 300 lines
  - All placeholder names follow the convention
  - No `{{PLACEHOLDER}}` tokens accidentally added
  - All existing tests still pass

### TASK-22: REFACTOR -- Regenerate golden files for all 8 profiles
- **Type:** REFACTOR
- **Test:** AT-4 (byte-for-byte golden file tests)
- **Depends On:** TASK-21
- **Parallel:** no
- **Description:** Regenerate golden files for all 8 profiles:
  1. Run `npm run generate` (or equivalent pipeline) for each profile: go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs
  2. Copy generated `.claude/skills/x-dev-epic-implement/SKILL.md` into each profile's golden directory under `tests/golden/{profile}/`
  3. Copy generated `.github/skills/x-dev-epic-implement/SKILL.md` (or equivalent GitHub path) into each profile's golden directory
  4. Run `npx vitest tests/node/content/byte-for-byte.test.ts` to verify all golden files match
- **Files:**
  - `tests/golden/go-gin/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/java-quarkus/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/java-spring/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/kotlin-ktor/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/python-click-cli/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/python-fastapi/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/rust-axum/.claude/skills/x-dev-epic-implement/SKILL.md`
  - `tests/golden/typescript-nestjs/.claude/skills/x-dev-epic-implement/SKILL.md`
  - (Plus corresponding `.github/` paths for all 8 profiles)
- **Acceptance:**
  - `npx vitest tests/node/content/byte-for-byte.test.ts` passes for all 8 profiles
  - All 16 golden files (8 profiles x 2 paths) are updated

### TASK-23: REFACTOR -- Final verification and cleanup
- **Type:** REFACTOR
- **Test:** Full test suite
- **Depends On:** TASK-22
- **Parallel:** no
- **Description:** Run the complete verification suite:
  1. `npx vitest tests/node/content/x-dev-epic-implement-content.test.ts` -- all content tests pass
  2. `npx vitest tests/node/content/byte-for-byte.test.ts` -- all golden file tests pass
  3. `npx tsc --noEmit` -- zero compilation errors (no TS changes, but verify no regression)
  4. `npx vitest run --coverage` -- verify overall coverage remains >= 95% line, >= 90% branch
  Clean up any test description inconsistencies or redundant assertions discovered during verification.
- **Files:**
  - `tests/node/content/x-dev-epic-implement-content.test.ts` (cleanup only)
- **Acceptance:**
  - All content tests pass
  - All golden file tests pass
  - TypeScript compilation clean
  - Coverage thresholds met
  - No compiler/linter warnings

---

## Dependency Graph

```
TASK-1 → TASK-2 → TASK-3 → TASK-4 ─┐
                      │    TASK-5 ─┤
                      │    TASK-6 ─┤
                      │            ↓
                      │         TASK-7 → TASK-8  ─┐
                      │                  TASK-9  ─┤
                      │                  TASK-10 ─┤
                      │                           ↓
                      │                        TASK-11 → TASK-12 ─┐
                      │                                  TASK-13 ─┤
                      │                                  TASK-14 ─┤
                      │                                  TASK-15 ─┤
                      │                                           ↓
                      │                                        TASK-16 → TASK-17 ─┐
                      │                                                  TASK-18 ─┤
                      │                                                           ↓
                      │                                                        TASK-19 → TASK-20
                      │                                                                    ↓
                      │                                                                 TASK-21 → TASK-22 → TASK-23
```

The chain is predominantly sequential, with small parallel groups within each RED batch (TASK-4/5/6, TASK-8/9/10, TASK-12/13/14/15, TASK-17/18).

---

## TDD Execution Order (Mapped to Test Plan)

| Step | Task(s) | Type | Test Plan Reference |
|------|---------|------|-------------------|
| 1 | TASK-1 | RED | UT-2 (split placeholder test) |
| 2 | TASK-2 | RED | AT-1 (Phase 1 not placeholder) |
| 3 | TASK-3 | RED | UT-1 (heading exists) |
| 4 | TASK-4, TASK-5, TASK-6 | RED | UT-3, UT-4, UT-5 (keywords) |
| 5 | TASK-7 | GREEN | AT-1 + UT-3/4/5 pass |
| 6 | TASK-8, TASK-9, TASK-10 | RED | UT-6, UT-7, UT-8 (collection keywords) |
| 7 | TASK-11 | GREEN | UT-6/7/8 pass |
| 8 | TASK-12, TASK-13, TASK-14, TASK-15 | RED | UT-9, UT-10, UT-11, UT-13 (composite) |
| 9 | TASK-16 | GREEN | UT-9/10/11 pass |
| 10 | TASK-17, TASK-18 | RED | UT-14, AT-2 (structural) |
| 11 | TASK-19 | RED | UT-12 (dual-copy new terms) |
| 12 | TASK-20 | GREEN | UT-12 + AT-3 pass |
| 13 | TASK-21 | REFACTOR | Content quality |
| 14 | TASK-22 | REFACTOR | AT-4 (golden files) |
| 15 | TASK-23 | REFACTOR | Full suite verification |

---

## Traceability Matrix

| Gherkin Scenario | Task(s) |
|-----------------|---------|
| Single story execution | TASK-6, TASK-7 (subagent dispatch) |
| Sequential two-phase execution with dependency | TASK-5, TASK-7 (getExecutableStories, phase loop) |
| Critical path prioritization | TASK-10, TASK-11 (critical path keywords) |
| Valid SubagentResult validation | TASK-8, TASK-11 (result fields, RULE-008) |
| Invalid SubagentResult -> FAILED | TASK-8, TASK-11, TASK-14, TASK-16 (validation + FAILED status) |
| Branch creation | TASK-9, TASK-11 (branch management) |
| Checkpoint after each story (RULE-002) | TASK-4, TASK-7 (checkpoint integration) |
| Context isolation (RULE-001) | TASK-12, TASK-15, TASK-16 (RULE-001, no imports) |
| BLOCKED story not dispatched | TASK-5, TASK-7 (getExecutableStories filters BLOCKED) |

---

## Estimated Effort

| Group | Tasks | Description | Estimated Lines (template) | Estimated Lines (test) |
|-------|-------|-------------|--------------------------|----------------------|
| A: Test Infrastructure | 1-2 | Split placeholder test, add AT-1 | 0 | ~25 |
| B: Foundation Keywords RED | 3-6 | UT-1, UT-3, UT-4, UT-5 | 0 | ~30 |
| C: First GREEN | 7 | Phase 1 sections 1.1-1.4 | ~80 | 0 |
| D: Collection Keywords | 8-11 | UT-6/7/8 + sections 1.5-1.6 | ~40 | ~20 |
| E: Composite + Structural | 12-18 | UT-9/10/11/13/14, AT-2 + section 1.7 | ~30 | ~40 |
| F: Dual-Copy | 19-20 | Extend CRITICAL_TERMS + GitHub mirror | ~40 | ~5 |
| G: Refactor + Golden | 21-23 | Review, regenerate, verify | ~0 | ~0 |
| **Total** | **23** | | **~190** | **~120** |
