---
name: code-review
description: >
  Runs a comprehensive code review with multiple specialist engineers
  in parallel, then consolidates results into a scored report.
---

# Code Review

Run a thorough code review for changes in **ia-dev-environment**.

## Prerequisites

- Code changes committed on a feature branch
- All tests passing locally

## Workflow

### Step 1 — Parallel Specialist Review

Use the **x-review** skill to launch parallel reviews:

```
/x-review
```

This launches specialist engineers in parallel:
- **Security Engineer** — OWASP Top 10, secrets, input validation
- **QA Engineer** — Test coverage, edge cases, test quality
- **Performance Engineer** — Latency, resource usage, scalability
- Additional specialists based on changes (Database, API, Events, DevOps)

### Step 2 — API Review (if applicable)

For REST API changes, run the dedicated API review:

```
/x-review-api
```

Validates RFC 7807 errors, pagination, versioning, OpenAPI docs, and status codes.

### Step 3 — Tech Lead Review

Run the holistic 40-point checklist review:

```
/x-review-pr
```

Produces a GO/NO-GO decision covering Clean Code, SOLID, architecture,
framework conventions, tests, security, and cross-file consistency.

### Step 4 — Fix and Re-review

Address findings by severity:
1. **CRITICAL** — Must fix before merge
2. **MEDIUM** — Should fix, may defer with justification
3. **LOW** — Optional improvements

## Agents Involved

- **tech-lead** — Holistic review and GO/NO-GO decision
- **security-engineer** — Security-focused review
- **qa-engineer** — Test quality and coverage review

## Tips

- Run x-review before x-review-pr for best results
- Fix CRITICAL findings immediately — they block merge
- The consolidated report includes scores per specialist
