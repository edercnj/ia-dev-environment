---
name: x-review
description: "Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID or --scope reviewer1,reviewer2]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Review (Specialist Parallel Review)

## Triggers

- `/x-review` -- review current branch
- `/x-review STORY-ID` -- review specific story
- `/x-review --scope security,qa` -- run only specific reviewers

## Execution Flow (Orchestrator Pattern)

```
1. DETECT      -> Identify branch, diff, applicable engineers (inline)
2. REVIEW      -> Launch N parallel subagents, one per engineer (SINGLE message)
3. CONSOLIDATE -> Collect reports, score, summarize (inline)
4. STORY       -> If CRITICAL/MEDIUM findings: ask user, generate correction story (inline)
```

## Phase 1: Detect Context (Orchestrator — Inline)

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
| Observability | observability != none |
| DevOps | container/orchestrator/iac != none |
| API | interfaces contain protocol types |
| Event | event_driven or event interfaces |

If `--scope` provided, filter to listed engineers only.

## Phase 2: Parallel Reviews (Subagents via Task Tool)

**CRITICAL: ALL review subagents MUST be launched in a SINGLE message for true parallelism.**

Launch one `general-purpose` subagent per applicable engineer.

### Subagent: Specialist Engineer Review

**Prompt template (substitute `{ENGINEER}`, `{KP_PATHS}`, `{DIFF}`, `{STORY_ID}`, `{CHECKLIST}`):**

> You are a **{ENGINEER} Engineer** performing a specialist code review.
>
> **Step 1 — Read Knowledge Pack:**
> Read these files to understand the standards: {KP_PATHS}
>
> **Step 2 — Review the Diff:**
> Run `git diff main` and review all changes against the standards you just read.
>
> **Step 3 — Score & Report:**
> Apply the following checklist and score each item (0 = fail, 1 = partial, 2 = pass).
> **ALL items MUST score 2/2 for approval.** Any item scoring 0 or 1 blocks the review.
>
> {CHECKLIST}
>
> **Output format (strict):**
> ```
> ENGINEER: {ENGINEER}
> STORY: {STORY_ID}
> SCORE: XX/YY
> STATUS: Approved | Rejected
> ---
> PASSED:
> - [ID] Description (2/2)
> FAILED:
> - [ID] Description (0/2) — file:line — Fix: suggestion [SEVERITY]
> PARTIAL:
> - [ID] Description (1/2) — file:line — Improvement: suggestion [SEVERITY]
> ```
>
> **STATUS = Approved** only if ALL items score 2/2.
> **STATUS = Rejected** if ANY item scores 0 or 1.

### Engineer → Knowledge Pack Mapping

| Engineer | KP Paths to Read |
|----------|-----------------|
| Security | `skills/security/SKILL.md` → then read `references/application-security.md`, `references/cryptography.md` |
| QA | `skills/testing/references/testing-philosophy.md`, `skills/testing/references/testing-conventions.md` — focus on TDD Workflow, Double-Loop TDD, and TPP sections |
| Performance | `skills/resilience/references/resilience-principles.md` |
| Database | `skills/database-patterns/SKILL.md` → then read files listed in references/ |
| Observability | `skills/observability/references/observability-principles.md` |
| DevOps | `skills/infrastructure/references/infrastructure-principles.md` |
| API | `skills/api-design/references/api-design-principles.md` + relevant protocol ref from `skills/protocols/references/` |
| Event | `skills/protocols/references/event-driven-conventions.md` |

### Engineer Checklists (include in subagent prompt)

**Security (10 items, /20):** Input validation, output encoding, authentication checks, authorization checks, sensitive data masking, error handling (no stack traces), cryptography usage, dependency vulnerabilities, CORS/CSP headers, audit logging.

**QA (18 items, /36):** Test exists for each AC, line coverage ≥95%, branch coverage ≥90%, test naming convention, AAA pattern, parametrized tests for data-driven, exception paths tested, no test interdependency, fixtures centralized, unique test data, edge cases, integration tests for DB/API, commits show test-first pattern, explicit refactoring after green, tests follow TPP progression, no test written after implementation, acceptance tests validate E2E behavior, TDD coverage thresholds maintained.

**Performance (13 items, /26):** No N+1 queries, connection pool sized, async where applicable, pagination on collections, caching strategy, no unbounded lists, timeout on external calls, circuit breaker on external, thread safety, resource cleanup, lazy loading, batch operations, index usage.

**Database (8 items, /16):** Migration reversible, indexes for query patterns, no SELECT *, audit columns, entity lifecycle callbacks, optimistic locking, connection pool config, query performance.

