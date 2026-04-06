# Specialist Review — EPIC-0021

**Date:** 2026-04-06
**Branch:** feat/epic-0021-full-implementation
**Reviewers:** Security, QA, Performance, DevOps

## Summary

| Review | Score | Status |
|--------|-------|--------|
| Security | 30/30 | Approved |
| QA | 28/36 | Approved |
| Performance | 22/26 | Approved |
| DevOps | 19/20 | Approved |
| **Total** | **99/112 (88%)** | **APPROVED** |

## Findings

### HIGH (2)

**[QA-14] RULE-004 collision**
- File: `.claude/skills/x-dev-epic-implement/SKILL.md`
- Lines: 49, 738, 906
- RULE-004 reused for `--auto-merge` flag AND Gate Enforcement. Gate Enforcement was previously RULE-006.
- Fix: Restore Gate Enforcement to RULE-006 label.

**[QA-11b] StoryEntry schema removed without replacement**
- File: `.claude/skills/x-dev-epic-implement/SKILL.md`
- The consolidated StoryEntry schema table was deleted. Fields (duration, blockedBy, rebaseStatus, etc.) are scattered across 6+ sections.
- Fix: Reintroduce consolidated StoryEntry schema table in Section 1.1 or 1.6.

### MEDIUM (2)

**[QA-11] Phase 0.5 intro contradicts advisory default**
- File: `.claude/skills/x-dev-epic-implement/SKILL.md`, line 205
- Intro describes strict-mode behavior as default, but default is advisory.
- Fix: Rewrite intro to describe advisory-by-default.

**[PERF-6] O(N^2) rebase amplification**
- File: `.claude/skills/x-dev-epic-implement/SKILL.md`, lines 565-617
- `autoRebaseAfterMerge` rebases ALL remaining PRs per merge without checking `lastRebaseSha`.
- Fix: Add guard `if story.lastRebaseSha == currentMainSha: skip`.

### LOW (4)

- [QA-7] PR merge timeout leaves timed-out story in ambiguous SUCCESS/OPEN state
- [QA-17] `--dry-run` output format specification removed (one-liner instead of template)
- [PERF-3] Sequential rebase bottleneck — could parallelize for non-overlapping stories
- [DEVOPS-7] Constants (POLL_INTERVAL, MERGE_TIMEOUT, etc.) not externalized as flags/env vars
