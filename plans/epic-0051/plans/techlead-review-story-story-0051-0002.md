# Tech Lead Review — story-0051-0002

**Story:** KP source-of-truth migration
**Reviewed:** 2026-04-23
**Reviewer role:** Tech Lead (45-point checklist)
**Specialist review:** PASS-with-observations (see [`./review-story-story-0051-0002.md`](./review-story-story-0051-0002.md))

## Checklist summary (pass/fail/N/A per dimension)

| Dimension | Pass | Notes |
|---|---|---|
| Clean Code | PASS-with-observations | All method lengths within the 25-line cap; naming intent-revealing (`isComplexKp`, `copyNonIndexEntry`); constants extracted (`KNOWLEDGE_DIR`, `INFRA_PATTERNS_DIR`, `SKILL_MD`, `SKILLS_OUTPUT`); try-with-resources on `Files.list`. Class size 323 lines — over the 250-line RULE-003 cap; pre-existing overrun per the class Javadoc but this story added ~50 lines. Duplication between `copyStackPatterns` (lines 174-185) and `copyInfraPatterns` (lines 223-236): candidate for `copyIndexWithSiblings` helper. Deferred to story-0051-0006. |
| SOLID | PASS | SRP: `copyKnowledgePack` / `copyNonIndexItems` / `copyNonIndexEntry` cleanly split. OCP: simple-vs-complex KP branch is closed over `Files.isDirectory`/`isRegularFile`, no type-code. DIP: uses `CopyHelpers` + `TemplateEngine` abstractions (no direct framework coupling). ISP / LSP: N/A for static helper. |
| Architecture | PASS | RULE-051-01/03/04/07 materially satisfied: old `skills/knowledge-packs/` removed; new `targets/claude/knowledge/` is the single SoT; `SkillsCopyHelper` only READS from it (no mutation of SoT). Dual-output emission to `.claude/skills/{pack}/SKILL.md` is explicitly annotated as transitional (STORY-0051-0002 / STORY-0051-0006 comments at lines 58-62, 157, 212) — acceptable per epic plan, tracked. Dependency direction preserved (adapter assembler → domain port `TemplateEngine`); domain unaffected. |
| Framework conventions | PASS | JUnit 5 (`@Test`, `@DisplayName`), AssertJ fluent assertions with `.as(...)` contextual messages, standard Maven layout (`src/main/resources` / `src/test/java`). Test class < 250 lines (80 lines). No forbidden APIs introduced. |
| Tests | PASS-with-observations | 3 invariant tests cover the core RULE-051-01 dual-source check (old dir absent, new dir exists, >= 32 KPs). `mvn test`: 4171/0/10 — 0 failures, 10 intentional skips (11 OLD-contract tests disabled with story-0051-0003 reference — acceptable deferred migration). Test naming follows the `[subject]_[scenario]_[expected]` convention. Gaps (all LOW): (a) Gherkin "dupla-fonte detectada" expects literal message `"dual source detected: architecture"` — current tests assert booleans only; (b) body-byte-identity on 3 sampled KPs is NOT automated (DoD §4 deferred to story-0051-0003); (c) no test asserts absence of forbidden frontmatter keys in new files. None are blockers. |
| TDD process | PASS | Invariant test (`KnowledgeMigrationInvariantTest`) predates or co-lands with the migration in the commit chain; test-first pattern honored at the story level. Disabling 11 OLD-contract tests (with explicit story-0051-0003 reference rather than deletion) is the correct temporary-bridge pattern for a contract migration — deletion before the retrofit would have been worse. |
| Security | PASS | No credentials / secrets in scope. Frontmatter sanitization strips `user-invocable`, `allowed-tools`, `argument-hint`, `context-budget` per RULE-051-07 — verified on sampled files by the specialist. `copyNonIndexEntry` uses `Path` entries from `Files.list(src)` (directory traversal, not external input) — no path-traversal risk. `migrate-kps.py` lives under `plans/` and is not shipped. Observation: `model: haiku` was added to KP frontmatter (Rule 23 alignment) but not mentioned in story §3.2 — documentation nit, not a security issue. |
| Cross-file consistency | PASS | All new helpers in `SkillsCopyHelper` follow the same shape: `Optional<String>` / `List<String>` return, `UncheckedIOException` wrapping with a formatted contextual message (`"Failed to list directory: " + dir`, `"Failed to copy knowledge items: %s".formatted(src)`), try-with-resources on every `Files.list` stream. Error handling uniform across `listDirsSorted`, `listEntriesSorted`, `copyNonIndexItems`. |

