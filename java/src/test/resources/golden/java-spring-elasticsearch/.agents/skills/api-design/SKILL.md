---
name: api-design
description: "API design principles: {{LANGUAGE}}-specific patterns for REST/gRPC/GraphQL. URL structure, status codes, RFC 7807 errors, pagination, content negotiation, validation, request/response shaping, versioning strategies, and protocol conventions."
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: API Design

## Purpose

Provides comprehensive API design patterns for {{LANGUAGE}} {{FRAMEWORK}}, covering REST, gRPC, GraphQL, and WebSocket protocols. Includes URL structure conventions, HTTP status codes, RFC 7807 error handling, pagination strategies, content negotiation, input validation, and protocol-specific best practices.

## Quick Reference (always in context)

See `references/api-design-principles.md` for the essential API design summary (URL structure, status codes, error format, pagination).

## Detailed References

Read these files for protocol-specific conventions:

| Reference | Content |
|-----------|---------|
| `protocols/rest/rest-conventions.md` | REST resource naming, HTTP methods, status codes, pagination (cursor vs offset), filtering, sorting, HATEOAS decision guide, error responses |
| `protocols/rest/openapi-conventions.md` | OpenAPI 3.1 specification format, schema organization, $ref reuse, example values, security schemes, operation IDs, tags, discriminator for polymorphism |
| `protocols/grpc/grpc-conventions.md` | Proto3 style guide, package/service/message naming, field conventions, enum design, oneof usage, streaming patterns, deadline propagation, error codes, health checks |
| `protocols/grpc/grpc-versioning.md` | Package-based versioning (v1, v2), backward compatibility rules, field deprecation, reserved fields, migration strategies, version lifecycle |
| `protocols/graphql/graphql-conventions.md` | Schema design, query patterns, mutation naming, subscription patterns, error handling, complexity analysis, DataLoader for N+1 prevention |
| `protocols/websocket/websocket-conventions.md` | Message framing, heartbeat/keep-alive, reconnection logic, connection draining, error handling over persistent connections |
| `protocols/event-driven/event-conventions.md` | CloudEvents envelope, event naming (reverse domain + past tense), schema registry, event versioning, correlation IDs, ordering guarantees |
| `protocols/event-driven/broker-patterns.md` | Topic naming, partition strategies, consumer lag monitoring, producer acknowledgments, retention policies, schema evolution, dead letter topics |
| `references/api-deprecation-checklist.md` | Step-by-step checklist for deprecating an API endpoint safely |

## API Deprecation Strategy

Defines the lifecycle for deprecating API endpoints, ensuring consumers have adequate notice and migration paths.

### Sunset Header (RFC 8594)

The `Sunset` HTTP response header (RFC 8594) communicates the deprecation date of an API endpoint:

```
Sunset: Sat, 31 Dec 2025 23:59:59 GMT
```

- Format follows RFC 7231 HTTP-date
- Include in responses as soon as deprecation is announced
- Consumers MUST check for this header and plan migration
- Combine with `Deprecation: true` header during the warn phase

### Deprecation Timeline

| Phase | Timing | Action | HTTP Response |
|-------|--------|--------|---------------|
| Announce | T-6 months | Changelog entry, email subscribers, dashboard banner | Normal (200) |
| Warn | T-3 months | Add `Deprecation: true` header, log consumer usage | Normal + `Deprecation: true` |
| Sunset | T-0 | Activate Sunset header with removal date | Normal + `Sunset: <date>` |
| Remove | T + grace period | Endpoint removed, returns 410 Gone | 410 Gone |

### Migration Guides

- Document alternative endpoints with full request/response examples
- Provide code examples for migrating from deprecated to new endpoints
- Offer automated migration tools or scripts where feasible
- Maintain a migration FAQ addressing common consumer concerns

### Consumer Notification

- **Email**: Notify API key holders and registered webhook subscribers
- **API Changelog**: Structured entry with date, endpoint, change type, and migration link
- **Dashboard Banner**: Visual notice in developer portal
- **Response Header**: `Deprecation: true` header in HTTP responses

### API Changelog Format

```
## [YYYY-MM-DD] — <endpoint>

- **Change Type**: deprecated | removed | added | modified
- **Endpoint**: `METHOD /path`
- **Migration Guide**: [link to migration documentation]
- **Sunset Date**: YYYY-MM-DD (if applicable)
- **Replacement**: `METHOD /new-path` (if applicable)
```

## API Versioning Patterns

Strategies for versioning APIs, with trade-offs and recommendations per use case.

### URI Versioning

Pattern: `/v1/resources`, `/v2/resources`

- **Pros**: Explicit, easily discoverable, simple routing, works with all HTTP clients
- **Cons**: URL pollution, harder to maintain multiple version routes, version embedded in URL
- **When to use**: Public APIs with long-lived versions, APIs consumed by external parties

### Header Versioning

Pattern: `Accept-Version: v2` or custom header `X-API-Version: 2`

- **Pros**: Clean URLs, version decoupled from resource path
- **Cons**: Hidden from URL inspection, harder to test in browser, requires header awareness
- **When to use**: Internal APIs, microservice-to-microservice communication

### Query Parameter Versioning

Pattern: `GET /resources?version=2`

- **Pros**: Explicit in URL, easy to test, simple to implement
- **Cons**: Mixes versioning with query concerns, non-standard
- **When to use**: Quick iteration, prototyping, internal tooling

### Content Negotiation Versioning

Pattern: `Accept: application/vnd.api+json;version=2`

- **Pros**: RESTful, fine-grained control, follows HTTP standards
- **Cons**: Complex implementation, limited tooling support, harder to debug
- **When to use**: API-first companies, public APIs requiring strict REST compliance

### Versioning Recommendation

| API Type | Recommended Strategy |
|----------|---------------------|
| Public / External | URI versioning (`/v1/`) |
| Internal / Microservices | Header versioning (`Accept-Version`) |
| Prototyping / Internal tools | Query parameter (`?version=`) |
| API-first / Strict REST | Content negotiation (`application/vnd.`) |
