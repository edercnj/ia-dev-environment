---
name: run-smoke-api
description: >
  Skill: REST API Smoke Tests — Runs automated smoke tests against the REST
  API using Newman/Postman. Validates that critical endpoints respond correctly
  in local, container-orchestrated, and staging environments. Covers health
  checks, CRUD operations, and error responses.
---

# Skill: REST API Smoke Tests (Newman)

## Description

Orchestrates black-box smoke tests against the application's REST API using Newman (Postman CLI). Tests run against a deployed environment to validate that critical endpoints respond correctly. Idempotent: each execution creates its own test data and cleans up after.

**Condition**: This skill applies when smoke tests are enabled AND REST is in protocols.

## Prerequisites

- Newman installed: `npm install -g newman`
- Application deployed and accessible (local or remote)
- Health endpoint responding at `/health` or equivalent
- Postman collection file exists in `smoke-tests/api/`

## Knowledge Pack References

Before running smoke tests, read:
- `.github/skills/testing/SKILL.md` — test categories, data uniqueness
- `.github/skills/api-design/SKILL.md` — REST conventions, status codes

## Execution Flow

1. **Verify prerequisites** — Check Newman installed (`newman --version`)
2. **Setup networking** (if container-orchestrated) — Create port-forward, register cleanup
3. **Wait for health check** — Poll health endpoint (30 retries, 2s interval)
4. **Run Newman collection** — Execute smoke tests from `smoke-tests/api/`
5. **Collect and report results** — Pass/fail per scenario, exit code
6. **Cleanup** — Automatic via trap EXIT

## Mandatory Scenarios

| Scenario | Method | Expected Status | Criticality |
|----------|--------|-----------------|-------------|
| Health/liveness | GET | 200 | CRITICAL |
| Health/readiness | GET | 200 | CRITICAL |
| Create resource | POST | 201 | CRITICAL |
| List resources | GET | 200 | HIGH |
| Get resource by ID | GET | 200 | HIGH |
| Update resource | PUT | 200 | HIGH |
| Delete resource | DELETE | 204 | HIGH |
| Duplicate (409) | POST | 409 | MEDIUM |
| Not found (404) | GET | 404 | MEDIUM |
| Invalid payload | POST | 400 | MEDIUM |

## Artifacts

| File | Description |
|------|-------------|
| `smoke-tests/api/*.postman_collection.json` | Newman test collection |
| `smoke-tests/api/environment.local.json` | Local environment config |
| `smoke-tests/api/environment.*.json` | Per-environment configs |
| `smoke-tests/api/run-smoke-api.sh` | Execution script |

## Troubleshooting

| Problem | Likely Cause | Solution |
|---------|-------------|----------|
| `newman: command not found` | Newman not installed | `npm install -g newman` |
| Health check timeout | App not started | Check application logs |
| 500 Internal Server Error | Application bug | Check application logs |
| Unexpected 409 | Data from previous run | Clean database or use unique IDs |

## Detailed References

For in-depth guidance on smoke testing, consult:
- `.github/skills/run-smoke-api/SKILL.md`
- `.github/skills/testing/SKILL.md`
