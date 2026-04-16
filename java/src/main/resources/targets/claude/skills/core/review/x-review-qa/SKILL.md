---
name: x-review-qa
description: "QA specialist review: validates test coverage, TDD compliance, test naming, fixtures, parametrized tests, and acceptance criteria coverage."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: QA Specialist Review

## Purpose

Review code changes for QA compliance: test coverage thresholds, TDD process adherence, test naming conventions, fixture centralization, parametrized tests for data-driven scenarios, and acceptance criteria coverage. Validates that tests follow the Transformation Priority Premise (TPP) and Double-Loop TDD.

## When to Use

- Pre-PR quality validation for test quality
- Verifying TDD compliance in commit history
- Ensuring coverage thresholds are met
- Checking test naming and organization

## Triggers

- `/x-review-qa 42` -- review PR #42 for QA compliance
- `/x-review-qa src/test/` -- review test files at specific paths
- `/x-review-qa` -- review all current test changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| testing | `skills/testing/SKILL.md` | Test categories, coverage thresholds, fixture patterns, TDD workflow |

## Checklist (20 Items, Max Score: /40)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Coverage & Criteria (QA-01 to QA-03)

| # | Item | Score |
|---|------|-------|
| QA-01 | Test exists for each acceptance criterion | /2 |
| QA-02 | Line coverage >= 95% | /2 |
| QA-03 | Branch coverage >= 90% | /2 |

### Test Quality (QA-04 to QA-10)

| # | Item | Score |
|---|------|-------|
| QA-04 | Test naming convention followed: `[method]_[scenario]_[expected]` | /2 |
| QA-05 | AAA pattern (Arrange-Act-Assert) in every test | /2 |
| QA-06 | Parametrized tests for data-driven scenarios | /2 |
| QA-07 | Exception paths tested with specific assertions | /2 |
| QA-08 | No test interdependency (tests run in any order) | /2 |
| QA-09 | Fixtures centralized (no duplicate records/classes across test files) | /2 |
| QA-10 | Unique test data per test (no shared mutable state) | /2 |

### Test Completeness (QA-11 to QA-12)

| # | Item | Score |
|---|------|-------|
| QA-11 | Edge cases covered (null, empty, boundary values) | /2 |
| QA-12 | Integration tests for DB/API interactions | /2 |

### TDD Compliance (QA-13 to QA-18)

| # | Item | Score |
|---|------|-------|
| QA-13 | Commits show test-first pattern (test precedes implementation in git log) | /2 |
| QA-14 | Explicit refactoring after green (separate refactor commits) | /2 |
| QA-15 | Tests follow TPP progression (simple to complex) | /2 |
| QA-16 | No test written after implementation (test-after is a violation) | /2 |
| QA-17 | Acceptance tests validate end-to-end behavior | /2 |
| QA-18 | TDD coverage thresholds maintained across all modules | /2 |

### Smoke Test Verification (QA-19 to QA-20) — EPIC-0042

| # | Item | Score |
|---|------|-------|
| QA-19 | Smoke tests exist and cover critical path (when `testing.smoke_tests == true`; N/A when false) | /2 |
| QA-20 | ALL smoke tests pass — `{{SMOKE_COMMAND}}` executed with zero failures (when `testing.smoke_tests == true`; N/A when false) | /2 |

## Workflow

### Step 1 -- Gather Context

Read the testing knowledge pack for project test conventions:
- `skills/testing/SKILL.md`

### Step 2 -- Identify Changed Files

Determine scope: PR diff or specified paths. Separate production code from test code.

### Step 3 -- Coverage Analysis

Check coverage reports or run coverage tool. Verify line >= 95% and branch >= 90%.

### Step 4 -- Test Naming Audit

Scan test files for naming convention compliance: `[methodUnderTest]_[scenario]_[expectedBehavior]`.

### Step 5 -- TDD Commit Audit

Analyze git log for test-first pattern: test commits should precede or accompany implementation commits.

### Step 6 -- Test Quality Review

For each test file:
- Check AAA pattern
- Check parametrized tests for multi-scenario validations
- Check exception path coverage
- Check fixture centralization
- Check unique test data

### Step 6.5 -- Smoke Test Verification (EPIC-0042)

Execute smoke test verification when `testing.smoke_tests == true`:

1. Check if smoke test infrastructure exists:
   - If `testing.smoke_tests == false`: mark QA-19 and QA-20 as **N/A** (not scored, excluded from max score)
   - If `testing.smoke_tests == true`: proceed with smoke verification
2. **QA-19 — Smoke test existence:** Verify smoke tests exist and cover at least the critical path (health check + primary flow)
3. **QA-20 — Smoke test execution:** Run `{{SMOKE_COMMAND}}` and verify ALL smoke tests pass:
   ```bash
   {{SMOKE_COMMAND}}
   ```
   - If ALL smoke tests **PASS**: QA-20 scores 2/2
   - If ANY smoke test **FAILS**: QA-20 scores 0/2 AND STATUS becomes **Rejected**
   - Log each failing smoke test name and failure reason
4. **Hard rule:** ALL unit + integration + smoke tests MUST pass for STATUS: Approved

### Step 7 -- Generate Report

Produce the scored report.

## Output Format

```
ENGINEER: QA
STORY: [story-id or change description]
SCORE: XX/40

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [QA-XX] [Item description]

### FAILED
- [QA-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [QA-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No test files found | Report INFO: no test code discovered |
| Coverage tool not configured | Warn and skip QA-02, QA-03 |
| Git log not available | Warn and skip QA-13, QA-14, QA-16 |
| Smoke test failure (QA-20) | STATUS becomes Rejected regardless of other scores |
| `testing.smoke_tests == false` | Mark QA-19, QA-20 as N/A (excluded from max score) |
