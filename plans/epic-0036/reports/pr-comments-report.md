# PR Review Comments — Consolidated Report

- **Epic:** EPIC-0036
- **Date:** 2026-04-11
- **PRs Analyzed:** 6 (#268, #269, #270, #271, #272, #273)
- **Total Comments:** 32 (26 inline + 6 review-level)
- **Unique Findings:** 22 (after deduplication)

## Summary

| Category | Count | % |
|----------|-------|---|
| Actionable | 22 | 100.0% |
| Suggestion | 0 | 0.0% |
| Question | 0 | 0.0% |
| Praise | 0 | 0.0% |
| Resolved | 0 | 0.0% |
| Duplicates Removed | 4 | -- |

> Note: every Copilot review comment in this epic is actionable. The 6 review-level comments are PR overviews (Copilot summaries) without findings — they are excluded from the actionable count.

## Actionable Findings

| # | PRs | File | Line | Summary | Has Suggestion | Theme |
|---|-----|------|------|---------|----------------|-------|
| F-001 | #268 | README.md | 225 | Callout says "10 category subfolders directly under .../skills/" but actual is core/{category}/ + conditional/{category}/ | No | consistency |
| F-002 | #268 | CLAUDE.md | 17 | Same callout wording mismatch as F-001 | No | consistency |
| F-003 | #268 | java/.../skills/_README-TEMPLATES.md | 8 | Document both core/{category}/ and conditional/{category}/ paths | No | consistency |
| F-004 | #268 | adr/ADR-0003-skill-taxonomy-and-naming.md | 149 | "~25 renamed skills" should be 19 to match D3 + staging doc | No | consistency |
| F-005 | #270 | java/.../SkillGroupConditions.java | 98 | Remove unused `patterns-outbox` predicate (no .md template exists) | Yes | code-quality |
| F-006 | #270 | java/.../SkillGroupConditions.java | 52 | Remove unused `setup-environment` predicate (no .md template exists) | Yes | code-quality |
| F-007 | #271 | README.md | 380 | `/x-dev-lifecycle` still referenced — replace with `/x-story-implement` | No | naming |
| F-008 | #271 | CLAUDE.md | 243 | Arch plan path mismatch (`arch-story-XXXX-YYYY.md` vs `architecture-story-XXXX-YYYY.md`) | Yes | consistency |
| F-009 | #271 | x-epic-orchestrate/README.md | 86 | Epic filename should be lowercase `epic-XXXX.md` not `EPIC-XXXX.md` | Yes | consistency |
| F-010 | #271 | x-story-implement/references/verification-phase.md | 27 | Phase model contradicts README (consolidated 3 vs separate 3-8) | Yes | documentation |
| F-011 | #271 | x-story-implement/references/scope-assessment.md | 52 | Same phase model inconsistency as F-010 | No | documentation |
| F-012 | #271 | x-task-implement/README.md | 3,8,15 | README positions skill as story-scoped but rename rationale says task-scoped | No | documentation |
| F-013 | #271 | x-test-tdd/README.md | 89 | x-task-implement described as "Story-level" — wrong scope | Yes | documentation |
| F-014 | #272 | README.md | 745 | x-test-smoke-socket: WebSocket vs tcp-custom; x-test-contract: parametrized vs consumer-driven | No | consistency |
| F-015 | #272 | (golden) x-pr-fix-epic/README.md | 39 | Findings report path inconsistent across docs | No | golden-files |
| F-016 | #272 | GitFlowCrossCuttingValidationTest.java | 262 | Nested test class `FixEpicPrComments` no longer matches renamed skill | No | naming |
| F-017 | #273 | scripts/verify-skill-renames.sh | 18 | Header claims CI wiring (.github/workflows/ci.yml) that doesn't exist | No | code-quality |
| F-018 | #273 | scripts/verify-skill-renames.sh | 100 | `PATTERN` built but never used (dead code) | Yes | code-quality |
| F-019 | #273 | scripts/verify-skill-renames.sh | 118 | `\|\| true` hides scan errors; capture exit code | Yes | code-quality |
| F-020 | #273 | scripts/verify-skill-renames.sh | 80 | ALLOWED_PATHS too broad (.github/, .claude/ excluded entirely) | No | code-quality |
| F-021 | #273 | CHANGELOG.md | 12 | Typo "Assimetry" → "Asymmetry" | Yes | typo |
| F-022 | #273 | CHANGELOG.md | 44 | Rename tables render with 2 cells instead of 3 (Old/New/Rationale) | Yes | typo |

## Suggestion Findings

(none — all comments are actionable)

## Questions Requiring Human Response

(none)

## Recurring Themes

| Theme | Count | Affected PRs | Description |
|-------|-------|--------------|-------------|
| consistency | 7 | #268, #271, #272 | Inconsistent paths/names/counts requiring standardization |
| code-quality | 6 | #270, #273 | Dead code, unused entries, error-handling weaknesses |
| documentation | 4 | #271 | Phase model and skill scope contradictions across docs |
| naming | 2 | #271, #272 | Stale references to pre-rename names |
| typo | 2 | #273 | Spelling and table-rendering bugs |
| golden-files | 1 | #272 | Golden file inconsistency (template fix + regen needed) |

## Notes

- F-007 (`x-dev-lifecycle`) is a stale reference predating EPIC-0036 (renamed under EPIC-0032). The guard script in PR #273 doesn't catch it because `x-dev-lifecycle` isn't in the forbidden list.
- F-015 affects a golden file; fix must be applied to the source template (`targets/claude/skills/core/pr/x-pr-fix-epic/`) and propagated via `GoldenFileRegenerator`.
- F-022 is a single finding covering both rename tables in CHANGELOG.md (originally 2 separate review comments but identical issue).
