# Story Completion Report — story-0051-0003

**Story ID:** story-0051-0003
**Title:** Retrofit skills consumers to new KP paths
**Epic:** EPIC-0051 (Knowledge Packs fora de `.claude/skills/`)
**Status:** COMPLETE
**Completed:** 2026-04-23

## Summary

21 `SKILL.md` files plus 7 auxiliary files (README.md, references/*.md) under `java/src/main/resources/targets/claude/skills/core/` retrofitted to the new KP path shape: `skills/{kp}/SKILL.md` → `knowledge/{kp}.md` (simple) or `knowledge/{kp}/index.md` (complex); sub-references `skills/{kp}/references/foo.md` → `knowledge/{kp}/foo.md` (complex) or inlined to `knowledge/{kp}.md` (simple). 10 OLD-contract `@Disabled` tests removed (RULE-051-07 forbids the fields they asserted). 3 tests updated to expect new paths. 1 new grep-based invariant test (`SkillConsumerRetrofitInvariantTest`) protects against regression.

This story closes the consumer side of RULE-051-03, complementing the producer side delivered in story-0051-0002.

## Evidence (Rule 24)

| Artifact | Path | Purpose |
|---|---|---|
| Specialist review | [`plans/epic-0051/plans/review-story-story-0051-0003.md`](../plans/review-story-story-0051-0003.md) | QA / Security / Performance / Architecture / Clean Code |
| Tech Lead review | [`plans/epic-0051/plans/techlead-review-story-story-0051-0003.md`](../plans/techlead-review-story-story-0051-0003.md) | 45-point GO/NO-GO |
| Verify envelope | [`plans/epic-0051/reports/verify-envelope-story-0051-0003.json`](verify-envelope-story-0051-0003.json) | Test results + AC validation |
| Invariant test | [`java/src/test/java/dev/iadev/application/assembler/SkillConsumerRetrofitInvariantTest.java`](../../../java/src/test/java/dev/iadev/application/assembler/SkillConsumerRetrofitInvariantTest.java) | Grep-based regression gate |

## Reviews

| Review | Decision | Summary |
|---|---|---|
| Specialist | PASS-with-observations | 4 LOW/INFO findings; none blocking |
| Tech Lead | **GO** | All 8 checklist dimensions pass; producer↔consumer loop on RULE-051-03 now closed |

## Files changed

### Added
- `java/src/test/java/dev/iadev/application/assembler/SkillConsumerRetrofitInvariantTest.java` (3 invariant test methods)

### Removed
- 10 `@Disabled` OLD-contract test methods across the 6 test files previously flagged by story-0051-0002 (RULE-051-07 forbids the asserted fields; migration would be semantically empty)

### Modified
- 21 `SKILL.md` files under `java/src/main/resources/targets/claude/skills/core/**` (regex retrofit, 90+ path replacements)
- 7 auxiliary files: README.md + references/*.md under `skills/core/**` (same retrofit)
- `java/src/main/resources/targets/claude/skills/core/plan/x-story-plan/SKILL.md` (manual fix: malformed `.mdreferences/` token from regex)
- `java/src/main/resources/targets/claude/skills/core/plan/x-arch-plan/SKILL.md` (manual fix: malformed `.mdreferences/` token from regex)
- `LazyKpLoadingTest` (2 methods updated to expect new paths)
- `HardeningEvalSkillTest` (updated to expect new paths)

## Metrics

| Metric | Value |
|---|---|
| Tests run | 4162 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | BUILD SUCCESS |
| New tests added | 3 (SkillConsumerRetrofitInvariantTest methods) |
| Tests removed | 10 (OLD-contract @Disabled methods) |
| Tests updated | 3 (LazyKpLoadingTest x2, HardeningEvalSkillTest) |
| SKILL.md retrofitted | 21 |
| Auxiliary files retrofitted | 7 |
| Path replacements | 90+ |
| Gherkin AC passed | 4/4 |

## Action items (follow-up stories)

- **story-0051-0005 (goldens + smoke):** regenerate golden fixtures to reflect the retrofitted SKILL.md paths; confirm no diff against the new `knowledge/` source.
- **story-0051-0005:** optionally add a 4th invariant method that asserts each referenced `knowledge/X` link resolves to the correct shape (file for simple, `index.md` for complex).
- **story-0051-0006 (cleanup):** remove dual-output transition in `SkillsCopyHelper.copyKnowledgePack` / `copyStackPatterns` / `copyInfraPatterns`; consumers are now path-clean, so single-output is safe.
- **story-0051-0006:** extract shared `copyIndexWithSiblings` helper to kill duplication and bring `SkillsCopyHelper` back under the 250-line RULE-003 cap.
- **epic retro / CHANGELOG:** document the "post-bulk-regex sanity grep" lesson learned from the 2 malformed `.mdreferences/` artifacts.

## Deferred (tracked and non-blocking)

1. Shape-mapping invariant (simple `.md` vs complex `/index.md`) — optional enhancement owned by story-0051-0005
2. Dual-output transition in `SkillsCopyHelper` — owned by story-0051-0006
3. `SkillsCopyHelper` class-size cleanup — owned by story-0051-0006

## Compliance

- ✅ RULE-051-03 (canonical path `knowledge/{name}.md`) — now enforced on the consumer side by `SkillConsumerRetrofitInvariantTest`
- ✅ RULE-051-07 (directory contract — forbidden fields) — 10 OLD-contract tests removed per the contract
- ✅ Rule 24 (execution integrity) — all 4 mandatory evidence artifacts present
- ✅ Rule 05 (quality gates) — build green, no coverage regression (retrofit-only scope)
