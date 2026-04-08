# x-lib-group-verifier

> Build gate verification between parallelism groups. Compiles code, classifies errors, decides retry vs escalate, extracts outputs for next group. Used between each implementation group in Phase 2.

| | |
|---|---|
| **Category** | Library (internal) |
| **Called by** | x-dev-lifecycle |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## Purpose

Acts as a build gate between parallelism groups (G1-G7) during implementation. Compiles the code after each group completes, classifies any errors by category (task error, missing dependency, build error), decides whether to retry or escalate, and extracts created file contents as dependency inputs for the next group.

## Integration Points

| Caller | Context | Input | Output |
|--------|---------|-------|--------|
| x-dev-lifecycle | Phase 2, after each group completes | Group ID (G1-G7), files created/modified by the group | Verification report (PASS/FAIL), extracted file contents for next group, atomic git commit |

## Procedure

1. **Compile** -- run the compile command (or full build command for G7/tests)
2. **Classify errors** -- parse compiler output into TASK_ERROR, MISSING_DEPENDENCY, BUILD_ERROR, or UNKNOWN
3. **Decide** -- retry same tier (up to 3 attempts), escalate to next tier, or halt pipeline based on error classification
4. **Extract outputs** -- read all created/modified files and store contents for subsequent groups
5. **Commit** -- create an atomic git commit per group using Conventional Commits format
6. **Report** -- emit group verification summary with task counts, retries, escalations, and warnings

## See Also

- [x-lib-task-decomposer](../x-lib-task-decomposer/) -- produces the task breakdown and parallelism groups verified by this skill
- [x-dev-lifecycle](../../x-dev-lifecycle/) -- orchestrator that invokes this skill after each implementation group
