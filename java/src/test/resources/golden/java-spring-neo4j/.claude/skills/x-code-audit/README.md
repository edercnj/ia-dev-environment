# x-code-audit

> Full codebase review against all project standards. Launches parallel subagents per audit dimension (Clean Code, SOLID, Architecture, Tests, Security, Cross-file), consolidates findings into a severity-categorized report with score. Use for periodic quality validation.

| | |
|---|---|
| **Category** | Review |
| **Invocation** | `/x-code-audit [--scope all\|rules\|patterns\|architecture\|cross-file]` |
| **Reads** | coding-standards, architecture, quality-gates |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Performs a comprehensive audit of the entire codebase against project standards by launching parallel subagents across six dimensions: Clean Code and SOLID, architecture layer violations, coding standards, test quality, security patterns, and cross-file consistency. Consolidates findings into a severity-categorized report (CRITICAL/MEDIUM/LOW/INFO) with a 0-100 score. Use periodically or before major releases for quality validation.

## Usage

```
/x-code-audit
/x-code-audit --scope architecture
/x-code-audit --scope security
```

## Workflow

1. **Detect** -- Determine audit scope and read project conventions
2. **Audit** -- Launch parallel subagents per dimension (Clean Code, Architecture, Standards, Tests, Security, Cross-file)
3. **Consolidate** -- Deduplicate, categorize by severity, and calculate score (0-100)
4. **Report** -- Generate audit report file with findings and recommendations

## Outputs

| Artifact | Path |
|----------|------|
| Audit report | `results/audits/codebase-audit-YYYY-MM-DD.md` |

## See Also

- [x-review](../x-review/) -- PR-scoped specialist review (per-story, not full codebase)
- [x-review-pr](../x-review-pr/) -- Tech Lead review for a single PR
- [x-owasp-scan](../x-owasp-scan/) -- Security-focused OWASP Top 10 verification
