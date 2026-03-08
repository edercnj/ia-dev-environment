---
name: x-test-run
description: >
  Runs tests with coverage reporting and threshold validation. Executes unit,
  integration, and API tests, then analyzes coverage gaps. Enforces line
  coverage >= 95% and branch coverage >= 90%. Use whenever writing, running,
  or analyzing tests.
---

# Skill: Run Tests

## Purpose

Testing is critical in my-fastapi-service. The project enforces strict coverage thresholds: line coverage >= 95%, branch coverage >= 90%. This skill guides test execution, coverage analysis, and gap identification.

**Condition**: Use whenever writing, running, or analyzing tests for my-fastapi-service.

## Prerequisites

- python test framework configured
- Coverage tool integrated with build system
- Test classes exist in the test source tree

## Knowledge Pack References

Before running tests, read:
- `.claude/skills/testing/references/testing-philosophy.md` — 8 test categories, fixture patterns, data uniqueness
- `.claude/skills/testing/references/testing-conventions.md` — python-specific test frameworks, assertion libraries

## Coverage Thresholds

| Metric | Minimum |
|--------|---------|
| Line Coverage | >= 95% |
| Branch Coverage | >= 90% |

## Test Naming Convention

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

## Test Pattern (Arrange-Act-Assert)

Every test method follows AAA with clear separation:

```
// Arrange -- setup objects and data
// Act -- execute the method under test
// Assert -- verify the outcome
```

## Test Categories

| Category | When to Use | Tools |
|----------|-------------|-------|
| Unit | Domain logic, engines, mappers | Test framework + assertions |
| Integration | DB interactions, framework features | Framework test support |
| API | REST endpoints | HTTP test client |
| E2E | Full flow (request -> process -> persist -> response) | Integration framework |
| Contract | Protocol/format compliance | Parametrized tests |
| Performance | Latency SLAs, throughput | Load testing framework |

## Coverage Analysis

### Common Coverage Gaps and Solutions

| Gap | Solution |
|-----|----------|
| Uncovered `else` branch | Add test for the negative case |
| Uncovered exception path | Test error paths with exception assertions |
| Uncovered `default` in switch | Add test with unexpected input |
| Uncovered validation | Test with invalid inputs |

### Per-Class Coverage Report Format

```
+--------------------------+-------+--------+
|          Class           | Line  | Branch |
+--------------------------+-------+--------+
| ClassA                   | 100%  | 95.0%  |
+--------------------------+-------+--------+

Tests: XX passing, XX failing, XX errors, XX skipped
Global Coverage: XX% line / XX% branch
```

## Fixture Pattern

- Centralize test data in utility classes
- Naming: `a{Entity}()` or `a{Entity}With{Variation}()`
- Domain fixtures separate from protocol/format fixtures
- Generate unique identifiers to avoid conflicts between test runs

## Detailed References

For in-depth guidance on test execution, consult:
- `.claude/skills/x-test-run/SKILL.md`
- `.claude/skills/testing/SKILL.md`
