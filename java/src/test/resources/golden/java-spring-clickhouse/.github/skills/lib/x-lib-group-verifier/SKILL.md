---
name: x-lib-group-verifier
description: >
  Build gate verification between parallelism groups. Compiles code,
  classifies errors, decides retry vs escalate, extracts outputs for
  next group. Used between each implementation group in Phase 2.
  Reference: `.github/skills/lib/x-lib-group-verifier/SKILL.md`
---

# Skill: Group Verifier (Build Gate)

## Purpose

Runs between each parallelism group (G1-G7) during Phase 2 of the feature lifecycle. Compiles the code, classifies any errors, decides whether to retry or escalate, and extracts created file contents for the next group's dependency inputs.

## When to Use

- **Feature Lifecycle Phase 2**: After each group completes, BEFORE starting the next group

## Procedure

### STEP 1 -- COMPILE

```bash
{{COMPILE_COMMAND}}
```

For G7 (Tests group), use the full build command instead:

```bash
{{BUILD_COMMAND}}
```

### STEP 2 -- ANALYZE

- If exit code = 0 -> Go to STEP 5 (success)
- If exit code != 0 -> Parse error output, go to STEP 3

### STEP 3 -- CLASSIFY ERRORS

| Error Category | Classification | Meaning |
| --- | --- | --- |
| Missing type/symbol from a PREVIOUS group | MISSING_DEPENDENCY | Previous group regression |
| Missing type/symbol from the CURRENT group | TASK_ERROR | Current task produced bad code |
| Type mismatch / incompatible types | TASK_ERROR | Wrong types in current task |
| Missing external dependency | BUILD_ERROR | Missing dependency in build file |
| Interface/contract mismatch | TASK_ERROR | Implementation doesn't match port |
| Any other error | UNKNOWN | Needs investigation |

### STEP 4 -- DECIDE

| Classification | Attempt | Action |
| --- | --- | --- |
| TASK_ERROR | 1st | RETRY same tier |
| TASK_ERROR | 2nd | RETRY same tier |
| TASK_ERROR | 3rd | ESCALATE to next tier |
| MISSING_DEPENDENCY | any | HALT pipeline |
| BUILD_ERROR | any | HALT pipeline |
| UNKNOWN | 1st | ESCALATE immediately |
| UNKNOWN | 2nd | HALT pipeline |

**Escalation path:** Junior -> Mid -> Senior -> Manual intervention

### STEP 5 -- EXTRACT OUTPUTS

For each task in the completed group, read all created/modified files and store content for subsequent groups.

### STEP 6 -- COMMIT

After successful compilation, commit following Conventional Commits format.

### STEP 7 -- REPORT

```
Group G{N} verified: {PASS|FAIL}
  Tasks: {N} completed, {N} retried, {N} escalated
  Files: {N} created, {N} modified
  Warnings: {N}
```

## G7 Extended Verification

Additional checks for the test group:
1. Parse test results from build reports
2. Count: tests, failures, errors, skipped
3. Parse coverage report
4. Verify: line coverage >= 95%, branch coverage >= 90%

## Integration Notes

- Invoked by `x-dev-story-implement` during Phase 2 after each group
- Uses `{{COMPILE_COMMAND}}` for G1-G6, `{{BUILD_COMMAND}}` for G7
- Reference: `.github/skills/lib/x-lib-group-verifier/SKILL.md`
