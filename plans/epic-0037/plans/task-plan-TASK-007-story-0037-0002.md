# Task Plan — TASK-007 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | merged(Architect, QA, TechLead) | Type | verification | TDD Phase | VERIFY | Effort | XS |

## Objective
Regenerate golden files and run full smoke + verify suite; ensure new Operation 5 content propagates to every profile and no spurious diffs appear.

## Implementation Guide
1. Confirm clean working tree on branch.
2. `mvn process-resources` (mandatory before regen — memory feedback).
3. Run `GoldenFileRegenerator` per memory `reference_golden_regen_command`.
4. `git diff --stat src/test/resources/golden/` — review for spurious diffs.
5. Expected diffs only in `src/test/resources/golden/*/.claude/skills/x-git-worktree/SKILL.md` (Operation 5 added) and possibly `.claude/skills/x-git-worktree/SKILL.md` mirror.
6. `mvn clean verify` — exit 0.
7. Confirm smoke tests green: PlatformDirectorySmokeTest, AssemblerRegressionSmokeTest, ContentIntegritySmokeTest, FrontmatterSmokeTest, CrossProfileConsistencySmokeTest.
8. Confirm `.claude/skills/x-git-worktree/SKILL.md` byte-identical to source.

## DoD
- [ ] `mvn process-resources` run before regen
- [ ] All affected golden files regenerated; new Operation 5 present in EVERY profile
- [ ] `.claude/skills/x-git-worktree/SKILL.md` byte-identical to source
- [ ] `mvn clean verify` exit 0
- [ ] All 5 smoke tests green
- [ ] Zero spurious diffs in unrelated golden files

## Dependencies
TASK-005.
