# Story Completion Report — story-0051-0004

**Story ID:** story-0051-0004
**Title:** Retrofit rules to new KP paths
**Epic:** EPIC-0051 (Knowledge Packs fora de `.claude/skills/`)
**Status:** COMPLETE
**Completed:** 2026-04-23

## Summary

9 rule files (7 core + 2 conditional) under `java/src/main/resources/targets/claude/rules/` retrofitted from the legacy KP path shape `skills/{kp}/SKILL.md` to the canonical `knowledge/{kp}.md` (simple) / `knowledge/{kp}/index.md` (complex) shape defined by RULE-051-03. Retrofit was applied via a regex script analogous to story-0051-0003, scoped to the rules/ tree (~10 mechanical replacements total). A new grep-based invariant test (`RuleKpRetrofitInvariantTest`) protects against regression.

This story extends RULE-051-03 enforcement from the skills/core/ consumer tree (story-0051-0003) to the rules/ consumer tree, completing the consumer-side retrofit surface for the epic.

## Evidence (Rule 24)

| Artifact | Path | Purpose |
|---|---|---|
| Specialist review | [`plans/epic-0051/plans/review-story-story-0051-0004.md`](../plans/review-story-story-0051-0004.md) | QA / Security / Performance / Architecture / Clean Code |
| Tech Lead review | [`plans/epic-0051/plans/techlead-review-story-story-0051-0004.md`](../plans/techlead-review-story-story-0051-0004.md) | 45-point GO/NO-GO |
| Verify envelope | [`plans/epic-0051/reports/verify-envelope-story-0051-0004.json`](verify-envelope-story-0051-0004.json) | Test results + AC validation |
| Invariant test | [`java/src/test/java/dev/iadev/application/assembler/RuleKpRetrofitInvariantTest.java`](../../../java/src/test/java/dev/iadev/application/assembler/RuleKpRetrofitInvariantTest.java) | Grep-based regression gate |

## Reviews

| Review | Decision | Summary |
|---|---|---|
| Specialist | PASS | 2 INFO findings, no LOW/MED/HIGH issues |
| Tech Lead | **GO** | All 8 checklist dimensions pass; RULE-051-03 enforcement now covers producer + skills/core/ + rules/ trees |

## Files changed

### Added
- `java/src/test/java/dev/iadev/application/assembler/RuleKpRetrofitInvariantTest.java` (1 invariant test method)

### Modified
- `java/src/main/resources/targets/claude/rules/01-project-identity.md`
- `java/src/main/resources/targets/claude/rules/03-coding-standards.md`
- `java/src/main/resources/targets/claude/rules/04-architecture-summary.md`
- `java/src/main/resources/targets/claude/rules/05-quality-gates.md`
- `java/src/main/resources/targets/claude/rules/06-security-baseline.md`
- `java/src/main/resources/targets/claude/rules/07-operations-baseline.md`
- `java/src/main/resources/targets/claude/rules/08-release-process.md`
- `java/src/main/resources/targets/claude/rules/09-branching-model.md`
- `java/src/main/resources/targets/claude/rules/conditional/09-data-management.md`

### Untouched by design
- `rules/02-domain.md` — placeholder-only template, no KP references
- `rules/13-skill-invocation-protocol.md` — references public skill names (x-pr-fix, x-pr-watch-ci), not KPs
- `rules/20-telemetry-privacy.md`, `rules/21-epic-branch-model.md` — no KP references

## Metrics

| Metric | Value |
|---|---|
| Tests run | 4163 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | BUILD SUCCESS |
| New tests added | 1 (RuleKpRetrofitInvariantTest.noOldKpReferences_inRules) |
| Rule files retrofitted | 9 (7 core + 2 conditional) |
| Path replacements | ~10 |
| Gherkin AC passed | 4/4 |

## Action items (follow-up stories)

- **story-0051-0005 (goldens + smoke):** regenerate golden fixtures for rule files as well as skills/core/; confirm no diff against the new `knowledge/` source.
- **story-0051-0005:** consider generalising `KnowledgeMigrationInvariantTest` to assert every `knowledge/{kp}` reference under skills/core/ AND rules/ resolves on disk to the correct shape.
- **story-0051-0006 (cleanup):** dual-output transition cleanup in `SkillsCopyHelper` remains open; rules/ retrofit does not touch that helper (different copy path).

## Deferred (tracked and non-blocking)

1. Dual-output transition in `SkillsCopyHelper` — owned by story-0051-0006
2. `SkillsCopyHelper` class-size cleanup — owned by story-0051-0006
3. Golden fixture regeneration (rules + skills/core/) — owned by story-0051-0005

## Compliance

- ✅ RULE-051-03 (canonical path `knowledge/{name}.md`) — now enforced on the rules/ consumer side by `RuleKpRetrofitInvariantTest`
- ✅ Rule 24 (execution integrity) — all 4 mandatory evidence artifacts present
- ✅ Rule 05 (quality gates) — build green, no coverage regression (retrofit-only scope; no production Java modified)
- ✅ Story Section 8 preservation contract — Rules 02, 13, 20, 21 verified byte-identical post-retrofit
