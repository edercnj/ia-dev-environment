---
name: x-review-pr
description: "Tech Lead holistic review with 45-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, TDD process, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion, Skill
argument-hint: "[PR-number or STORY-ID] [--no-auto-remediation] [--non-interactive] [--resume-review <pr>]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Review PR (Tech Lead Review)

## Purpose

Execute a senior-level holistic review with a 45-point rubric. This is the standalone version of Phase 6 from x-story-implement. The Tech Lead reviews the consolidated PR diff for cross-file consistency and overall quality.

## Triggers

- `/x-review-pr` — review current branch against main
- `/x-review-pr NNN` — review PR #NNN
- `/x-review-pr STORY-ID` — review by story ID

## Prerequisites

- Code must be committed
- Branch should have changes relative to main
- Ideally, specialist reviews (`/x-review`) have already been run

## Workflow

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

### Step 0 — Idempotency Pre-Check (RULE-002 — Artifact reuse)

Before executing the Tech Lead review, check if a report already exists and is still valid.

1. Extract story ID from argument or branch name (e.g., `story-XXXX-YYYY`)
2. Derive epic directory: `plans/epic-XXXX/reviews/`
3. Check if Tech Lead report exists:
   ```bash
   ls plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md 2>/dev/null
   ```
4. If report exists AND the branch has no new commits since last report:
   ```bash
   # Compare report mtime with latest commit date
   stat -c %Y plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md 2>/dev/null
   git log -1 --format=%ct HEAD
   ```
   - If `mtime(report) >= commit_date`: log `Reusing existing tech lead review from {date}` and skip to Step 5 (dashboard update)
   - If code changed after report: proceed with full review
5. If no report exists, proceed normally

**Idempotency Summary:**

| Aspect | Behavior |
|--------|----------|
| **Check** | Compare `mtime(report)` vs `mtime(latest commit)` |
| **Skip** | Reuse existing report when `mtime(report) >= commit_date` |
| **Override** | Proceed with full review when code changed after report |

### Step 1 — Detect Context

Determine what to review and set `[BASE_BRANCH]`:

- **PR number:** `gh pr view NNN --json title,body,baseRefName,headRefName,files`
- **STORY reference:** Find and checkout the branch
- **No argument:** Use current branch, `BASE_BRANCH=main`

Validate diff exists:
```bash
git diff [BASE_BRANCH] --stat
git diff [BASE_BRANCH] --name-only
```

### Step 2 — Gather Context

Read knowledge packs to calibrate the review:
- `knowledge/coding-standards/coding-conventions.md` — {{LANGUAGE}} naming, injection, mapper conventions
- `knowledge/architecture/architecture-principles.md` — layer boundaries, dependency direction
- `rules/05-quality-gates.md` — coverage thresholds, merge checklist
- `knowledge/testing/testing-philosophy.md` — TDD workflow, Double-Loop TDD, TPP ordering

Check for existing artifacts (extract epic ID XXXX and story sequence YYYY from story ID):
- Specialist review reports (`plans/epic-XXXX/reviews/review-*-story-XXXX-YYYY.md`)
- Implementation plan (`plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`)
- Test plan (`plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`)
- Common mistakes document

### Step 3 — Template Detection

Before executing the review, check if the Tech Lead review template exists:

```bash
test -f .claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md && echo "TL_TEMPLATE_AVAILABLE" || echo "TL_TEMPLATE_MISSING"
```

- If `TL_TEMPLATE_AVAILABLE`: Read template at `.claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md` for required output format. Follow ALL sections defined in the template. The report MUST include a standardized header with Story ID, Date, Author (Tech Lead), and Template Version (RULE-011 — Standardized artifact headers). Score MUST be in format `XX/45` with status `GO`/`NO-GO` (RULE-005 — Quality gates).
- If `TL_TEMPLATE_MISSING`: log warning `Template not found, using inline format` and use the inline format as fallback (RULE-012 — Graceful template fallback). Dashboard and remediation updates are skipped when template is absent.

> **Fallback (RULE-012 — Graceful template fallback):** When template is not available (pre-EPIC-0024 projects), the current inline format is used as fallback. Skip dashboard and remediation updates since they depend on template-based artifacts.

### Step 4 — Execute Tech Lead Review

The Tech Lead review covers:

