# QA Review — story-0004-0006

```
ENGINEER: QA
STORY: story-0004-0006
SCORE: 27/36
STATUS: Rejected
---
PASSED:
- [2] Line coverage >= 95% (2/2) — 99.5% line coverage, well above threshold
- [3] Branch coverage >= 90% (2/2) — 97.66% branch coverage, well above threshold
- [5] AAA pattern (2/2) — Tests follow Arrange (beforeAll + const), Act (regex/split), Assert (expect) consistently
- [8] No test interdependency (2/2) — Each test reads content independently via module-level const; no shared mutable state
- [10] Unique test data (2/2) — Tests validate static template content; no resource creation requiring unique IDs
- [12] Integration tests for DB/API (2/2) — N/A for template content; existing byte-for-byte golden file integration tests cover pipeline output across all 8 profiles (1784 tests all passing)
- [18] TDD coverage thresholds maintained (2/2) — 99.5% line / 97.66% branch; zero regression

PARTIAL:
- [1] Test exists for each AC (1/2) — tests/node/skills/x-dev-architecture-plan.test.ts — Improvement: Test plan specified 65 new tests; implementation delivers 55. Missing tests: dual-copy consistency (UT-59 to UT-63: 5 tests verifying RULE-001 that both Claude and GitHub templates contain equivalent content), detailed frontmatter validation (UT-4 description non-empty, UT-5 allowed-tools item list, UT-6 argument-hint STORY-ID content), infrastructure and compliance KP assertions (UT-19, UT-21), data model section (UT-34), Mermaid decision tree diagram (UT-12), and placeholder token presence (UT-47, UT-58). The 5 dual-copy consistency tests are particularly important as RULE-001 is a listed transversal rule for this story. [MEDIUM]
- [4] Test naming convention (1/2) — tests/node/skills/x-dev-architecture-plan.test.ts — Improvement: Tests use `it("sectionName_aspectTested")` pattern which is close to but not strictly `[methodUnderTest]_[scenario]_[expectedBehavior]`. Examples: `knowledgePacks_listsAtLeast6KPs` is good, but `githubTemplate_fileExists` omits the expected behavior suffix. Minor; the intent is clear. [LOW]
- [6] Parametrized tests for data-driven (1/2) — tests/node/skills/x-dev-architecture-plan.test.ts — Improvement: Knowledge pack validation (architecture, protocols, security, observability, resilience) and output structure sections (component diagram, sequence diagrams, etc.) are repetitive assertions that could benefit from `it.each()` parametrization. Currently 11 individual output structure tests and 5 KP tests follow the same pattern with different strings. [LOW]
- [9] Fixtures centralized (1/2) — tests/node/skills/x-dev-architecture-plan.test.ts — Improvement: File paths are defined as module-level constants (SKILL_PATH, GITHUB_SKILL_PATH) which is good, but content is loaded in `beforeAll` at the top level while some section extraction logic (`split("## Section")[1]!.split(/\n## /)[0]!`) is duplicated across 10+ tests. Extract a helper `extractSection(content, heading)` to centralize this pattern. [LOW]
- [11] Edge cases (1/2) — tests/node/skills/x-dev-architecture-plan.test.ts — Improvement: Missing edge case tests for: (a) what happens if SKILL.md frontmatter is malformed (no closing `---`), (b) placeholder `{{PROJECT_NAME}}` resolves correctly after template engine processing, (c) GitHub template uses `.github/skills/` paths instead of `.claude/skills/` paths consistently (UT-58 from test plan). The `githubTemplate_usesGithubPaths` test only checks one pattern. [LOW]
- [17] Acceptance tests validate E2E behavior (1/2) — Improvement: Acceptance test AT-1 from the test plan (full pipeline generates skill in all 3 output locations for every profile) is covered implicitly by existing byte-for-byte golden file tests, but there is no explicit acceptance-level test written as part of this story's TDD outer loop. The dual-copy consistency tests (UT-59 to UT-63) that would serve as a proxy for cross-location validation are absent. [MEDIUM]

FAILED:
- [7] Exception paths tested (0/2) — tests/node/skills/x-dev-architecture-plan.test.ts — Fix: No tests validate error conditions. Missing: (a) test that readFileSync throws if SKILL.md path is wrong, (b) test that `split("## Section")[1]` handles case where section is missing (currently uses non-null assertion `!` which would throw at runtime), (c) test for malformed frontmatter without closing `---` delimiter. Add at least 2 negative-path tests. [MEDIUM]
- [13] Commits show test-first pattern (0/2) — Commit 05dc0f2 bundles tests AND implementation in the same commit. The commit message claims "RED: wrote 53 unit tests... GREEN: created SKILL.md..." but git history shows a single commit with both test file and source templates added simultaneously. TDD requires separate commits: one for the failing test (RED), one for making it pass (GREEN), and optionally one for refactoring. There is no way to verify from the git log that tests were written first. [HIGH]
- [14] Explicit refactoring after green (0/2) — No REFACTOR commit exists. The commit message states "REFACTOR: noop" but the TDD workflow requires examining the code for duplication after GREEN. The repeated `split("## Section")[1]!.split(/\n## /)[0]!` pattern across 10+ tests is an obvious refactoring candidate that was not addressed. [MEDIUM]
- [15] Tests follow TPP progression (0/2) — tests/node/skills/x-dev-architecture-plan.test.ts — Fix: While the test plan (Section 9) defines a clear TPP ordering (degenerate -> frontmatter -> decision tree -> KPs -> output structure -> mini-ADR -> subagent), the actual test implementation starts at a mid-complexity level (regex matching on frontmatter fields) without establishing degenerate cases first. There is no `nil->constant` phase (e.g., testing empty content returns). Tests jump directly to `constant->variable` and `unconditional->conditional` transformations without the simpler predecessors. [MEDIUM]
- [16] No test written after implementation (0/2) — Cannot be verified. Since commit 05dc0f2 contains both test file creation and implementation file creation in the same commit, there is no git evidence that tests preceded implementation. The golden file regeneration commit (e4c7ec7) is separate, which is appropriate, but the core RED-GREEN cycle is opaque. [HIGH]
```

## Detailed Analysis

### Test Coverage Gap

The test plan (`docs/stories/epic-0004/plans/tests-story-0004-0006.md`) specifies 65 new tests. The implementation delivers 55 (53 in `tests/node/skills/x-dev-architecture-plan.test.ts` + 2 in `tests/node/assembler/github-skills-assembler.test.ts`). The 10 missing tests are:

| Test Plan ID | Description | Status |
|---|---|---|
| UT-4 | `frontmatter_descriptionIsNonEmpty` | Missing |
| UT-5 | `frontmatter_allowedToolsContainsReadWriteEditBashGrepGlob` | Missing |
| UT-6 | `frontmatter_argumentHintContainsStoryIdOrFeature` | Missing |
| UT-12 | `whenToUse_containsMermaidDecisionTree` | Missing |
| UT-19 | `knowledgePacks_containsInfrastructureKP` | Missing |
| UT-21 | `knowledgePacks_containsComplianceKP` | Missing |
| UT-34 | `outputStructure_containsDataModel` | Missing |
| UT-47 | `claudeSource_containsProjectNamePlaceholder` | Missing (subagent prompt test covers `{{PROJECT_NAME}}` partially) |
| UT-58 | `githubSource_containsPlaceholderTokens` | Missing |
| UT-59-63 | Dual copy consistency (5 tests for RULE-001) | Missing |

### TDD Compliance

The most significant finding is the **lack of separate RED/GREEN commits**. Commit `05dc0f2` contains both the test file and the implementation in a single atomic commit. While the commit message documents the TDD phases textually, the git history cannot prove that tests were written before implementation. Best practice per the testing philosophy requires:

1. Commit A: Tests only (RED -- all fail)
2. Commit B: Implementation (GREEN -- all pass)
3. Commit C: Refactoring (REFACTOR -- all still pass)

### What Works Well

- **53 content tests** provide thorough section-level validation of the SKILL.md template
- **GitHub template tests** (8 tests) validate the dual-copy target
- **Assembler registration tests** (2 tests) verify `SKILL_GROUPS["dev"]` membership
- **Golden file regeneration** covers all 8 profiles across 3 output directories (24 new golden files)
- **Coverage** remains excellent at 99.5% / 97.66%
- Test names are descriptive and follow `section_aspect` convention
- No test interdependencies; each test reads from module-level constants

### Recommendations for Approval

1. **[HIGH]** Split the implementation commit into separate RED and GREEN commits (or at minimum, add a note explaining why this was not feasible)
2. **[MEDIUM]** Add the 5 dual-copy consistency tests (UT-59 to UT-63) to validate RULE-001
3. **[MEDIUM]** Add at least 2 exception/negative-path tests
4. **[LOW]** Extract `extractSection(content, heading)` helper to eliminate section-splitting duplication
5. **[LOW]** Use `it.each()` for repetitive KP and output structure assertions
