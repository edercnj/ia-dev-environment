# x-test-tdd — Full Protocol

> **Slim/Full split** per [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md).
> The `SKILL.md` sibling carries the minimum viable contract; this file
> holds the full workflow, validation algorithms, TPP reference, and
> anti-patterns.

## 1. Workflow Overview

```
0. VALIDATE       -> Parse arguments, resolve task plan path, validate task ID
1. LOAD PLAN      -> Read task-plan-XXXX-YYYY-NNN.md, extract TDD Cycles section
2. DRY-RUN CHECK  -> If --dry-run, list cycles and exit
3. EXECUTE CYCLES -> For each cycle (from --from-cycle or 1):
   3a. RED        -> Write test, verify it FAILS, commit [TDD:RED]
   3b. GREEN      -> Implement minimum code, compile, verify PASS, check coverage, commit [TDD:GREEN]
   3c. REFACTOR   -> Improve design, verify PASS, commit [TDD:REFACTOR] (or skip)
4. REPORT         -> Summary of executed cycles and commits
```

## 2. Phase 0 — Validate and Resolve

### 2.1 Parse Task ID

The task ID MUST match the pattern `TASK-XXXX-YYYY-NNN` where:
- `XXXX` = 4-digit epic number (zero-padded)
- `YYYY` = 4-digit story number (zero-padded)
- `NNN` = 3-digit task number (zero-padded)

Extract:
- Epic ID: `XXXX`
- Story ID: `YYYY`
- Task number: `NNN`

If invalid: **ABORT** with `"Task ID invalid -- expected format: TASK-XXXX-YYYY-NNN"`.

### 2.2 Resolve Task Plan Path

1. Compute `EPIC_DIR` by searching for the epic directory:
   - Try exact: `plans/epic-XXXX`
   - Try suffix variant: `plans/epic-XXXX-*`
2. Resolve task plan: `<EPIC_DIR>/plans/task-plan-XXXX-YYYY-NNN.md`
3. If file does not exist: **ABORT** with `"Task plan not found: task-plan-XXXX-YYYY-NNN.md"`.

### 2.3 Validate `--from-cycle`

If `--from-cycle N` is provided:
- N MUST be ≥ 1.
- N MUST be ≤ total number of cycles in the task plan.
- If N is out of range: **ABORT** with `"Invalid --from-cycle: N (task plan has M cycles)"`.

## 3. Phase 1 — Load Task Plan

### 3.1 Read Task Plan

Read the task plan file at the resolved path. Extract the **TDD Cycles** section.

### 3.2 Parse Cycles

Each cycle follows this structure:

```markdown
#### Cycle N: [Description]

- **TPP Level**: [degenerate|constant|scalar|conditional|collection|complex]
- **Transform**: [TPP transformation applied]

**RED** (Write Failing Test):
- Test name: `[methodUnderTest]_[scenario]_[expectedBehavior]`
- Assertion: [primary assertion description]
- Run: `{{TEST_COMMAND}}`
- Expected: FAIL

**GREEN** (Minimum Implementation):
- Implementation: [minimum code to make test pass]
- Run: `{{COMPILE_COMMAND}}` then `{{TEST_COMMAND}}`
- Expected: PASS

**REFACTOR** (Improve Design):
- Opportunities: [description or "None at this cycle"]
- Run: `{{TEST_COMMAND}}`
- Expected: PASS

**Commit**: `feat(TASK-XXXX-YYYY-NNN): [description] [TDD:RED|GREEN|REFACTOR]`
```

Extract for each cycle: cycle number and description, TPP level and transformation, RED details (test name, assertion, expected behavior), GREEN details (implementation approach), REFACTOR opportunities (may be "None"), and commit message template.

### 3.3 Validate TPP Order

Verify cycles are ordered by TPP priority (lower priority number first); see §7. If cycles are not in TPP order: emit **WARNING** `"Cycles not in strict TPP order -- proceeding but review recommended"` and continue.

## 4. Phase 2 — Dry Run (if `--dry-run`)

When `--dry-run` is provided, output the cycle listing and exit without executing:

```
x-test-tdd dry-run for TASK-XXXX-YYYY-NNN
Task Plan: task-plan-XXXX-YYYY-NNN.md
Total Cycles: N

  Cycle 1: [description]
    TPP Level: [level] | Transform: [transform]
    RED:      [test name]
    GREEN:    [implementation summary]
    REFACTOR: [opportunity or "None"]

  Cycle 2: [description]
    ...

  Cycle N: [description]
    ...

Dry-run complete. No files modified, no commits created.
```

Exit after listing.

## 5. Phase 3 — Execute Cycles

For each cycle from `--from-cycle` (default 1) to the last cycle:

### 5.1 RED Phase — Write Failing Test

**Objective:** write a test that defines the expected behavior. The test MUST fail because the implementation does not yet exist.

