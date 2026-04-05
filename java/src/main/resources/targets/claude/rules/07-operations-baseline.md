# Rule 07 — Operations Baseline

> **Full reference:** Read `skills/observability/SKILL.md` for detailed SRE practices and monitoring patterns.

## Health Checks (Non-Negotiable)

| Check | Purpose | Endpoint |
|-------|---------|----------|
| Liveness | Process is alive and not deadlocked | `/health/live` |
| Readiness | Ready to accept traffic (dependencies up) | `/health/ready` |
| Startup | Initialization complete (one-time check) | `/health/started` |

- Every service MUST expose liveness, readiness, and startup probes
- Health checks MUST NOT include heavy computations or external calls (liveness only)
- Readiness checks MUST verify critical dependencies (database, cache, message broker)

## Graceful Shutdown (Non-Negotiable)

| Requirement | Detail |
|-------------|--------|
| Shutdown timeout | Configurable via environment variable (default: 30s) |
| In-flight requests | Complete all in-flight requests before shutdown |
| Connection draining | Stop accepting new connections, drain existing |
| Resource cleanup | Close database pools, flush caches, deregister from service registry |

## Structured Logging (Non-Negotiable)

| Field | Required | Description |
|-------|----------|-------------|
| `timestamp` | Yes | ISO-8601 UTC timestamp |
| `level` | Yes | Log level (DEBUG, INFO, WARN, ERROR) |
| `message` | Yes | Human-readable message |
| `trace_id` | Yes | Distributed trace identifier |
| `span_id` | Yes | Current span identifier |
| `service` | Yes | Service name |

- All logs MUST be structured JSON in production
- Log levels MUST be configurable at runtime without restart
- Sensitive data (PII, credentials) MUST NEVER appear in logs

## Correlation ID Propagation

- Every inbound request MUST have or generate a correlation ID
- Correlation ID MUST propagate through all downstream calls (HTTP headers, message metadata)
- Header name: `X-Correlation-ID` (configurable)

## Configuration Externalization (12-Factor)

- All configuration MUST come from environment variables or config maps
- ZERO hardcoded configuration values in source code
- Secrets MUST come from a secrets manager, never from env vars in plaintext

## Forbidden

- Hardcoded timeouts, ports, or hostnames
- Logging to files instead of stdout/stderr
- Health checks that perform database writes
- Shutdown without connection draining
- `System.exit()` / `os.Exit()` / `process.exit()` without cleanup

> Read `skills/observability/SKILL.md` for metrics, alerting, and SLO definitions.
