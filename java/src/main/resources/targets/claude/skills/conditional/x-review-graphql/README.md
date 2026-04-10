# x-review-graphql

> GraphQL Schema and Resolver Review -- validates GraphQL schema design, resolver implementation, security patterns, and observability.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `interfaces` contains `type: graphql` |
| **Invocation** | `/x-review-graphql [schema-file or resolver-name]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when the project uses the GraphQL protocol in its interface configuration.

## What It Does

Reviews GraphQL schema design, resolver implementation, security patterns, and observability for compliance with best practices. Validates naming conventions, Relay Connection spec for pagination, mutation input/payload design, subscription lifecycle, DataLoader usage for N+1 prevention, query complexity and depth limiting, field-level authorization, and error handling patterns.

## Usage

```
/x-review-graphql
/x-review-graphql schema.graphqls
/x-review-graphql OrderResolver
```

## See Also

- [x-review-api](../x-review-api/) -- REST API design review
- [x-review-grpc](../x-review-grpc/) -- gRPC service definition review
- [x-test-contract-lint](../x-test-contract-lint/) -- API contract validation
