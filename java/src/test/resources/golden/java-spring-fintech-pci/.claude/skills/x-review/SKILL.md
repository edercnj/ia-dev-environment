---
name: x-review
model: sonnet
description: "Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Invokes individual review skills in parallel via Skill tool, then consolidates into a scored report. Use for pre-PR quality validation."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, TaskCreate, TaskUpdate
argument-hint: "[STORY-ID or --scope reviewer1,reviewer2] [--no-auto-fix-story]"
context-budget: medium
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

<!-- phase-no-gate: read-only idempotency pre-check; no artifact produced -->
## Phase 0 -- Idempotency Pre-Check (Orchestrator -- Inline)

Open a phase tracker (close with `TaskUpdate(id: phase0TaskId, status: "completed")` after Step 5):

    TaskCreate(subject: "{STORY_ID} › Review › Phase 0 - Idempotency", activeForm: "Checking review idempotency")

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

<!-- phase-no-gate: read-only context detection; no artifact produced -->
## Phase 1 -- Detect Context (Orchestrator -- Inline)

Open a phase tracker (close with `TaskUpdate(id: phase1TaskId, status: "completed")` after Step 4):

    TaskCreate(subject: "{STORY_ID} › Review › Phase 1 - Detect", activeForm: "Detecting review context")

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

**CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
Do NOT expect source code, diffs, or knowledge pack content in this prompt.
Each review skill reads its own knowledge pack and runs `git diff` independently.**

**CRITICAL: ALL review skills MUST be invoked in a SINGLE message for true parallelism.**

For each applicable specialist determined in Phase 1, invoke the corresponding review skill using the Skill tool. Pass the story ID as argument.

### Invocation Pattern

