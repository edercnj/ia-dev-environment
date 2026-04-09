# Error Catalog — Standardized Error Classification

> **Purpose:** Provides a standardized catalog of error codes for systematic classification,
> enabling automated retry/fail/escalate decisions across all execution skills.

## How to Use

1. When a tool call or subagent fails, capture the error message
2. Match the error message against the **Detection Patterns** below (case-insensitive)
3. Apply the **Prescribed Action** for the matched code
4. If no pattern matches, classify as **PERMANENT** (conservative default)
5. Log: `"Error classified: {code} ({category}) — Action: {action}"`

## Error Categories

| Category | Description | Default Behavior |
|----------|-------------|-----------------|
| TRANSIENT | Temporary failures that may resolve on retry | Retry with exponential backoff |
| CONTEXT | Context window or output size limitations | Reduce scope and re-dispatch |
| PERMANENT | Deterministic failures that will not resolve on retry | Fail immediately with guidance |
| CIRCUIT | Repeated failures indicating systemic issues | Pause or abort execution |

---

## TRANSIENT Errors

Temporary failures caused by infrastructure load, rate limits, or network issues.
These errors are **retryable** with exponential backoff and jitter.

### ERR-TRANSIENT-001: Service Overloaded

| Field | Value |
|-------|-------|
| **Code** | `ERR-TRANSIENT-001` |
| **Category** | TRANSIENT |
| **Retryable** | Yes |
| **Max Retries** | 3 |
| **Detection Patterns** | `"overloaded"`, `"capacity"` |
| **Prescribed Action** | Retry up to 3 times with exponential backoff (1s, 2s, 4s + jitter) |

### ERR-TRANSIENT-002: Rate Limited

| Field | Value |
|-------|-------|
| **Code** | `ERR-TRANSIENT-002` |
| **Category** | TRANSIENT |
| **Retryable** | Yes |
| **Max Retries** | 3 |
| **Detection Patterns** | `"rate limit"`, `"429"` |
| **Prescribed Action** | Retry up to 3 times with exponential backoff (1s, 2s, 4s + jitter) |

### ERR-TRANSIENT-003: Timeout

| Field | Value |
|-------|-------|
| **Code** | `ERR-TRANSIENT-003` |
| **Category** | TRANSIENT |
| **Retryable** | Yes |
| **Max Retries** | 2 |
| **Detection Patterns** | `"timeout"`, `"ETIMEDOUT"` |
| **Prescribed Action** | Retry up to 2 times with exponential backoff (2s, 4s + jitter) |

### ERR-TRANSIENT-004: Server Error

| Field | Value |
|-------|-------|
| **Code** | `ERR-TRANSIENT-004` |
| **Category** | TRANSIENT |
| **Retryable** | Yes |
| **Max Retries** | 3 |
| **Detection Patterns** | `"503"`, `"504"`, `"502"` |
| **Prescribed Action** | Retry up to 3 times with exponential backoff (1s, 2s, 4s + jitter) |

---

## CONTEXT Errors

Failures caused by context window exhaustion or output size limits.
These errors are **not retryable** as-is but can be resolved by reducing scope.

### ERR-CONTEXT-001: Context Window Exhausted

| Field | Value |
|-------|-------|
| **Code** | `ERR-CONTEXT-001` |
| **Category** | CONTEXT |
| **Retryable** | No |
| **Detection Patterns** | `"context"`, `"token limit"` |
| **Prescribed Action** | Graceful degradation: reduce context by dropping non-essential references, compress instructions, or split into smaller sub-tasks |

### ERR-CONTEXT-002: Output Truncated

| Field | Value |
|-------|-------|
| **Code** | `ERR-CONTEXT-002` |
| **Category** | CONTEXT |
| **Retryable** | No |
| **Detection Patterns** | `"output too large"`, `"truncated"` |
| **Prescribed Action** | Re-dispatch with reduced scope: split the task into smaller units or request partial output |

---

## PERMANENT Errors

Deterministic failures that will not resolve on retry. Require human intervention or code fixes.

### ERR-PERM-001: Resource Not Found

| Field | Value |
|-------|-------|
| **Code** | `ERR-PERM-001` |
| **Category** | PERMANENT |
| **Retryable** | No |
| **Detection Patterns** | `"not found"`, `"no such file"` |
| **Prescribed Action** | Fail immediately with path suggestion: verify file paths, check if resource was created in a prior step |

### ERR-PERM-002: Invalid Input

| Field | Value |
|-------|-------|
| **Code** | `ERR-PERM-002` |
| **Category** | PERMANENT |
| **Retryable** | No |
| **Detection Patterns** | `"invalid"`, `"malformed"` |
| **Prescribed Action** | Fail immediately with format guidance: check input format against expected schema |

