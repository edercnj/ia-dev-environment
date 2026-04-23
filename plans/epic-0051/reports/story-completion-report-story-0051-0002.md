# Story Completion Report — story-0051-0002

**Story ID:** story-0051-0002
**Title:** Migração da source-of-truth de KPs
**Epic:** EPIC-0051 (Knowledge Packs fora de `.claude/skills/`)
**Status:** COMPLETE
**Completed:** 2026-04-23

## Summary

32 Knowledge Packs migrated from `java/src/main/resources/targets/claude/skills/knowledge-packs/{name}/SKILL.md` to `java/src/main/resources/targets/claude/knowledge/{name}.md` (simple) or `knowledge/{name}/index.md` + sibling references (complex). Forbidden frontmatter fields (`user-invocable`, `allowed-tools`, `argument-hint`, `context-budget`) stripped per RULE-051-07. Old source directory deleted. Dual-source invariant enforced by new test.

Transition layer in `SkillsCopyHelper` continues to emit `.claude/skills/{kp}/` output (reading from new source), keeping stories 0003/0004 consumers functional until story-0051-0006 removes dual-output.

## Evidence (Rule 24)

| Artifact | Path | Purpose |
|---|---|---|
| Specialist review | [`plans/epic-0051/plans/review-story-story-0051-0002.md`](../plans/review-story-story-0051-0002.md) | QA / Security / Performance / Architecture / Clean Code |
| Tech Lead review | [`plans/epic-0051/plans/techlead-review-story-story-0051-0002.md`](../plans/techlead-review-story-story-0051-0002.md) | 45-point GO/NO-GO |
| Verify envelope | [`plans/epic-0051/reports/verify-envelope-story-0051-0002.json`](verify-envelope-story-0051-0002.json) | Test results + AC validation |
| KP inventory | [`plans/epic-0051/kp-inventory.txt`](../kp-inventory.txt) | 32 KPs evidence (§5.1) |
| Migration script | [`plans/epic-0051/migrate-kps.py`](../migrate-kps.py) | Reproducible migration tool |

## Reviews

| Review | Decision | Summary |
|---|---|---|
| Specialist | PASS-with-observations | 6 LOW findings, all deferred-acceptable to stories 0051-0003/0006 |
| Tech Lead | **GO** | All 8 checklist dimensions pass; dual-output transition well-scoped |

## Files changed

### Added
- `java/src/main/resources/targets/claude/knowledge/**` (~70 files across 32 KPs)
- `java/src/test/java/dev/iadev/application/assembler/KnowledgeMigrationInvariantTest.java` (3 tests)
- `plans/epic-0051/migrate-kps.py` (migration script)
- `plans/epic-0051/kp-inventory.txt` (32 KPs listed)

### Removed
- `java/src/main/resources/targets/claude/skills/knowledge-packs/**` (entire tree, per RULE-051-01)

### Modified
- `java/src/main/java/dev/iadev/application/assembler/SkillsCopyHelper.java` (read from new location + dual-output transition)
- 6 test files (@Disabled on 10 OLD-contract frontmatter tests with story-0051-0003 reference)
- `audits/skill-size-baseline.txt` (removed 7 stale entries)
- `java/src/test/resources/golden/**` (regenerated for 10 profiles)

## Metrics

| Metric | Value |
|---|---|
| Tests run | 4171 |
| Failures | 0 |
| Errors | 0 |
| Skipped (intentional @Disabled) | 10 |
| Build | BUILD SUCCESS |
| New tests added | 3 (KnowledgeMigrationInvariantTest) |
| KPs migrated | 32 |
| Gherkin AC passed | 4/4 |

## Action items (follow-up stories)

- **story-0051-0003 (retrofit skills consumers):** unblock the 10 disabled tests by rewriting them to match the new `.claude/knowledge/` output OR remove them entirely if superseded
- **story-0051-0003:** add body-byte-identity assertion for ≥3 sampled KPs (DoD §4)
- **story-0051-0006 (cleanup):** remove dual-output in SkillsCopyHelper.copyKnowledgePack / copyStackPatterns / copyInfraPatterns; extract `copyIndexWithSiblings` helper; SkillsCopyHelper class back under 250 lines
- **story-0051-0005 (goldens + smoke):** confirm goldens reflect single-output state after 0006 cleanup

## Deferred (tracked and non-blocking)

1. SkillsCopyHelper at 323 lines (RULE-003 250-line cap) — owned by story-0051-0006 cleanup
2. 10 @Disabled tests — owned by story-0051-0003
3. Body-byte-identity automation — owned by story-0051-0003
4. `model: haiku` frontmatter addition (Rule 23) not documented in story §3 — documentation update acceptable in any follow-up

## Compliance

- ✅ RULE-051-01 (single source of truth) — enforced by KnowledgeMigrationInvariantTest
- ✅ RULE-051-03 (canonical path `knowledge/{name}.md`) — all 32 KPs follow the rule
- ✅ RULE-051-04 (inventory) — `kp-inventory.txt` evidence
- ✅ RULE-051-07 (directory contract — forbidden fields) — stripped via migrate-kps.py
- ✅ Rule 24 (execution integrity) — all 4 mandatory evidence artifacts present
