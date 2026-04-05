---
name: x-spec-drift-check
description: "Detects drift between story specifications and implemented code: verifies data contract fields, endpoints, Gherkin scenario coverage, and naming conventions. Produces itemized report with PASS/WARN/FAIL per check."
user-invocable: true
argument-hint: "STORY-ID [--source-root ./src] [--constitution CONSTITUTION.md]"
allowed-tools: Read, Write, Glob, Grep, Bash
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

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

## Step 1: PARSE Story

Read the story file `story-XXXX-YYYY.md` and extract:

### Data Contract Fields (Section 5)

For each table in the data contracts section, extract:
- **Field name**: the column value from `Campo` or first column
- **Type**: the declared type (e.g., `String`, `BigDecimal`, `List<FieldCheck>`)
- **Mandatory**: `M` (mandatory) or `O` (optional)
- **Parent entity**: the entity name above the table (e.g., `PaymentRequest`, `DriftCheckResult`)

### Endpoints

Search for HTTP method + path patterns:
- `GET /v1/...`
- `POST /v1/...`
- `PUT /v1/...`
- `DELETE /v1/...`
- `PATCH /v1/...`

### Gherkin Scenarios (Section 7)

Extract all `@GK-N` identifiers and their scenario titles from the acceptance criteria section.

### CONSTITUTION.md Reference

If `--constitution` flag is provided and the file exists, read naming conventions from it.

## Step 2: SCAN Codebase

### Field Scanning

For each data contract field:
1. Search for the field name in all DTO, Record, Entity, and Model classes
2. Match against class fields, constructor parameters, and getter methods
3. Extract the actual type of the found field

### Endpoint Scanning

For each declared endpoint:
1. Search for HTTP method annotations: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`, `@RequestMapping`, `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`
2. Match the path pattern against annotation values
3. Record the controller class and method name

### Gherkin Scenario Scanning

For each `@GK-N`:
1. Search test files for corresponding acceptance test references (AT-N, @GK-N in comments/annotations)
2. Match by scenario ID or title substring

### Naming Convention Scanning

If CONSTITUTION.md is provided:
1. Extract naming rules (e.g., camelCase for methods, PascalCase for classes)
2. Scan source files for violations

## Step 3: COMPARE

### Drift Detection Rules

| Type | Severity | Detection Logic |
|------|----------|----------------|
| Field Missing | FAIL | Mandatory (M) field from data contract not found in any DTO/Record |
| Field Missing (Optional) | WARN | Optional (O) field from data contract not found in code |
| Field Type Mismatch | WARN | Field found but type differs from declared (e.g., BigDecimal vs Double) |
| Endpoint Missing | FAIL | Declared endpoint not found in any controller |
| Scenario Uncovered | WARN | @GK-N without corresponding acceptance test |
| Naming Violation | WARN | CONSTITUTION.md naming convention violated in code |

## Step 4: REPORT

### Output Format

Generate an itemized report following this exact format:

```
=== Spec Drift Check -- STORY-XXXX-YYYY ===

Data Contracts:
  PASS  FieldName (Type, M) -> found in ClassName
  FAIL  FieldName (Type, M) -> NOT FOUND
  WARN  FieldName (Type, O) -> field absent (optional)
  WARN  FieldName (Type, M) -> type mismatch: expected Type, found ActualType in ClassName

Endpoints:
  PASS  POST /v1/path -> ControllerClass.methodName()
  FAIL  GET /v1/path -> NOT FOUND in any controller

Gherkin Coverage:
  PASS  @GK-1 "scenario title" -> AT found (TestClass)
  WARN  @GK-3 "scenario title" -> no AT found

Constitution Compliance:
  PASS  No violations detected
  -- or --
  WARN  methodName in ClassName violates camelCase convention

Summary: N FAIL, M WARN -- DRIFT DETECTED / NO DRIFT
```

### Exit Code Semantics

- **Exit code 0**: No FAIL results (only PASS and WARN allowed)
- **Exit code non-zero**: At least one FAIL result detected

### Empty Sections

- If no data contract is defined in the story: `Data Contracts: No data contract defined`
- If no endpoints are declared: `Endpoints: No endpoints declared`
- If no Gherkin scenarios exist: `Gherkin Coverage: No scenarios defined`
- If no CONSTITUTION.md provided: `Constitution Compliance: Not configured (no --constitution flag)`

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort with "Story file not found: {path}" |
| Source root not found | Abort with "Source root not found: {path}" |
| No data contract section | Report "No data contract defined" in Data Contracts section |
| No endpoints declared | Report "No endpoints declared" in Endpoints section |
| CONSTITUTION.md not found | Skip constitution checks, report "Not configured" |
| Malformed story markdown | Report parsing errors, continue with extractable data |

## Integration Points

- **Standalone mode** (this skill): User invokes manually with `/x-spec-drift-check STORY-ID`
- **Inline mode** (future): Integrated into `/x-dev-lifecycle` workflow
- **Scope assessment** (future): Aggregated across multiple stories for epic-level drift analysis
