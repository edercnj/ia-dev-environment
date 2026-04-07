# run-smoke-socket

> TCP Socket Smoke Tests -- runs automated smoke tests against the TCP socket server using a standalone Java client with message framing and protocol validation.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `smoke_tests = true` AND `protocols` contains `tcp-custom` |
| **Invocation** | `/run-smoke-socket [--scenario echo\|all] [--k8s] [--host <host>] [--port <port>]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when smoke tests are enabled and TCP custom protocol is included in the project's protocol configuration.

## What It Does

Orchestrates black-box smoke tests against the application's TCP socket server using a standalone Java client that speaks the real wire protocol with proper message framing (length-prefix header). Validates connectivity, request/response processing, error handling for malformed messages, and persistent connection behavior. Supports automatic port-forwarding for Kubernetes environments and multiple test scenarios.

## Usage

```
/run-smoke-socket
/run-smoke-socket --scenario echo
/run-smoke-socket --scenario all --k8s
/run-smoke-socket --host 192.168.1.10 --port 9090
```

## See Also

- [run-smoke-api](../run-smoke-api/) -- REST API smoke tests
- [run-e2e](../run-e2e/) -- End-to-end integration tests
- [setup-environment](../setup-environment/) -- Dev environment setup with orchestrator
