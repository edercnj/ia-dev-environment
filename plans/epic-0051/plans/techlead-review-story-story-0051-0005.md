# Tech Lead Review — story-0051-0005

**Story:** Goldens regeneration + end-to-end migration smoke test
**Reviewed:** 2026-04-23
**Reviewer role:** Tech Lead (45-point checklist)
**Specialist review:** PASS (see [`./review-story-story-0051-0005.md`](./review-story-story-0051-0005.md))

## Checklist summary (pass/fail/N/A per dimension)

| Dimension | Pass | Notes |
|---|---|---|
| Clean Code | PASS | `KnowledgePackMigrationSmokeTest` is ~219 lines, 5 single-concern `@Test` methods, constants extracted (`KNOWLEDGE_DIR`, `FORBIDDEN_FRONTMATTER_FIELDS`, `SAMPLED_CONSUMERS`, `SAMPLED_KPS`), AssertJ `.as(...)` context messages, no boolean flags, no duplication. |
| SOLID | PASS (N/A dominant) | Smoke test; SRP honoured — one class, one concern (end-to-end migration invariant). |
| Architecture | PASS | Correctly placed under `smoke/` (not `application/assembler/`) — classifies as full-stack verification. Converges producer (story-0051-0002) + skills/core/ consumer (story-0051-0003) + rules/ consumer (story-0051-0004) onto the generated `.claude/` tree. No new layer coupling. |
| Framework conventions | PASS | JUnit 5 + AssertJ; try-with-resources on `Files.walk`; precompiled `Pattern`. Standard Maven test layout under `src/test/java/dev/iadev/smoke/`. Reuses `FrontmatterParser` utility (no duplication). |
| Tests | PASS | 4163 total, 0 failures, 0 errors, 0 skipped. Smoke in isolation: 5/5 PASS. All 5 Section-3.2 invariants from story-0051-0005 covered. |
| TDD process | PASS | Smoke landed alongside golden regeneration in one atomic story, which is the correct grain per RULE-051-08 ("goldens regenerated in a single story"). Test-first at the epic convergence point. |
| Security | PASS | Read-only over generated `.claude/` tree; `Files.walk` without symlink-follow; no user input, no injection vector. Golden regeneration is a deterministic `mvn -Dtest=GoldenFileRegenerator test` invocation with a reviewable diff. |
| Cross-file consistency | PASS | Sampled-consumer paths and sampled-KP names are extracted as constants; all 5 tests follow the same assertion pattern (walk → parse → assert via AssertJ). Golden diff uniform across `.claude/skills/`, `.claude/rules/`, `.claude/knowledge/`, `.claude/README.md`. |

## Findings carried forward from specialist review

1. **INFO / QA — sampled vs exhaustive.** **Tech Lead take:** layered defence is the right design. Exhaustive sweep is already guaranteed by `SkillConsumerRetrofitInvariantTest` (skills/core/) and `RuleKpRetrofitInvariantTest` (rules/); smoke test's job is a fast representative end-to-end proof. No action.
2. **INFO / Architecture — `mvn process-resources` prerequisite.** **Tech Lead take:** surefire lifecycle handles this in CI; operator-friendly sanity check is a nice-to-have but not a blocker. Deferred.

## Tech Lead observations

This story closes the invariant-enforcement loop of EPIC-0051. Before: story-0051-0002 proved the producer emits correctly; stories 0051-0003/0051-0004 proved the two consumer trees reference correctly. Neither class of invariant can, alone, prove the **generated `.claude/` tree** is consistent — that is the job of `KnowledgePackMigrationSmokeTest`. The 5 tests map 1:1 onto Section 3.2 of the story, and the golden regeneration is the first full sweep since story-0051-0001 landed the new `KnowledgeAssembler`, so the diff is meaningful and reviewable as a single unit per RULE-051-08. No manual fix-ups required, no malformed artifacts, no Gherkin gaps. With this story merged, the only remaining work is story-0051-0006: remove the dual-output transition from `SkillsCopyHelper` (the transition layer that emits both `.claude/skills/{kp}/SKILL.md` — now obsolete — and the new paths during the migration window). The smoke test authored here acts as the regression gate for that cleanup: if story-0051-0006 accidentally re-introduces a `skills/{kp}/SKILL.md` path, test 3 (`sampledSkillConsumers_doNotReferenceOldPaths`) and the exhaustive consumer invariants will fail loud.

## GO/NO-GO

**Decision:** GO
**Justification:** All 8 checklist dimensions pass. End-to-end migration invariant proven against the generated `.claude/` tree via 5 smoke tests, all green. Golden regeneration is a single atomic sweep per RULE-051-08 with a reviewable diff. Full `mvn test` at 4163/0/0/0. Only 2 INFO findings, no blockers. Epic convergence point met.

## Action items for subsequent stories

- [story-0051-0006] Remove the dual-output transition from `SkillsCopyHelper` (the path that still emits `.claude/skills/{kp}/SKILL.md` in parallel with the new `.claude/knowledge/` output). `KnowledgePackMigrationSmokeTest` test 3 + `SkillConsumerRetrofitInvariantTest` + `RuleKpRetrofitInvariantTest` act as the regression gates — they MUST remain green after the cleanup.
- [story-0051-0006] Consider shrinking `SkillsCopyHelper` below the class-size guideline (Rule 03: ≤ 250 lines) as part of the same cleanup, since the dual-output branch is the main contributor to its current size.
- [post-epic] Optional: promote the sampled-consumer list in `KnowledgePackMigrationSmokeTest` from 3 to N if a new consumer category is introduced downstream.
