# Tech Lead Review — EPIC-0021

**Date:** 2026-04-06
**Branch:** feat/epic-0021-full-implementation
**Base:** main
**Reviewer:** Tech Lead (holistic)

## Decision

```
============================================================
 TECH LEAD REVIEW -- EPIC-0021
============================================================
 Decision:  NO-GO
 Score:     38/45 (GO >= 38 + zero issues)
 Critical:  0 issues
 High:      2 issues (blocks GO)
 Medium:    3 issues
 Low:       4 issues
------------------------------------------------------------
 Report: plans/epic-0021/reviews/review-tech-lead-epic-0021.md
============================================================
```

**Rationale:** Score meets the 38/45 threshold, but 2 HIGH issues block approval.
Both are fixable with targeted edits (no architectural change needed).

## Rubric Breakdown

| Section | Score | Details |
|---------|-------|---------|
| A. Code Hygiene | 6/8 | Stale placeholders in 1.7, 0.5.3 action column inconsistency |
| B. Naming | 4/4 | All clear |
| C. Functions | 4/5 | 10 flags approaches complexity limit |
| D. Vertical Formatting | 4/4 | Clean structure |
| E. Design | 3/3 | DRY respected |
| F. Error Handling | 2/3 | PR merge timeout ambiguity |
| G. Architecture | 3/5 | RULE-004 collision + missing StoryEntry schema |
| H. Framework & Infra | 3/4 | Constants not externalized |
| I. Tests | 3/3 | N/A (markdown) |
| J. Security | 1/1 | Excellent |
| K. TDD Process | 5/5 | N/A (markdown) |
| **Total** | **38/45** | |

## HIGH Issues (must fix)

### H1. RULE-004 Collision (G — Architecture)

**File:** `.claude/skills/x-dev-epic-implement/SKILL.md`
**Lines:** 49, 738, 906

RULE-004 is used for two unrelated concerns:
1. `--auto-merge` flag behavior (line 49, 738)
2. Gate Enforcement — integrity gate mandatory (line 906)

The Gate Enforcement was previously labeled RULE-006 in the original SKILL.md.
An AI agent interpreting RULE-004 would conflate auto-merge with gate enforcement.

**Fix:** Change line 906 from `(RULE-004)` to `(RULE-006)` to match the epic's
rule table where RULE-006 = "Integridade na Main".

### H2. Missing Consolidated StoryEntry Schema (G — Architecture)

**File:** `.claude/skills/x-dev-epic-implement/SKILL.md`

The pre-change SKILL.md had a consolidated StoryEntry schema table in Section 1.1
listing all fields with types, required flags, and descriptions. This was removed
during the refactoring. Fields are now scattered across:
- Section 1.5 (status, findingsCount, summary, commitSha, prUrl, prNumber)
- Section 1.6 (prMergeStatus)
- Section 1.4e (rebaseStatus, lastRebaseSha, rebaseAttempts)
- Resume Step 1 (PR_CREATED, PR_PENDING_REVIEW, PR_MERGED)

An AI agent implementing the checkpoint engine has no single source of truth
for the JSON shape.

**Fix:** Add a consolidated StoryEntry schema table in Section 1.1 or 1.6,
listing ALL fields: status, phase, retries, commitSha, summary, findingsCount,
prUrl, prNumber, prMergeStatus, rebaseStatus, lastRebaseSha, rebaseAttempts,
reviewsExecuted, reviewScores, coverageLine, coverageBranch, tddCycles, duration.

## MEDIUM Issues (should fix)

### M1. Phase 0.5 Intro Contradicts Advisory Default

**File:** `.claude/skills/x-dev-epic-implement/SKILL.md`, lines 203-206

The introductory paragraph states: "Stories with high code overlap are demoted to
sequential execution within phase N" — this describes STRICT mode. The default is
advisory (non-blocking). An AI agent reading only the intro would implement strict.

**Fix:** Rewrite intro: "the orchestrator performs a pre-flight analysis to detect
file-level overlaps between stories in the same phase. By default (advisory mode),
warnings are emitted but all stories execute in parallel. With `--strict-overlap`,
stories with high overlap are demoted to sequential execution."

### M2. O(N^2) Rebase Amplification

**File:** `.claude/skills/x-dev-epic-implement/SKILL.md`, lines 565-617

The `autoRebaseAfterMerge` function rebases ALL remaining open PRs on every merge
event without checking if a story was already rebased to the current main SHA.
For N stories merging sequentially: N*(N+1)/2 total rebase operations.

**Fix:** Add guard at line 576:
```
if story.lastRebaseSha == git rev-parse origin/main: continue  // already up-to-date
```

### M3. Status Mapping Table Incomplete

**File:** `.claude/skills/x-dev-epic-implement/SKILL.md`, lines 701-710

The Status Mapping table (Section 1.6b) does not include the new PR states:
PR_CREATED, PR_PENDING_REVIEW, PR_MERGED. These states need markdown equivalents
for the story file and implementation map updates.

**Fix:** Add rows:
| PR_CREATED | PR Criado | — |
| PR_PENDING_REVIEW | Em Review | — |
| PR_MERGED | PR Merged | Done |

## LOW Issues (nice to have)

- L1. Section 1.7 Extension Points still references old `story-0005-*` placeholders
- L2. `--dry-run` output has no format specification (just one-liner description)
- L3. Constants (POLL_INTERVAL, MERGE_TIMEOUT) not externalized as flags
- L4. Section 0.5.3 action column says "Demote to sequential" without noting this applies only in strict mode

## Positive Observations

- **Context isolation (RULE-001)**: Consistently enforced across all subagent prompts
- **Git safety**: `--force-with-lease` used exclusively, no bare `--force`
- **Backward compatibility**: `--single-pr` cleanly preserves legacy flow
- **Audit trail**: Checkpoint, PR comments, pre-flight analysis all persisted
- **Phase separation**: Clean Phase 0 → 0.5 → 1 → 2 → 3 flow
- **Significant simplification**: 844 deletions vs 559 insertions — removed complexity
