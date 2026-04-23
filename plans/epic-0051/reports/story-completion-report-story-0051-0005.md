# Story Completion Report — story-0051-0005

**Story ID:** story-0051-0005
**Title:** Goldens regeneration + end-to-end migration smoke test
**Epic:** EPIC-0051 (Knowledge Packs fora de `.claude/skills/`)
**Status:** COMPLETE
**Completed:** 2026-04-23

## Summary

Single atomic regeneration of `src/test/resources/golden/` (per RULE-051-08) plus a new end-to-end smoke test (`KnowledgePackMigrationSmokeTest`) that validates the migration against the **generated** `.claude/` tree. Five tests assert:

1. `.claude/knowledge/` is populated with >= 30 KPs (matches `kp-inventory.txt`).
2. No forbidden frontmatter fields (`user-invocable`, `allowed-tools`, etc.) in `.claude/knowledge/*` — RULE-051-07.
3. Sampled skill consumers (`x-arch-plan`, `x-review-pr`, `x-story-plan`) contain zero references to the legacy `skills/{kp}/SKILL.md` path shape.
4. Generated rules (`.claude/rules/*.md`) reference only the new `knowledge/` prefix.
5. Sampled KPs (`architecture`, `coding-standards`, `testing`, `security`, `observability`) resolve on disk to either `knowledge/{kp}.md` (simple) or `knowledge/{kp}/index.md` (complex).

This story is the convergence point of EPIC-0051: it validates that the producer (story-0051-0002) and the two consumer retrofits (stories 0051-0003 skills/core/ and 0051-0004 rules/) agree on the wire — i.e., the `.claude/` tree the downstream user receives is consistent with RULE-051-01, -03, -07, and -08.

## Evidence (Rule 24)

| Artifact | Path | Purpose |
|---|---|---|
| Specialist review | [`plans/epic-0051/plans/review-story-story-0051-0005.md`](../plans/review-story-story-0051-0005.md) | QA / Security / Performance / Architecture / Clean Code |
| Tech Lead review | [`plans/epic-0051/plans/techlead-review-story-story-0051-0005.md`](../plans/techlead-review-story-story-0051-0005.md) | 45-point GO/NO-GO |
| Verify envelope | [`plans/epic-0051/reports/verify-envelope-story-0051-0005.json`](verify-envelope-story-0051-0005.json) | Test results + AC validation |
| Smoke test | [`java/src/test/java/dev/iadev/smoke/KnowledgePackMigrationSmokeTest.java`](../../../java/src/test/java/dev/iadev/smoke/KnowledgePackMigrationSmokeTest.java) | 5 end-to-end invariants |

## Reviews

| Review | Decision | Summary |
|---|---|---|
| Specialist | PASS | 2 INFO findings, no LOW/MED/HIGH issues |
| Tech Lead | **GO** | All 8 checklist dimensions pass; EPIC-0051 invariant loop closed (producer + skills/core/ consumer + rules/ consumer + end-to-end) |

## Files changed

### Added
- `java/src/test/java/dev/iadev/smoke/KnowledgePackMigrationSmokeTest.java` (5 smoke tests, ~219 lines)

### Modified (regenerated)
- `src/test/resources/golden/**` — single atomic sweep per RULE-051-08:
  - **Deleted:** `.claude/skills/{kp}/SKILL.md` × ~32 (obsolete per-KP skill goldens)
  - **Created:** `.claude/knowledge/{kp}.md` or `.claude/knowledge/{kp}/index.md` × ~32
  - **Modified:** `.claude/skills/core/**/SKILL.md` (story-0051-0003 retrofit deltas), `.claude/rules/0[3-9]-*.md` (story-0051-0004 retrofit deltas), `.claude/README.md` (Knowledge Packs section)

### Untouched by design
- No production Java modified in this story (scope is regeneration + smoke).

## Metrics

| Metric | Value |
|---|---|
| Tests run | 4163 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | BUILD SUCCESS |
| New smoke tests added | 5 (KnowledgePackMigrationSmokeTest) |
| Obsolete tests retired | 5 (per-KP skill-golden assertions replaced by regeneration) |
| Net test delta | 0 (stable 4163) |
| Smoke suite isolated | 5/5 PASS (`mvn -Dtest=KnowledgePackMigrationSmokeTest test`) |
| Gherkin AC passed | 5/5 |
| Golden diff atomicity | 1 commit (RULE-051-08 compliant) |

## Action items (follow-up stories)

- **story-0051-0006 (cleanup):** remove the dual-output transition from `SkillsCopyHelper` (the branch that still emits `.claude/skills/{kp}/SKILL.md` in parallel with the new `.claude/knowledge/` output during the migration window). `KnowledgePackMigrationSmokeTest` test 3 acts as the regression gate — it MUST remain green after the cleanup.
- **story-0051-0006:** shrink `SkillsCopyHelper` below the Rule 03 class-size guideline (≤ 250 lines) as part of the dual-output removal, since the transition branch is its main size contributor.
- **post-epic:** optional — promote the sampled-consumer list in the smoke test from 3 to N when a new consumer category is introduced downstream.

## Deferred (tracked and non-blocking)

1. Dual-output transition in `SkillsCopyHelper` — owned by story-0051-0006.
2. `SkillsCopyHelper` class-size cleanup — owned by story-0051-0006.
3. Operator-friendly sanity check in `KnowledgePackMigrationSmokeTest` (`@BeforeAll` hint "did you run `mvn process-resources` first?") — nice-to-have, not blocking.

## Compliance

- ✅ RULE-051-01 (single source-of-truth for KPs) — asserted end-to-end by `KnowledgePackMigrationSmokeTest`
- ✅ RULE-051-03 (canonical path shape `knowledge/{kp}.md` / `knowledge/{kp}/index.md`) — asserted by smoke test #5
- ✅ RULE-051-07 (no forbidden frontmatter fields in knowledge/*) — asserted by smoke test #2
- ✅ RULE-051-08 (goldens regenerated in a single story) — golden diff is one atomic sweep in this story
- ✅ Rule 24 (execution integrity) — all 4 mandatory evidence artifacts present
- ✅ Rule 05 (quality gates) — build green, no coverage regression (regeneration + smoke scope; no production Java modified)
- ✅ Story Section 3.3 legacy audits — interactive gates, Rule 20 telemetry, and Rule 13 skill invocation audits all green post-regeneration
