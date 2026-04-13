# x-test-contract-lint

> Validates API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) against their specifications. Reports structural errors, missing fields, and spec violations.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `testing.contract_tests = true` |
| **Invocation** | `/x-test-contract-lint [contract-file-path]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when contract testing is enabled in the project configuration, indicating the project uses API contracts that need validation.

## What It Does

Validates API contract files against their respective specifications before approval. Automatically detects the contract format (OpenAPI 3.1, AsyncAPI 2.6, or Protobuf 3) from file extensions and content, then runs format-specific validation rules. Checks structural completeness, required fields, schema references, error response formats (RFC 7807), and naming conventions. Designed for pre-PR contract validation in Phase 0.5 of the development lifecycle.

## Usage

```
/x-test-contract-lint
/x-test-contract-lint docs/api-openapi.yaml
/x-test-contract-lint proto/service.proto
/x-test-contract-lint docs/events-asyncapi.yaml
```

## See Also

- [x-test-contract](../x-test-contract/) -- Consumer-driven contract test execution
- [x-review-api](../x-review-api/) -- REST API design review
- [x-review-grpc](../x-review-grpc/) -- gRPC service definition review
