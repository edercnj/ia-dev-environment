---
name: x-spec-drift-check
description: >
  Detects drift between story specifications and implemented code: verifies
  data contract fields, endpoints, Gherkin scenario coverage, and naming
  conventions. Produces itemized report with PASS/WARN/FAIL per check.
  Reference: `.github/skills/x-spec-drift-check/SKILL.md`
---

# Skill: Spec Drift Check (Standalone)

## Purpose

Compares story specifications against implemented code to detect divergences (drift) before opening a PR. Acts as a "spec linter" that validates data contracts, endpoints, Gherkin scenario coverage, and naming conventions against the actual codebase.

## Triggers

- `/x-spec-drift-check STORY-0007-0003` -- check drift for a specific story
- `/x-spec-drift-check STORY-0007-0003 --source-root ./src` -- specify source root
- `/x-spec-drift-check STORY-0007-0003 --constitution CONSTITUTION.md` -- include naming convention checks

## Workflow

```
1. PARSE   -> Read story markdown, extract data contracts, endpoints, Gherkin scenarios
2. SCAN    -> Search codebase for corresponding implementations
3. COMPARE -> Match spec declarations against code findings
4. REPORT  -> Generate itemized drift report with severity levels
```

## Drift Detection Rules

| Type | Severity | Detection Logic |
|------|----------|----------------|
| Field Missing | FAIL | Mandatory (M) field from data contract not found in any DTO/Record |
| Field Missing (Optional) | WARN | Optional (O) field from data contract not found in code |
| Field Type Mismatch | WARN | Field found but type differs from declared |
| Endpoint Missing | FAIL | Declared endpoint not found in any controller |
| Scenario Uncovered | WARN | @GK-N without corresponding acceptance test |
| Naming Violation | WARN | CONSTITUTION.md naming convention violated in code |

## Output Format

```
=== Spec Drift Check -- STORY-XXXX-YYYY ===

Data Contracts:
  PASS  FieldName (Type, M) -> found in ClassName
  FAIL  FieldName (Type, M) -> NOT FOUND

Endpoints:
  PASS  POST /v1/path -> ControllerClass.methodName()
  FAIL  GET /v1/path -> NOT FOUND in any controller

Gherkin Coverage:
  PASS  @GK-1 "scenario title" -> AT found (TestClass)
  WARN  @GK-3 "scenario title" -> no AT found

Constitution Compliance:
  PASS  No violations detected

Summary: N FAIL, M WARN -- DRIFT DETECTED / NO DRIFT
```

## Exit Code Semantics

- **Exit code 0**: No FAIL results (only PASS and WARN)
- **Exit code non-zero**: At least one FAIL result

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort with error message |
| Source root not found | Abort with error message |
| No data contract section | Report "No data contract defined" |
| No endpoints declared | Report "No endpoints declared" |
| CONSTITUTION.md not found | Skip constitution checks |
