---
name: observability
description: >
  Knowledge Pack: Observability -- Distributed tracing, metrics naming
  conventions, structured logging with mandatory fields, health checks,
  correlation IDs, and OpenTelemetry integration for {project_name}.
---

# Knowledge Pack: Observability

## Summary

Observability conventions for {project_name} using {language_name} {language_version} with {framework_name}.

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

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/) -- Traces, metrics, and logs instrumentation standard
- [W3C Trace Context](https://www.w3.org/TR/trace-context/) -- Distributed tracing context propagation
- [The RED Method](https://grafana.com/blog/2018/08/02/the-red-method-how-to-instrument-your-services/) -- Rate, Errors, Duration monitoring methodology
- [Google SRE Book — Monitoring](https://sre.google/sre-book/monitoring-distributed-systems/) -- Distributed systems observability practices
