---
name: x-test-smoke-socket
description: "Runs automated smoke tests against the TCP socket server using a standalone Java client with message framing and protocol validation."
user-invocable: true
allowed-tools: Read, Bash
argument-hint: "[--scenario echo|all] [--k8s] [--host <host>] [--port <port>]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: TCP Socket Smoke Tests

## Purpose

Orchestrate black-box smoke tests against the application's TCP socket server using a standalone Java client. The client speaks the real wire protocol with proper message framing (length-prefix header). Tests validate that the server correctly receives messages, processes them, and returns valid responses over persistent TCP connections.

## Activation Condition

Include this skill when smoke_tests is enabled AND "tcp-custom" is in protocols.

## Triggers

- `/x-test-smoke-socket` -- run all scenarios against local environment
- `/x-test-smoke-socket --k8s` -- run with automatic port-forward (orchestrator)
- `/x-test-smoke-socket --k8s --scenario echo` -- run specific scenario
- `/x-test-smoke-socket --host 10.0.0.1 --port 8583` -- run against custom host/port

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--scenario` | String | No | `all` | Scenario: `echo`, `request-approved`, `request-denied`, `malformed`, `persistent`, `all` |
| `--k8s` | Flag | No | false | Enable automatic port-forward for HTTP and TCP ports |
| `--host` | String | No | `localhost` | Target server hostname |
| `--port` | Integer | No | (configured) | Target server TCP port |

## Prerequisites

- Java 21+ installed
- Client JAR built: `cd smoke-tests/socket && {{BUILD_COMMAND}}`
- Application deployed and accessible (local or {{ORCHESTRATOR}})
- TCP socket listening on configured port

## Workflow

### Step 1 — Verify Prerequisites

- Check Java version: `java --version`
- Check client JAR exists in `smoke-tests/socket/target/`
- If JAR missing, suggest build command

### Step 2 — Setup Networking (if --k8s)

- Create port-forward for both HTTP (health) and TCP (socket) ports
- Register cleanup via trap EXIT

### Step 3 — Wait for Health Check

Poll HTTP health endpoint:
- Retry up to 30 times with 2s interval
- Abort with exit code 2 if health check fails

### Step 4 — Run Scenarios

Execute Java client JAR:
- Pass `--scenario` flag (default: `all`)
- Pass `--host` and `--port` for target server
- Each scenario: connect, send message, validate response, report

### Step 5 — Report Results

- Per-scenario: PASS / FAIL with details
- CRITICAL scenarios: all must pass
- HIGH scenarios: all must pass
- Overall exit code: 0 (success), 1 (test failure), 2 (setup failure), 3 (JAR not found)

### Step 6 — Cleanup

Automatic via trap EXIT:
- Stop port-forwards if started

## Available Scenarios

| Scenario | Flag | Description |
|----------|------|-------------|
| Echo/Ping | `echo` | Basic connectivity and response validation |
| Request Approved | `request-approved` | Standard request with successful response |
| Request Denied | `request-denied` | Request triggering a denial/error response |
| Malformed Message | `malformed` | Invalid bytes, expects error response |
| Persistent Connection | `persistent` | Multiple messages on same TCP connection |
| All | `all` | Runs all scenarios sequentially |

## Artifacts

| File | Description |
|------|-------------|
| `smoke-tests/socket/pom.xml` | Client build file (executable JAR) |
| `smoke-tests/socket/src/` | Java client source code |
| `smoke-tests/socket/x-test-smoke-socket.sh` | Execution script |

## Client Architecture

- `SmokeTestRunner` -- Main class: arg parsing, scenario dispatch, result aggregation
- `SmokeSocketClient` -- TCP client wrapper with length-prefix framing (send/receive)
- `SmokeScenario` -- Sealed interface for scenario implementations
- `ScenarioResult` -- Record: pass/fail + message

## Error Handling

| Scenario | Action |
|----------|--------|
| JAR not found | Suggest build command: `cd smoke-tests/socket && {{BUILD_COMMAND}}` |
| Connection refused (TCP) | Suggest using `--k8s` flag or starting app manually |
| Connection refused (HTTP) | Check orchestrator service status |
| Unexpected response | Suggest checking application logs for protocol or logic bugs |
| Message parse failure | Suggest verifying length-prefix and encoding config |
