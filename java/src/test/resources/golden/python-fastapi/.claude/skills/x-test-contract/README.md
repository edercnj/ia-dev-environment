# x-test-contract

> Runs consumer-driven contract tests (Pact, Spring Cloud Contract) to verify API compatibility between services.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `testing.contract_tests = true` |
| **Invocation** | `/x-test-contract [--provider \| --consumer \| --all]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `testing.contract_tests = true` in the project configuration.

## What It Does

Runs consumer-driven contract tests to verify API compatibility between services. Supports both consumer-side (pact generation) and provider-side (pact verification) workflows using Pact or Spring Cloud Contract. Consumer mode generates pact files from consumer expectations, provider mode verifies the service against published pacts, and combined mode runs the full cycle.

## Usage

```
/x-test-contract --consumer
/x-test-contract --provider
/x-test-contract --all
```

## See Also

- [x-test-contract-lint](../x-test-contract-lint/) -- API contract validation (OpenAPI, AsyncAPI, Protobuf)
- [x-review-api](../x-review-api/) -- REST API design review
- [x-test-e2e](../x-test-e2e/) -- End-to-end integration tests
