# Test Plan -- story-0003-0003: Rules 03 & 05 — TDD Practices and TDD Compliance

## Summary

- Total test classes: 0 new (existing `byte-for-byte.test.ts` covers all scenarios)
- Total test methods: ~0 new code (golden file updates only)
- Categories covered: Integration (golden file parity)
- Estimated line coverage: unchanged (no source code changes)

## Strategy

This story modifies **static markdown content** only — no TypeScript source code changes. The existing `byte-for-byte.test.ts` integration test already validates all generated output against golden files for all 8 profiles. The test strategy is:

1. Update golden files with expected new TDD content (RED — tests fail)
2. Update source-of-truth resource files (GREEN — tests pass)
3. Verify all tests pass and coverage is maintained (REFACTOR)

## Test Scenarios via Golden File Parity

### Scenario 1: Rule 03 contains TDD Practices section

| # | Profile | Golden File | Assertion |
|---|---------|-------------|-----------|
| 1 | All 8 | `.claude/rules/03-coding-standards.md` | Contains `## TDD Practices` heading |
| 2 | All 8 | `.claude/rules/03-coding-standards.md` | Contains `Red-Green-Refactor` keyword |
| 3 | All 8 | `.claude/rules/03-coding-standards.md` | Contains reference to `skills/testing/SKILL.md` |

### Scenario 2: Rule 05 merge checklist includes TDD items

| # | Checklist Item | Verified In |
|---|---------------|-------------|
| 1 | `Commits show test-first pattern` | `.claude/rules/05-quality-gates.md` × 8 profiles |
| 2 | `Explicit refactoring after green` | `.claude/rules/05-quality-gates.md` × 8 profiles |
| 3 | `Tests are incremental` | `.claude/rules/05-quality-gates.md` × 8 profiles |
| 4 | `No test written AFTER implementation` | `.claude/rules/05-quality-gates.md` × 8 profiles |
| 5 | `Acceptance tests exist` | `.claude/rules/05-quality-gates.md` × 8 profiles |

### Scenario 3: Rule 05 contains TDD Compliance section

| # | Profile | Golden File | Assertion |
|---|---------|-------------|-----------|
| 1 | All 8 | `.claude/rules/05-quality-gates.md` | Contains `## TDD Compliance` heading |
| 2 | All 8 | `.claude/rules/05-quality-gates.md` | Contains `Double-Loop TDD` |
| 3 | All 8 | `.claude/rules/05-quality-gates.md` | Contains `Transformation Priority Premise` |
| 4 | All 8 | `.claude/rules/05-quality-gates.md` | Contains `Atomic TDD commits` or `atomic` + `commit` |

### Scenario 4: Existing sections preserved (backward compatibility)

| # | Rule | Section | Status |
|---|------|---------|--------|
| 1 | 03 | `## Hard Limits` | Must exist in golden files |
| 2 | 03 | `## Naming` | Must exist |
| 3 | 03 | `## SOLID` | Must exist |
| 4 | 03 | `## Error Handling` | Must exist |
| 5 | 03 | `## Forbidden` | Must exist |
| 6 | 03 | `## Language-Specific Conventions` | Must exist |
| 7 | 05 | `## Coverage Thresholds` | Must exist, values unchanged |
| 8 | 05 | `## Test Categories` | Must exist |
| 9 | 05 | `## Test Naming` | Must exist |
| 10 | 05 | `## Merge Checklist` | Original 6 items preserved |
| 11 | 05 | `## Forbidden` | Must exist |

### Scenario 5: Coverage thresholds not duplicated

| # | Assertion |
|---|-----------|
| 1 | `## TDD Compliance` section does NOT contain `≥ 95%` or `≥ 90%` values |
| 2 | Coverage table appears exactly once in Rule 05 |

### Scenario 6: GitHub instructions golden files match

| # | Profile | Golden File | Change |
|---|---------|-------------|--------|
| 1 | All 8 | `.github/instructions/coding-standards.instructions.md` | Contains `## TDD Practices` |
| 2 | All 8 | `.github/instructions/quality-gates.instructions.md` | Contains 5 TDD checklist items + `## TDD Compliance` |

### Scenario 7: AGENTS.md golden files match

| # | Profile | Assertion |
|---|---------|-----------|
| 1 | All 8 | Coding Standards section contains `### TDD Practices` |
| 2 | All 8 | Quality Gates section contains 5 TDD checklist items |
| 3 | All 8 | Quality Gates section contains `### TDD Compliance` |

## Coverage Estimation

No TypeScript source code changes → existing coverage unaffected.

| Metric | Current | Expected |
|--------|---------|----------|
| Line Coverage | 99.6% | 99.6% (unchanged) |
| Branch Coverage | 97.84% | 97.84% (unchanged) |

## Risks and Gaps

- **AGENTS.md profiles differ**: Test Categories vary per profile (some exclude Contract/Performance/Smoke). TDD sections are static and profile-independent — safe to batch update.
- **Nunjucks rendering**: AGENTS.md golden files are generated from Nunjucks templates. The TDD sections have no template variables, so rendering is a passthrough.
- **Byte-for-byte sensitivity**: Even whitespace differences will cause test failures. Must match exactly.

## Files Modified (Golden Files Only — 40 files)

- 16 `.claude/rules/` golden files (8 profiles × 2 rules)
- 16 `.github/instructions/` golden files (8 profiles × 2 rules)
- 8 `AGENTS.md` golden files (8 profiles)