**Observability (9 items, /18):** Root span per request, child spans for sub-ops, mandatory span attributes, metrics (counter+histogram+gauge), structured JSON logging, trace-log correlation, health checks (liveness+readiness+startup), no sensitive data in traces/logs, sampling configured.

**DevOps (10 items, /20):** Multi-stage Dockerfile, non-root user, health check in container, resource limits in K8s, security context, probes configured, config externalized, secrets via vault/sealed-secrets, CI pipeline passing, image scanning.

**API (8 items, /16):** RESTful URLs (nouns, versioned), correct status codes, RFC 7807 errors, pagination on lists, request validation, response DTOs (no entities), OpenAPI documented, rate limiting.

**Event (14 items, /28):** Past tense event names, CloudEvents envelope, schema registered, idempotent consumer, dead letter topic, no sensitive data in payload, event after business op, trace context in headers, consumer lag monitored, graceful shutdown, outbox or at-least-once, offset commit after processing, deserialization error handling, processing timeout.

## Phase 3: Consolidation (Orchestrator — Inline)

### 3a. Collect & Score

Parse each subagent's output. Build consolidated table:

```
+---------------+-------+--------------------+
|    Review     | Score |      Status        |
+---------------+-------+--------------------+
| Security      | XX/20 | Approved           |
| QA            | XX/36 | Rejected           |
| ...           | ...   | ...                |
+---------------+-------+--------------------+
Total: XXX/YYY (XX%)
OVERALL: APPROVED | REJECTED
```

### 3b. Issue Summary

Group all findings by severity: `CRITICAL: N | HIGH: N | MEDIUM: N | LOW: N`

```
ANY item with score < 2 → MUST be fixed before merge. No exceptions.
Approval requires ALL engineers with STATUS: Approved (every item at 2/2).
OVERALL: APPROVED only when every engineer has STATUS: Approved.
```

### 3c. Save Artifacts

Save each engineer's report to `plans/epic-XXXX/reviews/review-{engineer}-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID). Ensure directory exists: `mkdir -p plans/epic-XXXX/reviews`.

### 3d. Threat Model Update

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

## Phase 4: Story Generation for Findings (Orchestrator — Inline)

This phase runs ONLY when CRITICAL, HIGH, or MEDIUM findings exist.

### 4a. Check Findings

After consolidation, evaluate if there are findings with severity CRITICAL, HIGH, or MEDIUM.
If all findings are LOW or there are no findings, skip this phase entirely.

### 4b. Ask User Confirmation

If CRITICAL or MEDIUM findings exist, use the `AskUserQuestion` tool with the following configuration:

```
question: "Deseja criar uma história para correção dos problemas encontrados?"
header: "Story"
options:
  - label: "Sim"
    description: "Gerar uma história com os findings CRITICAL e MEDIUM como critérios de aceite"
  - label: "Não"
    description: "Apenas manter o relatório de review sem gerar história"
multiSelect: false
```

If the user selects **"Não"**, end the review process normally.

### 4c. Generate Correction Story

If the user selects **"Sim"**, generate a correction story following these steps:

1. **Read the story template:**
   ```
   .claude/templates/_TEMPLATE-STORY.md
   ```

2. **Build the story content** using findings as input:

   - **Story ID**: `STORY-{STORY_ID}-FIX-{NNN}` (where NNN is sequential)
   - **Title**: `Correção de findings do review — {STORY_ID}`
   - **Descrição**: Summary of what was found, grouped by engineer and severity
   - **Regras Transversais**: Reference rules violated by the findings
   - **Critérios de Aceite (Gherkin)**: Transform each CRITICAL and MEDIUM finding into a Gherkin scenario:
     ```
     Cenário: {finding description}
       DADO que o código atual {describe current violation}
       QUANDO a correção for aplicada
       ENTÃO {expected fix result}
       E o score do review para {engineer} deve melhorar
     ```
   - **Sub-tarefas**: One `[Dev]` task per CRITICAL finding, grouped `[Dev]` tasks for MEDIUM findings by engineer, one `[Test]` task to re-run `/x-review` after fixes
   - **DoD Local**: All CRITICAL findings resolved, all MEDIUM findings resolved or justified, `/x-review` re-run with no new CRITICAL findings

3. **Save the story** to `plans/epic-XXXX/reviews/correction-story-XXXX-YYYY.md`

4. **Report** to the user: story file path, number of findings converted, and suggested next step (`/x-dev-implement` or manual fix).

## Integration Notes

- Produces the SAME artifacts as Phase 3 of `x-dev-lifecycle`
- If run standalone, Phase 3 of lifecycle can be skipped if reports exist and code unchanged
- Recommended flow: `/x-review` → fix criticals → `/x-review-pr` for final holistic review
- Phase 4 integrates with `/x-story-create` format — correction stories follow the same template and can be picked up by `/x-dev-implement`
