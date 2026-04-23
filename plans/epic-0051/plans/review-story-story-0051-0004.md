# Specialist Review — story-0051-0004

**Story:** Retrofit rules to new KP paths
**Reviewed:** 2026-04-23
**Scope:** 9 rule files retrofitted (7 core + 2 conditional); new grep-based invariant test (`RuleKpRetrofitInvariantTest`)
**Overall assessment:** PASS

## QA Review

`RuleKpRetrofitInvariantTest` mirrors the shape of `SkillConsumerRetrofitInvariantTest` from story-0051-0003 but scoped to `targets/claude/rules/`. One invariant method (`noOldKpReferences_inRules`) walks the rules tree via `Files.walk`, applies a compiled regex against the 32-element `KP_NAMES` whitelist, and asserts zero offenders. The regex `skills/(<kp>)/(SKILL\.md|references/)` captures both the primary and sub-reference legacy forms in one pass, which is tighter than the 3-method approach used for skills/core/ — acceptable here because the rules/ tree has much smaller surface (O(10) files vs O(100)). Test-count delta (4162 → 4163) is consistent with +1 test method. All 4 Gherkin scenarios from Section 6 of story-0051-0004 map to explicit test or grep evidence (baseline count, happy-path retrofit, Rule 02 preservation, Rule 13 preservation). No gaps.

## Security Review

Retrofit is pure markdown path-rewrite; no executable surface introduced. Targets are all under `src/main/resources/targets/claude/rules/`, a repo-internal tree. The grep-based invariant reads via `Files.walk` without symlink-follow. Scope strictly smaller than story-0051-0003 — no new attack surface. Rule 02 (domain template with `{PLACEHOLDER}` tokens) and Rule 13 (skill invocation protocol, which legitimately references public skill names like `x-pr-fix`) are documented as byte-identical post-retrofit, confirming the regex did not overreach. Acceptable.

## Performance Review

`RuleKpRetrofitInvariantTest` walks O(10) markdown files under `rules/` once per run, totalling a few dozen KB. Regex is a fixed alternation of 32 literal KP names — no backtracking risk. Sub-millisecond execution. `mvn test` reports BUILD SUCCESS at 4163 tests with no new perf regressions. Neutral impact.

## Architecture Review

This story extends RULE-051-03 enforcement from the skills/core/ tree (story-0051-0003) to the rules/ tree. Symmetric design: same grep-based invariant shape, same regex literal alternation pattern, same fail-loud semantics. No new architectural coupling. Rule files remain pure markdown — no import direction or layer boundary affected. The explicit exclusion of `02-domain.md` (placeholder-only) and `13-skill-invocation-protocol.md` (references public skill names, not KPs) is correctly handled by the regex narrowness (KP whitelist only). Closes the retrofit surface for RULE-051-03 across all consumer trees.

## Clean Code Review

`RuleKpRetrofitInvariantTest` is compact (~85 lines), single-concern, uses AssertJ with `.as(...)` context message, and follows `[subject]_[scenario]_[expected]` naming. `RULES_DIR`, `KP_NAMES`, and `OLD_KP_REF` extracted as private static constants. The `containsOldKpReference` helper is a small predicate, correctly swallowing IOException as `false` (fail-open for the test) since unreadable files are a separate concern from KP-reference detection. The 9 retrofitted rule files show ~10 mechanical path replacements total — genuinely small delta vs the 90+ replacements in story-0051-0003. No malformed artifacts reported (tighter regex scope than 0003).

## Findings Summary

| # | Severity | Dimension | Finding | Recommendation |
|---|---|---|---|---|
| 1 | INFO | QA | Single-method invariant (vs 3 methods in story-0051-0003) is justified by smaller surface but loses the "new-path present" positive assertion. | Optional; the migration's presence is already asserted by `KnowledgeMigrationInvariantTest` on the producer side. |
| 2 | INFO | Architecture | Rules 02, 13, 20, 21 correctly untouched — regex whitelist scope prevents overreach. | No action; documented in story Section 8. |

## Decision

PASS — Retrofit is mechanically correct, tightly scoped, and invariant-protected. Build green at 4163/0/0/0. Risk profile is lower than story-0051-0003 (fewer files, no regex-driven malformed artifacts). No blocking findings.