1. Read the cycle's RED section from the task plan.
2. Write the test file/method using the specified test name and assertion.
3. Follow test naming convention: `[methodUnderTest]_[scenario]_[expectedBehavior]`.
4. Execute the test: `{{TEST_COMMAND}}`.
5. **Validate RED:**
   - If the test **FAILS** (expected): proceed to commit.
   - If the test **PASSES** (unexpected): **ABORT** with:
     ```
     RED phase failed: test passes before implementation
     Cycle: N -- [description]
     Test: [test name]
     This means the behavior already exists. Either:
     1. The test is not testing new behavior
     2. The implementation was already done
     3. The test assertion is wrong (tautology)
     Aborting to prevent invalid TDD cycle.
     ```
6. Stage test files and delegate commit via the Skill tool (Rule 13 — INLINE-SKILL pattern):

       Skill(skill: "x-git-commit", args: "--task TASK-XXXX-YYYY-NNN --type test --subject \"add test for [description]\" --tdd RED")

### 5.2 GREEN Phase — Minimum Implementation

**Objective:** write the absolute minimum code to make the failing test pass. No gold-plating — only what the test requires.

1. Read the cycle's GREEN section from the task plan.
2. Implement the minimum code as described in the Implementation Guide.
3. Compile: `{{COMPILE_COMMAND}}`. If compilation fails: fix the compilation error and retry. Do NOT add untested behavior while fixing.
4. Execute tests: `{{TEST_COMMAND}}`.
   - If tests **PASS**: proceed to coverage check.
   - If tests **FAIL**: adjust implementation (minimum fix only) and retry.
5. Check coverage when `{{COVERAGE_COMMAND}}` is available.
   - Parse coverage output.
   - If line coverage < 95% or branch coverage < 90%:
     - If `--warn-only-coverage` is present: emit **WARNING** and proceed (legacy mode).
     - Otherwise: emit **ERROR** and **BLOCK** the cycle with:
       ```
       COVERAGE GATE FAILED after GREEN (EPIC-0042):
         Line:   XX% (threshold: 95%)
         Branch: XX% (threshold: 90%)
       Uncovered lines/branches:
         - [file:line] -- [description of uncovered branch]
         - ...
       Action required: add tests for uncovered branches before proceeding.
       Override: pass --warn-only-coverage to proceed with warning only.
       ```
     - The cycle does NOT advance to REFACTOR until coverage thresholds are met.
     - Identify specific uncovered lines/branches from the coverage report and list them.
     - Write additional tests to cover the gaps, then re-run `{{TEST_COMMAND}}` + `{{COVERAGE_COMMAND}}`.
     - Max 2 retry attempts to reach coverage; after 2 failures, ABORT the cycle.
6. Stage implementation files and delegate commit via the Skill tool (Rule 13 — INLINE-SKILL pattern):

       Skill(skill: "x-git-commit", args: "--task TASK-XXXX-YYYY-NNN --type feat --subject \"implement [description]\" --tdd GREEN")

### 5.3 REFACTOR Phase — Improve Design

**Objective:** improve code structure without changing behavior. Tests MUST continue passing after refactoring.

1. Read the cycle's REFACTOR section from the task plan.
2. If opportunities say "None at this cycle" or "None":
   - Log: `"No refactoring needed for Cycle N -- skipping REFACTOR phase"`.
   - Skip to next cycle (no REFACTOR commit).
3. If refactoring opportunities exist:
   a. Apply the refactoring (extract method, rename, simplify, eliminate duplication).
   b. Execute tests: `{{TEST_COMMAND}}`.
   c. **Validate REFACTOR:**
      - If tests **PASS**: proceed to commit.
      - If tests **FAIL**: **REVERT** the refactoring changes:
        ```bash
        git checkout -- .
        ```
        Log warning:
        ```
        Refactoring reverted: tests failed after refactoring
        Cycle: N -- [description]
        Refactoring: [what was attempted]
        The refactoring introduced a behavior change. Skipping REFACTOR commit.
        ```
        Skip the REFACTOR commit for this cycle (do NOT abort the entire execution).
   d. Stage refactored files and delegate commit via the Skill tool (Rule 13 — INLINE-SKILL pattern):

          Skill(skill: "x-git-commit", args: "--task TASK-XXXX-YYYY-NNN --type refactor --subject \"[refactoring description]\" --tdd REFACTOR")

### 5.4 Cycle Transition (Full vs Compact Log)

After completing all phases of a cycle, log progress and move to the next cycle.

**Full multi-line format** (interactive; `--orchestrated` absent):

```
Cycle N/M complete:
  RED:      test(TASK-...): add test for [desc] [TDD:RED]       -> {sha}
  GREEN:    feat(TASK-...): implement [desc] [TDD:GREEN]        -> {sha}
  REFACTOR: refactor(TASK-...): [desc] [TDD:REFACTOR]           -> {sha} | skipped
```

**Compact single-line format** (orchestrated mode; `--orchestrated` present):

```
Cycle {cycleNumber}/{totalCycles}: RED {testResult} GREEN {testResult} REFACTOR {refactorStatus} → {commitSha}
```

