## STORY-022 Review Report

**Reviewer:** Senior Code Reviewer
**Date:** 2026-03-11
**Commit:** `57bb478` — `fix(story-022): replace .claude/ paths with .github/ in GitHub skill templates`

### Check Results

| # | Check | Result | Notes |
|---|-------|--------|-------|
| 1 | Zero `.claude/` references in scoped templates | FAIL | 26 `.claude/` references remain across `dev/`, `review/`, `git-troubleshooting/`, `lib/`, `story/` |
| 2 | English content in story templates | PASS | All 5 story templates (`x-story-create.md`, `x-story-epic.md`, `x-story-map.md`, `x-story-epic-full.md`, `story-planning.md`) are in English; pt-BR output rules correctly preserved |
| 3 | YAML description fields in English | PASS | All 5 story template YAML frontmatter `description` fields are in English |
| 4 | KP mapping in `x-review.md` uses `.github/` paths | PASS | All 8 engineer entries point to `.github/skills/{pack}/SKILL.md`; zero `references/` subdirectory paths |
| 5 | `x-dev-lifecycle.md` uses `.github/` paths | PASS | All subagent prompt paths use `.github/skills/`; zero `.claude/` references |
| 6 | Lib templates use `.github/` paths | PASS | All 3 lib templates (`x-lib-task-decomposer.md`, `x-lib-group-verifier.md`, `x-lib-audit-rules.md`) use `.github/skills/` in Reference fields and STEP 0 paths |
| 7 | Golden file verification | PASS | `go-gin/x-story-create/SKILL.md` matches template structure exactly; `typescript-nestjs/x-dev-lifecycle/SKILL.md` has `.github/` paths and `{{LANGUAGE}}` tokens correctly present as runtime markers |
| 8 | `x-review-pr.md` uses `.github/` paths | PASS | Zero `.claude/` references; all KP paths use `.github/skills/` prefix |

### Issues Found

#### CRITICAL: 26 `.claude/` references remain in templates NOT addressed by STORY-022

The commit fixed the 7 templates explicitly listed in the story's GAP-1 table (Section 3.4). However, **7 additional templates** in the same scoped directories still contain `.claude/` references. These were not in the story's scope (Section 3.4 "Modules Affected"), but they violate the story's DoD criterion: "Zero grep results for `\.claude/skills/` in `resources/github-skills-templates/`".

**Out-of-scope templates with remaining `.claude/` references:**

| Template | `.claude/` Count | Lines |
|----------|-----------------|-------|
| `dev/x-dev-implement.md` | 7 | 40-43, 126-128 |
| `dev/layer-templates.md` | 3 | 479-481 |
| `review/x-review-api.md` | 5 | 26-27, 115-117 |
| `review/x-review-grpc.md` | 3 | 26, 125-126 |
| `review/x-review-events.md` | 3 | 26, 132-133 |
| `review/x-review-gateway.md` | 3 | 25, 69-70 |
| `git-troubleshooting/x-git-push.md` | 2 | 7, 157 |

**Analysis:** The story document (Section 3.2, GAP-1) identified "14 occurrences across 7 templates" and the fix addressed exactly those 7 templates. The remaining 26 occurrences across 7 additional templates were outside the story's audit scope. The DoD criterion "Zero grep results for `\.claude/skills/` in `resources/github-skills-templates/`" cannot be satisfied without fixing these additional files.

**Severity: CRITICAL** -- The DoD as written is not met. However, the work done is correct for the files that were in scope.

**Recommendation:** Either (a) expand the story scope to include the remaining 7 templates, or (b) create a follow-up story for the remaining templates and update the DoD to scope it to "templates listed in Section 3.4".

#### LOW: Story DoD has internal contradiction

The story's Section 3.4 explicitly lists 12 specific templates as the scope of work. However, Section 4 DoD includes "Zero grep results for `\.claude/skills/` in `resources/github-skills-templates/`" which implies ALL templates in the directory, not just the scoped ones. The broader directories contain templates outside the story scope (infrastructure, testing, knowledge-packs) that also have `.claude/` references (75 additional occurrences across those directories).

### Score

**7/8 checks passed**

Checks 2-8 all pass cleanly. Check 1 fails due to `.claude/` references remaining in templates that were not addressed by the story's commit, though they exist in the scoped directories being reviewed.

### Summary

The STORY-022 commit correctly addresses all templates explicitly listed in the story scope:
- All 7 GAP-1 templates had `.claude/` paths replaced with `.github/` paths
- All 5 story templates were translated from Portuguese to English with correct YAML frontmatter
- The `x-review.md` KP mapping table correctly uses `.github/skills/{pack}/SKILL.md` without `references/` paths
- Golden files were regenerated for all 8 profiles
- The `x-dev-lifecycle.md`, lib templates, `x-review-pr.md`, and `x-ops-troubleshoot.md` are clean

The remaining issue is that 7 templates outside the story's explicit scope still contain `.claude/` references, which contradicts the broad DoD criterion. A follow-up story or scope expansion is recommended.
