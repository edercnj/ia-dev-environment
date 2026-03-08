---
name: protocols
description: >
  Knowledge Pack: Communication Protocols -- REST, gRPC, GraphQL, WebSocket,
  and event-driven messaging conventions including URL structure, versioning,
  error handling, schema design, and integration patterns for my-spring-service.
---

# Knowledge Pack: Communication Protocols

## Summary

Protocol conventions for my-spring-service using java 21 with spring-boot.

### REST (OpenAPI 3.1)

- URL structure: `/api/v1/{resource}` (plural nouns, lowercase hyphens)
- Versioning: URL path prefix (`/v1/`, `/v2/`)
- Error responses: RFC 7807 ProblemDetail format
- Content negotiation: `Accept` and `Content-Type` headers

### gRPC (Proto3)

- Service definitions in `.proto` files with strict backward compatibility
- Unary, server-streaming, client-streaming, bidirectional patterns
- Deadline propagation and cancellation across service boundaries
- Status codes: use canonical gRPC codes, not HTTP equivalents

### GraphQL

- Schema-first design with SDL (Schema Definition Language)
- Query complexity limits and depth restrictions
- Pagination: Relay-style cursor-based connections
- Error handling: `errors` array with extensions for structured metadata

### WebSocket

- Connection lifecycle: handshake, heartbeat, reconnection with backoff
- Message framing: JSON or Protocol Buffers with type discriminator
- Authentication: token-based during handshake, periodic revalidation

### Event-Driven Messaging

- Event naming: past tense (`OrderCreated`, `PaymentProcessed`)
- Envelope: CloudEvents specification for metadata
- Topic naming: `{domain}.{entity}.{event-type}`
- Idempotency: consumer-side deduplication via event ID

## References

- `.claude/skills/protocols/SKILL.md` -- Full protocol conventions
- `.claude/skills/protocols/references/` -- Detailed documentation
