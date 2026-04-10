# x-review-grpc

> gRPC Service Review -- validates gRPC service definitions, proto3 conventions, implementation patterns, and operational readiness.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `interfaces` contains `type: grpc` |
| **Invocation** | `/x-review-grpc [service-name or proto-file]` |
| **Reads** | protocols (references: grpc-conventions) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when the project uses the gRPC protocol in its interface configuration.

## What It Does

Reviews gRPC service definitions, implementation patterns, and operational readiness for compliance with proto3 conventions. Validates package naming, service/message/field naming conventions, enum design with UNSPECIFIED=0, versioning and backward compatibility rules, streaming patterns, gRPC status code usage, health check implementation, and deadline/timeout propagation.

## Usage

```
/x-review-grpc
/x-review-grpc service.proto
/x-review-grpc PaymentService
```

## See Also

- [x-review-api](../x-review-api/) -- REST API design review
- [x-review-graphql](../x-review-graphql/) -- GraphQL schema and resolver review
- [x-test-contract-lint](../x-test-contract-lint/) -- Protobuf contract validation
