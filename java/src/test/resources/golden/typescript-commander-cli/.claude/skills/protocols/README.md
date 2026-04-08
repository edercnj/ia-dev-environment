# protocols

> Protocol conventions: REST (OpenAPI 3.1), gRPC (Proto3), GraphQL, WebSocket, and event-driven messaging. URL structure, versioning, error handling per protocol, schema design, and integration patterns.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-dev-implement`, `x-review` (API specialist), `x-dev-lifecycle`, `architect` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- REST conventions (resource naming, HTTP methods, status codes, RFC 7807 errors, pagination)
- OpenAPI 3.1 specification structure and multi-file organization
- gRPC conventions (Proto3, service/message naming, streaming patterns, deadline propagation)
- gRPC versioning (package-based, backward compatibility rules, migration strategies)
- GraphQL conventions (schema design, complexity analysis, DataLoader for N+1 prevention)
- WebSocket conventions (message framing, heartbeat, reconnection, backpressure)
- Event-driven messaging (CloudEvents envelope, event naming, schema registry, ordering guarantees)
- Broker patterns (topic naming, partition strategies, consumer lag, dead letter topics)

## Key Concepts

This pack defines protocol-specific design and implementation patterns across five communication protocols: REST, gRPC, GraphQL, WebSocket, and event-driven messaging. Each protocol section covers its idiomatic conventions for naming, versioning, error handling, and schema evolution. The event-driven section covers both the CloudEvents envelope standard and broker-level patterns including partition strategies and consumer lag monitoring. All protocols share common API design principles from the api-design pack while respecting each protocol's unique characteristics.

## See Also

- [api-design](../api-design/) — Universal API design principles (error handling, pagination, validation)
- [patterns-outbox](../patterns-outbox/) — Transactional outbox for reliable event publishing
- [resilience](../resilience/) — Timeout patterns and retry strategies for protocol-level resilience
