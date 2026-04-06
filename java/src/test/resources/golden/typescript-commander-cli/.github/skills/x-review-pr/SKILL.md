---
name: x-review-pr
description: >
  Tech Lead holistic review with 45-point checklist covering Clean Code, SOLID,
  architecture, framework conventions, tests, TDD process, security, and cross-file
  consistency. Produces GO/NO-GO decision. Use for final review before merge.
---

# Skill: Review PR (Tech Lead Review)

## Description

Senior-level holistic review with a 45-point rubric. This is the standalone version of Phase 6 from the x-dev-lifecycle. The Tech Lead reviews the consolidated PR diff for cross-file consistency and overall quality.

## Triggers

- `/x-review-pr` -- review current branch against main
- `/x-review-pr NNN` -- review PR #NNN
- `/x-review-pr STORY-ID` -- review by story ID

## Prerequisites

- Code must be committed
- Branch should have changes relative to main
- Ideally, specialist reviews (`/x-review`) have already been run

## Execution Flow

```
0. PRE-CHECK   -> Idempotency: skip if report exists and code unchanged (inline)
1. DETECT      -> Identify branch, diff, story context (inline)
2. GATHER      -> Read KPs, check existing artifacts (inline)
3. TEMPLATE    -> Detect Tech Lead review template (inline)
4. REVIEW      -> Execute 45-point Tech Lead review (inline)
5. DASHBOARD   -> Update consolidated dashboard with Tech Lead Score (inline)
6. REMEDIATION -> Update remediation tracking with FIXED/new findings (inline)
7. RESULT      -> Process and display result (inline)
8. NO-GO       -> Handle NO-GO decision (inline)
```

## Phase 0: Idempotency Pre-Check (RULE-002)

Before executing the Tech Lead review, check if a report already exists and is still valid.

1. Extract story ID from argument or branch name (e.g., `story-XXXX-YYYY`)
2. Derive epic directory: `plans/epic-XXXX/reviews/`
3. Check if Tech Lead report exists:
   ```bash
   ls plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md 2>/dev/null
   ```
4. If report exists AND the branch has no new commits since last report:
   ```bash
   stat -c %Y plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md 2>/dev/null
   git log -1 --format=%ct HEAD
   ```
   - If `mtime(report) >= commit_date`: log `Reusing existing tech lead review from {date}` and skip to Step 5 (dashboard update)
   - If code changed after report: proceed with full review
5. If no report exists, proceed normally

## Workflow

### Step 1 -- Detect Context

Determine what to review and set `[BASE_BRANCH]`:

- **PR number:** `gh pr view NNN --json title,body,baseRefName,headRefName,files`
- **STORY reference:** Find and checkout the branch
- **No argument:** Use current branch, `BASE_BRANCH=main`

Validate diff exists:
```bash
git diff [BASE_BRANCH] --stat
git diff [BASE_BRANCH] --name-only
```

### Step 2 -- Gather Context

Read knowledge packs to calibrate the review:
- `.github/skills/coding-standards/SKILL.md` -- typescript naming, injection, mapper conventions
- `.github/skills/architecture/SKILL.md` -- layer boundaries, dependency direction
- `.github/instructions/05-quality-gates.instructions.md` -- coverage thresholds, merge checklist
- `.github/skills/testing/SKILL.md` -- TDD workflow, Double-Loop TDD, TPP ordering

Check for existing artifacts (extract epic ID XXXX and story sequence YYYY from story ID):
- Specialist review reports (`plans/epic-XXXX/reviews/review-*-story-XXXX-YYYY.md`)
- Implementation plan (`plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`)
- Test plan (`plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`)

### Step 3 -- Template Detection

Before executing the review, check if the Tech Lead review template exists:

```bash
test -f .claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md && echo "TL_TEMPLATE_AVAILABLE" || echo "TL_TEMPLATE_MISSING"
```

- If `TL_TEMPLATE_AVAILABLE`: Read template at `.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md` for required output format. Follow ALL sections defined in the template. The report MUST include a standardized header with Story ID, Date, Author (Tech Lead), and Template Version (RULE-011). Score MUST be in format `XX/45` with status `GO`/`NO-GO` (RULE-005).
- If `TL_TEMPLATE_MISSING`: log warning `Template not found, using inline format` and use the inline format as fallback (RULE-012). Dashboard and remediation updates are skipped when template is absent.

> **Fallback (RULE-012):** When template is not available (pre-EPIC-0024 projects), the current inline format is used as fallback. Skip dashboard and remediation updates since they depend on template-based artifacts.

### Step 4 -- Execute Tech Lead Review

The Tech Lead review covers:

1. List ALL modified files: `git diff [BASE_BRANCH] --name-only`
2. View FULL diff: `git diff [BASE_BRANCH]`
3. For EACH source file, read FULL content and apply 45-point checklist
4. Focus on CROSS-FILE issues (inconsistencies, cross imports, repeated patterns)
5. If specialist reports exist, verify CRITICAL issues were fixed