Each specialist is invoked via `Skill(...)` (Rule 13 — INLINE-SKILL pattern, parallel execution) AND is tracked individually via a per-specialist TaskCreate/TaskUpdate pair (Story 0033-0003 Concern #3 — Level 3 tracking in x-review). ALL Skill calls and ALL TaskCreate calls in Batch A MUST be in the SAME assistant message for true parallelism — the Claude runtime dispatches tool calls in parallel only when they are siblings in one assistant turn.

**Activation conditions — evaluate BEFORE emitting the batch. Only emit the (TaskCreate, Skill) pair for specialists whose condition is true for the current project profile. Never emit a placeholder pair for inactive specialists.**

- `x-review-qa` — always active.
- `x-review-perf` — always active.
- `x-review-db` — only if `database != none`.
- `x-review-obs` — only if `observability != none`.
- `x-review-devops` — only if `container != none`.
- `x-review-data-modeling` — only if `database != none` AND `architecture` is one of `[hexagonal, ddd, cqrs]`.
- `x-review-security` — only if security frameworks are configured.
- `x-review-api` — only if a REST interface is present.
- `x-review-events` — only if event interfaces are present.

Progress is surfaced by the `TaskCreate`/`TaskUpdate` pairs emitted in
Batches A/B below — the earlier duplicate `TodoWrite(...)` block was
removed in EPIC-0055 (story-0055-0006) because it competed with the
canonical task hierarchy defined by Rule 25. Only emit the (TaskCreate,
Skill) pair for specialists whose activation condition is true (Phase 1).

**PRE gate (Rule 25 Invariant 4).** Before Batch A, verify Phase 1
detected a valid diff and the story context is consistent:

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-review --phase Phase-2-SpecialistReviews")

On gate exit 12, abort with a clear error — a failed PRE gate indicates
a stale execution-state.json or predecessor phase that must be resolved.

**Batch A — First assistant message (all TaskCreate + all Skill calls as sibling tool calls):**

Each `subject` follows the Rule 25 §3 canonical regex with the `›`
(U+203A) separator — `{STORY_ID} › Review › {Specialist}`:

    TaskCreate(subject: "{STORY_ID} › Review › QA",            activeForm: "Running QA review")
    TaskCreate(subject: "{STORY_ID} › Review › Performance",   activeForm: "Running performance review")
    TaskCreate(subject: "{STORY_ID} › Review › Database",      activeForm: "Running database review")
    TaskCreate(subject: "{STORY_ID} › Review › Observability", activeForm: "Running observability review")
    TaskCreate(subject: "{STORY_ID} › Review › DevOps",        activeForm: "Running DevOps review")
    TaskCreate(subject: "{STORY_ID} › Review › Data Modeling", activeForm: "Running data-modeling review")
    TaskCreate(subject: "{STORY_ID} › Review › Security",      activeForm: "Running security review")
    TaskCreate(subject: "{STORY_ID} › Review › API",           activeForm: "Running API review")
    TaskCreate(subject: "{STORY_ID} › Review › Events",        activeForm: "Running events review")

**MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24):** Each `Skill(skill: "x-review-*")` below is a tool call, not prose. Silent omission of any active specialist is a `PROTOCOL_VIOLATION` and the resulting `review-story-{STORY-ID}.md` will fail Camada 3 audit (`EIE_EVIDENCE_MISSING`):

    Skill(skill: "x-review-qa",            model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-perf",          model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-db",            model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-obs",           model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-devops",        model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-data-modeling", model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-security",      model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-api",           model: "sonnet", args: "{STORY_ID}")
    Skill(skill: "x-review-events",        model: "sonnet", args: "{STORY_ID}")

Record the returned TaskCreate integer IDs in an in-memory map `reviewTasks` indexed by specialist short name (e.g., `reviewTasks["qa"] = <id>`, `reviewTasks["perf"] = <id>`, ...). The mapping is used in Batch B below.

**Wait for all Skill calls to return.** The runtime handles this automatically — the next assistant message is only produced after every tool call in Batch A completes.

**Batch B — Second assistant message (all TaskUpdate calls as sibling tool calls):**

    TaskUpdate(id: reviewTasks["qa"],            status: "completed")
    TaskUpdate(id: reviewTasks["perf"],          status: "completed")
    TaskUpdate(id: reviewTasks["db"],            status: "completed")
    TaskUpdate(id: reviewTasks["obs"],           status: "completed")
    TaskUpdate(id: reviewTasks["devops"],        status: "completed")
    TaskUpdate(id: reviewTasks["data-modeling"], status: "completed")
    TaskUpdate(id: reviewTasks["security"],      status: "completed")
    TaskUpdate(id: reviewTasks["api"],           status: "completed")
    TaskUpdate(id: reviewTasks["events"],        status: "completed")

Only emit `TaskUpdate` for specialists that were active in Batch A. If a specialist review returned STATUS = Rejected (score 0 on critical items), the TaskUpdate still uses `status: "completed"` for UI visibility — the authoritative Pass/Fail verdict lives in the consolidated dashboard (Step 4), not in the Claude Code task list (CR-04 of EPIC-0033).

**Batch C — Wave POST gate (Rule 25 REGRA-003).** After Batch B, invoke the phase-gate skill in `--mode wave` to verify every task in `--expected-tasks` completed AND every file in `--expected-artifacts` exists:

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode wave --skill x-review --phase Phase-2-SpecialistReviews --expected-tasks {comma-separated-reviewTasks-ids} --expected-artifacts {comma-separated-report-paths}")

`--expected-tasks` = the `reviewTasks` IDs recorded in Batch A for the active specialists (same filter as Batch A/B — Rule 25 Invariant 3). `--expected-artifacts` = `plans/epic-XXXX/reviews/review-{specialist}-story-XXXX-YYYY.md` for each active specialist; reports are written in Step 3c. On gate exit 12, surface the failure and return — Phase 3 is skipped until the broken specialist is resolved.

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

<!-- phase-no-gate: consolidation aggregates Phase 2 artifacts (already validated by the Phase 2 wave gate); no additional gate needed -->
## Phase 3 -- Consolidation (Orchestrator -- Inline)

Open a phase tracker (close with `TaskUpdate(id: phase3TaskId, status: "completed")` after Step 3g):

    TaskCreate(subject: "{STORY_ID} › Review › Phase 3 - Consolidate", activeForm: "Consolidating review findings")

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

**Persistence (EPIC-0042):** Use the Write tool explicitly to save each specialist report:

    Write(file_path: "plans/epic-XXXX/reviews/review-{specialist}-story-XXXX-YYYY.md", content: "{report_content}")

Do NOT rely on generating report content in the conversation alone -- every report MUST be written to disk via the Write tool.

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
   - Tech Lead Score section: leave as placeholder `--/64 | Status: Pending` (updated by `x-review-pr`)
   - Dashboard is **cumulative** (RULE-006): if dashboard already exists, append a new round to Review History instead of overwriting

   **Persistence (EPIC-0042):** Use the Write tool explicitly to save the dashboard:

       Write(file_path: "plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md", content: "{dashboard_content}")

   Do NOT rely on generating dashboard content in the conversation alone -- the dashboard MUST be written to disk via the Write tool.

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

   **Persistence (EPIC-0042):** Use the Write tool explicitly to save the remediation file:

       Write(file_path: "plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md", content: "{remediation_content}")

   Do NOT rely on generating remediation content in the conversation alone -- the remediation file MUST be written to disk via the Write tool.

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

### 3g. Consolidated Summary Box (EPIC-0042)

After ALL specialists complete and all artifacts (reports, dashboard, remediation) are saved, emit a formatted summary to the terminal:

```
============================================================
 SPECIALIST REVIEW — [STORY_ID]
============================================================
 Overall Score:  XX/YYY (ZZ%)

 | Specialist     | Score   | Status   |
 |----------------|---------|----------|
 | QA             | XX/36   | APPROVED/REJECTED |
 | Performance    | XX/26   | APPROVED/REJECTED |
 | Security       | XX/30   | APPROVED/REJECTED |
 | Database       | XX/40   | APPROVED/REJECTED |
 | Observability  | XX/18   | APPROVED/REJECTED |
 | DevOps         | XX/20   | APPROVED/REJECTED |
 | Data Modeling  | XX/20   | APPROVED/REJECTED |
 | API            | XX/16   | APPROVED/REJECTED |

 Critical Issues: N
 Open Findings:   N
------------------------------------------------------------
 Dashboard:   plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md
 Remediation: plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md
============================================================
```

Only include rows for specialists that were active in Phase 2. Replace placeholders with actual scores and statuses from the consolidation data.

<!-- phase-no-gate: conditional correction-story generation; runs only when findings exist and is intrinsically guarded by Phase 3 consolidation result -->
## Phase 4 -- Story Generation for Findings (Orchestrator -- Inline)

Open a phase tracker only when this phase runs — see Step 4a (close with `TaskUpdate(id: phase4TaskId, status: "completed")` after Step 4c):

    TaskCreate(subject: "{STORY_ID} › Review › Phase 4 - Correction story", activeForm: "Generating correction story")

This phase runs ONLY when CRITICAL, HIGH, or MEDIUM findings exist.

### 4a. Check Findings

After consolidation, evaluate if there are findings with severity CRITICAL, HIGH, or MEDIUM.
If all findings are LOW or there are no findings, skip this phase entirely.

### 4b. Auto-Generate Correction Story (EPIC-0042)

**Default behavior (auto-execution):** When CRITICAL or HIGH findings exist, automatically
generate a correction story WITHOUT asking the user. Log:
`"Auto-generating correction story for {N} CRITICAL/HIGH findings (EPIC-0042)"`

**Exception — CRITICAL security findings:** When ANY finding with severity CRITICAL originates
from the Security specialist (`x-review-security`), pause for mandatory human confirmation
before proceeding. Use `AskUserQuestion`:

```
question: "CRITICAL security finding detected. Review the finding and confirm correction story generation."
header: "Security — Mandatory Human Review"
options:
  - label: "Generate correction story"
    description: "Proceed with auto-generating correction story including the CRITICAL security finding"
  - label: "Abort"
    description: "Do not generate correction story. Manual remediation required."
multiSelect: false
```

If "Abort", end the review process normally. Log:
`"Correction story generation aborted by user (CRITICAL security finding)"`

**Opt-out flag `--no-auto-fix-story` (EPIC-0042):** When `--no-auto-fix-story` is present,
suppress automatic correction story generation. Instead, use `AskUserQuestion` to confirm:

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

If "Nao", end the review process normally.

### 4c. Generate Correction Story

When auto-generation proceeds (default) or user selects "Sim" (with `--no-auto-fix-story`),
generate a correction story following these steps:

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

4. **Report** to the user: story file path, number of findings converted, and suggested next step (`/x-task-implement` or manual fix).

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
| `x-story-implement` | Called by (Phase 4) | Produces the same artifacts as lifecycle Phase 4 |
| `x-review-pr` | Followed by | Recommended flow: `/x-review` then fix criticals then `/x-review-pr` |
| `x-story-create` | Reads format | Correction stories (Phase 4) follow the story template |
| `x-task-implement` | Followed by | Correction stories can be picked up by `/x-task-implement` |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | Reads | Dashboard format, cumulative across rounds (RULE-006) |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | Reads | Remediation tracking format |
| `PlanTemplatesAssembler` | Depends on | Templates copied verbatim -- not rendered by the engine |
