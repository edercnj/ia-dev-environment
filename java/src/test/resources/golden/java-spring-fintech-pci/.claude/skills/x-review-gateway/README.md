# x-review-gateway

> Reviews API gateway configuration for routing rules, authentication, rate limiting, CORS, security headers, and observability integration.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | API gateway configured in project infrastructure |
| **Invocation** | `/x-review-gateway [gateway config files or PR]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when the project includes an API gateway component (Kong, Istio, AWS API Gateway, Traefik, or similar) in its infrastructure configuration.

## What It Does

Reviews API gateway configuration against best practices defined in the project's gateway knowledge pack. Validates routing rules, authentication configuration, rate limiting policies, CORS settings, security headers, TLS configuration, and observability integration (access logs, tracing). Produces a gateway review report with APPROVE or REQUEST CHANGES verdict.

## Usage

```
/x-review-gateway
/x-review-gateway k8s/gateway.yaml
/x-review-gateway 42
```

## See Also

- [x-review-api](../x-review-api/) -- REST API design review
- [x-security-infra](../x-security-infra/) -- Infrastructure-as-Code security scanning
- [x-obs-instrument](../x-obs-instrument/) -- OpenTelemetry instrumentation for gateway tracing
