---
name: x-review
description: "Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Invokes individual review skills in parallel via Skill tool, then consolidates into a scored report. Use for pre-PR quality validation."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[STORY-ID or --scope reviewer1,reviewer2]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Specialist Review (Orchestrator)

## Purpose

Perform parallel specialist code reviews across multiple engineering dimensions by delegating to individual review skills (`/x-review-qa`, `/x-review-perf`, `/x-review-db`, etc.), consolidating findings into a scored dashboard, and optionally generating correction stories for critical findings.

## When to Use

- `/x-review` -- review current branch
- `/x-review STORY-ID` -- review specific story
- `/x-review --scope security,qa` -- run only specific reviewers

## Workflow Overview

```
0. PRE-CHECK   -> Idempotency: skip if reports exist and code unchanged (inline)
1. DETECT      -> Identify branch, diff, applicable specialists (inline)
2. REVIEW      -> Invoke N review skills in parallel via Skill tool (SINGLE message)
3. CONSOLIDATE -> Collect reports, score, dashboard, remediation (inline)
4. STORY       -> If CRITICAL/MEDIUM findings: ask user, generate correction story (inline)
```

## Phase 0 -- Idempotency Pre-Check (Orchestrator -- Inline)

Before executing a review, check if reports already exist and are still valid.

1. Extract story ID from argument or branch name (e.g., `story-XXXX-YYYY`)
2. Derive epic directory: `plans/epic-XXXX/reviews/`
3. Check if report files exist:
   ```bash
   ls plans/epic-XXXX/reviews/review-*-story-XXXX-YYYY.md 2>/dev/null
   ```
4. If reports exist AND the branch has no new commits since last report:
   ```bash
   # Compare latest report mtime with latest commit date
   stat -c %Y plans/epic-XXXX/reviews/review-security-story-XXXX-YYYY.md 2>/dev/null
   git log -1 --format=%ct HEAD
   ```
   - If `mtime(report) >= commit_date`: log `Reusing existing review reports from {date}` and skip to Phase 3c (dashboard regeneration)
   - If code changed after reports: proceed with full review
5. If no reports exist, proceed normally

## Phase 1 -- Detect Context (Orchestrator -- Inline)

1. Extract story ID from argument or branch name
2. Get diff against main:
   ```bash
   git branch --show-current
   git diff main --stat
   git diff main --name-only
   ```
3. If no changes, abort: `No changes found relative to main.`
4. Determine applicable specialists using Specialist Reference Table below.

**Always active:** QA, Performance

**Conditional:** Activated only when their feature gate condition is met.

If `--scope` provided, filter to listed specialists only.

## Specialist Reference Table

| Specialist | Skill | Max Score | Condition |
|------------|-------|-----------|-----------|
| QA | `/x-review-qa` | /36 | Always |
| Performance | `/x-review-perf` | /26 | Always |
| Database | `/x-review-db` | /40 | database != none |
| Observability | `/x-review-obs` | /18 | observability != none |
| DevOps | `/x-review-devops` | /20 | container != none |
| Data Modeling | `/x-review-data-modeling` | /20 | database != none AND architecture in [hexagonal, ddd, cqrs] |
| Security | `/x-review-security` | /30 | security frameworks configured |
| API | `/x-review-api` | /16 | REST interface present |
| Event | `/x-review-events` | /28 | event-driven or event interfaces |

> Each individual skill contains its own checklist, knowledge pack references, and scoring logic. The orchestrator does NOT duplicate these -- it delegates entirely.

## Phase 2 -- Parallel Reviews (Skills via Skill Tool)

**CRITICAL: ALL review skills MUST be invoked in a SINGLE message for true parallelism.**

For each applicable specialist determined in Phase 1, invoke the corresponding review skill using the Skill tool. Pass the story ID as argument.

### Invocation Pattern

In a SINGLE message, invoke all applicable skills:

```
/x-review-qa {STORY_ID}
/x-review-perf {STORY_ID}
/x-review-db {STORY_ID}            (if database != none)
/x-review-obs {STORY_ID}           (if observability != none)
/x-review-devops {STORY_ID}        (if container != none)
/x-review-data-modeling {STORY_ID}  (if database AND hex/ddd/cqrs)
/x-review-security {STORY_ID}      (if security frameworks configured)
/x-review-api {STORY_ID}           (if REST interface)
/x-review-events {STORY_ID}        (if event interfaces)
```

Each skill produces output in the standard review format:

