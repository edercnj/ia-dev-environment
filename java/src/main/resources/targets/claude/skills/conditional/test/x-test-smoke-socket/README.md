# x-test-smoke-socket

> TCP Socket Smoke Tests -- runs automated smoke tests against the TCP socket server using a standalone Java client with message framing and protocol validation.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `smoke_tests = true` AND `protocols` contains `tcp-custom` |
| **Invocation** | `/x-test-smoke-socket [--scenario echo\|all] [--k8s] [--host <host>] [--port <port>]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when smoke tests are enabled and TCP custom protocol is included in the project's protocol configuration.

## What It Does

Orchestrates black-box smoke tests against the application's TCP socket server using a standalone Java client that speaks the real wire protocol with proper message framing (length-prefix header). Validates connectivity, request/response processing, error handling for malformed messages, and persistent connection behavior. Supports automatic port-forwarding for Kubernetes environments and multiple test scenarios.

## Usage

```
/x-test-smoke-socket
/x-test-smoke-socket --scenario echo
/x-test-smoke-socket --scenario all --k8s
/x-test-smoke-socket --host 192.168.1.10 --port 9090
```

## See Also

- [x-test-smoke-api](../x-test-smoke-api/) -- REST API smoke tests
- [x-test-e2e](../x-test-e2e/) -- End-to-end integration tests
- [setup-environment](../setup-environment/) -- Dev environment setup with orchestrator
