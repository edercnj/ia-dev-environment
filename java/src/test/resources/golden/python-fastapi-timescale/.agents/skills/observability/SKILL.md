---
name: observability
description: "Observability principles: distributed tracing (span trees, mandatory attributes), metrics naming conventions, structured logging with mandatory fields, health checks (liveness/readiness/startup), correlation IDs, and OpenTelemetry integration."
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Observability

## Purpose

Provides comprehensive observability patterns for {{LANGUAGE}} {{FRAMEWORK}}, enabling complete visibility into system behavior through distributed tracing, metrics collection, and structured logging. Includes span tree design, mandatory metric naming conventions, logging standards, health check implementation, and OpenTelemetry integration.

## Quick Reference (always in context)

See `references/observability-principles.md` for the essential observability summary (3 pillars: traces, metrics, logs; span tree pattern; mandatory attributes; health checks).

## Detailed References

Read these files for implementation patterns:

| Reference | Content |
|-----------|---------|
| `patterns/observability/distributed-tracing.md` | Span tree design, root span identification, child span creation, span lifecycle, context propagation, W3C Trace Context headers, baggage propagation |
| `patterns/observability/metrics-collection.md` | Metric types (counter, gauge, histogram, summary), naming convention (service.operation.metric), mandatory labels/tags, cardinality management, metric aggregation, dashboard patterns |
| `patterns/observability/structured-logging.md` | JSON log format, mandatory fields (timestamp, level, logger, message, trace_id, span_id), context objects, log levels (DEBUG/INFO/WARN/ERROR), masking sensitive data, structured field extraction |
| `patterns/observability/health-checks.md` | Liveness probe (process alive), readiness probe (can serve traffic), startup probe (initialization complete), probe implementation, dependency checks, degradation levels, Kubernetes integration |
| `patterns/observability/correlation-ids.md` | Correlation ID generation, propagation across services, header naming (X-Request-ID, X-Correlation-ID), storage in logs/traces, client-side persistence |
| `patterns/observability/opentelemetry-setup.md` | OTel agent/SDK initialization, instrumentation libraries, exporter configuration (OTLP, Jaeger, Prometheus), environment variables, sampling strategies, span processors |
| `references/alerting-patterns.md` | Alerting patterns (symptom-based, golden signals, multi-window multi-burn-rate), anti-patterns (alert on every metric, threshold-only, missing runbook links) |

## SLO/SLI Framework

### SLI Types

| Type | Description | Use Case |
|------|-------------|----------|
| **Request-based** | Ratio of good events to total events in a request/response model | HTTP services, gRPC endpoints, API calls |
| **Window-based** | Fraction of time windows where the metric meets a threshold | Batch jobs, background workers, scheduled tasks |

### SLI Measurement Methods

| Method | Description | Pros | Cons |
|--------|-------------|------|------|
| **Server-side** | Measured at the application or load balancer | Easy to implement, low overhead | Misses client-perceived failures (DNS, network) |
| **Client-side** | Measured at the client or edge proxy | Reflects user experience accurately | Higher implementation complexity, data collection cost |
| **Synthetic** | Probes that simulate user requests at regular intervals | Detects issues before users report them | Does not reflect real traffic patterns |

### Window Types

| Window | Description | Best For |
|--------|-------------|----------|
| **Rolling** | Sliding window (e.g., last 30 days from now) | Continuous monitoring, real-time dashboards |
| **Calendar** | Fixed calendar period (e.g., January 1-31) | Business reporting, compliance audits |

### SLI Specification Pattern

```
SLI = (good events / valid events) x 100%

Where:
- good events  = requests that meet the quality threshold
- valid events = all requests minus excluded categories
                 (health checks, internal probes, test traffic)
```

## Error Budget Management

### Error Budget Calculation

```
Error Budget = (1 - SLO Target) x Time Window

Examples:
  99.9% SLO over 30 days = 0.1% x 43,200 min = 43.2 min downtime allowed
  99.95% SLO over 30 days = 0.05% x 43,200 min = 21.6 min downtime allowed
  99.99% SLO over 30 days = 0.01% x 43,200 min = 4.32 min downtime allowed
```

### Burn Rate Alerts

| Alert Type | Burn Rate | Window | Severity | Action |
|-----------|-----------|--------|----------|--------|
| **Fast Burn** | 14.4x | 1 hour | P1 — Page | Immediate response, on-call paged |
| **Slow Burn** | 6x | 6 hours | P2 — Ticket | Investigation within 30 min (business hours) |
| **Steady Burn** | 3x | 3 days | P3 — Notification | Review in next working session |

### Exhaustion Policy (Escalation Ladder)

| Budget Consumed | Action | Owner |
|----------------|--------|-------|
| < 50% | Normal operations | Engineering Team |
| 50% | Review recent deployments and changes | Tech Lead |
| 75% | Freeze non-critical deployments | Engineering Manager |
| 90% | Prioritize reliability work exclusively | SRE + Engineering Manager |
| 100% | Freeze all deployments; incident response | VP Engineering |

### Budget Allocation Between Teams

- Each team owns the error budget for their services
- Shared dependencies: budget impact split proportionally by traffic
- Platform team maintains a separate budget for infrastructure components
- Cross-team budget disputes escalated to SRE leadership

## Alerting Strategy

### Alert Routing by Severity

| Severity | Route | Response Time | Escalation |
|----------|-------|---------------|------------|
| **P1 — Critical** | PagerDuty on-call | < 5 min ACK | Auto-escalate after 15 min |
| **P2 — High** | Slack #sre-alerts + Jira ticket | < 30 min (business hours) | Escalate after 2 hours |
| **P3 — Medium** | Slack #sre-notifications | Next business day | Review in weekly SRE meeting |
| **P4 — Low** | Dashboard only | Best effort | Batch review monthly |

### PagerDuty/OpsGenie Integration Patterns

- **Service mapping**: One PagerDuty service per microservice or bounded context
- **Escalation policy**: Primary on-call -> Secondary on-call -> Engineering Manager -> VP
- **Maintenance windows**: Auto-suppress alerts during planned maintenance
- **Incident linking**: Auto-create incident in PagerDuty when P1 alert fires

### On-Call Rotation Alerting

- Primary and secondary on-call with weekly rotation
- Handoff includes: active incidents, error budget status, recent changes
- On-call engineer has authority to rollback deployments
- Follow-the-sun rotation for globally distributed teams

### Alert Fatigue Prevention

| Strategy | Description |
|----------|-------------|
| **Deduplication** | Group identical alerts within a 5-minute window |
| **Grouping** | Aggregate related alerts (same service, same SLI) into a single notification |
| **Silencing** | Suppress known-noisy alerts during maintenance or known degradation |
| **Flap Detection** | Suppress alerts that rapidly toggle between firing and resolved |
| **Actionability Review** | Monthly review: every alert must have a runbook; remove alerts nobody acts on |