1. List ALL modified files: `git diff [BASE_BRANCH] --name-only`
2. View FULL diff: `git diff [BASE_BRANCH]`
3. For EACH source file, read FULL content and apply 45-point checklist
4. Focus on CROSS-FILE issues (inconsistencies, cross imports, repeated patterns)
5. Compile and verify: `{{COMPILE_COMMAND}}` + `{{BUILD_COMMAND}}`
6. **Execute full test suite** (MANDATORY — EPIC-0042):
   ```bash
   {{TEST_COMMAND}}
   ```
   - If ANY test fails: record test failures in report AND set decision to **automatic NO-GO** (overrides rubric score)
   - Log each failing test name and failure reason in the report under a dedicated **Test Execution Results** section
7. **Execute coverage analysis** (MANDATORY — EPIC-0042):
   ```bash
   {{COVERAGE_COMMAND}}
   ```
   - If line coverage < 95% or branch coverage < 90%: record as CRITICAL finding in report
   - Include coverage percentages in the **Test Execution Results** section
   - **Absolute-gate note (Rule 05 RULE-005-01):** the gate fires regardless of whether the deficit was caused by this PR or was pre-existing on the base branch. The Tech Lead MUST NOT override NO-GO with a "pre-existing" justification. The only permitted escape paths are: (a) close the gap in this PR, (b) close it in a predecessor PR, or (c) merge an approved ADR that temporarily lowers the gate for a specific package (with sunset date).
8. **Execute smoke tests** (CONDITIONAL — EPIC-0042, only when `testing.smoke_tests == true`):
   ```bash
   {{SMOKE_COMMAND}}
   ```
   - If ANY smoke test fails: record as CRITICAL finding AND set decision to **automatic NO-GO**
   - If `testing.smoke_tests == false`: log `"Smoke tests skipped (testing.smoke_tests=false)"` and proceed
9. If specialist reports exist, verify CRITICAL issues were fixed

## 45-Point Rubric

| Section                  | Points | What it checks                                                      |
| ------------------------ | ------ | ------------------------------------------------------------------- |
| A. Code Hygiene          | 8      | Unused imports/vars, dead code, warnings, method signatures, magic  |
| B. Naming                | 4      | Intention-revealing, no disinformation, meaningful distinctions      |
| C. Functions             | 5      | Single responsibility, size <= 25 lines, max 4 params, no flags     |
| D. Vertical Formatting   | 4      | Blank lines between concepts, Newspaper Rule, class size <= 250     |
| E. Design                | 3      | Law of Demeter, CQS, DRY                                           |
| F. Error Handling        | 3      | Rich exceptions, no null returns, no generic catch                  |
| G. Architecture          | 5      | SRP, DIP, architecture layer boundaries (per project rules), follows plan |
| H. Framework & Infra     | 4      | DI, externalized config, native-compatible, observability           |
| I. Tests & Execution     | 6      | ALL tests pass, coverage >= 95%/90%, smoke tests pass, test quality |
| J. Security & Production | 1      | Sensitive data protected, thread-safe                               |
| K. TDD Process           | 5      | Test-first commits, Double-Loop TDD, TPP progression, atomic cycles |

## Decision Criteria

| Condition                              | Decision        |
| -------------------------------------- | --------------- |
| >= 38/45 + zero issues | GO              |
| < 38/45 OR any issue   | NO-GO           |
| ANY test failure (unit, integration, or smoke) | NO-GO (automatic, overrides score) |
| Coverage below 95% line OR 90% branch  | NO-GO (automatic, overrides score — **absolute gate per Rule 05 RULE-005-01; pre-existing deficits are NOT an excuse**) |

### Step 5 — Update Consolidated Dashboard

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
   - Check dashboard template:
     ```bash
     test -f .claude/templates/_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md && echo "DASHBOARD_TEMPLATE_AVAILABLE" || echo "DASHBOARD_TEMPLATE_MISSING"
     ```
   - If template available: read template and create `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` with only the Tech Lead Score populated (specialist scores marked as `--` / `Pending`)
   - If template missing: skip dashboard creation with warning

### Step 6 — Update Remediation Tracking

After updating the dashboard, update the remediation tracking file.

1. **Check if remediation exists:**
   ```bash
   test -f plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md && echo "REMEDIATION_EXISTS" || echo "REMEDIATION_MISSING"
   ```

