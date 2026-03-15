# Task Breakdown -- story-0003-0014: x-dev-lifecycle TDD Restructure

## Overview

This story modifies 3 Markdown skill templates and regenerates 16 golden files. No TypeScript source code changes. All changes are additive or restructuring of existing Markdown content.

**Total files: 3 source templates + 16 golden files = 19 files.**

---

## G1: Source Template (Claude Code) -- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

### TASK-G1-01: Update Phase 0 -- Add test plan existence check

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Phase 0 -- Preparation
**Change type:** Additive (insert between existing steps 2 and 3)

Add new step 3 (renumber subsequent steps):
- Check if test plan exists at `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
- If absent: note that Phase 1B test planning is mandatory for TDD mode
- If present (from a prior `/x-test-plan` run): note test plan is available

**Impact:** Low. One step insertion, renumber steps 3-5 to 4-6.

---

### TASK-G1-02: Update Phase 1B -- Promote to MANDATORY DRIVER

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Phases 1B-1E, subsection 1B: Test Planning
**Change type:** Significant text addition

Changes:
1. Add annotation: **"MANDATORY DRIVER for Phase 2"** -- test plan output is the implementation roadmap
2. Add gate: if Phase 1B fails or produces no output, Phase 2 MUST use fallback mode (G1-G7)
3. Add note that `x-test-plan` (updated via story-0003-0007) now produces Double-Loop TDD format with TPP ordering, AT-N/UT-N/IT-N scenario IDs, and dependency/parallelism markers
4. Add note that the test plan output includes: acceptance tests (outer loop), unit tests in TPP order (inner loop), integration tests, dependency markers, and `Parallel: yes/no` per scenario

---

### TASK-G1-03: Update Phase 1C -- Derive tasks from test plan

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Phases 1B-1E, subsection 1C: Task Decomposition
**Change type:** Text addition

Changes:
1. Add note that `x-lib-task-decomposer` (updated via story-0003-0008) now auto-detects decomposition mode:
   - If test plan with TPP markers exists -> test-driven decomposition (RED/GREEN/REFACTOR tasks)
   - If no test plan -> falls back to G1-G7 layer-based decomposition
2. Reference that the task output now includes TDD task structure: `Test Scenario`, `TPP Level`, `RED`, `GREEN`, `REFACTOR`, `Parallel`, `Depends On`
3. Clarify that the task decomposer consumes Phase 1B output directly -- mode detection is internal to the decomposer

---

### TASK-G1-04: Restructure Phase 2 -- TDD Implementation (Primary Mode)

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Phase 2 -- Group-Based Implementation
**Change type:** Major restructure (largest single change)

Changes:
1. Rename heading from "Group-Based Implementation" to "TDD Implementation (with G1-G7 Fallback)"
2. Restructure subagent prompt Step 1 (Read context) to add:
   - Read test plan `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` -- MANDATORY
   - Read `x-dev-implement` skill reference for TDD workflow details
   - Keep all existing reads (implementation plan, task breakdown, coding standards, layer templates, architecture)
3. Replace Step 2 "Implement groups G1-G7" with TDD Loop:
   - Write acceptance test(s) (AT-N from test plan) -> must be RED
   - For each unit test scenario (UT-N) in TPP order:
     a. RED: Write failing unit test
     b. GREEN: Implement minimum production code (respecting layer order)
     c. REFACTOR: Improve design (no new behavior)
     d. Compile check: `{{COMPILE_COMMAND}}`
     e. Atomic commit (test + implementation)
   - After all UT-N for an AT-N complete -> verify AT-N turns GREEN
   - Final validation: `{{TEST_COMMAND}}` + `{{COVERAGE_COMMAND}}`
4. Update Step 3 (Commit) to reference TDD atomic commit format from `x-git-push` (story-0003-0013)

---

### TASK-G1-05: Add Phase 2 -- Parallelism section

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Phase 2 (new subsection within Phase 2)
**Change type:** Additive (new content block)

Add a **Parallelism in Phase 2** section:
- Independent test scenarios (no shared state/data dependencies) CAN run in parallel subagents
- Decision uses `Parallel: yes/no` markers from the test plan and task breakdown
- Subagents working on independent layers MUST be launched in a SINGLE message (RULE-009)
- Example: UT for outbound adapter can run in parallel with UT for inbound DTO validation if they share no state
- Dependent scenarios (marked `Depends On: TASK-N`) run sequentially

---

### TASK-G1-06: Add Phase 2 -- G1-G7 Fallback path (RULE-003)

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Phase 2 (new subsection after TDD primary mode)
**Change type:** Additive (preserves backward compatibility)

Add a **Fallback Mode (No Test Plan)** subsection:
- If Phase 1B failed or no test plan produced, Phase 2 uses legacy G1-G7 implementation
- Emit warning: "No TDD test plan available. Using G1-G7 group-based implementation."
- Preserve the exact current G1-G7 implementation instructions as the fallback content
- Log reminder: "Consider running /x-test-plan for future implementations"

---

### TASK-G1-07: Update Phases 3 and 6 -- Backward-compatible TDD references

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Sections:** Phase 3 -- Parallel Review, Phase 6 -- Tech Lead Review
**Change type:** Text annotation (low impact)

Phase 3 changes:
- Add note: if `x-review` includes TDD checklist (story-0003-0015), it validates TDD compliance
- Add backward-compatible guard: "If x-review does not include TDD checklist items, the review still proceeds with existing criteria"

Phase 6 changes:
- Add note: if `x-review-pr` includes TDD criteria (story-0003-0016), it validates TDD compliance in 40-point checklist
- Add backward-compatible guard: "If x-review-pr does not include TDD criteria, the review still proceeds with existing checklist"

---

### TASK-G1-08: Update Phases 4-5 -- TDD discipline for fixes and PR

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Sections:** Phase 4 -- Fixes + Feedback, Phase 5 -- Commit & PR
**Change type:** Additive text (low impact)

Phase 4 changes:
- Add: "For each fix, follow TDD discipline: write/update the test FIRST, then apply the fix"
- Add: use atomic TDD commits for fixes (same commit format as Phase 2)

Phase 5 changes:
- Add to PR body: "TDD Compliance" section mentioning number of TDD cycles, test-first pattern, TPP progression in commit history

---

### TASK-G1-09: Update Phase 7 -- Add TDD DoD items

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Phase 7 -- Final Verification + Cleanup
**Change type:** Additive checklist items (low impact)

Add TDD-specific DoD items after existing checks:
- [ ] Commits show test-first pattern (test precedes or accompanies implementation in git log)
- [ ] Acceptance tests exist and pass (AT-N GREEN)
- [ ] Tests follow TPP ordering (simple to complex)
- [ ] No test-after commits (all tests written before or with implementation)

These are additive items, not replacing existing checks.

---

### TASK-G1-10: Update Integration Notes

**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Section:** Integration Notes
**Change type:** Additive text (low impact)

Changes:
- Add `x-dev-implement` to invoked skills list
- Clarify: `x-lib-group-verifier` is used in fallback mode only
- Add note: TDD commit format follows `x-git-push` conventions (story-0003-0013)

---

## G2: GitHub Template -- `resources/github-skills-templates/dev/x-dev-lifecycle.md`

### TASK-G2-01: Mirror all G1 changes with GitHub path adjustments

**File:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`
**Change type:** Mirror of all TASK-G1-01 through TASK-G1-10

