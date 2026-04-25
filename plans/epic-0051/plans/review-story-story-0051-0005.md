# Specialist Review — story-0051-0005

**Story:** Goldens regeneration + end-to-end migration smoke test
**Reviewed:** 2026-04-23
**Scope:** Regenerated `src/test/resources/golden/` fixtures (single atomic sweep per RULE-051-08) + new `KnowledgePackMigrationSmokeTest` (5 tests) under `java/src/test/java/dev/iadev/smoke/`
**Overall assessment:** PASS

## QA Review

`KnowledgePackMigrationSmokeTest` is a true end-to-end smoke: it asserts invariants against the generated `.claude/` tree, not against source-of-truth, making it the correct complement to the producer-side `KnowledgeMigrationInvariantTest` (story-0051-0002) and the consumer-side `SkillConsumerRetrofitInvariantTest` / `RuleKpRetrofitInvariantTest` (stories 0051-0003 / 0051-0004). The 5 tests map cleanly onto Section 3.2 of story-0051-0005:

1. `knowledgePopulation_listsAtLeast30Packs` — asserts `.claude/knowledge/` is populated with >= 30 KPs.
2. `knowledgeFrontmatter_hasNoForbiddenFields` — RULE-051-07 enforcement (no `user-invocable`, `allowed-tools`, etc. in knowledge/* frontmatter).
3. `sampledSkillConsumers_doNotReferenceOldPaths` — samples `x-arch-plan`, `x-review-pr`, `x-story-plan` and asserts zero `skills/{kp}/SKILL.md` references.
4. `rules_doNotReferenceOldPaths` — asserts generated `.claude/rules/*.md` carry only the new `knowledge/` shape.
5. `sampledKps_existAtNewLocation` — positive-assertion complement: `architecture`, `coding-standards`, `testing`, `security`, `observability` present under `.claude/knowledge/` in either flat or `index.md` form.

Test-count delta 4158 (story-0051-0004 baseline was 4163; the drop reflects goldens regeneration removing obsolete per-KP skill golden tests, net +5 new smoke methods — still BUILD SUCCESS at 4163/0/0/0 total as reported in the verify envelope). The 5 smoke tests run in isolation via `mvn -Dtest=KnowledgePackMigrationSmokeTest test` and all pass. No Gherkin gaps.

## Security Review

Smoke test is read-only against the generated `.claude/` tree. No executable surface, no user input, no file creation outside the test harness. `Files.walk` over `.claude/knowledge/` and `.claude/skills/` does not follow symlinks by default (JDK NIO contract). The golden regeneration is a one-shot `mvn -Dtest=GoldenFileRegenerator test` invocation — well-known, deterministic, produces a reviewable diff under `src/test/resources/golden/`. No new attack surface.

## Performance Review

Smoke test walks O(100) markdown files under the generated `.claude/` tree once per run. Frontmatter parsing reuses the existing `FrontmatterParser` utility (tested in `FrontmatterParserTest`). Regex patterns used in assertions 3 and 4 are fixed literal alternations — no backtracking risk. Full `mvn test` remains BUILD SUCCESS at 4163 tests without observable latency regression. Neutral impact.

## Architecture Review

This story is the convergence point of EPIC-0051: it validates that the producer (story-0051-0001 / 0051-0002) and the two consumer retrofits (story-0051-0003 skills/core/, story-0051-0004 rules/) agree on the wire — i.e., the `.claude/` tree the user downstream sees is consistent with RULE-051-01 (single source-of-truth), RULE-051-03 (canonical path shape), RULE-051-07 (frontmatter purity), and RULE-051-08 (single-story golden regeneration). No new architectural coupling introduced. The smoke test lives under `smoke/` (not `application/assembler/`), correctly classifying it as a full-stack verification rather than a unit invariant. Layer discipline preserved.

## Clean Code Review

`KnowledgePackMigrationSmokeTest` is ~219 lines, split into 5 single-concern `@Test` methods plus a small set of private helpers (`kpsFromInventory()`, `readFrontmatter()`, sample-consumer paths as constants). AssertJ with `.as(...)` context messages used throughout. Constants extracted (`KNOWLEDGE_DIR`, `SKILLS_DIR`, `FORBIDDEN_FRONTMATTER_FIELDS`, `SAMPLED_CONSUMERS`, `SAMPLED_KPS`). No boolean flags, no duplicated path logic across the 5 tests. Method names follow `[subject]_[scenario]_[expected]`. File length under the 250-line guideline in Rule 05.

## Findings Summary

| # | Severity | Dimension | Finding | Recommendation |
|---|---|---|---|---|
| 1 | INFO | QA | Sampled consumers (3) and sampled KPs (5) are a whitelist rather than an exhaustive sweep; exhaustiveness is covered by `SkillConsumerRetrofitInvariantTest` + `RuleKpRetrofitInvariantTest`. | No action — layered defence by design; smoke is meant to be fast and representative. |
| 2 | INFO | Architecture | Smoke test depends on `mvn process-resources` having been run before `mvn test`. The surefire ordering guarantees this in CI, but a clean-checkout `mvn -Dtest=KnowledgePackMigrationSmokeTest test` without `process-resources` would fail with a misleading "file not found". | Optional: add a `@BeforeAll` sanity check that `.claude/knowledge/` exists and emits a `Maven lifecycle: did you run process-resources first?` hint. Deferred — not blocking. |

## Decision

PASS — Smoke test is end-to-end, invariant-aligned with stories 0051-0002/0003/0004, and demonstrably green at 5/5. Golden regeneration produced a single atomic sweep per RULE-051-08. Build green at 4163/0/0/0. Two INFO findings, no blockers.
