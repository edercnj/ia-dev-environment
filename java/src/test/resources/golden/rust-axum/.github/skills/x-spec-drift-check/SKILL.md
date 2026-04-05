---
name: x-spec-drift-check
description: >
  Detects spec-code drift by comparing story data contracts, endpoints, and Gherkin
  scenarios against implemented code. Supports standalone mode (full report) and inline
  mode (compact output for TDD loop integration in x-dev-lifecycle Phase 2).
  Reference: `.github/skills/x-spec-drift-check/SKILL.md`
---

# Skill: Spec Drift Check

## Purpose

Compares story specifications (data contracts, endpoints, Gherkin scenarios) against implemented code to detect drift. Operates in two modes:

- **Standalone mode** (default): Full report with all check categories
- **Inline mode**: Compact output for TDD loop integration

## Triggers

- `/x-spec-drift-check STORY-ID` -- standalone mode (full report)
- `/x-spec-drift-check STORY-ID --mode inline` -- inline mode (compact output)

## Workflow

```
1. PARSE    -> Read story file, extract data contracts + endpoints + Gherkin
2. SCAN     -> Search source code for matching fields, endpoints, tests
3. CHECK    -> Compare spec vs code, classify drift (PASS/WARN/FAIL)
4. REPORT   -> Generate itemized report (standalone) or compact summary (inline)
```

## Standalone Mode

Full itemized report covering:

| Check Type | Severity | Description |
|-----------|----------|-------------|
| Field Missing (M) | FAIL | Mandatory field not found in code |
| Field Missing (O) | WARN | Optional field absent |
| Field Type Mismatch | WARN | Field found but type differs |
| Endpoint Missing | FAIL | Declared endpoint not found |
| Scenario Uncovered | WARN | `@GK-N` without acceptance test |
| Naming Violation | WARN | CONSTITUTION.md convention violated |

## Inline Mode (TDD Loop Integration)

Compact output for x-dev-lifecycle Phase 2. Checks only data contracts and endpoints (skips Gherkin coverage and Constitution compliance).

- **WARN**: Non-blocking, displayed but loop continues
- **FAIL**: Critical drift, pauses loop for confirmation

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort with error message |
| No data contract | Standalone: empty section / Inline: skip |
| Malformed data contract | WARN with parse error details |
