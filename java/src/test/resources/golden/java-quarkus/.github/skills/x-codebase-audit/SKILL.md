---
name: x-codebase-audit
description: >
  Full codebase review against all project standards. Launches parallel subagents
  per audit dimension (Clean Code, SOLID, Architecture, Tests, Security, Cross-file),
  consolidates findings into a severity-categorized report with score.
  Use for periodic quality validation.
  Reference: `.github/skills/x-codebase-audit/SKILL.md`
---

# Skill: Codebase Audit

## Purpose

Performs a comprehensive audit of the entire {{PROJECT_NAME}} codebase against project standards, coding conventions, architecture rules, and quality gates.

## Triggers

- `/x-codebase-audit` -- audit entire codebase
- `/x-codebase-audit --scope rules` -- coding standards only
- `/x-codebase-audit --scope architecture` -- architecture violations only
- `/x-codebase-audit --scope security` -- security patterns only

## Execution Flow (Orchestrator Pattern)

```
1. DETECT      -> Determine scope, read project conventions (inline)
2. AUDIT       -> Launch parallel subagents per dimension (SINGLE message)
3. CONSOLIDATE -> Collect findings, score, categorize (inline)
4. REPORT      -> Generate audit report file (inline)
```

## Step 1: Detect Scope (Inline)

1. Parse scope argument (default: `all`)
2. Read project conventions:
   - `rules/03-coding-standards.md`
   - `rules/04-architecture-summary.md`
   - `rules/05-quality-gates.md`
   - `skills/coding-standards/references/coding-conventions.md`
   - `skills/architecture/references/architecture-principles.md`

## Step 2: Audit Dimensions (Parallel)

Launch one subagent per dimension:

### Dimension 1: Clean Code & SOLID
- Method length > 25 lines, class length > 250 lines
- Functions with > 4 parameters, boolean flag parameters
- Magic numbers, god classes, train wrecks
- SRP, OCP, LSP, ISP, DIP violations

### Dimension 2: Architecture Layer Violations
- Domain importing from adapter/framework
- Cross-layer dependency violations
- Circular dependencies

### Dimension 3: Coding Standards
- {{LANGUAGE}}-specific convention violations
- Null returns, null parameters, bad exception handling
- Comments that repeat code, mutable global state

### Dimension 4: Test Quality & TDD
- Coverage gaps, naming convention violations
- Mocked domain logic, order-dependent tests
- Missing boundary/error tests

### Dimension 5: Security Patterns
- SQL/XSS/command injection risks
- Hardcoded secrets, insecure randomness
- Missing input validation

### Dimension 6: Cross-File Consistency
- Duplicated logic, inconsistent patterns
- Unused exports, orphaned files

## Step 3: Consolidate

1. Deduplicate findings
2. Categorize: CRITICAL (-10), MEDIUM (-3), LOW (-1), INFO (0)
3. Calculate score (0-100, starting at 100)

## Step 4: Generate Report

Write to `docs/audits/codebase-audit-YYYY-MM-DD.md`:

```markdown
# Codebase Audit Report — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD | **Score:** {score}/100

| Severity | Count |
|----------|-------|
| CRITICAL | N |
| MEDIUM   | N |
| LOW      | N |

## CRITICAL Findings
### [C-001] {Title}
- **Location:** {file}:{line}
- **Description:** {description}
- **Recommendation:** {fix}

## Recommendations
1. {Top priority}
2. {Second priority}
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No source files | Abort with message |
| Subagent fails | Report dimension as "Unable to audit" |
| Conventions not found | Audit with defaults, add WARNING |
