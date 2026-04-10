# instrument-otel

> OpenTelemetry Instrumentation -- adds or reviews distributed tracing, metrics, and structured logging using OpenTelemetry SDK with OTLP export.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `observability != "none"` |
| **Invocation** | `/instrument-otel [component-name or 'full']` |
| **Reads** | observability (references: observability-principles) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when observability is configured to a value other than "none" in the project configuration.

## What It Does

Adds or reviews OpenTelemetry instrumentation across the application, covering the three observability pillars: distributed traces (spans with parent-child relationships), metrics (counters, histograms, gauges), and structured logs with trace correlation. Verifies OTel dependencies, checks configuration (service name, OTLP endpoint, sampling strategy), instruments inbound/outbound spans, adds business metrics, and ensures log correlation with trace/span IDs. Uses OTLP exporter for vendor-agnostic collection.

## Usage

```
/instrument-otel
/instrument-otel full
/instrument-otel PaymentService
/instrument-otel TransactionRepository
```

## See Also

- [x-review-events](../x-review-events/) -- Event-driven review with observability checks
- [setup-environment](../setup-environment/) -- Dev environment setup including OTel collector
- [x-review-gateway](../x-review-gateway/) -- Gateway review with tracing validation
