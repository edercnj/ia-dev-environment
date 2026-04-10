# x-review-api

> REST API Design Review -- validates REST API endpoints for RFC 7807 error responses, pagination, URL versioning, OpenAPI documentation, status codes, and DTO patterns.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `interfaces` contains REST protocol |
| **Invocation** | `/x-review-api [endpoint-path or feature-name]` |
| **Reads** | api-design (references: api-design-principles), protocols (references: rest-conventions) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when the project uses the REST protocol in its interface configuration.

## What It Does

Reviews REST API design for compliance with best practices: RFC 7807 error responses, pagination wrappers, URL versioning, OpenAPI annotations, proper HTTP status codes, and DTO separation from domain models. Discovers all endpoints, validates URL structure, checks status code usage per HTTP method, and verifies error response formats. Produces a categorized review report with actionable findings.

## Usage

```
/x-review-api
/x-review-api /api/v1/transactions
/x-review-api payment-processing
```

## See Also

- [x-test-contract-lint](../x-test-contract-lint/) -- API contract validation (OpenAPI, AsyncAPI, Protobuf)
- [x-review-graphql](../x-review-graphql/) -- GraphQL schema and resolver review
- [x-review-grpc](../x-review-grpc/) -- gRPC service definition review
- [run-smoke-api](../run-smoke-api/) -- REST API smoke tests
