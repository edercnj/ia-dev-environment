---
name: x-lib-audit-rules
description: >
  Audits compliance of all project rules AND knowledge packs against
  source code. Launches parallel subagents (one per rule/knowledge-pack)
  for scanning, then aggregates into a unified report with severity
  classification and story suggestions.
  Reference: `.github/skills/lib/x-lib-audit-rules/SKILL.md`
---

# Skill: Audit Rules & Patterns (Codebase Compliance Review)

## Input

**Arguments:** `$ARGUMENTS`

- `--scope all` (default): Review rules + knowledge packs
- `--scope rules`: Review rules only
- `--scope patterns`: Review knowledge packs only
- `--rules all` (default when scope includes rules): Review all rules
- `--rules 01,02,03`: Review specific rules only
- `--fix`: After report, prompt user to create stories

## Execution Flow (Orchestrator Pattern)

```
1. DISCOVER   -> List rules + knowledge packs (inline, lightweight)
2. SCAN       -> Launch N parallel subagents (one per rule + one per KP) in SINGLE message
3. AGGREGATE  -> Collect results, deduplicate, generate report + stories (inline)
```

## Phase 1: Discovery (Orchestrator -- Inline)

### 1a. Discover Rules

List all rules in `.github/instructions/*.instructions.md`. Filter by `--rules` argument if provided.

### 1b. Discover Knowledge Packs

Find all skill files in `.github/skills/*/SKILL.md`. Group by knowledge pack.

### 1c. Build Scan Plan

Document the number of rules and knowledge packs to scan. Calculate total subagents to launch.

## Phase 2: Parallel Scanning (Subagents)

Launch one subagent per rule and one per knowledge pack, all in the same message.

### Subagent: Rule Scanner

For each violation found, report:
- File path and line number
- Violation description (1 sentence)
- Severity: CRITICAL, HIGH, MEDIUM, LOW
- Rule section reference

### Subagent: Knowledge Pack Scanner

For each violation found, report:
- File path and line number
- Violation description (1 sentence)
- Severity: CRITICAL, HIGH, MEDIUM, LOW
- Reference file and section

## Phase 3: Aggregation & Report (Orchestrator -- Inline)

1. Collect and deduplicate findings
2. Generate compliance report with severity summary
3. Optionally create stories for violations (if `--fix`)

## Anti-Patterns

- Do NOT report false positives
- Do NOT flag test code for production rules
- Do NOT flag generated or third-party code
- Do NOT duplicate findings between rules and knowledge packs

## Integration Notes

- Can be run independently of the feature lifecycle
- Complements `/x-review` (diff-based) with full codebase scanning
- Reference: `.github/skills/lib/x-lib-audit-rules/SKILL.md`
