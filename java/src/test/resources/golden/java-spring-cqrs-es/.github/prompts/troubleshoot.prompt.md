---
name: troubleshoot
description: >
  Systematic troubleshooting for errors, stacktraces, build failures,
  and unexpected behavior using a structured diagnostic methodology.
---

# Troubleshoot

Use this prompt to diagnose and fix issues in **my-spring-cqrs**.

## Prerequisites

- An error message, stacktrace, build log, or description of unexpected behavior
- Access to the relevant source code and test suite

## Workflow

The **x-ops-troubleshoot** skill follows a strict 5-step methodology:

```
/x-ops-troubleshoot
```

### Step 1 — Reproduce

Get the exact error output: stacktrace, build log, or test failure.
Never skip this step — you need the precise error to diagnose correctly.

### Step 2 — Locate

Find where the error originates in the codebase.
Trace from the error message back to the source.

### Step 3 — Understand

Determine why the code is failing.
Compare expected vs actual behavior. Check recent changes.

### Step 4 — Fix

Write a test that reproduces the bug first, then fix the code.
The bug-reproducing test prevents regressions.

### Step 5 — Verify

Run the full test suite to ensure no regressions:
- Unit tests pass
- Integration tests pass
- Coverage thresholds met (line >= 95%, branch >= 90%)

## Common Error Categories

- **Compilation errors** — Missing imports, type mismatches, syntax
- **Test failures** — Assertion errors, timeout, flaky tests
- **Runtime exceptions** — NullPointer, ClassCast, connection refused
- **Build failures** — Dependency conflicts, plugin errors
- **Coverage gaps** — Untested branches, missing edge cases

## Agent Involved

- **java-developer** — Primary debugger and fixer

## Tips

- Always follow the 5-step order: reproduce, locate, understand, fix, verify
- Never skip "write a test first" in Step 4
- If the error is intermittent, focus on reproducing it reliably first
