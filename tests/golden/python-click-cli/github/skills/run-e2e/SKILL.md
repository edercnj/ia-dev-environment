---
name: run-e2e
description: >
  Skill: End-to-End Tests — Runs integration tests that validate the complete
  flow from request through all application layers to response, using a real
  database via containers. Covers happy path, error, timeout, and concurrent
  request scenarios.
---

# Skill: End-to-End Tests (E2E)

## Description

Runs or implements end-to-end tests that validate the complete application flow: inbound request -> parsing -> validation -> business logic -> persistence -> response. Tests use a real database (or in-memory equivalent) and exercise all layers of the architecture.

**Condition**: This skill applies to all projects with integration test infrastructure.

## Prerequisites

- click test extension configured
- Database available for tests (in-memory or containerized)
- Test dependencies in build file: assertion library, async utilities

## Knowledge Pack References

Before writing or running E2E tests, read:
- `.claude/skills/testing/references/testing-philosophy.md` — real vs in-memory DB decisions, fixture patterns, data uniqueness
- `.claude/skills/testing/references/testing-conventions.md` — python-specific test framework, assertion library, directory structure

## Execution Flow

1. **Verify test infrastructure** — Check framework test extension, database, assertion library
2. **Run E2E tests** — Execute with test profile active (in-memory DB, random ports)
3. **Validate test results** — All tests pass, no test pollution, database state verified
4. **Report results** — Total passed/failed/skipped, duration, coverage location

## Mandatory E2E Scenarios

| Scenario | Description | Priority |
|----------|-------------|----------|
| Happy path | Standard request processed and persisted | CRITICAL |
| Validation error | Invalid input rejected with proper error | CRITICAL |
| Not found | Non-existent resource returns proper error | HIGH |
| Duplicate/conflict | Duplicate creation returns conflict response | HIGH |
| Concurrent requests | Multiple simultaneous requests processed independently | HIGH |
| Malformed input | Garbage input handled gracefully | MEDIUM |
| Timeout behavior | Slow operations handled per configured timeouts | MEDIUM |

## Test Patterns

### Test Isolation
- Each test creates its own data, never depends on other tests
- Use unique identifiers per test execution to avoid conflicts
- Automatic cleanup via transaction rollback or explicit teardown

### Async Resource Readiness
- Wait for server/socket to be listening before connecting
- Use async polling (not sleep) for resource readiness
- Default timeout: 10 seconds for resource startup

### Assertion Style
- Use fluent assertion library (AssertJ, Chai, etc.)
- Verify both response content AND database state
- Check response status codes, headers, and body

## Review Checklist

- [ ] Tests exercise the complete flow (input -> all layers -> output)
- [ ] Real or realistic database used (not mocked)
- [ ] Each test is independent and idempotent
- [ ] Unique identifiers generated per test run
- [ ] Async resources awaited with polling (not sleep)
- [ ] Both response and database state verified
- [ ] Error scenarios covered (validation, not-found, conflict)

## Detailed References

For in-depth guidance on E2E testing, consult:
- `.claude/skills/run-e2e/SKILL.md`
- `.claude/skills/testing/SKILL.md`
