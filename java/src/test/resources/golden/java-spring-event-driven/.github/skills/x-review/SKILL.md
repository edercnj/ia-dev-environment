---
name: x-review
description: >
  Parallel code review with specialist engineers (Security, QA, Performance,
  Database, Observability, DevOps, API, Event). Launches parallel subagents,
  each reading their own knowledge pack, then consolidates into a scored report.
  Use for pre-PR quality validation.
---

# Skill: Review (Specialist Parallel Review)

## Triggers

- `/x-review` -- review current branch
- `/x-review STORY-ID` -- review specific story
- `/x-review --scope security,qa` -- run only specific reviewers

## Execution Flow (Orchestrator Pattern)

```
0. PRE-CHECK   -> Idempotency: skip if reports exist and code unchanged (inline)
1. DETECT      -> Identify branch, diff, applicable engineers (inline)
2. REVIEW      -> Launch N parallel subagents, one per engineer (SINGLE message)
3. CONSOLIDATE -> Collect reports, score, dashboard, remediation (inline)
4. STORY       -> If CRITICAL/MEDIUM findings: ask user, generate correction story (inline)
```

## Phase 0: Idempotency Pre-Check (Orchestrator -- Inline)

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
   - If `mtime(report) >= commit_date`: log `Reusing existing review reports from {date}` and skip to Phase 3d (dashboard regeneration)
   - If code changed after reports: proceed with full review
5. If no reports exist, proceed normally

## Phase 1: Detect Context (Orchestrator -- Inline)

1. Extract story ID from argument or branch name
2. Get diff against main:
   ```bash
   git branch --show-current
   git diff main --stat
   git diff main --name-only
   ```
3. If no changes, abort: `No changes found relative to main.`
4. Determine applicable engineers:

**Always active:** Security, QA, Performance

**Conditional:**

| Engineer | Condition |
|----------|-----------|
| Database | database/cache != none |
| Data Modeling | database != none AND architecture in [hexagonal, ddd, cqrs] |
| Observability | observability != none |
| DevOps | container/orchestrator/iac != none |
| API | interfaces contain protocol types |
| Event | event_driven or event interfaces |

If `--scope` provided, filter to listed engineers only.

## Phase 2: Parallel Reviews (Subagents via Task Tool)

**CRITICAL: ALL review subagents MUST be launched in a SINGLE message for true parallelism.**

Launch one `general-purpose` subagent per applicable engineer.

### Template Detection (before dispatching subagents)

Before launching subagents, check if the specialist review template exists:

```bash
test -f .claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md && echo "TEMPLATE_AVAILABLE" || echo "TEMPLATE_MISSING"
```

- If `TEMPLATE_AVAILABLE`: include template reference instruction in each subagent prompt (see below)
- If `TEMPLATE_MISSING`: log warning `Template not found, using inline format` and use the inline format as fallback

### Subagent: Specialist Engineer Review

**Prompt template (substitute `{ENGINEER}`, `{KP_PATHS}`, `{DIFF}`, `{STORY_ID}`, `{CHECKLIST}`):**

> You are a **{ENGINEER} Engineer** performing a specialist code review.
>
> **Step 1 -- Read Knowledge Pack:**
> Read these files to understand the standards: {KP_PATHS}
>
> **Step 1b -- Read Output Template:**
> Read template at `.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md` for required output format.
> Follow ALL sections defined in the template. Score MUST be in format `XX/YY | Status: Approved/Rejected/Partial`.
>
> **Step 2 -- Review the Diff:**
> Run `git diff main` and review all changes against the standards you just read.
>
> **Step 3 -- Score & Report:**
> Apply the following checklist and score each item (0 = fail, 1 = partial, 2 = pass).
> **ALL items MUST score 2/2 for approval.** Any item scoring 0 or 1 blocks the review.
>
> {CHECKLIST}
>
> **Output format (strict -- use template from Step 1b if available, otherwise use inline format below):**
> ```
> ENGINEER: {ENGINEER}
> STORY: {STORY_ID}
> SCORE: XX/YY
> STATUS: Approved | Rejected | Partial
> ---
> PASSED:
> - [ID] Description (2/2)
> FAILED:
> - [ID] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]
> PARTIAL:
> - [ID] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]
> ```
>
> **STATUS = Approved** only if ALL items score 2/2.
> **STATUS = Rejected** if ANY item scores 0.
> **STATUS = Partial** if ANY item scores 1 but none scores 0.