### ERR-PERM-003: Compilation Error

| Field | Value |
|-------|-------|
| **Code** | `ERR-PERM-003` |
| **Category** | PERMANENT |
| **Retryable** | No |
| **Detection Patterns** | `"compilation"`, `"compile error"` |
| **Prescribed Action** | Fail immediately with error details: include compiler output and affected file paths |

### ERR-PERM-004: Test Failure

| Field | Value |
|-------|-------|
| **Code** | `ERR-PERM-004` |
| **Category** | PERMANENT |
| **Retryable** | No |
| **Detection Patterns** | `"test failure"`, `"assertion"` |
| **Prescribed Action** | Fail immediately with test output: include failing test name, expected vs actual values |

### ERR-PERM-005: Permission Denied

| Field | Value |
|-------|-------|
| **Code** | `ERR-PERM-005` |
| **Category** | PERMANENT |
| **Retryable** | No |
| **Detection Patterns** | `"permission denied"`, `"forbidden"` |
| **Prescribed Action** | Fail immediately with access guidance: verify file permissions, check authentication tokens |

---

## CIRCUIT Errors

Systemic failure indicators that trigger execution pause or abort.
These errors are based on **failure frequency**, not individual error messages.

### ERR-CIRCUIT-001: Consecutive Failure Threshold

| Field | Value |
|-------|-------|
| **Code** | `ERR-CIRCUIT-001` |
| **Category** | CIRCUIT |
| **Retryable** | No |
| **Threshold** | 3+ consecutive failures (same story or task) |
| **Detection Pattern** | Count of consecutive failures >= 3 |
| **Prescribed Action** | Pause execution and prompt the user via `AskUserQuestion`: "3 consecutive failures detected. Continue, skip, or abort?" |

### ERR-CIRCUIT-002: Phase Failure Threshold

| Field | Value |
|-------|-------|
| **Code** | `ERR-CIRCUIT-002` |
| **Category** | CIRCUIT |
| **Retryable** | No |
| **Threshold** | 5+ total failures within a single phase |
| **Detection Pattern** | Count of total failures in current phase >= 5 |
| **Prescribed Action** | Abort the current phase. Mark remaining stories as BLOCKED. Generate phase failure report. |

---

## Classification Algorithm

```
function classifyError(errorMessage, failureCounters):
  // Step 1: Check circuit breaker thresholds first
  if failureCounters.consecutive >= 3:
    return ERR-CIRCUIT-001
  if failureCounters.phaseTotal >= 5:
    return ERR-CIRCUIT-002

  // Step 2: Match against catalog patterns (case-insensitive)
  normalizedMessage = errorMessage.toLowerCase()
  for each catalogEntry in [TRANSIENT, CONTEXT, PERMANENT]:
    for each pattern in catalogEntry.detectionPatterns:
      if normalizedMessage.contains(pattern.toLowerCase()):
        return catalogEntry.code

  // Step 3: No match — conservative default
  return ERR-PERM-002  // Treat unknown errors as PERMANENT
```

## Quick Reference Table

| Code | Category | Retryable | Patterns | Action |
|------|----------|-----------|----------|--------|
| ERR-TRANSIENT-001 | TRANSIENT | Yes | "overloaded", "capacity" | Retry 3x with backoff |
| ERR-TRANSIENT-002 | TRANSIENT | Yes | "rate limit", "429" | Retry 3x with backoff |
| ERR-TRANSIENT-003 | TRANSIENT | Yes | "timeout", "ETIMEDOUT" | Retry 2x with backoff |
| ERR-TRANSIENT-004 | TRANSIENT | Yes | "503", "504", "502" | Retry 3x with backoff |
| ERR-CONTEXT-001 | CONTEXT | No | "context", "token limit" | Graceful degradation |
| ERR-CONTEXT-002 | CONTEXT | No | "output too large", "truncated" | Re-dispatch reduced |
| ERR-PERM-001 | PERMANENT | No | "not found", "no such file" | Fail with path suggestion |
| ERR-PERM-002 | PERMANENT | No | "invalid", "malformed" | Fail with format guidance |
| ERR-PERM-003 | PERMANENT | No | "compilation", "compile error" | Fail with error details |
| ERR-PERM-004 | PERMANENT | No | "test failure", "assertion" | Fail with test output |
| ERR-PERM-005 | PERMANENT | No | "permission denied", "forbidden" | Fail with access guidance |
| ERR-CIRCUIT-001 | CIRCUIT | No | 3+ consecutive failures | Pause with AskUserQuestion |
| ERR-CIRCUIT-002 | CIRCUIT | No | 5+ total failures in phase | Abort phase |
