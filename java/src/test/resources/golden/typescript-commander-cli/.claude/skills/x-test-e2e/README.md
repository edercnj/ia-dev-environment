# x-test-e2e

> Runs integration tests that validate the complete flow from request through all application layers to response, using a real database.

| | |
|---|---|
| **Category** | Testing |
| **Invocation** | `/x-test-e2e [scenario: happy-path\|error\|timeout\|persistent\|all]` |
| **Reads** | testing |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

> **Conditional skill**: This skill is included only for projects with integration test infrastructure (framework test extension and database configured).

## What It Does

Runs or implements end-to-end tests that exercise the complete application flow: inbound request, parsing, validation, business logic, persistence, and response. Tests use a real or in-memory database and cover mandatory scenarios including happy path, validation errors, not-found, duplicate/conflict, concurrent requests, and error recovery. Each test is isolated with unique identifiers and uses async polling instead of sleep for resource readiness.

## Usage

```
/x-test-e2e
/x-test-e2e happy-path
/x-test-e2e persistent
/x-test-e2e all
```

## Workflow

1. **Verify** -- Check test infrastructure prerequisites (framework test extension, database, assertion library)
2. **Execute** -- Run E2E tests filtered by scenario tag if specified
3. **Validate** -- Confirm all tests pass, no test pollution, database state verified
4. **Report** -- Output pass/fail/skip counts, duration per class, and error details

## See Also

- [x-test-run](../../../core/test/x-test-run/) -- General test execution with coverage thresholds
- [x-spec-drift](../../../core/dev/x-spec-drift/) -- Validates spec-code alignment before E2E runs
