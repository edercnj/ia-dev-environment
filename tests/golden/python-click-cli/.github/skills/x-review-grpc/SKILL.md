---
name: x-review-grpc
description: >
  Skill: gRPC Service Review -- Validates gRPC service definitions, proto3
  conventions, protobuf message design, implementation patterns, and operational
  readiness. Use when reviewing proto files or gRPC service implementations.
---

# Skill: gRPC Service Review

## Description

Reviews gRPC service definitions, implementation patterns, and operational readiness for compliance with proto3 conventions, error handling standards, and observability requirements.

**Condition**: This skill applies when the project uses gRPC protocol.

## Prerequisites

- Proto files (`*.proto`) exist in the codebase
- gRPC framework dependency is configured
- gRPC code generation plugin is configured in build tool

## Knowledge Pack References

Before reviewing, read the gRPC conventions:
- `.github/skills/protocols/SKILL.md` -- Proto3 style guide, naming, streaming patterns, error codes, health checks

## Execution Flow

1. **Discover proto files** -- Scan for `*.proto` in project:
   - List all proto files with their package declarations
   - Identify service definitions and message types

2. **Discover gRPC service implementations** -- Scan for classes implementing generated stubs:
   - Identify server-side service implementations
   - Identify client-side stub usage

3. **Validate proto file conventions** -- Check each proto file:
   - Package naming follows reverse domain with version suffix
   - Service, message, and field naming conventions
   - Enum design with UNSPECIFIED=0
   - Versioning and compatibility rules

4. **Validate implementation patterns** -- Check server and client code:
   - Deadline/timeout propagation
   - Error model usage
   - Health checks and graceful shutdown
   - Interceptor usage for cross-cutting concerns

5. **Validate operational readiness** -- Check observability:
   - OpenTelemetry trace propagation
   - Metrics per method
   - Structured logging
   - Sensitive data exclusion

6. **Generate report** -- Summarize findings as checklist

## Proto3 Style Checklist (12 points)

### Package & Naming (1-4)
1. Package follows reverse domain convention with version suffix (`com.company.service.v1`)
2. Service names are PascalCase and end with `Service` (`TransactionService`)
3. Message names are PascalCase (`CreateTransactionRequest`, `TransactionResponse`)
4. Field names are snake_case (`transaction_id`, `created_at`)

### Message Design (5-10)
5. Every enum has `UNSPECIFIED = 0` as first value (proto3 default value safety)
6. Request/Response messages are method-specific (no generic `Request` reuse)
7. `oneof` used for mutually exclusive fields (not separate booleans)
8. `repeated` fields have bounded size expectations documented in comments
9. `google.protobuf.Timestamp` used for datetime (not string or int64)
10. `google.protobuf.FieldMask` used for partial updates

### Versioning & Compatibility (11-12)
11. Breaking changes go in new package version (`v1` -> `v2`), never modify existing
12. Deprecated fields use `reserved` statement (field number AND name)

## Implementation Checklist (12 points)

### Server-Side (13-18)
13. Deadline/timeout propagation: server reads deadline from context, propagates to downstream calls
14. Error model uses `google.rpc.Status` with details (not plain `StatusRuntimeException` without context)
15. Server reflection enabled for development/staging (disabled in production if security requires)
16. Health check protocol implemented (`grpc.health.v1.Health`)
17. Graceful shutdown drains in-flight RPCs before terminating
18. Interceptors used for cross-cutting concerns (auth, logging, tracing, metrics)

### Client-Side (19-22)
19. Deadlines always set on client calls (never unlimited timeout)
20. Retry policy configured with exponential backoff
21. Channel management: reuse channels, don't create per-call
22. Client-side load balancing configured (round-robin or pick-first)

### Streaming (23-24)
23. Server streaming: flow control implemented (backpressure)
24. Bidirectional streaming: error handling covers both directions independently

## Observability Checklist (4 points)

25. gRPC interceptor for OpenTelemetry trace propagation (W3C context)
26. Metrics: `grpc.server.duration`, `grpc.server.request.count`, `grpc.server.response.count` per method
27. Structured logging includes: method name, status code, duration, trace_id
28. No sensitive data in metadata, logs, or traces

## Output Format

```
## gRPC Review -- [Service/Change Description]

### Proto Compliance: HIGH / MEDIUM / LOW
### Implementation Quality: HIGH / MEDIUM / LOW

### Proto Findings
1. [Finding with file, line, issue, fix]

### Implementation Findings
1. [Finding with file, line, issue, fix]

### Verdict: APPROVE / REQUEST CHANGES
```

## Detailed References

For in-depth guidance on gRPC patterns, consult:
- `.github/skills/x-review-grpc/SKILL.md`
- `.github/skills/protocols/SKILL.md`