> **Fallback (RULE-012):** When template is not available (pre-EPIC-0024 projects), the inline format above is used as fallback. The subagent prompt omits Step 1b entirely.

### Engineer -> Knowledge Pack Mapping

| Engineer | KP Paths to Read |
|----------|-----------------|
| Security | `.github/skills/security/SKILL.md` |
| QA | `.github/skills/testing/SKILL.md` -- focus on TDD Workflow, Double-Loop TDD, and TPP sections |
| Performance | `.github/skills/resilience/SKILL.md` |
| Database | `.github/skills/database-patterns/SKILL.md` |
| Data Modeling | `.github/skills/data-modeling/SKILL.md` |
| Observability | `.github/skills/observability/SKILL.md` |
| DevOps | `.github/skills/infrastructure/SKILL.md` |
| API | `.github/skills/api-design/SKILL.md` + `.github/skills/protocols/SKILL.md` |
| Event | `.github/skills/protocols/SKILL.md` |

### Engineer Checklists (include in subagent prompt)

**Security (15 items, /30):** Input validation, output encoding, authentication checks, authorization checks, sensitive data masking, error handling (no stack traces), cryptography usage, dependency vulnerabilities, CORS/CSP headers, audit logging, secret detection compliance (ref: `x-secret-scan`), container security posture (ref: `x-container-scan`), supply chain risk (ref: `x-supply-chain-audit`), hardening compliance (ref: `x-hardening-eval`), OWASP Top 10 coverage (ref: `x-owasp-scan`).

> **Items 11-15 -- Adaptive Scan Integration:** When scan results exist in `results/security/` (e.g., `x-secret-scan-*.md`, `x-container-scan-*.md`, `x-supply-chain-audit-*.md`, `x-hardening-eval-*.md`, `x-owasp-scan-*.md`), the reviewer MUST reference real findings from those files. When no scan results are present, mark items as "NOT_SCANNED" with score 0. When scanning is enabled in config but not executed, apply partial penalty (1 point instead of 0) with note "Scanning enabled but not executed". Report both legacy score (/20, items 1-10 only) and enhanced score (/30, all 15 items) for backward compatibility.

**QA (18 items, /36):** Test exists for each AC, line coverage >=95%, branch coverage >=90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.

**Performance (13 items, /26):** No N+1 queries, connection pool sized, async where applicable, pagination on collections, caching strategy, no unbounded lists, timeout on external calls, circuit breaker on external, thread safety, resource cleanup, lazy loading, batch operations, index usage.

**Database (20 items, /40):** Migration reversible, indexes for query patterns, no SELECT *, audit columns, entity lifecycle callbacks, optimistic locking, connection pool config, query performance, naming conventions compliance (tables/columns/indexes follow DB conventions), soft delete pattern (deleted_at or equivalent when applicable), temporal audit trail (created_at/updated_at on all entities), encryption-at-rest for sensitive columns (PII, credentials), FK indexing (every foreign key has corresponding index), partitioning evaluation for large tables (>1M estimated rows), connection pool monitoring metrics (pool size, wait time, timeout config), dead tuple/compaction monitoring (VACUUM for SQL, compaction for NoSQL), [Conditional: NoSQL] schema validation enforcement (JSON Schema, schema registry), [Conditional: Graph] graph traversal depth limits (unbounded query prevention), [Conditional: Time-Series] cardinality management (tag/label limits, series explosion prevention), [Conditional: Distributed/NewSQL] shard key selection review (hot spots, data distribution uniformity).

**Data Modeling (10 items, /20):** Aggregate boundaries match transactional boundaries, entity lifecycle managed (factory creation, state transitions), value objects are immutable (no setters, final fields), DDD repository pattern per aggregate root (no generic DAO), no anemic domain model (behavior lives in domain entities not services), embeddable types for multi-field value objects, event-entity consistency (domain events reflect entity state changes), bounded context isolation (no cross-context entity sharing), anti-corruption layer at bounded context boundaries, domain events aligned with DB transactions (event published after commit).

> **Activation condition:** Data Modeling specialist is activated ONLY when `database != "none"` AND `architecture` is one of `[hexagonal, ddd, cqrs]`. When conditions are not met, this specialist is skipped entirely. The specialist references the `.github/skills/data-modeling/SKILL.md` knowledge pack for detailed standards.