Apply the same logical changes from G1 with these platform-specific differences:

| Aspect | Claude Code (G1) | GitHub Copilot (G2) |
|--------|-------------------|---------------------|
| Frontmatter | YAML with `allowed-tools`, `argument-hint` | YAML with `name`, `description` only |
| Global Output Policy | Present | Absent |
| KP references | `skills/X/references/Y.md` | `.github/skills/X/SKILL.md` |
| Phase 0 | 5+ steps (includes epic ID extraction + mkdir) | 5+ steps (same structure) |
| Phase 2 coding standards | Split: `coding-conventions.md` + `version-features.md` | Combined: `.github/skills/coding-standards/SKILL.md` |
| Phase 1D events | `skills/protocols/references/event-driven-conventions.md` | `.github/skills/protocols/SKILL.md` |
| Phase 1E security | `skills/security/SKILL.md`, `skills/compliance/SKILL.md` | `.github/skills/security/SKILL.md`, `.github/skills/compliance/SKILL.md` |
| Integration Notes footer | `placeholders resolved from project configuration` | `{{PLACEHOLDER}} tokens are runtime markers... NOT resolved during generation` + Detailed References section |
| Phase 7 completion | `>>> Phase N/7 completed. Proceeding to Phase N+1...` | Same + distinct Phase 7 message: `>>> Phase 7/7 completed. Lifecycle complete.` |

**Specific sub-tasks (mirror of G1):**

- TASK-G2-01a: Phase 0 -- add test plan existence check
- TASK-G2-01b: Phase 1B -- promote to mandatory driver
- TASK-G2-01c: Phase 1C -- derive from test plan
- TASK-G2-01d: Phase 2 -- TDD primary mode (replace G1-G7 as primary, with `.github/skills/` paths)
- TASK-G2-01e: Phase 2 -- parallelism section
- TASK-G2-01f: Phase 2 -- G1-G7 fallback path
- TASK-G2-01g: Phases 3 and 6 -- TDD references
- TASK-G2-01h: Phases 4-5 -- TDD discipline and PR description
- TASK-G2-01i: Phase 7 -- TDD DoD items
- TASK-G2-01j: Integration Notes + Detailed References update

---

## G3: Deployed Copy -- `.claude/skills/x-dev-lifecycle/SKILL.md`

### TASK-G3-01: Mirror G1 changes exactly