```
ENGINEER: {SPECIALIST}
STORY: {STORY_ID}
SCORE: XX/YY
STATUS: Approved | Rejected | Partial
---
PASSED:
- [ID] Description (2/2)
FAILED:
- [ID] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]
PARTIAL:
- [ID] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]
```

> **STATUS = Approved** only if ALL items score 2/2.
> **STATUS = Rejected** if ANY item scores 0.
> **STATUS = Partial** if ANY item scores 1 but none scores 0.

## Phase 3 -- Consolidation (Orchestrator -- Inline)

### 3a. Collect & Score

Parse each skill's output. Build consolidated table:

```
+---------------+-------+--------------------+
|    Review     | Score |      Status        |
+---------------+-------+--------------------+
| Security      | XX/30 | Approved           |
| QA            | XX/36 | Rejected           |
| ...           | ...   | ...                |
+---------------+-------+--------------------+
Total: XXX/YYY (XX%)
OVERALL: APPROVED | REJECTED
```

### 3b. Issue Summary

Group all findings by severity: `CRITICAL: N | HIGH: N | MEDIUM: N | LOW: N`

```
ANY item with score < 2 -> MUST be fixed before merge. No exceptions.
Approval requires ALL specialists with STATUS: Approved (every item at 2/2).
OVERALL: APPROVED only when every specialist has STATUS: Approved.
```

### 3c. Save Individual Reports

Save each specialist's report to `plans/epic-XXXX/reviews/review-{specialist}-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID). Ensure directory exists: `mkdir -p plans/epic-XXXX/reviews`.

### 3d. Generate Consolidated Dashboard

After saving all individual reports, generate a consolidated dashboard.

1. **Check dashboard template:**
   ```bash
   test -f .claude/templates/_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md && echo "DASHBOARD_TEMPLATE_AVAILABLE" || echo "DASHBOARD_TEMPLATE_MISSING"
   ```

2. **If template available:**
   - Read template at `.claude/templates/_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`
   - Create `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`
   - Populate with:
     - **Engineer Scores Table:** One row per specialist with Score, Max, and Status
     - **Overall Score:** Sum of all specialist scores / sum of all max scores, with percentage
     - **Overall Status:** `Approved` only if ALL specialists are Approved; `Rejected` if ANY has score 0 items; `Partial` otherwise
     - **Critical Issues Summary:** All findings with severity Critical or High from all reports
     - **Severity Distribution:** Aggregate counts across all specialists
     - **Review History:** Record as Round N with date, scores, and status
   - Tech Lead Score section: leave as placeholder `--/57 | Status: Pending` (updated by `x-review-pr`)
   - Dashboard is **cumulative** (RULE-006): if dashboard already exists, append a new round to Review History instead of overwriting

3. **If template missing:**
   - Log warning: `Dashboard template not found, skipping dashboard generation`
   - Continue to next phase without generating dashboard

### 3e. Generate Remediation Tracking

After generating the dashboard, create a remediation tracking file.

1. **Check remediation template:**
   ```bash
   test -f .claude/templates/_TEMPLATE-REVIEW-REMEDIATION.md && echo "REMEDIATION_TEMPLATE_AVAILABLE" || echo "REMEDIATION_TEMPLATE_MISSING"
   ```

2. **If template available:**
   - Read template at `.claude/templates/_TEMPLATE-REVIEW-REMEDIATION.md`
   - Create `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`
   - **Extract findings:** Parse all individual reports for items with status FAILED or PARTIAL
   - **Populate Findings Tracker:** One row per finding with:
     - `Finding ID`: Sequential `FIND-NNN`
     - `Engineer`: Specialist who reported the finding
     - `Severity`: Critical / High / Medium / Low
     - `Description`: Finding description from the report
     - `Status`: All initialized as `Open`
     - `Fix Commit SHA`: Empty (populated after fixes)
   - **Populate Remediation Summary:** Count of findings by status (all Open initially)
   - **Header:** Include total count: `{N} findings pending remediation`

3. **If template missing:**
   - Log warning: `Remediation template not found, skipping remediation tracking generation`
   - Continue to next phase

### 3f. Threat Model Update

After saving review artifacts, extract security findings from the Security specialist's report and update the project threat model incrementally.

1. **Check for security findings:** Parse the Security specialist's report for items with severity Critical, High, or Medium. If no security findings exist, skip this step.

2. **Read or create threat model:** If `results/security/threat-model.md` exists, read it. Otherwise, create it from the template `resources/templates/_TEMPLATE-THREAT-MODEL.md`.

3. **Map findings to STRIDE categories:** Classify each security finding into one of the 6 STRIDE categories (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege) based on the nature of the threat.

