# Smoke Test Configuration

## Purpose

Smoke tests validate that the application starts correctly and responds to basic health checks
after deployment. They are black-box tests run against a live environment.

## Endpoints to Test

| # | Method | Path | Expected Status | Expected Body (contains) |
|---|--------|------|----------------|--------------------------|
| 1 | GET | /health | 200 | "status" |

## Execution

1. Deploy the application to the target environment
2. Wait for readiness probe to pass
3. Run smoke tests against the deployed instance
4. Verify all endpoints return expected status codes

## Failure Handling

- If any smoke test fails, trigger rollback procedure
- Notify the on-call team via alerting channel
- Capture response body and logs for investigation
