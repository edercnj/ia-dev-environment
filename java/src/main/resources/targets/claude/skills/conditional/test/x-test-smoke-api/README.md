# x-test-smoke-api

> REST API Smoke Tests -- runs automated smoke tests against the REST API using Newman/Postman. Supports local, container-orchestrated, and staging environments.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `smoke_tests = true` AND `protocols` contains `rest` |
| **Invocation** | `/x-test-smoke-api [--env local\|k8s\|staging] [--k8s]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when smoke tests are enabled and REST is included in the project's protocol configuration.

## What It Does

Orchestrates black-box smoke tests against the application's REST API using Newman (Postman CLI). Tests run against a deployed environment to validate that critical endpoints respond correctly. Each execution is idempotent: it creates its own test data and cleans up after. Supports automatic port-forwarding for Kubernetes environments, health check polling before test execution, and structured result reporting.

## Usage

```
/x-test-smoke-api
/x-test-smoke-api --env local
/x-test-smoke-api --env k8s --k8s
/x-test-smoke-api --env staging
```

## See Also

- [x-test-smoke-socket](../x-test-smoke-socket/) -- TCP socket smoke tests
- [x-test-e2e](../x-test-e2e/) -- End-to-end integration tests
- [x-security-dast](../x-security-dast/) -- Dynamic application security testing against running app