**Observability (9 items, /18):** Root span per request, child spans for sub-ops, mandatory span attributes, metrics (counter+histogram+gauge), structured JSON logging, trace-log correlation, health checks (liveness+readiness+startup), no sensitive data in traces/logs, sampling configured.

**DevOps (10 items, /20):** Multi-stage Dockerfile, non-root user, health check in container, resource limits in K8s, security context, probes configured, config externalized, secrets via vault/sealed-secrets, CI pipeline passing, image scanning.

**API (8 items, /16):** RESTful URLs (nouns, versioned), correct status codes, RFC 7807 errors, pagination on lists, request validation, response DTOs (no entities), OpenAPI documented, rate limiting.

**Event (14 items, /28):** Past tense event names, CloudEvents envelope, schema registered, idempotent consumer, dead letter topic, no sensitive data in payload, event after business op, trace context in headers, consumer lag monitored, graceful shutdown, outbox or at-least-once, offset commit after processing, deserialization error handling, processing timeout.

## Phase 3: Consolidation (Orchestrator -- Inline)

### 3a. Collect & Score

Parse each subagent's output. Build consolidated table:

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
Approval requires ALL engineers with STATUS: Approved (every item at 2/2).
OVERALL: APPROVED only when every engineer has STATUS: Approved.
```

### 3c. Save Individual Reports

Save each engineer's report to `plans/epic-XXXX/reviews/review-{engineer}-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID). Ensure directory exists: `mkdir -p plans/epic-XXXX/reviews`.

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
   - Tech Lead Score section: leave as placeholder `--/53 | Status: Pending` (updated by `x-review-pr`)
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

After saving review artifacts, extract security findings from the Security Engineer's report and update the project threat model incrementally.

1. **Check for security findings:** Parse the Security Engineer's report for items with severity Critical, High, or Medium. If no security findings exist, skip this step.

2. **Read or create threat model:** If `results/security/threat-model.md` exists, read it. Otherwise, create it from the template `resources/templates/_TEMPLATE-THREAT-MODEL.md`.

3. **Map findings to STRIDE categories:** Classify each security finding into one of the 6 STRIDE categories (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege) based on the nature of the threat.

4. **Apply severity-based auto-add rules:**

   | Finding Severity | Auto-Add? | Initial Status |
   |-----------------|-----------|----------------|
   | Critical | Yes | `Open` |
   | High | Yes | `Open` |
   | Medium | Yes | `Under Review` |
   | Low | No | N/A (noted in review only) |

5. **Incremental update behavior:** Append new threats to the appropriate STRIDE category table. Preserve all existing entries — never remove or overwrite. If a finding matches an existing threat by description, update the existing entry instead of duplicating.

6. **Recompute Risk Summary:** Update the severity counts table in the Risk Summary section to reflect current Open and Under Review threats.

7. **Append Change History:** Add a new row with the current date, story reference, and summary of threats added or updated.

## Phase 4: Story Generation for Findings (Orchestrator -- Inline)

This phase runs ONLY when CRITICAL, HIGH, or MEDIUM findings exist.

If CRITICAL or MEDIUM findings exist, ask the user whether to generate a correction story. If yes, transform each finding into a Gherkin scenario and save to `plans/epic-XXXX/reviews/correction-story-XXXX-YYYY.md`.

## Integration Notes

- Produces the SAME artifacts as Phase 3 of `x-dev-lifecycle`
- If run standalone, Phase 3 of lifecycle can be skipped if reports exist and code unchanged
- Recommended flow: `/x-review` -> fix criticals -> `/x-review-pr` for final holistic review
- Dashboard (Phase 3d) is **cumulative** -- created by `/x-review`, updated by `/x-review-pr` (RULE-006)
- Remediation tracking (Phase 3e) enables structured follow-up of findings across review rounds
- Templates in `.claude/templates/` are copied verbatim by `PlanTemplatesAssembler` -- not rendered by the engine
- Fallback (RULE-012): When templates are absent (pre-EPIC-0024 projects), inline format is used and dashboard/remediation are skipped

## Detailed References

For in-depth guidance on review patterns, consult:
- `.github/skills/x-review/SKILL.md`
- `.github/skills/security/SKILL.md`
- `.github/skills/testing/SKILL.md`
- `.github/skills/observability/SKILL.md`
