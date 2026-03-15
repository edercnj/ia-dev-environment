# QA Review — story-0003-0009

```
ENGINEER: QA
STORY: story-0003-0009
SCORE: 16/24
STATUS: Rejected
---
PASSED:
- [2] Line coverage >= 95% (2/2) — 99.5% line coverage confirmed via `npx vitest run --coverage`
- [3] Branch coverage >= 90% (2/2) — 97.66% branch coverage confirmed
- [8] No test interdependency (2/2) — byte-for-byte tests are profile-independent; no shared mutable state
- [9] Fixtures centralized (2/2) — golden files follow centralized copy-from-source pattern; integration-constants.ts provides shared helpers
- [10] Unique test data (2/2) — each profile produces isolated golden file comparisons with no cross-contamination

FAILED:
- [1] Test exists for each acceptance criterion (0/2) — tests/node/content/x-story-create-content.test.ts — Fix: The test plan (tests-story-0003-0009.md) specifies 47 new content validation tests in `tests/node/content/x-story-create-content.test.ts` covering all acceptance criteria (mandatory categories, TPP ordering, minimum 4 scenarios, boundary triplet, RULE-001 dual copy consistency). This file was NEVER created. The story relies solely on existing byte-for-byte golden file tests, which validate file parity but NOT content semantics. If someone edits the source template to remove the enriched Gherkin sections while keeping the golden files in sync, no test would catch it. [CRITICAL]
- [6] Parametrized tests for data-driven scenarios (0/2) — tests/node/content/x-story-create-content.test.ts — Fix: The test plan specifies `describe.each` parametrized tests across both Claude and GitHub source templates (20 tests x 2 copies = 40 parametrized). None were implemented. Create the planned content validation tests using `describe.each` over both source paths. [HIGH]
- [7] Exception paths tested (0/2) — Fix: No tests validate negative scenarios such as: missing degenerate cases category, missing TPP ordering section, fewer than 4 scenarios instruction absent. The test plan defines these explicitly (tests #17-#20, #37-#40). Implement them. [HIGH]
- [11] Edge cases covered (0/2) — Fix: No tests validate boundary-specific content such as: the triplet pattern description (at-min, at-max, past-max), the "Less than 4 Gherkin scenarios" sizing heuristic update, or the 4 new common mistakes entries. These are all documented in the test plan but not implemented. [HIGH]

PARTIAL:
- [4] Test naming convention followed (1/2) — byte-for-byte.test.ts uses `describe.sequential.each` with profile parametrization (good), but since the 47 planned content tests were never created, naming convention compliance for the story's specific tests cannot be evaluated [LOW]
- [5] AAA pattern (Arrange-Act-Assert) (1/2) — existing byte-for-byte tests follow AAA (generate pipeline output, compare to golden file). However, the missing content validation tests would have provided more granular AAA testing per acceptance criterion [LOW]
- [12] Integration tests for DB/API (1/2) — N/A for this story (no DB/API). Byte-for-byte integration tests validate pipeline-to-golden-file parity across all 8 profiles (40 assertions pass). Partial because the planned content integration tests (dual copy consistency, RULE-001 checks #41-#47) are missing [MEDIUM]
```

## Detailed Findings

### F1 — Missing Content Validation Test File (CRITICAL)

**Test plan reference:** `docs/stories/epic-0003/plans/tests-story-0003-0009.md`, Section 2

The test plan explicitly defines a new test file `tests/node/content/x-story-create-content.test.ts` with 47 test cases organized in three describe blocks:

1. **Claude source validation** (20 tests) — validates enriched Gherkin content in the Claude template
2. **GitHub source validation** (20 tests) — mirrors Claude tests for the GitHub template
3. **Dual copy consistency / RULE-001** (7 tests) — validates both copies are semantically aligned

The existing pattern is established in `tests/node/content/refactoring-guidelines-content.test.ts`, so the implementation approach is well-defined.

**Impact:** Without these tests, the enriched Gherkin instructions are protected only by byte-for-byte golden file comparison. A future edit that accidentally removes the TPP ordering section or boundary triplet instructions would pass all tests as long as golden files are regenerated accordingly. The content validation tests serve as semantic guards.

### F2 — Scope Leak in Golden File Regeneration (LOW)

The commit `20eff0e` (golden file regeneration) includes changes to 24 files unrelated to this story:
- 8x `tests/golden/{profile}/.github/skills/x-test-plan/SKILL.md` — x-test-plan description change (case: "Go" to "go")
- 8x `tests/golden/{profile}/.claude/README.md` — x-test-plan description update
- 8x `tests/golden/{profile}/AGENTS.md` — x-test-plan description update

These appear to be from story-0003-0007 changes that were merged into main but not yet reflected in golden files. While functionally correct (the regeneration captures the latest state), it muddies the diff and makes this story's changes harder to review in isolation.

### F3 — Positive Findings

1. **RULE-001 dual copy consistency: SATISFIED** — The enriched Gherkin content (mandatory categories, TPP ordering, minimum validation, boundary triplet, common mistakes) is semantically identical between Claude and GitHub source templates. Only expected differences remain (paths, section header language).
2. **All 8 profiles updated: CONFIRMED** — .claude, .agents, and .github golden files are byte-identical to their respective source templates across all 8 profiles (go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs).
3. **All 1639 tests pass** with 99.5% line / 97.66% branch coverage.
4. **40 byte-for-byte integration assertions pass** confirming pipeline output matches updated golden files.
5. **Content accuracy: VERIFIED** — All enriched Gherkin additions match the acceptance criteria: 5 mandatory categories in TPP order, 4-scenario minimum floor, boundary value triplet pattern, sizing heuristic updated from 2 to 4, and 4 new common mistakes entries.

## Recommendation

Create `tests/node/content/x-story-create-content.test.ts` as specified in the test plan. The test plan is thorough and well-structured; the implementation gap is the only blocking issue. Estimated effort: ~2 hours following the pattern in `refactoring-guidelines-content.test.ts`.