## Findings carried forward from specialist review

1. **LOW / QA — `kp-inventory.txt` previously empty** (§5.1 DoR evidence). Per the specialist, this was remediated (the story passes the inventory-32-KPs invariant test). **Tech Lead take:** confirmed closed; no action needed.
2. **LOW / QA — Gherkin "dupla-fonte detectada" literal-message mismatch.** Boolean assertion is behaviorally correct; only the prose contract diverges. **Tech Lead take:** accept as-is or tighten the assertion description to match the Gherkin in a follow-up; not a blocker.
3. **LOW / QA — body-byte-identity on 3 sampled KPs not automated.** DoD §4 explicitly marks the item as "or diff executed by migrate-kps.py". **Tech Lead take:** defer to story-0051-0003 (frontmatter contract validator is the natural place).
4. **LOW / QA — no automated test asserts absence of forbidden frontmatter keys.** **Tech Lead take:** defer to story-0051-0003 — the 11 disabled OLD-contract tests need replacement with NEW-contract tests there anyway.
5. **INFO / Architecture — dual-output transition layer.** Correctly scoped and commented; removed in story-0051-0006. **Tech Lead take:** accept.
6. **LOW / Clean Code — `SkillsCopyHelper` at 323 lines; duplication between `copyStackPatterns` and `copyInfraPatterns`.** **Tech Lead take:** defer the extraction of `copyIndexWithSiblings` to story-0051-0006 cleanup — removing the dual-output code in that story will also shrink the class below the 250-line cap, so combining both refactors is more efficient than doing the DRY extraction here.
7. **INFO / Security — `model: haiku` added to KP frontmatter.** **Tech Lead take:** legitimate per Rule 23 tier assignment; record in CHANGELOG on the epic merge.

## Tech Lead observations

This is a mechanically correct, well-scoped migration story. The change set is almost exclusively file moves and a narrow, well-commented dual-output bridge in `SkillsCopyHelper`. The bridge is the right engineering call: it isolates the SoT migration from consumer retrofits (stories 0003-0005) so each subsequent story can land incrementally without breaking `develop`. The single most important risk — a silent dual-source state — is covered by an automated invariant test that fails the build if the old directory reappears. Test suite is green (4171 passing) and the 10 skips are intentional, referenced to a specific follow-up story.

Outstanding LOW findings are all deferred-acceptable and tracked to stories 0051-0003 (frontmatter contract + body-byte-identity automation) and 0051-0006 (class-size + dual-output cleanup). The only discretionary call is whether to tighten the invariant test's assertion message to literally match the Gherkin `"dual source detected: architecture"`; I consider this optional and not worth blocking on. The story fits the epic's overall "isolate each structural change behind a transition layer" strategy cleanly.

## GO/NO-GO

**Decision:** GO
**Justification:** All critical dimensions pass. RULE-051-01/03/04/07 invariants are automated and green. The 6 LOW findings are either already remediated (inventory) or explicitly tracked to follow-up stories (0003 / 0006) — none block merge. The transitional dual-output layer is annotated, scoped, and has a concrete removal plan.

## Action items for subsequent stories

- [story-0051-0003] Add automated frontmatter-contract validator test that asserts absence of `user-invocable`, `allowed-tools`, `argument-hint`, `context-budget` in every file under `targets/claude/knowledge/**`.
- [story-0051-0003] Automate body-byte-identity check (DoD §4) on at least 3 sampled KPs — can reuse `migrate-kps.py` diff or add a dedicated `KnowledgeBodyIdentityTest`.
- [story-0051-0003] Replace the 11 `@Disabled` OLD-KP-contract tests with NEW-contract equivalents; remove the `@Disabled` annotations in the same commit.
- [story-0051-0006] Remove the dual-output transition layer in `SkillsCopyHelper.copyKnowledgePack` (lines 58-99) and the mirrored branches in `copyStackPatterns` / `copyInfraPatterns`.
- [story-0051-0006] Extract shared `copyIndexWithSiblings(src, dest, engine, context)` helper to eliminate the duplication between `copyStackPatterns` and `copyInfraPatterns`; target class size < 250 lines per RULE-003.
- [story-0051-0006] Optional: tighten `KnowledgeMigrationInvariantTest.oldKnowledgePacksDir_doesNotExist` assertion description to include the Gherkin literal `"dual source detected: <name>"` for contract fidelity.
- [epic merge] Document in CHANGELOG that KP frontmatter gained `model: <tier>` per Rule 23 during this migration.
