# api-design

> API design principles: language-specific patterns for REST/gRPC/GraphQL. URL structure, status codes, RFC 7807 errors, pagination, content negotiation, validation, request/response shaping, versioning strategies, and protocol conventions.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-implement, x-dev-story-implement, x-review (API specialist), x-review-pr, architect agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- REST resource naming, HTTP methods, status codes, pagination, HATEOAS, error responses
- OpenAPI 3.1 specification format, schema organization, security schemes
- gRPC Proto3 style guide, streaming patterns, deadline propagation, error codes
- GraphQL schema design, query/mutation/subscription patterns, DataLoader for N+1 prevention
- WebSocket message framing, heartbeat/keep-alive, reconnection logic
- Event-driven conventions: CloudEvents envelope, schema registry, event versioning
- API deprecation strategy with Sunset header (RFC 8594) and migration guides
- API versioning patterns: URI, header, query parameter, and content negotiation

## Key Concepts

This pack provides protocol-specific API design guidance covering REST, gRPC, GraphQL, WebSocket, and event-driven messaging. It enforces RFC 7807 error format for problem details, cursor-based and offset-based pagination strategies, and content negotiation best practices. The deprecation lifecycle follows a four-phase timeline (Announce, Warn, Sunset, Remove) with Sunset headers per RFC 8594. Versioning recommendations are context-dependent: URI versioning for public APIs, header versioning for internal microservices, and content negotiation for strict REST compliance.

## See Also

- [architecture](../architecture/) — Full architecture reference with package structure and dependency rules
- [coding-standards](../coding-standards/) — Clean Code rules, SOLID principles, and language-specific idioms
- [architecture-patterns](../architecture-patterns/) — Microservice, resilience, data, and integration patterns