2. **If remediation exists (created by x-review):**
   - Read `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`
   - For each finding in the remediation tracker:
     - If the Tech Lead confirms the finding is fixed (code reviewed and issue resolved): update status from `Open` -> `Fixed`
     - If the finding remains unfixed: keep status as `Open`
   - Add new findings identified by the Tech Lead that are not present in the existing remediation tracker, with status `Open`
   - Update the **Remediation Summary** counts to reflect new statuses

3. **If remediation does not exist (x-review was not executed):**
   - Log: `Remediation not found, creating fresh remediation with Tech Lead findings`
   - Check remediation template:
     ```bash
     test -f .claude/templates/_TEMPLATE-REVIEW-REMEDIATION.md && echo "REMEDIATION_TEMPLATE_AVAILABLE" || echo "REMEDIATION_TEMPLATE_MISSING"
     ```
   - If template available: read template and create `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md` with only findings from the Tech Lead review (all as `Open`)
   - If template missing: skip remediation creation with warning

### Step 7 — Process Result

```
============================================================
 TECH LEAD REVIEW — [STORY_ID]
============================================================
 Decision:  GO | NO-GO
 Score:     XX/45 (GO >= 38)
 Critical:  N issues
 Medium:    N issues
 Low:       N issues

 Test Execution Results (EPIC-0042):
 Test Suite:    PASS (XXX tests, X failures)
 Coverage:      XX% line, XX% branch
 Smoke Tests:   PASS/FAIL/SKIP (N tests)
------------------------------------------------------------
 Report:      plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md
 Dashboard:   plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md (updated)
 Remediation: plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md (updated)
============================================================
```

Replace "Test Suite", "Coverage", and "Smoke Tests" placeholders with actual values from Step 4 execution (Steps 4.6, 4.7, 4.8). If smoke tests were skipped (testing.smoke_tests=false), show `SKIP (0 tests)`.

### Step 8 — Handle NO-GO (Auto-Remediation — EPIC-0042)

When the review results in NO-GO, automatically dispatch remediation instead of waiting for manual input:

1. **Classify NO-GO findings:**
   - `TEST_FAILURE`: unit/integration/smoke test failures detected in Step 4.6-4.8
   - `COVERAGE_GAP`: coverage below 95% line or 90% branch detected in Step 4.7
   - `CODE_QUALITY`: rubric score below threshold (non-test issues)

2. **Auto-remediate by classification:**

   **For TEST_FAILURE:**
   Dispatch a general-purpose agent to fix failing tests:
   ```
   Agent(
     subagent_type: "general-purpose",
     description: "Fix failing tests for NO-GO remediation",
     prompt: "Read the failing test output from the review report at plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md. Identify the root cause of each failing test. Fix the IMPLEMENTATION (NOT the test) to make tests pass. Run {{TEST_COMMAND}} to verify the fix. Commit via Skill(skill: 'x-git-commit', args: '--type fix --subject \"fix failing tests from tech lead review\"')."
   )
   ```

   **For COVERAGE_GAP:**
   Dispatch a general-purpose agent to add missing test coverage:
   ```
   Agent(
     subagent_type: "general-purpose",
     description: "Add test coverage for NO-GO remediation",
     prompt: "Read the coverage report. Identify uncovered lines/branches. Write tests for the uncovered code paths following TDD discipline (test first). Run {{TEST_COMMAND}} + {{COVERAGE_COMMAND}} to verify coverage meets 95% line / 90% branch. Commit via Skill(skill: 'x-git-commit', args: '--type test --subject \"add test coverage for uncovered branches\"')."
   )
   ```

   **For CODE_QUALITY:**
   Apply fixes inline following the remediation tracking file guidance.

3. **Re-run review automatically** (max 2 cycles total):
   - After remediation agent completes, re-execute Step 4 (full review)
   - If still NO-GO after 2 cycles: proceed to Step 8.4 (Exhausted-Retry Gate)

4. **Opt-out:** Pass `--no-auto-remediation` flag to force manual mode (skips auto-remediation agents; proceeds directly to Step 8.4 on NO-GO).

#### Step 8.4 — Exhausted-Retry Gate (Rule 20 Interactive Gate)

Reached when auto-remediation cycles are exhausted (2 retries without convergence) or when `--no-auto-remediation` is set and the review returns NO-GO.

**Non-interactive path (`--non-interactive` present):**
Skip `AskUserQuestion`. Print legacy HALT text and return NO-GO:
```
REVIEW NO-GO: Auto-remediation exhausted without convergence. Remaining issues recorded in report.
Run with --resume-review <pr> to re-enter the gate interactively.
```
Exit with NO-GO. No state file written.

