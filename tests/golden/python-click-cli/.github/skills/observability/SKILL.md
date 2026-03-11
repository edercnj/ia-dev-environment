---
name: observability
description: >
  Knowledge Pack: Observability -- Distributed tracing, metrics naming
  conventions, structured logging with mandatory fields, health checks,
  correlation IDs, and OpenTelemetry integration for my-cli-tool.
---

# Knowledge Pack: Observability

## Summary

Observability conventions for my-cli-tool using python 3.9 with click.

### Distributed Tracing

- Instrument all inbound/outbound calls with spans
- Mandatory span attributes: `service.name`, `operation`, `status`, `duration`
- Propagate trace context across service boundaries (W3C Trace Context)
- Create child spans for significant internal operations

### Metrics

- Naming convention: `{service}_{subsystem}_{metric}_{unit}`
- Mandatory metrics: request count, latency histogram, error rate
- Use labels sparingly (low cardinality only)
- RED method: Rate, Errors, Duration for every service

### Structured Logging

- JSON format with mandatory fields: `timestamp`, `level`, `service`, `traceId`, `spanId`
- Correlation ID propagation across all log entries in a request
- Log levels: ERROR (actionable), WARN (degraded), INFO (business events), DEBUG (development)
- Never log sensitive data (PII, credentials, tokens)

### Health Checks

- Liveness: application process is running (`/health/live`)
- Readiness: application can serve traffic (`/health/ready`)
- Startup: application has completed initialization (`/health/started`)
- Each probe returns HTTP 200 (healthy) or 503 (unhealthy)

## References

- `.github/skills/observability/SKILL.md` -- Full observability reference