**File:** `.claude/skills/x-dev-lifecycle/SKILL.md`
**Change type:** Full mirror of Claude Code source template (G1)

This file is the deployed copy for this project's own development workflow. It uses slightly different path conventions (e.g., `docs/plans/STORY-ID-*` instead of `docs/stories/epic-XXXX/plans/*`), but the TDD structural changes must match G1 exactly in logic and wording.

Key differences from G1 source template to account for:
- Path patterns use `docs/plans/STORY-ID-*` format (shorter paths)
- No epic ID extraction / mkdir steps in Phase 0 (only 3 steps currently)
- All skill path references use `skills/X/references/Y.md` pattern (same as G1)

**Sub-tasks (mirror of G1):**
- TASK-G3-01a: Phase 0 -- add test plan existence check (after step 2, before step 3)
- TASK-G3-01b: Phase 1B -- promote to mandatory driver
- TASK-G3-01c: Phase 1C -- derive from test plan
- TASK-G3-01d: Phase 2 -- TDD primary mode
- TASK-G3-01e: Phase 2 -- parallelism section
- TASK-G3-01f: Phase 2 -- G1-G7 fallback path
- TASK-G3-01g: Phases 3 and 6 -- TDD references
- TASK-G3-01h: Phases 4-5 -- TDD discipline and PR description
- TASK-G3-01i: Phase 7 -- TDD DoD items
- TASK-G3-01j: Integration Notes update

---

## G4: Golden Files -- Regenerate all 16 golden files

### TASK-G4-01: Regenerate golden files via test pipeline

**Files:** 16 golden files (8 profiles x 2 platform copies)

**Claude Code golden files (8):**

| # | Path |
|---|------|
| 1 | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 2 | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 3 | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 4 | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 5 | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 6 | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 7 | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` |
| 8 | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` |

**GitHub Copilot golden files (8):**

| # | Path |
|---|------|
| 9 | `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md` |
| 10 | `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md` |
| 11 | `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md` |
| 12 | `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md` |
| 13 | `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` |
| 14 | `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md` |
| 15 | `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md` |
| 16 | `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md` |

**Procedure:**
1. Run `npx vitest run` -- expect 16 golden file test failures for `x-dev-lifecycle`
2. Use the golden file update mechanism (likely `UPDATE_GOLDEN=true npx vitest run` or equivalent) to regenerate
3. Alternatively, run the generator for each profile and copy outputs to golden directories
4. Re-run `npx vitest run` to verify all 16 files match byte-for-byte

---

## G5: Compilation + Tests -- Verification

### TASK-G5-01: TypeScript compilation check

**Command:** `npx tsc --noEmit`
**Expected result:** Clean (no new TS source code changed)
**Purpose:** Verify no accidental TS file modifications

---

### TASK-G5-02: Full test suite

**Command:** `npx vitest run`
**Expected result:** All ~1,384 tests passing, including 16 regenerated golden files
**Coverage targets:** Line >= 95%, Branch >= 90% (expected unchanged at 99.6%/97.84%)

---

### TASK-G5-03: Byte-for-byte parity validation

**Validation:** The `byte-for-byte.test.ts` integration tests (using `describe.sequential.each`) must confirm:
- Each of the 8 `.claude/skills/x-dev-lifecycle/SKILL.md` golden files matches the generated output from `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
- Each of the 8 `.github/skills/x-dev-lifecycle/SKILL.md` golden files matches the generated output from `resources/github-skills-templates/dev/x-dev-lifecycle.md`

---

## Execution Order

```
TASK-G1-01 through TASK-G1-10  (sequential, same file)
         |
         v
TASK-G2-01                     (mirror G1 with GitHub paths)
         |
         v
TASK-G3-01                     (mirror G1 for deployed copy)
         |
         v
TASK-G5-01                     (compile check -- verify no TS breakage)
         |
         v
TASK-G4-01                     (regenerate golden files)
         |
         v
TASK-G5-02 + TASK-G5-03        (full test suite + byte-for-byte parity)
```

**Note:** G1 tasks are sequential because they edit the same file. G2 and G3 could run in parallel with each other (different files) but logically depend on G1 being complete. G4 depends on G1+G2+G3 because the generator reads source templates. G5 is final verification.

---

## Summary

| Group | Tasks | Files | Impact |
|-------|-------|-------|--------|
| G1: Claude Code source template | 10 tasks | 1 file | HIGH (Phase 2 restructure) |
| G2: GitHub Copilot template | 1 task (10 sub-tasks) | 1 file | HIGH (mirror of G1) |
| G3: Deployed copy | 1 task (10 sub-tasks) | 1 file | HIGH (mirror of G1) |
| G4: Golden files | 1 task | 16 files | MEDIUM (regeneration) |
| G5: Verification | 3 tasks | 0 files | LOW (read-only checks) |
| **Total** | **16 tasks** | **19 files** | |