**Interactive path (default — `--non-interactive` absent):**

Initialize `gateAttempts = 0`.

**Gate loop:**

```
WHILE gateAttempts < 3:
  Present AskUserQuestion:
    question: "Auto-remediation exhausted after 2 retry cycles. The Tech Lead review returned NO-GO. How would you like to proceed?"
    options:
      - { header: "Proceed", label: "Continue (Recommended)", description: "Re-dispatch auto-remediation (+2 loops). If the review converges to GO, the gate closes and the skill exits normally." }
      - { header: "Fix PR", label: "Run x-pr-fix and retry", description: "Invokes x-pr-fix on the current PR; reapresents this menu on return." }
      - { header: "Abort", label: "Cancel the operation", description: "Terminates the skill with REVIEW_REMEDIATION_EXHAUSTED. No further remediation is attempted." }

  On PROCEED (slot 1):
    gateAttempts++
    Re-dispatch auto-remediation agents (same classification logic as Step 8 sub-steps 1-3, with 2 new retry cycles)
    Re-execute Step 4 (full review)
    IF review returns GO:
      Exit gate with GO — skill completes normally
    ELSE:
      IF gateAttempts >= 3:
        Emit REVIEW_FIX_LOOP_EXCEEDED and terminate (see below)
      ELSE:
        Continue loop (reapresent menu)

  On FIX-PR (slot 2):
    gateAttempts++
    Write/update state file at plans/review/<pr>/state.json (opt-in persistence):
      phase: "GATE_FIX_PR"
      lastPhaseCompletedAt: <ISO-8601 UTC now>
      lastGateDecision: "FIX_PR"
      fixAttempts: [... previous ..., { at: <now>, delegateSkill: "x-pr-fix", prNumber: <PR>, outcome: "pending" }]
      schemaVersion: "1.0"
    Invoke x-pr-fix via Rule 13 Pattern 1 INLINE-SKILL:

        Skill(skill: "x-pr-fix", args: "<PR>")

    Update last fixAttempt.outcome to "applied" (or appropriate outcome)
    Update state file: lastGateDecision = "FIX_PR", lastPhaseCompletedAt = <now>
    IF gateAttempts >= 3:
      Emit REVIEW_FIX_LOOP_EXCEEDED and terminate (see below)
    ELSE:
      Continue loop (reapresent menu)

  On ABORT (slot 3):
    Emit: "Review NO-GO final: operator aborted after ${gateAttempts} remediation attempt(s) on PR ${PR}"
    Exit with code REVIEW_REMEDIATION_EXHAUSTED
```

**Guard-rail — `REVIEW_FIX_LOOP_EXCEEDED` (3 consecutive PROCEED or FIX-PR without convergence):**

When `gateAttempts >= 3` without converging to GO, terminate the gate automatically:
```
REVIEW_FIX_LOOP_EXCEEDED: Loop de fix excedeu 3 tentativas no review do PR ${PR};
gate encerrado com ABORT automático.
Retomar via --resume-review ${PR} com --non-interactive ou intervenção manual.
```
No 4th option is offered. The gate terminates immediately. The menu was presented exactly 3 times (RULE-002 invariant: total option count remains 3 at all previous presentations).

## State File (opt-in)

Written only when the operator selects **FIX-PR** (slot 2) in Step 8.4. Enables resume via `--resume-review <pr>`.

**Path:** `plans/review/<pr-number>/state.json`

**Schema (Rule 20 §State File Schema — version 1.0):**

