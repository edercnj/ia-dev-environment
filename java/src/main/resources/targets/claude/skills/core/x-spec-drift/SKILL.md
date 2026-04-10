---
name: x-spec-drift
description: "Detects spec-code drift by comparing story data contracts, endpoints, and Gherkin scenarios against implemented code. Supports standalone mode (full report) and inline mode (compact output for TDD loop integration in x-dev-lifecycle Phase 2)."
user-invocable: true
allowed-tools: Read, Bash, Grep, Glob
argument-hint: "[STORY-ID] [--mode standalone|inline]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Spec Drift Check

## Purpose

Compares story specifications (data contracts, endpoints, Gherkin scenarios) against implemented code to detect drift. Operates in two modes:

- **Standalone mode** (default): Full report with all check categories
- **Inline mode**: Compact output for TDD loop integration

## Triggers

- `/x-spec-drift-check STORY-ID` — standalone mode (full report)
- `/x-spec-drift-check STORY-ID --mode inline` — inline mode (compact output)

## Parameters

| Parameter | Type | Default | Values | Description |
|-----------|------|---------|--------|-------------|
| `STORY-ID` | String | required | story identifier | Story to check for drift |
| `--mode` | String | standalone | standalone, inline | Output mode |

### Mode Selection

| Mode | When to Use | Output |
|------|-------------|--------|
| `standalone` | Manual invocation before PR | Full itemized report |
| `inline` | Automated call in x-dev-lifecycle Phase 2 | Single-line compact summary |

## Workflow

### Standalone Mode

```
1. PARSE    -> Read story file, extract data contracts + endpoints + Gherkin
2. SCAN     -> Search source code for matching fields, endpoints, tests
3. CHECK    -> Compare spec vs code, classify drift (PASS/WARN/FAIL)
4. REPORT   -> Generate itemized report with severity
```

#### Step 1 — Parse Story

Read the story markdown file and extract:

1. **Data Contracts** (Section 5): field name, type, mandatory (M/O)
2. **Endpoints**: HTTP method + path (e.g., `POST /v1/payments`)
3. **Gherkin Scenarios** (Section 7): IDs `@GK-N` and titles
4. **Constitution reference**: Check if `CONSTITUTION.md` exists in project root

#### Step 2 — Scan Source Code

For each extracted item, search the source tree:

- **Fields**: Search DTOs, Records, data classes for field name + type match
- **Endpoints**: Search controllers/handlers for route annotations matching method + path
- **Gherkin Scenarios**: Search test files for acceptance test references to `@GK-N`
- **Constitution**: If present, validate naming conventions against source

#### Step 3 — Classify Drift

| Check Type | Severity | Condition |
|-----------|----------|-----------|
| Field Missing (M) | FAIL | Mandatory field from data contract not found in any DTO/Record |
| Field Missing (O) | WARN | Optional field absent from code |
| Field Type Mismatch | WARN | Field found but type differs from spec |
| Endpoint Missing | FAIL | Declared endpoint not found in any controller |
| Scenario Uncovered | WARN | `@GK-N` without corresponding acceptance test |
| Naming Violation | WARN | CONSTITUTION.md naming convention violated |

#### Step 4 — Report

Output follows itemized format with severity:

```
=== Spec Drift Check — STORY-XXXX-YYYY ===
Data Contracts:
  PASS  FieldName (Type, M) -> found in ClassName
  FAIL  FieldName (Type, M) -> NOT FOUND
  WARN  FieldName (Type, O) -> field absent (optional)
Endpoints:
  PASS  POST /path -> ControllerClass.methodName()
  FAIL  GET /path -> NOT FOUND in any controller
Gherkin Coverage:
  PASS  @GK-1 "title" -> AT-1 found (TestClass)
  WARN  @GK-3 "title" -> no AT found
Constitution Compliance:
  PASS  No violations detected
Summary: N FAIL, M WARN — DRIFT DETECTED / NO DRIFT
```

#### Exit Code

- **0**: No FAIL results (only PASS and WARN)
- **Non-zero**: At least one FAIL detected

### Inline Mode (x-dev-lifecycle TDD Loop Integration)

Provides compact, non-blocking drift checks after each RED-GREEN-REFACTOR cycle in x-dev-lifecycle Phase 2. Designed for automated integration — not manual invocation.

#### Scope Restrictions

Inline mode performs a **subset** of standalone checks:

| Check | Inline Mode | Standalone Mode |
|-------|------------|-----------------|
| Data Contract fields (M) | Yes | Yes |
| Data Contract fields (O) | Yes | Yes |
| Field Type Mismatch | Yes | Yes |
| Endpoint Missing | Yes | Yes |
| Gherkin Coverage | **No** — reserved for standalone | Yes |
| Constitution Compliance | **No** — reserved for standalone | Yes |

#### Compact Output Format

For non-critical results (PASS and WARN only):

```
Drift Check (inline): N PASS, M WARN — details of first warning
```

For critical drift (at least one FAIL):

```
Drift Check (inline): CRITICAL — FieldName (Type, M) NOT FOUND in DTO
   Continue TDD loop despite critical drift? (y/n)
```

#### Non-Blocking Behavior

- **WARN results** are displayed but do **not** interrupt the TDD loop. The non-blocking nature ensures development flow continues.
- **FAIL results** (critical drift) pause the loop with a confirmation prompt
- If the developer confirms: loop continues with FAIL logged
- If the developer denies: loop pauses for correction

#### TDD Loop Integration Point

Called automatically by x-dev-lifecycle after each TDD cycle:

```
Phase 2 TDD Loop:
  2.1 RED   — write failing test
  2.2 GREEN — make test pass
  2.3 REFACTOR — improve design
  2.4 DRIFT CHECK (inline) — verify spec alignment
  2.5 COMMIT — atomic TDD commit
```

#### InlineDriftCheckConfig

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | `true` | Activate inline drift check (auto-enabled when story has data contract) |
| `blockOnCritical` | boolean | `true` | Pause TDD loop on critical FAIL |

#### InlineDriftResult

| Field | Type | Description |
|-------|------|-------------|
| `passCount` | int | Total PASS checks |
| `warnCount` | int | Total WARN checks |
| `failCount` | int | Total critical FAIL checks |
| `summary` | String | Compact single-line message |
| `criticalDetails` | List | Details of FAIL items (empty if none) |

#### Decision Logic

```
if story has no data contract:
    skip inline check entirely
    return

run data contract field checks (M and O)
run endpoint checks

if failCount > 0 and blockOnCritical:
    emit CRITICAL message
    prompt for confirmation
else:
    emit summary message
    continue TDD loop
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort with "Story file not found: {path}" |
| No data contract in story | Standalone: report "No data contract defined" / Inline: skip silently |
| Source root not accessible | Abort with "Source root not accessible: {path}" |
| Malformed data contract | WARN with "Unable to parse data contract field: {line}" |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-dev-lifecycle | Called by | Invoked during Phase 2 TDD loop for inline drift detection |
| x-dev-implement | Complements | Drift check validates that implementation matches story spec |
| x-review-pr | Complements | Standalone drift check provides pre-PR validation of spec alignment |
