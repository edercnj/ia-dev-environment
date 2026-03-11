---
name: run-contract-tests
description: >
  Skill: Contract Tests — Runs consumer-driven contract tests using Pact or
  Spring Cloud Contract to verify API compatibility between services. Supports
  consumer-side pact generation and provider-side verification workflows.
---

# Skill: Contract Tests

## Description

Runs consumer-driven contract tests to verify API compatibility between services. Supports both consumer-side (pact generation) and provider-side (pact verification) workflows using Pact or Spring Cloud Contract.

**Condition**: This skill applies when contract tests are enabled in the project configuration.

## Prerequisites

- Contract testing framework installed (Pact or Spring Cloud Contract)
- Consumer and/or provider test files exist
- Build tool configured for contract test execution

## Knowledge Pack References

Before running contract tests, read:
- `.github/skills/testing/SKILL.md` — test categories, fixture patterns, kotlin-specific test conventions

## Execution Flow

1. **Verify framework** — Scan build file for Pact/Spring Cloud Contract dependencies
2. **Determine mode** — Consumer (generate pacts), Provider (verify against pacts), or All
3. **Run tests** — Execute appropriate test suite
4. **Report results** — Interactions tested, passed, failed

## Consumer Side

1. Discover consumer test files (scan for `@PactTest`, `Pact`, `pact.describe`)
2. Run consumer tests: generate pact files (JSON contracts)
3. Verify pact files generated in `target/pacts/` or `pacts/` directory
4. Validate pact content: interactions defined, request/response match

## Provider Side

1. Discover provider verification configuration
2. Start provider application (or use test server)
3. Run provider verification against published pacts
4. Report: which interactions passed/failed
5. Publish verification results (if pact broker configured)

## Contract Checklist (10 points)

1. Consumer defines explicit expectations (request headers, body, status code)
2. Provider states defined for each interaction (setup test data)
3. Pact files versioned and published to broker (or committed to repo)
4. Provider verification runs in CI pipeline
5. Breaking changes detected before deployment
6. Contract covers happy path AND error responses
7. Contract includes required headers (Content-Type, Authorization pattern)
8. No sensitive data in pact files (use example data)
9. Consumer version tagged (branch, commit SHA)
10. Can-I-Deploy check integrated in CI (if using Pact Broker)

## Output Format

```
## Contract Test Results — [Mode: Consumer/Provider]

### Summary
- Interactions tested: [N]
- Passed: [N]
- Failed: [N]

### Verdict: PASS / FAIL
```

## Detailed References

For in-depth guidance on contract testing, consult:
- `.github/skills/run-contract-tests/SKILL.md`
- `.github/skills/testing/SKILL.md`