```json
{
  "phase": "GATE_FIX_PR",
  "lastPhaseCompletedAt": "<ISO-8601 UTC>",
  "lastGateDecision": "<PROCEED|FIX_PR|ABORT|null>",
  "fixAttempts": [
    {
      "at": "<ISO-8601 UTC>",
      "delegateSkill": "x-pr-fix",
      "prNumber": 123,
      "outcome": "applied"
    }
  ],
  "schemaVersion": "1.0"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `phase` | String | Yes | Always `"GATE_FIX_PR"` for this skill |
| `lastPhaseCompletedAt` | String (ISO-8601 UTC) | Yes | Updated on each write |
| `lastGateDecision` | String \| null | Yes | One of `PROCEED`, `FIX_PR`, `ABORT`, or `null` before first interaction |
| `fixAttempts` | Array | Yes | Always present; `[]` before first fix; max 3 items |
| `schemaVersion` | String | Yes | Literal `"1.0"` |

**`fixAttempts` entry fields:** `at` (ISO-8601 UTC), `delegateSkill` (always `"x-pr-fix"`), `prNumber` (PR number), `outcome` (`applied` \| `no_comments` \| `compile_regression` \| `aborted`).

**Lifecycle:**
- Written atomically (write to `<path>.tmp`, rename) when slot 2 (FIX-PR) is selected
- Not written for PROCEED or ABORT selections
- Not written on `--non-interactive` path

**`--resume-review <pr>` flag:**

When present, reads the state file at `plans/review/<pr>/state.json` and restores `gateAttempts` from `fixAttempts.size()`. If the state file satisfies the schema (Rule 20), the gate loop resumes from the last decision point. If the state file is absent or invalid, the gate starts fresh (gateAttempts = 0) with a warning:
```
WARNING: State file not found at plans/review/<pr>/state.json. Starting gate from scratch.
```
If the state file fails schema validation, emit `GATE_SCHEMA_INVALID` with the path and the missing/malformed field name.

## Error Codes

| Code | Condition | Message |
|------|-----------|---------|
| `REVIEW_REMEDIATION_EXHAUSTED` | Operator selected ABORT in Step 8.4 gate | `"Review NO-GO final: operador abortou após ${N} tentativas de remediation no PR ${PR}"` |
| `REVIEW_FIX_LOOP_EXCEEDED` | 3 consecutive PROCEED or FIX-PR attempts without converging to GO | `"Loop de fix excedeu 3 tentativas no review do PR ${PR}; gate encerrado com ABORT automático. Retomar via --resume-review ${PR} com --non-interactive ou intervenção manual."` |
| `GATE_SCHEMA_INVALID` | State file at `plans/review/<pr>/state.json` fails Rule 20 schema validation | `"State file inválido para gate em {path}: {campo} ausente ou mal-formado"` |

## Output Artifacts

- `plans/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md` — Tech Lead review report
- `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` — Updated consolidated dashboard
- `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md` — Updated remediation tracking
- `plans/review/<pr-number>/state.json` — Gate state (opt-in, written only on FIX-PR selection)

## Error Handling

| Scenario | Action |
|----------|--------|
| No diff exists between branches | Abort with message: "No changes detected between current branch and base. Nothing to review." |
| Template `_TEMPLATE-TECH-LEAD-REVIEW.md` missing | Log warning, use inline format as fallback (RULE-012 — Graceful template fallback). Skip dashboard and remediation updates. |
| Specialist review reports not found | Proceed with Tech Lead review only; note absence in report |
| Compilation or build failure | Record failure in report, deduct points from Framework & Infra section |
| Test suite failure (unit/integration) | Automatic NO-GO regardless of rubric score; record all failing tests in report |
| Coverage below threshold (< 95% line or < 90% branch) | Automatic NO-GO; record coverage gap as CRITICAL finding |
| Smoke test failure | Automatic NO-GO; record failing smoke tests as CRITICAL finding |
| NO-GO after 2 retry cycles | Route to Step 8.4 Exhausted-Retry Gate |
| NO-GO with `--no-auto-remediation` | Route directly to Step 8.4 Exhausted-Retry Gate |
| `--non-interactive` after retry exhausted | Skip gate; emit legacy HALT text; return NO-GO |
| State file missing on `--resume-review` | Start gate fresh; emit warning |
| State file schema invalid | Emit `GATE_SCHEMA_INVALID`; start gate fresh |


## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-story-implement | called-by | Produces the same artifact as Phase 6 |
| x-review | reads | Reads specialist review reports for cross-validation |
| x-review | complements | `/x-review` = breadth (7 specialists), `/x-review-pr` = depth (1 Tech Lead) |

- Dashboard (Step 5) is **cumulative** — created by `/x-review`, updated by `/x-review-pr` (RULE-006)
- Remediation tracking (Step 6) enables FIXED status tracking after Tech Lead review
- Templates in `.claude/templates/` are copied verbatim by `PlanTemplatesAssembler` — not rendered by the engine
- Fallback (RULE-012 — Graceful template fallback): When templates are absent (pre-EPIC-0024 projects), inline format is used and dashboard/remediation updates are skipped
