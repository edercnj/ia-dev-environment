---
name: x-code-audit
description: "Full codebase review against all project standards. Launches parallel subagents per audit dimension (Clean Code, SOLID, Architecture, Tests, Security, Cross-file), consolidates findings into a severity-categorized report with score. Use for periodic quality validation."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
argument-hint: "[--scope all|rules|patterns|architecture|cross-file]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Codebase Audit (Orchestrator)

## Purpose

Performs a comprehensive audit of the entire {{PROJECT_NAME}} codebase against project standards, coding conventions, architecture rules, and quality gates. Similar to a Tech Lead review but for the whole codebase, not just a single PR.

## When to Use

- `/x-code-audit` — audit entire codebase (all dimensions)
- `/x-code-audit --scope rules` — audit coding standards compliance only
- `/x-code-audit --scope architecture` — audit architecture layer violations only
- `/x-code-audit --scope security` — audit security patterns only

## Workflow Overview

```
Phase 0 — DETECT     -> Determine scope, read project conventions (inline)
Phase 1 — AUDIT      -> Launch parallel subagents per dimension (SINGLE message)
Phase 2 — CONSOLIDATE -> Collect findings, score, categorize (inline)
Phase 3 — REPORT     -> Generate audit report file (inline)
```

## Phase 0 — Detect Scope (Inline)

1. Parse scope argument (default: `all`)
2. Read project conventions:
   - `rules/03-coding-standards.md` — coding rules
   - `rules/04-architecture-summary.md` — architecture layers
   - `rules/05-quality-gates.md` — coverage thresholds
   - `knowledge/coding-standards/coding-conventions.md` — {{LANGUAGE}} conventions
   - `knowledge/architecture/architecture-principles.md` — architecture principles
3. List all source files:
   ```bash
   find . -type f -name "*.{{LANGUAGE_EXT}}" | grep -v node_modules | grep -v target | grep -v build
   ```

## Phase 1 — Audit (Parallel Subagents)

Launch **one subagent per audit dimension** in a SINGLE Agent tool call. Each subagent receives the project conventions and audits one dimension.

### Dimension 1 — Clean Code and SOLID

> Audit all source files for Clean Code violations:
> - Method length > 25 lines
> - Class length > 250 lines
> - Functions with > 4 parameters
> - Boolean flags as function parameters
> - Magic numbers/strings (unnamed constants)
> - God classes (classes doing too much)
> - Train wrecks (long chained method calls)
> - Wildcard imports
> - SRP violations (class with multiple reasons to change)
> - OCP violations (modifying existing code instead of extending)
> - LSP violations (subtypes not substitutable)
> - ISP violations (empty method implementations)
> - DIP violations (depending on concrete implementations)

### Dimension 2 — Architecture Layer Violations

> Audit imports and dependencies for architecture violations:
> - Domain importing from adapter or framework
> - Adapter.inbound importing from adapter.outbound
> - Application importing from adapter
> - Framework types leaking into domain
> - Circular dependencies between packages

### Dimension 3 — Coding Standards

> Audit for {{LANGUAGE}}-specific coding convention violations:
> - Naming conventions (variables, methods, classes)
> - Null returns (should use Optional or empty collection)
> - Null parameters (forbidden)
> - Exception handling (catching at wrong level, missing context)
> - Comments that repeat what code says
> - Mutable global state

### Dimension 4 — Test Quality and TDD Compliance

> Audit test suite for quality:
> - Coverage gaps (files without tests)
> - Test naming convention violations ([method]_[scenario]_[expected])
> - Mocked domain logic (forbidden)
> - Tests depending on execution order
> - Sleep-based synchronization
> - Missing boundary value tests
> - Missing error path tests

### Dimension 5 — Security Patterns

> Audit for security vulnerabilities:
> - SQL injection risks (string concatenation in queries)
> - XSS risks (unescaped output)
> - Command injection (unsanitized input in shell commands)
> - Hardcoded secrets/credentials
> - Insecure random number generation
> - Missing input validation at system boundaries
> - Overly permissive CORS or auth

### Dimension 6 — Cross-File Consistency

> Audit for cross-file consistency issues:
> - Duplicated logic across files
> - Inconsistent error handling patterns
> - Inconsistent naming across modules
> - Unused exports/public methods
> - Missing interface implementations
> - Orphaned files (not referenced anywhere)

## Phase 2 — Consolidate Findings (Inline)

Collect findings from all subagents and:

1. **Deduplicate** — same issue found by multiple dimensions
2. **Categorize by severity:**
   - **CRITICAL** — security vulnerabilities, architecture violations, data corruption risk
   - **MEDIUM** — SOLID violations, missing tests, code smell clusters
   - **LOW** — naming issues, minor style deviations, documentation gaps
   - **INFO** — suggestions, optimization opportunities
3. **Score** — Calculate overall score (0-100):
   - Start at 100
   - CRITICAL: -10 per finding
   - MEDIUM: -3 per finding
   - LOW: -1 per finding
   - INFO: no deduction
   - Minimum: 0

## Phase 3 — Generate Report (Inline)

Write report to `results/audits/codebase-audit-YYYY-MM-DD.md`:

```markdown
# Codebase Audit Report — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Scope:** {scope}
**Score:** {score}/100

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | N |
| MEDIUM   | N |
| LOW      | N |
| INFO     | N |

## CRITICAL Findings

### [C-001] {Title}
- **Location:** {file}:{line}
- **Dimension:** {dimension}
- **Description:** {description}
- **Recommendation:** {fix}

## MEDIUM Findings
...

## LOW Findings
...

## INFO / Suggestions
...

## Recommendations

1. {Top priority action}
2. {Second priority action}
3. {Third priority action}
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No source files found | Abort with "No source files found for audit" |
| Subagent fails | Report dimension as "Unable to audit" with error |
| Project conventions not found | Audit with defaults, add WARNING to report |
| Invalid --scope value | Error with valid options list |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-review | Complements | x-review targets PR changes; codebase audit targets the entire codebase |
| x-review-pr | Complements | Tech Lead review for PRs; codebase audit for periodic whole-project review |
| x-ops-troubleshoot | Follows up | Audit findings may require troubleshooting to resolve |