Where:
- RED `{testResult}`: `FAIL_EXPECTED` (fails as intended) or `FAIL_UNEXPECTED` (passes prematurely — abort).
- GREEN `{testResult}`: `PASS` (tests pass after implementation) or `FAIL` (tests still failing — retry).
- `{refactorStatus}`: `PASS` (applied, tests still pass) or `skipped`.
- `{commitSha}`: 7-char abbreviated SHA of the last commit in the cycle.

**Detection algorithm (Story 0033-0003, explicit flag):**

```
if args.contains("--orchestrated"):
    emit_format = "compact"
else:
    emit_format = "full"
```

Once `emit_format` is determined at the start of execution, apply it consistently to every cycle log, the Phase 4 report, and any intermediate status output. The explicit flag replaces the earlier implicit "am I invoked via Skill tool?" heuristic, which was fragile because a skill cannot reliably determine how it was invoked from inside its own execution context.

## 6. Phase 4 — Report

After all cycles are complete, produce a summary:

```
x-test-tdd execution complete for TASK-XXXX-YYYY-NNN

Cycles executed: N (from cycle X to cycle Y)
Commits created: M

  Cycle 1: [description]
    RED:      {sha} test(TASK-...): ...
    GREEN:    {sha} feat(TASK-...): ...
    REFACTOR: {sha} refactor(TASK-...): ... | skipped

  Cycle 2: [description]
    RED:      {sha} ...
    GREEN:    {sha} ...
    REFACTOR: {sha} ... | skipped

  ...

Coverage: XX% line, XX% branch (after final GREEN)
Warnings: [count] (list any coverage or TPP order warnings)

All TDD cycles complete. Task TASK-XXXX-YYYY-NNN is done.
```

## 7. Transformation Priority Premise (TPP) Reference

| Priority | Transformation | From -> To | Example |
|----------|---------------|------------|---------|
| 1 | degenerate | `{} -> nil` | Method returns null/void/empty |
| 2 | constant | `nil -> constant` | Returns a fixed value |
| 3 | constant+ | `constant -> constant+` | Returns another fixed value for a different case |
| 4 | scalar | `constant -> scalar` | Returns a variable instead of a constant |
| 5 | statements | `statement -> statements` | Adds additional instructions |
| 6 | conditional | `unconditional -> conditional` | Adds if/switch branching |
| 7 | collection | `scalar -> collection` | Uses List/Map/Set instead of single value |
| 8 | recursion | `collection -> recursion` | Iterates or recurses over collection |

### TPP Ordering Rule

Tests MUST progress from lower priority (simpler transformations) to higher priority (more complex). Each cycle's implementation should use the simplest transformation that makes the test pass:

- Cycle 1 is ALWAYS degenerate (the simplest possible case).
- Each subsequent cycle applies the next-simplest transformation needed.
- Never jump to complex transformations when a simpler one suffices.
- If a GREEN implementation requires a more complex transformation than the test demands, the test is too broad — split it.

## 8. Double-Loop TDD Integration

This skill operates as the **inner loop** of Double-Loop TDD:

```
OUTER LOOP (Acceptance Test -- driven by x-story-implement):
  Write failing acceptance test (end-to-end scenario)
  |
  INNER LOOP (Unit Tests -- driven by x-test-tdd):
  |  Cycle 1: RED -> GREEN -> REFACTOR
  |  Cycle 2: RED -> GREEN -> REFACTOR
  |  ...
  |  Cycle N: RED -> GREEN -> REFACTOR
  |
  Acceptance test passes (all unit behavior composes into acceptance)
```

- The outer loop is managed by `x-story-implement` or `x-task-implement`.
- This skill (`x-test-tdd`) drives the inner loop: systematic unit-level TDD cycles.
- Each cycle builds on the previous, following TPP ordering from simple to complex.

## 9. Anti-Patterns

- **Gold-plating in GREEN:** implementing more than the test requires. GREEN must be the absolute minimum.
- **Skipping RED validation:** every RED test MUST be executed and MUST fail. Skipping validation defeats TDD.
- **REFACTOR changing behavior:** refactoring MUST NOT change test outcomes. If a test fails after refactoring, the change introduced new behavior.
- **Non-TPP ordering:** starting with complex cases (conditionals, collections) before degenerate cases violates TPP.
- **Batch commits:** combining multiple phases into a single commit. Each phase (RED, GREEN, REFACTOR) gets its own atomic commit.
- **Testing implementation details:** tests should verify behavior, not internal structure. Test the what, not the how.
- **Deferring coverage gaps:** coverage below threshold MUST be addressed in the current cycle, not deferred. Use `--warn-only-coverage` only for legacy migration.

## 10. Rationale

The slim `SKILL.md` carries only the contract that an invocation needs at runtime: parameters, output commits, exit conditions, and template variables. The step-by-step algorithms (RED/GREEN/REFACTOR validation, coverage gate retry logic, TPP ordering rules) live here because they are diagnostic material consulted when a cycle misbehaves or a new task plan is authored, not on every happy-path invocation.

See [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md).
