# Task Breakdown — story-0004-0012: Performance Baseline Tracking

## Decomposition Mode: Test-Driven (TDD)

Test plan available at `docs/stories/epic-0004/plans/tests-story-0004-0012.md`.

---

## TASK-1: RED — Template content validation tests (UT-1 through UT-16)

**Action:** Create `tests/node/content/performance-baseline-content.test.ts` with all 16 unit tests.
**File:** `tests/node/content/performance-baseline-content.test.ts` (NEW)
**TDD Phase:** RED — all tests must fail (template doesn't exist yet)
**Parallel:** yes (independent of TASK-2)
**Depends On:** —

## TASK-2: RED — Lifecycle performance baseline tests (UT-17 through UT-37)

**Action:** Extend `tests/node/content/x-dev-lifecycle-doc-phase.test.ts` with new `describe` blocks for performance baseline (Claude, GitHub, dual copy).
**File:** `tests/node/content/x-dev-lifecycle-doc-phase.test.ts` (EXTEND)
**TDD Phase:** RED — all tests must fail (performance baseline prompt not yet in lifecycle)
**Parallel:** yes (independent of TASK-1)
**Depends On:** —

## TASK-3: GREEN — Create performance baseline template

**Action:** Create `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` with Measurement Guide, Baselines table, Delta Interpretation, Tools by Stack.
**File:** `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` (NEW)
**TDD Phase:** GREEN — UT-1 through UT-16 turn green
**Parallel:** no
**Depends On:** TASK-1

## TASK-4: GREEN — Add performance baseline prompt to Claude lifecycle source

**Action:** Edit `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` to add performance baseline subsection in Phase 3 Documentation, after changelog and before Phase 4.
**File:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` (MODIFY)
**TDD Phase:** GREEN — UT-17 through UT-24 turn green
**Parallel:** no
**Depends On:** TASK-2

## TASK-5: GREEN — Add performance baseline prompt to GitHub lifecycle source

**Action:** Edit `resources/github-skills-templates/dev/x-dev-lifecycle.md` with parallel performance baseline prompt.
**File:** `resources/github-skills-templates/dev/x-dev-lifecycle.md` (MODIFY)
**TDD Phase:** GREEN — UT-25 through UT-37 turn green
**Parallel:** yes (can run after TASK-4 since dual copy tests need both)
**Depends On:** TASK-2, TASK-4

## TASK-6: GREEN — Update 24 golden files

**Action:** Mechanical copy of source templates to golden files across 8 profiles × 3 output dirs.
**Files:** 24 golden files (listed in implementation plan Section 12.1)
**TDD Phase:** GREEN — IT-1 (byte-for-byte) turns green
**Parallel:** no
**Depends On:** TASK-4, TASK-5

## TASK-7: REFACTOR — Run full test suite and verify coverage

**Action:** Run `npx vitest run` and `npx vitest run --coverage`. Verify no regressions, coverage maintained ≥ 95% line / ≥ 90% branch.
**TDD Phase:** REFACTOR — verify all green, no coverage regression
**Parallel:** no
**Depends On:** TASK-6

---

## Execution Order

```
TASK-1 ──┐
         ├── TASK-3 ──┐
TASK-2 ──┤            ├── TASK-4 → TASK-5 → TASK-6 → TASK-7
         └────────────┘
```

Tasks 1 and 2 can run in parallel (both RED phase).
Task 3 depends on Task 1 (template tests exist before template).
Tasks 4-5 depend on Task 2 (lifecycle tests exist before lifecycle changes).
Task 6 is mechanical copy after both sources updated.
Task 7 is final verification.
