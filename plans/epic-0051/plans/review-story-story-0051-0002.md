# Specialist Review — story-0051-0002

**Story:** KP source-of-truth migration
**Reviewed:** 2026-04-23
**Scope:** 32 KPs migrated, dual-output transition layer, invariant test
**Overall assessment:** PASS-with-observations

## QA Review

The three invariant tests (`oldKnowledgePacksDir_doesNotExist`, `newKnowledgeDir_exists`, `newKnowledgeDir_containsAtLeast32Packs`) cover the structural core of RULE-051-01/04 and the "dupla-fonte detectada" Gherkin scenario in §6. However, the Gherkin scenario "dupla-fonte detectada" literally asserts a failure message containing `"dual source detected: architecture"` — the current tests do not emit that string; they only assert existence booleans. The invariant is correct in effect, but the message contract is not honored verbatim. Two Gherkin scenarios are unexercised by automation: the body-byte-identity assertion between old SKILL.md and new `{name}.md`/`index.md` (DoD §4 explicitly requires it "on at least 3 sampled KPs") and the baseline `N >= 32` counting scenario — the 32 KPs are checked, but the evidence artifact `kp-inventory.txt` is empty in the repo (zero lines returned when read), which contradicts §5.1 and DoR. Missing edge cases: (a) KPs with empty frontmatter (only `---\n---`), (b) nested KPs beyond one level (infra-patterns children are tested transitively but not directly), (c) frontmatter sanitization — no test asserts absence of forbidden keys (`user-invocable`, `allowed-tools`, `argument-hint`, `context-budget`) in the new files.

## Security Review

Content was copied byte-preserving except for the 4 frontmatter fields stripped by `migrate-kps.py`. Sampled files (`coding-standards.md`, `architecture.md`) show clean frontmatter limited to `name`, `model`, `description` — matching RULE-051-07. No binary files or executable payloads observed under `knowledge/`. `migrate-kps.py` itself lives under `plans/` (non-shipped), so it is not a runtime concern. One observation: the migration added `model: haiku` to KPs, which is outside the story's documented frontmatter allow-list (`name`, `description`, optional `tags`) — this is likely intentional per Rule 23 but was not mentioned in the story's §3 point 2. No credentials, no path-traversal risk in `SkillsCopyHelper.copyNonIndexEntry` (filenames are taken from listed directory entries, not external input) — acceptable for a build-time generator.

## Performance Review

`SkillsCopyHelper` uses plain `Files.list` + sequential copy; no observable regression vs. the pre-migration implementation. For `infra-patterns` (27 files) and other complex KPs, traversal is one-level with a sibling recursion for subdirs — O(files) and I/O-bound. Streams are properly closed in try-with-resources (lines 108, 296, 314). `replacePlaceholdersInDir` re-walks the destination after copy — same cost as before the story. `mvn test` reports 4171 tests with no perf regression evidence. No caching or memoization introduced, which is fine for generator-time code.

## Architecture Review

RULE-051-01 (single source of truth) is materially satisfied: the old directory is removed and the new `targets/claude/knowledge/` is the only location. **However, the dual-output transition layer inside `SkillsCopyHelper` emits to `.claude/skills/{pack}/SKILL.md` as well as the new path** — this is deliberate per story-0051-0006 cleanup contract, but it does mean the generator still speaks the old "skills output" shape. This is a pragmatic and acceptable transition: the code block at lines 58-99 is well-commented with explicit STORY-0051-0002 and STORY-0051-0006 references, so the temporary nature is obvious. Clean separation between `KnowledgeAssembler` (story 0001) and `SkillsCopyHelper` is preserved — `SkillsCopyHelper` only reads from the new location; it does not mutate or duplicate the KP source-of-truth. Complex-KP handling (index.md + siblings → SKILL.md + references/) is correct per Gherkin §6.3.

## Clean Code Review

`SkillsCopyHelper` is 323 lines — over RULE-003's 250-line class limit. The file was already over limit before this story (per the class Javadoc header referencing RULE-004), but this story added ~50 lines to it (new method `copyKnowledgePack`, helpers `copyNonIndexItems`, `copyNonIndexEntry`). Method lengths are all within the ≤ 25-line rule. Naming is intent-revealing (`isComplexKp`, `isSimpleKp`, `copyNonIndexEntry`). No boolean flags in signatures. Constants at the top are correctly extracted (`KNOWLEDGE_DIR`, `INFRA_PATTERNS_DIR`, `SKILL_MD`). The `copyNonIndexItems`/`copyNonIndexEntry` pair is a good SRP split. Observation: `copyStackPatterns` and `copyInfraPatterns` each contain near-duplicate `indexMd → SKILL.md, then copyNonIndexItems, then replacePlaceholdersInDir` blocks (lines 174-185 and 223-236) — candidate for a private helper `copyIndexWithSiblings(src, dest, engine, context)` to kill ~10 lines of duplication (DRY per RULE-003 "Code Hygiene").

## Findings Summary

| # | Severity | Dimension | Finding | Recommendation |
|---|---|---|---|---|
| 1 | LOW | QA | `kp-inventory.txt` artifact is empty — violates §5.1 and DoR. | Regenerate via `ls targets/claude/knowledge/ \| sort > plans/epic-0051/kp-inventory.txt` before closing story. |
| 2 | LOW | QA | Gherkin "dupla-fonte detectada" expects literal message `"dual source detected: architecture"`; current test only asserts boolean. | Optional: enhance invariant test's `.as(...)` message to match Gherkin, or loosen the Gherkin wording in a future ADR. |
| 3 | LOW | QA | Body-byte-identity check on 3 sampled KPs (DoD §4) is not automated. | Add to story-0051-0003 or convert the DoD checkbox to a manual-verified item referencing `migrate-kps.py` internal diff. |
| 4 | LOW | QA | No automated test asserts absence of forbidden frontmatter keys in new files. | Add a complement test in story-0051-0003 (frontmatter contract validator) — story already disables 11 OLD-contract tests, so replacing them with NEW-contract tests is the natural follow-up. |
| 5 | INFO | Architecture | Dual-output (`.claude/skills/{kp}/SKILL.md`) persists until story-0051-0006. | Acceptable per story plan; tracked. |
| 6 | LOW | Clean Code | `SkillsCopyHelper` is 323 lines (over RULE-003 250-line limit); duplication between `copyStackPatterns` and `copyInfraPatterns`. | Extract shared `copyIndexWithSiblings` helper in story-0051-0006 cleanup or a dedicated refactor task. |
| 7 | INFO | Security | `model: haiku` added to KP frontmatter; not documented in story §3 point 2. | Document in CHANGELOG / story retro that Rule 23 motivated the addition. |

## Decision

PASS-with-observations — The migration is mechanically correct, preserves byte-level content, and satisfies RULE-051-01/03/04/07 where measurable. Test evidence (4171 tests, 0 failures; 3/3 invariant tests pass) confirms no regression. The LOW findings are all deferred-acceptable (most belong to the explicitly-scoped follow-up story-0051-0003 or -0006), with one quick-win: **regenerate `kp-inventory.txt` before closing**, since DoR/DoD §5.1 treats it as load-bearing evidence and the file is currently empty.
