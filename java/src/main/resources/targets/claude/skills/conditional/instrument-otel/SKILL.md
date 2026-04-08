---
name: instrument-otel
description: "Adds or reviews distributed tracing, metrics, and structured logging using OpenTelemetry SDK with OTLP export."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[component-name or 'full']"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: OpenTelemetry Instrumentation

## Purpose

Add or review OpenTelemetry instrumentation across the application. Cover the three observability pillars: distributed traces (spans), metrics (counters, histograms, gauges), and structured logs with trace correlation. Use OTLP exporter for vendor-agnostic collection.

## Activation Condition

Include this skill when observability is not "none" in the project configuration.

## Triggers

- `/instrument-otel full` -- instrument all components
- `/instrument-otel transaction-service` -- instrument a specific component
- `/instrument-otel persistence-layer` -- instrument a specific layer

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `component` | String | No | `full` | Component name to instrument, or `full` for all |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| observability | `skills/observability/references/observability-principles.md` | 3 pillars (traces, metrics, logs), span tree pattern, mandatory attributes, health checks |

## Prerequisites

- OpenTelemetry SDK dependency added to {{BUILD_FILE}}
- OTLP exporter configured (default: `http://otel-collector:4317`)
- {{FRAMEWORK}} OpenTelemetry integration enabled

## Workflow

### Step 1 — Verify OTel Dependencies

Check {{BUILD_FILE}} for:
- OpenTelemetry SDK or framework-specific OTel extension
- OTLP exporter (gRPC or HTTP)
- Confirm dependency versions are compatible

### Step 2 — Check Configuration

Verify OTel properties:
- `otel.service.name` set to project name
- `otel.exporter.otlp.endpoint` externalized (env var)
- Traces, metrics, and logs enabled
- Sampling strategy configured (e.g., `parentbased_always_on`)
- OTel disabled in dev/test profiles, enabled in staging/prod

### Step 3 — Instrument Traces

For the target component:
- Create root span for each inbound request/message
- Add child spans for: parsing, validation, business logic, persistence, response
- Set mandatory span attributes: operation type, identifier, status
- Set span status (OK or ERROR) and record exceptions
- Ensure sensitive data (passwords, tokens, PII) is NEVER in span attributes

### Step 4 — Instrument Metrics

Add application-specific metrics:
- **Counter**: requests processed, errors, specific event counts
- **Histogram**: request duration, query duration
- **UpDownCounter**: active connections, active sessions, pool usage
- Use consistent naming: `{{PROJECT_PREFIX}}.metric.name`
- Add relevant tags/attributes for filtering

### Step 5 — Instrument Logging

Set up structured logging:
- JSON format in production, text in dev
- Automatic trace_id/span_id correlation in log entries
- MDC/context fields for domain identifiers
- Appropriate log levels (DEBUG, INFO, WARN, ERROR)
- Sensitive data masking before logging

### Step 6 — Add Health Checks

Implement probes:
- Liveness: application process is alive
- Readiness: dependencies (DB, external services) are reachable
- Startup: application initialization complete

### Step 7 — Validate

Run {{BUILD_COMMAND}} and verify:
- No compilation errors from instrumentation code
- OTel configuration loads without warnings
- Health endpoints respond correctly

## Metric Naming Convention

- Prefix: `{{PROJECT_PREFIX}}.`
- Separator: `.` (dot)
- Style: snake_case for composite names
- Units: `seconds`, `bytes`, `1` (dimensionless count)

## Protocol-Specific Instrumentation

### gRPC (when interfaces contain grpc)
- Server interceptor for automatic span creation per RPC
- Client interceptor for outbound call tracing
- Span name: `grpc.{package}.{service}/{method}`
- Attributes: `rpc.system=grpc`, `rpc.method`, `rpc.service`, `rpc.grpc.status_code`
- Metrics: `rpc.server.duration`, `rpc.server.request.size`, `rpc.server.response.size`

### GraphQL (when interfaces contain graphql)
- Span per resolver execution
- Root span for query/mutation/subscription
- Attributes: `graphql.operation.name`, `graphql.operation.type`, `graphql.document` (sanitized)
- Metrics: `graphql.operation.duration`, `graphql.resolver.duration`, `graphql.error.count`

### Event-Driven (when interfaces contain event-consumer/event-producer)
- Producer span: `{topic} send` (span kind: PRODUCER)
- Consumer span: `{topic} receive` (span kind: CONSUMER)
- Attributes: `messaging.system`, `messaging.destination`, `messaging.operation`, `messaging.message.id`
- Trace context propagated in message headers
- Consumer lag as gauge metric

### WebSocket (when interfaces contain websocket)
- Connection span (long-lived, with events for messages)
- Attributes: `websocket.connection.id`, `websocket.message.type`
- Metrics: `websocket.connections.active`, `websocket.messages.sent`, `websocket.messages.received`

## Review Checklist

- [ ] OTel SDK dependency present in {{BUILD_FILE}}
- [ ] OTLP endpoint externalized via environment variable
- [ ] OTel disabled in dev/test, enabled in staging/prod
- [ ] Root span created for each inbound operation
- [ ] Child spans for sub-operations (parse, validate, persist, respond)
- [ ] Mandatory attributes set on spans (operation, identifier, status)
- [ ] ZERO sensitive data in spans, metrics, or logs
- [ ] Counter, histogram, and gauge metrics defined
- [ ] Structured JSON logging in production
- [ ] Trace ID and span ID correlated in log entries
- [ ] Health checks implemented (liveness, readiness, startup)
- [ ] Sampling strategy configured appropriately

## Error Handling

| Scenario | Action |
|----------|--------|
| OTel SDK dependency missing | Report missing dependency with install instructions |
| OTLP endpoint unreachable | Warn and continue; instrumentation code is independent of collector availability |
| Build fails after instrumentation | Roll back changes, report compilation errors with file and line |