4. **Apply severity-based auto-add rules:**

   | Finding Severity | Auto-Add? | Initial Status |
   |-----------------|-----------|----------------|
   | Critical | Yes | `Open` |
   | High | Yes | `Open` |
   | Medium | Yes | `Under Review` |
   | Low | No | N/A (noted in review only) |

5. **Incremental update behavior:** Append new threats to the appropriate STRIDE category table. Preserve all existing entries -- never remove or overwrite. If a finding matches an existing threat by description, update the existing entry instead of duplicating.

6. **Recompute Risk Summary:** Update the severity counts table in the Risk Summary section to reflect current Open and Under Review threats.

7. **Append Change History:** Add a new row with the current date, story reference, and summary of threats added or updated.

## Phase 4 -- Story Generation for Findings (Orchestrator -- Inline)

This phase runs ONLY when CRITICAL, HIGH, or MEDIUM findings exist.

### 4a. Check Findings

After consolidation, evaluate if there are findings with severity CRITICAL, HIGH, or MEDIUM.
If all findings are LOW or there are no findings, skip this phase entirely.

### 4b. Ask User Confirmation

If CRITICAL or MEDIUM findings exist, use the `AskUserQuestion` tool with the following configuration:

```
question: "Deseja criar uma historia para correcao dos problemas encontrados?"
header: "Story"
options:
  - label: "Sim"
    description: "Gerar uma historia com os findings CRITICAL e MEDIUM como criterios de aceite"
  - label: "Nao"
    description: "Apenas manter o relatorio de review sem gerar historia"
multiSelect: false
```

If the user selects **"Nao"**, end the review process normally.

### 4c. Generate Correction Story

If the user selects **"Sim"**, generate a correction story following these steps:

1. **Read the story template:**
   ```
   .claude/templates/_TEMPLATE-STORY.md
   ```

2. **Build the story content** using findings as input:

   - **Story ID**: `STORY-{STORY_ID}-FIX-{NNN}` (where NNN is sequential)
   - **Title**: `Correcao de findings do review -- {STORY_ID}`
   - **Descricao**: Summary of what was found, grouped by specialist and severity
   - **Regras Transversais**: Reference rules violated by the findings
   - **Criterios de Aceite (Gherkin)**: Transform each CRITICAL and MEDIUM finding into a Gherkin scenario:
     ```
     Cenario: {finding description}
       DADO que o codigo atual {describe current violation}
       QUANDO a correcao for aplicada
       ENTAO {expected fix result}
       E o score do review para {specialist} deve melhorar
     ```
   - **Sub-tarefas**: One `[Dev]` task per CRITICAL finding, grouped `[Dev]` tasks for MEDIUM findings by specialist, one `[Test]` task to re-run `/x-review` after fixes
   - **DoD Local**: All CRITICAL findings resolved, all MEDIUM findings resolved or justified, `/x-review` re-run with no new CRITICAL findings

3. **Save the story** to `plans/epic-XXXX/reviews/correction-story-XXXX-YYYY.md`

4. **Report** to the user: story file path, number of findings converted, and suggested next step (`/x-dev-implement` or manual fix).

## Error Handling

| Scenario | Action |
|----------|--------|
| No changes found relative to main | Abort with message: `No changes found relative to main.` |
| Skill returns invalid output (missing SCORE or STATUS) | Mark specialist as `FAILED`, score 0, continue with remaining specialists |
| Dashboard template not found | Log warning, skip dashboard generation, continue to next phase |
| Remediation template not found | Log warning, skip remediation tracking generation, continue |
| All specialists return FAILED | Overall status `REJECTED`, report saved with 0 scores |

## Template Fallback

Templates referenced by this skill follow RULE-012. When a template file does not exist (e.g., pre-EPIC-0024 projects), the skill degrades gracefully:

- `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` -- dashboard generation skipped
- `_TEMPLATE-REVIEW-REMEDIATION.md` -- remediation tracking skipped

When templates are absent, dashboard/remediation are skipped.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-dev-lifecycle` | Called by (Phase 4) | Produces the same artifacts as lifecycle Phase 4 |
| `x-review-pr` | Followed by | Recommended flow: `/x-review` then fix criticals then `/x-review-pr` |
| `x-story-create` | Reads format | Correction stories (Phase 4) follow the story template |
| `x-dev-implement` | Followed by | Correction stories can be picked up by `/x-dev-implement` |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | Reads | Dashboard format, cumulative across rounds (RULE-006) |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | Reads | Remediation tracking format |
| `PlanTemplatesAssembler` | Depends on | Templates copied verbatim -- not rendered by the engine |
