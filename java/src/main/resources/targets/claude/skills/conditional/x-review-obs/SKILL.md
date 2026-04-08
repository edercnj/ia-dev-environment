---
name: x-review-obs
description: "Observability specialist review: validates distributed tracing, metrics naming, structured logging, health checks, correlation IDs, and alerting configuration."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Observability Specialist Review

## Purpose

Review code changes for observability best practices: distributed tracing with proper span attributes, metrics naming conventions, structured logging with mandatory fields, health check implementation, correlation ID propagation, and alerting configuration.

## Activation Condition

Include this skill when `observability.tool != "none"` in the project configuration.

## When to Use

- Pre-PR quality validation for observability concerns
- Reviewing logging and tracing implementations
- Checking health check endpoints
- Validating metrics and alerting configuration

## Triggers

- `/x-review-obs 42` -- review PR #42 for observability
- `/x-review-obs src/main/java/com/example/config/` -- review specific paths
- `/x-review-obs` -- review all current observability changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| observability | `skills/observability/SKILL.md` | Tracing, metrics, logging, health checks, correlation IDs |

## Checklist (9 Items, Max Score: /18)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Distributed Tracing (OBS-01 to OBS-03)

| # | Item | Score |
|---|------|-------|
| OBS-01 | Spans created for key operations (inbound requests, outbound calls, DB queries) | /2 |
| OBS-02 | Span attributes include mandatory fields (service, operation, status) | /2 |
| OBS-03 | Trace context propagated across service boundaries (W3C Trace Context headers) | /2 |

### Structured Logging (OBS-04 to OBS-06)

| # | Item | Score |
|---|------|-------|
| OBS-04 | Logs are structured JSON with mandatory fields (timestamp, level, message, trace_id, span_id, service) | /2 |
| OBS-05 | No sensitive data in logs (PII, credentials, tokens masked or excluded) | /2 |
| OBS-06 | Log levels appropriate (DEBUG for development, INFO for operations, WARN/ERROR for issues) | /2 |

### Health Checks & Metrics (OBS-07 to OBS-09)

| # | Item | Score |
|---|------|-------|
| OBS-07 | Health check endpoints implemented (liveness, readiness, startup) | /2 |
| OBS-08 | Custom metrics follow naming convention ({service}_{subsystem}_{metric}_{unit}) | /2 |
| OBS-09 | Correlation ID generated or propagated on every inbound request | /2 |

## Workflow

### Step 1 -- Gather Context

Read the observability knowledge pack:
- `skills/observability/SKILL.md`

### Step 2 -- Identify Changed Files

Determine scope: configuration, middleware, service classes, health check endpoints.

### Step 3 -- Tracing Review

Check span creation, attributes, and context propagation.

### Step 4 -- Logging Review

Verify structured logging, mandatory fields, and sensitive data exclusion.

### Step 5 -- Health Check Review

Verify liveness, readiness, and startup probe implementations.

### Step 6 -- Metrics Review

Check custom metrics naming conventions and correlation ID propagation.

### Step 7 -- Generate Report

Produce the scored report.

## Output Format

```
ENGINEER: Observability
STORY: [story-id or change description]
SCORE: XX/18

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [OBS-XX] [Item description]

### FAILED
- [OBS-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [OBS-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No observability code found | Report INFO: no observability code discovered |
| OpenTelemetry not configured | Warn and check for alternative tracing libraries |
| Health check endpoints missing | Report as FAILED for OBS-07 |
