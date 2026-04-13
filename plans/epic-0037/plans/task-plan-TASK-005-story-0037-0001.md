# Task Plan — TASK-005

| Field | Value |
|-------|-------|
| Task ID | TASK-005 |
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Source Agent | merged(Architect, QA, TechLead, PO) |
| Type | verification |
| TDD Phase | VERIFY |
| Layer | cross-cutting |
| Estimated Effort | S |
| Date | 2026-04-13 |

## Objective

Regenerate golden files via `mvn process-resources` + `GoldenFileRegenerator`, then run the full smoke + verify suite. Confirm new rule file appears in every profile and golden diffs match SoT changes only.

## Implementation Guide

1. Ensure clean working tree on the implementation branch (no other staged changes).
2. `mvn process-resources` (per memory `feedback_mvn_process_resources_before_regen` — this stages templates into `target/classes`; skipping causes stale output).
3. Run `GoldenFileRegenerator` per memory `reference_golden_regen_command` (canonical command in README.md ~L820).
4. `git diff --stat src/test/resources/golden/` — review for spurious diffs. Expected: new `14-worktree-lifecycle.md` in every profile golden, modified x-git-worktree SKILL.md in every profile, possibly cross-ref updates in other skills.
5. If unrelated diffs appear, investigate root cause before committing.
6. `mvn clean verify` — must exit 0.
7. Confirm individual smoke tests pass: `PlatformDirectorySmokeTest`, `AssemblerRegressionSmokeTest`, `ContentIntegritySmokeTest`, `FrontmatterSmokeTest`, `CrossProfileConsistencySmokeTest`.
8. Confirm `.claude/rules/14-worktree-lifecycle.md` is byte-identical to `targets/claude/rules/14-worktree-lifecycle.md`.

## Definition of Done

- [ ] `mvn process-resources` executed before regen
- [ ] `GoldenFileRegenerator` executed from clean working tree
- [ ] New `14-worktree-lifecycle.md` present in EVERY profile under `src/test/resources/golden/*/.claude/rules/`
- [ ] x-git-worktree golden SKILL.md reflects pointer + section deletion in EVERY profile
- [ ] `.claude/rules/14-worktree-lifecycle.md` byte-identical to source
- [ ] `mvn clean verify` green
- [ ] All 5 named smoke tests green
- [ ] Zero spurious diffs in unrelated golden files
- [ ] No unresolved Pebble placeholders introduced

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-004 | All SoT edits must be complete before regen |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Regen produces noisy diffs from stale `target/classes` | Medium | Medium | Always run `mvn process-resources` first (memory rule) |
| Profile-specific template gap surfaces only after regen | Low | Medium | Re-run regen after fix; investigate per profile |
