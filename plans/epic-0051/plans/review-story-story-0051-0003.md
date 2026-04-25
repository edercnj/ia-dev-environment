# Specialist Review — story-0051-0003

**Story:** Retrofit skills consumers to new KP paths
**Reviewed:** 2026-04-23
**Scope:** 21 SKILL.md + 7 auxiliary files retrofitted; 10 OLD-contract @Disabled tests removed; 3 tests updated; new grep-based invariant test
**Overall assessment:** PASS-with-observations

## QA Review

`SkillConsumerRetrofitInvariantTest` operationalises RULE-051-03 by asserting, via grep, that no SKILL.md or auxiliary markdown under `skills/core/**` still references the old `skills/{kp}/SKILL.md` path or the old `skills/{kp}/references/` sub-tree. This is the correct shape for a retrofit gate: it is grep-deterministic, runs in milliseconds, and fails loudly on any regression that reintroduces the legacy link. The three invariant methods (old-path absent, new-path present, references mapped) cover the RULE-051-03 surface cleanly. Sampled retrofits (`x-arch-plan/SKILL.md`, `x-story-plan/SKILL.md`) show 19–22 occurrences of the new `knowledge/` path with zero residual legacy references. The removal of 10 OLD-contract `@Disabled` tests is the correct follow-through on the TL action item from story-0051-0002 — they asserted the pre-RULE-051-07 frontmatter (`user-invocable`, `allowed-tools`, `argument-hint`), which is structurally incompatible with the new format. Test-count delta (4171 → 4162, i.e. −10 removed + 3 new methods + existing KnowledgeMigrationInvariantTest = net −9) is consistent. Gaps: the invariant test does not currently enforce the simple-vs-complex mapping shape (`knowledge/{kp}.md` for simple, `knowledge/{kp}/index.md` for complex) — a retrofit could accidentally point a complex-KP link to the simple form and still pass. Minor.

## Security Review

Retrofit is pure path-rewrite in markdown; no executable surface introduced. The 2 manually-fixed artifacts (`x-story-plan`, `x-arch-plan` `.mdreferences/` malformed strings) are the expected failure mode of any regex-driven bulk edit and were correctly caught before commit. No secrets, no credentials, no path-traversal vectors — retrofit targets are all under `src/main/resources/targets/claude/skills/core/`, a repo-internal tree. The grep-based invariant test reads files via `Files.walk` inside the project root and does not follow symlinks by construction. Acceptable.

## Performance Review

`SkillConsumerRetrofitInvariantTest` walks the `skills/core/` subtree once per test method (3 methods). The tree has O(100) markdown files totalling a few hundred KB — sub-millisecond scan. No I/O amplification, no regex backtracking risk (patterns are literal path prefixes). `mvn test` reports 4162 tests with no new perf regressions observed. The 10 OLD-contract tests removed were also cheap; removal is neutral. No caching needed.

## Architecture Review

RULE-051-03 (canonical path shape) is now enforced from both ends: the producer side (KnowledgeAssembler + KnowledgeMigrationInvariantTest from story-0051-0002) and the consumer side (this story's retrofit + SkillConsumerRetrofitInvariantTest). That closes the loop. The retrofit preserves the existing skill architecture: SKILL.md files continue to be orchestration scripts that reference KPs by relative path; only the path value changed. No new coupling introduced. The transition layer in `SkillsCopyHelper` (dual-output to `.claude/skills/{kp}/SKILL.md`) is untouched — removal remains owned by story-0051-0006. Cross-layer impact is zero: domain and adapter packages are not modified.

## Clean Code Review

`SkillConsumerRetrofitInvariantTest` is compact, single-concern, uses AssertJ fluent assertions with `.as(...)` context messages, and follows the `[subject]_[scenario]_[expected]` naming convention. Constants at the top (old/new path literals). No boolean flags. No duplication with `KnowledgeMigrationInvariantTest` — the two tests assert different invariants on different trees, so co-existence is correct. The updated `LazyKpLoadingTest` / `HardeningEvalSkillTest` simply reflect the new path in their expectation strings — minimal, intent-revealing deltas. The 10 deleted tests were dead weight against the new contract; removing them is cleaner than keeping `@Disabled` markers indefinitely. One observation: the two manually-fixed `.mdreferences/` artifacts suggest the regex script could benefit from a post-run sanity grep for malformed tokens — worth noting in the story retro for future bulk retrofits.

## Findings Summary

| # | Severity | Dimension | Finding | Recommendation |
|---|---|---|---|---|
| 1 | LOW | QA | `SkillConsumerRetrofitInvariantTest` does not verify simple-vs-complex shape mapping (`.md` vs `/index.md`). | Optional: add a 4th invariant method that asserts each referenced `knowledge/X` link resolves to either a file or an `index.md`. |
| 2 | LOW | Clean Code | Bulk-regex retrofit produced 2 malformed artifacts (`.mdreferences/`) in `x-story-plan` and `x-arch-plan` — caught manually. | Document in story retro that future bulk retrofits should include a post-run sanity grep for concatenated tokens. |
| 3 | INFO | Architecture | Dual-output transition in `SkillsCopyHelper` persists (owned by story-0051-0006). | No action; tracked. |
| 4 | INFO | QA | 10 OLD-contract tests removed (not migrated). | Acceptable — tests asserted forbidden fields (RULE-051-07); replacement would be semantically empty. |

## Decision

PASS-with-observations — Retrofit is mechanically correct, invariant is automated and green, build is BUILD SUCCESS at 4162 tests with 0 failures. The 4 observations are all LOW/INFO and non-blocking; none prevent closure of the story.
