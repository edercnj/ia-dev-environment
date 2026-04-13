---
name: x-test-smoke-api
description: "Runs automated smoke tests against the REST API using Newman/Postman. Supports local, container-orchestrated, and staging environments."
user-invocable: true
allowed-tools: Read, Bash
argument-hint: "[--env local|k8s|staging] [--k8s]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: REST API Smoke Tests

## Purpose

Orchestrate black-box smoke tests against the application's REST API using Newman (Postman CLI). Tests run against a deployed environment to validate that critical endpoints respond correctly. Idempotent: each execution creates its own test data and cleans up after.

## Activation Condition

Include this skill when smoke_tests is enabled AND "rest" is in protocols.

## Triggers

- `/x-test-smoke-api` -- run against local environment (already running on default port)
- `/x-test-smoke-api --k8s` -- run with automatic port-forward (orchestrator)
- `/x-test-smoke-api --env staging` -- run against staging environment

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--env` | String | No | `local` | Target environment: `local`, `k8s`, `staging` |
| `--k8s` | Flag | No | false | Enable automatic port-forward to orchestrator |

## Prerequisites

- Newman installed: `npm install -g newman`
- Application deployed and accessible (local, {{ORCHESTRATOR}}, or remote)
- Health endpoint responding at `/health` or equivalent
- Postman collection file exists in `smoke-tests/api/`

## Workflow

### Step 1 — Verify Prerequisites

Check that Newman is installed:
- Run `newman --version`
- If missing, suggest: `npm install -g newman`

### Step 2 — Setup Networking (if --k8s)

- Create port-forward to the application service
- Register cleanup via trap EXIT

### Step 3 — Wait for Health Check

Poll health endpoint:
- Retry up to 30 times with 2s interval
- If health check fails after retries, abort with exit code 2

### Step 4 — Run Newman Collection

Execute smoke tests:
- Load Postman collection from `smoke-tests/api/`
- Load environment file matching `--env` flag
- Execute all requests in order
- Capture results (pass/fail per scenario)

### Step 5 — Report Results

- Total scenarios: passed / failed / skipped
- CRITICAL scenarios: all must pass
- HIGH scenarios: all must pass
- MEDIUM scenarios: >= 80% must pass
- Overall exit code: 0 (success), 1 (test failure), 2 (setup failure)

### Step 6 — Cleanup

Automatic via trap EXIT:
- Stop port-forward if started
- Report final status

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
| `smoke-tests/api/x-test-smoke-api.sh` | Execution script |

## Error Handling

| Scenario | Action |
|----------|--------|
| `newman: command not found` | Report Newman not installed, suggest `npm install -g newman` |
| Health check timeout | Abort with exit code 2, suggest checking application logs |
| 500 Internal Server Error | Report application bug, suggest checking application logs |
| Unexpected 409 | Suggest cleaning database or using unique IDs |
| Connection refused | Suggest using `--k8s` flag or starting app manually |
