# Task Plan — TASK-004

| Field | Value |
|-------|-------|
| Task ID | TASK-004 |
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Source Agent | merged(Architect, QA, Security, TechLead, PO) |
| Type | documentation |
| TDD Phase | GREEN |
| Layer | cross-cutting |
| Estimated Effort | XS |
| Date | 2026-04-13 |

## Objective

Update all `RULE-018` cross-references across `java/src/main/resources/targets/` to point to `targets/claude/rules/14-worktree-lifecycle.md`. Verify zero false positives, zero references to removed inline block, zero absolute filesystem paths.

## Implementation Guide

1. `grep -rn "RULE-018" java/src/main/resources/targets/` — enumerate all current matches with line context.
2. For each match, classify: (a) active doc → update redirect target; (b) historical/changelog/comment → leave as-is and document in PR body; (c) link to removed inline block → update.
3. Replace links pointing at `x-git-worktree/SKILL.md:49-57` (or anchor `#naming-convention-rule-018`) with the new rule file path (relative).
4. Update SKILL.md frontmatter description on line 3 of x-git-worktree if it references RULE-018 inline content.
5. Update README.md line 3 of x-git-worktree similarly.
6. Re-run grep — confirm only the new rule file path is referenced (or, for excluded contexts, documented exception).
7. Grep for absolute paths: `grep -rEn "/Users/|/home/|C:\\\\" java/src/main/resources/targets/` on changed files → 0 hits.

## Definition of Done

- [ ] All active-doc `RULE-018` references redirect to `14-worktree-lifecycle.md`
- [ ] Zero references to removed inline block (lines 49-57)
- [ ] False-positive matches (historical/comment) inspected and documented in PR body
- [ ] Zero absolute filesystem paths introduced (SEC, A05)
- [ ] Frontmatter YAML in modified files remains valid
- [ ] All relative paths verified to resolve

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-002 | Inline block must be removed before redirect targets verified |
| TASK-003 | Drift section deletion may invalidate other anchors |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| RULE-018 mention in ADR/changelog incorrectly redirected | Medium | Low | Manual classification; PR body documents each redirect |
| Relative path miscalculated for nested skill folders | Low | Low | Click-test in GitHub preview |