## 45-Point Rubric

| Section                  | Points | What it checks                                                      |
| ------------------------ | ------ | ------------------------------------------------------------------- |
| A. Code Hygiene          | 8      | Unused imports/vars, dead code, warnings, method signatures, magic  |
| B. Naming                | 4      | Intention-revealing, no disinformation, meaningful distinctions      |
| C. Functions             | 5      | Single responsibility, size <= 25 lines, max 4 params, no flags     |
| D. Vertical Formatting   | 4      | Blank lines between concepts, Newspaper Rule, class size <= 250     |
| E. Design                | 3      | Law of Demeter, CQS, DRY                                           |
| F. Error Handling        | 3      | Rich exceptions, no null returns, no generic catch                  |
| G. Architecture          | 5      | SRP, DIP, architecture layer boundaries, follows plan               |
| H. Framework & Infra     | 4      | DI, externalized config, native-compatible, observability           |
| I. Tests                 | 3      | Coverage thresholds, scenarios covered, test quality                |
| J. Security & Production | 1      | Sensitive data protected, thread-safe                               |
| K. TDD Process           | 5      | Test-first commits, Double-Loop TDD, TPP progression, atomic cycles |

## Decision Criteria

| Condition                              | Decision        |
| -------------------------------------- | --------------- |
| >= 38/45 + zero issues | GO              |
| < 38/45 OR any issue   | NO-GO           |

### Step 5 -- Update Consolidated Dashboard

After saving the Tech Lead report, update the consolidated dashboard (RULE-006).

The dashboard is **cumulative** (RULE-006): created by `/x-review` (specialist scores), updated by `/x-review-pr` (Tech Lead Score).

1. **Check if dashboard exists:**
   ```bash
   test -f plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md && echo "DASHBOARD_EXISTS" || echo "DASHBOARD_MISSING"
   ```

2. **If dashboard exists (created by x-review):**
   - Read `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`
   - Update the **Tech Lead Score** section: replace placeholder `--/45 | Status: Pending` with actual score `XX/45 | Status: GO/NO-GO`
   - Update the **Overall Score** to include Tech Lead score in the total
   - Update the **Overall Status** considering both specialist scores and Tech Lead decision: status is updated to reflect all 8 specialists + Tech Lead combined assessment
   - Append a new **Round** to the **Review History** section with date, Tech Lead score, and status
   - Preserve all existing specialist scores and previous rounds

3. **If dashboard does not exist (x-review was not executed):**
   - Log: `Dashboard not found, creating fresh dashboard`
   - Check dashboard template availability and create with only Tech Lead Score populated

### Step 6 -- Update Remediation Tracking

After updating the dashboard, update the remediation tracking file.

1. **Check if remediation exists:**
   ```bash
   test -f plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md && echo "REMEDIATION_EXISTS" || echo "REMEDIATION_MISSING"
   ```

2. **If remediation exists (created by x-review):**
   - Read `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`
   - For findings confirmed fixed by the Tech Lead: update status from `Open` -> `Fixed`
   - Add new Tech Lead findings not in existing tracker, with status `Open`
   - Update **Remediation Summary** counts

3. **If remediation does not exist (x-review was not executed):**
   - Log: `Remediation not found, creating fresh remediation with Tech Lead findings`
   - Create remediation with only Tech Lead findings (all as `Open`)

### Step 7 -- Process Result

```
============================================================
 TECH LEAD REVIEW -- [STORY_ID]
============================================================
 Decision:  GO | NO-GO
 Score:     XX/45 (GO >= 38)
 Critical:  N issues
 Medium:    N issues
 Low:       N issues
------------------------------------------------------------
 Report:      plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md
 Dashboard:   plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md (updated)
 Remediation: plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md (updated)
============================================================
```

### Step 8 -- Handle NO-GO

If NO-GO, offer options:
1. Fix critical issues now
2. View the full report
3. Skip -- handle manually

If fixing: apply corrections, commit, re-run review (max 2 cycles).

## Output Artifacts

- `plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md` -- Tech Lead review report
- `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` -- Updated consolidated dashboard
- `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md` -- Updated remediation tracking


## Integration Notes

- This skill produces the SAME artifact as Phase 6 of `x-dev-lifecycle`
- Recommended workflow: `/x-review` first (breadth), then `/x-review-pr` (depth)
- Dashboard (Step 5) is **cumulative** -- created by `/x-review`, updated by `/x-review-pr` (RULE-006)
- Remediation tracking (Step 6) enables FIXED status tracking after Tech Lead review
- Templates in `.claude/templates/` are copied verbatim by `PlanTemplatesAssembler` -- not rendered by the engine
- Fallback (RULE-012): When templates are absent (pre-EPIC-0024 projects), inline format is used and dashboard/remediation updates are skipped

## Detailed References

For in-depth guidance on the Tech Lead review process, consult:
- `.github/skills/x-review-pr/SKILL.md`
- `.github/skills/coding-standards/SKILL.md`
- `.github/skills/architecture/SKILL.md`
