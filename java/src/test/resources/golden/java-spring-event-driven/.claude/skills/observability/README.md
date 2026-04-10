# observability

> Observability principles: distributed tracing (span trees, mandatory attributes), metrics naming conventions, structured logging with mandatory fields, health checks (liveness/readiness/startup), correlation IDs, and OpenTelemetry integration.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-review` (Observability specialist), `x-dev-story-implement`, `x-ops-troubleshoot`, `devops-engineer` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Distributed tracing and span tree design
- Metrics collection and naming conventions
- Structured logging with mandatory fields
- Health checks (liveness, readiness, startup probes)
- Correlation ID propagation
- OpenTelemetry setup and configuration
- SLO/SLI framework (request-based, window-based)
- Error budget management and burn rate alerts
- Alerting strategy and fatigue prevention

## Key Concepts

This pack defines the three pillars of observability -- traces, metrics, and logs -- with concrete implementation patterns for each. It establishes mandatory structured logging fields (timestamp, level, trace_id, span_id, service), SLI measurement methods, and error budget policies with escalation ladders. The alerting strategy covers severity-based routing, PagerDuty/OpsGenie integration, on-call rotation, and alert fatigue prevention through deduplication, grouping, and actionability reviews.

## See Also

- [sre-practices](../sre-practices/) — Error budgets, incident management, and on-call practices
- [infrastructure](../infrastructure/) — Health probe endpoints and Kubernetes integration
- [resilience](../resilience/) — Resilience metrics and circuit breaker monitoring
