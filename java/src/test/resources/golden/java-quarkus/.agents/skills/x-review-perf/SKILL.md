---
name: x-review-perf
description: "Performance specialist review: validates N+1 queries, connection pools, async patterns, pagination, caching, timeouts, circuit breakers, and resource cleanup."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Performance Specialist Review

## Purpose

Review code changes for performance best practices: N+1 query detection, connection pool sizing, async patterns, pagination on collections, caching strategy, timeout configuration, circuit breaker usage, thread safety, resource cleanup, lazy loading, batch operations, and index usage.

## When to Use

- Pre-PR quality validation for performance concerns
- Reviewing database query patterns
- Checking resilience patterns (timeouts, circuit breakers)
- Validating resource management

## Triggers

- `/x-review-perf 42` -- review PR #42 for performance
- `/x-review-perf src/main/java/com/example/repository/` -- review specific paths
- `/x-review-perf` -- review all current changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| resilience | `skills/resilience/SKILL.md` | Circuit breaker, rate limiting, timeout, retry, bulkhead patterns |

## Checklist (13 Items, Max Score: /26)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Query Performance (PERF-01 to PERF-02)

| # | Item | Score |
|---|------|-------|
| PERF-01 | No N+1 queries (eager fetching or batch loading where needed) | /2 |
| PERF-02 | Connection pool sized appropriately for expected load | /2 |

### Async & Concurrency (PERF-03, PERF-09)

| # | Item | Score |
|---|------|-------|
| PERF-03 | Async processing where applicable (non-blocking I/O) | /2 |
| PERF-09 | Thread safety verified (no shared mutable state without synchronization) | /2 |

### Collection & Data (PERF-04 to PERF-06)

| # | Item | Score |
|---|------|-------|
| PERF-04 | Pagination on collection endpoints (no unbounded result sets) | /2 |
| PERF-05 | Caching strategy defined for frequently accessed data | /2 |
| PERF-06 | No unbounded lists in memory (streams or pagination for large datasets) | /2 |

### Resilience (PERF-07 to PERF-08)

| # | Item | Score |
|---|------|-------|
| PERF-07 | Timeout configured on all external calls (HTTP, DB, message broker) | /2 |
| PERF-08 | Circuit breaker on external service calls | /2 |

### Resource Management (PERF-10 to PERF-13)

| # | Item | Score |
|---|------|-------|
| PERF-10 | Resource cleanup in finally/try-with-resources (connections, streams, files) | /2 |
| PERF-11 | Lazy loading for expensive initializations | /2 |
| PERF-12 | Batch operations for bulk data processing (not row-by-row) | /2 |
| PERF-13 | Database indexes used for queried columns | /2 |

## Workflow

### Step 1 -- Gather Context

Read the resilience knowledge pack:
- `skills/resilience/SKILL.md`

### Step 2 -- Identify Changed Files

Determine scope: PR diff or specified paths. Focus on repository, service, and adapter layers.

### Step 3 -- Query Analysis

Scan for N+1 patterns, missing indexes, unbounded queries.

### Step 4 -- Resilience Check

Verify timeout, circuit breaker, and retry configurations on external calls.

### Step 5 -- Resource Management Check

Verify proper resource cleanup, connection pool configuration, and batch operations.

### Step 6 -- Concurrency Check

Check for thread safety issues, shared mutable state, and proper synchronization.

### Step 7 -- Generate Report

Produce the scored report.

## Output Format

```
ENGINEER: Performance
STORY: [story-id or change description]
SCORE: XX/26

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [PERF-XX] [Item description]

### FAILED
- [PERF-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [PERF-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No repository/service code found | Report INFO: no performance-relevant code discovered |
| No external calls detected | Skip PERF-07, PERF-08 and note N/A |
| No database queries detected | Skip PERF-01, PERF-02, PERF-13 and note N/A |
